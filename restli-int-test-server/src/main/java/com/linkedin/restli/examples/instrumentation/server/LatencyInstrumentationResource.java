/*
   Copyright (c) 2019 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.restli.examples.instrumentation.server;

import com.linkedin.d2.balancer.URIMapper;
import com.linkedin.d2.balancer.util.URIKeyPair;
import com.linkedin.d2.balancer.util.URIMappingResult;
import com.linkedin.data.transform.DataProcessingException;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.message.timing.TimingKey;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.BatchPartialUpdateEntityRequest;
import com.linkedin.restli.client.BatchPartialUpdateEntityRequestBuilder;
import com.linkedin.restli.client.DefaultScatterGatherStrategy;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.client.ScatterGatherStrategy;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.util.PatchGenerator;
import com.linkedin.restli.client.util.RestLiClientConfig;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateEntityStatus;
import com.linkedin.restli.examples.instrumentation.api.InstrumentationControl;
import com.linkedin.restli.examples.instrumentation.client.LatencyInstrumentationRequestBuilders;
import com.linkedin.restli.server.BatchPatchRequest;
import com.linkedin.restli.server.BatchUpdateEntityResult;
import com.linkedin.restli.server.CreateKVResponse;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateEntityResponse;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.annotations.ReturnEntity;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.server.resources.ResourceContextHolder;
import com.linkedin.restli.server.util.PatchApplier;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Resource used for testing framework latency instrumentation.
 *
 * The integration test using this resource queries {@link #create(InstrumentationControl)} (the "upstream endpoint"),
 * which queries {@link #batchPartialUpdate(BatchPatchRequest)} (the "downstream endpoint"). The "upstream endpoint"
 * collects all the client-side timing data after the downstream call has completed and packs it into the original
 * server-side request context so that the integration test has access to all of it.
 *
 * The input entity itself indicates to the resource whether to use streaming or rest, whether to throw an exception at
 * both endpoints, whether to use scatter-gather for the downstream request, and what its own hostname is so it can make
 * the circular downstream request. The "upstream endpoint" sets a special header so that the integration test knows
 * which request to analyze, this is done to avoid analyzing the protocol version fetch request.
 *
 * @author Evan Williams
 */
@RestLiCollection(
    name = "latencyInstrumentation",
    namespace = "com.linkedin.restli.examples.instrumentation.client"
)
public class LatencyInstrumentationResource extends ResourceContextHolder implements KeyValueResource<Long, InstrumentationControl>
{
  public static final String HAS_CLIENT_TIMINGS_HEADER = "X-RestLi-Test-HasClientTimings";
  public static final String UPSTREAM_ERROR_CODE = "UPSTREAM_ERROR";

  private static final String DOWNSTREAM_ERROR_CODE = "DOWNSTREAM_ERROR";
  private static final int DOWNSTREAM_BATCH_SIZE = 10;

  private static final LatencyInstrumentationRequestBuilders REQUEST_BUILDERS =
      new LatencyInstrumentationRequestBuilders();

  /**
   * This is the "upstream endpoint" which is queried directly by the integration test.
   * This endpoint makes a call to {@link #batchPartialUpdate(BatchPatchRequest)} (the "downstream endpoint"),
   * then packs all the client-side timing data into the original server-side request context.
   */
  @ReturnEntity
  @RestMethod.Create
  public CreateKVResponse<Long, InstrumentationControl> create(InstrumentationControl control)
  {
    final boolean forceException = control.isForceException();
    final boolean useScatterGather = control.isUseScatterGather();
    final String uriPrefix = control.getServiceUriPrefix();

    // Build the downstream request
    final BatchPartialUpdateEntityRequestBuilder<Long, InstrumentationControl> builder =
        REQUEST_BUILDERS.batchPartialUpdateAndGet();
    final PatchRequest<InstrumentationControl> patch = PatchGenerator.diffEmpty(control);
    for (long i = 0; i < DOWNSTREAM_BATCH_SIZE; i++)
    {
      builder.input(i, patch);
    }
    final BatchPartialUpdateEntityRequest<Long, InstrumentationControl> request = builder.build();

    // Set up the Rest.li client config
    final RestLiClientConfig clientConfig = new RestLiClientConfig();
    clientConfig.setUseStreaming(control.isUseStreaming());
    if (useScatterGather)
    {
      clientConfig.setScatterGatherStrategy(new DefaultScatterGatherStrategy(new DummyUriMapper()));
    }

    final TransportClient transportClient = new HttpClientFactory.Builder()
        .build()
        .getClient(Collections.emptyMap());
    final RestClient restClient = new ForceScatterGatherRestClient(new TransportClientAdapter(transportClient), uriPrefix, clientConfig);
    final RequestContext serverRequestContext = getContext().getRawRequestContext();
    final RequestContext clientRequestContext = new RequestContext();

    // Load the timing importance threshold from the server context into the client context
    clientRequestContext.putLocalAttr(TimingContextUtil.TIMING_IMPORTANCE_THRESHOLD_KEY_NAME,
        serverRequestContext.getLocalAttr(TimingContextUtil.TIMING_IMPORTANCE_THRESHOLD_KEY_NAME));

    try
    {
      // Make the request, then assert that the returned errors (if any) are as expected
      BatchKVResponse<Long, UpdateEntityStatus<InstrumentationControl>> response =
          restClient.sendRequest(request, clientRequestContext).getResponseEntity();

      final Map<Long, ErrorResponse> errors = response.getErrors();

      if (forceException && errors.isEmpty())
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Expected failures for the downstream batch request, but found none.");
      }

      if (!forceException && !errors.isEmpty())
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Expected no failures for the downstream batch request, but found some.");
      }

      for (ErrorResponse errorResponse : errors.values())
      {
        if (!DOWNSTREAM_ERROR_CODE.equals(errorResponse.getCode()))
        {
          throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
              "Encountered a downstream failure with an unexpected or missing error code.");
        }
      }
    }
    catch (RemoteInvocationException e)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
          "Downstream failures should be batch entry failures, but encountered a top-level request failure.", e);
    }

    Map<TimingKey, TimingContextUtil.TimingContext> clientTimingsMap = TimingContextUtil.getTimingsMap(clientRequestContext);
    Map<TimingKey, TimingContextUtil.TimingContext> serverTimingsMap = TimingContextUtil.getTimingsMap(serverRequestContext);

    // Load all client timings into the server timings map
    serverTimingsMap.putAll(clientTimingsMap);
    getContext().setResponseHeader(HAS_CLIENT_TIMINGS_HEADER, Boolean.TRUE.toString());

    if (forceException)
    {
      throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, "You wanted me to fail, so I failed.")
          .setCode(UPSTREAM_ERROR_CODE);
    }

    return new CreateKVResponse<>(1L, control);
  }

  /**
   * This is the "downstream endpoint", queried by {@link #create(InstrumentationControl)} (the "upstream endpoint").
   */
  @ReturnEntity
  @RestMethod.BatchPartialUpdate
  public BatchUpdateEntityResult<Long, InstrumentationControl> batchPartialUpdate(
      BatchPatchRequest<Long, InstrumentationControl> batchPatchRequest) throws DataProcessingException
  {
    final Map<Long, UpdateEntityResponse<InstrumentationControl>> results = new HashMap<>();
    final Map<Long, RestLiServiceException> errors = new HashMap<>();

    for (Map.Entry<Long, PatchRequest<InstrumentationControl>> entry : batchPatchRequest.getData().entrySet())
    {
      // Render each patch into a normal record so we know whether or not to force a failure
      InstrumentationControl control = new InstrumentationControl();
      PatchApplier.applyPatch(control, entry.getValue());

      if (control.isForceException())
      {
        RestLiServiceException error = new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST,
            "You wanted me to fail, so I failed.")
              .setCode(DOWNSTREAM_ERROR_CODE);
        errors.put(entry.getKey(), error);
      }
      else
      {
        results.put(entry.getKey(), new UpdateEntityResponse<>(HttpStatus.S_200_OK, control));
      }
    }

    return new BatchUpdateEntityResult<>(results, errors);
  }

  /**
   * Simple extended rest client which allows requests with any scheme to use scatter-gather, not just those using d2.
   */
  private static class ForceScatterGatherRestClient extends RestClient
  {
    ForceScatterGatherRestClient(Client client, String prefix, RestLiClientConfig config)
    {
      super(client, prefix, config);
    }

    @Override
    protected <T> boolean needScatterGather(
        Request<T> request, RequestContext requestContext, ScatterGatherStrategy scatterGatherStrategy)
    {
      return (scatterGatherStrategy != null) && scatterGatherStrategy.needScatterGather(request);
    }
  }

  /**
   * Simple implementation of {@link URIMapper} which indiscriminately allows scatter-gather and simply maps URIs evenly
   * among a few dummy host URIs. Assumes that the resource key is of type long.
   */
  private static class DummyUriMapper implements URIMapper
  {
    private static final int NUM_SCATTER_GATHER_HOSTS = 3;
    private static final String SCATTER_GATHER_HOST_URI_TEMPLATE = "http://host-%d/";

    @Override
    public <K> URIMappingResult<K> mapUris(List<URIKeyPair<K>> requestUriKeyPairs)
    {
      final Map<URI, Set<K>> mappingResults = new HashMap<>();
      for (URIKeyPair<K> keyPair : requestUriKeyPairs)
      {
        final K key = keyPair.getKey();
        if (!(key instanceof Long))
        {
          throw new IllegalArgumentException("Key must be of type Long, if it's not then this is seriously broken.");
        }

        final long hostId = ((Long) key % NUM_SCATTER_GATHER_HOSTS);
        final URI uriKey = URI.create(String.format(SCATTER_GATHER_HOST_URI_TEMPLATE, hostId));
        mappingResults.computeIfAbsent(uriKey, x -> new HashSet<>()).add(key);
      }

      return new URIMappingResult<>(mappingResults, Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    public boolean needScatterGather(String serviceName)
    {
      return true;
    }
  }
}

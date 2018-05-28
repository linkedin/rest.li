/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.restli.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.URIMapper;
import com.linkedin.d2.balancer.util.URIKeyPair;
import com.linkedin.d2.balancer.util.URIMappingResult;
import com.linkedin.data.DataMap;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;;
import com.linkedin.restli.internal.client.ResponseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Default implementation of {@link ScatterGatherStrategy}, where we only handle {@link BatchRequest} with
 * BATCH_CREATE excluded.
 *
 * @author mnchen
 */
public class DefaultScatterGatherStrategy implements ScatterGatherStrategy
{
  private static final Logger log = LoggerFactory.getLogger(DefaultScatterGatherStrategy.class);
  // batch method that supporting partition or sticky routing thus this default batching strategy can be applied.
  private static final Set<ResourceMethod> BATCHING_STRATEGY_METHODS = EnumSet.of(ResourceMethod.BATCH_GET, ResourceMethod.BATCH_DELETE,
          ResourceMethod.BATCH_PARTIAL_UPDATE, ResourceMethod.BATCH_UPDATE);
  private final URIMapper _uriMapper;

  public DefaultScatterGatherStrategy(URIMapper uriMapper)
  {
    _uriMapper = uriMapper;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> boolean needScatterGather(Request<T> request) {
    if (!BATCHING_STRATEGY_METHODS.contains(request.getMethod()))
    {
      return false;
    }
    final String serviceName = request.getServiceName();
    try
    {
      return _uriMapper.needScatterGather(serviceName);
    }
    catch (ServiceUnavailableException e)
    {
      log.error("Unable to determine scatter-gather capability for service :" + serviceName + " and treat as unsupported!", e);
      return false;
    }
  }

  private <T> BatchRequest<T> safeCastRequest(Request<T> request)
  {
    if (!(request instanceof BatchRequest) || request.getMethod() == ResourceMethod.BATCH_CREATE )
    {
      throw new UnsupportedOperationException("Unsupported batch request for scatter-gather: "+ request.getClass());
    }
    else
    {
      return (BatchRequest<T>)request;
    }
  }

  private void checkBatchRequest(BatchRequest<?> request)
  {
    if (request.getMethod() == ResourceMethod.BATCH_CREATE )
    {
      throw new UnsupportedOperationException("BATCH_CREATE is not supported for scatter-gather!");
    }
  }

  /**
   * Given a {@link BatchRequest} and a single key, construct a non-batch version request for that key.
   * @param batchRequest batch request (not BATCH_CREATE)
   * @param key individual resource key
   * @param <K> resource key type
   * @return non-batch version request for the key.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private <K> Request unbatchRequestByKey(BatchRequest<?> batchRequest, K key)
  {
    final SingleEntityRequestBuilder builder = getBuilder(batchRequest);
    // For BATCH_UPDATE and BATCH_PARTIAL_UPDATE, the generated Request is missing body,
    // but that is sufficient for us to implement getUris where we only care about individual request URI.
    builder.id(key);
    // keep all non-batch query parameters
    batchRequest.getQueryParamsObjects().entrySet().stream()
        .filter(queryParam -> !queryParam.getKey().equals(RestConstants.QUERY_BATCH_IDS_PARAM))
        .forEach(queryParam -> builder.setParam(queryParam.getKey(), queryParam.getValue()));
    // keep all headers
    batchRequest.getHeaders().forEach(builder::setHeader);
    return builder.build();
  }

  /**
   * Get corresponding request builder for the given batch request.
   * @param batchRequest batch request (not BATCH_CREATE)
   * @return request builder to construct a non-batch version request for each key.
   */
  @SuppressWarnings({"deprecation", "rawtypes", "unchecked"})
  private SingleEntityRequestBuilder getBuilder(BatchRequest<?> batchRequest)
  {
    checkBatchRequest(batchRequest);

    if (batchRequest instanceof BatchGetRequest || batchRequest instanceof BatchGetEntityRequest)
    {
      return new GetRequestBuilder(batchRequest.getBaseUriTemplate(),
          batchRequest.getResourceSpec().getValueClass(),
          batchRequest.getResourceSpec(),
          batchRequest.getRequestOptions());
    }
    else if (batchRequest instanceof BatchDeleteRequest)
    {
      return new DeleteRequestBuilder(batchRequest.getBaseUriTemplate(),
          batchRequest.getResourceSpec().getValueClass(),
          batchRequest.getResourceSpec(),
          batchRequest.getRequestOptions());
    }
    else if (batchRequest instanceof BatchUpdateRequest)
    {
      return new UpdateRequestBuilder(batchRequest.getBaseUriTemplate(),
          batchRequest.getResourceSpec().getValueClass(),
          batchRequest.getResourceSpec(),
          batchRequest.getRequestOptions());
    }
    else if (batchRequest instanceof BatchPartialUpdateRequest)
    {
      return new PartialUpdateRequestBuilder(batchRequest.getBaseUriTemplate(),
          batchRequest.getResourceSpec().getValueClass(),
          batchRequest.getResourceSpec(),
          batchRequest.getRequestOptions());
    }
    else
    {
      throw new UnsupportedOperationException("Unsupported batch request for scatter-gather: "+ batchRequest.getClass());
    }
  }

  /**
   * {@inheritDoc}
   *
   * Note that if the custom ScatterGatherStrategy overrides this method to associate each URI with a set of partition
   * Ids to bypass the partitioning by D2 later, it should also override {@link #onAllResponsesReceived(Request,
   * ProtocolVersion, Map, Map, Set, Callback)} to handle custom response gathering.
   */
  @Override
  @SuppressWarnings("rawtypes")
  public <K, T> List<URIKeyPair<K>> getUris(Request<T> request, ProtocolVersion version) {
    BatchRequest<T> batchRequest = safeCastRequest(request);
    @SuppressWarnings("unchecked")
    Set<K> keys = (Set<K>) batchRequest.getObjectIds();
    return keys.stream()
        .map(key -> {
          Request unbatchRequestByKey = unbatchRequestByKey(batchRequest, key);
          URI requestUri = RestliUriBuilderUtil.createUriBuilder(unbatchRequestByKey,
                  RestConstants.D2_URI_PREFIX, version).build();
          return new URIKeyPair<>(key, requestUri);
        })
        .collect(Collectors.toList());
  }

  /**
   * {@inheritDoc}
   *
   * We will use {@link URIMapper} to map batch request ids to host.
   * Before invoking {@link URIMapper}, we will first get the list of individual {@link URI} for
   * a given {@link BatchRequest} based on its contained object ids using {@link #getUris(Request, ProtocolVersion)}.
   * The number of resulting URI will be equal to the number of keys in the request.
   * For example, if the batch request is "d2://company/ids={1,2,3}, we will get 3 URIs, which are
   * "d2://company/1", "d2://company/2", "d2://company/3" respectively. These resulting URIs will be the parameters
   * passed to {@link URIMapper} to get their corresponding host information.
   */
  @Override
  public <K> URIMappingResult<K> mapUris(List<URIKeyPair<K>> uris) throws ServiceUnavailableException
  {
    return _uriMapper.mapUris(uris);
  }

  /**
   * Get corresponding batch request builder for the given batch request, with given keys or body properly
   * set in the builder.
   * @param batchRequest batch request (not BATCH_CREATE)
   * @param keys set of keys (optional for BATCH_UPDATE or BATCH_PARTIAL_UPDATE)
   * @param body entity map for the set of keys (required for BATCH_UPDATE or BATCH_PARTIAL_UPDATE)
   * @return request builder to construct a modified batch request for subset of keys.
   */
  @SuppressWarnings({"deprecation", "rawtypes", "unchecked"})
  private BatchKVRequestBuilder getBatchBuilder(BatchRequest<?> batchRequest, Set<?> keys, Map<?, ?> body)
  {
    checkBatchRequest(batchRequest);
    if (batchRequest instanceof BatchGetRequest)
    {
      if (keys == null)
      {
        throw new IllegalArgumentException("Missing keys for BatchGetRequest or BatchGetEntityRequest!");
      }
      return new BatchGetRequestBuilder(batchRequest.getBaseUriTemplate(),
            batchRequest.getResourceSpec().getValueClass(),
            batchRequest.getResourceSpec(),
            batchRequest.getRequestOptions()).ids(keys);
    }
    else if (batchRequest instanceof BatchGetEntityRequest)
    {
      if (keys == null)
      {
        throw new IllegalArgumentException("Missing keys for BatchGetEntityRequest or BatchGetEntityRequest!");
      }
      return new BatchGetEntityRequestBuilder(batchRequest.getBaseUriTemplate(),
              batchRequest.getResourceSpec(),
              batchRequest.getRequestOptions()).ids(keys);
    }
    else if (batchRequest instanceof BatchDeleteRequest)
    {
      if (keys == null)
      {
        throw new IllegalArgumentException("Missing keys for BatchDeleteRequest!");
      }
      return new BatchDeleteRequestBuilder(batchRequest.getBaseUriTemplate(),
          batchRequest.getResourceSpec().getValueClass(),
          batchRequest.getResourceSpec(),
          batchRequest.getRequestOptions()).ids(keys);
    }
    else if (batchRequest instanceof BatchUpdateRequest)
    {
      if (body == null)
      {
        throw new IllegalArgumentException("Missing body for BatchUpdateRequest!");
      }
      return new BatchUpdateRequestBuilder(batchRequest.getBaseUriTemplate(),
          batchRequest.getResourceSpec().getValueClass(),
          batchRequest.getResourceSpec(),
          batchRequest.getRequestOptions()).inputs(body);
    }
    else if (batchRequest instanceof BatchPartialUpdateRequest)
    {
      if (body == null)
      {
        throw new IllegalArgumentException("Missing body for BatchPartialUpdateRequest!");
      }
      return new BatchPartialUpdateRequestBuilder(batchRequest.getBaseUriTemplate(),
          batchRequest.getResourceSpec().getValueClass(),
          batchRequest.getResourceSpec(),
          batchRequest.getRequestOptions()).inputs(body);
    }
    else
    {
      throw new UnsupportedOperationException("Unsupported batch request for scatter-gather: " + batchRequest.getClass());
    }
  }

  /**
   * Given a {@link BatchRequest} and set of keys (for BATCH_GET, BATCH_DELETE) or entity map body for the set of keys
   * (for BATCH_UPDATE, BATCH_PARTIAL_UPDATE), construct a modified batch request for that set of keys.
   * @param batchRequest batch request (not BATCH_CREATE)
   * @param keys set of keys (optional for BATCH_UPDATE or BATCH_PARTIAL_UPDATE)
   * @param body entity map for the set of keys (required for BATCH_UPDATE or BATCH_PARTIAL_UPDATE)
   * @param <K> resource key type
   * @return modified batch request for selected set of keys
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private <K> Request buildScatterBatchRequestByKeys(BatchRequest<?> batchRequest, Set<K> keys, Map<K, ?> body)
  {
    final BatchKVRequestBuilder builder = getBatchBuilder(batchRequest, keys, body);
    // keep all non-batch query parameters
    batchRequest.getQueryParamsObjects().entrySet().stream()
        .filter(queryParam -> !queryParam.getKey().equals(RestConstants.QUERY_BATCH_IDS_PARAM))
        .forEach(queryParam -> builder.setParam(queryParam.getKey(), queryParam.getValue()));
    // keep all headers
    batchRequest.getHeaders().forEach(builder::setHeader);
    return builder.build();
  }

  /**
   * Given a {@link BatchRequest} and a set of D2 mapped keys, this utility constructs an entity body map for
   * BATCH_UPDATE/BATCH_PARTIAL_UPDATE for these keys.
   *
   * @param keys mapped keys.
   * @param batchRequest the {@link BatchUpdateRequest} or {@link BatchPartialUpdateRequest}.
   * @param <K> batch request key type.
   * @return an entity body map for given set of keys for BATCH_UPDATE/BATCH_PARTIAL_UPDATE request.
   */
  @SuppressWarnings("rawtypes")
  private <K> Map<K, ?> keyMapToInput(BatchRequest<?> batchRequest, Set<K> keys)
  {
    if (!(batchRequest instanceof BatchUpdateRequest) && !(batchRequest instanceof BatchPartialUpdateRequest))
    {
      throw new IllegalArgumentException("There shouldn't be request body for batch request: " + batchRequest.getClass());
    }

    final Map inputMap = (batchRequest instanceof BatchUpdateRequest) ?
        ((BatchUpdateRequest)batchRequest).getUpdateInputMap() :
        ((BatchPartialUpdateRequest)batchRequest).getPartialUpdateInputMap();

    if (inputMap == null)
    {
      throw new IllegalArgumentException("BatchUpdateRequest or BatchPartialUpdateRequest is missing input data!");
    }

    return keys.stream().collect(Collectors.toMap(key -> key, key ->
    {
      Object record = inputMap.get(key);
      if (record == null)
      {
        throw new IllegalArgumentException("BatchUpdateRequest or BatchPartialUpdateRequest is missing input for key: " + key);
      }
      else
      {
        return record;
      }
    }));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public <K, T> List<RequestInfo> scatterRequest(Request<T> request, RequestContext requestContext,
                                                 Map<URI, Set<K>> mappedKeys)
  {
    if (request.getMethod() == ResourceMethod.BATCH_CREATE)
    {
      throw new IllegalArgumentException("Batch create is not supported for scatter-gather!");
    }
    return mappedKeys.entrySet().stream().map((Map.Entry<URI, Set<K>> entry) ->
    {
      // for any non-BATCH request, we just fan out the same request. Custom strategy needs to override
      // this if this does not satisfy its logic.
      Request scatteredRequest = request;
      if (entry.getValue() != null && !entry.getValue().isEmpty())
      {
        // we only scatter batched requests when D2 host mapping result contains keys, empty key indicates
        // custom partition id specified in ScatterGatherStrategy.getUris method.
        if (request instanceof BatchGetRequest ||
                request instanceof BatchGetEntityRequest ||
                request instanceof BatchDeleteRequest)
        {
          scatteredRequest = buildScatterBatchRequestByKeys((BatchRequest) request, entry.getValue(), null);
        }
        else if (request instanceof BatchUpdateRequest ||
                request instanceof BatchPartialUpdateRequest)
        {
          scatteredRequest = buildScatterBatchRequestByKeys((BatchRequest) request, null,
                  keyMapToInput((BatchRequest) request, entry.getValue()));
        }
      }
      return new RequestInfo(scatteredRequest, updateRequestContextTarget(requestContext, entry.getKey()));
    }).collect(Collectors.toList());
  }

  private RequestContext updateRequestContextTarget(RequestContext requestContext, URI targetHost)
  {
    // we cannot update the give request context since that will be shared by scattered request.
    RequestContext context = requestContext.clone();
    KeyMapper.TargetHostHints.setRequestContextTargetHost(context, targetHost);
    return context;
  }

  /**
   * Initialize a final response object for gathering responses from scattered requests from original request.
   * @param request original request to be scattered.
   * @param protocolVersion rest.li protocol version.
   * @param <T> gathered response type.
   * @return a response object used to gather responses from scattered requests.
   */
  @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
  private <T> T initializeResponseContainer(BatchRequest<T> request, ProtocolVersion protocolVersion) {
    if (request instanceof BatchGetRequest)
    {
      DataMap emptyData = new DataMap();
      // this is to make sure that BatchResponse.getStatus can return empty map instead of null.
      emptyData.put(BatchResponse.STATUSES, new DataMap());
      return (T) new BatchResponse(emptyData,
          request.getResponseDecoder().getEntityClass());
    }
    else
    {
      return (T) new BatchKVResponse(new DataMap(),
          request.getResourceSpec().getKeyType(),
          request.getResourceSpec().getValueType(), request.getResourceSpec().getKeyParts(),
          request.getResourceSpec().getComplexKeyType(), protocolVersion);
    }
  }

  /**
   * Gather an incoming scattered request response and merge it into currently accumulated response.
   * @param accumulatedResponse currently accumulated response, For BatchRequest, it can be either BatchResponse (only for
   *                            BatchGetRequest) or BatchKVResponse. For non-batch request, it should be customized by
   *                            individual application.
   * @param requestInfo request which result in the incoming response.
   * @param newResponse incoming response from a scattered request.
   * @param <T> response type.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private <T> void gatherResponse(T accumulatedResponse, RequestInfo requestInfo, T newResponse)
  {
    if (!(newResponse instanceof BatchResponse) && !(newResponse instanceof BatchKVResponse))
    {
      throw new IllegalArgumentException("Unsupported response for scatter-gather: " + newResponse.getClass());
    }

    if (newResponse instanceof BatchResponse)
    {
      // BatchResponse
      BatchResponse batchResponse = (BatchResponse)newResponse;
      BatchResponse accumulatedBatchResponse = (BatchResponse)accumulatedResponse;
      accumulatedBatchResponse.getResults().putAll(batchResponse.getResults());
      accumulatedBatchResponse.getErrors().putAll(batchResponse.getErrors());
    }
    else
    {
      // BatchKVResponse
      BatchKVResponse batchKVResponse = (BatchKVResponse)newResponse;
      BatchKVResponse accumulatedBatchKVResponse = (BatchKVResponse)accumulatedResponse;
      accumulatedBatchKVResponse.getResults().putAll(batchKVResponse.getResults());
      accumulatedBatchKVResponse.getErrors().putAll(batchKVResponse.getErrors());
    }
  }

  /**
   * Gather an incoming scattered request error and merge it into currently accumulated batch response.
   * @param accumulatedResponse currently accumulated response, For BatchRequest, it can be either BatchResponse (only for
   *                            BatchGetRequest) or BatchKVResponse. For non-batch request, it should be customized by
   *                            individual application.
   * @param keys keys which result in the error.
   * @param e error exception.
   * @param version protocol version.
   * @param <K> request key type
   * @param <T> response type.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private <K, T> void gatherException(T accumulatedResponse, Set<K> keys, Throwable e,
                                      ProtocolVersion version)
  {
    if (!(accumulatedResponse instanceof BatchResponse) && !(accumulatedResponse instanceof BatchKVResponse))
    {
      throw new IllegalArgumentException("Unsupported response for scatter-gather: " + accumulatedResponse.getClass());
    }

    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setMessage(e.getMessage());
    errorResponse.setStatus(HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
    errorResponse.setExceptionClass(e.getClass().getName());
    if (accumulatedResponse instanceof BatchResponse)
    {
      keys.forEach(key ->
      {
        String keyString = BatchResponse.keyToString(key, version);
        ((BatchResponse)accumulatedResponse).getErrors().put(keyString, errorResponse);
      });
    }
    else
    {
      keys.forEach(key -> ((BatchKVResponse)accumulatedResponse).getErrors().put(key, errorResponse));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <K, T> void onAllResponsesReceived(Request<T> request, ProtocolVersion protocolVersion,
                                            Map<RequestInfo, Response<T>> successResponses,
                                            Map<RequestInfo, Throwable> failureResponses,
                                            Set<K> unmappedKeys,
                                            Callback<Response<T>> callback)
  {
    BatchRequest<T> batchRequest = safeCastRequest(request);
    // initialize a container for final response entity
    T gatheredResponse = initializeResponseContainer(batchRequest, protocolVersion);
    // gather success response
    successResponses.forEach((req, response) -> gatherResponse(gatheredResponse, req, response.getEntity()));
    // gather failure response
    failureResponses.forEach((req, e) ->
            {
              Set<K> failedKeys = (Set<K>)((BatchRequest<T>)req.getRequest()).getObjectIds();
              gatherException(gatheredResponse, failedKeys, e, protocolVersion);
            });
    // gather unmapped keys
    if (unmappedKeys != null && !unmappedKeys.isEmpty())
    {
      gatherException(gatheredResponse, unmappedKeys,
              new RestLiScatterGatherException("Unable to find a host for keys :" + unmappedKeys),
              protocolVersion);
    }
    if (!successResponses.isEmpty())
    {
      Response<T> firstResponse = successResponses.values().iterator().next();
      callback.onSuccess(new ResponseImpl<>(firstResponse, gatheredResponse));
    }
    else
    {
      // all scattered requests are failing
      callback.onSuccess(new ResponseImpl<>(HttpStatus.S_200_OK.getCode(),
              Collections.emptyMap(), Collections.emptyList(), gatheredResponse, null));
    }
  }
}

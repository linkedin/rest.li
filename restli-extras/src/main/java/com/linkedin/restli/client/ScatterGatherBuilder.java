/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.restli.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.MapKeyResult;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.common.BatchResponse;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class ScatterGatherBuilder<T extends RecordTemplate>
{
  private final KeyMapper _mapper;
  private final String D2_URI_PREFIX = "d2://";

  public ScatterGatherBuilder(KeyMapper mapper)
  {
    _mapper = mapper;
  }

  // for those who do not care about trouble keys.
  @Deprecated
  public Collection<RequestInfo<T>> buildRequests(BatchGetRequest<T> request, RequestContext requestContext) throws
      ServiceUnavailableException
  {
    return buildRequestsV2(request, requestContext).getRequestInfo();
  }

  // return value contains the request info and the unmapped keys (also the cause)
  // V2 is here to differentiate it from the older API
  public ScatterGatherResult<T> buildRequestsV2(BatchGetRequest<T> request, RequestContext requestContext) throws
          ServiceUnavailableException
  {
    Set<Object> idObjects = request.getIdObjects();
    Collection<String> ids = new HashSet<String>(idObjects.size());
    for (Object o : idObjects)
    {
      ids.add(o.toString());
    }

    URI serviceUri;
    try
    {
      serviceUri = new URI(D2_URI_PREFIX + request.getUri().toString());
    }
    catch (URISyntaxException e)
    {
      throw new IllegalArgumentException(e);
    }

    MapKeyResult<URI, String> mapKeyResult = _mapper.mapKeysV2(serviceUri, ids);

    Map<URI, Collection<String>> batches = mapKeyResult.getMapResult();
    Collection<RequestInfo<T>> scatterGatherRequests = new ArrayList<RequestInfo<T>>(batches.size());

    for (Map.Entry<URI, Collection<String>> batch : batches.entrySet())
    {
      BatchGetRequestBuilder<String, T> builder = new BatchGetRequestBuilder<String, T>(request.getBaseURI().toString(),
                                                                                        request.getResponseDecoder(),
                                                                                        request.getResourceSpec());
      builder.ids(batch.getValue());
      builder.fields(request.getFields().toArray(new PathSpec[0]));
      for (Map.Entry<String,String> header : request.getHeaders().entrySet())
      {
        builder.header(header.getKey(), header.getValue());
      }

      RequestContext context = requestContext.clone();
      KeyMapper.TargetHostHints.setRequestContextTargetHost(context, batch.getKey());

      scatterGatherRequests.add(new RequestInfo<T>(builder.build(), context));
    }

    return new ScatterGatherResult<T>(scatterGatherRequests, mapKeyResult.getUnmappedKeys());
  }

  /**
   * A convenience function for caller to issue batch request with one call.
   * If finer-grain control is required, users should call buildRequests instead and send requests by themselves
   *
   * @param client - the RestClient to use
   * @param request - the batch get request
   * @param requestContext - the original request context
   * @param callback - callback to be used for each request
   * @throws ServiceUnavailableException
   */
  public void sendRequests(RestClient client, BatchGetRequest<T> request, RequestContext requestContext, Callback<Response<BatchResponse<T>>> callback)
      throws ServiceUnavailableException
  {
    Collection<RequestInfo<T>> requestInfos = buildRequests(request, requestContext);
    for (RequestInfo<T> requestInfo : requestInfos)
    {
      client.sendRequest(requestInfo.getRequest(), requestInfo.getRequestContext(), callback);
    }
  }

  public static class RequestInfo<T extends RecordTemplate>
  {
    private final Request<BatchResponse<T>> _request;
    private final RequestContext _requestContext;

    public RequestInfo(Request<BatchResponse<T>> request, RequestContext requestContext)
    {
      _request = request;
      _requestContext = requestContext;
    }

    public Request<BatchResponse<T>> getRequest()
    {
      return _request;
    }

    public RequestContext getRequestContext()
    {
      return _requestContext;
    }
  }

  public static class ScatterGatherResult<T extends RecordTemplate>
  {
    private final Collection<RequestInfo<T>> _requestInfos;
    private final Collection<MapKeyResult.UnmappedKey<String>> _unmappedKeys;

    public ScatterGatherResult(Collection<RequestInfo<T>> requestInfos, Collection<MapKeyResult.UnmappedKey<String>> unmappedKeys)
    {
      _requestInfos = Collections.unmodifiableCollection(requestInfos);
      _unmappedKeys = Collections.unmodifiableCollection(unmappedKeys);
    }

    Collection<RequestInfo<T>> getRequestInfo()
    {
      return _requestInfos;
    }

    Collection<MapKeyResult.UnmappedKey<String>> getUnmappedKeys()
    {
      return _unmappedKeys;
    }
  }
}

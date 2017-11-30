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


import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.MapKeyResult;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.common.UpdateStatus;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class ScatterGatherBuilder<T extends RecordTemplate>
{
  private final KeyMapper _mapper;
  private final String D2_URI_PREFIX = "d2://";
  private static final int HOST_LIMIT = 1;

  public ScatterGatherBuilder(KeyMapper mapper)
  {
    _mapper = mapper;
  }

  // return value contains the request info and the unmapped keys (also the cause)
  // V2 is here to differentiate it from the older API
  @SuppressWarnings("deprecation")
  public ScatterGatherResult<T> buildRequestsV2(BatchGetRequest<T> request, RequestContext requestContext)
    throws ServiceUnavailableException
  {
    Set<Object> idObjects = request.getObjectIds();

    MapKeyResult<URI, Object> mapKeyResult = mapKeys(request, idObjects);

    Map<URI, Collection<Object>> batches = mapKeyResult.getMapResult();
    Collection<RequestInfo<T>> scatterGatherRequests = new ArrayList<RequestInfo<T>>(batches.size());

    for (Map.Entry<URI, Collection<Object>> batch : batches.entrySet())
    {
      BatchGetRequestBuilder<Object, T> builder = new BatchGetRequestBuilder<Object, T>(request.getBaseUriTemplate(),
                                                                                        request.getResponseDecoder(),
                                                                                        request.getResourceSpec(),
                                                                                        request.getRequestOptions());
      builder.ids(batch.getValue());
      for (Map.Entry<String, Object> param : request.getQueryParamsObjects().entrySet())
      {
        if (!param.getKey().equals(RestConstants.QUERY_BATCH_IDS_PARAM))
        {
          // keep all non-batch query parameters since they could be request specific
          builder.setParam(param.getKey(), param.getValue());
        }
      }
      for (Map.Entry<String, String> header : request.getHeaders().entrySet())
      {
        builder.setHeader(header.getKey(), header.getValue());
      }

      RequestContext context = requestContext.clone();
      KeyMapper.TargetHostHints.setRequestContextTargetHost(context, batch.getKey());

      scatterGatherRequests.add(new RequestInfo<T>(builder.build(), context));
    }

    return new ScatterGatherResult<T>(scatterGatherRequests, mapKeyResult.getUnmappedKeys());
  }

  @SuppressWarnings("deprecation")
  public <K> KVScatterGatherResult<K, EntityResponse<T>> buildRequests(BatchGetEntityRequest<K, T> request, RequestContext requestContext)
    throws ServiceUnavailableException
  {
    @SuppressWarnings("unchecked")
    final Set<K> idObjects = (Set<K>) request.getObjectIds();

    final MapKeyResult<URI, K> mapKeyResult = mapKeys(request, idObjects);

    final Map<URI, Collection<K>> batches = mapKeyResult.getMapResult();
    final Collection<KVRequestInfo<K, EntityResponse<T>>> scatterGatherRequests = new ArrayList<KVRequestInfo<K, EntityResponse<T>>>(batches.size());

    for (Map.Entry<URI, Collection<K>> batch : batches.entrySet())
    {
      final BatchGetEntityRequestBuilder<K, T> builder = new BatchGetEntityRequestBuilder<K, T>(request.getBaseUriTemplate(),
                                                                                                request.getResponseDecoder(),
                                                                                                request.getResourceSpec(),
                                                                                                request.getRequestOptions());
      builder.ids(batch.getValue());
      for (Map.Entry<String, Object> param : request.getQueryParamsObjects().entrySet())
      {
        if (!param.getKey().equals(RestConstants.QUERY_BATCH_IDS_PARAM))
        {
          // keep all non-batch query parameters since they could be request specific
          builder.setParam(param.getKey(), param.getValue());
        }
      }
      for (Map.Entry<String, String> header : request.getHeaders().entrySet())
      {
        builder.setHeader(header.getKey(), header.getValue());
      }

      final RequestContext context = requestContext.clone();
      KeyMapper.TargetHostHints.setRequestContextTargetHost(context, batch.getKey());

      scatterGatherRequests.add(new KVRequestInfo<K, EntityResponse<T>>(builder.build(), context));
    }

    return new KVScatterGatherResult<K, EntityResponse<T>>(scatterGatherRequests, mapKeyResult.getUnmappedKeys());
  }

  @SuppressWarnings({ "unchecked", "deprecation" })
  public <K> KVScatterGatherResult<K, T> buildRequestsKV(BatchGetKVRequest<K, T> request, RequestContext requestContext)
      throws ServiceUnavailableException
  {
    Set<K> idObjects = (Set<K>) request.getObjectIds();

    MapKeyResult<URI, K> mapKeyResult = mapKeys(request, idObjects);

    Map<URI, Collection<K>> batches = mapKeyResult.getMapResult();
    Collection<KVRequestInfo<K, T>> scatterGatherRequests = new ArrayList<KVRequestInfo<K, T>>(batches.size());

    for (Map.Entry<URI, Collection<K>> batch : batches.entrySet())
    {
      BatchGetRequestBuilder<K, T> builder =
          new BatchGetRequestBuilder<K, T>(request.getBaseUriTemplate(),
            (Class<T>)request.getResourceProperties().getValueType().getType(),
            request.getResourceSpec(),
            request.getRequestOptions());

      builder.ids(batch.getValue());
      for (Map.Entry<String, Object> param : request.getQueryParamsObjects().entrySet())
      {
        if (!param.getKey().equals(RestConstants.QUERY_BATCH_IDS_PARAM))
        {
          // keep all non-batch query parameters since they could be request specific
          builder.setParam(param.getKey(), param.getValue());
        }
      }
      for (Map.Entry<String, String> header : request.getHeaders().entrySet())
      {
        builder.setHeader(header.getKey(), header.getValue());
      }

      RequestContext context = requestContext.clone();
      KeyMapper.TargetHostHints.setRequestContextTargetHost(context, batch.getKey());

      scatterGatherRequests.add(new KVRequestInfo<K, T>(builder.buildKV(), context));
    }

    return new KVScatterGatherResult<K, T>(scatterGatherRequests, mapKeyResult.getUnmappedKeys());
  }

  @SuppressWarnings("deprecation")
  public <K> KVScatterGatherResult<K, UpdateStatus> buildRequests(BatchUpdateRequest<K, T> request, RequestContext requestContext)
    throws ServiceUnavailableException
  {
    Set<Object> idObjects = request.getObjectIds();
    Collection<K> ids = new HashSet<K>(idObjects.size());
    for (Object o : idObjects)
    {
      @SuppressWarnings("unchecked")
      K k = (K) o;
      ids.add(k);
    }

    MapKeyResult<URI, K> mapKeyResult = mapKeys(request, ids);

    @SuppressWarnings("unchecked")
    TypeSpec<T> valueType = (TypeSpec<T>) request.getResourceProperties().getValueType();
    Map<URI, Map<K, T>> batches = keyMapToInput(mapKeyResult, request);
    Collection<KVRequestInfo<K, UpdateStatus>> scatterGatherRequests = new ArrayList<KVRequestInfo<K, UpdateStatus>>(batches.size());

    for (Map.Entry<URI, Map<K, T>> batch : batches.entrySet())
    {
      BatchUpdateRequestBuilder<K, T> builder = new BatchUpdateRequestBuilder<K, T>(request.getBaseUriTemplate(),
                                                                                    valueType.getType(),
                                                                                    request.getResourceSpec(),
                                                                                    request.getRequestOptions());
      builder.inputs(batch.getValue());
      for (Map.Entry<String, Object> param : request.getQueryParamsObjects().entrySet())
      {
        if (!param.getKey().equals(RestConstants.QUERY_BATCH_IDS_PARAM))
        {
          builder.setParam(param.getKey(), param.getValue());
        }
      }
      for (Map.Entry<String, String> header : request.getHeaders().entrySet())
      {
        builder.setHeader(header.getKey(), header.getValue());
      }

      RequestContext context = requestContext.clone();
      KeyMapper.TargetHostHints.setRequestContextTargetHost(context, batch.getKey());

      scatterGatherRequests.add(new KVRequestInfo<K, UpdateStatus>(builder.build(), context));
    }

    return new KVScatterGatherResult<K, UpdateStatus>(scatterGatherRequests, mapKeyResult.getUnmappedKeys());
  }

  private <K> MapKeyResult<URI, K> mapKeys(BatchRequest<?> request, Collection<K> ids)
    throws ServiceUnavailableException
  {
    URI serviceUri;
    try
    {
      serviceUri = new URI(D2_URI_PREFIX + request.getServiceName());
    }
    catch (URISyntaxException e)
    {
      throw new IllegalArgumentException(e);
    }

    return _mapper.mapKeysV3(serviceUri, ids, HOST_LIMIT, null).toMapKeyResult();
  }

  /**
   * Helper function to map services to inputs, rather than services to ids.
   * Each input is represented by a Map from keys to {@link RecordTemplate}s.
   *
   * Essentially, instead of calling {@link com.linkedin.d2.balancer.util.MapKeyResult#getMapResult()}
   * to get a map from services to keys, you would instead call this to get a map from services to inputs.
   * You can then use this input to create a new BatchRequest by calling
   * {@link BatchUpdateRequestBuilder#inputs(java.util.Map)} or similar function.
   *
   * @param mapKeyResult {@link MapKeyResult} of mapping U to keys.
   * @param batchRequest the {@link BatchRequest}.
   * @param <U> the service that will handle each set of inputs; Generally {@link URI}, for a host that will handle the request.
   * @param <K> the key type.
   * @return a map from U to request input, where request input is a map from keys to {@link RecordTemplate}s.
   */
  private <U, K> Map<U, Map<K, T>> keyMapToInput(MapKeyResult<U, K> mapKeyResult, BatchUpdateRequest<K, T> batchRequest)
  {
    Map<K, T> updateInput = batchRequest.getUpdateInputMap();

    if (updateInput == null)
    {
      throw new IllegalArgumentException("given BatchRequest must have input data");
    }

    Map<U, Collection<K>> map = mapKeyResult.getMapResult();
    Map<U, Map<K, T>> result = new HashMap<U, Map<K, T>>(map.size());
    for(Map.Entry<U, Collection<K>> entry : map.entrySet())
    {
      Collection<K> keyList = entry.getValue();
      Map<K, T> keyRecordMap = new HashMap<K, T>(keyList.size());
      for(K key : keyList)
      {
        T record = updateInput.get(key);
        if (record == null)
        {
          throw new IllegalArgumentException("given BatchRequest input must have all keys present in mapKeyResult");
        }
        keyRecordMap.put(key, record);
      }
      result.put(entry.getKey(), keyRecordMap);
    }
    return result;
  }

  @SuppressWarnings("deprecation")
  public <K> KVScatterGatherResult<K, UpdateStatus> buildRequests(BatchDeleteRequest<K, T> request, RequestContext requestContext)
    throws ServiceUnavailableException
  {
    Set<Object> idObjects = request.getObjectIds();
    Collection<K> ids = new HashSet<K>(idObjects.size());
    for (Object o : idObjects)
    {
      @SuppressWarnings("unchecked")
      K k = (K) o;
      ids.add(k);
    }

    MapKeyResult<URI, K> mapKeyResult = mapKeys(request, ids);
    Map<URI, Collection<K>> batches = mapKeyResult.getMapResult();
    Collection<KVRequestInfo<K, UpdateStatus>> scatterGatherRequests = new ArrayList<KVRequestInfo<K, UpdateStatus>>(batches.size());

    for (Map.Entry<URI, Collection<K>> batch : batches.entrySet())
    {
      TypeSpec<? extends RecordTemplate> value = request.getResourceProperties().getValueType();
      @SuppressWarnings("unchecked")
      Class<T> valueClass = (Class<T>) ((value == null) ? null : value.getType());
      BatchDeleteRequestBuilder<K, T> builder = new BatchDeleteRequestBuilder<K, T>(request.getBaseUriTemplate(),
                                                                                    valueClass,
                                                                                    request.getResourceSpec(),
                                                                                    request.getRequestOptions());
      builder.ids(batch.getValue());
      for (Map.Entry<String, Object> param : request.getQueryParamsObjects().entrySet())
      {
        if (!param.getKey().equals(RestConstants.QUERY_BATCH_IDS_PARAM))
        {
          builder.setParam(param.getKey(), param.getValue());
        }
      }
      for (Map.Entry<String,String> header : request.getHeaders().entrySet())
      {
        builder.setHeader(header.getKey(), header.getValue());
      }

      RequestContext context = requestContext.clone();
      KeyMapper.TargetHostHints.setRequestContextTargetHost(context, batch.getKey());

      BatchRequest<BatchKVResponse<K, UpdateStatus>> build = builder.build();
      scatterGatherRequests.add(new KVRequestInfo<K, UpdateStatus>(build, context));
    }

    return new KVScatterGatherResult<K, UpdateStatus>(scatterGatherRequests, mapKeyResult.getUnmappedKeys());
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
    ScatterGatherResult<T> scatterGatherResult = buildRequestsV2(request, requestContext);
    for (RequestInfo<T> requestInfo : scatterGatherResult.getRequestInfo())
    {
      client.sendRequest(requestInfo.getRequest(), requestInfo.getRequestContext(), callback);
    }
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
  public <K> void sendRequests(RestClient client, BatchGetKVRequest<K, T> request, RequestContext requestContext, Callback<Response<BatchKVResponse<K, T>>> callback)
      throws ServiceUnavailableException
  {
    KVScatterGatherResult<K, T> scatterGatherResult = buildRequestsKV(request, requestContext);
    for (KVRequestInfo<K, T> requestInfo : scatterGatherResult.getRequestInfo())
    {
      client.sendRequest(requestInfo.getRequest(), requestInfo.getRequestContext(), callback);
    }
  }

  public <K> void sendRequests(RestClient client,
                               BatchUpdateRequest<K, T> request,
                               RequestContext requestContext,
                               Callback<Response<BatchKVResponse<K, UpdateStatus>>> callback)
    throws ServiceUnavailableException
  {
    KVScatterGatherResult<K, UpdateStatus> scatterGatherResult = buildRequests(request, requestContext);
    for(KVRequestInfo<K, UpdateStatus> requestInfo : scatterGatherResult.getRequestInfo())
    {
      client.sendRequest(requestInfo.getRequest(), requestInfo.getRequestContext(), callback);
    }
  }

  public <K> void sendRequests(RestClient client,
                               BatchDeleteRequest<K, T> request,
                               RequestContext requestContext,
                               Callback<Response<BatchKVResponse<K, UpdateStatus>>> callback)
    throws ServiceUnavailableException
  {
    KVScatterGatherResult<K, UpdateStatus> scatterGatherResult = buildRequests(request, requestContext);
    for(KVRequestInfo<K, UpdateStatus> requestInfo : scatterGatherResult.getRequestInfo())
    {
      client.sendRequest(requestInfo.getRequest(), requestInfo.getRequestContext(), callback);
    }
  }

  public static class RequestInfo<T extends RecordTemplate>
  {
    private final BatchRequest<BatchResponse<T>> _request;
    private final RequestContext _requestContext;

    public RequestInfo(BatchRequest<BatchResponse<T>> request, RequestContext requestContext)
    {
      _request = request;
      _requestContext = requestContext;
    }

    public Request<BatchResponse<T>> getRequest()
    {
      return _request;
    }

    public BatchRequest<BatchResponse<T>> getBatchRequest()
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
    private final Collection<MapKeyResult.UnmappedKey<Object>> _unmappedKeys;

    public ScatterGatherResult(Collection<RequestInfo<T>> requestInfos, Collection<MapKeyResult.UnmappedKey<Object>> unmappedKeys)
    {
      _requestInfos = Collections.unmodifiableCollection(requestInfos);
      _unmappedKeys = Collections.unmodifiableCollection(unmappedKeys);
    }

    public Collection<RequestInfo<T>> getRequestInfo()
    {
      return _requestInfos;
    }

    public Collection<MapKeyResult.UnmappedKey<Object>> getUnmappedKeys()
    {
      return _unmappedKeys;
    }
  }

  public static class KVRequestInfo<K, T extends RecordTemplate>
  {
    private final BatchRequest<BatchKVResponse<K, T>> _request;
    private final RequestContext _requestContext;

    public KVRequestInfo(BatchRequest<BatchKVResponse<K, T>> request, RequestContext requestContext)
    {
      _request = request;
      _requestContext = requestContext;
    }

    public BatchRequest<BatchKVResponse<K, T>> getRequest()
    {
      return _request;
    }

    public RequestContext getRequestContext()
    {
      return _requestContext;
    }
  }

  public static class KVScatterGatherResult<K, T extends RecordTemplate>
  {
    private final Collection<KVRequestInfo<K, T>> _requestInfos;
    private final Collection<MapKeyResult.UnmappedKey<K>> _unmappedKeys;

    public KVScatterGatherResult(Collection<KVRequestInfo<K, T>> requestInfos, Collection<MapKeyResult.UnmappedKey<K>> unmappedKeys)
    {
      _requestInfos = Collections.unmodifiableCollection(requestInfos);
      _unmappedKeys = Collections.unmodifiableCollection(unmappedKeys);
    }

    public Collection<KVRequestInfo<K, T>> getRequestInfo()
    {
      return _requestInfos;
    }

    public Collection<MapKeyResult.UnmappedKey<K>> getUnmappedKeys()
    {
      return _unmappedKeys;
    }
  }
}

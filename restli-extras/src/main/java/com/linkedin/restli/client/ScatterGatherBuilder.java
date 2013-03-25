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

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.linkedin.common.callback.Callback;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.MapKeyResult;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.UpdateStatus;

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

    MapKeyResult<URI, String> mapKeyResult = mapKeys(request, ids);

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

  public <K> KVScatterGatherResult<K, UpdateStatus> buildRequests(BatchUpdateRequest<K, T> request, RequestContext requestContext) throws
    ServiceUnavailableException
  {
    Set<Object> idObjects = request.getIdObjects();
    Collection<K> ids = new HashSet<K>(idObjects.size());
    for (Object o : idObjects)
    {
      @SuppressWarnings("unchecked")
      K k = (K) o;
      ids.add(k);
    }

    MapKeyResult<URI, K> mapKeyResult = mapKeys(request, ids);

    @SuppressWarnings("unchecked")
    Class<T> tClass = (Class<T>) request.getResourceSpec().getValueClass();
    Map<URI, Map<K, T>> batches = keyMapToInput(mapKeyResult, request, tClass);
    Collection<KVRequestInfo<K, UpdateStatus>> scatterGatherRequests = new ArrayList<KVRequestInfo<K, UpdateStatus>>(batches.size());

    for (Map.Entry<URI, Map<K, T>> batch : batches.entrySet())
    {
      BatchUpdateRequestBuilder<K, T> builder =
        new BatchUpdateRequestBuilder<K, T>(request.getBaseURI().toString(),
                                            tClass,
                                            request.getResourceSpec());
      builder.inputs(batch.getValue());
      for (Map.Entry<String,String> header : request.getHeaders().entrySet())
      {
        builder.header(header.getKey(), header.getValue());
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
      serviceUri = new URI(D2_URI_PREFIX + request.getUri().toString());
    }
    catch (URISyntaxException e)
    {
      throw new IllegalArgumentException(e);
    }

    return _mapper.mapKeysV2(serviceUri, ids);
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
   * @param tClass the {@link RecordTemplate} type of the entities in the input of the request.
   * @param <U> the service that will handle each set of inputs; Generally {@link URI}, for a host that will handle the request.
   * @param <K> the key type.
   * @return a map from U to request input, where request input is a map from keys to {@link RecordTemplate}s.
   */
  private <U, K> Map<U, Map<K, T>> keyMapToInput(MapKeyResult<U, K> mapKeyResult, BatchRequest batchRequest, Class<T> tClass)
  {
    DataMap dataMap = batchRequest.getInput().data().getDataMap(com.linkedin.restli.common.BatchRequest.ENTITIES);
    if (dataMap == null)
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
        DataMap recordDataMap = dataMap.getDataMap(key.toString());
        if (recordDataMap == null)
        {
          throw new IllegalArgumentException("given BatchRequest input must have all keys present in mapKeyResult");
        }
        T record;
        try
        {
          record = tClass.getConstructor(DataMap.class).newInstance(recordDataMap);
        }
        catch (InstantiationException e)
        {
          throw new IllegalArgumentException("RecordTemplate class " + tClass + " should have a public DataMap constructor", e);
        }
        catch (IllegalAccessException e)
        {
          throw new IllegalArgumentException("RecordTemplate class " + tClass + " should have a public DataMap constructor", e);
        }
        catch (InvocationTargetException e)
        {
          throw new IllegalArgumentException("RecordTemplate class " + tClass + " should have a public DataMap constructor", e);
        }
        catch (NoSuchMethodException e)
        {
          throw new IllegalArgumentException("RecordTemplate class " + tClass + " should have a public DataMap constructor", e);
        }
        keyRecordMap.put(key, record);
      }
      result.put(entry.getKey(), keyRecordMap);
    }
    return result;
  }

  public <K> KVScatterGatherResult<K, UpdateStatus> buildRequests(BatchDeleteRequest<K, T> request, RequestContext requestContext) throws
    ServiceUnavailableException
  {
    Set<Object> idObjects = request.getIdObjects();
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
      @SuppressWarnings("unchecked")
      Class<T> keyClass = (Class<T>) request.getResourceSpec().getValueClass();
      BatchDeleteRequestBuilder<K, T> builder =
        new BatchDeleteRequestBuilder<K, T>(request.getBaseURI().toString(),
                                            keyClass,
                                            request.getResourceSpec());
      builder.ids(batch.getValue());
      for (Map.Entry<String,String> header : request.getHeaders().entrySet())
      {
        builder.header(header.getKey(), header.getValue());
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

    public BatchRequest<BatchResponse<T>> getRequest()
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

  public static class KVRequestInfo<K,T extends RecordTemplate>
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

    Collection<KVRequestInfo<K, T>> getRequestInfo()
    {
      return _requestInfos;
    }

    Collection<MapKeyResult.UnmappedKey<K>> getUnmappedKeys()
    {
      return _unmappedKeys;
    }
  }

}

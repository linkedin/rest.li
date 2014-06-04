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

package com.linkedin.restli.client;


import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.client.BatchKVResponseDecoder;
import com.linkedin.restli.internal.client.BatchResponseDecoder;
import com.linkedin.restli.internal.client.RestResponseDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Builds a type-bound Request for a batch of resources.
 *
 * @param <V> batch element type
 *
 * @author Eran Leshem
 */
public class BatchGetRequestBuilder<K, V extends RecordTemplate> extends
    BatchKVRequestBuilder<K, V, BatchGetRequest<V>>
{
  private final RestResponseDecoder<BatchResponse<V>> _decoder;

  /**
   * Batches multiple requests into a single batch request.
   *
   * Requirements:
   * <ul>
   *   <li>Base URIs of all requests must be the same</li>
   * </ul>
   *
   * Batching is performed along two dimensions:
   * <ol>
   *   <li>entity ids are accumulated</li>
   *   <li>fields are accumulated. If any of the batched requests has a null projection,
   *   then the resulting batch request will also have a null projection</li>
   * </ol>
   *
   * @param requests to batch
   * @param <RT> type of entity template
   * @return batching request
   */
  public static <RT extends RecordTemplate> BatchGetRequest<RT> batch(List<BatchGetRequest<RT>> requests)
  {
    return batch(requests, true);
  }

  /**
   * Batches multiple requests into a single batch request.
   *
   * Requirements:
   * <ul>
   *   <li>Base URIs of all requests must be the same</li>
   *   <li>if {@code batchFields} is {@code false}, then all requests must have the same projection</li>
   * </ul>
   *
   * Batching is performed along one or two dimensions:
   * <ol>
   *   <li>entity ids are always accumulated</li>
   *   <li>if {@code batchFields} is {@code true}, fields are also accumulated. If any of the batched requests has a
   *   null projection, then the resulting batch request will also have a null projection.</li>
   * </ol>
   *
   *
   *
   * @param requests to batch
   * @param batchFields whether field batching is desired
   * @param <RT> type of entity template
   * @return batching request
   */
  public static <RT extends RecordTemplate> BatchGetRequest<RT> batch(List<BatchGetRequest<RT>> requests,
                                                                      boolean batchFields)
  {
    final BatchGetRequest<RT> firstRequest = requests.get(0);
    final ResourceSpec firstResourceSpec = firstRequest.getResourceSpec();
    final Map<String, Object> batchQueryParams = BatchGetRequestUtil.getBatchQueryParam(requests, batchFields);

    return new BatchGetRequest<RT>(firstRequest.getHeaders(),
                                   firstRequest.getResponseDecoder(),
                                   batchQueryParams,
                                   firstResourceSpec,
                                   firstRequest.getBaseUriTemplate(),
                                   firstRequest.getPathKeys(),
                                   firstRequest.getRequestOptions());
  }

  /**
   * Batches multiple requests into a single batch request.
   *
   * Requirements:
   * <ul>
   *   <li>Base URIs of all requests must be the same</li>
   * </ul>
   *
   * Batching is performed along two dimensions:
   * <ol>
   *   <li>entity ids are accumulated</li>
   *   <li>fields are accumulated. If any of the batched requests has a null projection,
   *   then the resulting batch request will also have a null projection</li>
   * </ol>
   *
   * @param requests to batch
   * @param <K> type of the key
   * @param <RT> type of entity template
   * @return batching request
   */
  public static <K, RT extends RecordTemplate> BatchGetKVRequest<K, RT> batchKV(List<BatchGetKVRequest<K, RT>> requests)
  {
    return batchKV(requests, true);
  }

  /**
   * Batches multiple requests into a single batch request.
   *
   * Requirements:
   * <ul>
   *   <li>Base URIs of all requests must be the same</li>
   *   <li>if {@code batchFields} is {@code false}, then all requests must have the same projection</li>
   * </ul>
   *
   * Batching is performed along one or two dimensions:
   * <ol>
   *   <li>entity ids are always accumulated</li>
   *   <li>if {@code batchFields} is {@code true}, fields are also accumulated. If any of the batched requests has a
   *   null projection, then the resulting batch request will also have a null projection.</li>
   * </ol>
   *
   *
   *
   * @param requests to batch
   * @param batchFields whether field batching is desired
   * @param <K> type of the key
   * @param <RT> type of entity template
   * @return batching request
   */
  public static <K, RT extends RecordTemplate> BatchGetKVRequest<K, RT> batchKV(List<BatchGetKVRequest<K, RT>> requests,
                                                                      boolean batchFields)
  {
    final BatchGetKVRequest<K, RT> firstRequest = requests.get(0);
    final ResourceSpec firstResourceSpec = firstRequest.getResourceSpec();
    final Map<String, Object> batchQueryParams = BatchGetRequestUtil.getBatchQueryParam(requests, batchFields);

    return new BatchGetKVRequest<K, RT>(firstRequest.getHeaders(),
                                   firstRequest.getResponseDecoder(),
                                   batchQueryParams,
                                   firstResourceSpec,
                                   firstRequest.getBaseUriTemplate(),
                                   firstRequest.getPathKeys(),
                                   firstRequest.getRequestOptions());
  }

  /**
   * Converts an entity request to a batch one, for subsequent batching with other requests.
   * @param request to convert
   * @param <RT> type of entity template
   * @return batch request
   */
  @SuppressWarnings("unchecked")
  public static <K, RT extends RecordTemplate> BatchGetKVRequest<K, RT> batchKV(GetRequest<RT> request)
  {
    Object id = request.getObjectId();

    if (id == null)
    {
      throw new IllegalArgumentException(
          "It is not possible to create a batch get request from a get request without an id.");
    }

    Map<String, Object> queryParams = new HashMap<String, Object>(request.getQueryParamsObjects());
    queryParams.put(RestConstants.QUERY_BATCH_IDS_PARAM,
                    new ArrayList<Object>(Arrays.asList(id)));

    return new BatchGetKVRequest<K, RT>(request.getHeaders(),
                                        new BatchKVResponseDecoder<K, RT>(
                                            request.getEntityClass(),
                                            (Class<K>)request.getResourceSpec().getKeyClass(),
                                            request.getResourceSpec().getKeyParts(),
                                            request.getResourceSpec().getKeyKeyClass(),
                                            request.getResourceSpec().getKeyParamsClass()),
                                        queryParams,
                                        request.getResourceSpec(),
                                        request.getBaseUriTemplate(),
                                        request.getPathKeys(),
                                        request.getRequestOptions());
  }

  /**
   * Converts an entity request to a batch one, for subsequent batching with other requests.
   * @param request to convert
   * @param <RT> type of entity template
   * @return batch request
   */
  public static <RT extends RecordTemplate> BatchGetRequest<RT> batch(GetRequest<RT> request)
  {
    Object id = request.getObjectId();

    if (id == null)
    {
      throw new IllegalArgumentException(
          "It is not possible to create a batch get request from a get request without an id.");
    }

    Map<String, Object> queryParams = new HashMap<String, Object>(request.getQueryParamsObjects());
    queryParams.put(RestConstants.QUERY_BATCH_IDS_PARAM,
                    new ArrayList<Object>(Arrays.asList(id)));

    return new BatchGetRequest<RT>(request.getHeaders(),
                                   new BatchResponseDecoder<RT>(request.getEntityClass()),
                                   queryParams,
                                   request.getResourceSpec(),
                                   request.getBaseUriTemplate(),
                                   request.getPathKeys(),
                                   request.getRequestOptions());
  }

  @Deprecated
  public BatchGetRequestBuilder(String baseUriTemplate, Class<V> modelClass, ResourceSpec resourceSpec)
  {
    this(baseUriTemplate, new BatchResponseDecoder<V>(modelClass), resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS);
  }

  public BatchGetRequestBuilder(String baseUriTemplate,
                                Class<V> modelClass,
                                ResourceSpec resourceSpec,
                                RestliRequestOptions requestOptions)
  {
    this(baseUriTemplate, new BatchResponseDecoder<V>(modelClass), resourceSpec, requestOptions);
  }

  @Deprecated
  public BatchGetRequestBuilder(String baseUriTemplate,
                                RestResponseDecoder<BatchResponse<V>> decoder,
                                ResourceSpec resourceSpec)
  {
    this(baseUriTemplate, decoder, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS);
  }

  public BatchGetRequestBuilder(String baseUriTemplate,
                                RestResponseDecoder<BatchResponse<V>> decoder,
                                ResourceSpec resourceSpec,
                                RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
    _decoder = decoder;
  }

  @SuppressWarnings("unchecked")
  public BatchGetRequestBuilder<K, V> ids(K... ids)
  {
    return ids(Arrays.asList(ids));
  }

  public BatchGetRequestBuilder<K, V> ids(Collection<K> ids)
  {
    addKeys(ids);
    return this;
  }

  @Override
  @Deprecated
  public BatchGetRequestBuilder<K, V> reqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  @Deprecated
  public BatchGetRequestBuilder<K, V> param(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public BatchGetRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public BatchGetRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public BatchGetRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public BatchGetRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  @Deprecated
  public BatchGetRequestBuilder<K, V> header(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public BatchGetRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public BatchGetRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public BatchGetRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public BatchGetRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  /**
   * Builds a GET request for this resource batch.
   *
   * @return a read request for the resource batch
   */
  @Override
  public BatchGetRequest<V> build()
  {
    ensureBatchKeys();

    Class<?> keyClass = _resourceSpec.getKeyClass();

    if (com.linkedin.restli.common.CompoundKey.class.isAssignableFrom(keyClass) ||
        keyClass == com.linkedin.restli.common.ComplexResourceKey.class)
    {
      throw new UnsupportedOperationException("The build method cannot be used with Compound and Complex key types. " +
                                                  "Please use the buildKV method instead.");
    }

    return new BatchGetRequest<V>(_headers,
                                  _decoder,
                                  _queryParams,
                                  _resourceSpec,
                                  getBaseUriTemplate(),
                                  _pathKeys,
                                  getRequestOptions());
  }

  public BatchGetKVRequest<K, V> buildKV()
  {
    ensureBatchKeys();

    //Framework code should ensure that the ResourceSpec matches the static types of these parameters
    @SuppressWarnings("unchecked")
    BatchKVResponseDecoder<K, V> decoder =
        new BatchKVResponseDecoder<K, V>((TypeSpec<V>) _resourceSpec.getValueType(),
                                         (TypeSpec<K>) _resourceSpec.getKeyType(),
                                         _resourceSpec.getKeyParts(),
                                         _resourceSpec.getComplexKeyType());

    return new BatchGetKVRequest<K, V>(_headers,
                                  decoder,
                                  _queryParams,
                                  _resourceSpec,
                                  getBaseUriTemplate(),
                                  _pathKeys,
                                  getRequestOptions());
  }

  public BatchGetRequestBuilder<K, V> fields(PathSpec... fieldPaths)
  {
    addFields(fieldPaths);
    return this;
  }
}

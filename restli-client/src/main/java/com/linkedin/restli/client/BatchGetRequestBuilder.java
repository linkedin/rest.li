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
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.client.BatchKVResponseDecoder;
import com.linkedin.restli.internal.client.BatchResponseDecoder;
import com.linkedin.restli.internal.client.RestResponseDecoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Builds a type-bound Request for a batch of resources.
 *
 * This class has been deprecated. Please use {@link BatchGetEntityRequestBuilder} instead.
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
  @SuppressWarnings("deprecation")
  public static <RT extends RecordTemplate> BatchGetRequest<RT> batch(List<BatchGetRequest<RT>> requests,
                                                                      boolean batchFields)
  {
    final BatchGetRequest<RT> firstRequest = requests.get(0);
    Class<?> keyClass = firstRequest.getResourceSpec().getKeyClass();

    throwIfClassCompoundOrComplex(keyClass, "batch", "batchKV");

    final ResourceSpec firstResourceSpec = firstRequest.getResourceSpec();
    final Map<String, Object> batchQueryParams =
        getReadOnlyQueryParameters(BatchGetRequestUtil.getBatchQueryParam(requests, batchFields));

    return new BatchGetRequest<RT>(getReadOnlyHeaders(firstRequest.getHeaders()),
                                   getReadOnlyCookies(firstRequest.getCookies()),
                                   firstRequest.getResponseDecoder(),
                                   batchQueryParams,
                                   firstRequest.getQueryParamClasses(),
                                   firstResourceSpec,
                                   firstRequest.getBaseUriTemplate(),
                                   getReadOnlyPathKeys(firstRequest.getPathKeys()),
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
  @SuppressWarnings("deprecation")
  public static <K, RT extends RecordTemplate> BatchGetKVRequest<K, RT> batchKV(List<BatchGetKVRequest<K, RT>> requests,
                                                                      boolean batchFields)
  {
    final BatchGetKVRequest<K, RT> firstRequest = requests.get(0);
    final ResourceSpec firstResourceSpec = firstRequest.getResourceSpec();
    final Map<String, Object> batchQueryParams =
        getReadOnlyQueryParameters(BatchGetRequestUtil.getBatchQueryParam(requests, batchFields));

    return new BatchGetKVRequest<K, RT>(
                                   getReadOnlyHeaders(firstRequest.getHeaders()),
                                   getReadOnlyCookies(firstRequest.getCookies()),
                                   firstRequest.getResponseDecoder(),
                                   batchQueryParams,
                                   Collections.<String, Class<?>>emptyMap(),
                                   firstResourceSpec,
                                   firstRequest.getBaseUriTemplate(),
                                   getReadOnlyPathKeys(firstRequest.getPathKeys()),
                                   firstRequest.getRequestOptions());
  }

  /**
   * Converts an entity request to a batch one, for subsequent batching with other requests.
   * @param request to convert
   * @param <RT> type of entity template
   * @return batch request
   */
  @SuppressWarnings({ "unchecked", "deprecation" })
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

    return new BatchGetKVRequest<K, RT>(getReadOnlyHeaders(request.getHeaders()),
                                        getReadOnlyCookies(request.getCookies()),
                                        new BatchKVResponseDecoder<K, RT>(
                                            request.getEntityClass(),
                                            (Class<K>)request.getResourceProperties().getKeyType().getType(),
                                            request.getResourceProperties().getKeyParts(),
                                            request.getResourceProperties().getComplexKeyType() == null ?
                                                null :
                                                request.
                                                    getResourceProperties().
                                                    getComplexKeyType().
                                                    getKeyType().
                                                    getType(),
                                            request.getResourceProperties().getComplexKeyType() == null ?
                                                null :
                                                request.
                                                    getResourceProperties().
                                                    getComplexKeyType().
                                                    getParamsType().
                                                    getType()),
                                        getReadOnlyQueryParameters(queryParams),
                                        request.getQueryParamClasses(),
                                        request.getResourceSpec(),
                                        request.getBaseUriTemplate(),
                                        getReadOnlyPathKeys(request.getPathKeys()),
                                        request.getRequestOptions());
  }

  /**
   * Converts an entity request to a batch one, for subsequent batching with other requests.
   * @param request to convert
   * @param <RT> type of entity template
   * @return batch request
   */
  @SuppressWarnings("deprecation")
  public static <RT extends RecordTemplate> BatchGetRequest<RT> batch(GetRequest<RT> request)
  {
    Object id = request.getObjectId();

    if (id == null)
    {
      throw new IllegalArgumentException(
          "It is not possible to create a batch get request from a get request without an id.");
    }

    Class<?> keyClass = request.getResourceSpec().getKeyClass();

    throwIfClassCompoundOrComplex(keyClass, "batch", "batchKV");

    Map<String, Object> queryParams = new HashMap<String, Object>(request.getQueryParamsObjects());
    queryParams.put(RestConstants.QUERY_BATCH_IDS_PARAM,
                    new ArrayList<Object>(Arrays.asList(id)));

    return new BatchGetRequest<RT>(getReadOnlyHeaders(request.getHeaders()),
                                   getReadOnlyCookies(request.getCookies()),
                                   new BatchResponseDecoder<RT>(request.getEntityClass()),
                                   getReadOnlyQueryParameters(queryParams),
                                   Collections.<String, Class<?>>emptyMap(),
                                   request.getResourceSpec(),
                                   request.getBaseUriTemplate(),
                                   getReadOnlyPathKeys(request.getPathKeys()),
                                   request.getRequestOptions());
  }

  public BatchGetRequestBuilder(String baseUriTemplate,
                                Class<V> modelClass,
                                ResourceSpec resourceSpec,
                                RestliRequestOptions requestOptions)
  {
    this(baseUriTemplate, new BatchResponseDecoder<V>(modelClass), resourceSpec, requestOptions);
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

    throwIfClassCompoundOrComplex(keyClass, "build", "buildKV");

    return new BatchGetRequest<V>(buildReadOnlyHeaders(),
                                  buildReadOnlyCookies(),
                                  _decoder,
                                  buildReadOnlyQueryParameters(),
                                  Collections.<String, Class<?>>emptyMap(),
                                  _resourceSpec,
                                  getBaseUriTemplate(),
                                  buildReadOnlyPathKeys(),
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

    return new BatchGetKVRequest<K, V>(buildReadOnlyHeaders(),
                                       buildReadOnlyCookies(),
                                      decoder,
                                      buildReadOnlyQueryParameters(),
                                      getQueryParamClasses(),
                                      _resourceSpec,
                                      getBaseUriTemplate(),
                                      buildReadOnlyPathKeys(),
                                      getRequestOptions());
  }

  public BatchGetRequestBuilder<K, V> fields(PathSpec... fieldPaths)
  {
    addFields(fieldPaths);
    return this;
  }

  private static void throwIfClassCompoundOrComplex(Class<?> keyClass, String currentMethod, String replacementMethod)
  {
    if (CompoundKey.class.isAssignableFrom(keyClass) ||
        keyClass == ComplexResourceKey.class)
    {
      throw new UnsupportedOperationException(
          "The " + currentMethod + " method cannot be used with Compound or Complex key types. " +
              "Please use the " + replacementMethod + " method instead.");
    }
  }
}

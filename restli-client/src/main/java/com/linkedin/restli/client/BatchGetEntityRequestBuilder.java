/*
   Copyright (c) 2014 LinkedIn Corp.

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
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.client.BatchEntityResponseDecoder;
import com.linkedin.restli.internal.client.RestResponseDecoder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Builds a Request for a batch of resources. Both key and entity classes are specified.
 *
 * @param <V> resource key class
 * @param <V> entity template class
 *
 * @author Keren Jin
 */
public class BatchGetEntityRequestBuilder<K, V extends RecordTemplate> extends
    BatchKVRequestBuilder<K, V, BatchGetEntityRequest<K, V>>
{
  private final RestResponseDecoder<BatchKVResponse<K, EntityResponse<V>>> _decoder;

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
   * @param <V> type of entity template
   * @return batching request
   */
  public static <K, V extends RecordTemplate> BatchGetEntityRequest<K, V> batch(List<BatchGetEntityRequest<K, V>> requests)
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
   * @param <V> type of entity template
   * @return batching request
   */
  public static <K, V extends RecordTemplate> BatchGetEntityRequest<K, V> batch(List<BatchGetEntityRequest<K, V>> requests,
                                                                                boolean batchFields)
  {
    final BatchGetEntityRequest<K, V> firstRequest = requests.get(0);
    @SuppressWarnings("deprecation")
    final ResourceSpec firstResourceSpec = firstRequest.getResourceSpec();
    final Map<String, Object> batchQueryParams =
        getReadOnlyQueryParameters(BatchGetRequestUtil.getBatchQueryParam(requests, batchFields));

    return new BatchGetEntityRequest<K, V>(firstRequest.getHeaders(),
                                           firstRequest.getCookies(),
                                           firstRequest.getResponseDecoder(),
                                           batchQueryParams,
                                           firstRequest.getQueryParamClasses(),
                                           firstResourceSpec,
                                           firstRequest.getBaseUriTemplate(),
                                           firstRequest.getPathKeys(),
                                           firstRequest.getRequestOptions());
  }

  public BatchGetEntityRequestBuilder(String baseUriTemplate,
                                      RestResponseDecoder<BatchKVResponse<K, EntityResponse<V>>> decoder,
                                      ResourceSpec resourceSpec,
                                      RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
    _decoder = decoder;
  }

  @SuppressWarnings("unchecked")
  public BatchGetEntityRequestBuilder(String baseUriTemplate,
                                      ResourceSpec resourceSpec,
                                      RestliRequestOptions requestOptions)
  {
    this(baseUriTemplate,
          new BatchEntityResponseDecoder<K, V>(
              (TypeSpec<V>) resourceSpec.getValueType(),
              (TypeSpec<K>) resourceSpec.getKeyType(),
              resourceSpec.getKeyParts(),
              resourceSpec.getComplexKeyType()),
          resourceSpec,
          requestOptions);
  }

  @SuppressWarnings("unchecked")
  public BatchGetEntityRequestBuilder<K, V> ids(K... ids)
  {
    return ids(Arrays.asList(ids));
  }

  public BatchGetEntityRequestBuilder<K, V> ids(Collection<K> ids)
  {
    addKeys(ids);
    return this;
  }

  @Override
  public BatchGetEntityRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public BatchGetEntityRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public BatchGetEntityRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public BatchGetEntityRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  public BatchGetEntityRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public BatchGetEntityRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public BatchGetEntityRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public BatchGetEntityRequestBuilder<K, V> pathKey(String name, Object value)
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
  public BatchGetEntityRequest<K, V> build()
  {
    ensureBatchKeys();

    return new BatchGetEntityRequest<K, V>(buildReadOnlyHeaders(),
                                           buildReadOnlyCookies(),
                                           _decoder,
                                           buildReadOnlyQueryParameters(),
                                           getQueryParamClasses(),
                                           _resourceSpec,
                                           getBaseUriTemplate(),
                                           buildReadOnlyPathKeys(),
                                           getRequestOptions());
  }

  public BatchGetEntityRequestBuilder<K, V> fields(PathSpec... fieldPaths)
  {
    addFields(fieldPaths);
    return this;
  }
}

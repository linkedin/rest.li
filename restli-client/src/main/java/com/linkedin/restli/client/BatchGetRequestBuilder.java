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


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.BatchKVResponseDecoder;
import com.linkedin.restli.internal.client.BatchResponseDecoder;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import com.linkedin.restli.internal.client.URIUtil;
import com.linkedin.restli.internal.common.QueryParamsDataMap;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Builds a type-bound Request for a batch of resources.
 *
 * @param <V> batch element type
 *
 * @author Eran Leshem
 */
public class BatchGetRequestBuilder<K, V extends RecordTemplate> extends
    RestfulRequestBuilder<K, V, BatchGetRequest<V>>
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
    if (requests.size() < 2)
    {
      throw new IllegalArgumentException("Must have at least two requests to batch");
    }

    BatchGetRequest<RT> firstRequest = requests.get(0);
    ResourceSpec firstResourceSpec = firstRequest.getResourceSpec();
    String firstRequestBaseUriTemplate = firstRequest.getBaseUriTemplate();
    Map<String, Object> firstRequestPathKeys = firstRequest.getPathKeys();
    Set<PathSpec> firstFields = firstRequest.getFields();
    Set<Object> ids = new HashSet<Object>();

    // Default to no fields or to first request's fields, depending on batchFields flag
    Set<PathSpec> fields =
        batchFields ? new HashSet<PathSpec>() : firstFields;

    // Defensive shallow copy
    Map<String, Object> firstQueryParams = new HashMap<String, Object>(firstRequest.getQueryParamsObjects());

    firstQueryParams.remove(RestConstants.QUERY_BATCH_IDS_PARAM);
    firstQueryParams.remove(RestConstants.FIELDS_PARAM);

    for (BatchGetRequest<RT> request : requests)
    {
      String currentRequestBaseUriTemplate = request.getBaseUriTemplate();
      Map<String, Object> currentRequestPathKeys = request.getPathKeys();
      if (!currentRequestBaseUriTemplate.equals(firstRequestBaseUriTemplate) ||
          !currentRequestPathKeys.equals(firstRequestPathKeys))
      {
        throw new IllegalArgumentException("Requests must have same base URI template and path keys to batch");
      }

      if (!request.getResourceSpec().equals(firstResourceSpec))
      {
        throw new IllegalArgumentException("Requests must be for the same resource to batch");
      }

      Set<Object> requestIds = request.getObjectIds();
      Set<PathSpec> requestFields = request.getFields();
      // Defensive shallow copy
      Map<String, Object> queryParams = new HashMap<String, Object>(request.getQueryParamsObjects());

      queryParams.remove(RestConstants.FIELDS_PARAM);
      queryParams.remove(RestConstants.QUERY_BATCH_IDS_PARAM);

      // Enforce uniformity of query params excluding ids and fields
      if (!firstQueryParams.equals(queryParams))
      {
        throw new IllegalArgumentException("Requests must have same parameters to batch");
      }

      if (requestIds != null && !requestIds.isEmpty())
      {
        ids.addAll(requestIds);
      }

      if (batchFields)
      {
        if (requestFields == null || requestFields.isEmpty())
        {
          // Need all fields
          fields = null;
        }
        else if (fields != null)
        {
          fields.addAll(requestFields);
        }
      }
      else if (!requestFields.equals(firstFields))
      {
        throw new IllegalArgumentException("Requests must have same fields to batch");
      }
    }

    firstQueryParams.put(RestConstants.QUERY_BATCH_IDS_PARAM, ids);

    if (fields != null && !fields.isEmpty())
    {
      firstQueryParams.put(RestConstants.FIELDS_PARAM,
                           URIUtil.encodeFields(fields.toArray(new PathSpec[0])));
    }

    return new BatchGetRequest<RT>(firstRequest.getHeaders(),
                                   firstRequest.getResponseDecoder(),
                                   firstQueryParams,
                                   firstResourceSpec,
                                   firstRequest.getBaseUriTemplate(),
                                   firstRequest.getPathKeys());
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
                                   request.getPathKeys());
  }

  public BatchGetRequestBuilder(String baseUriTemplate, Class<V> modelClass, ResourceSpec resourceSpec)
  {
    this(baseUriTemplate, new BatchResponseDecoder<V>(modelClass), resourceSpec);
  }

  public BatchGetRequestBuilder(String baseUriTemplate,
                                RestResponseDecoder<BatchResponse<V>> decoder,
                                ResourceSpec resourceSpec)
  {
    super(baseUriTemplate, resourceSpec);
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
    return new BatchGetRequest<V>(_headers,
                                  _decoder,
                                  _queryParams,
                                  _resourceSpec,
                                  _baseURITemplate,
                                  _pathKeys);
  }

  public BatchGetKVRequest<K, V> buildKV()
  {
    //Framework code should ensure that the ResourceSpec matches the static types of these parameters
    @SuppressWarnings("unchecked")
    BatchKVResponseDecoder<K, V> decoder =
        new BatchKVResponseDecoder<K, V>((Class<V>) _resourceSpec.getValueClass(),
                                         (Class<K>) _resourceSpec.getKeyClass(),
                                         _resourceSpec.getKeyParts(),
                                         _resourceSpec.getKeyKeyClass(),
                                         _resourceSpec.getKeyParamsClass());

    return new BatchGetKVRequest<K, V>(ResourceMethod.BATCH_GET,
                                  _headers,
                                  decoder,
                                  _queryParams,
                                  _resourceSpec,
                                  _baseURITemplate,
                                  _pathKeys);
  }

  public BatchGetRequestBuilder<K, V> fields(PathSpec... fieldPaths)
  {
    addFields(fieldPaths);
    return this;
  }

}

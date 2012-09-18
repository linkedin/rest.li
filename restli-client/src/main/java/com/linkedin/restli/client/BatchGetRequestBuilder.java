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


import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.BatchKVResponseDecoder;
import com.linkedin.restli.internal.client.BatchResponseDecoder;
import com.linkedin.restli.internal.client.RestResponseDecoder;


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
  private Set<String> _ids;

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
    URI baseURI = firstRequest.getBaseURI();
    Set<String> ids = new HashSet<String>();

    // Default to no fields or to first request's fields, depending on batchFields flag
    Set<PathSpec> fields = batchFields? new HashSet<PathSpec>() : firstRequest.getFields();

    for (BatchGetRequest<RT> request : requests)
    {
      if (! request.getBaseURI().equals(baseURI))
      {
        throw new IllegalArgumentException("Requests must have same URI to batch");
      }

      ids.addAll(request.getIds());

      if (batchFields)
      {
        if (request.getFields() == null)
        {
          // Need all fields
          fields = null;
        }
        else if (fields != null)
        {
          fields.addAll(request.getFields());
        }
      }
      else if (! request.getFields().equals(firstRequest.getFields()))
      {
        throw new IllegalArgumentException("Requests must have same fields to batch");
      }
    }

    UriBuilder urlBuilder = UriBuilder.fromUri(baseURI);
    urlBuilder.queryParam(RestConstants.QUERY_BATCH_IDS_PARAM, ids);
    URI uri = urlBuilder.build();
    return new BatchGetRequest<RT>(uri,
                                   firstRequest.getHeaders(),
                                   firstRequest.getResponseDecoder(),
                                   baseURI,
                                   ids,
                                   fields,
                                   firstRequest.getResourceSpec());
  }

  /**
   * Converts an entity request to a batch one, for subsequent batching with other requests.
   * @param request to convert
   * @param <RT> type of entity template
   * @return batch request
   */
  public static <RT extends RecordTemplate> BatchGetRequest<RT> batch(GetRequest<RT> request)
  {
    if (request.getMethod() != ResourceMethod.GET)
    {
      throw new IllegalArgumentException("Can only batch a get request");
    }

    URI baseURI = request.getBaseURI();
    String id = request.getId();

    UriBuilder urlBuilder = UriBuilder.fromUri(baseURI);
    urlBuilder.queryParam(RestConstants.QUERY_BATCH_IDS_PARAM, id);
    URI uri = urlBuilder.build();
    return new BatchGetRequest<RT>(uri,
                                   request.getHeaders(),
                                   new BatchResponseDecoder<RT>(request.getEntityClass()),
                                   baseURI,
                                   Collections.singleton(id),
                                   request.getFields(), request.getResourceSpec());
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
    _ids = Collections.emptySet();
  }

  public BatchGetRequestBuilder<K, V> ids(K... ids)
  {
    return ids(Arrays.asList(ids));
  }

  public BatchGetRequestBuilder<K, V> ids(Collection<K> ids)
  {
    _ids = toStringSet(ids);
    addKeyParams(ids);
    return this;
  }

  @Override
  public BatchGetRequestBuilder<K, V> reqParam(String key, Object value)
  {
    super.reqParam(key, value);
    return this;
  }

  @Override
  public BatchGetRequestBuilder<K, V> param(String key, Object value)
  {
    super.param(key, value);
    return this;
  }

  @Override
  public BatchGetRequestBuilder<K, V> header(String key, String value)
  {
    super.header(key, value);
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
    URI baseUri = bindPathKeys();
    UriBuilder b = UriBuilder.fromUri(baseUri);
    appendQueryParams(b);

    return new BatchGetRequest<V>(b.build(),
                                  _headers,
                                  _decoder,
                                  baseUri,
                                  _ids,
                                  _fields,
                                  _resourceSpec);
  }

  public BatchGetKVRequest<K, V> buildKV()
  {
    URI baseUri = bindPathKeys();
    UriBuilder b = UriBuilder.fromUri(baseUri);
    appendQueryParams(b);

    //Framework code should ensure that the ResourceSpec matches the static types of these parameters
    @SuppressWarnings("unchecked")
    BatchKVResponseDecoder<K, V> decoder =
        new BatchKVResponseDecoder<K, V>((Class<V>) _resourceSpec.getValueClass(),
                                         (Class<K>) _resourceSpec.getKeyClass(),
                                         _resourceSpec.getKeyParts(),
                                         _resourceSpec.getKeyKeyClass(),
                                         _resourceSpec.getKeyParamsClass());

    return new BatchGetKVRequest<K, V>(b.build(),
                                  ResourceMethod.BATCH_GET,
                                  _headers,
                                  decoder,
                                  baseUri,
                                  _ids,
                                  _fields,
                                  _resourceSpec);
  }

  public BatchGetRequestBuilder<K, V> fields(PathSpec... fieldPaths)
  {
    addFields(fieldPaths);
    return this;
  }

}

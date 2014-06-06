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
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;

import java.util.Map;


/**
 * REST collection GetAll request builder.
 *
 * @param <V> entity type for resource
 *
 */
public class GetAllRequestBuilder<K, V extends RecordTemplate> extends
    RestfulRequestBuilder<K, V, GetAllRequest<V>>
{

  private final Class<V> _elementClass;

  @Deprecated
  public GetAllRequestBuilder(String baseUriTemplate, Class<V> elementClass, ResourceSpec resourceSpec)
  {
    this(baseUriTemplate, elementClass, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS);
  }

  public GetAllRequestBuilder(String baseUriTemplate,
                              Class<V> elementClass,
                              ResourceSpec resourceSpec,
                              RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
    _elementClass = elementClass;
  }

  public GetAllRequestBuilder<K, V> paginate(int start, int count)
  {
    paginateStart(start);
    paginateCount(count);
    return this;
  }

  public GetAllRequestBuilder<K, V> paginateStart(int start)
  {
    setParam(RestConstants.START_PARAM, String.valueOf(start));
    return this;
  }

  public GetAllRequestBuilder<K, V> paginateCount(int count)
  {
    setParam(RestConstants.COUNT_PARAM, String.valueOf(count));
    return this;
  }

  public GetAllRequestBuilder<K, V> fields(PathSpec... fieldPaths)
  {
    addFields(fieldPaths);
    return this;
  }

  @Override
  @Deprecated
  public GetAllRequestBuilder<K, V> param(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  @Deprecated
  public GetAllRequestBuilder<K, V> reqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public GetAllRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public GetAllRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public GetAllRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public GetAllRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  @Deprecated
  public GetAllRequestBuilder<K, V> header(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public GetAllRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public GetAllRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public GetAllRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public GetAllRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public GetAllRequest<V> build()
  {
    return new GetAllRequest<V>(_headers,
                                _elementClass,
                                _resourceSpec,
                                _queryParams,
                                _baseURITemplate,
                                _pathKeys,
                                _requestOptions,
                                _assocKey);
  }
}

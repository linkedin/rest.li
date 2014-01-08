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

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.common.ResourceSpec;

import java.util.Map;


/**
 * @author Josh Walker
 * @version $Revision: $
 */


public class UpdateRequestBuilder<K, V extends RecordTemplate> extends
    RestfulRequestBuilder<K, V, UpdateRequest<V>>
{
  private V _input;
  private K _id;

  @Deprecated
  public UpdateRequestBuilder(String baseUriTemplate, Class<V> valueClass, ResourceSpec resourceSpec)
  {
    this(baseUriTemplate, valueClass, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS);
  }

  public UpdateRequestBuilder(String baseUriTemplate,
                              Class<V> valueClass,
                              ResourceSpec resourceSpec,
                              RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
  }

  public UpdateRequestBuilder<K, V> id(K id)
  {
    _id = id;
    return this;
  }

  public UpdateRequestBuilder<K, V> input(V entity)
  {
    _input = entity;
    return this;
  }

  @Override
  @Deprecated
  public UpdateRequestBuilder<K, V> param(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  @Deprecated
  public UpdateRequestBuilder<K, V> reqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  @Deprecated
  public UpdateRequestBuilder<K, V> header(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public UpdateRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public UpdateRequest<V> build()
  {
    return new UpdateRequest<V>(_input,
                                _headers,
                                _resourceSpec,
                                _queryParams,
                                _baseURITemplate,
                                _pathKeys,
                                _requestOptions,
                                _id);
  }

}

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


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.client.CreateResponseDecoder;

import java.util.Map;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class CreateRequestBuilder<K, V extends RecordTemplate>
                extends RestfulRequestBuilder<K, V, CreateRequest<V>>
{
  private V _input;

  /**
   * @deprecated Please use {@link #CreateRequestBuilder(String, Class, com.linkedin.restli.common.ResourceSpec, RestliRequestOptions)}
   * @param baseUriTemplate
   * @param valueClass
   * @param resourceSpec
   */
  @Deprecated
  public CreateRequestBuilder(String baseUriTemplate, Class<V> valueClass, ResourceSpec resourceSpec)
  {
    this(baseUriTemplate, valueClass, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS);
  }

  public CreateRequestBuilder(String baseUriTemplate,
                              Class<V> valueClass,
                              ResourceSpec resourceSpec,
                              RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
  }

  public CreateRequestBuilder<K, V> input(V entity)
  {
    _input = entity;
    return this;
  }

  /**
   * @deprecated Please use {@link #setParam(String, Object)}
   * @param key
   * @param value
   * @return
   */
  @Override
  @Deprecated
  public CreateRequestBuilder<K, V> param(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  /**
   * @deprecated Please use {@link #setReqParam(String, Object)}
   * @param key
   * @param value
   * @return
   */
  @Override
  @Deprecated
  public CreateRequestBuilder<K, V> reqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public CreateRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public CreateRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public CreateRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public CreateRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  /**
   * @deprecated Please use {@link #setHeader(String, String)}
   * @param key
   * @param value value of the header
   * @return
   */
  @Override
  @Deprecated
  public CreateRequestBuilder<K, V> header(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public CreateRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public CreateRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public CreateRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public CreateRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  /**
   * Builds a create request for a resource.
   *
   * @return a create request for the response
   */
  @Override
  public CreateRequest<V> build()
  {
    @SuppressWarnings("unchecked")
    CreateResponseDecoder<K> createResponseDecoder = new CreateResponseDecoder<K>((TypeSpec<K>)_resourceSpec.getKeyType(),
                                                                                  _resourceSpec.getKeyParts(),
                                                                                  _resourceSpec.getComplexKeyType());
    return new CreateRequest<V>(_input,
                                _headers,
                                createResponseDecoder,
                                _resourceSpec,
                                _queryParams,
                                getBaseUriTemplate(),
                                _pathKeys,
                                getRequestOptions());
  }

}

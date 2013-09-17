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


import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.common.ResourceSpec;

import java.net.URI;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */


public class GetRequestBuilder<K, V extends RecordTemplate> extends
    RestfulRequestBuilder<K,V, GetRequest<V>>
{
  private final Class<V> _elementClass;
  private K              _id;

  public GetRequestBuilder(String baseUriTemplate, Class<V> elementClass, ResourceSpec resourceSpec)
  {
    super(baseUriTemplate, resourceSpec);
    _elementClass = elementClass;
  }

  public GetRequestBuilder<K, V> id(K id)
  {
    _id = id;
    return this;
  }

  @Override
  @Deprecated
  public GetRequestBuilder<K, V> param(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  @Deprecated
  public GetRequestBuilder<K, V> reqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public GetRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public GetRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public GetRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public GetRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  @Deprecated
  public GetRequestBuilder<K, V> header(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public GetRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public GetRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public GetRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public GetRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public GetRequest<V> build()
  {
    URI baseUri = bindPathKeys();
    UriBuilder b = UriBuilder.fromUri(bindPathKeys());
    appendKeyToPath(b, _id);
    appendQueryParams(b);

    return new GetRequest<V>(b.build(),
                             _headers,
                             _elementClass,
                             baseUri,
                             _id,
                             _queryParams,
                             _resourceSpec,
                             getResourcePath());
  }

  public GetRequestBuilder<K, V> fields(PathSpec... fieldPaths)
  {
    addFields(fieldPaths);
    return this;
  }
}

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


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.common.BatchRequest;
import com.linkedin.restli.common.ResourceSpec;

import java.net.URI;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class BatchUpdateRequestBuilder<K, V extends RecordTemplate> extends
    RestfulRequestBuilder<K, V, BatchUpdateRequest<K, V>>
{
  private final BatchRequest<V> _input;

  public BatchUpdateRequestBuilder(String baseUriTemplate,
                                   Class<V> valueClass,
                                   ResourceSpec resourceSpec)
  {
    super(baseUriTemplate, resourceSpec);
    _input = new BatchRequest<V>(new DataMap(), valueClass);
  }

  public BatchUpdateRequestBuilder<K, V> input(K id, V entity)
  {
    _input.getEntities().put(keyToString(id), entity);
    addKey(id);
    return this;
  }

  public BatchUpdateRequestBuilder<K, V> inputs(Map<K, V> entities)
  {
    addKeys(entities.keySet());
    for (Map.Entry<K, V> entry : entities.entrySet())
    {
      _input.getEntities().put(keyToString(entry.getKey()), entry.getValue());
    }
    return this;
  }

  @Override
  @Deprecated
  public BatchUpdateRequestBuilder<K, V> param(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  @Deprecated
  public BatchUpdateRequestBuilder<K, V> reqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  @Deprecated
  public BatchUpdateRequestBuilder<K, V> header(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public BatchUpdateRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public BatchUpdateRequest<K, V> build()
  {
    URI baseUri = bindPathKeys();
    UriBuilder b = UriBuilder.fromUri(baseUri);
    appendQueryParams(b);

    return new BatchUpdateRequest<K, V>(b.build(),
                                        _headers,
                                        baseUri,
                                        _input,
                                        _queryParams,
                                        _resourceSpec,
                                        getResourcePath());
  }

}

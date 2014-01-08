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

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class BatchDeleteRequestBuilder<K, V extends RecordTemplate> extends
    RestfulRequestBuilder<K, V, BatchDeleteRequest<K, V>>
{

  @Deprecated
  public BatchDeleteRequestBuilder(String baseUriTemplate,
                                   Class<V> valueClass,
                                   ResourceSpec resourceSpec)
  {
    this(baseUriTemplate, valueClass, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS);
  }

  public BatchDeleteRequestBuilder(String baseUriTemplate,
                                   Class<V> valueClass,
                                   ResourceSpec resourceSpec,
                                   RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
  }

  @SuppressWarnings("unchecked")
  public BatchDeleteRequestBuilder<K, V> ids(K... ids)
  {
    return ids(Arrays.asList(ids));
  }

  public BatchDeleteRequestBuilder<K, V> ids(Collection<K> ids)
  {
    addKeys(ids);
    return this;
  }

  @Override
  @Deprecated
  public BatchDeleteRequestBuilder<K, V> param(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  @Deprecated
  public BatchDeleteRequestBuilder<K, V> reqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public BatchDeleteRequestBuilder<K, V> setParam(String key, Object value)
  {
    super.setParam(key, value);
    return this;
  }

  @Override
  public BatchDeleteRequestBuilder<K, V> setReqParam(String key, Object value)
  {
    super.setReqParam(key, value);
    return this;
  }

  @Override
  public BatchDeleteRequestBuilder<K, V> addParam(String key, Object value)
  {
    super.addParam(key, value);
    return this;
  }

  @Override
  public BatchDeleteRequestBuilder<K, V> addReqParam(String key, Object value)
  {
    super.addReqParam(key, value);
    return this;
  }

  @Override
  @Deprecated
  public BatchDeleteRequestBuilder<K, V> header(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public BatchDeleteRequestBuilder<K, V> setHeader(String key, String value)
  {
    super.setHeader(key, value);
    return this;
  }

  @Override
  public BatchDeleteRequestBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  @Override
  public BatchDeleteRequestBuilder<K, V> addHeader(String name, String value)
  {
    super.addHeader(name, value);
    return this;
  }

  @Override
  public BatchDeleteRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public BatchDeleteRequest<K, V> build()
  {
    return new BatchDeleteRequest<K, V>(_headers,
                                        _queryParams,
                                        _resourceSpec,
                                        _baseURITemplate,
                                        _pathKeys,
                                        _requestOptions);
  }
}

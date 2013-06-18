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

import java.net.URI;
import java.util.Map;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.common.BatchRequest;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceSpec;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class BatchPartialUpdateRequestBuilder<K, V extends RecordTemplate> extends
    RestfulRequestBuilder<K, V, BatchPartialUpdateRequest<K, V>>
{
  private final BatchRequest<PatchRequest<V>> _input;

  @SuppressWarnings({ "unchecked" })
  public BatchPartialUpdateRequestBuilder(String baseUriTemplate,
                                          Class<V> valueClass,
                                          ResourceSpec resourceSpec)
  {
    super(baseUriTemplate, resourceSpec);
    _input = new BatchRequest<PatchRequest<V>>(new DataMap(), (Class) PatchRequest.class);
  }

  public BatchPartialUpdateRequestBuilder<K, V> input(K id, PatchRequest<V> patch)
  {
    _input.getEntities().put(keyToString(id), patch);
    addKey(id);
    return this;
  }

  public BatchPartialUpdateRequestBuilder<K, V> inputs(Map<K, PatchRequest<V>> patches)
  {
    addKeys(patches.keySet());
    for (Map.Entry<K, PatchRequest<V>> entry : patches.entrySet())
    {
      _input.getEntities().put(keyToString(entry.getKey()), entry.getValue());
    }
    return this;
  }

  @Override
  public BatchPartialUpdateRequestBuilder<K, V> param(String key, Object value)
  {
    super.param(key, value);
    return this;
  }

  @Override
  public BatchPartialUpdateRequestBuilder<K, V> reqParam(String key, Object value)
  {
    super.reqParam(key, value);
    return this;
  }

  @Override
  public BatchPartialUpdateRequestBuilder<K, V> header(String key, String value)
  {
    super.header(key, value);
    return this;
  }

  @Override
  public BatchPartialUpdateRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public BatchPartialUpdateRequest<K, V> build()
  {
    URI baseUri = bindPathKeys();
    UriBuilder b = UriBuilder.fromUri(baseUri);
    appendQueryParams(b);

    return new BatchPartialUpdateRequest<K, V>(b.build(),
                                               _headers,
                                               baseUri,
                                               _input,
                                               _queryParams,
                                               _resourceSpec,
                                               getResourcePath());
  }

}

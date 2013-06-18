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
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceSpec;

/**
 * @author Josh Walker
 * @version $Revision: $
 */


public class PartialUpdateRequestBuilder<K, V extends RecordTemplate> extends
    RestfulRequestBuilder<K, V, PartialUpdateRequest<V>>
{
  private K _id;
  private PatchRequest<V> _input;

  public PartialUpdateRequestBuilder(String baseUriTemplate, Class<V> valueClass, ResourceSpec resourceSpec)
  {
    super(baseUriTemplate, resourceSpec);
  }

  public PartialUpdateRequestBuilder<K, V> id(K id)
  {
    _id = id;
    return this;
  }

  public PartialUpdateRequestBuilder<K, V> input(PatchRequest<V> entity)
  {
    _input = entity;
    return this;
  }

  @Override
  public PartialUpdateRequestBuilder<K, V> param(String key, Object value)
  {
    super.param(key, value);
    return this;
  }

  @Override
  public PartialUpdateRequestBuilder<K, V> reqParam(String key, Object value)
  {
    super.reqParam(key, value);
    return this;
  }

  @Override
  public PartialUpdateRequestBuilder<K, V> header(String key, String value)
  {
    super.header(key, value);
    return this;
  }

  @Override
  public PartialUpdateRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public PartialUpdateRequest<V> build()
  {
    UriBuilder b = UriBuilder.fromUri(bindPathKeys());
    appendKeyToPath(b, _id);
    appendQueryParams(b);

    return new PartialUpdateRequest<V>(b.build(), _input, _headers, _resourceSpec, getResourcePath());
  }

}

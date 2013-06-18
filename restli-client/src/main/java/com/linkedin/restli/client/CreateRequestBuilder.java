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

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.common.ResourceSpec;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class CreateRequestBuilder<K, V extends RecordTemplate>
                extends RestfulRequestBuilder<K, V, CreateRequest<V>>
{
  private V _input;

  public CreateRequestBuilder(String baseUriTemplate, Class<V> valueClass, ResourceSpec resourceSpec)
  {
    super(baseUriTemplate, resourceSpec);
  }

  public CreateRequestBuilder<K, V> input(V entity)
  {
    _input = entity;
    return this;
  }

  @Override
  public CreateRequestBuilder<K, V> param(String key, Object value)
  {
    super.param(key, value);
    return this;
  }

  @Override
  public CreateRequestBuilder<K, V> reqParam(String key, Object value)
  {
    super.reqParam(key, value);
    return this;
  }

  @Override
  public CreateRequestBuilder<K, V> header(String key, String value)
  {
    super.header(key, value);
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
    URI baseUri = bindPathKeys();
    UriBuilder b = UriBuilder.fromUri(baseUri);
    appendQueryParams(b);

    return new CreateRequest<V>(b.build(), _input, _headers, _resourceSpec, getResourcePath());
  }

}

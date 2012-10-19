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

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class DeleteRequestBuilder<K, V extends RecordTemplate>
  extends RestfulRequestBuilder<K, V, DeleteRequest<V>>
{
  private K _id;

  public DeleteRequestBuilder(String baseUriTemplate, Class<V> valueClass, ResourceSpec resourceSpec)
  {
    super(baseUriTemplate, resourceSpec);
  }

  public DeleteRequestBuilder<K, V> id(K id)
  {
    _id = id;
    return this;
  }

  @Override
  public DeleteRequestBuilder<K, V> param(String key, Object value)
  {
    super.param(key, value);
    return this;
  }

  @Override
  public DeleteRequestBuilder<K, V> reqParam(String key, Object value)
  {
    super.reqParam(key, value);
    return this;
  }

  @Override
  public DeleteRequestBuilder<K, V> header(String key, String value)
  {
    super.header(key, value);
    return this;
  }

  @Override
  public DeleteRequestBuilder<K, V> pathKey(String name, Object value)
  {
    super.pathKey(name, value);
    return this;
  }

  @Override
  public DeleteRequest<V> build()
  {
    if (_id == null)
    {
      throw new IllegalArgumentException("id required to build delete request");
    }
    UriBuilder b = UriBuilder.fromUri(bindPathKeys());
    appendKeyToPath(b, _id);
    appendQueryParams(b);

    return new DeleteRequest<V>(b.build(), _headers, _resourceSpec);
  }

}

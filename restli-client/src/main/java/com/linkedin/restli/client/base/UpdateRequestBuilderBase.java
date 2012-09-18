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

package com.linkedin.restli.client.base;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.UpdateRequestBuilder;
import com.linkedin.restli.common.ResourceSpec;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public abstract class UpdateRequestBuilderBase<
        K,
        V extends RecordTemplate,
        RB extends UpdateRequestBuilderBase<K, V, RB>>
        extends UpdateRequestBuilder<K, V>
{
  protected UpdateRequestBuilderBase(String baseUriTemplate, Class<V> valueClass, ResourceSpec resourceSpec)
  {
    super(baseUriTemplate, valueClass, resourceSpec);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB id(K id)
  {
    return (RB) super.id(id);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB input(V entity)
  {
    return (RB) super.input(entity);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB header(String key, String value)
  {
    return (RB) super.header(key, value);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB pathKey(String name, Object value)
  {
    return (RB) super.pathKey(name, value);
  }
}

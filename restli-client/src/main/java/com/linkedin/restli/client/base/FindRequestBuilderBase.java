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

import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.FindRequestBuilder;
import com.linkedin.restli.common.ResourceSpec;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public abstract class FindRequestBuilderBase<
        K,
        V extends RecordTemplate,
        RB extends FindRequestBuilderBase<K, V, RB>>
        extends FindRequestBuilder<K, V>
{
  protected FindRequestBuilderBase(String baseUriTemplate, Class<V> elementClass, ResourceSpec resourceSpec)
  {
    super(baseUriTemplate, elementClass, resourceSpec);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB name(String name)
  {
    return (RB) super.name(name);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB assocKey(String key, Object value)
  {
    return (RB) super.assocKey(key, value);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB paginate(int start, int count)
  {
    return (RB) super.paginate(start, count);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB fields(PathSpec... fieldPaths)
  {
    return (RB) super.fields(fieldPaths);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB header(String key, String value)
  {
    return (RB) super.header(key, value);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB param(String key, Object value)
  {
    return (RB) super.param(key, value);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB reqParam(String key, Object value)
  {
    return (RB) super.reqParam(key, value);
  }
  
  @SuppressWarnings({"unchecked"})
  @Override
  public RB pathKey(String name, Object value)
  {
    return (RB) super.pathKey(name, value);
  }
}

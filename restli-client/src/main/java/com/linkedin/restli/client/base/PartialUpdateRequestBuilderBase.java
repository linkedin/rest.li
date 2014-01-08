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
import com.linkedin.restli.client.PartialUpdateRequestBuilder;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.ResourceSpec;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public abstract class PartialUpdateRequestBuilderBase<
        K,
        V extends RecordTemplate,
        RB extends PartialUpdateRequestBuilderBase<K, V, RB>>
        extends PartialUpdateRequestBuilder<K, V>
{
  @Deprecated
  protected PartialUpdateRequestBuilderBase(String baseUriTemplate, Class<V> valueClass, ResourceSpec resourceSpec)
  {
    this(baseUriTemplate, valueClass, resourceSpec, RestliRequestOptions.DEFAULT_OPTIONS);
  }

  protected PartialUpdateRequestBuilderBase(String baseUriTemplate,
                                            Class<V> valueClass,
                                            ResourceSpec resourceSpec,
                                            RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, valueClass, resourceSpec, requestOptions);
  }

  /**
   * @deprecated This method is deprecated and replaced by a set* method for API consistency reasons.
   * This method cannot be removed permanently until all projects use a version of Rest.li containing the
   * set* methods in a multi-project build environment for binary compatibility.
   */
  @SuppressWarnings({"unchecked"})
  @Deprecated
  @Override
  public RB header(String key, String value)
  {
    return (RB) super.header(key, value);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB setHeader(String key, String value)
  {
    return (RB) super.setHeader(key, value);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB setParam(String key, Object value)
  {
    return (RB) super.setParam(key, value);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB setReqParam(String key, Object value)
  {
    return (RB) super.setReqParam(key, value);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB addParam(String key, Object value)
  {
    return (RB) super.addParam(key, value);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB addReqParam(String key, Object value)
  {
    return (RB) super.addReqParam(key, value);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB pathKey(String name, Object value)
  {
    return (RB) super.pathKey(name, value);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB id(K id)
  {
    return (RB) super.id(id);
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public RB input(PatchRequest<V> entity)
  {
    return (RB) super.input(entity);
  }
}

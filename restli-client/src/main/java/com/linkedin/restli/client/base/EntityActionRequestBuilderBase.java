/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.restli.client.base;

import com.linkedin.restli.client.ActionRequest;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.common.ResourceSpec;

/**
 * The abstract base class for all generated request builders classes for entity-level actions.
 *
 * For entity-level actions the action is being performed against a specific record which means
 * that the key/id must be present in the request to identify which record to perform the action
 * on. This class adds validation when the request is built that the id was set to a non-null
 * value.
 */
public abstract class EntityActionRequestBuilderBase<K, V, RB extends ActionRequestBuilderBase<K, V, RB>>
        extends ActionRequestBuilderBase<K, V, RB>
{
    protected EntityActionRequestBuilderBase(String baseUriTemplate, Class<V> elementClass, ResourceSpec resourceSpec,
        RestliRequestOptions requestOptions)
    {
      super(baseUriTemplate, elementClass, resourceSpec, requestOptions);
    }

    @Override
    public ActionRequest<V> build() {
      if (!hasId())
      {
        throw new IllegalStateException("Entity-level action request is missing required id value.");
      }
      return super.build();
    }
}

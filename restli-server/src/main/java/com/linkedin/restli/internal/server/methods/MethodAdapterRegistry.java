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

package com.linkedin.restli.internal.server.methods;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.linkedin.restli.common.ResourceMethod;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class MethodAdapterRegistry
{
  private static final Map<ResourceMethod, RestLiMethodAdapter> _adapters = buildAdapterRegistry();

  private static Map<ResourceMethod, RestLiMethodAdapter> buildAdapterRegistry()
  {
    Map<ResourceMethod, RestLiMethodAdapter> result =
        new HashMap<ResourceMethod, RestLiMethodAdapter>(ResourceMethod.values().length);
    result.put(ResourceMethod.GET, new GetMethodAdapter());
    result.put(ResourceMethod.BATCH_GET, new BatchGetMethodAdapter());
    result.put(ResourceMethod.FINDER, new FinderMethodAdapter());
    result.put(ResourceMethod.CREATE, new CreateMethodAdapter());
    result.put(ResourceMethod.PARTIAL_UPDATE, new PatchMethodAdapter());
    result.put(ResourceMethod.UPDATE, new UpdateMethodAdapter());
    result.put(ResourceMethod.DELETE, new DeleteMethodAdapter());
    result.put(ResourceMethod.ACTION, new ActionMethodAdapter());
    result.put(ResourceMethod.BATCH_UPDATE, new BatchUpdateMethodAdapter());
    result.put(ResourceMethod.BATCH_PARTIAL_UPDATE, new BatchPatchMethodAdapter());
    result.put(ResourceMethod.BATCH_CREATE, new BatchCreateMethodAdapter());
    result.put(ResourceMethod.BATCH_DELETE, new BatchDeleteMethodAdapter());
    return Collections.unmodifiableMap(result);
  }

  /**
   * Lookup {@link RestLiMethodAdapter} by {@link ResourceMethod}.
   *
   * @param resourceMethod {@link ResourceMethod}
   * @return the correct {@link RestLiMethodAdapter} for the provided
   *         {@link ResourceMethod}
   */
  public static RestLiMethodAdapter getMethodAdapter(final ResourceMethod resourceMethod)
  {
    return _adapters.get(resourceMethod);
  }
}

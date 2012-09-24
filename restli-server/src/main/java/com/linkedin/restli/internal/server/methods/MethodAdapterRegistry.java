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
import com.linkedin.restli.internal.server.methods.arguments.ActionArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.BatchCreateArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.BatchDeleteArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.BatchGetArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.BatchPatchArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.BatchUpdateArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.CreateArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.FinderArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.GetArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.PatchArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.UpdateArgumentBuilder;
import com.linkedin.restli.internal.server.methods.response.ActionResponseBuilder;
import com.linkedin.restli.internal.server.methods.response.BatchCreateResponseBuilder;
import com.linkedin.restli.internal.server.methods.response.BatchGetResponseBuilder;
import com.linkedin.restli.internal.server.methods.response.BatchUpdateResponseBuilder;
import com.linkedin.restli.internal.server.methods.response.CreateResponseBuilder;
import com.linkedin.restli.internal.server.methods.response.CollectionResponseBuilder;
import com.linkedin.restli.internal.server.methods.response.GetResponseBuilder;
import com.linkedin.restli.internal.server.methods.response.RestLiResponseBuilder;
import com.linkedin.restli.internal.server.methods.response.UpdateResponseBuilder;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class MethodAdapterRegistry
{
  private static final Map<ResourceMethod, RestLiArgumentBuilder> _adapters = buildAdapterRegistry();

  private static final Map<ResourceMethod, RestLiResponseBuilder> _responseBuilders = buildResponseBuilders();

  private static Map<ResourceMethod, RestLiArgumentBuilder> buildAdapterRegistry()
  {
    Map<ResourceMethod, RestLiArgumentBuilder> result =
        new HashMap<ResourceMethod, RestLiArgumentBuilder>(ResourceMethod.values().length);
    result.put(ResourceMethod.GET, new GetArgumentBuilder());
    result.put(ResourceMethod.BATCH_GET, new BatchGetArgumentBuilder());
    result.put(ResourceMethod.FINDER, new FinderArgumentBuilder());
    result.put(ResourceMethod.CREATE, new CreateArgumentBuilder());
    result.put(ResourceMethod.PARTIAL_UPDATE, new PatchArgumentBuilder());
    result.put(ResourceMethod.UPDATE, new UpdateArgumentBuilder());
    result.put(ResourceMethod.DELETE, new GetArgumentBuilder());
    result.put(ResourceMethod.ACTION, new ActionArgumentBuilder());
    result.put(ResourceMethod.BATCH_UPDATE, new BatchUpdateArgumentBuilder());
    result.put(ResourceMethod.BATCH_PARTIAL_UPDATE, new BatchPatchArgumentBuilder());
    result.put(ResourceMethod.BATCH_CREATE, new BatchCreateArgumentBuilder());
    result.put(ResourceMethod.BATCH_DELETE, new BatchDeleteArgumentBuilder());
    result.put(ResourceMethod.GET_ALL, new FinderArgumentBuilder());
    return Collections.unmodifiableMap(result);
  }

  private static Map<ResourceMethod, RestLiResponseBuilder> buildResponseBuilders()
  {
    Map<ResourceMethod, RestLiResponseBuilder> result =
        new HashMap<ResourceMethod, RestLiResponseBuilder>(ResourceMethod.values().length);

    result.put(ResourceMethod.GET, new GetResponseBuilder());
    result.put(ResourceMethod.BATCH_GET, new BatchGetResponseBuilder());
    result.put(ResourceMethod.FINDER, new CollectionResponseBuilder());
    result.put(ResourceMethod.CREATE, new CreateResponseBuilder());
    result.put(ResourceMethod.PARTIAL_UPDATE, new UpdateResponseBuilder());
    result.put(ResourceMethod.UPDATE, new UpdateResponseBuilder());
    result.put(ResourceMethod.DELETE, new UpdateResponseBuilder());
    result.put(ResourceMethod.ACTION, new ActionResponseBuilder());
    result.put(ResourceMethod.BATCH_UPDATE, new BatchUpdateResponseBuilder());
    result.put(ResourceMethod.BATCH_PARTIAL_UPDATE, new BatchUpdateResponseBuilder());
    result.put(ResourceMethod.BATCH_CREATE, new BatchCreateResponseBuilder());
    result.put(ResourceMethod.BATCH_DELETE, new BatchUpdateResponseBuilder());
    result.put(ResourceMethod.GET_ALL, new CollectionResponseBuilder());

    return Collections.unmodifiableMap(result);
  }

  /**
   * Lookup {@link RestLiArgumentBuilder} by {@link ResourceMethod}.
   *
   * @param resourceMethod {@link ResourceMethod}
   * @return the correct {@link RestLiArgumentBuilder} for the provided
   *         {@link ResourceMethod}
   */
  public static RestLiArgumentBuilder getArgumentBuilder(final ResourceMethod resourceMethod)
  {
    return _adapters.get(resourceMethod);
  }

  public static RestLiResponseBuilder getResponsebuilder(final ResourceMethod resourceMethod)
  {
    return _responseBuilders.get(resourceMethod);
  }
}

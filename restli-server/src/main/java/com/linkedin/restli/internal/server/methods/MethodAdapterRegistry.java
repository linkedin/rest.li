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

package com.linkedin.restli.internal.server.methods;

import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.methods.arguments.ActionArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.BatchCreateArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.BatchDeleteArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.BatchGetArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.BatchPatchArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.BatchUpdateArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.CollectionArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.CreateArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.GetArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.PatchArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.internal.server.methods.arguments.UpdateArgumentBuilder;
import com.linkedin.restli.internal.server.response.ActionResponseBuilder;
import com.linkedin.restli.internal.server.response.BatchCreateResponseBuilder;
import com.linkedin.restli.internal.server.response.BatchDeleteResponseBuilder;
import com.linkedin.restli.internal.server.response.BatchGetResponseBuilder;
import com.linkedin.restli.internal.server.response.BatchPartialUpdateResponseBuilder;
import com.linkedin.restli.internal.server.response.BatchUpdateResponseBuilder;
import com.linkedin.restli.internal.server.response.CreateResponseBuilder;
import com.linkedin.restli.internal.server.response.DeleteResponseBuilder;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.response.FinderResponseBuilder;
import com.linkedin.restli.internal.server.response.GetAllResponseBuilder;
import com.linkedin.restli.internal.server.response.GetResponseBuilder;
import com.linkedin.restli.internal.server.response.PartialUpdateResponseBuilder;
import com.linkedin.restli.internal.server.response.RestLiResponseBuilder;
import com.linkedin.restli.internal.server.response.UpdateResponseBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class MethodAdapterRegistry
{
  private final Map<ResourceMethod, RestLiArgumentBuilder> _adapters;
  private final Map<ResourceMethod, RestLiResponseBuilder<?>> _responseBuilders;

  public MethodAdapterRegistry(ErrorResponseBuilder errorResponseBuilder)
  {
    _adapters = buildAdapterRegistry();
    _responseBuilders = buildResponseBuilders(errorResponseBuilder);
  }

  private Map<ResourceMethod, RestLiArgumentBuilder> buildAdapterRegistry()
  {
    Map<ResourceMethod, RestLiArgumentBuilder> result =
        new HashMap<>(ResourceMethod.values().length);
    result.put(ResourceMethod.GET, new GetArgumentBuilder());
    result.put(ResourceMethod.BATCH_GET, new BatchGetArgumentBuilder());
    result.put(ResourceMethod.FINDER, new CollectionArgumentBuilder());
    result.put(ResourceMethod.CREATE, new CreateArgumentBuilder());
    result.put(ResourceMethod.PARTIAL_UPDATE, new PatchArgumentBuilder());
    result.put(ResourceMethod.UPDATE, new UpdateArgumentBuilder());
    result.put(ResourceMethod.DELETE, new GetArgumentBuilder());
    result.put(ResourceMethod.ACTION, new ActionArgumentBuilder());
    result.put(ResourceMethod.BATCH_UPDATE, new BatchUpdateArgumentBuilder());
    result.put(ResourceMethod.BATCH_PARTIAL_UPDATE, new BatchPatchArgumentBuilder());
    result.put(ResourceMethod.BATCH_CREATE, new BatchCreateArgumentBuilder());
    result.put(ResourceMethod.BATCH_DELETE, new BatchDeleteArgumentBuilder());
    result.put(ResourceMethod.GET_ALL, new CollectionArgumentBuilder());
    return Collections.unmodifiableMap(result);
  }

  private Map<ResourceMethod, RestLiResponseBuilder<?>> buildResponseBuilders(ErrorResponseBuilder errorResponseBuilder)
  {
    Map<ResourceMethod, RestLiResponseBuilder<?>> result =
        new HashMap<>(ResourceMethod.values().length);

    result.put(ResourceMethod.GET, new GetResponseBuilder());
    result.put(ResourceMethod.BATCH_GET, new BatchGetResponseBuilder(errorResponseBuilder));
    result.put(ResourceMethod.FINDER, new FinderResponseBuilder());
    result.put(ResourceMethod.CREATE, new CreateResponseBuilder());
    result.put(ResourceMethod.PARTIAL_UPDATE, new PartialUpdateResponseBuilder());
    result.put(ResourceMethod.UPDATE, new UpdateResponseBuilder());
    result.put(ResourceMethod.DELETE, new DeleteResponseBuilder());
    result.put(ResourceMethod.ACTION, new ActionResponseBuilder());
    result.put(ResourceMethod.BATCH_UPDATE, new BatchUpdateResponseBuilder(errorResponseBuilder));
    result.put(ResourceMethod.BATCH_PARTIAL_UPDATE, new BatchPartialUpdateResponseBuilder(errorResponseBuilder));
    result.put(ResourceMethod.BATCH_CREATE, new BatchCreateResponseBuilder(errorResponseBuilder));
    result.put(ResourceMethod.BATCH_DELETE, new BatchDeleteResponseBuilder(errorResponseBuilder));
    result.put(ResourceMethod.GET_ALL, new GetAllResponseBuilder());

    return Collections.unmodifiableMap(result);
  }

  /**
   * Lookup {@link RestLiArgumentBuilder} by {@link ResourceMethod}.
   *
   * @param resourceMethod {@link ResourceMethod}
   * @return the correct {@link RestLiArgumentBuilder} for the provided
   *         {@link ResourceMethod}
   */
  public RestLiArgumentBuilder getArgumentBuilder(final ResourceMethod resourceMethod)
  {
    return _adapters.get(resourceMethod);
  }

  public RestLiResponseBuilder<?> getResponseBuilder(final ResourceMethod resourceMethod)
  {
    return _responseBuilders.get(resourceMethod);
  }
}

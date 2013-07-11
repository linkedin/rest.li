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

package com.linkedin.restli.internal.server.methods.arguments;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.util.ArgumentUtils;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class PatchArgumentBuilder implements RestLiArgumentBuilder
{
  @Override
  public Object[] buildArguments(final RoutingResult routingResult,
                                 final RestRequest request)
  {
    Object[] positionalArgs;
    RecordTemplate patch = ArgumentUtils.extractEntity(request, PatchRequest.class);

    if (ArgumentUtils.hasResourceKey(routingResult))
    {
      Object keyValue = ArgumentUtils.getResourceKey(routingResult);
      positionalArgs = new Object[]{ keyValue, patch };
    }
    else
    {
      positionalArgs = new Object[] { patch };
    }

    return ArgumentBuilder.buildArgs(positionalArgs,
                                     routingResult.getResourceMethod().getParameters(),
                                     routingResult.getContext());
  }
}

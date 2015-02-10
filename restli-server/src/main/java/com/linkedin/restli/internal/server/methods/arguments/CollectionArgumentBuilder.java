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

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiRequestDataImpl;

/**
 * @author Josh Walker
 * @version $Revision: $
 */
public class CollectionArgumentBuilder implements RestLiArgumentBuilder
{
  @Override
  public Object[] buildArguments(RestLiRequestData requestData, RoutingResult routingResult)
  {
    return ArgumentBuilder.buildArgs(new Object[0],
                                     routingResult.getResourceMethod(),
                                     routingResult.getContext(),
                                     null);
  }

  @Override
  public RestLiRequestData extractRequestData(RoutingResult routingResult, RestRequest request)
  {
    return new RestLiRequestDataImpl.Builder().build();
  }
}

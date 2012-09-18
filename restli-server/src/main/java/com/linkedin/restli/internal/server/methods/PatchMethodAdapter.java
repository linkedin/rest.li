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

import java.io.IOException;
import java.util.Map;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.util.ArgumentUtils;
import com.linkedin.restli.server.UpdateResponse;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class PatchMethodAdapter implements RestLiMethodAdapter
{

  PatchMethodAdapter()
  {
  }

  @Override
  public PartialRestResponse buildResponse(final RestRequest request,
                                           final RoutingResult routingResult,
                                           final Object object,
                                           final Map<String, String> headers)
      throws IOException
  {
    UpdateResponse updateResponse = (UpdateResponse)object;
    return new PartialRestResponse(updateResponse.getStatus());
  }

  @Override
  public Object[] buildArguments(final RoutingResult routingResult,
                                 final RestRequest request)
  {
    Object keyValue = ArgumentUtils.getResourceKey(routingResult);
    RecordTemplate patch = ArgumentUtils.extractEntity(request, PatchRequest.class);

    Object[] positionalArgs = { keyValue, patch };
    return ArgumentBuilder.buildArgs(positionalArgs,
                                     routingResult.getResourceMethod().getParameters(),
                                     routingResult.getContext());
  }
}

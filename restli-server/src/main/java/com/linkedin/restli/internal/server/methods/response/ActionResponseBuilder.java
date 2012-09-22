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

package com.linkedin.restli.internal.server.methods.response;

import java.io.IOException;
import java.util.Map;

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RoutingResult;

public class ActionResponseBuilder implements RestLiResponseBuilder
{

  @Override
  public PartialRestResponse buildResponse(final RestRequest request,
                                           final RoutingResult routingResult,
                                           final Object result,
                                           final Map<String, String> headers) throws IOException
  {
    // note that we always return 200 OK for ActionResponse: all response meaning is
    // encoded in the response entity. The HTTP response code is unused.
    headers.put(RestConstants.HEADER_LINKEDIN_TYPE, ActionResponse.class.getName());
    headers.put(RestConstants.HEADER_LINKEDIN_SUB_TYPE, result.getClass().getName());

    ActionResponse<?> actionResponse = new ActionResponse<Object>(result);
    return new PartialRestResponse(actionResponse);
  }
}

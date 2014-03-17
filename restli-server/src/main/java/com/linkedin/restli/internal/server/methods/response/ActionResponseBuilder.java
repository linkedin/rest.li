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

import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.FieldDef;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.server.ActionResult;

import java.io.IOException;
import java.util.Map;

public class ActionResponseBuilder implements RestLiResponseBuilder
{

  @Override
  public PartialRestResponse buildResponse(final RestRequest request,
                                           final RoutingResult routingResult,
                                           final Object result,
                                           final Map<String, String> headers) throws IOException
  {
    final Object value;
    final HttpStatus status;

    if (result instanceof ActionResult)
    {
      final ActionResult<?> actionResult = (ActionResult<?>) result;
      value = actionResult.getValue();
      status = actionResult.getStatus();
    }
    else
    {
      value = result;
      status = HttpStatus.S_200_OK;
    }

    RecordDataSchema actionReturnRecordDataSchema = routingResult.getResourceMethod().getActionReturnRecordDataSchema();
    @SuppressWarnings("unchecked")
    FieldDef<Object> actionReturnFieldDef = (FieldDef<Object>)routingResult.getResourceMethod().getActionReturnFieldDef();
    final ActionResponse<?> actionResponse = new ActionResponse<Object>(value, actionReturnFieldDef, actionReturnRecordDataSchema);

    return new PartialRestResponse.Builder().status(status).entity(actionResponse).headers(headers).build();
  }
}

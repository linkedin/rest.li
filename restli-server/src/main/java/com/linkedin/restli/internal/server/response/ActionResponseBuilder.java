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

package com.linkedin.restli.internal.server.response;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.FieldDef;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.Request;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.server.ActionResult;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;


public class ActionResponseBuilder implements RestLiResponseBuilder<RestLiResponseData<ActionResponseEnvelope>>
{

  @Override
  public RestLiResponse buildResponse(RoutingResult routingResult,
                                           RestLiResponseData<ActionResponseEnvelope> responseData)
  {
    return new RestLiResponse.Builder().status(responseData.getResponseEnvelope().getStatus())
                                            .entity(responseData.getResponseEnvelope().getRecord())
                                            .headers(responseData.getHeaders())
                                            .cookies(responseData.getCookies())
                                            .build();
  }

  /**
   * {@inheritDoc}
   *
   * @param result The result for a Rest.li ACTION method. It can be the return value for the ACTION itself, or the
   *               return value wrapped in an {@link ActionResult}.
   */
  @Override
  public RestLiResponseData<ActionResponseEnvelope> buildRestLiResponseData(Request request,
                                                    RoutingResult routingResult,
                                                    Object result,
                                                    Map<String, String> headers,
                                                    List<HttpCookie> cookies)
  {
    final Object value;
    final HttpStatus status;
    if (result instanceof ActionResult)
    {
      final ActionResult<?> actionResult = (ActionResult<?>) result;
      value = actionResult.getValue();
      status = actionResult.getStatus();
      if (status == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null HttpStatus inside of an ActionResult returned by the resource method: "
                + routingResult.getResourceMethod());
      }

      // When it has an ActionResult<Void> type response, it should return null but not empty body record
      if (routingResult.getResourceMethod().getActionReturnType() == Void.TYPE) {
        return new RestLiResponseDataImpl<>(new ActionResponseEnvelope(status, null), headers, cookies);
      }
    }
    else
    {
      // when value == null and return type is void, it is handled outside in RestLiResponseHandler
      value = result;
      status = HttpStatus.S_200_OK;
    }
    RecordDataSchema actionReturnRecordDataSchema = routingResult.getResourceMethod().getActionReturnRecordDataSchema();

    if (value != null && RecordTemplate.class.isAssignableFrom(value.getClass())
        && routingResult.getContext().isFillInDefaultsRequested())
    {
      RecordTemplate actionResponseRecordTemplate = (RecordTemplate) value;
      DataMap dataWithoutDefault = actionResponseRecordTemplate.data();
      System.out.println("Fill in default for action result " + value.getClass().getSimpleName() + ", "
          + actionResponseRecordTemplate.schema().getFullName());
      DataMap dataWithDefault = ResponseUtils.fillInDefaultValues(actionResponseRecordTemplate.schema(), dataWithoutDefault);
      Object valueWithDefault = null;
      try
      {
        valueWithDefault = (Object) value.getClass().getConstructor(DataMap.class).newInstance(dataWithDefault);
      }
      catch (Exception e)
      {
        System.out.println("Happened " + e.getCause());
        valueWithDefault = value;
      }
      @SuppressWarnings("unchecked")
      FieldDef<Object> actionReturnFieldDef =
          (FieldDef<Object>) routingResult.getResourceMethod().getActionReturnFieldDef();
      final ActionResponse<?> actionResponse =
          new ActionResponse<>(valueWithDefault, actionReturnFieldDef, actionReturnRecordDataSchema);
      return new RestLiResponseDataImpl<>(new ActionResponseEnvelope(status, actionResponse), headers, cookies);
    }
    else
    {
      @SuppressWarnings("unchecked")
      FieldDef<Object> actionReturnFieldDef =
          (FieldDef<Object>) routingResult.getResourceMethod().getActionReturnFieldDef();
      final ActionResponse<?> actionResponse =
          new ActionResponse<>(value, actionReturnFieldDef, actionReturnRecordDataSchema);
      return new RestLiResponseDataImpl<>(new ActionResponseEnvelope(status, actionResponse), headers, cookies);
    }
  }
}

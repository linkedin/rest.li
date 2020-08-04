/*
    Copyright (c) 2017 LinkedIn Corp.

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
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.restli.common.EntityResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UpdateEntityResponse;
import com.linkedin.restli.server.UpdateResponse;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;


/**
 * Builder for {@link com.linkedin.restli.common.ResourceMethod#PARTIAL_UPDATE} responses.
 * Will build a response data object of type {@link ResponseType#STATUS_ONLY} if the response contains an entity,
 * otherwise will build an object of type {@link ResponseType#SINGLE_ENTITY}.
 *
 * @author Evan Williams
 */
public class PartialUpdateResponseBuilder implements RestLiResponseBuilder<RestLiResponseData<PartialUpdateResponseEnvelope>>
{
  @Override
  public RestLiResponse buildResponse(RoutingResult routingResult, RestLiResponseData<PartialUpdateResponseEnvelope> responseData)
  {
    return new RestLiResponse.Builder().entity(responseData.getResponseEnvelope().getRecord())
        .headers(responseData.getHeaders())
        .cookies(responseData.getCookies())
        .status(responseData.getResponseEnvelope().getStatus())
        .build();
  }

  @Override
  @SuppressWarnings({"unchecked"})
  public RestLiResponseData<PartialUpdateResponseEnvelope> buildRestLiResponseData(Request request,
                                                                                   RoutingResult routingResult,
                                                                                   Object result,
                                                                                   Map<String, String> headers,
                                                                                   List<HttpCookie> cookies)
  {
    UpdateResponse updateResponse = (UpdateResponse) result;

    // Verify that the status in the UpdateResponse is not null. If so, this is a developer error.
    if (updateResponse.getStatus() == null)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
          "Unexpected null encountered. HttpStatus is null inside of an UpdateResponse returned by the resource method: "
              + routingResult.getResourceMethod());
    }

    final ResourceContext resourceContext = routingResult.getContext();

    RecordTemplate entityResponse = null;
    // Add patched entity to the response if result is an UpdateEntityResponse and the client is asking for the entity
    if (result instanceof UpdateEntityResponse && resourceContext.isReturnEntityRequested())
    {
      UpdateEntityResponse<?> updateEntityResponse = (UpdateEntityResponse<?>) updateResponse;
      if (updateEntityResponse.hasEntity())
      {
        DataMap entityData = updateEntityResponse.getEntity().data();

        TimingContextUtil.beginTiming(resourceContext.getRawRequestContext(),
            FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_PROJECTION_APPLY.key());

        final DataMap data = RestUtils.projectFields(entityData, resourceContext);

        TimingContextUtil.endTiming(resourceContext.getRawRequestContext(),
            FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_PROJECTION_APPLY.key());

        // Returned entity is to be added to the response envelope
        entityResponse = new EntityResponse<>(data, updateEntityResponse.getEntity().getClass());
      }
      else
      {
        // The entity in the UpdateEntityResponse should not be null. This is a developer error.
        // If trying to return an error response, a RestLiServiceException should be thrown in the resource method.
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Entity is null inside of an UpdateEntityResponse returned by the resource method: "
                + routingResult.getResourceMethod());
      }
    }

    return new RestLiResponseDataImpl<>(new PartialUpdateResponseEnvelope(updateResponse.getStatus(), entityResponse), headers, cookies);
  }
}

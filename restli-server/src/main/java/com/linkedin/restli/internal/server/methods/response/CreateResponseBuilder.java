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


import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.response.RecordResponseEnvelope;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiServiceException;

import java.util.Map;


public class CreateResponseBuilder implements RestLiResponseBuilder
{
  @Override
  public PartialRestResponse buildResponse(RoutingResult routingResult, RestLiResponseEnvelope responseData)
  {
    return new PartialRestResponse.Builder().entity(responseData.getRecordResponseEnvelope().getRecord())
                                            .headers(responseData.getHeaders()).status(responseData.getStatus())
                                            .build();
  }

  @Override
  public RestLiResponseEnvelope buildRestLiResponseData(RestRequest request, RoutingResult routingResult,
                                                             Object result, Map<String, String> headers)
  {
    CreateResponse createResponse = (CreateResponse) result;
    if (createResponse.hasId())
    {
      final ProtocolVersion protocolVersion = ((ServerResourceContext) routingResult.getContext()).getRestliProtocolVersion();
      String stringKey = URIParamUtils.encodeKeyForUri(createResponse.getId(), UriComponent.Type.PATH_SEGMENT, protocolVersion);
      UriBuilder uribuilder = UriBuilder.fromUri(request.getURI());
      uribuilder.path(stringKey);
      headers.put(RestConstants.HEADER_LOCATION, uribuilder.build((Object) null).toString());
    }
    IdResponse<?> idResponse = new IdResponse<Object>(createResponse.getId());

    //Verify that a null status was not passed into the CreateResponse. If so, this is a developer error.
    if (createResponse.getStatus() == null)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
          "Unexpected null encountered. HttpStatus is null inside of a CreateResponse from the resource method: "
              + routingResult.getResourceMethod());
    }

    return new RecordResponseEnvelope(createResponse.getStatus(), idResponse, headers);
  }
}

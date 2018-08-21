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
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.r2.message.Request;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.IdResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.CreateKVResponse;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;


public class CreateResponseBuilder implements RestLiResponseBuilder<RestLiResponseData<CreateResponseEnvelope>>
{
  @Override
  public RestLiResponse buildResponse(RoutingResult routingResult, RestLiResponseData<CreateResponseEnvelope> responseData)
  {
    return new RestLiResponse.Builder().entity(responseData.getResponseEnvelope().getRecord())
                                            .headers(responseData.getHeaders())
                                            .cookies(responseData.getCookies())
                                            .status(responseData.getResponseEnvelope().getStatus())
                                            .build();
  }

  /**
   * {@inheritDoc}
   *
   * @param result The result of a Rest.li CREATE method. It is an instance of {@link CreateResponse}; or subclass
   *               {@link CreateKVResponse}, if the CREATE method returns the entity.
   */
  @Override
  public RestLiResponseData<CreateResponseEnvelope> buildRestLiResponseData(Request request,
                                                    RoutingResult routingResult,
                                                    Object result,
                                                    Map<String, String> headers,
                                                    List<HttpCookie> cookies)
  {
    CreateResponse createResponse = (CreateResponse) result;
    boolean isGetAfterCreate = createResponse instanceof CreateKVResponse;

    if (createResponse.hasError())
    {
      RestLiServiceException exception = createResponse.getError();
      return new RestLiResponseDataImpl<>(new CreateResponseEnvelope(exception, isGetAfterCreate), headers, cookies);
    }

    Object id = null;
    if (createResponse.hasId())
    {
      id = ResponseUtils.translateCanonicalKeyToAlternativeKeyIfNeeded(createResponse.getId(), routingResult);
      final ProtocolVersion protocolVersion = routingResult.getContext().getRestliProtocolVersion();
      String stringKey = URIParamUtils.encodeKeyForUri(id, UriComponent.Type.PATH_SEGMENT, protocolVersion);
      UriBuilder uribuilder = UriBuilder.fromUri(request.getURI());
      uribuilder.path(stringKey);
      uribuilder.replaceQuery(null);
      if (routingResult.getContext().hasParameter(RestConstants.ALT_KEY_PARAM))
      {
        // add altkey param to location URI
        uribuilder.queryParam(RestConstants.ALT_KEY_PARAM, routingResult.getContext().getParameter(RestConstants.ALT_KEY_PARAM));
      }
      headers.put(RestConstants.HEADER_LOCATION, uribuilder.build((Object) null).toString());
      headers.put(HeaderUtil.getIdHeaderName(protocolVersion), URIParamUtils.encodeKeyForHeader(id, protocolVersion));
    }

    // Verify that a null status was not passed into the CreateResponse. If so, this is a developer error.
    if (createResponse.getStatus() == null)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
          "Unexpected null encountered. HttpStatus is null inside of a CreateResponse from the resource method: "
              + routingResult.getResourceMethod());
    }

    final ResourceContext resourceContext = routingResult.getContext();

    RecordTemplate idResponse;
    if (createResponse instanceof CreateKVResponse && resourceContext.shouldReturnEntity())
    {
      RecordTemplate entity = ((CreateKVResponse<?, ?>) createResponse).getEntity();

      // Verify that a null entity was not passed into the CreateKVResponse. If so, this is a developer error.
      if (entity == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Entity is null inside of a CreateKVResponse when the entity should be returned. In resource method: " + routingResult.getResourceMethod());
      }

      DataMap entityData = entity.data();
      final DataMap data = RestUtils.projectFields(entityData, resourceContext.getProjectionMode(), resourceContext.getProjectionMask());
      idResponse = new AnyRecord(data);
      // Ideally, we should set an IdEntityResponse to the envelope. But we are keeping AnyRecord
      // to make sure the runtime object is backwards compatible.
      // idResponse = new IdEntityResponse<>(id, new AnyRecord(data));
    }
    else //Instance of idResponse
    {
      idResponse = new IdResponse<>(id);
    }

    return new RestLiResponseDataImpl<>(new CreateResponseEnvelope(createResponse.getStatus(), idResponse, isGetAfterCreate), headers, cookies);
  }
}

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

package com.linkedin.restli.internal.server.response;


import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.CreateIdEntityStatus;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.BatchCreateKVResult;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.CreateKVResponse;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.ResourceContext;


import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class BatchCreateResponseBuilder implements RestLiResponseBuilder
{
  private final ErrorResponseBuilder _errorResponseBuilder;

  public BatchCreateResponseBuilder(ErrorResponseBuilder errorResponseBuilder)
  {
    _errorResponseBuilder = errorResponseBuilder;
  }

  @Override
  @SuppressWarnings("unchecked")
  public PartialRestResponse buildResponse(RoutingResult routingResult, RestLiResponseData responseData)
  {
    List<BatchCreateResponseEnvelope.CollectionCreateResponseItem> collectionCreateResponses =
                                                responseData.getBatchCreateResponseEnvelope().getCreateResponses();
    List<CreateIdStatus<Object>> formattedResponses = new ArrayList<CreateIdStatus<Object>>(collectionCreateResponses.size());

    // Iterate through the responses and generate the ErrorResponse with the appropriate override for exceptions.
    // Otherwise, add the result as is.
    for (BatchCreateResponseEnvelope.CollectionCreateResponseItem response : collectionCreateResponses)
    {
      if (response.isErrorResponse())
      {
        RestLiServiceException exception = response.getException();
        formattedResponses.add(new CreateIdStatus<Object>(exception.getStatus().getCode(),
                                                          response.getId(),
                                                          _errorResponseBuilder.buildErrorResponse(exception),
                                                          ProtocolVersionUtil.extractProtocolVersion(responseData.getHeaders())));
      }
      else
      {
        formattedResponses.add((CreateIdStatus<Object>) response.getRecord());
      }
    }

    PartialRestResponse.Builder builder = new PartialRestResponse.Builder();
    BatchCreateIdResponse<Object> batchCreateIdResponse = new BatchCreateIdResponse<Object>(formattedResponses);
    return builder.headers(responseData.getHeaders()).cookies(responseData.getCookies()).entity(batchCreateIdResponse).build();
  }

  @Override
  public RestLiResponseData buildRestLiResponseData(RestRequest request,
                                                    RoutingResult routingResult,
                                                    Object result,
                                                    Map<String, String> headers,
                                                    List<HttpCookie> cookies)
  {
    Object altKey = null;
    if (routingResult.getContext().hasParameter(RestConstants.ALT_KEY_PARAM))
    {
      altKey = routingResult.getContext().getParameter(RestConstants.ALT_KEY_PARAM);
    }
    final ProtocolVersion protocolVersion = ProtocolVersionUtil.extractProtocolVersion(headers);


    if (result instanceof BatchCreateKVResult)
    {
      BatchCreateKVResult<?, ?> list = (BatchCreateKVResult<?, ?>)result;
      if (list.getResults() == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                         "Unexpected null encountered. Null List inside of a BatchCreateKVResult returned by the resource method: " + routingResult
                                             .getResourceMethod());
      }
      List<BatchCreateResponseEnvelope.CollectionCreateResponseItem> collectionCreateList = new ArrayList<BatchCreateResponseEnvelope.CollectionCreateResponseItem>(list.getResults().size());

      for (CreateKVResponse e : list.getResults())
      {
        if (e == null)
        {
          throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                           "Unexpected null encountered. Null element inside of List inside of a BatchCreateResult returned by the resource method: "
                                               + routingResult.getResourceMethod());
        }
        else
        {
          Object id = ResponseUtils.translateCanonicalKeyToAlternativeKeyIfNeeded(e.getId(), routingResult);
          if (e.getError() == null)
          {
            final ResourceContext resourceContext = routingResult.getContext();
            DataMap entityData = e.getEntity() != null ? e.getEntity().data() : null;
            final DataMap data = RestUtils.projectFields(entityData,
                                                         resourceContext.getProjectionMode(),
                                                         resourceContext.getProjectionMask());

            CreateIdEntityStatus<Object, RecordTemplate> entry = new CreateIdEntityStatus<Object, RecordTemplate>(
                    e.getStatus().getCode(),
                    id,
                    new AnyRecord(data),
                    getLocationUri(request, id, altKey, protocolVersion), // location uri
                    null,
                    protocolVersion);
            collectionCreateList.add(new BatchCreateResponseEnvelope.CollectionCreateResponseItem(entry));

          }
          else
          {
            collectionCreateList.add(new BatchCreateResponseEnvelope.CollectionCreateResponseItem(e.getError(), id));
          }
        }
      }
      RestLiResponseDataImpl responseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK, headers, cookies);
      responseData.setResponseEnvelope(new BatchCreateResponseEnvelope(collectionCreateList, true, responseData));

      return responseData;
    }
    else
    {
      BatchCreateResult<?, ?> list = (BatchCreateResult<?, ?>) result;

      //Verify that a null list was not passed into the BatchCreateResult. If so, this is a developer error.
      if (list.getResults() == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                         "Unexpected null encountered. Null List inside of a BatchCreateResult returned by the resource method: " + routingResult
                                             .getResourceMethod());
      }

      List<BatchCreateResponseEnvelope.CollectionCreateResponseItem> collectionCreateList = new ArrayList<BatchCreateResponseEnvelope.CollectionCreateResponseItem>(list.getResults().size());
      for (CreateResponse e : list.getResults())
      {
        //Verify that a null element was not passed into the BatchCreateResult list. If so, this is a developer error.
        if (e == null)
        {
          throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                           "Unexpected null encountered. Null element inside of List inside of a BatchCreateResult returned by the resource method: "
                                               + routingResult.getResourceMethod());
        }
        else
        {
          Object id = ResponseUtils.translateCanonicalKeyToAlternativeKeyIfNeeded(e.getId(), routingResult);
          if (e.getError() == null)
          {
            CreateIdStatus<Object> entry = new CreateIdStatus<Object>(
                    e.getStatus().getCode(),
                    id,
                    getLocationUri(request, id, altKey, protocolVersion), // location uri
                    null,
                    protocolVersion);
            collectionCreateList.add(new BatchCreateResponseEnvelope.CollectionCreateResponseItem(entry));
          }
          else
          {
            collectionCreateList.add(new BatchCreateResponseEnvelope.CollectionCreateResponseItem(e.getError(), id));
          }
        }
      }

      RestLiResponseDataImpl responseData = new RestLiResponseDataImpl(HttpStatus.S_200_OK, headers, cookies);
      responseData.setResponseEnvelope(new BatchCreateResponseEnvelope(collectionCreateList, responseData));

      return responseData;
    }
  }

  // construct location uri for each created entity id
  private String getLocationUri(RestRequest request, Object id, Object altKey, ProtocolVersion protocolVersion)
  {
    if (id == null)
    {
      // location uri is only set if object key is returned
      return null;
    }
    String stringKey = URIParamUtils.encodeKeyForUri(id, UriComponent.Type.PATH_SEGMENT, protocolVersion);
    UriBuilder uribuilder = UriBuilder.fromUri(request.getURI());
    uribuilder.path(stringKey);
    if (altKey != null)
    {
      // add altkey param to location URI
      uribuilder.queryParam(RestConstants.ALT_KEY_PARAM, altKey);
    }
    return uribuilder.build((Object) null).toString();
  }

}

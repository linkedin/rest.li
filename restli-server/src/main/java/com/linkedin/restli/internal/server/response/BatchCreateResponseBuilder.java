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
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.CreateIdEntityStatus;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.BatchCreateKVResult;
import com.linkedin.restli.server.BatchCreateResult;
import com.linkedin.restli.server.CreateKVResponse;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class BatchCreateResponseBuilder implements RestLiResponseBuilder<RestLiResponseData<BatchCreateResponseEnvelope>>
{
  private final ErrorResponseBuilder _errorResponseBuilder;

  public BatchCreateResponseBuilder(ErrorResponseBuilder errorResponseBuilder)
  {
    _errorResponseBuilder = errorResponseBuilder;
  }

  @Override
  @SuppressWarnings("unchecked")
  public RestLiResponse buildResponse(RoutingResult routingResult, RestLiResponseData<BatchCreateResponseEnvelope> responseData)
  {
    List<BatchCreateResponseEnvelope.CollectionCreateResponseItem> collectionCreateResponses =
                                                responseData.getResponseEnvelope().getCreateResponses();
    List<CreateIdStatus<Object>> formattedResponses = new ArrayList<>(collectionCreateResponses.size());

    // Iterate through the responses and generate the ErrorResponse with the appropriate override for exceptions.
    // Otherwise, add the result as is.
    for (BatchCreateResponseEnvelope.CollectionCreateResponseItem response : collectionCreateResponses)
    {
      if (response.isErrorResponse())
      {
        RestLiServiceException exception = response.getException();
        formattedResponses.add(new CreateIdStatus<>(exception.getStatus().getCode(),
                                                          response.getId(),
                                                          _errorResponseBuilder.buildErrorResponse(exception),
                                                          ProtocolVersionUtil.extractProtocolVersion(responseData.getHeaders())));
      }
      else
      {
        formattedResponses.add((CreateIdStatus<Object>) response.getRecord());
      }
    }

    RestLiResponse.Builder builder = new RestLiResponse.Builder();
    BatchCreateIdResponse<Object> batchCreateIdResponse = new BatchCreateIdResponse<>(formattedResponses);
    return builder.headers(responseData.getHeaders()).cookies(responseData.getCookies()).entity(batchCreateIdResponse).build();
  }

  /**
   * {@inheritDoc}
   *
   * @param result The result for a Rest.li BATCH_CREATE method. It's an instance of {@link BatchCreateResult}, if the
   *               BATCH_CREATE method doesn't return the entity; or an instance of {@link BatchCreateKVResult}, if it
   *               does.
   */
  @Override
  public RestLiResponseData<BatchCreateResponseEnvelope> buildRestLiResponseData(Request request,
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

    final ResourceContext resourceContext = routingResult.getContext();

    if (result instanceof BatchCreateKVResult && resourceContext.isReturnEntityRequested())
    {
      BatchCreateKVResult<?, ?> list = (BatchCreateKVResult<?, ?>) result;
      if (list.getResults() == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                         "Unexpected null encountered. Null List inside of a BatchCreateKVResult returned by the resource method: " + routingResult
                                             .getResourceMethod());
      }
      List<BatchCreateResponseEnvelope.CollectionCreateResponseItem> collectionCreateList = new ArrayList<>(list.getResults().size());

      TimingContextUtil.beginTiming(routingResult.getContext().getRawRequestContext(),
          FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_PROJECTION_APPLY.key());

      for (CreateKVResponse<?, ?> createKVResponse : list.getResults())
      {
        if (createKVResponse == null)
        {
          throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                           "Unexpected null encountered. Null element inside of List inside of a BatchCreateKVResult returned by the resource method: "
                                               + routingResult.getResourceMethod());
        }
        else
        {
          Object id = ResponseUtils.translateCanonicalKeyToAlternativeKeyIfNeeded(createKVResponse.getId(), routingResult);
          if (createKVResponse.getError() == null)
          {
            DataMap entityData = createKVResponse.getEntity() != null ? createKVResponse.getEntity().data() : null;

            final DataMap data = RestUtils.projectFields(entityData,
                                                         resourceContext.getProjectionMode(),
                                                         resourceContext.getProjectionMask());

            CreateIdEntityStatus<Object, RecordTemplate> entry = new CreateIdEntityStatus<>(
                    createKVResponse.getStatus().getCode(),
                    id,
                    new AnyRecord(data),
                    getLocationUri(request, id, altKey, protocolVersion), // location uri
                    null,
                    protocolVersion);
            collectionCreateList.add(new BatchCreateResponseEnvelope.CollectionCreateResponseItem(entry));

          }
          else
          {
            collectionCreateList.add(new BatchCreateResponseEnvelope.CollectionCreateResponseItem(createKVResponse.getError()));
          }
        }
      }

      TimingContextUtil.endTiming(routingResult.getContext().getRawRequestContext(),
          FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_PROJECTION_APPLY.key());

      return new RestLiResponseDataImpl<>(new BatchCreateResponseEnvelope(HttpStatus.S_200_OK, collectionCreateList, true), headers, cookies);
    }
    else
    {
      List<? extends CreateResponse> createResponses = extractCreateResponseList(result);

      //Verify that a null list was not passed into the BatchCreateResult. If so, this is a developer error.
      if (createResponses == null)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                         "Unexpected null encountered. Null List inside of a BatchCreateResult returned by the resource method: " + routingResult
                                             .getResourceMethod());
      }

      List<BatchCreateResponseEnvelope.CollectionCreateResponseItem> collectionCreateList = new ArrayList<>(createResponses.size());
      for (CreateResponse createResponse : createResponses)
      {
        //Verify that a null element was not passed into the BatchCreateResult list. If so, this is a developer error.
        if (createResponse == null)
        {
          throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                           "Unexpected null encountered. Null element inside of List inside of a BatchCreateResult returned by the resource method: "
                                               + routingResult.getResourceMethod());
        }
        else
        {
          Object id = ResponseUtils.translateCanonicalKeyToAlternativeKeyIfNeeded(createResponse.getId(), routingResult);
          if (createResponse.getError() == null)
          {
            CreateIdStatus<Object> entry = new CreateIdStatus<>(
                    createResponse.getStatus().getCode(),
                    id,
                    getLocationUri(request, id, altKey, protocolVersion), // location uri
                    null,
                    protocolVersion);
            collectionCreateList.add(new BatchCreateResponseEnvelope.CollectionCreateResponseItem(entry));
          }
          else
          {
            collectionCreateList.add(new BatchCreateResponseEnvelope.CollectionCreateResponseItem(createResponse.getError()));
          }
        }
      }

      return new RestLiResponseDataImpl<>(new BatchCreateResponseEnvelope(HttpStatus.S_200_OK, collectionCreateList, false), headers, cookies);
    }
  }

  // construct location uri for each created entity id
  private String getLocationUri(Request request, Object id, Object altKey, ProtocolVersion protocolVersion)
  {
    if (id == null)
    {
      // location uri is only set if object key is returned
      return null;
    }
    String stringKey = URIParamUtils.encodeKeyForUri(id, UriComponent.Type.PATH_SEGMENT, protocolVersion);
    UriBuilder uribuilder = UriBuilder.fromUri(request.getURI());
    uribuilder.path(stringKey);
    uribuilder.replaceQuery(null);
    if (altKey != null)
    {
      // add altkey param to location URI
      uribuilder.queryParam(RestConstants.ALT_KEY_PARAM, altKey);
    }
    return uribuilder.build((Object) null).toString();
  }

  /**
   * Extracts a list of {@link CreateResponse} objects from the given object. This helper method is needed
   * because {@link BatchCreateResult} and {@link BatchCreateKVResult} do not share a common superclass or interface.
   *
   * @param result object of type {@link BatchCreateResult} or {@link BatchCreateKVResult}.
   * @return list of objects extending {@link CreateResponse} extracted from the parameter object.
   */
  private List<? extends CreateResponse> extractCreateResponseList(Object result)
  {
    if (result instanceof BatchCreateKVResult)
    {
      return ((BatchCreateKVResult<?, ?>) result).getResults();
    }
    else if (result instanceof BatchCreateResult)
    {
      return ((BatchCreateResult<?, ?>) result).getResults();
    }
    else
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
          "BatchCreateResponseBuilder expects input of type BatchCreateResult or BatchCreateKVResult. Encountered type: " + result.getClass().getName());
    }
  }
}

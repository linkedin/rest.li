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


package com.linkedin.restli.internal.server;

import com.linkedin.data.DataMap;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.server.methods.MethodAdapterRegistry;
import com.linkedin.restli.internal.server.methods.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.methods.response.PartialRestResponse;
import com.linkedin.restli.internal.server.methods.response.RestLiResponseBuilder;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.resources.CollectionResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;


/**
 * Interprets the method response to generate a {@link RestResponse}. Per methods on
 * {@link CollectionResource}, response can be any of the following:
 *
 * <ul>
 * <li> V extends RecordTemplate  - get response, custom response
 * <li> Map&lt;K, RecordTemplate&gt; - batch get response
 * <li> List&lt;RecordTemplate&gt; - collection response (no total)
 * <li> {@link CollectionResult} - collection response (includes total)
 * <li> {@link CreateResponse} - create response
 * <li> {@link UpdateResponse} - update response
 * <li> {@link ActionResponse} - action response
 * </ul>
 *
 * @author dellamag
 * @author nshankar
 */
public class RestLiResponseHandler
{
  private final MethodAdapterRegistry _methodAdapterRegistry;
  private final ErrorResponseBuilder _errorResponseBuilder;

  public RestLiResponseHandler(MethodAdapterRegistry methodAdapterRegistry, ErrorResponseBuilder errorResponseBuilder)
  {
    _methodAdapterRegistry = methodAdapterRegistry;
    _errorResponseBuilder = errorResponseBuilder;
  }

  public static class Builder
  {
    private MethodAdapterRegistry _methodAdapterRegistry = null;
    private ErrorResponseBuilder _errorResponseBuilder = null;
    private boolean _permissiveEncoding = false;

    public Builder setMethodAdapterRegistry(MethodAdapterRegistry methodAdapterRegistry)
    {
      _methodAdapterRegistry = methodAdapterRegistry;
      return this;
    }

    public Builder setErrorResponseBuilder(ErrorResponseBuilder errorResponseBuilder)
    {
      _errorResponseBuilder = errorResponseBuilder;
      return this;
    }

    public RestLiResponseHandler build()
    {
      if (_errorResponseBuilder == null)
      {
        _errorResponseBuilder = new ErrorResponseBuilder();
      }
      if (_methodAdapterRegistry == null)
      {
        _methodAdapterRegistry = new MethodAdapterRegistry(_errorResponseBuilder);
      }
      return new RestLiResponseHandler(_methodAdapterRegistry, _errorResponseBuilder);
    }
  }

  /**
   * Build a RestResponse from response object, incoming RestRequest and RoutingResult.
   *
   * TODO: Can zap this method since we have the other two methods.
   *
   * @param request
   *          {@link RestRequest}
   * @param routingResult
   *          {@link RoutingResult}
   * @param responseObject
   *          response value
   * @return {@link RestResponse}
   * @throws IOException
   *           if cannot build response
   */
  public RestResponse buildResponse(final RestRequest request,
                                    final RoutingResult routingResult,
                                    final Object responseObject) throws IOException
  {
    return buildResponse(routingResult,
                         buildPartialResponse(routingResult,
                                              buildRestLiResponseData(request, routingResult, responseObject)));
  }


  /**
   * Build a RestResponse from PartialRestResponse and RoutingResult.
   *
   * @param routingResult
   *          {@link RoutingResult}
   * @param partialResponse
   *          {@link PartialRestResponse}
   * @return
   */
  public RestResponse buildResponse(final RoutingResult routingResult,
                                     PartialRestResponse partialResponse)
  {
    RestResponseBuilder builder =
        new RestResponseBuilder().setHeaders(partialResponse.getHeaders()).setStatus(partialResponse.getStatus()
                                                                                                    .getCode());
    if (partialResponse.hasData())
    {
      DataMap dataMap = partialResponse.getDataMap();
      String mimeType = ((ServerResourceContext) routingResult.getContext()).getResponseMimeType();
      builder = encodeResult(mimeType, builder, dataMap);
    }
    return builder.build();
  }

  /**
   * Build a ParialRestResponse from RestLiResponseDataInternal and RoutingResult.
   *
   * @param routingResult
   *          {@link RoutingResult}
   * @param responseData
   *          response value
   * @return {@link PartialRestResponse}
   * @throws IOException
   *           if cannot build response
   */
  public PartialRestResponse buildPartialResponse(final RoutingResult routingResult,
                                                  final AugmentedRestLiResponseData responseData)
  {
    if (responseData.isErrorResponse()){
      return _errorResponseBuilder.buildResponse(routingResult, responseData);
    }
    return chooseResponseBuilder(null, routingResult).buildResponse(routingResult, responseData);
  }

  /**
   * Build a RestLiResponseDataInternal from response object, incoming RestRequest and RoutingResult.
   *
   * @param request
   *          {@link RestRequest}
   * @param routingResult
   *          {@link RoutingResult}
   * @param responseObject
   *          response value
   * @return {@link AugmentedRestLiResponseData}
   * @throws IOException
   *           if cannot build response
   */
  public AugmentedRestLiResponseData buildRestLiResponseData(final RestRequest request,
                                                            final RoutingResult routingResult,
                                                            final Object responseObject) throws IOException
  {
    ServerResourceContext context = (ServerResourceContext) routingResult.getContext();
    final ProtocolVersion protocolVersion = context.getRestliProtocolVersion();
    Map<String, String> responseHeaders = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    responseHeaders.putAll(context.getResponseHeaders());
    responseHeaders.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString());

    if (responseObject == null)
    {
      //If we have a null result, we have to assign the correct response status
      if (routingResult.getResourceMethod().getType().equals(ResourceMethod.ACTION))
      {
        return new AugmentedRestLiResponseData.Builder(routingResult.getResourceMethod().getMethodType())
            .status(HttpStatus.S_200_OK).headers(responseHeaders).build();
      }
      else if (routingResult.getResourceMethod().getType().equals(ResourceMethod.GET))
      {
        throw new RestLiServiceException(HttpStatus.S_404_NOT_FOUND,
            "Requested entity not found: " + routingResult.getResourceMethod());
      }
      else
      {
        //All other cases do not permit null to be returned
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null returned by the resource method: " + routingResult.getResourceMethod());
      }
    }

    RestLiResponseBuilder responseBuilder = chooseResponseBuilder(responseObject, routingResult);

    if (responseBuilder == null)
    {
      // this should not happen if valid return types are specified
      ResourceMethodDescriptor resourceMethod = routingResult.getResourceMethod();
      String fqMethodName =
          resourceMethod.getResourceModel().getResourceClass().getName() + '#'
              + routingResult.getResourceMethod().getMethod().getName();
      throw new RestLiInternalException("Invalid return type '" + responseObject.getClass() + " from method '"
          + fqMethodName + '\'');
    }
    return responseBuilder.buildRestLiResponseData(request, routingResult, responseObject, responseHeaders);
  }

  public AugmentedRestLiResponseData buildExceptionResponseData(final RestRequest request,
                                                                final RoutingResult routingResult,
                                                                final Object object,
                                                                final Map<String, String> headers)
  {
    return _errorResponseBuilder.buildRestLiResponseData(request, routingResult, object, headers);
  }

  public RestException buildRestException(final Throwable e, PartialRestResponse partialResponse)
  {
    RestResponseBuilder builder =
        new RestResponseBuilder().setHeaders(partialResponse.getHeaders()).setStatus(partialResponse.getStatus()
                                                                                                    .getCode());
    if (partialResponse.hasData())
    {
      DataMap dataMap = partialResponse.getDataMap();
      ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
      DataMapUtils.write(dataMap, null, baos, true); // partialResponse.getSchema()
      builder.setEntity(baos.toByteArray());
    }
    RestResponse restResponse = builder.build();
    RestException restException = new RestException(restResponse, e);
    return restException;
  }


  private RestResponseBuilder encodeResult(String mimeType, RestResponseBuilder builder, DataMap dataMap)
  {
    if (RestConstants.HEADER_VALUE_APPLICATION_PSON.equalsIgnoreCase(mimeType))
    {
      builder.setHeader(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_PSON);
      builder.setEntity(DataMapUtils.mapToPsonBytes(dataMap));
    }
    else if (RestConstants.HEADER_VALUE_APPLICATION_JSON.equalsIgnoreCase(mimeType))
    {
      builder.setHeader(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_JSON);
      builder.setEntity(DataMapUtils.mapToBytes(dataMap));
    }
    else
    {
      throw new RoutingException("No acceptable types can be returned", HttpStatus.S_406_NOT_ACCEPTABLE.getCode());
    }
    return builder;
  }

  private RestLiResponseBuilder chooseResponseBuilder(final Object responseObject,
                                                      final RoutingResult routingResult)
  {
    if (responseObject != null && responseObject instanceof RestLiServiceException)
    {
      return _errorResponseBuilder;
    }

    return _methodAdapterRegistry.getResponsebuilder(routingResult.getResourceMethod()
                                                                 .getType());
  }
}

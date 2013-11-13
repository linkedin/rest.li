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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.linkedin.data.DataMap;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.restli.common.ActionResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.methods.MethodAdapterRegistry;
import com.linkedin.restli.internal.server.methods.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.methods.response.PartialRestResponse;
import com.linkedin.restli.internal.server.methods.response.RestLiResponseBuilder;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.CreateResponse;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.UpdateResponse;
import com.linkedin.restli.server.resources.CollectionResource;

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
 */
public class RestLiResponseHandler
{
  private final MethodAdapterRegistry _methodAdapterRegistry;
  private final ErrorResponseBuilder _errorResponseBuilder;
  private final boolean  _permissiveEncoding;

  public RestLiResponseHandler(MethodAdapterRegistry methodAdapterRegistry, ErrorResponseBuilder errorResponseBuilder, boolean permissiveEncoding)
  {
    _methodAdapterRegistry = methodAdapterRegistry;
    _errorResponseBuilder = errorResponseBuilder;
    _permissiveEncoding = permissiveEncoding;
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

    public Builder setPermissiveEncoding(boolean permissiveEncoding)
    {
      _permissiveEncoding = permissiveEncoding;
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
      return new RestLiResponseHandler(_methodAdapterRegistry, _errorResponseBuilder, _permissiveEncoding);
    }
  }
  /**
   * @param request {@link RestRequest}
   * @param routingResult {@link RoutingResult}
   * @param responseObject response value
   * @return {@link RestResponse}
   * @throws IOException if cannot build response
   */
  public RestResponse buildResponse(final RestRequest request,
                                    final RoutingResult routingResult,
                                    final Object responseObject) throws IOException
  {
    Map<String, String> headers = new HashMap<String, String>();
    headers.putAll(((ServerResourceContext) routingResult.getContext()).getResponseHeaders());

    if (responseObject == null)
    {
      boolean isAction =
          routingResult.getResourceMethod().getType().equals(ResourceMethod.ACTION);
      RestResponseBuilder builder = new RestResponseBuilder();
      builder.setStatus(isAction ? HttpStatus.S_200_OK.getCode()
          : HttpStatus.S_404_NOT_FOUND.getCode());
      builder.setHeaders(headers);
      if (!isAction)
      {
        builder.setHeader(RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE,
                          RestConstants.HEADER_VALUE_ERROR_APPLICATION);
      }
      return builder.build();
    }

    RestLiResponseBuilder responseBuilder = chooseResponseBuilder(responseObject, routingResult);

    if (responseBuilder == null)
    {
      // this should not happen if valid return types are specified
      ResourceMethodDescriptor resourceMethod = routingResult.getResourceMethod();
      String fqMethodName = resourceMethod.getResourceModel().getResourceClass().getName() + '#' +
                            routingResult.getResourceMethod().getMethod().getName();
      throw new RestLiInternalException("Invalid return type '" + responseObject.getClass() + " from method '" +
                                        fqMethodName + '\'');
    }

    PartialRestResponse partialResponse =
        responseBuilder.buildResponse(request, routingResult, responseObject, headers);


    RestResponseBuilder builder =
        new RestResponseBuilder().setHeaders(headers)
                                 .setStatus(partialResponse.getStatus().getCode());

    if (partialResponse.hasData())
    {
      DataMap dataMap = partialResponse.getDataMap();
      String acceptTypes = request.getHeader(RestConstants.HEADER_ACCEPT);
      builder = encodeResult(builder, dataMap, acceptTypes);
    }

    return builder.build();
  }

  public PartialRestResponse buildErrorResponse(final RestRequest request,
                                 final RoutingResult routingResult,
                                 final Object object,
                                 final Map<String, String> headers)
  {
    return _errorResponseBuilder.buildResponse(request, routingResult, object, headers);
  }

  private RestResponseBuilder encodeResult(RestResponseBuilder builder, DataMap dataMap, String acceptTypes)
  {
    String bestType = RestUtils.pickBestEncoding(acceptTypes);

    if (RestConstants.HEADER_VALUE_APPLICATION_PSON.equalsIgnoreCase(bestType))
    {
      builder.setHeader(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_PSON);
      builder.setEntity(DataMapUtils.mapToPsonBytes(dataMap));
    }
    else if (RestConstants.HEADER_VALUE_APPLICATION_JSON.equalsIgnoreCase(bestType))
    {
      builder.setHeader(RestConstants.HEADER_CONTENT_TYPE, RestConstants.HEADER_VALUE_APPLICATION_JSON);
      builder.setEntity(DataMapUtils.mapToBytes(dataMap, _permissiveEncoding));
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
    if (responseObject instanceof RestLiServiceException)
    {
      return _errorResponseBuilder;
    }

    return _methodAdapterRegistry.getResponsebuilder(routingResult.getResourceMethod()
                                                                 .getType());
  }
}
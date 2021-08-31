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
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.methods.DefaultMethodAdapterRegistry;
import com.linkedin.restli.internal.server.methods.MethodAdapterRegistry;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.restspec.ResourceEntityType;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * A Rest.li response is built in three steps.
 * <ol>
 *   <li>Build a {@link RestLiResponseData} from the result object returned by the server application resource
 *   implementation. The <code>RestLiResponseData</code> object is then sent through the response filter chain.</li>
 *   <li>Build a {@link RestLiResponse} from the <code>RestLiResponseData</code></li> after it has been processed
 *   by the Rest.li filters.
 *   <li>Build a {@link com.linkedin.r2.message.rest.RestResponse} or {@link com.linkedin.r2.message.stream.StreamResponse}
 *   from the <code>RestLiResponse</code>.</li>
 * </ol>
 *
 * <code>RestLiResponseHandler</code> uses appropriate {@link RestLiResponseBuilder} implementation to execute the
 * first two steps.
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

  /**
   * Build a RestResponse from PartialRestResponse and RoutingResult.
   *
   * @param routingResult
   *          {@link RoutingResult}
   * @param restLiResponse
   *          {@link RestLiResponse}
   *
   * @deprecated Internal to Rest.li implementation. Use {@link ResponseUtils#buildResponse(RoutingResult, RestLiResponse)}.
   */
  @Deprecated
  public RestResponse buildResponse(final RoutingResult routingResult,
                                    RestLiResponse restLiResponse)
  {
    return ResponseUtils.buildResponse(routingResult, restLiResponse);
  }

  /**
   * Executes {@linkplain RestLiResponseHandler the second step} of building the response.
   */
  public <D extends RestLiResponseData<?>> RestLiResponse buildPartialResponse(final RoutingResult routingResult,
                                                                                    final D responseData)
  {
    if (responseData.getResponseEnvelope().isErrorResponse())
    {
      return _errorResponseBuilder.buildResponse(responseData);
    }

    // The resource method in the routingResult must agree with that of responseData.
    @SuppressWarnings("unchecked")
    RestLiResponseBuilder<D> responseBuilder = (RestLiResponseBuilder<D>) _methodAdapterRegistry.getResponseBuilder(
        routingResult.getResourceMethod().getType());
    RestLiResponse restLiResponse = responseBuilder.buildResponse(routingResult, responseData);
    injectResponseMetadata(restLiResponse.getEntity(), responseData.getResponseEnvelope().getResponseMetadata());
    return restLiResponse;
  }

  private void injectResponseMetadata(RecordTemplate entity, DataMap responseMetadata) {
    // Inject the metadata map into the response entity if they both exist
    if (entity != null) {
      DataMap rawEntityData = entity.data();
      if (rawEntityData != null) {
        if (responseMetadata != null && responseMetadata.size() > 0) {
          rawEntityData.put(RestConstants.METADATA_RESERVED_FIELD, responseMetadata);
        }
      }
    }
  }

  /**
   * Executes {@linkplain RestLiResponseHandler the first step} of building the response.
   */
  public RestLiResponseData<?> buildRestLiResponseData(final Request request,
                                                       final RoutingResult routingResult,
                                                       final Object responseObject) throws IOException
  {
    ServerResourceContext context = routingResult.getContext();
    final ProtocolVersion protocolVersion = context.getRestliProtocolVersion();
    Map<String, String> responseHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    responseHeaders.putAll(context.getResponseHeaders());
    responseHeaders.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString());
    List<HttpCookie> responseCookies = context.getResponseCookies();


    if (responseObject == null)
    {
      //If we have a null result, we have to assign the correct response status
      if (routingResult.getResourceMethod().getType().equals(ResourceMethod.ACTION))
      {
        return new RestLiResponseDataImpl<>(new ActionResponseEnvelope(HttpStatus.S_200_OK, null), responseHeaders, responseCookies);
      }
      else if (routingResult.getResourceMethod().getType().equals(ResourceMethod.GET))
      {
        ResourceEntityType resourceEntityType = routingResult.getResourceMethod()
                                                             .getResourceModel()
                                                             .getResourceEntityType();
        if (ResourceEntityType.UNSTRUCTURED_DATA == resourceEntityType)
        {
          // TODO: A dummy empty record is used here to avoid NPE where a record is expected for GET, need a better fix.
          return new RestLiResponseDataImpl<>(
            new GetResponseEnvelope(HttpStatus.S_200_OK, new EmptyRecord()), responseHeaders, responseCookies
          );
        }
        else
        {
          throw new RestLiServiceException(HttpStatus.S_404_NOT_FOUND,
                                           "Requested entity not found: " + routingResult.getResourceMethod());
        }
      }
      else
      {
        //All other cases do not permit null to be returned
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
            "Unexpected null encountered. Null returned by the resource method: " + routingResult.getResourceMethod());
      }
    }

    if (responseObject instanceof RestLiServiceException)
    {
      return _errorResponseBuilder.buildRestLiResponseData(routingResult, (RestLiServiceException) responseObject, responseHeaders, responseCookies);
    }

    RestLiResponseBuilder<?> responseBuilder = _methodAdapterRegistry.getResponseBuilder(
        routingResult.getResourceMethod().getType());

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
    return responseBuilder.buildRestLiResponseData(request, routingResult, responseObject, responseHeaders, responseCookies);
  }

  public RestLiResponseData<?> buildExceptionResponseData(final RoutingResult routingResult,
      final RestLiServiceException exception,
      final Map<String, String> headers,
      final List<HttpCookie> cookies)
  {
    return _errorResponseBuilder.buildRestLiResponseData(routingResult, exception, headers, cookies);
  }
}

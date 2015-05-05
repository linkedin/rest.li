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

package com.linkedin.restli.internal.server.methods.response;


import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.ErrorDetails;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.response.BatchResponseEnvelope;
import com.linkedin.restli.internal.server.response.CreateCollectionResponseEnvelope;
import com.linkedin.restli.internal.server.response.CollectionResponseEnvelope;
import com.linkedin.restli.internal.server.response.EmptyResponseEnvelope;
import com.linkedin.restli.internal.server.response.RecordResponseEnvelope;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.server.ErrorResponseFormat;
import com.linkedin.restli.server.RestLiServiceException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;


/**
 * @author Josh Walker
 * @version $Revision: $
 */
public final class ErrorResponseBuilder implements RestLiResponseBuilder
{
  public static final String DEFAULT_INTERNAL_ERROR_MESSAGE = "Error in application code";
  private final ErrorResponseFormat _errorResponseFormat;
  private final String _internalErrorMessage;

  public ErrorResponseBuilder()
  {
    this(ErrorResponseFormat.defaultFormat());
  }

  public ErrorResponseBuilder(ErrorResponseFormat errorResponseFormat)
  {
    this(errorResponseFormat, DEFAULT_INTERNAL_ERROR_MESSAGE);
  }

  public ErrorResponseBuilder(ErrorResponseFormat errorResponseFormat, String internalErrorMessage)
  {
    _errorResponseFormat = errorResponseFormat;
    _internalErrorMessage = internalErrorMessage;
  }

  public String getInternalErrorMessage()
  {
    return _internalErrorMessage;
  }

  public ErrorResponse buildErrorResponse(RestLiServiceException result)
  {
    return buildErrorResponse(result, result.hasOverridingErrorResponseFormat() ? result.getOverridingFormat() : _errorResponseFormat);
  }

  private ErrorResponse buildErrorResponse(RestLiServiceException result, ErrorResponseFormat errorResponseFormat)
  {
    ErrorResponse er = new ErrorResponse();
    if (errorResponseFormat.showStatusCodeInBody())
    {
      er.setStatus(result.getStatus().getCode());
    }

    if (errorResponseFormat.showMessage() && result.getMessage() != null)
    {
      er.setMessage(result.getMessage());
    }
    if (errorResponseFormat.showServiceErrorCode() && result.hasServiceErrorCode())
    {
      er.setServiceErrorCode(result.getServiceErrorCode());
    }
    if (errorResponseFormat.showDetails() && result.hasErrorDetails())
    {
      er.setErrorDetails(new ErrorDetails(result.getErrorDetails()));
    }

    if (errorResponseFormat.showStacktrace())
    {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      result.printStackTrace(pw);
      er.setStackTrace(sw.toString());

      er.setExceptionClass(result.getClass().getName());
    }

    return er;
  }

  @Override
  public PartialRestResponse buildResponse(RoutingResult routingResult, RestLiResponseEnvelope responseData)
  {
    ErrorResponse errorResponse = buildErrorResponse(responseData.getServiceException());
    return new PartialRestResponse.Builder().headers(responseData.getHeaders()).status(responseData.getStatus())
                                            .entity(errorResponse).build();
  }

  @Override
  public RestLiResponseEnvelope buildRestLiResponseData(RestRequest request, RoutingResult routingResult,
                                                             Object object, Map<String, String> headers)
  {
    RestLiServiceException exceptionResult = (RestLiServiceException) object;
    if (_errorResponseFormat.showHeaders())
    {
      final ProtocolVersion protocolVersion = ProtocolVersionUtil.extractProtocolVersion(headers);
      headers.put(HeaderUtil.getErrorResponseHeaderName(protocolVersion), RestConstants.HEADER_VALUE_ERROR);
    }
    final ResourceMethod type;
    if (routingResult != null && routingResult.getResourceMethod() != null)
    {
      type = routingResult.getResourceMethod().getMethodType();
    }
    else
    {
      type = null;
    }

    switch (ResponseType.fromMethodType(type))
    {
      case SINGLE_ENTITY:
        return new RecordResponseEnvelope(exceptionResult, headers);
      case GET_COLLECTION:
        return new CollectionResponseEnvelope(exceptionResult, headers);
      case CREATE_COLLECTION:
        return new CreateCollectionResponseEnvelope(exceptionResult, headers);
      case BATCH_ENTITIES:
        return new BatchResponseEnvelope(exceptionResult, headers);
      case STATUS_ONLY:
        return new EmptyResponseEnvelope(exceptionResult, headers);
      default:
        throw new IllegalArgumentException();
    }
  }
}

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

import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ErrorDetails;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.server.ErrorResponseFormat;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;


/**
 * @author Josh Walker
 * @version $Revision: $
 */
public final class ErrorResponseBuilder
{
  public static final String DEFAULT_INTERNAL_ERROR_MESSAGE = "INTERNAL SERVER ERROR";
  private final ErrorResponseFormat _errorResponseFormat;

  public ErrorResponseBuilder()
  {
    this(ErrorResponseFormat.defaultFormat());
  }

  public ErrorResponseBuilder(ErrorResponseFormat errorResponseFormat)
  {
    _errorResponseFormat = errorResponseFormat;
  }

  /**
   * @deprecated internalErrorMessage is ignored. Use {@link #ErrorResponseBuilder(ErrorResponseFormat)}.
   */
  @Deprecated
  public ErrorResponseBuilder(ErrorResponseFormat errorResponseFormat, String internalErrorMessage)
  {
    _errorResponseFormat = errorResponseFormat;
  }

  public ErrorResponseFormat getErrorResponseFormat()
  {
    return _errorResponseFormat;
  }

  public ErrorResponse buildErrorResponse(RestLiServiceException result)
  {
    return buildErrorResponse(result, result.hasOverridingErrorResponseFormat() ? result.getOverridingFormat() : _errorResponseFormat);
  }

  @SuppressWarnings("deprecation")
  private ErrorResponse buildErrorResponse(RestLiServiceException result, ErrorResponseFormat errorResponseFormat)
  {
    ErrorResponse er = new ErrorResponse();

    if (errorResponseFormat.showStatusCodeInBody())
    {
      er.setStatus(result.getStatus().getCode());
    }

    if (errorResponseFormat.showServiceErrorCode())
    {
      if (result.hasCode())
      {
        er.setCode(result.getCode());
      }
      // TODO: eventually only add "code" and not "serviceErrorCode"
      if (result.hasServiceErrorCode())
      {
        er.setServiceErrorCode(result.getServiceErrorCode());
      }
    }

    if (errorResponseFormat.showMessage() && result.getMessage() != null)
    {
      er.setMessage(result.getMessage());
    }

    if (errorResponseFormat.showDocUrl() && result.hasDocUrl())
    {
      er.setDocUrl(result.getDocUrl());
    }

    if (errorResponseFormat.showRequestId() && result.hasRequestId())
    {
      er.setRequestId(result.getRequestId());
    }

    if (errorResponseFormat.showStacktrace())
    {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      result.printStackTrace(pw);
      er.setStackTrace(sw.toString());
    }

    if (errorResponseFormat.showStacktrace() || errorResponseFormat.showExceptionClass())
    {
      er.setExceptionClass(result.getClass().getName());
    }

    if (errorResponseFormat.showDetails() && result.hasErrorDetails())
    {
      er.setErrorDetails(new ErrorDetails(result.getErrorDetails()));
      final String errorDetailType = result.getErrorDetailType();
      if (errorDetailType != null)
      {
        er.setErrorDetailType(errorDetailType);
      }
    }

    return er;
  }

  public RestLiResponse buildResponse(RestLiResponseData<?> responseData)
  {
    ErrorResponse errorResponse = buildErrorResponse(responseData.getResponseEnvelope().getException());
    return new RestLiResponse.Builder()
        .headers(responseData.getHeaders())
        .cookies(responseData.getCookies())
        .status(responseData.getResponseEnvelope().getStatus())
        .entity(errorResponse).build();
  }

  public RestLiResponseData<?> buildRestLiResponseData(RoutingResult routingResult,
      RestLiServiceException exceptionResult,
      Map<String, String> headers,
      List<HttpCookie> cookies)
  {
    assert routingResult != null && routingResult.getResourceMethod() != null;

    if (_errorResponseFormat.showHeaders())
    {
      final ProtocolVersion protocolVersion = ProtocolVersionUtil.extractProtocolVersion(headers);
      headers.put(HeaderUtil.getErrorResponseHeaderName(protocolVersion), RestConstants.HEADER_VALUE_ERROR);
    }

    ResourceMethod type = routingResult.getResourceMethod().getMethodType();
    return buildErrorResponseData(type, exceptionResult, headers, cookies);
  }

  static RestLiResponseData<?> buildErrorResponseData(ResourceMethod method,
      RestLiServiceException exception,
      Map<String, String> headers,
      List<HttpCookie> cookies)
  {
    switch (method)
    {
      case GET:
        return new RestLiResponseDataImpl<>(new GetResponseEnvelope(exception), headers, cookies);
      case CREATE:
        return new RestLiResponseDataImpl<>(new CreateResponseEnvelope(exception, false), headers, cookies);
      case ACTION:
        return new RestLiResponseDataImpl<>(new ActionResponseEnvelope(exception), headers, cookies);
      case GET_ALL:
        return new RestLiResponseDataImpl<>(new GetAllResponseEnvelope(exception), headers, cookies);
      case FINDER:
        return new RestLiResponseDataImpl<>(new FinderResponseEnvelope(exception), headers, cookies);
      case BATCH_FINDER:
        return new RestLiResponseDataImpl<>(new BatchFinderResponseEnvelope(exception), headers, cookies);
      case BATCH_CREATE:
        return new RestLiResponseDataImpl<>(new BatchCreateResponseEnvelope(exception, false), headers, cookies);
      case BATCH_GET:
        return new RestLiResponseDataImpl<>(new BatchGetResponseEnvelope(exception), headers, cookies);
      case BATCH_UPDATE:
        return new RestLiResponseDataImpl<>(new BatchUpdateResponseEnvelope(exception), headers, cookies);
      case BATCH_PARTIAL_UPDATE:
        return new RestLiResponseDataImpl<>(new BatchPartialUpdateResponseEnvelope(exception), headers, cookies);
      case BATCH_DELETE:
        return new RestLiResponseDataImpl<>(new BatchDeleteResponseEnvelope(exception), headers, cookies);
      case PARTIAL_UPDATE:
        return new RestLiResponseDataImpl<>(new PartialUpdateResponseEnvelope(exception), headers, cookies);
      case UPDATE:
        return new RestLiResponseDataImpl<>(new UpdateResponseEnvelope(exception), headers, cookies);
      case DELETE:
        return new RestLiResponseDataImpl<>(new DeleteResponseEnvelope(exception), headers, cookies);
      case OPTIONS:
        return new RestLiResponseDataImpl<>(new OptionsResponseEnvelope(exception), headers, cookies);
      default:
        throw new IllegalArgumentException("Unexpected Rest.li resource method: " + method);
    }
  }
}

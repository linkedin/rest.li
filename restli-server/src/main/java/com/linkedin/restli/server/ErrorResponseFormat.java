/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.server;


import java.util.EnumSet;


/**
 * Provides settings for how error responses are formatted.
 *
 * jbetz@linkedin.com
 */
public class ErrorResponseFormat
{
  /**
   * All available error information is included in responses, including server side stack trace.
   */
  public static final ErrorResponseFormat FULL = new ErrorResponseFormat(EnumSet.allOf(ErrorResponsePart.class));

  /**
   * Only the error message and explicitly provided error details or service error code are included in responses and headers.
   */
  public static final ErrorResponseFormat MESSAGE_AND_DETAILS = new ErrorResponseFormat(EnumSet.of(ErrorResponsePart.MESSAGE,
                                                                                        ErrorResponsePart.DETAILS,
                                                                                        ErrorResponsePart.HEADERS));

  /**
   * Only the status code, error message, service error code, and headers.
   */
  public static final ErrorResponseFormat MESSAGE_AND_SERVICECODE = new ErrorResponseFormat(EnumSet.of(ErrorResponsePart.STATUS_CODE_IN_BODY,
                                                                                    ErrorResponsePart.MESSAGE,
                                                                                    ErrorResponsePart.SERVICE_ERROR_CODE,
                                                                                    ErrorResponsePart.HEADERS));

  /**
   * Only the status code, error message, service error code, exception class, and headers.
   */
  public static final ErrorResponseFormat MESSAGE_AND_SERVICECODE_AND_EXCEPTIONCLASS = new ErrorResponseFormat(EnumSet.of(ErrorResponsePart.STATUS_CODE_IN_BODY,
                                                                                                               ErrorResponsePart.MESSAGE,
                                                                                                               ErrorResponsePart.SERVICE_ERROR_CODE,
                                                                                                               ErrorResponsePart.EXCEPTION_CLASS,
                                                                                                               ErrorResponsePart.HEADERS));

  /**
   * Only the error message and headers.
   */
  public static final ErrorResponseFormat MESSAGE_ONLY = new ErrorResponseFormat(EnumSet.of(ErrorResponsePart.MESSAGE, ErrorResponsePart.HEADERS));

  /**
   * Clients only get back a HTTP Status code and {@link com.linkedin.restli.common.RestConstants#HEADER_RESTLI_ERROR_RESPONSE }
   * (or the deprecated {@link com.linkedin.restli.common.RestConstants#HEADER_LINKEDIN_ERROR_RESPONSE }) header, and nothing else.
   */
  public static final ErrorResponseFormat MINIMAL = new ErrorResponseFormat(EnumSet.of(ErrorResponsePart.HEADERS));

  public enum ErrorResponsePart
  {
    HEADERS,
    STATUS_CODE_IN_BODY,
    MESSAGE,
    DOC_URL,
    REQUEST_ID,
    EXCEPTION_CLASS,
    STACKTRACE,
    DETAILS,
    SERVICE_ERROR_CODE
  }

  private final EnumSet<ErrorResponsePart> _errorPartsToShow;

  /**
   *  Use this constructor to create a custom error response format using a set of
   *  ErrorResponseParts as argument.
   *  Consider using one of the predefined types. Use this constructor only if you have special
   *  formatting requires not satisfied by existing formatters. */
  public ErrorResponseFormat(EnumSet<ErrorResponsePart> errorPartsToShow)
  {
    _errorPartsToShow = errorPartsToShow;
  }

  public static ErrorResponseFormat defaultFormat()
  {
    return FULL;
  }

  public boolean showHeaders()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.HEADERS);
  }

  public boolean showStatusCodeInBody()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.STATUS_CODE_IN_BODY);
  }

  public boolean showMessage()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.MESSAGE);
  }

  public boolean showDocUrl()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.DOC_URL);
  }

  public boolean showRequestId()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.REQUEST_ID);
  }

  public boolean showExceptionClass()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.EXCEPTION_CLASS);
  }

  public boolean showStacktrace()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.STACKTRACE);
  }

  public boolean showDetails()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.DETAILS);
  }

  public boolean showServiceErrorCode()
  {
    return _errorPartsToShow.contains(ErrorResponsePart.SERVICE_ERROR_CODE);
  }
}

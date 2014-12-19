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

package com.linkedin.restli.client;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.GetMode;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.internal.common.HeaderUtil;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

/**
 * RestLiResponseException is thrown when the client receives a response with a non-success
 * HttpStatus (<200 or >=300).  If the response body includes an ErrorResponse document, it will
 * be parsed, and its contents may be accessed via the accessor methods in {@link RestLiResponseException}.
 * It also contains a decoded {@link Response} for the raw {@link RestResponse} when the raw
 * {@link RestResponse} does not contain an error response.
 */
public class RestLiResponseException extends RestException
{
  private static final long serialVersionUID = 1;

  private final int _status;
  private final ErrorResponse _errorResponse;
  private final Response<?> _decodedResponse;

  /**
   * @deprecated Please use {@link #RestLiResponseException(com.linkedin.r2.message.rest.RestResponse, Response, com.linkedin.restli.common.ErrorResponse)}
   * @param rawResponse
   * @param errorResponse
   */
  @Deprecated
  public RestLiResponseException(RestResponse rawResponse, ErrorResponse errorResponse)
  {
    super(rawResponse);
    _status = rawResponse.getStatus();
    _errorResponse = errorResponse;
    _decodedResponse = null;
  }

  public RestLiResponseException(RestResponse rawResponse,
                                 Response<?> decodedResponse,
                                 ErrorResponse errorResponse)
  {
    super(rawResponse);
    _status = rawResponse.getStatus();
    _errorResponse = errorResponse;
    _decodedResponse = decodedResponse;
  }

  /**
   * @deprecated Please use {@link #RestLiResponseException(com.linkedin.r2.message.rest.RestResponse, Response, com.linkedin.restli.common.ErrorResponse)}
   * @param rawResponse
   * @param errorResponse
   * @param cause
   */
  @Deprecated
  public RestLiResponseException(RestResponse rawResponse, ErrorResponse errorResponse,
                                 Throwable cause)
  {
    super(rawResponse, cause);
    _status = rawResponse.getStatus();
    _errorResponse = errorResponse;
    _decodedResponse = null;
  }

  /**
   * @deprecated Please use {@link #RestLiResponseException(com.linkedin.r2.message.rest.RestResponse, Response, com.linkedin.restli.common.ErrorResponse)}
   * @param errorResponse
   */
  @Deprecated
  public RestLiResponseException(ErrorResponse errorResponse)
  {
    super(RestResponse.NO_RESPONSE, errorResponse.getMessage());
    _status = errorResponse.getStatus();
    _errorResponse = errorResponse;
    _decodedResponse = null;
  }

  public RestLiResponseException(RestResponse rawResponse,
                                 Response<?> decodedResponse,
                                 ErrorResponse errorResponse,
                                 Throwable cause)
  {
    super(rawResponse, cause);
    _status = rawResponse.getStatus();
    _errorResponse = errorResponse;
    _decodedResponse = decodedResponse;
  }

  public int getStatus()
  {
    return _status;
  }

  public boolean hasServiceErrorCode()
  {
    return _errorResponse.hasServiceErrorCode();
  }

  public int getServiceErrorCode()
  {
    return _errorResponse.getServiceErrorCode(GetMode.NULL);
  }

  public boolean hasServiceErrorMessage()
  {
    return _errorResponse.hasMessage();
  }

  public String getServiceErrorMessage()
  {
    return _errorResponse.getMessage(GetMode.NULL);
  }

  public boolean hasServiceErrorStackTrace()
  {
    return _errorResponse.hasStackTrace();
  }

  public String getServiceErrorStackTrace()
  {
    return _errorResponse.getStackTrace(GetMode.NULL);
  }

  public boolean hasServiceExceptionClass()
  {
    return _errorResponse.hasExceptionClass();
  }

  public String getServiceExceptionClass()
  {
    return _errorResponse.getExceptionClass(GetMode.NULL);
  }

  public boolean hasErrorDetails()
  {
    return _errorResponse.hasErrorDetails();
  }

  public DataMap getErrorDetails()
  {
    if (hasErrorDetails())
    {
      return _errorResponse.getErrorDetails().data();
    }
    else
    {
      return null;
    }
  }

  public String getErrorSource()
  {
    RestResponse response = getResponse();
    return HeaderUtil.getErrorResponseHeaderValue(response.getHeaders());
  }

  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    builder.append(getClass().getName()).append(": Response status ");
    // This is the HTTP status code of the response
    builder.append(getResponse().getStatus());

    // The Rest.li error details may contain an error message from the server, an error code
    // from the server
    if (hasServiceErrorMessage())
    {
      builder.append(", serviceErrorMessage: ").append(getServiceErrorMessage());
    }
    if (hasServiceErrorCode())
    {
      builder.append(", serviceErrorCode: ").append(getServiceErrorCode());
    }

    // TODO: decide whether to include serviceErrorDetails and serverStackTrace.

    return builder.toString();

    // E.g.:
    // RestLiResponseException: Response status 400, serviceErrorMessage: Illegal content type "application/xml", serviceErrorCode: 999
  }

  public Response<?> getDecodedResponse()
  {
    return _decodedResponse;
  }

  public boolean hasDecodedResponse()
  {
    return _decodedResponse != null;
  }
}

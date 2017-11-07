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
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.internal.common.HeaderUtil;

import java.util.Optional;

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

  private final Optional<Integer> _status;
  private final Optional<ErrorResponse> _errorResponse;
  private final Optional<Response<?>> _decodedResponse;

  public RestLiResponseException(RestResponse rawResponse,
                                 Response<?> decodedResponse,
                                 ErrorResponse errorResponse)
  {
    super(rawResponse);
    _status = Optional.of(rawResponse.getStatus());
    _errorResponse = Optional.ofNullable(errorResponse);
    _decodedResponse =  Optional.ofNullable(decodedResponse);
  }

  public RestLiResponseException(RestResponse rawResponse,
                                 Response<?> decodedResponse,
                                 ErrorResponse errorResponse,
                                 Throwable cause)
  {
    super(rawResponse, cause);
    _status = Optional.of(rawResponse.getStatus());
    _errorResponse = Optional.ofNullable(errorResponse);
    _decodedResponse = Optional.ofNullable(decodedResponse);
  }

  RestLiResponseException(ErrorResponse errorResponse)
  {
    super(createErrorRestResponse(errorResponse));
    _status = Optional.ofNullable(errorResponse.getStatus());
    _errorResponse = Optional.ofNullable(errorResponse);
    _decodedResponse = Optional.empty();
  }

  public int getStatus()
  {
    return _status.orElse(0);
  }

  public ErrorResponse getErrorResponse() {
    return _errorResponse.orElse(null);
  }

  public boolean hasServiceErrorCode()
  {
    return _errorResponse.isPresent() && _errorResponse.get().hasServiceErrorCode();
  }

  public int getServiceErrorCode()
  {
    Integer serviceCode = 0;

    // Had to add the extra checks because getServiceErrorCode throws a null pointer exception if null
    if(_errorResponse.isPresent()) {
      serviceCode = _errorResponse.get().getServiceErrorCode(GetMode.NULL);
    }
    return serviceCode != null ? serviceCode : 0;
  }

  public boolean hasServiceErrorMessage()
  {
    return _errorResponse.isPresent() && _errorResponse.get().hasMessage();
  }

  public String getServiceErrorMessage()
  {
    return _errorResponse.isPresent() ? _errorResponse.get().getMessage(GetMode.NULL) : null;
  }

  public boolean hasServiceErrorStackTrace()
  {
    return _errorResponse.isPresent() && _errorResponse.get().hasStackTrace();
  }

  public String getServiceErrorStackTrace()
  {
    return _errorResponse.isPresent() ? _errorResponse.get().getStackTrace(GetMode.NULL) : null;
  }

  public boolean hasServiceExceptionClass()
  {
    return _errorResponse.isPresent() && _errorResponse.get().hasExceptionClass();
  }

  public String getServiceExceptionClass()
  {
    return _errorResponse.isPresent() ? _errorResponse.get().getExceptionClass(GetMode.NULL) : null;
  }

  public boolean hasErrorDetails()
  {
    return _errorResponse.isPresent() && _errorResponse.get().hasErrorDetails();
  }

  public DataMap getErrorDetails()
  {
    if (hasErrorDetails())
    {
      return _errorResponse.get().getErrorDetails().data();
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
    return _decodedResponse.orElse(null);
  }

  public boolean hasDecodedResponse()
  {
    return _decodedResponse.isPresent();
  }

  private static RestResponse createErrorRestResponse(ErrorResponse errorResponse)
  {
    Optional<ErrorResponse> response = Optional.ofNullable(errorResponse);
    Integer status = response.isPresent() ? response.get().getStatus() : 0;
    RestResponseBuilder builder = new RestResponseBuilder().setStatus(status);
    String errorMessage = response.isPresent() ? response.get().getMessage() : null;
    if (errorMessage != null)
    {
      builder.setEntity(errorMessage.getBytes());
    }

    return builder.build();
  }
}

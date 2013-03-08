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
import com.linkedin.restli.common.RestConstants;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

/**
 * RestLiResponseException is thrown when the client receives a response with a non-success
 * HttpStatus (<200 or >=300).  If the response body includes an ErrorResponse document, it will
 * be parsed, and its contents may be accessed via the accessor methods in RestLiResponseException.
 */
public class RestLiResponseException extends RestException
{
  private static final long serialVersionUID = 1;

  private final int _status;
  private final ErrorResponse _errorResponse;

  public RestLiResponseException(RestResponse rawResponse, ErrorResponse errorResponse)
  {
    super(rawResponse);
    _status = rawResponse.getStatus();
    _errorResponse = errorResponse;
  }

  public RestLiResponseException(RestResponse rawResponse, ErrorResponse errorResponse,
                                 Throwable cause)
  {
    super(rawResponse, cause);
    _status = rawResponse.getStatus();
    _errorResponse = errorResponse;
  }

  public RestLiResponseException(ErrorResponse errorResponse)
  {
    super(RestResponse.NO_RESPONSE, errorResponse.getMessage());
    _status = errorResponse.getStatus();
    _errorResponse = errorResponse;
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
    return _errorResponse.getErrorDetails();
  }
  
  public String getErrorSource()
  {
    return getResponse().getHeader(RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE);
  }

}

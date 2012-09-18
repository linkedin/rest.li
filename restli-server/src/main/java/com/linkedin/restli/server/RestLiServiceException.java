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

package com.linkedin.restli.server;

import com.linkedin.data.DataMap;
import com.linkedin.restli.common.HttpStatus;

/**
 *
 * @author dellamag
 */
public class RestLiServiceException extends RuntimeException
{
  private static final long serialVersionUID = 1L;

  private final HttpStatus  _status;
  private Integer           _serviceErrorCode;
  private DataMap           _errorDetails;

  public RestLiServiceException(final HttpStatus status)
  {
    this(status, null, null);
  }

  public RestLiServiceException(final HttpStatus status, final String message)
  {
    this(status, message, null);
  }

  public RestLiServiceException(final HttpStatus status,
                                final String message,
                                final Throwable cause)
  {
    super(message, cause);
    _status = status;
  }

  public HttpStatus getStatus()
  {
    return _status;
  }

  public RestLiServiceException setServiceErrorCode(final Integer serviceErrorCode)
  {
    _serviceErrorCode = serviceErrorCode;
    return this;
  }

  public boolean hasServiceErrorCode()
  {
    return _serviceErrorCode != null;
  }

  public int getServiceErrorCode()
  {
    return _serviceErrorCode;
  }

  public DataMap getErrorDetails()
  {
    return _errorDetails;
  }

  public boolean hasErrorDetails()
  {
    return _errorDetails != null;
  }

  public RestLiServiceException setErrorDetails(final DataMap errorDetails)
  {
    _errorDetails = errorDetails;
    return this;
  }

  @Override
  public String toString()
  {
    return "RestLiServiceException [_status=" + _status + ", _serviceErrorCode="
        + _serviceErrorCode + ", _errorDetails=" + _errorDetails + "]";
  }
}

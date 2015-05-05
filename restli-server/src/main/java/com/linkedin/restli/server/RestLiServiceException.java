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
 * Represents an unexpected service failure.
 *
 * @author dellamag
 */
public class RestLiServiceException extends RuntimeException
{
  private static final long serialVersionUID = 1L;

  private final HttpStatus    _status;
  private Integer             _serviceErrorCode;
  private DataMap             _errorDetails;
  private ErrorResponseFormat _errorResponseFormat;

  public RestLiServiceException(final HttpStatus status)
  {
    this(status, null, null);
  }

  public RestLiServiceException(final HttpStatus status, final String message)
  {
    this(status, message, null);
  }

  public RestLiServiceException(final HttpStatus status,
                                final Throwable cause)
  {
    super(cause);
    _status = status;
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

  public Integer getServiceErrorCode()
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
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getName());
    sb.append(" [HTTP Status:").append(_status.getCode());
    if (_serviceErrorCode != null)
    {
      sb.append(", serviceErrorCode:").append(_serviceErrorCode);
    }
    sb.append("]");
    String message = getLocalizedMessage();
    if (message != null)
    {
      sb.append(": ").append(message);
    }
    return sb.toString();
  }

  /**
   * Sets an error response format that will be used instead of the default server wide
   * error response format.
   *
   * @param errorResponseFormat the overriding ErrorResponseFormat this service exception should be built with.
   */
  public void setOverridingFormat(ErrorResponseFormat errorResponseFormat)
  {
    _errorResponseFormat = errorResponseFormat;
  }

  /**
   * Returns whether this exception has an overriding error format.
   *
   * @return true if this exception has an overriding error response format set.
   */
  public boolean hasOverridingErrorResponseFormat()
  {
    return _errorResponseFormat != null;
  }

  public ErrorResponseFormat getOverridingFormat()
  {
    return _errorResponseFormat;
  }
}

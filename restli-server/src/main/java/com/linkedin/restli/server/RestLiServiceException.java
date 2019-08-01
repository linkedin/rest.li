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
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ErrorDetails;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.errors.ServiceError;


/**
 * Represents a Rest.li service failure.
 *
 * @author dellamag
 */
public class RestLiServiceException extends RuntimeException
{
  private static final long serialVersionUID = 1L;

  private final HttpStatus _status;
  private String _code;
  private String _docUrl;
  private String _requestId;
  private RecordTemplate _errorDetails;
  private ErrorResponseFormat _errorResponseFormat;

  // This field is now deprecated, code should be used instead
  private Integer _serviceErrorCode;

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

  /**
   * Construct a new instance using the specified HTTP status, exception message, cause, and an option to disable
   * stacktrace. Consider setting {@code writableStackTrace} to {@code false} to conserve computation cost if the
   * stacktrace does not contribute meaningful insights.
   *
   * @param status the HTTP status to use along with the exception
   * @param message the exception message for this exception.
   * @param cause the cause of this exception.
   * @param writableStackTrace the exception stacktrace is filled in if true; false otherwise.
   */
  public RestLiServiceException(final HttpStatus status,
      final String message, final Throwable cause, boolean writableStackTrace)
  {
    super(message, cause, true, writableStackTrace);
    _status = status;
  }

  /**
   * Construct a Rest.li service exception from a given service error definition. The HTTP status, service error code,
   * and message are copied from the service error definition into this exception.
   *
   * @param serviceError service error definition
   */
  public RestLiServiceException(final ServiceError serviceError)
  {
    this(serviceError, null);
  }

  /**
   * Construct a Rest.li service exception from a given service error definition and an exception cause.
   * The HTTP status, service error code, and message are copied from the service error definition into this exception,
   * along with the exception cause.
   *
   * @param serviceError service error definition
   * @param cause exception cause
   */
  public RestLiServiceException(final ServiceError serviceError, final Throwable cause)
  {
    this(serviceError.httpStatus(), serviceError.message(), cause);
    _code = serviceError.code();
  }

  public HttpStatus getStatus()
  {
    return _status;
  }

  public String getCode()
  {
    return _code;
  }

  public boolean hasCode()
  {
    return _code != null;
  }

  public RestLiServiceException setCode(final String code)
  {
    _code = code;
    return this;
  }

  public String getDocUrl()
  {
    return _docUrl;
  }

  public boolean hasDocUrl()
  {
    return _docUrl != null;
  }

  public RestLiServiceException setDocUrl(final String docUrl)
  {
    _docUrl = docUrl;
    return this;
  }

  public String getRequestId()
  {
    return _requestId;
  }

  public boolean hasRequestId()
  {
    return _requestId != null;
  }

  public RestLiServiceException setRequestId(final String requestId)
  {
    _requestId = requestId;
    return this;
  }

  public DataMap getErrorDetails()
  {
    return _errorDetails == null ? null : _errorDetails.data();
  }

  public RecordTemplate getErrorDetailsRecord()
  {
    return _errorDetails;
  }

  public boolean hasErrorDetails()
  {
    return _errorDetails != null;
  }

  public RestLiServiceException setErrorDetails(final DataMap errorDetails)
  {
    _errorDetails = errorDetails == null ? null : new ErrorDetails(errorDetails);
    return this;
  }

  public RestLiServiceException setErrorDetails(final RecordTemplate errorDetails)
  {
    _errorDetails = errorDetails;
    return this;
  }

  /**
   * @return the fully-qualified name of the error detail record, if it exists.
   */
  public String getErrorDetailType()
  {
    if (hasErrorDetails())
    {
      final RecordDataSchema errorDetailSchema = _errorDetails.schema();
      if (errorDetailSchema != null)
      {
        final String errorDetailType = errorDetailSchema.getFullName();
        if (errorDetailType != null)
        {
          return errorDetailType;
        }
      }
    }
    return null;
  }

  /**
   * @deprecated Use {@link #getCode()} instead.
   */
  @Deprecated
  public Integer getServiceErrorCode()
  {
    return _serviceErrorCode;
  }

  /**
   * @deprecated Use {@link #hasCode()} instead.
   */
  @Deprecated
  public boolean hasServiceErrorCode()
  {
    return _serviceErrorCode != null;
  }

  /**
   * @deprecated Use {@link #setCode(String)} instead.
   */
  @Deprecated
  public RestLiServiceException setServiceErrorCode(final Integer serviceErrorCode)
  {
    _serviceErrorCode = serviceErrorCode;
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

    if (hasCode())
    {
      sb.append(", code:").append(_code);
    }

    if (hasDocUrl())
    {
      sb.append(", docUrl:").append(_docUrl);
    }

    if (hasRequestId())
    {
      sb.append(", requestId:").append(_requestId);
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

  public static RestLiServiceException fromThrowable(Throwable throwable)
  {
    RestLiServiceException restLiServiceException;
    if (throwable instanceof RestLiServiceException)
    {
      restLiServiceException = (RestLiServiceException) throwable;
    }
    else if (throwable instanceof RoutingException)
    {
      RoutingException routingException = (RoutingException) throwable;

      restLiServiceException = new RestLiServiceException(HttpStatus.fromCode(routingException.getStatus()),
          routingException.getMessage(),
          routingException);
    }
    else
    {
      restLiServiceException = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
          throwable.getMessage(),
          throwable);
    }

    return restLiServiceException;
  }

}

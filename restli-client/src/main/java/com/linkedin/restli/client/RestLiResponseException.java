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
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
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

  public RestLiResponseException(RestResponse rawResponse,
                                 Response<?> decodedResponse,
                                 ErrorResponse errorResponse)
  {
    super(rawResponse);
    _status = rawResponse.getStatus();
    _errorResponse = errorResponse;
    _decodedResponse = decodedResponse;
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

  public RestLiResponseException(ErrorResponse errorResponse)
  {
    super(createErrorRestResponse(errorResponse));
    _status = errorResponse.getStatus();
    _errorResponse = errorResponse;
    _decodedResponse = null;
  }

  public int getStatus()
  {
    return _status;
  }

  public boolean hasCode()
  {
    return _errorResponse.hasCode();
  }

  public String getCode()
  {
    return _errorResponse.getCode(GetMode.NULL);
  }

  public boolean hasServiceErrorMessage()
  {
    return _errorResponse.hasMessage();
  }

  public String getServiceErrorMessage()
  {
    return _errorResponse.getMessage(GetMode.NULL);
  }

  public boolean hasDocUrl()
  {
    return _errorResponse.hasDocUrl();
  }

  public String getDocUrl()
  {
    return _errorResponse.getDocUrl(GetMode.NULL);
  }

  public boolean hasRequestId()
  {
    return _errorResponse.hasRequestId();
  }

  public String getRequestId()
  {
    return _errorResponse.getRequestId(GetMode.NULL);
  }

  public boolean hasServiceExceptionClass()
  {
    return _errorResponse.hasExceptionClass();
  }

  public String getServiceExceptionClass()
  {
    return _errorResponse.getExceptionClass(GetMode.NULL);
  }

  public boolean hasServiceErrorStackTrace()
  {
    return _errorResponse.hasStackTrace();
  }

  public String getServiceErrorStackTrace()
  {
    return _errorResponse.getStackTrace(GetMode.NULL);
  }

  public boolean hasErrorDetailType()
  {
    return _errorResponse.hasErrorDetailType();
  }

  public String getErrorDetailType()
  {
    return _errorResponse.getErrorDetailType(GetMode.NULL);
  }

  public boolean hasErrorDetails()
  {
    return _errorResponse.hasErrorDetails();
  }

  @SuppressWarnings("ConstantConditions")
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

  /**
   * Gets the error details as a typed record based on the error detail type. {@code null} will be returned if
   * there are no error details, if there is no error detail type, or if no class is found that corresponds with
   * the error detail type.
   *
   * @param <T> the error detail type specified in the {@link ErrorResponse}
   * @return the error details as a typed record, or null
   */
  @SuppressWarnings({"unchecked", "ConstantConditions"})
  public <T extends RecordTemplate> T getErrorDetailsRecord()
  {
    if (_errorResponse.hasErrorDetails() && _errorResponse.hasErrorDetailType())
    {
      String type = _errorResponse.getErrorDetailType();
      try
      {
        Class<?> typeClass = Class.forName(type);
        if (RecordTemplate.class.isAssignableFrom(typeClass))
        {
          Class<? extends RecordTemplate> recordType = typeClass.asSubclass(RecordTemplate.class);
          RecordDataSchema schema = (RecordDataSchema) DataTemplateUtil.getSchema(typeClass);
          return (T) DataTemplateUtil.wrap(_errorResponse.getErrorDetails().data(), schema, recordType);
        }
      }
      catch (ClassNotFoundException e)
      {
        return null;
      }
    }
    return null;
  }

  @Deprecated
  public boolean hasServiceErrorCode()
  {
    return _errorResponse.hasServiceErrorCode();
  }

  @Deprecated
  public int getServiceErrorCode()
  {
    return _errorResponse.getServiceErrorCode(GetMode.NULL);
  }

  public String getErrorSource()
  {
    RestResponse response = getResponse();
    return HeaderUtil.getErrorResponseHeaderValue(response.getHeaders());
  }

  /**
   * Generates a string representation of this exception.
   *
   * e.g. RestLiResponseException: Response status 400, serviceErrorMessage: Illegal content type "application/xml",
   *      serviceErrorCode: 999, code: INVALID_INPUT, docUrl: https://example.com/errors/invalid-input, requestId: abc123
   * @return string representation
   */
  @SuppressWarnings("deprecation")
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

    // TODO: remove this eventually once this field is no longer supported
    if (hasServiceErrorCode())
    {
      builder.append(", serviceErrorCode: ").append(getServiceErrorCode());
    }

    if (hasCode())
    {
      builder.append(", code: ").append(getCode());
    }

    if (hasDocUrl())
    {
      builder.append(", docUrl: ").append(getDocUrl());
    }

    if (hasRequestId())
    {
      builder.append(", requestId: ").append(getRequestId());
    }

    return builder.toString();
  }

  public Response<?> getDecodedResponse()
  {
    return _decodedResponse;
  }

  public boolean hasDecodedResponse()
  {
    return _decodedResponse != null;
  }

  private static RestResponse createErrorRestResponse(ErrorResponse errorResponse)
  {
    RestResponseBuilder builder = new RestResponseBuilder().setStatus(errorResponse.getStatus());
    String errorMessage = errorResponse.getMessage();
    if (errorMessage != null)
    {
      builder.setEntity(errorMessage.getBytes());
    }

    return builder.build();
  }
}

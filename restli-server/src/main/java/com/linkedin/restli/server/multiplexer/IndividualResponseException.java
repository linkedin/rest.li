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

package com.linkedin.restli.server.multiplexer;


import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.multiplexer.IndividualBody;
import com.linkedin.restli.common.multiplexer.IndividualResponse;
import com.linkedin.restli.internal.server.methods.response.ErrorResponseBuilder;
import com.linkedin.restli.server.RestLiServiceException;


/**
 * An exception, which contains an IndividualResponse. This is used by the multiplexer tasks to indicate an operation has
 * failed and the corresponding failures are encoded as an IndividualResponse object stored within the exception.
 *
 * @author Gary Lin
 */
/* package private */ final class IndividualResponseException extends Exception
{
  private static final long serialVersionUID = 1;
  private final IndividualResponse _response;

  public IndividualResponseException(HttpStatus status, String message)
  {
    super(message);
    _response = createErrorIndividualResponse(status, createErrorResponse(status, message));
  }

  public IndividualResponseException(HttpStatus status, String message, Throwable e)
  {
    super(message, e);
    _response = createErrorIndividualResponse(status, createErrorResponse(status, message));
  }

  public IndividualResponseException(RestLiServiceException e)
  {
    super(e);
    _response = createErrorIndividualResponse(e.getStatus(), createErrorResponse(e));
  }

  public IndividualResponse getResponse()
  {
    return _response;
  }

  public static IndividualResponse createInternalServerErrorIndividualResponse(Throwable e)
  {
    return createInternalServerErrorIndividualResponse(e.getMessage());
  }

  public static IndividualResponse createInternalServerErrorIndividualResponse(String message)
  {
    ErrorResponse errorResponse = null;
    if (message != null && !message.isEmpty())
    {
      errorResponse = createErrorResponse(HttpStatus.S_500_INTERNAL_SERVER_ERROR, message);
    }
    return createErrorIndividualResponse(HttpStatus.S_500_INTERNAL_SERVER_ERROR, errorResponse);
  }

  public static IndividualResponse createErrorIndividualResponse(RestLiServiceException e)
  {
    return createErrorIndividualResponse(e.getStatus(), createErrorResponse(e));
  }

  private static IndividualResponse createErrorIndividualResponse(HttpStatus status, ErrorResponse errorResponse)
  {
    IndividualResponse response = new IndividualResponse();
    response.setStatus(status.getCode());
    if (errorResponse != null)
    {
      response.setBody(new IndividualBody(errorResponse.data()));
    }
    return response;
  }

  private static ErrorResponse createErrorResponse(HttpStatus status, String message)
  {
    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setStatus(status.getCode());
    errorResponse.setMessage(message);
    return errorResponse;
  }

  private static ErrorResponse createErrorResponse(RestLiServiceException e)
  {
    return new ErrorResponseBuilder().buildErrorResponse(e);
  }
}

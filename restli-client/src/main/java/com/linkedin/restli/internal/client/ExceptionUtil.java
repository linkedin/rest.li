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

package com.linkedin.restli.internal.client;

import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiDecodingException;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.RestConstants;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ExceptionUtil
{
  private static final EntityResponseDecoder<ErrorResponse> ERROR_DECODER =
                                                                              new EntityResponseDecoder<ErrorResponse>(ErrorResponse.class);

  private ExceptionUtil()
  {
  }

  public static RemoteInvocationException exceptionForThrowable(Throwable e, RestResponseDecoder<?> responseDecoder)
  {
    if (e instanceof RestException)
    {
      final RestException re = (RestException) e;
      final RestResponse response = re.getResponse();
      final ErrorResponse errorResponse;

      try
      {
        errorResponse = getErrorResponse(response);
      }
      catch (RestLiDecodingException decodingException)
      {
        return new RemoteInvocationException(decodingException);
      }

      Response<?> decodedResponse = null;
      String header = getErrorResponseHeaderValue(response);

      if (header == null)
      {
        try
        {
          decodedResponse = responseDecoder.decodeResponse(response);
        }
        catch (RestLiDecodingException decodingException)
        {
          return new RemoteInvocationException(decodingException);
        }
      }

      return new RestLiResponseException(response, decodedResponse, errorResponse, e);
    }

    if (e instanceof RemoteInvocationException)
    {
      return (RemoteInvocationException) e;
    }

    return new RemoteInvocationException(e);
  }

  static RemoteInvocationException wrapThrowable(Throwable e)
  {
    if (e instanceof RestLiResponseException)
    {
      final RestLiResponseException restliException = (RestLiResponseException) e;
      final ErrorResponse errorResponse;

      try
      {
        errorResponse = getErrorResponse(restliException.getResponse());
      }
      catch (RestLiDecodingException decodingException)
      {
        return new RemoteInvocationException(decodingException);
      }

      return new RestLiResponseException(restliException.getResponse(),
                                         restliException.getDecodedResponse(),
                                         errorResponse,
                                         restliException);
    }

    return new RemoteInvocationException(e);
  }

  private static ErrorResponse getErrorResponse(RestResponse response) throws RestLiDecodingException
  {
    ErrorResponse errorResponse = null;

    String header = getErrorResponseHeaderValue(response);

    if (header != null)
    {
      errorResponse =  ERROR_DECODER.decodeResponse(response).getEntity();
    }

    if (errorResponse == null)
    {
      errorResponse = new ErrorResponse();
    }

    return errorResponse;
  }

  public static String getErrorResponseHeaderValue(RestResponse response)
  {
    // we are deprecating all X-Linkedin header prefixes and replacing them with X-RestLi
    String newHeader = response.getHeader(RestConstants.HEADER_RESTLI_ERROR_RESPONSE);
    String oldHeader = response.getHeader(RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE);
    return (newHeader != null) ? newHeader : oldHeader;
  }
}

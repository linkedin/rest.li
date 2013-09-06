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

  /**
   * Returns a RemoteInvocationException
   *
   * @param e
   *          original cause
   * @param mustWrap
   *          true if the Throwable should always be wrapped, e.g. because it may have
   *          originated on a different thread and will be thrown on the current thread.
   */
  public static RemoteInvocationException exceptionForThrowable(Throwable e,
                                                                final boolean mustWrap)
  {
    if (e instanceof RestException)
    {
      RestException re = (RestException) e;
      RestResponse response = re.getResponse();
      ErrorResponse errorResponse;

      String header = getErrorResponseHeaderValue(response);

      if (header != null)
      {
        try
        {
          errorResponse =  ERROR_DECODER.decodeResponse(response).getEntity();
          if (errorResponse == null)
          {
            errorResponse = new ErrorResponse();
          }
        }
        catch (RestLiDecodingException e1)
        {
          errorResponse = new ErrorResponse();
        }
      }
      else
      {
        errorResponse = new ErrorResponse();
      }
      return new RestLiResponseException(response, errorResponse, e);
    }
    if (e instanceof RemoteInvocationException)
    {
      return mustWrap ? new RemoteInvocationException(e) : (RemoteInvocationException) e;
    }
    return new RemoteInvocationException(e);
  }

  public static String getErrorResponseHeaderValue(RestResponse response)
  {
    // we are deprecating all X-Linkedin header prefixes and replacing them with X-RestLi
    String newHeader = response.getHeader(RestConstants.HEADER_RESTLI_ERROR_RESPONSE);
    String oldHeader = response.getHeader(RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE);
    return (newHeader != null) ? newHeader : oldHeader;
  }
}

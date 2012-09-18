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

package com.linkedin.restli.internal.server;

import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.restli.common.ErrorResponse;

/**
 * Signifies an internal error in the rest-li framework.
 *
 * @author dellamag
 */
public class RestLiInternalException extends RuntimeException
{
  private static final long serialVersionUID = 1L;

  public RestLiInternalException()
  {
    super();
  }

  public RestLiInternalException(final String message, final Throwable cause)
  {
    super(message, cause);
  }

  public RestLiInternalException(final String message)
  {
    super(message);
  }

  public RestLiInternalException(final Throwable cause)
  {
    super(cause);
  }

  public ErrorResponse toErrorResponse()
  {
    ErrorResponse response = new ErrorResponse();
    response.setStatus(RestStatus.INTERNAL_SERVER_ERROR);
    response.setExceptionClass(RestLiInternalException.class.getName());
    if (getMessage() != null)
    {
      response.setMessage(getMessage());
    }

    return response;
  }
}

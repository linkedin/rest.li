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

package com.linkedin.restli.internal.server.methods.response;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.server.RestLiServiceException;

/**
* @author Josh Walker
* @version $Revision: $
*/
public final class ErrorResponseBuilder implements RestLiResponseBuilder
{
  private static final ErrorResponseBuilder _INSTANCE = new ErrorResponseBuilder();

  public static ErrorResponseBuilder getInstance()
  {
    return _INSTANCE;
  }

  private ErrorResponseBuilder()
  {
  }

  @Override
  public PartialRestResponse buildResponse(final RestRequest request,
                                           final RoutingResult routingResult,
                                           final Object object,
                                           final Map<String, String> headers)
  {
    RestLiServiceException result = (RestLiServiceException) object;
    ErrorResponse er = new ErrorResponse();
    er.setStatus(result.getStatus().getCode());
    if (result.getMessage() != null)
    {
      er.setMessage(result.getMessage());
    }
    if (result.hasServiceErrorCode())
    {
      er.setServiceErrorCode(result.getServiceErrorCode());
    }
    if (result.hasErrorDetails())
    {
      er.setErrorDetails(result.getErrorDetails());
    }

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    result.printStackTrace(pw);
    er.setStackTrace(sw.toString());

    er.setExceptionClass(result.getClass().getName());
    headers.put(RestConstants.HEADER_LINKEDIN_TYPE, er.getClass().getName());
    headers.put(RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE,
                RestConstants.HEADER_VALUE_ERROR_APPLICATION);
    return new PartialRestResponse(result.getStatus(), er);
  }
}

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
import com.linkedin.restli.internal.common.HeaderUtil;


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

      // There are three cases where client will receive RestException.
      // 1) Request has reached Rest.li server and Rest.li server throws RestliServiceException. In this case,
      // HEADER_RESTLI_ERROR_RESPONSE header will be set, and response body should be in JSON format representing
      // an ErrorResponse. Rest.li client should return RestliResponseException for this case with its errorResponse
      // field populated with decoded ErrorResponse.
      // 2) An action request has reached Rest.li server and Rest.li server returns an ActionResult<Value, StatusCode> where
      // StatusCode < 200 or StatusCode >= 300. In this case, after HttpBridge, Rest.li client will receive a
      // RestException with no HEADER_RESTLI_ERROR_RESPONSE header set, but response body can be valid action return
      // value in proper JSON format. Rest.li client will also return RestliResponseException for this case with its
      // decodedResponse populated with the action return value.
      // 3) Request has reached servlet container but haven't reached Rest.li server. RestException is thrown from Servlet
      // container or r2 request filters. In this case, no HEADER_RESTLI_ERROR_RESPONSE header is set, and the body can
      // be of any format (plain, html, etc) and cannot be guaranteed to be JSON at all. We should just treat such cases
      // as another type of RemoteInvocationException and wrap RestException as its cause so that client can check its
      // status and detailed error message.
      //
      // TODO: We should understand case #2 better to see if there is really an application need for such an ActionResult
      // behavior that cannot be achieved through throwing RestliServiceException. Currently for ActionResult with a
      // non-ok status code, client side will treat this as an error case anyway, application onError callback will be
      // invoked and most likely application error handler will not dig into RestliResponseException.decodedResponse to
      // get the action return value. If this use case ends up not realistic, we can simplify our logic below to only
      // decode the response body when HEADER_RESTLI_ERROR_RESPONSE header is set.
      try
      {
        errorResponse = getErrorResponse(response);
      }
      catch (RestLiDecodingException decodingException)
      {
        return new RemoteInvocationException(e.getMessage(), decodingException);
      }

      Response<?> decodedResponse = null;
      final String header = HeaderUtil.getErrorResponseHeaderValue(response.getHeaders());

      if (header == null)
      {
        // This is purely to handle case #2 commented above.
        try
        {
          decodedResponse = responseDecoder.decodeResponse(response);
        }
        catch (RestLiDecodingException decodingException)
        {
          return new RemoteInvocationException(e.getMessage(), e);
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

    final String header = HeaderUtil.getErrorResponseHeaderValue(response.getHeaders());

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
}

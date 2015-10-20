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

package com.linkedin.restli.internal.server.filter;


import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.common.ProtocolVersionUtil;
import com.linkedin.restli.internal.server.RestLiResponseEnvelope;
import com.linkedin.restli.internal.server.RestLiResponseHandler;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.filter.FilterResponseContext;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;


/**
 * Factory class that creates {@link FilterResponseContext} based on the given inputs.
 *
 * @param <T>
 *
 * @author nshankar
 */
public class RestLiResponseFilterContextFactory<T>
{
  private final RoutingResult _method;
  private final RestLiResponseHandler _responseHandler;
  private final RestRequest _request;

  public RestLiResponseFilterContextFactory(final RestRequest request,
                                            final RoutingResult method,
                                            final RestLiResponseHandler responseHandler)
  {
    _request = request;
    _method = method;
    _responseHandler = responseHandler;
  }

  /**
   * Create a {@link FilterResponseContext} based on the given result.
   *
   * @param result obtained from the resource method invocation.
   *
   * @return {@link FilterResponseContext} corresponding to the given input.
   *
   * @throws IOException If an error was encountered while building a {@link FilterResponseContext}.
   */
  public FilterResponseContext fromResult(T result) throws IOException
  {
    final RestLiResponseEnvelope responseData = _responseHandler.buildRestLiResponseData(_request, _method, result);
    return new FilterResponseContext()
    {
      @Override
      public RestLiResponseData getResponseData()
      {
        return responseData;
      }
    };
  }

  /**
   * Create a {@link FilterResponseContext} based on the given error.
   *
   * @param throwable Error obtained from the resource method invocation.
   *
   * @return {@link FilterResponseContext} corresponding to the given input.
   */
  public FilterResponseContext fromThrowable(Throwable throwable)
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

    Map<String, String> requestHeaders = _request.getHeaders();
    Map<String, String> headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION,
                ProtocolVersionUtil.extractProtocolVersion(requestHeaders).toString());
    headers.put(HeaderUtil.getErrorResponseHeaderName(requestHeaders), RestConstants.HEADER_VALUE_ERROR);
    final RestLiResponseEnvelope responseData = _responseHandler.buildExceptionResponseData(_request,
                                                                                            _method,
                                                                                            restLiServiceException,
                                                                                            headers,
                                                                                            Collections.<HttpCookie>emptyList());
    return new FilterResponseContext()
    {
      @Override
      public RestLiResponseData getResponseData()
      {
        return responseData;
      }
    };
  }
}

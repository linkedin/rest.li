/*
   Copyright (c) 2016 LinkedIn Corp.

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


import com.linkedin.common.callback.Callback;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.HeaderUtil;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.response.RestLiResponse;
import com.linkedin.restli.internal.server.response.RestLiResponseException;
import com.linkedin.restli.internal.server.response.RestLiResponseHandler;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Concrete implementation of {@link FilterChainCallback}.
 *
 * @author gye
 */
public class FilterChainCallbackImpl implements FilterChainCallback
{
  private static final Logger LOGGER = LoggerFactory.getLogger(FilterChainCallbackImpl.class);
  private RoutingResult _method;
  private RestLiResponseHandler _responseHandler;
  private Callback<RestLiResponse> _wrappedCallback;
  private final ErrorResponseBuilder _errorResponseBuilder;

  public FilterChainCallbackImpl(RoutingResult method,
      RestLiResponseHandler responseHandler,
      Callback<RestLiResponse> wrappedCallback,
      ErrorResponseBuilder errorResponseBuilder)
  {
    _method = method;
    _responseHandler = responseHandler;
    _wrappedCallback = wrappedCallback;
    _errorResponseBuilder = errorResponseBuilder;
  }

  @Override
  public void onResponseSuccess(final RestLiResponseData<?> responseData)
  {
    RestLiResponse partialResponse;
    try
    {
      partialResponse = _responseHandler.buildPartialResponse(_method, responseData);
    }
    catch (Throwable th)
    {
      LOGGER.error("Unexpected error while building the success response. Converting to error response.", th);
      _wrappedCallback.onError(new RestLiResponseException(th, buildErrorResponse(th, responseData)));
      return;
    }

    _wrappedCallback.onSuccess(partialResponse);
  }

  @Override
  public void onError(Throwable th, final RestLiResponseData<?> responseData)
  {
    // The Throwable passed in is not used at all. However, before every invocation, the throwable is wrapped inside
    // the RestLiResponseData parameter. This can potentially be refactored.

    Throwable error;
    try
    {
      RestLiServiceException serviceException = responseData.getResponseEnvelope().getException();
      final RestLiResponse response = _responseHandler.buildPartialResponse(_method, responseData);
      error = new RestLiResponseException(serviceException, response);
    }
    catch (Throwable throwable)
    {
      LOGGER.error("Unexpected error when processing error response.", responseData.getResponseEnvelope().getException());
      error = throwable;
    }

    _wrappedCallback.onError(error);
  }

  private RestLiResponse buildErrorResponse(Throwable th, RestLiResponseData<?> responseData)
  {
    Map<String, String> responseHeaders = responseData.getHeaders();
    responseHeaders.put(HeaderUtil.getErrorResponseHeaderName(responseHeaders), RestConstants.HEADER_VALUE_ERROR);
    RestLiServiceException ex;
    if (th instanceof RestLiServiceException)
    {
      ex = (RestLiServiceException) th;
    }
    else
    {
      ex = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, th.getMessage(), th);
    }

    return new RestLiResponse.Builder().headers(responseHeaders).cookies(responseData.getCookies())
        .status(ex.getStatus())
        .entity(_errorResponseBuilder.buildErrorResponse(ex))
        .build();
  }

}

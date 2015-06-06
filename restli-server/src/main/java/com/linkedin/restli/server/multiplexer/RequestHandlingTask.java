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


import com.linkedin.common.callback.Callback;
import com.linkedin.parseq.BaseTask;
import com.linkedin.parseq.Context;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.restli.common.HttpStatus;


/**
 * A task responsible for executing individual requests. It converts callback based interface of {@link RestRequestHandler}
 * into a promise based interface. The task never fails, but it may return an error response.
 *
 * @author Dmitriy Yefremov
 */
/* package private */ class RequestHandlingTask extends BaseTask<RestResponse>
{
  private final RestRequestHandler _requestHandler;

  private final RestRequest _request;

  private final RequestContext _requestContext;

  /* package private */ RequestHandlingTask(RestRequestHandler requestHandler, RestRequest request, RequestContext requestContext)
  {
    _requestHandler = requestHandler;
    _request = request;
    _requestContext = requestContext;
  }

  @Override
  protected Promise<RestResponse> run(Context context) throws Throwable
  {
    final SettablePromise<RestResponse> promise = Promises.settable();
    Callback<RestResponse> callback = new Callback<RestResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        RestResponse result = toErrorResponse(e);
        promise.done(result);
      }

      @Override
      public void onSuccess(RestResponse result)
      {
        promise.done(result);
      }
    };
    // try invoking the handler
    try {
      _requestHandler.handleRequest(_request, _requestContext, callback);
    }
    catch (Exception e)
    {
      callback.onError(e);
    }
    return promise;
  }

  private RestResponse toErrorResponse(Throwable e)
  {
    // It is assumed that the handler always returns RestException. It happens because the original callback passed into
    // the handler is wrapped into com.linkedin.restli.internal.server.RestLiCallback.
    // It is possible the callback is not wrapped and other exceptions are returned
    // (e.g. com.linkedin.restli.server.RestLiServiceException). This is considered either a programmers mistake or a
    // runtime exception.
    if (e instanceof RestException)
    {
      return ((RestException) e).getResponse();
    }
    return new RestResponseBuilder()
        .setStatus(HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode())
        .build();
  }
}

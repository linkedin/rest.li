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
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.restli.internal.server.RestLiMethodInvoker;


/**
 * A task responsible for executing individual requests. It converts callback based interface of {@link RestRequestHandler}
 * into a promise based interface.
 *
 * If request handling failed with a RestException, it will not fail the request.  Instead, it will just returned the encapsulated RestResponse object.
 * The task can fail immediately if the previous task failed. Any other unexpected exception can also cause the task to fail.
 *
 * @author Dmitriy Yefremov
 */
/* package private */ final class RequestHandlingTask extends BaseTask<RestResponse>
{
  private final RestRequestHandler _requestHandler;
  private final BaseTask<RestRequest> _request;
  private final RequestContext _requestContext;
  private final MultiplexerRunMode _multiplexerRunMode;

  /* package private */ RequestHandlingTask(RestRequestHandler requestHandler, BaseTask<RestRequest> request, RequestContext requestContext,
      MultiplexerRunMode multiplexerRunMode)
  {
    _requestHandler = requestHandler;
    _request = request;
    _requestContext = requestContext;
    _multiplexerRunMode = multiplexerRunMode;
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
        // It is assumed that the handler always returns RestException. It happens because the original callback passed into
        // the handler is wrapped into com.linkedin.restli.internal.server.RestLiCallback.
        // It is possible the callback is not wrapped and other exceptions are returned
        // (e.g. com.linkedin.restli.server.RestLiServiceException). This is considered either a programmers mistake or a
        // runtime exception.
        if (e instanceof RestException)
        {
          promise.done(((RestException) e).getResponse());
        }
        else
        {
          promise.fail(e);
        }
      }

      @Override
      public void onSuccess(RestResponse result)
      {
        promise.done(result);
      }
    };

    if (_request.isFailed())
    {
      callback.onError(_request.getError());
    }
    else
    {
      // try invoking the handler
      try
      {
        if (_multiplexerRunMode == MultiplexerRunMode.SINGLE_PLAN)
        {
          RestLiMethodInvoker.TASK_CONTEXT.set(context);
        }
        _requestHandler.handleRequest(_request.get(), _requestContext, callback);
      }
      catch (Exception e)
      {
        callback.onError(e);
      }
      finally
      {
        if (_multiplexerRunMode == MultiplexerRunMode.SINGLE_PLAN)
        {
          RestLiMethodInvoker.TASK_CONTEXT.set(null);
        }
      }
    }
    return promise;
  }
}

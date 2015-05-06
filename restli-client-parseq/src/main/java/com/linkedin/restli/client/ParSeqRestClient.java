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

package com.linkedin.restli.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.parseq.BaseTask;
import com.linkedin.parseq.Context;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.common.OperationNameGenerator;


/**
 * Wrapper around {@link RestClient} that facilitates usage with ParSeq.
 *
 * @author jnwang
 */
public class ParSeqRestClient
{
  private final RestClient            _wrappedClient;

  public ParSeqRestClient(final RestClient wrappedClient)
  {
    _wrappedClient = wrappedClient;
  }

  /**
   * Sends a type-bound REST request, returning a promise.
   *
   * @param request to send
   * @return response promise
   */
  public <T> Promise<Response<T>> sendRequest(final Request<T> request)
  {
    return sendRequest(request, new RequestContext());
  }

  /**
   * Sends a type-bound REST request, returning a promise.
   *
   * @param request to send
   * @param requestContext context for the request
   * @return response promise
   */
  public <T> Promise<Response<T>> sendRequest(final Request<T> request,
                                              final RequestContext requestContext)
  {
    final SettablePromise<Response<T>> promise = Promises.settable();

    // wrapper around the callback interface
    // when the request finishes, the callback updates the promise with the corresponding
    // result
    _wrappedClient.sendRequest(request, requestContext, new PromiseCallbackAdapter<T>(promise));
    return promise;
  }

  private class PromiseCallbackAdapter<T> implements Callback<Response<T>>
  {
    private final SettablePromise<Response<T>> _promise;

    public PromiseCallbackAdapter(final SettablePromise<Response<T>> promise)
    {
      this._promise = promise;
    }

    @Override
    public void onSuccess(final Response<T> result)
    {
      try
      {
        _promise.done(result);
      }
      catch (Exception e)
      {
        onError(e);
      }
    }

    @Override
    public void onError(final Throwable e)
    {
      _promise.fail(e);
    }
  }

  /**
   * Return a task that will send a type-bound REST request when run.
   *
   * @param request to send
   * @return response task
   */
  public <T> Task<Response<T>> createTask(final Request<T> request)
  {
    return createTask(request, new RequestContext());
  }

  /**
   * Return a task that will send a type-bound REST request when run. The task's name
   * defaults to information about the request.
   *
   * @param request to send
   * @param requestContext context for the request
   * @return response task
   */
  public <T> Task<Response<T>> createTask(final Request<T> request,
                                          final RequestContext requestContext)
  {
    return createTask(generateTaskName(request), request, requestContext);
  }

  /**
   * Generates a task name for the current task.
   * @param request the outgoing request
   * @return a task name
   */
  private String generateTaskName(final Request<?> request)
  {
    StringBuilder sb = new StringBuilder(request.getBaseUriTemplate());
    sb.append(" ");
    sb.append(OperationNameGenerator.generate(request.getMethod(), request.getMethodName()));
    return sb.toString();
  }

  /**
   * Return a task that will send a type-bound REST request when run.
   *
   * @param request to send
   * @param requestContext context for the request
   * @param name the name of the tasks
   * @return response task
   */
  public <T> Task<Response<T>> createTask(final String name,
                                          final Request<T> request,
                                          final RequestContext requestContext)
  {
    // simple wrapper around promise interface
    // the callback's purpose is to delay the actual request
    return new RestLiCallable<T>(name, request, requestContext);
  }

  private class RestLiCallable<T> extends BaseTask<Response<T>>
  {
    private final Request<T>     _request;
    private final RequestContext _requestContext;

    public RestLiCallable(final String name,
                          final Request<T> request,
                          final RequestContext requestContext)
    {
      super(name);
      this._request = request;
      this._requestContext = requestContext;
    }

    @Override
    protected Promise<? extends Response<T>> run(final Context context) throws Exception
    {
      return sendRequest(_request, _requestContext);
    }
  }
}
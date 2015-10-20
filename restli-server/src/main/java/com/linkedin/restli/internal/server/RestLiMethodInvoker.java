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


import com.linkedin.common.callback.Callback;
import com.linkedin.parseq.BaseTask;
import com.linkedin.parseq.Context;
import com.linkedin.parseq.Engine;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.PromiseListener;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.internal.server.filter.FilterRequestContextInternal;
import com.linkedin.restli.internal.server.filter.RestLiRequestFilterChain;
import com.linkedin.restli.internal.server.methods.MethodAdapterRegistry;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.internal.server.methods.response.ErrorResponseBuilder;
import com.linkedin.restli.internal.server.model.Parameter.ParamType;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.util.RestUtils;
import com.linkedin.restli.server.RequestExecutionCallback;
import com.linkedin.restli.server.RequestExecutionReport;
import com.linkedin.restli.server.RequestExecutionReportBuilder;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.filter.RequestFilter;
import com.linkedin.restli.internal.server.filter.RestLiRequestFilterChainCallback;
import com.linkedin.restli.server.resources.BaseResource;
import com.linkedin.restli.server.resources.ResourceFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * Invokes a resource method, binding contextual and URI-derived arguments to method
 * parameters.
 *
 * @author dellamag
 */
public class RestLiMethodInvoker
{
  private final ResourceFactory _resourceFactory;
  private final Engine _engine;
  private final ErrorResponseBuilder _errorResponseBuilder;
  private final MethodAdapterRegistry _methodAdapterRegistry;
  private final List<RequestFilter> _requestFilters;

  /**
   * Constructor.
   *
   * @param resourceFactory {@link ResourceFactory}
   * @param engine {@link Engine}
   */
  public RestLiMethodInvoker(final ResourceFactory resourceFactory, final Engine engine)
  {
    this(resourceFactory, engine, new ErrorResponseBuilder());
  }

  /**
   * Constructor.
   *
   * @param resourceFactory {@link ResourceFactory}
   * @param engine {@link Engine}
   * @param errorResponseBuilder {@link ErrorResponseBuilder}
   */
  public RestLiMethodInvoker(final ResourceFactory resourceFactory, final Engine engine, final ErrorResponseBuilder errorResponseBuilder)
  {
    this(resourceFactory, engine, errorResponseBuilder, new ArrayList<RequestFilter>());
  }

  /**
   * Constructor.
   *
   * @param resourceFactory {@link ResourceFactory}
   * @param engine {@link Engine}
   * @param errorResponseBuilder {@link ErrorResponseBuilder}
   * @param requestFilters List of {@link RequestFilter}
   */
  public RestLiMethodInvoker(final ResourceFactory resourceFactory, final Engine engine, final ErrorResponseBuilder errorResponseBuilder, final List<RequestFilter> requestFilters)
  {
    this(resourceFactory, engine, errorResponseBuilder, new MethodAdapterRegistry(errorResponseBuilder), requestFilters);
  }

  /**
   * Constructor.
   * @param resourceFactory {@link ResourceFactory}
   * @param engine {@link Engine}
   * @param errorResponseBuilder {@link ErrorResponseBuilder}
   * @param methodAdapterRegistry {@link MethodAdapterRegistry}
   * @param requestFilters List of {@link RequestFilter}
   */
  public RestLiMethodInvoker(final ResourceFactory resourceFactory,
                             final Engine engine,
                             final ErrorResponseBuilder errorResponseBuilder,
                             final MethodAdapterRegistry methodAdapterRegistry,
                             final List<RequestFilter> requestFilters)
  {
    _resourceFactory = resourceFactory;
    _engine = engine;
    _errorResponseBuilder = errorResponseBuilder;
    _methodAdapterRegistry = methodAdapterRegistry;
    if (requestFilters != null)
    {
      _requestFilters = requestFilters;
    }
    else
    {
      _requestFilters = new ArrayList<RequestFilter>();
    }
  }

  /**
   * Invokes the method with the specified callback and arguments built from the request.
   *
   * @param invocableMethod
   *          {@link RoutingResult}
   * @param request
   *          {@link RestRequest}
   * @param callback
   *          {@link RestLiCallback}
   * @param isDebugMode
   *          whether the invocation will be done as part of a debug request.
   * @param filterContext
   *          {@link FilterRequestContextInternal}
   */
  public void invoke(final RoutingResult invocableMethod,
                     final RestRequest request,
                     final RequestExecutionCallback<Object> callback,
                     final boolean isDebugMode,
                     final FilterRequestContextInternal filterContext)
  {
    RequestExecutionReportBuilder requestExecutionReportBuilder = null;

    if (isDebugMode)
    {
      requestExecutionReportBuilder = new RequestExecutionReportBuilder();
    }

    // Fast fail if the request headers are invalid.
    try
    {
      RestUtils.validateRequestHeadersAndUpdateResourceContext(request.getHeaders(),
                                                               (ServerResourceContext) invocableMethod.getContext());
    }
    catch (RestLiServiceException e)
    {
      callback.onError(e, getRequestExecutionReport(requestExecutionReportBuilder));
      return;
    }
    // Request headers are valid. Proceed with the invocation of the filters and eventually the resource.
    ResourceMethodDescriptor resourceMethodDescriptor =
        invocableMethod.getResourceMethod();

    RestLiArgumentBuilder adapter =
        _methodAdapterRegistry.getArgumentBuilder(resourceMethodDescriptor.getType());
    if (adapter == null)
    {
      throw new IllegalArgumentException("Unsupported method type: "
          + resourceMethodDescriptor.getType());
    }
    RestLiRequestData requestData = adapter.extractRequestData(invocableMethod, request);
    filterContext.setRequestData(requestData);
    // Kick off the request filter iterator, which finally would invoke the resource.
    RestLiRequestFilterChainCallback restLiRequestFilterChainCallback = new RestLiRequestFilterChainCallbackImpl(
        invocableMethod,
        adapter,
        callback,
        requestExecutionReportBuilder);
    new RestLiRequestFilterChain(_requestFilters, restLiRequestFilterChainCallback).onRequest(filterContext);
  }

  @SuppressWarnings("deprecation")
  private void doInvoke(final ResourceMethodDescriptor descriptor,
                        final RequestExecutionCallback<Object> callback,
                        final RequestExecutionReportBuilder requestExecutionReportBuilder,
                        final Object resource,
                        final Object... arguments) throws IllegalAccessException
  {
    Method method = descriptor.getMethod();

    try
    {
      switch (descriptor.getInterfaceType())
      {
      case CALLBACK:
        int callbackIndex = descriptor.indexOfParameterType(ParamType.CALLBACK);
        final RequestExecutionReport executionReport = getRequestExecutionReport(requestExecutionReportBuilder);

        //Delegate the callback call to the request execution callback along with the
        //request execution report.
        arguments[callbackIndex] = new Callback<Object>(){
          @Override
          public void onError(Throwable e)
          {
            callback.onError(e instanceof RestLiServiceException ? e : new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, e), executionReport);
          }

          @Override
          public void onSuccess(Object result)
          {
            callback.onSuccess(result, executionReport);
          }
        };

        method.invoke(resource, arguments);
        // App code should use the callback
        break;

      case SYNC:
        Object applicationResult = method.invoke(resource, arguments);
        callback.onSuccess(applicationResult, getRequestExecutionReport(requestExecutionReportBuilder));
        break;

      case PROMISE:
        if (!checkEngine(callback, descriptor, requestExecutionReportBuilder))
        {
          break;
        }
        int contextIndex = descriptor.indexOfParameterType(ParamType.PARSEQ_CONTEXT_PARAM);

        if (contextIndex == -1)
        {
          contextIndex = descriptor.indexOfParameterType(ParamType.PARSEQ_CONTEXT);
        }
        // run through the engine to get the context
        Task<Object> restliTask =
            new RestLiParSeqTask(arguments, contextIndex, method, resource);

        // propagate the result to the callback
        restliTask.addListener(new CallbackPromiseAdapter<Object>(callback, restliTask, requestExecutionReportBuilder));
        _engine.run(restliTask);
        break;

      case TASK:
        if (!checkEngine(callback, descriptor, requestExecutionReportBuilder))
        {
          break;
        }

        //addListener requires Task<Object> in this case
        @SuppressWarnings("unchecked")
        Task<Object> task = (Task<Object>) method.invoke(resource, arguments);
        if (task == null)
        {
          callback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                           "Error in application code: null Task"),
                              getRequestExecutionReport(requestExecutionReportBuilder));
        }
        else
        {
          task.addListener(new CallbackPromiseAdapter<Object>(callback, task, requestExecutionReportBuilder));
          _engine.run(task);
        }
        break;

      default:
          throw new AssertionError("Unexpected interface type "
              + descriptor.getInterfaceType());
      }
    }
    catch (InvocationTargetException e)
    {
      // Method runtime exceptions ar expected to fail with a top level
      // InvocationTargetException wrapped around the root cause.
      if (RestLiServiceException.class.isAssignableFrom(e.getCause().getClass()))
      {
        RestLiServiceException restLiServiceException =
            (RestLiServiceException) e.getCause();
        callback.onError(restLiServiceException, getRequestExecutionReport(requestExecutionReportBuilder));
      }
      else
      {
        callback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                       _errorResponseBuilder.getInternalErrorMessage(),
                                                       e.getCause()),
                            getRequestExecutionReport(requestExecutionReportBuilder));
      }
    }
  }

  private boolean checkEngine(final RequestExecutionCallback<Object> callback,
                              final ResourceMethodDescriptor desc,
                              final RequestExecutionReportBuilder executionReportBuilder)
  {
    if (_engine == null)
    {
      final String fmt =
          "ParSeq based method %s.%s, but no engine given. "
              + "Check your RestLiServer construction, spring wiring, "
              + "and container-pegasus-restli-server-cmpt version.";
      final String clazz = desc.getResourceModel().getResourceClass().getName();
      final String method = desc.getMethod().getName();
      final String msg = String.format(fmt, clazz, method);
      callback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                  msg),
                       getRequestExecutionReport(executionReportBuilder));
      return false;
    }
    else
    {
      return true;
    }
  }

  private static RequestExecutionReport getRequestExecutionReport(
      RequestExecutionReportBuilder requestExecutionReportBuilder)
  {
    return requestExecutionReportBuilder == null ? null : requestExecutionReportBuilder.build();
  }

  /**
   * A concrete implementation of {@link RestLiRequestFilterChainCallback}.
   */
  private class RestLiRequestFilterChainCallbackImpl implements RestLiRequestFilterChainCallback
  {
    private RoutingResult _invocableMethod;
    private RestLiArgumentBuilder _restLiArgumentBuilder;
    private RequestExecutionCallback<Object> _callback;
    private RequestExecutionReportBuilder _requestExecutionReportBuilder;

    public RestLiRequestFilterChainCallbackImpl(final RoutingResult invocableMethod,
                                                final RestLiArgumentBuilder restLiArgumentBuilder,
                                                final RequestExecutionCallback<Object> callback,
                                                final RequestExecutionReportBuilder requestExecutionReportBuilder)
    {
      _invocableMethod = invocableMethod;
      _restLiArgumentBuilder = restLiArgumentBuilder;
      _callback = callback;
      _requestExecutionReportBuilder = requestExecutionReportBuilder;
    }

    @Override
    public void onError(Throwable throwable)
    {
      _callback.onError(throwable,
                        _requestExecutionReportBuilder == null ? null : _requestExecutionReportBuilder.build());
    }

    @Override
    public void onSuccess(RestLiRequestData requestData)
    {
      try
      {
        ResourceMethodDescriptor resourceMethodDescriptor = _invocableMethod.getResourceMethod();
        Object resource = _resourceFactory.create(resourceMethodDescriptor.getResourceModel().getResourceClass());
        if (BaseResource.class.isAssignableFrom(resource.getClass()))
        {
          ((BaseResource) resource).setContext(_invocableMethod.getContext());
        }
        Object[] args = _restLiArgumentBuilder.buildArguments(requestData, _invocableMethod);
        // Now invoke the resource implementation.
        doInvoke(resourceMethodDescriptor, _callback, _requestExecutionReportBuilder, resource, args);
      }
      catch (Exception e)
      {
        _callback.onError(e, _requestExecutionReportBuilder == null ? null : _requestExecutionReportBuilder.build());
      }
    }
  }

  /**
   * ParSeq task that supplies a context to the resource class method.
   *
   * @author jnwang
   */
  private static class RestLiParSeqTask extends BaseTask<Object> {
    private final Object[] _arguments;
    private final int _contextIndex;
    private final Method _method;
    private final Object _resource;

    public RestLiParSeqTask(final Object[] arguments,
                            final int contextIndex,
                            final Method method,
                            final Object resource)
    {
      this._arguments = arguments;
      this._contextIndex = contextIndex;
      this._method = method;
      this._resource = resource;
    }

    @Override
    protected Promise<?> run(final Context context)
    {
      try
      {
        if (_contextIndex != -1)
        {
          // we can now supply the context
          _arguments[_contextIndex] = context;
        }
        Object applicationResult = _method.invoke(_resource, _arguments);
        if (applicationResult == null)
        {
          return Promises.error(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                           "Error in application code: null Promise"));
        }
        // TODO Should we guard against incorrectly returning a task that has no way of
        // starting?
        return (Promise<?>) applicationResult;
      }
      catch (Throwable t)
      {
        // Method runtime exceptions ar expected to fail with a top level
        // InvocationTargetException wrapped around the root cause.
        if (t instanceof InvocationTargetException && t.getCause() != null)
        {
          // Unbury the exception thrown from the resource method if it's there.
          return Promises.error(t.getCause() instanceof RestLiServiceException ?
                                  t.getCause() : new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, t.getCause()));
        }

        return Promises.error(t instanceof RestLiServiceException ? t : new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, t));
      }
    }
  }

  /**
   * Propagate promise results to a callback.
   *
   * @author jnwang
   */
  private static class CallbackPromiseAdapter<T> implements PromiseListener<T>
  {
    private final RequestExecutionCallback<T> _callback;
    private final RequestExecutionReportBuilder _executionReportBuilder;
    private final Task<T> _associatedTask;

    public CallbackPromiseAdapter(final RequestExecutionCallback<T> callback,
                                  final Task<T> associatedTask,
                                  final RequestExecutionReportBuilder executionReportBuilder)
    {
      _callback = callback;
      _associatedTask = associatedTask;
      _executionReportBuilder = executionReportBuilder;
    }

    @Override
    public void onResolved(final Promise<T> promise)
    {
      if (_executionReportBuilder != null)
      {
        _executionReportBuilder.setParseqTrace(_associatedTask.getTrace());
      }

      RequestExecutionReport executionReport = getRequestExecutionReport(_executionReportBuilder);

      if (promise.isFailed())
      {
        _callback.onError(promise.getError() instanceof RestLiServiceException ?
                            promise.getError() : new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, promise.getError()),
                          executionReport);
      }
      else
      {
        _callback.onSuccess(promise.get(), executionReport);
      }
    }
  }
}
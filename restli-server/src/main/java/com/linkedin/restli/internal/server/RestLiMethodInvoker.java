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
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.UnstructuredDataReactiveResult;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.internal.server.model.Parameter.ParamType;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.response.ErrorResponseBuilder;
import com.linkedin.restli.server.RequestExecutionCallback;
import com.linkedin.restli.server.RequestExecutionReportBuilder;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiResponseAttachments;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.resources.BaseResource;
import com.linkedin.restli.server.resources.ResourceFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


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

  // This ThreadLocal stores Context of task that is currently being executed.
  // When it is set, new tasks do not start new plans but instead are scheduled
  // with the Context.
  // This mechanism is used to process a MultiplexedRequest within single plan and
  // allow optimizations e.g. automatic batching.
  public static final ThreadLocal<Context> TASK_CONTEXT = new ThreadLocal<>();

  /**
   * Constructor.
   * @param resourceFactory {@link ResourceFactory}
   * @param engine {@link Engine}
   * @param errorResponseBuilder {@link ErrorResponseBuilder}
   */
  public RestLiMethodInvoker(final ResourceFactory resourceFactory,
                             final Engine engine,
                             final ErrorResponseBuilder errorResponseBuilder)
  {
    _resourceFactory = resourceFactory;
    _engine = engine;
    _errorResponseBuilder = errorResponseBuilder;
  }

  @SuppressWarnings("deprecation")
  private void doInvoke(final ResourceMethodDescriptor descriptor,
                        final RestLiCallback callback,
                        final RequestExecutionReportBuilder requestExecutionReportBuilder,
                        final Object resource,
                        final ServerResourceContext resourceContext,
                        final Object... arguments) throws IllegalAccessException
  {
    final Method method = descriptor.getMethod();

    try
    {
      switch (descriptor.getInterfaceType())
      {
        case CALLBACK:
          int callbackIndex = descriptor.indexOfParameterType(ParamType.CALLBACK);

          arguments[callbackIndex] = new Callback<Object>(){
            @Override
            public void onError(Throwable e)
            {
              callback.onError(e instanceof RestLiServiceException ? e : new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, e),
                  resourceContext.getResponseAttachments());
            }

            @Override
            public void onSuccess(Object result)
            {
              if (result instanceof UnstructuredDataReactiveResult)
              {
                // TODO Refactor this to stop piggybacking on RestLiResponseAttachments and using dummy EmptyRecord
                RestLiResponseAttachments.Builder builder = new RestLiResponseAttachments.Builder();
                UnstructuredDataReactiveResult reactiveResult = (UnstructuredDataReactiveResult) result;
                RestLiResponseAttachments responseAttachments = builder.appendUnstructuredDataReactiveResult(reactiveResult).build();
                callback.onSuccess(new EmptyRecord(), responseAttachments);
              }
              else
              {
                callback.onSuccess(result, resourceContext.getResponseAttachments());
              }
            }
          };

          method.invoke(resource, arguments);
          // App code should use the callback
          break;

        case SYNC:
          Object applicationResult = method.invoke(resource, arguments);
          callback.onSuccess(applicationResult,
                             resourceContext.getResponseAttachments());
          break;

        case PROMISE:
          if (!checkEngine(callback, descriptor))
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
          restliTask.addListener(new CallbackPromiseAdapter<>(callback, restliTask, requestExecutionReportBuilder,
                                                              resourceContext.getResponseAttachments()));
          runTask(restliTask, toPlanClass(descriptor));
          break;

        case TASK:
          if (!checkEngine(callback, descriptor))
          {
            break;
          }

          //addListener requires Task<Object> in this case
          @SuppressWarnings("unchecked")
          Task<Object> task = (Task<Object>) method.invoke(resource, arguments);
          if (task == null)
          {
            callback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                        "Error in application code: null Task"), resourceContext.getResponseAttachments());
          }
          else
          {
            task.addListener(new CallbackPromiseAdapter<>(callback, task, requestExecutionReportBuilder,
                                                          resourceContext.getResponseAttachments()));
            runTask(task, toPlanClass(descriptor));
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
        callback.onError(restLiServiceException, resourceContext.getResponseAttachments());
      }
      else
      {
        callback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                    _errorResponseBuilder.getInternalErrorMessage(),
                                                    e.getCause()), resourceContext.getResponseAttachments());
      }
    }
  }

  private String toPlanClass(ResourceMethodDescriptor descriptor)
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("resource=").append(descriptor.getResourceName());
    sb.append(",");
    sb.append("method=").append(descriptor.getType());
    if (descriptor.getFinderName() != null)
    {
      sb.append(",").append("finder=").append(descriptor.getFinderName());
    }
    if (descriptor.getActionName() != null)
    {
      sb.append(",").append("action=").append(descriptor.getActionName());
    }
    return sb.toString();
  }

  private void runTask(Task<Object> task, String planClass)
  {
    Context taskContext = TASK_CONTEXT.get();
    if (taskContext == null)
    {
      _engine.run(task, planClass);
    }
    else
    {
      taskContext.run(task);
    }
  }

  private boolean checkEngine(final RestLiCallback callback, final ResourceMethodDescriptor desc)
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
      callback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, msg), null); //No response attachments can possibly exist here, since the resource method has not been invoked.
      return false;
    }
    else
    {
      return true;
    }
  }

  /**
   * Invokes the method with the specified callback and arguments built from the request.
   *
   * @param requestData
   *          {@link RestLiRequestData}
   * @param invocableMethod
   *          {@link RoutingResult}
   * @param restLiArgumentBuilder
   *          {@link RestLiArgumentBuilder}
   * @param callback
   *          {@link RequestExecutionCallback}
   * @param requestExecutionReportBuilder
   *          {@link RequestExecutionReportBuilder}
   */
  public void invoke(final RestLiRequestData requestData,
                     final RoutingResult invocableMethod,
                     final RestLiArgumentBuilder restLiArgumentBuilder,
                     final RestLiCallback callback,
                     final RequestExecutionReportBuilder requestExecutionReportBuilder)
  {
    try
    {
      ResourceMethodDescriptor resourceMethodDescriptor = invocableMethod.getResourceMethod();
      Object resource = _resourceFactory.create(resourceMethodDescriptor.getResourceModel().getResourceClass());

      //Acquire a handle on the ResourceContext when setting it in order to obtain any response attachments that need to
      //be streamed back.
      final ServerResourceContext resourceContext;
      resourceContext = (ServerResourceContext)invocableMethod.getContext();
      if (BaseResource.class.isAssignableFrom(resource.getClass()))
      {
        ((BaseResource) resource).setContext(resourceContext);
      }

      Object[] args = restLiArgumentBuilder.buildArguments(requestData, invocableMethod);
      // Now invoke the resource implementation.
      doInvoke(resourceMethodDescriptor, callback, requestExecutionReportBuilder, resource, resourceContext, args);
    }
    catch (Exception e)
    {
      callback.onError(e, invocableMethod.getContext().getResponseAttachments()); //Technically response attachments
      //could exist here. One possible way is if there is a runtime exception during response
      //construction after the rest.li response filter chain has been completed.
    }
  }

  /**
   * ParSeq task that supplies a context to the resource class method.
   *
   * @author jnwang
   */
  private static class RestLiParSeqTask extends BaseTask<Object>
  {
    private final Object[] _arguments;
    private final int _contextIndex;
    private final Method _method;
    private final Object _resource;

    RestLiParSeqTask(final Object[] arguments,
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
    private final RestLiCallback _callback;
    private final RequestExecutionReportBuilder _executionReportBuilder;
    private final RestLiResponseAttachments _responseAttachments;
    private final Task<T> _associatedTask;

    CallbackPromiseAdapter(final RestLiCallback callback,
        final Task<T> associatedTask,
        final RequestExecutionReportBuilder executionReportBuilder,
        final RestLiResponseAttachments responseAttachments)
    {
      _callback = callback;
      _associatedTask = associatedTask;
      _executionReportBuilder = executionReportBuilder;
      _responseAttachments = responseAttachments;
    }

    @Override
    public void onResolved(final Promise<T> promise)
    {
      if (_executionReportBuilder != null)
      {
        _executionReportBuilder.setParseqTrace(_associatedTask.getTrace());
      }

      if (promise.isFailed())
      {
        _callback.onError(promise.getError() instanceof RestLiServiceException ?
                              promise.getError() : new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, promise.getError()),
            _responseAttachments);
      }
      else
      {
        _callback.onSuccess(promise.get(), _responseAttachments);
      }
    }
  }
}

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
import com.linkedin.parseq.Context;
import com.linkedin.parseq.Engine;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.PromiseListener;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.restli.common.ConfigValue;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.internal.server.model.Parameter.ParamType;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.NonResourceRequestHandler;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiRequestData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.UnstructuredDataReactiveResult;
import com.linkedin.restli.server.config.ResourceMethodConfig;
import com.linkedin.restli.server.resources.BaseResource;
import com.linkedin.restli.server.resources.ResourceFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;


/**
 * Invokes a resource method, binding contextual and URI-derived arguments to method
 * parameters.
 *
 * @author dellamag
 */
public class RestLiMethodInvoker
{
  /**
   * Through a local attribute in RequestContext, application customization like filters and
   * {@link NonResourceRequestHandler} can provide a PromiseListener to be registered to the
   * ParSeq execution task provided by the resource.
   *
   * This feature is internal and is only used by ParseqTraceDebugRequestHandler.
   */
  public static final String ATTRIBUTE_PROMISE_LISTENER = RestLiMethodInvoker.class.getCanonicalName() + ".promiseListener";

  private final ResourceFactory _resourceFactory;
  private final Engine _engine;
  private final String _internalErrorMessage;

  // This ThreadLocal stores Context of task that is currently being executed.
  // When it is set, new tasks do not start new plans but instead are scheduled
  // with the Context.
  // This mechanism is used to process a MultiplexedRequest within single plan and
  // allow optimizations e.g. automatic batching.
  public static final ThreadLocal<Context> TASK_CONTEXT = new ThreadLocal<>();

  public RestLiMethodInvoker(final ResourceFactory resourceFactory,
                             final Engine engine,
                             final String internalErrorMessage)
  {
    _resourceFactory = resourceFactory;
    _engine = engine;
    _internalErrorMessage = internalErrorMessage;
  }

  @SuppressWarnings("deprecation")
  private void doInvoke(final ResourceMethodDescriptor descriptor,
      final ResourceMethodConfig methodConfig,
      final RestLiCallback callback,
      final Object resource,
      final ServerResourceContext resourceContext,
      final Object... arguments) throws IllegalAccessException
  {
    final Method method = descriptor.getMethod();

    final RequestContext requestContext = resourceContext.getRawRequestContext();
    TimingContextUtil.endTiming(requestContext, FrameworkTimingKeys.SERVER_REQUEST_RESTLI.key());
    TimingContextUtil.endTiming(requestContext, FrameworkTimingKeys.SERVER_REQUEST.key());
    TimingContextUtil.beginTiming(requestContext, FrameworkTimingKeys.RESOURCE.key());

    try
    {
      switch (descriptor.getInterfaceType())
      {
        case CALLBACK:
          int callbackIndex = descriptor.indexOfParameterType(ParamType.CALLBACK);

          arguments[callbackIndex] = new Callback<Object>()
          {
            @Override
            public void onError(Throwable e)
            {
              callback.onError(e instanceof RestLiServiceException ? e : new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, e));
            }

            @Override
            public void onSuccess(Object result)
            {
              if (result instanceof UnstructuredDataReactiveResult)
              {
                UnstructuredDataReactiveResult reactiveResult = (UnstructuredDataReactiveResult) result;
                resourceContext.setResponseEntityStream(reactiveResult.getEntityStream());
                resourceContext.setResponseHeader(RestConstants.HEADER_CONTENT_TYPE, reactiveResult.getContentType());
                callback.onSuccess(new EmptyRecord());
              }
              else
              {
                callback.onSuccess(result);
              }
            }
          };

          method.invoke(resource, arguments);
          // App code should use the callback
          break;

        case SYNC:
          Object applicationResult = method.invoke(resource, arguments);
          callback.onSuccess(applicationResult);
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
          Task<Object> restliTask = withTimeout(createRestLiParSeqTask(arguments, contextIndex, method, resource),
                  methodConfig);

          // propagate the result to the callback
          restliTask.addListener(new CallbackPromiseAdapter<>(callback));
          addListenerFromContext(restliTask, resourceContext);

          runTask(restliTask, toPlanClass(descriptor));
          break;

        case TASK:
          if (!checkEngine(callback, descriptor))
          {
            break;
          }

          //addListener requires Task<Object> in this case
          @SuppressWarnings("unchecked")
          Task<Object> task = withTimeout((Task<Object>) method.invoke(resource, arguments),
                  methodConfig);
          if (task == null)
          {
            callback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                        "Error in application code: null Task"));
          }
          else
          {
            task.addListener(new CallbackPromiseAdapter<>(callback));
            addListenerFromContext(task, resourceContext);
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
        callback.onError(restLiServiceException);
      }
      else
      {
        callback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                    _internalErrorMessage,
                                                    e.getCause()));
      }
    }
  }

  // Apply timeout to parseq task if timeout configuration is specified for this method.
  private Task<Object> withTimeout(final Task<Object> task, ResourceMethodConfig config)
  {
    if (config != null)
    {
      ConfigValue<Long> timeout = config.getTimeoutMs();
      if (timeout != null && timeout.getValue() != null && timeout.getValue() > 0)
      {
        if (timeout.getSource().isPresent())
        {
          return task.withTimeout("src: " + timeout.getSource().get(), timeout.getValue(), TimeUnit.MILLISECONDS);
        }
        else
        {
          return task.withTimeout(timeout.getValue(), TimeUnit.MILLISECONDS);
        }
      }
    }
    return task;
  }

  private void addListenerFromContext(Task<Object> task, ResourceContext resourceContext)
  {
    @SuppressWarnings("unchecked")
    PromiseListener<Object> listener =
        (PromiseListener<Object>) resourceContext.getRawRequestContext().getLocalAttr(ATTRIBUTE_PROMISE_LISTENER);
    if (listener != null)
    {
      task.addListener(new PromiseListener<Object>()
      {
        @Override
        public void onResolved(Promise<Object> promise)
        {
          // ParSeq engine doesn't guarantee that the Promise passed in is the task object this listener attached to.
          // The original listener's business logic may depend on the task. We need this intermediate listener to relay
          // the task to the original listener.
          listener.onResolved(task);
        }
      });
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
      callback.onError(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, msg)); //No response attachments can possibly exist here, since the resource method has not been invoked.
      return false;
    }
    else
    {
      return true;
    }
  }

  /**
   * Invokes the method with the specified callback and arguments built from the request.
   */
  public void invoke(final RestLiRequestData requestData,
      final RoutingResult invokableMethod,
      final RestLiArgumentBuilder restLiArgumentBuilder,
      final RestLiCallback callback)
  {
    try
    {
      ResourceMethodDescriptor resourceMethodDescriptor = invokableMethod.getResourceMethod();
      ResourceMethodConfig resourceMethodConfig = invokableMethod.getResourceMethodConfig();
      Object resource = _resourceFactory.create(resourceMethodDescriptor.getResourceModel().getResourceClass());

      // Acquire a handle on the ResourceContext when setting it in order to obtain any response attachments that need to
      // be streamed back.
      final ServerResourceContext resourceContext = invokableMethod.getContext();
      if (BaseResource.class.isAssignableFrom(resource.getClass()))
      {
        ((BaseResource) resource).setContext(resourceContext);
      }

      Object[] args = restLiArgumentBuilder.buildArguments(requestData, invokableMethod);
      // Now invoke the resource implementation.
      doInvoke(resourceMethodDescriptor, resourceMethodConfig, callback, resource, resourceContext, args);
    }
    catch (Exception e)
    {
      callback.onError(e);
    }
  }

  /**
   * Creates a ParSeq task that supplies a context to the resource class method.
   */
  private static Task<Object> createRestLiParSeqTask(final Object[] arguments,
      final int contextIndex,
      final Method method,
      final Object resource)
  {
    return Task.async(context ->
    {
      try
      {
        if (contextIndex != -1)
        {
          // we can now supply the context
          arguments[contextIndex] = context;
        }
        Object applicationResult = method.invoke(resource, arguments);
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
          // Unwrap the exception thrown from the resource method if it's there.
          return Promises.error(t.getCause() instanceof RestLiServiceException ?
              t.getCause() : new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, t.getCause()));
        }

        return Promises.error(t instanceof RestLiServiceException ? t : new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, t));
      }
    });
  }

  /**
   * Propagate promise results to a callback.
   *
   * @author jnwang
   */
  private static class CallbackPromiseAdapter<T> implements PromiseListener<T>
  {
    private final RestLiCallback _callback;

    CallbackPromiseAdapter(final RestLiCallback callback)
    {
      _callback = callback;
    }

    @Override
    public void onResolved(final Promise<T> promise)
    {
      if (promise.isFailed())
      {
        _callback.onError(promise.getError() instanceof RestLiServiceException ?
                              promise.getError() : new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, promise.getError()));
      }
      else
      {
        _callback.onSuccess(promise.get());
      }
    }
  }
}

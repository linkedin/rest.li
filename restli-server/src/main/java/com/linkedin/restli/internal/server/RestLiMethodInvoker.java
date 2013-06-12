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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
import com.linkedin.restli.internal.server.methods.MethodAdapterRegistry;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.internal.server.model.Parameter.ParamType;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.RestLiCallback;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.resources.BaseResource;
import com.linkedin.restli.server.resources.ResourceFactory;

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

  /**
   * Constructor.
   *
   * @param resourceFactory {@link ResourceFactory}
   * @param engine {@link Engine}
   */
  public RestLiMethodInvoker(final ResourceFactory resourceFactory, final Engine engine)
  {
    _resourceFactory = resourceFactory;
    _engine = engine;
  }

  /**
   * Invokes the method with the specified callback and arguments built from the request.
   *
   * @param invocableMethod {@link RoutingResult}
   * @param request {@link RestRequest}
   * @param callback {@link RestLiCallback}
   */
  public void invoke(final RoutingResult invocableMethod,
                     final RestRequest request,
                     final RestLiCallback<Object> callback)
  {
    ResourceMethodDescriptor resourceMethodDescriptor =
        invocableMethod.getResourceMethod();

    Object resource =
        _resourceFactory.create(resourceMethodDescriptor.getResourceModel()
                                                        .getResourceClass());

    if (BaseResource.class.isAssignableFrom(resource.getClass()))
    {
      ((BaseResource) resource).setContext(invocableMethod.getContext());
    }

    RestLiArgumentBuilder adapter =
        MethodAdapterRegistry.getArgumentBuilder(resourceMethodDescriptor.getType());
    if (adapter == null)
    {
      throw new IllegalArgumentException("Unsupported method type: "
          + resourceMethodDescriptor.getType());
    }

    Object[] args = adapter.buildArguments(invocableMethod, request);

    try
    {
      doInvoke(resourceMethodDescriptor, callback, resource, args);
    }
    catch (IllegalAccessException e)
    {
      throw new RuntimeException(e);
    }
  }

  private void doInvoke(final ResourceMethodDescriptor descriptor,
                        final RestLiCallback<Object> callback,
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
        arguments[callbackIndex] = callback;
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
        final int contextIndex =
            descriptor.indexOfParameterType(ParamType.PARSEQ_CONTEXT);
        // run through the engine to get the context
        Task<Object> restliTask =
            new RestLiParSeqTask(arguments, contextIndex, method, resource);

        // propagate the result to the callback
        restliTask.addListener(new CallbackPromiseAdapter<Object>(callback));
        _engine.run(restliTask);
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
            callback.onErrorApp(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                           "Error in application code: null Task"));
        }
        else
        {
          task.addListener(new CallbackPromiseAdapter<Object>(callback));
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
      if (RestLiServiceException.class.isAssignableFrom(e.getCause().getClass()))
      {
        RestLiServiceException restLiServiceException =
            (RestLiServiceException) e.getCause();
        callback.onErrorApp(restLiServiceException);
      }
      else
      {
        callback.onErrorApp(new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                                                       "Error in application code",
                                                       e.getCause()));
      }
    }
  }

  private boolean checkEngine(final Callback<Object> callback,
                              final ResourceMethodDescriptor desc)
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
                                                  msg));
      return false;
    }
    else
    {
      return true;
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
        return Promises.error(t);
      }
    }
  }

  /**
   * Propagate promise results to a callback.
   *
   * @author jnwang
   */
  private static class CallbackPromiseAdapter<T> implements PromiseListener<T> {
    private final Callback<T> _callback;

    public CallbackPromiseAdapter(final Callback<T> callback)
    {
      _callback = callback;
    }

    @Override
    public void onResolved(final Promise<T> promise)
    {
      if (promise.isFailed())
      {
        _callback.onError(promise.getError());
      }
      else
      {
        _callback.onSuccess(promise.get());
      }
    }
  }
}

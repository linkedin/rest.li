/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.test.util;


import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.RequestBuilder;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.OptionsResponse;
import com.linkedin.restli.common.PatchRequest;
import com.linkedin.restli.common.UpdateStatus;
import com.linkedin.restli.internal.tools.RestLiToolsUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * Wrapper of the root request builders for testing purpose.
 *
 * This wrapper allows same test logic to be used on both Rest.li 2 and legacy request builders
 * that are generated for same resource.
 *
 * For the wrapper of method specific builder, check {@link MethodBuilderWrapper}.
 *
 * @author Keren Jin
 */
public class RootBuilderWrapper<K, V extends RecordTemplate>
{
  private final Object _rootBuilder;
  private final Class<V> _valueClass;

  /**
   * Wrapper of the method specific request builders for testing purpose.
   *
   * For the wrapper of method specific builder, check {@link com.linkedin.restli.test.util.RootBuilderWrapper}.
   *
   * @author Keren Jin
   */
  public static class MethodBuilderWrapper<K, V extends RecordTemplate, R>
  {
    private final RequestBuilder<? extends Request<R>> _methodBuilder;
    private final boolean _isRestLi2Builder;
    private final Class<V> _valueClass;

    public MethodBuilderWrapper(RequestBuilder<? extends Request<R>> builder, boolean isRestLi2Builder, Class<V> valueClass)
    {
      _methodBuilder = builder;
      _isRestLi2Builder = isRestLi2Builder;
      _valueClass = valueClass;
    }

    public Request<R> build()
    {
      try
      {
        @SuppressWarnings("unchecked")
        final Request<R> request = (Request<R>) getMethod("build").invoke(_methodBuilder);
        return request;
      }
      catch (IllegalAccessException e)
      {
        throw new RuntimeException(e);
      }
      catch (InvocationTargetException e)
      {
        throw handleInvocationTargetException(e);
      }
    }

    public RequestBuilder<? extends Request<R>> getBuilder()
    {
      return _methodBuilder;
    }

    public MethodBuilderWrapper<K, V, R> id(K id)
    {
      return invoke(getMethod("id", Object.class), id);
    }

    public MethodBuilderWrapper<K, V, R> ids(Collection<K> ids)
    {
      return invoke(getMethod("ids", Collection.class), ids);
    }

    @SuppressWarnings("unchecked")
    public MethodBuilderWrapper<K, V, R> ids(K... ids)
    {
      return invoke(getMethod("ids", Object[].class), (Object) ids);
    }

    public MethodBuilderWrapper<K, V, R> input(V entity)
    {
      return invoke(getMethod("input", RecordTemplate.class), entity);
    }

    public MethodBuilderWrapper<K, V, R> input(PatchRequest<V> entity)
    {
      return invoke(getMethod("input", PatchRequest.class), entity);
    }

    public MethodBuilderWrapper<K, V, R> input(K id, V entity)
    {
      return invoke(getMethod("input", Object.class, RecordTemplate.class), id, entity);
    }

    public MethodBuilderWrapper<K, V, R> input(K id, PatchRequest<V> patch)
    {
      return invoke(getMethod("input", Object.class, PatchRequest.class), id, patch);
    }

    public MethodBuilderWrapper<K, V, R> inputs(List<V> entities)
    {
      return invoke(getMethod("inputs", List.class), entities);
    }

    public MethodBuilderWrapper<K, V, R> inputs(Map<K, V> entities)
    {
      return invoke(getMethod("inputs", Map.class), entities);
    }

    public MethodBuilderWrapper<K, V, R> patchInputs(Map<K, PatchRequest<V>> entities)
    {
      return invoke(getMethod("inputs", Map.class), entities);
    }

    public MethodBuilderWrapper<K, V, R> fields(PathSpec... fieldPaths)
    {
      return invoke(getMethod("fields", PathSpec[].class), (Object) fieldPaths);
    }

    public MethodBuilderWrapper<K, V, R> metadataFields(PathSpec... metadataFieldPaths)
    {
      return invoke(getMethod("metadataFields", PathSpec[].class), (Object) metadataFieldPaths);
    }

    public MethodBuilderWrapper<K, V, R> pagingFields(PathSpec... pagingFieldPaths)
    {
      return invoke(getMethod("pagingFields", PathSpec[].class), (Object) pagingFieldPaths);
    }

    public MethodBuilderWrapper<K, V, R> name(String name)
    {
      return invoke(getMethod("name", String.class), name);
    }

    public MethodBuilderWrapper<K, V, R> assocKey(String key, Object value)
    {
      return invoke(getMethod("assocKey", String.class, Object.class), key, value);
    }

    public MethodBuilderWrapper<K, V, R> paginate(int start, int count)
    {
      return invoke(getMethod("paginate", int.class, int.class), start, count);
    }

    public MethodBuilderWrapper<K, V, R> paginateStart(int start)
    {
      return invoke(getMethod("paginateStart", int.class), start);
    }

    public MethodBuilderWrapper<K, V, R> paginateCount(int count)
    {
      return invoke(getMethod("paginateCount", int.class), count);
    }

    public MethodBuilderWrapper<K, V, R> setHeader(String name, String value)
    {
      return invoke(getMethod("setHeader", String.class, String.class), name, value);
    }

    public MethodBuilderWrapper<K, V, R> setParam(String name, Object value)
    {
      return invoke(getMethod("setParam", String.class, Object.class), name, value);
    }

    public MethodBuilderWrapper<K, V, R> setQueryParam(String name, Object value)
    {
      final String methodName = RestLiToolsUtils.nameCamelCase(name + "Param");
      final Method method;
      if (value instanceof Iterable)
      {
        method = getMethod(methodName, Iterable.class);
      }
      else
      {
        method = findMethod(methodName, value);
      }

      return invoke(method, value);
    }

    public MethodBuilderWrapper<K, V, R> addQueryParam(String name, Object value)
    {
      return invoke(
          findMethod(
              RestLiToolsUtils.nameCamelCase("add" + RestLiToolsUtils.normalizeCaps(name) + "Param"), value), value);
    }

    public MethodBuilderWrapper<K, V, R> setActionParam(String name, Object value)
    {
      if (isRestLi2Builder())
      {
        return invoke(findMethod(RestLiToolsUtils.nameCamelCase(name + "Param"), value), value);
      }
      else
      {
        return invoke(findMethod("param" + Character.toUpperCase(name.charAt(0)) + name.substring(1), value), value);
      }
    }

    public MethodBuilderWrapper<K, V, R> setPathKey(String name, Object value)
    {
      return invoke(findMethod(RestLiToolsUtils.nameCamelCase(name + "Key"), value), value);
    }

    public ValidationResult validateInput(V entity)
    {
      try
      {
        @SuppressWarnings("unchecked")
        final ValidationResult result = (ValidationResult) getMethod("validateInput", _valueClass).invoke(_methodBuilder, entity);
        return result;
      }
      catch (IllegalAccessException e)
      {
        throw new RuntimeException(e);
      }
      catch (InvocationTargetException e)
      {
        throw handleInvocationTargetException(e);
      }
    }

    public ValidationResult validateInput(PatchRequest<V> patch)
    {
      try
      {
        @SuppressWarnings("unchecked")
        final ValidationResult result = (ValidationResult) getMethod("validateInput", PatchRequest.class).invoke(_methodBuilder, patch);
        return result;
      }
      catch (IllegalAccessException e)
      {
        throw new RuntimeException(e);
      }
      catch (InvocationTargetException e)
      {
        throw handleInvocationTargetException(e);
      }
    }

    public boolean isRestLi2Builder()
    {
      return _isRestLi2Builder;
    }

    private MethodBuilderWrapper<K, V, R> invoke(Method method, Object... args)
    {
      try
      {
        @SuppressWarnings("unchecked")
        final RequestBuilder<? extends Request<R>> builder =
            (RequestBuilder<? extends Request<R>>) method.invoke(_methodBuilder, args);
        return new MethodBuilderWrapper<K, V, R>(builder, _isRestLi2Builder, _valueClass);
      }
      catch (IllegalAccessException e)
      {
        throw new RuntimeException(e);
      }
      catch (InvocationTargetException e)
      {
        throw handleInvocationTargetException(e);
      }
    }

    private Method getMethod(String name, Class<?>... parameterTypes)
    {
      try
      {
        return _methodBuilder.getClass().getMethod(name, parameterTypes);
      }
      catch (NoSuchMethodException e)
      {
        throw new RuntimeException(e);
      }
    }

    private Method findMethod(String name, Object value)
    {
      if (value == null)
      {
        for (Method m : _methodBuilder.getClass().getMethods())
        {
          if (m.getName().equals(name) && m.getParameterTypes().length == 1)
          {
            return m;
          }
        }

        return null;
      }
      else
      {
        return getMethod(name, value.getClass());
      }
    }
  }

  public RootBuilderWrapper(Object builder)
  {
    this(builder, null);
  }

  public RootBuilderWrapper(Object builder, Class<V> valueClass)
  {
    _rootBuilder = builder;
    _valueClass = valueClass;
  }

  public Object getBuilder()
  {
    return _rootBuilder;
  }

  public MethodBuilderWrapper<K, V, V> get()
  {
    return invoke("get");
  }

  public MethodBuilderWrapper<K, V, EmptyRecord> create()
  {
    return invoke("create");
  }

  public MethodBuilderWrapper<K, V, EmptyRecord> update()
  {
    return invoke("update");
  }

  public MethodBuilderWrapper<K, V, EmptyRecord> delete()
  {
    return invoke("delete");
  }

  public MethodBuilderWrapper<K, V, EmptyRecord> partialUpdate()
  {
    return invoke("partialUpdate");
  }

  public MethodBuilderWrapper<K, V, CollectionResponse<CreateStatus>> batchCreate()
  {
    return invoke("batchCreate");
  }

  public MethodBuilderWrapper<K, V, BatchKVResponse<K, UpdateStatus>> batchPartialUpdate()
  {
    return invoke("batchPartialUpdate");
  }

  public MethodBuilderWrapper<K, V, BatchKVResponse<K, UpdateStatus>> batchUpdate()
  {
    return invoke("batchUpdate");
  }

  public MethodBuilderWrapper<K, V, BatchKVResponse<K, UpdateStatus>> batchDelete()
  {
    return invoke("batchDelete");
  }

  public MethodBuilderWrapper<K, V, CollectionResponse<V>> getAll()
  {
    return invoke("getAll");
  }

  public MethodBuilderWrapper<K, V, OptionsResponse> options()
  {
    return invoke("options");
  }

  public MethodBuilderWrapper<K, V, CollectionResponse<V>> findBy(String name)
  {
    return invoke("findBy" + capitalize(name));
  }

  public <R> MethodBuilderWrapper<K, V, R> action(String name)
  {
    return invoke("action" + capitalize(name));
  }

  public RestliRequestOptions getRequestOptions()
  {
    try
    {
      return (RestliRequestOptions) _rootBuilder.getClass().getMethod("getRequestOptions").invoke(_rootBuilder);
    }
    catch (NoSuchMethodException e)
    {
      throw new RuntimeException(e);
    }
    catch (IllegalAccessException e)
    {
      throw new RuntimeException(e);
    }
    catch (InvocationTargetException e)
    {
      throw handleInvocationTargetException(e);
    }
  }

  boolean areRestLi2Builders()
  {
    String buildersClassName = _rootBuilder.getClass().getSimpleName();

    return buildersClassName.substring(getResourceName().length()).equals("RequestBuilders");
  }

  private <R> MethodBuilderWrapper<K, V, R> invoke(String methodName)
  {
    try
    {
      @SuppressWarnings("unchecked")
      final RequestBuilder<? extends Request<R>> builder =
          (RequestBuilder<? extends Request<R>>) _rootBuilder.getClass().getMethod(methodName).invoke(_rootBuilder);
      return new MethodBuilderWrapper<K, V, R>(
          builder,
          areRestLi2Builders(),
          _valueClass);
    }
    catch (NoSuchMethodException e)
    {
      throw new RuntimeException(e);
    }
    catch (IllegalAccessException e)
    {
      throw new RuntimeException(e);
    }
    catch (InvocationTargetException e)
    {
      throw handleInvocationTargetException(e);
    }
  }

  private static RuntimeException handleInvocationTargetException(InvocationTargetException e)
  {
    final Throwable targetException = e.getTargetException();
    if (targetException instanceof RuntimeException)
    {
      return (RuntimeException) targetException;
    }
    else
    {
      return new RuntimeException(e.getTargetException());
    }
  }

  private String getResourceName()
  {
    try
    {
      final String[] pathComponents = (String[]) _rootBuilder.getClass().getMethod("getPathComponents").invoke(_rootBuilder);
      return pathComponents[pathComponents.length - 1];
    }
    catch (NoSuchMethodException e)
    {
      throw new RuntimeException(e);
    }
    catch (IllegalAccessException e)
    {
      throw new RuntimeException(e);
    }
    catch (InvocationTargetException e)
    {
      throw handleInvocationTargetException(e);
    }
  }

  private static String capitalize(String name)
  {
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }
}

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

package com.linkedin.restli.client.util;


import com.linkedin.data.ByteString;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.SetMode;
import com.linkedin.data.transform.patch.request.PatchOpFactory;
import com.linkedin.data.transform.patch.request.PatchOperation;
import com.linkedin.data.transform.patch.request.PatchTree;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;


/**
 * cglib method interceptor which records method invocations into a {@link PatchTree}.
 *
 * @author jflorencio
 */
final class GeneratePatchMethodInterceptor implements MethodInterceptor
{
  /** Wrapped primitive types supported by {@link com.linkedin.data.DataMap} */
  private final static Set<Class<?>> _primitiveTypes = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays.<Class<?>>asList(
      Integer.TYPE,
      Integer.class,
      Long.TYPE,
      Long.class,
      Float.TYPE,
      Float.class,
      Double.TYPE,
      Double.class,
      Boolean.TYPE,
      Boolean.class,
      ByteString.class)));

  private final Class<?> _clazz;
  private final RecordDataSchema _schema;
  private final PathSpec _pathSpec;
  private final PatchTree _patchTree;

  GeneratePatchMethodInterceptor(Class<?> clazz, RecordDataSchema schema, PathSpec pathSpec, PatchTree patchTree)
  {
    _clazz = clazz;
    _schema = schema;
    _pathSpec = pathSpec;
    _patchTree = patchTree;
  }

  @Override
  public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy)
      throws Throwable
  {
    String methodName = method.getName();

    if (methodName.startsWith("set") && args.length > 0)
    {
      handleSet(methodName, args);
      // Setters are fluent on record templates, so return "this"
      return obj;
    }
    else if (methodName.startsWith("get"))
      return handleGet(method);
    else if (methodName.startsWith("remove"))
      return handleRemove(methodName);
    return ObjectProxyHelper.handleObjectMethods(_clazz, obj, method, args);
  }

  private void handleSet(String methodName, Object[] args)
  {
    String propName = toCamelCase(methodName, 3);
    assertPropertyInSchema(propName);

    // DISALLOW_NULL is the same default baked into the generated RecordTemplate classes
    SetMode setMode = args.length == 2 && args[1] instanceof SetMode ? (SetMode)args[1] : SetMode.DISALLOW_NULL;
    interpretSetModeAndSet(propName, args[0], setMode);
  }

  private void interpretSetModeAndSet(String propertyName, Object arg, SetMode setMode)
  {
    PathSpec pathSpec = new PathSpec(_pathSpec.getPathComponents(), propertyName);
    boolean doSet = false;

    switch (setMode)
    {
      case DISALLOW_NULL:
        if (arg == null)
          throw new NullPointerException("Cannot set field " + pathSpec + " on " + _schema.getFullName() + " to null");
        doSet = true;
        break;
      case IGNORE_NULL:
        if (arg != null)
          doSet = true;
        break;
      case REMOVE_IF_NULL:
        doSet = true;
        break;
      case REMOVE_OPTIONAL_IF_NULL:
        if (_schema.getField(propertyName).getOptional())
          doSet = true;
        else if (arg == null)
          throw new IllegalArgumentException("Cannot remove mandatory field " + pathSpec + " on " + _schema.getFullName());
        break;
    }

    if (doSet)
    {
      PatchOperation patchOp = arg == null ? PatchOpFactory.REMOVE_FIELD_OP : PatchOpFactory.setFieldOp(coerceSetValue(arg));
      _patchTree.addOperation(pathSpec, patchOp);
    }
  }

  /**
   * Convert the raw value of {@code arg} to a value suitable to be inserted into a {@link com.linkedin.data.DataMap}.
   *
   * @param arg value to coerce.
   * @return coerced value.
   */
  private Object coerceSetValue(Object arg)
  {
    @SuppressWarnings("unchecked")
    Class<Object> clazz = (Class<Object>)arg.getClass();
    if (_primitiveTypes.contains(clazz))
      return arg;
    else if (clazz.isEnum())
      return arg.toString();
    else if (DataTemplate.class.isAssignableFrom(clazz))
      return ((DataTemplate)arg).data();
    return DataTemplateUtil.coerceInput(arg, clazz, String.class);
  }

  private Object handleGet(Method method)
  {
    String propName = toCamelCase(method.getName(), 3);
    assertPropertyInSchema(propName);

    // If the return of the get is a supported complex type, propagate the proxy another level so it's possible to do things like:
    // foo.getBar().setBaz(123);
    // And the patch would store the PathSpec of ["bar", "baz"] being set to 123.
    return GeneratePatchProxyFactory.newInstance(method.getReturnType(),
                                                 _patchTree,
                                                 new PathSpec(_pathSpec.getPathComponents(), propName));
  }

  private Object handleRemove(String methodName)
  {
    String propName = toCamelCase(methodName, 6);
    assertPropertyInSchema(propName);

    _patchTree.addOperation(new PathSpec(_pathSpec.getPathComponents(), propName), PatchOpFactory.REMOVE_FIELD_OP);
    return null;
  }

  private String toCamelCase(String methodName, int startAt)
  {
    return methodName.substring(startAt, startAt + 1).toLowerCase() + methodName.substring(startAt + 1);
  }

  private void assertPropertyInSchema(String propertyName)
  {
    if (_schema.getField(propertyName) == null)
      throw new IllegalArgumentException("Attempted to access property " + propertyName +
                                             ", but it doesn't exist in the schema " + _schema.getFullName() + ": " +
                                             _schema);

  }
}
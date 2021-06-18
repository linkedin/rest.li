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

package com.linkedin.restli.examples.greetings.server;

import com.linkedin.common.callback.Callback;
import com.linkedin.parseq.BaseTask;
import com.linkedin.parseq.Context;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.Tasks;
import com.linkedin.parseq.promise.Promise;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor.InterfaceType;
import com.linkedin.restli.server.annotations.CallbackParam;
import com.linkedin.restli.server.annotations.ParSeqContextParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.BaseResource;
import com.linkedin.restli.server.resources.KeyValueResource;
import com.linkedin.restli.server.resources.ResourceContextHolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Generated;

/**
 * Generate concrete implementations of the greetings resource, {@link GreetingsResourceImpl}, with
 * all methods and annotations copied. This allows us to exhaustively test all interface types.
 * <p>
 * See the <code>generateGreetings</code> task in <code>build.gradle</code> and the
 * <code>TestGreetingsClientFlavors</code> test case.
 *
 * @author jnwang
 */
public class GreetingsResourceCodeGenerator
{
  private static final Class<?> IMPL_CLASS       = GreetingsResourceImpl.class;
  private static final String   NAMESPACE        = "com.linkedin.restli.examples.greetings.client";
  private static final Class<?> RES_ANNOTATION   = RestLiCollection.class;
  private static final Class<?> KEY              = Long.class;
  private static final Class<?> VALUE            = Greeting.class;
  private static final String   PACKAGE          = "com.linkedin.restli.examples.greetings.server";
  private static final String   BASIC_CLASS_NAME = "GreetingsResource";
  private static final String   BASIC_RES_NAME   = "greetings";

  private final Class<?>        _templateClass;
  private final InterfaceType   _type;
  private final String          _suffix;
  private final boolean         _useParSeqCtx;

  private final String          _resourceName;
  private final String          _className;

  private GreetingsResourceCodeGenerator(final Class<?> templateClass,
                                         final InterfaceType type,
                                         final String suffix,
                                         final boolean useParSeqCtx)
  {
    _templateClass = templateClass;
    _type = type;
    _suffix = suffix;
    _useParSeqCtx = useParSeqCtx;
    _resourceName = BASIC_RES_NAME + _suffix;
    _className = BASIC_CLASS_NAME + _suffix;
  }

  /**
   * @param args
   *          template, interface type, suffix, use ParSeq context
   * @throws ClassNotFoundException
   */
  public static void main(final String[] args) throws ClassNotFoundException, FileNotFoundException
  {
    // want a hard failure if something goes wrong, so gradle stops
    if (args.length != 5)
      throw new IllegalArgumentException("Bad arguments to code generator");

    final PrintStream outputStream = new PrintStream(new File(args[0]));
    final Class<?> template = Class.forName(args[1]);
    final InterfaceType type = InterfaceType.valueOf(args[2]);
    final String suffix = args[3];
    final boolean useParSeqCtx = Boolean.valueOf(args[4]);

    final GreetingsResourceCodeGenerator gen =
        new GreetingsResourceCodeGenerator(template, type, suffix, useParSeqCtx);
    gen.printAll(outputStream);
  }

  private void printAll(final PrintStream out)
  {
    // print out the main body later
    final ByteArrayOutputStream bodyOutStream = new ByteArrayOutputStream();
    final PrintStream bodyOut = new PrintStream(bodyOutStream);

    printClassHeader(bodyOut);
    bodyOut.println();
    printAllMethods(bodyOut);
    bodyOut.println("}"); // end class

    // package and imports first
    out.printf("package %s;%n", PACKAGE);
    out.println();
    for (final String c : _importedFull)
    {
      // don't make import statements for arrays, the base class will be imported separately
      if (!(c.endsWith("[]")))
      {
        out.printf("import %s;%n", c);
      }
    }
    out.println();

    // main code body
    out.print(new String(bodyOutStream.toByteArray()));
  }

  /**
   * class annotation, class definition, private variables
   */
  private void printClassHeader(final PrintStream out)
  {
    out.printf("/* WARNING: GENERATED CODE - DO NOT MODIFY BY HAND */%n%n");
    // generated annotation
    out.printf("@%s(\"%s\")%n", className(Generated.class), getClass().getName());
    // collection resource annotation
    out.printf("@%s(name = \"%s\", namespace = \"%s\")%n",
               className(RES_ANNOTATION),
               _resourceName,
               NAMESPACE);
    // class declaration
    out.printf("public class %s extends ", _className);
    if (_templateClass.isInterface())
      out.printf("%s implements ", className(ResourceContextHolder.class));
    out.printf("%s<%s, %s>", className(_templateClass), className(KEY), className(VALUE));

    out.println();
    out.println("{");

    // private constants
    out.printf("private static final GreetingsResourceImpl _impl = new GreetingsResourceImpl(\"%s\");%n",
               _resourceName);

    if (_type == InterfaceType.CALLBACK || _type == InterfaceType.PROMISE && !_useParSeqCtx)
    {
      out.printf("private static final %s _scheduler = %s.newScheduledThreadPool(1);%n",
                 className(ScheduledExecutorService.class),
                 className(Executors.class));
      out.println("private static final int DELAY = 100;");
    }
  }

  private void printAllMethods(final PrintStream out)
  {
    // avoid stuff declared in Object and superclasses
    final Method[] methods = IMPL_CLASS.getMethods();
    for (final Method method : methods)
      if (method.getDeclaringClass() == IMPL_CLASS)
        printMethod(out, method);
  }

  private void printMethod(final PrintStream out, final Method method)
  {
    final Type returnType = method.getGenericReturnType();
    // method annotations
    final Annotation[] annos = method.getAnnotations();
    for (final Annotation anno : annos)
    {
      if (RestMethod.class.equals(anno.annotationType().getEnclosingClass()))
        if (!_templateClass.equals(KeyValueResource.class))
          continue;
      out.println(toStrAnnotation(anno));
    }

    // method header
    out.print("public ");

    // return type
    switch (_type)
    {
    case CALLBACK:
      out.print("void");
      break;
    case PROMISE:
      out.print(genericWrapper(Promise.class, returnType));
      break;
    case SYNC:
      out.print(toStrGenericType(returnType));
      break;
    case TASK:
      out.print(genericWrapper(Task.class, returnType));
      break;
    }
    out.printf(" %s(", method.getName());

    printArgumentList(out, method);

    out.print(")");

    printThrowsClause(out, method);

    out.println();
    out.println("{"); // begin method body

    printMethodBody(out, method);

    out.println("}"); // end method
    out.println();
  }

  private void printArgumentList(final PrintStream out, final Method method)
  {
    final Type[] argTypes = method.getGenericParameterTypes();
    final Annotation[][] argAnnos = method.getParameterAnnotations();

    boolean first = true;
    for (int i = 0; i < argTypes.length; i++)
    {
      final Type argType = argTypes[i];
      if (argType == BaseResource.class)
        continue;

      if (!first)
        out.print(", ");
      first = false;
      for (final Annotation anno : argAnnos[i])
        out.print(toStrAnnotation(anno) + " ");

      out.printf("final %s %s", toStrGenericType(argType), (char) ('a' + i));
    }

    if (_type == InterfaceType.CALLBACK)
    {
      final Type returnType = method.getGenericReturnType();
      if (!first)
        out.print(", ");
      out.printf("@%s final %s callback",
                 className(CallbackParam.class),
                 genericWrapper(Callback.class, returnType));
    }
    else if (_useParSeqCtx)
    {
      if (!first)
        out.print(", ");
      out.printf("@%s final %s psContext", className(ParSeqContextParam.class), className(Context.class));
    }
  }

  private void printThrowsClause(final PrintStream out, final Method method)
  {
    final Class<?>[] exceptions = method.getExceptionTypes();
    if (exceptions.length > 0)
    {
      out.print(" throws ");
      for (int i = 0; i < exceptions.length; i++)
      {
        if (i != 0)
          out.print(", ");
        out.print(className(exceptions[i]));
      }
    }
  }

  private void printMethodBody(final PrintStream out, final Method method)
  {
    final Type returnType = method.getGenericReturnType();

    switch (_type)
    {
    case CALLBACK:
      out.println("final Runnable requestHandler = new Runnable() {");
      out.println("public void run () {");
      out.println("try {");
      if (returnType == void.class)
      {
        printImplCall(out, method);
        out.println(";");
        out.println("callback.onSuccess(null);");
      }
      else
      {
        out.print("callback.onSuccess(");
        printImplCall(out, method);
        out.println(");");
      }
      out.println("} catch (final Throwable throwable) {");
      out.println("callback.onError(throwable);");
      out.println("}"); // try catch
      out.println("}"); // run
      out.println("};"); // runnable
      out.printf("_scheduler.schedule(requestHandler, DELAY, %s.MILLISECONDS);",
                 className(TimeUnit.class));
      break;

    case PROMISE:
      if (_useParSeqCtx)
      {
        out.printf("final %s result = %s.settable();%n",
                   genericWrapper(SettablePromise.class, returnType),
                   className(Promises.class));
        out.println("final Runnable requestHandler = new Runnable() {");
        out.println("public void run () {");
        out.println("try {");
        if (returnType == void.class)
        {
          printImplCall(out, method);
          out.println(";");
          out.println("result.done(null);");
        }
        else
        {
          out.print("result.done(");
          printImplCall(out, method);
          out.println(");");
        }
        out.println("} catch (final Throwable throwable) {");
        out.println("result.fail(throwable);");
        out.println("}"); // try catch
        out.println("}"); // run
        out.println("};"); // runnable
        out.printf("psContext.run(%s.action(\"restli-%s\", requestHandler::run));%n",
                   className(Task.class),
                   method.getName());
        out.println("return result;");
      }
      else
      {
        out.printf("final %s result = %s.settable();%n",
                   genericWrapper(SettablePromise.class, returnType),
                   className(Promises.class));
        out.println("final Runnable requestHandler = new Runnable() {");
        out.println("public void run () {");
        out.println("try {");
        if (returnType == void.class)
        {
          printImplCall(out, method);
          out.println(";");
          out.println("result.done(null);");
        }
        else
        {
          out.print("result.done(");
          printImplCall(out, method);
          out.println(");");
        }
        out.println("} catch (final Throwable throwable) {");
        out.println("result.fail(throwable);");
        out.println("}"); // try catch
        out.println("}"); // run
        out.println("};"); // runnable
        out.printf("_scheduler.schedule(requestHandler, DELAY, %s.MILLISECONDS);",
                   className(TimeUnit.class));
        out.println("return result;");
      }
      break;

    case SYNC:
      if (!returnType.equals(void.class))
        out.print("return ");
      printImplCall(out, method);
      out.println(";");
      break;

    case TASK:
      out.printf("return new %s()%n", genericWrapper(BaseTask.class, returnType));
      out.println("{");
      out.printf("protected %s run(final %s context) throws Exception%n",
          genericWrapper(Promise.class, returnType),
          className(Context.class));
      out.println("{");
      if (returnType.equals(void.class))
      {
        printImplCall(out, method);
        out.println(";");
        out.printf("return %s.value(null);%n", className(Promises.class));
      }
      else
      {
        out.printf("return %s.value(", className(Promises.class));
        printImplCall(out, method);
        out.println(");");
      }
      out.println("}");
      out.println("};");
      break;
    }
  }

  /**
   * _impl.method(a, b, ...)
   */
  private void printImplCall(final PrintStream out, final Method method)
  {
    final Type[] argTypes = method.getGenericParameterTypes();

    out.printf("_impl.%s(", method.getName());
    for (int i = 0; i < argTypes.length; i++)
    {
      if (i > 0)
        out.print(", ");
      if (argTypes[i] == BaseResource.class)
        out.printf("%s.this", _className);
      else
        out.print((char) ('a' + i));
    }
    out.print(")");
  }

  /**
   * @return the source code corresponding to the given type
   */
  private String toStrGenericType(final Type t)
  {
    return toStrGenericType(t, false);
  }

  private static final Map<Class<?>, String> PRIMITIVES_TO_CLASSES;
  static
  {
    final Map<Class<?>, String> map = new HashMap<>();
    map.put(byte.class, "Byte");
    map.put(short.class, "Short");
    map.put(int.class, "Integer");
    map.put(long.class, "Long");
    map.put(float.class, "Float");
    map.put(double.class, "Double");
    map.put(boolean.class, "Boolean");
    map.put(char.class, "Character");
    map.put(void.class, "Void");
    PRIMITIVES_TO_CLASSES = Collections.unmodifiableMap(map);
  }

  /**
   * @param coercePrimitives
   *          should primitives be converted to their corresponding objects, e.g. int => Integer
   * @return source code for the given type
   */
  private String toStrGenericType(final Type t, final boolean coercePrimitives)
  {
    if (t instanceof Class)
    {
      final Class<?> c = (Class<?>) t;
      if (c.isPrimitive() && coercePrimitives)
      {
        if (PRIMITIVES_TO_CLASSES.containsKey(c))
          return PRIMITIVES_TO_CLASSES.get(c);
        else
          // map should be complete
          throw new AssertionError("unknown primitive: " + c.toString());
      }
      else
      {
        return className((Class<?>) t);
      }
    }

    if (t instanceof ParameterizedType)
    {
      final ParameterizedType param = (ParameterizedType) t;
      final Type[] args = param.getActualTypeArguments();
      final StringBuilder result = new StringBuilder();
      result.append(toStrGenericType(param.getRawType()));
      result.append('<'); // begin type params
      for (int i = 0; i < args.length; i++)
      {
        if (i > 0)
          result.append(", ");
        result.append(toStrGenericType(args[i], true));
      }
      result.append('>'); // end type params
      return result.toString();
    }

    if (t instanceof GenericArrayType)
    {
      final GenericArrayType type = (GenericArrayType) t;
      return toStrGenericType(type.getGenericComponentType()) + "[]";
    }

    throw new UnsupportedOperationException("Don't know how to handle " + t);
  }

  /**
   * @return the source code corresponding to the annotation
   */
  // NOTE does not handle all cases, just enough to work
  private String toStrAnnotation(final Annotation anno)
  {
    final StringBuilder sb = new StringBuilder();
    sb.append('@');
    final Class<? extends Annotation> annoType = anno.annotationType();
    sb.append(className(annoType));
    final Method[] methods = annoType.getDeclaredMethods();

    final Map<String, String> values = new TreeMap<>();
    for (final Method method : methods)
    {
      try
      {
        final Object value = method.invoke(anno);
        if (value.equals(method.getDefaultValue()))
          continue;
        final String name = method.getName();
        if (value instanceof String)
          values.put(name, "\"" + value + "\"");
        else if (value instanceof Enum)
          values.put(name, className(value.getClass()) + "." + value);
        else if (value instanceof Class)
          values.put(name, className((Class<?>) value) + ".class");
        else
          values.put(name, value.toString());
      }
      catch (final IllegalAccessException e)
      {
        throw new AssertionError(e);
      }
      catch (final InvocationTargetException e)
      {
        throw new AssertionError(e);
      }
    }

    // omit parens when no values
    if (values.size() > 0)
      sb.append('('); // begin annotation values

    boolean first = true;
    for (final Map.Entry<String, String> entry : values.entrySet())
    {
      if (!first)
        sb.append(", ");
      first = false;
      final String key = entry.getKey();
      // omit "value = " when only one
      if (values.size() == 1 && key.equals("value"))
      {
        sb.append(entry.getValue());
      }
      else
      {
        sb.append(entry.getKey());
        sb.append(" = ");
        sb.append(entry.getValue());
      }
    }

    if (values.size() > 0)
      sb.append(')'); // end annotation values

    return sb.toString();
  }

  /**
   * @return genericClass&lt;typeArg&gt;, using the boxed version of typeArg if it is a primitive
   */
  private String genericWrapper(final Class<?> genericClass, final Type typeArg)
  {
    return String.format("%s<%s>", className(genericClass), toStrGenericType(typeArg, true));
  }

  private final Set<String> _importedShort = new TreeSet<>();
  private final Set<String> _importedFull  = new TreeSet<>();

  /**
   * Import the class and give the short name if possible
   */
  private String className(final Class<?> c)
  {
    if (c.isPrimitive())
      return c.getCanonicalName();

    if (_importedFull.contains(c.getCanonicalName()))
      return c.getSimpleName();

    final String simple = c.getSimpleName();
    if (_importedShort.contains(simple))
      return c.getCanonicalName(); // can't import, name conflict

    _importedFull.add(c.getCanonicalName());
    _importedShort.add(c.getSimpleName());
    return c.getSimpleName();
  }
}

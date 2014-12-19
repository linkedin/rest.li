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

package com.linkedin.data.template;


/**
 * Custom Java class binding support.
 */
public class Custom
{
  public static final String REGISTER_COERCER = "REGISTER_COERCER";

  /**
   * Registers a new custom class with its {@link DirectCoercer}.
   *
   * This is retained for backwards compatibility.
   *
   * @deprecated Please use {@link #registerCoercer(DirectCoercer, Class)} instead
   * @param targetClass provides the custom class.
   * @param coercer provides the coercer that will be used to coerce the custom class to and from its primitive
   *                representation.
   * @param <T> the type of the custom class.
   * @throws IllegalArgumentException if the target class has already been registered previously.
   */
  @Deprecated
  public static <T> void registerCoercer(Class<T> targetClass, DirectCoercer<T> coercer)
  {
    DataTemplateUtil.registerCoercer(targetClass, coercer);
  }

  /**
   * Registers a new custom class with its {@link DirectCoercer}.
   *
   * Return value allows initialization as follows:
   *
   * <pre>
   * public class UriCoercer implements DirectCoercer<URI>
   * {
   *   private static final boolean REGISTER_COERCER = Custom.registerCoercer(new UriCoercer(), URI.class);
   *   ...
   * }
   * </pre>
   *
   * @param coercer provides the coercer that will be used to coerce the custom class to and from its primitive
   *                representation.
   * @param targetClass provides the custom class.
   * @param <T> the type of the custom class.
   * @return true.
   * @throws IllegalArgumentException if the target class has already been registered previously.
   */
  public static <T> boolean registerCoercer(DirectCoercer<T> coercer, Class<T> targetClass)
  {
    DataTemplateUtil.registerCoercer(targetClass, coercer);
    return true;
  }

  /**
   * Initialize coercer class.
   *
   * The preferred pattern is that custom class will register a coercer
   * through its static initializer. However, it is not always possible to
   * extend the custom class to add a static initializer.
   *
   * In this situation, an optional coercer class can also be specified
   * with the custom class binding declaration in the schema.
   *
   * <pre>
   * {
   *   ...
   *   "java" : {
   *     "class" : "java.net.URI",
   *     "coercerClass" : "com.linkedin.common.URICoercer"
   *   }
   * }
   * </pre>
   *
   * When another type refers to this type, the generated class for referrer
   * class will invoke this method on the coercer class within the referrer
   * class's static initializer.
   *
   * This method will initialize the specified coercer class using
   * {@link Class#forName(String, boolean, ClassLoader)} with {@code initialize}
   * flag set to true. This will cause the static initializers of the
   * coercer class to be invoked. One of these static initializer should
   * register the coercer using {@link #registerCoercer(DirectCoercer coercer, Class targetClass)}.
   *
   * Note: Simply referencing to the coercer class using a static variable or
   * accessing the class of the coercer class does not cause the static
   * initializer of the coercer class to be invoked. Hence, explicit
   * initialization of the class has to be performed by this method.
   *
   * The preferred implementation pattern for coercer class is as follows:
   *
   * <pre>
   * public class UriCoercer implements DirectCoercer<URI>
   * {
   *   private static final Object REGISTER_COERCER = Custom.registerCoercer(new UriCoercer(), URI.class);
   *   ...
   * }
   * </pre>
   */
  public static void initializeCoercerClass(Class<?> coercerClass)
  {
    DataTemplateUtil.initializeClass(coercerClass);
  }

  /**
   * Initialize custom class.
   *
   * The preferred pattern is that custom class will register a coercer
   * through its static initializer.
   *
   * <pre>
   * {
   *   ...
   *   "java" : {
   *     "class" : "com.linkedin.data.FooBar",
   *   }
   * }
   * </pre>
   *
   * When another type refers to this type, the generated class for referrer
   * class will invoke this method on the custom class within the referrer
   * class's static initializer.
   *
   * This method will initialize the specified custom class using
   * {@link Class#forName(String, boolean, ClassLoader)} with {@code initialize}
   * flag set to true. This will cause the static initializers of the
   * coercer class to be invoked. One of these static initializer should
   * register the coercer using {@link #registerCoercer(DirectCoercer coercer, Class targetClass)}.
   *
   * Note: Simply referencing to the coercer class using a static variable or
   * accessing the class of the coercer class does not cause the static
   * initializer of the coercer class to be invoked. Hence, explicit
   * initialization of the class has to be performed by this method.
   *
   * The preferred implementation pattern for custom class is as follows:
   *
   * <pre>
   * public class FooBar
   * {
   *   private static FooBarCoercer implements DirectCoercer<FooBar>
   *   {
   *     ...
   *   }
   *
   *   private static final Object REGISTER_COERCER = Custom.registerCoercer(new FooBarCoercer(), URI.class);
   *   ...
   * }
   * </pre>
   */
  public static void initializeCustomClass(Class<?> customClass)
  {
    DataTemplateUtil.initializeClass(customClass);
  }
}

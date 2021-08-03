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

package com.linkedin.util;


import com.linkedin.data.schema.DataSchema;

import java.util.Map;

public class CustomTypeUtil
{
  public static final String JAVA_PROPERTY = "java";
  public static final String CLASS_PROPERTY = "class";
  public static final String COERCER_CLASS_PROPERTY = "coercerClass";

  public static String getJavaCustomTypeClassNameFromSchema(final DataSchema schema)
  {
    final Object o = schema.getProperties().get(JAVA_PROPERTY);
    if (o == null || !(o instanceof Map))
    {
      return null;
    }

    @SuppressWarnings("unchecked")
    final Map<String, Object> map = (Map<String, Object>) o;
    final Object o2 = map.get(CLASS_PROPERTY);

    if (o2 == null || !(o2 instanceof String))
    {
      return null;
    }

    return (String)o2;
  }

  public static Class<?> getJavaCustomTypeClassFromSchema(DataSchema schema)
  {
    final Class<?> bindingClass;
    final String javaCoercerClassName = getJavaCustomTypeClassNameFromSchema(schema);

    if(javaCoercerClassName != null)
    {
      try
      {
        return Class.forName(javaCoercerClassName, false, CustomTypeUtil.class.getClassLoader());
      }
      catch (SecurityException | ClassNotFoundException e)
      {
        // If CustomTypeUtil.class.getClassLoader() throws exception
        // or CustomTypeUtil.class.getClassLoader() could not load class,
        // fall back to use thread context class loader
        return getBindingClass(javaCoercerClassName, Thread.currentThread().getContextClassLoader(), schema);
      }
    }
    else
    {
      bindingClass = null;
    }

    return bindingClass;
  }

  public static String getJavaCoercerClassFromSchema(final DataSchema schema)
  {
    final Object o = schema.getProperties().get(JAVA_PROPERTY);
    if (o == null || !(o instanceof Map))
    {
      return null;
    }

    @SuppressWarnings("unchecked")
    final Map<String, Object> map = (Map<String, Object>) o;
    final Object o2 = map.get(COERCER_CLASS_PROPERTY);

    if (o2 == null || !(o2 instanceof String))
    {
      return null;
    }

    return (String) o2;

  }

  private static Class<?> getBindingClass(String javaCoercerClassName, ClassLoader classLoader, DataSchema schema)
  {
    try
    {
      return Class.forName(javaCoercerClassName, false, classLoader);
    }
    catch (ClassNotFoundException e)
    {
      throw new IllegalArgumentException("Unable to find java coercer class of " + javaCoercerClassName + " for schema " + schema.getUnionMemberKey());
    }
  }
}

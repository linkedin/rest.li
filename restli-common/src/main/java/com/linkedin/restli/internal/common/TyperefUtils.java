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

package com.linkedin.restli.internal.common;

import java.util.Map;

import com.linkedin.data.schema.TyperefDataSchema;

public class TyperefUtils
{
  public static String getJavaClassNameFromSchema(final TyperefDataSchema schema)
  {
    Object o = schema.getProperties().get("java");
    if (o == null || !(o instanceof Map))
    {
      return null;
    }

    @SuppressWarnings("unchecked")
    Map<String,Object> map = (Map<String,Object>)o;
    Object o2 = map.get("class");

    if (o2 == null || !(o2 instanceof String))
    {
      return null;
    }

    return (String)o2;
  }

  public static Class<?> getJavaClassForSchema(TyperefDataSchema schema)
  {
    Class<?> bindingClass;
    String javaCoercerClassName = getJavaClassNameFromSchema(schema);

    if(javaCoercerClassName != null)
    {
      try
      {
        bindingClass = Class.forName(javaCoercerClassName, false, Thread.currentThread().getContextClassLoader());
      }
      catch (ClassNotFoundException e)
      {
        throw new IllegalArgumentException("Unable to find java coercer class of " + javaCoercerClassName + " for typeref " + schema.getFullName());
      }
    }
    else
    {
      bindingClass = null;
    }

    return bindingClass;
  }

  public static String getCoercerClassFromSchema(final TyperefDataSchema schema)
  {
    Object o = schema.getProperties().get("java");
    if (o == null || !(o instanceof Map))
    {
      return null;
    }

    @SuppressWarnings("unchecked")
    Map<String,Object> map = (Map<String,Object>) o;
    Object o2 = map.get("coercerClass");

    if (o2 == null || !(o2 instanceof String))
    {
      return null;
    }

    return (String) o2;

  }
}

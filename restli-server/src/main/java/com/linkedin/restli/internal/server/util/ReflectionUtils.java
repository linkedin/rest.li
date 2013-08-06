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

package com.linkedin.restli.internal.server.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dellamag
 */
public class ReflectionUtils
{
  /**
   * Get the underlying class for a type, or null if the type is a variable type.
   *
   * @param type the type
   * @return the underlying class
   */
  public static Class<?> getClass(final Type type)
  {
    if (type instanceof Class)
    {
      return (Class) type;
    }
    else if (type instanceof ParameterizedType)
    {
      return getClass(((ParameterizedType) type).getRawType());
    }
    else if (type instanceof GenericArrayType)
    {
      Type componentType = ((GenericArrayType) type).getGenericComponentType();
      Class<?> componentClass = getClass(componentType);
      if (componentClass != null)
      {
        return Array.newInstance(componentClass, 0).getClass();
      }
      else
      {
        return null;
      }
    }
    else
    {
      return null;
    }
  }

  private static Type walkParentsTypeChain(final Class<?> target,
                                           final Class<?> rawType,
                                           final Map<Type, Type> resolvedTypes)
  {
    Type result;
    result = walkTypeChain(target, rawType.getGenericSuperclass(), resolvedTypes);
    if (result != null)
    {
      return result;
    }

    for (Type t : rawType.getGenericInterfaces())
    {
      result = walkTypeChain(target, t, resolvedTypes);
      if (result != null)
      {
        return result;
      }
    }
    return null;
  }

  private static Type walkTypeChain(final Class<?> target,
                                    final Type type,
                                    final Map<Type, Type> resolvedTypes)
  {
    if (type == null)
    {
      return null;
    }

    Type result;
    if (type instanceof Class)
    {
      // there is no useful information for us in raw types, so just keep going.
      result = walkParentsTypeChain(target, (Class) type, resolvedTypes);
      if (result != null)
      {
        return result;
      }
    }
    else
    {
      mapTypeParameters((ParameterizedType) type, resolvedTypes);
      Class<?> rawType = (Class) ((ParameterizedType) type).getRawType();

      if (rawType.equals(target))
      {
        return type;
      }
      else
      {
        result = walkParentsTypeChain(target, rawType, resolvedTypes);
        if (result != null)
        {
          return result;
        }
      }
    }
    return null;
  }

  private static void mapTypeParameters(final ParameterizedType type,
                                        final Map<Type, Type> resolvedTypes)
  {
    Class<?> rawType = (Class) type.getRawType();

    Type[] actualTypeArguments = type.getActualTypeArguments();
    TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
    for (int i = 0; i < actualTypeArguments.length; i++)
    {
      resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
    }
  }

  /**
   * Get the actual type arguments a child class has used to extend a generic base class.
   *
   * @param baseClass the base class
   * @param childClass the child class
   * @return a list of the raw classes for the actual type arguments.
   */
  public static <T> List<Class<?>> getTypeArguments(final Class<T> baseClass,
                                                    final Class<? extends T> childClass)
  {
    List<Type> typeArguments = getTypeArgumentsParametrized(baseClass, childClass);
    List<Class<?>> rawTypeArguments = null;

    if (typeArguments != null)
    {
      rawTypeArguments = new ArrayList<Class<?>>();

      for (Type type : typeArguments)
      {
        rawTypeArguments.add(getClass(type));
      }
    }

    return rawTypeArguments;
  }

  /**
   * Get the actual type arguments a child class has used to extend a generic base class.
   *
   * @param baseClass the base class
   * @param childClass the child class
   * @return a list of the raw classes for the actual type arguments.
   */
  public static <T> List<Type> getTypeArgumentsParametrized(final Class<T> baseClass,
                                                    final Class<? extends T> childClass)
  {
    Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
    Type type = walkTypeChain(baseClass, childClass, resolvedTypes);
    if (type == null)
    {
      return null;
    }

    // finally, for each actual type argument provided to baseClass, determine (if
    // possible)
    // the raw class for that type argument.
    Type[] typeArguments;
    if (type instanceof Class)
    {
      typeArguments = ((Class) type).getTypeParameters();
    }
    else
    {
      typeArguments = ((ParameterizedType) type).getActualTypeArguments();
    }
    List<Type> typeArgumentsAsClasses = new ArrayList<Type>();
    // resolve types by chasing down type variables.
    for (Type baseType : typeArguments)
    {
      while (resolvedTypes.containsKey(baseType))
      {
        baseType = resolvedTypes.get(baseType);
      }
      typeArgumentsAsClasses.add(baseType);
    }
    return typeArgumentsAsClasses;
  }

}

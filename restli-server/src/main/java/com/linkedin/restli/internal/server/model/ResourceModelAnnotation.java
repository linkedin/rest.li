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

package com.linkedin.restli.internal.server.model;


import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.restspec.RestSpecAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Keren Jin
 */
public class ResourceModelAnnotation
{
  public static DataMap getAnnotationsMap(Annotation[] as)
  {
    return annotationsToData(as, true);
  }

  private static DataMap annotationsToData(Annotation[] as, boolean isTopLevel)
  {
    final DataMap annotationData = new DataMap();

    for (Annotation a: as)
    {
      final AnnotationEntry entry = getAnnotationData(a, isTopLevel);
      if (entry != null)
      {
        annotationData.put(entry.name, entry.data);
      }
    }

    return annotationData;
  }

  private static Object annotationMemberToData(Object memberValue)
  {
    final Class<?> memberClass = memberValue.getClass();
    final Object data;

    if (memberClass == boolean.class ||
        memberClass == Boolean.class ||
        memberClass == int.class     ||
        memberClass == Integer.class ||
        memberClass == long.class    ||
        memberClass == Long.class    ||
        memberClass == float.class   ||
        memberClass == Float.class   ||
        memberClass == double.class  ||
        memberClass == Double.class  ||
        memberClass == String.class)
    {
      data = memberValue;
    }
    else if (memberClass == Byte.class)
    {
      final byte[] singleByteArray = {(Byte) memberValue};
      data = ByteString.copy(singleByteArray);
    }
    else if (memberClass.isEnum())
    {
      data = memberValue.toString();
    }
    else if (memberClass == Class.class)
    {
      data = ((Class<?>) memberValue).getCanonicalName();
    }
    else if (memberClass.isArray() && Array.getLength(memberValue) > 0)
    {
      if (memberClass == byte[].class)
      {
        data = ByteString.copy((byte[]) memberValue);
      }
      else if (memberClass == Annotation[].class)
      {
        data = annotationsToData((Annotation[]) memberValue, false);
      }
      else
      {
        final DataList dataList = new DataList();
        final int memberArrayLen = Array.getLength(memberValue);
        for (int i = 0; i < memberArrayLen; ++i)
        {
          dataList.add(annotationMemberToData(Array.get(memberValue, i)));
        }

        if (dataList.isEmpty())
        {
          data = null;
        }
        else
        {
          data = dataList;
        }
      }
    }
    // Annotation's getClass() yields a Proxy with sun.reflect.annotation.AnnotationInvocationHandler as the InvocationHandler
    else if (memberValue instanceof Annotation)
    {
      final AnnotationEntry entry = getAnnotationData((Annotation) memberValue, false);
      if (entry == null)
      {
        data = null;
      }
      else
      {
        data = entry.data;
      }
    }
    else
    {
      data = null;
    }

    return data;
  }

  private static AnnotationEntry getAnnotationData(Annotation a, boolean isTopLevel)
  {
    final DataMap data = new DataMap();

    final Class<? extends Annotation> annotationClass = a.annotationType();
    AnnotationTrait trait = _traits.get(annotationClass);
    if (trait == null)
    {
      trait = getTraits(annotationClass, isTopLevel);
      _traits.put(annotationClass, trait);
    }

    for (Method m: annotationClass.getDeclaredMethods())
    {
      final MetaTrait methodTrait = trait.memberTraits.get(m);
      if (methodTrait == null ||
         (isTopLevel && !methodTrait.isRestSpecAnnotated))
      {
        continue;
      }

      try
      {
        final Object memberValue = m.invoke(a);
        if (methodTrait.skipDefault)
        {
          final Object annotationDefault = m.getDefaultValue();
          if (annotationDefault != null && annotationDefault.equals(memberValue))
          {
            continue;
          }
        }

        final Object valueData = annotationMemberToData(memberValue);
        if (valueData != null)
        {
          data.put(methodTrait.isRestSpecAnnotated ? methodTrait.name : m.getName(), valueData);
        }
      }
      catch (IllegalAccessException e)
      {
        throw new RestLiInternalException(e);
      }
      catch (IllegalArgumentException e)
      {
        throw new RestLiInternalException(e);
      }
      catch (InvocationTargetException e)
      {
        throw new RestLiInternalException(e);
      }
    }

    if (data.isEmpty())
    {
      if (trait.masterTrait.isRestSpecAnnotated)
      {
        return new AnnotationEntry(trait.masterTrait.name, new DataMap());
      }
      else
      {
        return null;
      }
    }
    else
    {
      return new AnnotationEntry(trait.masterTrait.name, data);
    }
  }

  private static AnnotationTrait getTraits(Class<? extends Annotation> clazz, boolean isTopLevel)
  {
    final AnnotationTrait trait = new AnnotationTrait();

    final RestSpecAnnotation classAnnotation = clazz.getAnnotation(RestSpecAnnotation.class);
    if (classAnnotation == null)
    {
      trait.masterTrait = new MetaTrait(false,
                                        clazz.getCanonicalName(),
                                        isTopLevel,
                                        RestSpecAnnotation.DEFAULT_SKIP_DEFAULT);
    }
    else
    {
      trait.masterTrait = new MetaTrait(classAnnotation, clazz.getCanonicalName());
    }

    trait.memberTraits = new HashMap<>();
    for (Method m: clazz.getDeclaredMethods())
    {
      final RestSpecAnnotation methodAnnotation = m.getAnnotation(RestSpecAnnotation.class);
      if (methodAnnotation == null && !trait.masterTrait.exclude)
      {
        trait.memberTraits.put(m, new MetaTrait(trait.masterTrait.isRestSpecAnnotated,
                                                m.getName(),
                                                trait.masterTrait.exclude,
                                                trait.masterTrait.skipDefault));
      }
      else if (methodAnnotation != null && !methodAnnotation.exclude())
      {
        trait.memberTraits.put(m, new MetaTrait(methodAnnotation, m.getName()));
      }
    }

    return trait;
  }

  private static class AnnotationEntry
  {
    AnnotationEntry(String name, Object data)
    {
      this.name = name;
      this.data = data;
    }

    final String name;
    final Object data;
  }

  private static class AnnotationTrait
  {
    MetaTrait masterTrait;
    Map<Method, MetaTrait> memberTraits;
  }

  private static class MetaTrait
  {
    public MetaTrait(boolean isRestSpecAnnotated, String customKeyName, boolean exclude, boolean skipDefault)
    {
      this.isRestSpecAnnotated = isRestSpecAnnotated;
      this.name = customKeyName;
      this.exclude = exclude;
      this.skipDefault = skipDefault;
    }

    public MetaTrait(RestSpecAnnotation a, String customName)
    {
      this.isRestSpecAnnotated = true;
      final String annotatedName = a.name();
      this.name = RestSpecAnnotation.DEFAULT_NAME.equals(annotatedName) ? customName : annotatedName;
      this.exclude = a.exclude();
      this.skipDefault = a.skipDefault();
    }

    public final boolean isRestSpecAnnotated;
    public final String name;
    public final boolean exclude;
    public final boolean skipDefault;
  }

  private static final Map<Class<? extends Annotation>, AnnotationTrait> _traits = new HashMap<>();
}

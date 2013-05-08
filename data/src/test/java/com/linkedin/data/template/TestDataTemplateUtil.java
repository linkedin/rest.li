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


import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;


public class TestDataTemplateUtil
{
  public static class FieldInfo
  {
    private final RecordDataSchema.Field _field;
    private final Class<?> _fieldClass;

    public FieldInfo(RecordDataSchema.Field field, Class<?> fieldClass)
    {
      _field = field;
      _fieldClass = fieldClass;
    }

    public RecordDataSchema.Field getField()
    {
      return _field;
    }

    public Class<?> getFieldClass()
    {
      return _fieldClass;
    }
  }

  public static FieldInfo fieldInfo(RecordTemplate recordTemplate, String fieldName)
  {
    RecordDataSchema schema = recordTemplate.schema();
    RecordDataSchema.Field field = schema.getField(fieldName);
    String getterName = methodName(field.getType().getDereferencedType() == DataSchema.Type.BOOLEAN ? "is" : "get", fieldName);
    try
    {
      Method method = recordTemplate.getClass().getMethod(getterName);
      Class<?> fieldClass = method.getReturnType();
      return new FieldInfo(field, fieldClass);
    }
    catch (NoSuchMethodException e)
    {
      throw new IllegalStateException("Cannot find method " + getterName, e);
    }
  }

  public static String capitalizeFirstCharacter(String s)
  {
    if (s != null && s.isEmpty() == false && Character.isLowerCase(s.charAt(0)))
    {
      return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    else
    {
      return s;
    }
  }

  public static String methodName(String prefix, String fieldName)
  {
    return prefix + capitalizeFirstCharacter(fieldName);
  }


  public static void assertCloneAndCopyReturnType(Class<? extends DataTemplate> dataTemplateClass)
  {
    String methodNames[] = { "clone", "copy" };
    for (String methodName : methodNames)
    {
      try
      {
        assert(dataTemplateClass.getMethod(methodName).getReturnType() == dataTemplateClass);
      }
      catch (NoSuchMethodException e)
      {
        throw new IllegalArgumentException("Cannot get " + methodName + " method from " + dataTemplateClass.getSimpleName(), e);
      }
    }
  }

  public static <T extends DataTemplate> void assertPresentInFields(Class<T> templateClass, String fieldName)
  {
    try
    {
      // fields
      Method fieldsMethod = templateClass.getMethod("fields");
      PathSpec recordPathSpec = (PathSpec) fieldsMethod.invoke(null);
      Method fieldPathSpecMethod = recordPathSpec.getClass().getMethod(fieldName);
      PathSpec fieldPathSpec = (PathSpec) fieldPathSpecMethod.invoke(recordPathSpec);
      List<String> components = fieldPathSpec.getPathComponents();
      assertEquals(components.size(), 1);
      assertEquals(components.get(0), fieldName);
    }
    catch (NoSuchMethodException exc)
    {
      fail("Unexpected exception", exc);
    }
    catch (IllegalAccessException exc)
    {
      fail("Unexpected exception", exc);
    }
    catch (InvocationTargetException exc)
    {
      fail("Unexpected exception", exc);
    }
  }
}

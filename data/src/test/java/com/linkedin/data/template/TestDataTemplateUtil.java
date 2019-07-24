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

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;


public class TestDataTemplateUtil
{
  private static final String NAN = "NaN";
  private static final String POSITIVE_INFINITY = "Infinity";
  private static final String NEGATIVE_INFINITY = "-Infinity";

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


  public static void assertCloneAndCopyReturnType(Class<? extends DataTemplate<?>> dataTemplateClass)
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

  public static <T extends DataTemplate<?>> void assertPresentInFields(Class<T> templateClass, String fieldName)
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

  @Test
  public static void testCoerceExceptions()
  {
    try
    {
      assertTrue(DataTemplateUtil.hasCoercer(Boolean.TYPE));
      DataTemplateUtil.coerceOutput("false", Boolean.TYPE);
      fail("Expected Exception");
    }
    catch (TemplateOutputCastException e)
    {
      assertEquals(e.getMessage(), "Output false has type java.lang.String, but expected type is java.lang.Boolean");
    }
    try
    {
      assertFalse(DataTemplateUtil.hasCoercer(Character.class));
      DataTemplateUtil.coerceOutput("string", Character.class);
      fail("Expected Exception");
    }
    catch (TemplateOutputCastException e)
    {
      assertEquals(e.getMessage(), "Output string has type java.lang.String, but does not have a registered coercer and cannot be coerced to type java.lang.Character");
    }
    try
    {
      assertTrue(DataTemplateUtil.hasCoercer(Float.class));
      DataTemplateUtil.coerceOutput("random string", Float.class);
      fail("Expected Exception");
    }
    catch (TemplateOutputCastException e)
    {
      assertEquals(e.getMessage(), "Cannot coerce String value : random string to type : java.lang.Float");
    }
    try
    {
      assertTrue(DataTemplateUtil.hasCoercer(Integer.class));
      DataTemplateUtil.coerceOutput(NAN, Integer.class);
      fail("Expected Exception");
    }
    catch (TemplateOutputCastException e)
    {
      assertEquals(e.getMessage(), "Output NaN has type java.lang.String, but expected type is java.lang.Integer");
    }
    try
    {
      assertTrue(DataTemplateUtil.hasCoercer(Integer.class));
      DataTemplateUtil.coerceOutput(false, Integer.class);
      fail("Expected Exception");
    }
    catch (TemplateOutputCastException e)
    {
      assertEquals(e.getMessage(), "Output false has type java.lang.Boolean, but expected type is java.lang.Integer");
    }
  }

  @Test
  public static void testCoerceOutputNumericNumberCases()
  {
    // Double case
    Object object1 = DataTemplateUtil.coerceOutput(1.2, Double.class);
    assertEquals(object1, 1.2);

    // Float case
    Object object2 = DataTemplateUtil.coerceOutput(1.2f, Float.class);
    assertEquals(object2, 1.2f);

    // Integer case
    Object object3 = DataTemplateUtil.coerceOutput(1, Integer.class);
    assertEquals(object3, 1);

    // Long case
    Object object4 = DataTemplateUtil.coerceOutput(1l, Long.class);
    assertEquals(object4, 1l);
  }

  @Test
  public static void testCoerceOutputNonNumericNumberCases()
  {
    // Convert Specific Floating point string - NaN to Double
    Object object1 = DataTemplateUtil.coerceOutput(NAN, Double.class);
    assertEquals(object1, Double.NaN);

    // Convert Specific Floating point string - POSITIVE_INFINITY to Double
    Object object2 = DataTemplateUtil.coerceOutput(POSITIVE_INFINITY, Double.class);
    assertEquals(object2, Double.POSITIVE_INFINITY);

    // Convert Specific Floating point string - NEGATIVE_INFINITY to Double
    Object object3 = DataTemplateUtil.coerceOutput(NEGATIVE_INFINITY, Double.class);
    assertEquals(object3, Double.NEGATIVE_INFINITY);

    // Convert Specific Floating point string - NaN to Float
    Object object4 = DataTemplateUtil.coerceOutput(NAN, Float.class);
    assertEquals(object4, Float.NaN);

    // Convert Specific Floating point string - POSITIVE_INFINITY to Float
    Object object5 = DataTemplateUtil.coerceOutput(POSITIVE_INFINITY, Float.class);
    assertEquals(object5, Float.POSITIVE_INFINITY);

    // Convert Specific Floating point string - NEGATIVE_INFINITY to Float
    Object object6 = DataTemplateUtil.coerceOutput(NEGATIVE_INFINITY, Float.class);
    assertEquals(object6, Float.NEGATIVE_INFINITY);
  }
}

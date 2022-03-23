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

package com.linkedin.pegasus.generator.override;

import com.linkedin.data.ByteString;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.*;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.testng.Assert.*;


/**
 * @author Min Chen
 */
public class TestRecord
{
  private void testField(RecordTemplate record, String fieldName, final Object value)
  {
    Class<?> valueClass = value.getClass();
    TestDataTemplateUtil.FieldInfo fieldInfo = TestDataTemplateUtil.fieldInfo(record, fieldName);
    String getterPrefix = fieldInfo.getFieldClass() == Boolean.class ? "is" : "get";
    String getMethodName = TestDataTemplateUtil.methodName(getterPrefix, fieldName);
    String setMethodName = TestDataTemplateUtil.methodName("set", fieldName);
    String hasMethodName = TestDataTemplateUtil.methodName("has", fieldName);
    String removeMethodName = TestDataTemplateUtil.methodName("remove", fieldName);
    Class<? extends RecordTemplate> recordClass = record.getClass();
    DataMap dataMap = record.data();
    boolean isOptional = fieldInfo.getField().getOptional();

    try
    {
      Method getMethod = recordClass.getMethod(getMethodName);
      Method getModeMethod = recordClass.getMethod(getMethodName, GetMode.class);
      Method setMethod = recordClass.getMethod(setMethodName, valueClass);
      Method setModeMethod = recordClass.getMethod(setMethodName, valueClass, SetMode.class);
      Method hasMethod = recordClass.getMethod(hasMethodName);
      Method removeMethod = recordClass.getMethod(removeMethodName);
      Object prevValue;

      // fields
      TestDataTemplateUtil.assertPresentInFields(recordClass, fieldName);

      // has
      if (dataMap.containsKey(fieldName))
      {
        assertTrue((Boolean) hasMethod.invoke(record));
        prevValue = getMethod.invoke(record);
      }
      else
      {
        assertFalse((Boolean) hasMethod.invoke(record));
        prevValue = null;
      }

      // set
      Object result = setMethod.invoke(record, value);
      assertSame(result, record);

      // has with field present
      assertTrue((Boolean) hasMethod.invoke(record));

      // get with field present
      result = getMethod.invoke(record);
      if (value instanceof DataTemplate || value instanceof Enum)
      {
        assertSame(result, value);
      }
      else
      {
        assertEquals(result, value);
      }

      // GetMode.NULL, GetMode.DEFAULT, GetMode.STRICT with field present
      assertSame(getModeMethod.invoke(record, GetMode.NULL), result);
      assertSame(getModeMethod.invoke(record, GetMode.DEFAULT), result);
      assertSame(getModeMethod.invoke(record, GetMode.STRICT), result);

      // remove
      removeMethod.invoke(record);

      // has with field absent
      assertFalse((Boolean) hasMethod.invoke(record));
      assertNull(getModeMethod.invoke(record, GetMode.NULL));

      // GetMode.NULL with field absent
      result = getModeMethod.invoke(record, GetMode.NULL);
      assertNull(result);

      // GetMode.DEFAULT with field absent
      Object defaultValue = getModeMethod.invoke(record, GetMode.DEFAULT);
      Object defaultValueFromSchema = fieldInfo.getField().getDefault();
      assertEquals(defaultValue == null, defaultValueFromSchema == null);
      if (defaultValue != null)
      {
        if (defaultValue instanceof DataTemplate)
        {
          assertEquals(((DataTemplate) defaultValue).data(), defaultValueFromSchema);
        }
        else if (defaultValue instanceof Enum)
        {
          assertEquals(defaultValue.toString(), defaultValueFromSchema);
        }
        else
        {
          assertSame(defaultValue, defaultValueFromSchema);
        }
      }

      // GetMode.STRICT with field absent
      boolean expectRequiredFieldNotFoundException = (! isOptional && defaultValue == null);
      try
      {
        result = getModeMethod.invoke(record, GetMode.STRICT);
        assertFalse(expectRequiredFieldNotFoundException);
        assertSame(result, defaultValue);
      }
      catch (InvocationTargetException exc)
      {
        assertTrue(expectRequiredFieldNotFoundException);
        assertTrue(exc.getTargetException() instanceof RequiredFieldNotPresentException);
      }

      // SetMode.IGNORE_NULL
      setModeMethod.invoke(record, value, SetMode.IGNORE_NULL);
      assertSame(getMethod.invoke(record), value);
      setModeMethod.invoke(record, null, SetMode.IGNORE_NULL);
      assertSame(getMethod.invoke(record), value);

      // SetMode.REMOVE_IF_NULL
      removeMethod.invoke(record);
      setModeMethod.invoke(record, value, SetMode.REMOVE_IF_NULL);
      assertSame(getMethod.invoke(record), value);
      setModeMethod.invoke(record, null, SetMode.REMOVE_IF_NULL);
      assertFalse((Boolean) hasMethod.invoke(record));

      // SetMode.REMOVE_OPTIONAL_IF_NULL
      removeMethod.invoke(record);
      setModeMethod.invoke(record, value, SetMode.REMOVE_OPTIONAL_IF_NULL);
      assertSame(getMethod.invoke(record), value);
      try
      {
        setModeMethod.invoke(record, null, SetMode.REMOVE_OPTIONAL_IF_NULL);
        assertTrue(isOptional);
        assertFalse((Boolean) hasMethod.invoke(record));
      }
      catch (InvocationTargetException exc)
      {
        assertFalse(isOptional);
        assertTrue(exc.getTargetException() instanceof IllegalArgumentException);
      }

      // SetMode.DISALLOW_NULL
      try
      {
        setModeMethod.invoke(record, null, SetMode.DISALLOW_NULL);
      }
      catch (InvocationTargetException exc)
      {
        assertTrue(exc.getTargetException() instanceof NullPointerException);
      }

      // restore original value
      if (prevValue != null)
      {
        result = setMethod.invoke(record, prevValue);
        assertSame(result, record);
        assertTrue((Boolean) hasMethod.invoke(record));
        assertEquals(getMethod.invoke(record), prevValue);
      }
    }
    catch (IllegalAccessException exc)
    {
      fail("Unexpected exception", exc);
    }
    catch (InvocationTargetException exc)
    {
      fail("Unexpected exception", exc);
    }
    catch (NoSuchMethodException exc)
    {
      fail("Unexpected exception", exc);
    }
  }

  private <T extends RecordTemplate> void testRecord(Class<T> recordClass)
  {
    try
    {
      T record = recordClass.getDeclaredConstructor().newInstance();
      RecordDataSchema schema = (RecordDataSchema) DataTemplateUtil.getSchema(recordClass);
      RecordDataSchema schema2 = record.schema();
      assertSame(schema, schema2);
    }
    catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException exc)
    {
      fail("Unexpected exception", exc);
    }
  }

  @Test
  public void testRecordTest() throws IOException
  {
    Object[][] inputs =
    {
      {
        "intField",
        8
      },
      {
        "intOptionalField",
        9
      },
      {
        "intDefaultField",
        10
      },
      {
        "intDefaultOptionalField",
        11
      },
      {
        "longField",
        12L
      },
      {
        "floatField",
        13.0f
      },
      {
        "doubleField",
        14.0
      },
      {
        "booleanField",
        true
      },
      {
        "stringField",
        "abc"
      },
      {
        "bytesField",
        ByteString.copyAvroString("abcdef", true)
      },
      {
        "enumField",
        EnumFruits.BANANA
      },
      {
        "fixedField",
        new FixedMD5("0123456789abcdef")
      },
      {
        "recordField",
        new RecordBar().setLocation("far")
      },
      {
        "arrayField",
        new IntegerArray(Arrays.asList(1, 2, 3, 4, 5))
      },
      {
        "mapField",
        new StringMap(TestUtil.asMap("k1", "v1", "k2", "v2", "k3", "v3"))
      },
      {
        "unionField",
        new RecordTest.UnionField(TestUtil.dataMapFromString("{ \"int\" : 3 }"))
      }
    };

    RecordTest record = new RecordTest();

    testRecord(record.getClass());
    for (Object[] row : inputs)
    {
      String fieldName = (String) row[0];
      Object value = row[1];
      testField(record, fieldName, value);
    }
  }

  @Test
  public void testIntField()
  {
    RecordTest record = new RecordTest();

    assertFalse(record.hasIntField());
    assertEquals(record.getIntField(GetMode.NULL), null);
    assertEquals(record.getIntField(GetMode.DEFAULT), null);
    Exception exc;
    try
    {
      exc = null;
      record.getIntField(GetMode.STRICT);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof RequiredFieldNotPresentException);
    try
    {
      exc = null;
      record.getIntField();
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof RequiredFieldNotPresentException);

    Integer intValue = 13;
    record.setIntField(intValue);
    assertTrue(record.hasIntField());
    assertEquals(record.getIntField(GetMode.NULL), intValue);
    assertEquals(record.getIntField(GetMode.DEFAULT), intValue);
    assertEquals(record.getIntField(GetMode.STRICT), intValue);
    assertEquals(record.getIntField(), intValue);
  }

  @Test
  public void testIntFieldAccessorMethodsExist() throws NoSuchMethodException, SecurityException
  {
    // test to make sure there is getter without mode
    assertSame(RecordTest.class.getMethod("getIntField").getReturnType(), Integer.class);

    // test to make sure there is getter with mode
    assertSame(RecordTest.class.getMethod("getIntField", GetMode.class).getReturnType(), Integer.class);

    // test to make sure that is a boxified setter with mode
    RecordTest.class.getMethod("setIntField", Integer.class, SetMode.class);

    // test to make sure there is unboxified setter without mode
    RecordTest.class.getMethod("setIntField", int.class);

    // test to make sure there is boxified setter without mode
    RecordTest.class.getMethod("setIntField", Integer.class);

    // test to make sure that is a boxified setter with mode
    RecordTest.class.getMethod("setIntField", Integer.class, SetMode.class);
  }

  @Test
  public void testIntOptionalField()
  {
    RecordTest record = new RecordTest();

    assertFalse(record.hasIntOptionalField());
    assertEquals(record.getIntOptionalField(GetMode.NULL), null);
    assertEquals(record.getIntOptionalField(GetMode.DEFAULT), null);
    assertEquals(record.getIntOptionalField(GetMode.STRICT), null);
    assertEquals(record.getIntOptionalField(), null);

    Integer intValue = 13;
    record.setIntOptionalField(intValue);
    assertTrue(record.hasIntOptionalField());
    assertEquals(record.getIntOptionalField(GetMode.NULL), intValue);
    assertEquals(record.getIntOptionalField(GetMode.DEFAULT), intValue);
    assertEquals(record.getIntOptionalField(GetMode.STRICT), intValue);
    assertEquals(record.getIntOptionalField(), intValue);
  }

  @Test
  public void testIntDefaultField()
  {
    RecordTest record = new RecordTest();

    assertFalse(record.hasIntDefaultField());
    assertEquals(record.getIntDefaultField(GetMode.NULL), null);
    Integer defaultValue = 17;
    assertEquals(record.getIntDefaultField(GetMode.DEFAULT), defaultValue);
    assertEquals(record.getIntDefaultField(GetMode.STRICT), defaultValue);
    assertEquals(record.getIntDefaultField(), defaultValue);

    Integer intValue = 13;
    record.setIntDefaultField(intValue);
    assertTrue(record.hasIntDefaultField());
    assertEquals(record.getIntDefaultField(GetMode.NULL), intValue);
    assertEquals(record.getIntDefaultField(GetMode.DEFAULT), intValue);
    assertEquals(record.getIntDefaultField(GetMode.STRICT), intValue);
    assertEquals(record.getIntDefaultField(), intValue);
  }

  @Test
  public void testIntDefaultOptionalField()
  {
    RecordTest record = new RecordTest();

    assertFalse(record.hasIntDefaultOptionalField());
    assertEquals(record.getIntDefaultOptionalField(GetMode.NULL), null);
    Integer defaultValue = 42;
    assertEquals(record.getIntDefaultOptionalField(GetMode.DEFAULT), defaultValue);
    assertEquals(record.getIntDefaultOptionalField(GetMode.STRICT), defaultValue);
    assertEquals(record.getIntDefaultOptionalField(), defaultValue);

    Integer intValue = 13;
    record.setIntDefaultOptionalField(intValue);
    assertTrue(record.hasIntDefaultOptionalField());
    assertEquals(record.getIntDefaultOptionalField(GetMode.NULL), intValue);
    assertEquals(record.getIntDefaultOptionalField(GetMode.DEFAULT), intValue);
    assertEquals(record.getIntDefaultOptionalField(GetMode.STRICT), intValue);
    assertEquals(record.getIntDefaultOptionalField(), intValue);
  }

  @Test
  public void testCloneChangePrimitiveField() throws CloneNotSupportedException
  {
    RecordTest record = new RecordTest();
    record.setIntField(52);
    record.setIntOptionalField(500);
    RecordTest recordClone = record.clone();
    assertEquals(recordClone, record);
    assertNotSame(recordClone.data(), record.data());
    assertSame(recordClone.getIntField(), record.getIntField());

    recordClone.setIntField(99);
    assertEquals(record.getIntField().intValue(), 52);
    assertEquals(recordClone.getIntField().intValue(), 99);

    recordClone.removeIntOptionalField();
    assertEquals(record.getIntOptionalField().intValue(), 500);
    assertNull(recordClone.getIntOptionalField());
  }

  @Test
  public void testCloneChangeRecordField() throws CloneNotSupportedException
  {
    RecordTest record = new RecordTest();
    record.setRecordField(new RecordBar());
    record.getRecordField().setLocation("near");
    record.setRecordOptionalField(new RecordBar());
    record.getRecordOptionalField().setLocation("near");
    record.getRecordOptionalField().setOptionalLocation("maybeNear");
    RecordTest recordClone = record.clone();
    assertEquals(recordClone, record);
    assertNotSame(recordClone.data(), record.data());
    assertSame(recordClone.getRecordField(), record.getRecordField());

    recordClone.getRecordField().setLocation("far");
    assertEquals(record.getRecordField().getLocation(), "far");
    assertEquals(recordClone.getRecordField().getLocation(), "far");

    recordClone.getRecordOptionalField().removeOptionalLocation();
    assertEquals(record.getRecordOptionalField().getLocation(), "near");
    assertNull(record.getRecordOptionalField().getOptionalLocation());
    assertEquals(recordClone.getRecordOptionalField().getLocation(), "near");
    assertNull(recordClone.getRecordOptionalField().getOptionalLocation());

    recordClone.removeRecordOptionalField();
    assertEquals(record.getRecordOptionalField().getLocation(), "near");
    assertNull(record.getRecordOptionalField().getOptionalLocation());
    assertNull(recordClone.getRecordOptionalField());
  }

  @Test
  public void testCopyChangePrimitiveField() throws CloneNotSupportedException
  {
    RecordTest record = new RecordTest();
    record.setIntField(52);
    record.setIntOptionalField(500);
    RecordTest recordCopy = record.copy();
    assertEquals(recordCopy, record);
    assertTrue(TestUtil.noCommonDataComplex(recordCopy, record));
    assertNotSame(recordCopy.data(), record.data());
    assertSame(recordCopy.getIntField(), record.getIntField());

    recordCopy.setIntField(99);
    assertEquals(record.getIntField().intValue(), 52);
    assertEquals(recordCopy.getIntField().intValue(), 99);

    recordCopy.removeIntOptionalField();
    assertEquals(record.getIntOptionalField().intValue(), 500);
    assertNull(recordCopy.getIntOptionalField());
  }

  @Test
  public void testCopyChangeRecordField() throws CloneNotSupportedException
  {
    RecordTest record = new RecordTest();
    record.setRecordField(new RecordBar());
    record.getRecordField().setLocation("near");
    record.setRecordOptionalField(new RecordBar());
    record.getRecordOptionalField().setLocation("near");
    record.getRecordOptionalField().setOptionalLocation("maybeNear");
    RecordTest recordCopy = record.copy();
    assertEquals(recordCopy, record);
    assertTrue(TestUtil.noCommonDataComplex(recordCopy.data(), record.data()));
    assertNotSame(recordCopy.data(), record.data());
    assertNotSame(recordCopy.getRecordField(), record.getRecordField());
    assertNotSame(recordCopy.getRecordField().data(), record.getRecordField().data());

    recordCopy.getRecordField().setLocation("far");
    assertEquals(record.getRecordField().getLocation(), "near");
    assertEquals(recordCopy.getRecordField().getLocation(), "far");

    recordCopy.getRecordOptionalField().removeOptionalLocation();
    assertEquals(record.getRecordOptionalField().getLocation(), "near");
    assertEquals(recordCopy.getRecordOptionalField().getLocation(), "near");
    assertNull(recordCopy.getRecordOptionalField().getOptionalLocation());

    recordCopy.removeRecordOptionalField();
    assertEquals(record.getRecordOptionalField().getLocation(), "near");
    assertNull(recordCopy.getRecordOptionalField());
  }

  @Test
  public void testSetOnRecordWrappingSameMap()
  {
    RecordBar bar = new RecordBar();
    bar.setLocation("some");
    RecordBar copy = new RecordBar(bar.data());
    assertEquals(copy.getLocation(), "some");
    copy.setLocation("other");
    assertEquals(bar.getLocation(), "other");
  }
}

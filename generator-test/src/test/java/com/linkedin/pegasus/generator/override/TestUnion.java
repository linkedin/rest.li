/*
   Copyright (c) 2013 LinkedIn Corp.

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
import com.linkedin.data.Data;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.template.*;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.testng.Assert.*;


/**
 * @author Min Chen
 */
public class TestUnion
{
  private static <T extends UnionTemplate> void testTypeValue(T union, String type, Object typeValue)
  {
    String isTypeMethodName = TestDataTemplateUtil.methodName("is", type);
    String getTypeMethodName = TestDataTemplateUtil.methodName("get", type);
    Class<? extends UnionTemplate> unionClass = union.getClass();
    try
    {
      Method[] methods = unionClass.getMethods();
      boolean foundIsMethod = false;
      boolean foundGetMethod = false;
      for (Method method : methods)
      {
        String methodName = method.getName();
        if (methodName.startsWith("is"))
        {
          boolean expectedValue = methodName.equals(isTypeMethodName);
          Boolean value = (Boolean) method.invoke(union);
          assertEquals(value.booleanValue(), expectedValue);
          foundIsMethod = true;
        }
        if (methodName.startsWith("get") && methodName.equals("getClass") == false)
        {
          Object expectedGetValue = methodName.equals(getTypeMethodName) ? typeValue : null;
          Object getValue = method.invoke(union);
          assertEquals(getValue, expectedGetValue);
          foundGetMethod = true;
        }
      }
      assertTrue(foundGetMethod);
      assertTrue(foundIsMethod);
    }
    catch (IllegalAccessException ex)
    {
      throw new IllegalStateException(ex);
    }
    catch (InvocationTargetException ex)
    {
      throw new IllegalStateException(ex);
    }
  }

  private static <T extends UnionTemplate> void testTypeValue(Class<T> unionClass, String type, Object typeValue)
  {
    try
    {
      // constructor with argument
      Constructor<T> ctor = unionClass.getDeclaredConstructor(Object.class);
      DataMap dataMap = new DataMap();
      if (typeValue instanceof DataTemplate)
      {
        DataTemplate<?> dataTemplate = (DataTemplate<?>) typeValue;
        dataMap.put(dataTemplate.schema().getUnionMemberKey(), dataTemplate.data());
      }
      else if (typeValue instanceof Enum)
      {
        String key = DataTemplateUtil.getSchema(typeValue.getClass()).getUnionMemberKey();
        dataMap.put(key, typeValue.toString());
      }
      else
      {
        dataMap.put(type, typeValue);
      }

      T unionFromCtor = ctor.newInstance(dataMap);
      testTypeValue(unionFromCtor, type, typeValue);

      // constructor with no argument followed by set
      String setTypeMethodName = TestDataTemplateUtil.methodName("set", type);
      Method setMethod = unionClass.getMethod(setTypeMethodName, typeValue.getClass());
      T unionToSet = unionClass.getConstructor().newInstance();
      setMethod.invoke(unionToSet, typeValue);
      testTypeValue(unionToSet, type, typeValue);

      // create method
      Method createMethod = unionClass.getMethod("create", typeValue.getClass());
      @SuppressWarnings("unchecked")
      T unionFromCreate = (T) createMethod.invoke(null, typeValue);
      testTypeValue(unionFromCreate, type, typeValue);
    }
    catch (IllegalAccessException ex)
    {
      throw new IllegalStateException(ex);
    }
    catch (InvocationTargetException ex)
    {
      throw new IllegalStateException(ex);
    }
    catch (InstantiationException ex)
    {
      throw new IllegalStateException(ex);
    }
    catch (NoSuchMethodException ex)
    {
      throw new IllegalStateException(ex);
    }
  }

  @Test
  public void testAccessors() throws IOException
  {
    Object inputs[][] =
    {
      {
        "int", 1
      },
      {
        "long", 2L
      },
      {
        "float", 3.0f
      },
      {
        "double", 4.0
      },
      {
        "boolean", Boolean.TRUE
      },
      {
        "string", "abc"
      },
      {
        "bytes", ByteString.copyAvroString("xyz", false)
      },
      {
        "EnumFruits", EnumFruits.BANANA
      },
      {
        "RecordBar", new RecordBar().setLocation("exotic")
      },
      {
        "FixedMD5", new FixedMD5(ByteString.copyAvroString("0123456789abcdef", false))
      },
      {
        "array", new StringArray(Arrays.asList("a1", "b2", "c3"))
      },
      {
        "map", new LongMap(TestUtil.dataMapFromString("{ \"k1\" : \"v1\" }"))
      }
    };

    for (Object[] row : inputs)
    {
      testTypeValue(UnionTest.UnionWithNull.class, (String) row[0], row[1]);
    }
  }

  @Test
  public void testCloneNullValue() throws CloneNotSupportedException
  {
    // union, null value
    UnionTest.UnionWithNull union = new UnionTest.UnionWithNull(Data.NULL);
    UnionTest.UnionWithNull unionClone = union.clone();

    assertSame(unionClone.data(), Data.NULL);
    assertSame(unionClone.data(), union.data());
    assertEquals(unionClone, union);
  }

  @Test
  public void testClonePrimitiveValue() throws CloneNotSupportedException
  {
    // union, primitive value
    UnionTest.UnionWithNull union = new UnionTest.UnionWithNull();
    union.setInt(1);
    UnionTest.UnionWithNull unionClone = union.clone();
    assertSame(unionClone.getInt(), union.getInt());
    assertNotSame(unionClone.data(), union.data());

    unionClone.setInt(2);
    assertEquals(union.getInt().intValue(), 1);
    assertEquals(unionClone.getInt().intValue(), 2);
  }

  @Test
  public void testCloneRecordValue() throws CloneNotSupportedException
  {
    // union, record value
    UnionTest.UnionWithNull union = new UnionTest.UnionWithNull();
    union.setRecordBar(new RecordBar());
    union.getRecordBar().setLocation("near");
    UnionTest.UnionWithNull unionClone = union.clone();
    assertSame(unionClone.getRecordBar().data(), union.getRecordBar().data());
    assertNotSame(unionClone.data(), union.data());

    unionClone.getRecordBar().setLocation("far");
    assertEquals(union.getRecordBar().getLocation(), "far");
    assertSame(unionClone.getRecordBar().getLocation(), union.getRecordBar().getLocation());
  }

  @Test
  public void testCopyNullValue() throws CloneNotSupportedException
  {
    // union, null value
    UnionTest.UnionWithNull union = new UnionTest.UnionWithNull(Data.NULL);
    UnionTest.UnionWithNull unionCopy = union.copy();

    assertTrue(TestUtil.noCommonDataComplex(unionCopy.data(), union.data()));
    assertSame(unionCopy.data(), Data.NULL);
    assertSame(unionCopy.data(), union.data());
    assertEquals(unionCopy, union);
  }

  @Test
  public void testCopyPrimitiveValue() throws CloneNotSupportedException
  {
    // union, primitive value
    UnionTest.UnionWithNull union = new UnionTest.UnionWithNull();
    union.setInt(1);
    UnionTest.UnionWithNull unionCopy = union.copy();
    assertTrue(TestUtil.noCommonDataComplex(unionCopy.data(), union.data()));
    assertSame(unionCopy.getInt(), union.getInt());
    assertNotSame(unionCopy.data(), union.data());

    unionCopy.setInt(2);
    assertEquals(union.getInt().intValue(), 1);
    assertEquals(unionCopy.getInt().intValue(), 2);
  }

  @Test
  public void testCopyRecordValue() throws CloneNotSupportedException
  {
    // union, record value
    UnionTest.UnionWithNull union = new UnionTest.UnionWithNull();
    union.setRecordBar(new RecordBar());
    union.getRecordBar().setLocation("near");
    UnionTest.UnionWithNull unionCopy = union.copy();
    assertTrue(TestUtil.noCommonDataComplex(unionCopy.data(), union.data()));
    assertNotSame(unionCopy.getRecordBar().data(), union.getRecordBar().data());
    assertNotSame(unionCopy.data(), union.data());

    unionCopy.getRecordBar().setLocation("far");
    assertEquals(unionCopy.getRecordBar().getLocation(), "far");
    assertEquals(union.getRecordBar().getLocation(), "near");
  }
}

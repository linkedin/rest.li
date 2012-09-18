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

package com.linkedin.pegasus.generator.test;

import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RequiredFieldNotPresentException;
import com.linkedin.data.template.SetMode;
import java.lang.reflect.Method;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class TestRecord
{
  @Test
  public void testIntField()
  {
    Exception exc = null;
    RecordTest record = new RecordTest();

    Integer intValue = 13;
    assertFalse(record.hasIntField());
    assertEquals(record.getIntField(GetMode.NULL), null);
    assertEquals(record.getIntField(GetMode.DEFAULT), null);
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

    record.setIntField(intValue);
    assertTrue(record.hasIntField());
    assertEquals(record.getIntField(GetMode.NULL), intValue);
    assertEquals(record.getIntField(GetMode.DEFAULT), intValue);
    assertEquals(record.getIntField(GetMode.STRICT), intValue);
    assertEquals(record.getIntField(), intValue);
  }

  public void dumpMethods(Class<?> clazz)
  {
    for (Method m : clazz.getMethods())
    {
      System.out.print(m.getName());
      for (Class<?> c : m.getParameterTypes())
      {
        System.out.print(" " + c.getName());
      }
      System.out.println();
    }
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
    Exception exc = null;
    RecordTest record = new RecordTest();

    Integer intValue = 13;
    assertFalse(record.hasIntOptionalField());
    assertEquals(record.getIntOptionalField(GetMode.NULL), null);
    assertEquals(record.getIntOptionalField(GetMode.DEFAULT), null);
    assertEquals(record.getIntOptionalField(GetMode.STRICT), null);
    assertEquals(record.getIntOptionalField(), null);

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
    Exception exc = null;
    RecordTest record = new RecordTest();

    Integer defaultValue = 17;
    Integer intValue = 13;
    assertFalse(record.hasIntDefaultField());
    assertEquals(record.getIntDefaultField(GetMode.NULL), null);
    assertEquals(record.getIntDefaultField(GetMode.DEFAULT), defaultValue);
    assertEquals(record.getIntDefaultField(GetMode.STRICT), defaultValue);
    assertEquals(record.getIntDefaultField(), defaultValue);

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
    Exception exc = null;
    RecordTest record = new RecordTest();

    Integer defaultValue = 42;
    Integer intValue = 13;
    assertFalse(record.hasIntDefaultOptionalField());
    assertEquals(record.getIntDefaultOptionalField(GetMode.NULL), null);
    assertEquals(record.getIntDefaultOptionalField(GetMode.DEFAULT), defaultValue);
    assertEquals(record.getIntDefaultOptionalField(GetMode.STRICT), defaultValue);
    assertEquals(record.getIntDefaultOptionalField(), defaultValue);

    record.setIntDefaultOptionalField(intValue);
    assertTrue(record.hasIntDefaultOptionalField());
    assertEquals(record.getIntDefaultOptionalField(GetMode.NULL), intValue);
    assertEquals(record.getIntDefaultOptionalField(GetMode.DEFAULT), intValue);
    assertEquals(record.getIntDefaultOptionalField(GetMode.STRICT), intValue);
    assertEquals(record.getIntDefaultOptionalField(), intValue);
  }
}

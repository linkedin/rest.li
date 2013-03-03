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

import org.testng.Assert;
import org.testng.annotations.Test;

import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RequiredFieldNotPresentException;
import com.linkedin.data.template.SetMode;

import static org.testng.Assert.*;


public class TestRecord
{
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
  public void testClone() throws CloneNotSupportedException
  {
    RecordTest record = new RecordTest();
    record.setBooleanField(true);
    record.setDoubleField(5.6);
    record.setEnumField(EnumFruits.APPLE);
    record.setRecordField(new RecordBar());
    RecordTest clone = record.clone();
    assertEquals(record, clone);
    Assert.assertNotSame(record, clone);
    Assert.assertNotSame(record.data(), clone.data());
    assertSame(record.getRecordField(), clone.getRecordField());
  }

  @Test
  public void testCopy() throws CloneNotSupportedException
  {
    RecordTest record = new RecordTest();
    record.setBooleanField(true);
    record.setDoubleField(5.6);
    record.setEnumField(EnumFruits.APPLE);
    RecordBar recordBar = new RecordBar();
    recordBar.setLocation("foo");
    record.setRecordField(recordBar);
    RecordTest copy = record.copy();
    assertEquals(record, copy);
    Assert.assertNotSame(record, copy);
    Assert.assertNotSame(record.data(), copy.data());
    Assert.assertNotSame(record.getRecordField(), copy.getRecordField());
    record.getRecordField().setLocation("bar");
    assertEquals(copy.getRecordField().getLocation(), "foo");
  }
}

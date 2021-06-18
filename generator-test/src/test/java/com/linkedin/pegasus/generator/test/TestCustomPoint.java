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

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.TestUtil;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.SetMode;
import com.linkedin.data.template.TestCustom.CustomPoint;
import com.linkedin.data.template.TestCustom.CustomPoint.CustomPointCoercer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.*;
import static org.testng.Assert.*;

public class TestCustomPoint
{
  @BeforeSuite
  public void testInitializer()
  {
    CustomPointRecord r = new CustomPointRecord();
    assertTrue(DataTemplateUtil.hasCoercer(CustomPoint.class));
  }

  @Test
  public void testCustomPointRecord()
  {
    CustomPoint input[] = { new CustomPoint("1,1"), new CustomPoint("2,2"), new CustomPoint("3,3") };
    CustomPointRecord foo = new CustomPointRecord();
    for (CustomPoint p : input)
    {
      foo.setCustomPoint(p);
      assertTrue(foo.hasCustomPoint());
      assertEquals(foo.getCustomPoint(), p);
      foo.removeCustomPoint();
      assertFalse(foo.hasCustomPoint());
    }

    CustomPointRecord foo1 = new CustomPointRecord();
    foo1.setCustomPoint(input[2]);
    CustomPointRecord foo2 = new CustomPointRecord();
    foo2.setCustomPoint(input[2]);
    assertEquals(foo1, foo2);
    assertEquals(foo1.data(), foo2.data());

    Exception exc = null;
    try
    {
      foo.setCustomPoint(null, SetMode.DISALLOW_NULL);
    }
    catch (RuntimeException e)
    {
      exc = e;
    }
    assertTrue(exc instanceof NullPointerException);
    foo.setCustomPoint(input[1], SetMode.DISALLOW_NULL);
    foo.setCustomPoint(null, SetMode.IGNORE_NULL);
    assertEquals(foo.getCustomPoint(), input[1]);
    foo.setCustomPoint(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo.hasCustomPoint());
  }

  @Test
  public void testCustomPointRecordUnion() throws CloneNotSupportedException
  {
    CustomPoint point = new CustomPoint("1,1");
    CustomPointRecord record = new CustomPointRecord();
    CustomPointRecord.CustomPointUnion u = new CustomPointRecord.CustomPointUnion();
    u.setCustomPoint(point);
    record.setCustomPointUnion(u);

    CustomPointRecord recordCopy = new CustomPointRecord(record.data().copy());
    assertEquals(recordCopy, record);
    assertTrue(recordCopy.getCustomPointUnion().isCustomPoint());
    assertEquals(recordCopy.getCustomPointUnion().getCustomPoint(), point);

    Integer i = 66;
    record.getCustomPointUnion().setInt(i);
    assertTrue(record.getCustomPointUnion().isInt());
    assertEquals(record.getCustomPointUnion().getInt(), i);

    // recordCopy has not changed
    assertTrue(recordCopy.getCustomPointUnion().isCustomPoint());
    assertEquals(recordCopy.getCustomPointUnion().getCustomPoint(), point);
  }

  @Test
  public void testCustomPointRecordArray() throws CloneNotSupportedException
  {
    final List<String> input = new ArrayList<>(Arrays.asList("1,1", "2,2", "3,3"));
    final DataList inputDataList = new DataList(input);

    CustomPointRecord record = new CustomPointRecord();
    CustomPointArray a1 = new CustomPointArray(inputDataList);
    record.setCustomPointArray(a1);

    CustomPointRecord recordCopy = new CustomPointRecord(record.data().copy());
    for (int i = 0; i < input.size(); i++)
    {
      assertEquals(recordCopy.getCustomPointArray().get(i), new CustomPoint(input.get(i)));
    }
  }

  @Test
  public void testCustomPointRecordMap() throws CloneNotSupportedException
  {
    final Map<String, CustomPoint> input = asMap("1", new CustomPoint("1,1"), "2", new CustomPoint("2,2"), "3", new CustomPoint("3,3"));
    final DataMap inputDataMap = new DataMap(asMap("1", "1,1", "2", "2,2", "3", "3,3"));

    CustomPointRecord record = new CustomPointRecord();
    CustomPointMap a1 = new CustomPointMap(inputDataMap);
    record.setCustomPointMap(a1);

    CustomPointRecord recordCopy = new CustomPointRecord(record.data().copy());
    for (Map.Entry<String, CustomPoint> e : input.entrySet())
    {
      assertEquals(recordCopy.getCustomPointMap().get(e.getKey()), e.getValue());
    }
  }

  @Test
  public void testCustomPointArray() throws IOException
  {
    final List<String> input = new ArrayList<>(Arrays.asList("1,1", "2,2", "3,3"));
    final DataList inputDataList = new DataList(input);
    final String customPointArraySchemaText = "{\"type\":\"array\",\"items\":{\"type\":\"typeref\",\"name\":\"CustomPoint\",\"namespace\":\"com.linkedin.pegasus.generator.test\",\"ref\":\"string\",\"java\":{\"class\":\"com.linkedin.data.template.TestCustom.CustomPoint\"}}}";

    CustomPointArray a1 = new CustomPointArray();
    assertEquals(a1.schema(), TestUtil.dataSchemaFromString(customPointArraySchemaText));

    for (String s : input)
    {
      a1.add(new CustomPoint(s));
      assertTrue(a1.contains(new CustomPoint(s)));
    }
    CustomPointArray a2 = new CustomPointArray(inputDataList);
    assertEquals(a1, a2);
    assertEquals(a1.data(), a2.data());
    for (String s : input)
    {
      assertTrue(a2.contains(new CustomPoint(s)));
    }

    for (int i = 0; i < input.size(); i++)
    {
      CustomPoint p = a1.get(i);
      assertEquals(p, new CustomPoint(input.get(i)));
    }

    CustomPointArray a3 = new CustomPointArray(input.size());
    for (int i = 0; i < input.size(); i++)
    {
      a3.add(new CustomPoint(input.get(i)));
      assertEquals(a3.get(i), new CustomPoint(input.get(i)));
    }

    for (int i = 0; i < input.size(); i++)
    {
      int j = input.size() - i - 1;
      a3.set(j, new CustomPoint(input.get(i)));
      assertEquals(a3.get(j), new CustomPoint(input.get(i)));
    }
  }

  @Test
  public void testCustomPointMap() throws IOException
  {
    final Map<String, CustomPoint> input = asMap("1", new CustomPoint("1,1"), "2", new CustomPoint("2,2"), "3", new CustomPoint("3,3"));
    final DataMap inputDataMap = new DataMap(asMap("1", "1,1", "2", "2,2", "3", "3,3"));
    final String customPointMapSchemaText = "{\"type\":\"map\",\"values\":{\"type\":\"typeref\",\"name\":\"CustomPoint\",\"namespace\":\"com.linkedin.pegasus.generator.test\",\"ref\":\"string\",\"java\":{\"class\":\"com.linkedin.data.template.TestCustom.CustomPoint\"}}}";

    CustomPointMap a1 = new CustomPointMap();
    assertEquals(a1.schema(), TestUtil.dataSchemaFromString(customPointMapSchemaText));

    for (Map.Entry<String, CustomPoint> e : input.entrySet())
    {
      a1.put(e.getKey(), e.getValue());
      assertTrue(a1.containsKey(e.getKey()));
      assertTrue(a1.containsValue(e.getValue()));
    }
    CustomPointMap a2 = new CustomPointMap(inputDataMap);
    assertEquals(a1, a2);
    assertEquals(a1.data(), a2.data());
    for (Map.Entry<String, CustomPoint> e : input.entrySet())
    {
      assertTrue(a2.containsKey(e.getKey()));
      assertTrue(a2.containsValue(e.getValue()));
    }

    for (Map.Entry<String, CustomPoint> e : input.entrySet())
    {
      CustomPoint p = a1.get(e.getKey());
      assertEquals(p, e.getValue());
    }

    CustomPointMap a3 = new CustomPointMap(input.size());
    for (Map.Entry<String, CustomPoint> e : input.entrySet())
    {
      String j = e.getKey() + "_";
      a3.put(j, e.getValue());
      assertEquals(a3.get(j), e.getValue());
    }
  }

  @Test
  public void testCustomPointUnionMember()
  {
    CustomPoint input[] = { new CustomPoint("1,1"), new CustomPoint("2,2"), new CustomPoint("3,3") };

    CustomPointRecord.CustomPointUnion u = new CustomPointRecord.CustomPointUnion();
    assertFalse(u.isCustomPoint());
    assertNull(u.getCustomPoint());

    Integer i = 66;
    for (CustomPoint p : input)
    {
      u.setCustomPoint(p);
      assertTrue(u.isCustomPoint());
      assertEquals(u.getCustomPoint(), p);
      assertFalse(u.isInt());
      assertNull(u.getInt());

      u.setInt(i);
      assertFalse(u.isCustomPoint());
      assertNull(u.getCustomPoint());
      assertTrue(u.isInt());
      assertEquals(u.getInt(), i);

      i += 11;
    }
  }

  private static class CustomPointRecordWithPublicObtainCustomType extends CustomPointRecord
  {
    // in order to verify the call count of the protected method from the test, we need to promote its access permission
    // generally not a good pattern to follow, but we only do this in specific test
    @Override
    public <T> T obtainCustomType(RecordDataSchema.Field field, Class<T> valueClass, GetMode mode)
    {
      return super.obtainCustomType(field, valueClass, mode);
    }
  }

  private static class CustomPointCoercer2 extends CustomPointCoercer
  {
  }

  @Test
  public void testCoercerRegistrationOverride()
  {
    try
    {
      Custom.registerCoercer(new CustomPointCoercer(), CustomPoint.class);
      Custom.registerCoercer(new CustomPointCoercer(), CustomPoint.class);
    }
    catch (IllegalArgumentException e)
    {
      fail("coercer registration failed for repeat registration of the same coercer, which is allowed");
    }

    try
    {
      Custom.registerCoercer(new CustomPointCoercer2(), CustomPoint.class);
      fail("coercer registration failed to throw IllegalArgumentException when a coercer was " +
        "registered for a class that already had been registered with a different coercer.");
    }
    catch (IllegalArgumentException e)
    {
      // expected
    }
  }
}

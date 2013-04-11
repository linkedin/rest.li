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


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.asMap;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;


public class TestCustom
{
  public static class CustomPoint
  {
    private int _x;
    private int _y;

    public CustomPoint(String s)
    {
      String parts[] = s.split(",");
      _x = Integer.parseInt(parts[0]);
      _y = Integer.parseInt(parts[1]);
    }

    public CustomPoint(int x, int y)
    {
      _x = x;
      _y = y;
    }

    public int getX()
    {
      return _x;
    }

    public int getY()
    {
      return _y;
    }

    public String toString()
    {
      return _x + "," + _y;
    }

    public boolean equals(Object o)
    {
      if (o == null)
        return false;
      if (this == o)
        return true;
      if (o.getClass() != getClass())
        return false;
      CustomPoint p = (CustomPoint) o;
      return (p._x == _x) && (p._y == _y);
    }

    public static class CustomPointCoercer implements DirectCoercer<CustomPoint>
    {
      @Override
      public Object coerceInput(CustomPoint object)
        throws ClassCastException
      {
        return object.toString();
      }

      @Override
      public CustomPoint coerceOutput(Object object)
        throws TemplateOutputCastException
      {
        if (object instanceof String == false)
        {
          throw new TemplateOutputCastException("Output " + object + " is not a string, and cannot be coerced to " + CustomPoint.class.getName());
        }
        return new CustomPoint((String) object);
      }
    }

    static
    {
      Custom.registerCoercer(new CustomPointCoercer(), CustomPoint.class);
    }
  }

  public static class CustomPointArray extends com.linkedin.data.template.DirectArrayTemplate<CustomPoint>
  {
    // missing custom Java class attributes, not needed for testing
    public static final ArrayDataSchema SCHEMA = (ArrayDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"array\", \"items\" : \"string\" }");

    public CustomPointArray()
    {
      this(new DataList());
    }
    public CustomPointArray(int capacity)
    {
      this(new DataList(capacity));
    }
    public CustomPointArray(Collection<CustomPoint> c)
    {
      this(new DataList(c.size()));
      addAll(c);
    }
    public CustomPointArray(DataList list)
    {
      super(list, SCHEMA, CustomPoint.class, String.class);
    }
  }

  @BeforeSuite
  public void testInitialization()
  {
    assertFalse(DataTemplateUtil.hasCoercer(CustomPoint.class));
    Custom.initializeCustomClass(CustomPoint.class);
    assertTrue(DataTemplateUtil.hasCoercer(CustomPoint.class));
  }

  @Test
  public void testCustomPointArray()
  {
    final List<String> input = new ArrayList<String>(Arrays.asList("1,1", "2,2", "3,3"));
    final DataList inputDataList = new DataList(input);

    CustomPointArray a1 = new CustomPointArray();
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

  public static class CustomPointMap extends com.linkedin.data.template.DirectMapTemplate<CustomPoint>
  {
    // missing custom Java class attributes, not needed for testing
    public static final MapDataSchema SCHEMA = (MapDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"map\", \"values\" : \"string\" }");

    public CustomPointMap()
    {
      this(new DataMap());
    }
    public CustomPointMap(int capacity)
    {
      this(new DataMap(capacity));
    }
    public CustomPointMap(Map<String, CustomPoint> m)
    {
      this(newDataMapOfSize(m.size()));
      putAll(m);
    }
    public CustomPointMap(DataMap list)
    {
      super(list, SCHEMA, CustomPoint.class, String.class);
    }
  }

  @Test
  public void testCustomPointMap()
  {
    final Map<String, CustomPoint> input = asMap("1", new CustomPoint("1,1"), "2", new CustomPoint("2,2"), "3", new CustomPoint("3,3"));
    final DataMap inputDataMap = new DataMap(asMap("1", "1,1", "2", "2,2", "3", "3,3"));

    CustomPointMap a1 = new CustomPointMap();
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

  public static class Foo extends RecordTemplate
  {
    public final static RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema
      (
        "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [\n" +
        "{ \"name\" : \"point\", \"type\" : \"string\", \"default\" : \"default_string\" } \n" +
        "] }"
      );


    static public final RecordDataSchema.Field FIELD_point = SCHEMA.getField("point");

    public Foo()
    {
      super(new DataMap(), SCHEMA);
    }

    public Foo(DataMap map)
    {
      super(map, SCHEMA);
    }

    public boolean hasCustomPoint()
    {
      return contains(FIELD_point);
    }

    public CustomPoint getCustomPoint()
    {
      return getCustomPoint(GetMode.STRICT);
    }

    public CustomPoint getCustomPoint(GetMode mode)
    {
      return obtainDirect(FIELD_point, CustomPoint.class, mode);
    }

    public void removeCustomPoint()
    {
      remove(FIELD_point);
    }

    public Foo setCustomPoint(CustomPoint value)
    {
      putDirect(FIELD_point, CustomPoint.class, String.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setCustomPoint(CustomPoint value, SetMode mode)
    {
      putDirect(FIELD_point, CustomPoint.class, String.class, value, mode);
      return this;
    }
  }

  @Test
  public void testCustomPointField()
  {
    CustomPoint input[] = { new CustomPoint("1,1"), new CustomPoint("2,2"), new CustomPoint("3,3") };
    Foo foo = new Foo();
    for (CustomPoint p : input)
    {
      foo.setCustomPoint(p);
      assertTrue(foo.hasCustomPoint());
      assertEquals(foo.getCustomPoint(), p);
      foo.removeCustomPoint();
      assertFalse(foo.hasCustomPoint());
    }

    Foo foo1 = new Foo();
    foo1.setCustomPoint(input[2]);
    Foo foo2 = new Foo();
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

  public final static class Union extends UnionTemplate
  {
    private final static UnionDataSchema SCHEMA = ((UnionDataSchema) DataTemplateUtil.parseSchema("[{\"type\":\"typeref\",\"name\":\"CustomPoint\",\"ref\":\"string\"}, \"int\"]"));
    private final static DataSchema MEMBER_CustomPoint = SCHEMA.getType("string");
    private final static DataSchema MEMBER_int = SCHEMA.getType("int");

    public Union() {
      super(new DataMap(), SCHEMA);
    }

    public Union(Object data) {
      super(data, SCHEMA);
    }

    public boolean isCustomPoint() {
      return memberIs("string");
    }

    public CustomPoint getCustomPoint() {
      return obtainDirect(MEMBER_CustomPoint, CustomPoint.class, "string");
    }

    public void setCustomPoint(CustomPoint value) {
      selectDirect(MEMBER_CustomPoint, CustomPoint.class, String.class, "string", value);
    }

    public boolean isInt() {
      return memberIs("int");
    }

    public Integer getInt() {
      return obtainDirect(MEMBER_int, Integer.class, "int");
    }

    public void setInt(Integer value) {
      selectDirect(MEMBER_int, Integer.class, Integer.class, "int", value);
    }
  }

  @Test
  public void testCustomPointUnionMember()
  {
    CustomPoint input[] = { new CustomPoint("1,1"), new CustomPoint("2,2"), new CustomPoint("3,3") };

    Union u = new Union();
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
}

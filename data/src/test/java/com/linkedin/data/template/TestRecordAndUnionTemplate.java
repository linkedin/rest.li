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


import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.IntegerDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.Test;

import static com.linkedin.data.TestUtil.asList;
import static com.linkedin.data.TestUtil.asMap;
import static org.testng.Assert.*;

/**
 * Unit tests for record and union {@link DataTemplate}'s.
 *
 * @author slim
 */
public class TestRecordAndUnionTemplate
{
  public static class Bar extends RecordTemplate
  {
    public final static RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema
    (
      "{ \"type\" : \"record\", \"name\" : \"Bar\", \"fields\" : [ { \"name\" : \"int\", \"type\" : \"int\" } ] }"
    );
    private final static RecordDataSchema.Field FIELD_int = SCHEMA.getField("int");

    public Bar()
    {
      super(new DataMap(), SCHEMA);
    }

    public Bar(DataMap map)
    {
      super(map, SCHEMA);
    }

    public Integer getInt()
    {
      return getInt(GetMode.STRICT);
    }

    public Integer getInt(GetMode mode)
    {
      return obtainDirect(FIELD_int, Integer.TYPE, mode);
    }

    public void removeInt()
    {
      remove(FIELD_int);
    }

    public Bar setInt(int value)
    {
      putDirect(FIELD_int, Integer.class, Integer.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Bar setInt(int value, SetMode mode)
    {
      putDirect(FIELD_int, Integer.class, Integer.class, value, mode);
      return this;
    }
  }

  public static enum EnumType
  {
    APPLE,
    ORANGE,
    BANANA,
    $UNKNOWN
  }

  public static class FixedType extends FixedTemplate
  {
    public static final FixedDataSchema SCHEMA = (FixedDataSchema) DataTemplateUtil.parseSchema("{ \"type\" : \"fixed\", \"name\" : \"fixedType\", \"size\" : 4 }");

    public FixedType(Object object)
    {
      super(object, SCHEMA);
    }
  }

  public static class Foo extends RecordTemplate
  {

    public final static RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema
    (
     "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [\n" +
     "{ \"name\" : \"boolean\", \"type\" : \"boolean\", \"default\" : true }, \n" +
     "{ \"name\" : \"int\", \"type\" : \"int\", \"default\" : -1 }, \n" +
     "{ \"name\" : \"long\", \"type\" : \"long\" }, \n" +
     "{ \"name\" : \"float\", \"type\" : \"float\", \"optional\" : true }, \n" +
     "{ \"name\" : \"double\", \"type\" : \"double\", \"optional\" : true, \"default\" : -4.0 }, \n" +
     "{ \"name\" : \"string\", \"type\" : \"string\", \"default\" : \"default_string\" }, \n" +
     "{ \"name\" : \"bytes\", \"type\" : \"bytes\", \"default\" : \"default_bytes\" }, \n" +
     "{ \"name\" : \"array\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\" }, \"default\" : [ -1, -2, -3, -4 ] }, \n" +
     "{ \"name\" : \"recordArray\", \"type\" : { \"type\" : \"array\", \"items\" : \"Foo\" }, \"default\" : [ ] }, \n" +
     "{ \"name\" : \"enum\", \"type\" : { \"type\" : \"enum\", \"name\" : \"EnumType\", \"symbols\" : [ \"APPLE\", \"ORANGE\", \"BANANA\" ] }, \"default\" : \"APPLE\" }, \n" +
     "{ \"name\" : \"fixed\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"fixedType\", \"size\" : 4 }, \"default\" : \"1234\" }, \n" +
     "{ \"name\" : \"map\", \"type\" : { \"type\" : \"map\", \"values\" : \"int\" }, \"default\" : { \"key1\" : -5 } }, \n" +
     "{ \"name\" : \"record\", \"type\" : { \"type\" : \"record\", \"name\" : \"Bar\", \"fields\" : [ { \"name\" : \"int\", \"type\" : \"int\" } ] }, \"default\" : { \"int\" : -6 } }, \n" +
     "{ \"name\" : \"recordNoDefault\", \"type\" : \"Bar\" }, \n" +
     "{ \"name\" : \"recordOptional\", \"type\" : \"Bar\", \"optional\" : true }, \n" +
     "{ \"name\" : \"union\", \"type\" : [ \"int\", \"Bar\", \"EnumType\", \"fixedType\", \"Foo\" ], \"default\" : { \"EnumType\" : \"ORANGE\"} }, \n" +
     "{ \"name\" : \"unionWithNull\", \"type\" : [ \"null\", \"EnumType\", \"fixedType\" ], \"default\" : null } \n" +
     "] }"
    );

    static public final RecordDataSchema.Field FIELD_boolean = SCHEMA.getField("boolean");
    static public final RecordDataSchema.Field FIELD_int = SCHEMA.getField("int");
    static public final RecordDataSchema.Field FIELD_long = SCHEMA.getField("long");
    static public final RecordDataSchema.Field FIELD_float = SCHEMA.getField("float");
    static public final RecordDataSchema.Field FIELD_double = SCHEMA.getField("double");
    static public final RecordDataSchema.Field FIELD_string = SCHEMA.getField("string");
    static public final RecordDataSchema.Field FIELD_bytes = SCHEMA.getField("bytes");

    static public final RecordDataSchema.Field FIELD_enum = SCHEMA.getField("enum");

    static public final RecordDataSchema.Field FIELD_fixed = SCHEMA.getField("fixed");
    static public final RecordDataSchema.Field FIELD_array = SCHEMA.getField("array");
    static public final RecordDataSchema.Field FIELD_arrayRecord = SCHEMA.getField("recordArray");
    static public final RecordDataSchema.Field FIELD_record = SCHEMA.getField("record");
    static public final RecordDataSchema.Field FIELD_recordNoDefault = SCHEMA.getField("recordNoDefault");
    static public final RecordDataSchema.Field FIELD_recordOptional = SCHEMA.getField("recordOptional");

    static public final RecordDataSchema.Field FIELD_union = SCHEMA.getField("union");

    public Foo()
    {
      super(new DataMap(), SCHEMA);
    }

    public Foo(DataMap map)
    {
      super(map, SCHEMA);
    }

    public boolean hasBoolean()
    {
      return contains(FIELD_boolean);
    }

    public Boolean isBoolean()
    {
      return isBoolean(GetMode.STRICT);
    }

    public Boolean isBoolean(GetMode mode)
    {
      return obtainDirect(FIELD_boolean, Boolean.class, mode);
    }

    public void removeBoolean()
    {
      remove(FIELD_boolean);
    }

    public Foo set1Boolean(boolean value)
    {
      // old generated code uses this
      putDirect(FIELD_boolean, Boolean.class, value);
      return this;
    }

    public Foo set2Boolean(boolean value)
    {
      // old generated code uses this
      putDirect(FIELD_boolean, Boolean.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo set2Boolean(Boolean value, SetMode mode)
    {
      // old generated code uses this
      putDirect(FIELD_boolean, Boolean.class, value, mode);
      return this;
    }

    public Foo setBoolean(boolean value)
    {
      putDirect(FIELD_boolean, Boolean.class, Boolean.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setBoolean(Boolean value, SetMode mode)
    {
      putDirect(FIELD_boolean, Boolean.class, Boolean.class, value, mode);
      return this;
    }

    public boolean hasInt()
    {
      return contains(FIELD_int);
    }

    public Integer getInt()
    {
      return getInt(GetMode.STRICT);
    }

    public Integer getInt(GetMode mode)
    {
      return obtainDirect(FIELD_int, Integer.class, mode);
    }

    public void removeInt()
    {
      remove(FIELD_int);
    }

    public Foo set1Int(int value)
    {
      // old generated code uses this
      putDirect(FIELD_int, Integer.class, value);
      return this;
    }

    public Foo set2Int(int value)
    {
      // old generated code uses this
      putDirect(FIELD_int, Integer.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo set2Int(Integer value, SetMode mode)
    {
      // old generated code uses this
      putDirect(FIELD_int, Integer.class, value, mode);
      return this;
    }

    public Foo setInt(int value)
    {
      putDirect(FIELD_int, Integer.class, Integer.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setInt(Integer value, SetMode mode)
    {
      putDirect(FIELD_int, Integer.class, Integer.class, value, mode);
      return this;
    }

    public boolean hasLong()
    {
      return contains(FIELD_long);
    }

    public Long getLong()
    {
      return getLong(GetMode.STRICT);
    }

    public Long getLong(GetMode mode)
    {
      return obtainDirect(FIELD_long, Long.class, mode);
    }

    public void removeLong()
    {
      remove(FIELD_long);
    }

    public Foo set1Long(long value)
    {
      // old generated code uses this
      putDirect(FIELD_long, Long.class, value);
      return this;
    }

    public Foo set2Long(long value)
    {
      // old generated code uses this
      putDirect(FIELD_long, Long.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo set2Long(Long value, SetMode mode)
    {
      // old generated code uses this
      putDirect(FIELD_long, Long.class, value, mode);
      return this;
    }

    public Foo setLong(long value)
    {
      putDirect(FIELD_long, Long.class, Long.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setLong(Long value, SetMode mode)
    {
      putDirect(FIELD_long, Long.class, Long.class, value, mode);
      return this;
    }

    public boolean hasFloat()
    {
      return contains(FIELD_float);
    }

    public Float getFloat()
    {
      return getFloat(GetMode.STRICT);
    }

    public Float getFloat(GetMode mode)
    {
      return obtainDirect(FIELD_float, Float.class, mode);
    }

    public void removeFloat()
    {
      remove(FIELD_float);
    }

    public Foo set1Float(float value)
    {
      // old generated code uses this
      putDirect(FIELD_float, Float.class, value);
      return this;
    }

    public Foo set2Float(float value)
    {
      // old generated code uses this
      putDirect(FIELD_float, Float.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo set2Float(Float value, SetMode mode)
    {
      // old generated code uses this
      putDirect(FIELD_float, Float.class, value, mode);
      return this;
    }

    public Foo setFloat(float value)
    {
      putDirect(FIELD_float, Float.class, Float.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setFloat(Float value, SetMode mode)
    {
      putDirect(FIELD_float, Float.class, Float.class, value, mode);
      return this;
    }

    public boolean hasDouble()
    {
      return contains(FIELD_double);
    }

    public Double getDouble()
    {
      return getDouble(GetMode.STRICT);
    }

    public Double getDouble(GetMode mode)
    {
      return obtainDirect(FIELD_double, Double.class, mode);
    }

    public void removeDouble()
    {
      remove(FIELD_double);
    }

    public Foo set1Double(double value)
    {
      // old generated code uses this
      putDirect(FIELD_double, Double.class, value);
      return this;
    }

    public Foo set2Double(double value)
    {
      // old generated code uses this
      putDirect(FIELD_double, Double.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo set2Double(Double value, SetMode mode)
    {
      // old generated code uses this
      putDirect(FIELD_double, Double.class, value, mode);
      return this;
    }

    public Foo setDouble(double value)
    {
      putDirect(FIELD_double, Double.class, Double.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setDouble(Double value, SetMode mode)
    {
      putDirect(FIELD_double, Double.class, Double.class, value, mode);
      return this;
    }

    public boolean hasString()
    {
      return contains(FIELD_string);
    }

    public String getString()
    {
      return getString(GetMode.STRICT);
    }

    public String getString(GetMode mode)
    {
      return obtainDirect(FIELD_string, String.class, mode);
    }

    public void removeString()
    {
      remove(FIELD_string);
    }

    public Foo set1String(String value)
    {
      // old generated code uses this
      putDirect(FIELD_string, String.class, value);
      return this;
    }

    public Foo set2String(String value)
    {
      // old generated code uses this
      putDirect(FIELD_string, String.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo set2String(String value, SetMode mode)
    {
      // old generated code uses this
      putDirect(FIELD_string, String.class, value, mode);
      return this;
    }

    public Foo setString(String value)
    {
      putDirect(FIELD_string, String.class, String.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setString(String value, SetMode mode)
    {
      putDirect(FIELD_string, String.class, String.class, value, mode);
      return this;
    }

    public boolean hasBytes()
    {
      return contains(FIELD_bytes);
    }

    public ByteString getBytes()
    {
      return getBytes(GetMode.STRICT);
    }

    public ByteString getBytes(GetMode mode)
    {
      return obtainDirect(FIELD_bytes, ByteString.class, mode);
    }

    public void removeBytes()
    {
      remove(FIELD_bytes);
    }

    public Foo set1Bytes(ByteString value)
    {
      // old generated code uses this
      putDirect(FIELD_bytes, ByteString.class, value);
      return this;
    }

    public Foo set2Bytes(ByteString value)
    {
      // old generated code uses this
      putDirect(FIELD_bytes, ByteString.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo set2Bytes(ByteString value, SetMode mode)
    {
      // old generated code uses this
      putDirect(FIELD_bytes, ByteString.class, value, mode);
      return this;
    }

    public Foo setBytes(ByteString value)
    {
      putDirect(FIELD_bytes, ByteString.class, ByteString.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setBytes(ByteString value, SetMode mode)
    {
      putDirect(FIELD_bytes, ByteString.class, ByteString.class, value, mode);
      return this;
    }

    public boolean hasEnum()
    {
      return contains(FIELD_enum);
    }

    public EnumType getEnum()
    {
      return getEnum(GetMode.STRICT);
    }

    public EnumType getEnum(GetMode mode)
    {
      return obtainDirect(FIELD_enum, EnumType.class, mode);
    }

    public void removeEnum()
    {
      remove(FIELD_enum);
    }

    public Foo set1Enum(EnumType value)
    {
      // old generated code uses this
      putDirect(FIELD_enum, EnumType.class, value);
      return this;
    }

    public Foo set2Enum(EnumType value)
    {
      // old generated code uses this
      putDirect(FIELD_enum, EnumType.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo set2Enum(EnumType value, SetMode mode)
    {
      // old generated code uses this
      putDirect(FIELD_enum, EnumType.class, value, mode);
      return this;
    }

    public Foo setEnum(EnumType value)
    {
      putDirect(FIELD_enum, EnumType.class, String.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setEnum(EnumType value, SetMode mode)
    {
      putDirect(FIELD_enum, EnumType.class, String.class, value, mode);
      return this;
    }

    public boolean hasFixed()
    {
      return contains(FIELD_fixed);
    }

    public FixedType getFixed()
    {
      return getFixed(GetMode.STRICT);
    }

    public FixedType getFixed(GetMode mode)
    {
      return obtainWrapped(FIELD_fixed, FixedType.class, mode);
    }

    public void removeFixed()
    {
      remove(FIELD_fixed);
    }

    public Foo set1Fixed(FixedType value)
    {
      // old generated code uses this
      putWrapped(FIELD_fixed, FixedType.class, value);
      return this;
    }

    public Foo setFixed(FixedType value)
    {
      putWrapped(FIELD_fixed, FixedType.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setFixed(FixedType value, SetMode mode)
    {
      putWrapped(FIELD_fixed, FixedType.class, value, mode);
      return this;
    }

    public boolean hasArray()
    {
      return contains(FIELD_array);
    }

    public IntegerArray getArray()
    {
      return getArray(GetMode.STRICT);
    }

    public IntegerArray getArray(GetMode mode)
    {
      return obtainWrapped(FIELD_array, IntegerArray.class, mode);
    }

    public void removeArray()
    {
      remove(FIELD_array);
    }

    public Foo set1Array(IntegerArray value)
    {
      // old generated code uses this
      putWrapped(FIELD_array, IntegerArray.class, value);
      return this;
    }

    public Foo setArray(IntegerArray value)
    {
      putWrapped(FIELD_array, IntegerArray.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setArray(IntegerArray value, SetMode mode)
    {
      putWrapped(FIELD_array, IntegerArray.class, value, mode);
      return this;
    }

    public boolean hasRecord()
    {
      return contains(FIELD_record);
    }

    public Bar getRecord()
    {
      return getRecord(GetMode.STRICT);
    }

    public Bar getRecord(GetMode mode)
    {
      return obtainWrapped(FIELD_record, Bar.class, mode);
    }

    public void removeRecord()
    {
      remove(FIELD_record);
    }

    public Foo set1Record(Bar value)
    {
      // old generated code uses this
      putWrapped(FIELD_record, Bar.class, value);
      return this;
    }

    public Foo setRecord(Bar value)
    {
      putWrapped(FIELD_record, Bar.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setRecord(Bar value, SetMode mode)
    {
      putWrapped(FIELD_record, Bar.class, value, mode);
      return this;
    }

    public boolean hasRecordNoDefault()
    {
      return contains(FIELD_recordNoDefault);
    }

    public Bar getRecordNoDefault()
    {
      return getRecordNoDefault(GetMode.STRICT);
    }

    public Bar getRecordNoDefault(GetMode mode)
    {
      return obtainWrapped(FIELD_recordNoDefault, Bar.class, mode);
    }

    public void removeRecordNoDefault()
    {
      remove(FIELD_recordNoDefault);
    }

    public Foo set1RecordNoDefault(Bar value)
    {
      // old generated code uses this
      putWrapped(FIELD_recordNoDefault, Bar.class, value);
      return this;
    }

    public Foo setRecordNoDefault(Bar value)
    {
      putWrapped(FIELD_recordNoDefault, Bar.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setRecordNoDefault(Bar value, SetMode mode)
    {
      putWrapped(FIELD_recordNoDefault, Bar.class, value, mode);
      return this;
    }

    public boolean hasRecordOptional()
    {
      return contains(FIELD_recordOptional);
    }

    public Bar getRecordOptional()
    {
      return getRecordOptional(GetMode.STRICT);
    }

    public Bar getRecordOptional(GetMode mode)
    {
      return obtainWrapped(FIELD_recordOptional, Bar.class, mode);
    }

    public void removeRecordOptional()
    {
      remove(FIELD_recordOptional);
    }

    public Foo set1RecordOptional(Bar value)
    {
      // old generated code uses this
      putWrapped(FIELD_recordOptional, Bar.class, value);
      return this;
    }

    public Foo setRecordOptional(Bar value)
    {
      putWrapped(FIELD_recordOptional, Bar.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setRecordOptional(Bar value, SetMode mode)
    {
      putWrapped(FIELD_recordOptional, Bar.class, value, mode);
      return this;
    }

    static public class Union extends UnionTemplate
    {
      static public final UnionDataSchema SCHEMA = (UnionDataSchema) FIELD_union.getType();
      static public final IntegerDataSchema MEMBER_int = (IntegerDataSchema) SCHEMA.getType("int");
      static public final EnumDataSchema MEMBER_EnumType = (EnumDataSchema) SCHEMA.getType("EnumType");
      static public final RecordDataSchema MEMBER_Bar = (RecordDataSchema) SCHEMA.getType("Bar");

      public Union()
      {
        super(Data.NULL, SCHEMA);
      }
      public Union(Object o)
      {
        super(o, SCHEMA);
      }

      public Integer getInt()
      {
        return obtainDirect(MEMBER_int, Integer.class, "int");
      }

      public void set2Int(int value)
      {
        // old generated code uses this
        selectDirect(MEMBER_int, Integer.class, "int", value);
      }

      public void setInt(int value)
      {
        selectDirect(MEMBER_int, Integer.class, Integer.class, "int", value);
      }

      public boolean isInt()
      {
        return memberIs("int");
      }

      public EnumType getEnumType()
      {
        return obtainDirect(MEMBER_EnumType, EnumType.class, "EnumType");
      }

      public void set2EnumType(EnumType value)
      {
        // old generated code uses this
        selectDirect(MEMBER_EnumType, EnumType.class, "EnumType", value);
      }

      public void setEnumType(EnumType value)
      {
        selectDirect(MEMBER_EnumType, EnumType.class, String.class, "EnumType", value);
      }

      public boolean isEnumType()
      {
        return memberIs("EnumType");
      }

      public Bar getBar()
      {
        return obtainWrapped(MEMBER_Bar, Bar.class, "Bar");
      }

      public void setBar(Bar value)
      {
        selectWrapped(MEMBER_Bar, Bar.class, "Bar", value);
      }

      public boolean isBar()
      {
        return memberIs("Bar");
      }

    }

    public boolean hasUnion()
    {
      return contains(FIELD_union);
    }

    public Union getUnion()
    {
      return getUnion(GetMode.STRICT);
    }

    public Union getUnion(GetMode mode)
    {
      return obtainWrapped(FIELD_union, Union.class, mode);
    }

    public void removeUnion()
    {
      remove(FIELD_union);
    }

    public Foo setUnion(Union value)
    {
      putWrapped(FIELD_union, Union.class, value, SetMode.DISALLOW_NULL);
      return this;
    }

    public Foo setUnion(Union value, SetMode mode)
    {
      putWrapped(FIELD_union, Union.class, value, mode);
      return this;
    }

  }

  @Test
  public void testGetMode()
  {
    Exception exc = null;
    Foo foo1 = new Foo();

    // has default value of -1
    assertFalse(foo1.hasInt());
    assertTrue(foo1.getInt(GetMode.NULL) == null);
    assertEquals(foo1.getInt(GetMode.DEFAULT), new Integer(-1));
    assertEquals(foo1.getInt(GetMode.STRICT), new Integer(-1));
    assertEquals(foo1.getInt(), new Integer(-1));
    assertFalse(foo1.hasInt());
    foo1.setInt(42);
    assertTrue(foo1.hasInt());
    assertEquals(foo1.getInt(), new Integer(42));
    assertEquals(foo1.getInt(GetMode.NULL), new Integer(42));
    assertEquals(foo1.getInt(GetMode.DEFAULT), new Integer(42));
    assertEquals(foo1.getInt(GetMode.STRICT), new Integer(42));
    assertTrue(foo1.hasInt());
    foo1.removeInt();
    assertFalse(foo1.hasInt());
    assertTrue(foo1.getInt(GetMode.NULL) == null);
    assertEquals(foo1.getInt(GetMode.DEFAULT), new Integer(-1));
    assertEquals(foo1.getInt(GetMode.STRICT), new Integer(-1));
    assertEquals(foo1.getInt(), new Integer(-1));

    Bar bar1 = new Bar();
    bar1.setInt(888);

    assertFalse(foo1.hasRecord());
    assertTrue(foo1.getRecord(GetMode.NULL) == null);
    assertEquals(foo1.getRecord(GetMode.DEFAULT).getInt(), Integer.valueOf(-6));
    assertEquals(foo1.getRecord(GetMode.STRICT).getInt(), Integer.valueOf(-6));
    assertEquals(foo1.getRecord().getInt(), Integer.valueOf(-6));
    assertEquals(foo1.getRecord(GetMode.DEFAULT).getInt().intValue(), -6);
    assertEquals(foo1.getRecord(GetMode.STRICT).getInt().intValue(), -6);
    assertEquals(foo1.getRecord().getInt().intValue(), -6);
    assertFalse(foo1.hasRecord());
    foo1.setRecord(bar1);
    assertTrue(foo1.hasRecord());
    assertEquals(foo1.getRecord().getInt(), new Integer(888));
    assertEquals(foo1.getRecord(GetMode.NULL).getInt(), new Integer(888));
    assertEquals(foo1.getRecord(GetMode.DEFAULT).getInt(), new Integer(888));
    assertEquals(foo1.getRecord(GetMode.STRICT).getInt(), new Integer(888));
    assertTrue(foo1.hasRecord());
    foo1.removeRecord();
    assertFalse(foo1.hasRecord());
    assertTrue(foo1.getRecord(GetMode.NULL) == null);
    assertEquals(foo1.getRecord(GetMode.DEFAULT).getInt(), new Integer(-6));
    assertEquals(foo1.getRecord(GetMode.STRICT).getInt(), new Integer(-6));

    // required and no default value
    assertFalse(foo1.hasLong());
    assertTrue(foo1.getLong(GetMode.NULL) == null);
    assertTrue(foo1.getLong(GetMode.DEFAULT) == null);
    try
    {
      exc = null;
      foo1.getLong(GetMode.STRICT);
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
      foo1.getLong();
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof RequiredFieldNotPresentException);
    assertFalse(foo1.hasLong());
    foo1.setLong(42);
    assertTrue(foo1.hasLong());
    assertEquals(foo1.getLong(), new Long(42));
    assertEquals(foo1.getLong(GetMode.NULL), new Long(42));
    assertEquals(foo1.getLong(GetMode.DEFAULT), new Long(42));
    assertEquals(foo1.getLong(GetMode.STRICT), new Long(42));
    assertTrue(foo1.hasLong());
    foo1.removeLong();
    assertFalse(foo1.hasLong());
    assertTrue(foo1.getLong(GetMode.NULL) == null);
    assertTrue(foo1.getLong(GetMode.DEFAULT) == null);
    try
    {
      exc = null;
      foo1.getLong(GetMode.STRICT);
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
      foo1.getLong();
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof RequiredFieldNotPresentException);

    // optional and no default value
    assertFalse(foo1.hasFloat());
    assertTrue(foo1.getFloat(GetMode.NULL) == null);
    assertTrue(foo1.getFloat(GetMode.DEFAULT) == null);
    assertTrue(foo1.getFloat(GetMode.STRICT) == null);
    assertTrue(foo1.getFloat() == null);
    assertFalse(foo1.hasFloat());
    foo1.setFloat(17.0f);
    assertTrue(foo1.hasFloat());
    assertEquals(foo1.getFloat(), new Float(17.0f));
    assertEquals(foo1.getFloat(GetMode.NULL), new Float(17.0f));
    assertEquals(foo1.getFloat(GetMode.DEFAULT), new Float(17.0f));
    assertEquals(foo1.getFloat(GetMode.STRICT), new Float(17.0f));
    assertTrue(foo1.hasFloat());
    foo1.removeFloat();
    assertFalse(foo1.hasFloat());
    assertTrue(foo1.getFloat(GetMode.NULL) == null);
    assertTrue(foo1.getFloat(GetMode.DEFAULT) == null);
    assertTrue(foo1.getFloat(GetMode.STRICT) == null);
    assertTrue(foo1.getFloat() == null);

    // optional and has default value
    assertFalse(foo1.hasDouble());
    assertTrue(foo1.getDouble(GetMode.NULL) == null);
    assertEquals(foo1.getDouble(GetMode.DEFAULT), new Double(-4.0));
    assertEquals(foo1.getDouble(GetMode.STRICT), new Double(-4.0));
    assertEquals(foo1.getDouble(), new Double(-4.0));
    assertFalse(foo1.hasDouble());
    foo1.setDouble(87.0);
    assertTrue(foo1.hasDouble());
    assertEquals(foo1.getDouble(), new Double(87.0));
    assertEquals(foo1.getDouble(GetMode.NULL), new Double(87.0));
    assertEquals(foo1.getDouble(GetMode.DEFAULT), new Double(87.0));
    assertEquals(foo1.getDouble(GetMode.STRICT), new Double(87.0));
    assertTrue(foo1.hasDouble());
    foo1.removeDouble();
    assertFalse(foo1.hasDouble());
    assertEquals(foo1.getDouble(GetMode.DEFAULT), new Double(-4.0));
    assertEquals(foo1.getDouble(GetMode.STRICT), new Double(-4.0));
    assertEquals(foo1.getDouble(), new Double(-4.0));

    // record with no default value
    assertFalse(foo1.hasRecordNoDefault());
    assertTrue(foo1.getRecordNoDefault(GetMode.NULL) == null);
    assertTrue(foo1.getRecordNoDefault(GetMode.DEFAULT) == null);
    try
    {
      exc = null;
      foo1.getRecordNoDefault(GetMode.STRICT);
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
      foo1.getRecordNoDefault();
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof RequiredFieldNotPresentException);
    assertFalse(foo1.hasRecordNoDefault());
    foo1.setRecordNoDefault(bar1);
    assertTrue(foo1.hasRecordNoDefault());
    assertEquals(foo1.getRecordNoDefault().getInt(), new Integer(888));
    assertEquals(foo1.getRecordNoDefault(GetMode.NULL).getInt(), new Integer(888));
    assertEquals(foo1.getRecordNoDefault(GetMode.DEFAULT).getInt(), new Integer(888));
    assertEquals(foo1.getRecordNoDefault(GetMode.STRICT).getInt(), new Integer(888));
    assertTrue(foo1.hasRecordNoDefault());
    foo1.removeRecordNoDefault();
    assertFalse(foo1.hasRecordNoDefault());
    assertTrue(foo1.getRecordNoDefault(GetMode.NULL) == null);
    assertTrue(foo1.getRecordNoDefault(GetMode.DEFAULT) == null);
    try
    {
      exc = null;
      foo1.getRecordNoDefault(GetMode.STRICT);
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
      foo1.getRecordNoDefault();
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof RequiredFieldNotPresentException);
  }

  @Test
  public void testSetMode()
  {
    Exception exc = null;
    Foo foo1 = new Foo();

    //
    // mandatory primitive field
    //
    assertFalse(foo1.hasInt());
    foo1.setInt(42);
    assertEquals(foo1.getInt().intValue(), 42);
    foo1.setInt(44, SetMode.IGNORE_NULL);
    assertEquals(foo1.getInt().intValue(), 44);
    foo1.setInt(46, SetMode.REMOVE_IF_NULL);
    assertEquals(foo1.getInt().intValue(), 46);
    foo1.setInt(48, SetMode.REMOVE_OPTIONAL_IF_NULL);
    assertEquals(foo1.getInt().intValue(), 48);
    foo1.setInt(50, SetMode.DISALLOW_NULL);
    assertEquals(foo1.getInt().intValue(), 50);

    // optional primitive field
    assertFalse(foo1.hasDouble());
    foo1.setDouble(42.0);
    assertEquals(foo1.getDouble().doubleValue(), 42.0);
    foo1.setDouble(44.0, SetMode.IGNORE_NULL);
    assertEquals(foo1.getDouble().doubleValue(), 44.0);
    foo1.setDouble(46.0, SetMode.REMOVE_IF_NULL);
    assertEquals(foo1.getDouble().doubleValue(), 46.0);
    foo1.setDouble(48.0, SetMode.REMOVE_OPTIONAL_IF_NULL);
    assertEquals(foo1.getDouble().doubleValue(), 48.0);
    foo1.setDouble(50.0, SetMode.DISALLOW_NULL);
    assertEquals(foo1.getDouble().doubleValue(), 50.0);

    // mandatory complex field
    assertFalse(foo1.hasRecord());
    foo1.setRecord((new Bar()).setInt(42));
    assertEquals(foo1.getRecord().getInt().intValue(), 42);
    foo1.setRecord((new Bar()).setInt(44), SetMode.IGNORE_NULL);
    assertEquals(foo1.getRecord().getInt().intValue(), 44);
    foo1.setRecord((new Bar()).setInt(46), SetMode.REMOVE_IF_NULL);
    assertEquals(foo1.getRecord().getInt().intValue(), 46);
    foo1.setRecord((new Bar()).setInt(48), SetMode.REMOVE_OPTIONAL_IF_NULL);
    assertEquals(foo1.getRecord().getInt().intValue(), 48);
    foo1.setRecord((new Bar()).setInt(50), SetMode.DISALLOW_NULL);
    assertEquals(foo1.getInt().intValue(), 50);

    // mandatory complex field
    assertFalse(foo1.hasRecordOptional());
    foo1.setRecordOptional((new Bar()).setInt(42));
    assertEquals(foo1.getRecordOptional().getInt().intValue(), 42);
    foo1.setRecordOptional((new Bar()).setInt(44), SetMode.IGNORE_NULL);
    assertEquals(foo1.getRecordOptional().getInt().intValue(), 44);
    foo1.setRecordOptional((new Bar()).setInt(46), SetMode.REMOVE_IF_NULL);
    assertEquals(foo1.getRecordOptional().getInt().intValue(), 46);
    foo1.setRecordOptional((new Bar()).setInt(48), SetMode.REMOVE_OPTIONAL_IF_NULL);
    assertEquals(foo1.getRecordOptional().getInt().intValue(), 48);
    foo1.setRecordOptional((new Bar()).setInt(50), SetMode.DISALLOW_NULL);
    assertEquals(foo1.getInt().intValue(), 50);

    // primitive mandatory field with null
    // IGNORE_NULL
    foo1 = new Foo();
    foo1.setInt(null, SetMode.IGNORE_NULL);
    assertFalse(foo1.hasInt());
    foo1.setInt(42);
    foo1.setInt(null, SetMode.IGNORE_NULL);
    assertEquals(foo1.getInt(GetMode.NULL).intValue(), 42);
    // REMOVE_IF_NULL
    foo1.setInt(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo1.hasInt());
    foo1.setInt(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo1.hasInt());
    foo1.setInt(44, SetMode.REMOVE_IF_NULL);
    assertEquals(foo1.getInt(GetMode.NULL).intValue(), 44);
    // REMOVE_OPTIONAL_IF_NULL
    try
    {
      exc = null;
      foo1.setInt(null, SetMode.REMOVE_OPTIONAL_IF_NULL);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof IllegalArgumentException);
    assertEquals(foo1.getInt(GetMode.NULL).intValue(), 44);
    // does not matter if value is present
    foo1.removeInt();
    try
    {
      exc = null;
      foo1.setInt(null, SetMode.REMOVE_OPTIONAL_IF_NULL);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof IllegalArgumentException);
    assertFalse(foo1.hasInt());
    foo1.setInt(46, SetMode.REMOVE_OPTIONAL_IF_NULL);
    // DISALLOW_NULL
    try
    {
      exc = null;
      foo1.setInt(null, SetMode.DISALLOW_NULL);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof NullPointerException);
    assertEquals(foo1.getInt(GetMode.NULL).intValue(), 46);
    // does not matter if value is present
    foo1.removeInt();
    try
    {
      exc = null;
      foo1.setInt(null, SetMode.DISALLOW_NULL);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof NullPointerException);
    assertFalse(foo1.hasInt());
    foo1.setInt(48, SetMode.DISALLOW_NULL);
    assertEquals(foo1.getInt(GetMode.NULL).intValue(), 48);

    //
    // primitive optional field with null
    //
    // IGNORE_NULL
    foo1 = new Foo();
    foo1.setDouble(null, SetMode.IGNORE_NULL);
    assertFalse(foo1.hasDouble());
    foo1.setDouble(42.0);
    foo1.setDouble(null, SetMode.IGNORE_NULL);
    assertEquals(foo1.getDouble(GetMode.NULL).doubleValue(), 42.0);
    // REMOVE_IF_NULL
    foo1.setDouble(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo1.hasDouble());
    foo1.setDouble(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo1.hasDouble());
    foo1.setDouble(44.0, SetMode.REMOVE_IF_NULL);
    assertEquals(foo1.getDouble(GetMode.NULL).doubleValue(), 44.0);
    // REMOVE_OPTIONAL_IF_NULL
    foo1.setDouble(null, SetMode.REMOVE_OPTIONAL_IF_NULL);
    assertFalse(foo1.hasDouble());
    foo1.setDouble(null, SetMode.REMOVE_OPTIONAL_IF_NULL);
    assertFalse(foo1.hasDouble());
    foo1.setDouble(46.0, SetMode.REMOVE_OPTIONAL_IF_NULL);
    assertEquals(foo1.getDouble(GetMode.NULL).doubleValue(), 46.0);
    // DISALLOW_NULL
    try
    {
      exc = null;
      foo1.setDouble(null, SetMode.DISALLOW_NULL);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof NullPointerException);
    assertEquals(foo1.getDouble(GetMode.NULL).doubleValue(), 46.0);
    // does not matter if value is present
    foo1.removeDouble();
    try
    {
      exc = null;
      foo1.setDouble(null, SetMode.DISALLOW_NULL);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof NullPointerException);
    assertFalse(foo1.hasDouble());
    foo1.setDouble(48.0, SetMode.DISALLOW_NULL);
    assertEquals(foo1.getDouble(GetMode.NULL).doubleValue(), 48.0);

    //
    // complex mandatory field with null
    //
    // IGNORE_NULL
    foo1 = new Foo();
    foo1.setRecord(null, SetMode.IGNORE_NULL);
    assertFalse(foo1.hasRecord());
    foo1.setRecord((new Bar()).setInt(42));
    foo1.setRecord(null, SetMode.IGNORE_NULL);
    assertEquals(foo1.getRecord(GetMode.NULL).getInt().intValue(), 42);
    // REMOVE_IF_NULL
    foo1.setRecord(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo1.hasRecord());
    foo1.setRecord(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo1.hasRecord());
    foo1.setRecord((new Bar()).setInt(44), SetMode.REMOVE_IF_NULL);
    assertEquals(foo1.getRecord(GetMode.NULL).getInt().intValue(), 44);
    // REMOVE_OPTIONAL_IF_NULL
    try
    {
      exc = null;
      foo1.setRecord(null, SetMode.REMOVE_OPTIONAL_IF_NULL);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof IllegalArgumentException);
    assertEquals(foo1.getRecord(GetMode.NULL).getInt().intValue(), 44);
    // does not matter if value is present
    foo1.removeRecord();
    try
    {
      exc = null;
      foo1.setRecord(null, SetMode.REMOVE_OPTIONAL_IF_NULL);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof IllegalArgumentException);
    assertFalse(foo1.hasRecord());
    foo1.setRecord((new Bar()).setInt(46), SetMode.REMOVE_OPTIONAL_IF_NULL);
    // DISALLOW_NULL
    try
    {
      exc = null;
      foo1.setRecord(null, SetMode.DISALLOW_NULL);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof NullPointerException);
    assertEquals(foo1.getRecord(GetMode.NULL).getInt().intValue(), 46);
    // does not matter if value is present
    foo1.removeRecord();
    try
    {
      exc = null;
      foo1.setRecord(null, SetMode.DISALLOW_NULL);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof NullPointerException);
    assertFalse(foo1.hasRecord());
    foo1.setRecord((new Bar()).setInt(48), SetMode.DISALLOW_NULL);
    assertEquals(foo1.getRecord(GetMode.NULL).getInt().intValue(), 48);

    //
    // complex optional field with null
    //
    // IGNORE_NULL
    foo1 = new Foo();
    foo1.setRecordOptional(null, SetMode.IGNORE_NULL);
    assertFalse(foo1.hasRecordOptional());
    foo1.setRecordOptional((new Bar()).setInt(42));
    foo1.setRecordOptional(null, SetMode.IGNORE_NULL);
    assertEquals(foo1.getRecordOptional(GetMode.NULL).getInt().intValue(), 42);
    // REMOVE_IF_NULL
    foo1.setRecordOptional(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo1.hasRecordOptional());
    foo1.setRecordOptional(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo1.hasRecordOptional());
    foo1.setRecordOptional((new Bar()).setInt(44), SetMode.REMOVE_IF_NULL);
    assertEquals(foo1.getRecordOptional(GetMode.NULL).getInt().intValue(), 44);
    // REMOVE_OPTIONAL_IF_NULL
    foo1.setRecordOptional(null, SetMode.REMOVE_OPTIONAL_IF_NULL);
    assertFalse(foo1.hasRecordOptional());
    foo1.setRecordOptional(null, SetMode.REMOVE_OPTIONAL_IF_NULL);
    assertFalse(foo1.hasRecordOptional());
    foo1.setRecordOptional((new Bar()).setInt(46), SetMode.REMOVE_OPTIONAL_IF_NULL);
    assertEquals(foo1.getRecordOptional(GetMode.NULL).getInt().intValue(), 46);
    // DISALLOW_NULL
    try
    {
      exc = null;
      foo1.setRecordOptional(null, SetMode.DISALLOW_NULL);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof NullPointerException);
    assertEquals(foo1.getRecordOptional(GetMode.NULL).getInt().intValue(), 46);
    // does not matter if value is present
    foo1.removeRecordOptional();
    try
    {
      exc = null;
      foo1.setRecordOptional(null, SetMode.DISALLOW_NULL);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof NullPointerException);
    assertFalse(foo1.hasRecordOptional());
    foo1.setRecordOptional((new Bar()).setInt(48), SetMode.DISALLOW_NULL);
    assertEquals(foo1.getRecordOptional(GetMode.NULL).getInt().intValue(), 48);
  }

  @Test
  public void testSchema()
  {
    assertEquals(Bar.SCHEMA, (new Bar().schema()));
    assertEquals(Foo.SCHEMA, (new Foo().schema()));
    assertEquals(Foo.Union.SCHEMA, (new Foo.Union().schema()));
  }

  @Test
  public void testHashCode()
  {
    List<EnumType> good = Arrays.asList(EnumType.APPLE, EnumType.ORANGE, EnumType.BANANA);
    Foo foo = new Foo();
    Integer lastHashCode = null;
    for (EnumType i : good)
    {
      foo.setEnum(i);
      int newHashCode = foo.hashCode();
      if (lastHashCode != null)
      {
        assertTrue(newHashCode != lastHashCode);
      }
      assertEquals(newHashCode, foo.data().hashCode());
      lastHashCode = newHashCode;
    }
  }

  @Test
  public void testBooleanField()
  {
    List<Boolean> good = Arrays.asList(Boolean.FALSE, Boolean.TRUE);
    Foo foo = new Foo();
    for (Boolean aBoolean : good)
    {
      // Object
      foo.setBoolean(aBoolean);
      assertEquals(foo.isBoolean(), aBoolean);
      assertTrue(foo.hasBoolean());
      assertEquals(foo.toString(), "{boolean=" + aBoolean + "}");
      Exception exc = null;
      try
      {
        Foo fooClone = (Foo) foo.clone();
        assertEquals(foo, fooClone);
        fooClone.removeBoolean();
        assertTrue(foo.hasBoolean());
        assertEquals(foo.isBoolean(), aBoolean);
        assertFalse(fooClone.hasBoolean());
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assertTrue(exc == null);
      foo.removeBoolean();
      assertFalse(foo.hasBoolean());
      assertEquals(foo.toString(), "{}");

      // primitive
      foo.setBoolean(aBoolean);
      assertEquals(foo.isBoolean(), aBoolean);
      assertTrue(foo.hasBoolean());
      assertEquals(foo.toString(), "{boolean=" + aBoolean + '}');
      exc = null;
      try
      {
        Foo fooClone = (Foo) foo.clone();
        assertEquals(foo, fooClone);
        fooClone.removeBoolean();
        assertTrue(foo.hasBoolean());
        assertEquals(foo.isBoolean(), aBoolean);
        assertFalse(fooClone.hasBoolean());
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assertTrue(exc == null);
      foo.removeBoolean();
      assertFalse(foo.hasBoolean());
      assertEquals(foo.toString(), "{}");
    }

    List<?> badInput = asList(3, "abc", new DataList());

    DataMap map = new DataMap();
    foo = new Foo(map);
    for (Object bad : badInput)
    {
      map.put("boolean", bad);
      Exception exc = null;
      try
      {
        Boolean result = foo.isBoolean();
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    // legacy test
    Boolean t[] = { true, false, true };
    foo = new Foo();
    foo.set1Boolean(t[0]);
    assertEquals(foo.isBoolean(), t[0]);
    foo.set2Boolean(t[1]);
    assertEquals(foo.isBoolean(), t[1]);
    foo.set2Boolean(t[2], SetMode.DISALLOW_NULL);
    assertEquals(foo.isBoolean(), t[2]);
    foo.set2Boolean(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo.hasBoolean());
  }

  @Test
  public void testEnumField()
  {
    List<EnumType> good = Arrays.asList(EnumType.APPLE, EnumType.ORANGE, EnumType.BANANA);
    Foo foo = new Foo();
    for (EnumType i : good)
    {
      foo.setEnum(i);
      assertEquals(foo.getEnum(), i);
      assertTrue(foo.hasEnum());
      assertEquals(foo.toString(), "{enum=" + i + "}");

      Exception exc = null;
      try
      {
        Foo fooClone = (Foo) foo.clone();
        assertEquals(foo, fooClone);
        fooClone.removeEnum();
        assertTrue(foo.hasEnum());
        assertEquals(foo.getEnum(), i);
        assertFalse(fooClone.hasEnum());
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assertTrue(exc == null);
      foo.removeEnum();
      assertFalse(foo.hasEnum());
      assertEquals(foo.toString(), "{}");
    }

    List<?> badInput = asList(false, "abc", new DataList());

    DataMap map = new DataMap();
    foo = new Foo(map);
    for (Object bad : badInput)
    {
      map.put("enum", bad);
      Exception exc = null;
      try
      {
        EnumType result = foo.getEnum();
        assertEquals(bad.getClass(), String.class);
        assertEquals(result, EnumType.$UNKNOWN);
      }
      catch (Exception e)
      {
        exc = e;
      }
      if (bad.getClass() != String.class)
      {
        assertTrue(exc != null);
        assertTrue(exc instanceof TemplateOutputCastException);
      }
    }

    foo.setEnum(EnumType.APPLE);
    assertEquals(foo.getEnum(), EnumType.APPLE);
    foo.setEnum(EnumType.ORANGE);
    assertEquals(foo.getEnum(), EnumType.ORANGE);
    foo.setEnum(EnumType.BANANA);
    assertEquals(foo.getEnum(), EnumType.BANANA);

    // legacy test
    EnumType t[] = { EnumType.APPLE, EnumType.ORANGE, EnumType.BANANA };
    foo = new Foo();
    foo.set1Enum(t[0]);
    assertEquals(foo.getEnum(), t[0]);
    foo.set2Enum(t[1]);
    assertEquals(foo.getEnum(), t[1]);
    foo.set2Enum(t[2], SetMode.DISALLOW_NULL);
    assertEquals(foo.getEnum(), t[2]);
    foo.set2Enum(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo.hasEnum());
  }

  @Test
  public void testIntegerField()
  {
    List<Integer> good = Arrays.asList(11, 22, 33);
    Foo foo = new Foo();
    for (Integer i : good)
    {
      foo.setInt(i);
      assertEquals(foo.getInt(), i);
      assertTrue(foo.hasInt());
      assertEquals(foo.toString(), "{int=" + i + "}");
      Exception exc = null;
      try
      {
        Foo fooClone = (Foo) foo.clone();
        assertEquals(foo, fooClone);
        fooClone.removeInt();
        assertTrue(foo.hasInt());
        assertEquals(foo.getInt(), i);
        assertFalse(fooClone.hasInt());
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assertTrue(exc == null);
      foo.removeInt();
      assertFalse(foo.hasInt());
      assertEquals(foo.toString(), "{}");
    }

    List<?> badInput = asList(false, "abc", new DataList());

    DataMap map = new DataMap();
    foo = new Foo(map);
    for (Object bad : badInput)
    {
      map.put("int", bad);
      Exception exc = null;
      try
      {
        Integer result = foo.getInt();
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    List<?> castFrom = asList(88L, 99.0f, 77.0);
    List<?> castTo = asList(88, 99, 77);
    for (int i = 0; i < castFrom.size(); ++i)
    {
      map.put("int", castFrom.get(i));
      Integer result = foo.getInt();
      assertEquals(result, castTo.get(i));
    }

    // legacy test
    Integer t[] = { 77, 88, 99 };
    foo = new Foo();
    foo.set1Int(t[0]);
    assertEquals(foo.getInt(), t[0]);
    foo.set2Int(t[1]);
    assertEquals(foo.getInt(), t[1]);
    foo.set2Int(t[2], SetMode.DISALLOW_NULL);
    assertEquals(foo.getInt(), t[2]);
    foo.set2Int(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo.hasInt());
  }

  @Test
  public void testLongField()
  {
    List<Long> good = Arrays.asList(11L, 22L, 33L);
    Foo foo = new Foo();
    for (Long i : good)
    {
      foo.setLong(i);
      assertEquals(foo.getLong(), i);
      assertTrue(foo.hasLong());
      assertEquals(foo.toString(), "{long=" + i + "}");
      Exception exc = null;
      try
      {
        Foo fooClone = (Foo) foo.clone();
        assertEquals(foo, fooClone);
        fooClone.removeLong();
        assertTrue(foo.hasLong());
        assertEquals(foo.getLong(), i);
        assertFalse(fooClone.hasLong());
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assertTrue(exc == null);
      foo.removeLong();
      assertFalse(foo.hasLong());
      assertEquals(foo.toString(), "{}");
    }

    List<?> badInput = asList(false, "abc", new DataList());

    DataMap map = new DataMap();
    foo = new Foo(map);
    for (Object bad : badInput)
    {
      map.put("long", bad);
      Exception exc = null;
      try
      {
        Long result = foo.getLong();
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    List<?> castFrom = asList(88, 99.0f, 77.0);
    List<?> castTo = asList(88L, 99L, 77L);
    for (int i = 0; i < castFrom.size(); ++i)
    {
      map.put("long", castFrom.get(i));
      Long result = foo.getLong();
      assertEquals(result, castTo.get(i));
    }

    // legacy test
    Long t[] = { 77L, 88L, 99L };
    foo = new Foo();
    foo.set1Long(t[0]);
    assertEquals(foo.getLong(), t[0]);
    foo.set2Long(t[1]);
    assertEquals(foo.getLong(), t[1]);
    foo.set2Long(t[2], SetMode.DISALLOW_NULL);
    assertEquals(foo.getLong(), t[2]);
    foo.set2Long(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo.hasLong());
  }

  @Test
  public void testFloatField()
  {
    List<Float> good = Arrays.asList(11.0f, 22.0f, 33.0f);
    Foo foo = new Foo();
    for (Float i : good)
    {
      foo.setFloat(i);
      assertEquals(foo.getFloat(), i);
      assertTrue(foo.hasFloat());
      assertEquals(foo.toString(), "{float=" + i + "}");
      Exception exc = null;
      try
      {
        Foo fooClone = (Foo) foo.clone();
        assertEquals(foo, fooClone);
        fooClone.removeFloat();
        assertTrue(foo.hasFloat());
        assertEquals(foo.getFloat(), i);
        assertFalse(fooClone.hasFloat());
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assertTrue(exc == null);
      foo.removeFloat();
      assertFalse(foo.hasFloat());
      assertEquals(foo.toString(), "{}");
    }

    List<?> badInput = asList(false, "abc", new DataList());

    DataMap map = new DataMap();
    foo = new Foo(map);
    for (Object bad : badInput)
    {
      map.put("float", bad);
      Exception exc = null;
      try
      {
        Float result = foo.getFloat();
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    List<?> castFrom = asList(88, 99.0, 77.0);
    List<?> castTo = asList(88.0f, 99.0f, 77.0f);
    for (int i = 0; i < castFrom.size(); ++i)
    {
      map.put("float", castFrom.get(i));
      Float result = foo.getFloat();
      assertEquals(result, castTo.get(i));
    }

    // legacy test
    Float t[] = { 77f, 88f, 99f };
    foo = new Foo();
    foo.set1Float(t[0]);
    assertEquals(foo.getFloat(), t[0]);
    foo.set2Float(t[1]);
    assertEquals(foo.getFloat(), t[1]);
    foo.set2Float(t[2], SetMode.DISALLOW_NULL);
    assertEquals(foo.getFloat(), t[2]);
    foo.set2Float(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo.hasFloat());
  }

  @Test
  public void testDoubleField()
  {
    List<Double> good = Arrays.asList(11.0, 22.0, 33.0);
    Foo foo = new Foo();
    for (Double i : good)
    {
      foo.setDouble(i);
      assertEquals(foo.getDouble(), i);
      assertTrue(foo.hasDouble());
      assertEquals(foo.toString(), "{double=" + i + "}");
      Exception exc = null;
      try
      {
        Foo fooClone = (Foo) foo.clone();
        assertEquals(foo, fooClone);
        fooClone.removeDouble();
        assertTrue(foo.hasDouble());
        assertEquals(foo.getDouble(), i);
        assertFalse(fooClone.hasDouble());
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assertTrue(exc == null);
      foo.removeDouble();
      assertFalse(foo.hasDouble());
      assertEquals(foo.toString(), "{}");
    }

    List<?> badInput = asList(false, "abc", new DataList());

    DataMap map = new DataMap();
    foo = new Foo(map);
    for (Object bad : badInput)
    {
      map.put("double", bad);
      Exception exc = null;
      try
      {
        Double result = foo.getDouble();
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    List<?> castFrom = asList(88, 99L, 77.0f);
    List<?> castTo = asList(88.0, 99.0, 77.0);
    for (int i = 0; i < castFrom.size(); ++i)
    {
      map.put("double", castFrom.get(i));
      Double result = foo.getDouble();
      assertEquals(result, castTo.get(i));
    }

    // legacy test
    Double t[] = { 77.0, 88.0, 99.0 };
    foo = new Foo();
    foo.set1Double(t[0]);
    assertEquals(foo.getDouble(), t[0]);
    foo.set2Double(t[1]);
    assertEquals(foo.getDouble(), t[1]);
    foo.set2Double(t[2], SetMode.DISALLOW_NULL);
    assertEquals(foo.getDouble(), t[2]);
    foo.set2Double(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo.hasDouble());
  }

  @Test
  public void testStringField()
  {
    List<String> good = Arrays.asList("11", "22.0", "33.0");
    Foo foo = new Foo();
    for (String i : good)
    {
      foo.setString(i);
      assertEquals(foo.getString(), i);
      assertTrue(foo.hasString());
      assertEquals(foo.toString(), "{string=" + i + "}");
      Exception exc = null;
      try
      {
        Foo fooClone = (Foo) foo.clone();
        assertEquals(foo, fooClone);
        fooClone.removeString();
        assertTrue(foo.hasString());
        assertEquals(foo.getString(), i);
        assertFalse(fooClone.hasString());
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assertTrue(exc == null);
      foo.removeString();
      assertFalse(foo.hasString());
      assertEquals(foo.toString(), "{}");
    }

    List<?> badInput = asList(false, 4, 5L, 6.0f, 7.0, new DataList());

    DataMap map = new DataMap();
    foo = new Foo(map);
    for (Object bad : badInput)
    {
      map.put("string", bad);
      Exception exc = null;
      try
      {
         String result = foo.getString();
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    // legacy test
    String t[] = { "77", "88", "99" };
    foo = new Foo();
    foo.set1String(t[0]);
    assertEquals(foo.getString(), t[0]);
    foo.set2String(t[1]);
    assertEquals(foo.getString(), t[1]);
    foo.set2String(t[2], SetMode.DISALLOW_NULL);
    assertEquals(foo.getString(), t[2]);
    foo.set2String(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo.hasString());
  }

  @Test
  public void testBytesField()
  {
    List<ByteString> good = Arrays.asList(ByteString.copyAvroString("11", false), ByteString.copyAvroString("22", false), ByteString.copyAvroString("33", false));
    Foo foo = new Foo();
    for (ByteString i : good)
    {
      foo.setBytes(i);
      assertEquals(foo.getBytes(), i);
      assertTrue(foo.hasBytes());
      assertEquals(foo.toString(), "{bytes=" + i + "}");
      Exception exc = null;
      try
      {
        Foo fooClone = (Foo) foo.clone();
        assertEquals(foo, fooClone);
        fooClone.removeBytes();
        assertTrue(foo.hasBytes());
        assertEquals(foo.getBytes(), i);
        assertFalse(fooClone.hasBytes());
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assertTrue(exc == null);
      foo.removeBytes();
      assertFalse(foo.hasBytes());
      assertEquals(foo.toString(), "{}");
    }

    List<?> badInput = asList(false, 33, "\u0100", new DataList());

    DataMap map = new DataMap();
    foo = new Foo(map);
    for (Object bad : badInput)
    {
      map.put("bytes", bad);
      Exception exc = null;
      try
      {
        ByteString result = foo.getBytes();
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    List<?> castFrom = asList("88");
    List<?> castTo = asList(ByteString.copyAvroString("88", false));
    for (int i = 0; i < castFrom.size(); ++i)
    {
      map.put("bytes", castFrom.get(i));
      ByteString result = foo.getBytes();
      assertEquals(result, castTo.get(i));
    }

    // legacy test
    ByteString t[] = {
      ByteString.copyAvroString("apple", false), 
      ByteString.copyAvroString("orange", false), 
      ByteString.copyAvroString("banana", false) 
    };
    foo = new Foo();
    foo.set1Bytes(t[0]);
    assertEquals(foo.getBytes(), t[0]);
    foo.set2Bytes(t[1]);
    assertEquals(foo.getBytes(), t[1]);
    foo.set2Bytes(t[2], SetMode.DISALLOW_NULL);
    assertEquals(foo.getBytes(), t[2]);
    foo.set2Bytes(null, SetMode.REMOVE_IF_NULL);
    assertFalse(foo.hasBytes());
  }

  @Test
  public void testFixedField()
  {
    List<FixedType> good = Arrays.asList(
        new FixedType(ByteString.copyAvroString("abcd", false)),
        new FixedType(ByteString.copyAvroString("1234", false))
    );
    for (FixedType fixed : good)
    {
      Exception exc = null;
      try
      {
        FixedTemplate clone = fixed.clone();
        assertSame(clone.data(), fixed.data());
        assertSame(clone.schema(), fixed.schema());
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assert(exc == null);
    }
    Foo foo = new Foo();
    int index = 0;
    for (FixedType i : good)
    {
      foo.setFixed(i);
      assertEquals(foo.getFixed(), i);
      assertEquals(foo.getFixed().bytes(), i.bytes());
      assertSame(foo.getFixed().bytes(), i.bytes());
      assertTrue(foo.hasFixed());
      assertEquals(foo.toString(), "{fixed=" + i.bytes() + "}");
      Exception exc = null;
      try
      {
        Foo fooClone = (Foo) foo.clone();
        assertEquals(foo, fooClone);
        assertSame(fooClone.getFixed(), foo.getFixed());
        fooClone.removeFixed();
        assertTrue(foo.hasFixed());
        assertEquals(foo.getFixed(), i);
        assertFalse(fooClone.hasFixed());
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assertTrue(exc == null);
      foo.removeFixed();
      assertFalse(foo.hasFixed());
      assertEquals(foo.toString(), "{}");
      index++;
    }

    List<?> badInput = asList(false, "abc", new DataMap(), ByteString.empty(), ByteString.copyAvroString("abc", false));

    DataMap map = new DataMap();
    foo = new Foo(map);
    for (Object bad : badInput)
    {
      map.put("fixed", bad);
      Exception exc = null;
      try
      {
        FixedType result = foo.getFixed();
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    List<?> castFrom = asList("8888");
    List<?> castTo = asList(ByteString.copyAvroString("8888", false));
    for (int i = 0; i < castFrom.size(); ++i)
    {
      map.put("fixed", castFrom.get(i));
      ByteString result = foo.getFixed().bytes();
      assertEquals(result, castTo.get(i));
    }
  }

  @Test
  public void testBarField()
  {
    List<Bar> good = Arrays.asList(new Bar(), new Bar(), new Bar());
    Foo foo = new Foo();
    int index = 0;
    for (Bar i : good)
    {
      int value = index * index;
      i.setInt(value);
      foo.setRecord(i);
      assertEquals(foo.getRecord(), i);
      assertSame(foo.getRecord(), i);
      assertTrue(foo.hasRecord());
      assertEquals(foo.toString(), "{record={int=" + value + "}}");
      Exception exc = null;
      try
      {
        Foo fooClone = (Foo) foo.clone();
        assertEquals(foo, fooClone);
        assertSame(fooClone.getRecord(), foo.getRecord());
        fooClone.removeRecord();
        assertTrue(foo.hasRecord());
        assertEquals(foo.getRecord(), i);
        assertFalse(fooClone.hasRecord());
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assertTrue(exc == null);
      foo.removeRecord();
      assertFalse(foo.hasRecord());
      assertEquals(foo.toString(), "{}");
      index++;
    }

    List<?> badInput = asList(false, "abc", new DataList());

    DataMap map = new DataMap();
    foo = new Foo(map);
    for (Object bad : badInput)
    {
      map.put("record", bad);
      Exception exc = null;
      try
      {
        Bar result = foo.getRecord();
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    Exception exc = null;
    try
    {
      foo.setRecord(new Bar() { });

    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof ClassCastException);
  }

  @Test
  public void testIntegerArrayField()
  {
    List<IntegerArray> good = Arrays.asList(new IntegerArray(), new IntegerArray(), new IntegerArray());
    Foo foo = new Foo();
    int index = 0;
    for (IntegerArray i : good)
    {
      int value = index * index;
      i.add(value);
      foo.setArray(i);
      assertEquals(foo.getArray(), i);
      assertSame(foo.getArray(), i);
      assertTrue(foo.hasArray());
      assertEquals(foo.toString(), "{array=[" + value + "]}");
      Exception exc = null;
      try
      {
        Foo fooClone = (Foo) foo.clone();
        assertEquals(foo, fooClone);
        assertSame(fooClone.getArray(), foo.getArray());
        fooClone.removeArray();
        assertTrue(foo.hasArray());
        assertEquals(foo.getArray(), i);
        assertFalse(fooClone.hasArray());
      }
      catch (CloneNotSupportedException e)
      {
        exc = e;
      }
      assertTrue(exc == null);
      foo.removeArray();
      assertFalse(foo.hasArray());
      assertEquals(foo.toString(), "{}");
      index++;
    }

    List<?> badInput = asList(false, "abc", new DataMap());

    DataMap map = new DataMap();
    foo = new Foo(map);
    for (Object bad : badInput)
    {
      map.put("array", bad);
      Exception exc = null;
      try
      {
        IntegerArray result = foo.getArray();
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }
  }

  @Test
  public void testUnionField() throws CloneNotSupportedException
  {
    List<?> badOutput = asList(false, "abc", new DataList());

    DataMap map = new DataMap();
    Foo foo = new Foo(map);
    for (Object bad : badOutput)
    {
      map.put("union", bad);
      Exception exc = null;
      try
      {
        Foo.Union result = foo.getUnion();
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    // test memberType
    List<Object> badOutputMap = new ArrayList<Object>();
    badOutputMap.add(Data.NULL);
    badOutputMap.add(new DataMap());
    badOutputMap.add(new DataMap(asMap("int", 1, "invalid", 2)));
    badOutputMap.add(new DataMap(asMap("invalid", 2)));
    for (Object bad : badOutputMap)
    {
      map.put("union", bad);
      Exception exc = null;
      try
      {
        DataSchema result = foo.getUnion().memberType();
      }
      catch (Exception e)
      {
        exc = e;
      }
      assertTrue(exc != null);
      assertTrue(exc instanceof TemplateOutputCastException);
    }

    // test union accessing underlying map
    DataMap unionMap = new DataMap();
    Integer value = 4;
    unionMap.put("int", value);
    map.put("union", unionMap);
    Foo.Union union2 = foo.getUnion();
    assertFalse(union2.isNull());
    assertTrue(union2.isInt());
    assertEquals(union2.getInt(), value);
    assertTrue(union2.memberType() == Foo.Union.MEMBER_int);
    assertNull(union2.getBar());
    int lastHashCode = union2.hashCode();
    Foo.Union lastUnion = (Foo.Union) union2.clone();

    // test union set and get wrapped
    unionMap.clear();
    Bar bar = new Bar();
    value = 32;
    bar.setInt(value);
    union2.setBar(bar);
    assertFalse(union2.isNull());
    assertTrue(union2.isBar());
    assertEquals(union2.getBar().getInt(), value);
    assertTrue(union2.memberType() == Foo.Union.MEMBER_Bar);
    assertNull(union2.getInt());
    assertTrue(union2.equals(union2));
    assertFalse(union2.equals(null));
    assertFalse(union2.equals(new Object()));
    int hashCode = union2.hashCode();
    assertFalse(hashCode == lastHashCode);
    lastHashCode = hashCode;
    assertFalse(union2.equals(lastUnion));
    lastUnion = (Foo.Union) union2.clone();

    // test union clone with wrapped member
    Exception exc = null;
    try
    {
      exc = null;
      Foo.Union unionClone = (Foo.Union) union2.clone();
      assertFalse(unionClone.isNull());
      assertFalse(unionClone.isInt());
      assertTrue(unionClone.isBar());
      assertEquals(unionClone.getBar().getInt(), value);
      assertTrue(unionClone.getBar() == union2.getBar());
      assertEquals(unionClone.getBar(), union2.getBar());
      assertEquals(unionClone, union2);
    }
    catch (CloneNotSupportedException e)
    {
      exc = e;
    }
    assertTrue(exc == null);

    // test set bad wrapped
    try
    {
      exc = null;
      union2.setBar(new Bar() {});
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof ClassCastException);

    // test union set and get direct
    value = 127;
    union2.setInt(127);
    assertFalse(union2.isNull());
    assertTrue(union2.isInt());
    assertEquals(union2.getInt(), value);
    assertTrue(union2.memberType() == Foo.Union.MEMBER_int);
    assertEquals(union2.toString(), "{int=127}");
    assertNull(union2.getBar());
    assertTrue(union2.equals(union2));
    assertFalse(union2.equals(null));
    assertFalse(union2.equals(new Object()));
    hashCode = union2.hashCode();
    assertFalse(hashCode == lastHashCode);
    lastHashCode = hashCode;
    assertFalse(union2.equals(lastUnion));
    lastUnion = (Foo.Union) union2.clone();

    // test union clone with direct member
    try
    {
      exc = null;
      Foo.Union unionClone = (Foo.Union) union2.clone();
      assertFalse(unionClone.isNull());
      assertTrue(unionClone.isInt());
      assertFalse(unionClone.isBar());
      assertEquals(unionClone.getInt(), value);
      assertEquals(unionClone.getInt(), union2.getInt());
      assertSame(unionClone.getInt(), union2.getInt());
      assertEquals(unionClone, union2);
      Integer newValue = 256;
      unionClone.setInt(newValue);
      assertEquals(unionClone.getInt(), newValue);
      assertEquals(union2.getInt(), value);
    }
    catch (CloneNotSupportedException e)
    {
      exc = e;
    }
    assertTrue(exc == null);

    // test null union
    foo.setUnion(new Foo.Union());
    Foo.Union union = foo.getUnion();
    assertTrue(union.isNull());
    assertFalse(union.isInt());
    assertFalse(union.isBar());
    assertEquals(union.toString(), Data.NULL.toString());
    assertSame(union.data(), Data.NULL);
    try
    {
      exc = null;
      Foo.Union unionClone = (Foo.Union) union.clone();
      assertTrue(unionClone.isNull());
      assertFalse(unionClone.isInt());
      assertFalse(unionClone.isBar());
      assertEquals(unionClone.toString(), Data.NULL.toString());
      assertSame(unionClone.data(), Data.NULL);
    }
    catch (CloneNotSupportedException e)
    {
      exc = e;
    }
    assertTrue(exc == null);

    // test null unions throw exception on get and set methods.
    try
    {
      exc = null;
      union.getInt();
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof NullUnionUnsupportedOperationException);

    try
    {
      exc = null;
      union.setInt(4);
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof NullUnionUnsupportedOperationException);

    try
    {
      exc = null;
      union.getBar();
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof NullUnionUnsupportedOperationException);

    try
    {
      exc = null;
      union.setBar(new Bar());
    }
    catch (Exception e)
    {
      exc = e;
    }
    assertTrue(exc != null);
    assertTrue(exc instanceof NullUnionUnsupportedOperationException);

    // test legacy
    union = new Foo.Union(new DataMap());
    Integer i = 43;
    union.set2Int(i); // legacy set
    assertTrue(union.isInt());
    assertEquals(union.getInt(), i);
    assertFalse(union.isEnumType());
    assertNull(union.getEnumType());

    union.set2EnumType(EnumType.ORANGE); // legacy set
    assertTrue(union.isEnumType());
    assertSame(union.getEnumType(), EnumType.ORANGE);
    assertFalse(union.isInt());
    assertNull(union.getInt());

    union.set2EnumType(EnumType.APPLE); // legacy set
    assertTrue(union.isEnumType());
    assertSame(union.getEnumType(), EnumType.APPLE);
    assertFalse(union.isInt());
    assertNull(union.getInt());

    i = 66;
    union.set2Int(i); // legacy set
    assertTrue(union.isInt());
    assertEquals(union.getInt(), i);
    assertFalse(union.isEnumType());
    assertNull(union.getEnumType());
  }

  @Test
  public void testWrapping()
      throws InstantiationException, IllegalAccessException
  {
    DataMap map1 = new DataMap();
    Foo foo = DataTemplateUtil.wrap(map1, Foo.class);
    assertSame(map1, foo.data());

    DataMap map2 = new DataMap();
    Foo foo2 = DataTemplateUtil.wrap(map2, Foo.SCHEMA, Foo.class);
    assertSame(map2, foo2.data());

    DataMap map3 = new DataMap();
    Foo.Union u3 = DataTemplateUtil.wrap(map3, Foo.Union.class);
    assertSame(map3, u3.data());

    DataMap map4 = new DataMap();
    Foo.Union u4 = DataTemplateUtil.wrap(map4, Foo.Union.SCHEMA, Foo.Union.class);
    assertSame(map4, u4.data());

    Foo.Union u5 = DataTemplateUtil.wrap(null, Foo.Union.class);
    assertSame(Data.NULL, u5.data());

    Foo.Union u6 = DataTemplateUtil.wrap(null, Foo.Union.SCHEMA, Foo.Union.class);
    assertSame(Data.NULL, u6.data());

    Foo.Union u7 = DataTemplateUtil.wrap(Data.NULL, Foo.Union.class);
    assertSame(Data.NULL, u7.data());

    Foo.Union u8 = DataTemplateUtil.wrap(Data.NULL, Foo.Union.SCHEMA, Foo.Union.class);
    assertSame(Data.NULL, u8.data());
  }
}

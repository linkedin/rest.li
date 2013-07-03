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
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;
import java.util.ArrayList;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Unit tests for dynamic record template.
 */
public class TestDynamicRecordTemplate
{
  public static final RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema
      (
          "{ \"type\" : \"record\", \"name\" : \"Foo\", \"fields\" : [\n" +
              "{ \"name\" : \"boolean\", \"type\" : \"boolean\" }, \n" +
              "{ \"name\" : \"int\", \"type\" : \"int\" }, \n" +
              "{ \"name\" : \"long\", \"type\" : \"long\" }, \n" +
              "{ \"name\" : \"float\", \"type\" : \"float\" }, \n" +
              "{ \"name\" : \"double\", \"type\" : \"double\" }, \n" +
              "{ \"name\" : \"string\", \"type\" : \"string\" }, \n" +
              "{ \"name\" : \"bytes\", \"type\" : \"bytes\" }, \n" +
              "{ \"name\" : \"intArray\", \"type\" : { \"type\" : \"array\", \"items\" : \"int\" } }, \n" +
              "{ \"name\" : \"recordArray\", \"type\" : { \"type\" : \"array\", \"items\" : \"Foo\" } }, \n" +
              "{ \"name\" : \"enum\", \"type\" : { \"type\" : \"enum\", \"name\" : \"EnumType\", \"symbols\" : [ \"APPLE\", \"ORANGE\", \"BANANA\" ] } }, \n" +
              "{ \"name\" : \"enumArray\", \"type\" : { \"type\" : \"array\", \"items\" : \"EnumType\" } }, \n" +
              "{ \"name\" : \"fixed\", \"type\" : { \"type\" : \"fixed\", \"name\" : \"fixedType\", \"size\" : 4 } }, \n" +
              "{ \"name\" : \"fixedArray\", \"type\" : { \"type\" : \"array\", \"items\" : \"fixedType\" } }, \n" +
              "{ \"name\" : \"record\", \"type\" : { \"type\" : \"record\", \"name\" : \"Bar\", \"fields\" : [ { \"name\" : \"int\", \"type\" : \"int\" } ] } } \n" +
              "] }"
      );

  public static final FieldDef<Boolean> FIELD_boolean =
      new FieldDef<Boolean>("boolean", Boolean.class, SCHEMA.getField("boolean").getType());
  public static final FieldDef<Integer> FIELD_int =
      new FieldDef<Integer>("int", Integer.class, SCHEMA.getField("int").getType());
  public static final FieldDef<Long> FIELD_long =
      new FieldDef<Long>("long", Long.class, SCHEMA.getField("long").getType());
  public static final FieldDef<Float> FIELD_float =
      new FieldDef<Float>("float", Float.class, SCHEMA.getField("float").getType());
  public static final FieldDef<Double> FIELD_double =
      new FieldDef<Double>("double", Double.class, SCHEMA.getField("double").getType());
  public static final FieldDef<String> FIELD_string =
      new FieldDef<String>("string", String.class, SCHEMA.getField("string").getType());
  public static final FieldDef<ByteString> FIELD_bytes =
      new FieldDef<ByteString>("bytes", ByteString.class, SCHEMA.getField("bytes").getType());
  public static final FieldDef<TestRecordAndUnionTemplate.EnumType> FIELD_enum =
      new FieldDef<TestRecordAndUnionTemplate.EnumType>("enum", TestRecordAndUnionTemplate.EnumType.class, SCHEMA.getField("enum").getType());
  public static final FieldDef<TestRecordAndUnionTemplate.FixedType> FIELD_fixed =
      new FieldDef<TestRecordAndUnionTemplate.FixedType>("fixed", TestRecordAndUnionTemplate.FixedType.class, SCHEMA.getField("fixed").getType());
  public static final FieldDef<TestRecordAndUnionTemplate.Bar> FIELD_record =
      new FieldDef<TestRecordAndUnionTemplate.Bar>("record", TestRecordAndUnionTemplate.Bar.class, SCHEMA.getField("record").getType());

  public static final FieldDef<TestRecordAndUnionTemplate.FixedType[]> FIELD_fixedArray =
      new FieldDef<TestRecordAndUnionTemplate.FixedType[]>("fixedArray", TestRecordAndUnionTemplate.FixedType[].class, SCHEMA.getField("fixedArray").getType());
  public static final FieldDef<Integer[]> FIELD_intArray =
      new FieldDef<Integer[]>("intArray", Integer[].class, SCHEMA.getField("intArray").getType());
  public static final FieldDef<TestRecordAndUnionTemplate.Bar[]> FIELD_recordArray =
      new FieldDef<TestRecordAndUnionTemplate.Bar[]>("recordArray", TestRecordAndUnionTemplate.Bar[].class, SCHEMA.getField("recordArray").getType());
  public static final FieldDef<TestRecordAndUnionTemplate.EnumType[]> FIELD_enumArray =
      new FieldDef<TestRecordAndUnionTemplate.EnumType[]>("enumArray", TestRecordAndUnionTemplate.EnumType[].class, SCHEMA.getField("enumArray").getType());
  public static final FieldDef<IntegerArray> FIELD_intArrayTemplate =
      new FieldDef<IntegerArray>("intArrayTemplate", IntegerArray.class, SCHEMA.getField("intArray").getType());
  public static DynamicRecordMetadata METADATA;

  static
  {
    ArrayList<FieldDef<?>> fieldDefs = new ArrayList<FieldDef<?>>();
    fieldDefs.add(FIELD_boolean);
    fieldDefs.add(FIELD_bytes);
    fieldDefs.add(FIELD_double);
    fieldDefs.add(FIELD_enum);
    fieldDefs.add(FIELD_enumArray);
    fieldDefs.add(FIELD_fixed);
    fieldDefs.add(FIELD_fixedArray);
    fieldDefs.add(FIELD_float);
    fieldDefs.add(FIELD_int);
    fieldDefs.add(FIELD_intArray);
    fieldDefs.add(FIELD_intArrayTemplate);
    fieldDefs.add(FIELD_long);
    fieldDefs.add(FIELD_record);
    fieldDefs.add(FIELD_recordArray);
    fieldDefs.add(FIELD_string);
    METADATA = new DynamicRecordMetadata("dynamic", fieldDefs);
  }

  public class DynamicFoo extends DynamicRecordTemplate
  {
    public DynamicFoo()
    {
      super(new DataMap(), METADATA.getRecordDataSchema());
    }

    public boolean getBoolean()
    {
      return getValue(FIELD_boolean);
    }

    public void setBoolean(boolean value)
    {
      setValue(FIELD_boolean, value);
    }

    public int getInt()
    {
      return getValue(FIELD_int);
    }

    public void setInt(int value)
    {
      setValue(FIELD_int, value);
    }

    public long getLong()
    {
      return getValue(FIELD_long);
    }

    public void setLong(long value)
    {
      setValue(FIELD_long, value);
    }

    public double getDouble()
    {
      return getValue(FIELD_double);
    }

    public void setDouble(double value)
    {
      setValue(FIELD_double, value);
    }

    public float getFloat()
    {
      return getValue(FIELD_float);
    }

    public void setFloat(float value)
    {
      setValue(FIELD_float, value);
    }

    public String getString()
    {
      return getValue(FIELD_string);
    }

    public void setString(String value)
    {
      setValue(FIELD_string, value);
    }

    public ByteString getBytes()
    {
      return getValue(FIELD_bytes);
    }

    public void setBytes(ByteString value)
    {
      setValue(FIELD_bytes, value);
    }

    public TestRecordAndUnionTemplate.EnumType getEnum()
    {
      return getValue(FIELD_enum);
    }

    public void setEnum(TestRecordAndUnionTemplate.EnumType value)
    {
      setValue(FIELD_enum, value);
    }

    public TestRecordAndUnionTemplate.FixedType getFixed()
    {
      return getValue(FIELD_fixed);
    }

    public void setFixed(TestRecordAndUnionTemplate.FixedType value)
    {
      setValue(FIELD_fixed, value);
    }

    public TestRecordAndUnionTemplate.Bar getRecord()
    {
      return getValue(FIELD_record);
    }

    public void setRecord(TestRecordAndUnionTemplate.Bar value)
    {
      setValue(FIELD_record, value);
    }

    public Integer[] getIntArray()
    {
      return getValue(FIELD_intArray);
    }

    public void setIntArray(Integer[] value)
    {
      setValue(FIELD_intArray, value);
    }

    public IntegerArray getIntArrayTemplate()
    {
      return getValue(FIELD_intArrayTemplate);
    }

    public void setIntArrayTemplate(IntegerArray value)
    {
      setValue(FIELD_intArrayTemplate, value);
    }

    public TestRecordAndUnionTemplate.EnumType[] getEnumArray()
    {
      return getValue(FIELD_enumArray);
    }

    public void setEnumArray(TestRecordAndUnionTemplate.EnumType[] value)
    {
      setValue(FIELD_enumArray, value);
    }

    public TestRecordAndUnionTemplate.Bar[] getRecordArray()
    {
      return getValue(FIELD_recordArray);
    }

    public void setRecordArray(TestRecordAndUnionTemplate.Bar[] value)
    {
      setValue(FIELD_recordArray, value);
    }

    public TestRecordAndUnionTemplate.FixedType[] getFixedArray()
    {
      return getValue(FIELD_fixedArray);
    }

    public void setFixedArray(TestRecordAndUnionTemplate.FixedType[] value)
    {
      setValue(FIELD_fixedArray, value);
    }
  }

  @Test
  public void TestPrimitiveFieldsOnDynamicRecord()
  {
    DynamicFoo foo = new DynamicFoo();

    foo.setBoolean(true);
    Assert.assertEquals(true, foo.getBoolean());
    foo.setInt(54);
    Assert.assertEquals(54, foo.getInt());
    foo.setFloat(5.67F);
    Assert.assertEquals(5.67F, foo.getFloat());
    foo.setDouble(4.45);
    Assert.assertEquals(4.45, foo.getDouble());
    foo.setLong(12345L);
    Assert.assertEquals(12345L, foo.getLong());
    ByteString byteString = ByteString.copyAvroString("someString", false);
    foo.setBytes(byteString);
    Assert.assertEquals(byteString, foo.getBytes());
    foo.setString("myString");
    Assert.assertEquals("myString", foo.getString());
    foo.setEnum(TestRecordAndUnionTemplate.EnumType.BANANA);
    Assert.assertEquals(TestRecordAndUnionTemplate.EnumType.BANANA, foo.getEnum());
  }

  @Test
  public void TestComplexTypeFieldsOnDynamicRecord()
  {
    DynamicFoo foo = new DynamicFoo();

    TestRecordAndUnionTemplate.Bar bar = new TestRecordAndUnionTemplate.Bar().setInt(54);
    foo.setRecord(bar);
    Assert.assertEquals(bar, foo.getRecord());

    TestRecordAndUnionTemplate.FixedType fixed = new TestRecordAndUnionTemplate.FixedType("abcd");
    foo.setFixed(fixed);
    Assert.assertEquals(fixed, foo.getFixed());
  }

  @Test
  public void TestArrayFieldsOnDynamicRecord()
  {
    DynamicFoo foo = new DynamicFoo();

    //enum array
    TestRecordAndUnionTemplate.EnumType[] enumArray =
        new TestRecordAndUnionTemplate.EnumType[] { TestRecordAndUnionTemplate.EnumType.APPLE,
            TestRecordAndUnionTemplate.EnumType.ORANGE };

    foo.setEnumArray(enumArray);

    TestRecordAndUnionTemplate.EnumType[] enumArray2 = foo.getEnumArray();

    Assert.assertEquals(enumArray[0], enumArray2[0]);
    Assert.assertEquals(enumArray[1], enumArray2[1]);

    //Java int array
    Integer[] intArray = new Integer[] { 53, 54 };
    foo.setIntArray(intArray);

    Integer[] intArray2 = foo.getIntArray();

    Assert.assertEquals(intArray[0], intArray2[0]);
    Assert.assertEquals(intArray[1], intArray2[1]);

    //integer array template
    IntegerArray intArrayTemplate = new IntegerArray();
    intArrayTemplate.add(63);
    intArrayTemplate.add(64);

    foo.setIntArrayTemplate(intArrayTemplate);

    IntegerArray intArrayTemplate2 = foo.getIntArrayTemplate();

    Assert.assertEquals(intArrayTemplate.get(0), intArrayTemplate2.get(0));
    Assert.assertEquals(intArrayTemplate.get(1), intArrayTemplate2.get(1));

    //record array
    TestRecordAndUnionTemplate.Bar[] barArray =
        new TestRecordAndUnionTemplate.Bar[] { new TestRecordAndUnionTemplate.Bar().setInt(53),
            new TestRecordAndUnionTemplate.Bar().setInt(54) };
    foo.setRecordArray(barArray);
    TestRecordAndUnionTemplate.Bar[] barArray2 = foo.getRecordArray();

    Assert.assertEquals(barArray[0], barArray2[0]);
    Assert.assertEquals(barArray[1], barArray2[1]);

    //fixed array
    TestRecordAndUnionTemplate.FixedType[] fixedArray =
        new TestRecordAndUnionTemplate.FixedType[] { new TestRecordAndUnionTemplate.FixedType("abcd"),
            new TestRecordAndUnionTemplate.FixedType("efgh") };
    foo.setFixedArray(fixedArray);
    TestRecordAndUnionTemplate.FixedType[] fixedArray2 = foo.getFixedArray();

    Assert.assertEquals(fixedArray[0], fixedArray2[0]);
    Assert.assertEquals(fixedArray[1], fixedArray2[1]);
  }
}

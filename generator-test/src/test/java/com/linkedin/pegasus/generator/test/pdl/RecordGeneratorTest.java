/*
 Copyright 2015 Coursera Inc.

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

package com.linkedin.pegasus.generator.test.pdl;

import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.data.template.IntegerMap;
import com.linkedin.pegasus.generator.test.idl.enums.Fruits;
import com.linkedin.pegasus.generator.test.idl.maps.WithOrders;
import com.linkedin.pegasus.generator.test.idl.records.InlineOptionalRecord;
import com.linkedin.pegasus.generator.test.idl.records.InlineRecord;
import com.linkedin.pegasus.generator.test.idl.records.Note;
import com.linkedin.pegasus.generator.test.idl.records.Simple;
import com.linkedin.pegasus.generator.test.idl.records.SimpleMap;
import com.linkedin.pegasus.generator.test.idl.records.WithAliases;
import com.linkedin.pegasus.generator.test.idl.records.WithComplexTypeDefaults;
import com.linkedin.pegasus.generator.test.idl.records.WithComplexTypes;
import com.linkedin.pegasus.generator.test.idl.records.WithInclude;
import com.linkedin.pegasus.generator.test.idl.records.WithIncludeAfter;
import com.linkedin.pegasus.generator.test.idl.records.WithInlineRecord;
import com.linkedin.pegasus.generator.test.idl.records.WithOptionalComplexTypeDefaults;
import com.linkedin.pegasus.generator.test.idl.records.WithOptionalComplexTypes;
import com.linkedin.pegasus.generator.test.idl.records.WithOptionalPrimitiveCustomTypes;
import com.linkedin.pegasus.generator.test.idl.records.WithOptionalPrimitiveDefaults;
import com.linkedin.pegasus.generator.test.idl.records.WithOptionalPrimitiveTyperefs;
import com.linkedin.pegasus.generator.test.idl.records.WithOptionalPrimitives;
import com.linkedin.pegasus.generator.test.idl.records.WithPrimitiveCustomTypes;
import com.linkedin.pegasus.generator.test.idl.records.WithPrimitiveDefaults;
import com.linkedin.pegasus.generator.test.idl.records.WithPrimitiveTyperefs;
import com.linkedin.pegasus.generator.test.idl.records.WithPrimitives;
import com.linkedin.pegasus.generator.test.pdl.fixtures.CustomInt;
import java.util.Collections;
import java.util.List;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;


public class RecordGeneratorTest extends GeneratorTest
{

  private String primitiveRecordFieldsJson = load("WithPrimitives_flex_defaults.json");

  @Test
  public void testWithPrimitives()
      throws Throwable
  {
    WithPrimitives original = new WithPrimitives();
    original.setIntField(1);
    original.setLongField(3000000000L);
    original.setFloatField(3.3f);
    original.setDoubleField(4.4e38d);
    original.setBooleanField(true);
    original.setStringField("str");
    original.setBytesField(SchemaFixtures.bytes1);
    assertJson(original, primitiveRecordFieldsJson);

    WithPrimitives roundTripped = new WithPrimitives(roundTrip(original.data()));
    assertEquals(roundTripped.getIntField(), (Integer) 1);
    assertEquals(roundTripped.getLongField(), (Long) 3000000000L);
    assertEquals(roundTripped.getFloatField(), 3.3f);
    assertEquals(roundTripped.getDoubleField(), 4.4e38d);
    assertEquals(roundTripped.isBooleanField(), Boolean.TRUE);
    assertEquals(roundTripped.getStringField(), "str");
    assertEquals(roundTripped.getBytesField(), SchemaFixtures.bytes1);

    assertJson(original, primitiveRecordFieldsJson);
  }

  @Test
  public void testWithOptionalPrimitives_present()
      throws Throwable
  {
    WithOptionalPrimitives original = new WithOptionalPrimitives();
    original.setIntField(1);
    original.setLongField(3000000000L);
    original.setFloatField(3.3f);
    original.setDoubleField(4.4e38d);
    original.setBooleanField(true);
    original.setStringField("str");
    original.setBytesField(SchemaFixtures.bytes1);
    assertJson(original, primitiveRecordFieldsJson);

    WithOptionalPrimitives roundTripped = new WithOptionalPrimitives(roundTrip(original.data()));
    assertEquals(roundTripped.getIntField(), (Integer) 1);
    assertEquals(roundTripped.getLongField(), (Long) 3000000000L);
    assertEquals(roundTripped.getFloatField(), 3.3f);
    assertEquals(roundTripped.getDoubleField(), 4.4e38d);
    assertEquals(roundTripped.isBooleanField(), Boolean.TRUE);
    assertEquals(roundTripped.getStringField(), "str");
    assertEquals(roundTripped.getBytesField(), SchemaFixtures.bytes1);
    assertJson(roundTripped, primitiveRecordFieldsJson);
  }

  @Test
  public void testWithOptionalPrimitives_absent()
      throws Throwable
  {
    WithOptionalPrimitives original = new WithOptionalPrimitives();
    assertJson(original, "{ }");

    WithOptionalPrimitives roundTripped = new WithOptionalPrimitives(roundTrip(original.data()));
    assertNull(roundTripped.getIntField());
    assertNull(roundTripped.getLongField());
    assertNull(roundTripped.getFloatField());
    assertNull(roundTripped.getDoubleField());
    assertNull(roundTripped.isBooleanField());
    assertNull(roundTripped.getStringField());
    assertNull(roundTripped.getBytesField());

    assertJson(roundTripped, "{ }");
  }

  @Test
  public void testWithPrimitiveTyperefs()
      throws Throwable
  {
    WithPrimitiveTyperefs original = new WithPrimitiveTyperefs();
    original.setIntField(1);
    original.setLongField(3000000000L);
    original.setFloatField(3.3f);
    original.setDoubleField(4.4e38d);
    original.setBooleanField(true);
    original.setStringField("str");
    original.setBytesField(SchemaFixtures.bytes1);
    assertJson(original, primitiveRecordFieldsJson);

    WithPrimitiveTyperefs roundTripped = new WithPrimitiveTyperefs(roundTrip(original.data()));
    assertEquals(roundTripped.getIntField(), (Integer) 1);
    assertEquals(roundTripped.getLongField(), (Long) 3000000000L);
    assertEquals(roundTripped.getFloatField(), 3.3f);
    assertEquals(roundTripped.getDoubleField(), 4.4e38d);
    assertEquals(roundTripped.isBooleanField(), Boolean.TRUE);
    assertEquals(roundTripped.getStringField(), "str");
    assertEquals(roundTripped.getBytesField(), SchemaFixtures.bytes1);
    assertJson(roundTripped, primitiveRecordFieldsJson);
  }

  @Test
  public void testWithOptionalPrimitiveTyperefs_Some()
      throws Throwable
  {
    WithOptionalPrimitiveTyperefs original = new WithOptionalPrimitiveTyperefs();
    original.setIntField(1);
    original.setLongField(3000000000L);
    original.setFloatField(3.3f);
    original.setDoubleField(4.4e38d);
    original.setBooleanField(true);
    original.setStringField("str");
    original.setBytesField(SchemaFixtures.bytes1);
    assertJson(original, primitiveRecordFieldsJson);

    WithOptionalPrimitiveTyperefs roundTripped = new WithOptionalPrimitiveTyperefs(roundTrip(original.data()));
    assertEquals(roundTripped.getIntField(), (Integer) 1);
    assertEquals(roundTripped.getLongField(), (Long) 3000000000L);
    assertEquals(roundTripped.getFloatField(), 3.3f);
    assertEquals(roundTripped.getDoubleField(), 4.4e38d);
    assertEquals(roundTripped.isBooleanField(), Boolean.TRUE);
    assertEquals(roundTripped.getStringField(), "str");
    assertEquals(roundTripped.getBytesField(), SchemaFixtures.bytes1);
    assertJson(roundTripped, primitiveRecordFieldsJson);
  }

  @Test
  public void testWithOptionalPrimitiveTyperefs_None()
      throws Throwable
  {
    WithOptionalPrimitiveTyperefs original = new WithOptionalPrimitiveTyperefs();
    assertJson(original, "{ }");

    WithOptionalPrimitiveTyperefs roundTripped = new WithOptionalPrimitiveTyperefs(roundTrip(original.data()));
    assertNull(roundTripped.getIntField());
    assertNull(roundTripped.getLongField());
    assertNull(roundTripped.getFloatField());
    assertNull(roundTripped.getDoubleField());
    assertNull(roundTripped.isBooleanField());
    assertNull(roundTripped.getStringField());
    assertNull(roundTripped.getBytesField());

    assertJson(roundTripped, "{ }");
  }

  @Test
  public void testWithPrimitiveCustomTypes()
      throws Throwable
  {
    WithPrimitiveCustomTypes original = new WithPrimitiveCustomTypes();
    original.setIntField(new CustomInt(1));
    assertJson(original, load("WithPrimitiveCustomTypes.json"));

    WithPrimitiveCustomTypes roundTripped = new WithPrimitiveCustomTypes(roundTrip(original.data()));
    assertEquals(roundTripped.getIntField(), new CustomInt(1));
    assertJson(roundTripped, load("WithPrimitiveCustomTypes.json"));
  }

  @Test
  public void testWithOptionalPrimitiveCustomTypes()
      throws Throwable
  {
    WithOptionalPrimitiveCustomTypes original = new WithOptionalPrimitiveCustomTypes();
    original.setIntField(new CustomInt(1));
    WithOptionalPrimitiveCustomTypes roundTripped = new WithOptionalPrimitiveCustomTypes(roundTrip(original.data()));

    assertEquals(roundTripped.getIntField(), new CustomInt(1));
  }

  @Test
  public void testWithInclude()
      throws Throwable
  {
    WithInclude original = new WithInclude();
    original.setMessage("message");
    original.setDirect(1);
    WithInclude roundTripped = new WithInclude(roundTrip(original.data()));

    assertEquals(roundTripped.getMessage(), "message");
    assertEquals(roundTripped.getDirect(), (Integer) 1);
  }

  @Test
  public void testWithInlineRecord_present()
      throws Throwable
  {
    InlineRecord inlineRecord = new InlineRecord();
    inlineRecord.setValue(1);
    InlineOptionalRecord inlineOptional = new InlineOptionalRecord();
    inlineOptional.setValue("str");
    WithInlineRecord original = new WithInlineRecord();
    original.setInline(inlineRecord);
    original.setInlineOptional(inlineOptional);

    WithInlineRecord roundTripped = new WithInlineRecord(roundTrip(original.data()));
    assertEquals(roundTripped.getInline().getValue(), (Integer) 1);
    assertEquals(roundTripped.getInlineOptional().getValue(), "str");
  }

  @Test
  public void testWithInlineRecord_absent()
      throws Throwable
  {
    InlineRecord inlineRecord = new InlineRecord();
    inlineRecord.setValue(1);
    WithInlineRecord original = new WithInlineRecord();
    original.setInline(inlineRecord);

    WithInlineRecord roundTripped = new WithInlineRecord(roundTrip(original.data()));
    assertEquals(roundTripped.getInline().getValue(), (Integer) 1);
    assertNull(roundTripped.getInlineOptional());
  }

  @Test
  public void testWithComplexTypes()
      throws Throwable
  {
    Simple simple = new Simple();
    simple.setMessage("message");

    IntegerMap intMap = new IntegerMap();
    intMap.put("a", 1);

    SimpleMap simpleMap = new SimpleMap();
    simpleMap.put("a", simple);

    WithComplexTypes original = new WithComplexTypes()
        .setRecord(simple)
        .setEnum(Fruits.APPLE)
        .setUnion(WithComplexTypes.Union.create(1))
        .setArray(new IntegerArray(Collections.singletonList(1)))
        .setMap(intMap)
        .setComplexMap(simpleMap)
        .setCustom(new CustomInt(1));

    WithComplexTypes roundTripped = new WithComplexTypes(roundTrip(original.data()));
    assertEquals(roundTripped.getRecord(), simple);
    assertEquals(roundTripped.getEnum(), Fruits.APPLE);
  }

  @Test
  public void testWithPrimitiveDefaults()
  {
    WithPrimitiveDefaults withDefaults = new WithPrimitiveDefaults();
    assertEquals(withDefaults.getIntWithDefault(), (Integer) 1);
    assertEquals(withDefaults.getLongWithDefault(), (Long) 3000000000L);
    assertEquals(withDefaults.getFloatWithDefault(), 3.3f);
    assertEquals(withDefaults.getDoubleWithDefault(), 4.4e38d);
    assertEquals(withDefaults.isBooleanWithDefault(), Boolean.TRUE);
    assertEquals(withDefaults.getStringWithDefault(), "DEFAULT");
  }

  @Test
  public void testWithOptionalPrimitiveDefaults()
  {
    WithOptionalPrimitiveDefaults withDefaults = new WithOptionalPrimitiveDefaults();
    assertEquals(withDefaults.getIntWithDefault(), (Integer) 1);
    assertEquals(withDefaults.getLongWithDefault(), (Long) 3000000000L);
    assertEquals(withDefaults.getFloatWithDefault(), 3.3f);
    assertEquals(withDefaults.getDoubleWithDefault(), 4.4e38d);
    assertEquals(withDefaults.isBooleanWithDefault(), Boolean.TRUE);
    assertEquals(withDefaults.getStringWithDefault(), "DEFAULT");
  }

  @Test
  public void testWithOptionalPrimitive_empty()
  {
    WithOptionalPrimitives withDefaults = new WithOptionalPrimitives();
    assertNull(withDefaults.getIntField());
    assertNull(withDefaults.getLongField());
    assertNull(withDefaults.getFloatField());
    assertNull(withDefaults.getDoubleField());
    assertNull(withDefaults.isBooleanField());
    assertNull(withDefaults.getStringField());
  }

  @Test
  public void testWithComplexTypesDefaults()
  {
    WithComplexTypeDefaults withDefaults = new WithComplexTypeDefaults();
    Simple simple = new Simple();
    simple.setMessage("defaults!");
    assertEquals(withDefaults.getRecord(), simple);
    assertEquals(withDefaults.getEnum(), Fruits.APPLE);
    assertEquals(withDefaults.getUnion(), WithComplexTypeDefaults.Union.create(1));
    IntegerArray intArray = new IntegerArray(Collections.singletonList(1));
    assertEquals(withDefaults.getArray(), intArray);
    IntegerMap intMap = new IntegerMap();
    intMap.put("a", 1);
    assertEquals(withDefaults.getMap(), intMap);
    assertEquals(withDefaults.getCustom(), new CustomInt(1));
  }

  @Test
  public void testWithOptionalComplexTypesDefaults()
  {
    WithOptionalComplexTypeDefaults withDefaults = new WithOptionalComplexTypeDefaults();
    Simple simple = new Simple();
    simple.setMessage("defaults!");
    assertEquals(withDefaults.getRecord(), simple);
    assertEquals(withDefaults.getEnum(), Fruits.APPLE);
    assertEquals(withDefaults.getUnion(), WithComplexTypeDefaults.Union.create(1));
    IntegerArray intArray = new IntegerArray(Collections.singletonList(1));
    assertEquals(withDefaults.getArray(), intArray);
    IntegerMap intMap = new IntegerMap();
    intMap.put("a", 1);
    assertEquals(withDefaults.getMap(), intMap);
    assertEquals(withDefaults.getCustom(), new CustomInt(1));
  }

  @Test
  public void testWithOptionalComplexTypes_empty()
  {
    WithOptionalComplexTypes withDefaults = new WithOptionalComplexTypes();
    assertNull(withDefaults.getRecord());
    assertNull(withDefaults.getEnum());
    assertNull(withDefaults.getUnion());
    assertNull(withDefaults.getArray());
    assertNull(withDefaults.getMap());
    assertNull(withDefaults.getCustom());
  }

  @Test
  public void testWithAliases()
  {
    WithAliases withAliases = new WithAliases();
    RecordDataSchema schema = withAliases.schema();
    List<Name> schemaAliases = schema.getAliases();
    assertTrue(schemaAliases.contains(new Name("org.example.RecordAlias1")));
    assertTrue(schemaAliases.contains(new Name("com.linkedin.pegasus.generator.test.idl.records.RecordAlias2")));
    List<String> fieldAliases = schema.getField("name").getAliases();
    assertTrue(fieldAliases.contains("fieldAlias1"));
    assertTrue(fieldAliases.contains("fieldAlias2"));
  }

  @Test
  public void testWithIncludeAfter()
  {
    WithIncludeAfter withIncludeAfter = new WithIncludeAfter();
    assertTrue(withIncludeAfter.schema().isFieldsBeforeIncludes());
    assertTrue(withIncludeAfter.schema().getInclude().contains(new Simple().schema()));
    assertTrue(withIncludeAfter.schema().getInclude().contains(new Note().schema()));
  }

  @Test
  public void testWithOrder()
  {
    WithOrders withOrders = new WithOrders();
    assertTrue(withOrders.schema().getField("desc").getOrder() == RecordDataSchema.Field.Order.DESCENDING);
  }
}

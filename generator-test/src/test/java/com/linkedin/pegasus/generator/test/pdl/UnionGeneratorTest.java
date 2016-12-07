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

import com.linkedin.pegasus.generator.test.idl.enums.Fruits;
import com.linkedin.pegasus.generator.test.idl.records.Empty;
import com.linkedin.pegasus.generator.test.idl.records.Simple;
import com.linkedin.pegasus.generator.test.idl.records.SimpleArray;
import com.linkedin.pegasus.generator.test.idl.records.SimpleMap;
import com.linkedin.pegasus.generator.test.idl.unions.WithComplexTypesUnion;
import com.linkedin.pegasus.generator.test.idl.unions.WithPrimitiveCustomTypesUnion;
import com.linkedin.pegasus.generator.test.idl.unions.WithPrimitivesUnion;
import com.linkedin.pegasus.generator.test.pdl.fixtures.CustomInt;
import org.testng.annotations.Test;


public class UnionGeneratorTest extends GeneratorTest
{

  @Test
  public void testWithComplexTypesUnion()
      throws Throwable
  {
    WithComplexTypesUnion.Union recordMember = WithComplexTypesUnion.Union.create(new Empty());
    WithComplexTypesUnion.Union enumMember = WithComplexTypesUnion.Union.create(Fruits.APPLE);
    SimpleMap simpleMap = new SimpleMap();
    Simple m1 = new Simple();
    m1.setMessage("m1");
    simpleMap.put("a", m1);
    WithComplexTypesUnion.Union mapMember = WithComplexTypesUnion.Union.create(simpleMap);
    SimpleArray simpleArray = new SimpleArray();
    Simple a1 = new Simple();
    a1.setMessage("a1");
    simpleArray.add(a1);
    WithComplexTypesUnion.Union arrayMember = WithComplexTypesUnion.Union.create(simpleArray);

    WithComplexTypesUnion withRecord = new WithComplexTypesUnion();
    withRecord.setUnion(recordMember);
    assertJson(withRecord, load("WithComplexTypesUnion_Empty.json"));

    WithComplexTypesUnion withEnum = new WithComplexTypesUnion();
    withEnum.setUnion(enumMember);
    assertJson(withEnum, load("WithComplexTypesUnion_Enum.json"));

    WithComplexTypesUnion withArray = new WithComplexTypesUnion();
    withArray.setUnion(arrayMember);
    assertJson(withArray, load("WithComplexTypesUnion_Array.json"));

    WithComplexTypesUnion withMap = new WithComplexTypesUnion();
    withMap.setUnion(mapMember);
    assertJson(withMap, load("WithComplexTypesUnion_Map.json"));
  }

  @Test
  public void testWithPrimitivesUnion()
      throws Throwable
  {
    WithPrimitivesUnion.Union intMember = WithPrimitivesUnion.Union.create(1);
    WithPrimitivesUnion.Union longMember = WithPrimitivesUnion.Union.create(2L);
    WithPrimitivesUnion.Union floatMember = WithPrimitivesUnion.Union.create(3.0f);
    WithPrimitivesUnion.Union doubleMember = WithPrimitivesUnion.Union.create(4.0d);
    WithPrimitivesUnion.Union stringMember = WithPrimitivesUnion.Union.create("str");
    WithPrimitivesUnion.Union booleanMember = WithPrimitivesUnion.Union.create(true);
    WithPrimitivesUnion.Union bytesMember = WithPrimitivesUnion.Union.create(SchemaFixtures.bytes1);

    WithPrimitivesUnion withInt = new WithPrimitivesUnion();
    withInt.setUnion(intMember);
    assertJson(withInt, load("WithPrimitivesUnion_int.json"));

    WithPrimitivesUnion withLong = new WithPrimitivesUnion();
    withLong.setUnion(longMember);
    assertJson(withLong, load("WithPrimitivesUnion_long.json"));

    WithPrimitivesUnion withFloat = new WithPrimitivesUnion();
    withFloat.setUnion(floatMember);
    assertJson(withFloat, load("WithPrimitivesUnion_float.json"));

    WithPrimitivesUnion withDouble = new WithPrimitivesUnion();
    withDouble.setUnion(doubleMember);
    assertJson(withDouble, load("WithPrimitivesUnion_double.json"));

    WithPrimitivesUnion withBoolean = new WithPrimitivesUnion();
    withBoolean.setUnion(booleanMember);
    assertJson(withBoolean, load("WithPrimitivesUnion_boolean.json"));

    WithPrimitivesUnion withString = new WithPrimitivesUnion();
    withString.setUnion(stringMember);
    assertJson(withString, load("WithPrimitivesUnion_string.json"));

    WithPrimitivesUnion withBytes = new WithPrimitivesUnion();
    withBytes.setUnion(bytesMember);
    assertJson(withBytes, load("WithPrimitivesUnion_bytes.json"));
  }

  @Test
  public void testWithPrimitiveCustomTypesUnion()
      throws Throwable
  {
    String json = load("WithPrimitiveCustomTypesUnion_int.json");
    WithPrimitiveCustomTypesUnion original = new WithPrimitiveCustomTypesUnion();
    original.setUnion(WithPrimitiveCustomTypesUnion.Union.create(new CustomInt(1)));
    assertJson(original, json);

    WithPrimitiveCustomTypesUnion roundTripped = new WithPrimitiveCustomTypesUnion(roundTrip(original.data()));
    assertJson(roundTripped, json);
  }
}

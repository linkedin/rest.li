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

import com.linkedin.data.template.BooleanArray;
import com.linkedin.data.template.BytesArray;
import com.linkedin.data.template.DoubleArray;
import com.linkedin.data.template.FloatArray;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.data.template.LongArray;
import com.linkedin.data.template.StringArray;
import com.linkedin.pegasus.generator.test.idl.arrays.WithCustomTypesArray;
import com.linkedin.pegasus.generator.test.idl.arrays.WithCustomTypesArrayUnion;
import com.linkedin.pegasus.generator.test.idl.arrays.WithCustomTypesArrayUnionArray;
import com.linkedin.pegasus.generator.test.idl.arrays.WithPrimitivesArray;
import com.linkedin.pegasus.generator.test.idl.arrays.WithRecordArray;
import com.linkedin.pegasus.generator.test.idl.customtypes.CustomIntArray;
import com.linkedin.pegasus.generator.test.idl.enums.Fruits;
import com.linkedin.pegasus.generator.test.idl.enums.FruitsArray;
import com.linkedin.pegasus.generator.test.idl.fixed.Fixed8;
import com.linkedin.pegasus.generator.test.idl.fixed.Fixed8Array;
import com.linkedin.pegasus.generator.test.idl.records.Empty;
import com.linkedin.pegasus.generator.test.idl.records.EmptyArray;
import com.linkedin.pegasus.generator.test.idl.records.Simple;
import com.linkedin.pegasus.generator.test.idl.records.SimpleArray;
import com.linkedin.pegasus.generator.test.idl.records.SimpleArrayArray;
import com.linkedin.pegasus.generator.test.idl.records.SimpleMap;
import com.linkedin.pegasus.generator.test.idl.records.SimpleMapArray;
import com.linkedin.pegasus.generator.test.pdl.fixtures.CustomInt;
import org.testng.annotations.Test;


public class ArrayGeneratorTest extends GeneratorTest
{

  @Test
  public void testWithRecordArray()
      throws Throwable
  {
    String json = load("WithRecordArray.json");
    EmptyArray empties = new EmptyArray();
    empties.add(new Empty());
    empties.add(new Empty());
    empties.add(new Empty());
    FruitsArray fruitsArray = new FruitsArray();
    fruitsArray.add(Fruits.APPLE);
    fruitsArray.add(Fruits.BANANA);
    fruitsArray.add(Fruits.ORANGE);
    WithRecordArray original = new WithRecordArray();
    original.setEmpties(empties);
    original.setFruits(fruitsArray);
    assertJson(original, json);

    WithRecordArray roundTripped = new WithRecordArray(roundTrip(original.data()));
    assertJson(roundTripped, json);
  }

  @Test
  public void testWithPrimitivesArray()
      throws Throwable
  {
    String json = load("WithPrimitivesArray.json");
    WithPrimitivesArray original = new WithPrimitivesArray();
    IntegerArray ints = new IntegerArray();
    ints.add(1);
    ints.add(2);
    ints.add(3);
    original.setInts(ints);
    LongArray longs = new LongArray();
    longs.add(10L);
    longs.add(20L);
    longs.add(30L);
    original.setLongs(longs);
    FloatArray floats = new FloatArray();
    floats.add(1.1f);
    floats.add(2.2f);
    floats.add(3.3f);
    original.setFloats(floats);
    DoubleArray doubles = new DoubleArray();
    doubles.add(11.1d);
    doubles.add(22.2d);
    doubles.add(33.3d);
    original.setDoubles(doubles);
    BooleanArray booleans = new BooleanArray();
    booleans.add(false);
    booleans.add(true);
    original.setBooleans(booleans);
    StringArray strings = new StringArray();
    strings.add("a");
    strings.add("b");
    strings.add("c");
    original.setStrings(strings);
    BytesArray bytes = new BytesArray();
    bytes.add(SchemaFixtures.bytes1);
    bytes.add(SchemaFixtures.bytes2);
    original.setBytes(bytes);
    assertJson(original, json);

    WithPrimitivesArray roundTripped = new WithPrimitivesArray(roundTrip(original.data()));
    assertJson(roundTripped, json);
  }

  @Test
  public void testWithCustomTypesArray()
      throws Throwable
  {
    String json = load("WithCustomTypesArray.json");
    WithCustomTypesArray original = new WithCustomTypesArray();
    CustomIntArray ints = new CustomIntArray();
    ints.add(new CustomInt(1));
    ints.add(new CustomInt(2));
    ints.add(new CustomInt(3));
    original.setInts(ints);
    SimpleArrayArray arrays = new SimpleArrayArray();
    SimpleArray simpleArray = new SimpleArray();
    Simple simple = new Simple();
    simple.setMessage("a1");
    simpleArray.add(simple);
    arrays.add(simpleArray);
    original.setArrays(arrays);
    SimpleMapArray maps = new SimpleMapArray();
    SimpleMap map = new SimpleMap();
    Simple simplem1 = new Simple();
    simplem1.setMessage("m1");
    map.put("a", simplem1);
    maps.add(map);
    original.setMaps(maps);
    Simple simpleu1 = new Simple();
    simpleu1.setMessage("u1");
    WithCustomTypesArrayUnionArray unions = new WithCustomTypesArrayUnionArray();
    unions.add(WithCustomTypesArrayUnion.create(1));
    unions.add(WithCustomTypesArrayUnion.create("str"));
    unions.add(WithCustomTypesArrayUnion.create(simpleu1));
    original.setUnions(unions);
    Fixed8 fixed8 = new Fixed8(SchemaFixtures.bytesFixed8);
    Fixed8Array fixed8Array = new Fixed8Array();
    fixed8Array.add(fixed8);
    original.setFixed(fixed8Array);
    assertJson(original, json);

    WithCustomTypesArray roundTripped = new WithCustomTypesArray(roundTrip(original.data()));
    assertJson(roundTripped, json);
  }
}

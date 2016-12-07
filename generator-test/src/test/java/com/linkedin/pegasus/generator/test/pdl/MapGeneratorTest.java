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

import com.linkedin.data.template.BooleanMap;
import com.linkedin.data.template.BytesMap;
import com.linkedin.data.template.DoubleMap;
import com.linkedin.data.template.FloatMap;
import com.linkedin.data.template.IntegerMap;
import com.linkedin.data.template.LongMap;
import com.linkedin.data.template.StringMap;
import com.linkedin.pegasus.generator.test.idl.customtypes.CustomIntMap;
import com.linkedin.pegasus.generator.test.idl.enums.Fruits;
import com.linkedin.pegasus.generator.test.idl.enums.FruitsMap;
import com.linkedin.pegasus.generator.test.idl.fixed.Fixed8;
import com.linkedin.pegasus.generator.test.idl.fixed.Fixed8Map;
import com.linkedin.pegasus.generator.test.idl.maps.WithComplexTypesMap;
import com.linkedin.pegasus.generator.test.idl.maps.WithComplexTypesMapUnion;
import com.linkedin.pegasus.generator.test.idl.maps.WithComplexTypesMapUnionMap;
import com.linkedin.pegasus.generator.test.idl.maps.WithCustomTypesMap;
import com.linkedin.pegasus.generator.test.idl.maps.WithPrimitivesMap;
import com.linkedin.pegasus.generator.test.idl.records.Empty;
import com.linkedin.pegasus.generator.test.idl.records.EmptyMap;
import com.linkedin.pegasus.generator.test.idl.records.Simple;
import com.linkedin.pegasus.generator.test.idl.records.SimpleArray;
import com.linkedin.pegasus.generator.test.idl.records.SimpleArrayMap;
import com.linkedin.pegasus.generator.test.idl.records.SimpleMap;
import com.linkedin.pegasus.generator.test.idl.records.SimpleMapMap;
import com.linkedin.pegasus.generator.test.pdl.fixtures.CustomInt;
import org.testng.annotations.Test;


public class MapGeneratorTest extends GeneratorTest
{

  @Test
  public void testWithComplexTypesMap()
      throws Throwable
  {
    String json = load("WithComplexTypesMap.json");
    WithComplexTypesMap original = new WithComplexTypesMap();
    EmptyMap empties = new EmptyMap();
    empties.put("a", new Empty());
    empties.put("b", new Empty());
    empties.put("c", new Empty());
    original.setEmpties(empties);
    FruitsMap fruits = new FruitsMap();
    fruits.put("a", Fruits.APPLE);
    fruits.put("b", Fruits.BANANA);
    fruits.put("c", Fruits.ORANGE);
    original.setFruits(fruits);
    SimpleArrayMap simpleArrays = new SimpleArrayMap();
    Simple simplev1 = new Simple();
    simplev1.setMessage("v1");
    Simple simplev2 = new Simple();
    simplev2.setMessage("v2");
    SimpleArray simpleArray = new SimpleArray();
    simpleArray.add(simplev1);
    simpleArray.add(simplev2);
    simpleArrays.put("a", simpleArray);
    original.setArrays(simpleArrays);

    SimpleMap simpleMapi1 = new SimpleMap();
    Simple simpleo1i1 = new Simple();
    simpleo1i1.setMessage("o1i1");
    simpleMapi1.put("i1", simpleo1i1);

    Simple simpleo1i2 = new Simple();
    simpleo1i2.setMessage("o1i2");
    simpleMapi1.put("i2", simpleo1i2);

    SimpleMapMap maps = new SimpleMapMap();
    maps.put("o1", simpleMapi1);
    original.setMaps(maps);

    WithComplexTypesMapUnionMap unions = new WithComplexTypesMapUnionMap();
    unions.put("a", WithComplexTypesMapUnion.create(1));
    unions.put("b", WithComplexTypesMapUnion.create("u1"));
    original.setUnions(unions);

    Fixed8Map fixed = new Fixed8Map();
    fixed.put("a", new Fixed8(SchemaFixtures.bytesFixed8));
    original.setFixed(fixed);

    assertJson(original, json);

    WithComplexTypesMap roundTripped = new WithComplexTypesMap(roundTrip(original.data()));
    assertJson(roundTripped, json);
  }

  @Test
  public void testWithPrimitivesMap()
      throws Throwable
  {
    String json = load("WithPrimitivesMap.json");
    WithPrimitivesMap original = new WithPrimitivesMap();
    IntegerMap ints = new IntegerMap();
    ints.put("a", 1);
    ints.put("b", 2);
    ints.put("c", 3);
    original.setInts(ints);
    LongMap longs = new LongMap();
    longs.put("a", 10L);
    longs.put("b", 20L);
    longs.put("c", 30L);
    original.setLongs(longs);
    FloatMap floats = new FloatMap();
    floats.put("a", 1.1f);
    floats.put("b", 2.2f);
    floats.put("c", 3.3f);
    original.setFloats(floats);
    DoubleMap doubles = new DoubleMap();
    doubles.put("a", 11.1d);
    doubles.put("b", 22.2d);
    doubles.put("c", 33.3d);
    original.setDoubles(doubles);
    BooleanMap booleans = new BooleanMap();
    booleans.put("a", true);
    booleans.put("b", false);
    booleans.put("c", true);
    original.setBooleans(booleans);
    StringMap strings = new StringMap();
    strings.put("a", "string1");
    strings.put("b", "string2");
    strings.put("c", "string3");
    original.setStrings(strings);
    BytesMap bytes = new BytesMap();
    bytes.put("a", SchemaFixtures.bytes1);
    bytes.put("b", SchemaFixtures.bytes2);
    bytes.put("c", SchemaFixtures.bytes3);
    original.setBytes(bytes);
    assertJson(original, json);

    WithPrimitivesMap roundTripped = new WithPrimitivesMap(roundTrip(original.data()));
    assertJson(roundTripped, json);
  }

  @Test
  public void testWithCustomTypesMap()
      throws Throwable
  {
    String json = load("WithCustomTypesMap.json");
    WithCustomTypesMap original = new WithCustomTypesMap();
    CustomIntMap ints = new CustomIntMap();
    ints.put("a", new CustomInt(1));
    ints.put("b", new CustomInt(2));
    ints.put("c", new CustomInt(3));
    original.setInts(ints);
    assertJson(original, json);

    WithCustomTypesMap roundTripped = new WithCustomTypesMap(roundTrip(original.data()));
    assertJson(roundTripped, json);
  }
}

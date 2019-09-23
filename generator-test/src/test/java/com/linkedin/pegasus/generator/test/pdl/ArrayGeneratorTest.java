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
import java.util.Arrays;
import org.testng.annotations.Test;


public class ArrayGeneratorTest extends GeneratorTest
{

  @Test
  public void testWithRecordArray()
      throws Throwable
  {
    String json = load("WithRecordArray.json");

    WithRecordArray original = new WithRecordArray()
        .setEmpties(new EmptyArray(new Empty(), new Empty(), new Empty()))
        .setFruits(new FruitsArray(Fruits.APPLE, Fruits.BANANA, Fruits.ORANGE));

    assertJson(original, json);

    WithRecordArray roundTripped = new WithRecordArray(roundTrip(original.data()));
    assertJson(roundTripped, json);
  }

  @Test
  public void testWithPrimitivesArray()
      throws Throwable
  {
    String json = load("WithPrimitivesArray.json");

    WithPrimitivesArray original = new WithPrimitivesArray()
        .setInts(new IntegerArray(Arrays.asList(1, 2, 3)))
        .setLongs(new LongArray(10L, 20L, 30L))
        .setFloats(new FloatArray(1.1f, 2.2f, 3.3f))
        .setDoubles(new DoubleArray(11.1d, 22.2d, 33.3d))
        .setBooleans(new BooleanArray(false, true))
        .setStrings(new StringArray("a", "b", "c"))
        .setBytes(new BytesArray(SchemaFixtures.bytes1, SchemaFixtures.bytes2));

    assertJson(original, json);

    WithPrimitivesArray roundTripped = new WithPrimitivesArray(roundTrip(original.data()));
    assertJson(roundTripped, json);
  }

  @Test
  public void testWithCustomTypesArray()
      throws Throwable
  {
    String json = load("WithCustomTypesArray.json");

    SimpleMap map = new SimpleMap();
    map.put("a", new Simple().setMessage("m1"));

    WithCustomTypesArray original = new WithCustomTypesArray()
        .setInts(new CustomIntArray(new CustomInt(1), new CustomInt(2), new CustomInt(3)))
        .setArrays(new SimpleArrayArray(new SimpleArray(new Simple().setMessage("a1"))))
        .setMaps(new SimpleMapArray(map))
        .setUnions(new WithCustomTypesArrayUnionArray(
            WithCustomTypesArrayUnion.create(1),
            WithCustomTypesArrayUnion.create("str"),
            WithCustomTypesArrayUnion.create(new Simple().setMessage("u1"))))
        .setFixed(new Fixed8Array(new Fixed8(SchemaFixtures.bytesFixed8)));

    assertJson(original, json);

    WithCustomTypesArray roundTripped = new WithCustomTypesArray(roundTrip(original.data()));
    assertJson(roundTripped, json);
  }
}

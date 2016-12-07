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

import com.linkedin.data.DataMap;
import com.linkedin.pegasus.generator.test.idl.enums.Fruits;
import com.linkedin.pegasus.generator.test.idl.records.Empty;
import com.linkedin.pegasus.generator.test.idl.records.EmptyArray;
import com.linkedin.pegasus.generator.test.idl.records.EmptyMap;
import com.linkedin.pegasus.generator.test.idl.records.WithComplexTyperefs;
import com.linkedin.pegasus.generator.test.idl.records.WithCustomRecord;
import com.linkedin.pegasus.generator.test.idl.typerefs.UnionTyperef;
import com.linkedin.pegasus.generator.test.idl.unions.WithRecordCustomTypeUnion;
import com.linkedin.pegasus.generator.test.pdl.fixtures.CustomRecord;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


public class TyperefGeneratorTest extends GeneratorTest
{
  @Test
  public void testWithComplexTyperefs()
      throws Throwable
  {
    WithComplexTyperefs original = new WithComplexTyperefs();
    original.setEnum(Fruits.APPLE);
    original.setRecord(new Empty());
    EmptyMap emptyMap = new EmptyMap();
    emptyMap.put("a", new Empty());
    original.setMap(emptyMap);
    EmptyArray emptyArray = new EmptyArray();
    emptyArray.add(new Empty());
    original.setArray(emptyArray);
    original.setUnion(UnionTyperef.create(1));
    WithComplexTyperefs roundTripped = new WithComplexTyperefs(roundTrip(original.data()));
    assertEquals(original.data(), roundTripped.data());
  }

  @Test
  public void testCustomTypeOfRecordDefault()
      throws Throwable
  {
    WithCustomRecord original = new WithCustomRecord();
    assertEquals(original.getCustom().getTitle(), "defaultTitle");
    assertEquals(original.getCustom().getBody(), "defaultBody");
  }

  @Test
  public void testCustomTypeRecordInUnion()
      throws Throwable
  {
    CustomRecord customRecord = new CustomRecord("title", "body");
    WithRecordCustomTypeUnion original =
        new WithRecordCustomTypeUnion((DataMap) WithRecordCustomTypeUnion.Union.create(customRecord).data());
    WithRecordCustomTypeUnion roundTripped = new WithRecordCustomTypeUnion(roundTrip(original.data()));
    assertEquals(original, roundTripped);
  }
}

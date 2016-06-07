/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.restli.common.testutils;

import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataMap;
import com.linkedin.restli.common.EmptyRecord;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static com.linkedin.restli.common.testutils.TestDataBuilders.toDataList;
import static com.linkedin.restli.common.testutils.TestDataBuilders.toDataMap;


@Test
public class TestDataCompare
{
  public void testMismatchedValue()
  {
    assertComparisonResultFailedWith(
        DataCompare.compare(toDataMap("field", "foo"), toDataMap("field", "bar")),
        "field\nExpected: foo\n got: bar"
    );
  }

  public void testMismatchedType()
  {
    assertComparisonResultFailedWith(
        DataCompare.compare(toDataMap("field", "foo"), toDataMap("field", 1L)),
        "field\nExpected: string\n got: long"
    );
  }

  public void testMismatchedValueInNestedMap()
  {
    assertComparisonResultFailedWith(
        DataCompare.compare(
            toDataMap("field", toDataMap("nestedField", "foo")),
            toDataMap("field", toDataMap("nestedField", "bar"))),
        "field.nestedField\nExpected: foo\n got: bar"
    );
  }

  public void testMismatchedValueList()
  {
    assertComparisonResultFailedWith(
        DataCompare.compare(toDataList("foo", "bar"), toDataList("foo", "qux")),
        "[1]\nExpected: bar\n got: qux"
    );
  }

  public void testMismatchedTypeList()
  {
    assertComparisonResultFailedWith(
        DataCompare.compare(toDataList("foo"), toDataList(1L)),
        "[0]\nExpected: string\n got: long"
    );
  }

  public void testMismatchedValueInNestedList()
  {
    assertComparisonResultFailedWith(
        DataCompare.compare(
            toDataMap("field", toDataList("foo")),
            toDataMap("field", toDataList("bar"))
        ),
        "field[0]\nExpected: foo\n got: bar"
    );
  }

  public void testMissingField()
  {
    assertComparisonResultFailedWith(
        DataCompare.compare(
            toDataMap("missingField", 0),
            new DataMap()
        ),
        "Expected: missingField\n but none found"
    );
  }

  public void testExtraField()
  {
    assertComparisonResultFailedWith(
        DataCompare.compare(
            new DataMap(),
            toDataMap("extraField", 0)
        ),
        "Unexpected: extraField"
    );
  }

  public void testMismatchedListSize()
  {
    assertComparisonResultFailedWith(
        DataCompare.compare(
            toDataList("", ""),
            toDataList("", "", "")
        ),
        "[] Expected 2 values but got 3"
    );
  }

  public void testMismatchedValueUnion()
  {
    assertComparisonResultFailedWith(
        DataCompare.compare(
            toDataMap("field", toDataMap(EmptyRecord.class.getCanonicalName(), "foo")),
            toDataMap("field", toDataMap(EmptyRecord.class.getCanonicalName(), "bar"))
        ),
        "field{EmptyRecord}\nExpected: foo\n got: bar"
    );
  }

  public void testMatch()
  {
    DataMap data = toDataMap("mapField", toDataMap("key", toDataList("value1", "value2")));
    DataCompare.Result result = DataCompare.compare(data, data);
    assertFalse(result.hasError());
  }

  public void testNumbersMismatch()
  {
    assertComparisonResultFailedWith(
        DataCompare.compare(
            toDataMap("numberField", 0),
            toDataMap("numberField", 0L),
            new DataCompare.Options(false, false)
        ),
        "numberField\nExpected: int\n got: long"
    );
  }

  public void testNumbersMatch()
  {
    DataCompare.Result compareResult = DataCompare.compare(
        toDataMap("longField", 0, "doubleField", 0.0f, "floatField", 0),
        toDataMap("longField", 0, "doubleField", 0.0d, "floatField", 0.0f)
    );
    assertFalse(compareResult.hasError());
  }

  public void testStringLikeMismatch()
  {
    assertComparisonResultFailedWith(
        DataCompare.compare(
            toDataMap("fixedField", "foo"),
            toDataMap("fixedField", ByteString.copy(new byte[]{102, 111, 111})),
            new DataCompare.Options(false, false)
        ),
        "fixedField\nExpected: string\n got: bytes"
    );
  }

  public void testStringLikeMatch()
  {
    DataCompare.Result compareResult = DataCompare.compare(
        toDataMap("fixedField", "foo"),
        toDataMap("fixedField", ByteString.copy(new byte[]{102, 111, 111}))
    );
    assertFalse(compareResult.hasError());
  }

  public void testNull()
  {
    DataMap nullFieldMap = toDataMap("nullField", Data.NULL);
    assertFalse(DataCompare.compare(nullFieldMap, nullFieldMap).hasError());
  }

  private void assertComparisonResultFailedWith(DataCompare.Result actual, String expected)
  {
    assertTrue(actual.hasError());
    assertEquals(actual.toString().replaceAll(" +", " ").trim(), expected);
  }
}
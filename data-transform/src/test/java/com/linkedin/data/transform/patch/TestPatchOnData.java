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

package com.linkedin.data.transform.patch;


import com.linkedin.data.DataMap;
import com.linkedin.data.transform.DataComplexProcessor;
import com.linkedin.data.transform.DataProcessingException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.linkedin.data.TestUtil.dataMapFromString;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;


public class TestPatchOnData
{

  /**
   * Set of tests. Each test contains a description, data, on which patch
   * will be performed and expected result.
   * Data and patch are expressed as JSON strings.
   *
   * For clarity it is allowed to use ' instead of ". Every ' character will be
   * replaced by " before parsing. THIS IS ONLY RELATED TO THIS TEST CLASS
   * TO MAKE TESTS CASES MORE CLEAR, PEGASUS DOESN'T DO ANYTHING LIKE THAT.
   */
  @DataProvider
  public Object[][] patch()
  {
    return new String[][] {
    {
      /*description:*/"Patch is empty. Data object should not be modified.",
      /*data:*/       "{'a': 10, 'b': {'c': 'aaa'}}",
      /*patch:*/     "{}",
      /*expected*/    "{'a': 10, 'b': {'c': 'aaa'}}"
    },
    {
      /*description:*/"Delete field that does not exist should not cause error or stop processing, it " +
      		"should be ignored.",
      /*data:*/       "{'a': 10, 'b': {'c': 'aaa'}}",
      /*patch:*/     "{'$delete': ['d', 'a'], 'b': { '$delete': ['e']}}",
      /*expected*/    "{'b': {'c': 'aaa'}}"
    },
    {
      /*description:*/"Branch of patch with deeply nested $delete field should be ignored when such a " +
      		"branch does not exist in object.",
      /*data:*/       "{'a': 10, 'b': {'c': 'aaa'}}",
      /*patch:*/     "{'e': { '$delete': ['c']}}",
      /*expected*/    "{'a': 10, 'b': {'c': 'aaa'}}"
    },
    {
      /*description:*/"Branch of patch with deeply nested $delete field should not be ignored when such a " +
            "branch does not exist in object and deeply nested $delete resides along with deeply nested $set.",
      /*data:*/       "{'a': 10, 'b': {'c': 'aaa'}}",
      /*patch:*/     "{'e': { '$delete': ['c'], '$set': {'q': 1}}}",
      /*expected*/    "{'a': 10, 'b': {'c': 'aaa'}, 'e': {'q': 1}}"
    },
    {
      /*description:*/ "Patch of two fields of simple type at the root level." +
      		" $set command should overwrite value if it already" +
      		"exists in data and add to the parent object if it doesn't exist.",
      /*data:*/        "{'a': 10, 'b': {'c': 'aaa'}}",
      /*patch:*/      "{'$set': {'a': 20, 'd': 'ola'}}",
      /*expected*/     "{'a': 20, 'b': {'c': 'aaa'}, 'd': 'ola'}"
    },
    {
      /*description:*/"$delete command containing two fields at root level and one field at " +
            "nested level. Specified fields should be remved.",
      /*data:*/       "{'a': 10, 'b': {'c': 'aaa', 'e': 'hej'}, 'd': 'ola'}",
      /*patch:*/     "{'$delete': ['a', 'd'], 'b': {'$delete': ['e']}}",
      /*expected*/    "{'b': {'c': 'aaa'}}"
    },
    {
      /*description:*/"Deeply nested $delete operation aong with $set operation, while data object " +
      		"does not have branch those commands reside in. In such case the $set operation should " +
      		"succeed and missing path leading to it should be created.",
      /*data:*/       "{'a': 10}",
      /*patch:*/     "{'$delete': ['a', 'd'], 'b': { 'c': {'$delete': ['a'], '$set': {'e': 'f'}}}}",
      /*expected*/    "{'b': {'c': {'e': 'f'}}}"
    },
    {
      /*description:*/"Deeply nested $set operation. Only referenced field is set, rest of fields " +
      		"remain unchanged.",
      /*data:*/
      "{"                       +
      "  'a' : 0,"            +
      "  'b' : {"             +
      "     'b1' : 0,"        +
      "     'b2' : {"         +
      "        'c1' : 0,"     +
      "        'c2' : {"      +
      "           'd1' : 0"   +
      "        }"               +
      "     }"                  +
      "   }"                    +
      "}",
      /*patch:*/     "{ 'b' : { 'b2' : { 'c2' : { '$set' : { 'd1' : 3 } } } }}",
      /*expected*/
      "{"                       +
      "  'a' : 0,"            +
      "  'b' : {"             +
      "     'b1' : 0,"        +
      "     'b2' : {"         +
      "        'c1' : 0,"     +
      "        'c2' : {"      +
      "           'd1' : 3"   +
      "        }"               +
      "     }"                  +
      "   }"                    +
      "}",
    },
    {
      /*description:*/"Reorder an array item (move forward)",
      /*data:*/       "{'arrayField': [" +
                      "  {'foo': 1, 'bar': 'a'}," +
                      "  {'foo': 2, 'bar': 'b'}" +
                      "]}",
      /*patch:*/      "{'arrayField': {'$reorder': [{'$fromIndex': 1, '$toIndex': 0}]}}",
      /*expected:*/   "{'arrayField': [" +
                      "  {'foo': 2, 'bar': 'b'}," +
                      "  {'foo': 1, 'bar': 'a'}" +
                      "]}",
    },
    {
      /*description:*/"Reorder an array item (move back)",
      /*data:*/       "{'arrayField': [" +
                      "  {'foo': 1, 'bar': 'a'}," +
                      "  {'foo': 2, 'bar': 'b'}" +
                      "]}",
      /*patch:*/      "{'arrayField': {'$reorder': [{'$fromIndex': 0, '$toIndex': 1}]}}",
      /*expected:*/   "{'arrayField': [" +
                      "  {'foo': 2, 'bar': 'b'}," +
                      "  {'foo': 1, 'bar': 'a'}" +
                      "]}",
    },
    {
      /*description:*/"Reorder two arrays",
      /*data:*/       "{'arrayField': [0, 1, 2, 3, 4, 5], 'containerField': {'nestedArrayField': ['0', '1', '2', '3', '4', '5']}}",
      /*patch:*/      "{'arrayField': {'$reorder': [{'$fromIndex': 0, '$toIndex': 3}]}, 'containerField': {'nestedArrayField': {'$reorder': [{'$fromIndex': 5, '$toIndex': 2}]}}}",
      /*expected:*/   "{'arrayField': [1, 2, 3, 0, 4, 5], 'containerField': {'nestedArrayField': ['0', '1', '5', '2', '3', '4']}}",
    }};
  }

  private void genericPatchTest(DataMap data, DataMap patch,
                                        DataMap expected, String description) throws DataProcessingException
  {
    String dataBefore = data.toString();
    DataComplexProcessor processor = new DataComplexProcessor(new Patch(), patch, data);
    processor.run(false);
    assertEquals(data, expected, "The following test failed: \n" + description  +
                 "\nData: " + dataBefore + "\nPatch: " + patch +
                 "\nExpected: " + expected + "\nActual result: " + data);
  }

  @Test(dataProvider = "patch")
  public void testPatchOnData(String description, String data, String patch, String expected)
      throws IOException, DataProcessingException
  {
    genericPatchTest(dataMapFromString(data.replace('\'', '"')),
                     dataMapFromString(patch.replace('\'', '"')),
                     dataMapFromString(expected.replace('\'', '"')),
                     description);
  }

  @DataProvider
  public Object[][] invalidPatch()
  {
    return new String[][] {
        {
            "ImplicitSetOperationInPatchIsNotSupported",
            "{}",
            "{ \"a\": 1 }"
        },
        {
            "MergingSimpleTypeValueWithComplexPatchNotSupported",
            "{\"a\": 1}",
            "{ \"a\": { \"b\": 1} }"
        },
        {
            "DeleteAndSetSameField",
            "{\"a\": 1}",
            "{ \"$set\": { \"b\": 1}, \"$delete\": [\"b\"] }"
        },
        {
            "DeleteAndBeBranchAtSameTime",
            "{\"a\": 1}",
            "{ \"b\": { \"$set\": { \"b\": 1} }, \"$delete\": [\"b\"] }"
        },
        {
            "SetAndBeBranchAtSameTime",
            "{\"a\": 1}",
            "{ \"b\": { \"$set\": { \"b\": 1} }, \"$set\": {\"b\": 1} }"
        },
        {
            "SetAndReorderAtSameTime",
            "{\"a\": [1, 2]}",
            "{\"a\": {\"$reorder\": [{\"$fromIndex\": 1, \"$toIndex\": 0}]}, \"$set\": {\"a\": [100, 200]} }"
        },
        {
            "DeleteAndReorderAtSameTime",
            "{\"a\": [1, 2]}",
            "{\"a\": {\"$reorder\": [{\"$fromIndex\": 1, \"$toIndex\": 0}]}, \"$delete\": [\"a\"] }"
        },
        {
            "ReorderMultipleArrayItems",
            "{\"a\": [1, 2, 3, 4]}",
            "{\"a\": {\"$reorder\": [{\"$fromIndex\": 1, \"$toIndex\": 0}, {\"$fromIndex\": 2, \"$toIndex\": 3}]}}"
        },
        {
            "ReorderInvalidFromIndex",
            "{\"a\": [1, 2, 3, 4]}",
            "{\"a\": {\"$reorder\": [{\"$fromIndex\": -2, \"$toIndex\": 0}]}}"
        },
        {
            "ReorderInvalidFromIndex",
            "{\"a\": [1, 2, 3, 4]}",
            "{\"a\": {\"$reorder\": [{\"$fromIndex\": 4, \"$toIndex\": 0}]}}"
        },
        {
            "ReorderInvalidToIndex",
            "{\"a\": [1, 2, 3, 4]}",
            "{\"a\": {\"$reorder\": [{\"$fromIndex\": 0, \"$toIndex\": -1}]}}"
        },
        {
            "ReorderInvalidToIndex",
            "{\"a\": [1, 2, 3, 4]}",
            "{\"a\": {\"$reorder\": [{\"$fromIndex\": 0, \"$toIndex\": 5}]}}"
        },
    };
  }

  @Test(dataProvider = "invalidPatch")
  public void testInvalidPatch(String description, String data, String patch) throws IOException, DataProcessingException
  {
    DataComplexProcessor processor = new DataComplexProcessor(new Patch(),
        dataMapFromString(patch),
        dataMapFromString(data));
    boolean thrown = false;
    try
    {
      processor.run(false);
    }
    catch (DataProcessingException e)
    {
      thrown = true;
    }
    if (!thrown)
    {
      fail(description + " - expected DataProcessingException to be thrown");
    }
  }

}

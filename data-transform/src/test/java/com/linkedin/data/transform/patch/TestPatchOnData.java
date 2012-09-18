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

/**
 * $id$
 */
package com.linkedin.data.transform.patch;

import static org.testng.Assert.*;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.testng.annotations.Test;

import com.linkedin.data.DataMap;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.transform.DataMapProcessor;
import com.linkedin.data.transform.DataProcessingException;
import static com.linkedin.data.TestUtil.dataMapFromString;


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
  public static final String[][] TESTS = new String[][] {
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

    }

  };

  private void genericPatchTest(DataMap data, DataMap patch,
                                        DataMap expected, String description) throws DataProcessingException {
    String dataBefore = data.toString();
    DataMapProcessor processor = new DataMapProcessor(new Patch(), patch, data);
    processor.run(false);
    assertEquals(data, expected, "The following test failed: \n" + description  +
                 "\nData: " + dataBefore + "\nPatch: " + patch +
                 "\nExpected: " + expected + "\nActual result: " + data);
  }

  @Test
  public void testPatchOnData() throws JsonParseException,
      IOException,
      DataProcessingException
  {
    for (String[] testCase : TESTS) {
      genericPatchTest(dataMapFromString(testCase[1].replace('\'', '"')),
                               dataMapFromString(testCase[2].replace('\'', '"')),
                               dataMapFromString(testCase[3].replace('\'', '"')),
                               testCase[0]);
    }
  }

  @Test
  public void testImplicitSetOperationInPatchIsNotSupported() throws JsonParseException, IOException, DataProcessingException {
    DataMapProcessor processor = new DataMapProcessor(new Patch(),
                                                      dataMapFromString("{ \"a\": 1 }"),  //command $set should be used
                                                      dataMapFromString("{}"));
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
      fail("expected DataProcessingException to be thrown");

  }

  @Test
  public void testMergingSimpleTypeValueWithComplexPatchNotSupported() throws JsonParseException, IOException, DataProcessingException {
    DataMapProcessor processor = new DataMapProcessor(new Patch(),
                                                      dataMapFromString("{ \"a\": { \"b\": 1} }"),  //command $set should be used
                                                      dataMapFromString("{\"a\": 1}"));
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
      fail("expected DataProcessingException to be thrown");
  }

  @Test
  public void testDeleteAndSetSameField() throws JsonParseException, IOException, DataProcessingException {
    DataMapProcessor processor = new DataMapProcessor(new Patch(),
                                                      dataMapFromString(
                                                        "{ \"$set\": { \"b\": 1}, \"$delete\": [\"b\"] }"),  //command $set should be used
                                                      dataMapFromString("{\"a\": 1}"));
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
      fail("expected DataProcessingException to be thrown");
  }

  @Test
  public void testDeleteAndBeBranchAtSameTime() throws JsonParseException, IOException, DataProcessingException {
    DataMapProcessor processor = new DataMapProcessor(new Patch(),
                                                      dataMapFromString(
                                                        "{ \"b\": { \"$set\": { \"b\": 1} }, \"$delete\": [\"b\"] }"),  //command $set should be used
                                                      dataMapFromString("{\"a\": 1}"));
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
      fail("expected DataProcessingException to be thrown");
  }

  @Test
  public void testSetAndBeBranchAtSameTime() throws JsonParseException, IOException, DataProcessingException {
    DataMapProcessor processor = new DataMapProcessor(new Patch(),
                                                      dataMapFromString(
                                                        "{ \"b\": { \"$set\": { \"b\": 1} }, \"$set\": {\"b\": 1} }"),  //command $set should be used
                                                      dataMapFromString("{\"a\": 1}"));
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
      fail("expected DataProcessingException to be thrown");
  }

}

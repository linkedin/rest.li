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
package com.linkedin.data.transform.filter;


import com.fasterxml.jackson.core.JsonParseException;
import com.linkedin.data.DataMap;
import com.linkedin.data.transform.DataComplexProcessor;
import com.linkedin.data.transform.DataProcessingException;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.linkedin.data.TestUtil.dataMapFromString;
import static org.testng.Assert.assertEquals;

public class TestFilterOnData
{

  /**
   * Set of tests. Each test contains a description, data, on which filter
   * will be performed and expected result.
   * Data and filter are expressed as JSON strings.
   *
   * For clarity it is allowed to use ' instead of ". Every ' character will be
   * replaced by " before parsing. THIS IS ONLY RELATED TO THIS TEST CLASS
   * TO MAKE TESTS CASES MORE CLEAR, PEGASUS DOESN'T DO ANYTHING LIKE THAT.
   */
  public static final String[][] TESTS = new String[][] {
    {
      /*description:*/"Filter is empty. Data object should not be modified.",
      /*data:*/       "{'a': 10, 'b': {'c': 'aaa'}}",
      /*filter:*/     "{}",
      /*expected*/    "{'a': 10, 'b': {'c': 'aaa'}}"
    },
    {
      /*description:*/"Filter is positive mask with one field at root level. " +
      		"Data object should contain just one field.",
      /*data:*/       "{'a': 10, 'b': {'c': 'aaa'}}",
      /*filter:*/     "{'a': 1}",
      /*expected*/    "{'a': 10}"
    },
    {
      /*description:*/"Filter is negative mask with one field at root level. " +
            "Data object should contain just one field.",
      /*data:*/       "{'a': 10, 'b': {'c': 'aaa'}}",
      /*filter:*/     "{'a': 0}",
      /*expected*/    "{'b': {'c': 'aaa'}}"
    },
    {
      /*description:*/"Filter is negative, nested mask. " +
            "Only nested, masked out field should be removed from the object.",
      /*data:*/       "{'a': 10, 'b': {'c': 'aaa', 'd': 'hello'}}",
      /*filter:*/     "{'b': { 'd': 0}}",
      /*expected*/    "{'a': 10, 'b': {'c': 'aaa'}}"
    },
    {
      /*description:*/"Basic test of positive wildcard - all fields are included by default",
      /*data:*/       "{'c': 'v', 'd': 'v'}",
      /*filter:*/     "{'$*': 1 }",
      /*expected*/    "{'c': 'v', 'd': 'v'}"
    },
    {
      /*description:*/"Basic test of negative wildcard - all fields are removed by default",
      /*data:*/       "{'c': 'v', 'd': 'v'}",
      /*filter:*/     "{'$*': 0 }",
      /*expected*/    "{}"
    },
    {
      /*description:*/"Simple wildcard filter. All values of and object (or map) should be filtered using wildcard filter. " +
      		"In this case positive filter is used inside wildcard",
      /*data:*/       "{'a': 10, 'b': {'c1': { 'a': 'aaa1', 'b': 'bbb'}, 'c2': { 'a': 'aaa2', 'd': 'ddd'} }}",
      /*filter:*/     "{'b': { '$*': { 'a': 1 } } }",
      /*expected*/    "{'b': {'c1': { 'a': 'aaa1'}, 'c2': { 'a': 'aaa2'} }}"
    },
    {
      /*description:*/"Simple wildcard filter. All values of and object (or map) should be filtered using wildcard filter. " +
            "In this case negative filter is used inside wildcard",
      /*data:*/       "{'a': 10, 'b': {'c1': { 'a': 'aaa1', 'b': 'bbb'}, 'c2': { 'a': 'aaa2', 'd': 'ddd'} }}",
      /*filter:*/     "{'b': { '$*': { 'a': 0 } } }",
      /*expected*/    "{'a': 10, 'b': {'c1': { 'b': 'bbb'}, 'c2': { 'd': 'ddd'} }}"
    },
    {
      /*description:*/"Simple wildcard filter along with normal filter. All values of and object (or map) should be " +
      		"filtered using wildcard filter. In this case negative filter is used inside wildcard along with normal positive filter.",
      /*data:*/       "{'a': 10, 'b': {'c1': { 'a': 'aaa1', 'b': 'bbb'}, 'c2': { 'a': 'aaa2', 'd': 'ddd'} }}",
      /*filter:*/     "{'b': { '$*': { 'a': 0 },  'c1': 1 } }",
      /*expected*/    "{'b': {'c1': { 'b': 'bbb'} }}"
    },
    {
      /*description:*/"Simple wildcard filter along with normal filter. All values of and object (or map) should be " +
            "filtered using wildcard filter. In this case positive filter is used inside wildcard along with normal positive filter.",
      /*data:*/       "{'a': 10, 'b': {'c1': { 'a': 'aaa1', 'b': 'bbb'}, 'c2': { 'a': 'aaa2', 'd': 'ddd'} }}",
      /*filter:*/     "{'b': { '$*': { 'a': 1 },  'c1': 1 } }",
      /*expected*/    "{'b': {'c1': { 'a': 'aaa1', 'b': 'bbb'}, 'c2': { 'a': 'aaa2'} }}"
    },
    {
      /*description:*/"Filter is positive, nested mask. " +
            "Only fields explicitly selected in the filter shoudl be in the result.",
      /*data:*/       "{'a': 10, 'b': {'c': 'aaa', 'd': 'hello'}}",
      /*filter:*/     "{'b': { 'd': 1}}",
      /*expected*/    "{'b': {'d': 'hello'}}"
    },
    {
      /*description:*/"Wildcard filter with only negative mask should not cause any field to show up if there " +
            "exist positive field on the same level.",
      /*data:*/       "{'a': 10, 'b': {'c1': { 'a': 'aaa1', 'b': 'bbb'}, 'c2': { 'a': 'aaa2', 'd': 'ddd'} }}",
      /*filter:*/     "{'b': { '$*': { 'a': 0 },  'c1': 1 } }",
      /*expected*/    "{'b': {'c1': { 'b': 'bbb'} }}"
    },
    {
      /*description:*/"Wildcard filter with only negative mask should cause all fields to show up if there " +
            "are no positive fields on this level.",
      /*data:*/       "{'a': 10, 'b': {'c1': { 'a': 'aaa1', 'b': 'bbb'}, 'c2': { 'a': 'aaa2', 'd': 'ddd'} }}",
      /*filter:*/     "{'b': { '$*': { 'a': 0 }} }",
      /*expected*/    "{'a': 10, 'b': {'c1': { 'b': 'bbb'}, 'c2': { 'd': 'ddd'}}}"
    },
    {
      /*description:*/"Wildcard filter with only negative mask after composition with 1",
      /*data:*/       "{'a': 10, 'b': {'c1': { 'a': 'aaa1', 'b': 'bbb'}, 'c2': { 'a': 'aaa2', 'd': 'ddd'} }}",
      /*filter:*/     "{'b': { '$*': { '$*': 1, 'a': 0 }} }",
      /*expected*/    "{'b': {'c1': { 'b': 'bbb'}, 'c2': { 'd': 'ddd'}}}"
    },
    {
      /*description:*/"Wildcard filter with only negative mask after composition with 1. Some objects in result become empty.",
      /*data:*/       "{'a': 10, 'b': {'c1': { 'a': 'aaa1', 'b': 'bbb'}, 'c2': { 'a': 'aaa2', 'd': 'ddd'} }}",
      /*filter:*/     "{'b': { '$*': { '$*': 1, 'a': 0, 'b': 0 }} }",
      /*expected*/    "{'b': {'c1': { }, 'c2': { 'd': 'ddd'}}}"
    },
    {
      /*description:*/"Wildcard filter with only negative mask after composition with 1. Some objects in result become empty.",
      /*data:*/       "{'a': 10, 'b': {'c1': { 'a': 'aaa1', 'b': 'bbb'}, 'c2': { 'a': 'aaa2', 'd': 'ddd'} }}",
      /*filter:*/     "{'b': { '$*': { '$*': 1, 'a': 0, 'b': 0 }} }",
      /*expected*/    "{'b': {'c1': { }, 'c2': { 'd': 'ddd'}}}"
    },
    {
      /*description:*/"Test that wildcard $=1 can override default node mode. In this case 'c' should be selected, because $* selects it.",
      /*data:*/       "{'c': 'v'}",
      /*filter:*/     "{'$*': { 'a': 0, '$*': 1}, 'b': 1 }",
      /*expected*/    "{'c': 'v'}"
    },
    {
      /*description:*/"Test that it is enough to mark $* as merged with 1 and it means that all siblings of $* detect it.",
      /*data:*/       "{'a': 'x', 'b': {'c': 'x', 'a': 'x', 'b': 'v', 'e': 'v'}, 'c': {'c': 'v', 'a': 'x', 'b': 'v', 'e': 'v'}}",
      /*filter:*/     "{'$*': { '$*': 1, 'a': 0 }, 'a': 0, 'b' : { 'c': 0, 'e': 1}}",
      /*expected*/    "{'b': {'b': 'v', 'e': 'v'}, 'c': {'c': 'v', 'b': 'v', 'e': 'v'}}"
    },
    {
      /*description:*/"Test that $*=1 gets reconginzed deep in siblings. Even though in 'b' node only 'e' is being explicitly" +
      		          "selected,  'c' should also be returned, because parent has $*=1",
      /*data:*/       "{'a': { 'b': { 'c': { 'd': 'x', 'f': 'v'}, 'g': 'v', 'e': 'v'}}, 'h': 'x'}",
      /*filter:*/     "{'a': { '$*': 1, 'b': { 'c': { 'd': 0 }, 'e': 1}}}",
      /*expected*/    "{'a': { 'b': { 'c': { 'f': 'v'}, 'g': 'v', 'e': 'v'}}}"
    },
    {
      /*description:*/"Test that $*=1 gets reconginzed deep in siblings, but it does not leak outside the scope. In this example" +
      		          " $*=1 is defined in scope of 'a', so it should not leak outside it.",
      /*data:*/       "{'a': { 'b': { 'c': { 'd': 'x', 'f': 'v', 'h': 'v'}, 'g': 'v', 'e': 'x', 'f': 'v'}}, 'h': 'x', 'e': { 'f': { 'g': 'x', 'h': 1, 'a': 'x'}}}",
      /*filter:*/     "{'a': { '$*': { '$*': 1, 'e': 0 }, 'b': { 'c': { 'd': 0 , 'h': 1}}}, 'e': { 'f': { 'g': 0, 'h': 1 }}}",
      /*expected*/    "{'a': { 'b': { 'c': { 'f': 'v', 'h': 'v'}, 'g': 'v', 'f': 'v'}}, 'e': { 'f': { 'h': 1}}}"
    },
  };

  /**
   * Tests containing filtering arrays. Each test contains a description, data, on which filter
   * will be performed and expected result.
   * Data and filter are expressed as JSON strings.
   *
   * For clarity it is allowed to use ' instead of ". Every ' character will be
   * replaced by " before parsing. THIS IS ONLY RELATED TO THIS TEST CLASS
   * TO MAKE TESTS CASES MORE CLEAR, PEGASUS DOESN'T DO ANYTHING LIKE THAT.
   */
  public static final String[][] ARRAY_TESTS = new String[][] {
    {
      /*description:*/"Filter is empty. Array contains objects.",
      /*data:*/       "{'a': 10, 'b': [{'a': 'aaa'}, {'b': 'bbb'}]}",
      /*filter:*/     "{}",
      /*expected*/    "{'a': 10, 'b': [{'a': 'aaa'}, {'b': 'bbb'}]}"
    },
    {
      /*description:*/"Filter is empty. Array contains integers.",
      /*data:*/       "{'a': 10, 'b': [1, 2, 3]}",
      /*filter:*/     "{}",
      /*expected*/    "{'a': 10, 'b': [1, 2, 3]}"
    },
    {
      /*description:*/"Filter is empty. Array contains integers and objects.",
      /*data:*/       "{'a': 10, 'b': [1, 2, 3, {'a': 'aaa'}, {'b': 'bbb'}]}",
      /*filter:*/     "{}",
      /*expected*/    "{'a': 10, 'b': [1, 2, 3, {'a': 'aaa'}, {'b': 'bbb'}]}"
    },
    {
      /*description:*/"Filter contains simple range. Array contains objects.",
      /*data:*/       "{'a': 10, 'b': [{'a': 'aaa'}, {'b': 'bbb'}, {'c': 'ccc'}, {'d': 'ddd'}, {'e': 'eee'}]}",
      /*filter:*/     "{'b' : { '$start': 2, '$count': 2}}",
      /*expected*/    "{'b': [{'c': 'ccc'}, {'d': 'ddd'}]}"
    },
    {
      /*description:*/"Filter contains simple range. Array contains integers.",
      /*data:*/       "{'a': 10, 'b': [1, 2, 3, 4, 5, 6]}",
      /*filter:*/     "{'b' : { '$start': 2, '$count': 2}}",
      /*expected*/    "{'b': [3, 4]}"
    },
    {
      /*description:*/"Filter contains simple range. Array contains integers and objects.",
      /*data:*/       "{'a': 10, 'b': [1, 2, 3, {'a': 'aaa'}, {'b': 'bbb'}]}",
      /*filter:*/     "{'a': 1, 'b' : { '$start': 2, '$count': 2}}",
      /*expected*/    "{'a': 10, 'b': [3, {'a': 'aaa'}]}"
    },
    {
      /*description:*/"Filter contains simple range. Array contains integers and end of range is higher than array length.",
      /*data:*/       "{'a': 10, 'b': [1, 2, 3, 4, 5, 6]}",
      /*filter:*/     "{'b' : { '$start': 2, '$count': 100}}",
      /*expected*/    "{'b': [3, 4, 5, 6]}"
    },
    {
      /*description:*/"Filter contains simple range. Array contains integers and start of range is higher than array length.",
      /*data:*/       "{'a': 10, 'b': [1, 2, 3, 4, 5, 6]}",
      /*filter:*/     "{'b' : { '$start': 20, '$count': 2}}",
      /*expected*/    "{'b': []}"
    },
    {
      /*description:*/"Filter contains simple range. Array contains integers and with just start of the range specified.",
      /*data:*/       "{'a': 10, 'b': [1, 2, 3, 4, 5, 6]}",
      /*filter:*/     "{'b' : { '$start': 3}}",
      /*expected*/    "{'b': [4, 5, 6]}"
    },
    {
      /*description:*/"Filter contains simple range. Array contains integers and with just count of the range specified.",
      /*data:*/       "{'a': 10, 'b': [1, 2, 3, 4, 5, 6]}",
      /*filter:*/     "{'b' : { '$count': 3}}",
      /*expected*/    "{'b': [1, 2, 3]}"
    },
    {
      /*description:*/"Filter contains simple positive wildcard. Array contains objects.",
      /*data:*/       "{'a': 10, 'b': [{'a': 'aaa'}, {'b': 'bbb'}, {'c': 'ccc'}, {'d': 'ddd'}, {'e': 'eee'}]}",
      /*filter:*/     "{'b' : { '$*': 1}}",
      /*expected*/    "{'b': [{'a': 'aaa'}, {'b': 'bbb'}, {'c': 'ccc'}, {'d': 'ddd'}, {'e': 'eee'}]}"
    },
    {
        /*description:*/"Filter contains simple positive mask on array field. Array contains objects.",
        /*data:*/       "{'a': 10, 'b': [{'a': 'aaa'}, {'b': 'bbb'}, {'c': 'ccc'}, {'d': 'ddd'}, {'e': 'eee'}]}",
        /*filter:*/     "{'b' : 1}",
        /*expected*/    "{'b': [{'a': 'aaa'}, {'b': 'bbb'}, {'c': 'ccc'}, {'d': 'ddd'}, {'e': 'eee'}]}"
    },
    {
      /*description:*/"Filter contains only negative wildcard.",
      /*data:*/       "{'a': 10, 'b': [1, 2, 3, 4, 5, 6]}",
      /*filter:*/     "{'b' : { '$*': 0}}",
      /*expected*/    "{'a': 10, 'b': []}"
    },
    {
      /*description:*/"Filter contains negative wildcard but there is a positive filter on the same level. " +
      		"In that case ",
      /*data:*/       "{'a': 10, 'b': [1, 2, 3, 4, 5, 6]}",
      /*filter:*/     "{'b' : { '$*': 0}, 'c': 1}",
      /*expected*/    "{}"
    },
    {
      /*description:*/"Filter contains complex wildcard, which selects only fields with name 'c'. Array contains objects.",
      /*data:*/       "{'a': 10, 'b': [{'a': 'aaa'}, {'b': 'bbb'}, {'c': 'ccc'}, {'d': 'ddd'}, {'e': 'eee'}]}",
      /*filter:*/     "{'b' : { '$*': { 'c': 1} }}",
      /*expected*/    "{'b': [{}, {}, {'c': 'ccc'}, {}, {}]}"
    },
    {
      /*description:*/"Filter contains complex wildcard, which removes only fields with name 'c'. Array contains objects.",
      /*data:*/       "{'a': 10, 'b': [{'a': 'aaa'}, {'b': 'bbb'}, {'c': 'ccc'}, {'d': 'ddd'}, {'e': 'eee'}]}",
      /*filter:*/     "{'b' : { '$*': { 'c': 0} }}",
      /*expected*/    "{'a': 10, 'b': [{'a': 'aaa'}, {'b': 'bbb'}, {}, {'d': 'ddd'}, {'e': 'eee'}]}"
    },
    {
      /*description:*/"Filter contains complex wildcard, which removes only fields with name 'c'. Array contains objects.",
      /*data:*/       "{'a': 10, 'b': [{'a': 'aaa'}, {'b': 'bbb'}, {'c': 'ccc'}, {'d': 'ddd'}, {'e': 'eee'}]}",
      /*filter:*/     "{'b' : { '$*': { 'c': 0} }, 'c': 1}",
      /*expected*/    "{}"
    },
    {
      /*description:*/"Filter contains complex wildcard, which removes only fields with name 'c' and selects all other fields. " +
      		"Array contains objects.",
      /*data:*/       "{'a': 10, 'b': [{'a': 'aaa'}, {'b': 'bbb'}, {'c': 'ccc'}, {'d': 'ddd'}, {'e': 'eee'}]}",
      /*filter:*/     "{'b' : { '$*': { 'c': 0, '$*': 1} }}",
      /*expected*/    "{'b': [{'a': 'aaa'}, {'b': 'bbb'}, {}, {'d': 'ddd'}, {'e': 'eee'}]}"
    },
    {
      /*description:*/"Filter contains complex wildcard, which removes only fields with name 'c' and selects all other fields. " +
            "Along that there is a range filter. Array contains objects.",
      /*data:*/       "{'a': 10, 'b': [{'a': 'aaa'}, {'b': 'bbb'}, {'c': 'ccc'}, {'d': 'ddd'}, {'e': 'eee'}]}",
      /*filter:*/     "{'b' : { '$*': { 'c': 0, '$*': 1}, '$start': 2, '$count': 2 }}",
      /*expected*/    "{'b': [{}, {'d': 'ddd'}]}"
    },
    {
      /*description:*/"Filter contains complex wildcard, which removes only fields with name 'c' and selects all other fields. " +
            "Along that there is a range filter. Array contains objects. Since range selection in array is considered to be " +
            "a positive mask, the 'a' field in main object should be filtered out.",
      /*data:*/       "{'a': 10, 'b': [{'a': 'aaa'}, {'b': 'bbb'}, {'c': 'ccc'}, {'d': 'ddd'}, {'e': 'eee'}]}",
      /*filter:*/     "{'b' : { '$*': { 'c': 0}, '$start': 2, '$count': 2 }}",
      /*expected*/    "{'b': [{}, {'d': 'ddd'}]}"
    },
  };

  /**
   * Set of tests. Each test contains a description, data, on which filter
   * will be performed and expected result.
   * Data and filter are expressed as JSON strings.
   *
   * For clarity it is allowed to use ' instead of ". Every ' character will be
   * replaced by " before parsing. THIS IS ONLY RELATED TO THIS TEST CLASS
   * TO MAKE TESTS CASES MORE CLEAR, PEGASUS DOESN'T DO ANYTHING LIKE THAT.
   */
  public static final String[][] ESCAPING_TESTS = new String[][] {
    {
      /*description:*/"Filter is empty. Data object should not be modified.",
      /*data:*/       "{'$$': 10, '*': {'$$*': 'aaa'}}",
      /*filter:*/     "{}",
      /*expected*/    "{'$$': 10, '*': {'$$*': 'aaa'}}"
    },
    {
      /*description:*/"Test if simple filter gets unescaped.",
      /*data:*/       "{'$*': 10, '$start': {'$count': 'aaa'}}",
      /*filter:*/     "{'$$*': 1, '$$start': {'$$count': 1} }",
      /*expected*/    "{'$*': 10, '$start': {'$count': 'aaa'}}"
    },
    {
      /*description:*/"Test if complex filter gets unescaped.",
      /*data:*/       "{'$*': 10, 'b': {'c1': { '$*': 'aaa1', '$': 'bbb'}, '*': { '$*': 'aaa2', '$start': 'ddd'} }}",
      /*filter:*/     "{'b': { '$*': { '$$*': 0 }} }",
      /*expected*/    "{'$*': 10, 'b': {'c1': { '$': 'bbb'}, '*': { '$start': 'ddd'}}}"
    },
    {
      /*description:*/"Test if filter gets unescaped for arrays.",
      /*data:*/       "{'$*': 10, '*': [1, 2, 3, {'*': 'aaa'}, {'$start': 'bbb'}]}",
      /*filter:*/     "{'$$*': 1, '*' : { '$start': 2, '$count': 2}}",
      /*expected*/    "{'$*': 10, '*': [3, {'*': 'aaa'}]}"
    },
    {
      /*description:*/"Filter contains complex wildcard, which removes only fields with name 'c' and selects all other fields. " +
            "Along that there is a range filter. Array contains objects.",
      /*data:*/       "{'$*': 10, '$': [{'*': 'aaa'}, {'b': 'bbb'}, {'$*': 'ccc'}, {'$start': 'ddd'}, {'e': 'eee'}]}",
      /*filter:*/     "{'$$' : { '$*': { '$$*': 0, '$*': 1}, '$start': 2, '$count': 2 }}",
      /*expected*/    "{'$': [{}, {'$start': 'ddd'}]}"
    },
  };


  protected void genericFilterTest(DataMap data, DataMap filter,
                                        DataMap expected, String description) throws DataProcessingException {
    String dataBefore = data.toString();
    DataComplexProcessor processor = new DataComplexProcessor(new Filter(), filter, data);
    processor.run(false);
    assertEquals(data, expected, "The following test failed: \n" + description  +
                 "\nData: " + dataBefore + "\nFilter: " + filter +
                 "\nExpected: " + expected + "\nActual result: " + data);
  }

  @Test
  public void testFilterOnData() throws JsonParseException,
      IOException,
      DataProcessingException
  {
    for (String[] testCase : TESTS) {
      genericFilterTest(dataMapFromString(testCase[1].replace('\'', '"')),
                               dataMapFromString(testCase[2].replace('\'', '"')),
                               dataMapFromString(testCase[3].replace('\'', '"')),
                               testCase[0]);
    }
  }

  @Test
  public void testFilterOnDataContainingArrays() throws JsonParseException,
      IOException,
      DataProcessingException
  {
    for (String[] testCase : ARRAY_TESTS) {
      genericFilterTest(dataMapFromString(testCase[1].replace('\'','"')),
                        dataMapFromString(testCase[2].replace('\'','"')),
                        dataMapFromString(testCase[3].replace('\'','"')),
                               testCase[0]);
    }
  }

  @Test
  public void testFilterOnDataWithEscaping() throws JsonParseException,
      IOException,
      DataProcessingException
  {
    for (String[] testCase : ESCAPING_TESTS) {
      genericFilterTest(dataMapFromString(testCase[1].replace('\'','"')),
                        dataMapFromString(testCase[2].replace('\'','"')),
                        dataMapFromString(testCase[3].replace('\'','"')),
                               testCase[0]);
    }
  }
}

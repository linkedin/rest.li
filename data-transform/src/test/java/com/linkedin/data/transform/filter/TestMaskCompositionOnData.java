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

public class TestMaskCompositionOnData
{

  /**
   * Set of tests. Each test contains a description, data, and expected result.
   * Data is expressed as JSON strings.
   *
   * For clarity it is allowed to use ' instead of ". Every ' character will be
   * replaced by " before parsing. THIS IS ONLY RELATED TO THIS TEST CLASS
   * TO MAKE TESTS CASES MORE CLEAR, PEGASUS DOESN'T DO ANYTHING LIKE THAT.
   */
  public static final String[][] SIMPLE_TESTS = new String[][] {
    {
      /*description:*/"Compose with empty mask.",
      /*data1:*/      "{'a': 1, 'b': 0}",
      /*data2:*/      "{}",
      /*expected*/    "{'a': 1, 'b': 0}"
    },
    {
      /*description:*/"Compose simple positive with negative mask. Negative mask should " +
      		"overwrite the positive mask.",
      /*data1:*/      "{'a': 1}",
      /*data2:*/      "{'a': 0}",
      /*expected*/    "{'a': 0}"
    },
    {
      /*description:*/"Compose simple positive with positive mask.",
      /*data1:*/      "{'a': 1}",
      /*data2:*/      "{'b': 1}",
      /*expected*/    "{'a': 1, 'b': 1}"
    },
    {
      /*description:*/"Compose simple negative with negative mask.",
      /*data1:*/      "{'a': 0}",
      /*data2:*/      "{'b': 0}",
      /*expected*/    "{'a': 0, 'b': 0}"
    },
    {
      /*description:*/"Composition of 0 with complex mask containing only negative mask.",
      /*data1:*/      "{'a': 0}",
      /*data2:*/      "{'a': { 'a': 0}}",
      /*expected*/    "{'a': 0}"
    },
    {
      /*description:*/"Composition of 1 with complex mask containing only negative mask.",
      /*data1:*/      "{'a': 1}",
      /*data2:*/      "{'a': { 'a': 0}}",
      /*expected*/    "{'a': { '$*': 1, 'a': 0}}"
    },
    {
      /*description:*/"Composition of 0 with complex mask containing only positive mask.",
      /*data1:*/      "{'a': 0}",
      /*data2:*/      "{'a': { 'a': 1}}",
      /*expected*/    "{'a': 0}"
    },
    {
      /*description:*/"Composition of 1 with complex mask containing only positive mask.",
      /*data1:*/      "{'a': 1}",
      /*data2:*/      "{'a': { 'a': 1}}",
      /*expected*/    "{'a': 1}"
    },
    {
      /*description:*/"Composition of 1 with complex mask containing only positive mask few levels deep.",
      /*data1:*/      "{'a': 1}",
      /*data2:*/      "{'a': { 'a': 1, 'b': { 'c': { 'd': 1, 'e': 1}} }}",
      /*expected*/    "{'a': 1}"
    },
    {
      /*description:*/"Composition of 1 with complex mask containing only array range mask few levels deep.",
      /*data1:*/      "{'a': 1}",
      /*data2:*/      "{'a': { 'a': 1, 'b': { 'c': { 'd': {'$start': 10, '$count': 7}}} }}",
      /*expected*/    "{'a': 1}"
    },
  };


  /**
   * Set of tests. Each test contains a description, data, and expected result.
   * Data is expressed as JSON strings.
   *
   * For clarity it is allowed to use ' instead of ". Every ' character will be
   * replaced by " before parsing. THIS IS ONLY RELATED TO THIS TEST CLASS
   * TO MAKE TESTS CASES MORE CLEAR, PEGASUS DOESN'T DO ANYTHING LIKE THAT.
   */
  public static final String[][] WILDCARD_TESTS = new String[][] {
    {
      /*description:*/"Composition of mask containing simple positive wildcard.",
      /*data1:*/      "{'$*': 1, 'a': 0}",
      /*data2:*/      "{'a': 1, 'b': 0, 'c': 0}",
      /*expected*/    "{'$*': 1, 'a': 0, 'b': 0, 'c': 0}"
    },
    {
      /*description:*/"Composition of mask containing simple negative wildcard.",
      /*data1:*/      "{'$*': 0, 'a': 0}",
      /*data2:*/      "{'a': 1, 'b': 0, 'c': 0}",
      /*expected*/    "{'$*': 0}"
    },
    {
      /*description:*/"Composition of 1 with complex mask.",
      /*data1:*/      "{'a': 1}",
      /*data2:*/      "{'a': { 'a': 0}}",
      /*expected*/    "{'a': { '$*': 1, 'a': 0}}"
    },
    {
      /*description:*/"Composition of 1 with complex mask already containing wildcard with value 1.",
      /*data1:*/      "{'a': 1}",
      /*data2:*/      "{'a': { '$*': 1, 'a': 0}}",
      /*expected*/    "{'a': { '$*': 1, 'a': 0}}"
    },
    {
      /*description:*/"Composition of 1 with complex mask already containing complex wildcard.",
      /*data1:*/      "{'a': 1}",
      /*data2:*/      "{'a': { '$*': { 'b': 0}, 'a': 0}}",
      /*expected*/    "{'a': { '$*': { '$*': 1, 'b': 0}, 'a': 0}}"
    },
    {
      /*description:*/"Composition of $*=1 with complex mask.",
      /*data1:*/      "{'a': { '$*': 1, 'b': 0}}",
      /*data2:*/      "{'a': { 'a': 0}}",
      /*expected*/    "{'a': { '$*': 1, 'a': 0, 'b': 0}}"
    },
    {
      /*description:*/"Composition of $*=1 with complex mask already containing complex wildcard.",
      /*data1:*/      "{'a': { '$*': 1, 'b': 0}}",
      /*data2:*/      "{'a': { '$*': { 'c': 0}, 'a': 0}}",
      /*expected*/    "{'a': { '$*': { '$*': 1, 'c': 0}, 'a': 0, 'b': 0}}"
    },
    {
      /*description:*/"Composition complex wildcard with positive wildcard.",
      /*data1:*/      "{'$*': { 'a': 0 }, 'a': 0, 'b' : { '$*': { 'c': 0 }}}",
      /*data2:*/      "{'$*': 1}",
      /*expected*/    "{'$*': { '$*': 1, 'a': 0 }, 'a': 0, 'b' : { '$*': { 'c': 0 }}}"
    },
    {
      /*description:*/"Composition of 1 with complex mask containing only negative mask.",
      /*data1:*/      "{'b': 1}",
      /*data2:*/      "{'b': { '$*': { 'a': 0 }} }",
      /*expected*/    "{'b': { '$*': { '$*': 1, 'a': 0 }} }"
    },
    {
      /*description:*/"Composition of mask containing deeply nested $*=1 with a negative mask.",
      /*data1:*/      "{'b': { '$*': { '$*': 1, 'a': 0 }} }",
      /*data2:*/      "{'b': { '$*': { 'b': 0 }} }",
      /*expected*/    "{'b': { '$*': { '$*': 1, 'a': 0, 'b': 0 }} }"
    },
    {
      /*description:*/"Composition of 1 with two level deep nested negative mask. 1 should be propagated further " +
      		"down using $*:1 notation",
      /*data1:*/      "{'a': 1}",
      /*data2:*/      "{'a': { 'b': { 'c': { 'd': 0 }}}}",
      /*expected*/    "{'a': { '$*': 1, 'b': { 'c': { 'd': 0 }}}}"
    },
    {
      /*description:*/"Test propagation of 1, when 1 is defined by $*",
      /*data1:*/      "{'a': { '$*': 1}}",
      /*data2:*/      "{'a': { 'b': { 'c': { 'd': 0 }}}, 'e': { 'f': { 'g': 0 }}}",
      /*expected*/    "{'a': { '$*': 1, 'b': { 'c': { 'd': 0 }}}, 'e': { 'f': { 'g': 0 }}}"
    },
    {
      /*description:*/"Test of propagation of 1, when 1 is defined as deeply nested $*=1",
      /*data1:*/      "{'a': { '$*': { '$*': 1, 'e': 0 } }}",
      /*data2:*/      "{'a': { 'b': { 'c': { 'd': 0 }}}, 'e': { 'f': { 'g': 0 }}}",
      /*expected*/    "{'a': { '$*': { '$*': 1, 'e': 0 }, 'b': { 'c': { 'd': 0 }}}, 'e': { 'f': { 'g': 0 }}}"
    },
    {
      /*description:*/"Test pushing down $*=1 few levels deep",
      /*data1:*/      "{'a': 1 }",
      /*data2:*/      "{'a': { '$*': { '$*': { 'k': 0}}}}",
      /*expected*/    "{'a': { '$*': { '$*': { '$*': 1, 'k': 0}}}}"
    },
    {
      /*description:*/"Test detecting $*=1 from few levels deep",
      /*data1:*/      "{ '$*': { '$*': { '$*': 1, 'k': 0}}}",
      /*data2:*/      "{'c': { '$*': { '$*': { 'w': 0}}}}",
      /*expected*/    "{'$*': { '$*': { '$*': 1, 'k': 0}}, 'c': { '$*': { '$*': { 'w': 0}}}}"
    },
  };

  /**
   * Set of tests. Each test contains a description, data, and expected result.
   * Data is expressed as JSON strings.
   *
   * For clarity it is allowed to use ' instead of ". Every ' character will be
   * replaced by " before parsing. THIS IS ONLY RELATED TO THIS TEST CLASS
   * TO MAKE TESTS CASES MORE CLEAR, PEGASUS DOESN'T DO ANYTHING LIKE THAT.
   */
  public static final String[][] ARRAY_TESTS = new String[][] {
    {
      /*description:*/"Compose same array ranges.",
      /*data1:*/      "{'a': { '$start': 2, '$count': 3 }}",
      /*data2:*/      "{'a': { '$start': 2, '$count': 3 }}",
      /*expected*/    "{'a': { '$start': 2, '$count': 3 }}"
    },
    {
      /*description:*/"Compose array range with empty mask.",
      /*data1:*/      "{'a': { '$start': 2, '$count': 3 }}",
      /*data2:*/      "{}",
      /*expected*/    "{'a': { '$start': 2, '$count': 3 }}"
    },
    {
      /*description:*/"Compose array range with 1. a is an array in data object. Range should be removed.",
      /*data1:*/      "{'a': { '$start': 2, '$count': 3 }}",
      /*data2:*/      "{'a': 1}",
      /*expected*/    "{'a': 1}"
    },
    {
      /*description:*/"Compose array range with '$*': 1. a is an array in data object. Range should be merged with wildcard.",
      /*data1:*/      "{'a': { '$start': 2, '$count': 3 }}",
      /*data2:*/      "{'a': { '$*': 1 }}",
      /*expected*/    "{'a': { '$*': 1, '$start': 2, '$count': 3 }}"
    },
    {
      /*description:*/"Compose array range with complex wildcard merged with 1. a is an array in data object. Range should be merged with wildcard.",
      /*data1:*/      "{'a': { '$start': 2, '$count': 3 }}",
      /*data2:*/      "{'a': { '$*': { '$*': 1, 'b': 0} }}",
      /*expected*/    "{'a': { '$*': { '$*': 1, 'b': 0}, '$start': 2, '$count': 3 }}"
    },
    {
      /*description:*/"Compose disjoint array ranges. The result is smallest range containing both ranges.",
      /*data1:*/      "{'a': { '$start': 2, '$count': 3 }}",
      /*data2:*/      "{'a': { '$start': 12, '$count': 3 }}",
      /*expected*/    "{'a': { '$start': 2, '$count': 13 }}"
    },
    {
      /*description:*/"Compose disjoint array ranges with a default for start. The result is smallest range containing both ranges.",
      /*data1:*/      "{'a': { '$count': 10 }}",
      /*data2:*/      "{'a': { '$start': 20, '$count': 50 }}",
      /*expected*/    "{'a': { '$count': 70 }}"
    },
    {
      /*description:*/"Compose disjoint array ranges with a default for start and count. The result is smallest range containing both ranges.",
      /*data1:*/      "{'a': { '$count': 10 }}",
      /*data2:*/      "{'a': { '$start': 20 }}",
      /*expected*/    "{'a': { '$*': 1}}"
    },
    {
      /*description:*/"Compose array ranges when one range contain other. The result is larger range.",
      /*data1:*/      "{'a': { '$start': 2, '$count': 3 }}",
      /*data2:*/      "{'a': { '$start': 1, '$count': 8 }}",
      /*expected*/    "{'a': { '$start': 1, '$count': 8 }}"
    },
    {
      /*description:*/"Compose array ranges overlaps with the other. The result is smallest range containing both ranges.",
      /*data1:*/      "{'a': { '$start': 2, '$count': 5 }}",
      /*data2:*/      "{'a': { '$start': 4, '$count': 5 }}",
      /*expected*/    "{'a': { '$start': 2, '$count': 7 }}"
    },
    {
      /*description:*/"Compose array ranges with one of them having the default values specified explicitly.",
      /*data1:*/      "{'a': { '$start': 0, '$count': 2147483647 }}",
      /*data2:*/      "{'a': { '$start': 20, '$count': 50 }}",
      /*expected*/    "{'a': {'$*': 1}}"
    },
    {
      /*description:*/"Compose array ranges with one of them having the count default value specified explicitly.",
      /*data1:*/      "{'a': { '$count': 2147483647 }}",
      /*data2:*/      "{'a': { '$start': 20, '$count': 50 }}",
      /*expected*/    "{'a': {'$*': 1}}"
    },
    {
      /*description:*/"Compose array ranges with one of them having the start default value specified explicitly.",
      /*data1:*/      "{'a': { '$start': 0 }}",
      /*data2:*/      "{'a': { '$start': 20, '$count': 50 }}",
      /*expected*/    "{'a': {'$*': 1}}"
    },
    {
      /*description:*/"Compose array ranges with one of them having range values that overflows the integer range.",
      /*data1:*/      "{'a': { '$*': 1 }}",
      /*data2:*/      "{'a': { '$start': 2147483640, '$count': 200 }}", // Adding the count to start will cause an overflow
      /*expected*/    "{'a': { '$*': 1, '$start': 2147483640, '$count': 7 }}"
    },
    {
      /*description:*/"Compose array ranges with both of them having range values that overflows the integer range.",
      /*data1:*/      "{'a': { '$start': 10, '$count': 2147483640 }}", // Adding the count to start will cause an overflow
      /*data2:*/      "{'a': { '$start': 2147483640, '$count': 10 }}", // Adding the count to start will cause an overflow
      /*expected*/    "{'a': { '$start': 10, '$count': 2147483637 }}"
    }
  };

  /**
   * Set of tests. Each test contains a description, data, and expected result.
   * Data is expressed as JSON strings.
   *
   * For clarity it is allowed to use ' instead of ". Every ' character will be
   * replaced by " before parsing. THIS IS ONLY RELATED TO THIS TEST CLASS
   * TO MAKE TESTS CASES MORE CLEAR, PEGASUS DOESN'T DO ANYTHING LIKE THAT.
   */
  public static final String[][] ESCAPING_TESTS = new String[][] {
    {
      /*description:*/"Compose with empty mask.",
      /*data1:*/      "{'$$a': 1, '*': 0}",
      /*data2:*/      "{}",
      /*expected*/    "{'$$a': 1, '*': 0}"
    },
    {
      /*description:*/"Compose simple positive with negative mask. Negative mask should " +
            "overwrite the positive mask.",
      /*data1:*/      "{'$$': 1}",
      /*data2:*/      "{'$$': 0}",
      /*expected*/    "{'$$': 0}"
    },
    {
      /*description:*/"Compose simple positive with positive mask.",
      /*data1:*/      "{'$$*': 1}",
      /*data2:*/      "{'*': 1}",
      /*expected*/    "{'$$*': 1, '*': 1}"
    },
    {
      /*description:*/"Compose simple negative with negative mask.",
      /*data1:*/      "{'$$a': 0}",
      /*data2:*/      "{'b': 0}",
      /*expected*/    "{'$$a': 0, 'b': 0}"
    },
    {
      /*description:*/"Composition of 0 with complex mask containing only negative mask.",
      /*data1:*/      "{'$$*a': 0}",
      /*data2:*/      "{'$$*a': { 'a': 0}}",
      /*expected*/    "{'$$*a': 0}"
    },
    {
      /*description:*/"Composition of 1 with complex mask containing only negative mask.",
      /*data1:*/      "{'$$*': 1}",
      /*data2:*/      "{'$$*': { 'a': 0}}",
      /*expected*/    "{'$$*': { '$*': 1, 'a': 0}}"
    },
    {
      /*description:*/"Composition of 0 with complex mask containing only positive mask.",
      /*data1:*/      "{'*': 0}",
      /*data2:*/      "{'*': { '*': 1}}",
      /*expected*/    "{'*': 0}"
    },
    {
      /*description:*/"Composition of 1 with complex mask containing only positive mask.",
      /*data1:*/      "{'*': 1}",
      /*data2:*/      "{'*': { '*': 1}}",
      /*expected*/    "{'*': 1}"
    },
    {
      /*description:*/"Composition complex wildcard with positive wildcard.",
      /*data1:*/      "{'$*': { '$$*': 0 }, '*': 0, '$$' : { '$*': { '$$**': 0 }}}",
      /*data2:*/      "{'$*': 1}",
      /*expected*/    "{'$*': { '$*': 1, '$$*': 0 }, '*': 0, '$$' : { '$*': { '$$**': 0 }}}"
    },

  };

  private void genericCompositionTest(DataMap data1, DataMap data2,
                                        DataMap expected, String description) throws DataProcessingException, CloneNotSupportedException {
    String dataBefore = data1.toString();
    String data2Clone = data2.toString();
    DataComplexProcessor processor = new DataComplexProcessor(new MaskComposition(), data2, data1);
    processor.run(false);
    assertEquals(data1, expected, "The following test failed: \n" + description  +
                 "\nData1: " + dataBefore + "\nData2: " + data2 +
                 "\nExpected: " + expected + "\nActual result: " + data1);
    assertEquals(data2.toString(), data2Clone, "Operation data should not be modified");
  }

  public void executeTests(String[][] testSuite) throws JsonParseException,
      IOException,
      DataProcessingException, CloneNotSupportedException
  {
    for (String[] testCase : testSuite) {
      genericCompositionTest(dataMapFromString(testCase[1].replace('\'','"')),
                             dataMapFromString(testCase[2].replace('\'','"')),
                             dataMapFromString(testCase[3].replace('\'','"')),
                               testCase[0]);
      //order of data i composition should not matter
      genericCompositionTest(dataMapFromString(testCase[2].replace('\'','"')),
                             dataMapFromString(testCase[1].replace('\'','"')),
                             dataMapFromString(testCase[3].replace('\'','"')),
                             testCase[0]);
    }
  }


  @Test
  public void simpleTests() throws JsonParseException,
      IOException,
      DataProcessingException, CloneNotSupportedException
  {
    executeTests(SIMPLE_TESTS);
  }

  @Test
  public void wildcardsTests() throws JsonParseException,
      IOException,
      DataProcessingException, CloneNotSupportedException
  {
    executeTests(WILDCARD_TESTS);
  }

  @Test
  public void arrayTests() throws JsonParseException,
      IOException,
      DataProcessingException, CloneNotSupportedException
  {
    executeTests(ARRAY_TESTS);
  }

  @Test
  public void escapingTests() throws JsonParseException,
      IOException,
      DataProcessingException, CloneNotSupportedException
  {
    executeTests(ESCAPING_TESTS);
  }

}

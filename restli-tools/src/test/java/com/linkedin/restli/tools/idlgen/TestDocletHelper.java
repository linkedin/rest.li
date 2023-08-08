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

package com.linkedin.restli.tools.idlgen;


import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Tests to ensure that util methods in {@link DocletHelper} work correctly.
 *
 * @author Yan Zhou
 */
public class TestDocletHelper
{
  @Test
  public void testGetCanonicalName() {
    // input is null, so no op
    String input = null;
    String actualOutput = DocletHelper.getCanonicalName(input);
    String expectedOutput = null;
    Assert.assertTrue(actualOutput == expectedOutput);

    // input does not match the pattern, so no op
    input = "asdfljk sdf  \n * ";
    actualOutput = DocletHelper.getCanonicalName(input);
    expectedOutput = input;
    Assert.assertEquals(actualOutput, expectedOutput);

    // input matches the pattern i.e. <*>, so relevant substring is removed
    input = "))?? <Set>  )* * ";
    actualOutput = DocletHelper.getCanonicalName(input);
    expectedOutput = "))??   )* * ";
    Assert.assertEquals(actualOutput, expectedOutput);
  }

  @Test
  public void testProcessDocCommentStr() {
    // input is null, so no op
    String input = null;
    String actualOutput = DocletHelper.processDocCommentStr(input);
    String expectedOutput = null;
    Assert.assertTrue(actualOutput == expectedOutput);

    // input does not match the pattern, so no op
    input = "asdf  \n () , ? * ";
    actualOutput = DocletHelper.processDocCommentStr(input);
    expectedOutput = input;
    Assert.assertEquals(actualOutput, expectedOutput);

    // input matches pattern, i.e. ,{@xxx  }, so redundant commas are removed
    input = " ,{@xxx  },   ";
    actualOutput = DocletHelper.processDocCommentStr(input);
    expectedOutput = " {@xxx  }   ";
    Assert.assertEquals(actualOutput, expectedOutput);

    // input matches the pattern, i.e. ,{@xxx  } so redundant commas are removed
    input = " ,{@xxx  }   ";
    actualOutput = DocletHelper.processDocCommentStr(input);
    expectedOutput = " {@xxx  }   ";
    Assert.assertEquals(actualOutput, expectedOutput);

    // input matches, i.e. ,>, so redundant commas are removed
    input = " ?  ,>, ,";
    actualOutput = DocletHelper.processDocCommentStr(input);
    expectedOutput = " ?  > ,";
    Assert.assertEquals(actualOutput, expectedOutput);

    // input matches the pattern, i.e. ,<, so redundant commas are removed
    input = " ?  ,<, ,";
    actualOutput = DocletHelper.processDocCommentStr(input);
    expectedOutput = " ?  < ,";
    Assert.assertEquals(actualOutput, expectedOutput);

    // input matches the pattern, i.e. ,< so redundant commas are removed
    input = " ?  ,< ,";
    actualOutput = DocletHelper.processDocCommentStr(input);
    expectedOutput = " ?  < ,";
    Assert.assertEquals(actualOutput, expectedOutput);

    // input matches the pattern, i.e. ,<,>, so redundant commas are removed
    input = " ,<,>,  ? *(,";
    actualOutput = DocletHelper.processDocCommentStr(input);
    expectedOutput = " <>  ? *(,";
    Assert.assertEquals(actualOutput, expectedOutput);

    // input matches the pattern, i.e. ,<sdfdf>, so redundant commas are removed
    input = " ,<sdfdf>,,?";
    actualOutput = DocletHelper.processDocCommentStr(input);
    expectedOutput = " <sdfdf>,?";
    Assert.assertEquals(actualOutput, expectedOutput);
  }
}

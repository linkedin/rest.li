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

package com.linkedin.restli.internal.common;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestAsciiHexEncoding
{
  public static final AsciiHexEncoding TEST_ENCODING_1 = new AsciiHexEncoding('~', new char[] {'.','[', ']', '\017', '\020', '\000', '\177'});

  @Test
  public void testOutOfRangeReservedChar()
  {
    try
    {
      new AsciiHexEncoding('~', new char[] { '\200' });
      Assert.fail("Exception should be thrown if reserved char out of ASCII range is used");
    }
    catch (IllegalArgumentException e)
    {
    }

    try
    {
      new AsciiHexEncoding('\200', new char[] {});
      Assert.fail("Exception should be thrown if encoding char out of ASCII range is used");
    }
    catch (IllegalArgumentException e)
    {
    }
  }

  @Test
  public void testSingleCharEncode()
  {
    // test chars we care about plus the corner cases
    Assert.assertEquals(TEST_ENCODING_1.encode("."), "~2E");
    Assert.assertEquals(TEST_ENCODING_1.encode("["), "~5B");
    Assert.assertEquals(TEST_ENCODING_1.encode("]"), "~5D");
    Assert.assertEquals(TEST_ENCODING_1.encode("~"), "~7E");
    Assert.assertEquals(TEST_ENCODING_1.encode("\020"), "~10"); // first char that does not require 0 padding
    Assert.assertEquals(TEST_ENCODING_1.encode("\017"), "~0F"); // last char that requires 0 padding
    Assert.assertEquals(TEST_ENCODING_1.encode("\000"), "~00"); // first char
    Assert.assertEquals(TEST_ENCODING_1.encode("\177"), "~7F"); // last char
  }

  @Test
  public void testSingleCharDecode() throws Exception
  {
    Assert.assertEquals(TEST_ENCODING_1.decode("~2E"), ".");
    Assert.assertEquals(TEST_ENCODING_1.decode("~5B"), "[");
    Assert.assertEquals(TEST_ENCODING_1.decode("~5D"), "]");
    Assert.assertEquals(TEST_ENCODING_1.decode("~7E"), "~");
    Assert.assertEquals(TEST_ENCODING_1.decode("~10"), "\020"); // first char that does not require 0 padding
    Assert.assertEquals(TEST_ENCODING_1.decode("~0F"), "\017"); // last char that requires 0 padding
  }

  @Test
  public void testMultiCharEncode()
  {
    Assert.assertEquals(TEST_ENCODING_1.encode("a.b.c[d]~"), "a~2Eb~2Ec~5Bd~5D~7E");
  }

  @Test
  public void testMultiCharRoundTrip() throws Exception
  {
    assertRoundTrip("a.b.c[d]~");
    assertRoundTrip("");
    assertRoundTrip(null);
  }

  private void assertRoundTrip(String str) throws Exception
  {
    Assert.assertEquals(TEST_ENCODING_1.decode(TEST_ENCODING_1.encode(str)), str);
  }
}

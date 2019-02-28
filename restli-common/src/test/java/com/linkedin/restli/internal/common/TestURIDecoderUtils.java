/*
   Copyright (c) 2018 LinkedIn Corp.

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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests for {@link URIDecoderUtils}.
 *
 * @author Evan Williams
 */
public class TestURIDecoderUtils
{
  @DataProvider(name = "validEncodedText")
  private Object[][] provideValidEncodedText()
  {
    return new Object[][]
        {
            { "hello", "hello" },
            { "%25", "%" },
            { "I%20have%20spaces.", "I have spaces." },
            { "consecutive%20%20%20%20bytes%3D%3Dokay", "consecutive    bytes==okay" },
            { "%28beginning%26end%29", "(beginning&end)" },
            { "konnichiwa%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF", "konnichiwaこんにちは" },
            { "smiley%E2%98%BA", "smiley☺" },
            { "surrogatePairs%F0%9F%98%9B", "surrogatePairs\uD83D\uDE1B" }
        };
  }

  @Test(dataProvider = "validEncodedText")
  public void testDecodeValidStrings(String encoded, String expected)
  {
    String actual = URIDecoderUtils.decode(encoded);

    Assert.assertEquals(actual, expected, "Encoded string was incorrectly decoded.");
  }

  @DataProvider(name = "invalidEncodedText")
  private Object[][] provideInvalidEncodedText()
  {
    return new Object[][]
        {
            { "%",          "Malformed percent-encoded octet at index 0" },
            { "%A",         "Malformed percent-encoded octet at index 0" },
            { "% ",         "Malformed percent-encoded octet at index 0" },
            { "Hello%1",    "Malformed percent-encoded octet at index 5" },
            { "Hello%20%2", "Malformed percent-encoded octet at index 8" },
            { "%25%20%2",   "Malformed percent-encoded octet at index 6" },
            { "%!=",        "Malformed percent-encoded octet at index 1, invalid hexadecimal digit '!'" },
            { "%25%F^ ",    "Malformed percent-encoded octet at index 5, invalid hexadecimal digit '^'" }
        };
  }

  @Test(dataProvider = "invalidEncodedText")
  public void testDecodeInvalidStrings(String encoded, String expectedErrorMessage)
  {
    IllegalArgumentException exception = null;

    try
    {
      URIDecoderUtils.decode(encoded);
    }
    catch (IllegalArgumentException e)
    {
      exception = e;
    }

    Assert.assertNotNull(exception, "Expected exception when decoding string \"" + encoded + "\".");
    Assert.assertEquals(exception.getMessage(), expectedErrorMessage, "Unexpected error message during decoding.");
  }

  @DataProvider(name = "validConsecutiveOctetData")
  private Object[][] provideValidConsecutiveOctetData()
  {
    return new Object[][]
        {
            { "%20", 0, " ", 3 },
            { "%20_%3D", 0, " ", 3 },
            { "%20_%3D", 4, "=", 3 },
            { "...%20...", 3, " ", 3 },
            { "%26%3D%25", 0, "&=%", 9 },
            { "..._%26%3D%25_...", 4, "&=%", 9 },
            { "..._%26%3D%25_..._%28%29_...", 4, "&=%", 9 }
        };
  }

  @Test(dataProvider = "validConsecutiveOctetData")
  public void testDecodeValidConsecutiveOctets(String encoded, int startIndex, String expected, int expectedCharsConsumed)
  {
    StringBuilder result = new StringBuilder();
    int numCharsConsumed = URIDecoderUtils.decodeConsecutiveOctets(result, encoded, startIndex);

    Assert.assertEquals(result.toString(), expected);
    Assert.assertEquals(numCharsConsumed, expectedCharsConsumed);
  }

  @DataProvider(name = "invalidConsecutiveOctetData")
  private Object[][] provideInvalidConsecutiveOctetData()
  {
    return new Object[][]
        {
            { "120", 0, "Must begin decoding from a percent-escaped octet, but found '1'" },
            { "", 0, "Cannot decode from index 0 of a length-0 string" },
            { "%20", 3, "Cannot decode from index 3 of a length-3 string" }
        };
  }

  @Test(dataProvider = "invalidConsecutiveOctetData")
  public void testDecodeInvalidConsecutiveOctets(String encoded, int startIndex, String expectedErrorMessage)
  {
    IllegalArgumentException exception = null;

    try
    {
      URIDecoderUtils.decodeConsecutiveOctets(new StringBuilder(), encoded, startIndex);
    }
    catch (IllegalArgumentException e)
    {
      exception = e;
    }

    Assert.assertNotNull(exception, "Expected exception when decoding consecutive bytes for string \"" + encoded + "\".");
    Assert.assertEquals(exception.getMessage(), expectedErrorMessage, "Unexpected error message during decoding.");
  }
}

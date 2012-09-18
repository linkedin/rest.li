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

/* $Id$ */
package test.r2.message;

import com.linkedin.r2.message.rest.RestMessage;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TestRestBuilders
{
  @Test
  public void testSetValidHeader()
  {
    final String headerName = "testName";
    final String headerValue = "testValue";

    final RestResponse res = new RestResponseBuilder()
            .setHeader(headerName, headerValue)
            .build();

    Assert.assertEquals(headerValue, res.getHeader(headerName));
  }

  @Test
  public void testSetHeaderNameWithSeparator()
  {
    final String headerBaseName = "invalidName";
    final String headerValue = "testValue";

    // Invalid chars per RFC 2616, section 2.2
    final char[] separators = new char[] {
            '(', ')', '<', '>', '@',
            ',', ';', ':', '\\', '"',
            '/', '[', ']', '?', '=',
            '{', '}', ' ', 9 /* HT */
    };

    for (char separator : separators)
    {
      final RestResponseBuilder builder = new RestResponseBuilder();
      try
      {
        builder.setHeader(headerBaseName + separator, headerValue);
        Assert.fail("Should have thrown exception for invalid char (separator): " + separator);
      }
      catch (IllegalArgumentException e)
      {
        // expected
      }
    }
  }

  @Test
  public void testSetHeaderNameWithControlChar()
  {
    final String headerBaseName = "invalidName";
    final String headerValue = "testValue";

    for (char i = 0; i <= 31; i++)
    {
      final RestResponseBuilder builder = new RestResponseBuilder();
      try
      {
        builder.setHeader(headerBaseName + i, headerValue);
        Assert.fail("Should have thrown exception for invalid char (control char): " + i);
      }
      catch (IllegalArgumentException e)
      {
        // expected
      }
    }

    for (char i = 127; i <= 255; i++)
    {
      final RestResponseBuilder builder = new RestResponseBuilder();
      try
      {
        builder.setHeader(headerBaseName + i, headerValue);
        Assert.fail("Should have thrown exception for invalid char (control char): " + i);
      }
      catch (IllegalArgumentException e)
      {
        // expected
      }
    }
  }

  @Test
  public void testSetMultiValueHeader()
  {
    final String headerName = "key";
    final String headerVal1 = "value1";
    final String headerVal2 = "value2";
    final String headerValue = headerVal1 + ',' + headerVal2;

    final RestMessage msg = new RestResponseBuilder()
            .setHeader(headerName, headerValue)
            .build();

    Assert.assertEquals(headerValue, msg.getHeader(headerName));
    Assert.assertEquals(Arrays.asList(headerVal1, headerVal2), msg.getHeaderValues(headerName));
  }

  @Test
  public void testAddMultipleValuesToHeader()
  {
    final String headerName = "key";
    final String headerVal1 = "value1";
    final String headerVal2 = "value2";
    final String headerValue = headerVal1 + ',' + headerVal2;

    final RestMessage msg = new RestResponseBuilder()
            .addHeaderValue(headerName, headerVal1)
            .addHeaderValue(headerName, headerVal2)
            .build();

    Assert.assertEquals(headerValue, msg.getHeader(headerName));
    Assert.assertEquals(Arrays.asList(headerVal1, headerVal2), msg.getHeaderValues(headerName));
  }

  @Test
  public void testAddMultipleValuesToHeader2()
  {
    final String headerName = "key";
    final String headerVal1a = "value1a";
    final String headerVal1b = "value1b";
    final String headerVal1 = headerVal1a + "," + headerVal1b;
    final String headerVal2 = "value2";
    final String headerValue = headerVal1 + ',' + headerVal2;

    final RestMessage msg = new RestResponseBuilder()
            .addHeaderValue(headerName, headerVal1)
            .addHeaderValue(headerName, headerVal2)
            .build();

    Assert.assertEquals(headerValue, msg.getHeader(headerName));
    Assert.assertEquals(Arrays.asList(headerVal1a, headerVal1b, headerVal2), msg.getHeaderValues(headerName));
  }

  @Test
  public void testHeaderWithNullListElem()
  {
    final String headerName = "key";
    final String headerVal1 = "value1";
    final String headerVal2 = "value2";
    final String headerValue = headerVal1 + ", ," + headerVal2;

    final RestMessage msg = new RestResponseBuilder()
            .addHeaderValue(headerName, headerValue)
            .build();

    Assert.assertEquals(headerValue, msg.getHeader(headerName));
    Assert.assertEquals(Arrays.asList(headerVal1, headerVal2), msg.getHeaderValues(headerName));
  }
}

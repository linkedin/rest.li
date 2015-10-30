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


import com.linkedin.r2.message.MessageHeaders;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  public void testGetHeaderCaseInsensitive()
  {
    final String headerName = "testName";
    final String headerNameDifferentCase = "TestName";
    final String headerValue = "testValue";

    final RestResponse res = new RestResponseBuilder()
        .setHeader(headerName, headerValue)
        .build();

    Assert.assertEquals(headerValue, res.getHeader(headerNameDifferentCase));
  }

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

    final MessageHeaders msg = new RestResponseBuilder()
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

    final MessageHeaders msg = new RestResponseBuilder()
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

    final MessageHeaders msg = new RestResponseBuilder()
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

    final MessageHeaders msg = new RestResponseBuilder()
            .addHeaderValue(headerName, headerValue)
            .build();

    Assert.assertEquals(headerValue, msg.getHeader(headerName));
    Assert.assertEquals(Arrays.asList(headerVal1, headerVal2), msg.getHeaderValues(headerName));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSetHeaderWithCookieFails()
  {
    final String headerName = "set-cookie";
    final String headerValue = "value";

    final RestResponse res = new RestResponseBuilder()
        .setHeader(headerName, headerValue)
        .build();
  }

  @Test
  public void testAddCookieSingleValue()
  {
    final String cookie = "cookie";

    final RestResponse res = new RestResponseBuilder()
        .addCookie(cookie).build();

    Assert.assertNotNull(res.getHeaders());
    Assert.assertNotNull(res.getCookies());
    Assert.assertTrue(res.getHeaders().isEmpty());
    Assert.assertEquals(res.getCookies().size(), 1);
    Assert.assertEquals(res.getCookies().get(0), cookie);
  }

  @Test
  public void testAddCookieMultipleValues()
  {
    final String cookie1 = "cookie1";
    final String cookie2 = "cookie2";

    final RestResponse res = new RestResponseBuilder()
        .addCookie(cookie1)
        .addCookie(cookie2)
        .build();

    Assert.assertNotNull(res.getHeaders());
    Assert.assertNotNull(res.getCookies());
    Assert.assertTrue(res.getHeaders().isEmpty());
    Assert.assertEquals(res.getCookies().size(), 2);
    Assert.assertEquals(res.getCookies().get(0), cookie1);
    Assert.assertEquals(res.getCookies().get(1), cookie2);
  }

  @Test
  public void testSetCookiesMultipleValues()
  {
    final String cookie1 = "cookie1";
    final String cookie2 = "cookie2";
    final String cookie3 = "cookie3";
    List<String> cookies = new ArrayList<String>();
    cookies.add(cookie2);
    cookies.add(cookie3);

    final RestResponse res = new RestResponseBuilder()
        .addCookie(cookie1)
        .setCookies(cookies)
        .build();

    Assert.assertNotNull(res.getHeaders());
    Assert.assertNotNull(res.getCookies());
    Assert.assertTrue(res.getHeaders().isEmpty());
    Assert.assertEquals(res.getCookies().size(), 2);
    Assert.assertEquals(res.getCookies().get(0), cookie2);
    Assert.assertEquals(res.getCookies().get(1), cookie3);
  }

  @Test
  public void testSetHeadersAndCookiesMultipleValues()
  {
    final String header1 = "key1";
    final String header2 = "key2";
    final String value1 = "value1";
    final String value2 = "value2";
    final String cookie1 = "cookie1";
    final String cookie2 = "cookie2";
    Map<String, String> headers = new HashMap<String, String>();
    headers.put(header1, value1);
    headers.put(header2, value2);
    List<String> cookies = new ArrayList<String>();
    cookies.add(cookie1);
    cookies.add(cookie2);

    final RestResponse res = new RestResponseBuilder()
        .setHeaders(headers)
        .setCookies(cookies)
        .build();

    Assert.assertNotNull(res.getHeaders());
    Assert.assertNotNull(res.getCookies());
    Assert.assertEquals(res.getHeaders().size(), 2);
    Assert.assertEquals(res.getHeader(header1), value1);
    Assert.assertEquals(res.getHeader(header2), value2);
    Assert.assertEquals(res.getCookies().size(), 2);
    Assert.assertEquals(res.getCookies().get(0), cookie1);
    Assert.assertEquals(res.getCookies().get(1), cookie2);
  }
}

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

package com.linkedin.restli.client.testutils.test;


import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.client.testutils.MockRestliResponseExceptionBuilder;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import java.net.HttpCookie;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


public class TestMockRestliResponseExceptionBuilder
{
  @Test
  public void testBuildDefaults()
  {
    RestLiResponseException exception = new MockRestliResponseExceptionBuilder().build();
    RestResponse errorResponse = exception.getResponse();
    assertEquals(exception.getStatus(), 500);
    assertEquals(errorResponse.getHeader(RestConstants.HEADER_RESTLI_ERROR_RESPONSE), "true");
    assertEquals(errorResponse.getHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION),
        AllProtocolVersions.LATEST_PROTOCOL_VERSION.toString());
    assertTrue(errorResponse.getCookies()
        .isEmpty());
  }

  @Test
  public void testOldProtocolVersion()
  {
    ProtocolVersion expectedProtocolVersion = AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion();
    RestLiResponseException exception =
        new MockRestliResponseExceptionBuilder().setProtocolVersion(expectedProtocolVersion)
            .build();

    RestResponse errorResponse = exception.getResponse();
    assertEquals(errorResponse.getHeader(RestConstants.HEADER_LINKEDIN_ERROR_RESPONSE), "true");
    assertEquals(errorResponse.getHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION),
        expectedProtocolVersion.toString());
  }

  @Test
  public void testOverwriteStatus()
  {
    ErrorResponse noStatusErrorResponse = new ErrorResponse();
    RestLiResponseException exception = new MockRestliResponseExceptionBuilder().setErrorResponse(noStatusErrorResponse)
        .build();
    assertEquals(exception.getStatus(), 500);
  }

  @Test
  public void testSetStatus()
  {
    RestLiResponseException exception = new MockRestliResponseExceptionBuilder()
        .setStatus(HttpStatus.S_403_FORBIDDEN)
        .build();

    assertEquals(exception.getStatus(), 403);
  }

  @Test
  public void testAddCookiesAndHeaders()
  {
    Map.Entry<String, String> expectedEntry = new AbstractMap.SimpleEntry<>("foo", "bar");
    HttpCookie expectedCookie = new HttpCookie("bar", "foo");
    Map<String, String> headers = new HashMap<>();
    headers.put(expectedEntry.getKey(), expectedEntry.getValue());
    List<HttpCookie> cookies = new ArrayList<>();
    cookies.add(expectedCookie);

    RestLiResponseException exception = new MockRestliResponseExceptionBuilder().setHeaders(headers)
        .setCookies(cookies)
        .build();

    RestResponse errorResponse = exception.getResponse();
    assertEquals(errorResponse.getHeader(expectedEntry.getKey()), expectedEntry.getValue());
    assertEquals(errorResponse.getCookies().get(0), "bar=foo");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullCookies()
  {
    MockRestliResponseExceptionBuilder exceptionBuilder = new MockRestliResponseExceptionBuilder();
    exceptionBuilder.setCookies(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullErrorResponse()
  {
    MockRestliResponseExceptionBuilder exceptionBuilder = new MockRestliResponseExceptionBuilder();
    exceptionBuilder.setErrorResponse(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullHeaders()
  {
    MockRestliResponseExceptionBuilder exceptionBuilder = new MockRestliResponseExceptionBuilder();
    exceptionBuilder.setHeaders(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullProtocolVersion()
  {
    MockRestliResponseExceptionBuilder exceptionBuilder = new MockRestliResponseExceptionBuilder();
    exceptionBuilder.setProtocolVersion(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullStatus()
  {
    MockRestliResponseExceptionBuilder exceptionBuilder = new MockRestliResponseExceptionBuilder();
    exceptionBuilder.setStatus(null);
  }
}

/*
 * Copyright (c) 2017 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.restli.examples;


import com.linkedin.restli.common.RestConstants;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.restli.common.RestConstants.HEADER_CONTENT_DISPOSITION;
import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.CONTENT_DISPOSITION_VALUE;
import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.MIME_TYPE;
import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.UNSTRUCTURED_DATA_BYTES;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;


/**
 * Integration tests for all types of unstructured data resources
 */
public class TestGreetingUnstructuredDataResourcesReactive extends UnstructuredDataResourceTestBase
{
  @DataProvider(name = "goodURLs")
  private static Object[][] goodURLs()
  {
    return new Object[][] {
      { "/reactiveGreetingCollectionUnstructuredData/good" },
      { "/reactiveGreetingAssociationUnstructuredData/src=good&dest=bar" },
      { "/reactiveGreetingSimpleUnstructuredData/" }
    };
  }

  @DataProvider(name = "goodMultiWrites")
  private static Object[][] goodMultiWrites()
  {
    return new Object[][] {
      { "/reactiveGreetingCollectionUnstructuredData/goodMultiWrites" }
    };
  }

  @DataProvider(name = "goodInlineURLs")
  private static Object[][] goodInlineURLs()
  {
    return new Object[][] {
      { "/reactiveGreetingCollectionUnstructuredData/goodInline" }
    };
  }

  @DataProvider(name = "goodNullContentTypeURLs")
  private static Object[][] goodNullContentTypeURLs()
  {
    return new Object[][] {
      { "/reactiveGreetingCollectionUnstructuredData/goodNullContentType" }
    };
  }

  @DataProvider(name = "internalErrorURLs")
  private static Object[][] internalErrorURLs()
  {
    return new Object[][] {
      { "/reactiveGreetingCollectionUnstructuredData/exception" }
    };
  }

  @DataProvider(name = "callbackErrorURLs")
  private static Object[][] callbackErrorURLs()
  {
    return new Object[][] {
      { "/reactiveGreetingCollectionUnstructuredData/callbackError" }
    };
  }

  @Test(dataProvider = "goodMultiWrites")
  public void testGetGoodMultiWrites(String resourceURL)
    throws Throwable
  {
    sendGet(resourceURL, (conn) ->
    {
      assertEquals(conn.getResponseCode(), 200);
      assertEquals(conn.getHeaderField(RestConstants.HEADER_CONTENT_TYPE), MIME_TYPE);
      assertUnstructuredDataResponse(conn.getInputStream(), UNSTRUCTURED_DATA_BYTES);
    });
  }

  @Test(dataProvider = "goodURLs")
  public void testGetGood(String resourceURL)
    throws Throwable
  {
    sendGet(resourceURL, (conn) ->
    {
      assertEquals(conn.getResponseCode(), 200);
      assertEquals(conn.getHeaderField(RestConstants.HEADER_CONTENT_TYPE), MIME_TYPE);
      assertUnstructuredDataResponse(conn.getInputStream(), UNSTRUCTURED_DATA_BYTES);
    });
  }

  @Test(dataProvider = "goodNullContentTypeURLs")
  public void testGetGoodNullContentType(String resourceURL)
    throws Throwable
  {
    sendGet(resourceURL, (conn) ->
    {
      assertEquals(conn.getResponseCode(), 200);
      assertNull(conn.getHeaderField(RestConstants.HEADER_CONTENT_TYPE));
      assertUnstructuredDataResponse(conn.getInputStream(), UNSTRUCTURED_DATA_BYTES);
    });
  }
  @Test(dataProvider = "goodInlineURLs")
  public void testGetGoodInline(String resourceURL)
    throws Throwable
  {
    sendGet(resourceURL, (conn) ->
    {
      assertEquals(conn.getResponseCode(), 200);
      assertEquals(conn.getHeaderField(RestConstants.HEADER_CONTENT_TYPE), MIME_TYPE);
      assertEquals(conn.getHeaderField(HEADER_CONTENT_DISPOSITION), CONTENT_DISPOSITION_VALUE);
      assertUnstructuredDataResponse(conn.getInputStream(), UNSTRUCTURED_DATA_BYTES);
    });
  }

  @Test(dataProvider = "callbackErrorURLs")
  public void testCallbackError(String resourceURL)
    throws Throwable
  {
    sendGet(resourceURL, (conn) -> {
      assertEquals(conn.getResponseCode(), 500);
    });
  }

  @Test(dataProvider = "internalErrorURLs")
  public void testInternalError(String resourceURL)
    throws Throwable
  {
    sendGet(resourceURL, (conn) -> {
      assertEquals(conn.getResponseCode(), 500);
    });
  }

  @Test
  public void testBadResponse()
    throws Throwable
  {
    sendGet("/reactiveGreetingCollectionUnstructuredData/bad", (conn) -> {

      // R2 streaming handles response data error with status 200 with corrupted content (empty in this case)

      assertEquals(conn.getResponseCode(), 200);
      assertEquals(conn.getHeaderField(RestConstants.HEADER_CONTENT_TYPE), MIME_TYPE);
      assertUnstructuredDataResponse(conn.getInputStream(), "".getBytes());
    });
  }
}

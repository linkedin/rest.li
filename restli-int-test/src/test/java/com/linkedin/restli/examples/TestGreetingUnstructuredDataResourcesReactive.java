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

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestMethod;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.common.RestConstants;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.restli.common.RestConstants.*;
import static com.linkedin.restli.examples.RestLiIntTestServer.*;
import static com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils.*;
import static org.testng.Assert.*;


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
      // Content type is required.
      assertEquals(conn.getResponseCode(), 500);
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

  @DataProvider(name = "createSuccess")
  private static Object[][] createSuccess()
  {
    byte[] testBytes = "Hello World!".getBytes();
    return new Object[][] {
        { "/reactiveGreetingCollectionUnstructuredData" , ByteString.empty()},
        { "/reactiveGreetingAssociationUnstructuredData", ByteString.empty()},
        { "/reactiveGreetingCollectionUnstructuredData" , ByteString.copy(testBytes)},
        { "/reactiveGreetingAssociationUnstructuredData", ByteString.copy(testBytes)}
    };
  }

  @Test(dataProvider = "createSuccess")
  public void testCreate(String resourceURL, ByteString entity) throws Throwable
  {
    RestResponse response = sentRequest(resourceURL, entity, RestMethod.POST);
    assertEquals(response.getStatus(), 201);
    assertEquals(response.getHeader(RestConstants.HEADER_ID),"1");
  }

  @DataProvider(name = "updateSuccess")
  private static Object[][] updateSuccess()
  {
    byte[] testBytes = "Hello World!".getBytes();
    return new Object[][] {
        { "/reactiveGreetingCollectionUnstructuredData/1" , ByteString.empty()},
        { "/reactiveGreetingAssociationUnstructuredData/src=good&dest=bar", ByteString.empty()},
        { "/reactiveGreetingCollectionUnstructuredData/1" , ByteString.copy(testBytes)},
        { "/reactiveGreetingAssociationUnstructuredData/src=good&dest=bar", ByteString.copy(testBytes)},
        { "/reactiveGreetingSimpleUnstructuredData" , ByteString.empty()},
        { "/reactiveGreetingSimpleUnstructuredData", ByteString.copy(testBytes)}
    };
  }

  @Test(dataProvider = "updateSuccess")
  public void testUpdate(String resourceURL, ByteString entity) throws Throwable
  {
    RestResponse response = sentRequest(resourceURL, entity, RestMethod.PUT);
    assertEquals(response.getStatus(), 200);
  }

  @DataProvider(name = "deleteSuccess")
  private static Object[][] deleteSuccess()
  {
    byte[] testBytes = "Hello World!".getBytes();
    return new Object[][] {
        { "/reactiveGreetingCollectionUnstructuredData/1" , ByteString.empty()},
        { "/reactiveGreetingAssociationUnstructuredData/src=good&dest=bar", ByteString.empty()},
        { "/reactiveGreetingCollectionUnstructuredData/1" , ByteString.copy(testBytes)},
        { "/reactiveGreetingAssociationUnstructuredData/src=good&dest=bar", ByteString.copy(testBytes)},
        { "/reactiveGreetingSimpleUnstructuredData" , ByteString.empty()},
        { "/reactiveGreetingSimpleUnstructuredData", ByteString.copy(testBytes)}
    };
  }

  @Test(dataProvider = "deleteSuccess")
  public void testDelete(String resourceURL, ByteString entity) throws Throwable
  {
    RestResponse response = sentRequest(resourceURL, entity, RestMethod.DELETE);
    assertEquals(response.getStatus(), 200);
  }

  private Client getR2Client()
  {
    Map<String, String>
        transportProperties = Collections.singletonMap(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "10000");
    return newTransportClient(transportProperties);
  }

  private RestResponse sentRequest(String getPartialUrl, ByteString entity, String restMethod) throws Throwable
  {
    Client client = getR2Client();
    URI uri = URI.create("http://localhost:" + DEFAULT_PORT + getPartialUrl);
    RestRequest r = new RestRequestBuilder(uri).setEntity(entity).setMethod(restMethod).build();
    return client.restRequest(r).get();
  }
}

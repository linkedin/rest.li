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

package com.linkedin.restli.client;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.FullEntityReader;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.examples.RestLiIntegrationTest;
import com.linkedin.restli.examples.greetings.server.GreetingUnstructuredDataUtils;

import java.net.URI;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestUnstructuredDataClient extends RestLiIntegrationTest
{
  private static final ByteString CONTENT = ByteString.copy(new byte[8092]);
  private HttpClientFactory _clientFactory;
  private TransportClientAdapter _client;

  @BeforeClass
  public void init()
    throws Exception
  {
    super.init();
    _clientFactory = new HttpClientFactory.Builder().build();
    _client = new TransportClientAdapter(_clientFactory.getClient(Collections.emptyMap()), true);
  }

  @AfterClass
  private void cleanup()
    throws Exception
  {
    final FutureCallback<None> factoryCallback = new FutureCallback<None>();
    final FutureCallback<None> clientCallback = new FutureCallback<None>();
    _client.shutdown(clientCallback);
    _clientFactory.shutdown(factoryCallback);
    clientCallback.get();
    factoryCallback.get();
    super.shutdown();
  }

  @DataProvider(name = "goodURLs")
  private static Object[][] goodURLs()
  {
    return new Object[][] {
      { "greetingCollectionUnstructuredData/good" },
      { "reactiveGreetingCollectionUnstructuredData/good" },
      { "reactiveGreetingCollectionUnstructuredData/goodMultiWrites" }
    };
  }

  @DataProvider(name = "exceptionURLs")
  private static Object[][] exceptionURLs()
  {
    return new Object[][] {
      { "greetingCollectionUnstructuredData/exception" },
      { "reactiveGreetingCollectionUnstructuredData/exception" }
    };
  }

  @DataProvider(name = "deleteURLs")
  private static Object[][] deleteURLs()
  {
    return new Object[][] {
        { "reactiveGreetingCollectionUnstructuredData/good" }
    };
  }

  @Test(dataProvider = "goodURLs")
  public void testClientWithGoodURLs(String resourceURL)
    throws Throwable
  {
    URI testURI = URI.create(RestLiIntegrationTest.URI_PREFIX + resourceURL);
    RestRequest request = new RestRequestBuilder(testURI).build();
    Future<RestResponse> responseFuture = _client.restRequest(request);
    RestResponse restResponse = responseFuture.get();
    assertNotNull(restResponse.getEntity());
    assertEquals(restResponse.getEntity().copyBytes(), GreetingUnstructuredDataUtils.UNSTRUCTURED_DATA_BYTES);
  }

  @Test(dataProvider = "goodURLs")
  public void testClientGETGoodURLsWithBody(String resourceURL)
      throws Throwable
  {
    URI testURI = URI.create(RestLiIntegrationTest.URI_PREFIX + resourceURL);
    RestRequest request = new RestRequestBuilder(testURI).setEntity(CONTENT).build();
    Future<RestResponse> responseFuture = _client.restRequest(request);
    RestResponse restResponse = responseFuture.get();
    assertNotNull(restResponse.getEntity());
    assertEquals(restResponse.getEntity().copyBytes(), GreetingUnstructuredDataUtils.UNSTRUCTURED_DATA_BYTES);
  }

  @Test(dataProvider = "deleteURLs")
  public void testClientDeleteGoodURLs(String resourceURL)
      throws Throwable
  {
    URI testURI = URI.create(RestLiIntegrationTest.URI_PREFIX + resourceURL);
    RestRequest request = new RestRequestBuilder(testURI).setMethod("DELETE").build();
    Future<RestResponse> responseFuture = _client.restRequest(request);
    RestResponse restResponse = responseFuture.get();
    assertEquals(restResponse.getStatus(), 200);
  }

  @Test(dataProvider = "deleteURLs")
  public void testClientDeleteGoodURLsWithBody(String resourceURL)
      throws Throwable
  {
    URI testURI = URI.create(RestLiIntegrationTest.URI_PREFIX + resourceURL);
    RestRequest request = new RestRequestBuilder(testURI).setMethod("DELETE").setEntity(CONTENT).build();
    Future<RestResponse> responseFuture = _client.restRequest(request);
    RestResponse restResponse = responseFuture.get();
    assertEquals(restResponse.getStatus(), 200);
  }

  @Test(dataProvider = "exceptionURLs", expectedExceptions = ExecutionException.class)
  public void testClientWithExceptionURLs(String resourceURL)
    throws Throwable
  {
    URI testURI = URI.create(RestLiIntegrationTest.URI_PREFIX + resourceURL);
    RestRequest request = new RestRequestBuilder(testURI).build();
    Future<RestResponse> responseFuture = _client.restRequest(request);
    RestResponse restResponse = responseFuture.get();
  }

  @Test(dataProvider = "goodURLs")
  public void testClientWithStreamResponse(String resourceURL)
    throws Throwable
  {
    ByteString expectedContent = ByteString.copy(GreetingUnstructuredDataUtils.UNSTRUCTURED_DATA_BYTES);
    URI testURI = URI.create(RestLiIntegrationTest.URI_PREFIX + resourceURL);
    EntityStream emptyStream = EntityStreams.emptyStream();
    StreamRequest streamRequest = new StreamRequestBuilder(testURI).build(emptyStream);
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean();
    _client.streamRequest(streamRequest, new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        fail("failed to get response", e);
      }

      @Override
      public void onSuccess(StreamResponse result)
      {
        assertEquals(result.getHeader(RestConstants.HEADER_CONTENT_TYPE), GreetingUnstructuredDataUtils.MIME_TYPE);
        FullEntityReader fullEntityReader = new FullEntityReader(new Callback<ByteString>()
        {
          @Override
          public void onError(Throwable e)
          {
            success.set(false);
            latch.countDown();
          }

          @Override
          public void onSuccess(ByteString result)
          {
            assertEquals(result, expectedContent); // Won't fail the test, only use to print out error
            success.set(result.equals(expectedContent)); // Will fail the test if content is not identical
            latch.countDown();
          }
        });
        result.getEntityStream().setReader(fullEntityReader);
      }
    });
    latch.await(10, TimeUnit.SECONDS);
    if (!success.get()) fail("Failed to read response data from stream!");
  }
}
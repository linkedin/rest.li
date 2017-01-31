/*
   Copyright (c) 2017 LinkedIn Corp.

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

package test.r2.integ;

import com.linkedin.r2.disruptor.DisruptedException;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.linkedin.r2.disruptor.DisruptContexts;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 * @version $Revision$
 */
public class TestDisruptor
{
  private static final String DISRUPT_CONTEXT_KEY = "R2_DISRUPT_CONTEXT";

  private static final int PORT = 8100;
  private static final boolean REST_OVER_STREAM = true;
  private static final String SCHEME = "http://";
  private static final String ADDRESS = "localhost";
  private static final String PATH = "/headerEcho";
  private static final String REQUEST_URI = SCHEME + ADDRESS + ":" + PORT + PATH;

  private static final int REQUEST_TIMEOUT = 0;
  private static final long REQUEST_LATENCY = 0;

  private static final int TEST_TIMEOUT = 5000;

  private HttpServer _server;

  @BeforeClass
  public void setup() throws IOException
  {
    final TransportDispatcher dispatcher = new TransportDispatcherBuilder(REST_OVER_STREAM)
        .addRestHandler(URI.create(PATH), new AbstractHttpServerTest.HeaderEchoHandler())
        .build();
    final HttpServerFactory factory = new HttpServerFactory();
    _server = factory.createH2cServer(PORT, dispatcher, REST_OVER_STREAM);
    _server.start();
  }

  @AfterClass
  public void tearDown() throws Exception
  {
    _server.stop();
    _server.waitForStop();
  }

  @Test
  public void testRestNoDisrupt() throws Exception
  {
    final Map<String, String> properties = new HashMap<>();
    final TransportClientFactory factory = new HttpClientFactory.Builder().build();
    final TransportClient client = factory.getClient(properties);
    final RequestContext requestContext = new RequestContext();

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    client.restRequest(new RestRequestBuilder(new URI(REQUEST_URI)).build(), requestContext,
        new HashMap<>(), response -> {
          success.set(!response.hasError() && response.getResponse() != null);
          latch.countDown();
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }

  @Test
  public void testStreamNoDisrupt() throws Exception
  {
    final Map<String, String> properties = new HashMap<>();
    final TransportClientFactory factory = new HttpClientFactory.Builder().build();
    final TransportClient client = factory.getClient(properties);
    final RequestContext requestContext = new RequestContext();

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    client.streamRequest(new StreamRequestBuilder(new URI(REQUEST_URI)).build(EntityStreams.emptyStream()),
        requestContext, new HashMap<>(), response -> {
          success.set(!response.hasError() && response.getResponse() != null);
          latch.countDown();
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }

  @Test
  public void testRestLatencyDisrupt() throws Exception
  {
    final Map<String, String> properties = new HashMap<>();
    final TransportClientFactory factory = new HttpClientFactory.Builder().build();
    final TransportClient client = factory.getClient(properties);

    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.delay(REQUEST_LATENCY));

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    client.restRequest(new RestRequestBuilder(new URI(REQUEST_URI)).build(), requestContext,
        new HashMap<>(), response -> {
          success.set(!response.hasError() && response.getResponse() != null);
          latch.countDown();
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }

  @Test
  public void testStreamLatencyDisrupt() throws Exception
  {
    final Map<String, String> properties = new HashMap<>();
    final TransportClientFactory factory = new HttpClientFactory.Builder().build();
    final TransportClient client = factory.getClient(properties);

    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.delay(REQUEST_LATENCY));

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    client.streamRequest(new StreamRequestBuilder(new URI(REQUEST_URI)).build(EntityStreams.emptyStream()),
        requestContext, new HashMap<>(), response -> {
          success.set(!response.hasError() && response.getResponse() != null);
          latch.countDown();
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }

  @Test
  public void testRestTimeoutDisrupt() throws Exception
  {
    final Map<String, String> properties = new HashMap<>();
    properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(REQUEST_TIMEOUT));
    final TransportClientFactory factory = new HttpClientFactory.Builder().build();
    final TransportClient client = factory.getClient(properties);

    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.timeout());

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    client.restRequest(new RestRequestBuilder(new URI(REQUEST_URI)).build(), requestContext, new HashMap<>(),
        response -> {
          success.set(response.hasError() && response.getError() instanceof TimeoutException);
          latch.countDown();
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }

  @Test
  public void testStreamTimeoutDisrupt() throws Exception
  {
    final Map<String, String> properties = new HashMap<>();
    properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(REQUEST_TIMEOUT));
    final TransportClientFactory factory = new HttpClientFactory.Builder().build();
    final TransportClient client = factory.getClient(properties);

    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.timeout());

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    client.streamRequest(new StreamRequestBuilder(new URI(REQUEST_URI)).build(EntityStreams.emptyStream()),
        requestContext, new HashMap<>(), response -> {
          success.set(response.hasError() && response.getError() instanceof TimeoutException);
          latch.countDown();
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }

  @Test
  public void testRestErrorDisrupt() throws Exception
  {
    final Map<String, String> properties = new HashMap<>();
    properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(REQUEST_TIMEOUT));
    final TransportClientFactory factory = new HttpClientFactory.Builder().build();
    final TransportClient client = factory.getClient(properties);

    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.error(REQUEST_LATENCY));

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    client.restRequest(new RestRequestBuilder(new URI(REQUEST_URI)).build(), requestContext, new HashMap<>(),
        response -> {
          success.set(response.hasError() && response.getError() instanceof DisruptedException);
          latch.countDown();
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }

  @Test
  public void testStreamErrorDisrupt() throws Exception
  {
    final Map<String, String> properties = new HashMap<>();
    properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(REQUEST_TIMEOUT));
    final TransportClientFactory factory = new HttpClientFactory.Builder().build();
    final TransportClient client = factory.getClient(properties);

    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.error(REQUEST_LATENCY));

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    client.streamRequest(new StreamRequestBuilder(new URI(REQUEST_URI)).build(EntityStreams.emptyStream()), requestContext,
        new HashMap<>(), response -> {
          success.set(response.hasError() && response.getError() instanceof DisruptedException);
          latch.countDown();
        });

    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Test execution timeout");
    Assert.assertTrue(success.get(), "Unexpected transport response");
  }
}

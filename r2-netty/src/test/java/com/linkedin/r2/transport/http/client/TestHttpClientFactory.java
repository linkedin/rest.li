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

/**
 * $Id: $
 */

package com.linkedin.r2.transport.http.client;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.compression.streaming.StreamEncodingType;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.timing.TimingKey;
import com.linkedin.r2.testutils.server.HttpServerBuilder;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import com.linkedin.pegasus.io.netty.channel.EventLoopGroup;
import com.linkedin.pegasus.io.netty.channel.nio.NioEventLoopGroup;
import org.eclipse.jetty.server.Server;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TestHttpClientFactory
{
  public static final String HTTP_1_1 = HttpProtocolVersion.HTTP_1_1.name();
  public static final String HTTP_2 = HttpProtocolVersion.HTTP_2.name();

  public static final String URI = "http://localhost:8080/";

  @DataProvider
  public static Object[][] configsExpectedRequestCount()
  {
    return new Object[][] {
      { true, HTTP_1_1,100 },
      { true, HTTP_2 ,200}, // 200 because HTTP2 has also the initial OPTIONS request
      { false, HTTP_1_1 ,100},
      { false, HTTP_2 ,100},
    };
  }

  @Test(dataProvider = "configsExpectedRequestCount")
  public void testSuccessfulRequest(boolean restOverStream, String protocolVersion, int expectedRequests) throws Exception
  {
    EventLoopGroup eventLoop = new NioEventLoopGroup();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    HttpClientFactory factory = getHttpClientFactory(eventLoop, true, scheduler, true);

    HttpServerBuilder.HttpServerStatsProvider httpServerStatsProvider = new HttpServerBuilder.HttpServerStatsProvider();

    Server server = new HttpServerBuilder().serverStatsProvider(httpServerStatsProvider).build();
    try
    {
      server.start();
      List<Client> clients = new ArrayList<>();

      int savedTimingKeyCount = TimingKey.getCount();
      for (int i = 0; i < 100; i++)
      {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, protocolVersion);
        clients.add(new TransportClientAdapter(factory.getClient(properties), restOverStream));
      }
      int addedTimingKeyCount = TimingKey.getCount() - savedTimingKeyCount;
      // In current implementation, one client can have around 30 TimingKeys by default.
      Assert.assertTrue(addedTimingKeyCount >= 30 * clients.size());
      for (Client c : clients)
      {
        RestRequest r = new RestRequestBuilder(new URI(URI)).build();
        c.restRequest(r).get(30, TimeUnit.SECONDS);
      }
      Assert.assertEquals(httpServerStatsProvider.requestCount(), expectedRequests);

      savedTimingKeyCount = TimingKey.getCount();
      for (Client c : clients)
      {
        FutureCallback<None> callback = new FutureCallback<>();
        c.shutdown(callback);
        callback.get(30, TimeUnit.SECONDS);
      }

      FutureCallback<None> factoryShutdown = new FutureCallback<>();
      factory.shutdown(factoryShutdown);
      factoryShutdown.get(30, TimeUnit.SECONDS);
      int removedTimingKeyCount = savedTimingKeyCount - TimingKey.getCount();
      Assert.assertEquals(addedTimingKeyCount, removedTimingKeyCount);
    }
    finally
    {
      server.stop();
    }
  }

  @DataProvider
  public static Object[][] configs()
  {
    return new Object[][] {
      { true, HTTP_1_1 },
      { true, HTTP_2 },
      { false, HTTP_1_1 },
      { false, HTTP_2 },
    };
  }

  @Test(dataProvider = "configs")
  public void testShutdownBeforeClients(boolean restOverStream, String protocolVersion) throws Exception
  {
    EventLoopGroup eventLoop = new NioEventLoopGroup();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    HttpClientFactory factory = getHttpClientFactory(eventLoop, true, scheduler, true);
    Server server = new HttpServerBuilder().build();
    try
    {
      server.start();
      List<Client> clients = new ArrayList<>();
      for (int i = 0; i < 100; i++)
      {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, protocolVersion);
        clients.add(new TransportClientAdapter(factory.getClient(properties), restOverStream));
      }

      for (Client c : clients)
      {
        RestRequest r = new RestRequestBuilder(new URI(URI)).build();
        c.restRequest(r).get(30, TimeUnit.SECONDS);
      }

      FutureCallback<None> factoryShutdown = new FutureCallback<>();
      factory.shutdown(factoryShutdown);

      for (Client c : clients)
      {
        FutureCallback<None> callback = new FutureCallback<>();
        c.shutdown(callback);
        callback.get(30, TimeUnit.SECONDS);
      }

      factoryShutdown.get(30, TimeUnit.SECONDS);

      Assert.assertTrue(eventLoop.awaitTermination(30, TimeUnit.SECONDS), "Failed to shut down event-loop");
      Assert.assertTrue(scheduler.awaitTermination(30, TimeUnit.SECONDS), "Failed to shut down scheduler");
    }
    finally
    {
      server.stop();
    }
  }

  private void createRawClientHelper(String protocolVersion)
  {
    EventLoopGroup eventLoop = new NioEventLoopGroup();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    HttpClientFactory factory = new HttpClientFactory.Builder()
        .setEventLoopGroup(eventLoop)
        .setShutDownFactory(true)
        .setScheduleExecutorService(scheduler)
        .setShutdownScheduledExecutorService(true)
        .build();

    Map<String, String> properties = new HashMap<>();

    String requestTimeout = "7000";
    String poolSize = "10";
    String maxResponse = "3000";
    String idleTimeout = String.valueOf((long)Integer.MAX_VALUE + 1);
    String sslIdleTimeout = String.valueOf((long)Integer.MAX_VALUE + 1);

    String shutdownTimeout = "14000";
    HttpClientFactory.MixedClient client;

    //test creation using default values
    factory.getRawClient(properties);

    //test using only new config keys
    properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, requestTimeout);
    properties.put(HttpClientFactory.HTTP_POOL_SIZE, poolSize);
    properties.put(HttpClientFactory.HTTP_IDLE_TIMEOUT, idleTimeout);
    properties.put(HttpClientFactory.HTTP_SSL_IDLE_TIMEOUT, sslIdleTimeout);
    properties.put(HttpClientFactory.HTTP_MAX_RESPONSE_SIZE, maxResponse);
    properties.put(HttpClientFactory.HTTP_SHUTDOWN_TIMEOUT, shutdownTimeout);
    properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, protocolVersion);
    factory.getRawClient(properties);
  }

  @Test
  public void testGetHttpRawClient()
  {
    createRawClientHelper(HTTP_1_1);
  }

  @Test
  public void testGetHttp2RawClient()
  {
    createRawClientHelper(HTTP_2);
  }

  @Test
  public void testNewSSLProperties() throws Exception
  {
    HttpClientFactory factory = new HttpClientFactory.Builder().build();
    Map<String,Object> params = new HashMap<>();
    SSLParameters sslParameters = new SSLParameters();
    sslParameters.setProtocols(new String[]{ "Unsupported" });
    params.put(HttpClientFactory.HTTP_SSL_CONTEXT, SSLContext.getDefault());
    params.put(HttpClientFactory.HTTP_SSL_PARAMS, sslParameters);

    try
    {
      factory.getClient(Collections.unmodifiableMap(params));
      Assert.fail("Should have failed");
    }
    catch (IllegalArgumentException e)
    {
      Assert.assertTrue(e.getMessage().contains("None of the requested protocols: [Unsupported] are found in SSLContext"),
                        "Unexpected error message " + e.getMessage());
    }
  }

  @Test
  public void testSSLParams() throws Exception
  {
    HttpClientFactory factory = new HttpClientFactory.Builder().build();
    Map<String,Object> params = new HashMap<>();
    SSLParameters sslParameters = new SSLParameters();
    sslParameters.setProtocols(new String[]{ "Unsupported" });

    params.put(HttpClientFactory.HTTP_SSL_CONTEXT, SSLContext.getDefault());
    params.put(HttpClientFactory.HTTP_SSL_PARAMS, sslParameters);

    try
    {
      factory.getClient(Collections.unmodifiableMap(params));
      Assert.fail("Should have failed");
    }
    catch (IllegalArgumentException e)
    {
      Assert.assertTrue(e.getMessage().contains("None of the requested protocols: [Unsupported] are found in SSLContext"),
                        "Unexpected error message " + e.getMessage());
    }
  }

  @Test(dataProvider = "configs")
  public void testShutdownTimeout(boolean restOverStream, String protocolVersion) throws Exception
  {
    EventLoopGroup eventLoop = new NioEventLoopGroup();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    HttpClientFactory factory = getHttpClientFactory(eventLoop, true, scheduler, true);
    Server server = new HttpServerBuilder().build();
    try
    {
      server.start();
      List<Client> clients = new ArrayList<>();
      for (int i = 0; i < 100; i++)
      {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, protocolVersion);
        clients.add(new TransportClientAdapter(factory.getClient(properties), restOverStream));
      }

      for (Client c : clients)
      {
        RestRequest r = new RestRequestBuilder(new URI(URI)).build();
        c.restRequest(r).get(30, TimeUnit.SECONDS);
      }

      FutureCallback<None> factoryShutdown = new FutureCallback<>();
      factory.shutdown(factoryShutdown, 1, TimeUnit.SECONDS);

      factoryShutdown.get(30, TimeUnit.SECONDS);

      Assert.assertTrue(eventLoop.awaitTermination(30, TimeUnit.SECONDS), "Failed to shut down event-loop");
      Assert.assertTrue(scheduler.awaitTermination(30, TimeUnit.SECONDS), "Failed to shut down scheduler");
    }
    finally
    {
      server.stop();
    }
  }

  @Test(dataProvider = "configs")
  public void testShutdownNoTimeout(boolean restOverStream, String protocolVersion) throws Exception
  {
    EventLoopGroup eventLoop = new NioEventLoopGroup();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    HttpClientFactory factory = getHttpClientFactory(eventLoop, true, scheduler, true);
    Server server = new HttpServerBuilder().build();
    try
    {
      server.start();
      List<Client> clients = new ArrayList<>();
      for (int i = 0; i < 100; i++)
      {
        HashMap<String, String> properties = new HashMap<>();
        properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, protocolVersion);
        clients.add(new TransportClientAdapter(factory.getClient(properties), restOverStream));
      }

      for (Client c : clients)
      {
        RestRequest r = new RestRequestBuilder(new URI(URI)).build();
        c.restRequest(r).get(30, TimeUnit.SECONDS);
      }
    }
    finally
    {
      server.stop();
    }

    FutureCallback<None> factoryShutdown = new FutureCallback<>();
    factory.shutdown(factoryShutdown);
    try
    {
      factoryShutdown.get(1, TimeUnit.SECONDS);
      Assert.fail("Factory shutdown should have timed out");
    }
    catch (TimeoutException e)
    {
      // Expected
    }

    Assert.assertFalse(eventLoop.isShutdown(), "Boss should not be shut down");
    Assert.assertFalse(scheduler.isShutdown(), "Scheduler should not be shut down");
  }

  @Test(dataProvider = "configs")
  public void testShutdownIOThread(boolean restOverStream, String protocolVersion) throws Exception
  {
    EventLoopGroup eventLoop = new NioEventLoopGroup();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    ExecutorService callbackExecutor = Executors.newFixedThreadPool(1);
    HttpClientFactory factory = getHttpClientFactory(eventLoop, true, scheduler, true, callbackExecutor, false);
    CountDownLatch responseLatch = new CountDownLatch(1);
    Server server = new HttpServerBuilder().responseLatch(responseLatch).build();
    try
    {
      server.start();
      HashMap<String, String> properties = new HashMap<>();
      properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, protocolVersion);
      Client client = new TransportClientAdapter(factory.getClient(properties), restOverStream);

      URI uri = new URI(URI);
      Future<RestResponse> responseFuture = client.restRequest(new RestRequestBuilder(uri).build());

      FutureCallback<None> factoryShutdown = new FutureCallback<>();
      factory.shutdown(factoryShutdown);

      FutureCallback<None> clientShutdown = new FutureCallback<>();
      client.shutdown(clientShutdown);

      // Client and factory shutdowns are now pending.  When we release the latch, the response will
      // be returned, which causes the shutdowns to complete on the Netty IO thread that received the
      // response.
      responseLatch.countDown();

      clientShutdown.get(60, TimeUnit.SECONDS);
      factoryShutdown.get(60, TimeUnit.SECONDS);
    }
    finally
    {
      server.stop();
    }

    Assert.assertTrue(eventLoop.awaitTermination(30, TimeUnit.SECONDS), "Failed to shut down event-loop");
    Assert.assertTrue(scheduler.awaitTermination(60, TimeUnit.SECONDS), "Failed to shut down scheduler");
    callbackExecutor.shutdown();
  }

  /**
   * Tests that even when the factory is shutdown with a long timeout, it does not occupy
   * any executors with tasks that might prevent them shutting down properly.
   * @throws InterruptedException
   * @throws ExecutionException
   * @throws TimeoutException
   */
  @Test
  public void testShutdownTimeoutDoesNotOccupyExecutors()
          throws InterruptedException, ExecutionException, TimeoutException
  {
    EventLoopGroup eventLoop = new NioEventLoopGroup();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    HttpClientFactory factory = getHttpClientFactory(eventLoop, false, scheduler, false);

    FutureCallback<None> callback = new FutureCallback<>();
    factory.shutdown(callback, 60, TimeUnit.MINUTES);
    callback.get(60, TimeUnit.SECONDS);
    scheduler.shutdown();
    eventLoop.shutdownGracefully();
    Assert.assertTrue(scheduler.awaitTermination(60, TimeUnit.SECONDS));
    Assert.assertTrue(eventLoop.awaitTermination(60, TimeUnit.SECONDS));
  }

  @Test
  public void testClientShutdownBeingCalledMultipleTimes()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    HttpClientFactory factory = new HttpClientFactory.Builder().build();
    TransportClient client = factory.getClient(Collections.<String, Object>emptyMap());
    // first shutdown call
    FutureCallback<None> clientShutdown = new FutureCallback<>();
    client.shutdown(clientShutdown);
    clientShutdown.get(30, TimeUnit.SECONDS);
    // second shutdown call
    clientShutdown = new FutureCallback<>();
    client.shutdown(clientShutdown);
    try
    {
      clientShutdown.get(30, TimeUnit.SECONDS);
      Assert.fail("should have thrown exception on the second shutdown call.");
    }
    catch (ExecutionException ex)
    {
      Assert.assertTrue(ex.getCause() instanceof IllegalStateException);
    }

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    factory.shutdown(shutdownCallback);
    shutdownCallback.get(30, TimeUnit.SECONDS);
  }

  @DataProvider(name = "compressionConfigsData")
  private Object[][] compressionConfigsData()
  {
    return new Object[][] {
        {"service1", 10000, new CompressionConfig(0)},
        {"service2", 10000, new CompressionConfig(Integer.MAX_VALUE)},
        {"service3", 10000, new CompressionConfig(111)},
        {"service4", 10000, new CompressionConfig(10000)},
        {"service1", 0, new CompressionConfig(0)},
        {"service2", 0, new CompressionConfig(Integer.MAX_VALUE)},
        {"service3", 0, new CompressionConfig(111)},
        {"service4", 0, new CompressionConfig(0)}
    };
  }

  @Test(dataProvider = "compressionConfigsData")
  public void testGetRequestCompressionConfig(String serviceName, int requestCompressionThresholdDefault, CompressionConfig expectedConfig)
  {
    Map<String, CompressionConfig> requestCompressionConfigs = new HashMap<>();
    requestCompressionConfigs.put("service1", new CompressionConfig(0));
    requestCompressionConfigs.put("service2", new CompressionConfig(Integer.MAX_VALUE));
    requestCompressionConfigs.put("service3", new CompressionConfig(111));
    HttpClientFactory factory = new HttpClientFactory.Builder()
        .setRequestCompressionThresholdDefault(requestCompressionThresholdDefault)
        .setRequestCompressionConfigs(requestCompressionConfigs)
        .build();
    Assert.assertEquals(factory.getStreamRequestCompressionConfig(serviceName, StreamEncodingType.SNAPPY_FRAMED), expectedConfig);
  }

  private static HttpClientFactory getHttpClientFactory(EventLoopGroup eventLoopGroup,
                                                        boolean shutdownFactory,
                                                        ScheduledExecutorService scheduler,
                                                        boolean shutdownScheduler)
  {
    return getHttpClientFactory(eventLoopGroup, shutdownFactory, scheduler, shutdownScheduler,
        Executors.newFixedThreadPool(1), true);
  }

  private static HttpClientFactory getHttpClientFactory(EventLoopGroup eventLoopGroup,
                                                        boolean shutdownFactory,
                                                        ScheduledExecutorService scheduler,
                                                        boolean shutdownScheduler,
                                                        ExecutorService callbackExecutor,
                                                        boolean shutdownCallbackExecutor)
  {
    return new HttpClientFactory.Builder()
        .setEventLoopGroup(eventLoopGroup)
        .setShutDownFactory(shutdownFactory)
        .setScheduleExecutorService(scheduler)
        .setShutdownScheduledExecutorService(shutdownScheduler)
        .setCallbackExecutor(callbackExecutor)
        .setShutdownCallbackExecutor(shutdownCallbackExecutor)
        .build();
  }
}

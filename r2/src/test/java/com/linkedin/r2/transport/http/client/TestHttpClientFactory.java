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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.linkedin.r2.message.rest.RestResponse;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TestHttpClientFactory
{
  private TestServer _testServer;

  @BeforeClass
  public void setup() throws IOException
  {
    _testServer = new TestServer();
  }

  @AfterClass
  public void tearDown() throws IOException, InterruptedException
  {
    _testServer.shutdown();
  }

  @Test
  public void testShutdownAfterClients() throws ExecutionException, TimeoutException, InterruptedException
  {
    ExecutorService boss = Executors.newCachedThreadPool();
    ExecutorService worker = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    ClientSocketChannelFactory channelFactory = new NioClientSocketChannelFactory(boss, worker);
    HttpClientFactory factory = new HttpClientFactory(FilterChains.empty(), channelFactory, true, scheduler, true);

    List<Client> clients = new ArrayList<Client>();
    for (int i = 0; i < 100; i++)
    {
      clients.add(new TransportClientAdapter(factory.getClient(Collections.<String, String>emptyMap())));
    }

    for (Client c : clients)
    {
      RestRequest r = new RestRequestBuilder(_testServer.getRequestURI()).build();
      c.restRequest(r).get(30, TimeUnit.SECONDS);
    }

    for (Client c : clients)
    {
      FutureCallback<None> callback = new FutureCallback<None>();
      c.shutdown(callback);
      callback.get(30, TimeUnit.SECONDS);
    }

    FutureCallback<None> factoryShutdown = new FutureCallback<None>();
    factory.shutdown(factoryShutdown);
    factoryShutdown.get(30, TimeUnit.SECONDS);

    Assert.assertTrue(boss.awaitTermination(30, TimeUnit.SECONDS), "Failed to shut down boss");
    Assert.assertTrue(worker.awaitTermination(30, TimeUnit.SECONDS), "Failed to shut down worker");
    Assert.assertTrue(scheduler.awaitTermination(30, TimeUnit.SECONDS), "Failed to shut down scheduler");
  }

  @Test
  public void testShutdownBeforeClients() throws ExecutionException, TimeoutException, InterruptedException
  {
    ExecutorService boss = Executors.newCachedThreadPool();
    ExecutorService worker = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    ClientSocketChannelFactory channelFactory = new NioClientSocketChannelFactory(boss, worker);
    HttpClientFactory factory = new HttpClientFactory(FilterChains.empty(), channelFactory, true, scheduler, true);

    List<Client> clients = new ArrayList<Client>();
    for (int i = 0; i < 100; i++)
    {
      clients.add(new TransportClientAdapter(factory.getClient(Collections.<String, String>emptyMap())));
    }

    for (Client c : clients)
    {
      RestRequest r = new RestRequestBuilder(_testServer.getRequestURI()).build();
      c.restRequest(r).get(30, TimeUnit.SECONDS);
    }

    FutureCallback<None> factoryShutdown = new FutureCallback<None>();
    factory.shutdown(factoryShutdown);

    for (Client c : clients)
    {
      FutureCallback<None> callback = new FutureCallback<None>();
      c.shutdown(callback);
      callback.get(30, TimeUnit.SECONDS);
    }

    factoryShutdown.get(30, TimeUnit.SECONDS);

    Assert.assertTrue(boss.awaitTermination(30, TimeUnit.SECONDS), "Failed to shut down boss");
    Assert.assertTrue(worker.awaitTermination(30, TimeUnit.SECONDS), "Failed to shut down worker");
    Assert.assertTrue(scheduler.awaitTermination(30, TimeUnit.SECONDS), "Failed to shut down scheduler");
  }

  @Test
  public void testGetRawClient()
  {
    ExecutorService boss = Executors.newCachedThreadPool();
    ExecutorService worker = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    ClientSocketChannelFactory channelFactory = new NioClientSocketChannelFactory(boss, worker);
    HttpClientFactory factory = new HttpClientFactory(FilterChains.empty(), channelFactory, true, scheduler, true);

    Map<String, String> properties = new HashMap<String, String>();

    String oldGetTimeout = "5000";
    String oldRequestTimeout = "6000";
    String requestTimeout = "7000";
    String oldPoolSize = "5";
    String poolSize = "10";
    String oldMaxResponse = "2000";
    String maxResponse = "3000";
    String idleTimeout = "8000";
    String oldIdleTimeout = "12000";
    String oldShutdownTimeout = "13000";
    String shutdownTimeout = "14000";
    HttpNettyClient client;

    //test creation using default values
    client = factory.getRawClient(properties);
    Assert.assertEquals(client.getMaxResponseSize(), HttpClientFactory.DEFAULT_MAX_RESPONSE_SIZE);
    Assert.assertEquals(client.getRequestTimeout(), HttpClientFactory.DEFAULT_REQUEST_TIMEOUT);
    Assert.assertEquals(client.getShutdownTimeout(), HttpClientFactory.DEFAULT_SHUTDOWN_TIMEOUT);

    //test creation using old config keys TODO remove this once we delete all the old config keys
    properties.put(HttpClientFactory.OLD_GET_TIMEOUT_KEY, oldGetTimeout);
    properties.put(HttpClientFactory.OLD_POOL_SIZE_KEY, oldPoolSize);
    properties.put(HttpClientFactory.OLD_IDLE_TIMEOUT_KEY, oldIdleTimeout);
    properties.put(HttpClientFactory.OLD_SHUTDOWN_TIMEOUT_KEY, oldShutdownTimeout);
    properties.put(HttpClientFactory.OLD_MAX_RESPONSE_SIZE, oldMaxResponse);

    client = factory.getRawClient(properties);
    Assert.assertEquals(client.getMaxResponseSize(), Integer.parseInt(oldMaxResponse));
    Assert.assertEquals(client.getRequestTimeout(), Integer.parseInt(oldGetTimeout));
    Assert.assertEquals(client.getShutdownTimeout(), Integer.parseInt(oldShutdownTimeout));

    properties.put(HttpClientFactory.OLD_REQUEST_TIMEOUT_KEY, oldRequestTimeout);
    client = factory.getRawClient(properties);
    Assert.assertEquals(client.getRequestTimeout(), Integer.parseInt(oldRequestTimeout));

    //test if both old and new config keys are there
    properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, requestTimeout);
    properties.put(HttpClientFactory.HTTP_POOL_SIZE, poolSize);
    properties.put(HttpClientFactory.HTTP_IDLE_TIMEOUT, idleTimeout);
    properties.put(HttpClientFactory.HTTP_MAX_RESPONSE_SIZE, maxResponse);
    properties.put(HttpClientFactory.HTTP_SHUTDOWN_TIMEOUT, shutdownTimeout);
    client = factory.getRawClient(properties);
    Assert.assertEquals(client.getMaxResponseSize(), Integer.parseInt(maxResponse));
    Assert.assertEquals(client.getRequestTimeout(), Integer.parseInt(requestTimeout));
    Assert.assertEquals(client.getShutdownTimeout(), Integer.parseInt(shutdownTimeout));

    //test using only new config keys
    properties.remove(HttpClientFactory.OLD_GET_TIMEOUT_KEY);
    properties.remove(HttpClientFactory.OLD_POOL_SIZE_KEY);
    properties.remove(HttpClientFactory.OLD_IDLE_TIMEOUT_KEY);
    properties.remove(HttpClientFactory.OLD_SHUTDOWN_TIMEOUT_KEY);
    properties.remove(HttpClientFactory.OLD_MAX_RESPONSE_SIZE);

    client = factory.getRawClient(properties);
    Assert.assertEquals(client.getMaxResponseSize(), Integer.parseInt(maxResponse));
    Assert.assertEquals(client.getRequestTimeout(), Integer.parseInt(requestTimeout));
    Assert.assertEquals(client.getShutdownTimeout(), Integer.parseInt(shutdownTimeout));

    properties.remove(HttpClientFactory.OLD_REQUEST_TIMEOUT_KEY);
    client = factory.getRawClient(properties);
    Assert.assertEquals(client.getMaxResponseSize(), Integer.parseInt(maxResponse));
    Assert.assertEquals(client.getRequestTimeout(), Integer.parseInt(requestTimeout));
    Assert.assertEquals(client.getShutdownTimeout(), Integer.parseInt(shutdownTimeout));
  }

  @Test
  public void testOldSSLProperties() throws Exception
  {
    HttpClientFactory factory = new HttpClientFactory();
    Map<String,Object> params = new HashMap<String, Object>();
    SSLParameters sslParameters = new SSLParameters();
    sslParameters.setProtocols(new String[]{ "Unsupported" });
    params.put(HttpClientFactory.OLD_SSL_CONTEXT, SSLContext.getDefault());
    params.put(HttpClientFactory.OLD_SSL_PARAMS, sslParameters);

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
  public void testNewSSLProperties() throws Exception
  {
    HttpClientFactory factory = new HttpClientFactory();
    Map<String,Object> params = new HashMap<String, Object>();
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
  public void testOldAndNewSSLParams() throws Exception
  {
    HttpClientFactory factory = new HttpClientFactory();
    Map<String,Object> params = new HashMap<String, Object>();
    SSLParameters sslParameters = new SSLParameters();
    sslParameters.setProtocols(new String[]{ "Unsupported" });

    SSLParameters oldSslParameters = new SSLParameters();
    oldSslParameters.setProtocols(new String[]{ "OLDSSLPARAMETERS" });

    params.put(HttpClientFactory.HTTP_SSL_CONTEXT, SSLContext.getDefault());
    params.put(HttpClientFactory.HTTP_SSL_PARAMS, sslParameters);
    params.put(HttpClientFactory.OLD_SSL_PARAMS, oldSslParameters);

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
  public void testShutdownTimeout() throws ExecutionException, TimeoutException, InterruptedException
  {
    ExecutorService boss = Executors.newCachedThreadPool();
    ExecutorService worker = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    ClientSocketChannelFactory channelFactory = new NioClientSocketChannelFactory(boss, worker);
    HttpClientFactory factory = new HttpClientFactory(FilterChains.empty(), channelFactory, true, scheduler, true);

    List<Client> clients = new ArrayList<Client>();
    for (int i = 0; i < 100; i++)
    {
      clients.add(new TransportClientAdapter(factory.getClient(Collections.<String, String>emptyMap())));
    }

    for (Client c : clients)
    {
      RestRequest r = new RestRequestBuilder(_testServer.getRequestURI()).build();
      c.restRequest(r).get(30, TimeUnit.SECONDS);
    }

    FutureCallback<None> factoryShutdown = new FutureCallback<None>();
    factory.shutdown(factoryShutdown, 1, TimeUnit.SECONDS);

    factoryShutdown.get(30, TimeUnit.SECONDS);

    Assert.assertTrue(boss.awaitTermination(30, TimeUnit.SECONDS), "Failed to shut down boss");
    Assert.assertTrue(worker.awaitTermination(30, TimeUnit.SECONDS), "Failed to shut down worker");
    Assert.assertTrue(scheduler.awaitTermination(30, TimeUnit.SECONDS), "Failed to shut down scheduler");
  }

  @Test
  public void testShutdownNoTimeout() throws ExecutionException, TimeoutException, InterruptedException
  {
    ExecutorService boss = Executors.newCachedThreadPool();
    ExecutorService worker = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    ClientSocketChannelFactory channelFactory = new NioClientSocketChannelFactory(boss, worker);
    HttpClientFactory factory = new HttpClientFactory(FilterChains.empty(), channelFactory, true, scheduler, true);

    List<Client> clients = new ArrayList<Client>();
    for (int i = 0; i < 100; i++)
    {
      clients.add(new TransportClientAdapter(factory.getClient(Collections.<String, String>emptyMap())));
    }

    for (Client c : clients)
    {
      RestRequest r = new RestRequestBuilder(_testServer.getRequestURI()).build();
      c.restRequest(r).get(30, TimeUnit.SECONDS);
    }

    FutureCallback<None> factoryShutdown = new FutureCallback<None>();
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

    Assert.assertFalse(boss.isShutdown(), "Boss should not be shut down");
    Assert.assertFalse(worker.isShutdown(), "Worker should not be shut down");
    Assert.assertFalse(scheduler.isShutdown(), "Scheduler should not be shut down");
  }

  @Test
  public void testShutdownIOThread() throws ExecutionException, TimeoutException, InterruptedException
  {
    ExecutorService boss = Executors.newCachedThreadPool();
    ExecutorService worker = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    ClientSocketChannelFactory channelFactory = new NioClientSocketChannelFactory(boss, worker);
    HttpClientFactory factory = new HttpClientFactory(FilterChains.empty(), channelFactory, true, scheduler, true);

    Client client = new TransportClientAdapter(factory.getClient(
            Collections.<String, Object>emptyMap()));

    Future<RestResponse> responseFuture = client.restRequest(new RestRequestBuilder(_testServer.resetResponseLatch(1)).build());


    FutureCallback<None> factoryShutdown = new FutureCallback<None>();
    factory.shutdown(factoryShutdown);

    FutureCallback<None> clientShutdown = new FutureCallback<None>();
    client.shutdown(clientShutdown);

    // Client and factory shutdowns are now pending.  When we release the latch, the response will
    // be returned, which causes the shutdowns to complete on the Netty IO thread that received the
    // response.
    _testServer.releaseResponseLatch();

    responseFuture.get(60, TimeUnit.SECONDS);
    clientShutdown.get(60, TimeUnit.SECONDS);
    factoryShutdown.get(60, TimeUnit.SECONDS);

    Assert.assertTrue(boss.awaitTermination(60, TimeUnit.SECONDS));
    Assert.assertTrue(worker.awaitTermination(60, TimeUnit.SECONDS));
    Assert.assertTrue(scheduler.awaitTermination(60, TimeUnit.SECONDS));
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
    ExecutorService boss = Executors.newCachedThreadPool();
    ExecutorService worker = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    ClientSocketChannelFactory channelFactory = new NioClientSocketChannelFactory(boss, worker);
    HttpClientFactory factory = new HttpClientFactory(FilterChains.empty(), channelFactory, false, scheduler, false);

    FutureCallback<None> callback = new FutureCallback<None>();
    factory.shutdown(callback, 60, TimeUnit.MINUTES);
    callback.get(60, TimeUnit.SECONDS);
    scheduler.shutdown();
    channelFactory.releaseExternalResources();
    Assert.assertTrue(scheduler.awaitTermination(60, TimeUnit.SECONDS));
    Assert.assertTrue(boss.awaitTermination(60, TimeUnit.SECONDS));
    Assert.assertTrue(worker.awaitTermination(60, TimeUnit.SECONDS));
  }

  @Test
  public void testRequestTimeoutConfig()
  {
    HttpClientFactory factory = new HttpClientFactory();

    try
    {
      Map<String,String> config = new HashMap<String, String>();

      config.put("getTimeout", "999");
      HttpNettyClient client = factory.getRawClient(config);
      Assert.assertEquals(client.getRequestTimeout(), 999);

      config.put("requestTimeout", "999");

      client = factory.getRawClient(config);
      Assert.assertEquals(client.getRequestTimeout(), 999);

      config.put("requestTimeout", "888");
      client = factory.getRawClient(config);
      Assert.assertEquals(client.getRequestTimeout(), 888);

      config.remove("getTimeout");
      client = factory.getRawClient(config);
      Assert.assertEquals(client.getRequestTimeout(), 888);
    }
    finally
    {
      factory.shutdown(Callbacks.<None>empty());
    }

  }

}

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

import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.common.util.None;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

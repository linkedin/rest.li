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

package com.linkedin.d2.balancer.simple;

import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.LoadBalancerState.LoadBalancerStateListenerCallback;
import com.linkedin.d2.balancer.LoadBalancerState.NullStateListenerCallback;
import com.linkedin.d2.balancer.clients.DegraderTrackerClient;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.NullPartitionProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.RangeBasedPartitionProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState.SimpleLoadBalancerStateListener;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerTest;
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.d2.discovery.event.SynchronousExecutorService;
import com.linkedin.d2.discovery.stores.mock.MockStore;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportCallbackAdapter;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionNotTrustedException;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionValidator;
import com.linkedin.util.clock.SystemClock;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class SimpleLoadBalancerStateTest
{
  private ScheduledExecutorService                                                  _executorService;
  private MockStore<UriProperties>                                                 _uriRegistry;
  private MockStore<ClusterProperties>                                             _clusterRegistry;
  private MockStore<ServiceProperties>                                             _serviceRegistry;
  private Map<String, TransportClientFactory>                                      _clientFactories;
  private Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> _loadBalancerStrategyFactories;
  private SimpleLoadBalancerState                                                  _state;
  private SSLContext                                                               _sslContext;
  private SSLParameters _sslParameters;
  private boolean                                                                  isSslEnabled;
  private static final SslSessionValidatorFactory                                  SSL_SESSION_VALIDATOR_FACTORY =
      validationStrings -> sslSession -> {
        if (validationStrings == null || validationStrings.isEmpty())
        {
          throw new SslSessionNotTrustedException("no validation string");
        }
      };
  private static final String CLUSTER1_CLUSTER_NAME = "cluster-1";
  private static final String CLUSTER2_CLUSTER_NAME = "cluster-2";

  public static void main(String[] args) throws Exception
  {
    SimpleLoadBalancerStateTest test = new SimpleLoadBalancerStateTest();

    test.testRegister();
    test.testUnregister();
    test.testShutdown();
    test.testListenToService();
    test.testListenToCluster();
    test.testGetClient();
    test.testGetStrategy();
    test.testRefreshServiceStrategies();
    test.testVersion();
  }

  public void reset()
  {
    reset(false);
  }

  public void reset(boolean useSSL)
  {
    _executorService = new SynchronousExecutorService();
    _uriRegistry = new MockStore<UriProperties>();
    _clusterRegistry = new MockStore<ClusterProperties>();
    _serviceRegistry = new MockStore<ServiceProperties>();
    _clientFactories = new HashMap<String, TransportClientFactory>();
    _loadBalancerStrategyFactories =
        new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();
    _loadBalancerStrategyFactories.put("random", new RandomLoadBalancerStrategyFactory());
    _loadBalancerStrategyFactories.put("degraderV3", new DegraderLoadBalancerStrategyFactoryV3());
    try {
      _sslContext = SSLContext.getDefault();
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new RuntimeException(e);
    }

    _sslParameters = new SSLParameters();

    if (useSSL)
    {
      _clientFactories.put("https", new SimpleLoadBalancerTest.DoNothingClientFactory());
      _state =
          new SimpleLoadBalancerState(_executorService,
              new PropertyEventBusImpl<>(_executorService, _uriRegistry),
              new PropertyEventBusImpl<>(_executorService, _clusterRegistry),
              new PropertyEventBusImpl<>(_executorService, _serviceRegistry),
              _clientFactories,
              _loadBalancerStrategyFactories,
              _sslContext,
              _sslParameters,
            true, null,
              SSL_SESSION_VALIDATOR_FACTORY);
    }
    else
    {
      _clientFactories.put("http", new SimpleLoadBalancerTest.DoNothingClientFactory());
      _state =
        new SimpleLoadBalancerState(_executorService,
                                    _uriRegistry,
                                    _clusterRegistry,
                                    _serviceRegistry,
                                    _clientFactories,
                                    _loadBalancerStrategyFactories);
    }

    FutureCallback<None> callback = new FutureCallback<None>();
    _state.start(callback);
    try
    {
      callback.get();
    }
    catch (Exception e)
    {
      Assert.fail("State start failed", e);
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testRegister()
  {
    reset();

    TestListener listener = new TestListener();
    List<String> schemes = new ArrayList<String>();

    schemes.add("http");
    _state.register(listener);

    assertNull(listener.scheme);
    assertNull(listener.strategy);
    assertNull(listener.serviceName);

    // trigger a strategy add

    // first add a cluster
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());
    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));

    // then add a service
    _state.listenToService("service-1", new NullStateListenerCallback());
    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
                                                            "cluster-1",
                                                            "/test",
                                                            Arrays.asList("random"),
                                                            Collections.<String, Object>emptyMap(),
                                                            null,
                                                            null,
                                                            schemes,
                                                            null));

    // this should trigger a refresh
    assertEquals(listener.scheme, "http");
    assertTrue(listener.strategy instanceof RandomLoadBalancerStrategy);
    assertEquals(listener.serviceName, "service-1");

    // then update the cluster
    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));

    // this triggered a second refresh, but also an onStrategyRemoved. The onStrategyRemoved should
    // be done first, and then the onStrategyAdd, so we should still see a valid strategy.
    assertEquals(listener.scheme, "http");
    assertTrue(listener.strategy instanceof RandomLoadBalancerStrategy);
    assertEquals(listener.serviceName, "service-1");

    _state.listenToCluster("partition-cluster-1", new NullStateListenerCallback());
    _clusterRegistry.put("partition-cluster-1", new ClusterProperties("partition-cluster-1", null,
        new HashMap<String, String>(), new HashSet<URI>(), new RangeBasedPartitionProperties("id=(\\d+)", 0, 100, 2)));

    _state.listenToService("partition-service-1", new NullStateListenerCallback());
    _serviceRegistry.put("partition-service-1",
        new ServiceProperties("partition-service-1",
        "partition-cluster-1", "/partition-test", Arrays.asList("degraderV3"),
        Collections.<String, Object>emptyMap(),
        null,
        null,
        schemes,
        null));

    assertEquals(listener.scheme, "http");
    assertTrue(listener.strategy instanceof DegraderLoadBalancerStrategyV3);

  }

  @Test(groups = { "small", "back-end" })
  public void testUnregister()
  {
    reset();

    TestListener listener = new TestListener();
    List<String> schemes = new ArrayList<String>();

    schemes.add("http");
    _state.register(listener);

    assertNull(listener.scheme);
    assertNull(listener.strategy);
    assertNull(listener.serviceName);

    // trigger a strategy add

    // first add a cluster
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());
    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));

    // then add a service
    _state.listenToService("service-1", new NullStateListenerCallback());
    _serviceRegistry.put("service-1", new ServiceProperties("service-1", "cluster-1", "/test",
                                                            Arrays.asList("random"),
                                                            Collections.<String, Object> emptyMap(),
                                                            null, null,
                                                            schemes, null));

    // this should trigger a refresh
    assertEquals(listener.scheme, "http");
    assertTrue(listener.strategy instanceof RandomLoadBalancerStrategy);
    assertEquals(listener.serviceName, "service-1");

    _state.unregister(listener);

    // then update the cluster
    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));

    // there should be no update here, since we unregistered
    assertEquals(listener.scheme, "http");
    assertTrue(listener.strategy instanceof RandomLoadBalancerStrategy);
    assertEquals(listener.serviceName, "service-1");
  }

  @Test(groups = { "small", "back-end" })
  public void testShutdown() throws URISyntaxException,
      InterruptedException
  {
    reset();

    URI uri = URI.create("http://cluster-1/test");
    TestListener listener = new TestListener();
    List<String> schemes = new ArrayList<String>();
    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
    uriData.put(uri, partitionData);
    schemes.add("http");
    _state.register(listener);

    assertNull(listener.scheme);
    assertNull(listener.strategy);
    assertNull(listener.serviceName);

    // set up state
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());
    _state.listenToService("service-1", new NullStateListenerCallback());
    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1", schemes));
    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));
    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
                                                            "cluster-1",
                                                            "/test",
                                                            Arrays.asList("random")));

    TrackerClient client = _state.getClient("cluster-1", uri);

    TestShutdownCallback callback = new TestShutdownCallback();

    _state.shutdown(callback);

    if (!callback.await(10, TimeUnit.SECONDS))
    {
      fail("unable to shut down state");
    }

    for (TransportClientFactory factory : _clientFactories.values())
    {
      SimpleLoadBalancerTest.DoNothingClientFactory f = (SimpleLoadBalancerTest.DoNothingClientFactory)factory;
      assertEquals(f.getRunningClientCount(), 0, "Not all clients were shut down");
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testShutdownWithListener() throws URISyntaxException,
                                                InterruptedException
  {
    reset();

    URI uri = URI.create("http://cluster-1/test");
    TestListener listener = new TestListener();
    List<String> schemes = new ArrayList<String>();
    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
    uriData.put(uri, partitionData);
    schemes.add("http");
    _state.register(listener);

    assertNull(listener.scheme);
    assertNull(listener.strategy);
    assertNull(listener.serviceName);

    // set up state
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());
    _state.listenToService("service-1", new NullStateListenerCallback());
    List<String> strategyList = Arrays.asList("degraderV3");

    _state.refreshServiceStrategies(
        new ServiceProperties("service-1", "cluster-1", "/test", strategyList, Collections.<String, Object>emptyMap(),
            Collections.<String, Object>emptyMap(),
        Collections.<String, String>emptyMap(),
        schemes,
        Collections.<URI>emptySet()));

    assertEquals(listener.scheme, "http");
    assertNotNull(listener.strategy);
    assertEquals(listener.serviceName, "service-1");

    TrackerClient client = _state.getClient("cluster-1", uri);

    TestShutdownCallback callback = new TestShutdownCallback();

    _state.shutdown(callback);

    if (!callback.await(10, TimeUnit.SECONDS))
    {
      fail("unable to shut down state");
    }

    for (TransportClientFactory factory : _clientFactories.values())
    {
      SimpleLoadBalancerTest.DoNothingClientFactory f = (SimpleLoadBalancerTest.DoNothingClientFactory)factory;
      assertEquals(f.getRunningClientCount(), 0, "Not all clients were shut down");
    }

    assertNull(listener.scheme);
    assertNull(listener.strategy);
    assertNull(listener.serviceName);
  }

  @Test(groups = { "small", "back-end" })
  public void testListenToService() throws InterruptedException
  {
    reset();

    assertFalse(_state.isListeningToService("service-1"));
    assertNull(_state.getServiceProperties("service-1"));

    final CountDownLatch latch = new CountDownLatch(1);
    LoadBalancerStateListenerCallback callback = new LoadBalancerStateListenerCallback()
    {
      @Override
      public void done(int type, String name)
      {
        latch.countDown();
      }
    };

    _state.listenToService("service-1", callback);

    if (!latch.await(5, TimeUnit.SECONDS))
    {
      fail("didn't get callback when listenToService was called");
    }

    assertTrue(_state.isListeningToService("service-1"));
    assertNotNull(_state.getServiceProperties("service-1"));
    assertNull(_state.getServiceProperties("service-1").getProperty());

    ServiceProperties property =
        new ServiceProperties("service-1", "cluster-1", "/test", Arrays.asList("random"));

    _serviceRegistry.put("service-1", property);

    assertTrue(_state.isListeningToService("service-1"));
    assertNotNull(_state.getServiceProperties("service-1"));
    assertEquals(_state.getServiceProperties("service-1").getProperty(), property);
  }

  @Test(groups = { "small", "back-end" })
  public void testListenToCluster() throws URISyntaxException,
      InterruptedException
  {
    reset();

    List<String> schemes = new ArrayList<String>();

    schemes.add("http");

    assertFalse(_state.isListeningToCluster("cluster-1"));
    assertNull(_state.getClusterProperties("cluster-1"));

    final CountDownLatch latch = new CountDownLatch(1);
    LoadBalancerStateListenerCallback callback = new LoadBalancerStateListenerCallback()
    {
      @Override
      public void done(int type, String name)
      {
        latch.countDown();
      }
    };

    _state.listenToCluster("cluster-1", callback);

    if (!latch.await(5, TimeUnit.SECONDS))
    {
      fail("didn't get callback when listenToCluster was called");
    }

    assertTrue(_state.isListeningToCluster("cluster-1"));
    assertNotNull(_state.getClusterProperties("cluster-1"));
    assertNull(_state.getClusterProperties("cluster-1").getProperty());

    ClusterProperties property = new ClusterProperties("cluster-1", schemes);

    _clusterRegistry.put("cluster-1", property);

    assertTrue(_state.isListeningToCluster("cluster-1"));
    assertNotNull(_state.getClusterProperties("cluster-1"));
    assertEquals(_state.getClusterProperties("cluster-1").getProperty(), property);
  }

  @Test(groups = { "small", "back-end" })
  public void testGetClient() throws URISyntaxException
  {
    reset();

    URI uri = URI.create("http://cluster-1/test");
    List<String> schemes = new ArrayList<String>();
    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
    uriData.put(uri, partitionData);

    schemes.add("http");

    assertNull(_state.getClient("service-1", uri));

    // set up state
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());

    assertNull(_state.getClient("service-1", uri));

    _state.listenToService("service-1", new NullStateListenerCallback());

    assertNull(_state.getClient("service-1", uri));

    _serviceRegistry.put("service-1", new ServiceProperties("service-1", "cluster-1",
                                                            "/test", Arrays.asList("random"),
                                                            Collections.<String, Object>emptyMap(),
                                                             null, null, schemes, null));

    assertNull(_state.getClient("service-1", uri));

    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    TrackerClient client = _state.getClient("service-1", uri);

    assertNotNull(client);
    assertEquals(client.getUri(), uri);
  }

  @Test(groups = { "small", "back-end" })
  public void testGetClientWithoutScheme() throws URISyntaxException
  {
    reset();

    URI uri = URI.create("cluster-1/test");
    List<String> schemes = new ArrayList<String>();
    Map<Integer, PartitionData> partitionData = new HashMap<>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<>();
    uriData.put(uri, partitionData);

    schemes.add("http");
    // set up state
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());
    _state.listenToService("service-1", new NullStateListenerCallback());
    _serviceRegistry.put("service-1", new ServiceProperties("service-1", "cluster-1",
        "/test", Arrays.asList("random"), Collections.emptyMap(),
        null, null, schemes, null));
    assertNull(_state.getClient("service-1", uri));

    // the URI without Scheme will get us nothing
    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));
    assertNull(_state.getClient("service-1", uri));

    // correct URI will return the right client
    uri = URI.create("http://cluster-1/test1");
    uriData.put(uri, partitionData);
    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));
    TrackerClient client = _state.getClient("service-1", uri);

    assertNotNull(client);
    assertEquals(client.getUri(), uri);
  }

  @Test(groups = { "small", "back-end" })
  public void testGetStrategy() throws URISyntaxException
  {
    reset();

    URI uri = URI.create("http://cluster-1/test");
    List<String> schemes = new ArrayList<String>();
    Map<URI, Double> weights = new HashMap<URI, Double>();

    weights.put(uri, 1d);
    schemes.add("http");

    assertNull(_state.getStrategy("service-1", "http"));

    // set up state
    _state.listenToService("service-1", new NullStateListenerCallback());

    assertNull(_state.getStrategy("service-1", "http"));

    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
                                                            "cluster-1",
                                                            "/test",
                                                            Arrays.asList("random"),
                                                            Collections.<String, Object>emptyMap(),
                                                            null,
                                                            null,
                                                            schemes,
                                                            null));

    LoadBalancerStrategy strategy = _state.getStrategy("service-1", "http");

    assertNotNull(strategy);
    assertTrue(strategy instanceof RandomLoadBalancerStrategy);
  }

  @Test(groups = { "small", "back-end" })
  public void testRefreshServiceStrategies() throws URISyntaxException, InterruptedException
  {
    reset();

    URI uri = URI.create("http://cluster-1/test");
    List<String> schemes = new ArrayList<String>();
    Map<URI, Double> weights = new HashMap<URI, Double>();

    weights.put(uri, 1d);
    schemes.add("http");

    assertNull(_state.getStrategy("service-1", "http"));

    // set up state
    _state.listenToService("service-1", new NullStateListenerCallback());
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());

    assertNull(_state.getStrategy("service-1", "http"));

    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
                                                            "cluster-1",
                                                            "/test",
                                                            Arrays.asList("random"),
                                                            Collections.<String,Object>emptyMap(),
                                                            null,
                                                            null,
                                                            schemes,
                                                            null));

    assertNotNull(_state.getStrategy("service-1", "http"));

    LoadBalancerStrategy strategy = _state.getStrategy("service-1", "http");

    assertNotNull(strategy);
    assertTrue(strategy instanceof RandomLoadBalancerStrategy);

    // check that we're getting the exact same strategy (by pointer) every time
    assertTrue(strategy == _state.getStrategy("service-1", "http"));
    assertTrue(strategy == _state.getStrategy("service-1", "http"));

    // now make sure adding a cluster property won't change the strategy
    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));

    // we should still have the same strategy now
    LoadBalancerStrategy newStrategy = _state.getStrategy("service-1", "http");
    assertNotNull(newStrategy);
    assertTrue(strategy == newStrategy);
    assertTrue(newStrategy instanceof RandomLoadBalancerStrategy);

    strategy = newStrategy;

    // refresh by adding service
    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
                                                                "cluster-1",
                                                                "/test",
                                                                Arrays.asList("random"),
                                                                Collections.<String,Object>emptyMap(),
                                                                null,
                                                                null,
                                                                schemes,
                                                                null));

    // we should have a different strategy
    newStrategy = _state.getStrategy("service-1", "http");
    assertNotNull(newStrategy);
    assertFalse(strategy == newStrategy);
    assertTrue(newStrategy instanceof RandomLoadBalancerStrategy);

    TestShutdownCallback callback = new TestShutdownCallback();
    _state.shutdown(callback);
    assertTrue(callback.await(10, TimeUnit.SECONDS), "Failed to shut down state");
  }

  @Test(groups = { "small", "back-end" })
  public void testServiceStrategyList() throws URISyntaxException, InterruptedException
  {
    reset();
    LinkedList<String> strategyList = new LinkedList<String>();
    URI uri = URI.create("http://cluster-1/test");
    List<String> schemes = new ArrayList<String>();
    Map<URI, Double> weights = new HashMap<URI, Double>();

    weights.put(uri, 1d);
    schemes.add("http");

    assertNull(_state.getStrategy("service-1", "http"));

    // set up state
    _state.listenToService("service-1", new NullStateListenerCallback());
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());

    assertNull(_state.getStrategy("service-1", "http"));

    // Put degrader into the strategyList, it it not one of the supported strategies in
    // this strategyFactory, so we should not get a strategy back for http.
    strategyList.add("degrader");
    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
                                                            "cluster-1",
                                                            "/test",
                                                            strategyList,
                                                            Collections.<String, Object>emptyMap(),
                                                            null,
                                                            null,
                                                            schemes,
                                                            null));
    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));
    assertNull(_state.getStrategy("service-1", "http"));

    // put the random strategy into the Strategy list, it is one of the supported strategies in the
    // strategyFactory for this unit test
    strategyList.clear();
    strategyList.add("random");
    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
                                                            "cluster-1",
                                                            "/test",
                                                            strategyList,
                                                            Collections.<String, Object>emptyMap(),
                                                            null,
                                                            null,
                                                            schemes,
                                                            null));

    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));

    LoadBalancerStrategy strategy = _state.getStrategy("service-1", "http");

    assertNotNull(strategy);
    assertTrue(strategy instanceof RandomLoadBalancerStrategy);

    // now add the degraderV3 strategy into the Strategy list
    strategyList.addFirst("degraderV3");
    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
                                                            "cluster-1",
                                                            "/test",
                                                            strategyList,
                                                            Collections.<String, Object>emptyMap(),
                                                            null,
                                                            null,
                                                            schemes,
                                                            null));

    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));

    strategy = _state.getStrategy("service-1", "http");

    assertNotNull(strategy);
    assertTrue(strategy instanceof DegraderLoadBalancerStrategyV3);
  }

  // This test is to verify a fix for a specific bug, where the d2 client receives a zookeeper
  // update and concurrent getTrackerClient requests. In that case, all but the first concurrent
  // requests got a null tracker client because the degraderLoadBalancerState was not fully initialized
  // (hashring was empty), and this continued until the first request had atomically swamped a
  // fully initialized state for other requests to use. This test failed on pre-fix code, it now
  // succeeds.
  @Test(groups = { "small", "back-end" })
  public void testRefreshWithConcurrentGetTC() throws URISyntaxException, InterruptedException
  {
    reset();
    LinkedList<String> strategyList = new LinkedList<String>();
    URI uri = URI.create("http://cluster-1/test");
    final List<String> schemes = new ArrayList<String>();

    schemes.add("http");
    strategyList.add("degraderV3");
    // set up state
    _state.listenToService("service-1", new NullStateListenerCallback());
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());

    assertNull(_state.getStrategy("service-1", "http"));

    // Use the _clusterRegistry.put to populate the _state.clusterProperties, used by
    // _state.refreshServiceStrategies
    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));
    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
                                                              "cluster-1",
                                                              "/test",
                                                              strategyList,
                                                              Collections.<String, Object>emptyMap(),
                                                              Collections.<String, Object>emptyMap(),
                                                              Collections.<String, String>emptyMap(),
                                                              schemes,
                                                              Collections.<URI>emptySet()));

    LoadBalancerStrategy strategy = _state.getStrategy("service-1", "http");
    assertNotNull(strategy, "got null strategy in setup");

    // test serial to make sure things are working before concurrent test
    TransportClient resultTC = _state.getClient("service-1", "http");
    assertNotNull(resultTC, "got null tracker client in non-concurrent env");

    ExecutorService myExecutor = Executors.newCachedThreadPool();
    ArrayList<TcCallable> cArray = new ArrayList<TcCallable>();

    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(2);
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    clients.add(new DegraderTrackerClient(uri, partitionDataMap, new DegraderLoadBalancerTest.TestLoadBalancerClient(uri),
                                          SystemClock.instance(), null));

    for (int i = 0; i < 20; i++)
    {
      cArray.add(i, new TcCallable(clients, _state));
    }

    Runnable refreshTask = new Runnable()
    {
      @Override
      public void run()
      {
        while(true)
        {
          List<String> myStrategyList = new LinkedList<String>();
          myStrategyList.add("degraderV3");
          _state.refreshServiceStrategies(new ServiceProperties("service-1",
                                                                "cluster-1",
                                                                "/test",
                                                                myStrategyList,
                                                                Collections.<String, Object>emptyMap(),
                                                                Collections.<String, Object>emptyMap(),
                                                                Collections.<String, String>emptyMap(),
                                                                schemes,
                                                                Collections.<URI>emptySet()));

          if(Thread.interrupted())
          {
            return;
          }
        }
      }
    };

    myExecutor.execute(refreshTask);
    Integer badResults = 0;
    ArrayList<Future<Integer>> myList = new ArrayList<Future<Integer>>();
    for (int i=0; i<cArray.size(); i++)
    {
      @SuppressWarnings("unchecked")
      Callable<Integer> c = (Callable)cArray.get(i);
      myList.add(i, myExecutor.submit(c));
    }

    try
    {
      for (int i=0; i<cArray.size(); i++)
      {
        badResults += myList.get(i).get();
      }
    }
    catch (ExecutionException e)
    {
      Assert.assertFalse(true, "got ExecutionException");
    }
    finally
    {
      try{
        // call shutdownNow() to send an interrupt to the refreshTask
        myExecutor.shutdownNow();
        boolean status = myExecutor.awaitTermination(5, TimeUnit.SECONDS);
        if (status == false)
        {
          Assert.assertFalse(true, "failed to shutdown threads correctly");
        }
      }
      catch (InterruptedException ie)
      {
        // this thread was interrupted
        myExecutor.shutdownNow();
      }
    }
    Assert.assertTrue(badResults == 0, "getTrackerClients returned null");
  }

  private class TcCallable implements Callable<Integer>
  {
    private final List<TrackerClient> _tcList;
    private final SimpleLoadBalancerState _myState;

    public TcCallable(List<TrackerClient> tcList, SimpleLoadBalancerState state)
    {
      _tcList = tcList;
      _myState = state;
    }

    @Override
    public Integer call() throws Exception
    {
      TransportClient trackerClient;
      int badCall = 0;
      for (int i = 0; i < 100; i++)
      {
        trackerClient = _myState.getStrategy("service-1", "http").
                getTrackerClient(null, new RequestContext(), 0, DefaultPartitionAccessor.DEFAULT_PARTITION_ID, _tcList);
        if (trackerClient == null)
        {
          badCall++;
        }
      }
      return badCall;
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testClientsShutdownAfterPropertyUpdatesRestRequest() throws URISyntaxException, InterruptedException
  {
    reset();

    URI uri = URI.create("http://cluster-1/test");
    List<String> schemes = new ArrayList<String>();

    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
    uriData.put(uri, partitionData);
    schemes.add("http");

    // set up state
    _state.listenToService("service-1", new NullStateListenerCallback());
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());
    _state.setDelayedExecution(0);
    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
        "cluster-1",
        "/test",
        Arrays.asList("random"),
        Collections.<String, Object>emptyMap(),
        Collections.<String, Object>emptyMap(),
        Collections.<String, String>emptyMap(),
        schemes,
        Collections.<URI>emptySet()));

    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));


    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    URI uri1 = URI.create("http://partition-cluster-1/test1");
    URI uri2 = URI.create("http://partition-cluster-1/test2");

    _state.listenToCluster("partition-cluster-1", new NullStateListenerCallback());
    _clusterRegistry.put("partition-cluster-1", new ClusterProperties("partition-cluster-1", null,
        new HashMap<String, String>(), new HashSet<URI>(), new RangeBasedPartitionProperties("id=(\\d+)", 0, 100, 2)));

    _state.listenToService("partition-service-1", new NullStateListenerCallback());
    _serviceRegistry.put("partition-service-1",
        new ServiceProperties("partition-service-1",
            "partition-cluster-1", "/partition-test", Arrays.asList("degraderV3"), Collections.<String, Object>emptyMap(),
            Collections.<String, Object>emptyMap(),
            Collections.<String, String>emptyMap(),
            schemes,
            Collections.<URI>emptySet()));

    Map<Integer, PartitionData> partitionWeight = new HashMap<Integer, PartitionData>();
    partitionWeight.put(0, new PartitionData(1d));
    partitionWeight.put(1, new PartitionData(2d));

    Map<URI, Map<Integer, PartitionData>> partitionDesc =
        new HashMap<URI, Map<Integer, PartitionData>>();
    partitionDesc.put(uri1, partitionWeight);

    partitionWeight.remove(0);
    partitionWeight.put(2, new PartitionData(1d));
    partitionDesc.put(uri2, partitionWeight);


    _uriRegistry.put("partition-cluster-1", new UriProperties("partition-cluster-1", partitionDesc));
    TrackerClient client1 = _state.getClient("partition-service-1", uri1);
    TrackerClient client2 = _state.getClient("partition-service-1", uri2);
    assertEquals(client2.getPartitionWeight(1), 2d);
    assertEquals(client2.getPartitionWeight(2), 1d);
    assertEquals(client1.getPartitionWeight(1), 2d);


    // Get client, then refresh cluster
    TrackerClient client = _state.getClient("service-1", uri);
    client.restRequest(new RestRequestBuilder(URI.create("d2://service-1/foo")).build(),
        new RequestContext(),
        Collections.<String, String>emptyMap(),
        new TransportCallbackAdapter<RestResponse>(Callbacks.<RestResponse>empty()));

    // now force a refresh by adding cluster
    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));

    // Get client, then refresh service
    client = _state.getClient("service-1", uri);
    client.restRequest(new RestRequestBuilder(URI.create("d2://service-1/foo")).build(),
        new RequestContext(),
        Collections.<String, String>emptyMap(),
        new TransportCallbackAdapter<RestResponse>(Callbacks.<RestResponse>empty()));

    // refresh by adding service
    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
        "cluster-1",
        "/test",
        Arrays.asList("random"),
        Collections.<String, Object>emptyMap(),
        null,
        null,
        schemes,
        null));

    // Get client, then mark server up/down
    client = _state.getClient("service-1", uri);
    client.restRequest(new RestRequestBuilder(URI.create("d2://service-1/foo")).build(),
        new RequestContext(),
        Collections.<String, String>emptyMap(),
        new TransportCallbackAdapter<RestResponse>(Callbacks.<RestResponse>empty()));

    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", Collections.<URI, Map<Integer, PartitionData>>emptyMap()));

    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    // Get the client one last time
    client = _state.getClient("service-1", uri);
    client.restRequest(new RestRequestBuilder(URI.create("d2://service-1/foo")).build(),
        new RequestContext(),
        Collections.<String, String>emptyMap(),
        new TransportCallbackAdapter<RestResponse>(Callbacks.<RestResponse>empty()));



    TestShutdownCallback callback = new TestShutdownCallback();
    _state.shutdown(callback);
    assertTrue(callback.await(10, TimeUnit.SECONDS), "Failed to shut down state");

    for (TransportClientFactory factory : _clientFactories.values())
    {
      SimpleLoadBalancerTest.DoNothingClientFactory f = (SimpleLoadBalancerTest.DoNothingClientFactory)factory;
      assertEquals(f.getRunningClientCount(), 0, "not all clients were shut down");
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testClientsShutdownAfterPropertyUpdatesStreamRequest() throws URISyntaxException, InterruptedException
  {
    reset();

    URI uri = URI.create("http://cluster-1/test");
    List<String> schemes = new ArrayList<String>();

    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
    uriData.put(uri, partitionData);
    schemes.add("http");

    // set up state
    _state.listenToService("service-1", new NullStateListenerCallback());
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());
    _state.setDelayedExecution(0);
    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
                                                            "cluster-1",
                                                            "/test",
                                                            Arrays.asList("random"),
                                                            Collections.<String, Object>emptyMap(),
                                                            Collections.<String, Object>emptyMap(),
                                                            Collections.<String, String>emptyMap(),
                                                            schemes,
                                                            Collections.<URI>emptySet()));

    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));


    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    URI uri1 = URI.create("http://partition-cluster-1/test1");
    URI uri2 = URI.create("http://partition-cluster-1/test2");

    _state.listenToCluster("partition-cluster-1", new NullStateListenerCallback());
    _clusterRegistry.put("partition-cluster-1", new ClusterProperties("partition-cluster-1", null,
        new HashMap<String, String>(), new HashSet<URI>(), new RangeBasedPartitionProperties("id=(\\d+)", 0, 100, 2)));

    _state.listenToService("partition-service-1", new NullStateListenerCallback());
    _serviceRegistry.put("partition-service-1",
        new ServiceProperties("partition-service-1",
            "partition-cluster-1", "/partition-test", Arrays.asList("degraderV3"), Collections.<String, Object>emptyMap(),
                                                                        Collections.<String, Object>emptyMap(),
                                                                        Collections.<String, String>emptyMap(),
                                                                        schemes,
                                                                        Collections.<URI>emptySet()));

    Map<Integer, PartitionData> partitionWeight = new HashMap<Integer, PartitionData>();
    partitionWeight.put(0, new PartitionData(1d));
    partitionWeight.put(1, new PartitionData(2d));

    Map<URI, Map<Integer, PartitionData>> partitionDesc =
        new HashMap<URI, Map<Integer, PartitionData>>();
    partitionDesc.put(uri1, partitionWeight);

    partitionWeight.remove(0);
    partitionWeight.put(2, new PartitionData(1d));
    partitionDesc.put(uri2, partitionWeight);


    _uriRegistry.put("partition-cluster-1", new UriProperties("partition-cluster-1", partitionDesc));
    TrackerClient client1 = _state.getClient("partition-service-1", uri1);
    TrackerClient client2 = _state.getClient("partition-service-1", uri2);
    assertEquals(client2.getPartitionWeight(1), 2d);
    assertEquals(client2.getPartitionWeight(2), 1d);
    assertEquals(client1.getPartitionWeight(1), 2d);


    // Get client, then refresh cluster
    TrackerClient client = _state.getClient("service-1", uri);
    client.streamRequest(new StreamRequestBuilder(URI.create("d2://service-1/foo")).build(EntityStreams.emptyStream()),
        new RequestContext(),
        Collections.<String, String>emptyMap(),
        new TransportCallbackAdapter<StreamResponse>(Callbacks.<StreamResponse>empty()));

    // now force a refresh by adding cluster
    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));

    // Get client, then refresh service
    client = _state.getClient("service-1", uri);
    client.streamRequest(new StreamRequestBuilder(URI.create("d2://service-1/foo")).build(EntityStreams.emptyStream()),
        new RequestContext(),
        Collections.<String, String>emptyMap(),
        new TransportCallbackAdapter<StreamResponse>(Callbacks.<StreamResponse>empty()));

    // refresh by adding service
    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
                                                            "cluster-1",
                                                            "/test",
                                                            Arrays.asList("random"),
                                                            Collections.<String, Object>emptyMap(),
                                                            null,
                                                            null,
                                                            schemes,
                                                            null));

    // Get client, then mark server up/down
    client = _state.getClient("service-1", uri);
    client.streamRequest(new StreamRequestBuilder(URI.create("d2://service-1/foo")).build(EntityStreams.emptyStream()),
        new RequestContext(),
        Collections.<String, String>emptyMap(),
        new TransportCallbackAdapter<StreamResponse>(Callbacks.<StreamResponse>empty()));

    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", Collections.<URI, Map<Integer, PartitionData>>emptyMap()));

    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    // Get the client one last time
    client = _state.getClient("service-1", uri);
    client.streamRequest(new StreamRequestBuilder(URI.create("d2://service-1/foo")).build(EntityStreams.emptyStream()),
        new RequestContext(),
        Collections.<String, String>emptyMap(),
        new TransportCallbackAdapter<StreamResponse>(Callbacks.<StreamResponse>empty()));



    TestShutdownCallback callback = new TestShutdownCallback();
    _state.shutdown(callback);
    assertTrue(callback.await(10, TimeUnit.SECONDS), "Failed to shut down state");

    for (TransportClientFactory factory : _clientFactories.values())
    {
      SimpleLoadBalancerTest.DoNothingClientFactory f = (SimpleLoadBalancerTest.DoNothingClientFactory)factory;
      assertEquals(f.getRunningClientCount(), 0, "not all clients were shut down");
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testVersion() throws URISyntaxException
  {
    reset();

    int expectedVersion = 0;

    URI uri = URI.create("http://cluster-1/test");
    List<String> schemes = new ArrayList<String>();
    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
    uriData.put(uri, partitionData);
    schemes.add("http");

    // set up state
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());

    // one for uri properties onInit, and one for cluster properties onInit
    expectedVersion += 2;

    assertEquals(_state.getVersion(), expectedVersion);

    _state.listenToService("service-1", new NullStateListenerCallback());

    // one for service onInit
    ++expectedVersion;

    assertEquals(_state.getVersion(), expectedVersion);

    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1", schemes));

    // one for cluster onAdd
    ++expectedVersion;

    assertEquals(_state.getVersion(), expectedVersion);

    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    // one for uri onAdd
    ++expectedVersion;

    assertEquals(_state.getVersion(), expectedVersion);

    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
                                                            "cluster-1",
                                                            "/test",
                                                            Arrays.asList("random")));

    // one for service onAdd
    ++expectedVersion;

    assertEquals(_state.getVersion(), expectedVersion);

    // this shouldn't change the version
    _state.getClient("cluster-1", uri);

    assertEquals(_state.getVersion(), expectedVersion);

    // this shouldn't change the version
    _state.getStrategy("service-1", "http");

    assertEquals(_state.getVersion(), expectedVersion);
  }

  @Test(groups = { "small", "back-end" })
  public void testGetClientWithSSLValidation() throws URISyntaxException
  {
    reset(true);

    URI uri = URI.create("https://cluster-1/test");
    List<String> schemes = new ArrayList<String>();
    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
    uriData.put(uri, partitionData);

    schemes.add("https");

    // set up state
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());
    _state.listenToService("service-1", new NullStateListenerCallback());

    Map<String,Object> transportClientProperties = new HashMap<String,Object>();
    transportClientProperties.put(HttpClientFactory.HTTP_SSL_CONTEXT, _sslContext);
    transportClientProperties.put(HttpClientFactory.HTTP_SSL_PARAMS, _sslParameters);
    transportClientProperties = Collections.unmodifiableMap(transportClientProperties);

    final List<String> sslValidationList = Arrays.asList("validation1", "validation2");
    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1", Collections.emptyList(),
        Collections.emptyMap(), Collections.emptySet(), NullPartitionProperties.getInstance(), sslValidationList,
                                                            (Map<String, Object>)null, false));
    _serviceRegistry.put("service-1", new ServiceProperties("service-1", "cluster-1",
        "/test", Arrays.asList("random"), Collections.<String, Object>emptyMap(),
        transportClientProperties, null, schemes, null));
    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    TrackerClient client = _state.getClient("service-1", uri);
    assertNotNull(client);
    assertEquals(client.getUri(), uri);


    RequestContext requestContext = new RequestContext();
    client.restRequest(new RestRequestBuilder(URI.create("http://cluster-1/test")).build(), requestContext, new HashMap<>(),
        response -> {});
    @SuppressWarnings("unchecked")
    final SslSessionValidator validator = (SslSessionValidator) requestContext.getLocalAttr(R2Constants.REQUESTED_SSL_SESSION_VALIDATOR);
    assertNotNull(validator);
  }

  @Test(groups = { "small", "back-end" })
  public void testGetSSLClient() throws URISyntaxException
  {
    reset(true);

    URI uri = URI.create("https://cluster-1/test");
    List<String> schemes = new ArrayList<String>();
    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
    uriData.put(uri, partitionData);

    schemes.add("https");

    assertNull(_state.getClient("service-1", uri));

    // set up state
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());

    assertNull(_state.getClient("service-1", uri));

    _state.listenToService("service-1", new NullStateListenerCallback());

    assertNull(_state.getClient("service-1", uri));

    Map<String,Object> transportClientProperties = new HashMap<String,Object>();
    transportClientProperties.put(HttpClientFactory.HTTP_SSL_CONTEXT, _sslContext);
    transportClientProperties.put(HttpClientFactory.HTTP_SSL_PARAMS, _sslParameters);
    transportClientProperties = Collections.unmodifiableMap(transportClientProperties);

    _serviceRegistry.put("service-1", new ServiceProperties("service-1", "cluster-1",
                                                            "/test", Arrays.asList("random"),
                                                            Collections.<String, Object>emptyMap(),
                                                            transportClientProperties, null, schemes, null));

    assertNull(_state.getClient("service-1", uri));

    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    TrackerClient client = _state.getClient("service-1", uri);

    assertNotNull(client);
    assertEquals(client.getUri(), uri);
  }

  @Test(groups = { "small", "back-end" })
  public void testSSLDisabledWithHttpsInstances() throws URISyntaxException
  {
    reset();

    URI uri = URI.create("http://cluster-1/test");
    URI httpsUri = URI.create("https://cluster-1/test");
    List<String> schemes = new ArrayList<String>();
    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
    uriData.put(uri, partitionData);
    uriData.put(httpsUri, partitionData);

    schemes.add("https");
    schemes.add("http");


    assertNull(_state.getClient("service-1", uri));

    // set up state
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());

    assertNull(_state.getClient("service-1", uri));

    _state.listenToService("service-1", new NullStateListenerCallback());

    assertNull(_state.getClient("service-1", uri));

    Map<String,Object> transportClientProperties = new HashMap<String,Object>();
    transportClientProperties.put(HttpClientFactory.HTTP_SSL_CONTEXT, _sslContext);
    transportClientProperties.put(HttpClientFactory.HTTP_SSL_PARAMS, _sslParameters);
    transportClientProperties = Collections.unmodifiableMap(transportClientProperties);

    ServiceProperties serviceProperties = new ServiceProperties("service-1", "cluster-1",
                                                                "/test", Arrays.asList("random"),
                                                                Collections.emptyMap(),
                                                                transportClientProperties, null, schemes, null);
    _serviceRegistry.put("service-1", serviceProperties);

    assertNull(_state.getClient("service-1", uri));

    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    List<LoadBalancerState.SchemeStrategyPair> schemeStrategyPairs = _state.getStrategiesForService("service-1", schemes);

    for (LoadBalancerState.SchemeStrategyPair pair : schemeStrategyPairs)
    {
      assertFalse("https".equalsIgnoreCase(pair.getScheme()), "https shouldn't be in any schemeStrategyPair");
    }

    TrackerClient client = _state.getClient("service-1", uri);

    assertNotNull(client);
    assertEquals(client.getUri(), uri);

    // Unfortunately, I don't know a good way to intercept the logs
    client = _state.getClient("service-1", httpsUri);
    assertNull(client, "shouldn't pick an https uri");

    _state.refreshClients(serviceProperties);

  }

  @Test(groups = { "small", "back-end" })
  public void testListValueInTransportClientProperties() throws URISyntaxException
  {
    reset();

    URI uri = URI.create("http://cluster-1/test");
    List<String> schemes = new ArrayList<String>();
    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
    uriData.put(uri, partitionData);

    schemes.add("http");

    assertNull(_state.getClient("service-1", uri));

    // set up state
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());

    assertNull(_state.getClient("service-1", uri));

    _state.listenToService("service-1", new NullStateListenerCallback());

    assertNull(_state.getClient("service-1", uri));

    Map<String,Object> transportClientProperties = new HashMap<String,Object>();

    List<String> allowedClientOverrideKeys = new ArrayList<String>();
    allowedClientOverrideKeys.add(PropertyKeys.HTTP_REQUEST_TIMEOUT);
    allowedClientOverrideKeys.add(PropertyKeys.HTTP_RESPONSE_COMPRESSION_OPERATIONS);
    transportClientProperties.put(PropertyKeys.ALLOWED_CLIENT_OVERRIDE_KEYS, allowedClientOverrideKeys);

    List<String> compressionOperations = new ArrayList<String>();
    compressionOperations.add("get");
    compressionOperations.add("batch_get");
    compressionOperations.add("get_all");
    transportClientProperties.put(PropertyKeys.HTTP_RESPONSE_COMPRESSION_OPERATIONS, compressionOperations);

    transportClientProperties = Collections.unmodifiableMap(transportClientProperties);

    _serviceRegistry.put("service-1", new ServiceProperties("service-1", "cluster-1",
                                                            "/test", Arrays.asList("random"),
                                                            Collections.<String, Object>emptyMap(),
                                                            transportClientProperties, null, schemes, null));

    assertNull(_state.getClient("service-1", uri));

    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    TrackerClient client = _state.getClient("service-1", uri);

    assertNotNull(client);
    assertEquals(client.getUri(), uri);
  }

  @Test(groups = { "small", "back-end" })
  public void testGetClientAfterServiceMetadataChange()
  {
    reset();
    URI uri = URI.create("http://cluster-1/test");
    List<String> schemes = new ArrayList<String>();
    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
    uriData.put(uri, partitionData);

    schemes.add("http");

    assertNull(_state.getClient("service-1", uri));

    // set up state
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());

    assertNull(_state.getClient("service-1", uri));

    _state.listenToService("service-1", new NullStateListenerCallback());

    assertNull(_state.getClient("service-1", uri));

    _serviceRegistry.put("service-1", new ServiceProperties("service-1", "cluster-1",
                                                            "/test", Arrays.asList("random"),
                                                            Collections.<String, Object>emptyMap(),
                                                             null, null, schemes, null));

    assertNull(_state.getClient("service-1", uri));

    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    TrackerClient client = _state.getClient("service-1", uri);

    assertNotNull(client);
    assertEquals(client.getUri(), uri);

    //now we publish an event that tells service-1 changes cluster. Now it's hosted in cluster-2
    _serviceRegistry.put("service-1", new ServiceProperties("service-1", "cluster-2",
                                                            "/test",  Arrays.asList("random"),
                                                            Collections.<String, Object>emptyMap(),
                                                             null, null, schemes, null));

    //this time, since we haven't listened to cluster-2 and there's no uri in cluster-2, we get no client.
    assertNull(_state.getClient("service-1", uri));

    //we shouldn't be affected by any update to cluster-1 uris because now service-1 listen to cluster-2
    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));
    assertNull(_state.getClient("service-1", uri));

    //now let's create URI for cluster 2 and make the state listen to cluster_2
    _state.listenToCluster("cluster-2", new NullStateListenerCallback());

    URI uri2 = URI.create("http://cluster-2/test");
    Map<Integer, PartitionData> partitionData2 = new HashMap<Integer, PartitionData>(1);
    partitionData2.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData2 = new HashMap<URI, Map<Integer, PartitionData>>();
    uriData2.put(uri2, partitionData2);

    //if we start publishing new event to cluster-2 then we should get trackerClient
    _uriRegistry.put("cluster-2", new UriProperties("cluster-2", uriData2));

    client = _state.getClient("service-1", uri2);

    assertNotNull(client);
    assertEquals(client.getUri(), uri2);

    //just to make sure that any event from cluster-1 doesn't affect us anymore
    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));
    client = _state.getClient("service-1", uri2);

    assertNotNull(client);
    assertEquals(client.getUri(), uri2);

    //if we pass uri from cluster-1, we also get no tracker client
    client = _state.getClient("service-1", uri);

    assertNull(client);
  }

  @Test(groups = { "small", "back-end" })
  public void testGetClientAfterBadProperties() throws URISyntaxException, InterruptedException
  {
    reset();

    URI uri = URI.create("http://cluster-1/test");
    List<String> schemes = new ArrayList<String>();
    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
    uriData.put(uri, partitionData);

    schemes.add("http");

    assertNull(_state.getClient("service-1", uri));

    Map<String,Object> transportProperties = new HashMap<String,Object>();
    transportProperties.put("foobar", "unsupportedValue");
    _serviceRegistry.put("service-1", new ServiceProperties("service-1", "cluster-1",
                                                            "/test", Arrays.asList("random"),
                                                            Collections.<String, Object>emptyMap(),
                                                            transportProperties, null, schemes, null));

    // we add the property first before listening to the service because the MockStore will
    // immediately publish to the eventBus when listenToService() is called, whereas the
    // ZooKeeper stores wait until we get a response back from zookeeper, which triggers handlePut.
    CountDownLatch cdl1 = new CountDownLatch(1);
    _state.listenToService("service-1", new SimpleLoadBalancer.SimpleLoadBalancerCountDownCallback(cdl1));

    // Verify the callback did NOT get invoked, i.e., the exception was thrown during handlePut()
    assertEquals(cdl1.getCount(), 1);

    // set up state
    CountDownLatch cdl2 = new CountDownLatch(1);
    _state.listenToCluster("cluster-1", new SimpleLoadBalancer.SimpleLoadBalancerCountDownCallback(cdl2));
    assertTrue(cdl2.await(60, TimeUnit.SECONDS));

    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    assertNull(_state.getClient("service-1", uri));

    _serviceRegistry.put("service-1", new ServiceProperties("service-1", "cluster-1",
                                                            "/test", Arrays.asList("random"),
                                                            Collections.<String, Object>emptyMap(),
                                                            null, null, schemes, null));

    CountDownLatch cdl = new CountDownLatch(1);
    _state.listenToService("service-1", new SimpleLoadBalancer.SimpleLoadBalancerCountDownCallback(cdl));

    assertTrue(cdl.await(60, TimeUnit.SECONDS));

  }

  @Test(groups = {"small", "back-end"})
  public void testUpdatePartitionDataMap()
  {
    reset();
    URI uri = URI.create("http://cluster-1/test");
    List<String> schemes = new ArrayList<String>();
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(1);
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
    uriData.put(uri, partitionDataMap);

    schemes.add("http");
    _state.listenToCluster("cluster-1", new NullStateListenerCallback());
    _state.listenToService("service-1", new NullStateListenerCallback());
    _serviceRegistry.put("service-1", new ServiceProperties("service-1", "cluster-1",
        "/test",  Arrays.asList("random"),
        Collections.<String, Object>emptyMap(),
        null, null, schemes, null));

    // first we announce uri with empty partition data map
    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    TrackerClient client = _state.getClient("service-1", uri);

    assertNotNull(client);
    assertEquals(client.getUri(), uri);
    // tracker client should see empty partition data map
    assertTrue(client.getPartitionDataMap().isEmpty());

    // then we update this uri to have a non-empty partition data map
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    TrackerClient updatedClient = _state.getClient("service-1", uri);

    assertNotNull(updatedClient);
    // should have got a different tracker client
    assertNotSame(client, updatedClient);
    assertEquals(updatedClient.getUri(), uri);
    // this updated client should have updated partition data map
    assertFalse(updatedClient.getPartitionDataMap().isEmpty());
    assertEquals(updatedClient.getPartitionDataMap(), partitionDataMap);

  }

  @Test
  public void testRegisterClusterListener()
  {
    reset();

    MockClusterListener clusterListener = new MockClusterListener();
    _state.registerClusterListener(clusterListener);
    assertEquals(clusterListener.getClusterAddedCount(CLUSTER1_CLUSTER_NAME), 0, "expected zero count since no action has been triggered");

    // first add a cluster
    _state.listenToCluster(CLUSTER1_CLUSTER_NAME, new NullStateListenerCallback());
    _clusterRegistry.put(CLUSTER1_CLUSTER_NAME, new ClusterProperties(CLUSTER1_CLUSTER_NAME));

    assertEquals(clusterListener.getClusterAddedCount(CLUSTER1_CLUSTER_NAME), 1, "expected 1 call after clusterRegistry put");

    // then update the cluster
    _clusterRegistry.put(CLUSTER1_CLUSTER_NAME, new ClusterProperties(CLUSTER1_CLUSTER_NAME));
    assertEquals(clusterListener.getClusterAddedCount(CLUSTER1_CLUSTER_NAME), 2, "expected 2 calls after additional clusterRegistry put");
  }

  @Test
  public void testUnregisterClusterListener()
  {
    reset();

    MockClusterListener clusterListener = new MockClusterListener();
    _state.registerClusterListener(clusterListener);
    assertEquals(clusterListener.getClusterAddedCount(CLUSTER1_CLUSTER_NAME), 0, "expected zero count");

    // first add a cluster
    _state.listenToCluster(CLUSTER1_CLUSTER_NAME, new NullStateListenerCallback());
    _clusterRegistry.put(CLUSTER1_CLUSTER_NAME, new ClusterProperties(CLUSTER1_CLUSTER_NAME));

    assertEquals(clusterListener.getClusterAddedCount(CLUSTER1_CLUSTER_NAME), 1, "expected 1 call after put");

    _state.unregisterClusterListener(clusterListener);
    _clusterRegistry.put(CLUSTER1_CLUSTER_NAME, new ClusterProperties(CLUSTER1_CLUSTER_NAME));
    assertEquals(clusterListener.getClusterAddedCount(CLUSTER1_CLUSTER_NAME), 1, "expected 1 call, since we shouldn't have seen the latest put");
  }

  @Test
  public void testOnRemoveCluster()
  {
    reset();

    MockClusterListener clusterListener = new MockClusterListener();
    _state.registerClusterListener(clusterListener);
    assertEquals(clusterListener.getClusterAddedCount(CLUSTER1_CLUSTER_NAME), 0, "expected zero count");

    // first add a cluster
    _state.listenToCluster(CLUSTER1_CLUSTER_NAME, new NullStateListenerCallback());
    _clusterRegistry.put(CLUSTER1_CLUSTER_NAME, new ClusterProperties(CLUSTER1_CLUSTER_NAME));
    assertEquals(clusterListener.getClusterAddedCount(CLUSTER1_CLUSTER_NAME), 1, "expected 1 call after put");
    assertEquals(clusterListener.getClusterRemovedCount(CLUSTER1_CLUSTER_NAME), 0, "expected nothing yet");

    _clusterRegistry.remove(CLUSTER1_CLUSTER_NAME);
    assertEquals(clusterListener.getClusterRemovedCount(CLUSTER1_CLUSTER_NAME), 1, "expected 1 after remove");
    assertEquals(clusterListener.getClusterAddedCount(CLUSTER1_CLUSTER_NAME), 1, "Nothing more should have been added to the added count");
  }

  @Test
  public void testRegisterClusterListenerDuplicates()
  {
    reset();

    MockClusterListener clusterListener = new MockClusterListener();
    _state.registerClusterListener(clusterListener);
    _state.registerClusterListener(clusterListener);
    _state.listenToCluster(CLUSTER1_CLUSTER_NAME, new NullStateListenerCallback());
    _clusterRegistry.put(CLUSTER1_CLUSTER_NAME, new ClusterProperties(CLUSTER1_CLUSTER_NAME));
    assertEquals(clusterListener.getClusterAddedCount(CLUSTER1_CLUSTER_NAME), 1, "expected 1 call since duplicates are not allowed");

  }

  @Test
  public void testRegisterMultipleClusterListener()
  {
    reset();

    MockClusterListener clusterListener1 = new MockClusterListener();
    _state.registerClusterListener(clusterListener1);
    MockClusterListener clusterListener2 = new MockClusterListener();
    _state.registerClusterListener(clusterListener2);

    _state.listenToCluster(CLUSTER1_CLUSTER_NAME, new NullStateListenerCallback());
    _state.listenToCluster(CLUSTER2_CLUSTER_NAME, new NullStateListenerCallback());

    _clusterRegistry.put(CLUSTER1_CLUSTER_NAME, new ClusterProperties(CLUSTER1_CLUSTER_NAME));
    _clusterRegistry.put(CLUSTER2_CLUSTER_NAME, new ClusterProperties(CLUSTER2_CLUSTER_NAME));
    _clusterRegistry.put(CLUSTER2_CLUSTER_NAME, new ClusterProperties(CLUSTER2_CLUSTER_NAME));

    assertEquals(clusterListener1.getClusterAddedCount(CLUSTER1_CLUSTER_NAME), 1, "expected 1 call for cluster1");
    assertEquals(clusterListener2.getClusterAddedCount(CLUSTER2_CLUSTER_NAME), 2, "expected 2 call for cluster2");
    assertEquals(clusterListener1.getClusterAddedCount(CLUSTER2_CLUSTER_NAME), 2, "expected 1 call for cluster2");
    assertEquals(clusterListener2.getClusterAddedCount(CLUSTER1_CLUSTER_NAME), 1, "expected 1 call for cluster1");
  }

  @Test
  public void testShutdownWithClusterListener() throws URISyntaxException,
                                                       InterruptedException
  {
    reset();
    MockClusterListener clusterListener1 = new MockClusterListener();
    _state.registerClusterListener(clusterListener1);
    MockClusterListener clusterListener2 = new MockClusterListener();
    _state.registerClusterListener(clusterListener2);

    _state.listenToCluster(CLUSTER1_CLUSTER_NAME, new NullStateListenerCallback());
    _state.listenToCluster(CLUSTER2_CLUSTER_NAME, new NullStateListenerCallback());

    assertEquals(clusterListener1.getClusterRemovedCount(CLUSTER1_CLUSTER_NAME), 0, "expected 0 call");
    assertEquals(clusterListener1.getClusterRemovedCount(CLUSTER2_CLUSTER_NAME), 0, "expected 0 call");
    TestShutdownCallback callback = new TestShutdownCallback();

    _state.shutdown(callback);

    if (!callback.await(10, TimeUnit.SECONDS))
    {
      fail("unable to shut down state");
    }

    assertEquals(clusterListener1.getClusterRemovedCount(CLUSTER1_CLUSTER_NAME), 1, "expected 1 call indicating removal on shutdown");
    assertEquals(clusterListener1.getClusterRemovedCount(CLUSTER2_CLUSTER_NAME), 1, "expected 1 call indicating removal on shutdown");
  }

  private static class TestShutdownCallback implements PropertyEventShutdownCallback
  {
    private final CountDownLatch _latch = new CountDownLatch(1);

    @Override
    public void done()
    {
      _latch.countDown();
    }

    public boolean await(long timeout, TimeUnit timeoutUnit) throws InterruptedException
    {
      return _latch.await(timeout, timeoutUnit);
    }
  }
  public static class TestListener implements SimpleLoadBalancerStateListener
  {
    public String               serviceName;
    public String               scheme;
    public LoadBalancerStrategy strategy;

    @Override
    public void onStrategyAdded(String serviceName,
                                String scheme,
                                LoadBalancerStrategy strategy)
    {
      this.serviceName = serviceName;
      this.scheme = scheme;
      this.strategy = strategy;
    }

    @Override
    public void onStrategyRemoved(String serviceName,
                                  String scheme,
                                  LoadBalancerStrategy strategy)
    {
      this.serviceName = null;
      this.scheme = null;
      this.strategy = null;
    }

    @Override
    public void onClientAdded(String serviceName, TrackerClient client)
    {
    }

    @Override
    public void onClientRemoved(String serviceName, TrackerClient client)
    {
    }
  }
}

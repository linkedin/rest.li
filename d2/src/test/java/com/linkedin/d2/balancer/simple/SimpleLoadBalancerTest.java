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


import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.LoadBalancerTestState;
import com.linkedin.d2.balancer.PartitionedLoadBalancerTestState;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.clients.RewriteClient;
import com.linkedin.d2.balancer.clients.RewriteLoadBalancerClient;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.HashBasedPartitionProperties;
import com.linkedin.d2.balancer.properties.NullPartitionProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.RangeBasedPartitionProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.simulator.SimpleLoadBalancerSimulation;
import com.linkedin.d2.balancer.simulator.SimpleLoadBalancerSimulation.PropertyStoreFactory;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.util.FileSystemDirectory;
import com.linkedin.d2.balancer.util.HostToKeyMapper;
import com.linkedin.d2.balancer.util.KeysAndHosts;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.d2.balancer.util.MapKeyResult;
import com.linkedin.d2.balancer.util.URIRequest;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.HashFunction;
import com.linkedin.d2.balancer.util.hashing.MD5Hash;
import com.linkedin.d2.balancer.util.hashing.RandomHash;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessException;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.d2.discovery.event.SynchronousExecutorService;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.mock.MockStore;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.util.NamedThreadFactory;
import com.linkedin.util.degrader.DegraderImpl;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor.DEFAULT_PARTITION_ID;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class SimpleLoadBalancerTest
{
  private static final String CLUSTER1_NAME = "cluster-1";
  private static final String DARK_CLUSTER1_NAME = CLUSTER1_NAME + "-dark";

  private List<File> _dirsToDelete;

  private ScheduledExecutorService _d2Executor;

  @BeforeSuite
  public void initialize()
  {
    _d2Executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("D2 PropertyEventExecutor for Tests"));
  }

  @AfterSuite
  public void shutdown()
  {
    _d2Executor.shutdown();
  }

  public static void main(String[] args) throws ServiceUnavailableException,
      URISyntaxException,
      IOException,
      InterruptedException
  {
    new SimpleLoadBalancerTest().testLoadBalancerWithWait();
    System.err.println("done");
  }

  @BeforeSuite
  public void doOneTimeSetUp()
  {
    _dirsToDelete = new ArrayList<File>();
  }

  @AfterSuite
  public void doOneTimeTearDown() throws IOException
  {
    for (File dirToDelete : _dirsToDelete)
    {
      FileUtils.deleteDirectory(dirToDelete);
    }
  }

  private SimpleLoadBalancer setupLoadBalancer(LoadBalancerState state, MockStore<ServiceProperties> serviceRegistry,
      MockStore<ClusterProperties> clusterRegistry, MockStore<UriProperties> uriRegistry)
      throws ExecutionException, InterruptedException {
    Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        new HashMap<>();
    Map<String, TransportClientFactory> clientFactories = new HashMap<>();
    LoadBalancerState loadBalancerState = state;

    loadBalancerStrategyFactories.put("degrader", new DegraderLoadBalancerStrategyFactoryV3());
    clientFactories.put(PropertyKeys.HTTP_SCHEME, new DoNothingClientFactory());
    clientFactories.put(PropertyKeys.HTTPS_SCHEME, new DoNothingClientFactory());

    if (loadBalancerState == null) {
      loadBalancerState =
          new SimpleLoadBalancerState(new SynchronousExecutorService(), uriRegistry, clusterRegistry, serviceRegistry,
              clientFactories, loadBalancerStrategyFactories);
    }
    SimpleLoadBalancer loadBalancer =
        new SimpleLoadBalancer(loadBalancerState, 5, TimeUnit.SECONDS, _d2Executor);

    FutureCallback<None> balancerCallback = new FutureCallback<None>();
    loadBalancer.start(balancerCallback);
    balancerCallback.get();
    return loadBalancer;
  }

  @DataProvider
  public Object[][] provideKeys()
  {
    return new Object[][] {
        // numHttp, numHttps, expectedNumHttp, expectedNumHttps, partitionIdForAdd, partitionIdForCheck
        {0, 3, 0, 3, 0, 0},
        {3, 0, 3, 0, 0, 0},
        {1, 1, 1, 1, 0, 0},
        {0, 0, 0, 0, 0, 0},
        // alter the partitions to check
        {0, 3, 0, 0, 0, 1},
        {3, 0, 0, 0, 0, 1},
        {1, 1, 0, 0, 0, 2},
        {0, 0, 0, 0, 0, 1},
        // alter the partitions to add and check to match
        {0, 3, 0, 3, 1, 1},
        {3, 0, 3, 0, 1, 1},
        {1, 1, 1, 1, 2, 2},
        {0, 0, 0, 0, 1, 1}
    };
  }

  @Test(dataProvider = "provideKeys")
  public void testClusterInfoProvider(int numHttp, int numHttps, int expectedNumHttp, int expectedNumHttps,
      int partitionIdForAdd, int partitionIdForCheck)
      throws InterruptedException, ExecutionException, ServiceUnavailableException
  {
    MockStore<ServiceProperties> serviceRegistry = new MockStore<>();
    MockStore<ClusterProperties> clusterRegistry = new MockStore<>();
    MockStore<UriProperties> uriRegistry = new MockStore<>();
    SimpleLoadBalancer loadBalancer = setupLoadBalancer(null, serviceRegistry, clusterRegistry, uriRegistry);

    populateUriRegistry(numHttp, numHttps, partitionIdForAdd, uriRegistry);
    clusterRegistry.put(CLUSTER1_NAME, new ClusterProperties(CLUSTER1_NAME));

    Assert.assertEquals(loadBalancer.getClusterCount(CLUSTER1_NAME, PropertyKeys.HTTP_SCHEME, partitionIdForCheck), expectedNumHttp,
        "Http cluster count for partitionId: " + partitionIdForCheck + " should be: " + expectedNumHttp);
    Assert.assertEquals(loadBalancer.getClusterCount(CLUSTER1_NAME, PropertyKeys.HTTPS_SCHEME, partitionIdForCheck), expectedNumHttps,
        "Https cluster count for partitionId: " + partitionIdForCheck + " should be: " + expectedNumHttps);
  }

  private void populateUriRegistry(int numHttp, int numHttps, int partitionIdForAdd, MockStore<UriProperties> uriRegistry)
  {
    Map<Integer, PartitionData> partitionData = new HashMap<>(1);
    partitionData.put(partitionIdForAdd, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>(numHttp);
    Set<String> schemeSet = new HashSet<>();
    schemeSet.add(PropertyKeys.HTTP_SCHEME);
    schemeSet.add(PropertyKeys.HTTPS_SCHEME);
    for (String scheme : schemeSet)
    {
      for (int i = 0; i < (scheme.equals(PropertyKeys.HTTP_SCHEME) ? numHttp : numHttps); i++) {
        uriData.put(URI.create(scheme + "://test.qa" + i + ".com:1234"), partitionData);
      }
    }
    uriRegistry.put(CLUSTER1_NAME, new UriProperties(CLUSTER1_NAME, uriData));
  }
  @Test
  public void testClusterInfoProviderGetDarkClusters()
      throws InterruptedException, ExecutionException, ServiceUnavailableException
  {
    int numHttp = 3;
    int numHttps = 4;
    int partitionIdForAdd = 0;
    MockStore<ServiceProperties> serviceRegistry = new MockStore<>();
    MockStore<ClusterProperties> clusterRegistry = new MockStore<>();
    MockStore<UriProperties> uriRegistry = new MockStore<>();
    SimpleLoadBalancer loadBalancer = setupLoadBalancer(null, serviceRegistry, clusterRegistry, uriRegistry);

    DarkClusterConfig darkClusterConfig = new DarkClusterConfig().setMultiplier(1.0f);
    DarkClusterConfigMap darkClusterConfigMap = new DarkClusterConfigMap();
    darkClusterConfigMap.put(DARK_CLUSTER1_NAME, darkClusterConfig);

    clusterRegistry.put(CLUSTER1_NAME, new ClusterProperties(CLUSTER1_NAME, Collections.emptyList(), Collections.emptyMap(),
        Collections.emptySet(), NullPartitionProperties.getInstance(), Collections.emptyList(), darkClusterConfigMap));

    populateUriRegistry(numHttp, numHttps, partitionIdForAdd, uriRegistry);

    DarkClusterConfigMap returnedDarkClusterConfigMap = loadBalancer.getDarkClusterConfigMap(CLUSTER1_NAME);
    Assert.assertEquals(returnedDarkClusterConfigMap, darkClusterConfigMap, "dark cluster configs should be equal");
    Assert.assertEquals(returnedDarkClusterConfigMap.get(DARK_CLUSTER1_NAME).getMultiplier(), 1.0f, "multiplier should match");
  }

  @Test
  public void testClusterInfoProviderGetDarkClustersNoUris()
      throws InterruptedException, ExecutionException, ServiceUnavailableException
  {
    MockStore<ServiceProperties> serviceRegistry = new MockStore<>();
    MockStore<ClusterProperties> clusterRegistry = new MockStore<>();
    MockStore<UriProperties> uriRegistry = new MockStore<>();
    SimpleLoadBalancer loadBalancer = setupLoadBalancer(null, serviceRegistry, clusterRegistry, uriRegistry);

    DarkClusterConfig darkClusterConfig = new DarkClusterConfig().setMultiplier(1.0f);
    DarkClusterConfigMap darkClusterConfigMap = new DarkClusterConfigMap();
    darkClusterConfigMap.put(DARK_CLUSTER1_NAME, darkClusterConfig);

    clusterRegistry.put(CLUSTER1_NAME, new ClusterProperties(CLUSTER1_NAME, Collections.emptyList(), Collections.emptyMap(),
        Collections.emptySet(), NullPartitionProperties.getInstance(), Collections.emptyList(), darkClusterConfigMap));

    DarkClusterConfigMap returnedDarkClusterConfigMap = loadBalancer.getDarkClusterConfigMap(CLUSTER1_NAME);
    Assert.assertEquals(returnedDarkClusterConfigMap, darkClusterConfigMap, "dark cluster configs should be equal");
    Assert.assertEquals(returnedDarkClusterConfigMap.get(DARK_CLUSTER1_NAME).getMultiplier(), 1.0f, "multiplier should match");
  }

  @Test(groups = { "small", "back-end" })
  public void testLoadBalancerSmoke() throws URISyntaxException,
          ServiceUnavailableException,
          InterruptedException, ExecutionException
  {
    for (int tryAgain = 0; tryAgain < 1000; ++tryAgain)
    {
      Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
          new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();
      Map<String, TransportClientFactory> clientFactories =
          new HashMap<String, TransportClientFactory>();
      List<String> prioritizedSchemes = new ArrayList<String>();

      MockStore<ServiceProperties> serviceRegistry = new MockStore<ServiceProperties>();
      MockStore<ClusterProperties> clusterRegistry = new MockStore<ClusterProperties>();
      MockStore<UriProperties> uriRegistry = new MockStore<UriProperties>();

      ScheduledExecutorService executorService = new SynchronousExecutorService();

      //loadBalancerStrategyFactories.put("rr", new RandomLoadBalancerStrategyFactory());
      loadBalancerStrategyFactories.put("degrader", new DegraderLoadBalancerStrategyFactoryV3());
      // PrpcClientFactory();
      clientFactories.put(PropertyKeys.HTTP_SCHEME, new DoNothingClientFactory()); // new
      // HttpClientFactory();

      SimpleLoadBalancerState state =
          new SimpleLoadBalancerState(executorService,
                                      uriRegistry,
                                      clusterRegistry,
                                      serviceRegistry,
                                      clientFactories,
                                      loadBalancerStrategyFactories);

      SimpleLoadBalancer loadBalancer =
        new SimpleLoadBalancer(state, 5, TimeUnit.SECONDS, _d2Executor);

      FutureCallback<None> balancerCallback = new FutureCallback<None>();
      loadBalancer.start(balancerCallback);
      balancerCallback.get();

      URI uri1 = URI.create("http://test.qa1.com:1234");
      URI uri2 = URI.create("http://test.qa2.com:2345");
      URI uri3 = URI.create("http://test.qa3.com:6789");

      Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
      partitionData.put(DEFAULT_PARTITION_ID, new PartitionData(1d));
      Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>(3);
      uriData.put(uri1, partitionData);
      uriData.put(uri2, partitionData);
      uriData.put(uri3, partitionData);

      prioritizedSchemes.add(PropertyKeys.HTTP_SCHEME);

      clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));

      serviceRegistry.put("foo", new ServiceProperties("foo",
                                                        "cluster-1",
                                                        "/foo",
                                                        Arrays.asList("degrader"),
                                                        Collections.<String,Object>emptyMap(),
                                                        null,
                                                        null,
                                                        prioritizedSchemes,
                                                        null));
      uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

      URI expectedUri1 = URI.create("http://test.qa1.com:1234/foo");
      URI expectedUri2 = URI.create("http://test.qa2.com:2345/foo");
      URI expectedUri3 = URI.create("http://test.qa3.com:6789/foo");

      Set<URI> expectedUris = new HashSet<URI>();

      expectedUris.add(expectedUri1);
      expectedUris.add(expectedUri2);
      expectedUris.add(expectedUri3);

      for (int i = 0; i < 100; ++i)
      {
        RewriteLoadBalancerClient client =
            (RewriteLoadBalancerClient) loadBalancer.getClient(new URIRequest("d2://foo/52"),
                                                   new RequestContext());

        assertTrue(expectedUris.contains(client.getUri()));
        assertEquals(client.getUri().getScheme(), PropertyKeys.HTTP_SCHEME);
      }

      final CountDownLatch latch = new CountDownLatch(1);
      PropertyEventShutdownCallback callback = new PropertyEventShutdownCallback()
      {
        @Override
        public void done()
        {
          latch.countDown();
        }
      };

      state.shutdown(callback);

      if (!latch.await(60, TimeUnit.SECONDS))
      {
        fail("unable to shutdown state");
      }

      executorService.shutdownNow();

      assertTrue(executorService.isShutdown(), "ExecutorService should have shut down!");
    }
  }

  @Test
  public void testGetClientWithBannedURI() throws Exception
  {
    Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();
    Map<String, TransportClientFactory> clientFactories = new HashMap<String, TransportClientFactory>();
    List<String> prioritizedSchemes = new ArrayList<String>();

    MockStore<ServiceProperties> serviceRegistry = new MockStore<ServiceProperties>();
    MockStore<ClusterProperties> clusterRegistry = new MockStore<ClusterProperties>();
    MockStore<UriProperties> uriRegistry = new MockStore<UriProperties>();

    ScheduledExecutorService executorService = new SynchronousExecutorService();

    loadBalancerStrategyFactories.put("degrader", new DegraderLoadBalancerStrategyFactoryV3());
    clientFactories.put(PropertyKeys.HTTP_SCHEME, new DoNothingClientFactory());

    SimpleLoadBalancerState state =
        new SimpleLoadBalancerState(executorService,
            uriRegistry,
            clusterRegistry,
            serviceRegistry,
            clientFactories,
            loadBalancerStrategyFactories);

    SimpleLoadBalancer loadBalancer =
      new SimpleLoadBalancer(state, 5, TimeUnit.SECONDS, _d2Executor);

    FutureCallback<None> balancerCallback = new FutureCallback<None>();
    loadBalancer.start(balancerCallback);
    balancerCallback.get();

    URI uri1Banned = URI.create("http://test.qd.com:1234");
    URI uri2Usable = URI.create("http://test.qd.com:5678");
    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>(2);
    uriData.put(uri1Banned, partitionData);
    uriData.put(uri2Usable, partitionData);

    prioritizedSchemes.add(PropertyKeys.HTTP_SCHEME);

    Set<URI> bannedSet = new HashSet<>();
    bannedSet.add(uri1Banned);
    clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1", Collections.emptyList(),
        Collections.emptyMap(), bannedSet, NullPartitionProperties.getInstance()));

    serviceRegistry.put("foo", new ServiceProperties("foo",
        "cluster-1",
        "/foo",
        Arrays.asList("degrader"),
        Collections.<String,Object>emptyMap(),
        null,
        null,
        prioritizedSchemes,
        null));
    uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    URI expectedUri = URI.create("http://test.qd.com:5678/foo");
    URIRequest uriRequest = new URIRequest("d2://foo/52");
    for (int i = 0; i < 10; ++i)
    {
      RewriteLoadBalancerClient client =
          (RewriteLoadBalancerClient) loadBalancer.getClient(uriRequest, new RequestContext());
      Assert.assertEquals(client.getUri(), expectedUri);
    }
  }

  /**
   * This tests getClient(). When TargetHints and scheme does not match, throw ServiceUnavailableException
   * @throws Exception
   */
  @Test (expectedExceptions = ServiceUnavailableException.class)
  public void testGetClient() throws Exception
  {

    Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();
    Map<String, TransportClientFactory> clientFactories =
        new HashMap<String, TransportClientFactory>();
    List<String> prioritizedSchemes = new ArrayList<String>();

    MockStore<ServiceProperties> serviceRegistry = new MockStore<ServiceProperties>();
    MockStore<ClusterProperties> clusterRegistry = new MockStore<ClusterProperties>();
    MockStore<UriProperties> uriRegistry = new MockStore<UriProperties>();

    ScheduledExecutorService executorService = new SynchronousExecutorService();

    //loadBalancerStrategyFactories.put("rr", new RandomLoadBalancerStrategyFactory());
    loadBalancerStrategyFactories.put("degrader", new DegraderLoadBalancerStrategyFactoryV3());
    // PrpcClientFactory();
    clientFactories.put(PropertyKeys.HTTPS_SCHEME, new DoNothingClientFactory()); // new
    // HttpClientFactory();

    SimpleLoadBalancerState state =
        new SimpleLoadBalancerState(executorService,
            uriRegistry,
            clusterRegistry,
            serviceRegistry,
            clientFactories,
            loadBalancerStrategyFactories);

    SimpleLoadBalancer loadBalancer =
      new SimpleLoadBalancer(state, 5, TimeUnit.SECONDS, _d2Executor);

    FutureCallback<None> balancerCallback = new FutureCallback<None>();
    loadBalancer.start(balancerCallback);
    balancerCallback.get(5, TimeUnit.SECONDS);

    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>(3);

    prioritizedSchemes.add(PropertyKeys.HTTPS_SCHEME);

    clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));

    serviceRegistry.put("foo", new ServiceProperties("foo",
        "cluster-1",
        "/foo",
        Arrays.asList("degrader"),
        Collections.<String,Object>emptyMap(),
        null,
        null,
        prioritizedSchemes,
        null));
    uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));



    URI uri = URI.create("http://test.qd.com:1234/foo");

    RequestContext requestContextWithHint = new RequestContext();
    LoadBalancerUtil.TargetHints.setRequestContextTargetService(requestContextWithHint, uri);

    URIRequest uriRequest = new URIRequest("d2://foo");
    loadBalancer.getClient(uriRequest, requestContextWithHint);
  }

  /**
   * This tests the getPartitionInfo() when given a collection of keys (actually a test for KeyMapper.mapKeysV3()).
   */
  @Test
  public void testGetPartitionInfoOrdering()
    throws Exception
  {
    String serviceName = "articles";
    String clusterName = "cluster";
    String path = "path";
    String strategyName = "degrader";

    // setup 3 partitions. Partition 1 and Partition 2 both have server1 - server3. Partition 3 only has server1.
    Map<URI,Map<Integer, PartitionData>> partitionDescriptions = new HashMap<URI, Map<Integer, PartitionData>>();

    final URI server1 = new URI("http://foo1.com");
    Map<Integer, PartitionData> server1Data = new HashMap<Integer, PartitionData>();
    server1Data.put(1, new PartitionData(1.0));
    server1Data.put(2, new PartitionData(1.0));
    server1Data.put(3, new PartitionData(1.0));
    partitionDescriptions.put(server1, server1Data);

    final URI server2 = new URI("http://foo2.com");
    Map<Integer, PartitionData> server2Data = new HashMap<Integer, PartitionData>();
    server2Data.put(1, new PartitionData(1.0));
    server2Data.put(2, new PartitionData(1.0));
    partitionDescriptions.put(server2, server2Data);

    final URI server3 = new URI("http://foo3.com");
    Map<Integer, PartitionData> server3Data = new HashMap<Integer, PartitionData>();
    server3Data.put(1, new PartitionData(1.0));
    server3Data.put(2, new PartitionData(1.0));
    partitionDescriptions.put(server3, server3Data);

    //setup strategy which involves tweaking the hash ring to get partitionId -> URI host
    List<LoadBalancerState.SchemeStrategyPair> orderedStrategies = new ArrayList<LoadBalancerState.SchemeStrategyPair>();
    LoadBalancerStrategy strategy = new TestLoadBalancerStrategy(partitionDescriptions);

    orderedStrategies.add(new LoadBalancerState.SchemeStrategyPair(PropertyKeys.HTTP_SCHEME, strategy));

    //setup the partition accessor which can only map keys from 1 - 3.
    PartitionAccessor accessor = new TestPartitionAccessor();

    URI serviceURI = new URI("d2://" + serviceName);
    SimpleLoadBalancer balancer = new SimpleLoadBalancer(new PartitionedLoadBalancerTestState(
      clusterName, serviceName, path, strategyName, partitionDescriptions, orderedStrategies,
      accessor
    ), _d2Executor);

    List<Integer> keys = new ArrayList<Integer>();
    keys.add(1);
    keys.add(2);
    keys.add(3);
    keys.add(123);

    HostToKeyMapper<Integer> result = balancer.getPartitionInformation(serviceURI, keys, 3, 123);

    Assert.assertEquals(result.getLimitHostPerPartition(), 3);

    Assert.assertEquals(1, result.getUnmappedKeys().size());
    Assert.assertEquals(123, (int)result.getUnmappedKeys().iterator().next().getKey());

    //partition 0 should be null
    Assert.assertNull(result.getPartitionInfoMap().get(0));
    // results for partition 1 should contain server1, server2 and server3
    KeysAndHosts<Integer> keysAndHosts1 = result.getPartitionInfoMap().get(1);
    Assert.assertTrue(keysAndHosts1.getKeys().size() == 1);
    Assert.assertTrue(keysAndHosts1.getKeys().iterator().next() == 1);
    List<URI> ordering1 = keysAndHosts1.getHosts();
    // results for partition 2 should be the same as partition1.
    KeysAndHosts<Integer> keysAndHosts2 = result.getPartitionInfoMap().get(2);
    Assert.assertTrue(keysAndHosts2.getKeys().size() == 1);
    Assert.assertTrue(keysAndHosts2.getKeys().iterator().next() == 2);
    List<URI> ordering2 = keysAndHosts2.getHosts();
    //for partition 3
    KeysAndHosts<Integer> keysAndHosts3 = result.getPartitionInfoMap().get(3);
    Assert.assertTrue(keysAndHosts3.getKeys().size() == 1);
    Assert.assertTrue(keysAndHosts3.getKeys().iterator().next() == 3);
    List<URI> ordering3 = keysAndHosts3.getHosts();

    // Just compare the size and contents of the list, not the ordering.
    Assert.assertTrue(ordering1.size() == 3);
    List<URI> allServers = new ArrayList<>();
    allServers.add(server1);
    allServers.add(server2);
    allServers.add(server3);
    Assert.assertTrue(ordering1.containsAll(allServers));
    Assert.assertTrue(ordering2.containsAll(allServers));
    Assert.assertEquals(ordering1, ordering2);
    Assert.assertEquals(ordering3.get(0), server1);

    Assert.assertTrue(result.getPartitionsWithoutEnoughHosts().containsKey(3));
    Assert.assertEquals((int)result.getPartitionsWithoutEnoughHosts().get(3), 2);
  }

  private static Map<Integer, PartitionData> generatePartitionData(Integer... partitions)
  {
    Map<Integer, PartitionData> server1Data = new HashMap<>();
    Arrays.asList(partitions).forEach(partitionId -> server1Data.put(partitionId, new PartitionData(1.0)));
    return server1Data;
  }

  private static <T> Set<T> iteratorToSet(Iterator<T> iterator)
  {
    return StreamSupport.stream(
      Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
      false).collect(Collectors.toSet());
  }

  /**
   * Test falling back of strategy if partition can't be found in the original one
   */
  @Test
  public void testStrategyFallbackInGetPartitionInformationAndRing() throws Exception
  {
    // setup 3 partitions. Partition 1 and Partition 2 both have server1 - server3. Partition 3 only has server1.

    // create HTTP strategy
    Map<URI, Map<Integer, PartitionData>> partitionDescriptionsPlain = new HashMap<>();
    final URI server1Plain = new URI("http://foo1.com");
    partitionDescriptionsPlain.put(server1Plain, generatePartitionData(1, 2, 3));
    LoadBalancerStrategy plainStrategy = new TestLoadBalancerStrategy(partitionDescriptionsPlain);

    // create HTTPS strategy
    Map<URI, Map<Integer, PartitionData>> partitionDescriptionsSSL = new HashMap<>();
    final URI server2Https = new URI("https://foo2.com");
    partitionDescriptionsSSL.put(server2Https, generatePartitionData(1, 2));

    final URI server3Https = new URI("https://foo3.com");
    partitionDescriptionsSSL.put(server3Https, generatePartitionData(1, 2));
    LoadBalancerStrategy SSLStrategy = new TestLoadBalancerStrategy(partitionDescriptionsSSL);

    // Prioritize HTTPS over HTTP
    List<LoadBalancerState.SchemeStrategyPair> orderedStrategies = new ArrayList<>();
    orderedStrategies.add(new LoadBalancerState.SchemeStrategyPair(PropertyKeys.HTTPS_SCHEME, SSLStrategy));
    orderedStrategies.add(new LoadBalancerState.SchemeStrategyPair(PropertyKeys.HTTP_SCHEME, plainStrategy));

    // setup the partition accessor which can only map keys from 1 - 3.
    PartitionAccessor accessor = new TestPartitionAccessor();

    HashMap<URI, Map<Integer, PartitionData>> allUris = new HashMap<>();
    allUris.putAll(partitionDescriptionsSSL);
    allUris.putAll(partitionDescriptionsPlain);

    String serviceName = "articles";
    String clusterName = "cluster";
    String path = "path";
    String strategyName = "degrader";
    URI serviceURI = new URI("d2://" + serviceName);
    SimpleLoadBalancer balancer = new SimpleLoadBalancer(new PartitionedLoadBalancerTestState(
      clusterName, serviceName, path, strategyName, allUris, orderedStrategies,
      accessor
    ), _d2Executor);

    List<Integer> keys = Arrays.asList(1, 2, 3, 123);
    HostToKeyMapper<Integer> resultPartInfo = balancer.getPartitionInformation(serviceURI, keys, 3, 123);
    MapKeyResult<Ring<URI>, Integer> resultRing = balancer.getRings(serviceURI, keys);
    Assert.assertEquals(resultPartInfo.getLimitHostPerPartition(), 3);
    Assert.assertEquals(resultRing.getMapResult().size(), 3);

    Map<Integer, Ring<URI>> ringPerKeys = new HashMap<>();
    resultRing.getMapResult().forEach((uriRing, keysAssociated) -> keysAssociated.forEach(key -> ringPerKeys.put(key, uriRing)));

    // Important section

    // partition 1 and 2
    List<URI> ordering1 = resultPartInfo.getPartitionInfoMap().get(1).getHosts();
    Set<URI> ordering1Ring = iteratorToSet(ringPerKeys.get(1).getIterator(0));

    List<URI> ordering2 = resultPartInfo.getPartitionInfoMap().get(2).getHosts();
    Set<URI> ordering2Ring = iteratorToSet(ringPerKeys.get(2).getIterator(0));

    // partition 1 and 2. check that the HTTPS hosts are there
    // all the above variables should be the same, since all the hosts are in both partitions
    Assert.assertEqualsNoOrder(ordering1.toArray(), ordering2.toArray());
    Assert.assertEqualsNoOrder(ordering1.toArray(), ordering1Ring.toArray());
    Assert.assertEqualsNoOrder(ordering1.toArray(), ordering2Ring.toArray());
    Assert.assertEqualsNoOrder(ordering1.toArray(), Arrays.asList(server2Https, server3Https).toArray());


    // partition 3, test that is falling back to HTTP
    List<URI> ordering3 = resultPartInfo.getPartitionInfoMap().get(3).getHosts();
    Set<URI> ordering3Ring = iteratorToSet(ringPerKeys.get(3).getIterator(0));

    Assert.assertEquals(ordering3.size(), 1, "There should be just 1 http client in partition 3 (falling back from https)");
    Assert.assertEqualsNoOrder(ordering3.toArray(), ordering3Ring.toArray());
    Assert.assertEquals(ordering3.get(0), server1Plain);
  }

  /**
   * This tests the getPartitionInfo() when keys are null (actually a test for KeyMapper.getAllPartitionMultipleHosts()).
   */
  @Test
  public void testGetAllPartitionMultipleHostsOrdering()
      throws Exception
  {
    String serviceName = "articles";
    String clusterName = "cluster";
    String path = "path";
    String strategyName = "degrader";

    //setup partition
    Map<URI,Map<Integer, PartitionData>> partitionDescriptions = new HashMap<URI, Map<Integer, PartitionData>>();

    final URI server1 = new URI("http://foo1.com");
    Map<Integer, PartitionData> server1Data = new HashMap<Integer, PartitionData>();
    server1Data.put(1, new PartitionData(1.0));
    server1Data.put(2, new PartitionData(1.0));
    server1Data.put(3, new PartitionData(1.0));
    partitionDescriptions.put(server1, server1Data);

    final URI server2 = new URI("http://foo2.com");
    Map<Integer, PartitionData> server2Data = new HashMap<Integer, PartitionData>();
    server2Data.put(1, new PartitionData(1.0));
    server2Data.put(2, new PartitionData(1.0));
    //server2Data.put(3, new PartitionData(1.0));
    partitionDescriptions.put(server2, server2Data);

    final URI server3 = new URI("http://foo3.com");
    Map<Integer, PartitionData> server3Data = new HashMap<Integer, PartitionData>();
    server3Data.put(1, new PartitionData(1.0));
    server3Data.put(2, new PartitionData(1.0));
    //server3Data.put(3, new PartitionData(1.0));
    partitionDescriptions.put(server3, server3Data);

    //setup strategy which involves tweaking the hash ring to get partitionId -> URI host
    List<LoadBalancerState.SchemeStrategyPair> orderedStrategies = new ArrayList<LoadBalancerState.SchemeStrategyPair>();
    LoadBalancerStrategy strategy = new TestLoadBalancerStrategy(partitionDescriptions);

    orderedStrategies.add(new LoadBalancerState.SchemeStrategyPair(PropertyKeys.HTTP_SCHEME, strategy));

    //setup the partition accessor which is used to get partitionId -> keys
    PartitionAccessor accessor = new TestPartitionAccessor();

    URI serviceURI = new URI("d2://" + serviceName);
    SimpleLoadBalancer balancer = new SimpleLoadBalancer(new PartitionedLoadBalancerTestState(
            clusterName, serviceName, path, strategyName, partitionDescriptions, orderedStrategies,
            accessor
    ), _d2Executor);

    HostToKeyMapper<URI> result = balancer.getPartitionInformation(serviceURI, null, 3, 123);

    Assert.assertEquals(result.getPartitionInfoMap().size(), 4);
    Assert.assertEquals(4, result.getPartitionCount());
    // partition 0 should be empty
    Assert.assertTrue(result.getPartitionInfoMap().get(0).getHosts().isEmpty());
    // partition 1 should have server1, server2 and server3.
    List<URI> ordering1 = result.getPartitionInfoMap().get(1).getHosts();

    List<URI> allServers = new ArrayList<>();
    allServers.add(server1);
    allServers.add(server2);
    allServers.add(server3);

    Assert.assertTrue(ordering1.size() == 3);
    Assert.assertTrue(ordering1.containsAll(allServers));

    // partition 2 should be the same as partition 1
    List<URI> ordering2 = result.getPartitionInfoMap().get(2).getHosts();
    Assert.assertEquals(ordering1, ordering2);
    // partition 3 should only contain server1
    List<URI> ordering3 = result.getPartitionInfoMap().get(3).getHosts();
    Assert.assertEquals(ordering3.get(0), server1);

    // partition 0 and partition 3 should not have enough hosts: lacking 3 and 2 respectively.
    Assert.assertTrue(result.getPartitionsWithoutEnoughHosts().containsKey(3));
    Assert.assertTrue(result.getPartitionsWithoutEnoughHosts().containsKey(0));
    Assert.assertEquals((int)result.getPartitionsWithoutEnoughHosts().get(3), 2);
    Assert.assertEquals((int)result.getPartitionsWithoutEnoughHosts().get(0), 3);
  }

  // load balancer working with partitioned cluster
  @Test(groups = { "small", "back-end" })
  public void testLoadBalancerWithPartitionsSmoke() throws URISyntaxException,
      ServiceUnavailableException,
      InterruptedException, ExecutionException
  {
    for (int tryAgain = 0; tryAgain < 12; ++tryAgain)
    {
      Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
          new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();
      Map<String, TransportClientFactory> clientFactories =
          new HashMap<String, TransportClientFactory>();
      List<String> prioritizedSchemes = new ArrayList<String>();

      MockStore<ServiceProperties> serviceRegistry = new MockStore<ServiceProperties>();
      MockStore<ClusterProperties> clusterRegistry = new MockStore<ClusterProperties>();
      MockStore<UriProperties> uriRegistry = new MockStore<UriProperties>();

      ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
      loadBalancerStrategyFactories.put("degrader", new DegraderLoadBalancerStrategyFactoryV3());

      clientFactories.put(PropertyKeys.HTTP_SCHEME, new DoNothingClientFactory());

      SimpleLoadBalancerState state =
          new SimpleLoadBalancerState(executorService,
              uriRegistry,
              clusterRegistry,
              serviceRegistry,
              clientFactories,
              loadBalancerStrategyFactories);

      SimpleLoadBalancer loadBalancer =
        new SimpleLoadBalancer(state, 5, TimeUnit.SECONDS, executorService);

      FutureCallback<None> balancerCallback = new FutureCallback<None>();
      loadBalancer.start(balancerCallback);
      balancerCallback.get();

      URI uri1 = URI.create("http://test.qa1.com:1234");
      URI uri2 = URI.create("http://test.qa2.com:2345");
      URI uri3 = URI.create("http://test.qa3.com:6789");

      Map<URI, Double> uris = new HashMap<URI, Double>();

      uris.put(uri1, 1d);
      uris.put(uri2, 1d);
      uris.put(uri3, 1d);

      Map<URI,Map<Integer, PartitionData>> partitionDesc =
          new HashMap<URI, Map<Integer, PartitionData>>();

      Map<Integer, PartitionData> server1 = new HashMap<Integer, PartitionData>();
      server1.put(0, new PartitionData(1d));
      server1.put(1, new PartitionData(1d));

      Map<Integer, PartitionData> server2 = new HashMap<Integer, PartitionData>();
      server2.put(0, new PartitionData(1d));

      Map<Integer, PartitionData> server3 = new HashMap<Integer, PartitionData>();
      server3.put(1, new PartitionData(1d));
      partitionDesc.put(uri1, server1);
      partitionDesc.put(uri2, server2);
      partitionDesc.put(uri3, server3);

      prioritizedSchemes.add(PropertyKeys.HTTP_SCHEME);

      int partitionMethod = tryAgain % 4;
      switch (partitionMethod)
      {
        case 0:
          clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1", null, new HashMap<String, String>(),
            new HashSet<URI>(), new RangeBasedPartitionProperties("id=(\\d+)", 0, 50, 2)));
          break;
        case 1:
          clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1", null, new HashMap<String, String>(),
              new HashSet<URI>(), new HashBasedPartitionProperties("id=(\\d+)", 2, HashBasedPartitionProperties.HashAlgorithm.valueOf("MODULO"))));
          break;
        case 2:
          clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1", null, new HashMap<String, String>(),
              new HashSet<URI>(), new HashBasedPartitionProperties("id=(\\d+)", 2, HashBasedPartitionProperties.HashAlgorithm.valueOf("MD5"))));
          break;
        case 3:
          // test getRings with gap. here, no server serves partition 2
          clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1", null, new HashMap<String, String>(),
              new HashSet<URI>(), new RangeBasedPartitionProperties("id=(\\d+)", 0, 50, 4)));
          server3.put(3, new PartitionData(1d));
          partitionDesc.put(uri3, server3);
          break;
        default: break;
      }


      serviceRegistry.put("foo", new ServiceProperties("foo",
                                                        "cluster-1",
                                                        "/foo",
                                                        Arrays.asList("degrader"),
                                                        Collections.singletonMap(PropertyKeys.HTTP_LB_CONSISTENT_HASH_ALGORITHM, "pointBased"),
                                                        null,
                                                        null,
                                                        prioritizedSchemes,
                                                        null));

      uriRegistry.put("cluster-1", new UriProperties("cluster-1", partitionDesc));

      if (partitionMethod == 3)
      {
        Map<Integer, Ring<URI>> ringMap = loadBalancer.getRings(URI.create("d2://foo"));
        assertEquals(ringMap.size(), 4);
        // the ring for partition 2 should be empty
        assertEquals(ringMap.get(2).toString(), new ConsistentHashRing<URI>(Collections.emptyList()).toString());
        continue;
      }

      URI expectedUri1 = URI.create("http://test.qa1.com:1234/foo");
      URI expectedUri2 = URI.create("http://test.qa2.com:2345/foo");
      URI expectedUri3 = URI.create("http://test.qa3.com:6789/foo");

      Set<URI> expectedUris = new HashSet<URI>();
      expectedUris.add(expectedUri1);
      expectedUris.add(expectedUri2);
      expectedUris.add(expectedUri3);

      for (int i = 0; i < 1000; ++i)
      {
        int ii = i % 100;
        RewriteLoadBalancerClient client =
            (RewriteLoadBalancerClient) loadBalancer.getClient(new URIRequest("d2://foo/id=" + ii), new RequestContext());
        String clientUri = client.getUri().toString();
        HashFunction<String[]> hashFunction = null;
        String[] str = new String[1];

        // test KeyMapper target host hint: request is always to target host regardless of what's in d2 URI and whether it's hash-based or range-based partitions
        RequestContext requestContextWithHint = new RequestContext();
        KeyMapper.TargetHostHints.setRequestContextTargetHost(requestContextWithHint, uri1);
        RewriteLoadBalancerClient
            hintedClient1 = (RewriteLoadBalancerClient)loadBalancer.getClient(new URIRequest("d2://foo/id=" + ii), requestContextWithHint);
        String hintedUri1 = hintedClient1.getUri().toString();
        Assert.assertEquals(hintedUri1, uri1.toString() + "/foo");
        RewriteLoadBalancerClient hintedClient2 = (RewriteLoadBalancerClient)loadBalancer.getClient(new URIRequest("d2://foo/action=purge-all"), requestContextWithHint);
        String hintedUri2 = hintedClient2.getUri().toString();
        Assert.assertEquals(hintedUri2, uri1.toString() + "/foo");
        // end test KeyMapper target host hint

        if (partitionMethod == 2)
        {
          hashFunction = new MD5Hash();
        }
        for (URI uri : expectedUris)
        {
          if (clientUri.contains(uri.toString()))
          {
            // check if only key belonging to partition 0 gets uri2
            if (uri.equals(uri2))
            {
              if (partitionMethod == 0)
              {
                assertTrue(ii < 50);
              }
              else if (partitionMethod == 1)
              {
                assertTrue(ii % 2 == 0);
              }
              else
              {
                str[0] = ii + "";
                assertTrue(hashFunction.hash(str) % 2 == 0);
              }
            }
            // check if only key belonging to partition 1 gets uri3
            if (uri.equals(uri3))
            {
              if (partitionMethod == 0)
              {
                assertTrue(ii >= 50);
              }
              else if (partitionMethod == 1)
              {
                assertTrue(ii % 2 == 1);
              }
              else
              {
                str[0] = ii + "";
                assertTrue(hashFunction.hash(str) % 2 == 1);
              }
            }
          }
        }
      }

      // two rings for two partitions
      Map<Integer, Ring<URI>> ringMap = loadBalancer.getRings(URI.create("d2://foo"));
      assertEquals(ringMap.size(), 2);

      if (partitionMethod != 2)
      {
        Set<String> keys = new HashSet<String>();
        for (int j = 0; j < 50; j++)
        {
          if (partitionMethod == 0)
          {
            keys.add(j + "");
          }
          else
          {
            keys.add(j * 2 + "");
          }
        }

        // if it is range based partition, all keys from 0 ~ 49 belong to partition 0 according to the range definition
        // if it is modulo based partition, all even keys belong to partition 0 because the partition count is 2
        // only from partition 0
        MapKeyResult<Ring<URI>, String> mapKeyResult = loadBalancer.getRings(URI.create("d2://foo"), keys);
        Map<Ring<URI>, Collection<String>> keyToPartition = mapKeyResult.getMapResult();
        assertEquals(keyToPartition.size(), 1);
        for (Ring<URI> ring : keyToPartition.keySet())
        {
          assertEquals(ring, ringMap.get(0));
        }

        // now also from partition 1
        keys.add("51");
        mapKeyResult = loadBalancer.getRings(URI.create("d2://foo"), keys);
        assertEquals(mapKeyResult.getMapResult().size(), 2);
        assertEquals(mapKeyResult.getUnmappedKeys().size(), 0);

        // now only from partition 1
        keys.clear();
        keys.add("99");
        mapKeyResult = loadBalancer.getRings(URI.create("d2://foo"), keys);
        keyToPartition = mapKeyResult.getMapResult();
        assertEquals(keyToPartition.size(), 1);
        assertEquals(mapKeyResult.getUnmappedKeys().size(), 0);
        for (Ring<URI> ring : keyToPartition.keySet())
        {
          assertEquals(ring, ringMap.get(1));
        }

        keys.add("100");

        mapKeyResult = loadBalancer.getRings(URI.create("d2://foo"), keys);
        if (partitionMethod == 0)
        {
          // key out of range
          Collection<MapKeyResult.UnmappedKey<String>> unmappedKeys = mapKeyResult.getUnmappedKeys();
          assertEquals(unmappedKeys.size(), 1);
        }

        try
        {
          loadBalancer.getClient(new URIRequest("d2://foo/id=100"), new RequestContext());
          if (partitionMethod == 0)
          {
            // key out of range
            fail("Should throw ServiceUnavailableException caused by PartitionAccessException");
          }
        }
        catch(ServiceUnavailableException e) {}
      }

      final CountDownLatch latch = new CountDownLatch(1);
      PropertyEventShutdownCallback callback = new PropertyEventShutdownCallback()
      {
        @Override
        public void done()
        {
          latch.countDown();
        }
      };

      state.shutdown(callback);

      if (!latch.await(60, TimeUnit.SECONDS))
      {
        fail("unable to shutdown state");
      }

      executorService.shutdownNow();

      assertTrue(executorService.isShutdown(), "ExecutorService should have shut down!");
    }
  }


  @Test(groups = { "small", "back-end" })
  public void testLoadBalancerWithWait() throws URISyntaxException,
      ServiceUnavailableException,
      InterruptedException
  {
    URIRequest uriRequest = new URIRequest("d2://NonExistentService");
    LoadBalancerTestState state = new LoadBalancerTestState();
    SimpleLoadBalancer balancer = new SimpleLoadBalancer(state, 2, TimeUnit.SECONDS, _d2Executor);

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception, case 1");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.listenToService = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception, case 2");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.isListeningToService = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception, case 3");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.getServiceProperties = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception, case 4");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.listenToCluster = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception, case 5");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.isListeningToCluster = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception, case 6");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.getClusterProperties = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception, case 7");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.getUriProperties = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception, case 8");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.getClient = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception, case 9" +
        "" +
        "");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.getStrategy = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception, case 10");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.getPartitionAccessor = true;

    // victory
    assertNotNull(balancer.getClient(uriRequest, new RequestContext()));
  }

  @Test(groups = { "medium", "back-end" }, enabled =  false)
  public void testLoadBalancerSimulationRandom() throws URISyntaxException,
      IOException,
      ServiceUnavailableException,
      InterruptedException
  {
    SimpleLoadBalancerSimulation simulator =
        new SimpleLoadBalancerSimulation(new RandomLoadBalancerStrategyFactory());

    simulator.simulateMultithreaded(1, 1000, 20);
    simulator.reset();

    simulator.simulateMultithreaded(50, 10000, 20);
    simulator.reset();
  }

  @Test(enabled = false, groups = { "large", "back-end" })
  public void testLoadBalancerSimulationRandomLarge() throws URISyntaxException,
      IOException,
      ServiceUnavailableException,
      InterruptedException
  {
    SimpleLoadBalancerSimulation simulator =
        new SimpleLoadBalancerSimulation(new RandomLoadBalancerStrategyFactory());

    simulator.simulateMultithreaded(1, 1000, 20);
    simulator.reset();

    simulator.simulateMultithreaded(1, 10000, 20);
    simulator.reset();

    simulator.simulateMultithreaded(8, 10000, 750);
    simulator.reset();

    simulator.simulateMultithreaded(50, 10000, 100);
    simulator.reset();

    simulator.simulateMultithreaded(50, 10000, 100);
    simulator.reset();
  }

  @Test(groups = { "medium", "back-end" })
  public void testLoadBalancerSimulationDegrader() throws URISyntaxException,
      IOException,
      ServiceUnavailableException,
      InterruptedException
  {
    SimpleLoadBalancerSimulation simulator =
        new SimpleLoadBalancerSimulation(new DegraderLoadBalancerStrategyFactoryV3());

    simulator.simulateMultithreaded(1, 1000, 20);
    simulator.reset();

    simulator.simulateMultithreaded(50, 10000, 20);
    simulator.reset();
  }

  @Test(enabled = false, groups = { "large", "back-end" })
  public void testLoadBalancerSimulationDegraderLarge() throws URISyntaxException,
      IOException,
      ServiceUnavailableException,
      InterruptedException
  {
    SimpleLoadBalancerSimulation simulator =
        new SimpleLoadBalancerSimulation(new DegraderLoadBalancerStrategyFactoryV3());

    simulator.simulateMultithreaded(1, 1000, 20);
    simulator.reset();

    simulator.simulateMultithreaded(1, 10000, 20);
    simulator.reset();

    simulator.simulateMultithreaded(8, 10000, 750);
    simulator.reset();

    simulator.simulateMultithreaded(50, 10000, 100);
    simulator.reset();

    simulator.simulateMultithreaded(50, 10000, 100);
    simulator.reset();
  }

  @Test(groups = { "medium", "back-end" })
  public void testLoadBalancerSimulationDegraderWithFileStore() throws URISyntaxException,
      IOException,
      ServiceUnavailableException,
      InterruptedException
  {

    SimpleLoadBalancerSimulation simulator =
        new SimpleLoadBalancerSimulation(new DegraderLoadBalancerStrategyFactoryV3(),
                                         new FileStoreTestFactory<ClusterProperties>("cluster",
                                                                                     new ClusterPropertiesJsonSerializer()),
                                         new FileStoreTestFactory<ServiceProperties>("service",
                                                                                     new ServicePropertiesJsonSerializer()),
                                         new FileStoreTestFactory<UriProperties>("uri",
                                                                                 new UriPropertiesJsonSerializer()));

    simulator.simulateMultithreaded(1, 1000, 20);
    simulator.reset();

    simulator.simulateMultithreaded(50, 10000, 20);
    simulator.reset();
  }

  @Test(enabled = false, groups = { "large", "back-end" })
  public void testLoadBalancerSimulationDegraderWithFileStoreLarge() throws URISyntaxException,
      IOException,
      ServiceUnavailableException,
      InterruptedException
  {
    SimpleLoadBalancerSimulation simulator =
        new SimpleLoadBalancerSimulation(new DegraderLoadBalancerStrategyFactoryV3(),
                                         new FileStoreTestFactory<ClusterProperties>("cluster",
                                                                                     new ClusterPropertiesJsonSerializer()),
                                         new FileStoreTestFactory<ServiceProperties>("service",
                                                                                     new ServicePropertiesJsonSerializer()),
                                         new FileStoreTestFactory<UriProperties>("uri",
                                                                                 new UriPropertiesJsonSerializer()));

    simulator.simulateMultithreaded(1, 1000, 20);
    simulator.reset();

    simulator.simulateMultithreaded(1, 10000, 20);
    simulator.reset();

    simulator.simulateMultithreaded(8, 10000, 750);
    simulator.reset();

    simulator.simulateMultithreaded(50, 10000, 100);
    simulator.reset();

    simulator.simulateMultithreaded(50, 10000, 100);
    simulator.reset();
  }

  public class FileStoreTestFactory<T> implements PropertyStoreFactory<T>
  {
    private final String                _subfolder;
    private final PropertySerializer<T> _serializer;
    private final File                  _testDirectory;

    public FileStoreTestFactory(String subfolder, PropertySerializer<T> serializer) throws IOException
    {
      _subfolder = subfolder;
      _serializer = serializer;

      _testDirectory =
          LoadBalancerUtil.createTempDirectory("lb-degrader-witih-file-store-large");

      _dirsToDelete.add(_testDirectory);

      new File(_testDirectory + File.separator + _subfolder).mkdir();
    }

    @Override
    public PropertyStore<T> getStore()
    {
      return new FileStore<T>(_testDirectory + File.separator + _subfolder,
                              FileSystemDirectory.FILE_STORE_EXTENSION,
                              _serializer);
    }
  }

  public static class DoNothingClientFactory implements TransportClientFactory
  {
    private final AtomicLong _count = new AtomicLong();

    @SuppressWarnings("unchecked")
    @Override
    public TransportClient getClient(Map<String, ? extends Object> properties)
    {
      _count.incrementAndGet();
      if (properties.containsKey("foobar"))
      {
        throw new IllegalArgumentException();
      }
      return new DoNothingClient();
    }

    private class DoNothingClient implements TransportClient
    {
      @Override
      public void streamRequest(StreamRequest request,
                              RequestContext requestContext,
                              Map<String, String> wireAttrs,
                              TransportCallback<StreamResponse> callback)
      {
      }

      @Override
      public void restRequest(RestRequest request,
                       RequestContext requestContext,
                       Map<String, String> wireAttrs,
                       TransportCallback<RestResponse> callback)
      {
      }

      @Override
      public void shutdown(Callback<None> callback)
      {
        _count.decrementAndGet();
        callback.onSuccess(None.none());
      }
    }

    @Override
    public void shutdown(Callback<None> callback)
    {
      callback.onSuccess(None.none());
    }

    public long getRunningClientCount()
    {
      return _count.get();
    }
  }

  private static class TestLoadBalancerStrategy implements LoadBalancerStrategy
  {
    Map<Integer, Map<URI, Integer>> _partitionData;

    public TestLoadBalancerStrategy(Map<URI, Map<Integer, PartitionData>> partitionDescriptions)
    {
      _partitionData = new HashMap<Integer, Map<URI, Integer>>();
      for (Map.Entry<URI, Map<Integer, PartitionData>> uriPartitionPair : partitionDescriptions.entrySet())
      {
        for (Map.Entry<Integer, PartitionData> partitionData : uriPartitionPair.getValue().entrySet())
        {
          if (!_partitionData.containsKey(partitionData.getKey()))
          {
            _partitionData.put(partitionData.getKey(), new HashMap<URI, Integer>());
          }
          _partitionData.get(partitionData.getKey()).put(uriPartitionPair.getKey(), 100);
        }
      }
    }

    @Override
    public TrackerClient getTrackerClient(Request request,
                                          RequestContext requestContext,
                                          long clusterGenerationId,
                                          int partitionId,
                                          List<TrackerClient> trackerClients)
    {
      throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public Ring<URI> getRing(long clusterGenerationId, int partitionId, List<TrackerClient> trackerClients)
    {
      if (_partitionData.containsKey(partitionId))
      {
        return new ConsistentHashRing<URI>(_partitionData.get(partitionId));
      }
      else
      {
        return new ConsistentHashRing<URI>(new HashMap<URI, Integer>());
      }
    }

    @Override
    public HashFunction<Request> getHashFunction()
    {
      return new RandomHash();
    }
  }

  private static class TestPartitionAccessor implements PartitionAccessor
  {

    @Override
    public int getPartitionId(URI uri)
        throws PartitionAccessException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getPartitionId(String key)
        throws PartitionAccessException
    {
      Integer i = Integer.parseInt(key);
      if (i == 1)
      {
        return 1;
      }
      else if (i == 2)
      {
        return 2;
      }
      else if (i == 3)
      {
        return 3;
      }
      else
        throw new PartitionAccessException("No partition for this");
    }

    @Override
    public int getMaxPartitionId()
    {
      return 3;
    }

  }

  /**
   * This test simulates dropping requests by playing with OverrideDropRate in config
   *
   */
  @Test(groups = { "small", "back-end" })
  public void testLoadBalancerDropRate() throws ServiceUnavailableException,
          ExecutionException, InterruptedException {
    final int RETRY=10;
    for (int tryAgain = 0; tryAgain < RETRY; ++tryAgain)
    {
      Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
              new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();
      Map<String, TransportClientFactory> clientFactories = new HashMap<String, TransportClientFactory>();
      List<String> prioritizedSchemes = new ArrayList<String>();

      MockStore<ServiceProperties> serviceRegistry = new MockStore<ServiceProperties>();
      MockStore<ClusterProperties> clusterRegistry = new MockStore<ClusterProperties>();
      MockStore<UriProperties> uriRegistry = new MockStore<UriProperties>();

      ScheduledExecutorService executorService = new SynchronousExecutorService();

      //loadBalancerStrategyFactories.put("rr", new RandomLoadBalancerStrategyFactory());
      loadBalancerStrategyFactories.put("degrader", new DegraderLoadBalancerStrategyFactoryV3());
      // PrpcClientFactory();
      clientFactories.put(PropertyKeys.HTTP_SCHEME, new DoNothingClientFactory()); // new
      // HttpClientFactory();

      SimpleLoadBalancerState state =
              new SimpleLoadBalancerState(executorService,
                      uriRegistry,
                      clusterRegistry,
                      serviceRegistry,
                      clientFactories,
                      loadBalancerStrategyFactories);

      SimpleLoadBalancer loadBalancer =
        new SimpleLoadBalancer(state, 5, TimeUnit.SECONDS, _d2Executor);

      FutureCallback<None> balancerCallback = new FutureCallback<None>();
      loadBalancer.start(balancerCallback);
      balancerCallback.get();

      URI uri1 = URI.create("http://test.qa1.com:1234");
      URI uri2 = URI.create("http://test.qa2.com:2345");
      URI uri3 = URI.create("http://test.qa3.com:6789");

      Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
      partitionData.put(DEFAULT_PARTITION_ID, new PartitionData(1d));
      Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>(3);
      uriData.put(uri1, partitionData);
      uriData.put(uri2, partitionData);
      uriData.put(uri3, partitionData);

      prioritizedSchemes.add(PropertyKeys.HTTP_SCHEME);

      clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1"));

      serviceRegistry.put("foo", new ServiceProperties("foo",
              "cluster-1",
              "/foo",
              Arrays.asList("degrader"),
              Collections.<String,Object>emptyMap(),
              null,
              null,
              prioritizedSchemes,
              null));
      uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

      URI expectedUri1 = URI.create("http://test.qa1.com:1234/foo");
      URI expectedUri2 = URI.create("http://test.qa2.com:2345/foo");
      URI expectedUri3 = URI.create("http://test.qa3.com:6789/foo");

      Set<URI> expectedUris = new HashSet<URI>();

      expectedUris.add(expectedUri1);
      expectedUris.add(expectedUri2);
      expectedUris.add(expectedUri3);
      Random random = new Random();

      for (int i = 0; i < 100; ++i)
      {
        try
        {
          RewriteLoadBalancerClient client =
                  (RewriteLoadBalancerClient) loadBalancer.getClient(new URIRequest("d2://foo/52"), new RequestContext());
          assertTrue(client.getDecoratedClient() instanceof RewriteClient);
          RewriteClient rewriteClient = (RewriteClient) client.getDecoratedClient();
          assertTrue(rewriteClient.getDecoratedClient() instanceof TrackerClient);
          TrackerClient tClient = (TrackerClient) rewriteClient.getDecoratedClient();
          DegraderImpl degrader = (DegraderImpl)tClient.getDegrader(DEFAULT_PARTITION_ID);
          DegraderImpl.Config cfg = new DegraderImpl.Config(degrader.getConfig());
          // Change DropRate to 0.0 at the rate of 1/3
          cfg.setOverrideDropRate((random.nextInt(2) == 0) ? 1.0 : 0.0);
          degrader.setConfig(cfg);

          assertTrue(expectedUris.contains(client.getUri()));
          assertEquals(client.getUri().getScheme(), PropertyKeys.HTTP_SCHEME);
        }
        catch (ServiceUnavailableException e)
        {
          assertTrue(e.toString().contains("in a bad state (high latency/high error)"));
        }
      }

      final CountDownLatch latch = new CountDownLatch(1);
      PropertyEventShutdownCallback callback = new PropertyEventShutdownCallback()
      {
        @Override
        public void done()
        {
          latch.countDown();
        }
      };

      state.shutdown(callback);

      if (!latch.await(60, TimeUnit.SECONDS))
      {
        fail("unable to shutdown state");
      }

      executorService.shutdownNow();

      assertTrue(executorService.isShutdown(), "ExecutorService should have shut down!");
    }
  }
}

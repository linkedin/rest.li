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
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.LoadBalancerTestState;
import com.linkedin.d2.balancer.PartitionedLoadBalancerTestState;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.clients.RewriteClient;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.HashBasedPartitionProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
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
import com.linkedin.d2.balancer.util.HostToKeyMapper;
import com.linkedin.d2.balancer.util.KeysAndHosts;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.d2.balancer.util.MapKeyResult;
import com.linkedin.d2.balancer.util.URIRequest;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.HashFunction;
import com.linkedin.d2.balancer.util.hashing.MD5Hash;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class SimpleLoadBalancerTest
{
  private List<File> _dirsToDelete;

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
      clientFactories.put("http", new DoNothingClientFactory()); // new
      // HttpClientFactory();

      SimpleLoadBalancerState state =
          new SimpleLoadBalancerState(executorService,
                                      uriRegistry,
                                      clusterRegistry,
                                      serviceRegistry,
                                      clientFactories,
                                      loadBalancerStrategyFactories);

      SimpleLoadBalancer loadBalancer =
          new SimpleLoadBalancer(state, 5, TimeUnit.SECONDS);

      FutureCallback<None> balancerCallback = new FutureCallback<None>();
      loadBalancer.start(balancerCallback);
      balancerCallback.get();

      URI uri1 = URI.create("http://test.qa1.com:1234");
      URI uri2 = URI.create("http://test.qa2.com:2345");
      URI uri3 = URI.create("http://test.qa3.com:6789");

      Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
      partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
      Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>(3);
      uriData.put(uri1, partitionData);
      uriData.put(uri2, partitionData);
      uriData.put(uri3, partitionData);

      prioritizedSchemes.add("http");

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
        RewriteClient client =
            (RewriteClient) loadBalancer.getClient(new URIRequest("d2://foo/52"),
                                                   new RequestContext());

        assertTrue(expectedUris.contains(client.getUri()));
        assertEquals(client.getUri().getScheme(), "http");
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

    orderedStrategies.add(new LoadBalancerState.SchemeStrategyPair("http", strategy));

    //setup the partition accessor which can only map keys from 1 - 3.
    PartitionAccessor accessor = new TestPartitionAccessor();

    URI serviceURI = new URI("d2://" + serviceName);
    SimpleLoadBalancer balancer = new SimpleLoadBalancer(new PartitionedLoadBalancerTestState(
            clusterName, serviceName, path, strategyName, partitionDescriptions, orderedStrategies,
            accessor
    ));

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

    Assert.assertEquals(ordering1.get(0), server2);
    Assert.assertEquals(ordering1.get(1), server3);
    Assert.assertEquals(ordering1.get(2), server1);
    Assert.assertEquals(ordering1, ordering2);
    Assert.assertEquals(ordering3.get(0), server1);

    Assert.assertTrue(result.getPartitionsWithoutEnoughHosts().containsKey(3));
    Assert.assertEquals((int)result.getPartitionsWithoutEnoughHosts().get(3), 2);
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

    orderedStrategies.add(new LoadBalancerState.SchemeStrategyPair("http", strategy));

    //setup the partition accessor which is used to get partitionId -> keys
    PartitionAccessor accessor = new TestPartitionAccessor();

    URI serviceURI = new URI("d2://" + serviceName);
    SimpleLoadBalancer balancer = new SimpleLoadBalancer(new PartitionedLoadBalancerTestState(
            clusterName, serviceName, path, strategyName, partitionDescriptions, orderedStrategies,
            accessor
    ));

    HostToKeyMapper<URI> result = balancer.getPartitionInformation(serviceURI, null, 3, 123);

    Assert.assertEquals(result.getPartitionInfoMap().size(), 4);
    Assert.assertEquals(4, result.getPartitionCount());
    // partition 0 should be empty
    Assert.assertTrue(result.getPartitionInfoMap().get(0).getHosts().isEmpty());
    // partition 1 should have server1, server2 and server3.
    List<URI> ordering1 = result.getPartitionInfoMap().get(1).getHosts();
    Assert.assertEquals(ordering1.get(0), server2);
    Assert.assertEquals(ordering1.get(1), server3);
    Assert.assertEquals(ordering1.get(2), server1);
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

      clientFactories.put("http", new DoNothingClientFactory());

      SimpleLoadBalancerState state =
          new SimpleLoadBalancerState(executorService,
              uriRegistry,
              clusterRegistry,
              serviceRegistry,
              clientFactories,
              loadBalancerStrategyFactories);

      SimpleLoadBalancer loadBalancer =
          new SimpleLoadBalancer(state, 5, TimeUnit.SECONDS);

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

      prioritizedSchemes.add("http");

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
                                                        Collections.<String,Object>emptyMap(),
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
        assertEquals(ringMap.get(2).toString(), new ConsistentHashRing<URI>(new HashMap<URI, Integer>()).toString());
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
        RewriteClient client =
            (RewriteClient) loadBalancer.getClient(new URIRequest("d2://foo/id=" + ii), new RequestContext());
        String clientUri = client.getUri().toString();
        HashFunction<String[]> hashFunction = null;
        String[] str = new String[1];

        // test KeyMapper target host hint: request is always to target host regardless of what's in d2 URI and whether it's hash-based or range-based partitions
        RequestContext requestContextWithHint = new RequestContext();
        KeyMapper.TargetHostHints.setRequestContextTargetHost(requestContextWithHint, uri1);
        RewriteClient hintedClient1 = (RewriteClient)loadBalancer.getClient(new URIRequest("d2://foo/id=" + ii), requestContextWithHint);
        String hintedUri1 = hintedClient1.getUri().toString();
        Assert.assertEquals(hintedUri1, uri1.toString() + "/foo");
        RewriteClient hintedClient2 = (RewriteClient)loadBalancer.getClient(new URIRequest("d2://foo/action=purge-all"), requestContextWithHint);
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
    SimpleLoadBalancer balancer = new SimpleLoadBalancer(state, 5, TimeUnit.SECONDS);

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.listenToService = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.isListeningToService = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.getServiceProperties = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.listenToCluster = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.isListeningToCluster = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.getClusterProperties = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.getUriProperties = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.getClient = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception");
    }
    catch (ServiceUnavailableException e)
    {
    }

    state.getStrategy = true;

    try
    {
      balancer.getClient(uriRequest, new RequestContext());
      fail("should have received a service unavailable exception");
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
                              ".ini",
                              _serializer);
    }
  }

  public static class DoNothingClientFactory implements TransportClientFactory
  {
    private final AtomicLong _count = new AtomicLong();

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

    public TestLoadBalancerStrategy(Map<URI, Map<Integer, PartitionData>> partitionDescriptions) {
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
      clientFactories.put("http", new DoNothingClientFactory()); // new
      // HttpClientFactory();

      SimpleLoadBalancerState state =
              new SimpleLoadBalancerState(executorService,
                      uriRegistry,
                      clusterRegistry,
                      serviceRegistry,
                      clientFactories,
                      loadBalancerStrategyFactories);

      SimpleLoadBalancer loadBalancer =
              new SimpleLoadBalancer(state, 5, TimeUnit.SECONDS);

      FutureCallback<None> balancerCallback = new FutureCallback<None>();
      loadBalancer.start(balancerCallback);
      balancerCallback.get();

      URI uri1 = URI.create("http://test.qa1.com:1234");
      URI uri2 = URI.create("http://test.qa2.com:2345");
      URI uri3 = URI.create("http://test.qa3.com:6789");

      Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
      partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
      Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>(3);
      uriData.put(uri1, partitionData);
      uriData.put(uri2, partitionData);
      uriData.put(uri3, partitionData);

      prioritizedSchemes.add("http");

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
          RewriteClient client =
                  (RewriteClient) loadBalancer.getClient(new URIRequest("d2://foo/52"), new RequestContext());
          TrackerClient tClient = (TrackerClient) client.getWrappedClient();
          DegraderImpl degrader = (DegraderImpl)tClient.getDegrader(DefaultPartitionAccessor.DEFAULT_PARTITION_ID);
          DegraderImpl.Config cfg = new DegraderImpl.Config(degrader.getConfig());
          // Change DropRate to 0.0 at the rate of 1/3
          cfg.setOverrideDropRate((random.nextInt(2) == 0) ? 1.0 : 0.0);
          degrader.setConfig(cfg);

          assertTrue(expectedUris.contains(client.getUri()));
          assertEquals(client.getUri().getScheme(), "http");
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

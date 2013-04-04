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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.linkedin.d2.discovery.event.SynchronousExecutorService;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.LoadBalancerTestState;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.clients.RewriteClient;
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
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.d2.balancer.util.MapKeyResult;
import com.linkedin.d2.balancer.util.URIRequest;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.HashFunction;
import com.linkedin.d2.balancer.util.hashing.MD5Hash;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.mock.MockStore;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;

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
                                                        "degrader",
                                                        Collections.<String>emptyList(),
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
                                                        "degrader",
                                                        Collections.<String>emptyList(),
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

  @Test(groups = { "medium", "back-end" })
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
      return new DoNothingClient();
    }

    private class DoNothingClient implements TransportClient
    {
      @Override
      public void restRequest(RestRequest request,
                              RequestContext requestContext,
                              Map<String, String> wireAttrs,
                              TransportCallback<RestResponse> callback)
      {
      }

      @Override
      public void rpcRequest(RpcRequest request,
                             RequestContext requestContext,
                             Map<String, String> wireAttrs,
                             TransportCallback<RpcResponse> callback)
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
}

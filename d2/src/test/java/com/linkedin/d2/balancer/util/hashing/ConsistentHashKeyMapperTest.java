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

package com.linkedin.d2.balancer.util.hashing;

import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.PartitionedLoadBalancerTestState;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;
import com.linkedin.d2.balancer.util.HostToKeyMapper;
import com.linkedin.d2.balancer.util.HostToKeyResult;
import com.linkedin.d2.balancer.util.KeysAndHosts;
import com.linkedin.d2.balancer.util.MapKeyResult;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessException;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class ConsistentHashKeyMapperTest
{
  private static final double TOLERANCE = 0.05d;
  private static final long RANDOM_SEED = 42;

  static Map<URI, Set<Integer>> mapKeys(KeyMapper mapper, URI uri, Set<Integer> keys) throws ServiceUnavailableException
  {
    MapKeyResult<URI, Integer> mapKeyResult = mapper.mapKeysV2(uri, keys);
    Map<URI, Collection<Integer>> collectionResult = mapKeyResult.getMapResult();
    Map<URI, Set<Integer>> result = new HashMap<URI, Set<Integer>>(collectionResult.size() * 2);
    for (Map.Entry<URI, Collection<Integer>> entry : collectionResult.entrySet())
    {
      result.put(entry.getKey(), new HashSet<Integer>(entry.getValue()));
    }
    return result;
  }

  @Test
  public void testMapKeysV3() throws URISyntaxException, ServiceUnavailableException
  {
    URI serviceURI = new URI("d2://articles");
    ConsistentHashKeyMapper mapper = getConsistentHashKeyMapper();

    List<Integer> keys = Arrays.asList(1, 2, 3, 4, 9, 10, 13, 15, 16);

    HostToKeyMapper<Integer> result = mapper.mapKeysV3(serviceURI, keys, 2);
    verifyHostToMapperWithKeys(result);
  }

  @Test
  public void testMapKeysV3StickKey() throws URISyntaxException, ServiceUnavailableException
  {
    int numHost = 2;
    URI serviceURI = new URI("d2://articles");
    ConsistentHashKeyMapper mapper = getConsistentHashKeyMapper();

    List<Integer> keys = Arrays.asList(1, 2, 3, 4, 9, 10, 13, 15, 16);

    String myStickyKey = "sticky";
    HostToKeyMapper<Integer> result = mapper.mapKeysV3(serviceURI, keys, numHost, myStickyKey);
    Map<Integer, List<URI>> originalOrderingOfHost = getOrderingOfHostsForEachKey(result, numHost);

    // repeat 100 times. The ordering of the hosts should always be the same because of sticky key
    int numOfMatch = 0;
    for (int i = 0; i < 100; i++)
    {
      result = mapper.mapKeysV3(serviceURI, keys, numHost, myStickyKey);
      Map<Integer, List<URI>> newOrderingOfHost = getOrderingOfHostsForEachKey(result, 2);
      if (newOrderingOfHost.equals(originalOrderingOfHost))
      {
        numOfMatch++;
      }
    }
    Assert.assertEquals(100, numOfMatch);
  }

  @Test
  public void testAllPartitionMultipleHosts() throws URISyntaxException, ServiceUnavailableException
  {
    URI serviceURI = new URI("d2://articles");
    ConsistentHashKeyMapper mapper = getConsistentHashKeyMapper();

    HostToKeyMapper<Integer> result = mapper.getAllPartitionsMultipleHosts(serviceURI, 2);
    verifyHostToMapperWithoutKeys(result);
  }

  @Test
  public void testAllPartitionMultipleHostsStickKey() throws URISyntaxException, ServiceUnavailableException
  {
    int numHost = 2;
    URI serviceURI = new URI("d2://articles");

    ConsistentHashKeyMapper mapper = getConsistentHashKeyMapper();

    String myStickyKey = "sticky";
    HostToKeyMapper<Integer> result = mapper.getAllPartitionsMultipleHosts(serviceURI, numHost, myStickyKey);
    Map<Integer, List<URI>> originalOrderingOfHost = getOrderingOfHostsForEachKey(result, numHost);

    // repeat 100 times. The ordering of the hosts should always be the same because of sticky key
    int numOfMatch = 0;
    for (int i = 0; i < 100; i++)
    {
      result = mapper.getAllPartitionsMultipleHosts(serviceURI, numHost, myStickyKey);
      Map<Integer, List<URI>> newOrderingOfHost = getOrderingOfHostsForEachKey(result, numHost);
      if (newOrderingOfHost.equals(originalOrderingOfHost))
      {
        numOfMatch++;
      }
    }
    Assert.assertEquals(100, numOfMatch);
  }


  private ConsistentHashKeyMapper getConsistentHashKeyMapper() throws URISyntaxException
  {
    String serviceName = "articles";
    String clusterName = "cluster";
    String path = "path";
    String strategyName = "degrader";

    //setup partition
    Map<URI, Map<Integer, PartitionData>> partitionDescriptions = new HashMap<URI, Map<Integer, PartitionData>>();

    final URI foo1 = new URI("http://foo1.com");
    Map<Integer, PartitionData> foo1Data = new HashMap<Integer, PartitionData>();
    foo1Data.put(0, new PartitionData(1.0));
    partitionDescriptions.put(foo1, foo1Data);

    final URI foo2 = new URI("http://foo2.com");
    Map<Integer, PartitionData> foo2Data = new HashMap<Integer, PartitionData>();
    foo2Data.put(3, new PartitionData(1.0));
    foo2Data.put(4, new PartitionData(1.0));
    partitionDescriptions.put(foo2, foo2Data);

    final URI foo3 = new URI("http://foo3.com");
    Map<Integer, PartitionData> foo3Data = new HashMap<Integer, PartitionData>();
    foo3Data.put(0, new PartitionData(1.0));
    partitionDescriptions.put(foo3, foo3Data);

    final URI foo4 = new URI("http://foo4.com");
    Map<Integer, PartitionData> foo4Data = new HashMap<Integer, PartitionData>();
    foo4Data.put(1, new PartitionData(1.0));
    partitionDescriptions.put(foo4, foo4Data);

    final URI foo5 = new URI("http://foo5.com");
    Map<Integer, PartitionData> foo5Data = new HashMap<Integer, PartitionData>();
    foo5Data.put(1, new PartitionData(1.0));
    partitionDescriptions.put(foo5, foo5Data);

    final URI foo6 = new URI("http://foo6.com");
    Map<Integer, PartitionData> foo6Data = new HashMap<Integer, PartitionData>();
    foo6Data.put(1, new PartitionData(1.0));
    partitionDescriptions.put(foo6, foo6Data);

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

    ConsistentHashKeyMapper mapper = new ConsistentHashKeyMapper(balancer, balancer);

    return mapper;
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void testMapKeysConcurrency() throws Exception
  {
    String serviceName = "articles";
    String clusterName = "cluster";
    String path = "path";
    String strategyName = "degrader";
    int numPartitions = 500;

    // setup partition
    Map<URI,Map<Integer, PartitionData>> partitionDescriptions = new HashMap<URI, Map<Integer, PartitionData>>();
    final URI foo1 = new URI("http://foo1.com");
    Map<Integer, PartitionData> foo1Data = new HashMap<Integer, PartitionData>();
    for (int i = 0; i < numPartitions; i++)
    {
      foo1Data.put(i, new PartitionData(1.0));
    }
    partitionDescriptions.put(foo1, foo1Data);

    DegraderLoadBalancerStrategyV3 strategy = new DegraderLoadBalancerStrategyV3(new DegraderLoadBalancerStrategyConfig(5000), serviceName, null);
    List<LoadBalancerState.SchemeStrategyPair> orderedStrategies = new ArrayList<LoadBalancerState.SchemeStrategyPair>();
    orderedStrategies.add(new LoadBalancerState.SchemeStrategyPair("http", strategy));

    PartitionAccessor accessor = new TestDeadlockPartitionAccessor(numPartitions);

    SimpleLoadBalancer balancer = new SimpleLoadBalancer(new PartitionedLoadBalancerTestState(
            clusterName, serviceName, path, strategyName, partitionDescriptions, orderedStrategies,
            accessor
    ));
    ConsistentHashKeyMapper mapper = new ConsistentHashKeyMapper(balancer, balancer);

    CountDownLatch latch = new CountDownLatch(numPartitions);
    List<Runnable> runnables = createRunnables(numPartitions, mapper, serviceName, latch);
    final ExecutorService executor = Executors.newFixedThreadPool(numPartitions);
    List<Future> futures = new ArrayList<Future>();
    for (int i = 0; i < numPartitions; i++)
    {
      futures.add(executor.submit(runnables.get(i)));
    }

    // wait for threads to finish
    Thread.sleep(3000);

    // every thread should have finished, otherwise there is a deadlock
    for (int i = 0; i < numPartitions; i++)
    {
      Assert.assertTrue(futures.get(i).isDone());
    }
  }

  /**
   * Create tasks for the deadlock test
   */
  private List<Runnable> createRunnables(int num, final ConsistentHashKeyMapper mapper, String serviceName, final CountDownLatch latch) throws URISyntaxException
  {
    final URI serviceURI = new URI("d2://" + serviceName);

    List<Runnable> runnables = new ArrayList<Runnable>();
    for (int i = 0; i < num; i++)
    {
      // since i < numPartitions, the keys will be distributed to different partitions
      final List<String> keys = generateKeys(i);
      Runnable runnable = new Runnable()
      {
        @Override
        public void run()
        {
          // wait until all jobs submitted
          latch.countDown();
          try
          {
            latch.await();
            mapper.mapKeysV3(serviceURI, keys, 1);
          }
          catch (InterruptedException e)
          {
            e.printStackTrace();
          }
          catch (ServiceUnavailableException e)
          {
            e.printStackTrace();
          }
        }
      };
      runnables.add(runnable);
    }

    return runnables;
  }

  private List<String> generateKeys(int partition)
  {
    List<String> keys = new ArrayList<String>();
    keys.add(String.valueOf(partition));
    return keys;
  }

  private void verifyHostToMapperWithKeys(HostToKeyMapper<Integer> result)
  {
    Map<Integer, KeysAndHosts<Integer>> partitionInfoMap = result.getPartitionInfoMap();
    Assert.assertTrue(Arrays.asList(1, 2, 3).containsAll(partitionInfoMap.get(0).getKeys()));
    Assert.assertTrue(Arrays.asList(4).containsAll(partitionInfoMap.get(1).getKeys()));
    Assert.assertTrue(Arrays.asList(9).containsAll(partitionInfoMap.get(2).getKeys()));
    Assert.assertTrue(Arrays.asList(10).containsAll(partitionInfoMap.get(3).getKeys()));
    Assert.assertTrue(Arrays.asList(13, 15).containsAll(partitionInfoMap.get(4).getKeys()));
    HostToKeyResult.UnmappedKey<Integer> unmappedKey = result.getUnmappedKeys().iterator().next();
    Assert.assertEquals((int)unmappedKey.getKey(), 16);
    Assert.assertEquals(unmappedKey.getErrorType(), HostToKeyResult.ErrorType.FAIL_TO_FIND_PARTITION);

    verifyHostToMapperWithoutKeys(result);
  }
  private void verifyHostToMapperWithoutKeys(HostToKeyMapper<Integer> result)
  {
    Map<Integer, KeysAndHosts<Integer>> partitionInfoMap = result.getPartitionInfoMap();
    try {
      Assert.assertTrue(Arrays.asList(new URI[]{new URI("http://foo1.com"), new URI("http://foo3.com")}).containsAll(partitionInfoMap.get(0).getHosts()));
      Assert.assertTrue(Arrays.asList(new URI[]{new URI("http://foo5.com"), new URI("http://foo4.com"), new URI("http://foo6.com")}).containsAll(partitionInfoMap.get(1).getHosts()));
      Assert.assertTrue(Arrays.asList(new URI[]{new URI("http://foo2.com")}).containsAll(partitionInfoMap.get(3).getHosts()));
      Assert.assertTrue(Arrays.asList(new URI[]{new URI("http://foo2.com")}).containsAll(partitionInfoMap.get(4).getHosts()));
    }
    catch (URISyntaxException e)
    {
    }
  }

  @SuppressWarnings("unchecked")
  private Map<Integer, List<URI>> getOrderingOfHostsForEachKey(HostToKeyMapper<Integer> result, int numHost)
  {
    Map<Integer, List<URI>> keyToHosts = new HashMap<Integer, List<URI>>();
    for (int i = 0; i < numHost; i++)
    {
      HostToKeyResult<Integer> hostToKeyResult = result.getResult(i);
      for (Map.Entry<URI, Collection<Integer>> entry : hostToKeyResult.getMapResult().entrySet())
      {
        Collection<Integer> keys = entry.getValue();
        for (Integer key : keys)
        {
          List<URI> hosts = keyToHosts.get(key);
          if (hosts == null)
          {
            hosts = new ArrayList<URI>();
            keyToHosts.put(key, hosts);
          }
          hosts.add(entry.getKey());
        }
      }
    }
    return keyToHosts;
  }

  @Test
  public void testOneBatch() throws URISyntaxException, ServiceUnavailableException
  {
    ConsistentHashKeyMapper batcher = getKeyToHostMapper();

    Set<Integer> keys = new HashSet<Integer>();
    keys.add(1);

    Map<URI, Set<Integer>> batchedKeys = mapKeys(batcher, URI.create("d2://fooservice/"), keys);

    checkBatchCoverage(keys, batchedKeys);
    Assert.assertEquals(batchedKeys.keySet().size(), 1);
  }

  @Test
  public void testOneBatchManyKeys() throws URISyntaxException, ServiceUnavailableException
  {
    ConsistentHashKeyMapper batcher = getKeyToHostMapper();
    Set<Integer> keys = getRandomKeys(1000);
    Map<URI, Set<Integer>> batchedKeys = mapKeys(batcher, URI.create("d2://fooservice/"), keys);

    checkBatchCoverage(keys, batchedKeys);
    Assert.assertEquals(batchedKeys.keySet().size(), 1);
  }

  private Set<Integer> getRandomKeys(int n)
  {
    Set<Integer> keys = new HashSet<Integer>();
    Random r = new Random(RANDOM_SEED);

    for (int ii=0; ii<n; ++ii)
    {
      keys.add(Math.abs(r.nextInt()));
    }
    return keys;
  }

  @Test
  public void testTwoBatches() throws URISyntaxException, ServiceUnavailableException
  {
    Map<URI, Integer> endpoints = new HashMap<URI, Integer>();
    endpoints.put(new URI("test1"), 100);
    endpoints.put(new URI("test2"), 100);
    ConsistentHashKeyMapper batcher = getKeyToHostMapper(endpoints);

    Set<Integer> keys = getRandomKeys(1000);
    Map<URI, Set<Integer>> batchedKeys = mapKeys(batcher, URI.create("d2://fooservice/"), keys);

    Assert.assertEquals(batchedKeys.size(), 2);

    checkBatchCoverage(keys, batchedKeys);
    checkBatchLoad(keys, batchedKeys, 0.5);
  }

  @Test
  public void testThreePartitionsTwoBatches() throws URISyntaxException, ServiceUnavailableException
  {
    Map<URI, Integer> endpoints = new HashMap<URI, Integer>();
    endpoints.put(new URI("test1"), 100);
    endpoints.put(new URI("test2"), 100);
    endpoints.put(new URI("test3"), 100);

    ConsistentHashKeyMapper batcher = getKeyToHostMapper(endpoints, 3);

    Set<Integer> rawkeys = getRandomKeys(3000);
    Set<Integer> keys = new HashSet<Integer>();
    for (Integer key : rawkeys)
    {
      if (key % 3 != 0)
      {
        keys.add(key);
      }
    }

    Map<URI, Set<Integer>> batchedKeys = mapKeys(batcher, URI.create("d2://fooservice"), keys);
    // should only have two batches, as no keys belong to partition 3
    Assert.assertEquals(batchedKeys.size(), 2);
    checkBatchCoverage(keys, batchedKeys);
  }

  private void checkBatchLoad(Set<Integer> keys, Map<URI, Set<Integer>> batchedKeys,
      double expectedLoad)
  {
    for (Set<Integer> batch : batchedKeys.values())
    {
      double load = (double)batch.size() / keys.size();
      Assert.assertEquals(load, expectedLoad, TOLERANCE);
    }
  }

  @Test
  public void testManyBatches() throws URISyntaxException, ServiceUnavailableException
  {
    ConsistentHashKeyMapper batcher = getKeyToHostMapper(createEndpoints(100));
    Set<Integer> keys = getRandomKeys(1000);
    Map<URI, Set<Integer>> batchedKeys = mapKeys(batcher, URI.create("d2://fooservice/"), keys);
    checkBatchCoverage(keys, batchedKeys);
    checkBatchLoad(keys, batchedKeys, 1.0 / 100.0);
  }

  @Test
  public void testThreePartitionsManyBatches() throws URISyntaxException, ServiceUnavailableException
  {
    Map<URI, Integer> endpoints = createEndpoints(300);

    ConsistentHashKeyMapper batcher = getKeyToHostMapper(endpoints, 3);

    Set<Integer> rawkeys = getRandomKeys(3000);
    Set<Integer> keys = new HashSet<Integer>();
    for (Integer key : rawkeys)
    {
      if (key % 3 != 0)
      {
        keys.add(key);
      }
    }

    Map<URI, Set<Integer>> batchedKeys = mapKeys(batcher, URI.create("d2://fooservice"), keys);
    // should only have 200 batches, as no keys belong to partition 3
    Assert.assertEquals(batchedKeys.size(), 200);
    checkBatchCoverage(keys, batchedKeys);
    // keys should be evenly distributed
    checkBatchLoad(keys, batchedKeys, 1.0/200.0);
  }

  @Test
  public void testSparseBatches() throws URISyntaxException, ServiceUnavailableException
  {
    ConsistentHashKeyMapper batcher = getKeyToHostMapper(createEndpoints(1000));
    Set<Integer> keys = getRandomKeys(100);
    Map<URI, Set<Integer>> batchedKeys = mapKeys(batcher, URI.create("d2://fooservice/"), keys);
    checkBatchCoverage(keys, batchedKeys);
    checkBatchLoad(keys, batchedKeys, 1.0 / 1000.0);
  }

  @Test
  public void testConsistencyWithEndpointRemoval() throws URISyntaxException, ServiceUnavailableException
  {
    int nKeys = 10000;
    int nEndpoints = 100;

    Map<URI, Integer> endpoints = createEndpoints(nEndpoints);
    ConsistentHashKeyMapper batcher1 = getKeyToHostMapper(endpoints);

    Set<Integer> keys = getRandomKeys(nKeys);

    Map<URI, Set<Integer>> batchedKeys1 = mapKeys(batcher1, URI.create("d2://fooservice/"), keys);
    checkBatchCoverage(keys, batchedKeys1);
    Assert.assertEquals(batchedKeys1.size(), nEndpoints);

    endpoints.remove(endpoints.keySet().iterator().next());
    Assert.assertEquals(endpoints.size(), 99);
    ConsistentHashKeyMapper batcher2 = getKeyToHostMapper(endpoints);

    Map<URI, Set<Integer>> batchedKeys2 = mapKeys(batcher2, URI.create("d2://fooservice/"), keys);
    checkBatchCoverage(keys, batchedKeys2);
    Assert.assertEquals(batchedKeys2.size(), nEndpoints-1);

    Map<Integer, URI> keyMap1 = invert(batchedKeys1);
    Map<Integer, URI> keyMap2 = invert(batchedKeys2);
    int misses = 0;
    for (Integer key : keys)
    {
      if (!keyMap1.get(key).equals(keyMap2.get(key)))
      {
        ++misses;
      }
    }

    Assert.assertEquals((double)misses/nKeys, 1.0/nEndpoints, TOLERANCE);

  }

  @Test
  public void testConsistencyWithRepeatedHashing() throws URISyntaxException, ServiceUnavailableException
  {
    final int nRuns=3;

    Map<URI, Integer> endpoints = createEndpoints(100);
    ConsistentHashKeyMapper mapper = getKeyToHostMapper(endpoints);

    Set<Integer> keys = getRandomKeys(1000);

    Map<URI, Set<Integer>> batchedKeys = mapKeys(mapper, URI.create("d2://fooservice/"), keys);
    for (int ii=0; ii<nRuns; ++ii)
    {
      Map<URI, Set<Integer>> batchedKeysRepeat = mapKeys(mapper, URI.create("d2://fooservice/"), keys);
      Assert.assertEquals(batchedKeys, batchedKeysRepeat);
    }

  }

  ConsistentHashKeyMapper getKeyToHostMapper() throws URISyntaxException, ServiceUnavailableException
  {
    Map<URI, Integer> one = new HashMap<URI, Integer>();
    one.put(new URI("test"), 100);
    return getKeyToHostMapper(one);
  }


  private Map<Integer, URI> invert(Map<URI, Set<Integer>> batchedKeys1)
  {
    Map<Integer, URI> keyMappings = new HashMap<Integer, URI>();
    for (Map.Entry<URI, Set<Integer>> entry : batchedKeys1.entrySet())
    {
      for (Integer value : entry.getValue())
      {
        keyMappings.put(value, entry.getKey());
      }
    }
    return keyMappings;
  }

  private Map<URI, Integer> createEndpoints(int n) throws URISyntaxException, ServiceUnavailableException
  {
    Map<URI, Integer> endpoints = new HashMap<URI, Integer>();
    for (int ii=0; ii<n; ++ii)
    {
      endpoints.put(new URI("test" + String.valueOf(ii)), 100);
    }
    return endpoints;
  }

  private void checkBatchCoverage(Set<Integer> keys, Map<URI, Set<Integer>> batchedKeys)
  {
    Set<Integer> mergedBatches = new HashSet<Integer>();
    for (Iterable<Integer> batch : batchedKeys.values())
    {
      boolean batchEmpty = true;
      for (Integer key : batch)
      {
        batchEmpty = false;
        Assert.assertTrue(keys.contains(key));
        Assert.assertFalse(mergedBatches.contains(key));
        mergedBatches.add(key);
      }
      Assert.assertFalse(batchEmpty);
    }
  }

  private ConsistentHashKeyMapper getKeyToHostMapper(Map<URI, Integer> endpoints)
  {
    ConsistentHashRing<URI> testRing = new ConsistentHashRing<URI>(endpoints);
    ConsistentHashKeyMapper batcher = new ConsistentHashKeyMapper(new StaticRingProvider(testRing), new TestPartitionInfoProvider());

    return batcher;
  }

  private static class TestPartitionInfoProvider implements PartitionInfoProvider
  {
    @Override
    public <K> HostToKeyMapper<K> getPartitionInformation(URI serviceUri, Collection<K> keys, int limitHostPerPartition, int hash) throws ServiceUnavailableException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public PartitionAccessor getPartitionAccessor(URI serviceUri) throws ServiceUnavailableException
    {
      throw new UnsupportedOperationException();
    }
  }

  private ConsistentHashKeyMapper getKeyToHostMapper(Map<URI, Integer> endpoints, int partitionNum)
  {

    final int partitionSize = endpoints.size() / partitionNum;
    List<Map<URI, Integer>> mapList = new ArrayList<Map<URI, Integer>>();
    int count = 0;
    for(final URI uri : endpoints.keySet())
    {
      final int index = count / partitionSize;
      if (index == mapList.size())
      {
        mapList.add(new HashMap<URI, Integer>());
      }
      Map<URI, Integer> map = mapList.get(index);
      map.put(uri, endpoints.get(uri));
      count++;
    }

    List<Ring<URI>> rings = new ArrayList<Ring<URI>>();
    for (final Map<URI, Integer> map : mapList)
    {
      final ConsistentHashRing<URI> ring = new ConsistentHashRing<URI>(map);
      rings.add(ring);
    }

    return new ConsistentHashKeyMapper(new StaticRingProvider(rings), new TestPartitionInfoProvider());
  }

  public static class TestLoadBalancerStrategy implements LoadBalancerStrategy
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

  public static class TestPartitionAccessor implements PartitionAccessor
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
      if (i >= 1 && i <=3)
      {
        return 0;
      }
      else if (i >= 4 && i <=6)
      {
        return 1;
      }
      else if (i >= 7 && i <= 9)
      {
        return 2;
      }
      else if (i >= 10 && i <= 12)
      {
        return 3;
      }
      else if (i >= 13 && i <= 15)
      {
        return 4;
      }
      else
        throw new PartitionAccessException("No partition for this");
    }

    @Override
    public int getMaxPartitionId()
    {
      return 4;
    }

  }

  private class TestDeadlockPartitionAccessor implements PartitionAccessor
  {

    private int _maxPartitionId;

    public TestDeadlockPartitionAccessor(int maxPartitionId)
    {
      _maxPartitionId = maxPartitionId;
    }

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
      return i % _maxPartitionId;
    }

    @Override
    public int getMaxPartitionId()
    {
      return _maxPartitionId;
    }

  }
}

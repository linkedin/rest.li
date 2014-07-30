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
import com.linkedin.d2.balancer.util.AllPartitionsMultipleHostsResult;
import com.linkedin.d2.balancer.util.AllPartitionsResult;
import com.linkedin.d2.balancer.util.HostToKeyMapper;
import com.linkedin.d2.balancer.util.HostToKeyResult;
import com.linkedin.d2.balancer.util.KeysAndHosts;
import com.linkedin.d2.balancer.util.MapKeyHostPartitionResult;
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

  /**
   * This test getPartitionMapping if we pass a hashing provider that randomly return int
   * For the test scenario let's assume:
   * We have a d2 service called "articles". Articles is split into 4 partitions.
   * Partition 0 is hosted in foo1.com and foo3.com
   * Partition 1 is hosted in foo5.com, foo6.com and foo4.com.
   * Partition 2 has no hosts
   * Partition 3 is hosted in foo2.com
   * partition 4 is hosted in foo2.com
   *
   * Let's say
   * keys 1,2,3 are hosted in partition 0
   * keys 4, 5, 6 are hosted in partition 1,
   * keys 7,8,9 are hosted in partition 2
   * keys 10,11,12 are hosted in partition 3
   * keys 13,14,15 are hosted in partition 4
   *
   * So given serviceUri = d2://articles, keys = [1,2,3,4,9,10,13,15,16] and desired num of host = 2
   *
   * the expected return is:
   *
   * for HostToKeyMapper.getFirstResult()
   * {
   *   list:
   *   keys 1,2,3 mapped to either foo1.com or foo3.com
   *   key 4 mapped to foo5.com or foo4.com or foo6.com
   *   keys 10,13,15 mapped to foo2.com <-- this is a result of merging partition 3 and 4 together
   *   UnmappedKeys = 9 -> because there is no host
   *                  16 -> because there is no partition mapping
   * }
   *
   * for HostToKeyMapper.getResult(1) a.k.a for the 2nd iteration
   * {
   *   list:
   *   keys 1,2,3 mapped to either foo1.com or foo3.com (must not be the one from previous iteration)
   *   key 4 mapped to foo5.com/foo4.com/foo6.com but must not duplicate from previous iteration
   *   UnmappedKeys = 9, 10, 13, 15 -> because there is no host
   *                  16 -> because there is no partition mapping
   * }
   *
   * for HostToKeyMapper.getResult(0, [10,13,9,16]
   * {
   *   list:
   *   keys 10,13 mapped to foo2.com
   *   unmappedKeys 9 -> because there's no host
   *                16 -> because there is no partition mapping
   * }
   *
   * for HostToKeyMapper.getResult(2)
   * {
   *   return null
   * }
   */
  @Test
  public void testMapKeysWithLimitHost()
      throws Exception
  {

    String serviceName = "articles";
    String clusterName = "cluster";
    String path = "path";
    String strategyName = "degrader";

    //setup partition
    Map<URI,Map<Integer, PartitionData>> partitionDescriptions = new HashMap<URI, Map<Integer, PartitionData>>();

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

    List<Integer> keys = new ArrayList<Integer>();
    keys.add(1);
    keys.add(2);
    keys.add(3);
    keys.add(4);
    keys.add(9);
    keys.add(10);
    keys.add(13);
    keys.add(15);
    keys.add(16);

    ConsistentHashKeyMapper mapper = new ConsistentHashKeyMapper(balancer, balancer);
    int numHost = 2;
    HostToKeyMapper<Integer> result = mapper.mapKeysV3(serviceURI, keys, numHost);
    verifyMapKeyResultWithHost(result, foo1, foo2, foo3, foo4, foo5, foo6);

    //test stickiness
    String myStickyKey = "sticky";
    result = mapper.mapKeysV3(serviceURI, keys, numHost, myStickyKey);
    verifyMapKeyResultWithHost(result, foo1, foo2, foo3, foo4, foo5, foo6);
    Map<Integer, List<URI>> originalOrderingOfHost = getOrderingOfHostsForEachKey(result, numHost);
    int numOfMatch = 0;
    for (int i = 0; i < 100; i++)
    {
      result = mapper.mapKeysV3(serviceURI, keys, numHost, myStickyKey);
      Map<Integer, List<URI>> newOrderingOfHost = getOrderingOfHostsForEachKey(result, numHost);
      if (newOrderingOfHost.equals(originalOrderingOfHost))
      {
        numOfMatch++;
      }
    }
    Assert.assertEquals(100, numOfMatch);
  }

  /**
   * Test getAllPartitionsMultipleHosts API
   * We create a fake d2 service with a set of partitions and replicas for the partitions.
   * For the test scenario let's assume:
   * We have a d2 service called "articles". Articles is split into 4 partitions.
   * Partition 0 is hosted in foo1.com and foo3.com
   * Partition 1 is hosted in foo5.com, foo6.com and foo4.com.
   * Partition 2 has no hosts
   * Partition 3 is hosted in foo2.com
   * partition 4 is hosted in foo2.com
   *
   * Once we have this, we invoke getAllPartitionsMultipleHosts asking for 2 hosts per partition.
   * We ensure that the total number of available + unavailable partitions is 2 for each partition.
   * We do a similar thing for getAllPartitionsMultipleHosts with sticky key and additionally ensure
   * that calling the same API with the same sticky key results in the same ordering of the hosts.
   *
   * @throws Exception
   */
  @Test
  public void testAllPartitionsMultipleHost()
      throws Exception
  {

    String serviceName = "articles";
    String clusterName = "cluster";
    String path = "path";
    String strategyName = "degrader";

    //setup partition
    Map<URI,Map<Integer, PartitionData>> partitionDescriptions = new HashMap<URI, Map<Integer, PartitionData>>();

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
    int numHost = 2;
    AllPartitionsMultipleHostsResult<URI> result = mapper.getAllPartitionsMultipleHosts(serviceURI, numHost);
    verifyAllPartitionMultipleHostResult(result, numHost);
    //test stickiness
    String myStickyKey = "sticky";
    AllPartitionsMultipleHostsResult<URI> originalResult = mapper.getAllPartitionsMultipleHosts(serviceURI, numHost, myStickyKey);
    verifyAllPartitionMultipleHostResult(originalResult, numHost);
    int numOfMatch = 0;
    for (int i = 0; i < 100; i++)
    {
      result = mapper.getAllPartitionsMultipleHosts(serviceURI, numHost, myStickyKey);
      verifyAllPartitionMultipleHostResult(result, numHost);
      if (verifyResultsEqual(originalResult, result))
      {
        numOfMatch++;
      }
    }
    Assert.assertEquals(100, numOfMatch);
  }

  private boolean verifyResultsEqual(AllPartitionsMultipleHostsResult<URI> originalResult,
      AllPartitionsMultipleHostsResult<URI> result)
  {
    if (originalResult.getPartitionCount() != result.getPartitionCount())
    {
      return false;
    }
    if (!originalResult.getPartitionsWithoutEnoughHosts().equals(result.getPartitionsWithoutEnoughHosts()))
    {
      return false;
    }
    for (int i = 0; i < originalResult.getPartitionCount(); i++)
    {
      if (!originalResult.getPartitionInfo(i).equals(result.getPartitionInfo(i)))
      {
        return false;
      }
    }
    return true;
  }

  private void verifyAllPartitionMultipleHostResult(AllPartitionsMultipleHostsResult<URI> result, int numHost) {
    Assert.assertEquals(5, result.getPartitionCount());
    Map<Integer, Integer> partitionToHostsCount = new HashMap<Integer, Integer>();
    for (int i = 0; i < result.getPartitionCount(); i++)
    {
      partitionToHostsCount.put(i, result.getPartitionInfo(i).size());
    }

    for (Map.Entry<Integer, Integer> partitionToUnavailableHostCount : result.getPartitionsWithoutEnoughHosts().entrySet())
    {
      if (partitionToHostsCount.containsKey(partitionToUnavailableHostCount.getKey()))
      {
        partitionToHostsCount.put(partitionToUnavailableHostCount.getKey(),
            partitionToHostsCount.get(partitionToUnavailableHostCount.getKey()) + partitionToUnavailableHostCount.getValue());
      }
      else
      {
        partitionToHostsCount.put(partitionToUnavailableHostCount.getKey(), partitionToUnavailableHostCount.getValue());
      }
    }

    for (Map.Entry<Integer, Integer> partitionToHost : partitionToHostsCount.entrySet())
    {
      Assert.assertEquals(numHost, partitionToHost.getValue().intValue());
    }

    /* We have a d2 service called "articles". Articles is split into 4 partitions.
        * Partition 0 is hosted in foo1.com and foo3.com
        * Partition 1 is hosted in foo5.com, foo6.com and foo4.com.
        * Partition 2 has no hosts
        * Partition 3 is hosted in foo2.com
        * partition 4 is hosted in foo2.com */

    try {
      Assert.assertTrue(
          Arrays.asList(new URI[]{new URI("http://foo1.com"), new URI("http://foo3.com")}).containsAll(result.getPartitionInfo(0)));
      Assert.assertTrue(
          Arrays.asList(new URI[]{new URI("http://foo5.com"), new URI("http://foo4.com"), new URI("http://foo6.com")}).containsAll(result.getPartitionInfo(1)));
      Assert.assertEquals(result.getPartitionInfo(3).toArray(),
          new URI[]{new URI("http://foo2.com")});
      Assert.assertEquals(result.getPartitionInfo(4).toArray(),
          new URI[]{new URI("http://foo2.com")});
    }
    catch (URISyntaxException e)
    {
    }
  }

  private Map<Integer, List<URI>> getOrderingOfHostsForEachKey(HostToKeyMapper<Integer> result, int numHost)
  {
    Map<Integer, List<URI>> keyToHosts = new HashMap<Integer, List<URI>>();
    for (int i = 0; i < numHost; i++)
    {
      HostToKeyResult<URI, Integer> hostToKeyResult = result.getResult(i);
      for (KeysAndHosts<Integer> entry : hostToKeyResult.getMapResult())
      {
        Collection<Integer> keys = entry.getKeys();
        for (Integer key : keys)
        {
          List<URI> hosts = keyToHosts.get(key);
          if (hosts == null)
          {
            hosts = new ArrayList<URI>();
            keyToHosts.put(key, hosts);
          }
          hosts.add(entry.getHosts().get(0));
        }
      }
    }
    return keyToHosts;
  }

  private void verifyMapKeyResultWithHost(HostToKeyMapper<Integer> result,
      URI foo1,
      URI foo2,
      URI foo3,
      URI foo4,
      URI foo5,
      URI foo6)
  {
    Assert.assertNotNull(result);

    // Test the first iteration

    HostToKeyResult<URI, Integer> firstIteration = result.getFirstResult();
    Assert.assertEquals(firstIteration.getUnmappedKeys().size(), 2);

    Assert.assertTrue(firstIteration.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(9,
        HostToKeyResult.ErrorType.NO_HOST_AVAILABLE_IN_PARTITION)));
    Assert.assertTrue(firstIteration.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(16, HostToKeyResult.ErrorType.FAIL_TO_FIND_PARTITION)));
    Collection<KeysAndHosts<Integer>> mapResult = firstIteration.getMapResult();
    Assert.assertNotNull(mapResult);
    Assert.assertTrue(mapResult.size() == 3);
    Collection<Integer> partition0Values = new HashSet<Integer>();
    partition0Values.add(1);
    partition0Values.add(2);
    partition0Values.add(3);
    Collection<URI> partition0Hosts = new HashSet<URI>();
    partition0Hosts.add(foo1);
    partition0Hosts.add(foo3);
    Collection<Integer> partition1Values = new HashSet<Integer>();
    partition1Values.add(4);
    Collection<URI> partition1Hosts = new HashSet<URI>();
    partition1Hosts.add(foo4);
    partition1Hosts.add(foo5);
    partition1Hosts.add(foo6);
    Collection<Integer> partition34Values = new HashSet<Integer>();
    partition34Values.add(10);
    partition34Values.add(13);
    partition34Values.add(15);
    Collection<URI> partition34Hosts = new HashSet<URI>();
    partition34Hosts.add(foo2);

    URI partition0HostFirstIteration = null;
    URI partition1HostFirstIteration = null;
    URI partition34HostFirstIteration = null;
    for (KeysAndHosts<Integer> entry : mapResult)
    {
      if (equals(entry.getKeys(), partition0Values))
      {
        Assert.assertTrue(partition0Hosts.contains(entry.getHosts().get(0)));
        partition0HostFirstIteration = entry.getHosts().get(0);
      }
      else if (equals(entry.getKeys(), partition1Values))
      {
        Assert.assertTrue(partition1Hosts.contains(entry.getHosts().get(0)));
        partition1HostFirstIteration = entry.getHosts().get(0);
      }
      else if (equals(entry.getKeys(), partition34Values))
      {
        Assert.assertTrue(partition34Hosts.contains(entry.getHosts().get(0)));
        partition34HostFirstIteration = entry.getHosts().get(0);
      }
      else
      {
        Assert.fail("Values should be either for partition 0,1 or 3 and 4 merged");
      }
    }
    Assert.assertNotNull(partition0HostFirstIteration);
    Assert.assertNotNull(partition1HostFirstIteration);
    Assert.assertNotNull(partition34HostFirstIteration);
    Assert.assertEquals(partition34HostFirstIteration, foo2);

    partition0Hosts.remove(partition0HostFirstIteration);
    partition1Hosts.remove(partition1HostFirstIteration);

    //test second iteration

    HostToKeyResult<URI, Integer> secondIteration = result.getResult(1);
    Assert.assertEquals(secondIteration.getUnmappedKeys().size(), 5);
    Assert.assertTrue(secondIteration.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(9, HostToKeyResult.ErrorType.NO_HOST_AVAILABLE_IN_PARTITION)));
    Assert.assertTrue(secondIteration.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(10, HostToKeyResult.ErrorType.NO_HOST_AVAILABLE_IN_PARTITION)));
    Assert.assertTrue(secondIteration.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(13, HostToKeyResult.ErrorType.NO_HOST_AVAILABLE_IN_PARTITION)));
    Assert.assertTrue(secondIteration.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(15, HostToKeyResult.ErrorType.NO_HOST_AVAILABLE_IN_PARTITION)));
    Assert.assertTrue(secondIteration.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(16, HostToKeyResult.ErrorType.FAIL_TO_FIND_PARTITION)));
    mapResult = secondIteration.getMapResult();
    URI partition0HostSecondIteration = null;
    URI partition1HostSecondIteration = null;

    for (KeysAndHosts<Integer> entry : mapResult)
    {
      if (equals(entry.getKeys(), partition0Values))
      {
        Assert.assertTrue(partition0Hosts.contains(entry.getHosts().get(0)));
        partition0HostSecondIteration = entry.getHosts().get(0);
      }
      else if (equals(entry.getKeys(), partition1Values))
      {
        Assert.assertTrue(partition1Hosts.contains(entry.getHosts().get(0)));
        partition1HostSecondIteration = entry.getHosts().get(0);
      }
      else
      {
        Assert.fail("Values should be either for partition 0,1 or 3 and 4 merged");
      }
    }

    Assert.assertNotEquals(partition0HostFirstIteration, partition0HostSecondIteration);
    Assert.assertNotEquals(partition1HostFirstIteration, partition1HostSecondIteration);

    //test third iteration

    HostToKeyResult<URI, Integer> thirdIteration = result.getResult(2);
    Assert.assertNull(thirdIteration);

    //test getResult with subset of keys
    Collection<Integer> subsetKeys = new HashSet<Integer>();
    subsetKeys.add(10);
    subsetKeys.add(13);
    subsetKeys.add(9);
    subsetKeys.add(16);
    HostToKeyResult<URI, Integer> subsetKeyResult = result.getResult(0, subsetKeys);
    Assert.assertNotNull(subsetKeyResult);
    Assert.assertEquals(subsetKeyResult.getUnmappedKeys().size(), 2);
    Assert.assertTrue(subsetKeyResult.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(9, HostToKeyResult.ErrorType.NO_HOST_AVAILABLE_IN_PARTITION)));
    Assert.assertTrue(subsetKeyResult.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(16, HostToKeyResult.ErrorType.FAIL_TO_FIND_PARTITION)));
    mapResult = subsetKeyResult.getMapResult();
    Assert.assertEquals(mapResult.size(), 1);
    for (KeysAndHosts<Integer> entry : mapResult)
    {
      Assert.assertEquals(entry.getHosts().get(0), foo2);
      Assert.assertTrue(entry.getKeys().contains(new Integer(10)));
      Assert.assertTrue(entry.getKeys().contains(new Integer(13)));
    }
  }

  private <E> boolean equals(Collection<E> one, Collection<E> two)
  {
    if (one.size() != two.size())
    {
      return false;
    }
    Collection<E> list2 = new ArrayList<E>(two);
    for (E e : one)
    {
      boolean isInTwo = list2.contains(e);
      if (isInTwo)
      {
        list2.remove(e);
      }
      else
      {
        return false;
      }
    }
    return list2.isEmpty();
  }

  /**
   * This test getPartitionMapping if we pass a hashing provider that randomly return int
   * For the test scenario let's assume:
   * We have a d2 service called "articles". Articles is split into 3 partitions.
   * Partition 0 is hosted in foo1.com and foo2.com
   * Partition 1 is hosted in bar1.com, bar2.com and bar3.com.
   * Partition 2 has no hosts
   * Let's say keys 1,2,3 are hosted in partition 0 and keys 4, 5, 6 are hosted in partition 1,
   * and keys 7,8,9 are hosted in partition 2, and lastly keys 100,101,102 are hosted in partition 3.
   *
   * So given serviceUri = d2://articles, keys = [1,2,3,4,9,10], limitNumHostsPerPartition = 2
   *
   * returns:
   * {
   *   0: hostUris = [foo1.com, foo2.com], keys = [1,2,3]
   *   1: hostUris = [bar1.com, bar2.com], keys = [4]
   *   2: hostUris = [], keys = [9]
   *
   *   unmappedKeys = [10]
   * }
   */
  @Test
  public void testGetPartitionMapping() throws Exception
  {
    String serviceName = "articles";
    String clusterName = "cluster";
    String path = "path";
    String strategyName = "degrader";

    //setup partition
    Map<URI,Map<Integer, PartitionData>> partitionDescriptions = new HashMap<URI, Map<Integer, PartitionData>>();

    final URI foo1 = new URI("http://foo1.com");
    Map<Integer, PartitionData> foo1Data = new HashMap<Integer, PartitionData>();
    foo1Data.put(0, new PartitionData(1.0));
    partitionDescriptions.put(foo1, foo1Data);

    final URI foo2 = new URI("http://foo2.com");
    Map<Integer, PartitionData> foo2Data = new HashMap<Integer, PartitionData>();
    foo2Data.put(0, new PartitionData(1.0));
    partitionDescriptions.put(foo2, foo2Data);

    final URI bar1 = new URI("http://bar1.com");
    Map<Integer, PartitionData> bar1Data = new HashMap<Integer, PartitionData>();
    bar1Data.put(1, new PartitionData(1.0));
    partitionDescriptions.put(bar1, bar1Data);

    final URI bar2 = new URI("http://bar2.com");
    Map<Integer, PartitionData> bar2Data = new HashMap<Integer, PartitionData>();
    bar2Data.put(1, new PartitionData(1.0));
    partitionDescriptions.put(bar2, bar2Data);

    final URI bar3 = new URI("http://bar3.com");
    Map<Integer, PartitionData> bar3Data = new HashMap<Integer, PartitionData>();
    bar3Data.put(1, new PartitionData(1.0));
    partitionDescriptions.put(bar3, bar3Data);

    //setup strategy which involves tweaking the hash ring to get partitionId -> URI host
    List<LoadBalancerState.SchemeStrategyPair> orderedStrategies = new ArrayList<LoadBalancerState.SchemeStrategyPair>();
    LoadBalancerStrategy strategy = new LoadBalancerStrategy()
    {
      @Override
      public TrackerClient getTrackerClient(Request request,
          RequestContext requestContext,
          long clusterGenerationId,
          int partitionId,
          List<TrackerClient> trackerClients)
      {
        throw new UnsupportedOperationException();
      }

      Map<URI, Integer> pointsMap1 = new HashMap<URI, Integer>();
      Map<URI, Integer> pointsMap2 = new HashMap<URI, Integer>();


      @Override
      public Ring<URI> getRing(long clusterGenerationId, int partitionId, List<TrackerClient> trackerClients)
      {
        //partition 0
        pointsMap1.put(foo1, 100);
        pointsMap1.put(foo2, 100);

        //partition 1
        pointsMap2.put(bar1, 100);
        pointsMap2.put(bar2, 100);
        pointsMap2.put(bar3, 100);

        if (partitionId == 0)
        {
          return new ConsistentHashRing<URI>(pointsMap1);
        }
        else if (partitionId == 1)
        {
          return new ConsistentHashRing<URI>(pointsMap2);
        }
        else
        {
          return new ConsistentHashRing<URI>(new HashMap<URI, Integer>());
        }
      }

      public Ring<URI> getRing(long clusterGenerationId,
          int partitionId,
          List<TrackerClient> trackerClients,
          List<URI> excludedURIs)
      {
        Map<URI, Integer> map;
        if (partitionId == 0)
        {
          map = new HashMap<URI, Integer>(pointsMap1);
          for (URI uri : excludedURIs)
          {
            map.remove(uri);
          }
          return new ConsistentHashRing<URI>(map);
        }
        else if (partitionId == 1)
        {
          map = new HashMap<URI, Integer>(pointsMap2);
          for (URI uri : excludedURIs)
          {
            map.remove(uri);
          }
          return new ConsistentHashRing<URI>(map);
        }
        else
        {
          return new ConsistentHashRing<URI>(new HashMap<URI, Integer>());
        }
      }
    };

    orderedStrategies.add(new LoadBalancerState.SchemeStrategyPair("http", strategy));

    //setup the partition accessor which is used to get partitionId -> keys
    PartitionAccessor accessor = new PartitionAccessor()
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
        else if (i == 100 || i == 101 || i == 102)
        {
          return 3;
        }
        else
          throw new PartitionAccessException("No partition for this");
      }

      @Override
      public int getMaxPartitionId()
      {
        throw new UnsupportedOperationException();
      }
    };
    URI serviceURI = new URI("d2://" + serviceName);
    SimpleLoadBalancer balancer = new SimpleLoadBalancer(new PartitionedLoadBalancerTestState(
        clusterName, serviceName, path, strategyName, partitionDescriptions, orderedStrategies,
        accessor
    ));
    List<Integer> keys = new ArrayList<Integer>();
    keys.add(1);
    keys.add(2);
    keys.add(3);
    keys.add(4);
    keys.add(9);
    keys.add(10);

    ConsistentHashKeyMapper mapper = new ConsistentHashKeyMapper(balancer, balancer);
    int numHost = 2;
    MapKeyHostPartitionResult<Integer> result = mapper.getPartitionInfo(serviceURI, keys, numHost);
    List<URI> partition0Uris = new ArrayList<URI>();
    partition0Uris.add(foo1);
    partition0Uris.add(foo2);
    List<URI> partition1Uris = new ArrayList<URI>();
    partition1Uris.add(bar1);
    partition1Uris.add(bar2);
    partition1Uris.add(bar3);
    verifyPartitionResult(result, numHost, partition0Uris, partition1Uris);

    /**
     * the following tests getPartitionMapping if we pass a hashing provider that uses stickiness
     */
    String myStickyKey = "sticky";
    result = mapper.getPartitionInfo(serviceURI, keys, numHost, myStickyKey);
    verifyPartitionResult(result, numHost, partition0Uris, partition1Uris);
    //verify that the order of hosts always the same every time we call getPartitionInfo
    List<URI> originalPartition0 = result.getPartitionInfoMap().get(new Integer(0)).getHosts();
    List<URI> originalPartition1 = result.getPartitionInfoMap().get(new Integer(1)).getHosts();

    for (int i = 0; i < 100; i++)
    {
      result = mapper.getPartitionInfo(serviceURI, keys, numHost, myStickyKey);
      verifyPartitionResult(result, numHost, partition0Uris, partition1Uris);
      List<URI> newPartition0 = result.getPartitionInfoMap().get(new Integer(0)).getHosts();
      List<URI> newPartition1 = result.getPartitionInfoMap().get(new Integer(1)).getHosts();
      Assert.assertEquals(newPartition0, originalPartition0);
      Assert.assertEquals(newPartition1, originalPartition1);
    }

  }

  private void verifyPartitionResult(MapKeyHostPartitionResult<Integer> result,
      int numHost,
      List<URI> partition0Host,
      List<URI> partition1Host)
  {
    Assert.assertNotNull(result);
    Collection<Integer> unmappedKeys = result.getUnmappedKeys();
    Assert.assertTrue(unmappedKeys.size() == 1);
    for (Integer i : unmappedKeys)
    {
      Assert.assertEquals(i, new Integer(10));
    }
    Map<Integer, KeysAndHosts<Integer>> partitionInfoMap = result.getPartitionInfoMap();
    //we should only return partition 0,1,2. Partition 3 won't be returned because we didn't pass key 100,101 and 102
    Assert.assertTrue(partitionInfoMap.size() == 3);

    //checks partition 0
    KeysAndHosts<Integer> kh0 = partitionInfoMap.get(new Integer(0));
    List<URI> hosts0 = kh0.getHosts();
    Assert.assertTrue(hosts0.size() == numHost);
    Assert.assertTrue(hosts0.contains(partition0Host.get(0)));
    Assert.assertTrue(hosts0.contains(partition0Host.get(1)));
    Collection<Integer> keys0 = kh0.getKeys();
    Assert.assertTrue(keys0.size() == 3);
    Assert.assertTrue(keys0.contains(new Integer(1)));
    Assert.assertTrue(keys0.contains(new Integer(2)));
    Assert.assertTrue(keys0.contains(new Integer(3)));

    //checks partition 1
    KeysAndHosts<Integer> kh1 = partitionInfoMap.get(new Integer(1));
    List<URI> hosts1 = kh1.getHosts();
    Assert.assertTrue(hosts1.size() == numHost);
    for (URI host : hosts1)
    {
      if (host != partition1Host.get(0) && host != partition1Host.get(1) && host != partition1Host.get(2))
      {
        Assert.fail("partition 1 should contain 2 URIs. They should be a combination of bar1, bar2 or bar3 ");
      }
    }
    Collection<Integer> keys1 = kh1.getKeys();
    Assert.assertTrue(keys1.size() == 1);
    Assert.assertTrue(keys1.contains(new Integer(4)));

    //checks partition 2 which should ont have any host based on our setup
    KeysAndHosts<Integer> kh2 = partitionInfoMap.get(new Integer(2));
    List<URI> hosts2 = kh2.getHosts();
    Assert.assertTrue(hosts2.isEmpty());
    Collection<Integer> keys2 = kh2.getKeys();
    Assert.assertTrue(keys2.size() == 1);
    Assert.assertTrue(keys2.contains(new Integer(9)));
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
    checkBatchLoad(keys, batchedKeys, 1.0/100.0);
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

    AllPartitionsResult<URI> uriResult = batcher.getAllPartitions(URI.create("d2://fooservice"));
    Assert.assertEquals(uriResult.getPartitionCount(), 3);
    Assert.assertEquals(uriResult.getUnavailablePartitions().size(), 0);
    Assert.assertTrue(uriResult.getPartitionInfo().size() <= 3);
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
    public <K> MapKeyHostPartitionResult<K> getPartitionInformation(URI serviceUri,
        Collection<K> keys,
        int limitHostPerPartition,
        HashProvider hashProvider)
        throws ServiceUnavailableException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public PartitionAccessor getPartitionAccessor(URI serviceUri)
        throws ServiceUnavailableException
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public AllPartitionsMultipleHostsResult<URI> getAllPartitionMultipleHosts(URI serviceUri, int numHostPerPartition,
        HashProvider hashProvider)
        throws ServiceUnavailableException
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

  private class TestLoadBalancerStrategy implements LoadBalancerStrategy
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

  private class TestPartitionAccessor implements PartitionAccessor
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
}

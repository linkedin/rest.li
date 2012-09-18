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
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.AllPartitionsResult;
import com.linkedin.d2.balancer.util.MapKeyResult;
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
    checkBatchLoad(keys, batchedKeys, 1.0/1000.0);
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
    ConsistentHashKeyMapper batcher = new ConsistentHashKeyMapper(new StaticRingProvider(testRing));

    return batcher;
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

    return new ConsistentHashKeyMapper(new StaticRingProvider(rings));
  }

}

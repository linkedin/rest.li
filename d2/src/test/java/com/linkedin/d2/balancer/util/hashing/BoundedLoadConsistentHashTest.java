/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util.hashing;

import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.strategies.degrader.MPConsistentHashRingFactory;
import com.linkedin.d2.balancer.strategies.degrader.PointBasedConsistentHashRingFactory;
import com.linkedin.d2.balancer.strategies.degrader.RingFactory;
import com.linkedin.util.degrader.CallTracker;
import com.linkedin.util.degrader.CallTrackerImpl;
import com.linkedin.util.degrader.DegraderImpl;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;


/**
 * @author Rick Zhou
 */
public class BoundedLoadConsistentHashTest
{
  private static final int TEST_ITERATION_NUMBER = 1000;
  private static final double BOUNDED_LOAD_BALANCING_FACTOR = 1.25;

  private Random _random;
  private Map<URI, Integer> _pointsMap;
  private Map<URI, Integer> _loadMap;

  @BeforeMethod
  public void setUp()
  {
    _random = new Random(0);
    _pointsMap = new HashMap<>();
    _loadMap = new HashMap<>();
  }

  private DegraderLoadBalancerStrategyConfig getConfig(String hashAlgorithm)
  {
    return new DegraderLoadBalancerStrategyConfig(1000, DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_ONLY_AT_INTERVAL,
        100, null, Collections.<String, Object>emptyMap(), DegraderLoadBalancerStrategyConfig.DEFAULT_CLOCK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_INITIAL_RECOVERY_LEVEL,
        DegraderLoadBalancerStrategyConfig.DEFAULT_RAMP_FACTOR,
        DegraderLoadBalancerStrategyConfig.DEFAULT_HIGH_WATER_MARK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_LOW_WATER_MARK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_UP,
        DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_DOWN,
        DegraderLoadBalancerStrategyConfig.DEFAULT_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_HASHRING_POINT_CLEANUP_RATE, hashAlgorithm,
        DegraderLoadBalancerStrategyConfig.DEFAULT_NUM_PROBES,
        DegraderLoadBalancerStrategyConfig.DEFAULT_POINTS_PER_HOST,
        BOUNDED_LOAD_BALANCING_FACTOR, null,
        DegraderLoadBalancerStrategyConfig.DEFAULT_QUARANTINE_MAXPERCENT, null, null, DegraderLoadBalancerStrategyConfig.DEFAULT_QUARANTINE_METHOD,
        null, DegraderImpl.DEFAULT_LOW_LATENCY, null,
        DegraderLoadBalancerStrategyConfig.DEFAULT_LOW_EVENT_EMITTING_INTERVAL,
        DegraderLoadBalancerStrategyConfig.DEFAULT_HIGH_EVENT_EMITTING_INTERVAL,
        DegraderLoadBalancerStrategyConfig.DEFAULT_CLUSTER_NAME);
  }

  @DataProvider(name = "ringFactories")
  public Object[][] getRingFactories()
  {
    RingFactory<URI> pointBased = new PointBasedConsistentHashRingFactory<>(getConfig("pointBased"));
    RingFactory<URI> multiProbe = new MPConsistentHashRingFactory<>(5, MPConsistentHashRing.DEFAULT_POINTS_PER_HOST);
    return new Object[][]{{pointBased}, {multiProbe}};
  }

  @Test(dataProvider = "ringFactories")
  public void testCapacityOneItem(RingFactory<URI> ringFactory)
  {
    URI uri = URI.create("http://test.linkedin.com");
    _pointsMap.put(uri, 5);
    _loadMap.put(uri, 10);
    BoundedLoadConsistentHashRing<URI> test = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);

    test.get(0);

    int totalCapacity = (int) Math.ceil(11 * BOUNDED_LOAD_BALANCING_FACTOR);
    assertEquals(test.getCapacity(uri), totalCapacity);
  }

  @Test(dataProvider = "ringFactories")
  public void testCapacityOneItemStrictBalance(RingFactory<URI> ringFactory)
  {
    URI uri = URI.create("http://test.linkedin.com");
    _pointsMap.put(uri, 5);
    _loadMap.put(uri, 10);
    BoundedLoadConsistentHashRing<URI> test = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), 1);

    test.get(0);

    assertEquals(test.getCapacity(uri), 11);
  }

  @Test(dataProvider = "ringFactories")
  public void testCapacityTwoItemsEqualWeight(RingFactory<URI> ringFactory)
  {
    URI uri1 = URI.create("http://test1.linkedin.com");
    URI uri2 = URI.create("http://test2.linkedin.com");

    _pointsMap.put(uri1, 5);
    _pointsMap.put(uri2, 5);
    _loadMap.put(uri1, 3);
    _loadMap.put(uri2, 5);

    BoundedLoadConsistentHashRing<URI> test = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);

    test.get(0);

    int totalCapacity = (int) Math.ceil((3 + 5 + 1) * BOUNDED_LOAD_BALANCING_FACTOR);
    assertEquals(test.getCapacity(uri1), totalCapacity / 2);
    assertEquals(test.getCapacity(uri2), totalCapacity / 2);
  }

  @Test(dataProvider = "ringFactories")
  public void testCapacityTwoItemsUnequalWeight(RingFactory<URI> ringFactory)
  {
    URI uri1 = URI.create("http://test1.linkedin.com");
    URI uri2 = URI.create("http://test2.linkedin.com");

    _pointsMap.put(uri1, 2);
    _pointsMap.put(uri2, 3);
    _loadMap.put(uri1, 3);
    _loadMap.put(uri2, 4);

    BoundedLoadConsistentHashRing<URI> test = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);

    test.get(0);

    int totalCapacity = (int) Math.ceil((3 + 4 + 1) * BOUNDED_LOAD_BALANCING_FACTOR);
    assertEquals(test.getCapacity(uri1), totalCapacity * 2 / 5);
    assertEquals(test.getCapacity(uri2), totalCapacity * 3 / 5);
  }

  /**
   * If all the servers have equal weights, the min and max capacities should be at most 1 request apart
   */
  @Test(dataProvider = "ringFactories")
  public void testCapacityMultipleItemsEqualWeight(RingFactory<URI> ringFactory)
  {
    for (int i = 0; i < 100; i++)
    {
      URI uri = URI.create("http://test" + i + ".linkedin.com");
      _pointsMap.put(uri, 2);
      _loadMap.put(uri, 3);
    }

    BoundedLoadConsistentHashRing<URI> test = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);

    test.get(0);

    int minCapacity = Integer.MAX_VALUE;
    int maxCapacity = 0;
    int totalCapacity = 0;

    for (int i = 0; i < 100; i++)
    {
      URI uri = URI.create("http://test" + i + ".linkedin.com");
      int capacity = test.getCapacity(uri);
      minCapacity = Integer.min(minCapacity, capacity);
      maxCapacity = Integer.max(maxCapacity, capacity);
      totalCapacity += capacity;
    }

    assertTrue(maxCapacity - minCapacity <= 1);
    assertEquals(totalCapacity, (int) Math.ceil(301 * BOUNDED_LOAD_BALANCING_FACTOR));
  }

  @Test(dataProvider = "ringFactories")
  public void testCapacityMultipleItemsUnequalWeight(RingFactory<URI> ringFactory)
  {
    for (int i = 0; i < 100; i++)
    {
      URI uri = URI.create("http://test" + i + ".linkedin.com");
      _pointsMap.put(uri, _random.nextInt(3) + 10);
      _loadMap.put(uri, 10);
    }

    BoundedLoadConsistentHashRing<URI> test = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);

    test.get(0);

    int totalCapacity = (int) Math.ceil(1001 * BOUNDED_LOAD_BALANCING_FACTOR);
    int totalPoints = _pointsMap.values()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .sum();

    // no server should exceed its fair share of capacity by 1 request
    for (int i = 0; i < 100; i++)
    {
      URI uri = URI.create("http://test" + i + ".linkedin.com");
      assertTrue(Math.abs(test.getCapacity(uri) - totalCapacity * ((double) _pointsMap.get(uri) / totalPoints)) <= 1);
    }
  }

  @Test(dataProvider = "ringFactories")
  public void testGetZeroItems(RingFactory<URI> ringFactory)
  {
    BoundedLoadConsistentHashRing<URI> test = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);

    assertNull(test.get(0));
  }

  @Test(dataProvider = "ringFactories")
  public void testGetOneItem(RingFactory<URI> ringFactory)
  {
    URI uri = URI.create("http://test.linkedin.com");
    _pointsMap.put(uri, 5);
    _loadMap.put(uri, 10);

    BoundedLoadConsistentHashRing<URI> test = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);

    for (int i = 0; i < TEST_ITERATION_NUMBER; i++)
    {
      int key = _random.nextInt();
      assertEquals(test.get(key), uri);
    }
  }

  @Test(dataProvider = "ringFactories")
  public void testTwoItemsWithOverload(RingFactory<URI> ringFactory)
  {
    URI idle = URI.create("http://testIdle.linkedin.com");
    URI overload = URI.create("http://testOverload.linkedin.com");

    _pointsMap.put(idle, 2);
    _pointsMap.put(overload, 2);

    _loadMap.put(idle, 10);
    _loadMap.put(overload, 50);

    BoundedLoadConsistentHashRing<URI> test = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);

    for (int i = 0; i < TEST_ITERATION_NUMBER; i++)
    {
      int key = _random.nextInt();
      assertEquals(test.get(key), idle);
    }
  }

  @Test(dataProvider = "ringFactories")
  public void testTwoItemsWithoutOverload(RingFactory<URI> ringFactory)
  {
    URI idle1 = URI.create("http://testIdle1.linkedin.com");
    URI idle2 = URI.create("http://testIdle2.linkedin.com");

    _pointsMap.put(idle1, 2);
    _pointsMap.put(idle2, 2);

    // Two non-full servers, should behave exactly the same as strict consistent hashing
    _loadMap.put(idle1, 10);
    _loadMap.put(idle2, 11);

    BoundedLoadConsistentHashRing<URI> test = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);
    BoundedLoadConsistentHashRing<URI> strict = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), Integer.MAX_VALUE);

    for (int i = 0; i < TEST_ITERATION_NUMBER; i++)
    {
      int key = _random.nextInt();
      assertEquals(test.get(key), strict.get(key));
    }
  }

  private <T> Integer getMinFromCallTrackerMap(Map<T, CallTracker> callTrackerMap) {
    return callTrackerMap
        .values()
        .stream()
        .map(CallTracker::getCurrentConcurrency)
        .min(Comparator.comparingInt(Integer::intValue))
        .orElse(null);
  }

  private <T> Integer getMaxFromCallTrackerMap(Map<T, CallTracker> callTrackerMap) {
    return callTrackerMap
        .values()
        .stream()
        .map(CallTracker::getCurrentConcurrency)
        .max(Comparator.comparingInt(Integer::intValue))
        .orElse(null);
  }

  private <T> void assertLoadOK(BoundedLoadConsistentHashRing<T> ring, Map<T, CallTracker> callTrackerMap, T targetServer)
  {
    int capacity = ring.getCapacity(targetServer);
    assertTrue(callTrackerMap.get(targetServer).getCurrentConcurrency() <= capacity);
  }

  @Test(dataProvider = "ringFactories")
  public void testBalancedLoad(RingFactory<URI> ringFactory)
  {
    for (int i = 0; i < 5; i++)
    {
      URI uri = URI.create("http://test" + i + ".linkedin.com");
      _pointsMap.put(uri, 1);
      _loadMap.put(uri, 0);
    }

    Map<URI, CallTracker> callTrackerMap1 = createCallTrackerMap(_loadMap);
    Map<URI, CallTracker> callTrackerMap2 = createCallTrackerMap(_loadMap);

    // test1 should be more balanced than test2
    BoundedLoadConsistentHashRing<URI> test1 = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, callTrackerMap1, 1.2);
    BoundedLoadConsistentHashRing<URI> test2 = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, callTrackerMap2, 2.2);

    for (int i = 0; i < TEST_ITERATION_NUMBER; i++)
    {
      int key = _random.nextInt();
      URI server1 = test1.get(key);
      assertLoadOK(test1, callTrackerMap1, server1);
      callTrackerMap1.get(server1).startCall();

      URI server2 = test2.get(key);
      assertLoadOK(test2, callTrackerMap2, server2);
      callTrackerMap2.get(server2).startCall();
    }

    Integer minLoad1 = getMinFromCallTrackerMap(callTrackerMap1);
    Integer maxLoad1 = getMaxFromCallTrackerMap(callTrackerMap1);

    Integer minLoad2 = getMinFromCallTrackerMap(callTrackerMap2);
    Integer maxLoad2 = getMaxFromCallTrackerMap(callTrackerMap2);

    Assert.assertTrue(maxLoad1 - minLoad1 < maxLoad2 - minLoad2);
  }

  @Test(dataProvider = "ringFactories")
  public void testIteratorOneItem(RingFactory<URI> ringFactory)
  {
    for (int i = 0; i < 100; i++)
    {
      URI uri = URI.create("http://test" + i + ".linkedin.com");
      _pointsMap.put(uri, 10);
      _loadMap.put(uri, 0);
    }

    Ring<URI> ring = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);

    int key = _random.nextInt();
    Iterator<URI> iter = ring.getIterator(key);
    Assert.assertTrue(iter.hasNext());
    Assert.assertSame(iter.next(), ring.get(key));
  }

  @Test(dataProvider = "ringFactories")
  public void testIteratorOtherItems(RingFactory<URI> ringFactory)
  {
    for (int i = 0; i < 100; i++)
    {
      URI uri = URI.create("http://test" + i + ".linkedin.com");
      _pointsMap.put(uri, 10);
      _loadMap.put(uri, 0);

    }

    Ring<URI> ring = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);

    int key = _random.nextInt();
    Iterator<URI> iter = ring.getIterator(key);
    int iterations = 0;
    Set<URI> iterResults = new HashSet<>();
    while (iter.hasNext())
    {
      iterResults.add(iter.next());
      iterations++;
    }

    //test iteration should equal to number of hosts so no duplicates
    assertEquals(iterations, 100);

    for (URI host : _pointsMap.keySet())
    {
      Assert.assertTrue(iterResults.contains(host));
    }
  }

  @Test(dataProvider = "ringFactories")
  public void testStickyOrdering(RingFactory<URI> ringFactory)
  {
    for (int i = 0; i < 100; i++)
    {
      URI uri = URI.create("http://test" + i + ".linkedin.com");
      _pointsMap.put(uri, 10);
      _loadMap.put(uri, 0);

    }

    for (int i = 0; i < TEST_ITERATION_NUMBER; i++)
    {
      int key = _random.nextInt();

      Ring<URI> firstRing = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);
      Iterator<URI> firstIter = firstRing.getIterator(key);

      Ring<URI> secondRing = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);
      Iterator<URI> secondIter = secondRing.getIterator(key);

      while (firstIter.hasNext() || secondIter.hasNext())
      {
        Assert.assertSame(firstIter.next(), secondIter.next());
      }
    }
  }

  @Test(dataProvider = "ringFactories")
  public void testNoDeadloop(RingFactory<URI> ringFactory)
  {
    for (int i = 0; i < TEST_ITERATION_NUMBER; i++)
    {
      _pointsMap = new HashMap<>();
      int numHosts = Math.abs(_random.nextInt()) % 100;

      for (int j = 0; j < numHosts; j++)
      {
        URI uri = URI.create("http://test" +j + ".linkedin.com");
        _pointsMap.put(uri, 10);
        _loadMap.put(uri, 0);

      }

      Ring<URI> ring = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);
      Iterator<URI> iter = ring.getIterator(_random.nextInt());
      int iteration = 0;
      while (iter.hasNext())
      {
        iter.next();
        iteration++;
      }

      assertEquals(iteration, numHosts);
    }
  }

  @Test(dataProvider = "ringFactories")
  public void testEmptyRing(RingFactory<URI> ringFactory)
  {
    for (int i = 0; i < TEST_ITERATION_NUMBER; i++)
    {
      int key = _random.nextInt();
      Ring<URI> ring = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);
      Assert.assertNull(ring.get(key));

      Iterator<URI> iterator = ring.getIterator(key);
      Assert.assertFalse(iterator.hasNext());
    }

    URI uri1 = URI.create("http://test1.linkedin.com");
    URI uri2 = URI.create("http://test2.linkedin.com");

    _pointsMap.put(uri1, 0);
    _pointsMap.put(uri2, 0);

    for (int i = 0; i < TEST_ITERATION_NUMBER; i++)
    {
      int key = _random.nextInt();
      Ring<URI> ring = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, createCallTrackerMap(_loadMap), BOUNDED_LOAD_BALANCING_FACTOR);
      Assert.assertNull(ring.get(key));

      Iterator<URI> iterator = ring.getIterator(key);
      Assert.assertFalse(iterator.hasNext());
    }
  }

  @Test(dataProvider = "ringFactories")
  public void testThreadSafe(RingFactory<URI> ringFactory)
  {
    for (int i = 0; i < 100; i++)
    {
      URI uri = URI.create("http://test" + i + ".linkedin.com");
      _pointsMap.put(uri, 2);
      _loadMap.put(uri, 3);
    }

    Map<URI, CallTracker> callTrackerMap = createCallTrackerMap(_loadMap);
    BoundedLoadConsistentHashRing<URI> test = new BoundedLoadConsistentHashRing<>(ringFactory, _pointsMap, callTrackerMap, BOUNDED_LOAD_BALANCING_FACTOR);

    for (int i = 0; i < 100; i++)
    {
      new Thread(() ->
      {
        for (int j = 0; j < TEST_ITERATION_NUMBER; j++)
        {
          URI host = test.get(_random.nextInt());
          callTrackerMap.get(host).startCall();
        }
      }).start();
    }
  }

  private Map<URI, CallTracker> createCallTrackerMap(Map<URI, Integer> loadMap)
  {
    Map<URI, CallTracker> callTrackerMap = new HashMap<>();

    for (Map.Entry<URI, Integer> entry : loadMap.entrySet())
    {
      CallTracker callTracker = new CallTrackerImpl(5000L);

      IntStream.range(0, entry.getValue())
          .forEach(e -> callTracker.startCall());

      callTrackerMap.put(entry.getKey(), callTracker);
    }

    return callTrackerMap;
  }
}

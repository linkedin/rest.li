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

import com.linkedin.d2.balancer.strategies.degrader.RingFactory;
import com.linkedin.util.degrader.CallTracker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A bounded-load consistent hash ring based on the following paper:
 * <a href="https://arxiv.org/pdf/1608.01350.pdf">Consistent Hashing with Bounded Loads</a>
 * and RFC:
 * <a href="https://docs.google.com/document/d/1IF6bDxe3ba_pDWknYUHmtdqYlSwBu2AvMU4tcxmtdA8/edit?usp=sharing">
 *   Improving consistent hashing with bounded loads</a>
 *
 * The algorithm sets an upper limit on any host's load with respect to the average load of all the hosts,
 * and will forward the jobs from the full host to the next non-full host on the hash ring. We use the number
 * of concurrent inflight requests to measure the load. BalanceFactor ensures that no
 * host has a load higher than (sum_of_loads / num_of_hosts) * balanceFactor.
 *
 * The implementation of the bounded-load algorithm is a decorator on top of any existing consistent hash rings.
 * It directly calls its underlying consistent hash ring to get the most wanted host for a key.
 *
 * The implementation is thread-safe.
 *
 * @author Rick Zhou
 */
public class BoundedLoadConsistentHashRing<T> implements Ring<T>
{
  private static final Logger LOG = LoggerFactory.getLogger(ConsistentHashRing.class);

  private final Map<T, Integer> _pointsMap;
  private final Map<T, CallTracker> _callTrackerMap;
  private final int _totalPoints;
  private final Map<T, Integer> _hosts;
  private final double _boundedLoadBalanceFactor;
  private final Ring<T> _ring;
  private final Lock _lock;

  private volatile LoadDistribution<T> _loadDistribution;

  /**
   * Creates a bounded-load consistent hash ring with the underlying hash ring, points per host, callTrackerMap and balanceFactor
   *
   * @param ringFactory   The factory used to generate the underlying hash ring for bounded-load algorithm
   * @param pointsMap     A map between object to store in the ring and its points. The more points
   *                      one has, the higher its weight is.
   * @param callTrackerMap A map between object to store in the ring and its {@link CallTracker}. CallTracker will
   *                       be used to get the number of concurrent inflight requests of each host
   * @param boundedLoadBalanceFactor A double always greater than 1. The higher the number, the more consistent and less balanced the hash ring
   */
  public BoundedLoadConsistentHashRing(RingFactory<T> ringFactory, Map<T, Integer> pointsMap,
      Map<T, CallTracker> callTrackerMap, double boundedLoadBalanceFactor)
  {
    _pointsMap = pointsMap;
    _callTrackerMap = callTrackerMap;
    _hosts = new HashMap<>();
    _boundedLoadBalanceFactor = boundedLoadBalanceFactor;
    _ring = ringFactory.createRing(pointsMap);
    _lock = new ReentrantLock();
    _totalPoints = initHostCumulativePoints(pointsMap);
  }

  /**
   * A helper method to initialize cumulative points for each host. Given an ordering of all the hosts, the
   * cumulative point for a host is the sum of the points of all the hosts before it. With cumulative points,
   * we are able to maintain a strict ordering of the hosts, and assign capacities accurately using their cumulative
   * points.
   *
   * @param pointsMap   A map between object to store in the ring and its points.
   * @return The total points of all the hosts in the pointsMap.
   */
  private int initHostCumulativePoints(Map<T, Integer> pointsMap)
  {
    Map<T, Integer> loadMap = new HashMap<>();

    int cumulative = 0;

    for (Map.Entry<T, Integer> entry : _pointsMap.entrySet())
    {
      if (pointsMap.get(entry.getKey()) > 0)
      {
        loadMap.put(entry.getKey(), 0);
        cumulative += _pointsMap.get(entry.getKey());
        _hosts.put(entry.getKey(), cumulative);
      }
    }

    _loadDistribution = new LoadDistribution<>(loadMap, 0);

    return cumulative;
  }

  /**
   * Calculates the capacity of a given host. The capacity of a host is proportional to
   * the number of points it has. Visible for testing.
   *
   * @param host    The host to get capacity from
   * @return The capacity of the host
   */
  int getCapacity(T host)
  {
    int cumulativePoints = _hosts.get(host);
    int totalCapacity = _loadDistribution.getTotalCapacity();

    int capacityPerPoint = totalCapacity / _totalPoints;
    int remainder = totalCapacity % _totalPoints;

    // First, allocate the integer part of capacity
    int capacity = _pointsMap.get(host) * capacityPerPoint;

    // Then, distribute the remainder proportionally to their points. The following calculation ensures that
    // no server exceeds its fair share of capacity by 1 request.
    capacity += ((cumulativePoints + _pointsMap.get(host)) * remainder) / _totalPoints
        - (cumulativePoints * remainder) / _totalPoints;

    return Integer.max(1, capacity);
  }

  /**
   * Gets an ordering of the hosts based only on the key value and the mostWantedHost.
   * It shuffles the host list with a deterministic random seed, and moves the mostWantedHost
   * to the front of the list.
   *
   * @param key   The key used to deterministically shuffle the host list
   * @param mostWantedHost  The host that will be placed at the front of the list
   * @return A list of hosts deterministically shuffled by a given key, with mostWantedHost at the front
   */
  private List<T> getOrderByKey(int key, T mostWantedHost)
  {
    List<T> hosts = new ArrayList<>(_hosts.keySet());
    Collections.shuffle(hosts, new Random(key));

    if (!hosts.isEmpty())
    {
      Collections.swap(hosts, 0, hosts.indexOf(mostWantedHost));
    }
    return hosts;
  }

  /**
   * The hash ring will first update the current load of the objects using callTracker information,
   * then return an object using the bounded-load algorithm.
   *
   * Note that this method relies on the get method of the underlying hash ring to find the
   * most wanted host. It then generates a pseudorandom ordering of the hosts based on the
   * key to ensure that the same requests are more likely to go to the same non-full host.
   */
  @Nullable
  @Override
  public T get(int key)
  {
    if (_ring.isEmpty())
    {
      LOG.debug("get called on a hash ring with nothing in it");
      return null;
    }

    T mostWantedHost = _ring.get(key);

    updateLoad();

    if (_loadDistribution.getLoad(mostWantedHost) < getCapacity(mostWantedHost))
    {
      return mostWantedHost;
    }

    // When the mostWantedHost is full, we generate a pseudorandom ordering of the hosts based
    // on the key value and search for the next non-full host. For performance optimization,
    // we decide not to involve the iterator of the underlying ring here, because the getIterator
    // operation might be expensive.
    for (T host : getOrderByKey(key, mostWantedHost))
    {
      if (_loadDistribution.getLoad(host) < getCapacity(host))
      {
        return host;
      }
    }

    return mostWantedHost;
  }

  /**
   * Note that for better performance, only a single thread can update the loads at a time.
   * The other threads trying to acquire the lock that has already been granted will
   * end up using the old load map and total capacity.
   */
  private void updateLoad()
  {
    if (_lock.tryLock())
    {
      try
      {
        Map<T, Integer> newLoadMap = new HashMap<>();
        int loadSum = 0;

        for (Map.Entry<T, CallTracker> entry : _callTrackerMap.entrySet())
        {
          int load = entry.getValue().getCurrentConcurrency();
          loadSum += load;
          newLoadMap.put(entry.getKey(), load);
        }

        // Total capacity is the total number of concurrent inflight requests plus the one that we are
        // about to process times the balanceFactor, rounded up.
        int totalCapacity = (int) Math.ceil((loadSum + 1) * _boundedLoadBalanceFactor);
        _loadDistribution = new LoadDistribution<>(newLoadMap, totalCapacity);
      } finally
      {
        _lock.unlock();
      }
    }
  }

  /**
   * Get an iterator starting from a specified host. The ordering of the hosts is generated using
   * {@link #getOrderByKey(int, Object)}.
   *
   * @param key The iteration will start from the point corresponded by this key
   * @return An Iterator starting from a specified host. It contains no objects when the hash ring is empty
   */
  @Nonnull
  @Override
  public Iterator<T> getIterator(int key)
  {
    updateLoad();
    return getOrderByKey(key, get(key)).listIterator();
  }

  @Override
  public boolean isStickyRoutingCapable()
  {
    return _ring.isStickyRoutingCapable();
  }

  @Override
  public boolean isEmpty()
  {
    return _ring.isEmpty();
  }

  /**
   * Records the load distribution of all the hosts in the hash ring.
   */
  private static class LoadDistribution<T>
  {
    private final Map<T, Integer> _loadMap;
    private final int _totalCapacity;

    LoadDistribution(Map<T, Integer> loadMap, int totalCapacity)
    {
      _loadMap = loadMap;
      _totalCapacity = totalCapacity;
    }

    int getTotalCapacity()
    {
      return _totalCapacity;
    }

    int getLoad(T host)
    {
      return _loadMap.getOrDefault(host, 0);
    }
  }
}

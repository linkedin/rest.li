/*
   Copyright (c) 2018 LinkedIn Corp.

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A distribution based implementation of {@code Ring<T>} interface. This ring takes in a map of hosts and its points and construct a cumulative distribution function.
 * Host selection is based on this probability distribution instead of the given key.
 *
 * NOTE: this ring does not support sticky routing!
 */
public class DistributionNonDiscreteRing<T> implements Ring<T>
{
  private static final Logger LOG = LoggerFactory.getLogger(DistributionNonDiscreteRing.class);
  private final TreeMap<Integer, T> _cumulativePointsMap;
  private final int _totalPoints;

  public DistributionNonDiscreteRing(Map<T, Integer> pointsMap)
  {
    _cumulativePointsMap = calculateCDF(pointsMap);
    _totalPoints = _cumulativePointsMap.isEmpty() ? 0 : _cumulativePointsMap.lastKey();
  }

  @Override
  public T get(int unused)
  {
    if (_cumulativePointsMap.isEmpty())
    {
      LOG.warn("Calling get on an empty ring, null value will be returned");
      return null;
    }
    int rand = ThreadLocalRandom.current().nextInt(_totalPoints);
    return _cumulativePointsMap.higherEntry(rand).getValue();
  }

  /**
   * This iterator does not honor the points of the hosts except the first one. This is acceptable because the other two real rings behave this way.
   */
  @Nonnull
  @Override
  public Iterator<T> getIterator(int unused)
  {
    List<T> hosts = new ArrayList<>(_cumulativePointsMap.values());
    if (!hosts.isEmpty())
    {
      Collections.shuffle(hosts);
      //we try to put host with higher probability as the first by calling get. This avoids the situation where unhealthy host is returned first.
      try
      {
        Collections.swap(hosts, 0, hosts.indexOf(get(0)));
      } catch (IndexOutOfBoundsException e)
      {
        LOG.warn("Got indexOutOfBound when trying to shuffle list:" + e.getMessage());
      }
    }
    return hosts.iterator();
  }

  @Override
  public boolean isStickyRoutingCapable()
  {
    return false;
  }

  @Override
  public boolean isEmpty()
  {
    return _cumulativePointsMap.isEmpty();
  }

  private TreeMap<Integer, T> calculateCDF(Map<T, Integer> pointsMap)
  {
    int cumulativeSum = 0;
    TreeMap<Integer, T> cumulativePointsMap = new TreeMap<>();

    for (Map.Entry<T, Integer> entry : pointsMap.entrySet())
    {
      if (entry.getValue() == 0)
      {
        continue;
      }
      cumulativeSum += entry.getValue();
      cumulativePointsMap.put(cumulativeSum, entry.getKey());
    }
    return cumulativePointsMap;
  }
}

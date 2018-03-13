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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DistributionNonDiscreteRing<T> implements Ring<T> {
  private static final Logger LOG = LoggerFactory.getLogger(DistributionNonDiscreteRing.class);
  private TreeMap<Integer, T> _cumulativePointsMap;
  private int _totalPoints;
  private int _totalHosts;

  public DistributionNonDiscreteRing(Map<T, Integer> pointsMap) {
    _cumulativePointsMap = calculateCDF(pointsMap);
    _totalPoints = _cumulativePointsMap.lastKey();
    _totalHosts = pointsMap.size();
  }

  @Override
  public T get(int _unused) {
    if (_unused != 0) {
      LOG.warn("Distribution based ring does not support sticky routing, but non-null key is passed in");
    }
    int rand = ThreadLocalRandom.current().nextInt(_totalPoints);
    return _cumulativePointsMap.higherEntry(rand).getValue();
  }

  @Override
  public Iterator<T> getIterator(int _unused) {
    if (_unused != 0) {
      LOG.warn("Distribution based ring does not support sticky routing, but non-zero key is passed in");
    }

    return new Iterator<T>() {
      private Set<T> _visitedHosts = ConcurrentHashMap.newKeySet();

      @Override
      public boolean hasNext() {
        return _visitedHosts.size() < _totalHosts;
      }

      @Override
      public T next() {
        T pickedHost = get(0);//argument is not used, keep redrawing from the distribution
        while (_visitedHosts.contains(pickedHost)) {
          //Potential problem: since we are using randomized selection here, we might encounter prolonged drawing
          pickedHost = get(0);
        }
        _visitedHosts.add(pickedHost);
        return pickedHost;
      }
    };
  }

  @Override
  public boolean isStickyRoutingCapable() {
    return false;
  }

  private TreeMap<Integer, T> calculateCDF(Map<T, Integer> pointsMap) {
    int cumulativeSum = 0;
    TreeMap<Integer, T> cumulativePointsMap = new TreeMap<Integer, T>();

    for (Map.Entry<T, Integer> entry : pointsMap.entrySet()) {
      if (entry.getValue() == 0) {
        continue;
      }
      cumulativeSum += entry.getValue();
      cumulativePointsMap.put(cumulativeSum, entry.getKey());
    }
    return cumulativePointsMap;
  }
}

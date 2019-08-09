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

package com.linkedin.d2.balancer.util.hashing.simulator;

import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.util.degrader.CallTracker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * A helper class that aggregates the information of consistent hash ring in the {@link ConsistentHashRingSimulator}
 */
class ConsistentHashRingState
{
  private final Ring<String> _ring;
  private final Map<String, CallTracker> _callTrackerMap;
  private final Map<String, List<Integer>> _latencyMap;

  public ConsistentHashRingState(Ring<String> ring, Map<String, CallTracker> callTrackerMap,
      Map<String, List<Integer>> latencyMap)
  {
    _ring = ring;
    _callTrackerMap = callTrackerMap;
    _latencyMap = latencyMap;
  }

  public Map<String, Integer> getPendingRequestsNum()
  {
    return _callTrackerMap.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getCurrentConcurrency()));
  }

  public Map<String, Long> getTotalRequestsNum()
  {
    return _callTrackerMap.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getCurrentCallStartCountTotal()));
  }

  public Map<String, Integer> getAverageLatency()
  {
    Map<String, Integer> averageLatency = new HashMap<>();

    for (Map.Entry<String, List<Integer>> entry : _latencyMap.entrySet())
    {
      int average = (int) entry.getValue().stream().mapToInt(Integer::intValue).average().orElse(0);
      averageLatency.put(entry.getKey(), average);
    }

    return averageLatency;
  }

  public Map<String, CallTracker> getCallTrackerMap()
  {
    return _callTrackerMap;
  }

  public Map<String, List<Integer>> getLatencyMap()
  {
    return _latencyMap;
  }

  public Ring<String> getRing()
  {
    return _ring;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    getTotalRequestsNum().forEach((k, v) ->
    {
      long complete = getTotalRequestsNum().get(k) == null ? 0 : getTotalRequestsNum().get(k);
      sb.append(String.format("%s : Pending = %d, Total = %d\t", k, v, complete));
    });
    return sb.toString();
  }
}

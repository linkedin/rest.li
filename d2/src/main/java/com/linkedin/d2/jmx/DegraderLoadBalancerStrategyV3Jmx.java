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

package com.linkedin.d2.jmx;


import com.linkedin.d2.balancer.clients.DegraderTrackerClient;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;
import com.linkedin.d2.balancer.strategies.relative.StateUpdater;
import com.linkedin.d2.balancer.strategies.relative.TrackerClientState;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class DegraderLoadBalancerStrategyV3Jmx implements DegraderLoadBalancerStrategyV3JmxMBean
{
  private final DegraderLoadBalancerStrategyV3 _strategy;

  public DegraderLoadBalancerStrategyV3Jmx(DegraderLoadBalancerStrategyV3 strategy)
  {
    _strategy = strategy;
  }

  @Override
  public double getOverrideClusterDropRate()
  {
    double rate = _strategy.getState().getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getCurrentOverrideDropRate();
    return rate;
  }

  @Override
  public String toString()
  {
    return "DegraderLoadBalancerStrategyV3Jmx [_strategy=" + _strategy + "]";
  }

  @Override
  public int getTotalPointsInHashRing()
  {
    Map<URI, Integer> uris = _strategy.getState().getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).
        getPointsMap();
    int total = 0;
    for (Map.Entry<URI, Integer> entry : uris.entrySet())
    {
      total += entry.getValue();
    }
    return total;
  }

  @Override
  public String getPointsMap(int partitionId)
  {
    return _strategy.getState().getPartitionState(partitionId).getPointsMap().toString();
  }

  @Override
  public String getUnhealthyClientsPoints(int partitionId)
  {
    int pointsPerWeight = _strategy.getConfig().getPointsPerWeight();
    List<String> result = new ArrayList<String>();
    for (Map.Entry<URI, Integer> entry : _strategy.getState().getPartitionState(partitionId).getPointsMap().entrySet())
    {
      if (entry.getValue() < pointsPerWeight)
      {
        result.add(entry.getKey().toString() + ":" + entry.getValue() + "/" + pointsPerWeight);
      }
    }
    return result.toString();
  }

  @Override
  public String getRingInformation(int partitionId)
  {
    Ring<URI> ring = _strategy.getRing(partitionId);
    if (ring == null)
    {
      return "Ring for that partition is null";
    }
    return ring.toString();
  }

  @Override
  public long getCurrentClusterCallCount(int partitionId)
  {
    long currentClusterCallCount = _strategy.getState().getPartitionState(partitionId).getCurrentClusterCallCount();
    return currentClusterCallCount;
  }

  @Override
  public double getCurrentAvgClusterLatency(int partitionId)
  {
    double currentAvgClusterLatency = _strategy.getState().getPartitionState(partitionId).getCurrentAvgClusterLatency();
    return currentAvgClusterLatency;
  }

  @Override
  public double getLatencyStandardDeviation()
  {
    Set<DegraderTrackerClient> trackerClients = _strategy.getState()
        .getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getTrackerClients();

    return RelativeLoadBalancerStrategyJmx.calculateStandardDeviation(trackerClients);
  }

  @Override
  public double getMaxLatencyRelativeFactor()
  {
    Set<DegraderTrackerClient> trackerClients = _strategy.getState()
        .getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getTrackerClients();

    double avgLatency = RelativeLoadBalancerStrategyJmx.getAvgClusterLatency(trackerClients);
    long maxLatency = trackerClients.stream()
        .map(trackerClient -> StateUpdater.getAvgHostLatency(trackerClient.getCallTracker().getCallStats()))
        .mapToLong(Long::longValue)
        .max()
        .orElse(0L);

    return avgLatency == 0 ? 0 : maxLatency / avgLatency;
  }

  @Override
  public double getNthPercentileLatencyRelativeFactor(double pct)
  {
    Set<DegraderTrackerClient> trackerClients = _strategy.getState()
        .getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getTrackerClients();

    if (trackerClients.size() == 0)
    {
      return 0.0;
    }

    double avgLatency = RelativeLoadBalancerStrategyJmx.getAvgClusterLatency(trackerClients);
    List<Long> weightedLatencies = trackerClients.stream()
        .map(trackerClient -> StateUpdater.getAvgHostLatency(trackerClient.getCallTracker().getCallStats()))
        .sorted()
        .collect(Collectors.toList());

    int nth = Math.max((int) (pct * weightedLatencies.size()) - 1, 0);
    long nthLatency = weightedLatencies.get(nth);

    return avgLatency == 0 ? 0 : nthLatency / avgLatency;
  }
}

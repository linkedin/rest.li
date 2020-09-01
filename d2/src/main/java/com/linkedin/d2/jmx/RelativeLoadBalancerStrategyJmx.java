/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.LoadBalancerQuarantine;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.relative.StateUpdater;
import com.linkedin.d2.balancer.strategies.relative.TrackerClientState;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.util.degrader.CallTracker;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class RelativeLoadBalancerStrategyJmx implements RelativeLoadBalancerStrategyJmxMBean
{
  private final RelativeLoadBalancerStrategy _strategy;

  public RelativeLoadBalancerStrategyJmx(RelativeLoadBalancerStrategy strategy)
  {
    _strategy = strategy;
  }

  @Override
  public double getLatencyStandardDeviation()
  {
    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getTrackerClientStateMap();

    return calculateStandardDeviation(stateMap.keySet());
  }

  @Override
  public double getLatencyMeanAbsoluteDeviation()
  {
    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getTrackerClientStateMap();

    double avgLatency = getAvgClusterLatency(stateMap.keySet());

    return stateMap.keySet().stream()
        .filter(this::hasTraffic)
        .map(trackerClient -> Math.abs(StateUpdater.getAvgHostLatency(trackerClient.getCallTracker().getCallStats()) - avgLatency))
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0);
  }

  @Override
  public double getAboveAverageLatencyStandardDeviation()
  {
    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getTrackerClientStateMap();

    double avgLatency = getAvgClusterLatency(stateMap.keySet());

    Set<TrackerClient> aboveAvgClients = stateMap.keySet().stream()
        .filter(trackerClient -> StateUpdater.getAvgHostLatency(trackerClient.getCallTracker().getCallStats()) > avgLatency)
        .collect(Collectors.toSet());

    return calculateStandardDeviation(aboveAvgClients);
  }

  @Override
  public double getMaxLatencyRelativeFactor()
  {
    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getTrackerClientStateMap();

    double avgLatency = getAvgClusterLatency(stateMap.keySet());
    long maxLatency = stateMap.keySet().stream()
        .map(trackerClient -> StateUpdater.getAvgHostLatency(trackerClient.getCallTracker().getCallStats()))
        .mapToLong(Long::longValue)
        .max()
        .orElse(0L);

    return avgLatency == 0 ? 0 : maxLatency / avgLatency;
  }

  @Override
  public double getNthPercentileLatencyRelativeFactor(double pct)
  {
    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getTrackerClientStateMap();

    if (stateMap.size() == 0)
    {
      return 0.0;
    }

    double avgLatency = getAvgClusterLatency(stateMap.keySet());
    List<Long> weightedLatencies = stateMap.keySet()
        .stream()
        .map(trackerClient -> StateUpdater.getAvgHostLatency(trackerClient.getCallTracker().getCallStats()))
        .sorted()
        .collect(Collectors.toList());

    int nth = Math.max((int) (pct * weightedLatencies.size()) - 1, 0);
    long nthLatency = weightedLatencies.get(nth);

    return avgLatency == 0 ? 0 : nthLatency / avgLatency;
  }

  @Override
  public int getUnhealthyHostsCount()
  {
    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getTrackerClientStateMap();

    return (int) stateMap.values().stream()
        .filter(TrackerClientState::isUnhealthy)
        .count();
  }

  @Override
  public int getQuarantineHostsCount()
  {
    Map<TrackerClient, LoadBalancerQuarantine> quarantineMap =
        _strategy.getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getQuarantineMap();

    return (int) quarantineMap.values().stream()
        .filter(LoadBalancerQuarantine::isInQuarantine)
        .count();
  }

  @Override
  public int getTotalPointsInHashRing()
  {
    Map<URI, Integer> uris = _strategy.getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getPointsMap();

    return uris.values().stream()
        .mapToInt(Integer::intValue)
        .sum();
  }

  private boolean hasTraffic(TrackerClient trackerClient)
  {
    CallTracker.CallStats stats = trackerClient.getCallTracker().getCallStats();
    return stats.getOutstandingCount() + stats.getCallCount() > 0;
  }

  private double calculateStandardDeviation(Set<TrackerClient> trackerClients)
  {
    double avgLatency = getAvgClusterLatency(trackerClients);
    double variance = trackerClients.stream()
        .filter(this::hasTraffic)
        .map(trackerClient -> Math.pow(StateUpdater.getAvgHostLatency(trackerClient.getCallTracker().getCallStats()) - avgLatency, 2))
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0);

    return Math.sqrt(variance);
  }

  private long getAvgClusterLatency(Set<TrackerClient> trackerClients)
  {
    long latencySum = 0;
    long outstandingLatencySum = 0;
    int callCountSum = 0;
    int outstandingCallCountSum = 0;

    for (TrackerClient trackerClient : trackerClients)
    {
      CallTracker.CallStats latestCallStats = trackerClient.getCallTracker().getCallStats();

      int callCount = latestCallStats.getCallCount();
      int outstandingCallCount = latestCallStats.getOutstandingCount();
      latencySum += latestCallStats.getCallTimeStats().getAverage() * callCount;
      outstandingLatencySum += latestCallStats.getOutstandingStartTimeAvg() * outstandingCallCount;
      callCountSum += callCount;
      outstandingCallCountSum += outstandingCallCount;
    }

    return callCountSum + outstandingCallCountSum == 0
        ? 0
        : Math.round((latencySum + outstandingLatencySum) / (double) (callCountSum + outstandingCallCountSum));
  }
}

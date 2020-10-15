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
import com.linkedin.util.degrader.CallTracker;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class RelativeLoadBalancerStrategyJmx implements RelativeLoadBalancerStrategyJmxMBean
{
  private static final double DEFAULT_DOUBLE_METRICS = 0;
  private static final int DEFAULT_INT_METRICS = 0;
  private final RelativeLoadBalancerStrategy _strategy;

  public RelativeLoadBalancerStrategyJmx(RelativeLoadBalancerStrategy strategy)
  {
    _strategy = strategy;
  }

  @Override
  public double getLatencyStandardDeviation()
  {
    if (isPartitionDataUnavailable())
    {
      return DEFAULT_DOUBLE_METRICS;
    }

    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(_strategy.getValidPartitionId()).getTrackerClientStateMap();

    return calculateStandardDeviation(stateMap.keySet());
  }

  @Override
  public double getLatencyMeanAbsoluteDeviation()
  {
    if (isPartitionDataUnavailable())
    {
      return DEFAULT_DOUBLE_METRICS;
    }

    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(_strategy.getValidPartitionId()).getTrackerClientStateMap();

    double avgLatency = getAvgClusterLatency(stateMap.keySet());

    return stateMap.keySet().stream()
        .filter(RelativeLoadBalancerStrategyJmx::hasTraffic)
        .map(trackerClient -> Math.abs(StateUpdater.getAvgHostLatency(trackerClient.getCallTracker().getCallStats()) - avgLatency))
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0);
  }

  @Override
  public double getAboveAverageLatencyStandardDeviation()
  {
    if (isPartitionDataUnavailable())
    {
      return DEFAULT_DOUBLE_METRICS;
    }

    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(_strategy.getValidPartitionId()).getTrackerClientStateMap();

    double avgLatency = getAvgClusterLatency(stateMap.keySet());

    Set<TrackerClient> aboveAvgClients = stateMap.keySet().stream()
        .filter(trackerClient -> StateUpdater.getAvgHostLatency(trackerClient.getCallTracker().getCallStats()) > avgLatency)
        .collect(Collectors.toSet());

    return calculateStandardDeviation(aboveAvgClients);
  }

  @Override
  public double getMaxLatencyRelativeFactor()
  {
    if (isPartitionDataUnavailable())
    {
      return DEFAULT_DOUBLE_METRICS;
    }

    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(_strategy.getValidPartitionId()).getTrackerClientStateMap();

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
    if (isPartitionDataUnavailable())
    {
      return DEFAULT_DOUBLE_METRICS;
    }

    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(_strategy.getValidPartitionId()).getTrackerClientStateMap();

    if (stateMap.size() == 0)
    {
      return DEFAULT_DOUBLE_METRICS;
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
    if (isPartitionDataUnavailable())
    {
      return DEFAULT_INT_METRICS;
    }

    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(_strategy.getValidPartitionId()).getTrackerClientStateMap();

    return (int) stateMap.values().stream()
        .filter(TrackerClientState::isUnhealthy)
        .count();
  }

  @Override
  public int getQuarantineHostsCount()
  {
    if (isPartitionDataUnavailable())
    {
      return DEFAULT_INT_METRICS;
    }

    Map<TrackerClient, LoadBalancerQuarantine> quarantineMap =
        _strategy.getPartitionState(_strategy.getValidPartitionId()).getQuarantineMap();

    return (int) quarantineMap.values().stream()
        .filter(LoadBalancerQuarantine::isInQuarantine)
        .count();
  }

  @Override
  public int getTotalPointsInHashRing()
  {
    if (isPartitionDataUnavailable())
    {
      return DEFAULT_INT_METRICS;
    }

    Map<URI, Integer> uris = _strategy.getPartitionState(_strategy.getValidPartitionId()).getPointsMap();

    return uris.values().stream()
        .mapToInt(Integer::intValue)
        .sum();
  }

  static boolean hasTraffic(TrackerClient trackerClient)
  {
    CallTracker.CallStats stats = trackerClient.getCallTracker().getCallStats();
    return stats.getOutstandingCount() + stats.getCallCount() > 0;
  }

  static double calculateStandardDeviation(Set<? extends TrackerClient> trackerClients)
  {
    double avgLatency = getAvgClusterLatency(trackerClients);
    double variance = trackerClients.stream()
        .filter(RelativeLoadBalancerStrategyJmx::hasTraffic)
        .map(trackerClient -> Math.pow(StateUpdater.getAvgHostLatency(trackerClient.getCallTracker().getCallStats()) - avgLatency, 2))
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0);

    return Math.sqrt(variance);
  }

  static long getAvgClusterLatency(Set<? extends TrackerClient> trackerClients)
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

  private boolean isPartitionDataUnavailable()
  {
    return _strategy.getPartitionState(_strategy.getValidPartitionId()) == null;
  }
}

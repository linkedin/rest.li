package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.LoadBalancerQuarantine;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.relative.TrackerClientState;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.util.degrader.CallTracker;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    List<Double> weightedLatencies = stateMap.keySet().stream()
        .map(trackerClient -> getWeightedLatency(trackerClient.getLatestCallStats()))
        .collect(Collectors.toList());

    double avgLatency = weightedLatencies.stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0);

    double variance = weightedLatencies.stream()
        .map(latency -> Math.pow(latency - avgLatency, 2))
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0);

    return Math.sqrt(variance);
  }

  @Override
  public double getLatencyAverageAbsoluteDeviation()
  {
    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getTrackerClientStateMap();

    List<Double> weightedLatencies = stateMap.keySet().stream()
        .map(trackerClient -> getWeightedLatency(trackerClient.getLatestCallStats()))
        .collect(Collectors.toList());

    double avgLatency = weightedLatencies.stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0);

    return weightedLatencies.stream()
        .map(latency -> Math.abs(latency - avgLatency))
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0);
  }

  @Override
  public double getLatencyMedianAbsoluteDeviation()
  {
    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getTrackerClientStateMap();

    List<Double> weightedLatencies = stateMap.keySet().stream()
        .map(trackerClient -> getWeightedLatency(trackerClient.getLatestCallStats()))
        .collect(Collectors.toList());

    double medianLatency = getMedian(weightedLatencies);

    List<Double> medianAbsolutes = weightedLatencies.stream()
        .map(latency -> Math.abs(latency - medianLatency))
        .collect(Collectors.toList());

    return getMedian(medianAbsolutes);
  }

  @Override
  public double getLatencyMaxAbsoluteDeviation()
  {
    Map<TrackerClient, TrackerClientState> stateMap =
        _strategy.getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getTrackerClientStateMap();

    List<Double> weightedLatencies = stateMap.keySet().stream()
        .map(trackerClient -> getWeightedLatency(trackerClient.getLatestCallStats()))
        .collect(Collectors.toList());

    double avgLatency = weightedLatencies.stream()
        .mapToDouble(Double::doubleValue)
        .average()
        .orElse(0);

    return weightedLatencies.stream()
        .map(latency -> Math.abs(latency - avgLatency))
        .mapToDouble(Double::doubleValue)
        .max()
        .orElse(0);
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
  public int getQuarantineHostsCount() {
    Map<TrackerClient, LoadBalancerQuarantine> quarantineMap =
        _strategy.getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getQuarantineMap();

    return (int) quarantineMap.values().stream()
        .filter(LoadBalancerQuarantine::isInQuarantine)
        .count();
  }

  @Override
  public int getTotalPointsInHashRing() {
    Map<URI, Integer> uris = _strategy.getPartitionState(DefaultPartitionAccessor.DEFAULT_PARTITION_ID).getPointsMap();

    return uris.values().stream()
        .mapToInt(Integer::intValue)
        .sum();
  }

  private static double getWeightedLatency(CallTracker.CallStats callStats)
  {
    int callCount = callStats.getCallCount();
    int outstandingCallCount = callStats.getOutstandingCount();

    return (callStats.getCallTimeStats().getAverage() * callCount +
        callStats.getOutstandingStartTimeAvg() * outstandingCallCount) / (callCount + outstandingCallCount);
  }

  private static double getMedian(List<Double> values)
  {
    Collections.sort(values);

    if (values.size() == 0)
    {
      return 0;
    }
    else if (values.size() % 2 == 0)
    {
      return (values.get(values.size() / 2) + values.get(values.size() / 2 - 1)) / 2;
    }
    else
    {
      return values.get(values.size() / 2);
    }
  }
}

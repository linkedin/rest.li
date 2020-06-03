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

package com.linkedin.d2.balancer.strategies.relative;

import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.StateUpdater;
import com.linkedin.d2.balancer.strategies.degrader.DelegatingRingFactory;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.util.degrader.CallTracker;
import com.linkedin.util.degrader.ErrorType;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Update the state of the RelativeLoadBalancerStrategy periodically
 * TODO: add more logging
 */
public class RelativeStateUpdater implements StateUpdater
{
  public static final double MIN_HEALTH_SCORE = 0.0;
  public static final double MAX_HEALTH_SCORE = 1.0;
  private static final double SLOW_START_INITIAL_HEALTH_SCORE = 0.01;
  private static final int SLOW_START_RECOVERY_FACTOR = 2;

  private final D2RelativeStrategyProperties _relativeStrategyProperties;
  private final QuarantineManager _quarantineManager;
  private final ScheduledExecutorService _executorService;
  private final List<PartitionRelativeLoadBalancerStateListener.Factory> _listenerFactories;

  private ConcurrentMap<Integer, PartitionRelativeLoadBalancerState> _partitionLoadBalancerStateMap;

  RelativeStateUpdater(D2RelativeStrategyProperties relativeStrategyProperties,
                       QuarantineManager quarantineManager,
                       ScheduledExecutorService executorService,
                       List<PartitionRelativeLoadBalancerStateListener.Factory> listenerFactories)
  {
    _relativeStrategyProperties = relativeStrategyProperties;
    _quarantineManager = quarantineManager;
    _executorService = executorService;
    _listenerFactories = listenerFactories;
    _partitionLoadBalancerStateMap = new ConcurrentHashMap<>();

    _executorService.schedule((Runnable) this::updateState, _relativeStrategyProperties.getUpdateIntervalMs(), TimeUnit.MILLISECONDS);
  }

  @Override
  public void updateState(Set<TrackerClient> trackerClients, int partitionId, long clusterGenerationId)
  {
    if (!_partitionLoadBalancerStateMap.containsKey(partitionId))
    {
      PartitionRelativeLoadBalancerState partitionRelativeLoadBalancerState =  new PartitionRelativeLoadBalancerState(partitionId,
          new DelegatingRingFactory<>(_relativeStrategyProperties.getRingProperties()),
          _relativeStrategyProperties.getRingProperties().getPointsPerWeight(),
          _listenerFactories.stream().map(factory -> factory.create(partitionId)).collect(Collectors.toList()));
      // If it is the very first request for the partition, update the state synchronously
      if(partitionRelativeLoadBalancerState.getLock().tryLock())
      {
        try
        {
          if (!_partitionLoadBalancerStateMap.containsKey(partitionId))
          {
            _partitionLoadBalancerStateMap.put(partitionId, partitionRelativeLoadBalancerState);
            updateStateForPartition(trackerClients, partitionId, clusterGenerationId);
          }
        }
        finally
        {
          partitionRelativeLoadBalancerState.getLock().unlock();
        }
      }
    } else if (_partitionLoadBalancerStateMap.get(partitionId).getClusterGenerationId() != clusterGenerationId)
    {
      // If there are uris change in the cluster, use executor to update the state asynchronously
      _executorService.execute(() -> updateStateForPartition(trackerClients, partitionId, clusterGenerationId));
    }
  }

  @Override
  public Ring<URI> getRing(int partitionId)
  {
    return _partitionLoadBalancerStateMap.get(partitionId) == null
        ? null
        : _partitionLoadBalancerStateMap.get(partitionId).getRing();
  }

  /**
   * Update the partition state.
   * This is scheduled by executor, and the update should not involve any cluster change
   */
  private void updateState()
  {
    // Update state for each partition
    for (Integer partitionId : _partitionLoadBalancerStateMap.keySet())
    {
      updateStateForPartition(_partitionLoadBalancerStateMap.get(partitionId).getTrackerClients(), partitionId, null);
    }
  }

  /**
   * Update the partition state when there is a cluster uris change.
   */
  private void updateStateForPartition(Set<TrackerClient> trackerClients, int partitionId, Long clusterGenerationId)
  {
    PartitionRelativeLoadBalancerState oldPartitionRelativeLoadBalancerState = _partitionLoadBalancerStateMap.get(partitionId);
    PartitionRelativeLoadBalancerState newPartitionRelativeLoadBalancerState = oldPartitionRelativeLoadBalancerState.copy();

    // Step 1: Update the base health scores for each {@link TrackerClient} in the cluster
    Map<TrackerClient, CallTracker.CallStats> latestCallStatsMap = new HashMap<>();
    long avgClusterLatency = getAvgClusterLatency(trackerClients, latestCallStatsMap);
    updateHealthScoreAndState(trackerClients, newPartitionRelativeLoadBalancerState, avgClusterLatency, clusterGenerationId);

    // Step 2: Handle quarantine and recovery for all tracker clients in this cluster
    // this will adjust the base health score if there is any change in quarantine and recovery map
    _quarantineManager.updateQuarantineState(newPartitionRelativeLoadBalancerState,
        oldPartitionRelativeLoadBalancerState, avgClusterLatency);

    // Step 3: Calculate the new ring for each partition
    newPartitionRelativeLoadBalancerState.resetRing();

    if (clusterGenerationId != null)
    {
      newPartitionRelativeLoadBalancerState.setClusterGenerationId(clusterGenerationId);
    }
    _partitionLoadBalancerStateMap.put(partitionId, newPartitionRelativeLoadBalancerState);

    // Step 4: Emit monitor event asynchronously
    _executorService.execute(() -> emitMonitorEvents(newPartitionRelativeLoadBalancerState));
  }

  /**
   * Update the health score of all tracker clients for the service
   * @param trackerClients All the tracker clients in this cluster
   */
  private void updateHealthScoreAndState(Set<TrackerClient> trackerClients,
      PartitionRelativeLoadBalancerState partitionRelativeLoadBalancerState, long clusterAvgLatency,
      Long clusterGenerationId)
  {
    // Calculate the base health score before we override them when handling the quarantine and recovery
    calculateBaseHealthScore(trackerClients, partitionRelativeLoadBalancerState, clusterAvgLatency);

    // Remove the trackerClients from original map if there is any change in uri list
    Map<TrackerClient, TrackerClientState> trackerClientStateMap = partitionRelativeLoadBalancerState.getTrackerClientStateMap();
    if (clusterGenerationId != null)
    {
      for (TrackerClient trackerClient : trackerClientStateMap.keySet())
      {
        if (!trackerClients.contains(trackerClient))
        {
          trackerClientStateMap.remove(trackerClient);
          partitionRelativeLoadBalancerState.getQuarantineMap().remove(trackerClient);
          partitionRelativeLoadBalancerState.getQuarantineHistory().remove(trackerClient);
          partitionRelativeLoadBalancerState.getRecoveryMap().remove(trackerClient);
        }
      }
    }
  }

  private void calculateBaseHealthScore(Set<TrackerClient> trackerClients, PartitionRelativeLoadBalancerState partitionRelativeLoadBalancerState,
      long avgClusterLatency)
  {
    Map<TrackerClient, TrackerClientState> trackerClientStateMap = partitionRelativeLoadBalancerState.getTrackerClientStateMap();
    Map<TrackerClient, CallTracker.CallStats> latestCallStatsMap = new HashMap<>();

    // Update health score
    long clusterCallCount = 0;
    long clusterErrorCount = 0;
    for (TrackerClient trackerClient : trackerClients)
    {
      CallTracker.CallStats latestCallStats = latestCallStatsMap.get(trackerClient);

      if (trackerClientStateMap.containsKey(trackerClient))
      {
        TrackerClientState trackerClientState = trackerClientStateMap.get(trackerClient);
        int callCount = latestCallStats.getCallCount() + latestCallStats.getOutstandingCount();
        double errorRate = getErrorRateByType(latestCallStats.getErrorTypeCounts(), callCount);
        long avglatency = getAvgHostLatency(latestCallStats);
        double oldHealthScore = trackerClientState.getHealthScore();
        double newHealthScore = oldHealthScore;

        clusterCallCount += callCount;
        clusterErrorCount += errorRate * callCount;

        if (isUnhealthy(trackerClientState, avgClusterLatency, callCount, avglatency, errorRate))
        {
          // If it is above high latency, we reduce the health score by down step
          newHealthScore = Double.max(trackerClientState.getHealthScore() - _relativeStrategyProperties.getDownStep(), MIN_HEALTH_SCORE);
          trackerClientState.setHealthState(TrackerClientState.HealthState.UNHEALTHY);
        }
        else if (trackerClientState.getHealthScore() < MAX_HEALTH_SCORE
            && isHealthy(trackerClientState, avgClusterLatency, callCount, avglatency, errorRate))
        {
          if (oldHealthScore < _relativeStrategyProperties.getSlowStartThreshold())
          {
            // If the client is healthy and slow start is enabled, we double the health score
            newHealthScore = oldHealthScore > MIN_HEALTH_SCORE
                ? Math.min(MAX_HEALTH_SCORE, SLOW_START_RECOVERY_FACTOR * oldHealthScore)
                : SLOW_START_INITIAL_HEALTH_SCORE;
          } else {
            // If slow start is not enabled, we just increase the health score by up step
            newHealthScore = Math.min(MAX_HEALTH_SCORE, oldHealthScore + _relativeStrategyProperties.getUpStep());
          }
          trackerClientState.setHealthState(TrackerClientState.HealthState.HEALTHY);
        } else
        {
          trackerClientState.setHealthState(TrackerClientState.HealthState.NEUTRAL);
        }
        trackerClientState.setHealthScore(newHealthScore);
        trackerClientState.setCallCount(callCount);
      } else
      {
        // If it is a new client, we directly set health score as the initial health score to initialize
        trackerClientStateMap.put(trackerClient, new TrackerClientState(_relativeStrategyProperties.getInitialHealthScore(),
            _relativeStrategyProperties.getMinCallCount()));
      }
    }
    partitionRelativeLoadBalancerState.setPartitionStats(avgClusterLatency, clusterCallCount, clusterErrorCount);
  }

  private long getAvgClusterLatency(Set<TrackerClient> trackerClients, Map<TrackerClient, CallTracker.CallStats> latestCallStatsMap)
  {
    long latencySum = 0;
    long outstandingLatencySum = 0;
    int callCountSum = 0;
    int outstandingCallCountSum = 0;

    for (TrackerClient trackerClient : trackerClients)
    {
      CallTracker.CallStats latestCallStats = trackerClient.getLatestCallStats();
      latestCallStatsMap.put(trackerClient, latestCallStats);

      int callCount = latestCallStats.getCallCount();
      int outstandingCallCount = latestCallStats.getOutstandingCount();
      latencySum += latestCallStats.getCallTimeStats().getAverage() * callCount;
      outstandingLatencySum += latestCallStats.getOutstandingStartTimeAvg() * outstandingCallCount;
      callCountSum += callCount;
      outstandingCallCountSum += outstandingCallCount;
    }

    return callCountSum + outstandingCallCountSum == 0
        ? 0
        : latencySum * (callCountSum / (callCountSum + outstandingCallCountSum))
            + outstandingLatencySum * (outstandingCallCountSum / (callCountSum + outstandingCallCountSum));
  }

  private static long getAvgHostLatency(CallTracker.CallStats callStats)
  {
    double avgLatency = callStats.getCallTimeStats().getAverage();
    long avgOutstandingLatency = callStats.getOutstandingStartTimeAvg();
    int callCount = callStats.getCallCount();
    int outstandingCallCount = callStats.getOutstandingCount();
    return callCount + outstandingCallCount == 0
        ? 0
        : Math.round(avgLatency * (callCount / (callCount + outstandingCallCount))
            + avgOutstandingLatency * (outstandingCallCount / (callCount + outstandingCallCount)));
  }

  /**
   * Identify if a client is unhealthy
   */
  private boolean isUnhealthy(TrackerClientState trackerClientState, long avgClusterLatency,
      int callCount, long latency, double errorRate)
  {
    return callCount >= trackerClientState.getAdjustedMinCallCount()
        && (latency >= avgClusterLatency * _relativeStrategyProperties.getRelativeLatencyHighThresholdFactor()
        || errorRate >= _relativeStrategyProperties.getHighErrorRate());
  }

  /**
   * Identify if a client is healthy
   */
  private boolean isHealthy(TrackerClientState trackerClientState, long avgClusterLatency,
      int callCount, long latency, double errorRate)
  {
    return callCount >= trackerClientState.getAdjustedMinCallCount()
        && (latency <= avgClusterLatency * _relativeStrategyProperties.getRelativeLatencyLowThresholdFactor()
        || errorRate <= _relativeStrategyProperties.getLowErrorRate());
  }

  private void emitMonitorEvents(PartitionRelativeLoadBalancerState state)
  {
    state.getListeners().forEach(listener -> listener.onUpdate(state));
  }

  private static double getErrorRateByType(Map<ErrorType, Integer> errorTypeCounts, int callCount)
  {
    Integer connectExceptionCount = errorTypeCounts.getOrDefault(ErrorType.CONNECT_EXCEPTION, 0);
    Integer closedChannelExceptionCount = errorTypeCounts.getOrDefault(ErrorType.CLOSED_CHANNEL_EXCEPTION, 0);
    Integer serverErrorCount = errorTypeCounts.getOrDefault(ErrorType.SERVER_ERROR, 0);
    Integer timeoutExceptionCount = errorTypeCounts.getOrDefault(ErrorType.TIMEOUT_EXCEPTION, 0);
    return callCount == 0
        ? 0
        : (double) (connectExceptionCount + closedChannelExceptionCount + serverErrorCount + timeoutExceptionCount) / callCount;
  }
}

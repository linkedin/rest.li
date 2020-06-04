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
import com.linkedin.d2.balancer.strategies.PartitionLoadBalancerStateListener;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.*;


/**
 * Update the state of the RelativeLoadBalancerStrategy periodically
 */
public class RelativeStateUpdater implements StateUpdater
{
  private static final Logger LOG = LoggerFactory.getLogger(RelativeStateUpdater.class);
  public static final double MIN_HEALTH_SCORE = 0.0;
  public static final double MAX_HEALTH_SCORE = 1.0;
  private static final double SLOW_START_INITIAL_HEALTH_SCORE = 0.01;
  private static final int SLOW_START_RECOVERY_FACTOR = 2;
  private static final int LOG_UNHEALTHY_CLIENT_NUMBERS = 10;

  private final D2RelativeStrategyProperties _relativeStrategyProperties;
  private final QuarantineManager _quarantineManager;
  private final ScheduledExecutorService _executorService;
  private final List<PartitionLoadBalancerStateListener.Factory<PartitionRelativeLoadBalancerState>> _listenerFactories;

  private ConcurrentMap<Integer, PartitionRelativeLoadBalancerState> _partitionLoadBalancerStateMap;

  RelativeStateUpdater(D2RelativeStrategyProperties relativeStrategyProperties,
                       QuarantineManager quarantineManager,
                       ScheduledExecutorService executorService,
                       List<PartitionLoadBalancerStateListener.Factory<PartitionRelativeLoadBalancerState>> listenerFactories)
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
    if (shouldUpdateSynchronously(partitionId, clusterGenerationId))
    {
      PartitionRelativeLoadBalancerState partitionRelativeLoadBalancerState = _partitionLoadBalancerStateMap.containsKey(partitionId)
          ? _partitionLoadBalancerStateMap.get(partitionId)
          : new PartitionRelativeLoadBalancerState(partitionId,
              new DelegatingRingFactory<>(_relativeStrategyProperties.getRingProperties()),
              _relativeStrategyProperties.getRingProperties().getPointsPerWeight(),
              _listenerFactories.stream().map(factory -> factory.create(partitionId)).collect(Collectors.toList()));

      if(partitionRelativeLoadBalancerState.getLock().tryLock())
      {
        try
        {
          if (shouldUpdateSynchronously(partitionId, clusterGenerationId))
          {
            updateStateForPartition(trackerClients, partitionId, partitionRelativeLoadBalancerState, clusterGenerationId);
          }
        }
        finally
        {
          partitionRelativeLoadBalancerState.getLock().unlock();
        }
      }
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
      PartitionRelativeLoadBalancerState partitionState = _partitionLoadBalancerStateMap.get(partitionId);
      updateStateForPartition(partitionState.getTrackerClients(), partitionId, partitionState, null);
    }
  }

  /**
   * Update the partition state when there is a cluster uris change.
   */
  private void updateStateForPartition(Set<TrackerClient> trackerClients, int partitionId,
      PartitionRelativeLoadBalancerState oldPartitionState, Long clusterGenerationId)
  {
    debug(LOG, "Updating for partition: " + partitionId + ", state: " + oldPartitionState);
    PartitionRelativeLoadBalancerState newPartitionState = oldPartitionState.copy();

    // Step 1: Update the base health scores for each {@link TrackerClient} in the cluster
    Map<TrackerClient, CallTracker.CallStats> latestCallStatsMap = new HashMap<>();
    long avgClusterLatency = getAvgClusterLatency(trackerClients, latestCallStatsMap);
    updateHealthScoreAndState(trackerClients, newPartitionState, avgClusterLatency, clusterGenerationId);

    // Step 2: Handle quarantine and recovery for all tracker clients in this cluster
    // this will adjust the base health score if there is any change in quarantine and recovery map
    _quarantineManager.updateQuarantineState(newPartitionState,
        oldPartitionState, avgClusterLatency);

    // Step 3: Calculate the new ring for each partition
    newPartitionState.resetRing();

    if (clusterGenerationId != null)
    {
      newPartitionState.setClusterGenerationId(clusterGenerationId);
    }
    _partitionLoadBalancerStateMap.put(partitionId, newPartitionState);

    // Step 4: Log and emit monitor event
    _executorService.execute(() -> {
      logState(oldPartitionState, newPartitionState, partitionId);
      emitMonitorEvents(newPartitionState);
    });
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
          partitionRelativeLoadBalancerState.removeTrackerClient(trackerClient);
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

  private boolean shouldUpdateSynchronously(int partitionId, long clusterGenerationId)
  {
    return !_partitionLoadBalancerStateMap.containsKey(partitionId)
        || clusterGenerationId != _partitionLoadBalancerStateMap.get(partitionId).getClusterGenerationId();
  }

  private static void logState(PartitionRelativeLoadBalancerState oldState,
      PartitionRelativeLoadBalancerState newState,
      int partitionId) {
    Map<TrackerClient, TrackerClientState> newTrackerClientStateMap = newState.getTrackerClientStateMap();
    Map<TrackerClient, TrackerClientState> oldTrackerClientStateMap = oldState.getTrackerClientStateMap();
    Set<TrackerClient> newUnhealthyClients = newTrackerClientStateMap.keySet().stream()
        .filter(trackerClient -> newTrackerClientStateMap.get(trackerClient).getHealthScore() < MAX_HEALTH_SCORE)
        .collect(Collectors.toSet());
    Set<TrackerClient> oldUnhealthyClients = oldTrackerClientStateMap.keySet().stream()
        .filter(trackerClient -> oldTrackerClientStateMap.get(trackerClient).getHealthScore() < MAX_HEALTH_SCORE)
        .collect(Collectors.toSet());

    if (LOG.isDebugEnabled()) {
      LOG.debug("Strategy updated: partitionId= " + partitionId + ", newState=" + newState + ", unhealthyClients = ["
          + (newUnhealthyClients.stream().map(client -> getClientStats(client, newTrackerClientStateMap))
          .collect(Collectors.joining(","))) + "]");
    } else if (allowToLog(oldState, newState, newUnhealthyClients, oldUnhealthyClients)) {
      LOG.info("Strategy updated: partitionId= " + partitionId + ", newState=" + newState + ", unhealthyClients = ["
          + (newUnhealthyClients.stream().limit(LOG_UNHEALTHY_CLIENT_NUMBERS)
          .map(client -> getClientStats(client, newTrackerClientStateMap)).collect(Collectors.joining(",")))
          + (newUnhealthyClients.size() > LOG_UNHEALTHY_CLIENT_NUMBERS ? "...(total "
          + newUnhealthyClients.size() + ")" : "") + "]");
    }
  }

  private static boolean allowToLog(PartitionRelativeLoadBalancerState oldState, PartitionRelativeLoadBalancerState newState,
      Set<TrackerClient> newUnhealthyClients, Set<TrackerClient> oldUnhealthyClients)
  {
    // if host number changes
    if (oldState.getPointsMap().size() != newState.getPointsMap().size())
    {
      return true;
    }
    // if the unhealthy client changes
    for (TrackerClient client : newUnhealthyClients)
    {
      if (!oldUnhealthyClients.contains(client))
      {
        return true;
      }
    }
    // if hosts number changes in recoveryMap or quarantineMap
    return oldState.getRecoveryMap().size() != newState.getRecoveryMap().size()
        || oldState.getQuarantineMap().size() != newState.getQuarantineMap().size();
  }

  private static String getClientStats(TrackerClient client, Map<TrackerClient, TrackerClientState> trackerClientStateMap)
  {
    return client.getUri() + ":" + trackerClientStateMap.get(client).getHealthScore();
  }
}

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
import com.linkedin.util.degrader.CallTracker;
import com.linkedin.util.degrader.ErrorType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Update the state of the RelativeLoadBalancerStrategy periodically
 */
public class RelativeStateUpdater implements StateUpdater
{
  public static final double LOWEST_HEALTH_SCORE = 0.0;
  public static final double HEALTHY_HEALTH_SCORE = 1.0;
  // If slow start is enabled, it will always start from 0.01
  private static final double SLOW_START_HEALTH_SCORE = 0.01;
  private static final int SLOW_START_RECOVERY_FACTOR = 2;

  private final D2RelativeStrategyProperties _relativeStrategyProperties;
  private final QuarantineManager _quarantineManager;
  private final ScheduledExecutorService _executorService;

  // Keeps the state of each partition
  private ConcurrentMap<Integer, PartitionLoadBalancerState> _partitionLoadBalancerStateMap;

  RelativeStateUpdater(D2RelativeStrategyProperties relativeStrategyProperties,
                       QuarantineManager quarantineManager,
                       ScheduledExecutorService executorService)
  {
    _relativeStrategyProperties = relativeStrategyProperties;
    _quarantineManager = quarantineManager;
    _executorService = executorService;
    _partitionLoadBalancerStateMap = new ConcurrentHashMap<>();

    _executorService.schedule(this::updateState, _relativeStrategyProperties.getUpdateIntervalMs(), TimeUnit.MILLISECONDS);
  }

  @Override
  public void updateStateByRequest(Set<TrackerClient> trackerClients, int partitionId, long clusterGenerationId)
  {
    if (!_partitionLoadBalancerStateMap.containsKey(partitionId))
    {
      PartitionLoadBalancerState partitionLoadBalancerState =  new PartitionLoadBalancerState(partitionId);
      // If it is the very first request for the partition, update the state synchronously
      if(partitionLoadBalancerState.getLock().tryLock())
      {
        try
        {
          _partitionLoadBalancerStateMap.put(partitionId,partitionLoadBalancerState);
          updateStateForPartition(trackerClients, partitionId, Optional.of(clusterGenerationId));
        }
        finally
        {
          partitionLoadBalancerState.getLock().unlock();
        }
      }
    } else if (_partitionLoadBalancerStateMap.get(partitionId).getClusterGenerationId() != clusterGenerationId)
    {
      // If there are uris change in the cluster, use executor to update the state asynchronously
      _executorService.execute(() -> updateStateForPartition(trackerClients, partitionId, Optional.of(clusterGenerationId)));
    }
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
      updateStateForPartition(_partitionLoadBalancerStateMap.get(partitionId).getTrackerClients(), partitionId, Optional.empty());
    }
  }

  /**
   * Update the partition state when there is a cluster uris change.
   */
  private void updateStateForPartition(Set<TrackerClient> trackerClients, int partitionId, Optional<Long> maybeClusterGenerationId)
  {
    // Step 1: Update the base health scores for each {@link TrackerClient} in the cluster
    updateHealthScoreAndState(trackerClients, partitionId, maybeClusterGenerationId);
    // Step 2: TODO check if we can enable quarantine, we enable quarantine only if at least one of the clients return success for the checking.
    // Step 3: Handle quarantine and recovery for all tracker clients in this cluster
    // this will adjust the base health score if there is any change in quarantine and recovery map
    _quarantineManager.updateQuarantineState(_partitionLoadBalancerStateMap.get(partitionId));
    // Step 4: TODO Calculate the new ring for each partition
  }

  /**
   * Update the health score of all tracker clients for the service
   * @param trackerClients All the tracker clients in this cluster
   */
  private void updateHealthScoreAndState(Set<TrackerClient> trackerClients, int partitionId, Optional<Long> maybeClusterGenerationId)
  {
    PartitionLoadBalancerState partitionLoadBalancerState = _partitionLoadBalancerStateMap.get(partitionId);
    ConcurrentMap<TrackerClient, TrackerClientState> trackerClientStateMap = partitionLoadBalancerState.getTrackerClientStateMap();

    // Remove the trackerClients from original map if there is any change in uri list
    if (maybeClusterGenerationId.isPresent())
    {
      for (TrackerClient trackerClient : trackerClientStateMap.keySet())
      {
        if (!trackerClients.contains(trackerClient))
        {
          trackerClientStateMap.remove(trackerClient);
          partitionLoadBalancerState.getQuarantineMap().remove(trackerClient);
          partitionLoadBalancerState.getQuarantineHistory().remove(trackerClient);
          partitionLoadBalancerState.getRecoveryMap().remove(trackerClient);
        }
      }
    }

    // Calculate the base health score before we override them when handling the quarantine and recovery
    calculateBaseHealthScore(trackerClients, partitionId);

    // Update cluster generation id if it's changed
    if (maybeClusterGenerationId.isPresent())
    {
      _partitionLoadBalancerStateMap.get(partitionId).setClusterGenerationId(maybeClusterGenerationId.get());
    }
  }

  private void calculateBaseHealthScore(Set<TrackerClient> trackerClients, int partitionId)
  {
    // Snap stats for each tracker client, we want to get snap the stats because they can change any time during we calculate the new health score
    long sumAvgLatency = 0;
    PartitionLoadBalancerState partitionLoadBalancerState = _partitionLoadBalancerStateMap.get(partitionId);
    ConcurrentMap<TrackerClient, TrackerClientState> trackerClientStateMap = partitionLoadBalancerState.getTrackerClientStateMap();
    Map<TrackerClient, CallTracker.CallStats> latestCallStatsMap = new HashMap<>();

    for (TrackerClient trackerClient : trackerClients)
    {
      CallTracker.CallStats latestCallStats = trackerClient.getLatestCallStats();
      latestCallStatsMap.put(trackerClient, latestCallStats);

      sumAvgLatency += Math.round(latestCallStats.getCallTimeStats().getAverage());
    }
    long clusterAvgLatency = sumAvgLatency / trackerClients.size();
    partitionLoadBalancerState.setClusterAvgLatency(clusterAvgLatency);

    // Update health score
    for (TrackerClient trackerClient : trackerClients)
    {
      CallTracker.CallStats latestCallStats = latestCallStatsMap.get(trackerClient);

      if (trackerClientStateMap.containsKey(trackerClient))
      {
        TrackerClientState trackerClientState = trackerClientStateMap.get(trackerClient);
        int callCount = latestCallStats.getCallCount();
        double errorRate = TrackerClientState.getErrorRateByType(latestCallStats.getErrorTypeCounts(), callCount);
        long latency = Math.round(latestCallStats.getCallTimeStats().getAverage());
        // If it is an existing tracker client
        double oldHealthScore = trackerClientState.getHealthScore();
        double newHealthScore = oldHealthScore;
        if (TrackerClientState.isUnhealthy(trackerClientState, clusterAvgLatency, callCount, latency, errorRate,
            _relativeStrategyProperties.getRelativeLatencyHighThresholdFactor(),
            _relativeStrategyProperties.getHighErrorRate()))
        {
          // If it is above high latency, we reduce the health score by down step
          newHealthScore = Double.min(trackerClientState.getHealthScore() - _relativeStrategyProperties.getDownStep(), LOWEST_HEALTH_SCORE);
          trackerClientState.setIsUnhealthy();
        }
        else if (trackerClientState.getHealthScore() < HEALTHY_HEALTH_SCORE
            && TrackerClientState.isHealthy(trackerClientState, clusterAvgLatency, callCount, latency, errorRate,
            _relativeStrategyProperties.getRelativeLatencyLowThresholdFactor(),
            _relativeStrategyProperties.getLowErrorRate()))
        {
          if (oldHealthScore < _relativeStrategyProperties.getSlowStartThreshold())
          {
            // If the client is healthy and slow start is enabled, we double the health score
            newHealthScore = oldHealthScore > LOWEST_HEALTH_SCORE
                ? Math.min(HEALTHY_HEALTH_SCORE, SLOW_START_RECOVERY_FACTOR * oldHealthScore)
                : SLOW_START_HEALTH_SCORE;
            trackerClientState.setIsHealthy();
          } else {
            // If slow start is not enabled, we just increase the health score by up step
            newHealthScore = Math.min(HEALTHY_HEALTH_SCORE, oldHealthScore + _relativeStrategyProperties.getUpStep());
          }
        }
        trackerClientState.setHealthScore(newHealthScore);

      } else
      {
        // If it is a new client, we directly set health score as the initial health score to initialize
        trackerClientStateMap.put(trackerClient, new TrackerClientState(_relativeStrategyProperties.getInitialHealthScore()));
      }
    }
  }
}

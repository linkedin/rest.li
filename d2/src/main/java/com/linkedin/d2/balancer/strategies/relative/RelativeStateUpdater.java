package com.linkedin.d2.balancer.strategies.relative;

import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.StateUpdater;
import com.linkedin.util.degrader.CallTracker;
import com.linkedin.util.degrader.ErrorType;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Update the state of the load balancer
 * This class corresponds to one d2 service
 */
public class RelativeStateUpdater implements StateUpdater
{
  private static final double LOWEST_HEALTH_SCORE = 0.0;
  private static final double HEALTHY_HEALTH_SCORE = 1.0;
  // If slow start is enabled, it will always start from 0.01
  private static final double SLOW_START_HEALTH_SCORE = 0.01;
  private static final int SLOW_START_RECOVERY_FACTOR = 2;

  private final D2RelativeStrategyProperties _relativeStrategyProperties;
  private final ScheduledExecutorService _executorService;

  // Keeps the state of each partition
  private ConcurrentMap<Integer, PartitionLoadBalancerState> _partitionLoadBalancerStateMap;

  RelativeStateUpdater(D2RelativeStrategyProperties relativeStrategyProperties,
                       ScheduledExecutorService executorService)
  {
    _relativeStrategyProperties = relativeStrategyProperties;
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
    // Step 1: Update all the health scores for each {@link TrackerClient} in the cluster
    updateTrackerClientHealthScore(trackerClients, partitionId, maybeClusterGenerationId);
    // Step 2: TODO Handle quarantine and recovery for all tracker clients in this cluster
    // Step 3: TODO Calculate the new ring for each partition
  }

  /**
   * Update the health score of all tracker clients for the service
   * @param trackerClients All the tracker clients in this cluster
   */
  private void updateTrackerClientHealthScore(Set<TrackerClient> trackerClients, int partitionId, Optional<Long> maybeClusterGenerationId)
  {
    ConcurrentMap<TrackerClient, TrackerClientState> trackerClientStateMap =
        _partitionLoadBalancerStateMap.get(partitionId).getTrackerClientStateMap();

    // Remove the trackerClients from original map if there is any change in uri list
    if (maybeClusterGenerationId.isPresent())
    {
      for (TrackerClient trackerClient : trackerClientStateMap.keySet())
      {
        if (!trackerClients.contains(trackerClient))
        {
          trackerClientStateMap.remove(trackerClient);
        }
      }
    }

    // Snap stats for each tracker client, we want to get snap the stats because they can change any time during we calculate the new health score
    long sumAvgLatency = 0;
    for (TrackerClient trackerClient : trackerClients)
    {
      CallTracker.CallStats latestCallStats = trackerClient.getLatestCallStats();
      long avgLatency = Math.round(latestCallStats.getCallTimeStats().getAverage());
      int errorCount = latestCallStats.getErrorCount();
      Map<ErrorType, Integer> errorTypeCounts = latestCallStats.getErrorTypeCounts();

      if (!trackerClientStateMap.containsKey(trackerClient))
      {
        trackerClientStateMap.put(trackerClient, new TrackerClientState(avgLatency, errorCount, errorTypeCounts,
            _relativeStrategyProperties.getInitialHealthScore()));
      } else {
        trackerClientStateMap.get(trackerClient).updateStats(avgLatency, errorCount, errorTypeCounts);
      }
      sumAvgLatency += avgLatency;
    }
    long clusterAvgLatency = sumAvgLatency / trackerClients.size();

    // Update health score
    for (TrackerClientState trackerClientState : trackerClientStateMap.values())
    {
      double oldHealthScore = trackerClientState.getHealthScore();
      double newHealthScore = oldHealthScore;
      if (TrackerClientState.isUnhealthy(trackerClientState, clusterAvgLatency,
          _relativeStrategyProperties.getRelativeLatencyHighThresholdFactor(),
          _relativeStrategyProperties.getHighErrorRate()))
      {
        newHealthScore = Double.min(trackerClientState.getHealthScore() - _relativeStrategyProperties.getDownStep(), LOWEST_HEALTH_SCORE);
      }
      else if (trackerClientState.getHealthScore() < HEALTHY_HEALTH_SCORE
          && TrackerClientState.isHealthy(trackerClientState, clusterAvgLatency,
          _relativeStrategyProperties.getRelativeLatencyLowThresholdFactor(),
          _relativeStrategyProperties.getLowErrorRate())) {
        if (oldHealthScore < _relativeStrategyProperties.getSlowStartThreshold())
        {
          newHealthScore = oldHealthScore > LOWEST_HEALTH_SCORE
              ? Math.min(HEALTHY_HEALTH_SCORE, SLOW_START_RECOVERY_FACTOR * oldHealthScore)
              : SLOW_START_HEALTH_SCORE;
        } else {
          newHealthScore = Math.min(HEALTHY_HEALTH_SCORE, oldHealthScore + _relativeStrategyProperties.getUpStep());
        }
      }
      trackerClientState.updateHealthScore(newHealthScore);
    }

    // Update cluster generation id if it's changed
    if (maybeClusterGenerationId.isPresent())
    {
      _partitionLoadBalancerStateMap.get(partitionId).setClusterGenerationId(maybeClusterGenerationId.get());
    }
  }
}

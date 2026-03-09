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

import com.google.common.annotations.VisibleForTesting;
import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.LoadBalancerQuarantine;
import com.linkedin.d2.balancer.strategies.PartitionStateUpdateListener;
import com.linkedin.d2.balancer.strategies.DelegatingRingFactory;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.jmx.NoOpRelativeLoadBalancerStrategyOtelMetricsProvider;
import com.linkedin.d2.jmx.RelativeLoadBalancerStrategyOtelMetricsProvider;
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Update the state of the RelativeLoadBalancerStrategy
 * There are 2 types of updates:
 * 1. The scheduled updates are scheduled with an executor service
 * 2. The incoming request may trigger an update too if the cluster is not initialized or cluster generation changed
 */
public class StateUpdater
{
  private static final Logger LOG = LoggerFactory.getLogger(StateUpdater.class);
  public static final double MIN_HEALTH_SCORE = 0.0;
  public static final double MAX_HEALTH_SCORE = 1.0;
  private static final double SLOW_START_INITIAL_HEALTH_SCORE = 0.01;
  private static final int SLOW_START_RECOVERY_FACTOR = 2;
  private static final int LOG_UNHEALTHY_CLIENT_NUMBERS = 10;
  private static final long EXECUTOR_INITIAL_DELAY = 10;
  private static final String NO_VALUE = "-";

  private final D2RelativeStrategyProperties _relativeStrategyProperties;
  private final QuarantineManager _quarantineManager;
  private final ScheduledExecutorService _executorService;
  private final Lock _lock;
  private final List<PartitionStateUpdateListener.Factory<PartitionState>> _listenerFactories;
  private final String _serviceName;
  private volatile String _scheme;
  private final ScheduledFuture<?> scheduledFuture;
  private ConcurrentMap<Integer, PartitionState> _partitionLoadBalancerStateMap;
  private int _firstPartitionId = -1;
  private final boolean _loadBalanceStreamException;
  private final RelativeLoadBalancerStrategyOtelMetricsProvider _relativeLbOtelMetricsProvider;

  @Deprecated
  StateUpdater(D2RelativeStrategyProperties relativeStrategyProperties,
                       QuarantineManager quarantineManager,
                       ScheduledExecutorService executorService,
                       List<PartitionStateUpdateListener.Factory<PartitionState>> listenerFactories,
                       String serviceName)
  {
    this(relativeStrategyProperties, quarantineManager, executorService, new ConcurrentHashMap<>(), listenerFactories,
        serviceName, false);
  }

  StateUpdater(D2RelativeStrategyProperties relativeStrategyProperties,
      QuarantineManager quarantineManager,
      ScheduledExecutorService executorService,
      List<PartitionStateUpdateListener.Factory<PartitionState>> listenerFactories,
      String serviceName, boolean loadBalanceStreamException)
  {
    this(relativeStrategyProperties, quarantineManager, executorService, new ConcurrentHashMap<>(), listenerFactories,
        serviceName, loadBalanceStreamException);
  }

  StateUpdater(D2RelativeStrategyProperties relativeStrategyProperties,
      QuarantineManager quarantineManager,
      ScheduledExecutorService executorService,
      List<PartitionStateUpdateListener.Factory<PartitionState>> listenerFactories,
      String serviceName, boolean loadBalanceStreamException,
      RelativeLoadBalancerStrategyOtelMetricsProvider relativeLbOtelMetricsProvider)
  {
    this(relativeStrategyProperties, quarantineManager, executorService, new ConcurrentHashMap<>(), listenerFactories,
        serviceName, loadBalanceStreamException, relativeLbOtelMetricsProvider);
  }

  StateUpdater(D2RelativeStrategyProperties relativeStrategyProperties,
      QuarantineManager quarantineManager,
      ScheduledExecutorService executorService,
      ConcurrentMap<Integer, PartitionState> partitionLoadBalancerStateMap,
      List<PartitionStateUpdateListener.Factory<PartitionState>> listenerFactories,
      String serviceName)
  {
    this(relativeStrategyProperties, quarantineManager, executorService, partitionLoadBalancerStateMap,
        listenerFactories, serviceName, false);
  }

  StateUpdater(D2RelativeStrategyProperties relativeStrategyProperties,
      QuarantineManager quarantineManager,
      ScheduledExecutorService executorService,
      ConcurrentMap<Integer, PartitionState> partitionLoadBalancerStateMap,
      List<PartitionStateUpdateListener.Factory<PartitionState>> listenerFactories,
      String serviceName, boolean loadBalanceStreamException)
  {
    this(relativeStrategyProperties, quarantineManager, executorService, partitionLoadBalancerStateMap,
        listenerFactories, serviceName, loadBalanceStreamException, new NoOpRelativeLoadBalancerStrategyOtelMetricsProvider());
  }

  StateUpdater(D2RelativeStrategyProperties relativeStrategyProperties,
      QuarantineManager quarantineManager,
      ScheduledExecutorService executorService,
      ConcurrentMap<Integer, PartitionState> partitionLoadBalancerStateMap,
      List<PartitionStateUpdateListener.Factory<PartitionState>> listenerFactories,
      String serviceName, boolean loadBalanceStreamException,
      RelativeLoadBalancerStrategyOtelMetricsProvider relativeLbOtelMetricsProvider)
  {
    _relativeStrategyProperties = relativeStrategyProperties;
    _quarantineManager = quarantineManager;
    _executorService = executorService;
    _listenerFactories = listenerFactories;
    _partitionLoadBalancerStateMap = partitionLoadBalancerStateMap;
    _lock = new ReentrantLock();
    _serviceName = serviceName;
    _scheme = NO_VALUE;
    _relativeLbOtelMetricsProvider = relativeLbOtelMetricsProvider;

    scheduledFuture = executorService.scheduleWithFixedDelay(this::updateState, EXECUTOR_INITIAL_DELAY,
        _relativeStrategyProperties.getUpdateIntervalMs(),
        TimeUnit.MILLISECONDS);
    _loadBalanceStreamException = loadBalanceStreamException;
  }

  /**
   * Sets the scheme for this state updater. Used for OTEL metrics tagging.
   * This is called after strategy creation when the scheme becomes available.
   *
   * @param scheme the load balancer scheme (e.g., "http", "https")
   */
  public void setScheme(String scheme)
  {
    if (scheme != null && !scheme.equals(NO_VALUE))
    {
      _scheme = scheme;
    }
  }

  /**
   * Update the state of the partition if necessary
   * This update is triggered by the request. If the cluster is not initialized or the uris changed, we will update the state.
   *  @param trackerClients The set of hosts for this partition
   * @param partitionId The id of the partition
   * @param clusterGenerationId The id that uniquely identifies a set of hosts in the cluster
   * @param shouldForceUpdate Whether or not to force update
   */
  public void updateState(Set<TrackerClient> trackerClients, int partitionId, long clusterGenerationId,
      boolean shouldForceUpdate)
  {
    if (!_partitionLoadBalancerStateMap.containsKey(partitionId))
    {
      // If the partition is not initialized, initialize the state synchronously
      _lock.lock();
      try
      {
        initializePartition(trackerClients, partitionId, clusterGenerationId);
      }
      finally
      {
        _lock.unlock();
      }

    }
    else if (shouldForceUpdate || clusterGenerationId != _partitionLoadBalancerStateMap.get(partitionId).getClusterGenerationId()
        || trackerClients.size() != _partitionLoadBalancerStateMap.get(partitionId).getPointsMap().size())
    {
      // Asynchronously update the state if it is from uri properties change
      _executorService.execute(() -> updateStateDueToClusterChange(trackerClients, partitionId, clusterGenerationId,
          shouldForceUpdate));
    }
  }

  /**
   * Get the hash ring for the partition
   *
   * @param partitionId The id of the partition
   * @return The lastest hash ring of the partition
   */
  Ring<URI> getRing(int partitionId)
  {
    return _partitionLoadBalancerStateMap.get(partitionId).getRing();
  }

  /**
   * Exposed for testings
   */
  Map<URI, Integer> getPointsMap(int partitionId)
  {
    return _partitionLoadBalancerStateMap.get(partitionId) == null
        ? new HashMap<>()
        : _partitionLoadBalancerStateMap.get(partitionId).getPointsMap();
  }

  PartitionState getPartitionState(int partitionId)
  {
    return _partitionLoadBalancerStateMap.get(partitionId);
  }

  /**
   * Return the total tracker clients in all partitions regardless of their statuses.
   */
  int getTotalHostsInAllPartitions()
  {
    return _partitionLoadBalancerStateMap.values().stream()
        .mapToInt(partitionState -> partitionState.getTrackerClients().size())
        .sum();
  }

  /**
   * Return the first valid partition id. This is mainly used for monitoring at least one valid partition.
   */
  int getFirstValidPartitionId()
  {
    return _firstPartitionId;
  }

  /**
   * Update the partition state.
   * This is scheduled by executor, we do not expect any host added/removed from this change
   */
  void updateState()
  {
    try {
      // Update state for each partition
      for (Integer partitionId : _partitionLoadBalancerStateMap.keySet())
      {
        PartitionState partitionState = _partitionLoadBalancerStateMap.get(partitionId);
        updateStateForPartition(partitionState.getTrackerClients(), partitionId, partitionState, partitionState.getClusterGenerationId(),
            false);
      }
    } catch (Exception ex)
    {
      LOG.error("Failed to update the state for service: " + _serviceName, ex);
    }
  }

  /**
   * Update the partition state, steps include
   * 1. Update the base health scores for each {@link TrackerClient} in the cluster based on call stats
   * 2. Handle quarantine and recovery of each host, which may adjust the healthscore further
   * 3. Update the hash ring for this partition
   * 4. Log and notify listeners after the update is done
   *  @param  trackerClients Hosts that belong to this partition
   * @param partitionId Identifies the partition to be updated
   * @param oldPartitionState The partition state of the last interval
   * @param clusterGenerationId The id that identifies the cluster version
   * @param shouldForceUpdate Whether or not to force update
   */
  void updateStateForPartition(Set<TrackerClient> trackerClients, int partitionId, PartitionState oldPartitionState,
      Long clusterGenerationId, boolean shouldForceUpdate)
  {
    LOG.debug("Updating for partition: " + partitionId + ", state: " + oldPartitionState);
    PartitionState newPartitionState = new PartitionState(oldPartitionState);

    // Register per-call OTel latency listener for clients joining this partition for the first time.
    Map<TrackerClient, TrackerClientState> oldStateMap = oldPartitionState.getTrackerClientStateMap();
    for (TrackerClient trackerClient : trackerClients)
    {
      if (!oldStateMap.containsKey(trackerClient) && !trackerClient.doNotLoadBalance())
      {
        trackerClient.setPerCallDurationListener(
            duration -> _relativeLbOtelMetricsProvider.recordHostLatency(_serviceName, _scheme, duration));
      }
    }

    // Step 1: Update the base health scores for each {@link TrackerClient} in the cluster
    Map<TrackerClient, CallTracker.CallStats> latestCallStatsMap = new HashMap<>();
    long avgClusterLatency = getAvgClusterLatency(trackerClients, latestCallStatsMap);
    boolean clusterUpdated = shouldForceUpdate || (clusterGenerationId != oldPartitionState.getClusterGenerationId());
    updateBaseHealthScoreAndState(trackerClients, newPartitionState, avgClusterLatency, clusterUpdated, latestCallStatsMap);

    // Step 2: Handle quarantine and recovery for all tracker clients in this cluster
    // this will adjust the base health score if there is any change in quarantine and recovery map
    _quarantineManager.updateQuarantineState(newPartitionState,
        oldPartitionState, avgClusterLatency);

    // Step 3: Calculate the new ring for each partition
    newPartitionState.updateRing();
    newPartitionState.setClusterGenerationId(clusterGenerationId);
    _partitionLoadBalancerStateMap.put(partitionId, newPartitionState);

    // Step 4: Log and emit monitor event
    _executorService.execute(() -> {
      logState(oldPartitionState, newPartitionState, partitionId);
      emitOtelMetrics(newPartitionState);
      notifyPartitionStateUpdateListener(newPartitionState);
    });
  }

  /**
   * Right after a cluster change, multiple requests may schedule more than 1 update due to async updates
   * We will check the cluster generation id again before performing the actual update to make sure only one updates got executed
   * This can be guaranteed because the executor service has has 1 thread
   */
  void updateStateDueToClusterChange(Set<TrackerClient> trackerClients, int partitionId, Long newClusterGenerationId,
      boolean shouldForceUpdate)
  {
    if (shouldForceUpdate || newClusterGenerationId != _partitionLoadBalancerStateMap.get(partitionId).getClusterGenerationId()
        || trackerClients.size() != _partitionLoadBalancerStateMap.get(partitionId).getPointsMap().size())
    {
      PartitionState oldPartitionState = _partitionLoadBalancerStateMap.get(partitionId);
      updateStateForPartition(trackerClients, partitionId, oldPartitionState, newClusterGenerationId, shouldForceUpdate);
    }
  }

  /**
   * Update the health score of all tracker clients for the service
   */
  private void updateBaseHealthScoreAndState(Set<TrackerClient> trackerClients,
      PartitionState partitionState, long clusterAvgLatency,
      boolean clusterUpdated, Map<TrackerClient, CallTracker.CallStats> lastCallStatsMap)
  {
    // Calculate the base health score before we override them when handling the quarantine and recovery
    calculateBaseHealthScore(trackerClients, partitionState, clusterAvgLatency, lastCallStatsMap);

    // Remove the trackerClients from original map if there is any change in uri list
    Map<TrackerClient, TrackerClientState> trackerClientStateMap = partitionState.getTrackerClientStateMap();
    if (clusterUpdated)
    {
      List<TrackerClient> trackerClientsToRemove = trackerClientStateMap.keySet().stream()
          .filter(oldTrackerClient -> !trackerClients.contains(oldTrackerClient))
          .collect(Collectors.toList());
      for (TrackerClient trackerClient : trackerClientsToRemove)
      {
        partitionState.removeTrackerClient(trackerClient);
      }
    }
  }

  private void calculateBaseHealthScore(Set<TrackerClient> trackerClients, PartitionState partitionState,
      long avgClusterLatency, Map<TrackerClient, CallTracker.CallStats> lastCallStatsMap)
  {
    Map<TrackerClient, TrackerClientState> trackerClientStateMap = partitionState.getTrackerClientStateMap();

    // Update health score
    long clusterCallCount = 0;
    long clusterErrorCount = 0;
    for (TrackerClient trackerClient : trackerClients)
    {
      CallTracker.CallStats latestCallStats = lastCallStatsMap.get(trackerClient);

      if (trackerClientStateMap.containsKey(trackerClient))
      {
        TrackerClientState trackerClientState = trackerClientStateMap.get(trackerClient);
        int callCount = latestCallStats.getCallCount() + latestCallStats.getOutstandingCount();

        if (trackerClient.doNotLoadBalance())
        {
          trackerClientState.setHealthState(TrackerClientState.HealthState.HEALTHY);
          trackerClientState.setHealthScore(MAX_HEALTH_SCORE);
          trackerClientState.setCallCount(callCount);
        }
        else
        {
          double errorRate = getErrorRate(latestCallStats.getErrorTypeCounts(), callCount);
          long avgLatency = getAvgHostLatency(latestCallStats);
          double oldHealthScore = trackerClientState.getHealthScore();
          double newHealthScore = oldHealthScore;

          clusterCallCount += callCount;
          clusterErrorCount += errorRate * callCount;

          if (isUnhealthy(trackerClientState, avgClusterLatency, callCount, avgLatency, errorRate))
          {
            // If it is above high latency, we reduce the health score by down step
            newHealthScore = Double.max(trackerClientState.getHealthScore() - _relativeStrategyProperties.getDownStep(), MIN_HEALTH_SCORE);
            trackerClientState.setHealthState(TrackerClientState.HealthState.UNHEALTHY);

            LOG.debug("Host is unhealthy. Host: " + trackerClient.toString()
                        + ", errorRate: " + errorRate
                        + ", latency: " + avgClusterLatency
                        + ", callCount: " + callCount
                        + ", healthScore dropped from " + trackerClientState.getHealthScore() + " to " + newHealthScore);
          }
          else if (trackerClientState.getHealthScore() < MAX_HEALTH_SCORE
            && isHealthy(trackerClientState, avgClusterLatency, callCount, avgLatency, errorRate))
          {
            if (oldHealthScore < _relativeStrategyProperties.getSlowStartThreshold())
            {
              // If the client is healthy and slow start is enabled, we double the health score
              newHealthScore = oldHealthScore > MIN_HEALTH_SCORE
                ? Math.min(MAX_HEALTH_SCORE, SLOW_START_RECOVERY_FACTOR * oldHealthScore)
                : SLOW_START_INITIAL_HEALTH_SCORE;
            }
            else
            {
              // If slow start is not enabled, we just increase the health score by up step
              newHealthScore = Math.min(MAX_HEALTH_SCORE, oldHealthScore + _relativeStrategyProperties.getUpStep());
            }
            trackerClientState.setHealthState(TrackerClientState.HealthState.HEALTHY);
          }
          else
          {
            trackerClientState.setHealthState(TrackerClientState.HealthState.NEUTRAL);
          }
          trackerClientState.setHealthScore(newHealthScore);
          trackerClientState.setCallCount(callCount);
        }
      }
      else
      {
        // Initializing a new client score
        if (trackerClient.doNotSlowStart() || trackerClient.doNotLoadBalance())
        {
          trackerClientStateMap.put(trackerClient, new TrackerClientState(MAX_HEALTH_SCORE,
              _relativeStrategyProperties.getMinCallCount()));
        }
        else
        {
          trackerClientStateMap.put(trackerClient,
              new TrackerClientState(_relativeStrategyProperties.getInitialHealthScore(), _relativeStrategyProperties.getMinCallCount()));
        }
      }
    }
    partitionState.setPartitionStats(avgClusterLatency, clusterCallCount, clusterErrorCount);
  }

  /**
   * Get the weighted average cluster latency
   */
  private long getAvgClusterLatency(Set<TrackerClient> trackerClients, Map<TrackerClient, CallTracker.CallStats> latestCallStatsMap)
  {
    long latencySum = 0;
    long outstandingLatencySum = 0;
    int callCountSum = 0;
    int outstandingCallCountSum = 0;

    for (TrackerClient trackerClient : trackerClients)
    {
      CallTracker.CallStats latestCallStats = trackerClient.getCallTracker().getCallStats();
      latestCallStatsMap.put(trackerClient, latestCallStats);

      if (trackerClient.doNotLoadBalance())
      {
        continue;
      }

      int callCount = latestCallStats.getCallCount();
      int outstandingCallCount = latestCallStats.getOutstandingCount();
      latencySum += latestCallStats.getCallTimeStats().getAverage() * callCount;
      outstandingLatencySum += latestCallStats.getOutstandingStartTimeAvg() * outstandingCallCount;
      callCountSum += callCount;
      outstandingCallCountSum += outstandingCallCount;
    }

    return callCountSum + outstandingCallCountSum == 0
        ? 0
        : (long) Math.ceil((latencySum + outstandingLatencySum) / (double) (callCountSum + outstandingCallCountSum));
  }

  public static long getAvgHostLatency(CallTracker.CallStats callStats)
  {
    double avgLatency = callStats.getCallTimeStats().getAverage();
    long avgOutstandingLatency = callStats.getOutstandingStartTimeAvg();
    int callCount = callStats.getCallCount();
    int outstandingCallCount = callStats.getOutstandingCount();
    return callCount + outstandingCallCount == 0
        ? 0
        : Math.round(avgLatency * ((double)callCount / (callCount + outstandingCallCount))
            + avgOutstandingLatency * ((double)outstandingCallCount / (callCount + outstandingCallCount)));
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
        && latency <= avgClusterLatency * _relativeStrategyProperties.getRelativeLatencyLowThresholdFactor()
        && errorRate <= _relativeStrategyProperties.getLowErrorRate();
  }

  private void notifyPartitionStateUpdateListener(PartitionState state)
  {
    state.getListeners().forEach(listener -> listener.onUpdate(state));
  }

  /**
   * Emit OpenTelemetry metrics for the current partition state.
   * Host latencies are emitted per-call via the listener registered in
   * {@link #calculateBaseHealthScore}.
   */
  private void emitOtelMetrics(PartitionState partitionState)
  {
    // Update gauge metrics
    _relativeLbOtelMetricsProvider.updateTotalHostsInAllPartitionsCount(_serviceName, _scheme, getTotalHostsInAllPartitions());

    // Count unhealthy hosts
    int unhealthyCount = (int) partitionState.getTrackerClientStateMap().values().stream()
        .filter(TrackerClientState::isUnhealthy)
        .count();
    _relativeLbOtelMetricsProvider.updateUnhealthyHostsCount(_serviceName, _scheme, unhealthyCount);

    // Count quarantine hosts
    Map<TrackerClient, LoadBalancerQuarantine> quarantineMap = partitionState.getQuarantineMap();
    int quarantineCount = (int) quarantineMap.values().stream()
        .filter(LoadBalancerQuarantine::isInQuarantine)
        .count();
    _relativeLbOtelMetricsProvider.updateQuarantineHostsCount(_serviceName, _scheme, quarantineCount);

    // Total points in hash ring
    int totalPoints = partitionState.getPointsMap().values().stream()
        .mapToInt(Integer::intValue)
        .sum();
    _relativeLbOtelMetricsProvider.updateTotalPointsInHashRing(_serviceName, _scheme, totalPoints);
  }

  @VisibleForTesting
  double getErrorRate(Map<ErrorType, Integer> errorTypeCounts, int callCount)
  {
    Integer connectExceptionCount = errorTypeCounts.getOrDefault(ErrorType.CONNECT_EXCEPTION, 0);
    Integer closedChannelExceptionCount = errorTypeCounts.getOrDefault(ErrorType.CLOSED_CHANNEL_EXCEPTION, 0);
    Integer serverErrorCount = errorTypeCounts.getOrDefault(ErrorType.SERVER_ERROR, 0);
    Integer timeoutExceptionCount = errorTypeCounts.getOrDefault(ErrorType.TIMEOUT_EXCEPTION, 0);
    Integer streamErrorCount = errorTypeCounts.getOrDefault(ErrorType.STREAM_ERROR, 0);

    double validExceptionCount = connectExceptionCount + closedChannelExceptionCount + serverErrorCount
        + timeoutExceptionCount;
    if (_loadBalanceStreamException)
    {
      validExceptionCount += streamErrorCount;
    }
    return callCount == 0 ? 0 : validExceptionCount / callCount;
  }

  private void initializePartition(Set<TrackerClient> trackerClients, int partitionId, long clusterGenerationId)
  {
    if (!_partitionLoadBalancerStateMap.containsKey(partitionId))
    {
      PartitionState partitionState = new PartitionState(partitionId,
          new DelegatingRingFactory<>(_relativeStrategyProperties.getRingProperties()),
          _relativeStrategyProperties.getRingProperties().getPointsPerWeight(),
          _listenerFactories.stream().map(factory -> factory.create(partitionId)).collect(Collectors.toList()));

      updateStateForPartition(trackerClients, partitionId, partitionState, clusterGenerationId, false);

      if (_firstPartitionId < 0)
      {
        _firstPartitionId = partitionId;
      }
    }
  }

  private void logState(PartitionState oldState,
      PartitionState newState,
      int partitionId)
  {
    Map<TrackerClient, TrackerClientState> newTrackerClientStateMap = newState.getTrackerClientStateMap();
    Map<TrackerClient, TrackerClientState> oldTrackerClientStateMap = oldState.getTrackerClientStateMap();
    Set<TrackerClient> newUnhealthyClients = newTrackerClientStateMap.keySet().stream()
        .filter(trackerClient -> newTrackerClientStateMap.get(trackerClient).getHealthScore() < MAX_HEALTH_SCORE)
        .collect(Collectors.toSet());
    Set<TrackerClient> oldUnhealthyClients = oldTrackerClientStateMap.keySet().stream()
        .filter(trackerClient -> oldTrackerClientStateMap.get(trackerClient).getHealthScore() < MAX_HEALTH_SCORE)
        .collect(Collectors.toSet());

    if (LOG.isDebugEnabled())
    {
      LOG.debug("Strategy updated: service=" + _serviceName
          + ", partitionId=" + partitionId
          + ", unhealthyClientNumber=" + newUnhealthyClients.size()
          + ", newState=" + newState
          + ", unhealthyClients={" + (newUnhealthyClients.stream().limit(LOG_UNHEALTHY_CLIENT_NUMBERS)
          .map(client -> getClientStats(client, newTrackerClientStateMap)).collect(Collectors.joining(",")))
          + (newUnhealthyClients.size() > LOG_UNHEALTHY_CLIENT_NUMBERS ? "...(total "
          + newUnhealthyClients.size() + ")" : "") + "},"
          + ", oldState=" + oldState);
    }
    else if (allowToLog(oldState, newState, newUnhealthyClients, oldUnhealthyClients))
    {
      LOG.info("Strategy updated: service=" + _serviceName
          + ", partitionId=" + partitionId
          + ", unhealthyClientNumber=" + newUnhealthyClients.size()
          + ", newState=" + newState
          + ", unhealthyClients={" + (newUnhealthyClients.stream().limit(LOG_UNHEALTHY_CLIENT_NUMBERS)
          .map(client -> getClientStats(client, newTrackerClientStateMap)).collect(Collectors.joining(",")))
          + (newUnhealthyClients.size() > LOG_UNHEALTHY_CLIENT_NUMBERS ? "...(total "
          + newUnhealthyClients.size() + ")" : "") + "},"
          + ", oldState=" + oldState);
    }
  }

  /**
   * Only allow to log if there are health score related updates in some hosts
   */
  private static boolean allowToLog(PartitionState oldState, PartitionState newState,
      Set<TrackerClient> newUnhealthyClients, Set<TrackerClient> oldUnhealthyClients)
  {
    for (URI uri : newState.getPointsMap().keySet())
    {
      if (!oldState.getPointsMap().containsKey(uri))
      {
        return true;
      }
    }

    for (TrackerClient client : newUnhealthyClients)
    {
      if (!oldUnhealthyClients.contains(client))
      {
        return true;
      }
    }

    for (TrackerClient trackerClient : newState.getRecoveryTrackerClients())
    {
      if (!oldState.getRecoveryTrackerClients().contains(trackerClient))
      {
        return true;
      }
    }

    for (TrackerClient trackerClient : newState.getQuarantineMap().keySet())
    {
      if (!oldState.getQuarantineMap().containsKey(trackerClient))
      {
        return true;
      }
    }
    return false;
  }

  private static String getClientStats(TrackerClient client, Map<TrackerClient, TrackerClientState> trackerClientStateMap)
  {
    return client.getUri() + ":" + trackerClientStateMap.get(client).getHealthScore();
  }

  public void shutdown()
  {
    LOG.debug("Shutting down the state updater for service: {}", _serviceName);
    scheduledFuture.cancel(true);
  }
}

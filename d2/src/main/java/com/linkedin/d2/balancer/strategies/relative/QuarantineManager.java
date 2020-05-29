package com.linkedin.d2.balancer.strategies.relative;

import com.linkedin.d2.D2QuarantineProperties;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.degrader.LoadBalancerQuarantine;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import com.linkedin.util.clock.Clock;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QuarantineManager {
  private static final Logger _log = LoggerFactory.getLogger(QuarantineManager.class);
  private static final double QUARANTINE_ENABLED_PERCENTAGE = 0.0;
  private static final double FAST_RECOVERY_FACTOR = 2.0;
  private static final double FAST_RECOVERY_HEALTH_SCORE_THRESHOLD = 0.5;
  private static final long QUARANTINE_REENTRY_TIME_MS = 30000;
  // If client weight is set to 0, it means we should not send traffic to that client
  private static final double ZOOKEEPER_CLIENT_WEIGHT_THRESHOLD = 0.0;

  private final String _serviceName;
  private final String _servicePath;
  private final HealthCheckOperations _healthCheckOperations;
  private final D2QuarantineProperties _quarantineProperties;
  private final boolean _isQuarantineEnabled;
  private final boolean _fastRecoveryEnabled;
  private final double _initialHealthScore;
  private final ScheduledExecutorService _executorService;
  private final Clock _clock;
  private final long _updateIntervalMs;
  // lowThresholdFactor reflects the expected health threshold for the service
  //so we can use this factor times avg latency as the quarantine health checking latency
  private final double _relativeLatencyLowThresholdFactor;

  QuarantineManager(String serviceName, String servicePath, HealthCheckOperations healthCheckOperations,
      D2QuarantineProperties quarantineProperties, boolean fastRecoveryEnabled, double initialHealthScore,
      ScheduledExecutorService executorService, Clock clock, long updateIntervalMs, double relativeLatencyLowThresholdFactor)
  {
    _serviceName = serviceName;
    _servicePath = servicePath;
    _healthCheckOperations = healthCheckOperations;
    _quarantineProperties = quarantineProperties;
    _isQuarantineEnabled = quarantineProperties.hasQuarantineMaxPercent()
        && quarantineProperties.getQuarantineMaxPercent() > QUARANTINE_ENABLED_PERCENTAGE;
    _fastRecoveryEnabled = fastRecoveryEnabled;
    _initialHealthScore = initialHealthScore;
    _executorService = executorService;
    _clock = clock;
    _updateIntervalMs = updateIntervalMs;
    _relativeLatencyLowThresholdFactor = relativeLatencyLowThresholdFactor;
  }

  public void updateQuarantineState(PartitionLoadBalancerState partitionLoadBalancerState)
  {
    // Step 1: check if quarantine state still applies. If not, remove it from the quarantine map
    checkAndRemoveQuarantine(partitionLoadBalancerState);
    // Step 2: Handle special clients recovery logic from the recovery map
    handleClientsRecovery(partitionLoadBalancerState);
    // Step 3: Enroll new quarantine and recovery map
    enrollNewQuarantineAndRecoveryMap(partitionLoadBalancerState, partitionLoadBalancerState.getClusterAvgLatency());
  }

  private void checkAndRemoveQuarantine(PartitionLoadBalancerState partitionLoadBalancerState)
  {
    Map<TrackerClient, LoadBalancerQuarantine> quarantineMap = partitionLoadBalancerState.getQuarantineMap();
    Map<TrackerClient, LoadBalancerQuarantine> quarantineHistory = partitionLoadBalancerState.getQuarantineHistory();
    Map<TrackerClient, Double> recoveryMap = partitionLoadBalancerState.getRecoveryMap();

    for (TrackerClient trackerClient : partitionLoadBalancerState.getTrackerClients())
    {
      // Check/update quarantine state if current client is already under quarantine
      LoadBalancerQuarantine quarantine = quarantineMap.get(trackerClient);
      if (quarantine != null && quarantine.checkUpdateQuarantineState())
      {
        // Evict client from quarantine
        quarantineMap.remove(quarantine);
        quarantineHistory.put(trackerClient, quarantine);
        _log.info("TrackerClient {} evicted from quarantine", trackerClient.getUri());

        // When a client get out of the quarantine map, we put it to recovery map with the 0.0 health score
        // TODO: should we use initialHealthScore instead?
        recoveryMap.put(trackerClient, RelativeStateUpdater.LOWEST_HEALTH_SCORE);
      }
    }
  }

  private void handleClientsRecovery(PartitionLoadBalancerState partitionLoadBalancerState)
  {
    for (TrackerClient trackerClient : partitionLoadBalancerState.getTrackerClients())
    {
      handleClientInRecoveryMap(trackerClient, partitionLoadBalancerState.getTrackerClientStateMap().get(trackerClient),
          partitionLoadBalancerState.getRecoveryMap());
    }
  }

  private void enrollNewQuarantineAndRecoveryMap(PartitionLoadBalancerState partitionLoadBalancerState, long clusterAvgLatency) {
    int partitionId = partitionLoadBalancerState.getPartitionId();
    Map<TrackerClient, LoadBalancerQuarantine> quarantineMap = partitionLoadBalancerState.getQuarantineMap();
    Map<TrackerClient, LoadBalancerQuarantine> quarantineHistory = partitionLoadBalancerState.getQuarantineHistory();
    Map<TrackerClient, Double> recoveryMap = partitionLoadBalancerState.getRecoveryMap();

    for (TrackerClient trackerClient : partitionLoadBalancerState.getTrackerClients()) {
      TrackerClientState trackerClientState = partitionLoadBalancerState.getTrackerClientStateMap().get(trackerClient);

      double clientWeight = trackerClient.getPartitionWeight(partitionId);
      double healthScore = trackerClientState.getHealthScore();
      // Check and enroll quarantine map
      boolean isQuarantined = enrollClientInQuarantineMap(trackerClient, trackerClientState, clientWeight, quarantineMap,
          quarantineHistory, partitionLoadBalancerState.getTrackerClientStateMap().size(), clusterAvgLatency);
      // check and enroll recovery map
      enrollClientInRecoveryMap(isQuarantined, trackerClient, trackerClientState, clientWeight, recoveryMap);
    }
  }

  private void handleClientInRecoveryMap(TrackerClient trackerClient, TrackerClientState trackerClientState,
      Map<TrackerClient, Double> recoveryMap)
  {
    if (trackerClientState.getCallCount() < trackerClientState.getAdjustedMinCallCount())
    {
      double healthScore = trackerClientState.getHealthScore();
      if (healthScore <= RelativeStateUpdater.LOWEST_HEALTH_SCORE)
      {
        // Reset the health score to initial health score if health score dropped to 0 before
        trackerClientState.setHealthScore(_initialHealthScore);
      } else if (_fastRecoveryEnabled)
      {
        // If fast recovery is enabled, we perform fast recovery: double the health score
        healthScore *= FAST_RECOVERY_FACTOR;
        trackerClientState.setHealthScore(Math.max(healthScore, RelativeStateUpdater.HEALTHY_HEALTH_SCORE));
      }
    } else if (!_fastRecoveryEnabled
        || !trackerClientState.isUnhealthy()
        || trackerClientState.getHealthScore() > FAST_RECOVERY_HEALTH_SCORE_THRESHOLD)
    {
      /**
       * Remove the client from the map if:
       * 1. fast recovery is not enabled OR
       * 2. the client is not unhealthy any more OR
       * 3. The health score is beyond 0.5, we will let it perform normal recovery
       */
      recoveryMap.remove(trackerClient);
    }
  }

  private boolean enrollClientInQuarantineMap(TrackerClient trackerClient, TrackerClientState trackerClientState,
      double clientWeight, Map<TrackerClient, LoadBalancerQuarantine> quarantineMap,
      Map<TrackerClient, LoadBalancerQuarantine> quarantineHistory, int trackerClientSize, long clusterAvgLatency)
  {
    if (_isQuarantineEnabled) {
      double healthScore = trackerClientState.getHealthScore();

      if (quarantineMap.containsKey(trackerClient)) {
        // If the client is still in quarantine, keep the points to 0 so no real traffic will be used
        trackerClientState.setHealthScore(RelativeStateUpdater.LOWEST_HEALTH_SCORE);
        return true;
      }
      else if (healthScore <= RelativeStateUpdater.LOWEST_HEALTH_SCORE
          && clientWeight > ZOOKEEPER_CLIENT_WEIGHT_THRESHOLD
          && trackerClientState.isUnhealthy()) {
        /**
         * To put a TrackerClient into quarantine, it needs to meet all the following criteria:
         * 1. its health score is less than or equal to the threshold (0.0).
         * 2. The call state in current interval is becoming worse, eg the latency or error rate is higher than the threshold.
         * 3. its clientWeight is greater than 0
         *    (ClientWeight can be 0 when the server's clientWeight in zookeeper is explicitly set to 0 in order to put the server into standby.
         *    In this particular case, we should not put the tracker client into the quarantine).
         * 4. The total clients in the quarantine is less than the pre-configured number max percentage
         */
        if (quarantineMap.size() < Math.ceil(trackerClientSize * _quarantineProperties.getQuarantineMaxPercent()))
        {
          // If quarantine exists, reuse the same object
          LoadBalancerQuarantine quarantine = quarantineHistory.remove(trackerClient);
          if (quarantine == null) {
            long quarantineLatencyThreshold = (long) (clusterAvgLatency * _relativeLatencyLowThresholdFactor);
            quarantine = new LoadBalancerQuarantine(trackerClient, _executorService, _clock, _updateIntervalMs, quarantineLatencyThreshold,
                _quarantineProperties.getHealthCheckMethod().toString(), _quarantineProperties.getHealthCheckPath(), _serviceName,
                _servicePath, _healthCheckOperations);
          }

          // If the trackerClient was just recently evicted from quarantine, it is possible that
          // the service is already in trouble while the quarantine probing approach works
          // fine. In such case we'll reuse the previous waiting duration instead of starting
          // from scratch again
          quarantine.reset((_clock.currentTimeMillis() - quarantine.getLastChecked()) > QUARANTINE_REENTRY_TIME_MS);
          quarantineMap.put(trackerClient, quarantine);

          // reduce the health score to 0 so no real traffic will be used
          trackerClientState.setHealthScore(RelativeStateUpdater.LOWEST_HEALTH_SCORE);
          return true;
        } else {
          _log.error("Quarantine for service {} is full! Could not add {}", _serviceName, trackerClient);
        }
      }
    }
    return false;
  }

  private void enrollClientInRecoveryMap(boolean isQuarantined, TrackerClient trackerClient,
      TrackerClientState trackerClientState, double clientWeight, Map<TrackerClient, Double> recoveryMap)
  {
    if (!isQuarantined
        && trackerClientState.getHealthScore() == RelativeStateUpdater.LOWEST_HEALTH_SCORE
        && clientWeight > ZOOKEEPER_CLIENT_WEIGHT_THRESHOLD)
    {
      // Enroll the client to recovery map if the health score dropped to 0, but zookeeper does not set the client weight to be 0
      trackerClientState.setHealthScore(_initialHealthScore);
      if (!recoveryMap.containsKey(trackerClient)) {
        // TODO: what value to put here
        recoveryMap.put(trackerClient, RelativeStateUpdater.LOWEST_HEALTH_SCORE);
      }
    }

    // TODO: also enroll new client into the recovery map if fast recovery is enabled, double check existing implementation
//    if (!recoveryMap.containsKey(trackerClient)                               // client is not in the map yet
//        && !state.getTrackerClients().contains(client)                        // client is new  ////// TODO
//        && _fastRecoveryEnabled                                               // Fast recovery is enabled
//        && degraderControl.getInitialDropRate() > SLOW_START_THRESHOLD        // Slow start is enabled
//        && !degraderControl.isHigh())                                         // current client is not degrading or QPS is too low
//    {
//      recoveryMap.put(client, clientUpdater.getMaxDropRate());
//      // also set the maxDropRate to the computedDropRate if not 1;
//      double maxDropRate = 1.0 - config.getInitialRecoveryLevel();
//      clientUpdater.setMaxDropRate(Math.min(degraderControl.getCurrentComputedDropRate(), maxDropRate));
//    }
  }
}

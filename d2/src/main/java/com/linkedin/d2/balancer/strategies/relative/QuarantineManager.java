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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.D2QuarantineProperties;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.degrader.LoadBalancerQuarantine;
import com.linkedin.d2.balancer.util.RateLimitedLogger;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheck;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckClientBuilder;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import com.linkedin.util.clock.Clock;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handles the quarantine and recovery logic, see {@link LoadBalancerQuarantine} to understand how quarantine works
 */
public class QuarantineManager {
  private static final Logger LOG = LoggerFactory.getLogger(QuarantineManager.class);
  private static final double SLOW_START_ENABLED_THRESHOLD = 0;

  private static final double QUARANTINE_ENABLED_PERCENTAGE_THRESHOLD = 0.0;
  private static final double FAST_RECOVERY_FACTOR = 2.0;
  private static final double FAST_RECOVERY_HEALTH_SCORE_THRESHOLD = 0.5;
  private static final long QUARANTINE_REENTRY_TIME_MS = 30000;
  private static final double MIN_ZOOKEEPER_CLIENT_WEIGHT = 0.0;
  private static final int MAX_RETRIES_TO_CHECK_QUARANTINE = 5;
  private static final int MAX_HOSTS_TO_PRE_CHECK_QUARANTINE = 10;
  private static final double INITIAL_RECOVERY_HEALTH_SCORE = 0.01;
  private static final long MIN_QUANRANTINE_LATENCY_MS = 300;

  private final String _serviceName;
  private final String _servicePath;
  private final HealthCheckOperations _healthCheckOperations;
  private final D2QuarantineProperties _quarantineProperties;
  private final boolean _slowStartEnabled;
  private final boolean _fastRecoveryEnabled;
  private final double _initialHealthScore;
  private final ScheduledExecutorService _executorService;
  private final Clock _clock;
  private final long _updateIntervalMs;
  private final double _relativeLatencyLowThresholdFactor;
  private final RateLimitedLogger _rateLimitedLogger;

  private final AtomicBoolean _quarantineEnabled;
  private final AtomicInteger _quarantineRetries;
  private ConcurrentMap<TrackerClient, HealthCheck> _healthCheckMap;

  QuarantineManager(String serviceName, String servicePath, HealthCheckOperations healthCheckOperations,
      D2QuarantineProperties quarantineProperties, double slowStartThreshold, boolean fastRecoveryEnabled,
      double initialHealthScore, ScheduledExecutorService executorService, Clock clock, long updateIntervalMs,
      double relativeLatencyLowThresholdFactor)
  {
    _serviceName = serviceName;
    _servicePath = servicePath;
    _healthCheckOperations = healthCheckOperations;
    _quarantineProperties = quarantineProperties;
    _slowStartEnabled = slowStartThreshold > SLOW_START_ENABLED_THRESHOLD;
    _fastRecoveryEnabled = fastRecoveryEnabled;
    _initialHealthScore = initialHealthScore;
    _executorService = executorService;
    _clock = clock;
    _updateIntervalMs = updateIntervalMs;
    _relativeLatencyLowThresholdFactor = relativeLatencyLowThresholdFactor;
    _rateLimitedLogger = new RateLimitedLogger(LOG, RelativeLoadBalancerStrategyFactory.DEFAULT_UPDATE_INTERVAL_MS, clock);

    _quarantineEnabled = new AtomicBoolean(false);
    _quarantineRetries = new AtomicInteger(0);
    _healthCheckMap = new ConcurrentHashMap<>();
  }

  public void updateQuarantineState(PartitionRelativeLoadBalancerState newPartitionRelativeLoadBalancerState,
      PartitionRelativeLoadBalancerState oldPartitionRelativeLoadBalancerState, long clusterAvgLatency)
  {
    long quarantineLatency = Math.max((long) (clusterAvgLatency * _relativeLatencyLowThresholdFactor),
        MIN_QUANRANTINE_LATENCY_MS);
    // Step 0: Pre-check if quarantine method works for clients, if it works, we will mark _quarantineEnabled as true
    preCheckQuarantine(newPartitionRelativeLoadBalancerState, quarantineLatency);
    // Step 1: check if quarantine state still applies. If not, remove it from the quarantine map
    checkAndRemoveQuarantine(newPartitionRelativeLoadBalancerState);
    // Step 2: Handle special clients recovery logic from the recovery map
    handleClientsRecovery(newPartitionRelativeLoadBalancerState);
    // Step 3: Enroll new quarantine and recovery map
    enrollNewQuarantineAndRecoveryMap(newPartitionRelativeLoadBalancerState, oldPartitionRelativeLoadBalancerState, quarantineLatency);
  }

  /**
   * Before actually putting a client into quarantine, check if the specified quarantine method and path works
   */
  private void preCheckQuarantine(PartitionRelativeLoadBalancerState partitionRelativeLoadBalancerState, long quarantineLatency)
  {
    boolean isQuarantineConfigured = _quarantineProperties.hasQuarantineMaxPercent()
        && _quarantineProperties.getQuarantineMaxPercent() > QUARANTINE_ENABLED_PERCENTAGE_THRESHOLD;
    if (isQuarantineConfigured && !_quarantineEnabled.get()
        && _quarantineRetries.incrementAndGet() <= MAX_RETRIES_TO_CHECK_QUARANTINE)
    {
      // if quarantine is configured but not enabled, and we haven't tried MAX_RETRIES_TIMES,
      // check the hosts to see if the quarantine can be enabled.
      Set<TrackerClient> trackerClients = partitionRelativeLoadBalancerState.getTrackerClients();
      _executorService.submit(() -> preCheckQuarantineState(trackerClients, quarantineLatency));

    }
  }

  private boolean tryEnableQuarantine()
  {
    return _quarantineEnabled.compareAndSet(false, true);
  }

  private void preCheckQuarantineState(Set<TrackerClient> trackerClients, long quarantineLatency)
  {
    Callback<None> healthCheckCallback = new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        if (!_quarantineEnabled.get())
        {
          _rateLimitedLogger.warn("Error enabling quarantine. Health checking failed for service {}: ", _serviceName, e);
        }
      }

      @Override
      public void onSuccess(None result)
      {
        if (tryEnableQuarantine())
        {
          LOG.info("Quarantine is enabled for service {}", _serviceName);
        }
      }
    };

    // Ideally we would like to healthchecking all the service hosts (ie all TrackerClients) because
    // this can help to warm up the R2 connections to the service hosts, thus speed up the initial access
    // speed when d2client starts to access those hosts. However this can expose/expedite the problem that
    // the d2client host needs too many connections or file handles to all the hosts, when the downstream
    // services have large amount of hosts. Before that problem is addressed, we limit the number of hosts
    // for pre-healthchecking to a small number
    trackerClients.stream().limit(MAX_HOSTS_TO_PRE_CHECK_QUARANTINE)
        .forEach(client -> {
          try
          {
            HealthCheck healthCheckClient = _healthCheckMap.get(client);
            if (healthCheckClient == null)
            {
              // create a new client if not exits
              healthCheckClient =  new HealthCheckClientBuilder()
                  .setHealthCheckOperations(_healthCheckOperations)
                  .setHealthCheckPath(_quarantineProperties.getHealthCheckPath())
                  .setServicePath(_servicePath)
                  .setClock(_clock)
                  .setLatency(quarantineLatency)
                  .setMethod(_quarantineProperties.getHealthCheckMethod().toString())
                  .setClient(client)
                  .build();
              _healthCheckMap.put(client, healthCheckClient);
            }
            healthCheckClient.checkHealth(healthCheckCallback);
          }
          catch (URISyntaxException e)
          {
            LOG.error("Error to build healthCheckClient ", e);
          }
        });

    // also remove the entries that the corresponding trackerClientUpdaters do not exist anymore
    for (TrackerClient client : _healthCheckMap.keySet())
    {
      if (!trackerClients.contains(client))
      {
        _healthCheckMap.remove(client);
      }
    }
  }

  private void checkAndRemoveQuarantine(PartitionRelativeLoadBalancerState partitionRelativeLoadBalancerState)
  {
    Map<TrackerClient, LoadBalancerQuarantine> quarantineMap = partitionRelativeLoadBalancerState.getQuarantineMap();
    Map<TrackerClient, LoadBalancerQuarantine> quarantineHistory = partitionRelativeLoadBalancerState.getQuarantineHistory();
    Map<TrackerClient, Double> recoveryMap = partitionRelativeLoadBalancerState.getRecoveryMap();

    for (TrackerClient trackerClient : partitionRelativeLoadBalancerState.getTrackerClients())
    {
      // Check/update quarantine state if current client is already under quarantine
      LoadBalancerQuarantine quarantine = quarantineMap.get(trackerClient);
      if (quarantine != null && quarantine.checkUpdateQuarantineState())
      {
        // Evict client from quarantine
        quarantineMap.remove(quarantine);
        quarantineHistory.put(trackerClient, quarantine);
        LOG.info("TrackerClient {} evicted from quarantine", trackerClient.getUri());

        recoveryMap.put(trackerClient, INITIAL_RECOVERY_HEALTH_SCORE);
      }
    }
  }

  private void handleClientsRecovery(PartitionRelativeLoadBalancerState partitionRelativeLoadBalancerState)
  {
    for (TrackerClient trackerClient : partitionRelativeLoadBalancerState.getTrackerClients())
    {
      handleClientInRecoveryMap(trackerClient, partitionRelativeLoadBalancerState.getTrackerClientStateMap().get(trackerClient),
          partitionRelativeLoadBalancerState.getRecoveryMap());
    }
  }

  private void enrollNewQuarantineAndRecoveryMap(
      PartitionRelativeLoadBalancerState newPartitionRelativeLoadBalancerState,
      PartitionRelativeLoadBalancerState oldPartitionRelativeLoadBalancerState, long clusterAvgLatency) {
    int partitionId = newPartitionRelativeLoadBalancerState.getPartitionId();
    Map<TrackerClient, LoadBalancerQuarantine> quarantineMap = newPartitionRelativeLoadBalancerState.getQuarantineMap();
    Map<TrackerClient, LoadBalancerQuarantine> quarantineHistory = newPartitionRelativeLoadBalancerState.getQuarantineHistory();
    Map<TrackerClient, Double> recoveryMap = newPartitionRelativeLoadBalancerState.getRecoveryMap();

    for (TrackerClient trackerClient : newPartitionRelativeLoadBalancerState.getTrackerClients()) {
      TrackerClientState trackerClientState = newPartitionRelativeLoadBalancerState.getTrackerClientStateMap().get(trackerClient);

      double clientWeight = trackerClient.getPartitionWeight(partitionId);
      // Check and enroll quarantine map
      boolean isQuarantined = enrollClientInQuarantineMap(trackerClient, trackerClientState, clientWeight, quarantineMap,
          quarantineHistory, newPartitionRelativeLoadBalancerState.getTrackerClientStateMap().size(), clusterAvgLatency);
      // check and enroll recovery map
      enrollClientInRecoveryMap(isQuarantined, trackerClient, trackerClientState, clientWeight, recoveryMap,
          oldPartitionRelativeLoadBalancerState);
    }
  }

  private void handleClientInRecoveryMap(TrackerClient trackerClient, TrackerClientState trackerClientState,
      Map<TrackerClient, Double> recoveryMap)
  {
    if (trackerClientState.getCallCount() < trackerClientState.getAdjustedMinCallCount())
    {
      double healthScore = trackerClientState.getHealthScore();
      if (healthScore <= RelativeStateUpdater.MIN_HEALTH_SCORE)
      {
        // Reset the health score to initial recovery health score if health score dropped to 0 before
        trackerClientState.setHealthScore(INITIAL_RECOVERY_HEALTH_SCORE);
      } else if (_fastRecoveryEnabled)
      {
        // If fast recovery is enabled, we perform fast recovery: double the health score
        healthScore *= FAST_RECOVERY_FACTOR;
        trackerClientState.setHealthScore(Math.min(healthScore, RelativeStateUpdater.MAX_HEALTH_SCORE));
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
      Map<TrackerClient, LoadBalancerQuarantine> quarantineHistory, int trackerClientSize, long quarantineLatency)
  {
    if (_quarantineEnabled.get()) {
      double healthScore = trackerClientState.getHealthScore();

      if (quarantineMap.containsKey(trackerClient)) {
        // If the client is still in quarantine, keep the points to 0 so no real traffic will be used
        trackerClientState.setHealthScore(RelativeStateUpdater.MIN_HEALTH_SCORE);
        return true;
      }
      else if (healthScore <= RelativeStateUpdater.MIN_HEALTH_SCORE
          && clientWeight > MIN_ZOOKEEPER_CLIENT_WEIGHT
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
            quarantine = new LoadBalancerQuarantine(trackerClient, _executorService, _clock, _updateIntervalMs, quarantineLatency,
                _quarantineProperties.getHealthCheckMethod().toString(), _quarantineProperties.getHealthCheckPath(), _serviceName,
                _servicePath, _healthCheckOperations);
          }
          quarantine.reset((_clock.currentTimeMillis() - quarantine.getLastChecked()) > QUARANTINE_REENTRY_TIME_MS);
          quarantineMap.put(trackerClient, quarantine);
          return true;
        } else {
          LOG.error("Quarantine for service {} is full! Could not add {}", _serviceName, trackerClient);
        }
      }
    }
    return false;
  }

  private void enrollClientInRecoveryMap(boolean isQuarantined, TrackerClient trackerClient,
      TrackerClientState trackerClientState, double clientWeight, Map<TrackerClient, Double> recoveryMap,
      PartitionRelativeLoadBalancerState oldPartitionRelativeLoadBalancerState)
  {
    if (!isQuarantined
        && trackerClientState.getHealthScore() == RelativeStateUpdater.MIN_HEALTH_SCORE
        && clientWeight > MIN_ZOOKEEPER_CLIENT_WEIGHT)
    {
      // Enroll the client to recovery map if the health score dropped to 0, but zookeeper does not set the client weight to be 0
      trackerClientState.setHealthScore(INITIAL_RECOVERY_HEALTH_SCORE);
      if (!recoveryMap.containsKey(trackerClient)) {
        recoveryMap.put(trackerClient, RelativeStateUpdater.MIN_HEALTH_SCORE);
      }
    }

    // Also enroll new client into the recovery map if fast recovery and slow start are both enabled
    if (!recoveryMap.containsKey(trackerClient)
        && !oldPartitionRelativeLoadBalancerState.getTrackerClients().contains(trackerClient)
        && _fastRecoveryEnabled
        && _slowStartEnabled)
    {
      recoveryMap.put(trackerClient, _initialHealthScore);
    }
  }
}

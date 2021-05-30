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
import com.linkedin.d2.balancer.strategies.LoadBalancerQuarantine;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheck;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckClientBuilder;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import com.linkedin.util.RateLimitedLogger;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
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
  public static final double SLOW_START_ENABLED_THRESHOLD = 0;
  public static final double FAST_RECOVERY_HEALTH_SCORE_THRESHOLD = 0.5;
  public static final double INITIAL_RECOVERY_HEALTH_SCORE = 0.01;

  private static final double DOUBLE_COMPARISON_THRESHOLD = 10e-4;
  private static final double QUARANTINE_ENABLED_PERCENTAGE_THRESHOLD = 0.0;
  private static final double FAST_RECOVERY_FACTOR = 2.0;
  private static final double MIN_ZOOKEEPER_SERVER_WEIGHT = 0.0;
  private static final int MAX_RETRIES_TO_CHECK_QUARANTINE = 5;
  private static final int MAX_HOSTS_TO_PRE_CHECK_QUARANTINE = 10;
  private static final long MIN_QUANRANTINE_LATENCY_MS = 300;

  private final String _serviceName;
  private final String _servicePath;
  private final HealthCheckOperations _healthCheckOperations;
  private final D2QuarantineProperties _quarantineProperties;
  private final boolean _slowStartEnabled;
  private final boolean _fastRecoveryEnabled;
  private final ScheduledExecutorService _executorService;
  private final Clock _clock;
  private final long _updateIntervalMs;
  private final double _relativeLatencyLowThresholdFactor;
  private final RateLimitedLogger _rateLimitedLogger;

  private final AtomicBoolean _quarantineEnabled;
  private final AtomicInteger _quarantineRetries;

  QuarantineManager(String serviceName, String servicePath, HealthCheckOperations healthCheckOperations,
      D2QuarantineProperties quarantineProperties, double slowStartThreshold, boolean fastRecoveryEnabled,
      ScheduledExecutorService executorService, Clock clock, long updateIntervalMs, double relativeLatencyLowThresholdFactor)
  {
    _serviceName = serviceName;
    _servicePath = servicePath;
    _healthCheckOperations = healthCheckOperations;
    _quarantineProperties = quarantineProperties;
    _slowStartEnabled = slowStartThreshold > SLOW_START_ENABLED_THRESHOLD;
    _fastRecoveryEnabled = fastRecoveryEnabled;
    _executorService = executorService;
    _clock = clock;
    _updateIntervalMs = updateIntervalMs;
    _relativeLatencyLowThresholdFactor = relativeLatencyLowThresholdFactor;
    _rateLimitedLogger = new RateLimitedLogger(LOG, RelativeLoadBalancerStrategyFactory.DEFAULT_UPDATE_INTERVAL_MS, clock);

    _quarantineEnabled = new AtomicBoolean(false);
    _quarantineRetries = new AtomicInteger(0);
  }

  /**
   * Update the health scores in {@link PartitionState} based on quarantine and recovery condition
   *
   * @param newPartitionState the new state of the load balancer
   * @param oldPartitionState the existing state of the load balancer
   * @param clusterAvgLatency The average latency of the cluster
   */
  public void updateQuarantineState(PartitionState newPartitionState,
      PartitionState oldPartitionState, long clusterAvgLatency)
  {
    long quarantineLatency = Math.max((long) (clusterAvgLatency * _relativeLatencyLowThresholdFactor),
        MIN_QUANRANTINE_LATENCY_MS);
    long currentTime = _clock.millis();
    // Step 0: Pre-check if quarantine method works for clients, if it works, we will mark _quarantineEnabled as true
    preCheckQuarantine(newPartitionState, quarantineLatency);
    // Step 1: check if quarantine state still applies. If not, remove it from the quarantine map
    checkAndRemoveQuarantine(newPartitionState);
    // Step 2: Handle special clients recovery logic from the recovery map
    handleClientsRecovery(newPartitionState);
    // Step 3: Enroll new quarantine and recovery map
    enrollNewQuarantineAndRecovery(newPartitionState, oldPartitionState, quarantineLatency, currentTime);
  }

  /**
   * Before actually putting a client into quarantine, check if the specified quarantine method and path works
   */
  private void preCheckQuarantine(PartitionState partitionState, long quarantineLatency)
  {
    boolean isQuarantineConfigured = _quarantineProperties.hasQuarantineMaxPercent()
        && _quarantineProperties.getQuarantineMaxPercent() > QUARANTINE_ENABLED_PERCENTAGE_THRESHOLD;
    if (isQuarantineConfigured && !_quarantineEnabled.get()
        && _quarantineRetries.incrementAndGet() <= MAX_RETRIES_TO_CHECK_QUARANTINE)
    {
      // if quarantine is configured but not enabled, and we haven't tried MAX_RETRIES_TIMES,
      // check the hosts to see if the quarantine can be enabled.
      _executorService.submit(() -> preCheckQuarantineState(partitionState, quarantineLatency));

    }
  }

  boolean tryEnableQuarantine()
  {
    return _quarantineEnabled.compareAndSet(false, true);
  }

  /**
   * Pre-check if quarantine can be enabled before directly enabling it
   * We limit the number of server hosts to prevent too many connections to be made at once when the downstream cluster is large
   *
   * @param partitionState The state of the partition
   * @param quarantineLatency The quarantine latency threshold
   */
  private void preCheckQuarantineState(PartitionState partitionState, long quarantineLatency)
  {
    Callback<None> healthCheckCallback = new HealthCheckCallBack<>();
    partitionState.getTrackerClients().stream().limit(MAX_HOSTS_TO_PRE_CHECK_QUARANTINE)
        .forEach(client -> {
          try
          {
            HealthCheck healthCheckClient = partitionState.getHealthCheckMap().get(client);
            if (healthCheckClient == null)
            {
              healthCheckClient = new HealthCheckClientBuilder()
                  .setHealthCheckOperations(_healthCheckOperations)
                  .setHealthCheckPath(_quarantineProperties.getHealthCheckPath())
                  .setServicePath(_servicePath)
                  .setClock(_clock)
                  .setLatency(quarantineLatency)
                  .setMethod(_quarantineProperties.getHealthCheckMethod().toString())
                  .setClient(client)
                  .build();
              partitionState.getHealthCheckMap().put(client, healthCheckClient);
            }
            healthCheckClient.checkHealth(healthCheckCallback);
          }
          catch (URISyntaxException e)
          {
            LOG.error("Error to build healthCheckClient ", e);
          }
        });
  }

  /**
   * Check if the quarantine still applies for each tracker client.
   * Remove it from the map if the quarantine is no long applicable. Put the client into recovery state right after the quarantine.
   *
   * @param partitionState The current state of the partition
   */
  private void checkAndRemoveQuarantine(PartitionState partitionState)
  {
    Map<TrackerClient, LoadBalancerQuarantine> quarantineMap = partitionState.getQuarantineMap();
    Map<TrackerClient, LoadBalancerQuarantine> quarantineHistory = partitionState.getQuarantineHistory();
    Set<TrackerClient> recoverySet = partitionState.getRecoveryTrackerClients();

    for (TrackerClient trackerClient : partitionState.getTrackerClients())
    {
      LoadBalancerQuarantine quarantine = quarantineMap.get(trackerClient);
      if (quarantine != null && quarantine.checkUpdateQuarantineState())
      {
        // Evict client from quarantine
        quarantineMap.remove(trackerClient);
        quarantineHistory.put(trackerClient, quarantine);

        recoverySet.add(trackerClient);
      }
    }
  }

  /**
   * Handle the recovery for all the tracker clients in the recovery set
   *
   * @param partitionState The current state of the partition
   */
  private void handleClientsRecovery(PartitionState partitionState)
  {
    for (TrackerClient trackerClient : partitionState.getTrackerClients())
    {
      Set<TrackerClient> recoverySet = partitionState.getRecoveryTrackerClients();
      if (recoverySet.contains(trackerClient))
      {
        handleSingleClientInRecovery(trackerClient, partitionState.getTrackerClientStateMap().get(trackerClient),
            partitionState.getRecoveryTrackerClients());
      }
    }
  }

  /**
   * Enroll new tracker client to quarantine or recovery state
   *
   * @param newPartitionState The new state of the partition
   * @param oldPartitionState The old state of the partition
   * @param clusterAvgLatency The average latency of the cluster of last interval
   */
  private void enrollNewQuarantineAndRecovery(
      PartitionState newPartitionState,
      PartitionState oldPartitionState, long clusterAvgLatency, long currentTime)
  {
    int partitionId = newPartitionState.getPartitionId();
    Map<TrackerClient, LoadBalancerQuarantine> quarantineMap = newPartitionState.getQuarantineMap();
    Map<TrackerClient, LoadBalancerQuarantine> quarantineHistory = newPartitionState.getQuarantineHistory();
    Set<TrackerClient> recoverySet = newPartitionState.getRecoveryTrackerClients();

    for (TrackerClient trackerClient : newPartitionState.getTrackerClients())
    {
      TrackerClientState trackerClientState = newPartitionState.getTrackerClientStateMap().get(trackerClient);

      double serverWeight = trackerClient.getPartitionWeight(partitionId);
      // Check and enroll quarantine map
      boolean isQuarantined = enrollClientInQuarantineMap(trackerClient, trackerClientState, serverWeight, quarantineMap,
          quarantineHistory, newPartitionState.getTrackerClientStateMap().size(), clusterAvgLatency, currentTime);

      if (!isQuarantined)
      {
        if (!_fastRecoveryEnabled)
        {
          performNormalRecovery(trackerClientState);
        }
        else
        {
          // Only enroll the client into recovery state if fast recovery is enabled
          enrollSingleClientInRecoverySet(trackerClient, trackerClientState, serverWeight, recoverySet,
              oldPartitionState);
        }
      }
    }
  }

  /**
   * Perform fast recovery for hosts in the recovery set
   * Fast recovery will double the current health score
   *
   * @param trackerClient The {@link TrackerClient} to be recovered
   * @param trackerClientState The state of the {@link TrackerClient}
   * @param recoverySet A set of {@link TrackerClient} to be recovered
   */
  private void handleSingleClientInRecovery(TrackerClient trackerClient, TrackerClientState trackerClientState,
      Set<TrackerClient> recoverySet)
  {
    if (trackerClientState.getCallCount() < trackerClientState.getAdjustedMinCallCount())
    {
      double healthScore = trackerClientState.getHealthScore();
      if (healthScore <= StateUpdater.MIN_HEALTH_SCORE + DOUBLE_COMPARISON_THRESHOLD)
      {
        // Reset the health score to initial recovery health score if health score dropped to 0 before
        trackerClientState.setHealthScore(INITIAL_RECOVERY_HEALTH_SCORE);
      }
      else
      {
        // Perform fast recovery: double the health score
        healthScore *= FAST_RECOVERY_FACTOR;
        trackerClientState.setHealthScore(Math.min(healthScore, StateUpdater.MAX_HEALTH_SCORE));
      }
    }
    else if (trackerClientState.isUnhealthy() || trackerClientState.getHealthScore() > FAST_RECOVERY_HEALTH_SCORE_THRESHOLD)
    {
      /**
       * Remove the client from the map if the client is unhealthy or he health score is beyond 0.5
       */
      recoverySet.remove(trackerClient);
    }
  }

  /**
   * To put a TrackerClient into quarantine, it needs to meet all the following criteria:
   * 1. its health score is less than or equal to the threshold (0.0).
   * 2. The call state in current interval is becoming worse, eg the latency or error rate is higher than the threshold.
   * 3. its clientWeight is greater than 0
   *    (ClientWeight can be 0 when the server's clientWeight in zookeeper is explicitly set to 0 in order to put the server into standby.
   *    In this particular case, we should not put the tracker client into the quarantine).
   * 4. The total clients in the quarantine is less than the pre-configured number max percentage
   *
   * @param trackerClient The server to be quarantined
   * @param trackerClientState The current state of the server
   * @param serverWeight The weight of the server host specified from Zookeeper
   * @param quarantineMap A map of current quarantined hosts
   * @param quarantineHistory The hosts that used to be quarantined
   * @param trackerClientSize The total number of hosts in the partition
   * @param quarantineLatency The quarantine latency threshold
   * @return True if the host is quarantined
   */
  private boolean enrollClientInQuarantineMap(TrackerClient trackerClient, TrackerClientState trackerClientState,
      double serverWeight, Map<TrackerClient, LoadBalancerQuarantine> quarantineMap,
      Map<TrackerClient, LoadBalancerQuarantine> quarantineHistory, int trackerClientSize, long quarantineLatency,
      long currentTime)
  {
    if (_quarantineEnabled.get())
    {
      double healthScore = trackerClientState.getHealthScore();

      if (quarantineMap.containsKey(trackerClient))
      {
        return true;
      }
      else if (healthScore <= StateUpdater.MIN_HEALTH_SCORE + DOUBLE_COMPARISON_THRESHOLD
          && serverWeight > MIN_ZOOKEEPER_SERVER_WEIGHT
          && trackerClientState.isUnhealthy())
      {
        if (quarantineMap.size() < Math.ceil(trackerClientSize * _quarantineProperties.getQuarantineMaxPercent()))
        {
          // If quarantine exists, reuse the same object
          LoadBalancerQuarantine quarantine = quarantineHistory.remove(trackerClient);
          if (quarantine == null)
          {
            quarantine = new LoadBalancerQuarantine(trackerClient, _executorService, _clock, _updateIntervalMs, quarantineLatency,
                _quarantineProperties.getHealthCheckMethod().toString(), _quarantineProperties.getHealthCheckPath(), _serviceName,
                _servicePath, _healthCheckOperations);
          }
          quarantine.reset(currentTime);
          quarantineMap.put(trackerClient, quarantine);
          return true;
        }
        else
        {
          LOG.warn("Quarantine for service {} is full! Could not add {}", _serviceName, trackerClient);
        }
      }
    }
    return false;
  }

  /**
   * For normal recovery, if a client is not quarantined, we will adjust the health score back to 0.01 from 0 so that it can get some traffic
   */
  private void performNormalRecovery(TrackerClientState trackerClientState)
  {
    if (trackerClientState.getHealthScore() <= StateUpdater.MIN_HEALTH_SCORE + DOUBLE_COMPARISON_THRESHOLD)
    {
      trackerClientState.setHealthScore(INITIAL_RECOVERY_HEALTH_SCORE);
    }
  }

  private void enrollSingleClientInRecoverySet(TrackerClient trackerClient,
      TrackerClientState trackerClientState, double serverWeight, Set<TrackerClient> recoverySet,
      PartitionState oldPartitionState)
  {
    if (trackerClientState.getHealthScore() <= StateUpdater.MIN_HEALTH_SCORE + DOUBLE_COMPARISON_THRESHOLD
        && serverWeight > MIN_ZOOKEEPER_SERVER_WEIGHT)
    {
      // Enroll the client to recovery set if the health score dropped to 0, but zookeeper does not set the client weight to be 0
      trackerClientState.setHealthScore(INITIAL_RECOVERY_HEALTH_SCORE);
      if (!recoverySet.contains(trackerClient))
      {
        recoverySet.add(trackerClient);
      }
    }

    // Also enroll new client into the recovery set if slow start is enabled
    if (!recoverySet.contains(trackerClient)
        && !oldPartitionState.getTrackerClients().contains(trackerClient)
        && _slowStartEnabled)
    {
      recoverySet.add(trackerClient);
    }
  }

  private class HealthCheckCallBack<None> implements Callback<None>
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
  }
}

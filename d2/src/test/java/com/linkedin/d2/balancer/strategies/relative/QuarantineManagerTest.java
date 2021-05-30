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

import com.linkedin.d2.D2QuarantineProperties;
import com.linkedin.d2.HttpMethod;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.LoadBalancerQuarantine;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;

import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


/**
 * Test for {@link QuarantineManager}
 */
public class QuarantineManagerTest
{
  private static final String SERVICE_NAME = "dummyService";
  private static final String SERVICE_PATH = "dummyServicePath";
  private static final HealthCheckOperations HEALTH_CHECK_OPERATIONS = new HealthCheckOperations();
  private static final long DEFAULT_AVG_CLUSTER_LATENCY = 100;

  private final ScheduledExecutorService _executorService = Mockito.mock(ScheduledExecutorService.class);
  private QuarantineManager _quarantineManager;

  private void setup(double quarantineMaxPercent, boolean slowStartEnabled, boolean fastRecoveryEnabled)
  {
    double slowStartThreshold = slowStartEnabled ? 0.5 : 0;
    D2QuarantineProperties d2QuarantineProperties = new D2QuarantineProperties().setQuarantineMaxPercent(quarantineMaxPercent)
        .setHealthCheckMethod(HttpMethod.OPTIONS);
    _quarantineManager = new QuarantineManager(SERVICE_NAME, SERVICE_PATH, HEALTH_CHECK_OPERATIONS, d2QuarantineProperties,
        slowStartThreshold, fastRecoveryEnabled, _executorService, Clock.systemUTC(),
        RelativeLoadBalancerStrategyFactory.DEFAULT_UPDATE_INTERVAL_MS,
        RelativeLoadBalancerStrategyFactory.DEFAULT_RELATIVE_LATENCY_LOW_THRESHOLD_FACTOR);
  }

  @Test
  public void testQuarantineNotEnabledInConfig()
  {
    setup(RelativeLoadBalancerStrategyFactory.DEFAULT_QUARANTINE_MAX_PERCENT, false, false);

    PartitionState state = new PartitionStateTestDataBuilder()
        .setTrackerClientStateMap(TrackerClientMockHelper.mockTrackerClients(2),
            Arrays.asList(StateUpdater.MIN_HEALTH_SCORE, 0.6),
            Arrays.asList(TrackerClientState.HealthState.UNHEALTHY, TrackerClientState.HealthState.UNHEALTHY),
            Arrays.asList(20, 20))
        .build();

    _quarantineManager.updateQuarantineState(state, state, DEFAULT_AVG_CLUSTER_LATENCY);

    Mockito.verifyZeroInteractions(_executorService);
    assertTrue(state.getQuarantineMap().isEmpty(), "Quarantine should not be enabled.");
  }

  @Test(dataProvider = "unhealthyHealthScore")
  public void testQuarantineHost(double unhealthyHealthScore)
  {
    setup(0.5, false, false);
    _quarantineManager.tryEnableQuarantine();

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(2);
    PartitionState state = new PartitionStateTestDataBuilder()
        .setTrackerClientStateMap(trackerClients,
            Arrays.asList(unhealthyHealthScore, StateUpdater.MAX_HEALTH_SCORE),
            Arrays.asList(TrackerClientState.HealthState.UNHEALTHY, TrackerClientState.HealthState.UNHEALTHY),
            Arrays.asList(20, 20))
        .build();

    _quarantineManager.updateQuarantineState(state, state, DEFAULT_AVG_CLUSTER_LATENCY);

    if (unhealthyHealthScore == StateUpdater.MIN_HEALTH_SCORE)
    {
      assertEquals(state.getQuarantineMap().size(), 1, "Only 1 host should be quarantined.");
      assertTrue(state.getQuarantineMap().containsKey(trackerClients.get(0)));
      assertTrue(state.getRecoveryTrackerClients().isEmpty());
    }
    else
    {
      assertTrue(state.getQuarantineMap().isEmpty(), "No host should be quarantined.");
      assertTrue(state.getRecoveryTrackerClients().isEmpty());
    }

  }

  @DataProvider(name = "unhealthyHealthScore")
  Object[][] getUnhealthyHealthScore()
  {
    return new Object[][]
        {
            {StateUpdater.MIN_HEALTH_SCORE},
            {0.6},
            {0.8}
        };
  }

  @Test
  public void testQuarantinedMaxPercentage()
  {
    setup(0.5, false, false);
    _quarantineManager.tryEnableQuarantine();

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(4);
    PartitionState state = new PartitionStateTestDataBuilder()
        .setTrackerClientStateMap(trackerClients,
            Arrays.asList(StateUpdater.MIN_HEALTH_SCORE, StateUpdater.MIN_HEALTH_SCORE, StateUpdater.MIN_HEALTH_SCORE, 0.6),
            Arrays.asList(TrackerClientState.HealthState.UNHEALTHY, TrackerClientState.HealthState.UNHEALTHY,
                TrackerClientState.HealthState.UNHEALTHY, TrackerClientState.HealthState.UNHEALTHY),
            Arrays.asList(20, 20, 20, 20))
        .build();

    _quarantineManager.updateQuarantineState(state, state, DEFAULT_AVG_CLUSTER_LATENCY);

    assertEquals(state.getQuarantineMap().size(), 2, "Only 2 hosts should be quarantined even if 3 hosts are unhealthy.");
  }

  @Test(dataProvider = "trueFalse")
  public void testQuarantineCheck(boolean quarantineCheckResult)
  {
    setup(0.5, false, false);
    LoadBalancerQuarantine quarantine = Mockito.mock(LoadBalancerQuarantine.class);
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3);
    Map<TrackerClient, LoadBalancerQuarantine> existingQuarantineMap = new HashMap<>();
    existingQuarantineMap.put(trackerClients.get(0), quarantine);
    Mockito.when(quarantine.checkUpdateQuarantineState()).thenReturn(quarantineCheckResult);

    PartitionState state = new PartitionStateTestDataBuilder()
        .setTrackerClientStateMap(trackerClients,
            Arrays.asList(StateUpdater.MIN_HEALTH_SCORE, 0.6, 0.6),
            Arrays.asList(TrackerClientState.HealthState.NEUTRAL, TrackerClientState.HealthState.UNHEALTHY, TrackerClientState.HealthState.UNHEALTHY),
            Arrays.asList(20, 20, 20))
        .setQuarantineMap(existingQuarantineMap)
        .build();

    _quarantineManager.tryEnableQuarantine();
    _quarantineManager.updateQuarantineState(state, state, DEFAULT_AVG_CLUSTER_LATENCY);

    if (quarantineCheckResult)
    {
      assertTrue(state.getQuarantineMap().isEmpty());
      assertEquals(state.getRecoveryTrackerClients().size(), 1,
          "The quarantine should be over and the host should be put into recovery");
      assertEquals(state.getTrackerClientStateMap().get(trackerClients.get(0)).getHealthScore(), QuarantineManager.INITIAL_RECOVERY_HEALTH_SCORE);
    }
    else
    {
      assertEquals(state.getQuarantineMap().size(), 1,
          "Quarantine health check failed, the host should be kept in quarantine state");
      assertTrue(state.getRecoveryTrackerClients().isEmpty(), "No client should be in recovery state");
      assertTrue(state.getQuarantineMap().containsKey(trackerClients.get(0)));
    }
  }

  @Test(dataProvider = "trackerClientState")
  public void testFastRecoveryInRecoveryMap(int callCount, TrackerClientState.HealthState healthState, double healthScore)
  {
    setup(0.5, false, true);
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3);
    Set<TrackerClient> recoverySet = new HashSet<>();
    recoverySet.add(trackerClients.get(0));

    PartitionState state = new PartitionStateTestDataBuilder()
        .setTrackerClientStateMap(trackerClients,
            Arrays.asList(healthScore, 0.6, 0.6),
            Arrays.asList(healthState, TrackerClientState.HealthState.UNHEALTHY, TrackerClientState.HealthState.UNHEALTHY),
            Arrays.asList(callCount, 20, 20))
        .setRecoveryClients(recoverySet)
        .build();

    _quarantineManager.tryEnableQuarantine();

    _quarantineManager.updateQuarantineState(state, state, DEFAULT_AVG_CLUSTER_LATENCY);

    if (callCount <= RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT)
    {
      assertEquals(state.getTrackerClientStateMap().get(trackerClients.get(0)).getHealthScore(), healthScore * 2,
          "The health score should be doubled when fast recovery is enabled");
      assertTrue(state.getRecoveryTrackerClients().contains(trackerClients.get(0)));
    }
    else if (healthState != TrackerClientState.HealthState.UNHEALTHY && healthScore <= QuarantineManager.FAST_RECOVERY_HEALTH_SCORE_THRESHOLD)
    {
      assertEquals(state.getTrackerClientStateMap().get(trackerClients.get(0)).getHealthScore(), healthScore,
          "The health score should not change");
      assertTrue(state.getRecoveryTrackerClients().contains(trackerClients.get(0)));
    }
    else
    {
      assertTrue(state.getRecoveryTrackerClients().isEmpty(), "The host should come out of recovery");
    }
  }

  @DataProvider(name = "trackerClientState")
  Object[][] getTrackerClientStates()
  {
    return new Object[][]
        {
            {0, TrackerClientState.HealthState.NEUTRAL, QuarantineManager.INITIAL_RECOVERY_HEALTH_SCORE},
            {15, TrackerClientState.HealthState.UNHEALTHY, QuarantineManager.INITIAL_RECOVERY_HEALTH_SCORE},
            {15, TrackerClientState.HealthState.UNHEALTHY, 0.6},
            {15, TrackerClientState.HealthState.HEALTHY, QuarantineManager.INITIAL_RECOVERY_HEALTH_SCORE}
        };
  }

  @Test(dataProvider = "trueFalse")
  public void testEnrollNewClientInRecoveryMap(boolean fastRecoveryEnabled)
  {
    setup(0.5, true, fastRecoveryEnabled);
    _quarantineManager.tryEnableQuarantine();

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(2);
    PartitionState oldState = new PartitionStateTestDataBuilder()
        .setTrackerClientStateMap(Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList())
        .build();
    PartitionState newState = new PartitionStateTestDataBuilder()
        .setTrackerClientStateMap(trackerClients,
            Arrays.asList(QuarantineManager.INITIAL_RECOVERY_HEALTH_SCORE, QuarantineManager.INITIAL_RECOVERY_HEALTH_SCORE),
            Arrays.asList(TrackerClientState.HealthState.UNHEALTHY, TrackerClientState.HealthState.UNHEALTHY),
            Arrays.asList(20, 20))
        .build();

    _quarantineManager.updateQuarantineState(newState, oldState, DEFAULT_AVG_CLUSTER_LATENCY);

    if (fastRecoveryEnabled)
    {
      assertEquals(newState.getRecoveryTrackerClients().size(), 2);
    }
    else
    {
      assertTrue(newState.getRecoveryTrackerClients().isEmpty());
    }
  }

  @Test
  public void testEnrollOneQuarantineOneRecovery()
  {
    LoadBalancerQuarantine quarantine = Mockito.mock(LoadBalancerQuarantine.class);
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3);
    Map<TrackerClient, LoadBalancerQuarantine> existingQuarantineMap = new HashMap<>();
    existingQuarantineMap.put(trackerClients.get(1), quarantine);
    Mockito.when(quarantine.checkUpdateQuarantineState()).thenReturn(true);

    setup(0.5, true, true);
    _quarantineManager.tryEnableQuarantine();

    PartitionState state = new PartitionStateTestDataBuilder()
        .setTrackerClientStateMap(trackerClients,
            Arrays.asList(StateUpdater.MIN_HEALTH_SCORE, StateUpdater.MIN_HEALTH_SCORE, QuarantineManager.INITIAL_RECOVERY_HEALTH_SCORE),
            Arrays.asList(TrackerClientState.HealthState.UNHEALTHY, TrackerClientState.HealthState.NEUTRAL, TrackerClientState.HealthState.UNHEALTHY),
            Arrays.asList(20, 20, 20))
        .build();

    _quarantineManager.updateQuarantineState(state, state, DEFAULT_AVG_CLUSTER_LATENCY);

    assertEquals(state.getRecoveryTrackerClients().size(), 1);
    assertTrue(state.getRecoveryTrackerClients().contains(trackerClients.get(1)));
    assertEquals(state.getQuarantineMap().size(), 1);
    assertTrue(state.getQuarantineMap().containsKey(trackerClients.get(0)));
  }

  @DataProvider(name = "trueFalse")
  Object[][] enable()
  {
    return new Object[][]
        {
            {true},
            {false}
        };
  }
}

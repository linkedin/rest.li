package com.linkedin.d2.balancer.strategies.relative;

import com.linkedin.d2.D2QuarantineProperties;
import com.linkedin.d2.HttpMethod;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.degrader.DistributionNonDiscreteRingFactory;
import com.linkedin.d2.balancer.strategies.degrader.LoadBalancerQuarantine;
import com.linkedin.d2.balancer.strategies.degrader.RingFactory;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import com.linkedin.util.clock.Clock;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.balancer.strategies.relative.TrackerClientState.HealthState.*;


public class QuarantineManagerTest {
  private static final String SERVICE_NAME = "dummyService";
  private static final String SERVICE_PATH = "dummyServicePath";
  private static final HealthCheckOperations HEALTH_CHECK_OPERATIONS = new HealthCheckOperations();
  private static final long DEFAULT_AVG_CLUSTER_LATENCY = 100;
  private static final RingFactory<URI> RING_FACTORY = new DistributionNonDiscreteRingFactory<>();

  @Test
  public void testQuarantineNotEnabledInConfig() throws URISyntaxException {
    Mocks mocks = new Mocks(RelativeLoadBalancerStrategyFactory.DEFAULT_QUARANTINE_MAX_PERCENT, false, false);

    PartitionState state = new PartitionStateDataBuilder(RING_FACTORY)
        .setTrackerClientStateMap(TrackerClientMockHelper.mockTrackerClients(2), Arrays.asList(0.0, 0.6),
            Arrays.asList(TrackerClientState.HealthState.UNHEALTHY, TrackerClientState.HealthState.UNHEALTHY),
            Arrays.asList(20, 20), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE,
            RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT).build();

    mocks._quarantineManager.updateQuarantineState(state, state, DEFAULT_AVG_CLUSTER_LATENCY);

    // Verify quarantine is not enabled
    Mockito.verifyZeroInteractions(mocks._executorService);
    Assert.assertTrue(state.getQuarantineMap().isEmpty());
  }

  @Test
  public void testQuarantineOneHost() throws URISyntaxException {
    Mocks mocks = new Mocks(0.5, false, false);
    mocks._quarantineManager.tryEnableQuarantine();

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(2);
    PartitionState state = new PartitionStateDataBuilder(RING_FACTORY)
        .setTrackerClientStateMap(trackerClients, Arrays.asList(0.0, 0.6),
            Arrays.asList(TrackerClientState.HealthState.UNHEALTHY, TrackerClientState.HealthState.UNHEALTHY),
            Arrays.asList(20, 20), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE,
            RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT).build();

    mocks._quarantineManager.updateQuarantineState(state, state, DEFAULT_AVG_CLUSTER_LATENCY);

    // Verify quarantine is enabled and one client is quarantined
    Assert.assertEquals(state.getQuarantineMap().size(), 1);
    Assert.assertTrue(state.getQuarantineMap().containsKey(trackerClients.get(0)));
    Assert.assertTrue(state.getRecoveryTrackerClients().isEmpty());
  }

  @Test
  public void testQuarantinedMaxPercentage() throws URISyntaxException {
    Mocks mocks = new Mocks(0.5, false, false);
    mocks._quarantineManager.tryEnableQuarantine();

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(4);
    PartitionState state = new PartitionStateDataBuilder(RING_FACTORY)
        .setTrackerClientStateMap(trackerClients, Arrays.asList(0.0, 0.0, 0.0, 0.6),
            Arrays.asList(TrackerClientState.HealthState.UNHEALTHY, TrackerClientState.HealthState.UNHEALTHY,
                TrackerClientState.HealthState.UNHEALTHY, TrackerClientState.HealthState.UNHEALTHY),
            Arrays.asList(20, 20, 20, 20), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE,
            RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT).build();

    mocks._quarantineManager.updateQuarantineState(state, state, DEFAULT_AVG_CLUSTER_LATENCY);

    // Verify quarantine is enabled and 2 clients are quarantined.
    // In theory, there are 3 candidates, but because of the max percentage is 50%, we only quarantine 2, the other one is put into recovery
    Assert.assertEquals(state.getQuarantineMap().size(), 2);
    Assert.assertEquals(state.getRecoveryTrackerClients().size(), 1);
  }

  @Test(dataProvider = "quarantineCheckResult")
  public void testQuarantineCheck(boolean quarantineCheckResult) throws URISyntaxException {
    LoadBalancerQuarantine quarantine = Mockito.mock(LoadBalancerQuarantine.class);
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3);
    Map<TrackerClient, LoadBalancerQuarantine> existingQuarantineMap = new HashMap<>();
    existingQuarantineMap.put(trackerClients.get(0), quarantine);
    Mockito.when(quarantine.checkUpdateQuarantineState()).thenReturn(quarantineCheckResult);

    PartitionState state = new PartitionStateDataBuilder(RING_FACTORY)
        .setTrackerClientStateMap(trackerClients, Arrays.asList(0.0, 0.6, 0.6),
            Arrays.asList(NEUTRAL, TrackerClientState.HealthState.UNHEALTHY,
                TrackerClientState.HealthState.UNHEALTHY),
            Arrays.asList(20, 20, 20), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE,
            RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT)
        .setQuarantineMap(existingQuarantineMap)
        .build();

    Mocks mocks = new Mocks(0.5, false, false);
    mocks._quarantineManager.tryEnableQuarantine();

    mocks._quarantineManager.updateQuarantineState(state, state, DEFAULT_AVG_CLUSTER_LATENCY);

    if (quarantineCheckResult)
    {
      // If quarantine check passed, verify the tracker client is put into recovery map, and the initial recovery rate is 0.01
      Assert.assertTrue(state.getQuarantineMap().isEmpty());
      Assert.assertEquals(state.getRecoveryTrackerClients().size(), 1);
      Assert.assertEquals(state.getTrackerClientStateMap().get(trackerClients.get(0)).getHealthScore(), 0.01);
    } else
    {
      // Otherwise, the client stays in the quarantine map
      Assert.assertEquals(state.getQuarantineMap().size(), 1);
      Assert.assertTrue(state.getRecoveryTrackerClients().isEmpty());
      Assert.assertTrue(state.getQuarantineMap().containsKey(trackerClients.get(0)));
    }
  }

  @DataProvider(name = "quarantineCheckResult")
  Object[][] getQuarantineCheckResult()
  {
    return new Object[][]
        {
            {true},
            {false}
        };
  }

  @Test(dataProvider = "trackerClientState")
  public void testFastRecoveryInRecoveryMap(int callCount, TrackerClientState.HealthState healthState, double healthScore) throws URISyntaxException {
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3);
    Set<TrackerClient> recoverySet = new HashSet<>();
    recoverySet.add(trackerClients.get(0));

    PartitionState state = new PartitionStateDataBuilder(RING_FACTORY)
        .setTrackerClientStateMap(trackerClients, Arrays.asList(healthScore, 0.6, 0.6),
            Arrays.asList(healthState, TrackerClientState.HealthState.UNHEALTHY,
                TrackerClientState.HealthState.UNHEALTHY),
            Arrays.asList(callCount, 20, 20), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE,
            RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT)
        .setRecoveryClients(recoverySet)
        .build();

    Mocks mocks = new Mocks(0.5, false, true);
    mocks._quarantineManager.tryEnableQuarantine();

    mocks._quarantineManager.updateQuarantineState(state, state, DEFAULT_AVG_CLUSTER_LATENCY);

    if (callCount <= RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT)
    {
      // Verify the health score doubled
      Assert.assertEquals(state.getTrackerClientStateMap().get(trackerClients.get(0)).getHealthScore(), healthScore * 2);
      Assert.assertTrue(state.getRecoveryTrackerClients().contains(trackerClients.get(0)));
    } else if (healthState == UNHEALTHY && healthScore <= QuarantineManager.FAST_RECOVERY_HEALTH_SCORE_THRESHOLD)
    {
      // Verify the health score keeps the same, and the client is still in recovery map
      Assert.assertEquals(state.getTrackerClientStateMap().get(trackerClients.get(0)).getHealthScore(), healthScore);
      Assert.assertTrue(state.getRecoveryTrackerClients().contains(trackerClients.get(0)));
    } else
    {
      // Verify the client comes out of recovery map
      Assert.assertTrue(state.getRecoveryTrackerClients().isEmpty());
    }
  }

  @DataProvider(name = "trackerClientState")
  Object[][] getTrackerClientStates()
  {
    return new Object[][]
        {
            {0, NEUTRAL, 0.01},
            {15, UNHEALTHY, 0.01},
            {15, UNHEALTHY, 0.6},
            {15, HEALTHY, 0.01}
        };
  }

  @Test(dataProvider = "enableFastRecovery")
  public void testEnrollNewClientInRecoveryMap(boolean fastRecoveryEnabled) throws URISyntaxException {
    Mocks mocks = new Mocks(0.5, true, fastRecoveryEnabled);
    mocks._quarantineManager.tryEnableQuarantine();

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(2);
    PartitionState oldState = new PartitionStateDataBuilder(RING_FACTORY)
        .setTrackerClientStateMap(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE,
            RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT).build();
    PartitionState newState = new PartitionStateDataBuilder(RING_FACTORY)
        .setTrackerClientStateMap(trackerClients, Arrays.asList(0.01, 0.01),
            Arrays.asList(TrackerClientState.HealthState.UNHEALTHY, TrackerClientState.HealthState.UNHEALTHY),
            Arrays.asList(20, 20), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE,
            RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT).build();

    mocks._quarantineManager.updateQuarantineState(newState, oldState, DEFAULT_AVG_CLUSTER_LATENCY);

    if (fastRecoveryEnabled)
    {
      Assert.assertEquals(newState.getRecoveryTrackerClients().size(), 2);
    } else
    {
      Assert.assertTrue(newState.getRecoveryTrackerClients().isEmpty());
    }
  }

  @DataProvider(name = "enableFastRecovery")
  Object[][] getFastRecoveryEnabled()
  {
    return new Object[][]
        {
            {true},
            {false}
        };
  }

  @Test
  public void testEnrollOneQuarantineOneRecovery() throws URISyntaxException {
    LoadBalancerQuarantine quarantine = Mockito.mock(LoadBalancerQuarantine.class);
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3);
    Map<TrackerClient, LoadBalancerQuarantine> existingQuarantineMap = new HashMap<>();
    existingQuarantineMap.put(trackerClients.get(1), quarantine);
    Mockito.when(quarantine.checkUpdateQuarantineState()).thenReturn(true);

    Mocks mocks = new Mocks(0.5, true, true);
    QuarantineManager quarantineManager = mocks._quarantineManager;
    mocks._quarantineManager.tryEnableQuarantine();

    PartitionState state = new PartitionStateDataBuilder(RING_FACTORY)
        .setTrackerClientStateMap(trackerClients, Arrays.asList(0.0, 0.0, 0.01),
            Arrays.asList(UNHEALTHY, NEUTRAL, UNHEALTHY),
            Arrays.asList(20, 20, 20), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE,
            RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT).build();

    quarantineManager.updateQuarantineState(state, state, DEFAULT_AVG_CLUSTER_LATENCY);

    Assert.assertEquals(state.getRecoveryTrackerClients().size(), 1);
    Assert.assertTrue(state.getRecoveryTrackerClients().contains(trackerClients.get(1)));
    Assert.assertEquals(state.getQuarantineMap().size(), 1);
    Assert.assertTrue(state.getQuarantineMap().containsKey(trackerClients.get(0)));
  }

  private static class Mocks
  {
    private final ScheduledExecutorService _executorService = Mockito.mock(ScheduledExecutorService.class);
    private final Clock _clock = Mockito.mock(Clock.class);

    private final QuarantineManager _quarantineManager;
    private Mocks(double quarantineMaxPercent, boolean slowStartEnabled, boolean fastRecoveryEnabled)
    {
      double slowStartThreshold = slowStartEnabled ? 0.5 : 0;
      D2QuarantineProperties d2QuarantineProperties = new D2QuarantineProperties().setQuarantineMaxPercent(quarantineMaxPercent)
          .setHealthCheckMethod(HttpMethod.OPTIONS);
      _quarantineManager = new QuarantineManager(SERVICE_NAME, SERVICE_PATH, HEALTH_CHECK_OPERATIONS, d2QuarantineProperties,
          slowStartThreshold, fastRecoveryEnabled, _executorService, _clock,
          RelativeLoadBalancerStrategyFactory.DEFAULT_UPDATE_INTERVAL_MS,
          RelativeLoadBalancerStrategyFactory.DEFAULT_RELATIVE_LATENCY_LOW_THRESHOLD_FACTOR);
    }
  }
}

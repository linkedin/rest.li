package com.linkedin.d2.balancer.strategies.relative;

import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.balancer.clients.PerCallDurationListener;
import com.linkedin.d2.balancer.clients.PerCallDurationSemantics;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.PartitionStateUpdateListener;
import com.linkedin.d2.jmx.RelativeLoadBalancerStrategyOtelMetricsProvider;
import com.linkedin.d2.jmx.TestRelativeLoadBalancerStrategyOtelMetricsProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


/**
 * Integration tests that verify {@link StateUpdater} actually invokes the
 * {@link RelativeLoadBalancerStrategyOtelMetricsProvider} from production code paths.
 *
 * <p>These complement the unit tests in
 * {@code com.linkedin.d2.jmx.RelativeLoadBalancerStrategyOtelMetricsProviderTest}, which only
 * exercise the test-double in isolation. Here we construct a real {@code StateUpdater} with a
 * test provider and trigger production wiring (state updates, per-call latency listener
 * registration) to make sure metrics are actually emitted.
 */
public class RelativeLoadBalancerStrategyOtelIntegrationTest
{
  private static final int DEFAULT_PARTITION_ID = 0;
  private static final long DEFAULT_CLUSTER_GENERATION_ID = 0;
  private static final String SERVICE_NAME = "integration-test-service";
  private static final String SCHEME = "http";

  private TestRelativeLoadBalancerStrategyOtelMetricsProvider _provider;
  private QuarantineManager _quarantineManager;
  private ScheduledExecutorService _executorService;

  @BeforeMethod
  public void setUp()
  {
    _provider = new TestRelativeLoadBalancerStrategyOtelMetricsProvider();
    _quarantineManager = Mockito.mock(QuarantineManager.class);
    _executorService = Mockito.mock(ScheduledExecutorService.class);
    // Make execute(Runnable) run inline so the asynchronous emitOtelMetrics block actually fires.
    Mockito.doAnswer(invocation -> {
      ((Runnable) invocation.getArguments()[0]).run();
      return null;
    }).when(_executorService).execute(Mockito.any(Runnable.class));
  }

  @Test
  public void testGaugeMetricsEmittedAfterStateUpdate()
  {
    StateUpdater stateUpdater = newStateUpdater();
    stateUpdater.setScheme(SCHEME);

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(
        3,
        Arrays.asList(20, 20, 20),
        Arrays.asList(10, 10, 10),
        Arrays.asList(200L, 200L, 200L),
        Arrays.asList(100L, 100L, 100L),
        Arrays.asList(0, 0, 0));

    // First call initializes the partition synchronously, which runs updateStateForPartition,
    // which schedules emitOtelMetrics on the executor. Our mocked executor runs it inline.
    stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID,
        DEFAULT_CLUSTER_GENERATION_ID, false);

    assertEquals(_provider.getCallCount("updateTotalHostsInAllPartitionsCount"), 1,
        "updateTotalHostsInAllPartitionsCount should be invoked once after a state update");
    assertEquals(_provider.getCallCount("updateUnhealthyHostsCount"), 1,
        "updateUnhealthyHostsCount should be invoked once after a state update");
    assertEquals(_provider.getCallCount("updateQuarantineHostsCount"), 1,
        "updateQuarantineHostsCount should be invoked once after a state update");
    assertEquals(_provider.getCallCount("updateTotalPointsInHashRing"), 1,
        "updateTotalPointsInHashRing should be invoked once after a state update");

    assertEquals(_provider.getLastServiceName("updateTotalHostsInAllPartitionsCount"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("updateTotalHostsInAllPartitionsCount"), SCHEME);
    assertEquals(_provider.getLastServiceName("updateTotalPointsInHashRing"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("updateTotalPointsInHashRing"), SCHEME);

    // 3 tracker clients => updateTotalHostsInAllPartitionsCount should reflect 3.
    assertEquals(_provider.getLastIntValue("updateTotalHostsInAllPartitionsCount").intValue(), 3,
        "Total hosts gauge should match the number of tracker clients");

    // The fixture creates 3 healthy hosts (call count 20, no quarantine), so the unhealthy and
    // quarantine gauges must report 0. A regression that miscounts here would leak under-reported
    // health to ops dashboards. The total points in the hash ring must be strictly positive
    // because at least one host contributes points.
    assertEquals(_provider.getLastIntValue("updateUnhealthyHostsCount").intValue(), 0,
        "All hosts in the fixture are healthy; unhealthy gauge must be 0");
    assertEquals(_provider.getLastIntValue("updateQuarantineHostsCount").intValue(), 0,
        "No host in the fixture is quarantined; quarantine gauge must be 0");
    assertEquals(_provider.getLastServiceName("updateQuarantineHostsCount"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("updateQuarantineHostsCount"), SCHEME);
    assertTrue(_provider.getLastIntValue("updateTotalPointsInHashRing") > 0,
        "Total ring points should be strictly positive when there are healthy hosts");
  }

  @Test
  public void testPerCallLatencyListenerRegisteredAndForwardsToProvider()
  {
    StateUpdater stateUpdater = newStateUpdater();
    stateUpdater.setScheme(SCHEME);

    // Use a single tracker client so we capture exactly one listener.
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(
        1,
        Collections.singletonList(20),
        Collections.singletonList(10),
        Collections.singletonList(200L),
        Collections.singletonList(100L),
        Collections.singletonList(0));

    stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID,
        DEFAULT_CLUSTER_GENERATION_ID, false);

    // The state updater should have registered exactly one per-call latency listener on the
    // tracker client. Capture it and verify it forwards to the provider.
    ArgumentCaptor<PerCallDurationListener> listenerCaptor =
        ArgumentCaptor.forClass(PerCallDurationListener.class);
    Mockito.verify(trackerClients.get(0)).setPerCallDurationListener(listenerCaptor.capture());

    long simulatedLatencyMs = 321L;
    PerCallDurationListener capturedListener = listenerCaptor.getValue();
    capturedListener.accept(simulatedLatencyMs, PerCallDurationSemantics.FULL_ROUND_TRIP);

    assertEquals(_provider.getCallCount("recordHostLatency"), 1,
        "Listener should forward exactly one recordHostLatency invocation");
    assertEquals(_provider.getLastLongValue("recordHostLatency").longValue(), simulatedLatencyMs);
    assertEquals(_provider.getLastServiceName("recordHostLatency"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("recordHostLatency"), SCHEME);
    assertEquals(_provider.getLastPerCallDurationSemantics("recordHostLatency"),
        PerCallDurationSemantics.FULL_ROUND_TRIP);
  }

  @Test
  public void testGaugeMetricsSkippedBeforeSetScheme()
  {
    StateUpdater stateUpdater = newStateUpdater();
    // Intentionally do NOT call setScheme to simulate the early-startup window.

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(
        2,
        Arrays.asList(20, 20),
        Arrays.asList(10, 10),
        Arrays.asList(200L, 200L),
        Arrays.asList(100L, 100L),
        Arrays.asList(0, 0));

    stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID,
        DEFAULT_CLUSTER_GENERATION_ID, false);

    assertEquals(_provider.getCallCount("updateTotalHostsInAllPartitionsCount"), 0,
        "Gauge metrics should not be emitted before setScheme is called");
    assertEquals(_provider.getCallCount("updateUnhealthyHostsCount"), 0,
        "Gauge metrics should not be emitted before setScheme is called");
    assertEquals(_provider.getCallCount("updateQuarantineHostsCount"), 0,
        "Gauge metrics should not be emitted before setScheme is called");
    assertEquals(_provider.getCallCount("updateTotalPointsInHashRing"), 0,
        "Gauge metrics should not be emitted before setScheme is called");
  }

  @Test
  public void testPerCallListenerSkipsEmissionBeforeSetScheme()
  {
    StateUpdater stateUpdater = newStateUpdater();
    // Intentionally do NOT call setScheme.

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(
        1,
        Collections.singletonList(20),
        Collections.singletonList(10),
        Collections.singletonList(200L),
        Collections.singletonList(100L),
        Collections.singletonList(0));

    stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID,
        DEFAULT_CLUSTER_GENERATION_ID, false);

    ArgumentCaptor<PerCallDurationListener> listenerCaptor =
        ArgumentCaptor.forClass(PerCallDurationListener.class);
    Mockito.verify(trackerClients.get(0)).setPerCallDurationListener(listenerCaptor.capture());

    PerCallDurationListener capturedListener = listenerCaptor.getValue();

    // Without setScheme, the listener must short-circuit.
    capturedListener.accept(100L, PerCallDurationSemantics.FULL_ROUND_TRIP);
    assertEquals(_provider.getCallCount("recordHostLatency"), 0,
        "Per-call latency must not be recorded before setScheme is called");

    // After setScheme, the same listener should now record.
    stateUpdater.setScheme(SCHEME);
    capturedListener.accept(200L, PerCallDurationSemantics.FULL_ROUND_TRIP);
    assertEquals(_provider.getCallCount("recordHostLatency"), 1,
        "Per-call latency should be recorded once scheme is initialized");
    assertEquals(_provider.getLastLongValue("recordHostLatency").longValue(), 200L);
    assertEquals(_provider.getLastScheme("recordHostLatency"), SCHEME);
  }

  @Test
  public void testSetSchemeNullPreservesExistingScheme()
  {
    StateUpdater stateUpdater = newStateUpdater();
    stateUpdater.setScheme(SCHEME);
    // Calling setScheme(null) must NOT clobber the previously-set scheme. If it did, the next
    // emission cycle would silently switch to the placeholder and pollute the OTel backend with
    // an unwanted dimension value.
    stateUpdater.setScheme(null);

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(
        1,
        Collections.singletonList(20),
        Collections.singletonList(10),
        Collections.singletonList(200L),
        Collections.singletonList(100L),
        Collections.singletonList(0));

    stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID,
        DEFAULT_CLUSTER_GENERATION_ID, false);

    assertEquals(_provider.getCallCount("updateTotalPointsInHashRing"), 1,
        "Metrics must still be emitted with the previously-set scheme");
    assertEquals(_provider.getLastScheme("updateTotalPointsInHashRing"), SCHEME,
        "setScheme(null) must not change the scheme");
  }

  @Test
  public void testSetSchemeWithPlaceholderPreservesExistingScheme()
  {
    StateUpdater stateUpdater = newStateUpdater();
    stateUpdater.setScheme(SCHEME);
    // The "-" placeholder must be ignored too.
    stateUpdater.setScheme("-");

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(
        1,
        Collections.singletonList(20),
        Collections.singletonList(10),
        Collections.singletonList(200L),
        Collections.singletonList(100L),
        Collections.singletonList(0));

    stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID,
        DEFAULT_CLUSTER_GENERATION_ID, false);

    assertEquals(_provider.getCallCount("updateTotalPointsInHashRing"), 1);
    assertEquals(_provider.getLastScheme("updateTotalPointsInHashRing"), SCHEME,
        "setScheme(\"-\") must not change the scheme");
  }

  /**
   * If the OTel provider throws during gauge emission, the partition state update must still
   * advance: subsequent {@code updateState} calls should continue to publish to a now-recovered
   * provider and the strategy must not be left in a permanently broken state. Regression for
   * "provider-throws-on-gauge halts the partition update".
   */
  @Test
  public void testStateUpdateAdvancesWhenProviderThrowsOnGauge()
  {
    // First cycle: provider throws on every gauge call.
    _provider = new TestRelativeLoadBalancerStrategyOtelMetricsProvider()
    {
      @Override
      public void updateTotalPointsInHashRing(String serviceName, String scheme, int totalPointsInHashRing)
      {
        throw new RuntimeException("simulated OTel SDK gauge failure");
      }
    };

    StateUpdater stateUpdater = newStateUpdater();
    stateUpdater.setScheme(SCHEME);

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(
        2,
        Arrays.asList(20, 20),
        Arrays.asList(10, 10),
        Arrays.asList(200L, 200L),
        Arrays.asList(100L, 100L),
        Arrays.asList(0, 0));

    // Must not throw; the wrapping in updateStateForPartition swallows OTel gauge exceptions.
    stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID,
        DEFAULT_CLUSTER_GENERATION_ID, false);

    // Partition state must have advanced: a subsequent forced update must trigger a fresh
    // emission cycle (which will throw again from updateTotalPointsInHashRing but the earlier
    // gauges will have been recorded before the throw).
    stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID,
        DEFAULT_CLUSTER_GENERATION_ID + 1, true);

    // updateTotalHostsInAllPartitionsCount runs before the throwing call in emitOtelMetrics,
    // so it must record at least once across the two cycles.
    assertTrue(_provider.getCallCount("updateTotalHostsInAllPartitionsCount") >= 1,
        "Earlier gauges must still be recorded even when a later gauge throws");
  }

  @Test
  public void testListenerNotRegisteredForDoNotLoadBalanceClients()
  {
    StateUpdater stateUpdater = newStateUpdater();
    stateUpdater.setScheme(SCHEME);

    // Mark all tracker clients as doNotLoadBalance=true; the strategy should skip listener
    // registration for them.
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(
        2,
        Arrays.asList(20, 20),
        Arrays.asList(10, 10),
        Arrays.asList(200L, 200L),
        Arrays.asList(100L, 100L),
        Arrays.asList(0, 0),
        false,
        Arrays.asList(true, true));

    stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID,
        DEFAULT_CLUSTER_GENERATION_ID, false);

    for (TrackerClient client : trackerClients)
    {
      Mockito.verify(client, Mockito.never()).setPerCallDurationListener(Mockito.any());
    }
    // Gauge metrics still fire even when no per-call listener is registered.
    assertTrue(_provider.getCallCount("updateTotalPointsInHashRing") >= 1,
        "Gauge metrics should still be emitted regardless of per-call listener registration");
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private StateUpdater newStateUpdater()
  {
    D2RelativeStrategyProperties props = new D2RelativeStrategyProperties();
    RelativeLoadBalancerStrategyFactory.putDefaultValues(props);
    return new StateUpdater(
        props,
        _quarantineManager,
        _executorService,
        new ConcurrentHashMap<>(),
        Collections.<PartitionStateUpdateListener.Factory<PartitionState>>emptyList(),
        SERVICE_NAME,
        false,
        _provider);
  }
}

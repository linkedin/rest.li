/*
   Copyright (c) 2026 LinkedIn Corp.

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
import com.linkedin.d2.balancer.clients.PerCallDurationListener;
import com.linkedin.d2.balancer.clients.PerCallDurationSemantics;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.PartitionStateUpdateListener;
import com.linkedin.d2.jmx.HostStatus;
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


/** Integration tests for {@link StateUpdater} OTel metrics wiring. */
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

    stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID,
        DEFAULT_CLUSTER_GENERATION_ID, false);

    assertEquals(_provider.getCallCount("updateTotalHostsInAllPartitionsCount"), 1,
        "updateTotalHostsInAllPartitionsCount should be invoked once after a state update");
    assertEquals(_provider.getCallCountForHostStatus("updateDegradedHostsCount", HostStatus.UNHEALTHY), 1,
        "updateDegradedHostsCount(UNHEALTHY) should be invoked once after a state update");
    assertEquals(_provider.getCallCountForHostStatus("updateDegradedHostsCount", HostStatus.QUARANTINED), 1,
        "updateDegradedHostsCount(QUARANTINED) should be invoked once after a state update");
    assertEquals(_provider.getCallCount("updateTotalPointsInHashRing"), 1,
        "updateTotalPointsInHashRing should be invoked once after a state update");

    assertEquals(_provider.getLastServiceName("updateTotalHostsInAllPartitionsCount"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("updateTotalHostsInAllPartitionsCount"), SCHEME);
    assertEquals(_provider.getLastServiceName("updateTotalPointsInHashRing"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("updateTotalPointsInHashRing"), SCHEME);

    assertEquals(_provider.getLastIntValue("updateTotalHostsInAllPartitionsCount").intValue(), 3,
        "Total hosts gauge should match the number of tracker clients");

    assertEquals(_provider.getLastIntValueForHostStatus("updateDegradedHostsCount", HostStatus.UNHEALTHY).intValue(), 0,
        "All hosts in the fixture are healthy; unhealthy gauge must be 0");
    assertEquals(_provider.getLastIntValueForHostStatus("updateDegradedHostsCount", HostStatus.QUARANTINED).intValue(), 0,
        "No host in the fixture is quarantined; quarantine gauge must be 0");
    assertEquals(_provider.getLastServiceName("updateDegradedHostsCount"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("updateDegradedHostsCount"), SCHEME);
    assertTrue(_provider.getLastIntValue("updateTotalPointsInHashRing") > 0,
        "Total ring points should be strictly positive when there are healthy hosts");
  }

  @Test
  public void testFourTupleGaugeSnapshotIsInternallyConsistent()
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

    stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID,
        DEFAULT_CLUSTER_GENERATION_ID, false);

    assertEquals(_provider.getCallCount("updateTotalHostsInAllPartitionsCount"), 1,
        "snapshot: total-hosts gauge must fire exactly once per cycle");
    assertEquals(_provider.getCallCountForHostStatus("updateDegradedHostsCount", HostStatus.UNHEALTHY), 1,
        "snapshot: degraded(UNHEALTHY) gauge must fire exactly once per cycle");
    assertEquals(_provider.getCallCountForHostStatus("updateDegradedHostsCount", HostStatus.QUARANTINED), 1,
        "snapshot: degraded(QUARANTINED) gauge must fire exactly once per cycle");
    assertEquals(_provider.getCallCount("updateTotalPointsInHashRing"), 1,
        "snapshot: ring-points gauge must fire exactly once per cycle");

    int total = _provider.getLastIntValue("updateTotalHostsInAllPartitionsCount");
    int unhealthy =
        _provider.getLastIntValueForHostStatus("updateDegradedHostsCount", HostStatus.UNHEALTHY);
    int quarantined =
        _provider.getLastIntValueForHostStatus("updateDegradedHostsCount", HostStatus.QUARANTINED);
    int ringPoints = _provider.getLastIntValue("updateTotalPointsInHashRing");

    assertEquals(_provider.getLastServiceName("updateTotalHostsInAllPartitionsCount"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("updateTotalHostsInAllPartitionsCount"), SCHEME);
    assertEquals(_provider.getLastServiceName("updateDegradedHostsCount"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("updateDegradedHostsCount"), SCHEME);
    assertEquals(_provider.getLastServiceName("updateTotalPointsInHashRing"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("updateTotalPointsInHashRing"), SCHEME);

    assertEquals(total, 3, "total-hosts must match the tracker-client fixture size");
    assertTrue(unhealthy >= 0, "unhealthy count must be non-negative");
    assertTrue(quarantined >= 0, "quarantined count must be non-negative");
    assertTrue(unhealthy + quarantined <= total,
        "degraded counts cannot exceed total hosts: unhealthy=" + unhealthy
            + ", quarantined=" + quarantined + ", total=" + total);
    assertTrue(ringPoints > 0,
        "ring points must be strictly positive when the cluster has any healthy hosts");
  }

  @Test
  public void testPerCallLatencyListenerRegisteredAndForwardsToProvider()
  {
    StateUpdater stateUpdater = newStateUpdater();
    stateUpdater.setScheme(SCHEME);

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
    assertEquals(_provider.getCallCount("updateDegradedHostsCount"), 0,
        "Gauge metrics should not be emitted before setScheme is called");
    assertEquals(_provider.getCallCount("updateTotalPointsInHashRing"), 0,
        "Gauge metrics should not be emitted before setScheme is called");
  }

  @Test
  public void testPerCallListenerSkipsEmissionBeforeSetScheme()
  {
    StateUpdater stateUpdater = newStateUpdater();
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

    capturedListener.accept(100L, PerCallDurationSemantics.FULL_ROUND_TRIP);
    assertEquals(_provider.getCallCount("recordHostLatency"), 0,
        "Per-call latency must not be recorded before setScheme is called");

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

  @Test
  public void testStateUpdateAdvancesWhenProviderThrowsOnGauge()
  {
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

    stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID,
        DEFAULT_CLUSTER_GENERATION_ID, false);

    stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID,
        DEFAULT_CLUSTER_GENERATION_ID + 1, true);

    assertTrue(_provider.getCallCount("updateTotalHostsInAllPartitionsCount") >= 1,
        "Earlier gauges must still be recorded even when a later gauge throws");
  }

  @Test
  public void testListenerNotRegisteredForDoNotLoadBalanceClients()
  {
    StateUpdater stateUpdater = newStateUpdater();
    stateUpdater.setScheme(SCHEME);

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
    assertTrue(_provider.getCallCount("updateTotalPointsInHashRing") >= 1,
        "Gauge metrics should still be emitted regardless of per-call listener registration");
  }

  @Test
  public void testNullProviderIsCoalescedToNoOpAndDoesNotNpeOnEmission()
  {
    D2RelativeStrategyProperties props = new D2RelativeStrategyProperties();
    RelativeLoadBalancerStrategyFactory.putDefaultValues(props);
    StateUpdater stateUpdater = new StateUpdater(
        props,
        _quarantineManager,
        _executorService,
        new ConcurrentHashMap<>(),
        Collections.<PartitionStateUpdateListener.Factory<PartitionState>>emptyList(),
        SERVICE_NAME,
        false,
        null);
    stateUpdater.setScheme(SCHEME);

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(
        2,
        Arrays.asList(20, 20),
        Arrays.asList(10, 10),
        Arrays.asList(200L, 200L),
        Arrays.asList(100L, 100L),
        Arrays.asList(0, 0));

    stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID,
        DEFAULT_CLUSTER_GENERATION_ID, false);
  }

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

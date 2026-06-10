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
package com.linkedin.d2.balancer.strategies.degrader;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.clients.DegraderTrackerClient;
import com.linkedin.d2.balancer.clients.PerCallDurationListener;
import com.linkedin.d2.balancer.clients.PerCallDurationSemantics;
import com.linkedin.d2.balancer.clients.DegraderTrackerClientImpl;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.jmx.DegraderLoadBalancerStrategyV3OtelMetricsProvider;
import com.linkedin.d2.jmx.TestDegraderLoadBalancerStrategyV3OtelMetricsProvider;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.util.clock.SystemClock;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


/** Integration tests for {@link DegraderLoadBalancerStrategyV3} OTel metrics wiring. */
public class DegraderLoadBalancerStrategyV3OtelIntegrationTest
{
  private static final String SERVICE_NAME = "integration-test-service";
  private static final String SCHEME = "http";
  private static final long CLUSTER_GENERATION_ID = 1L;
  private static final int PARTITION_ID = DefaultPartitionAccessor.DEFAULT_PARTITION_ID;
  private static final List<PartitionDegraderLoadBalancerStateListener.Factory> NO_LISTENERS =
      Collections.emptyList();

  private TestDegraderLoadBalancerStrategyV3OtelMetricsProvider _provider;

  @BeforeMethod
  public void setUp()
  {
    _provider = new TestDegraderLoadBalancerStrategyV3OtelMetricsProvider();
  }

  @Test
  public void testGaugeMetricsEmittedOnStateUpdate()
  {
    DegraderLoadBalancerStrategyV3 strategy = newStrategy();
    strategy.setScheme(SCHEME);

    Map<URI, TrackerClient> trackerClients = newTrackerClientMap(3);

    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, trackerClients);

    assertEquals(_provider.getCallCount("updateOverrideClusterDropRate"), 1,
        "updateOverrideClusterDropRate should be invoked exactly once after a state update");
    assertEquals(_provider.getCallCount("updateTotalPointsInHashRing"), 1,
        "updateTotalPointsInHashRing should be invoked exactly once after a state update");
    assertEquals(_provider.getLastServiceName("updateOverrideClusterDropRate"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("updateOverrideClusterDropRate"), SCHEME);
    assertEquals(_provider.getLastServiceName("updateTotalPointsInHashRing"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("updateTotalPointsInHashRing"), SCHEME);

    assertEquals(_provider.getLastIntValue("updateTotalPointsInHashRing").intValue(), 300,
        "Total points in hash ring should reflect 3 healthy hosts at 100 points each");
  }

  @Test
  public void testPerCallLatencyListenerRegisteredAndForwardsToProvider()
  {
    DegraderLoadBalancerStrategyV3 strategy = newStrategy();
    strategy.setScheme(SCHEME);

    List<ListenerCapturingTrackerClient> capturingClients = new ArrayList<>();
    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    for (int i = 0; i < 2; i++)
    {
      ListenerCapturingTrackerClient c = newListenerCapturingTrackerClient(URI.create("http://host" + i + ":1234"));
      capturingClients.add(c);
      trackerClients.put(c.getUri(), c);
    }

    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, trackerClients);

    long simulatedLatencyMs = 123L;
    int invokedListeners = 0;
    for (ListenerCapturingTrackerClient client : capturingClients)
    {
      if (client.capturedListener != null)
      {
        client.capturedListener.accept(simulatedLatencyMs, PerCallDurationSemantics.FULL_ROUND_TRIP);
        invokedListeners++;
      }
    }
    assertTrue(invokedListeners > 0, "Expected at least one tracker client to have a per-call listener registered");

    assertEquals(_provider.getCallCount("recordHostLatency"), invokedListeners,
        "recordHostLatency should be invoked once per simulated per-call listener invocation");
    assertEquals(_provider.getLastLongValue("recordHostLatency").longValue(), simulatedLatencyMs);
    assertEquals(_provider.getLastServiceName("recordHostLatency"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("recordHostLatency"), SCHEME);
    assertEquals(_provider.getLastPerCallDurationSemantics("recordHostLatency"),
        PerCallDurationSemantics.FULL_ROUND_TRIP);
  }

  @Test
  public void testPerCallListenerNotReRegisteredForExistingClientOnSubsequentCycle()
  {
    DegraderLoadBalancerStrategyV3 strategy = newStrategy();
    strategy.setScheme(SCHEME);

    URI uri = URI.create("http://host0:1234");
    CountingListenerCapturingTrackerClient client = newCountingListenerCapturingTrackerClient(uri);
    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(uri, client);

    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, trackerClients);
    assertEquals(client.setListenerCallCount, 1,
        "Listener must be registered exactly once on the first state update for a new client");

    strategy.getRing(CLUSTER_GENERATION_ID + 1, PARTITION_ID, trackerClients);
    assertEquals(client.setListenerCallCount, 1,
        "Listener must not be re-registered for a client already known to the partition");
  }

  @Test
  public void testPerCallListenerReRegisteredAfterClusterRegenWithSameUris()
  {
    DegraderLoadBalancerStrategyV3 strategy = newStrategy();
    strategy.setScheme(SCHEME);

    URI uri = URI.create("http://host0:1234");
    ListenerCapturingTrackerClient firstGeneration = newListenerCapturingTrackerClient(uri);
    Map<URI, TrackerClient> firstClients = new HashMap<>();
    firstClients.put(uri, firstGeneration);

    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, firstClients);
    assertTrue(firstGeneration.capturedListener != null, "First generation client should get a listener");

    ListenerCapturingTrackerClient secondGeneration = newListenerCapturingTrackerClient(uri);
    Map<URI, TrackerClient> secondClients = new HashMap<>();
    secondClients.put(uri, secondGeneration);

    strategy.getRing(CLUSTER_GENERATION_ID + 1, PARTITION_ID, secondClients);

    assertTrue(secondGeneration.capturedListener != null,
        "New tracker client instance at an existing URI must receive a per-call listener after cluster regen");
    secondGeneration.capturedListener.accept(99L, PerCallDurationSemantics.FULL_ROUND_TRIP);
    assertEquals(_provider.getCallCount("recordHostLatency"), 1);
    assertEquals(_provider.getLastLongValue("recordHostLatency").longValue(), 99L);
  }

  @Test
  public void testStateUpdateBeforeSetSchemeSkipsEmission()
  {
    DegraderLoadBalancerStrategyV3 strategy = newStrategy();
    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, newTrackerClientMap(1));

    assertEquals(_provider.getCallCount("updateOverrideClusterDropRate"), 0,
        "Gauge metrics should not be emitted before setScheme is called");
    assertEquals(_provider.getCallCount("updateTotalPointsInHashRing"), 0,
        "Gauge metrics should not be emitted before setScheme is called");
  }

  @Test
  public void testPerCallListenerSkipsEmissionBeforeSetScheme()
  {
    DegraderLoadBalancerStrategyV3 strategy = newStrategy();
    ListenerCapturingTrackerClient client = newListenerCapturingTrackerClient(URI.create("http://host:1234"));
    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(client.getUri(), client);

    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, trackerClients);

    assertTrue(client.capturedListener != null, "Listener must still be registered on the tracker client");

    client.capturedListener.accept(123L, PerCallDurationSemantics.FULL_ROUND_TRIP);
    assertEquals(_provider.getCallCount("recordHostLatency"), 0,
        "Per-call latency must not be recorded before setScheme is called");

    strategy.setScheme(SCHEME);
    client.capturedListener.accept(321L, PerCallDurationSemantics.FULL_ROUND_TRIP);
    assertEquals(_provider.getCallCount("recordHostLatency"), 1,
        "Per-call latency should be recorded once scheme is initialized");
    assertEquals(_provider.getLastLongValue("recordHostLatency").longValue(), 321L);
    assertEquals(_provider.getLastScheme("recordHostLatency"), SCHEME);
  }

  @Test
  public void testSetSchemeNullPreservesExistingScheme()
  {
    DegraderLoadBalancerStrategyV3 strategy = newStrategy();
    strategy.setScheme(SCHEME);
    strategy.setScheme(null);

    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, newTrackerClientMap(1));

    assertEquals(_provider.getCallCount("updateOverrideClusterDropRate"), 1,
        "Metrics must still be emitted with the previously-set scheme");
    assertEquals(_provider.getLastScheme("updateOverrideClusterDropRate"), SCHEME,
        "setScheme(null) must not change the scheme");
  }

  @Test
  public void testSetSchemeWithPlaceholderPreservesExistingScheme()
  {
    DegraderLoadBalancerStrategyV3 strategy = newStrategy();
    strategy.setScheme(SCHEME);
    strategy.setScheme("-");

    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, newTrackerClientMap(1));

    assertEquals(_provider.getCallCount("updateOverrideClusterDropRate"), 1);
    assertEquals(_provider.getLastScheme("updateOverrideClusterDropRate"), SCHEME,
        "setScheme(\"-\") must not change the scheme");
  }

  @Test
  public void testGaugeEmissionRuntimeExceptionDoesNotAbortRingUpdate()
  {
    DegraderLoadBalancerStrategyV3OtelMetricsProvider provider = new TestDegraderLoadBalancerStrategyV3OtelMetricsProvider()
    {
      @Override
      public void updateOverrideClusterDropRate(String serviceName, String scheme, double overrideClusterDropRate)
      {
        throw new RuntimeException("simulated OTel gauge failure");
      }
    };
    DegraderLoadBalancerStrategyV3 strategy = new DegraderLoadBalancerStrategyV3(
        new DegraderLoadBalancerStrategyConfig(5000),
        SERVICE_NAME,
        null,
        NO_LISTENERS,
        provider);
    strategy.setScheme(SCHEME);

    Ring<URI> ring = strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, newTrackerClientMap(1));
    assertNotNull(ring, "Ring must be returned even when gauge emission throws");

    Ring<URI> secondRing = strategy.getRing(CLUSTER_GENERATION_ID + 1, PARTITION_ID, newTrackerClientMap(2));
    assertNotNull(secondRing,
        "Subsequent ring must still be returned; provider exceptions on gauges must not halt updates");
  }

  @Test
  public void testNullProviderIsCoalescedToNoOpAndDoesNotNpeOnEmission()
  {
    DegraderLoadBalancerStrategyV3 strategy = new DegraderLoadBalancerStrategyV3(
        new DegraderLoadBalancerStrategyConfig(5000),
        SERVICE_NAME,
        null,
        NO_LISTENERS,
        null);
    strategy.setScheme(SCHEME);

    Ring<URI> ring = strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, newTrackerClientMap(2));
    assertNotNull(ring,
        "Ring must be returned when provider is null (constructor must coalesce to NoOp)");
  }

  @Test
  public void testEmptyTrackerClientsDoesNotEmitMetrics()
  {
    DegraderLoadBalancerStrategyV3 strategy = newStrategy();
    strategy.setScheme(SCHEME);

    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, Collections.emptyMap());

    assertEquals(_provider.getCallCount("updateOverrideClusterDropRate"), 0,
        "No metrics should be emitted when there are no tracker clients");
    assertEquals(_provider.getCallCount("updateTotalPointsInHashRing"), 0,
        "No metrics should be emitted when there are no tracker clients");
    assertEquals(_provider.getCallCount("recordHostLatency"), 0,
        "No latencies should be recorded when there are no tracker clients");
  }

  private DegraderLoadBalancerStrategyV3 newStrategy()
  {
    return new DegraderLoadBalancerStrategyV3(
        new DegraderLoadBalancerStrategyConfig(5000),
        SERVICE_NAME,
        null,
        NO_LISTENERS,
        _provider);
  }

  private static Map<URI, TrackerClient> newTrackerClientMap(int count)
  {
    Map<URI, TrackerClient> clients = new HashMap<>();
    for (int i = 0; i < count; i++)
    {
      DegraderTrackerClient client = newDegraderTrackerClient(URI.create("http://host" + i + ":1234"));
      clients.put(client.getUri(), client);
    }
    return clients;
  }

  private static DegraderTrackerClient newDegraderTrackerClient(URI uri)
  {
    Map<Integer, PartitionData> partitionDataMap = new HashMap<>();
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1));
    return new DegraderTrackerClientImpl(uri, partitionDataMap, new NoopTransportClient(),
        SystemClock.instance(), null);
  }

  private static ListenerCapturingTrackerClient newListenerCapturingTrackerClient(URI uri)
  {
    Map<Integer, PartitionData> partitionDataMap = new HashMap<>();
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1));
    return new ListenerCapturingTrackerClient(uri, partitionDataMap, new NoopTransportClient());
  }

  private static CountingListenerCapturingTrackerClient newCountingListenerCapturingTrackerClient(URI uri)
  {
    Map<Integer, PartitionData> partitionDataMap = new HashMap<>();
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1));
    return new CountingListenerCapturingTrackerClient(uri, partitionDataMap, new NoopTransportClient());
  }

  private static final class ListenerCapturingTrackerClient extends DegraderTrackerClientImpl
  {
    volatile PerCallDurationListener capturedListener;

    ListenerCapturingTrackerClient(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient client)
    {
      super(uri, partitionDataMap, client, SystemClock.instance(), null);
    }

    @Override
    public void setPerCallDurationListener(PerCallDurationListener listener)
    {
      capturedListener = listener;
      super.setPerCallDurationListener(listener);
    }
  }

  private static final class CountingListenerCapturingTrackerClient extends DegraderTrackerClientImpl
  {
    volatile int setListenerCallCount;

    CountingListenerCapturingTrackerClient(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient client)
    {
      super(uri, partitionDataMap, client, SystemClock.instance(), null);
    }

    @Override
    public void setPerCallDurationListener(PerCallDurationListener listener)
    {
      setListenerCallCount++;
      super.setPerCallDurationListener(listener);
    }
  }

  private static final class NoopTransportClient implements TransportClient
  {
    @Override
    public void restRequest(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
        TransportCallback<RestResponse> callback)
    {
    }

    @Override
    public void streamRequest(StreamRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
        TransportCallback<StreamResponse> callback)
    {
    }

    @Override
    public void shutdown(Callback<None> callback)
    {
    }
  }
}

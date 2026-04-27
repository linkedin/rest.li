package com.linkedin.d2.balancer.strategies.degrader;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.clients.DegraderTrackerClient;
import com.linkedin.d2.balancer.clients.DegraderTrackerClientImpl;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.PartitionData;
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
import java.util.function.Consumer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


/**
 * Integration tests that verify {@link DegraderLoadBalancerStrategyV3} actually invokes the
 * {@link DegraderLoadBalancerStrategyV3OtelMetricsProvider} from production code paths.
 *
 * <p>These complement the unit tests in
 * {@code com.linkedin.d2.jmx.DegraderLoadBalancerStrategyV3OtelMetricsProviderTest}, which only
 * exercise the test-double in isolation. Here we construct the real strategy with a test provider
 * and trigger production wiring (state updates, per-call latency listener registration) to make
 * sure metrics are actually emitted.
 */
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

    // Triggers checkUpdatePartitionState -> updatePartitionState -> emitOtelMetrics.
    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, trackerClients);

    assertEquals(_provider.getCallCount("updateOverrideClusterDropRate"), 1,
        "updateOverrideClusterDropRate should be invoked exactly once after a state update");
    assertEquals(_provider.getCallCount("updateTotalPointsInHashRing"), 1,
        "updateTotalPointsInHashRing should be invoked exactly once after a state update");
    assertEquals(_provider.getLastServiceName("updateOverrideClusterDropRate"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("updateOverrideClusterDropRate"), SCHEME);
    assertEquals(_provider.getLastServiceName("updateTotalPointsInHashRing"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("updateTotalPointsInHashRing"), SCHEME);

    // 3 healthy hosts at default 100 points each.
    assertEquals(_provider.getLastIntValue("updateTotalPointsInHashRing").intValue(), 300,
        "Total points in hash ring should reflect 3 healthy hosts at 100 points each");
  }

  @Test
  public void testPerCallLatencyListenerRegisteredAndForwardsToProvider()
  {
    DegraderLoadBalancerStrategyV3 strategy = newStrategy();
    strategy.setScheme(SCHEME);

    // Use a subclass of DegraderTrackerClientImpl that captures the listener so we can later
    // invoke it directly and verify the wiring forwards calls to the provider.
    List<ListenerCapturingTrackerClient> capturingClients = new ArrayList<>();
    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    for (int i = 0; i < 2; i++)
    {
      ListenerCapturingTrackerClient c = newListenerCapturingTrackerClient(URI.create("http://host" + i + ":1234"));
      capturingClients.add(c);
      trackerClients.put(c.getUri(), c);
    }

    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, trackerClients);

    // Each tracker client newly observed by the strategy should have received a per-call listener.
    long simulatedLatencyMs = 123L;
    int invokedListeners = 0;
    for (ListenerCapturingTrackerClient client : capturingClients)
    {
      if (client.capturedListener != null)
      {
        client.capturedListener.accept(simulatedLatencyMs);
        invokedListeners++;
      }
    }
    assertTrue(invokedListeners > 0, "Expected at least one tracker client to have a per-call listener registered");

    assertEquals(_provider.getCallCount("recordHostLatency"), invokedListeners,
        "recordHostLatency should be invoked once per simulated per-call listener invocation");
    assertEquals(_provider.getLastLongValue("recordHostLatency").longValue(), simulatedLatencyMs);
    assertEquals(_provider.getLastServiceName("recordHostLatency"), SERVICE_NAME);
    assertEquals(_provider.getLastScheme("recordHostLatency"), SCHEME);
  }

  @Test
  public void testStateUpdateBeforeSetSchemeSkipsEmission()
  {
    DegraderLoadBalancerStrategyV3 strategy = newStrategy();
    // Intentionally do NOT call setScheme to simulate the early-startup window before
    // D2ClientJmxManager.doRegisterLoadBalancerStrategy fires.

    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, newTrackerClientMap(1));

    // Metrics must NOT be emitted with the placeholder scheme; otherwise the OTel backend
    // would accumulate spurious cardinality under scheme="-".
    assertEquals(_provider.getCallCount("updateOverrideClusterDropRate"), 0,
        "Gauge metrics should not be emitted before setScheme is called");
    assertEquals(_provider.getCallCount("updateTotalPointsInHashRing"), 0,
        "Gauge metrics should not be emitted before setScheme is called");
  }

  @Test
  public void testPerCallListenerSkipsEmissionBeforeSetScheme()
  {
    DegraderLoadBalancerStrategyV3 strategy = newStrategy();
    // Intentionally do NOT call setScheme.

    ListenerCapturingTrackerClient client = newListenerCapturingTrackerClient(URI.create("http://host:1234"));
    Map<URI, TrackerClient> trackerClients = new HashMap<>();
    trackerClients.put(client.getUri(), client);

    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, trackerClients);

    assertTrue(client.capturedListener != null, "Listener must still be registered on the tracker client");

    // Listener fires but should short-circuit because scheme is uninitialized.
    client.capturedListener.accept(123L);
    assertEquals(_provider.getCallCount("recordHostLatency"), 0,
        "Per-call latency must not be recorded before setScheme is called");

    // Once setScheme is called, the same listener should start forwarding to the provider.
    strategy.setScheme(SCHEME);
    client.capturedListener.accept(321L);
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
    // Calling setScheme(null) must NOT clobber the previously-set scheme. If it did, the next
    // emission cycle would silently switch to the placeholder and pollute the OTel backend with
    // an unwanted dimension value.
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
    // The "-" placeholder must be ignored too -- otherwise a buggy caller could regress a real
    // scheme back to the placeholder and re-introduce the spurious-cardinality risk this guard
    // exists to prevent.
    strategy.setScheme("-");

    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, newTrackerClientMap(1));

    assertEquals(_provider.getCallCount("updateOverrideClusterDropRate"), 1);
    assertEquals(_provider.getLastScheme("updateOverrideClusterDropRate"), SCHEME,
        "setScheme(\"-\") must not change the scheme");
  }

  @Test
  public void testEmptyTrackerClientsDoesNotEmitMetrics()
  {
    DegraderLoadBalancerStrategyV3 strategy = newStrategy();
    strategy.setScheme(SCHEME);

    // No tracker clients => the strategy short-circuits and skips the state update.
    strategy.getRing(CLUSTER_GENERATION_ID, PARTITION_ID, Collections.emptyMap());

    assertEquals(_provider.getCallCount("updateOverrideClusterDropRate"), 0,
        "No metrics should be emitted when there are no tracker clients");
    assertEquals(_provider.getCallCount("updateTotalPointsInHashRing"), 0,
        "No metrics should be emitted when there are no tracker clients");
    assertEquals(_provider.getCallCount("recordHostLatency"), 0,
        "No latencies should be recorded when there are no tracker clients");
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

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

  /**
   * {@link DegraderTrackerClientImpl} subclass that captures any per-call duration listener
   * registered by the strategy so the test can invoke it directly and verify the wiring.
   */
  private static final class ListenerCapturingTrackerClient extends DegraderTrackerClientImpl
  {
    volatile Consumer<Long> capturedListener;

    ListenerCapturingTrackerClient(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient client)
    {
      super(uri, partitionDataMap, client, SystemClock.instance(), null);
    }

    @Override
    public void setPerCallDurationListener(Consumer<Long> listener)
    {
      capturedListener = listener;
      super.setPerCallDurationListener(listener);
    }
  }

  /**
   * Minimal {@link TransportClient} that does nothing. The strategy only needs a non-null transport
   * client to construct the tracker; we never actually issue requests in these tests.
   */
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

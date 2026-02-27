/*
   Copyright (c) 2024 LinkedIn Corp.

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

package com.linkedin.d2.balancer.simple;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.clients.RewriteLoadBalancerClient;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.NullPartitionProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.util.URIRequest;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.SynchronousExecutorService;
import com.linkedin.d2.discovery.stores.mock.MockStore;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.util.NamedThreadFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor.*;
import static org.testng.Assert.*;


/**
 * Property-style tests that verify the precomputed potential clients cache produces
 * results equivalent to the original O(n) per-request computation.
 *
 * For each test scenario, two load balancers are created — one with the cache enabled,
 * one without — populated identically, and then compared for behavioral equivalence.
 */
public class PotentialClientsCacheTest
{
  private ScheduledExecutorService _d2Executor;

  @BeforeSuite
  public void initialize()
  {
    _d2Executor = Executors.newSingleThreadScheduledExecutor(
        new NamedThreadFactory("D2 PropertyEventExecutor for PotentialClientsCacheTest"));
  }

  @AfterSuite
  public void shutdown()
  {
    _d2Executor.shutdown();
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────

  /**
   * Creates a started SimpleLoadBalancer backed by a SimpleLoadBalancerState
   * with the given enablePotentialClientsCache flag.
   */
  private static class LBSetup
  {
    final MockStore<ServiceProperties> serviceRegistry = new MockStore<>();
    final MockStore<ClusterProperties> clusterRegistry = new MockStore<>();
    final MockStore<UriProperties> uriRegistry = new MockStore<>();
    final SimpleLoadBalancerState state;
    final SimpleLoadBalancer loadBalancer;

    LBSetup(boolean enableCache, ScheduledExecutorService d2Executor) throws Exception
    {
      ScheduledExecutorService executorService = new SynchronousExecutorService();

      Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> strategyFactories = new HashMap<>();
      strategyFactories.put("degrader", new DegraderLoadBalancerStrategyFactoryV3());

      Map<String, com.linkedin.r2.transport.common.TransportClientFactory> clientFactories = new HashMap<>();
      clientFactories.put(PropertyKeys.HTTP_SCHEME, new SimpleLoadBalancerTest.DoNothingClientFactory());

      state = new SimpleLoadBalancerState(
          executorService,
          new PropertyEventBusImpl<>(executorService, uriRegistry),
          new PropertyEventBusImpl<>(executorService, clusterRegistry),
          new PropertyEventBusImpl<>(executorService, serviceRegistry),
          clientFactories,
          strategyFactories,
          null, null, false, null, null, null, null, false,
          enableCache);

      loadBalancer = new SimpleLoadBalancer(state, 5, TimeUnit.SECONDS, d2Executor);
      FutureCallback<None> cb = new FutureCallback<>();
      loadBalancer.start(cb);
      cb.get();
    }

    /**
     * Populates registries and triggers a getClient() call to force the
     * PropertyEventBus subscriber callbacks to fire (MockStore only publishes
     * when startPublishing has been called, which happens lazily on first listen).
     */
    void populate(String cluster, String service, ClusterProperties clusterProps,
                  ServiceProperties serviceProps, UriProperties uriProps) throws Exception
    {
      clusterRegistry.put(cluster, clusterProps);
      serviceRegistry.put(service, serviceProps);
      uriRegistry.put(cluster, uriProps);

      // Trigger listening — this causes startPublishing → publishInitialize for each property
      triggerListening(service);
    }

    /**
     * Triggers listening for a service by calling getClient(), which causes the
     * PropertyEventBus to call startPublishing on the MockStore registries.
     */
    void triggerListening(String service)
    {
      URIRequest request = new URIRequest("d2://" + service + "/resource");
      try
      {
        loadBalancer.getClient(request, new RequestContext());
      }
      catch (ServiceUnavailableException e)
      {
        // May happen if setup is incomplete — that's fine, the subscribers have still fired
      }
    }
  }

  private static UriProperties buildUriProperties(String cluster, List<URI> uris, int partitionId)
  {
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<>();
    for (URI uri : uris)
    {
      uriData.put(uri, Collections.singletonMap(partitionId, new PartitionData(1d)));
    }
    return new UriProperties(cluster, uriData);
  }

  private static List<URI> generateUris(int count)
  {
    List<URI> uris = new ArrayList<>(count);
    for (int i = 0; i < count; i++)
    {
      uris.add(URI.create("http://host" + i + ".example.com:8080"));
    }
    return uris;
  }

  /**
   * Asserts that the two load balancers route the same request to the same set of
   * possible hosts. Since the strategy may pick different individual hosts due to ring
   * randomness, we compare the potential client maps directly from the state.
   */
  private void assertPotentialClientsEquivalent(LBSetup cached, LBSetup uncached,
                                                String service, String scheme, int partitionId)
  {
    Map<URI, TrackerClient> cachedClients = cached.state.getPotentialClients(service, scheme, partitionId);
    // The uncached state returns null (cache disabled) — build the set from _trackerClients instead
    // by querying the load balancer which exercises the fallback O(n) path.
    // We compare the URI key sets since the TrackerClient instances differ between the two states.
    assertNotNull(cachedClients, "Cached potential clients should not be null when cache is enabled");

    // Get the uncached set by querying through the uncached load balancer.
    // The uncached state.getPotentialClients returns null, so we use the tracker clients as ground truth.
    Map<URI, TrackerClient> uncachedTrackerClients = uncached.state.getTrackerClients().get(service);

    // Both should have the same URI key set (modulo banned URIs which are filtered identically)
    assertEquals(cachedClients.keySet(), filterForPartition(uncachedTrackerClients, cached, service, scheme, partitionId),
        "Cached and uncached potential clients should have the same URI set");
  }

  /**
   * Filters the uncached tracker clients to match what getPotentialClients would produce:
   * only URIs in the given scheme+partition, excluding banned URIs.
   */
  private Set<URI> filterForPartition(Map<URI, TrackerClient> allClients, LBSetup setup,
                                      String service, String scheme, int partitionId)
  {
    if (allClients == null)
    {
      return Collections.emptySet();
    }
    ServiceProperties serviceProps = setup.state.getServiceProperties().get(service).getProperty();
    String cluster = serviceProps.getClusterName();
    UriProperties uriProps = setup.state.getUriProperties().get(cluster).getProperty();
    ClusterProperties clusterProps = setup.state.getClusterInfo().get(cluster)
        .getClusterPropertiesItem().getProperty();

    Set<URI> possibleUris = uriProps.getUriBySchemeAndPartition(scheme, partitionId);
    if (possibleUris == null)
    {
      return Collections.emptySet();
    }

    Set<URI> result = new HashSet<>();
    for (URI uri : possibleUris)
    {
      if (!serviceProps.isBanned(uri) && !clusterProps.isBanned(uri) && allClients.containsKey(uri))
      {
        result.add(uri);
      }
    }
    return result;
  }

  // ─── Data Providers ───────────────────────────────────────────────────────

  @DataProvider
  public Object[][] potentialClientsScenarios()
  {
    return new Object[][] {
        // {numUris, numBannedFromCluster, numBannedFromService, partitionId}
        {1,  0, 0, 0},   // single host, no bans
        {5,  0, 0, 0},   // multiple hosts, no bans
        {10, 0, 0, 0},   // more hosts, no bans
        {5,  1, 0, 0},   // cluster-banned host
        {5,  0, 1, 0},   // service-banned host
        {5,  2, 1, 0},   // mixed bans
        {10, 5, 0, 0},   // half banned
        {10, 0, 5, 0},   // half service-banned
        {10, 3, 3, 0},   // mixed heavy bans (some may overlap)
        {10, 10, 0, 0},  // all banned
        {20, 0, 0, 0},   // larger cluster
        {20, 5, 3, 0},   // larger cluster with bans
    };
  }

  // ─── Tests ────────────────────────────────────────────────────────────────

  @Test(dataProvider = "potentialClientsScenarios")
  public void testCachedEqualsUncached(int numUris, int numClusterBanned,
                                       int numServiceBanned, int partitionId) throws Exception
  {
    String cluster = "cluster-1";
    String service = "foo";
    String scheme = PropertyKeys.HTTP_SCHEME;

    List<URI> allUris = generateUris(numUris);

    // Pick banned URIs from the beginning of the list
    Set<URI> clusterBanned = new HashSet<>(allUris.subList(0, Math.min(numClusterBanned, numUris)));
    Set<URI> serviceBanned = new HashSet<>();
    int serviceStart = Math.min(numClusterBanned, numUris);
    int serviceEnd = Math.min(serviceStart + numServiceBanned, numUris);
    serviceBanned.addAll(allUris.subList(serviceStart, serviceEnd));

    ClusterProperties clusterProps = new ClusterProperties(cluster, Collections.emptyList(),
        Collections.emptyMap(), clusterBanned, NullPartitionProperties.getInstance());

    ServiceProperties serviceProps = new ServiceProperties(service, cluster, "/" + service,
        Collections.singletonList("degrader"), Collections.emptyMap(), null, null,
        Collections.singletonList(scheme), serviceBanned);

    UriProperties uriProps = buildUriProperties(cluster, allUris, partitionId);

    LBSetup cached = new LBSetup(true, _d2Executor);
    LBSetup uncached = new LBSetup(false, _d2Executor);

    cached.populate(cluster, service, clusterProps, serviceProps, uriProps);
    uncached.populate(cluster, service, clusterProps, serviceProps, uriProps);

    assertPotentialClientsEquivalent(cached, uncached, service, scheme, partitionId);

    // Also verify the cached set has the expected size
    Map<URI, TrackerClient> cachedClients = cached.state.getPotentialClients(service, scheme, partitionId);
    int expectedSize = numUris - Math.min(numClusterBanned, numUris)
        - (serviceEnd - serviceStart);
    assertEquals(cachedClients.size(), expectedSize,
        "Expected " + expectedSize + " clients after filtering " + numClusterBanned + " cluster-banned and "
            + numServiceBanned + " service-banned from " + numUris + " total");
  }

  @Test
  public void testCacheRebuiltOnUriChange() throws Exception
  {
    String cluster = "cluster-1";
    String service = "foo";
    String scheme = PropertyKeys.HTTP_SCHEME;

    LBSetup setup = new LBSetup(true, _d2Executor);

    ClusterProperties clusterProps = new ClusterProperties(cluster);
    ServiceProperties serviceProps = new ServiceProperties(service, cluster, "/" + service,
        Collections.singletonList("degrader"), Collections.emptyMap(), null, null,
        Collections.singletonList(scheme), null);

    List<URI> initialUris = generateUris(3);
    setup.populate(cluster, service, clusterProps, serviceProps,
        buildUriProperties(cluster, initialUris, 0));

    Map<URI, TrackerClient> initial = setup.state.getPotentialClients(service, scheme, 0);
    assertNotNull(initial);
    assertEquals(initial.size(), 3);

    // Add more URIs
    List<URI> expandedUris = generateUris(6);
    setup.uriRegistry.put(cluster, buildUriProperties(cluster, expandedUris, 0));

    Map<URI, TrackerClient> expanded = setup.state.getPotentialClients(service, scheme, 0);
    assertNotNull(expanded);
    assertEquals(expanded.size(), 6);

    // Remove URIs
    List<URI> reducedUris = generateUris(2);
    setup.uriRegistry.put(cluster, buildUriProperties(cluster, reducedUris, 0));

    Map<URI, TrackerClient> reduced = setup.state.getPotentialClients(service, scheme, 0);
    assertNotNull(reduced);
    assertEquals(reduced.size(), 2);
  }

  @Test
  public void testCacheRebuiltOnBanListChange() throws Exception
  {
    String cluster = "cluster-1";
    String service = "foo";
    String scheme = PropertyKeys.HTTP_SCHEME;

    LBSetup setup = new LBSetup(true, _d2Executor);

    List<URI> uris = generateUris(5);
    ClusterProperties clusterProps = new ClusterProperties(cluster);
    ServiceProperties serviceProps = new ServiceProperties(service, cluster, "/" + service,
        Collections.singletonList("degrader"), Collections.emptyMap(), null, null,
        Collections.singletonList(scheme), null);

    setup.populate(cluster, service, clusterProps, serviceProps,
        buildUriProperties(cluster, uris, 0));

    assertEquals(setup.state.getPotentialClients(service, scheme, 0).size(), 5);

    // Ban 2 URIs via cluster properties update
    Set<URI> banned = new HashSet<>(uris.subList(0, 2));
    setup.clusterRegistry.put(cluster, new ClusterProperties(cluster, Collections.emptyList(),
        Collections.emptyMap(), banned, NullPartitionProperties.getInstance()));

    assertEquals(setup.state.getPotentialClients(service, scheme, 0).size(), 3);
  }

  @Test
  public void testCacheInvalidatedOnServiceRemoval() throws Exception
  {
    String cluster = "cluster-1";
    String service = "foo";
    String scheme = PropertyKeys.HTTP_SCHEME;

    LBSetup setup = new LBSetup(true, _d2Executor);

    setup.populate(cluster, service,
        new ClusterProperties(cluster),
        new ServiceProperties(service, cluster, "/" + service,
            Collections.singletonList("degrader"), Collections.emptyMap(), null, null,
            Collections.singletonList(scheme), null),
        buildUriProperties(cluster, generateUris(3), 0));

    assertNotNull(setup.state.getPotentialClients(service, scheme, 0));

    // Remove the service
    setup.serviceRegistry.remove(service);

    assertNull(setup.state.getPotentialClients(service, scheme, 0),
        "Cache should be invalidated after service removal");
  }

  @Test
  public void testCacheDisabledReturnsNull() throws Exception
  {
    String cluster = "cluster-1";
    String service = "foo";
    String scheme = PropertyKeys.HTTP_SCHEME;

    LBSetup setup = new LBSetup(false, _d2Executor);

    setup.populate(cluster, service,
        new ClusterProperties(cluster),
        new ServiceProperties(service, cluster, "/" + service,
            Collections.singletonList("degrader"), Collections.emptyMap(), null, null,
            Collections.singletonList(scheme), null),
        buildUriProperties(cluster, generateUris(3), 0));

    assertNull(setup.state.getPotentialClients(service, scheme, 0),
        "getPotentialClients should return null when cache is disabled");
  }

  @Test(dataProvider = "potentialClientsScenarios")
  public void testGetClientEquivalence(int numUris, int numClusterBanned,
                                       int numServiceBanned, int partitionId) throws Exception
  {
    // Skip edge case where all URIs are banned — getClient throws ServiceUnavailableException
    if (numUris == 0 || numClusterBanned + numServiceBanned >= numUris)
    {
      return;
    }

    String cluster = "cluster-1";
    String service = "foo";
    String scheme = PropertyKeys.HTTP_SCHEME;

    List<URI> allUris = generateUris(numUris);

    Set<URI> clusterBanned = new HashSet<>(allUris.subList(0, Math.min(numClusterBanned, numUris)));
    int serviceStart = Math.min(numClusterBanned, numUris);
    int serviceEnd = Math.min(serviceStart + numServiceBanned, numUris);
    Set<URI> serviceBanned = new HashSet<>(allUris.subList(serviceStart, serviceEnd));

    ClusterProperties clusterProps = new ClusterProperties(cluster, Collections.emptyList(),
        Collections.emptyMap(), clusterBanned, NullPartitionProperties.getInstance());
    ServiceProperties serviceProps = new ServiceProperties(service, cluster, "/" + service,
        Collections.singletonList("degrader"), Collections.emptyMap(), null, null,
        Collections.singletonList(scheme), serviceBanned);
    UriProperties uriProps = buildUriProperties(cluster, allUris, partitionId);

    LBSetup cached = new LBSetup(true, _d2Executor);
    LBSetup uncached = new LBSetup(false, _d2Executor);

    cached.populate(cluster, service, clusterProps, serviceProps, uriProps);
    uncached.populate(cluster, service, clusterProps, serviceProps, uriProps);

    // Both load balancers should be able to route requests successfully.
    // We verify the selected host is in the expected set (not banned).
    URIRequest request = new URIRequest("d2://" + service + "/resource");
    Set<URI> expectedUris = new HashSet<>(allUris);
    expectedUris.removeAll(clusterBanned);
    expectedUris.removeAll(serviceBanned);
    // Transform to expected form with path appended
    Set<URI> expectedWithPath = new HashSet<>();
    for (URI uri : expectedUris)
    {
      expectedWithPath.add(URI.create(uri + "/" + service));
    }

    RewriteLoadBalancerClient cachedClient =
        (RewriteLoadBalancerClient) cached.loadBalancer.getClient(request, new RequestContext());
    assertTrue(expectedWithPath.contains(cachedClient.getUri()),
        "Cached LB selected " + cachedClient.getUri() + " which is not in expected set " + expectedWithPath);

    RewriteLoadBalancerClient uncachedClient =
        (RewriteLoadBalancerClient) uncached.loadBalancer.getClient(request, new RequestContext());
    assertTrue(expectedWithPath.contains(uncachedClient.getUri()),
        "Uncached LB selected " + uncachedClient.getUri() + " which is not in expected set " + expectedWithPath);
  }

  @Test
  public void testMultipleServicesOnSameCluster() throws Exception
  {
    String cluster = "cluster-1";
    String scheme = PropertyKeys.HTTP_SCHEME;

    LBSetup cached = new LBSetup(true, _d2Executor);
    LBSetup uncached = new LBSetup(false, _d2Executor);

    ClusterProperties clusterProps = new ClusterProperties(cluster);
    List<URI> uris = generateUris(5);
    UriProperties uriProps = buildUriProperties(cluster, uris, 0);

    // Two services on the same cluster, one with bans
    Set<URI> svc2Banned = new HashSet<>(uris.subList(0, 2));
    ServiceProperties svc1Props = new ServiceProperties("svc1", cluster, "/svc1",
        Collections.singletonList("degrader"), Collections.emptyMap(), null, null,
        Collections.singletonList(scheme), null);
    ServiceProperties svc2Props = new ServiceProperties("svc2", cluster, "/svc2",
        Collections.singletonList("degrader"), Collections.emptyMap(), null, null,
        Collections.singletonList(scheme), svc2Banned);

    for (LBSetup setup : new LBSetup[]{cached, uncached})
    {
      setup.clusterRegistry.put(cluster, clusterProps);
      setup.serviceRegistry.put("svc1", svc1Props);
      setup.serviceRegistry.put("svc2", svc2Props);
      setup.uriRegistry.put(cluster, uriProps);
      setup.triggerListening("svc1");
      setup.triggerListening("svc2");
    }

    // svc1 should see all 5 hosts
    assertPotentialClientsEquivalent(cached, uncached, "svc1", scheme, 0);
    assertEquals(cached.state.getPotentialClients("svc1", scheme, 0).size(), 5);

    // svc2 should see 3 hosts (2 banned)
    assertPotentialClientsEquivalent(cached, uncached, "svc2", scheme, 0);
    assertEquals(cached.state.getPotentialClients("svc2", scheme, 0).size(), 3);
  }

  @Test
  public void testCacheRebuiltOnUriChangeMultipleServices() throws Exception
  {
    String cluster = "cluster-1";
    String scheme = PropertyKeys.HTTP_SCHEME;

    LBSetup setup = new LBSetup(true, _d2Executor);

    ClusterProperties clusterProps = new ClusterProperties(cluster);
    ServiceProperties svc1 = new ServiceProperties("svc1", cluster, "/svc1",
        Collections.singletonList("degrader"), Collections.emptyMap(), null, null,
        Collections.singletonList(scheme), null);
    ServiceProperties svc2 = new ServiceProperties("svc2", cluster, "/svc2",
        Collections.singletonList("degrader"), Collections.emptyMap(), null, null,
        Collections.singletonList(scheme), null);

    setup.clusterRegistry.put(cluster, clusterProps);
    setup.serviceRegistry.put("svc1", svc1);
    setup.serviceRegistry.put("svc2", svc2);
    setup.uriRegistry.put(cluster, buildUriProperties(cluster, generateUris(4), 0));
    setup.triggerListening("svc1");
    setup.triggerListening("svc2");

    assertEquals(setup.state.getPotentialClients("svc1", scheme, 0).size(), 4);
    assertEquals(setup.state.getPotentialClients("svc2", scheme, 0).size(), 4);

    // URI change should rebuild cache for BOTH services
    setup.uriRegistry.put(cluster, buildUriProperties(cluster, generateUris(7), 0));

    assertEquals(setup.state.getPotentialClients("svc1", scheme, 0).size(), 7);
    assertEquals(setup.state.getPotentialClients("svc2", scheme, 0).size(), 7);
  }
}

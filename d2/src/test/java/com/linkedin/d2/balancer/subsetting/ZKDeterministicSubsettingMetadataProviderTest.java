/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.d2.balancer.subsetting;

import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerTest;
import com.linkedin.d2.balancer.simple.SslSessionValidatorFactory;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.SynchronousExecutorService;
import com.linkedin.d2.discovery.stores.mock.MockStore;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionNotTrustedException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;


public class ZKDeterministicSubsettingMetadataProviderTest
{
  private static final String CLUSTER_NAME = "cluster-1";
  private static final String HOST_NAME = "test2.linkedin.com";

  private MockStore<UriProperties>                                                 _uriRegistry;
  private MockStore<ClusterProperties>                                             _clusterRegistry;
  private MockStore<ServiceProperties>                                             _serviceRegistry;
  private SimpleLoadBalancerState _state;

  private ZKDeterministicSubsettingMetadataProvider _metadataProvider;

  private static final SslSessionValidatorFactory SSL_SESSION_VALIDATOR_FACTORY =
      validationStrings -> sslSession ->
      {
        if (validationStrings == null || validationStrings.isEmpty())
        {
          throw new SslSessionNotTrustedException("no validation string");
        }
      };

  @BeforeMethod
  public void setUp()
  {
    ScheduledExecutorService executorService = new SynchronousExecutorService();
    _uriRegistry = new MockStore<>();
    _clusterRegistry = new MockStore<>();
    _serviceRegistry = new MockStore<>();
    Map<String, TransportClientFactory> clientFactories = new HashMap<>();
    Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        new HashMap<>();
    loadBalancerStrategyFactories.put("random", new RandomLoadBalancerStrategyFactory());

    SSLContext sslContext;
    try {
      sslContext = SSLContext.getDefault();
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new RuntimeException(e);
    }

    SSLParameters sslParameters = new SSLParameters();
    clientFactories.put("https", new SimpleLoadBalancerTest.DoNothingClientFactory());
    _state =
        new SimpleLoadBalancerState(executorService,
            new PropertyEventBusImpl<>(executorService, _uriRegistry),
            new PropertyEventBusImpl<>(executorService, _clusterRegistry),
            new PropertyEventBusImpl<>(executorService, _serviceRegistry), clientFactories,
            loadBalancerStrategyFactories, sslContext, sslParameters,
            true, null,
            SSL_SESSION_VALIDATOR_FACTORY);

    _metadataProvider = new ZKDeterministicSubsettingMetadataProvider(CLUSTER_NAME, HOST_NAME, 1000, TimeUnit.MILLISECONDS);
  }

  @Test
  public void testGetSubsettingMetadata()
  {
    List<String> schemes = new ArrayList<>();
    Map<Integer, PartitionData> partitionData = new HashMap<>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<>();
    for (int i = 0; i < 10; i++)
    {
      uriData.put(URI.create("http://test" + i + ".linkedin.com:8888/test"), partitionData);
    }
    schemes.add("http");

    _state.listenToCluster("cluster-1", new LoadBalancerState.NullStateListenerCallback());
    _state.listenToService("service-1", new LoadBalancerState.NullStateListenerCallback());
    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1", schemes));
    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));
    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
        "cluster-1",
        "/test", Collections.singletonList("random")));

    DeterministicSubsettingMetadata metadata = _metadataProvider.getSubsettingMetadata(_state);

    assertEquals(metadata.getInstanceId(), 2);
    assertEquals(metadata.getTotalInstanceCount(), 10);
    assertEquals(metadata.getPeerClusterVersion(), 5);

    uriData.remove(URI.create("http://test0.linkedin.com:8888/test"));
    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    metadata = _metadataProvider.getSubsettingMetadata(_state);
    assertEquals(metadata.getInstanceId(), 1);
    assertEquals(metadata.getTotalInstanceCount(), 9);
    assertEquals(metadata.getPeerClusterVersion(), 7);

    uriData.remove(URI.create("http://test2.linkedin.com:8888/test"));
    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    metadata = _metadataProvider.getSubsettingMetadata(_state);
    assertNull(metadata);
  }

  @Test
  public void testCandidateIdentities_includesFqdnIpv4AndIpv6() throws Exception
  {
    InetAddress ipv4 = InetAddress.getByName("10.20.30.40");
    InetAddress ipv6 = InetAddress.getByName("2a04:f547:43:e66a::1a95");
    ZKDeterministicSubsettingMetadataProvider provider = new ZKDeterministicSubsettingMetadataProvider(
        CLUSTER_NAME, HOST_NAME, 1000, TimeUnit.MILLISECONDS,
        host -> new InetAddress[] {ipv4, ipv6});

    String ipv6Str = ipv6.getHostAddress();
    Set<String> identities = provider.getCandidateIdentities();
    assertEquals(identities.size(), 4, "identities=" + identities);
    assertTrue(identities.contains(HOST_NAME));
    assertTrue(identities.contains("10.20.30.40"));
    assertTrue(identities.contains("[" + ipv6Str + "]"));
    assertTrue(identities.contains(ipv6Str));
  }

  @Test
  public void testCandidateIdentities_dnsFailure_fallsBackToFqdn()
  {
    ZKDeterministicSubsettingMetadataProvider provider = new ZKDeterministicSubsettingMetadataProvider(
        CLUSTER_NAME, HOST_NAME, 1000, TimeUnit.MILLISECONDS,
        host -> { throw new UnknownHostException(host); });

    assertEquals(provider.getCandidateIdentities(), Collections.singleton(HOST_NAME));
  }

  @Test
  public void testCandidateIdentities_keepsIpv6ScopeId() throws Exception
  {
    byte[] linkLocalBytes = {(byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
    InetAddress linkLocal = Inet6Address.getByAddress("fe80::1", linkLocalBytes, 1);
    String linkLocalStr = linkLocal.getHostAddress();
    assertTrue(linkLocalStr.contains("%"), "expected scope id in stringified address; got " + linkLocalStr);

    ZKDeterministicSubsettingMetadataProvider provider = new ZKDeterministicSubsettingMetadataProvider(
        CLUSTER_NAME, HOST_NAME, 1000, TimeUnit.MILLISECONDS,
        host -> new InetAddress[] {linkLocal});

    Set<String> identities = provider.getCandidateIdentities();
    assertTrue(identities.contains("[" + linkLocalStr + "]"),
        "expected bracketed scope-bearing identity; got " + identities);
    assertTrue(identities.contains(linkLocalStr),
        "expected unbracketed scope-bearing identity; got " + identities);
  }

  @Test
  public void testGetSubsettingMetadata_matchesViaIpv6() throws Exception
  {
    InetAddress selfIpv6 = InetAddress.getByName("2a04:f547:43:e66a::1a95");
    InetAddress otherIpv6 = InetAddress.getByName("2a04:f547:43:0::1");
    String selfIpv6Str = selfIpv6.getHostAddress();
    String otherIpv6Str = otherIpv6.getHostAddress();

    ZKDeterministicSubsettingMetadataProvider ipv6Provider = new ZKDeterministicSubsettingMetadataProvider(
        CLUSTER_NAME, HOST_NAME, 1000, TimeUnit.MILLISECONDS,
        host -> new InetAddress[] {selfIpv6});

    Map<Integer, PartitionData> partitionData = new HashMap<>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<>();
    uriData.put(URI.create("https://[" + otherIpv6Str + "]:8888/test"), partitionData);
    uriData.put(URI.create("https://[" + selfIpv6Str + "]:8888/test"), partitionData);

    _state.listenToCluster(CLUSTER_NAME, new LoadBalancerState.NullStateListenerCallback());
    _state.listenToService("service-1", new LoadBalancerState.NullStateListenerCallback());
    _clusterRegistry.put(CLUSTER_NAME, new ClusterProperties(CLUSTER_NAME, Collections.singletonList("https")));
    _uriRegistry.put(CLUSTER_NAME, new UriProperties(CLUSTER_NAME, uriData));
    _serviceRegistry.put("service-1", new ServiceProperties("service-1", CLUSTER_NAME, "/test",
        Collections.singletonList("random")));

    DeterministicSubsettingMetadata metadata = ipv6Provider.getSubsettingMetadata(_state);

    assertNotNull(metadata, "expected non-null metadata when self IPv6 appears in peer cluster URIs");
    assertEquals(metadata.getTotalInstanceCount(), 2);
    // URI.getHost() returns "[ipv6]"; sortedHosts is sorted ASCII over those two strings.
    List<String> expectedSorted = new ArrayList<>();
    expectedSorted.add("[" + selfIpv6Str + "]");
    expectedSorted.add("[" + otherIpv6Str + "]");
    Collections.sort(expectedSorted);
    assertEquals(metadata.getInstanceId(), expectedSorted.indexOf("[" + selfIpv6Str + "]"));
  }

  @Test
  public void testGetSubsettingMetadata_noMatch_warnsOnceThenReArmsOnRecovery()
  {
    InetAddress otherIpv4;
    try
    {
      otherIpv4 = InetAddress.getByName("10.20.30.40");
    }
    catch (UnknownHostException e)
    {
      throw new AssertionError(e);
    }
    ZKDeterministicSubsettingMetadataProvider provider = new ZKDeterministicSubsettingMetadataProvider(
        CLUSTER_NAME, HOST_NAME, 1000, TimeUnit.MILLISECONDS,
        host -> new InetAddress[] {otherIpv4});

    Map<Integer, PartitionData> partitionData = new HashMap<>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));

    // 1) Initial state has only foreign hosts → no match → WARN guard arms.
    Map<URI, Map<Integer, PartitionData>> noMatchData = new HashMap<>();
    noMatchData.put(URI.create("http://otherA.linkedin.com:8888/test"), partitionData);
    noMatchData.put(URI.create("http://otherB.linkedin.com:8888/test"), partitionData);

    _state.listenToCluster(CLUSTER_NAME, new LoadBalancerState.NullStateListenerCallback());
    _state.listenToService("service-1", new LoadBalancerState.NullStateListenerCallback());
    _clusterRegistry.put(CLUSTER_NAME, new ClusterProperties(CLUSTER_NAME, Collections.singletonList("http")));
    _uriRegistry.put(CLUSTER_NAME, new UriProperties(CLUSTER_NAME, noMatchData));
    _serviceRegistry.put("service-1", new ServiceProperties("service-1", CLUSTER_NAME, "/test",
        Collections.singletonList("random")));

    assertNull(provider.getSubsettingMetadata(_state));
    assertTrue(provider.hasWarnedIdentityNotFound(), "first no-match should arm the WARN guard");

    // 2) Another no-match URI update — guard must remain armed (no second WARN). Push a
    //    different URI set so the version changes and the inner branch actually executes.
    Map<URI, Map<Integer, PartitionData>> noMatchData2 = new HashMap<>(noMatchData);
    noMatchData2.put(URI.create("http://otherC.linkedin.com:8888/test"), partitionData);
    _uriRegistry.put(CLUSTER_NAME, new UriProperties(CLUSTER_NAME, noMatchData2));

    assertNull(provider.getSubsettingMetadata(_state));
    assertTrue(provider.hasWarnedIdentityNotFound(),
        "subsequent no-match should NOT re-WARN — guard stays armed");

    // 3) Recovery: peer cluster now contains the self FQDN → match → guard re-arms (false).
    Map<URI, Map<Integer, PartitionData>> matchData = new HashMap<>(noMatchData2);
    matchData.put(URI.create("http://" + HOST_NAME + ":8888/test"), partitionData);
    _uriRegistry.put(CLUSTER_NAME, new UriProperties(CLUSTER_NAME, matchData));

    DeterministicSubsettingMetadata metadata = provider.getSubsettingMetadata(_state);
    assertNotNull(metadata, "FQDN now in peer cluster — expected non-null metadata");
    assertFalse(provider.hasWarnedIdentityNotFound(),
        "successful match must re-arm the guard so the next outage logs once again");

    // 4) Outage again → guard arms once more (proving "next outage logs once again").
    _uriRegistry.put(CLUSTER_NAME, new UriProperties(CLUSTER_NAME, noMatchData));
    assertNull(provider.getSubsettingMetadata(_state));
    assertTrue(provider.hasWarnedIdentityNotFound(), "post-recovery outage should re-arm guard");
  }
}

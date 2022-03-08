/*
   Copyright (c) 2022 LinkedIn Corp.

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

package com.linkedin.d2.balancer.clusterfailout;

import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.properties.ClusterFailoutProperties;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterStoreProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerTest;
import com.linkedin.d2.balancer.simple.SslSessionValidatorFactory;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategyFactory;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.SynchronousExecutorService;
import com.linkedin.d2.discovery.stores.mock.MockStore;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionNotTrustedException;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class ZKClusterFailoutConfigProviderTest
{
  private static final String CLUSTER_NAME = "cluster-1";
  private static final String PEER_CLUSTER_NAME1 = "cluster-peer1";
  private static final String PEER_CLUSTER_NAME2 = "cluster-peer2";

  private MockStore<UriProperties> _uriRegistry;
  private MockStore<ClusterProperties> _clusterRegistry;
  private MockStore<ServiceProperties> _serviceRegistry;
  private SimpleLoadBalancerState _state;

  private ZKClusterFailoutConfigProvider _clusterFailoutConfigProvider;

  private static final SslSessionValidatorFactory SSL_SESSION_VALIDATOR_FACTORY = validationStrings -> sslSession -> {
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
    Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories = new HashMap<>();
    loadBalancerStrategyFactories.put("random", new RandomLoadBalancerStrategyFactory());

    SSLContext sslContext;
    try
    {
      sslContext = SSLContext.getDefault();
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new RuntimeException(e);
    }

    SSLParameters sslParameters = new SSLParameters();
    clientFactories.put("https", new SimpleLoadBalancerTest.DoNothingClientFactory());
    _state = new SimpleLoadBalancerState(executorService, new PropertyEventBusImpl<>(executorService, _uriRegistry),
                                         new PropertyEventBusImpl<>(executorService, _clusterRegistry),
                                         new PropertyEventBusImpl<>(executorService, _serviceRegistry), clientFactories,
                                         loadBalancerStrategyFactories, sslContext, sslParameters, true, null, SSL_SESSION_VALIDATOR_FACTORY);

    _clusterFailoutConfigProvider = new TestingZKClusterFailoutConfigProvider(_state);
    _clusterFailoutConfigProvider.start();
  }

  @Test
  public void testNewCluster()
  {
    _state.listenToCluster(CLUSTER_NAME, new LoadBalancerState.NullStateListenerCallback());
    _clusterRegistry.put(CLUSTER_NAME, createClusterStoreProperties(false, false, Collections.emptySet()));
    assertNull(_clusterFailoutConfigProvider.getFailoutConfig(CLUSTER_NAME));
  }

  @Test
  public void testModifyClusterConfig()
  {
    testNewCluster();
    _clusterRegistry.put(CLUSTER_NAME, createClusterStoreProperties(true, false, Collections.emptySet()));
    ClusterFailoutConfig config = _clusterFailoutConfigProvider.getFailoutConfig(CLUSTER_NAME);
    assertNotNull(config);
    assertFalse(config.isFailedOut());
    assertTrue(config.getPeerClusters().isEmpty());

    _clusterRegistry.put(CLUSTER_NAME, createClusterStoreProperties(true, true, Collections.singleton(PEER_CLUSTER_NAME1)));
    config = _clusterFailoutConfigProvider.getFailoutConfig(CLUSTER_NAME);
    assertNotNull(config);
    assertTrue(config.isFailedOut());
    assertEquals(config.getPeerClusters(), Collections.singletonList(PEER_CLUSTER_NAME1));

    _clusterRegistry
      .put(CLUSTER_NAME, createClusterStoreProperties(true, true, new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1, PEER_CLUSTER_NAME2))));
    config = _clusterFailoutConfigProvider.getFailoutConfig(CLUSTER_NAME);
    assertNotNull(config);
    assertTrue(config.isFailedOut());
    assertEquals(config.getPeerClusters(), new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1, PEER_CLUSTER_NAME2)));

    _clusterRegistry.put(CLUSTER_NAME, createClusterStoreProperties(true, false, Collections.emptySet()));
    config = _clusterFailoutConfigProvider.getFailoutConfig(CLUSTER_NAME);
    assertNotNull(config);
    assertFalse(config.isFailedOut());
    assertTrue(config.getPeerClusters().isEmpty());
  }

  @Test
  public void testRemoveClusterConfig()
  {
    testModifyClusterConfig();

    _clusterRegistry.put(CLUSTER_NAME, createClusterStoreProperties(false, false, Collections.emptySet()));
    assertNull(_clusterFailoutConfigProvider.getFailoutConfig(CLUSTER_NAME));
  }

  private static class TestingZKClusterFailoutConfigProvider extends ZKClusterFailoutConfigProvider
  {

    public TestingZKClusterFailoutConfigProvider(@Nonnull LoadBalancerState loadBalancerState)
    {
      super(loadBalancerState);
    }

    @Nullable
    @Override
    public TestingClusterFailoutConfig createFailoutConfig(@Nonnull String clusterName, @Nullable ClusterFailoutProperties clusterFailoutProperties)
    {
      if (clusterFailoutProperties == null)
      {
        return null;
      }

      Set<String> peerClusters = clusterFailoutProperties.getClusterFailoutRedirectConfigs().stream().map(config -> config.get("peer").toString())
        .collect(Collectors.toSet());

      if (clusterFailoutProperties.getClusterFailoutBucketConfigs().isEmpty())
      {
        return new TestingClusterFailoutConfig(false, peerClusters);
      }

      if (clusterFailoutProperties.getClusterFailoutRedirectConfigs() != null)
      {
        return new TestingClusterFailoutConfig(true, peerClusters);
      }

      return null;
    }
  }

  private ClusterStoreProperties createClusterStoreProperties(boolean hasClusterFailoutConfig, boolean isFailedOut, Set<String> peerClusters)
  {
    ClusterFailoutProperties properties = null;
    if (hasClusterFailoutConfig)
    {
      List<Map<String, Object>> redirectConfigs = new ArrayList<>();
      peerClusters.forEach(cluster -> redirectConfigs.add(Collections.singletonMap("peer", cluster)));
      if (!isFailedOut)
      {
        properties = new ClusterFailoutProperties(redirectConfigs, Collections.emptyList());
      }
      else
      {
        properties = new ClusterFailoutProperties(redirectConfigs, Collections.singletonList(Collections.emptyMap()));
      }
    }
    return new ClusterStoreProperties(new ClusterProperties(CLUSTER_NAME), null, null, properties);
  }

  private static class TestingClusterFailoutConfig implements ClusterFailoutConfig
  {
    private final boolean _isFailedOut;
    private final Set<String> _peerClusters;

    public TestingClusterFailoutConfig(boolean isFailedOut, Set<String> peerClusters)
    {
      _isFailedOut = isFailedOut;
      _peerClusters = peerClusters;
    }

    @Override
    public boolean isFailedOut()
    {
      return _isFailedOut;
    }

    @Override
    public Set<String> getPeerClusters()
    {
      return _peerClusters;
    }
  }
}

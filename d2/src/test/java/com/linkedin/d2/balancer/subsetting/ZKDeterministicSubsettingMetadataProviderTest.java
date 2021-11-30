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
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;


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
}

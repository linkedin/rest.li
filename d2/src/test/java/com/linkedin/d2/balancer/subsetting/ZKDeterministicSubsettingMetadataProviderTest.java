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
  private static final URI NODE_URI = URI.create("http://cluster-1/test2");

  private ScheduledExecutorService _executorService;
  private MockStore<UriProperties> _uriRegistry;
  private MockStore<ClusterProperties>                                             _clusterRegistry;
  private MockStore<ServiceProperties>                                             _serviceRegistry;
  private Map<String, TransportClientFactory>                                      _clientFactories;
  private Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> _loadBalancerStrategyFactories;
  private SimpleLoadBalancerState _state;
  private SSLContext _sslContext;
  private SSLParameters _sslParameters;

  private ZKDeterministicSubsettingMetadataProvider _metadataProvider;

  private static final SslSessionValidatorFactory SSL_SESSION_VALIDATOR_FACTORY =
      validationStrings -> sslSession -> {
        if (validationStrings == null || validationStrings.isEmpty())
        {
          throw new SslSessionNotTrustedException("no validation string");
        }
      };

  @BeforeMethod
  public void setUp()
  {
    _executorService = new SynchronousExecutorService();
    _uriRegistry = new MockStore<>();
    _clusterRegistry = new MockStore<>();
    _serviceRegistry = new MockStore<>();
    _clientFactories = new HashMap<>();
    _loadBalancerStrategyFactories = new HashMap<>();
    _loadBalancerStrategyFactories.put("random", new RandomLoadBalancerStrategyFactory());

    try {
      _sslContext = SSLContext.getDefault();
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new RuntimeException(e);
    }

    _sslParameters = new SSLParameters();
    _clientFactories.put("https", new SimpleLoadBalancerTest.DoNothingClientFactory());
    _state =
        new SimpleLoadBalancerState(_executorService,
            new PropertyEventBusImpl<>(_executorService, _uriRegistry),
            new PropertyEventBusImpl<>(_executorService, _clusterRegistry),
            new PropertyEventBusImpl<>(_executorService, _serviceRegistry),
            _clientFactories,
            _loadBalancerStrategyFactories,
            _sslContext,
            _sslParameters,
            true, null,
            SSL_SESSION_VALIDATOR_FACTORY);

    _metadataProvider = new ZKDeterministicSubsettingMetadataProvider(CLUSTER_NAME, NODE_URI, _state, 1000, TimeUnit.MILLISECONDS);
  }

  @Test
  public void testGetSubsettingMetadata()
  {
    List<String> schemes = new ArrayList<String>();
    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();
    for (int i = 0; i < 10; i++)
    {
      uriData.put(URI.create("http://cluster-1/test" + i), partitionData);
    }
    schemes.add("http");

    _state.listenToCluster("cluster-1", new LoadBalancerState.NullStateListenerCallback());
    _state.listenToService("service-1", new LoadBalancerState.NullStateListenerCallback());
    _clusterRegistry.put("cluster-1", new ClusterProperties("cluster-1", schemes));
    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));
    _serviceRegistry.put("service-1", new ServiceProperties("service-1",
        "cluster-1",
        "/test", Collections.singletonList("random")));

    DeterministicSubsettingMetadata metadata = _metadataProvider.getSubsettingMetadata();

    assertEquals(metadata.getInstanceId(), 2);
    assertEquals(metadata.getTotalInstanceCount(), 10);

    uriData.remove(URI.create("http://cluster-1/test0"));
    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    metadata = _metadataProvider.getSubsettingMetadata();
    assertEquals(metadata.getInstanceId(), 1);
    assertEquals(metadata.getTotalInstanceCount(), 9);

    uriData.remove(URI.create("http://cluster-1/test2"));
    _uriRegistry.put("cluster-1", new UriProperties("cluster-1", uriData));

    metadata = _metadataProvider.getSubsettingMetadata();
    assertNull(metadata);
  }
}

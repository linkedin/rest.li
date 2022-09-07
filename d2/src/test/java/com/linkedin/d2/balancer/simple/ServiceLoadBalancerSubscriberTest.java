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

package com.linkedin.d2.balancer.simple;

import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.properties.CanaryDistributionStrategy;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServiceStoreProperties;
import com.linkedin.d2.balancer.util.canary.CanaryDistributionProvider;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


/**
 * Test the behavior of {@link ServiceLoadBalancerSubscriber}
 */
public class ServiceLoadBalancerSubscriberTest
{
  private static final String CLUSTER_NAME = "testCluster";
  private static final String SERVICE_NAME = "testService";
  private static final String PATH = "/foo";

  private static final class ServiceLoadBalancerSubscriberFixture
  {
    @Mock
    SimpleLoadBalancerState _simpleLoadBalancerState;
    @Mock
    CanaryDistributionProvider _canaryDistributionProvider;
    @Mock
    PropertyEventBus<ServiceProperties> _eventBus;
    @Mock
    AtomicLong _version;
    @Captor
    private ArgumentCaptor<ServiceProperties> _refreshServiceStrategyPropertiesArgCaptor;

    @Captor
    private ArgumentCaptor<ServiceProperties> _refreshClientsPropertiesArgCaptor;

    @Captor
    private ArgumentCaptor<LoadBalancerStateItem<ServiceProperties>> _servicePropertiesUpdateArgsCaptor;

    Map<String, LoadBalancerStateItem<ServiceProperties>> _serviceProperties;
    Map<String, Set<String>> _servicesPerCluster;

    ServiceLoadBalancerSubscriberFixture() {
      MockitoAnnotations.initMocks(this);
      _serviceProperties = new HashMap<>();
      _servicesPerCluster = new HashMap<>();
      _version = new AtomicLong(0);
    }

    ServiceLoadBalancerSubscriber getMockSubscriber(boolean hasCanaryProvider) {
      if (hasCanaryProvider)
      {
        when(_simpleLoadBalancerState.getCanaryDistributionProvider()).thenReturn(_canaryDistributionProvider);
      }
      else
      {
        when(_simpleLoadBalancerState.getCanaryDistributionProvider()).thenReturn(null);
      }
      when(_simpleLoadBalancerState.getServiceProperties()).thenReturn(_serviceProperties);
      when(_simpleLoadBalancerState.getServicesPerCluster()).thenReturn(_servicesPerCluster);
      when(_simpleLoadBalancerState.getVersionAccess()).thenReturn(_version);
      doNothing().when(_simpleLoadBalancerState).notifyListenersOnServicePropertiesUpdates(
          _servicePropertiesUpdateArgsCaptor.capture());
      doNothing().when(_simpleLoadBalancerState).refreshServiceStrategies(
          _refreshServiceStrategyPropertiesArgCaptor.capture());
      doNothing().when(_simpleLoadBalancerState).refreshClients(_refreshClientsPropertiesArgCaptor.capture());
      return new ServiceLoadBalancerSubscriber(_eventBus, _simpleLoadBalancerState);
    }
  }

  @Test
  public void testHandleRemove()
  {
    String serviceName = "mock-service-foo";
    String clusterName = "mock-cluster-foo";
    ServiceLoadBalancerSubscriberFixture fixture = new ServiceLoadBalancerSubscriberFixture();
    LoadBalancerStateItem<ServiceProperties> servicePropertiesToRemove = new LoadBalancerStateItem<>(
        new ServiceProperties(serviceName, clusterName, "MockPath", new ArrayList<>(Arrays.asList("foo", "bar"))),
        0, 0);

    fixture._serviceProperties.put(serviceName, servicePropertiesToRemove);
    fixture.getMockSubscriber(false).handleRemove(serviceName);

    Assert.assertEquals(fixture._simpleLoadBalancerState.getServiceProperties().size(), 0);
    verify(
        fixture._simpleLoadBalancerState,
        times(1)
    ).notifyListenersOnServicePropertiesRemovals(servicePropertiesToRemove);
    verify(
        fixture._simpleLoadBalancerState,
        times(1)
    ).shutdownClients(serviceName);
  }

  /**
   * Provide objects with the structure:
   * {
   *   ServiceProperties -- stable configs,
   *   ServiceProperties -- canary configs,
   *   CanaryDistributionStrategy -- distribution strategy,
   *   CanaryDistributionProvider.Distribution -- distribution result (stable or canary)
   * }
   */
  @DataProvider(name = "getConfigsAndDistributions")
  public Object[][] getConfigsAndDistributions()
  {
    ServiceProperties stableConfigs = new ServiceProperties(SERVICE_NAME, CLUSTER_NAME, PATH, Collections.singletonList("aa"));
    ServiceProperties canaryConfigs = new ServiceProperties(SERVICE_NAME, CLUSTER_NAME, PATH, Collections.singletonList("bb"));
    CanaryDistributionStrategy dummyDistributionStrategy = new CanaryDistributionStrategy("any", Collections.emptyMap(),
                                                                                          Collections.emptyMap(), Collections.emptyMap());
    return new Object[][] {
        {stableConfigs, null, null, null}, // no canary configs and no distribution strategy
        {stableConfigs, canaryConfigs, null, null}, // no distribution strategy
        {stableConfigs, canaryConfigs, dummyDistributionStrategy, null}, // no distribution provider
        {stableConfigs, canaryConfigs, dummyDistributionStrategy, CanaryDistributionProvider.Distribution.STABLE},
        {stableConfigs, canaryConfigs, dummyDistributionStrategy, CanaryDistributionProvider.Distribution.CANARY}
    };
  }
  @Test(dataProvider = "getConfigsAndDistributions")
  public void testWithCanaryConfigs(ServiceProperties stableConfigs, ServiceProperties canaryConfigs, CanaryDistributionStrategy distributionStrategy,
                                    CanaryDistributionProvider.Distribution distribution)
  {
    ServiceLoadBalancerSubscriberFixture fixture = new ServiceLoadBalancerSubscriberFixture();
    when(fixture._canaryDistributionProvider.distribute(any())).thenReturn(distribution);
    fixture.getMockSubscriber(distribution != null).handlePut(SERVICE_NAME,
                                                                              new ServiceStoreProperties(stableConfigs, canaryConfigs, distributionStrategy));

    ServiceProperties expectedPickedProperties = distribution == CanaryDistributionProvider.Distribution.CANARY ? canaryConfigs : stableConfigs;
    Assert.assertEquals(fixture._servicePropertiesUpdateArgsCaptor.getValue().getProperty(), expectedPickedProperties);
    Assert.assertEquals(
        fixture._servicePropertiesUpdateArgsCaptor.getValue().getDistribution(),
        distribution == null ? CanaryDistributionProvider.Distribution.STABLE : distribution);
    Assert.assertEquals(fixture._refreshClientsPropertiesArgCaptor.getValue(), expectedPickedProperties);
    Assert.assertEquals(fixture._refreshClientsPropertiesArgCaptor.getValue(), expectedPickedProperties);
    Assert.assertEquals(fixture._serviceProperties.get(SERVICE_NAME).getProperty(), expectedPickedProperties);
  }
}

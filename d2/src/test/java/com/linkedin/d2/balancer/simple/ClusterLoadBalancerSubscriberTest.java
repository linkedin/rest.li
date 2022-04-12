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
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterStoreProperties;
import com.linkedin.d2.balancer.properties.FailoutProperties;
import com.linkedin.d2.balancer.util.canary.CanaryDistributionProvider;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;


/**
 * Test the behavior of {@link ClusterLoadBalancerSubscriber}
 */
public class ClusterLoadBalancerSubscriberTest
{
  private static final String CLUSTER_NAME = "testCluster";

  private static final class ClusterLoadBalancerSubscriberFixture
  {
    @Mock
    SimpleLoadBalancerState _simpleLoadBalancerState;
    @Mock
    CanaryDistributionProvider _canaryDistributionProvider;
    @Mock
    PropertyEventBus<ClusterProperties> _eventBus;
    @Mock
    AtomicLong _version;

    Map<String, ClusterInfoItem> _clusterInfo;

    ClusterLoadBalancerSubscriberFixture() {
      MockitoAnnotations.initMocks(this);
      _clusterInfo = new HashMap<>();
      _version = new AtomicLong(0);
    }

    ClusterLoadBalancerSubscriber getMockSubscriber(boolean hasCanaryProvider)
    {
      if (hasCanaryProvider) {
        when(_simpleLoadBalancerState.getCanaryDistributionProvider()).thenReturn(_canaryDistributionProvider);
      } else {
        when(_simpleLoadBalancerState.getCanaryDistributionProvider()).thenReturn(null);
      }

      when(_simpleLoadBalancerState.getClusterInfo()).thenReturn(_clusterInfo);
      when(_simpleLoadBalancerState.getVersionAccess()).thenReturn(_version);
      doNothing().when(_simpleLoadBalancerState).notifyClusterListenersOnAdd(any());
      return new ClusterLoadBalancerSubscriber(_simpleLoadBalancerState, _eventBus, null);
    }
  }

  /**
   * Provide objects with the structure:
   * {
   *   ClusterProperties -- stable configs,
   *   ClusterProperties -- canary configs,
   *   CanaryDistributionStrategy -- distribution strategy,
   *   CanaryDistributionProvider.Distribution -- distribution result (stable or canary)
   * }
   */
  @DataProvider(name = "getConfigsAndDistributions")
  public Object[][] getConfigsAndDistributions()
  {
    ClusterProperties stableConfigs = new ClusterProperties(CLUSTER_NAME, Collections.singletonList("aa"));
    ClusterProperties canaryConfigs = new ClusterProperties(CLUSTER_NAME, Collections.singletonList("bb"));
    List<Map<String, Object>> emptyList =  new ArrayList<Map<String, Object>>();
    emptyList.add(Collections.emptyMap());
    FailoutProperties failoutProperties = new FailoutProperties(emptyList, emptyList);
    CanaryDistributionStrategy dummyDistributionStrategy = new CanaryDistributionStrategy("any", Collections.emptyMap(),
        Collections.emptyMap(), Collections.emptyMap());
    return new Object[][] {
        {stableConfigs, null, null, null, null}, // no canary configs and no distribution strategy and no failout properties
        {stableConfigs, canaryConfigs, null, null, null}, // no distribution strategy, no failout properties
        {stableConfigs, canaryConfigs, dummyDistributionStrategy, null, null}, // no distribution provider, no failout properties
        {stableConfigs, canaryConfigs, dummyDistributionStrategy, CanaryDistributionProvider.Distribution.STABLE, null},
        {stableConfigs, canaryConfigs, dummyDistributionStrategy, CanaryDistributionProvider.Distribution.CANARY, null},
        {stableConfigs, canaryConfigs, dummyDistributionStrategy, CanaryDistributionProvider.Distribution.STABLE, failoutProperties},
    };
  }
  @Test(dataProvider = "getConfigsAndDistributions")
  public void testWithCanaryConfigs(ClusterProperties stableConfigs, ClusterProperties canaryConfigs, CanaryDistributionStrategy distributionStrategy,
      CanaryDistributionProvider.Distribution distribution, FailoutProperties failoutProperties)
  {
    ClusterLoadBalancerSubscriberFixture fixture = new ClusterLoadBalancerSubscriberFixture();
    when(fixture._canaryDistributionProvider.distribute(any())).thenReturn(distribution);
    fixture.getMockSubscriber(distribution != null).handlePut(CLUSTER_NAME,
        new ClusterStoreProperties(stableConfigs, canaryConfigs, distributionStrategy, failoutProperties));

    Assert.assertEquals(fixture._clusterInfo.get(CLUSTER_NAME).getClusterPropertiesItem().getProperty(),
        distribution == CanaryDistributionProvider.Distribution.CANARY ? canaryConfigs : stableConfigs);
  }

  @DataProvider(name = "getConfigsWithFailoutProperties")
  public Object[][] getConfigsWithFailoutProperties()
  {
    ClusterProperties stableConfigs = new ClusterProperties(CLUSTER_NAME, Collections.singletonList("aa"));

    return new Object[][] {
      {stableConfigs, null},
      {stableConfigs, new FailoutProperties(Collections.emptyList(), Collections.emptyList())},
    };
  }
  @Test(dataProvider = "getConfigsWithFailoutProperties")
  public void testWithFailoutConfigs(ClusterProperties stableConfigs, FailoutProperties clusterFailoutProperties)
  {
    ClusterLoadBalancerSubscriberFixture fixture = new ClusterLoadBalancerSubscriberFixture();
    fixture.getMockSubscriber(false).handlePut(CLUSTER_NAME, new ClusterStoreProperties(
      stableConfigs, null, null, clusterFailoutProperties));

    LoadBalancerStateItem<FailoutProperties> failoutPropertiesItem = fixture._clusterInfo.get(CLUSTER_NAME).getFailoutPropertiesItem();
    Assert.assertNotNull(failoutPropertiesItem);
    Assert.assertEquals(failoutPropertiesItem.getProperty(), clusterFailoutProperties);
  }
}

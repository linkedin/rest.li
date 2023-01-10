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

package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.simple.ClusterInfoItem;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.util.canary.CanaryDistributionProvider;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import org.mockito.Captor;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


public class D2ClientJmxManagerTest {
  @Mock
  SimpleLoadBalancerState _simpleLoadBalancerState;
  @Mock
  JmxManager _jmxManager;
  @Captor
  ArgumentCaptor<String> _simpleLoadBalancerStateNameCaptor;
  @Captor
  ArgumentCaptor<SimpleLoadBalancerState> _simpleLoadBalancerStateCaptor;
  @Captor
  ArgumentCaptor<SimpleLoadBalancerState.SimpleLoadBalancerStateListener> _simpleLoadBalancerStateListenerCaptor;
  @Captor
  ArgumentCaptor<String> _unregisteredObjectNameCaptor;
  @Captor
  ArgumentCaptor<String> _registerObjectNameCaptor;
  @Captor
  ArgumentCaptor<ClusterInfoItem> _clusterInfoArgumentCaptor;
  @Captor
  ArgumentCaptor<LoadBalancerStateItem<ServiceProperties>> _servicePropertiesArgumentCaptor;

  D2ClientJmxManager _d2ClientJmxManager;
  private ClusterInfoItem _clusterInfoItem;
  private ClusterInfoItem _noPropertyClusterInfoItem;
  private LoadBalancerStateItem<ServiceProperties> _servicePropertiesLBState;
  private LoadBalancerStateItem<ServiceProperties> _noPropertyLBStateItem;

  @BeforeMethod
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);
    AtomicLong version = new AtomicLong(0);
    when(_simpleLoadBalancerState.getVersionAccess()).thenReturn(version);
    _clusterInfoItem =
        new ClusterInfoItem(_simpleLoadBalancerState, new ClusterProperties("C_Foo"), new PartitionAccessor() {
          @Override
          public int getMaxPartitionId() {
            return 0;
          }

          @Override
          public int getPartitionId(URI uri) {
            return 0;
          }
        }, CanaryDistributionProvider.Distribution.CANARY);
    _noPropertyClusterInfoItem = new ClusterInfoItem(_simpleLoadBalancerState, null, null,
        CanaryDistributionProvider.Distribution.STABLE);
    _servicePropertiesLBState = new LoadBalancerStateItem<>(
        new ServiceProperties("S_Foo", "Bar", "/", Collections.singletonList("Random")),
        0,
        0,
        CanaryDistributionProvider.Distribution.CANARY
    );
    _noPropertyLBStateItem = new LoadBalancerStateItem<ServiceProperties>(null, 0, 0,
        CanaryDistributionProvider.Distribution.STABLE);
    _d2ClientJmxManager = new D2ClientJmxManager("Foo", _jmxManager);
    Mockito.doReturn(_jmxManager).when(_jmxManager).unregister(_unregisteredObjectNameCaptor.capture());
    Mockito.doReturn(_jmxManager).when(_jmxManager).registerLoadBalancerState(
        _simpleLoadBalancerStateNameCaptor.capture(), _simpleLoadBalancerStateCaptor.capture());
    Mockito.doReturn(_jmxManager).when(_jmxManager).registerClusterInfo(
        _registerObjectNameCaptor.capture(),
        _clusterInfoArgumentCaptor.capture());
    Mockito.doReturn(_jmxManager).when(_jmxManager).registerServiceProperties(
        _registerObjectNameCaptor.capture(),
        _servicePropertiesArgumentCaptor.capture());
    Mockito.doNothing().when(_simpleLoadBalancerState).register(_simpleLoadBalancerStateListenerCaptor.capture());
  }

  @Test()
  public void testSetSimpleLBStateListenerUpdateServiceProperties()
  {
    _d2ClientJmxManager.setSimpleLoadBalancerState(_simpleLoadBalancerState);
    _simpleLoadBalancerStateListenerCaptor.getValue().onServicePropertiesUpdate(null);
    Mockito.verify(_jmxManager, never()).registerServiceProperties(any(), any());
    _simpleLoadBalancerStateListenerCaptor.getValue().onServicePropertiesUpdate(_noPropertyLBStateItem);
    Mockito.verify(_jmxManager, never()).registerServiceProperties(any(), any());

    _simpleLoadBalancerStateListenerCaptor.getValue().onServicePropertiesUpdate(_servicePropertiesLBState);
    Assert.assertEquals(
        _registerObjectNameCaptor.getValue(),
        "S_Foo-ServiceProperties"
    );
    Assert.assertEquals(
        _servicePropertiesArgumentCaptor.getValue(),
        _servicePropertiesLBState
    );
  }

  @Test
  public void testSetSimpleLBStateListenerUpdateClusterInfo()
  {
    _d2ClientJmxManager.setSimpleLoadBalancerState(_simpleLoadBalancerState);
    _simpleLoadBalancerStateListenerCaptor.getValue().onClusterInfoUpdate(null);
    Mockito.verify(_jmxManager, never()).registerClusterInfo(any(), any());
    _simpleLoadBalancerStateListenerCaptor.getValue().onClusterInfoUpdate(_noPropertyClusterInfoItem);
    Mockito.verify(_jmxManager, never()).registerClusterInfo(any(), any());

    _simpleLoadBalancerStateListenerCaptor.getValue().onClusterInfoUpdate(_clusterInfoItem);
    Assert.assertEquals(
        _registerObjectNameCaptor.getValue(),
        "C_Foo-ClusterInfo"
    );
    Assert.assertEquals(
        _clusterInfoArgumentCaptor.getValue(),
        _clusterInfoItem
    );
  }

  @Test
  public void testSetSimpleLBStateListenerRemoveClusterInfo()
  {
    _d2ClientJmxManager.setSimpleLoadBalancerState(_simpleLoadBalancerState);
    Assert.assertEquals(_simpleLoadBalancerStateNameCaptor.getValue(), "Foo-LoadBalancerState");
    Assert.assertEquals(_simpleLoadBalancerStateCaptor.getValue(), _simpleLoadBalancerState);
    _simpleLoadBalancerStateListenerCaptor.getValue().onClusterInfoRemoval(null);
    Mockito.verify(_jmxManager, never()).unregister(anyString());
    _simpleLoadBalancerStateListenerCaptor.getValue().onClusterInfoRemoval(_noPropertyClusterInfoItem);
    Mockito.verify(_jmxManager, never()).unregister(anyString());

    _simpleLoadBalancerStateListenerCaptor.getValue().onClusterInfoRemoval(_clusterInfoItem);
    Assert.assertEquals(
        _unregisteredObjectNameCaptor.getValue(),
        _clusterInfoItem.getClusterPropertiesItem().getProperty().getClusterName() + "-ClusterInfo");
  }

  @Test
  public void testSetSimpleLBStateListenerRemoveServiceProperties()
  {
    _d2ClientJmxManager.setSimpleLoadBalancerState(_simpleLoadBalancerState);
    Assert.assertEquals(_simpleLoadBalancerStateNameCaptor.getValue(), "Foo-LoadBalancerState");
    Assert.assertEquals(_simpleLoadBalancerStateCaptor.getValue(), _simpleLoadBalancerState);
    _simpleLoadBalancerStateListenerCaptor.getValue().onServicePropertiesRemoval(null);
    Mockito.verify(_jmxManager, never()).unregister(anyString());
    _simpleLoadBalancerStateListenerCaptor.getValue().onServicePropertiesRemoval(_noPropertyLBStateItem);
    Mockito.verify(_jmxManager, never()).unregister(anyString());

    _simpleLoadBalancerStateListenerCaptor.getValue().onServicePropertiesRemoval(_servicePropertiesLBState);
    Assert.assertEquals(
        _unregisteredObjectNameCaptor.getValue(),
        _servicePropertiesLBState.getProperty().getServiceName() + "-ServiceProperties");
  }
}

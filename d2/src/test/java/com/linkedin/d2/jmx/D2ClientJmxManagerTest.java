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
import com.linkedin.d2.balancer.dualread.DualReadModeProvider;
import com.linkedin.d2.balancer.dualread.DualReadStateManager;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.simple.ClusterInfoItem;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategy;
import com.linkedin.d2.balancer.util.canary.CanaryDistributionProvider;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.discovery.stores.file.FileStore;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import org.mockito.Captor;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


public class D2ClientJmxManagerTest {

  private static final LoadBalancerStateItem<ServiceProperties> SERVICE_PROPERTIES_LOAD_BALANCER_STATE_ITEM = new LoadBalancerStateItem<>(
      new ServiceProperties("S_Foo", "Bar", "/", Collections.singletonList("Random")),
      0,
      0,
  CanaryDistributionProvider.Distribution.CANARY
  );

  private static final LoadBalancerStateItem<ServiceProperties> NO_PROPERTY_LB_STATE_ITEM = new LoadBalancerStateItem<>(
      null, 0, 0, CanaryDistributionProvider.Distribution.STABLE);

  private static final LoadBalancerStateItem<ServiceProperties> UPDATED_SERVICE_PROPERTIES_LB_STATE_ITEM = new LoadBalancerStateItem<>(
      new ServiceProperties("S_Foo", "Bar", "/", Collections.singletonList("Random")),
      0,
      0,
      CanaryDistributionProvider.Distribution.STABLE
  );

  private static final class D2ClientJmxManagerFixture
  {
    @Mock
    SimpleLoadBalancer _loadBalancer;
    @Mock
    SimpleLoadBalancerState _simpleLoadBalancerState;
    @Mock
    JmxManager _jmxManager;
    @Mock
    FileStore<UriProperties> _uriStore;
    @Mock
    FileStore<ClusterProperties> _clusterStore;
    @Mock
    FileStore<ServiceProperties> _serviceStore;
    @Mock
    RelativeLoadBalancerStrategy _relativeLoadBalancerStrategy;
    @Mock
    DualReadModeProvider _dualReadModeProvider;
    @Mock
    ScheduledExecutorService _executorService;
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
    @SuppressWarnings("rawtypes")
    @Captor
    ArgumentCaptor<D2ClientJmxManager.D2ClientJmxDualReadModeWatcher> _addWatcherCaptor;

    D2ClientJmxManager _d2ClientJmxManager;
    private final ClusterInfoItem _clusterInfoItem;
    private final ClusterInfoItem _updatedClusterInfoItem;
    private final ClusterInfoItem _noPropertyClusterInfoItem;
    private final DualReadStateManager _dualReadStateManager;

    D2ClientJmxManagerFixture()
    {
      MockitoAnnotations.initMocks(this);
      AtomicLong version = new AtomicLong(0);
      when(_simpleLoadBalancerState.getVersionAccess()).thenReturn(version);
      PartitionAccessor partitionAccessor = new PartitionAccessor() {
        @Override
        public int getMaxPartitionId() {
          return 0;
        }

        @Override
        public int getPartitionId(URI uri) {
          return 0;
        }
      };
      _clusterInfoItem =
          new ClusterInfoItem(_simpleLoadBalancerState, new ClusterProperties("C_Foo"), partitionAccessor,
              CanaryDistributionProvider.Distribution.CANARY);
      _updatedClusterInfoItem =
          new ClusterInfoItem(_simpleLoadBalancerState, new ClusterProperties("C_Foo"), partitionAccessor,
              CanaryDistributionProvider.Distribution.STABLE);
      _noPropertyClusterInfoItem = new ClusterInfoItem(_simpleLoadBalancerState, null, null,
          CanaryDistributionProvider.Distribution.STABLE);
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

      _dualReadStateManager = spy(new DualReadStateManager(_dualReadModeProvider, _executorService));

      doCallRealMethod().when(_dualReadStateManager).addGlobalWatcher(any());
      doCallRealMethod().when(_dualReadStateManager).addServiceWatcher(any(), any());
      doCallRealMethod().when(_dualReadStateManager).addClusterWatcher(any(), any());
      doCallRealMethod().when(_dualReadStateManager).updateGlobal(any());
      doCallRealMethod().when(_dualReadStateManager).updateService(any(), any());
      doCallRealMethod().when(_dualReadStateManager).updateCluster(any(), any());
    }

    D2ClientJmxManager getD2ClientJmxManager(String prefix, D2ClientJmxManager.DiscoverySourceType sourceType, Boolean isDualReadLB)
    {
      if (sourceType == null)
      { // default to ZK source type, null dualReadStateManager
        _d2ClientJmxManager = new D2ClientJmxManager(prefix, _jmxManager);
      }
      else
      {
        _d2ClientJmxManager = new D2ClientJmxManager(prefix, _jmxManager, sourceType, isDualReadLB ? _dualReadStateManager : null);
      }
      return _d2ClientJmxManager;
    }
  }

  @DataProvider(name = "nonDualReadD2ClientJmxManagers")
  public Object[][] nonDualReadD2ClientJmxManagers()
  {
    return new Object[][]
        {
            {"Foo", null, false},
            {"Foo", D2ClientJmxManager.DiscoverySourceType.ZK, false},
            {"Foo", D2ClientJmxManager.DiscoverySourceType.XDS, false}
        };
  }

  @Test(dataProvider = "nonDualReadD2ClientJmxManagers")
  public void testSetSimpleLBStateListenerUpdateServiceProperties(String prefix, D2ClientJmxManager.DiscoverySourceType sourceType,
      Boolean isDualReadLB)
  {
    D2ClientJmxManagerFixture fixture = new D2ClientJmxManagerFixture();
    D2ClientJmxManager d2ClientJmxManager = fixture.getD2ClientJmxManager(prefix, sourceType, isDualReadLB);

    d2ClientJmxManager.setSimpleLoadBalancerState(fixture._simpleLoadBalancerState);
    fixture._simpleLoadBalancerStateListenerCaptor.getValue().onServicePropertiesUpdate(null);
    Mockito.verify(fixture._jmxManager, never()).registerServiceProperties(any(), any());
    fixture._simpleLoadBalancerStateListenerCaptor.getValue().onServicePropertiesUpdate(NO_PROPERTY_LB_STATE_ITEM);
    Mockito.verify(fixture._jmxManager, never()).registerServiceProperties(any(), any());

    fixture._simpleLoadBalancerStateListenerCaptor.getValue().onServicePropertiesUpdate(
        SERVICE_PROPERTIES_LOAD_BALANCER_STATE_ITEM);
    Assert.assertEquals(
        fixture._registerObjectNameCaptor.getValue(),
        "S_Foo-ServiceProperties"
    );
    Assert.assertEquals(
        fixture._servicePropertiesArgumentCaptor.getValue(), SERVICE_PROPERTIES_LOAD_BALANCER_STATE_ITEM
    );
  }

  @Test(dataProvider = "nonDualReadD2ClientJmxManagers")
  public void testSetSimpleLBStateListenerUpdateClusterInfo(String prefix, D2ClientJmxManager.DiscoverySourceType sourceType,
      Boolean isDualReadLB)
  {
    D2ClientJmxManagerFixture fixture = new D2ClientJmxManagerFixture();
    D2ClientJmxManager d2ClientJmxManager = fixture.getD2ClientJmxManager(prefix, sourceType, isDualReadLB);

    d2ClientJmxManager.setSimpleLoadBalancerState(fixture._simpleLoadBalancerState);
    fixture._simpleLoadBalancerStateListenerCaptor.getValue().onClusterInfoUpdate(null);
    Mockito.verify(fixture._jmxManager, never()).registerClusterInfo(any(), any());
    fixture._simpleLoadBalancerStateListenerCaptor.getValue().onClusterInfoUpdate(fixture._noPropertyClusterInfoItem);
    Mockito.verify(fixture._jmxManager, never()).registerClusterInfo(any(), any());

    fixture._simpleLoadBalancerStateListenerCaptor.getValue().onClusterInfoUpdate(fixture._clusterInfoItem);
    Assert.assertEquals(
        fixture._registerObjectNameCaptor.getValue(),
        "C_Foo-ClusterInfo"
    );
    Assert.assertEquals(
        fixture._clusterInfoArgumentCaptor.getValue(),
        fixture._clusterInfoItem
    );
  }

  @Test(dataProvider = "nonDualReadD2ClientJmxManagers")
  public void testSetSimpleLBStateListenerRemoveClusterInfo(String prefix, D2ClientJmxManager.DiscoverySourceType sourceType,
      Boolean isDualReadLB)
  {
    D2ClientJmxManagerFixture fixture = new D2ClientJmxManagerFixture();
    D2ClientJmxManager d2ClientJmxManager = fixture.getD2ClientJmxManager(prefix, sourceType, isDualReadLB);

    d2ClientJmxManager.setSimpleLoadBalancerState(fixture._simpleLoadBalancerState);
    Assert.assertEquals(fixture._simpleLoadBalancerStateNameCaptor.getValue(), "Foo-LoadBalancerState");
    Assert.assertEquals(fixture._simpleLoadBalancerStateCaptor.getValue(), fixture._simpleLoadBalancerState);
    fixture._simpleLoadBalancerStateListenerCaptor.getValue().onClusterInfoRemoval(null);
    Mockito.verify(fixture._jmxManager, never()).unregister(anyString());
    fixture._simpleLoadBalancerStateListenerCaptor.getValue().onClusterInfoRemoval(fixture._noPropertyClusterInfoItem);
    Mockito.verify(fixture._jmxManager, never()).unregister(anyString());

    fixture._simpleLoadBalancerStateListenerCaptor.getValue().onClusterInfoRemoval(fixture._clusterInfoItem);
    Assert.assertEquals(
        fixture._unregisteredObjectNameCaptor.getValue(),
        fixture._clusterInfoItem.getClusterPropertiesItem().getProperty().getClusterName() + "-ClusterInfo");
  }

  @Test(dataProvider = "nonDualReadD2ClientJmxManagers")
  public void testSetSimpleLBStateListenerRemoveServiceProperties(String prefix, D2ClientJmxManager.DiscoverySourceType sourceType,
      Boolean isDualReadLB)
  {
    D2ClientJmxManagerFixture fixture = new D2ClientJmxManagerFixture();
    D2ClientJmxManager d2ClientJmxManager = fixture.getD2ClientJmxManager(prefix, sourceType, isDualReadLB);

    d2ClientJmxManager.setSimpleLoadBalancerState(fixture._simpleLoadBalancerState);
    Assert.assertEquals(fixture._simpleLoadBalancerStateNameCaptor.getValue(), "Foo-LoadBalancerState");
    Assert.assertEquals(fixture._simpleLoadBalancerStateCaptor.getValue(), fixture._simpleLoadBalancerState);
    fixture._simpleLoadBalancerStateListenerCaptor.getValue().onServicePropertiesRemoval(null);
    Mockito.verify(fixture._jmxManager, never()).unregister(anyString());
    fixture._simpleLoadBalancerStateListenerCaptor.getValue().onServicePropertiesRemoval(NO_PROPERTY_LB_STATE_ITEM);
    Mockito.verify(fixture._jmxManager, never()).unregister(anyString());

    fixture._simpleLoadBalancerStateListenerCaptor.getValue().onServicePropertiesRemoval(
        SERVICE_PROPERTIES_LOAD_BALANCER_STATE_ITEM);
    Assert.assertEquals(
        fixture._unregisteredObjectNameCaptor.getValue(),
        SERVICE_PROPERTIES_LOAD_BALANCER_STATE_ITEM.getProperty().getServiceName() + "-ServiceProperties");
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  public void testAddAndRemoveWatcherAtServicePropertiesUpdate()
  {
    D2ClientJmxManagerFixture fixture = new D2ClientJmxManagerFixture();
    D2ClientJmxManager d2ClientJmxManager = fixture.getD2ClientJmxManager("Foo", D2ClientJmxManager.DiscoverySourceType.XDS, true);
    // Initial dual read mode is ZK only.
    DualReadStateManager dualReadStateManager = fixture._dualReadStateManager;
    Mockito.doReturn(DualReadModeProvider.DualReadMode.OLD_LB_ONLY).when(dualReadStateManager).getGlobalDualReadMode();
    Mockito.doReturn(DualReadModeProvider.DualReadMode.OLD_LB_ONLY).when(dualReadStateManager).getServiceDualReadMode(any());
    Mockito.doReturn(DualReadModeProvider.DualReadMode.OLD_LB_ONLY).when(dualReadStateManager).getClusterDualReadMode(any());

    d2ClientJmxManager.setSimpleLoadBalancerState(fixture._simpleLoadBalancerState);
    SimpleLoadBalancerState.SimpleLoadBalancerStateListener lbStateListener = fixture._simpleLoadBalancerStateListenerCaptor.getValue();
    ArgumentCaptor<D2ClientJmxManager.D2ClientJmxDualReadModeWatcher> addWatcherCaptor = fixture._addWatcherCaptor;

    lbStateListener.onServicePropertiesUpdate(SERVICE_PROPERTIES_LOAD_BALANCER_STATE_ITEM);
    // Verify watcher is added with properties inside
    verify(dualReadStateManager).addServiceWatcher(eq("S_Foo"), addWatcherCaptor.capture());
    D2ClientJmxManager.D2ClientJmxDualReadModeWatcher<LoadBalancerStateItem<ServiceProperties>> watcher = addWatcherCaptor.getValue();
    Assert.assertEquals(watcher.getLatestJmxProperty(), SERVICE_PROPERTIES_LOAD_BALANCER_STATE_ITEM);

    lbStateListener.onServicePropertiesUpdate(UPDATED_SERVICE_PROPERTIES_LB_STATE_ITEM);
    // Verify watcher is not added again, and properties in the watcher is updated
    verify(dualReadStateManager, times(1)).addServiceWatcher(any(), any());
    Assert.assertEquals(watcher.getLatestJmxProperty(), UPDATED_SERVICE_PROPERTIES_LB_STATE_ITEM);

    // Verify watch is removed
    lbStateListener.onServicePropertiesRemoval(UPDATED_SERVICE_PROPERTIES_LB_STATE_ITEM);
    verify(dualReadStateManager).removeServiceWatcher(eq("S_Foo"), eq(watcher));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  public void testAddAndRemoveWatcherAtClusterInfoItemUpdate()
  {
    D2ClientJmxManagerFixture fixture = new D2ClientJmxManagerFixture();
    D2ClientJmxManager d2ClientJmxManager = fixture.getD2ClientJmxManager("Foo", D2ClientJmxManager.DiscoverySourceType.XDS, true);
    d2ClientJmxManager.setSimpleLoadBalancerState(fixture._simpleLoadBalancerState);
    // Initial dual read mode is ZK only.
    DualReadStateManager dualReadStateManager = fixture._dualReadStateManager;
    Mockito.doReturn(DualReadModeProvider.DualReadMode.OLD_LB_ONLY).when(dualReadStateManager).getGlobalDualReadMode();
    Mockito.doReturn(DualReadModeProvider.DualReadMode.OLD_LB_ONLY).when(dualReadStateManager).getServiceDualReadMode(any());
    Mockito.doReturn(DualReadModeProvider.DualReadMode.OLD_LB_ONLY).when(dualReadStateManager).getClusterDualReadMode(any());

    SimpleLoadBalancerState.SimpleLoadBalancerStateListener lbStateListener = fixture._simpleLoadBalancerStateListenerCaptor.getValue();
    ArgumentCaptor<D2ClientJmxManager.D2ClientJmxDualReadModeWatcher> addWatcherCaptor = fixture._addWatcherCaptor;

    lbStateListener.onClusterInfoUpdate(fixture._clusterInfoItem);

    // Verify watcher is added with properties inside
    verify(dualReadStateManager).addClusterWatcher(eq("C_Foo"), addWatcherCaptor.capture());
    D2ClientJmxManager.D2ClientJmxDualReadModeWatcher<ClusterInfoItem> watcher = addWatcherCaptor.getValue();
    Assert.assertEquals(watcher.getLatestJmxProperty(), fixture._clusterInfoItem);

    lbStateListener.onClusterInfoUpdate(fixture._updatedClusterInfoItem);
    // Verify watcher is not added again, and properties in the watcher is updated
    verify(dualReadStateManager, times(1)).addClusterWatcher(any(), any());
    Assert.assertEquals(watcher.getLatestJmxProperty(), fixture._updatedClusterInfoItem);

    // Verify watch is removed
    lbStateListener.onClusterInfoRemoval(fixture._updatedClusterInfoItem);
    verify(dualReadStateManager).removeClusterWatcher(eq("C_Foo"), eq(watcher));
  }

  @DataProvider(name = "sourceTypeAndDualReadModeForLixSwitch")
  public Object[][] sourceTypeAndDualReadModeForDualReadModeSwitch()
  {
    return new Object[][]
        {
            // ZK source is still primary switching OLD_LB_ONLY -> DUAL_READ
            {D2ClientJmxManager.DiscoverySourceType.ZK, DualReadModeProvider.DualReadMode.OLD_LB_ONLY,
                DualReadModeProvider.DualReadMode.DUAL_READ, true, true},
            // XDS source is still secondary switching OLD_LB_ONLY -> DUAL_READ
            {D2ClientJmxManager.DiscoverySourceType.XDS, DualReadModeProvider.DualReadMode.OLD_LB_ONLY,
                DualReadModeProvider.DualReadMode.DUAL_READ, false, false},
            // ZK source becomes secondary switching DUAL_READ -> NEW_LB_ONLY
            {D2ClientJmxManager.DiscoverySourceType.ZK, DualReadModeProvider.DualReadMode.DUAL_READ,
                DualReadModeProvider.DualReadMode.NEW_LB_ONLY, true, false},
            // XDS source becomes primary switching DUAL_READ -> NEW_LB_ONLY
            {D2ClientJmxManager.DiscoverySourceType.XDS, DualReadModeProvider.DualReadMode.DUAL_READ,
                DualReadModeProvider.DualReadMode.NEW_LB_ONLY, false, true},
            // ZK source becomes primary switching NEW_LB_ONLY -> DUAL_READ
            {D2ClientJmxManager.DiscoverySourceType.ZK, DualReadModeProvider.DualReadMode.NEW_LB_ONLY,
                DualReadModeProvider.DualReadMode.DUAL_READ, false, true},
            // XDS source becomes secondary switching NEW_LB_ONLY -> DUAL_READ
            {D2ClientJmxManager.DiscoverySourceType.XDS, DualReadModeProvider.DualReadMode.NEW_LB_ONLY,
                DualReadModeProvider.DualReadMode.DUAL_READ, true, false},
            // ZK source is still primary switching DUAL_READ -> OLD_LB_ONLY
            {D2ClientJmxManager.DiscoverySourceType.ZK, DualReadModeProvider.DualReadMode.DUAL_READ,
                DualReadModeProvider.DualReadMode.OLD_LB_ONLY, true, true},
            // XDS source is still secondary switching DUAL_READ -> OLD_LB_ONLY
            {D2ClientJmxManager.DiscoverySourceType.XDS, DualReadModeProvider.DualReadMode.DUAL_READ,
                DualReadModeProvider.DualReadMode.OLD_LB_ONLY, false, false},
            // ZK source is still primary switching NEW_LB_ONLY -> OLD_LB_ONLY
            {D2ClientJmxManager.DiscoverySourceType.ZK, DualReadModeProvider.DualReadMode.NEW_LB_ONLY,
                DualReadModeProvider.DualReadMode.OLD_LB_ONLY, false, true},
            // XDS source is still secondary switching NEW_LB_ONLY -> OLD_LB_ONLY
            {D2ClientJmxManager.DiscoverySourceType.XDS, DualReadModeProvider.DualReadMode.NEW_LB_ONLY,
                DualReadModeProvider.DualReadMode.OLD_LB_ONLY, true, false}
        };
  }
  @Test(dataProvider = "sourceTypeAndDualReadModeForLixSwitch")
  public void testJmxNamesOnDualReadModeSwitch(D2ClientJmxManager.DiscoverySourceType sourceType,
      DualReadModeProvider.DualReadMode oldMode, DualReadModeProvider.DualReadMode newMode, boolean isPrimaryBefore, boolean isPrimaryAfter)
  {
    D2ClientJmxManagerFixture fixture = new D2ClientJmxManagerFixture();
    D2ClientJmxManager d2ClientJmxManager = fixture.getD2ClientJmxManager("Foo", sourceType, true);

    DualReadStateManager dualReadStateManager = fixture._dualReadStateManager;
    dualReadStateManager.updateGlobal(oldMode);
    doReturn(oldMode).when(dualReadStateManager).getGlobalDualReadMode();
    doReturn(oldMode).when(dualReadStateManager).getServiceDualReadMode(any());
    doReturn(oldMode).when(dualReadStateManager).getClusterDualReadMode(any());

    d2ClientJmxManager.setSimpleLoadBalancer(fixture._loadBalancer);
    d2ClientJmxManager.setSimpleLoadBalancerState(fixture._simpleLoadBalancerState);
    SimpleLoadBalancerState.SimpleLoadBalancerStateListener lbStateListener = fixture._simpleLoadBalancerStateListenerCaptor.getValue();
    lbStateListener.onServicePropertiesUpdate(SERVICE_PROPERTIES_LOAD_BALANCER_STATE_ITEM);
    lbStateListener.onClusterInfoUpdate(fixture._clusterInfoItem);
    lbStateListener.onStrategyAdded("S_Foo", "https", fixture._relativeLoadBalancerStrategy);
    d2ClientJmxManager.setFsUriStore(fixture._uriStore);
    d2ClientJmxManager.setFsClusterStore(fixture._clusterStore);
    d2ClientJmxManager.setFsServiceStore(fixture._serviceStore);

    verifyJmxNames(fixture, sourceType, isPrimaryBefore, false);

    doReturn(newMode).when(dualReadStateManager).getGlobalDualReadMode();
    doReturn(newMode).when(dualReadStateManager).getServiceDualReadMode(any());
    doReturn(newMode).when(dualReadStateManager).getClusterDualReadMode(any());

    // trigger notifying watchers
    dualReadStateManager.updateGlobal(newMode);
    dualReadStateManager.updateService("S_Foo", newMode);
    dualReadStateManager.updateCluster("C_Foo", newMode);

    verifyJmxNames(fixture, sourceType, isPrimaryAfter, isPrimaryBefore == isPrimaryAfter);
  }

  private void verifyJmxNames(D2ClientJmxManagerFixture fixture, D2ClientJmxManager.DiscoverySourceType sourceType,
      boolean expectedToBePrimary, boolean calledTwice)
  {
    JmxManager jmxManager = fixture._jmxManager;
    int callTimes = calledTwice ? 2 : 1;
    if (expectedToBePrimary)
    {
      verify(jmxManager, times(callTimes)).registerLoadBalancer(eq("Foo-LoadBalancer"), eq(fixture._loadBalancer));
      verify(jmxManager, times(callTimes)).registerLoadBalancerState(eq("Foo-LoadBalancerState"), eq(fixture._simpleLoadBalancerState));
      verify(jmxManager, times(callTimes)).registerFileStore(eq("Foo-FileStoreUriStore"), eq(fixture._uriStore));
      verify(jmxManager, times(callTimes)).registerFileStore(eq("Foo-FileStoreClusterStore"), eq(fixture._clusterStore));
      verify(jmxManager, times(callTimes)).registerFileStore(eq("Foo-FileStoreServiceStore"), eq(fixture._serviceStore));
      verify(jmxManager, times(callTimes)).registerServiceProperties(eq("S_Foo-ServiceProperties"), eq(SERVICE_PROPERTIES_LOAD_BALANCER_STATE_ITEM));
      verify(jmxManager, times(callTimes)).registerClusterInfo(eq("C_Foo-ClusterInfo"), eq(fixture._clusterInfoItem));
      verify(jmxManager, times(callTimes)).registerLoadBalancerStrategy(eq("S_Foo-https-LoadBalancerStrategy"), eq(fixture._relativeLoadBalancerStrategy));
    }
    else
    { // secondary source, include source type name in jmx names
      switch (sourceType)
      {
        case XDS:
          verify(jmxManager, times(callTimes)).registerLoadBalancer(eq("Foo-xDS-LoadBalancer"), eq(fixture._loadBalancer));
          verify(jmxManager, times(callTimes)).registerLoadBalancerState(eq("Foo-xDS-LoadBalancerState"), eq(fixture._simpleLoadBalancerState));
          verify(jmxManager, times(callTimes)).registerFileStore(eq("Foo-xDS-FileStoreUriStore"), eq(fixture._uriStore));
          verify(jmxManager, times(callTimes)).registerFileStore(eq("Foo-xDS-FileStoreClusterStore"), eq(fixture._clusterStore));
          verify(jmxManager, times(callTimes)).registerFileStore(eq("Foo-xDS-FileStoreServiceStore"), eq(fixture._serviceStore));
          verify(jmxManager, times(callTimes)).registerServiceProperties(eq("xDS-S_Foo-ServiceProperties"), eq(SERVICE_PROPERTIES_LOAD_BALANCER_STATE_ITEM));
          verify(jmxManager, times(callTimes)).registerClusterInfo(eq("xDS-C_Foo-ClusterInfo"), eq(fixture._clusterInfoItem));
          verify(jmxManager, times(callTimes)).registerLoadBalancerStrategy(eq("xDS-S_Foo-https-LoadBalancerStrategy"), eq(fixture._relativeLoadBalancerStrategy));
          break;
        case ZK:
          verify(jmxManager, times(callTimes)).registerLoadBalancer(eq("Foo-ZK-LoadBalancer"), eq(fixture._loadBalancer));
          verify(jmxManager, times(callTimes)).registerLoadBalancerState(eq("Foo-ZK-LoadBalancerState"), eq(fixture._simpleLoadBalancerState));
          verify(jmxManager, times(callTimes)).registerFileStore(eq("Foo-ZK-FileStoreUriStore"), eq(fixture._uriStore));
          verify(jmxManager, times(callTimes)).registerFileStore(eq("Foo-ZK-FileStoreClusterStore"), eq(fixture._clusterStore));
          verify(jmxManager, times(callTimes)).registerFileStore(eq("Foo-ZK-FileStoreServiceStore"), eq(fixture._serviceStore));
          verify(jmxManager, times(callTimes)).registerServiceProperties(eq("ZK-S_Foo-ServiceProperties"), eq(SERVICE_PROPERTIES_LOAD_BALANCER_STATE_ITEM));
          verify(jmxManager, times(callTimes)).registerClusterInfo(eq("ZK-C_Foo-ClusterInfo"), eq(fixture._clusterInfoItem));
          verify(jmxManager, times(callTimes)).registerLoadBalancerStrategy(eq("ZK-S_Foo-https-LoadBalancerStrategy"), eq(fixture._relativeLoadBalancerStrategy));
          break;
        default:
          Assert.fail(String.format("Unknown source type: %s", sourceType));
      }
    }
  }
}

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class FailedoutClusterManagerTest
{
  private final static String CLUSTER_NAME = "Cluster";
  private final static String PEER_CLUSTER_NAME1 = "ClusterPeer1";
  private final static String PEER_CLUSTER_NAME2 = "ClusterPeer2";
  private final static long PEER_WATCH_TEAR_DOWN_DELAY_MS = 60000;

  @Mock
  private LoadBalancerState _loadBalancerState;

  @Mock
  private FailedoutClusterConnectionWarmUpHandler _warmUpHandler;

  @Mock
  private ScheduledExecutorService _scheduledExecutorService;

  private FailedoutClusterManager _manager;

  @BeforeMethod
  public void setup()
  {
    MockitoAnnotations.initMocks(this);
    _manager = new FailedoutClusterManager(CLUSTER_NAME, _loadBalancerState, _warmUpHandler,
        PEER_WATCH_TEAR_DOWN_DELAY_MS, _scheduledExecutorService);

    // Setup LoadBalancerStateListenerCallback
    doAnswer((Answer<Object>) invocation -> {
      Object arg0 = invocation.getArguments()[0];
      Object arg1 = invocation.getArguments()[1];
      assertTrue(arg0 instanceof String);
      assertTrue(arg1 instanceof LoadBalancerState.LoadBalancerStateListenerCallback);
      ((LoadBalancerState.LoadBalancerStateListenerCallback) arg1).done(LoadBalancerState.LoadBalancerStateListenerCallback.CLUSTER, (String) arg0);
      return null;
    }).when(_loadBalancerState).listenToCluster(any(), any());
  }

  @Test
  public void testAddPeerClusterWatches()
  {
    _manager.addPeerClusterWatches(new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1, PEER_CLUSTER_NAME2)), mock(FailoutConfig.class));
    verify(_loadBalancerState).listenToCluster(eq(PEER_CLUSTER_NAME1), any());
    verify(_loadBalancerState).listenToCluster(eq(PEER_CLUSTER_NAME2), any());
    verify(_warmUpHandler, times(1)).warmUpConnections(eq(PEER_CLUSTER_NAME1), any());
    verify(_warmUpHandler, times(1)).warmUpConnections(eq(PEER_CLUSTER_NAME2), any());
    verify(_warmUpHandler, never()).cancelPendingRequests(any());
  }

  @Test
  public void testAddPeerClusterWatchesWithPeerClusterAdded()
  {
    _manager.addPeerClusterWatches(new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1)), mock(FailoutConfig.class));
    _manager.addPeerClusterWatches(new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1, PEER_CLUSTER_NAME2)), mock(FailoutConfig.class));
    verify(_loadBalancerState, times(1)).listenToCluster(eq(PEER_CLUSTER_NAME2), any());
    verify(_loadBalancerState, times(1)).listenToCluster(eq(PEER_CLUSTER_NAME1), any());
    verify(_warmUpHandler, times(1)).warmUpConnections(eq(PEER_CLUSTER_NAME1), any());
    verify(_warmUpHandler, times(1)).warmUpConnections(eq(PEER_CLUSTER_NAME2), any());
    verify(_warmUpHandler, never()).cancelPendingRequests(any());
  }

  @Test
  public void testAddPeerClusterWatchesWithPeerClusterRemoved()
  {
    _manager.addPeerClusterWatches(new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1, PEER_CLUSTER_NAME2)), mock(FailoutConfig.class));
    _manager.addPeerClusterWatches(new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1)), mock(FailoutConfig.class));
    verify(_loadBalancerState, times(1)).listenToCluster(eq(PEER_CLUSTER_NAME1), any());
    verify(_loadBalancerState, times(1)).listenToCluster(eq(PEER_CLUSTER_NAME2), any());
    verify(_warmUpHandler, times(1)).cancelPendingRequests(eq(PEER_CLUSTER_NAME2));

    ArgumentCaptor<Callable> captor = ArgumentCaptor.forClass(Callable.class);
    verify(_scheduledExecutorService, times(1)).schedule(captor.capture(),
        eq(PEER_WATCH_TEAR_DOWN_DELAY_MS), eq(TimeUnit.MILLISECONDS));

    captor.getValue().call();
    verify(_loadBalancerState, times(1)).stopListenToCluster(eq(PEER_CLUSTER_NAME2));
  }

  @Test
  public void testPeerClusterRemovalWithoutScheduledExecutorService()
  {
    FailedoutClusterManager manager = new FailedoutClusterManager(CLUSTER_NAME, _loadBalancerState, _warmUpHandler,
        0, null);
    manager.addPeerClusterWatches(new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1)), mock(FailoutConfig.class));
    manager.removePeerClusterWatches();

    verify(_loadBalancerState, times(1)).stopListenToCluster(eq(PEER_CLUSTER_NAME1));
  }

  @Test
  public void testDoesRemovePeerClusterWatchIfWatchExistsBeforeFailout()
  {
    when(_loadBalancerState.isListeningToCluster(eq(PEER_CLUSTER_NAME1))).thenReturn(true);
    _manager.addPeerClusterWatches(new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1)), mock(FailoutConfig.class));
    _manager.removePeerClusterWatches();

    verify(_scheduledExecutorService, never()).schedule(any(Runnable.class), anyLong(), any());
    verify(_loadBalancerState, never()).stopListenToCluster(any());
  }

  @Test
  public void testDoesRemovePeerClusterWatchIfWatchNotEstablished()
  {
    doNothing().when(_loadBalancerState).listenToCluster(any(), any());
    _manager.addPeerClusterWatches(new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1)), mock(FailoutConfig.class));
    _manager.removePeerClusterWatches();

    verify(_scheduledExecutorService, never()).schedule(any(Runnable.class), anyLong(), any());
    verify(_loadBalancerState, never()).stopListenToCluster(any());
  }

  @Test
  public void testUpdateFailoutConfigWithNull() {
    _manager.updateFailoutConfig(null);
    verify(_loadBalancerState, never()).listenToCluster(any(), any());
    assertNull(_manager.getFailoutConfig());
    verify(_warmUpHandler, never()).warmUpConnections(any(), any());
    verify(_warmUpHandler, never()).cancelPendingRequests(any());
  }

  @Test
  public void testUpdateFailoutConfigWithoutActiveFailout() {
    FailoutConfig config = mock(FailoutConfig.class);
    when(config.isFailedOut()).thenReturn(false);
    when(config.getPeerClusters()).thenReturn(Collections.singleton(PEER_CLUSTER_NAME1));
    _manager.updateFailoutConfig(config);
    verify(_loadBalancerState, never()).listenToCluster(any(), any());
    assertNotNull(_manager.getFailoutConfig());
    verify(_warmUpHandler, never()).warmUpConnections(any(), any());
    verify(_warmUpHandler, never()).cancelPendingRequests(any());
  }

  @Test
  public void testUpdateFailoutConfigWithActiveFailout() {
    FailoutConfig config = mock(FailoutConfig.class);
    when(config.isFailedOut()).thenReturn(true);
    when(config.getPeerClusters()).thenReturn(Collections.singleton(PEER_CLUSTER_NAME1));
    _manager.updateFailoutConfig(config);
    verify(_loadBalancerState, times(1)).listenToCluster(eq(PEER_CLUSTER_NAME1), any());
    verify(_warmUpHandler, times(1)).warmUpConnections(eq(PEER_CLUSTER_NAME1), any());
    verify(_warmUpHandler, never()).cancelPendingRequests(any());
    assertNotNull(_manager.getFailoutConfig());
  }

  @Test
  public void testUpdateFailoutConfigUpdate() {
    FailoutConfig config = mock(FailoutConfig.class);
    when(config.isFailedOut()).thenReturn(true);
    when(config.getPeerClusters()).thenReturn(Collections.singleton(PEER_CLUSTER_NAME1));
    _manager.updateFailoutConfig(config);
    verify(_loadBalancerState, times(1)).listenToCluster(eq(PEER_CLUSTER_NAME1), any());
    verify(_warmUpHandler, times(1)).warmUpConnections(eq(PEER_CLUSTER_NAME1), any());
    verify(_warmUpHandler, never()).cancelPendingRequests(any());

    when(config.getPeerClusters()).thenReturn(new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1, PEER_CLUSTER_NAME2)));
    _manager.updateFailoutConfig(config);
    verify(_loadBalancerState, times(1)).listenToCluster(eq(PEER_CLUSTER_NAME2), any());
    verify(_warmUpHandler, times(1)).warmUpConnections(eq(PEER_CLUSTER_NAME2), any());
    verify(_warmUpHandler, never()).cancelPendingRequests(any());
    assertNotNull(_manager.getFailoutConfig());
  }

  @Test
  public void testUpdateFailoutConfigUpdateToNull() {
    FailoutConfig config = mock(FailoutConfig.class);
    when(config.isFailedOut()).thenReturn(true);
    when(config.getPeerClusters()).thenReturn(Collections.singleton(PEER_CLUSTER_NAME1));
    _manager.updateFailoutConfig(config);
    assertNotNull(_manager.getFailoutConfig());
    verify(_loadBalancerState, times(1)).listenToCluster(eq(PEER_CLUSTER_NAME1), any());
    verify(_warmUpHandler, times(1)).warmUpConnections(eq(PEER_CLUSTER_NAME1), eq(config));

    reset(_warmUpHandler);

    _manager.updateFailoutConfig(null);
    assertNull(_manager.getFailoutConfig());

    verify(_warmUpHandler, never()).warmUpConnections(any(), any());
    verify(_warmUpHandler, times(1)).cancelPendingRequests(eq(PEER_CLUSTER_NAME1));

    ArgumentCaptor<Callable> captor = ArgumentCaptor.forClass(Callable.class);
    verify(_scheduledExecutorService, times(1)).schedule(captor.capture(),
        eq(PEER_WATCH_TEAR_DOWN_DELAY_MS), eq(TimeUnit.MILLISECONDS));

    captor.getValue().call();
    verify(_loadBalancerState, times(1)).stopListenToCluster(eq(PEER_CLUSTER_NAME1));
  }

  @Test
  public void testShutdown() {
    _manager.shutdown();
    verify(_warmUpHandler, times(1)).shutdown();
  }
}

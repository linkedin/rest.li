package com.linkedin.d2.balancer.clusterfailout;

import com.linkedin.d2.balancer.LoadBalancerState;
import java.util.Arrays;
import java.util.HashSet;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


public class FailedoutClusterWatchManagerTest {
  private final static String CLUSTER_NAME = "Cluster";
  private final static String PEER_CLUSTER_NAME1 = "ClusterPeer1";
  private final static String PEER_CLUSTER_NAME2 = "ClusterPeer2";

  @Mock
  private LoadBalancerState _loadBalancerState;

  private FailedoutClusterWatchManager _manager;

  @BeforeMethod
  public void setup() {
    MockitoAnnotations.initMocks(this);
    _manager = new FailedoutClusterWatchManager(_loadBalancerState);
  }

  @Test
  public void testAddPeerClusterWatches() {
    _manager.addPeerClusterWatches(CLUSTER_NAME, new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1, PEER_CLUSTER_NAME2)));
    verify(_loadBalancerState).listenToCluster(eq(PEER_CLUSTER_NAME1), any());
    verify(_loadBalancerState).listenToCluster(eq(PEER_CLUSTER_NAME2), any());
  }

  @Test
  public void testAddPeerClusterWatchesWithPeerClusterAdded() {
    _manager.addPeerClusterWatches(CLUSTER_NAME, new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1)));
    _manager.addPeerClusterWatches(CLUSTER_NAME, new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1, PEER_CLUSTER_NAME2)));
    verify(_loadBalancerState, times(1)).listenToCluster(eq(PEER_CLUSTER_NAME2), any());
    verify(_loadBalancerState, times(1)).listenToCluster(eq(PEER_CLUSTER_NAME1), any());
  }

  @Test
  public void testAddPeerClusterWatchesWithPeerClusterRemoved() {
    _manager.addPeerClusterWatches(CLUSTER_NAME, new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1, PEER_CLUSTER_NAME2)));
    _manager.addPeerClusterWatches(CLUSTER_NAME, new HashSet<>(Arrays.asList(PEER_CLUSTER_NAME1)));
    verify(_loadBalancerState, times(1)).listenToCluster(eq(PEER_CLUSTER_NAME1), any());
    verify(_loadBalancerState, times(1)).listenToCluster(eq(PEER_CLUSTER_NAME2), any());

    // TODO(RESILIEN-51): Unregister watch for PEER_CLUSTER_NAME2
  }
}

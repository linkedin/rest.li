package com.linkedin.d2.balancer.clusterfailout;

import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.LoadBalancerState.LoadBalancerStateListenerCallback;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * This class is responsible for handling logistics for peer clusters of a failed out cluster.
 * Some example tasks include:
 * - Adding cluster and URI watches for the peer clusters.
 * - Establishing connections to instances in the peer clusters.
 */
public class FailedoutClusterWatchManager {
  private final String _clusterName;
  private final LoadBalancerState _loadBalancerState;
  private final ConcurrentMap<String, LoadBalancerStateListenerCallback> _clusterListeners =
      new ConcurrentHashMap<>();

  public FailedoutClusterWatchManager(String clusterName, LoadBalancerState loadBalancerState) {
    _clusterName = clusterName;
    _loadBalancerState = loadBalancerState;
  }

  /**
   * Calle this method when a cluster is failed out and/or its new peer clusters are identified.
   * @param newPeerClusters name of the peer clusters of the failed out clsuters
   */
  public void addPeerClusterWatches(Set<String> newPeerClusters) {
    final Set<String> existingPeerClusters = _clusterListeners.keySet();

    if (newPeerClusters.isEmpty()) {
      removeClusterWatches(existingPeerClusters);
      return;
    }

    final Set<String> peerClustersToAdd = new HashSet<>(newPeerClusters);
    peerClustersToAdd.removeAll(existingPeerClusters);

    if (!peerClustersToAdd.isEmpty()) {
      addClusterWatches(peerClustersToAdd);
    }

    final Set<String> peerClustersToRemove = new HashSet<>(existingPeerClusters);
    peerClustersToRemove.removeAll(newPeerClusters);

    if (!peerClustersToRemove.isEmpty()) {
      removeClusterWatches(peerClustersToRemove);
    }
  }

  /**
   * Call this method when a cluster failed out is over and we do not need to monitor its peer clusters.
   */
  public void removePeerClusterWatches() {
    removeClusterWatches(_clusterListeners.keySet());
  }

  private void removeClusterWatches(Set<String> peerClustersToRemove) {
    for (String peerCluster : peerClustersToRemove) {
      final LoadBalancerStateListenerCallback listener = _clusterListeners.remove(peerCluster);
      if (listener != null) {
        // TODO(RESILIEN-51): Unregister watches when failout is over
      }
    }
  }

  private void addClusterWatches(Set<String> peerClusters) {
    for (final String peerCluster : peerClusters) {
      _clusterListeners.computeIfAbsent(peerCluster, clusterName -> {
        // TODO(RESILIEN-50): Establish connections to peer clusters when listener#done() is invoked.
        LoadBalancerStateListenerCallback listener = new LoadBalancerState.NullStateListenerCallback();
        _loadBalancerState.listenToCluster(peerCluster, listener);
        return listener;
      });
    }
  }
}

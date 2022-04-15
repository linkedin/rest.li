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
import com.linkedin.d2.balancer.LoadBalancerState.LoadBalancerStateListenerCallback;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing one failed out cluster.
 * Some example tasks include:
 * - Adding cluster and URI watches for the peer clusters of the failed out downstream cluster.
 * - Establishing connections to instances in the peer clusters of the failed out downstream cluster.
 * - Managing failout config updates for the cluster.
 */
public class FailedoutClusterManager
{
  private static final Logger _log = LoggerFactory.getLogger(FailedoutClusterManager.class);
  private final String _clusterName;
  private final LoadBalancerState _loadBalancerState;
  private final ConcurrentMap<String, LoadBalancerStateListenerCallback> _clusterListeners = new ConcurrentHashMap<>();
  private FailoutConfig _failoutConfig;

  public FailedoutClusterManager(@Nonnull String clusterName, @Nonnull LoadBalancerState loadBalancerState)
  {
    _clusterName = clusterName;
    _loadBalancerState = loadBalancerState;
  }

  public String getClusterName()
  {
    return _clusterName;
  }

  /**
   * Gets the current failout config.
   * @return null when there is not failout config found.
   */
  public FailoutConfig getFailoutConfig()
  {
    return _failoutConfig;
  }

  /**
   * Updates to a new failout config version.
   * @param failoutConfig The new failout config. Null when there is not active failout associated with the cluster.
   */
  public void updateFailoutConfig(@Nullable FailoutConfig failoutConfig)
  {
    if (failoutConfig == null)
    {
      removePeerClusterWatches();
    }
    else
    {
      processNewConfig(failoutConfig);
    }

    _failoutConfig = failoutConfig;
  }

  /**
   * Processes failout config of the failed out downstream cluster.
   */
  private void processNewConfig(@Nonnull FailoutConfig failoutConfig)
  {
    if (!failoutConfig.isFailedOut())
    {
      _log.debug("Failout completed for downstream cluster: {}. Removing all peer cluster watches.", _clusterName);
      removePeerClusterWatches();
    }
    else
    {
      Set<String> peerClusters = failoutConfig.getPeerClusters();
      addPeerClusterWatches(peerClusters);
    }
  }

  /**
   * Calle this method when a cluster is failed out and/or new peer clusters of the failed out downstream cluster are identified.
   * @param newPeerClusters Name of the peer clusters of the failed out downstream clusters
   */
  void addPeerClusterWatches(@Nonnull Set<String> newPeerClusters)
  {
    final Set<String> existingPeerClusters = _clusterListeners.keySet();

    if (newPeerClusters.isEmpty())
    {
      removePeerClusterWatches();
      return;
    }

    final Set<String> peerClustersToAdd = new HashSet<>(newPeerClusters);
    peerClustersToAdd.removeAll(existingPeerClusters);

    if (!peerClustersToAdd.isEmpty())
    {
      addClusterWatches(peerClustersToAdd);
    }

    final Set<String> peerClustersToRemove = new HashSet<>(existingPeerClusters);
    peerClustersToRemove.removeAll(newPeerClusters);

    if (!peerClustersToRemove.isEmpty())
    {
      removeClusterWatches(peerClustersToRemove);
    }
  }

  /**
   * Call this method when a cluster failed out is over and we do not need to monitor its peer clusters.
   */
  void removePeerClusterWatches()
  {
    removeClusterWatches(_clusterListeners.keySet());
  }

  private void addClusterWatches(@Nonnull Set<String> clustersToWatch)
  {
    if (_log.isDebugEnabled())
    {
      _log.debug("Watching peer clusters: " + String.join(",", clustersToWatch));
    }
    for (final String cluster : clustersToWatch)
    {
      _clusterListeners.computeIfAbsent(cluster, clusterName -> {
        // TODO(RESILIEN-50): Establish connections to peer clusters when listener#done() is invoked.
        LoadBalancerStateListenerCallback listener = new LoadBalancerState.NullStateListenerCallback();
        _loadBalancerState.listenToCluster(cluster, listener);
        return listener;
      });
    }
  }

  private void removeClusterWatches(@Nonnull Set<String> clustersToRemove)
  {
    if (_log.isDebugEnabled())
    {
      _log.debug("Removing peer clusters: " + String.join(",", clustersToRemove));
    }
    for (String cluster : clustersToRemove)
    {
      final LoadBalancerStateListenerCallback listener = _clusterListeners.remove(cluster);
      if (listener != null)
      {
        // TODO(RESILIEN-51): Unregister watches when failout is over
      }
    }
  }
}

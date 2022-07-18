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

import com.linkedin.d2.balancer.LoadBalancerClusterListener;
import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.properties.FailoutProperties;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible for providing cluster failout config for each cluster.
 */
public abstract class ZKFailoutConfigProvider implements FailoutConfigProvider, LoadBalancerClusterListener
{
  private static final Logger _log = LoggerFactory.getLogger(FailedoutClusterManager.class);
  private final ConcurrentMap<String, FailedoutClusterManager> _failedoutClusterManagers = new ConcurrentHashMap<>();
  private final LoadBalancerState _loadBalancerState;

  public ZKFailoutConfigProvider(@Nonnull LoadBalancerState loadBalancerState)
  {
    _loadBalancerState = loadBalancerState;
  }

  @Override
  public void start()
  {
    _log.info("Registering cluster listener to watch for failout config changes");
    _loadBalancerState.registerClusterListener(this);
  }

  @Override
  public void shutdown()
  {
    _loadBalancerState.unregisterClusterListener(this);
    _failedoutClusterManagers.values().forEach(FailedoutClusterManager::shutdown);
  }

  /**
   * Converts {@link FailoutProperties} into a {@link FailoutConfig} that will be used for routing requests.
   * @param failoutProperties The properties defined for a cluster failout.
   * @return Parsed and processed config that's ready to be used for routing requests.
   */
  public abstract @Nullable
  FailoutConfig createFailoutConfig(@Nonnull String clusterName, @Nullable FailoutProperties failoutProperties);

  @Override
  public FailoutConfig getFailoutConfig(String clusterName)
  {
    final FailedoutClusterManager failedoutClusterManager = _failedoutClusterManagers.get(clusterName);
    return failedoutClusterManager != null ? failedoutClusterManager.getFailoutConfig() : null;
  }

  @Override
  public void onClusterAdded(String clusterName)
  {
    LoadBalancerStateItem<FailoutProperties> item = _loadBalancerState.getFailoutProperties(clusterName);
    if (item != null)
    {
      final FailoutProperties failoutProperties = item.getProperty();
      _log.info("Detected cluster failout property change for cluster: {}. New properties: {}", clusterName, failoutProperties);

      final FailoutConfig failoutConfig = createFailoutConfig(clusterName, failoutProperties);
      _failedoutClusterManagers
        .computeIfAbsent(clusterName, name -> new FailedoutClusterManager(clusterName, _loadBalancerState, createConnectionWarmUpHandler()))
        .updateFailoutConfig(failoutConfig);
    }
    else
    {
      _log.debug("Cluster properties change for cluster: {}. No cluster failout property found.", clusterName);
    }
  }

  @Override
  public void onClusterRemoved(String clusterName)
  {
    FailedoutClusterManager manager = _failedoutClusterManagers.remove(clusterName);
    if (manager != null)
    {
      _log.info("Cluster: {} removed. Resetting cluster failout config.", clusterName);
      manager.updateFailoutConfig(null);
    }
  }

  /**
   * Creates a {@link FailedoutClusterConnectionWarmUpHandler} to warm-up the connection to peer clusters before
   * sending the actual request. Establishing connections can be costly and possibly overload the peer clusters
   * when a large number of clients trying to connect to the peer clusters at the same time when fail out starts.
   * Sub-classes can override this method to return a non-null handler to warm up the connections. Method will be
   * invoked for each cluster failed out.
   * @return null if no connection warm-up is required.
   *         An instance of {@link FailedoutClusterConnectionWarmUpHandler} to handle warm-up.
   *         Handler will be invoked once when we first start watching a peer cluster.
   */
  @Nullable
  public FailedoutClusterConnectionWarmUpHandler createConnectionWarmUpHandler()
  {
    return null;
  }
}

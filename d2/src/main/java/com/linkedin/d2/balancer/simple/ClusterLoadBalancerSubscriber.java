/*
   Copyright (c) 2017 LinkedIn Corp.

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

import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.config.CanaryDistributionStrategyConverter;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterStoreProperties;
import com.linkedin.d2.balancer.properties.FailoutProperties;
import com.linkedin.d2.balancer.util.canary.CanaryDistributionProvider;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessorFactory;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessorRegistry;
import com.linkedin.d2.discovery.event.PropertyEventBus;

/**
 * Subscriber to the cluster data to update the SimpleLoadBalancerState
 */
class ClusterLoadBalancerSubscriber extends
  AbstractLoadBalancerSubscriber<ClusterProperties>
{

  final private SimpleLoadBalancerState _simpleLoadBalancerState;
  final private PartitionAccessorRegistry _partitionAccessorRegistry;

  public ClusterLoadBalancerSubscriber(SimpleLoadBalancerState simpleLoadBalancerState,
      PropertyEventBus<ClusterProperties> cPropertyEventBus, PartitionAccessorRegistry partitionAccessorRegistry)
  {
    super(LoadBalancerState.LoadBalancerStateListenerCallback.CLUSTER, cPropertyEventBus);
    this._simpleLoadBalancerState = simpleLoadBalancerState;
    this._partitionAccessorRegistry = partitionAccessorRegistry;
  }

  @Override
  protected void handlePut(final String listenTo, final ClusterProperties discoveryProperties)
  {
    if (discoveryProperties != null)
    {
      ClusterProperties pickedProperties = pickActiveProperties(discoveryProperties);

      _simpleLoadBalancerState.getClusterInfo().put(listenTo,
        new ClusterInfoItem(_simpleLoadBalancerState, pickedProperties,
          PartitionAccessorFactory.getPartitionAccessor(pickedProperties.getClusterName(),
              _partitionAccessorRegistry, pickedProperties.getPartitionProperties()),
              getFailoutProperties(discoveryProperties)));
      // notify the cluster listeners only when discoveryProperties is not null, because we don't
      // want to count initialization (just because listenToCluster is called)
      _simpleLoadBalancerState.notifyClusterListenersOnAdd(listenTo);
    }
    else
    {
      // still insert the ClusterInfoItem when discoveryProperties is null, but don't create accessor
      _simpleLoadBalancerState.getClusterInfo().put(listenTo,
        new ClusterInfoItem(_simpleLoadBalancerState, null, null, null));
    }
  }

  @Override
  protected void handleRemove(final String listenTo)
  {
    _simpleLoadBalancerState.getClusterInfo().remove(listenTo);
    _simpleLoadBalancerState.notifyClusterListenersOnRemove(listenTo);
  }

  /**
   * Pick the active properties (stable or canary configs) based on canary distribution strategy.
   * @param discoveryProperties a composite properties containing all data on the cluster store (stable configs, canary configs, etc.).
   * @return the picked active properties
   */
  private ClusterProperties pickActiveProperties(final ClusterProperties discoveryProperties)
  {
    final ClusterStoreProperties clusterStoreProperties = toClusterStoreProperties(discoveryProperties);
    if (clusterStoreProperties == null)
    {
      // this should not happen since the serializer returns the composite class
      return discoveryProperties;
    }

    CanaryDistributionProvider.Distribution distribution = CanaryDistributionProvider.Distribution.STABLE;
    CanaryDistributionProvider canaryDistributionProvider = _simpleLoadBalancerState.getCanaryDistributionProvider();
    if (clusterStoreProperties.hasCanary() && canaryDistributionProvider != null)
    {
      // Canary config and canary distribution provider exist, distribute to use either stable config or canary config.
      distribution = canaryDistributionProvider
          .distribute(CanaryDistributionStrategyConverter.toConfig(clusterStoreProperties.getCanaryDistributionStrategy()));
    }
    // TODO: set canary/stable config metric
    return clusterStoreProperties.getDistributedClusterProperties(distribution);
  }

  private FailoutProperties getFailoutProperties(final ClusterProperties clusterProperties)
  {
    final ClusterStoreProperties clusterStoreProperties = toClusterStoreProperties(clusterProperties);
    if (clusterStoreProperties == null)
    {
      // this should not happen since the serializer returns the composite class
      return null;
    }
    return clusterStoreProperties.getFailoutProperties();
  }

  private ClusterStoreProperties toClusterStoreProperties(final ClusterProperties clusterProperties)
  {
    // Cast should always succeed since the serializer returns the composite class
    return clusterProperties instanceof ClusterStoreProperties ? (ClusterStoreProperties) clusterProperties : null;
  }
}

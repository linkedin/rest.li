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
import com.linkedin.d2.balancer.properties.ClusterPropertiesWithCanary;
import com.linkedin.d2.balancer.util.CanaryDistributionProvider;
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
      ClusterProperties pickedProperties = discoveryProperties;
      if (discoveryProperties instanceof ClusterPropertiesWithCanary) {
        // has canary config, distribute to use either stable config or canary config
        ClusterPropertiesWithCanary propertiesWithCanary = (ClusterPropertiesWithCanary) discoveryProperties;
        CanaryDistributionProvider.Distribution distribution = _simpleLoadBalancerState.getCanaryDistributionProvider()
                .distribute(CanaryDistributionStrategyConverter.toConfig(propertiesWithCanary.getCanaryDistributionStrategy()));
        if (distribution == CanaryDistributionProvider.Distribution.CANARY) {
          pickedProperties = propertiesWithCanary.getCanaryConfigs();
        }
        // TODO: set canary config metric
      }

      _simpleLoadBalancerState.getClusterInfo().put(listenTo,
        new ClusterInfoItem(_simpleLoadBalancerState, pickedProperties,
          PartitionAccessorFactory.getPartitionAccessor(pickedProperties.getClusterName(),
              _partitionAccessorRegistry, pickedProperties.getPartitionProperties())));
      // notify the cluster listeners only when discoveryProperties is not null, because we don't
      // want to count initialization (just because listenToCluster is called)
      _simpleLoadBalancerState.notifyClusterListenersOnAdd(listenTo);
    }
    else
    {
      // still insert the ClusterInfoItem when discoveryProperties is null, but don't create accessor
      _simpleLoadBalancerState.getClusterInfo().put(listenTo,
        new ClusterInfoItem(_simpleLoadBalancerState, discoveryProperties, null));
    }
  }

  @Override
  protected void handleRemove(final String listenTo)
  {
    _simpleLoadBalancerState.getClusterInfo().remove(listenTo);
    _simpleLoadBalancerState.notifyClusterListenersOnRemove(listenTo);
  }
}

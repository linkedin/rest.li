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

import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.util.canary.CanaryDistributionProvider;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;

/**
 * We put together the cluster properties and the partition accessor for a cluster so that we don't have to
 * maintain two separate maps (which have to be in sync all the time)
 */
public class ClusterInfoItem
{
  private final LoadBalancerStateItem<ClusterProperties> _clusterPropertiesItem;
  private final LoadBalancerStateItem<PartitionAccessor> _partitionAccessorItem;

  ClusterInfoItem(SimpleLoadBalancerState simpleLoadBalancerState, ClusterProperties clusterProperties, PartitionAccessor partitionAccessor)
  {
    this(simpleLoadBalancerState, clusterProperties, partitionAccessor, CanaryDistributionProvider.Distribution.STABLE);
  }

  ClusterInfoItem(
      SimpleLoadBalancerState simpleLoadBalancerState,
      ClusterProperties clusterProperties,
      PartitionAccessor partitionAccessor,
      CanaryDistributionProvider.Distribution distribution)
  {
    long version = simpleLoadBalancerState.getVersionAccess().incrementAndGet();
    _clusterPropertiesItem = new LoadBalancerStateItem<>(clusterProperties,
        version,
        System.currentTimeMillis(),
        distribution);
    _partitionAccessorItem = new LoadBalancerStateItem<>(partitionAccessor,
        version,
        System.currentTimeMillis());
  }


  public LoadBalancerStateItem<ClusterProperties> getClusterPropertiesItem()
  {
    return _clusterPropertiesItem;
  }

  public LoadBalancerStateItem<PartitionAccessor> getPartitionAccessorItem()
  {
    return _partitionAccessorItem;
  }

  @Override
  public String toString()
  {
    return "_clusterProperties = " + _clusterPropertiesItem.getProperty();
  }
}

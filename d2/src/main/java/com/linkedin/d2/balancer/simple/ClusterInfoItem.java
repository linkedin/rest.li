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
import com.linkedin.d2.balancer.properties.FailoutProperties;
import com.linkedin.d2.balancer.util.canary.CanaryDistributionProvider;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * We put together the cluster properties and the partition accessor for a cluster so that we don't have to
 * maintain two separate maps (which have to be in sync all the time)
 */
public class ClusterInfoItem
{
  private final LoadBalancerStateItem<ClusterProperties> _clusterPropertiesItem;
  private final LoadBalancerStateItem<PartitionAccessor> _partitionAccessorItem;
  private final LoadBalancerStateItem<FailoutProperties> _failoutPropertiesItem;

  public ClusterInfoItem(SimpleLoadBalancerState simpleLoadBalancerState, ClusterProperties clusterProperties, PartitionAccessor partitionAccessor)
  {
    this(simpleLoadBalancerState, clusterProperties, partitionAccessor, CanaryDistributionProvider.Distribution.STABLE);
  }

  public ClusterInfoItem(
    SimpleLoadBalancerState simpleLoadBalancerState,
    ClusterProperties clusterProperties,
    PartitionAccessor partitionAccessor,
    @Nonnull
      CanaryDistributionProvider.Distribution distribution)
  {
    this(simpleLoadBalancerState, clusterProperties, partitionAccessor, distribution, null);
  }
  public ClusterInfoItem(
      SimpleLoadBalancerState simpleLoadBalancerState,
      ClusterProperties clusterProperties,
      PartitionAccessor partitionAccessor,
      @Nonnull
      CanaryDistributionProvider.Distribution distribution,
      @Nullable FailoutProperties failoutProperties)
  {
    long version = simpleLoadBalancerState.getVersionAccess().incrementAndGet();
    _clusterPropertiesItem = new LoadBalancerStateItem<>(clusterProperties,
        version,
        System.currentTimeMillis(),
        distribution);
    _partitionAccessorItem = new LoadBalancerStateItem<>(partitionAccessor,
        version,
        System.currentTimeMillis());
    _failoutPropertiesItem = new LoadBalancerStateItem<>(failoutProperties,
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

  LoadBalancerStateItem<FailoutProperties> getFailoutPropertiesItem()
  {
    return _failoutPropertiesItem;
  }

  @Override
  public String toString()
  {
    return "_clusterProperties = " + _clusterPropertiesItem.getProperty();
  }


}

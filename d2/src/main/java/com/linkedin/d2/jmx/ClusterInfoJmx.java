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
import com.linkedin.d2.balancer.simple.ClusterInfoItem;
import com.linkedin.d2.jmx.ClusterInfoOtelMetricsProvider;


public class ClusterInfoJmx implements ClusterInfoJmxMBean
{
  private final ClusterInfoItem _clusterInfoItem;
  private final ClusterInfoOtelMetricsProvider _clusterInfoOtelMetricsProvider;

  public ClusterInfoJmx(ClusterInfoItem clusterInfoItem)
  {
    this(clusterInfoItem, new NoOpClusterInfoOtelMetricsProvider());
  }

  public ClusterInfoJmx(ClusterInfoItem clusterInfoItem, ClusterInfoOtelMetricsProvider clusterInfoOtelMetricsProvider) {
    _clusterInfoItem = clusterInfoItem;
    _clusterInfoOtelMetricsProvider = clusterInfoOtelMetricsProvider;
  }

  @Override
  public ClusterInfoItem getClusterInfoItem()
  {
    return _clusterInfoItem;
  }

  @Override
  public int getCanaryDistributionPolicy()
  {
    String clusterName = _clusterInfoItem.getClusterPropertiesItem().getProperty().getClusterName();
    
    switch (_clusterInfoItem.getClusterPropertiesItem().getDistribution())
    {
      case STABLE: 
        _clusterInfoOtelMetricsProvider.recordCanaryDistributionPolicy(clusterName, 0);
        return 0;
      case CANARY: 
        _clusterInfoOtelMetricsProvider.recordCanaryDistributionPolicy(clusterName, 1);
        return 1;
      default: 
        _clusterInfoOtelMetricsProvider.recordCanaryDistributionPolicy(clusterName, -1);
        return -1;
    }
  }
}

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


public class ClusterInfoJmx implements ClusterInfoJmxMBean
{
  private final ClusterInfoItem _clusterInfoItem;

  public ClusterInfoJmx(ClusterInfoItem clusterInfoItem) {
    _clusterInfoItem = clusterInfoItem;
  }

  @Override
  public int getCanaryDistributionPolicy()
  {
    switch (_clusterInfoItem.getClusterPropertiesItem().getDistribution())
    {
      case STABLE: return 0;
      case CANARY: return 1;
      default: return -1;
    }
  }
}

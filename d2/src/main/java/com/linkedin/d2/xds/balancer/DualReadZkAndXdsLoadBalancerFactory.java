/*
   Copyright (c) 2023 LinkedIn Corp.
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

package com.linkedin.d2.xds.balancer;

import com.linkedin.d2.balancer.D2ClientConfig;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.LoadBalancerWithFacilitiesFactory;
import com.linkedin.d2.balancer.ZKFSLoadBalancerWithFacilitiesFactory;
import com.linkedin.d2.balancer.dualread.DualReadLoadBalancer;
import com.linkedin.d2.balancer.dualread.DualReadModeProvider;
import com.linkedin.d2.balancer.dualread.DualReadStateManager;
import javax.annotation.Nonnull;


/**
 * @deprecated Use {@link com.linkedin.d2.xds.balancer.XdsLoadBalancerWithFacilitiesFactory} instead.
 * This factory creates a {@link DualReadLoadBalancer} that performs dual read from two service
 * discovery data sources: direct ZooKeeper data and xDS data. The {@link DualReadModeProvider} will
 * determine dynamically at run-time which read mode to use.
 */
@Deprecated
public class DualReadZkAndXdsLoadBalancerFactory implements LoadBalancerWithFacilitiesFactory
{
  private final LoadBalancerWithFacilitiesFactory _zkLbFactory;
  private final LoadBalancerWithFacilitiesFactory _xdsLbFactory;
  private final DualReadStateManager _dualReadStateManager;

  public DualReadZkAndXdsLoadBalancerFactory(@Nonnull DualReadStateManager dualReadStateManager)
  {
    _zkLbFactory = new ZKFSLoadBalancerWithFacilitiesFactory();
    _xdsLbFactory = new XdsLoadBalancerWithFacilitiesFactory();
    _dualReadStateManager = dualReadStateManager;
  }

  @Override
  public LoadBalancerWithFacilities create(D2ClientConfig config)
  {
    return new DualReadLoadBalancer(_zkLbFactory.create(config), _xdsLbFactory.create(config), _dualReadStateManager, config.dualReadNewLbExecutor);
  }
}
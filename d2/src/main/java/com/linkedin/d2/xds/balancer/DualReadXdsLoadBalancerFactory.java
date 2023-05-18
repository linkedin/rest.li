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
import java.util.concurrent.Executors;


/**
 * This factory creates a {@link DualReadLoadBalancer} that performs dual read from two service
 * discovery data sources: direct ZooKeeper data and xDS data. The {@link DualReadModeProvider} will
 * determine dynamically at run-time which read mode to use.
 */
public class DualReadXdsLoadBalancerFactory implements LoadBalancerWithFacilitiesFactory
{
  private final LoadBalancerWithFacilitiesFactory _oldLbFactory;
  private final LoadBalancerWithFacilitiesFactory _newLbFactory;
  private final DualReadModeProvider _dualReadModeProvider;

  public DualReadXdsLoadBalancerFactory(DualReadModeProvider dualReadModeProvider)
  {
    _oldLbFactory = new ZKFSLoadBalancerWithFacilitiesFactory();
    _newLbFactory = new XdsLoadBalancerWithFacilitiesFactory();
    _dualReadModeProvider = dualReadModeProvider;
  }

  @Override
  public LoadBalancerWithFacilities create(D2ClientConfig config)
  {
    return new DualReadLoadBalancer(_oldLbFactory.create(config), _newLbFactory.create(config), _dualReadModeProvider,
        Executors.newSingleThreadScheduledExecutor(), 10);
  }
}

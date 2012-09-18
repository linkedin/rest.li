/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;

public class DegraderLoadBalancerStrategyV3Jmx implements DegraderLoadBalancerStrategyV3JmxMBean
{
  private final DegraderLoadBalancerStrategyV3 _strategy;

  public DegraderLoadBalancerStrategyV3Jmx(DegraderLoadBalancerStrategyV3 strategy)
  {
    _strategy = strategy;
  }

  @Override
  public double getOverrideClusterDropRate()
  {
    @SuppressWarnings("deprecation")
    double rate = _strategy.getCurrentOverrideDropRate();
    return rate;
  }

  @Override
  public String toString()
  {
    return "DegraderLoadBalancerStrategyV3Jmx [_strategy=" + _strategy + "]";
  }
}

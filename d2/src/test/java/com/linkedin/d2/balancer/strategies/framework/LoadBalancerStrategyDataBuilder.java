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

package com.linkedin.d2.balancer.strategies.framework;

import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategy;
import com.linkedin.d2.loadBalancerStrategyType;
import java.util.HashMap;
import java.util.Map;


/**
 * Builder class that helps build different types of {@link LoadBalancerStrategy} with less effort
 */
class LoadBalancerStrategyDataBuilder
{
  private final loadBalancerStrategyType _type;
  private final String _serviceName;
  private Map<String, Object> _v3StrategyProperties = new HashMap<>();
  private Map<String, String> _v3DegraderProperties = new HashMap<>();

  public LoadBalancerStrategyDataBuilder(final loadBalancerStrategyType type, final String serviceName)
  {
    _type = type;
    _serviceName = serviceName;
  }

  public LoadBalancerStrategyDataBuilder setDegraderProperties(Map<String, Object> strategyProperties,
      Map<String, String> degraderProperties)
  {
    _v3StrategyProperties = strategyProperties;
    _v3DegraderProperties = degraderProperties;
    return this;
  }

  public LoadBalancerStrategy build()
  {
    switch (_type)
    {
      case RANDOM:
        return new RandomLoadBalancerStrategy();
      case DEGRADER:
      default:
        // TODO: Change the StrategyV3 constructor, add new strategy case
        return new DegraderLoadBalancerStrategyFactoryV3().newLoadBalancer(_serviceName, _v3StrategyProperties, _v3DegraderProperties);
    }
  }
}

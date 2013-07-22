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

package com.linkedin.d2.balancer.strategies.random;

import static com.linkedin.d2.discovery.util.LogUtil.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;

import java.util.Map;

public class RandomLoadBalancerStrategyFactory implements
    LoadBalancerStrategyFactory<RandomLoadBalancerStrategy>
{
  private static final Logger _log =
                                    LoggerFactory.getLogger(RandomLoadBalancerStrategyFactory.class);

  @Override
  public RandomLoadBalancerStrategy newLoadBalancer(String serviceName,
                                                    Map<String, Object> strategyProperties,
                                                    Map<String, String> degraderProperties)
  {
    debug(_log, "created a random load balancer strategy");

    return new RandomLoadBalancerStrategy();
  }
}

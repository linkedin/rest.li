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

package com.linkedin.d2.balancer.strategies.degrader;

import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.linkedin.d2.discovery.util.LogUtil.debug;

public class DegraderLoadBalancerStrategyFactoryV3 implements
    LoadBalancerStrategyFactory<DegraderLoadBalancerStrategyV3>
{
  private static final Logger  _log =
               LoggerFactory.getLogger(DegraderLoadBalancerStrategyFactoryV3.class);
  private final HealthCheckOperations _healthCheckOperations;
  private final ScheduledExecutorService _executorService;

  public DegraderLoadBalancerStrategyFactoryV3()
  {
    _healthCheckOperations = null;
    _executorService = null;
  }

  public DegraderLoadBalancerStrategyFactoryV3(HealthCheckOperations healthCheckOperations,
      ScheduledExecutorService executorService)
  {
    _healthCheckOperations = healthCheckOperations;
    _executorService = executorService;
  }

  @Override
  public DegraderLoadBalancerStrategyV3 newLoadBalancer(String serviceName,
                                                      Map<String, Object> strategyProperties,
                                                      Map<String, String> degraderProperties)
  {
    debug(_log, "created a degrader load balancer strategyV3");

    final DegraderLoadBalancerStrategyConfig config =
        DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(strategyProperties,
            _healthCheckOperations, _executorService, degraderProperties);
    return new DegraderLoadBalancerStrategyV3(config, serviceName, degraderProperties);
  }
}

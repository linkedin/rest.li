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

import com.linkedin.d2.balancer.event.EventEmitter;
import com.linkedin.d2.balancer.event.NoopEventEmitter;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.linkedin.d2.discovery.util.LogUtil.debug;

public class DegraderLoadBalancerStrategyFactoryV3 implements
    LoadBalancerStrategyFactory<DegraderLoadBalancerStrategyV3>
{
  private static final Logger LOG = LoggerFactory.getLogger(DegraderLoadBalancerStrategyFactoryV3.class);

  private final HealthCheckOperations _healthCheckOperations;
  private final ScheduledExecutorService _executorService;
  private final EventEmitter _eventEmitter;
  private final List<PartitionDegraderLoadBalancerStateListener.Factory> _degraderStateListenerFactories;

  public DegraderLoadBalancerStrategyFactoryV3()
  {
    _healthCheckOperations = null;
    _executorService = null;
    _eventEmitter = new NoopEventEmitter();
    _degraderStateListenerFactories = Collections.emptyList();
  }

  public DegraderLoadBalancerStrategyFactoryV3(HealthCheckOperations healthCheckOperations,
      ScheduledExecutorService executorService, EventEmitter emitter,
      List<PartitionDegraderLoadBalancerStateListener.Factory> degraderStateListenerFactories)
  {
    _healthCheckOperations = healthCheckOperations;
    _executorService = executorService;
    _eventEmitter = (emitter == null) ? new NoopEventEmitter() : emitter;
    _degraderStateListenerFactories = degraderStateListenerFactories;
  }

  @Override
  public DegraderLoadBalancerStrategyV3 newLoadBalancer(ServiceProperties serviceProperties)
  {
    return newLoadBalancer(serviceProperties.getServiceName(),
                           serviceProperties.getLoadBalancerStrategyProperties(),
                           serviceProperties.getDegraderProperties(),
                           serviceProperties.getPath(),
                           serviceProperties.getClusterName());
  }

  private DegraderLoadBalancerStrategyV3 newLoadBalancer(String serviceName,
      Map<String, Object> strategyProperties, Map<String, String> degraderProperties, String path, String clusterName)
  {
    debug(LOG, "created a degrader load balancer strategyV3");

    Map<String, Object> strategyPropertiesCopy = new HashMap<>(strategyProperties);
    // Save the service path as a property -- Quarantine may need this info to construct correct
    // health checking path
    strategyPropertiesCopy.put(PropertyKeys.PATH, path);
    // Also save the clusterName as a property
    strategyPropertiesCopy.put(PropertyKeys.CLUSTER_NAME, clusterName);

    final DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(strategyPropertiesCopy,
                                                                                                                 _healthCheckOperations,
                                                                                                                 _executorService,
                                                                                                                 degraderProperties,
                                                                                                                 _eventEmitter);

    // Adds the default degrader state listener factories
    final List<PartitionDegraderLoadBalancerStateListener.Factory> listeners = new ArrayList<>();
    listeners.add(new DegraderMonitorEventEmitter.Factory(serviceName));
    listeners.addAll(_degraderStateListenerFactories);

    return new DegraderLoadBalancerStrategyV3(config, serviceName, degraderProperties, listeners);
  }
}

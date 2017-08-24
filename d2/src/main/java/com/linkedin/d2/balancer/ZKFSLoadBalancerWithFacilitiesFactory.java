/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.d2.balancer;

import com.linkedin.d2.balancer.event.EventEmitter;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.util.WarmUpLoadBalancer;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import com.linkedin.d2.balancer.zkfs.ZKFSComponentFactory;
import com.linkedin.d2.balancer.zkfs.ZKFSLoadBalancer;
import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of {@link LoadBalancerWithFacilitiesFactory} interface, which creates
 * instance of {@link ZKFSLoadBalancer}.
 */
public class ZKFSLoadBalancerWithFacilitiesFactory implements LoadBalancerWithFacilitiesFactory
{
  private static final Logger LOG = LoggerFactory.getLogger(ZKFSLoadBalancerWithFacilitiesFactory.class);

  @Override
  public LoadBalancerWithFacilities create(D2ClientConfig config)
  {
    LOG.info("Creating D2 LoadBalancer based on ZKFSLoadBalancerWithFacilitiesFactory");

    ZKFSLoadBalancer zkfsLoadBalancer = new ZKFSLoadBalancer(config.zkHosts,
      (int) config.zkSessionTimeoutInMs,
      (int) config.zkStartupTimeoutInMs,
      createLoadBalancerFactory(config),
      config.flagFile,
      config.basePath,
      config.shutdownAsynchronously,
      config.isSymlinkAware,
      config._executorService,
      config.zooKeeperDecorator);

    LoadBalancerWithFacilities balancer = zkfsLoadBalancer;

    if (config.warmUp)
    {
      balancer = new WarmUpLoadBalancer(balancer, zkfsLoadBalancer, config._executorService, config.fsBasePath,
        config.d2ServicePath, config.warmUpTimeoutSeconds, config.warmUpConcurrentRequests);
    }
    return balancer;
  }


  private ZKFSLoadBalancer.TogglingLoadBalancerFactory createLoadBalancerFactory(D2ClientConfig config)
  {
    final ZKFSTogglingLoadBalancerFactoryImpl.ComponentFactory loadBalancerComponentFactory;
    if (config.componentFactory == null)
    {
      loadBalancerComponentFactory = new ZKFSComponentFactory();
    }
    else
    {
      loadBalancerComponentFactory = config.componentFactory;
    }

    final Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        createDefaultLoadBalancerStrategyFactories(config.healthCheckOperations, config._executorService, config.eventEmitter);

    return new ZKFSTogglingLoadBalancerFactoryImpl(loadBalancerComponentFactory,
                                                   config.lbWaitTimeout,
                                                   config.lbWaitUnit,
                                                   config.basePath,
                                                   config.fsBasePath,
                                                   config.clientFactories,
                                                   loadBalancerStrategyFactories,
                                                   config.d2ServicePath,
                                                   config.sslContext,
                                                   config.sslParameters,
                                                   config.isSSLEnabled,
                                                   config.clientServicesConfig,
                                                   config.useNewEphemeralStoreWatcher,
                                                   config.partitionAccessorRegistry,
                                                   config.enableSaveUriDataOnDisk
    );
  }

  private Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> createDefaultLoadBalancerStrategyFactories(
      HealthCheckOperations healthCheckOperations, ScheduledExecutorService executorService, EventEmitter emitter)
  {
    final Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
      new HashMap<>();

    final RandomLoadBalancerStrategyFactory randomStrategyFactory = new RandomLoadBalancerStrategyFactory();
    final DegraderLoadBalancerStrategyFactoryV3 degraderStrategyFactoryV3 = new DegraderLoadBalancerStrategyFactoryV3(
        healthCheckOperations, executorService, emitter);

    loadBalancerStrategyFactories.put("random", randomStrategyFactory);
    loadBalancerStrategyFactories.put("degrader", degraderStrategyFactoryV3);
    loadBalancerStrategyFactories.put("degraderV2", degraderStrategyFactoryV3);
    loadBalancerStrategyFactories.put("degraderV3", degraderStrategyFactoryV3);
    loadBalancerStrategyFactories.put("degraderV2_1", degraderStrategyFactoryV3);

    return loadBalancerStrategyFactories;
  }


}

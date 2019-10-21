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

import com.linkedin.d2.balancer.util.WarmUpLoadBalancer;
import com.linkedin.d2.balancer.zkfs.ZKFSComponentFactory;
import com.linkedin.d2.balancer.zkfs.ZKFSLoadBalancer;
import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl;
import com.linkedin.d2.jmx.D2ClientJmxManager;
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
      balancer = new WarmUpLoadBalancer(balancer, zkfsLoadBalancer, config.startUpExecutorService, config.fsBasePath,
        config.d2ServicePath, config.downstreamServicesFetcher, config.warmUpTimeoutSeconds,
        config.warmUpConcurrentRequests);
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

    D2ClientJmxManager d2ClientJmxManager = new D2ClientJmxManager(config.d2JmxManagerPrefix, config.jmxManager);

    return new ZKFSTogglingLoadBalancerFactoryImpl(loadBalancerComponentFactory,
                                                   config.lbWaitTimeout,
                                                   config.lbWaitUnit,
                                                   config.basePath,
                                                   config.fsBasePath,
                                                   config.clientFactories,
                                                   config.loadBalancerStrategyFactories,
                                                   config.d2ServicePath,
                                                   config.sslContext,
                                                   config.sslParameters,
                                                   config.isSSLEnabled,
                                                   config.clientServicesConfig,
                                                   config.useNewEphemeralStoreWatcher,
                                                   config.partitionAccessorRegistry,
                                                   config.enableSaveUriDataOnDisk,
                                                   config.sslSessionValidatorFactory,
                                                   d2ClientJmxManager
    );
  }
}

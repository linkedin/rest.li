package com.linkedin.d2.xds.balancer;

import com.linkedin.d2.balancer.D2ClientConfig;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.LoadBalancerWithFacilitiesFactory;
import com.linkedin.d2.jmx.D2ClientJmxManager;
import com.linkedin.d2.xds.Node;
import com.linkedin.d2.xds.XdsChannelFactory;


public class XdsLoadBalancerWithFacilitiesFactory implements LoadBalancerWithFacilitiesFactory
{
  @Override
  public LoadBalancerWithFacilities create(D2ClientConfig config)
  {
    D2ClientJmxManager d2ClientJmxManager = new D2ClientJmxManager(config.d2JmxManagerPrefix, config.jmxManager);

    return new XdsLoadBalancer(
        new XdsTogglingLoadBalancerFactory(Node.DEFAULT_NODE, new XdsChannelFactory(), config.xdsServer,
            config.lbWaitTimeout, config.lbWaitUnit, config.fsBasePath, config.clientFactories,
            config.loadBalancerStrategyFactories, config.d2ServicePath, config.sslContext, config.sslParameters,
            config.isSSLEnabled, config.clientServicesConfig, config.partitionAccessorRegistry,
            config.sslSessionValidatorFactory, d2ClientJmxManager, config.deterministicSubsettingMetadataProvider,
            config.failoutConfigProviderFactory, config.canaryDistributionProvider));
  }
}

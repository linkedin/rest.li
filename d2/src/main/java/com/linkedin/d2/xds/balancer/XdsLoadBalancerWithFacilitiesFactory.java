package com.linkedin.d2.xds.balancer;

import com.linkedin.d2.balancer.D2ClientConfig;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.LoadBalancerWithFacilitiesFactory;
import com.linkedin.d2.jmx.D2ClientJmxManager;
import com.linkedin.d2.xds.Node;
import com.linkedin.d2.xds.XdsChannelFactory;
import com.linkedin.d2.xds.XdsClient;
import com.linkedin.d2.xds.XdsClientImpl;
import com.linkedin.d2.xds.XdsToD2PropertiesAdaptor;
import com.linkedin.r2.util.NamedThreadFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class XdsLoadBalancerWithFacilitiesFactory implements LoadBalancerWithFacilitiesFactory
{
  @Override
  public LoadBalancerWithFacilities create(D2ClientConfig config)
  {
    D2ClientJmxManager d2ClientJmxManager = new D2ClientJmxManager(config.d2JmxManagerPrefix, config.jmxManager);
    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
        new NamedThreadFactory("D2 PropertyEventExecutor"));

    XdsClient xdsClient = new XdsClientImpl(Node.DEFAULT_NODE,
        new XdsChannelFactory(config.grpcSslContext, config.xdsServer).createChannel(), executorService);
    XdsToD2PropertiesAdaptor adaptor = new XdsToD2PropertiesAdaptor(xdsClient);

    return new XdsLoadBalancer(adaptor, executorService,
        new XdsTogglingLoadBalancerFactory(config.lbWaitTimeout, config.lbWaitUnit, config.fsBasePath,
            config.clientFactories, config.loadBalancerStrategyFactories, config.d2ServicePath, config.sslContext,
            config.sslParameters, config.isSSLEnabled, config.clientServicesConfig, config.partitionAccessorRegistry,
            config.sslSessionValidatorFactory, d2ClientJmxManager, config.deterministicSubsettingMetadataProvider,
            config.failoutConfigProviderFactory, config.canaryDistributionProvider));
  }
}

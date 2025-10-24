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
import com.linkedin.d2.balancer.util.WarmUpLoadBalancer;
import com.linkedin.d2.balancer.util.downstreams.DownstreamServicesFetcher;
import com.linkedin.d2.balancer.util.downstreams.IndisBasedDownstreamServicesFetcher;
import com.linkedin.d2.jmx.D2ClientJmxManager;
import com.linkedin.d2.xds.Node;
import com.linkedin.d2.xds.XdsChannelFactory;
import com.linkedin.d2.xds.XdsClient;
import com.linkedin.d2.xds.XdsClientImpl;
import com.linkedin.d2.xds.XdsToD2PropertiesAdaptor;
import com.linkedin.r2.util.NamedThreadFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.commons.lang3.ObjectUtils;


/**
 * Implementation of {@link LoadBalancerWithFacilitiesFactory} interface, which creates
 * an instance of {@link XdsLoadBalancer}.
 */
public class XdsLoadBalancerWithFacilitiesFactory implements LoadBalancerWithFacilitiesFactory
{
  @Override
  public boolean isIndisOnly()
  {
    return true;
  }

  @Override
  public LoadBalancerWithFacilities create(D2ClientConfig config)
  {
    D2ClientJmxManager d2ClientJmxManager = new D2ClientJmxManager(config.d2JmxManagerPrefix, config.jmxManager,
        D2ClientJmxManager.DiscoverySourceType.XDS, config.dualReadStateManager);

    if (config.dualReadStateManager != null)
    {
      d2ClientJmxManager.registerDualReadLoadBalancerJmx(config.dualReadStateManager.getDualReadLoadBalancerJmx());
    }
    ScheduledExecutorService executorService = ObjectUtils.defaultIfNull(config.xdsExecutorService,
        Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("D2 xDS PropertyEventExecutor")));
    long xdsStreamReadyTimeout = ObjectUtils.defaultIfNull(config.xdsStreamReadyTimeout,
        XdsClientImpl.DEFAULT_READY_TIMEOUT_MILLIS);
    XdsClient xdsClient = new XdsClientImpl(
        new Node(config.hostName),
        new XdsChannelFactory(config.grpcSslContext, config.xdsServer,
            config.xdsChannelLoadBalancingPolicy, config.xdsChannelLoadBalancingPolicyConfig,
            config.xdsChannelKeepAliveTimeMins).createChannel(),
        executorService,
        xdsStreamReadyTimeout,
        config.subscribeToUriGlobCollection,
        config._xdsServerMetricsProvider,
        config.xdsInitialResourceVersionsEnabled,
        config.xdsStreamMaxRetryBackoffSeconds,
        config.xdsMinimumJavaVersion,
        config.actionOnPrecheckFailure
    );
    d2ClientJmxManager.registerXdsClientJmx(xdsClient.getXdsClientJmx());

    XdsToD2PropertiesAdaptor adaptor = new XdsToD2PropertiesAdaptor(xdsClient, config.dualReadStateManager,
        config.serviceDiscoveryEventEmitter, config.clientServicesConfig);

    XdsDirectory directory = new XdsDirectory(xdsClient);

    XdsLoadBalancer xdsLoadBalancer = new XdsLoadBalancer(
        adaptor,
        executorService,
        new XdsFsTogglingLoadBalancerFactory(config.lbWaitTimeout, config.lbWaitUnit, config.indisFsBasePath,
            config.clientFactories, config.loadBalancerStrategyFactories, config.d2ServicePath, config.sslContext,
            config.sslParameters, config.isSSLEnabled, config.clientServicesConfig, config.partitionAccessorRegistry,
            config.sslSessionValidatorFactory, d2ClientJmxManager, config.deterministicSubsettingMetadataProvider,
            config.failoutConfigProviderFactory, config.canaryDistributionProvider, config.loadBalanceStreamException),
        directory
    );

    LoadBalancerWithFacilities balancer = xdsLoadBalancer;

    if (config.warmUp)
    {
      DownstreamServicesFetcher downstreamServicesFetcher = config.enableIndisDownstreamServicesFetcher
          ? new IndisBasedDownstreamServicesFetcher(xdsClient, config.indisDownstreamServicesFetchTimeout,
              config.xdsExecutorService, config.indisDownstreamServicesFetcher)
          : config.indisDownstreamServicesFetcher;
      balancer = new WarmUpLoadBalancer(balancer, xdsLoadBalancer, config.indisStartUpExecutorService, config.indisFsBasePath,
          config.d2ServicePath, downstreamServicesFetcher, config.indisWarmUpConcurrentRequests, config.indisWarmUpTimeoutSeconds,
          config.dualReadStateManager, true, config.d2CalleeInfoRecorder);
    }

    return balancer;
  }
}

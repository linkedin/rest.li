/*
   Copyright (c) 2017 LinkedIn Corp.

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

import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriPropertiesMerger;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.util.FileSystemDirectory;
import com.linkedin.d2.balancer.util.WarmUpLoadBalancer;
import com.linkedin.d2.balancer.zkfs.LastSeenLoadBalancerWithFacilities;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.zk.LastSeenZKStore;
import com.linkedin.d2.discovery.stores.zk.ZKConnectionBuilder;
import com.linkedin.d2.discovery.stores.zk.ZKPersistentConnection;
import com.linkedin.d2.discovery.stores.zk.builder.ZooKeeperEphemeralStoreBuilder;
import com.linkedin.d2.discovery.stores.zk.builder.ZooKeeperPermanentStoreBuilder;
import com.linkedin.d2.jmx.D2ClientJmxManager;
import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of {@link LoadBalancerWithFacilitiesFactory} interface, which creates an instance of
 * {@link LastSeenLoadBalancerWithFacilities}
 */
public class LastSeenBalancerWithFacilitiesFactory implements LoadBalancerWithFacilitiesFactory
{
  public static final int MATURITY_LEVEL = 1;
  private static final Logger LOG = LoggerFactory.getLogger(LastSeenBalancerWithFacilitiesFactory.class);

  @Override
  public LoadBalancerWithFacilities create(D2ClientConfig config)
  {
    LOG.info("Creating D2 LoadBalancer based on LastSeenLoadBalancerWithFacilities");

    D2ClientJmxManager d2ClientJmxManager = new D2ClientJmxManager(config.d2JmxManagerPrefix, config.jmxManager);

    // init connection
    ZKConnectionBuilder zkConnectionBuilder = new ZKConnectionBuilder(config.zkHosts);
    zkConnectionBuilder.setShutdownAsynchronously(config.shutdownAsynchronously)
      .setIsSymlinkAware(config.isSymlinkAware).setTimeout((int) config.zkSessionTimeoutInMs);

    ZKPersistentConnection zkPersistentConnection;
    if (config.zkConnectionToUseForLB != null)
    {
      LOG.info("LastSeenLoadBalancer using shared connection to zookeeper");
      zkPersistentConnection = config.zkConnectionToUseForLB;
    } else {
      LOG.info("LastSeenLoadBalancer using its own connection to zookeeper");
      zkPersistentConnection = new ZKPersistentConnection(zkConnectionBuilder);
    }

    // init all the stores
    LastSeenZKStore<ClusterProperties> lsClusterStore =
      getClusterPropertiesLastSeenZKStore(config, zkPersistentConnection, d2ClientJmxManager,
                                          config._executorService, config.zookeeperReadWindowMs);
    PropertyEventBus<ClusterProperties> clusterBus = new PropertyEventBusImpl<>(config._executorService);
    clusterBus.setPublisher(lsClusterStore);

    LastSeenZKStore<ServiceProperties> lsServiceStore =
      getServicePropertiesLastSeenZKStore(config, zkPersistentConnection, d2ClientJmxManager,
                                          config._executorService, config.zookeeperReadWindowMs);
    PropertyEventBus<ServiceProperties> serviceBus = new PropertyEventBusImpl<>(config._executorService);
    serviceBus.setPublisher(lsServiceStore);

    LastSeenZKStore<UriProperties> lsUrisStore =
      getUriPropertiesLastSeenZKStore(config, zkPersistentConnection, d2ClientJmxManager,
                                      config._executorService, config.zookeeperReadWindowMs);
    PropertyEventBus<UriProperties> uriBus = new PropertyEventBusImpl<>(config._executorService);
    uriBus.setPublisher(lsUrisStore);

    // create the simple load balancer
    SimpleLoadBalancerState state = new SimpleLoadBalancerState(
      config._executorService, uriBus, clusterBus, serviceBus, config.clientFactories, config.loadBalancerStrategyFactories,
      config.sslContext, config.sslParameters, config.isSSLEnabled, config.partitionAccessorRegistry,
      config.sslSessionValidatorFactory, config.deterministicSubsettingMetadataProvider, config.canaryDistributionProvider);
    d2ClientJmxManager.setSimpleLoadBalancerState(state);

    SimpleLoadBalancer simpleLoadBalancer = new SimpleLoadBalancer(state, config.lbWaitTimeout, config.lbWaitUnit, config._executorService);
    d2ClientJmxManager.setSimpleLoadBalancer(simpleLoadBalancer);

    // add facilities
    LastSeenLoadBalancerWithFacilities lastSeenLoadBalancer = new LastSeenLoadBalancerWithFacilities(simpleLoadBalancer, config.basePath, config.d2ServicePath,
                                                                                 zkPersistentConnection, lsClusterStore, lsServiceStore, lsUrisStore);

    LoadBalancerWithFacilities balancer = lastSeenLoadBalancer;

    if (config.warmUp)
    {
      balancer = new WarmUpLoadBalancer(balancer, lastSeenLoadBalancer, config.startUpExecutorService, config.fsBasePath,
                                        config.d2ServicePath, config.downstreamServicesFetcher, config.warmUpTimeoutSeconds,
                                        config.warmUpConcurrentRequests);
    }

    return balancer;
  }

  private LastSeenZKStore<UriProperties> getUriPropertiesLastSeenZKStore(
    D2ClientConfig config, ZKPersistentConnection zkPersistentConnection, D2ClientJmxManager d2ClientJmxManager,
    ScheduledExecutorService executorService, int zookeeperReadWindowMs)
  {
    ZooKeeperEphemeralStoreBuilder<UriProperties> zkUrisStoreBuilder = new ZooKeeperEphemeralStoreBuilder<UriProperties>()
      .setSerializer(new UriPropertiesJsonSerializer()).setPath(ZKFSUtil.uriPath(config.basePath)).setMerger(new UriPropertiesMerger())
      .setUseNewWatcher(config.useNewEphemeralStoreWatcher)
      .setExecutorService(executorService)
      .setZookeeperReadWindowMs(zookeeperReadWindowMs)
      // register jmx every time the object is created
      .addOnBuildListener(d2ClientJmxManager::setZkUriRegistry);

    FileStore<UriProperties> fileStore = new FileStore<>(config.fsBasePath + File.separator + ZKFSUtil.URI_PATH, new UriPropertiesJsonSerializer());
    d2ClientJmxManager.setFsUriStore(fileStore);

    if (config.enableSaveUriDataOnDisk)
    {
      zkUrisStoreBuilder.setBackupStoreFilePath(config.fsBasePath);
    }

    return new LastSeenZKStore<>(fileStore,
      zkUrisStoreBuilder,
      zkPersistentConnection,
      config._executorService,
      config.warmUpTimeoutSeconds,
      config.warmUpConcurrentRequests
    );
  }

  private LastSeenZKStore<ServiceProperties> getServicePropertiesLastSeenZKStore(
    D2ClientConfig config, ZKPersistentConnection zkPersistentConnection, D2ClientJmxManager d2ClientJmxManager,
    ScheduledExecutorService executorService, int zookeeperReadWindowMs)
  {
    ZooKeeperPermanentStoreBuilder<ServiceProperties> zkServiceStoreBuilder = new ZooKeeperPermanentStoreBuilder<ServiceProperties>()
      .setSerializer(new ServicePropertiesJsonSerializer(config.clientServicesConfig))
      .setPath(ZKFSUtil.servicePath(config.basePath, config.d2ServicePath))
      .setExecutorService(executorService)
      .setZookeeperReadWindowMs(zookeeperReadWindowMs)
      // register jmx every time the object is created
      .addOnBuildListener(d2ClientJmxManager::setZkServiceRegistry);

    FileStore<ServiceProperties> fileStore = new FileStore<>(FileSystemDirectory.getServiceDirectory(config.fsBasePath, config.d2ServicePath), new ServicePropertiesJsonSerializer());
    d2ClientJmxManager.setFsServiceStore(fileStore);

    return new LastSeenZKStore<>(fileStore,
      zkServiceStoreBuilder,
      zkPersistentConnection,
      config._executorService,
      config.warmUpTimeoutSeconds,
      config.warmUpConcurrentRequests
    );
  }

  private LastSeenZKStore<ClusterProperties> getClusterPropertiesLastSeenZKStore(
    D2ClientConfig config, ZKPersistentConnection zkPersistentConnection, D2ClientJmxManager d2ClientJmxManager,
    ScheduledExecutorService executorService, int zookeeperReadWindowMs)
  {
    ZooKeeperPermanentStoreBuilder<ClusterProperties> zkClusterStoreBuilder = new ZooKeeperPermanentStoreBuilder<ClusterProperties>()
      .setSerializer(new ClusterPropertiesJsonSerializer()).setPath(ZKFSUtil.clusterPath(config.basePath))
      .setExecutorService(executorService)
      .setZookeeperReadWindowMs(zookeeperReadWindowMs)
      // register jmx every time the object is created
      .addOnBuildListener(d2ClientJmxManager::setZkClusterRegistry);

    FileStore<ClusterProperties> fileStore = new FileStore<>( FileSystemDirectory.getClusterDirectory(config.fsBasePath), new ClusterPropertiesJsonSerializer());
    d2ClientJmxManager.setFsClusterStore(fileStore);

    return new LastSeenZKStore<>(fileStore,
      zkClusterStoreBuilder,
      zkPersistentConnection,
      config._executorService,
      config.warmUpTimeoutSeconds,
      config.warmUpConcurrentRequests
    );
  }
}

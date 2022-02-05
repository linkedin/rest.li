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

/**
 * $Id: $
 */

package com.linkedin.d2.balancer.zkfs;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriPropertiesMerger;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.simple.SslSessionValidatorFactory;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.subsetting.DeterministicSubsettingMetadataProvider;
import com.linkedin.d2.balancer.util.CanaryDistributionProvider;
import com.linkedin.d2.balancer.util.FileSystemDirectory;
import com.linkedin.d2.balancer.util.TogglingLoadBalancer;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessorRegistry;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessorRegistryImpl;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.toggling.TogglingPublisher;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPropertyMerger;
import com.linkedin.d2.jmx.D2ClientJmxManager;
import com.linkedin.d2.jmx.NoOpJmxManager;
import com.linkedin.r2.transport.common.TransportClientFactory;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating ZK session-specific toggling load balancers.  I.e., this load balancer
 * is bound to a specific ZK session and should be shutdown after that session expires.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ZKFSTogglingLoadBalancerFactoryImpl implements ZKFSLoadBalancer.TogglingLoadBalancerFactory
{
  private final ComponentFactory _factory;
  private final long _lbTimeout;
  private final TimeUnit _lbTimeoutUnit;
  private final String _baseZKPath;
  private final String _fsd2DirPath;
  private final Map<String, TransportClientFactory> _clientFactories;
  private final Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> _loadBalancerStrategyFactories;
  private boolean _enableSaveUriDataOnDisk;
  private final D2ClientJmxManager _d2ClientJmxManager;
  private final int _zookeeperReadWindowMs;
  private final String _d2ServicePath;
  private final SSLContext _sslContext;
  private final SSLParameters _sslParameters;
  private final boolean _isSSLEnabled;
  private final Map<String, Map<String, Object>> _clientServicesConfig;
  private final boolean _useNewEphemeralStoreWatcher;
  private final PartitionAccessorRegistry _partitionAccessorRegistry;
  private final SslSessionValidatorFactory _sslSessionValidatorFactory;
  private final DeterministicSubsettingMetadataProvider _deterministicSubsettingMetadataProvider;
  private final CanaryDistributionProvider _canaryDistributionProvider;

  private static final Logger _log = LoggerFactory.getLogger(ZKFSTogglingLoadBalancerFactoryImpl.class);

  /**
   *
   * @param timeout Timeout for individual LoadBalancer operations
   * @param timeoutUnit Unit for the timeout
   * @param baseZKPath Path to the root ZNode where discovery information is stored
   * @param fsBasePath Path to the root filesystem directory where backup file stores will live
   * @param clientFactories Factory for transport clients
   * @param loadBalancerStrategyFactories Factory for LoadBalancer strategies
   */
  public ZKFSTogglingLoadBalancerFactoryImpl(ComponentFactory factory,
                                             long timeout, TimeUnit timeoutUnit,
                                             String baseZKPath, String fsBasePath,
                                             Map<String, TransportClientFactory> clientFactories,
                                             Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories)
  {
    this(factory, timeout, timeoutUnit,
         baseZKPath, fsBasePath,
         clientFactories, loadBalancerStrategyFactories,
      "", null,
      null, false);
  }

  /**
   *
   * @param timeout Timeout for individual LoadBalancer operations
   * @param timeoutUnit Unit for the timeout
   * @param baseZKPath Path to the root ZNode where discovery information is stored
   * @param fsBasePath Path to the root filesystem directory where backup file stores will live
   * @param clientFactories Factory for transport clients
   * @param loadBalancerStrategyFactories Factory for LoadBalancer strategies
   * @param d2ServicePath  alternate service discovery znodes path, relative to baseZKPath.
   *                       d2ServicePath is "services" if it is an empty string or null.
   * @param sslContext sslContext needed for SSL support
   * @param sslParameters parameters needed for SSL support
   * @param isSSLEnabled boolean whether to enable SSL in the https transport client
   */
  public ZKFSTogglingLoadBalancerFactoryImpl(ComponentFactory factory,
                                             long timeout, TimeUnit timeoutUnit,
                                             String baseZKPath, String fsBasePath,
                                             Map<String, TransportClientFactory> clientFactories,
                                             Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories,
                                             String d2ServicePath,
                                             SSLContext sslContext,
                                             SSLParameters sslParameters,
                                             boolean isSSLEnabled)
  {
    this(factory,
      timeout,
      timeoutUnit,
      baseZKPath,
      fsBasePath,
      clientFactories,
      loadBalancerStrategyFactories,
      d2ServicePath,
      sslContext,
      sslParameters,
      isSSLEnabled,
      Collections.emptyMap(),
      false,
      new PartitionAccessorRegistryImpl(),
      false,
      validationStrings -> null,
      new D2ClientJmxManager("notSpecified", new NoOpJmxManager()),
      ZooKeeperEphemeralStore.DEFAULT_READ_WINDOW_MS);
  }

  public ZKFSTogglingLoadBalancerFactoryImpl(ComponentFactory factory,
                                             long timeout,
                                             TimeUnit timeoutUnit,
                                             String baseZKPath,
                                             String fsBasePath,
                                             Map<String, TransportClientFactory> clientFactories,
                                             Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories,
                                             String d2ServicePath,
                                             SSLContext sslContext,
                                             SSLParameters sslParameters,
                                             boolean isSSLEnabled,
                                             Map<String, Map<String, Object>> clientServicesConfig,
                                             boolean useNewEphemeralStoreWatcher,
                                             PartitionAccessorRegistry partitionAccessorRegistry,
                                             boolean enableSaveUriDataOnDisk,
                                             SslSessionValidatorFactory sslSessionValidatorFactory,
                                             D2ClientJmxManager d2ClientJmxManager,
                                             int zookeeperReadWindowMs)
  {
    this(factory,
        timeout,
        timeoutUnit,
        baseZKPath,
        fsBasePath,
        clientFactories,
        loadBalancerStrategyFactories,
        d2ServicePath,
        sslContext,
        sslParameters,
        isSSLEnabled,
        clientServicesConfig,
        useNewEphemeralStoreWatcher,
        partitionAccessorRegistry,
        enableSaveUriDataOnDisk,
        sslSessionValidatorFactory,
        d2ClientJmxManager,
        zookeeperReadWindowMs,
        null);
  }

  public ZKFSTogglingLoadBalancerFactoryImpl(ComponentFactory factory,
                                             long timeout,
                                             TimeUnit timeoutUnit,
                                             String baseZKPath,
                                             String fsBasePath,
                                             Map<String, TransportClientFactory> clientFactories,
                                             Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories,
                                             String d2ServicePath,
                                             SSLContext sslContext,
                                             SSLParameters sslParameters,
                                             boolean isSSLEnabled,
                                             Map<String, Map<String, Object>> clientServicesConfig,
                                             boolean useNewEphemeralStoreWatcher,
                                             PartitionAccessorRegistry partitionAccessorRegistry,
                                             boolean enableSaveUriDataOnDisk,
                                             SslSessionValidatorFactory sslSessionValidatorFactory,
                                             D2ClientJmxManager d2ClientJmxManager,
                                             int zookeeperReadWindowMs,
                                             DeterministicSubsettingMetadataProvider deterministicSubsettingMetadataProvider)
  {
    this(factory,
            timeout,
            timeoutUnit,
            baseZKPath,
            fsBasePath,
            clientFactories,
            loadBalancerStrategyFactories,
            d2ServicePath,
            sslContext,
            sslParameters,
            isSSLEnabled,
            clientServicesConfig,
            useNewEphemeralStoreWatcher,
            partitionAccessorRegistry,
            enableSaveUriDataOnDisk,
            sslSessionValidatorFactory,
            d2ClientJmxManager,
            zookeeperReadWindowMs,
            deterministicSubsettingMetadataProvider,
            null);
  }

  public ZKFSTogglingLoadBalancerFactoryImpl(ComponentFactory factory,
                                             long timeout,
                                             TimeUnit timeoutUnit,
                                             String baseZKPath,
                                             String fsBasePath,
                                             Map<String, TransportClientFactory> clientFactories,
                                             Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories,
                                             String d2ServicePath,
                                             SSLContext sslContext,
                                             SSLParameters sslParameters,
                                             boolean isSSLEnabled,
                                             Map<String, Map<String, Object>> clientServicesConfig,
                                             boolean useNewEphemeralStoreWatcher,
                                             PartitionAccessorRegistry partitionAccessorRegistry,
                                             boolean enableSaveUriDataOnDisk,
                                             SslSessionValidatorFactory sslSessionValidatorFactory,
                                             D2ClientJmxManager d2ClientJmxManager,
                                             int zookeeperReadWindowMs,
                                             DeterministicSubsettingMetadataProvider deterministicSubsettingMetadataProvider,
                                             CanaryDistributionProvider canaryDistributionProvider)
  {
    _factory = factory;
    _lbTimeout = timeout;
    _lbTimeoutUnit = timeoutUnit;
    _baseZKPath = baseZKPath;
    _fsd2DirPath = fsBasePath;
    _clientFactories = clientFactories;
    _loadBalancerStrategyFactories = loadBalancerStrategyFactories;
    _enableSaveUriDataOnDisk = enableSaveUriDataOnDisk;
    _d2ServicePath = d2ServicePath;
    _sslContext = sslContext;
    _sslParameters = sslParameters;
    _isSSLEnabled = isSSLEnabled;
    _clientServicesConfig = clientServicesConfig;
    _useNewEphemeralStoreWatcher = useNewEphemeralStoreWatcher;
    _partitionAccessorRegistry = partitionAccessorRegistry;
    _sslSessionValidatorFactory = sslSessionValidatorFactory;
    _d2ClientJmxManager = d2ClientJmxManager;
    _zookeeperReadWindowMs = zookeeperReadWindowMs;
    _deterministicSubsettingMetadataProvider = deterministicSubsettingMetadataProvider;
    _canaryDistributionProvider = canaryDistributionProvider;
  }

  @Override
  public TogglingLoadBalancer createLoadBalancer(ZKConnection zkConnection, ScheduledExecutorService executorService)
  {
    _log.info("Using d2ServicePath: " + _d2ServicePath);
    ZooKeeperPermanentStore<ClusterProperties> zkClusterRegistry = createPermanentStore(
      zkConnection, ZKFSUtil.clusterPath(_baseZKPath),
      new ClusterPropertiesJsonSerializer(), executorService, _zookeeperReadWindowMs);
    _d2ClientJmxManager.setZkClusterRegistry(zkClusterRegistry);

    ZooKeeperPermanentStore<ServiceProperties> zkServiceRegistry = createPermanentStore(
      zkConnection, ZKFSUtil.servicePath(_baseZKPath, _d2ServicePath),
      new ServicePropertiesJsonSerializer(_clientServicesConfig), executorService, _zookeeperReadWindowMs);
    _d2ClientJmxManager.setZkServiceRegistry(zkServiceRegistry);

    String backupStoreFilePath = null;
    if (_enableSaveUriDataOnDisk)
    {
      backupStoreFilePath = _fsd2DirPath + File.separator + "urisValues";
    }

    ZooKeeperEphemeralStore<UriProperties> zkUriRegistry =  createEphemeralStore(
      zkConnection, ZKFSUtil.uriPath(_baseZKPath), new UriPropertiesJsonSerializer(),
      new UriPropertiesMerger(), _useNewEphemeralStoreWatcher, backupStoreFilePath, executorService, _zookeeperReadWindowMs);
    _d2ClientJmxManager.setZkUriRegistry(zkUriRegistry);

    FileStore<ClusterProperties> fsClusterStore = createFileStore(FileSystemDirectory.getClusterDirectory(_fsd2DirPath), new ClusterPropertiesJsonSerializer());
    _d2ClientJmxManager.setFsClusterStore(fsClusterStore);

    FileStore<ServiceProperties> fsServiceStore = createFileStore(FileSystemDirectory.getServiceDirectory(_fsd2DirPath, _d2ServicePath), new ServicePropertiesJsonSerializer());
    _d2ClientJmxManager.setFsServiceStore(fsServiceStore);

    FileStore<UriProperties> fsUriStore = createFileStore(_fsd2DirPath + File.separator + "uris", new UriPropertiesJsonSerializer());
    _d2ClientJmxManager.setFsUriStore(fsUriStore);

    PropertyEventBus<ClusterProperties> clusterBus = new PropertyEventBusImpl<>(executorService);
    PropertyEventBus<ServiceProperties> serviceBus = new PropertyEventBusImpl<>(executorService);
    PropertyEventBus<UriProperties> uriBus = new PropertyEventBusImpl<>(executorService);

    // This ensures the filesystem store receives the events from the event bus so that
    // it can keep a local backup.
    clusterBus.register(fsClusterStore);
    serviceBus.register(fsServiceStore);
    uriBus.register(fsUriStore);

    TogglingPublisher<ClusterProperties> clusterToggle = _factory.createClusterToggle(zkClusterRegistry,
                                                                             fsClusterStore,
                                                                             clusterBus);
    TogglingPublisher<ServiceProperties> serviceToggle = _factory.createServiceToggle(zkServiceRegistry,
                                                                             fsServiceStore,
                                                                             serviceBus);
    TogglingPublisher<UriProperties> uriToggle = _factory.createUriToggle(zkUriRegistry, fsUriStore, uriBus);

    SimpleLoadBalancerState state = new SimpleLoadBalancerState(
            executorService, uriBus, clusterBus, serviceBus, _clientFactories, _loadBalancerStrategyFactories,
            _sslContext, _sslParameters, _isSSLEnabled, _partitionAccessorRegistry,
            _sslSessionValidatorFactory, _deterministicSubsettingMetadataProvider, _canaryDistributionProvider);
    _d2ClientJmxManager.setSimpleLoadBalancerState(state);

    SimpleLoadBalancer balancer = new SimpleLoadBalancer(state, _lbTimeout, _lbTimeoutUnit, executorService);
    _d2ClientJmxManager.setSimpleLoadBalancer(balancer);

    TogglingLoadBalancer togLB = _factory.createBalancer(balancer, state, clusterToggle, serviceToggle, uriToggle);
    togLB.start(new Callback<None>() {

      @Override
      public void onError(Throwable e)
      {
        _log.warn("Failed to run start on the TogglingLoadBalancer, may not have registered " +
                          "SimpleLoadBalancer and State with JMX.");
      }

      @Override
      public void onSuccess(None result)
      {
        _log.info("Registered SimpleLoadBalancer and State with JMX.");
      }
    });
    return togLB;
  }

  protected <T> ZooKeeperPermanentStore<T> createPermanentStore(ZKConnection zkConnection, String nodePath,
                                                                PropertySerializer<T> serializer,
                                                                ScheduledExecutorService executorService,
                                                                int zookeeperReadWindowMs)
  {
    return new ZooKeeperPermanentStore<>(zkConnection, serializer, nodePath,
                                         executorService, zookeeperReadWindowMs);
  }

  protected <T> ZooKeeperEphemeralStore<T> createEphemeralStore(ZKConnection zkConnection, String nodePath,
                                                                PropertySerializer<T> serializer,
                                                                ZooKeeperPropertyMerger<T> merger,
                                                                boolean useNewWatcher, String backupStoreFilePath,
                                                                ScheduledExecutorService executorService,
                                                                int readWindow)
  {
    return new ZooKeeperEphemeralStore<>(zkConnection, serializer, merger, nodePath,
      false, useNewWatcher, backupStoreFilePath, executorService, readWindow);
  }

  protected <T> FileStore<T> createFileStore(String path, PropertySerializer<T> serializer)
  {
    return new FileStore<>(path, FileSystemDirectory.FILE_STORE_EXTENSION, serializer);
  }

  public interface ComponentFactory
  {
    TogglingLoadBalancer createBalancer(SimpleLoadBalancer balancer,
                                        SimpleLoadBalancerState state,
                                        TogglingPublisher<ClusterProperties> clusterToggle,
                                        TogglingPublisher<ServiceProperties> serviceToggle,
                                        TogglingPublisher<UriProperties> uriToggle);

    TogglingPublisher<ClusterProperties> createClusterToggle(ZooKeeperPermanentStore<ClusterProperties> zk, FileStore<ClusterProperties> fs, PropertyEventBus<ClusterProperties> bus);
    TogglingPublisher<ServiceProperties> createServiceToggle(ZooKeeperPermanentStore<ServiceProperties> zk, FileStore<ServiceProperties> fs, PropertyEventBus<ServiceProperties> bus);
    TogglingPublisher<UriProperties> createUriToggle(ZooKeeperEphemeralStore<UriProperties> zk, FileStore<UriProperties> fs, PropertyEventBus<UriProperties> bus);

  }

}

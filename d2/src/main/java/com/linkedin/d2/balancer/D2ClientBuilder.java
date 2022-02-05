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

package com.linkedin.d2.balancer;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.backuprequests.BackupRequestsStrategyStatsConsumer;
import com.linkedin.d2.balancer.clients.BackupRequestsClient;
import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.clients.RequestTimeoutClient;
import com.linkedin.d2.balancer.clients.RetryClient;
import com.linkedin.d2.balancer.event.EventEmitter;
import com.linkedin.d2.balancer.simple.SslSessionValidatorFactory;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.subsetting.DeterministicSubsettingMetadataProvider;
import com.linkedin.d2.balancer.util.CanaryDistributionProvider;
import com.linkedin.d2.balancer.util.downstreams.DownstreamServicesFetcher;
import com.linkedin.d2.balancer.util.downstreams.FSBasedDownstreamServicesFetcher;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessorRegistry;
import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.d2.discovery.stores.zk.ZKPersistentConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeper;
import com.linkedin.d2.jmx.JmxManager;
import com.linkedin.d2.jmx.NoOpJmxManager;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.util.NamedThreadFactory;
import com.linkedin.util.ArgumentUtil;
import com.linkedin.util.clock.SystemClock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Build a {@link D2Client} with basic ZooKeeper setup to connect D2 protocol.
 * The client could be further wrapped by other client classes.
 */
@SuppressWarnings("deprecation")
public class D2ClientBuilder
{
  private static final Logger LOG = LoggerFactory.getLogger(D2ClientBuilder.class);

  private boolean _restOverStream = false;
  private final D2ClientConfig _config = new D2ClientConfig();

  /**
   * @return {@link D2Client} that is not started yet. Call start(Callback) to start it.
   */
  public D2Client build()
  {
    final Map<String, TransportClientFactory> transportClientFactories = (_config.clientFactories == null) ?
        createDefaultTransportClientFactories() :  // if user didn't provide transportClientFactories we'll use default ones
        _config.clientFactories;

    List<ScheduledExecutorService> executorsToShutDown= new ArrayList<>();

    if (_config.startUpExecutorService == null)
    {
      // creating an executor that when there are no tasks to execute doesn't create any thread.
      _config.startUpExecutorService =
        Executors.newScheduledThreadPool(0, new NamedThreadFactory("D2 StartupOnlyExecutor"));
      executorsToShutDown.add(_config.startUpExecutorService);
    }

    if (_config._executorService == null)
    {
      LOG.warn("No executor service passed as argument. Pass it for " +
        "enhanced monitoring and to have better control over the executor.");
      _config._executorService =
        Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("D2 PropertyEventExecutor"));
      executorsToShutDown.add(_config._executorService);
    }

    if (_config.downstreamServicesFetcher == null)
    {
      _config.downstreamServicesFetcher = new FSBasedDownstreamServicesFetcher(_config.fsBasePath, _config.d2ServicePath);
    }

    if (_config.jmxManager == null)
    {
      _config.jmxManager = new NoOpJmxManager();
    }

    if(_config.d2ServicePath == null
        // checking empty for backward compatibility with ZKFS behavior
        || _config.d2ServicePath.isEmpty())
    {
      _config.d2ServicePath = ZKFSUtil.SERVICE_PATH;
    }

    final Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        createDefaultLoadBalancerStrategyFactories();

    final D2ClientConfig cfg = new D2ClientConfig(_config.zkHosts,
                  _config.zkSessionTimeoutInMs,
                  _config.zkStartupTimeoutInMs,
                  _config.lbWaitTimeout,
                  _config.lbWaitUnit,
                  _config.flagFile,
                  _config.basePath,
                  _config.fsBasePath,
                  _config.componentFactory,
                  transportClientFactories,
                  _config.lbWithFacilitiesFactory,
                  _config.sslContext,
                  _config.sslParameters,
                  _config.isSSLEnabled,
                  _config.shutdownAsynchronously,
                  _config.isSymlinkAware,
                  _config.clientServicesConfig,
                  _config.d2ServicePath,
                  _config.useNewEphemeralStoreWatcher,
                  _config.healthCheckOperations,
                  _config._executorService,
                  _config.retry,
                  _config.restRetryEnabled,
                  _config.streamRetryEnabled,
                  _config.retryLimit,
                  _config.retryUpdateIntervalMs,
                  _config.retryAggregatedIntervalNum,
                  _config.warmUp,
                  _config.warmUpTimeoutSeconds,
                  _config.warmUpConcurrentRequests,
                  _config.downstreamServicesFetcher,
                  _config.backupRequestsEnabled,
                  _config.backupRequestsStrategyStatsConsumer,
                  _config.backupRequestsLatencyNotificationInterval,
                  _config.backupRequestsLatencyNotificationIntervalUnit,
                  _config.enableBackupRequestsClientAsync,
                  _config._backupRequestsExecutorService,
                  _config.eventEmitter,
                  _config.partitionAccessorRegistry,
                  _config.zooKeeperDecorator,
                  _config.enableSaveUriDataOnDisk,
                  loadBalancerStrategyFactories,
                  _config.requestTimeoutHandlerEnabled,
                  _config.sslSessionValidatorFactory,
                  _config.zkConnectionToUseForLB,
                  _config.startUpExecutorService,
                  _config.jmxManager,
                  _config.d2JmxManagerPrefix,
                  _config.zookeeperReadWindowMs,
                  _config.enableRelativeLoadBalancer,
                  _config.deterministicSubsettingMetadataProvider,
                  _config.canaryDistributionProvider);

    final LoadBalancerWithFacilitiesFactory loadBalancerFactory = (_config.lbWithFacilitiesFactory == null) ?
      new ZKFSLoadBalancerWithFacilitiesFactory() :
      _config.lbWithFacilitiesFactory;

    LoadBalancerWithFacilities loadBalancer = loadBalancerFactory.create(cfg);

    D2Client d2Client = new DynamicClient(loadBalancer, loadBalancer, _restOverStream);

    if (_config.requestTimeoutHandlerEnabled)
    {
      d2Client = new RequestTimeoutClient(d2Client, loadBalancer, _config._executorService);
    }

    if (_config.backupRequestsEnabled)
    {
      ScheduledExecutorService executor = _config._backupRequestsExecutorService;
      if (executor == null) {
        LOG.warn("Backup Requests Executor not configured, creating one with core pool size equal to: " +
            Runtime.getRuntime().availableProcessors());
        executor =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(),
                new NamedThreadFactory("Backup Requests Executor"));
        executorsToShutDown.add(executor);

      }
      d2Client = new BackupRequestsClient(d2Client, loadBalancer, executor,
          _config.backupRequestsStrategyStatsConsumer, _config.backupRequestsLatencyNotificationInterval,
          _config.backupRequestsLatencyNotificationIntervalUnit, _config.enableBackupRequestsClientAsync);
    }

    if (_config.retry)
    {
      d2Client = new RetryClient(d2Client, loadBalancer, _config.retryLimit,
          _config.retryUpdateIntervalMs, _config.retryAggregatedIntervalNum, SystemClock.instance(),
          true, true);
    }
    else if (_config.restRetryEnabled || _config.streamRetryEnabled)
    {
      d2Client = new RetryClient(d2Client, loadBalancer, _config.retryLimit,
          _config.retryUpdateIntervalMs, _config.retryAggregatedIntervalNum, SystemClock.instance(),
          _config.restRetryEnabled, _config.streamRetryEnabled);
    }

    // If we created default transport client factories, we need to shut them down when d2Client
    // is being shut down.
    if (_config.clientFactories != transportClientFactories)
    {
      d2Client = new TransportClientFactoryAwareD2Client(d2Client, transportClientFactories.values());
    }

    if (executorsToShutDown.size() > 0)
    {
      d2Client = new ExecutorShutdownAwareD2Client(d2Client, executorsToShutDown);
    }
    return d2Client;
  }

  public D2ClientBuilder setZkHosts(String zkHosts)
  {
    _config.zkHosts = zkHosts;
    return this;
  }

  public D2ClientBuilder setZkSessionTimeout(long zkSessionTimeout, TimeUnit unit)
  {
    _config.zkSessionTimeoutInMs = unit.toMillis(zkSessionTimeout);
    return this;
  }

  public D2ClientBuilder setZkStartupTimeout(long zkStartupTimeout, TimeUnit unit)
  {
    _config.zkStartupTimeoutInMs = unit.toMillis(zkStartupTimeout);
    return this;
  }

  public D2ClientBuilder setLbWaitTimeout(long lbWaitTimeout, TimeUnit unit)
  {
    _config.lbWaitTimeout = lbWaitTimeout;
    _config.lbWaitUnit = unit;
    return this;
  }

  public D2ClientBuilder setFlagFile(String flagFile)
  {
    _config.flagFile = flagFile;
    return this;
  }

  public D2ClientBuilder setBasePath(String basePath)
  {
    _config.basePath = basePath;
    return this;
  }

  public D2ClientBuilder setFsBasePath(String fsBasePath)
  {
    _config.fsBasePath = fsBasePath;
    return this;
  }

  public D2ClientBuilder setComponentFactory(ZKFSTogglingLoadBalancerFactoryImpl.ComponentFactory componentFactory)
  {
    _config.componentFactory = componentFactory;
    return this;
  }

  public D2ClientBuilder setSSLContext(SSLContext sslContext)
  {
    _config.sslContext = sslContext;
    return this;
  }

  public D2ClientBuilder setSSLParameters(SSLParameters sslParameters)
  {
    _config.sslParameters = sslParameters;
    return this;
  }

  public D2ClientBuilder setIsSSLEnabled(boolean isSSLEnabled)
  {
    _config.isSSLEnabled = isSSLEnabled;
    return this;
  }

  public D2ClientBuilder setShutdownAsynchronously(boolean shutdownAsynchronously)
  {
    _config.shutdownAsynchronously = shutdownAsynchronously;
    return this;
  }

  public D2ClientBuilder setIsSymlinkAware(boolean isSymlinkAware)
  {
    _config.isSymlinkAware = isSymlinkAware;
    return this;
  }

  public D2ClientBuilder setClientServicesConfig(Map<String, Map<String, Object>> clientServicesConfig)
  {
    _config.clientServicesConfig = clientServicesConfig;
    return this;
  }

  public D2ClientBuilder setD2ServicePath(String d2ServicePath)
  {
    _config.d2ServicePath = d2ServicePath;
    return this;
  }

  public D2ClientBuilder setHealthCheckOperations(HealthCheckOperations healthCheckOperations)
  {
    _config.healthCheckOperations = healthCheckOperations;
    return this;
  }

  /**
   * Single-threaded executor service intended to manage the internal eventBus only
   */
  public D2ClientBuilder setExecutorService(ScheduledExecutorService executorService)
  {
    _config._executorService = executorService;
    return this;
  }

  public D2ClientBuilder setBackupRequestsExecutorService(ScheduledExecutorService executorService)
  {
    _config._backupRequestsExecutorService = executorService;
    return this;
  }

  public D2ClientBuilder setRetry(boolean retry)
  {
    _config.retry = retry;
    return this;
  }

  public D2ClientBuilder setRestRetryEnabled(boolean restRetryEnabled)
  {
    _config.restRetryEnabled = restRetryEnabled;
    return this;
  }

  public D2ClientBuilder setStreamRetryEnabled(boolean streamRetryEnabled)
  {
    _config.streamRetryEnabled = streamRetryEnabled;
    return this;
  }

  public D2ClientBuilder setBackupRequestsEnabled(boolean backupRequestsEnabled)
  {
    _config.backupRequestsEnabled = backupRequestsEnabled;
    return this;
  }

  public D2ClientBuilder setBackupRequestsStrategyStatsConsumer(BackupRequestsStrategyStatsConsumer backupRequestsStrategyStatsConsumer)
  {
    _config.backupRequestsStrategyStatsConsumer = backupRequestsStrategyStatsConsumer;
    return this;
  }

  public D2ClientBuilder setBackupRequestsLatencyNotificationInterval(long backupRequestsLatencyNotificationInterval)
  {
    _config.backupRequestsLatencyNotificationInterval = backupRequestsLatencyNotificationInterval;
    return this;
  }

  public D2ClientBuilder setBackupRequestsLatencyNotificationIntervalUnit(TimeUnit backupRequestsLatencyNotificationIntervalUnit)
  {
    _config.backupRequestsLatencyNotificationIntervalUnit = backupRequestsLatencyNotificationIntervalUnit;
    return this;
  }

  public D2ClientBuilder setEnableBackupRequestsClientAsync(boolean enableBackupRequestsClientAsync)
  {
    _config.enableBackupRequestsClientAsync = enableBackupRequestsClientAsync;
    return this;
  }

  public D2ClientBuilder setRetryLimit(int retryLimit)
  {
    _config.retryLimit = retryLimit;
    return this;
  }

  public D2ClientBuilder setRetryUpdateIntervalMs(long retryUpdateIntervalMs)
  {
    _config.retryUpdateIntervalMs = retryUpdateIntervalMs;
    return this;
  }

  public D2ClientBuilder setRetryAggregatedIntervalNum(int retryAggregatedIntervalNum)
  {
    _config.retryAggregatedIntervalNum = retryAggregatedIntervalNum;
    return this;
  }

  public D2ClientBuilder setEventEmitter(EventEmitter eventEmitter)
  {
    _config.eventEmitter = eventEmitter;
    return this;
  }

  /**
   * Specify {@link TransportClientFactory} to generate the client for specific protocol.
   * Caller is responsible to maintain the life cycle of the factories.
   * If not specified, the default client factory map will be used, which is suboptimal in performance
   */
  public D2ClientBuilder setClientFactories(Map<String, TransportClientFactory> clientFactories)
  {
    _config.clientFactories = clientFactories;
    return this;
  }

  public D2ClientBuilder setLoadBalancerWithFacilitiesFactory(LoadBalancerWithFacilitiesFactory lbWithFacilitiesFactory)
  {
    _config.lbWithFacilitiesFactory = lbWithFacilitiesFactory;
    return this;
  }

  public D2ClientBuilder setRestOverStream(boolean restOverStream)
  {
    _restOverStream = restOverStream;
    return this;
  }

  public D2ClientBuilder setUseNewEphemeralStoreWatcher(boolean useNewEphemeralStoreWatcher)
  {
    _config.useNewEphemeralStoreWatcher = useNewEphemeralStoreWatcher;
    return this;
  }

  public D2ClientBuilder setWarmUp(boolean warmUp){
    _config.warmUp = warmUp;
    return this;
  }

  public D2ClientBuilder setWarmUpTimeoutSeconds(int warmUpTimeoutSeconds){
    _config.warmUpTimeoutSeconds = warmUpTimeoutSeconds;
    return this;
  }

  public D2ClientBuilder setZookeeperReadWindowMs(int zookeeperReadWindowMs){
    _config.zookeeperReadWindowMs = zookeeperReadWindowMs;
    return this;
  }

  public D2ClientBuilder setWarmUpConcurrentRequests(int warmUpConcurrentRequests){
    _config.warmUpConcurrentRequests = warmUpConcurrentRequests;
    return this;
  }

  public D2ClientBuilder setDownstreamServicesFetcher(DownstreamServicesFetcher downstreamServicesFetcher)
  {
    _config.downstreamServicesFetcher = downstreamServicesFetcher;
    return this;
  }

  public D2ClientBuilder setEnableSaveUriDataOnDisk(boolean enableSaveUriDataOnDisk){
    _config.enableSaveUriDataOnDisk = enableSaveUriDataOnDisk;
    return this;
  }

  public D2ClientBuilder setPartitionAccessorRegistry(PartitionAccessorRegistry registry)
  {
    _config.partitionAccessorRegistry = registry;
    return this;
  }

  public D2ClientBuilder setZooKeeperDecorator(Function<ZooKeeper, ZooKeeper> zooKeeperDecorator){
    _config.zooKeeperDecorator = zooKeeperDecorator;
    return this;
  }

  public D2ClientBuilder setLoadBalancerStrategyFactories (
      Map<String, LoadBalancerStrategyFactory<?>> loadBalancerStrategyFactories)
  {
    _config.loadBalancerStrategyFactories = loadBalancerStrategyFactories;
    return this;
  }

  public D2ClientBuilder setRequestTimeoutHandlerEnabled(boolean requestTimeoutHandlerEnabled)
  {
    _config.requestTimeoutHandlerEnabled = requestTimeoutHandlerEnabled;
    return this;
  }

  public D2ClientBuilder setZKConnectionForloadBalancer(ZKPersistentConnection connection)
  {
    _config.zkConnectionToUseForLB = connection;
    return this;
  }

  public D2ClientBuilder setSslSessionValidatorFactory(SslSessionValidatorFactory sslSessionValidatorFactory)
  {
    _config.sslSessionValidatorFactory = ArgumentUtil.ensureNotNull(sslSessionValidatorFactory, "sslSessionValidatorFactor");
    return this;
  }

  public D2ClientBuilder setD2JmxManager(JmxManager d2JmxManager)
  {
    _config.jmxManager = d2JmxManager;
    return this;
  }

  public D2ClientBuilder setD2JmxManagerPrefix(String d2JmxManagerPrefix)
  {
    _config.d2JmxManagerPrefix = d2JmxManagerPrefix;
    return this;
  }

  public D2ClientBuilder setEnableRelativeLoadBalancer(boolean enableRelativeLoadBalancer)
  {
    _config.enableRelativeLoadBalancer = enableRelativeLoadBalancer;
    return this;
  }

  public D2ClientBuilder setDeterministicSubsettingMetadataProvider(DeterministicSubsettingMetadataProvider provider)
  {
    _config.deterministicSubsettingMetadataProvider = provider;
    return this;
  }

  public D2ClientBuilder setCanaryDistributionProvider(CanaryDistributionProvider provider)
  {
    _config.canaryDistributionProvider = provider;
    return this;
  }

  private Map<String, TransportClientFactory> createDefaultTransportClientFactories()
  {
    final Map<String, TransportClientFactory> clientFactories = new HashMap<>();
    TransportClientFactory transportClientFactory = new HttpClientFactory.Builder().build();
    clientFactories.put("http", transportClientFactory);
    clientFactories.put("https", transportClientFactory);
    return clientFactories;
  }

  /**
   * Adds the default load balancer strategy factories only if they are not present in the provided factories
   * during the transition period.
   *
   * @return Default mapping of the load balancer strategy names and the strategies
   */
  private Map<String, LoadBalancerStrategyFactory<?>> createDefaultLoadBalancerStrategyFactories()
  {
    final Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        new HashMap<>(_config.loadBalancerStrategyFactories);

    final RandomLoadBalancerStrategyFactory randomStrategyFactory = new RandomLoadBalancerStrategyFactory();
    loadBalancerStrategyFactories.putIfAbsent("random", randomStrategyFactory);

    final DegraderLoadBalancerStrategyFactoryV3 degraderStrategyFactoryV3 = new DegraderLoadBalancerStrategyFactoryV3(
        _config.healthCheckOperations, _config._executorService, _config.eventEmitter, Collections.emptyList());
    loadBalancerStrategyFactories.putIfAbsent("degrader", degraderStrategyFactoryV3);
    loadBalancerStrategyFactories.putIfAbsent("degraderV2", degraderStrategyFactoryV3);
    loadBalancerStrategyFactories.putIfAbsent("degraderV3", degraderStrategyFactoryV3);
    loadBalancerStrategyFactories.putIfAbsent("degraderV2_1", degraderStrategyFactoryV3);

    if (_config.enableRelativeLoadBalancer)
    {
      final RelativeLoadBalancerStrategyFactory relativeLoadBalancerStrategyFactory = new RelativeLoadBalancerStrategyFactory(
          _config._executorService, _config.healthCheckOperations, Collections.emptyList(), _config.eventEmitter,
          SystemClock.instance());
      loadBalancerStrategyFactories.putIfAbsent(RelativeLoadBalancerStrategy.RELATIVE_LOAD_BALANCER_STRATEGY_NAME,
          relativeLoadBalancerStrategyFactory);
    }

    return loadBalancerStrategyFactories;
  }

  private class TransportClientFactoryAwareD2Client extends D2ClientDelegator
  {
    private Collection<TransportClientFactory> _clientFactories;

    TransportClientFactoryAwareD2Client(D2Client d2Client, Collection<TransportClientFactory> clientFactories)
    {
      super(d2Client);
      _clientFactories = clientFactories;
    }

    @Override
    public void shutdown(Callback<None> callback)
    {
      _d2Client.shutdown(callback);

      for (TransportClientFactory clientFactory: _clientFactories)
      {
        clientFactory.shutdown(new FutureCallback<>());
      }
    }
  }

  private class ExecutorShutdownAwareD2Client extends D2ClientDelegator
  {
    private List<ScheduledExecutorService> _executors;

    ExecutorShutdownAwareD2Client(D2Client d2Client, List<ScheduledExecutorService> executors)
    {
      super(d2Client);
      _executors = executors;
    }

    @Override
    public void shutdown(Callback<None> callback)
    {
      _d2Client.shutdown(new Callback<None>()
      {
        @Override
        public void onError(Throwable e)
        {
          _executors.forEach(ExecutorService::shutdown);
          callback.onError(e);
        }

        @Override
        public void onSuccess(None result)
        {
          _executors.forEach(ExecutorService::shutdown);
          callback.onSuccess(result);
        }
      });
    }
  }
}

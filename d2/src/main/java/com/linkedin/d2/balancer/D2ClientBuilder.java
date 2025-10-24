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
import com.linkedin.d2.balancer.clients.FailoutClient;
import com.linkedin.d2.balancer.clients.FailoutRedirectStrategy;
import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.clients.RequestTimeoutClient;
import com.linkedin.d2.balancer.util.D2CalleeInfoRecorder;
import java.time.Duration;
import javax.annotation.Nonnull;
import com.linkedin.d2.balancer.clients.RetryClient;
import com.linkedin.d2.balancer.clusterfailout.FailoutConfigProviderFactory;
import com.linkedin.d2.balancer.dualread.DualReadStateManager;
import com.linkedin.d2.balancer.event.EventEmitter;
import com.linkedin.d2.balancer.simple.SslSessionValidatorFactory;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.subsetting.DeterministicSubsettingMetadataProvider;
import com.linkedin.d2.balancer.util.canary.CanaryDistributionProvider;
import com.linkedin.d2.balancer.util.downstreams.DownstreamServicesFetcher;
import com.linkedin.d2.balancer.util.downstreams.FSBasedDownstreamServicesFetcher;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessorRegistry;
import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl;
import com.linkedin.d2.balancer.zkfs.ZKFSUtil;
import com.linkedin.d2.discovery.event.ServiceDiscoveryEventEmitter;
import com.linkedin.d2.discovery.stores.zk.ZKPersistentConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeper;
import com.linkedin.d2.jmx.XdsServerMetricsProvider;
import com.linkedin.d2.jmx.JmxManager;
import com.linkedin.d2.xds.XdsClientValidator;
import com.linkedin.d2.jmx.NoOpJmxManager;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.util.NamedThreadFactory;
import com.linkedin.util.ArgumentUtil;
import com.linkedin.util.clock.SystemClock;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.*;


/**
 * ATTENTION: Using this class MUST be reading from INDIS instead of Zookeeper. ZK read will crash in October 2025.
 * See instructions at go/onboardindis.
 * Build a {@link D2Client} with basic setup to connect D2 protocol.
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
    if (!_config.disableDetectLiRawD2Client && isLiRawD2Client())
    {
      LOG.warn("ATTENTION: Using hard-coded D2ClientBuilder to create a raw LI D2 client. Always consider using the "
          + "D2DefaultClientFactory in container. Raw D2 client will not have future features and migrations done "
          + "automatically, requiring lots of manual toil from your team.");
      _config.isLiRawD2Client = true;
    }

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

    if (_config.indisStartUpExecutorService == null)
    {
      _config.indisStartUpExecutorService =
          Executors.newScheduledThreadPool(0, new NamedThreadFactory("INDIS D2 StartupOnlyExecutor"));
      executorsToShutDown.add(_config.indisStartUpExecutorService);
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

    if (_config.indisDownstreamServicesFetcher == null)
    {
      _config.indisDownstreamServicesFetcher = new FSBasedDownstreamServicesFetcher(_config.indisFsBasePath, _config.d2ServicePath);
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
                  _config.xdsServer,
                  _config.hostName,
                  _config.zkSessionTimeoutInMs,
                  _config.zkStartupTimeoutInMs,
                  _config.lbWaitTimeout,
                  _config.lbWaitUnit,
                  _config.flagFile,
                  _config.basePath,
                  _config.fsBasePath,
                  _config.indisFsBasePath,
                  _config.componentFactory,
                  transportClientFactories,
                  _config.lbWithFacilitiesFactory,
                  _config.sslContext,
                  _config.grpcSslContext,
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
                  _config.indisWarmUpTimeoutSeconds,
                  _config.warmUpConcurrentRequests,
                  _config.indisWarmUpConcurrentRequests,
                  _config.downstreamServicesFetcher,
                  _config.indisDownstreamServicesFetcher,
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
                  _config.indisStartUpExecutorService,
                  _config.jmxManager,
                  _config.d2JmxManagerPrefix,
                  _config.zookeeperReadWindowMs,
                  _config.enableRelativeLoadBalancer,
                  _config.deterministicSubsettingMetadataProvider,
                  _config.canaryDistributionProvider,
                  _config.enableClusterFailout,
                  _config.failoutConfigProviderFactory,
                  _config.failoutRedirectStrategy,
                  _config.serviceDiscoveryEventEmitter,
                  _config.dualReadStateManager,
                  _config.xdsExecutorService,
                  _config.xdsStreamReadyTimeout,
                  _config.dualReadNewLbExecutor,
                  _config.xdsChannelLoadBalancingPolicy,
                  _config.xdsChannelLoadBalancingPolicyConfig,
                  _config.subscribeToUriGlobCollection,
                  _config._xdsServerMetricsProvider,
                  _config.loadBalanceStreamException,
                  _config.xdsInitialResourceVersionsEnabled,
                  _config.disableDetectLiRawD2Client,
                  _config.isLiRawD2Client,
                  _config.xdsStreamMaxRetryBackoffSeconds,
                  _config.xdsChannelKeepAliveTimeMins,
                  _config.xdsMinimumJavaVersion,
                  _config.actionOnPrecheckFailure,
                  _config.d2CalleeInfoRecorder,
                  _config.enableIndisDownstreamServicesFetcher,
                  _config.indisDownstreamServicesFetchTimeout
    );

    final LoadBalancerWithFacilitiesFactory loadBalancerFactory = (_config.lbWithFacilitiesFactory == null) ?
      new ZKFSLoadBalancerWithFacilitiesFactory() : _config.lbWithFacilitiesFactory;

    // log error for not using INDIS in raw d2 client
    if (_config.isLiRawD2Client && !loadBalancerFactory.isIndisOnly())
    {
      String stackTrace = Arrays.stream(Thread.currentThread().getStackTrace())
          .map(StackTraceElement::toString)
          .collect(Collectors.joining("\n"));
      //TODO: After Oct 1st, throw exception to hard fail non INDIS raw d2 client.
      // throw new IllegalStateException("Creating Zookeeper-reading raw D2 Client in app-custom code is prohibited. "
      //    + "See instructions at go/onboardindis to find the code owner and migrate to INDIS.\n");
      LOG.error("[ATTENTION!!! ACTION REQUIRED] Creating Zookeeper-reading raw D2 Client in app-custom code WILL CRASH"
          + " after OCTOBER 1st 2025. See instructions at go/onboardindis to find the code owner and migrate to INDIS.\n"
          + "Using in stack: {}", stackTrace);
    }

    if (loadBalancerFactory.isIndisOnly() && cfg.xdsServer == null)
    {
      throw new IllegalStateException("xdsServer is null. Call setXdsServer with a valid indis server address. "
          + "Reference go/onboardindis for guidelines.");
    }

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

    if (_config.enableClusterFailout)
    {
      if (_config.failoutRedirectStrategy == null)
      {
        LOG.warn("A URI rewrite strategy is required for failout.");
      }
      else
      {
        LOG.info("Enabling D2Client failout support");
        d2Client = new FailoutClient(d2Client, loadBalancer, _config.failoutRedirectStrategy);
      }
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

  /**
   * Check if the d2 client builder is to build a LI raw D2 client. When LI container D2ClientFactory is used, it sets
   * hostName and d2JmxManagerPrefix with LI-specific values (app name, machine name, etc) at runtime. All LI raw D2
   * client usages are known not setting these values according to code search.
   * @return true if this is a LI raw D2 client, false otherwise.
   */
  private boolean isLiRawD2Client()
  {
    return Objects.equals(_config.hostName, D2ClientConfig.HOST_NAME_DEFAULT)
        || Objects.equals(_config.d2JmxManagerPrefix, D2ClientConfig.D2_JMX_MANAGER_PREFIX_DEFAULT);
  }

  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. Use setXdsServer instead. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  @Deprecated
  public D2ClientBuilder setZkHosts(String zkHosts)
  {
    _config.zkHosts = zkHosts;
    return this;
  }

  public D2ClientBuilder setXdsServer(String xdsServer)
  {
    if (_config.lbWithFacilitiesFactory == null || _config.lbWithFacilitiesFactory.isIndisOnly())
    {
      checkNotNull(xdsServer, "xdsServer");
    }
    _config.xdsServer = xdsServer;
    return this;
  }

  public D2ClientBuilder setHostName(String hostName)
  {
    _config.hostName = hostName;
    return this;
  }

  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. Use setXdsServer instead. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  @Deprecated
  public D2ClientBuilder setZkSessionTimeout(long zkSessionTimeout, TimeUnit unit)
  {
    _config.zkSessionTimeoutInMs = unit.toMillis(zkSessionTimeout);
    return this;
  }

  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  @Deprecated
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

  public D2ClientBuilder setIndisFsBasePath(String indisFsBasePath)
  {
    _config.indisFsBasePath = indisFsBasePath;
    return this;
  }

  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  @Deprecated
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

  public D2ClientBuilder setGrpcSslContext(SslContext grpcSslContext)
  {
    _config.grpcSslContext = grpcSslContext;
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

  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. INDIS always support symlink. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  @Deprecated
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

  /**
   * Legacy feature that has been deprecated for years. Do not use.
   */
  @Deprecated
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

  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  @Deprecated
  public D2ClientBuilder setUseNewEphemeralStoreWatcher(boolean useNewEphemeralStoreWatcher)
  {
    _config.useNewEphemeralStoreWatcher = useNewEphemeralStoreWatcher;
    return this;
  }

  public D2ClientBuilder setWarmUp(boolean warmUp){
    _config.warmUp = warmUp;
    return this;
  }

  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  public D2ClientBuilder setWarmUpTimeoutSeconds(int warmUpTimeoutSeconds)
  {
    _config.warmUpTimeoutSeconds = warmUpTimeoutSeconds;
    return this;
  }

  public D2ClientBuilder setIndisWarmUpTimeoutSeconds(int indisWarmUpTimeoutSeconds)
  {
    _config.indisWarmUpTimeoutSeconds = indisWarmUpTimeoutSeconds;
    return this;
  }

  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  @Deprecated
  public D2ClientBuilder setZookeeperReadWindowMs(int zookeeperReadWindowMs)
  {
    _config.zookeeperReadWindowMs = zookeeperReadWindowMs;
    return this;
  }

  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  @Deprecated
  public D2ClientBuilder setWarmUpConcurrentRequests(int warmUpConcurrentRequests)
  {
    _config.warmUpConcurrentRequests = warmUpConcurrentRequests;
    return this;
  }

  public D2ClientBuilder setIndisWarmUpConcurrentRequests(int indisWarmUpConcurrentRequests)
  {
    _config.indisWarmUpConcurrentRequests = indisWarmUpConcurrentRequests;
    return this;
  }

  public D2ClientBuilder setStartUpExecutorService(ScheduledExecutorService executorService)
  {
    _config.startUpExecutorService = executorService;
    return this;
  }

  public D2ClientBuilder setIndisStartUpExecutorService(ScheduledExecutorService executorService)
  {
    _config.indisStartUpExecutorService = executorService;
    return this;
  }

  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  @Deprecated
  public D2ClientBuilder setDownstreamServicesFetcher(DownstreamServicesFetcher downstreamServicesFetcher)
  {
    _config.downstreamServicesFetcher = downstreamServicesFetcher;
    return this;
  }

  public D2ClientBuilder setIndisDownstreamServicesFetcher(DownstreamServicesFetcher indisDownstreamServicesFetcher)
  {
    _config.indisDownstreamServicesFetcher = indisDownstreamServicesFetcher;
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

  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  @Deprecated
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

  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  @Deprecated
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

  public D2ClientBuilder setEnableClusterFailout(boolean enableClusterFailout)
  {
    _config.enableClusterFailout = enableClusterFailout;
    return this;
  }

  public D2ClientBuilder setFailoutConfigProviderFactory(FailoutConfigProviderFactory failoutConfigProviderFactory)
  {
    _config.failoutConfigProviderFactory = failoutConfigProviderFactory;
    return this;
  }

  public D2ClientBuilder setFailoutRedirectStrategy(FailoutRedirectStrategy failoutRedirectStrategy)
  {
    _config.failoutRedirectStrategy = failoutRedirectStrategy;
    return this;
  }

  public D2ClientBuilder setServiceDiscoveryEventEmitter(ServiceDiscoveryEventEmitter emitter) {
    _config.serviceDiscoveryEventEmitter = emitter;
    return this;
  }

  public D2ClientBuilder setDualReadStateManager(DualReadStateManager dualReadStateManager) {
    _config.dualReadStateManager = dualReadStateManager;
    return this;
  }

  public D2ClientBuilder setDualReadNewLbExecutor(ExecutorService dualReadNewLbExecutor) {
    _config.dualReadNewLbExecutor = dualReadNewLbExecutor;
    return this;
  }

  /**
   * Single-threaded executor service for xDS communication.
   */
  public D2ClientBuilder setXdsExecutorService(ScheduledExecutorService xdsExecutorService) {
    _config.xdsExecutorService = xdsExecutorService;
    return this;
  }

  public D2ClientBuilder setXdsStreamReadyTimeout(long xdsStreamReadyTimeout) {
    _config.xdsStreamReadyTimeout = xdsStreamReadyTimeout;
    return this;
  }

  public D2ClientBuilder setXdsChannelLoadBalancingPolicy(String xdsChannelLoadBalancingPolicy) {
    _config.xdsChannelLoadBalancingPolicy = xdsChannelLoadBalancingPolicy;
    return this;
  }

  public D2ClientBuilder xdsChannelLoadBalancingPolicyConfig(Map<String, ?> xdsChannelLoadBalancingPolicyConfig) {
    _config.xdsChannelLoadBalancingPolicyConfig = xdsChannelLoadBalancingPolicyConfig;
    return this;
  }

  public D2ClientBuilder setXdsChannelKeepAliveTimeMins(Long keepAliveTimeMins) {
    _config.xdsChannelKeepAliveTimeMins = keepAliveTimeMins;
    return this;
  }

  public D2ClientBuilder setSubscribeToUriGlobCollection(boolean subscribeToUriGlobCollection) {
    _config.subscribeToUriGlobCollection = subscribeToUriGlobCollection;
    return this;
  }

  public D2ClientBuilder setXdsServerMetricsProvider(XdsServerMetricsProvider xdsServerMetricsProvider) {
    _config._xdsServerMetricsProvider = xdsServerMetricsProvider;
    return this;
  }

  public D2ClientBuilder setLoadBalanceStreamException(boolean loadBalanceStreamException) {
    _config.loadBalanceStreamException = loadBalanceStreamException;
    return this;
  }

  public D2ClientBuilder setXdsInitialResourceVersionsEnabled(boolean xdsIRVEnabled)
  {
    _config.xdsInitialResourceVersionsEnabled = xdsIRVEnabled;
    return this;
  }

  public D2ClientBuilder setXdsStreamMaxRetryBackoffSeconds(int xdsStreamMaxRetryBackoffSeconds)
  {
    _config.xdsStreamMaxRetryBackoffSeconds = xdsStreamMaxRetryBackoffSeconds;
    return this;
  }

  public D2ClientBuilder setXdsMinimumJavaVersion(String xdsMinimumJavaVersion)
  {
    _config.xdsMinimumJavaVersion = xdsMinimumJavaVersion;
    return this;
  }

  public D2ClientBuilder setActionOnPrecheckFailure(XdsClientValidator.ActionOnPrecheckFailure actionOnPrecheckFailure)
  {
    _config.actionOnPrecheckFailure = actionOnPrecheckFailure;
    return this;
  }

  /**
   * Disable the detection of LI raw D2 client. This is intended for non-LI users who want to use this class to build
   * a D2 client. All LI apps/jobs should NEVER set this to true (unless for test apps from service discovery team).
   * When hostName and d2JmxManagerPrefix are not set, the app/job will be detected as a LI raw D2 client.
   * @param disableDetectLiRawD2Client true to disable the detection, false to enable it.
   * @return this builder.
   */
  public D2ClientBuilder setDisableDetectLiRawD2Client(boolean disableDetectLiRawD2Client)
  {
    _config.disableDetectLiRawD2Client = disableDetectLiRawD2Client;
    return this;
  }

  public D2ClientBuilder setD2CalleeInfoRecorder(D2CalleeInfoRecorder d2CalleeInfoRecorder)
  {
    _config.d2CalleeInfoRecorder = d2CalleeInfoRecorder;
    return this;
  }

  public D2ClientBuilder setIndisDownstreamServicesFetchTimeout(Duration indisDownstreamServicesFetchTimeout) {
    _config.indisDownstreamServicesFetchTimeout = indisDownstreamServicesFetchTimeout;
    return this;
  }

  public D2ClientBuilder setEnableIndisDownstreamServicesFetcher(boolean enableIndisDownstreamServicesFetcher) {
    _config.enableIndisDownstreamServicesFetcher = enableIndisDownstreamServicesFetcher;
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
      // TODO: create StateUpdater.LoadBalanceConfig and pass it to the RelativeLoadBalancerStrategyFactory
      final RelativeLoadBalancerStrategyFactory relativeLoadBalancerStrategyFactory = new RelativeLoadBalancerStrategyFactory(
          _config._executorService, _config.healthCheckOperations, Collections.emptyList(), _config.eventEmitter,
          SystemClock.instance(), _config.loadBalanceStreamException);
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

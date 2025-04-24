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

import com.linkedin.d2.backuprequests.BackupRequestsStrategyStatsConsumer;
import com.linkedin.d2.balancer.clients.FailoutRedirectStrategy;
import com.linkedin.d2.balancer.clients.RetryClient;
import com.linkedin.d2.balancer.clusterfailout.FailoutConfigProviderFactory;
import com.linkedin.d2.balancer.dualread.DualReadStateManager;
import com.linkedin.d2.balancer.event.EventEmitter;
import com.linkedin.d2.balancer.simple.SslSessionValidatorFactory;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.subsetting.DeterministicSubsettingMetadataProvider;
import com.linkedin.d2.balancer.util.canary.CanaryDistributionProvider;
import com.linkedin.d2.balancer.util.WarmUpLoadBalancer;
import com.linkedin.d2.balancer.util.downstreams.DownstreamServicesFetcher;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessorRegistry;
import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl;
import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl.ComponentFactory;
import com.linkedin.d2.discovery.event.LogOnlyServiceDiscoveryEventEmitter;
import com.linkedin.d2.discovery.event.ServiceDiscoveryEventEmitter;
import com.linkedin.d2.discovery.stores.zk.ZKPersistentConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeper;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperStore;
import com.linkedin.d2.jmx.XdsServerMetricsProvider;
import com.linkedin.d2.jmx.JmxManager;
import com.linkedin.d2.jmx.NoOpXdsServerMetricsProvider;
import com.linkedin.d2.jmx.NoOpJmxManager;
import com.linkedin.r2.transport.common.TransportClientFactory;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class D2ClientConfig
{
  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. Use xdsServer instead. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  @Deprecated
  String zkHosts = null;
  public String xdsServer = null;
  public String hostName = HOST_NAME_DEFAULT;
  public static final String HOST_NAME_DEFAULT = null;
  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  @Deprecated
  long zkSessionTimeoutInMs = 3600000L;
  @Deprecated
  long zkStartupTimeoutInMs = 10000L;
  @Deprecated
  ZKFSTogglingLoadBalancerFactoryImpl.ComponentFactory componentFactory = null;
  @Deprecated
  boolean useNewEphemeralStoreWatcher = true;
  @Deprecated
  public int warmUpTimeoutSeconds = WarmUpLoadBalancer.DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS;
  @Deprecated
  public int warmUpConcurrentRequests = WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS;
  @Deprecated
  public DownstreamServicesFetcher downstreamServicesFetcher = null;
  @Deprecated
  Function<ZooKeeper, ZooKeeper> zooKeeperDecorator = null;
  @Deprecated
  int zookeeperReadWindowMs = ZooKeeperStore.DEFAULT_READ_WINDOW_MS;
  @Deprecated
  ZKPersistentConnection zkConnectionToUseForLB = null;

  public long lbWaitTimeout = 5000L;
  public TimeUnit lbWaitUnit = TimeUnit.MILLISECONDS;
  String flagFile = "/no/flag/file/set";
  String basePath = "/d2";
  public String fsBasePath = "/tmp/d2";
  public String indisFsBasePath = "/tmp/d2/indis";

  public Map<String, TransportClientFactory> clientFactories = null;
  LoadBalancerWithFacilitiesFactory lbWithFacilitiesFactory = null;
  /**
   * Legacy feature that has been deprecated for years. Do not use.
   */
  @Deprecated
  public String d2ServicePath = null;
  public SSLContext sslContext = null;
  public SslContext grpcSslContext = null;
  public SSLParameters sslParameters = null;
  public boolean isSSLEnabled = false;
  boolean shutdownAsynchronously = false;
  /**
   * @deprecated ZK-based D2 is deprecated. Please onboard to INDIS. INDIS always support symlink. See instructions at
   * https://iwww.corp.linkedin.com/wiki/cf/display/ENGS/INDIS+Rollout+Issue+Guidelines+for+Java+Apps
   */
  @Deprecated
  boolean isSymlinkAware = true;
  public Map<String, Map<String, Object>> clientServicesConfig = Collections.<String, Map<String, Object>>emptyMap();

  HealthCheckOperations healthCheckOperations = null;
  boolean enableSaveUriDataOnDisk = false;
  /**
   * By default is a single threaded executor
   */
  ScheduledExecutorService _executorService = null;
  ScheduledExecutorService _backupRequestsExecutorService = null;

  /**
   * @deprecated Use restRetryEnabled and streamRetryEnabled instead
   */
  @Deprecated()
  boolean retry = false;

  boolean restRetryEnabled = false;
  boolean streamRetryEnabled = false;
  int retryLimit = DEFAULT_RETRY_LIMIT;
  long retryUpdateIntervalMs = RetryClient.DEFAULT_UPDATE_INTERVAL_MS;
  int retryAggregatedIntervalNum = RetryClient.DEFAULT_AGGREGATED_INTERVAL_NUM;
  public boolean warmUp = true;
  public int indisWarmUpTimeoutSeconds = WarmUpLoadBalancer.DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS;
  public int indisWarmUpConcurrentRequests = WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS;
  public DownstreamServicesFetcher indisDownstreamServicesFetcher = null;
  boolean backupRequestsEnabled = true;
  BackupRequestsStrategyStatsConsumer backupRequestsStrategyStatsConsumer = null;
  long backupRequestsLatencyNotificationInterval = 1;
  TimeUnit backupRequestsLatencyNotificationIntervalUnit = TimeUnit.MINUTES;
  // TODO: Once the change is fully verified, we should always enable the async feature
  boolean enableBackupRequestsClientAsync = false;
  EventEmitter eventEmitter = null;
  public PartitionAccessorRegistry partitionAccessorRegistry = null;
  public Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories = Collections.emptyMap();
  boolean requestTimeoutHandlerEnabled = false;
  public SslSessionValidatorFactory sslSessionValidatorFactory = null;
  public ScheduledExecutorService startUpExecutorService = null;
  public ScheduledExecutorService indisStartUpExecutorService = null;
  public JmxManager jmxManager = new NoOpJmxManager();
  public String d2JmxManagerPrefix = D2_JMX_MANAGER_PREFIX_DEFAULT;
  public static final String D2_JMX_MANAGER_PREFIX_DEFAULT = "UnknownPrefix";
  boolean enableRelativeLoadBalancer = false;
  public DeterministicSubsettingMetadataProvider deterministicSubsettingMetadataProvider = null;
  public CanaryDistributionProvider canaryDistributionProvider = null;
  public static final int DEFAULT_RETRY_LIMIT = 3;
  boolean enableClusterFailout = false;
  public FailoutConfigProviderFactory failoutConfigProviderFactory;
  FailoutRedirectStrategy failoutRedirectStrategy;
  public ServiceDiscoveryEventEmitter serviceDiscoveryEventEmitter = new LogOnlyServiceDiscoveryEventEmitter(); // default to use log-only emitter
  public DualReadStateManager dualReadStateManager = null;

  public ScheduledExecutorService xdsExecutorService = null;
  public Long xdsStreamReadyTimeout = null;
  public ExecutorService dualReadNewLbExecutor = null;
  public String xdsChannelLoadBalancingPolicy = null;
  public Map<String, ?> xdsChannelLoadBalancingPolicyConfig = null;
  public boolean subscribeToUriGlobCollection = false;
  public XdsServerMetricsProvider _xdsServerMetricsProvider = new NoOpXdsServerMetricsProvider();
  public boolean loadBalanceStreamException = false;
  public boolean xdsInitialResourceVersionsEnabled = false;

  /**
   * When this is true, D2 client builder will detect if it's used to build a raw D2 client (as opposed to using standard
   * D2 client factory in LI container library) and set the isLiRawD2Client flag accordingly in the load balancer factory.
   * Open Source Users can set this to false to disable this behavior.
   */
  public boolean detectLiRawD2Client = true;

  public D2ClientConfig()
  {
  }

  D2ClientConfig(String zkHosts,
                 String xdsServer,
                 String hostName,
                 long zkSessionTimeoutInMs,
                 long zkStartupTimeoutInMs,
                 long lbWaitTimeout,
                 TimeUnit lbWaitUnit,
                 String flagFile,
                 String basePath,
                 String fsBasePath,
                 String indisFsBasePath,
                 ComponentFactory componentFactory,
                 Map<String, TransportClientFactory> clientFactories,
                 LoadBalancerWithFacilitiesFactory lbWithFacilitiesFactory,
                 SSLContext sslContext,
                 SslContext grpcSslContext,
                 SSLParameters sslParameters,
                 boolean isSSLEnabled,
                 boolean shutdownAsynchronously,
                 boolean isSymlinkAware,
                 Map<String, Map<String, Object>> clientServicesConfig,
                 String d2ServicePath,
                 boolean useNewEphemeralStoreWatcher,
                 HealthCheckOperations healthCheckOperations,
                 ScheduledExecutorService executorService,
                 boolean retry,
                 boolean restRetryEnabled,
                 boolean streamRetryEnabled,
                 int retryLimit,
                 long retryUpdateIntervalMs,
                 int retryAggregatedIntervalNum,
                 boolean warmUp,
                 int warmUpTimeoutSeconds,
                 int indisWarmUpTimeoutSeconds,
                 int warmUpConcurrentRequests,
                 int indisWarmUpConcurrentRequests,
                 DownstreamServicesFetcher downstreamServicesFetcher,
                 DownstreamServicesFetcher indisDownstreamServicesFetcher,
                 boolean backupRequestsEnabled,
                 BackupRequestsStrategyStatsConsumer backupRequestsStrategyStatsConsumer,
                 long backupRequestsLatencyNotificationInterval,
                 TimeUnit backupRequestsLatencyNotificationIntervalUnit,
                 boolean enableBackupRequestsClientAsync,
                 ScheduledExecutorService backupRequestsExecutorService,
                 EventEmitter emitter,
                 PartitionAccessorRegistry partitionAccessorRegistry,
                 Function<ZooKeeper, ZooKeeper> zooKeeperDecorator,
                 boolean enableSaveUriDataOnDisk,
                 Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories,
                 boolean requestTimeoutHandlerEnabled,
                 SslSessionValidatorFactory sslSessionValidatorFactory,
                 ZKPersistentConnection zkConnection,
                 ScheduledExecutorService startUpExecutorService,
                 ScheduledExecutorService indisStartUpExecutorService,
                 JmxManager jmxManager,
                 String d2JmxManagerPrefix,
                 int zookeeperReadWindowMs,
                 boolean enableRelativeLoadBalancer,
                 DeterministicSubsettingMetadataProvider deterministicSubsettingMetadataProvider,
                 CanaryDistributionProvider canaryDistributionProvider,
                 boolean enableClusterFailout,
                 FailoutConfigProviderFactory failoutConfigProviderFactory,
                 FailoutRedirectStrategy failoutRedirectStrategy,
                 ServiceDiscoveryEventEmitter serviceDiscoveryEventEmitter,
                 DualReadStateManager dualReadStateManager,
                 ScheduledExecutorService xdsExecutorService,
                 Long xdsStreamReadyTimeout,
                 ExecutorService dualReadNewLbExecutor,
                 String xdsChannelLoadBalancingPolicy,
                 Map<String, ?> xdsChannelLoadBalancingPolicyConfig,
                 boolean subscribeToUriGlobCollection,
                 XdsServerMetricsProvider xdsServerMetricsProvider,
                 boolean loadBalanceStreamException,
                 boolean xdsInitialResourceVersionsEnabled,
                 boolean detectLiRawD2Client)
  {
    this.zkHosts = zkHosts;
    this.xdsServer = xdsServer;
    this.hostName = hostName;
    this.zkSessionTimeoutInMs = zkSessionTimeoutInMs;
    this.zkStartupTimeoutInMs = zkStartupTimeoutInMs;
    this.lbWaitTimeout = lbWaitTimeout;
    this.lbWaitUnit = lbWaitUnit;
    this.flagFile = flagFile;
    this.basePath = basePath;
    this.fsBasePath = fsBasePath;
    this.indisFsBasePath = indisFsBasePath;
    this.componentFactory = componentFactory;
    this.clientFactories = clientFactories;
    this.lbWithFacilitiesFactory = lbWithFacilitiesFactory;
    this.sslContext = sslContext;
    this.grpcSslContext = grpcSslContext;
    this.sslParameters = sslParameters;
    this.isSSLEnabled = isSSLEnabled;
    this.shutdownAsynchronously = shutdownAsynchronously;
    this.isSymlinkAware = isSymlinkAware;
    this.clientServicesConfig = clientServicesConfig;
    this.d2ServicePath = d2ServicePath;
    this.useNewEphemeralStoreWatcher = useNewEphemeralStoreWatcher;
    this.healthCheckOperations = healthCheckOperations;
    this._executorService = executorService;
    this.retry = retry;
    this.restRetryEnabled = restRetryEnabled;
    this.streamRetryEnabled = streamRetryEnabled;
    this.retryLimit = retryLimit;
    this.retryUpdateIntervalMs = retryUpdateIntervalMs;
    this.retryAggregatedIntervalNum = retryAggregatedIntervalNum;
    this.warmUp = warmUp;
    this.warmUpTimeoutSeconds = warmUpTimeoutSeconds;
    this.indisWarmUpTimeoutSeconds = indisWarmUpTimeoutSeconds;
    this.warmUpConcurrentRequests = warmUpConcurrentRequests;
    this.indisWarmUpConcurrentRequests = indisWarmUpConcurrentRequests;
    this.downstreamServicesFetcher = downstreamServicesFetcher;
    this.indisDownstreamServicesFetcher = indisDownstreamServicesFetcher;
    this.backupRequestsEnabled = backupRequestsEnabled;
    this.backupRequestsStrategyStatsConsumer = backupRequestsStrategyStatsConsumer;
    this.backupRequestsLatencyNotificationInterval = backupRequestsLatencyNotificationInterval;
    this.backupRequestsLatencyNotificationIntervalUnit = backupRequestsLatencyNotificationIntervalUnit;
    this.enableBackupRequestsClientAsync = enableBackupRequestsClientAsync;
    this._backupRequestsExecutorService = backupRequestsExecutorService;
    this.eventEmitter = emitter;
    this.partitionAccessorRegistry = partitionAccessorRegistry;
    this.zooKeeperDecorator = zooKeeperDecorator;
    this.enableSaveUriDataOnDisk = enableSaveUriDataOnDisk;
    this.loadBalancerStrategyFactories = loadBalancerStrategyFactories;
    this.requestTimeoutHandlerEnabled = requestTimeoutHandlerEnabled;
    this.sslSessionValidatorFactory = sslSessionValidatorFactory;
    this.zkConnectionToUseForLB = zkConnection;
    this.startUpExecutorService = startUpExecutorService;
    this.indisStartUpExecutorService = indisStartUpExecutorService;
    this.jmxManager = jmxManager;
    this.d2JmxManagerPrefix = d2JmxManagerPrefix;
    this.zookeeperReadWindowMs = zookeeperReadWindowMs;
    this.enableRelativeLoadBalancer = enableRelativeLoadBalancer;
    this.deterministicSubsettingMetadataProvider = deterministicSubsettingMetadataProvider;
    this.canaryDistributionProvider = canaryDistributionProvider;
    this.enableClusterFailout = enableClusterFailout;
    this.failoutConfigProviderFactory = failoutConfigProviderFactory;
    this.failoutRedirectStrategy = failoutRedirectStrategy;
    this.serviceDiscoveryEventEmitter = serviceDiscoveryEventEmitter;
    this.dualReadStateManager = dualReadStateManager;
    this.xdsExecutorService = xdsExecutorService;
    this.xdsStreamReadyTimeout = xdsStreamReadyTimeout;
    this.dualReadNewLbExecutor = dualReadNewLbExecutor;
    this.xdsChannelLoadBalancingPolicy = xdsChannelLoadBalancingPolicy;
    this.xdsChannelLoadBalancingPolicyConfig = xdsChannelLoadBalancingPolicyConfig;
    this.subscribeToUriGlobCollection = subscribeToUriGlobCollection;
    this._xdsServerMetricsProvider = xdsServerMetricsProvider;
    this.loadBalanceStreamException = loadBalanceStreamException;
    this.xdsInitialResourceVersionsEnabled = xdsInitialResourceVersionsEnabled;
    this.detectLiRawD2Client = detectLiRawD2Client;
  }
}

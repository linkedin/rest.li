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
import com.linkedin.d2.balancer.clients.RetryClient;
import com.linkedin.d2.balancer.event.EventEmitter;
import com.linkedin.d2.balancer.simple.SslSessionValidatorFactory;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.subsetting.DeterministicSubsettingMetadataProvider;
import com.linkedin.d2.balancer.util.CanaryDistributionProvider;
import com.linkedin.d2.balancer.util.WarmUpLoadBalancer;
import com.linkedin.d2.balancer.util.downstreams.DownstreamServicesFetcher;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessorRegistry;
import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl;
import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl.ComponentFactory;
import com.linkedin.d2.discovery.stores.zk.ZKPersistentConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeper;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperStore;
import com.linkedin.d2.jmx.JmxManager;
import com.linkedin.d2.jmx.NoOpJmxManager;
import com.linkedin.r2.transport.common.TransportClientFactory;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class D2ClientConfig
{
  String zkHosts = null;
  long zkSessionTimeoutInMs = 3600000L;
  long zkStartupTimeoutInMs = 10000L;
  long lbWaitTimeout = 5000L;
  TimeUnit lbWaitUnit = TimeUnit.MILLISECONDS;
  String flagFile = "/no/flag/file/set";
  String basePath = "/d2";
  String fsBasePath = "/tmp/d2";
  ZKFSTogglingLoadBalancerFactoryImpl.ComponentFactory componentFactory = null;
  Map<String, TransportClientFactory> clientFactories = null;
  LoadBalancerWithFacilitiesFactory lbWithFacilitiesFactory = null;
  String d2ServicePath = null;
  SSLContext sslContext = null;
  SSLParameters sslParameters = null;
  boolean isSSLEnabled = false;
  boolean shutdownAsynchronously = false;
  boolean isSymlinkAware = true;
  Map<String, Map<String, Object>> clientServicesConfig = Collections.<String, Map<String, Object>>emptyMap();
  boolean useNewEphemeralStoreWatcher = true;
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
  boolean warmUp = true;
  int warmUpTimeoutSeconds = WarmUpLoadBalancer.DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS;
  int zookeeperReadWindowMs = ZooKeeperStore.DEFAULT_READ_WINDOW_MS;
  int warmUpConcurrentRequests = WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS;
  DownstreamServicesFetcher downstreamServicesFetcher = null;
  boolean backupRequestsEnabled = true;
  BackupRequestsStrategyStatsConsumer backupRequestsStrategyStatsConsumer = null;
  long backupRequestsLatencyNotificationInterval = 1;
  TimeUnit backupRequestsLatencyNotificationIntervalUnit = TimeUnit.MINUTES;
  // TODO: Once the change is fully verified, we should always enable the async feature
  boolean enableBackupRequestsClientAsync = false;
  EventEmitter eventEmitter = null;
  PartitionAccessorRegistry partitionAccessorRegistry = null;
  Function<ZooKeeper, ZooKeeper> zooKeeperDecorator = null;
  Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories = Collections.emptyMap();
  boolean requestTimeoutHandlerEnabled = false;
  SslSessionValidatorFactory sslSessionValidatorFactory = null;
  ZKPersistentConnection zkConnectionToUseForLB = null;
  ScheduledExecutorService startUpExecutorService = null;
  JmxManager jmxManager = new NoOpJmxManager();
  String d2JmxManagerPrefix = "UnknownPrefix";
  boolean enableRelativeLoadBalancer = false;
  DeterministicSubsettingMetadataProvider deterministicSubsettingMetadataProvider = null;
  CanaryDistributionProvider canaryDistributionProvider = null;
  public static final int DEFAULT_RETRY_LIMIT = 3;

  public D2ClientConfig()
  {
  }

  D2ClientConfig(String zkHosts,
                 long zkSessionTimeoutInMs,
                 long zkStartupTimeoutInMs,
                 long lbWaitTimeout,
                 TimeUnit lbWaitUnit,
                 String flagFile,
                 String basePath,
                 String fsBasePath,
                 ComponentFactory componentFactory,
                 Map<String, TransportClientFactory> clientFactories,
                 LoadBalancerWithFacilitiesFactory lbWithFacilitiesFactory,
                 SSLContext sslContext,
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
                 int warmUpConcurrentRequests,
                 DownstreamServicesFetcher downstreamServicesFetcher,
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
                 JmxManager jmxManager,
                 String d2JmxManagerPrefix,
                 int zookeeperReadWindowMs,
                 boolean enableRelativeLoadBalancer,
                 DeterministicSubsettingMetadataProvider deterministicSubsettingMetadataProvider,
                 CanaryDistributionProvider canaryDistributionProvider)
  {
    this.zkHosts = zkHosts;
    this.zkSessionTimeoutInMs = zkSessionTimeoutInMs;
    this.zkStartupTimeoutInMs = zkStartupTimeoutInMs;
    this.lbWaitTimeout = lbWaitTimeout;
    this.lbWaitUnit = lbWaitUnit;
    this.flagFile = flagFile;
    this.basePath = basePath;
    this.fsBasePath = fsBasePath;
    this.componentFactory = componentFactory;
    this.clientFactories = clientFactories;
    this.lbWithFacilitiesFactory = lbWithFacilitiesFactory;
    this.sslContext = sslContext;
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
    this.warmUpConcurrentRequests = warmUpConcurrentRequests;
    this.downstreamServicesFetcher = downstreamServicesFetcher;
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
    this.jmxManager = jmxManager;
    this.d2JmxManagerPrefix = d2JmxManagerPrefix;
    this.zookeeperReadWindowMs = zookeeperReadWindowMs;
    this.enableRelativeLoadBalancer = enableRelativeLoadBalancer;
    this.deterministicSubsettingMetadataProvider = deterministicSubsettingMetadataProvider;
    this.canaryDistributionProvider = canaryDistributionProvider;
  }
}

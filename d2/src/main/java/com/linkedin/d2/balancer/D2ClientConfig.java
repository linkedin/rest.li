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
import com.linkedin.d2.balancer.event.EventEmitter;
import com.linkedin.d2.balancer.util.WarmUpLoadBalancer;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessorRegistry;
import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl;
import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl.ComponentFactory;
import com.linkedin.r2.transport.common.TransportClientFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
  boolean isSymlinkAware = false;
  Map<String, Map<String, Object>> clientServicesConfig = Collections.<String, Map<String, Object>>emptyMap();
  boolean useNewEphemeralStoreWatcher = false;
  HealthCheckOperations healthCheckOperations = null;
  /**
   * By default is a single threaded executor
   */
  ScheduledExecutorService _executorService = null;
  ScheduledExecutorService _backupRequestsExecutorService = null;
  boolean retry = false;
  int retryLimit = DEAULT_RETRY_LIMIT;
  boolean warmUp = false;
  int warmUpTimeoutSeconds = WarmUpLoadBalancer.DEFAULT_SEND_REQUESTS_TIMEOUT_SECONDS;
  int warmUpConcurrentRequests = WarmUpLoadBalancer.DEFAULT_CONCURRENT_REQUESTS;
  boolean backupRequestsEnabled = true;
  BackupRequestsStrategyStatsConsumer backupRequestsStrategyStatsConsumer = null;
  long backupRequestsLatencyNotificationInterval = 1;
  TimeUnit backupRequestsLatencyNotificationIntervalUnit = TimeUnit.MINUTES;
  EventEmitter eventEmitter = null;
  PartitionAccessorRegistry partitionAccessorRegistry = null;

  private static final int DEAULT_RETRY_LIMIT = 3;

  public D2ClientConfig()
  {
  }

  public D2ClientConfig(String zkHosts,
                long zkSessionTimeoutInMs,
                long zkStartupTimeoutInMs,
                long lbWaitTimeout,
                TimeUnit lbWaitUnit,
                String flagFile,
                String basePath,
                String fsBasePath,
                ComponentFactory componentFactory,
                Map<String, TransportClientFactory> clientFactories,
                LoadBalancerWithFacilitiesFactory lbWithFacilitiesFactory)
  {
    this(zkHosts, zkSessionTimeoutInMs, zkStartupTimeoutInMs, lbWaitTimeout,
         lbWaitUnit, flagFile, basePath, fsBasePath, componentFactory,
         clientFactories, lbWithFacilitiesFactory, null, null, false);
  }

  public D2ClientConfig(String zkHosts,
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
                boolean isSSLEnabled)
  {
    this(zkHosts, zkSessionTimeoutInMs, zkStartupTimeoutInMs, lbWaitTimeout, lbWaitUnit, flagFile, basePath, fsBasePath, componentFactory, clientFactories, lbWithFacilitiesFactory, sslContext, sslParameters, isSSLEnabled, false);
  }

  public D2ClientConfig(String zkHosts,
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
                boolean shutdownAsynchronously)
  {
    this(zkHosts,
         zkSessionTimeoutInMs,
         zkStartupTimeoutInMs,
         lbWaitTimeout,
         lbWaitUnit,
         flagFile,
         basePath,
         fsBasePath,
         componentFactory,
         clientFactories,
         lbWithFacilitiesFactory,
         sslContext,
         sslParameters,
         isSSLEnabled,
         shutdownAsynchronously,
         false);
  }

  public D2ClientConfig(String zkHosts,
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
                        boolean isSymlinkAware)
  {
    this(zkHosts,
        zkSessionTimeoutInMs,
        zkStartupTimeoutInMs,
        lbWaitTimeout,
        lbWaitUnit,
        flagFile,
        basePath,
        fsBasePath,
        componentFactory,
        clientFactories,
        lbWithFacilitiesFactory,
        sslContext,
        sslParameters,
        isSSLEnabled,
        shutdownAsynchronously,
        isSymlinkAware,
        Collections.<String, Map<String, Object>>emptyMap());
  }

  public D2ClientConfig(String zkHosts,
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
                        Map<String, Map<String, Object>> clientServicesConfig)
  {
    this(zkHosts,
         zkSessionTimeoutInMs,
         zkStartupTimeoutInMs,
         lbWaitTimeout,
         lbWaitUnit,
         flagFile,
         basePath,
         fsBasePath,
         componentFactory,
         clientFactories,
         lbWithFacilitiesFactory,
         sslContext,
         sslParameters,
         isSSLEnabled,
         shutdownAsynchronously,
         isSymlinkAware,
         clientServicesConfig, null);
  }

  public D2ClientConfig(String zkHosts,
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
      String d2ServicePath)
  {
    this(zkHosts,
        zkSessionTimeoutInMs,
        zkStartupTimeoutInMs,
        lbWaitTimeout,
        lbWaitUnit,
        flagFile,
        basePath,
        fsBasePath,
        componentFactory,
        clientFactories,
        lbWithFacilitiesFactory,
        sslContext,
        sslParameters,
        isSSLEnabled,
        shutdownAsynchronously,
        isSymlinkAware,
        clientServicesConfig,
        d2ServicePath,
        false, null, null, false, 3);
  }

  public D2ClientConfig(String zkHosts,
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
                        int retryLimit)
  {
    this(zkHosts,
      zkSessionTimeoutInMs,
      zkStartupTimeoutInMs,
      lbWaitTimeout,
      lbWaitUnit,
      flagFile,
      basePath,
      fsBasePath,
      componentFactory,
      clientFactories,
      lbWithFacilitiesFactory,
      sslContext,
      sslParameters,
      isSSLEnabled,
      shutdownAsynchronously,
      isSymlinkAware,
      clientServicesConfig,
      d2ServicePath,
      useNewEphemeralStoreWatcher,
      healthCheckOperations,
      executorService,
      retry,
      retryLimit,
      false,
      0,
      0,
      null,
      null
    );
  }

  public D2ClientConfig(String zkHosts,
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
      int retryLimit,
      boolean warmUp,
      int warmUpTimeoutSeconds,
      int warmUpConcurrentRequests,
      EventEmitter emitter,
      PartitionAccessorRegistry partitionAccessorRegistry)
  {
    this(zkHosts,
        zkSessionTimeoutInMs,
        zkStartupTimeoutInMs,
        lbWaitTimeout,
        lbWaitUnit,
        flagFile,
        basePath,
        fsBasePath,
        componentFactory,
        clientFactories,
        lbWithFacilitiesFactory,
        sslContext,
        sslParameters,
        isSSLEnabled,
        shutdownAsynchronously,
        isSymlinkAware,
        clientServicesConfig,
        d2ServicePath,
        useNewEphemeralStoreWatcher,
        healthCheckOperations,
        executorService,
        retry,
        retryLimit,
        warmUp,
        warmUpTimeoutSeconds,
        warmUpConcurrentRequests,
        true,
        null,
        1,
        TimeUnit.MINUTES,
        null,
        emitter,
        partitionAccessorRegistry);
  }

  public D2ClientConfig(String zkHosts,
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
                        int retryLimit,
                        boolean warmUp,
                        int warmUpTimeoutSeconds,
                        int warmUpConcurrentRequests,
                        boolean backupRequestsEnabled,
                        BackupRequestsStrategyStatsConsumer backupRequestsStrategyStatsConsumer,
                        long backupRequestsLatencyNotificationInterval,
                        TimeUnit backupRequestsLatencyNotificationIntervalUnit,
                        ScheduledExecutorService backupRequestsExecutorService,
                        EventEmitter emitter,
                        PartitionAccessorRegistry partitionAccessorRegistry)
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
    this.retryLimit = retryLimit;
    this.warmUp = warmUp;
    this.warmUpTimeoutSeconds = warmUpTimeoutSeconds;
    this.warmUpConcurrentRequests = warmUpConcurrentRequests;
    this.backupRequestsEnabled = backupRequestsEnabled;
    this.backupRequestsStrategyStatsConsumer = backupRequestsStrategyStatsConsumer;
    this.backupRequestsLatencyNotificationInterval = backupRequestsLatencyNotificationInterval;
    this.backupRequestsLatencyNotificationIntervalUnit = backupRequestsLatencyNotificationIntervalUnit;
    this._backupRequestsExecutorService = backupRequestsExecutorService;
    this.eventEmitter = emitter;
    this.partitionAccessorRegistry = partitionAccessorRegistry;
  }
}

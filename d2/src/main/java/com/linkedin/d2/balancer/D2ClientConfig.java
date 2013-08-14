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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl;
import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl.ComponentFactory;
import com.linkedin.r2.transport.common.TransportClientFactory;

public class D2ClientConfig
{
  String zkHosts = "localhost:2121";
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
  Map<String, Map<String, Object>> clientServicesConfig = Collections.<String, Map<String, Object>>emptyMap();

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
                        Map<String, Map<String, Object>> clientServicesConfig)
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
    this.clientServicesConfig = clientServicesConfig;
  }

}

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


import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV2;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.zkfs.ZKFSComponentFactory;
import com.linkedin.d2.balancer.zkfs.ZKFSLoadBalancer;
import com.linkedin.d2.balancer.zkfs.ZKFSTogglingLoadBalancerFactoryImpl;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.http.client.HttpClientFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Build a {@link D2Client} with basic ZooKeeper setup to connect D2 protocol.
 * The client could be further wrapped by other client classes.
 */
public class D2ClientBuilder
{
  /**
   * @return {@link D2Client} that is not started yet. Call start(Callback) to start it.
   */
  public D2Client build()
  {
    final ZKFSLoadBalancer loadBalancer = new ZKFSLoadBalancer(_config.zkHosts,
                                                               (int) _config.zkSessionTimeoutInMs,
                                                               (int) _config.zkStartupTimeoutInMs,
                                                               createLoadBalancerFactory(),
                                                               _config.flagFile,
                                                               _config.basePath);

    return new DynamicClient(loadBalancer, loadBalancer.getFacilities());
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

  public D2ClientBuilder setClientFactories(Map<String, TransportClientFactory> clientFactories)
  {
    _config.clientFactories = clientFactories;
    return this;
  }

  private Map<String, TransportClientFactory> createDefaultClientFactories()
  {
    final Map<String, TransportClientFactory> clientFactories = new HashMap<String, TransportClientFactory>();
    clientFactories.put("http", new HttpClientFactory());
    return clientFactories;
  }

  private Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> createDefaultLoadBalancerStrategyFactories()
  {
    final Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();

    final RandomLoadBalancerStrategyFactory randomStrategyFactory = new RandomLoadBalancerStrategyFactory();
    final DegraderLoadBalancerStrategyFactoryV2 degraderStrategyFactoryV2 = new DegraderLoadBalancerStrategyFactoryV2();
    final DegraderLoadBalancerStrategyFactoryV3 degraderStrategyFactoryV3 = new DegraderLoadBalancerStrategyFactoryV3();

    loadBalancerStrategyFactories.put("random", randomStrategyFactory);
    loadBalancerStrategyFactories.put("degrader", degraderStrategyFactoryV2);
    loadBalancerStrategyFactories.put("degraderV2", degraderStrategyFactoryV2);
    loadBalancerStrategyFactories.put("degraderV3", degraderStrategyFactoryV3);

    return loadBalancerStrategyFactories;
  }

  private ZKFSTogglingLoadBalancerFactoryImpl createLoadBalancerFactory()
  {
    final ZKFSTogglingLoadBalancerFactoryImpl.ComponentFactory loadBalancerComponentFactory;
    if (_config.componentFactory == null)
    {
      loadBalancerComponentFactory = new ZKFSComponentFactory();
    }
    else
    {
      loadBalancerComponentFactory = _config.componentFactory;
    }

    final Map<String, TransportClientFactory> loadBalancerClientFactories;
    if (_config.clientFactories == null)
    {
      loadBalancerClientFactories = createDefaultClientFactories();
    }
    else
    {
      loadBalancerClientFactories = _config.clientFactories;
    }

    final Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        createDefaultLoadBalancerStrategyFactories();

    return new ZKFSTogglingLoadBalancerFactoryImpl(loadBalancerComponentFactory,
                                                   _config.lbWaitTimeout,
                                                   _config.lbWaitUnit,
                                                   _config.basePath,
                                                   _config.fsBasePath,
                                                   loadBalancerClientFactories,
                                                   loadBalancerStrategyFactories);
  }

  private final Config _config = new Config();

  private class Config
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
  }
}
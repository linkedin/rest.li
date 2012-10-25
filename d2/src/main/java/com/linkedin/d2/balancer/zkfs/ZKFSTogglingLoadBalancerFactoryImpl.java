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

import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.ServicePropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.properties.UriPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.UriPropertiesMerger;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.util.TogglingLoadBalancer;
import com.linkedin.d2.discovery.PropertySerializer;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.toggling.TogglingPublisher;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPropertyMerger;
import com.linkedin.common.callback.Callback;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.common.util.None;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
  private final String _fsDir;
  private final Map<String, TransportClientFactory> _clientFactories;
  private final Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> _loadBalancerStrategyFactories;
  private final String _d2ServicePath;

  private static final Logger _log = LoggerFactory.getLogger(ZKFSTogglingLoadBalancerFactoryImpl.class);

  /**
   *
   * @param timeout Timeout for individual LoadBalancer operations
   * @param timeoutUnit Unit for the timeout
   * @param baseZKPath Path to the root ZNode where discovery information is stored
   * @param fsDir Path to the root filesystem directory where backup file stores will live
   * @param clientFactories Factory for transport clients
   * @param loadBalancerStrategyFactories Factory for LoadBalancer strategies
   */
  public ZKFSTogglingLoadBalancerFactoryImpl(ComponentFactory factory,
                                             long timeout, TimeUnit timeoutUnit,
                                             String baseZKPath, String fsDir,
                                             Map<String, TransportClientFactory> clientFactories,
                                             Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories)
  {
    this(factory, timeout, timeoutUnit,
         baseZKPath, fsDir,
         clientFactories, loadBalancerStrategyFactories,
         "");
  }

  /**
   *
   * @param timeout Timeout for individual LoadBalancer operations
   * @param timeoutUnit Unit for the timeout
   * @param baseZKPath Path to the root ZNode where discovery information is stored
   * @param fsDir Path to the root filesystem directory where backup file stores will live
   * @param clientFactories Factory for transport clients
   * @param loadBalancerStrategyFactories Factory for LoadBalancer strategies
   * @param d2ServicePath  alternate service discovery znodes path, relative to baseZKPath.
   *                       d2ServicePath is "services" if it is an empty string or null.
   */
  public ZKFSTogglingLoadBalancerFactoryImpl(ComponentFactory factory,
                                             long timeout, TimeUnit timeoutUnit,
                                             String baseZKPath, String fsDir,
                                             Map<String, TransportClientFactory> clientFactories,
                                             Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories,
                                             String d2ServicePath)
  {
    _factory = factory;
    _lbTimeout = timeout;
    _lbTimeoutUnit = timeoutUnit;
    _baseZKPath = baseZKPath;
    _fsDir = fsDir;
    _clientFactories = clientFactories;
    _loadBalancerStrategyFactories = loadBalancerStrategyFactories;
    if(d2ServicePath == null || d2ServicePath.isEmpty())
    {
      _d2ServicePath = "services";
    }
    else
    {
      _d2ServicePath = d2ServicePath;
    }
  }

  @Override
  public TogglingLoadBalancer createLoadBalancer(ZKConnection zkConnection, ScheduledExecutorService executorService)
  {
    _log.info("Using d2ServicePath: " + _d2ServicePath);
    ZooKeeperPermanentStore<ClusterProperties> zkClusterRegistry = createPermanentStore(
            zkConnection, ZKFSUtil.clusterPath(_baseZKPath), new ClusterPropertiesJsonSerializer());
    ZooKeeperPermanentStore<ServiceProperties> zkServiceRegistry = createPermanentStore(
            zkConnection, ZKFSUtil.servicePath(_baseZKPath, _d2ServicePath), new ServicePropertiesJsonSerializer());
    ZooKeeperEphemeralStore<UriProperties> zkUriRegistry =  createEphemeralStore(
            zkConnection, ZKFSUtil.uriPath(_baseZKPath), new UriPropertiesJsonSerializer(), new UriPropertiesMerger());

    FileStore<ClusterProperties> fsClusterStore = createFileStore("clusters", new ClusterPropertiesJsonSerializer());
    FileStore<ServiceProperties> fsServiceStore = createFileStore(_d2ServicePath, new ServicePropertiesJsonSerializer());
    FileStore<UriProperties> fsUriStore = createFileStore("uris", new UriPropertiesJsonSerializer());

    PropertyEventBus<ClusterProperties> clusterBus = new PropertyEventBusImpl<ClusterProperties>(executorService);
    PropertyEventBus<ServiceProperties> serviceBus = new PropertyEventBusImpl<ServiceProperties>(executorService);
    PropertyEventBus<UriProperties> uriBus = new PropertyEventBusImpl<UriProperties>(executorService);

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
            executorService, uriBus, clusterBus, serviceBus, _clientFactories, _loadBalancerStrategyFactories);
    SimpleLoadBalancer balancer = new SimpleLoadBalancer(state, _lbTimeout, _lbTimeoutUnit);

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

  protected <T> ZooKeeperPermanentStore<T> createPermanentStore(ZKConnection zkConnection, String nodePath, PropertySerializer<T> serializer)
  {
    ZooKeeperPermanentStore<T> store = new ZooKeeperPermanentStore<T>(zkConnection, serializer, nodePath);
    return store;
  }

  protected <T> ZooKeeperEphemeralStore<T> createEphemeralStore(ZKConnection zkConnection, String nodePath, PropertySerializer<T> serializer, ZooKeeperPropertyMerger<T> merger)
  {
    ZooKeeperEphemeralStore<T> store = new ZooKeeperEphemeralStore<T>(zkConnection, serializer, merger, nodePath);
    return store;
  }

  protected <T> FileStore<T> createFileStore(String baseName, PropertySerializer<T> serializer)
  {
    FileStore<T> store = new FileStore<T>(_fsDir + File.separator + baseName, ".ini", serializer);
    return store;
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

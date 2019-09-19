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

package com.linkedin.d2.balancer.zkfs;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.MultiCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.Directory;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashKeyMapper;
import com.linkedin.d2.balancer.util.hashing.HashRingProvider;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.d2.discovery.stores.zk.LastSeenZKStore;
import com.linkedin.d2.discovery.stores.zk.ZKPersistentConnection;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import java.io.IOException;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The class adds the facilities interface to the LoadBalancer and takes care of starting all components.
 * It uses the LastSeenZKStore which allow reading the last values fetched from ZK even if ZK is not reachable
 * when the request is made
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class LastSeenLoadBalancerWithFacilities implements LoadBalancerWithFacilities {
  private static final Logger LOG = LoggerFactory.getLogger(LastSeenLoadBalancerWithFacilities.class);

  private final ZKFSDirectory _directory;
  private ZKPersistentConnection _zkPersistentConnection;
  private LastSeenZKStore<ClusterProperties> _lsClusterStore;
  private LastSeenZKStore<ServiceProperties> _lsServiceStore;
  private LastSeenZKStore<UriProperties> _lsUrisStore;
  private final SimpleLoadBalancer _loadBalancer;
  private final KeyMapper _keyMapper;

  public LastSeenLoadBalancerWithFacilities(SimpleLoadBalancer loadBalancer, String basePath, String d2ServicePath,
      ZKPersistentConnection zkPersistentConnection, LastSeenZKStore<ClusterProperties> lsClusterStore, LastSeenZKStore<ServiceProperties> lsServiceStore,
      LastSeenZKStore<UriProperties> lsUrisStore) {
    _loadBalancer = loadBalancer;
    _directory = new ZKFSDirectory(basePath, d2ServicePath);
    _zkPersistentConnection = zkPersistentConnection;

    _lsClusterStore = lsClusterStore;
    _lsServiceStore = lsServiceStore;
    _lsUrisStore = lsUrisStore;
    _keyMapper = new ConsistentHashKeyMapper(_loadBalancer, _loadBalancer);
    zkPersistentConnection.addListeners(Collections.singleton(new ZKPersistentConnection.EventListenerNotifiers() {
      @Override
      public void sessionEstablished(ZKPersistentConnection.Event event) {
        _directory.setConnection(zkPersistentConnection.getZKConnection());
      }
    }));
  }

  // #################### lifecycle ####################

  @Override
  public void start(final Callback<None> callback) {
    try {
      _zkPersistentConnection.start();
    } catch (IOException e) {
      LOG.error("Error in starting connection while starting load balancer. The connection be already started. "
          + "The LoadBalancer will continue booting up", e);
    }

    MultiCallback multiCallback = new MultiCallback(callback, 4);
    _lsClusterStore.start(multiCallback);
    _lsServiceStore.start(multiCallback);
    _lsUrisStore.start(multiCallback);
    _loadBalancer.start(multiCallback);
  }

  @Override
  public void shutdown(final PropertyEventThread.PropertyEventShutdownCallback callback) {
    LOG.info("Shutting down");
    MultiCallback multiCallback = new MultiCallback(new Callback<None>() {
      @Override
      public void onError(Throwable e) {
        callback.done();
      }

      @Override
      public void onSuccess(None result) {
        callback.done();
      }
    }, 4);

    _loadBalancer.shutdown(() -> multiCallback.onSuccess(None.none()));
    try {
      _zkPersistentConnection.shutdown();
    } catch (InterruptedException e) {
      LOG.info("Error in shutting down connection while shutting down load balancer");
    }

    _lsClusterStore.shutdown(multiCallback);
    _lsServiceStore.shutdown(multiCallback);
    _lsUrisStore.shutdown(multiCallback);
  }

  // #################### delegation ####################

  @Override
  public void getClient(Request request, RequestContext requestContext, Callback<TransportClient> clientCallback)
  {
    _loadBalancer.getClient(request, requestContext, clientCallback);
  }

  @Override
  public void getLoadBalancedServiceProperties(String serviceName, Callback<ServiceProperties> clientCallback)
  {
    _loadBalancer.getLoadBalancedServiceProperties(serviceName, clientCallback);
  }

  /**
   * Get a {@link Directory} associated with this load balancer's ZooKeeper connection.  The
   * directory will not operate until the load balancer is started.  The directory is
   * persistent across ZooKeeper connection expiration, just like the ZKFSLoadBalancer.
   *
   * @return the Directory
   */
  @Override
  public Directory getDirectory() {
    return _directory;
  }

  @Override
  public PartitionInfoProvider getPartitionInfoProvider() {
    return _loadBalancer;
  }

  @Override
  public HashRingProvider getHashRingProvider() {
    return _loadBalancer;
  }

  /**
   * Get a {@link KeyMapper} associated with this load balancer's strategies.  The
   * KeyMapper will not operate until the load balancer is started.  The KeyMapper is
   * persistent across ZooKeeper connection expiration, just like the ZKFSLoadBalancer.
   *
   * @return KeyMapper provided by this load balancer
   */
  @Override
  public KeyMapper getKeyMapper() {
    return _keyMapper;
  }

  @Override
  public TransportClientFactory getClientFactory(String scheme) {
    return _loadBalancer.getClientFactory(scheme);
  }
}

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
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.subsetting.SubsettingState;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * This interface holds the "state" necessary for knowing how to load balance a request.
 *
 * This class listens to PropertyStore to new updates so it can keep track
 * how many clients are available, what properties are related to the cluster,
 * which cluster corresponds to a service, etc
 *
 * Example of state that we keep track of:
 *
 * We have a service called Foo and Bar
 * We have 10 servers, the addresses are myhost1.domain.com, myhost2.domain.com, ..., myhost10.domain.com
 *
 * We can create 2 clusters of servers for example:
 * - cluster 1 consists of 4 servers from myhost1.domain.com to myhost4.domain.com
 * - cluster 2 consists of 6 servers from myhost5.domain.com to myhost10.domain.com
 * We'll assign service Foo to cluster 1 and service Bar to cluster 2.
 *
 * Let's say cluster 1 has timeout of 5000ms and cluster 2 has timeout of 7500 ms.
 *
 * When we start LoadBalancerState, it will first obtain all the information described
 * above from the property store (can be zookeeper, filestore, in memory store).
 *
 * When there's a change, let's say another server was added to cluster 1. Then
 * LoadBalancerState will be notified and it can adjust the state accordingly.
 * Hence the state is a place to inquire things like for a given cluster what are its properties,
 * or what are the server URI associated with a particular cluster, etc
 *
 */
public interface LoadBalancerState
{
  boolean isListeningToCluster(String clusterName);

  boolean isListeningToService(String serviceName);

  void listenToService(String serviceName, LoadBalancerStateListenerCallback callback);

  void listenToCluster(String clusterName, LoadBalancerStateListenerCallback callback);

  void start(Callback<None> callback);

  void shutdown(PropertyEventShutdownCallback shutdown);

  LoadBalancerStateItem<UriProperties> getUriProperties(String clusterName);

  LoadBalancerStateItem<ClusterProperties> getClusterProperties(String clusterName);

  LoadBalancerStateItem<PartitionAccessor> getPartitionAccessor(String clusterName);

  LoadBalancerStateItem<ServiceProperties> getServiceProperties(String serviceName);

  TrackerClient getClient(String serviceName, URI uri);

  TransportClient getClient(String serviceName, String scheme);

  LoadBalancerStrategy getStrategy(String serviceName, String scheme);

  List<SchemeStrategyPair> getStrategiesForService(String serviceName,
                                                    List<String> prioritizedSchemes);

  default SubsettingState.SubsetItem getClientsSubset(String serviceName,
                                                   int minClusterSubsetSize,
                                                   int partitionId,
                                                   Map<URI, Double> possibleUris,
                                                   long version)
  {
    return new SubsettingState.SubsetItem(false, possibleUris, Collections.emptySet());
  }

  /**
   * This registers the LoadBalancerClusterListener with the LoadBalancerState, so that
   * the user can receive updates.
   */
  default void registerClusterListener(LoadBalancerClusterListener clusterListener)
  {
  }

  /**
   * Unregister the LoadBalancerClusterListener.
   */
  default void unregisterClusterListener(LoadBalancerClusterListener clusterListener)
  {
  }

  interface LoadBalancerStateListenerCallback
  {
    int SERVICE = 0;
    int CLUSTER = 1;

    void done(int type, String name);
  }

  class NullStateListenerCallback implements
      LoadBalancerStateListenerCallback
  {
    @Override
    public void done(int type, String name)
    {
    }
  }

  class SchemeStrategyPair
  {
    private final String _scheme;
    private final LoadBalancerStrategy _strategy;

    public SchemeStrategyPair(String scheme, LoadBalancerStrategy strategy)
    {
      _scheme = scheme;
      _strategy = strategy;
    }

    public String getScheme()
    {
      return _scheme;
    }

    public LoadBalancerStrategy getStrategy()
    {
      return _strategy;
    }
  }
}

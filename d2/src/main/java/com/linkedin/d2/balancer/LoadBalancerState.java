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
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;

import java.net.URI;
import java.util.List;


/**
 * This interface holds the "state" necessary for knowing how to load balance a request.
 *
 * This class listens to PropertyStore to new updates so it can keep track
 * how many clients are available, what properties are related to the cluster,
 * which cluster corresponds to a service.
 *
 * Example: profile service is assigned to cluster 231 which consists of
 * machine #1 with URI dev1.linkedin.com:1111,
 * machine #2 with URI ...
 * ...
 * The cluster properties has timeout of 5000ms, etc.
 *
 * Since this class knows all the clients, cluster properties, service name, we can
 * get a bunch of information like for this particular service what is the appropriate
 * load balancing strategy, for a particular cluster what is the clients that we can get, etc
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

  TrackerClient getClient(String clusterName, URI uri);

  TransportClient getClient(String clusterName, String scheme);

  LoadBalancerStrategy getStrategy(String serviceName, String scheme);

  List<SchemeStrategyPair> getStrategiesForService(String serviceName,
                                                    List<String> prioritizedSchemes);

  public static interface LoadBalancerStateListenerCallback
  {
    public static int SERVICE = 0;
    public static int CLUSTER = 1;

    void done(int type, String name);
  }

  public static class NullStateListenerCallback implements
      LoadBalancerStateListenerCallback
  {
    @Override
    public void done(int type, String name)
    {
    }
  }

  public static class SchemeStrategyPair
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

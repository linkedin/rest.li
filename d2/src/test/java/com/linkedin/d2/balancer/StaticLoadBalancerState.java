/*
   Copyright (c) 2018 LinkedIn Corp.

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
import com.linkedin.d2.balancer.properties.FailoutProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PartitionProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class is implemented for testing convenience.
 * It comes with one default cluster and service definition and a default loadbalancing strategy. These should suffice simple functional testing.
 * Customized properties and hosts can be added to respective maps followed by calling {@code refreshDefaultProperties} to update the property objects.
 *
 * More services and clusters can be added to the map manually and the methods are intended to be overridden.
 */
public class StaticLoadBalancerState implements LoadBalancerState
{
  public String TEST_SERVICE = "testService";
  public String TEST_CLUSTER = "testCluster";

  // TEST_SERVICE properties
  public final List<String> TEST_SERVICE_STRATEGIES_LIST = Collections.singletonList("DegraderV3");
  public final List<String> TEST_SERVICE_PRIORITIZED_SCHEMES = Arrays.asList("https", "http");
  public final String TEST_SERVICE_PATH = "/resources";
  public final Map<String,Object> TEST_SERVICE_LB_STRATEGY_PROPERTIES = new HashMap<>();
  public final Map<String,Object> TEST_SERVICE_TRANSPORT_CLIENT_PROPERTIES = new HashMap<>();
  public final List<Map<String,Object>> TEST_SERVICE_BACKUP_REQUEST_PROPERTIES = new ArrayList<>();  // each map in the list represents one backup requests strategy
  public final Map<String,String> TEST_SERVICE_DEGRADER_PROPERTIES = new HashMap<>();
  public final Set<URI> TEST_SERVICE_BANNED_URIS = new HashSet<>();
  public final Map<String,Object> TEST_SERVICE_META_PROPERTIES = new HashMap<>();

  // TEST_CLUSTER properties
  public final Map<String, String>   TEST_CLUSTER_PROPERTIES = new HashMap<>();
  public final Set<URI> TEST_CLUSTER_BANNED_URIS = new HashSet<>();
  public final PartitionProperties TEST_CLUSTER_PARTITION_PROPERTIES = null;
  public final List<String> TEST_CLUSTER_SSL_VALIDATION_STRINGS = new ArrayList<>();

  // TEST_CLUSTER uris
  public final Map<URI, Map<Integer, PartitionData>> TEST_URIS_PARTITIONDESCRIPTIONS = new HashMap<>();
  public final Map<URI, Map<String, Object>> TEST_URI_PROPERTIES = new HashMap<>();

  // default LB strategy for testing
  public LoadBalancerStrategy TEST_STRATEGY = new DegraderLoadBalancerStrategyV3(
      new DegraderLoadBalancerStrategyConfig(5000), TEST_SERVICE,
      null, Collections.emptyList());

  // LoadBalancer state maps
  public Map<String, ServiceProperties> _serviceProperties = new HashMap<>();
  public Map<String, List<LoadBalancerStrategy>> _strategiesBySerivce = new HashMap<>();
  public Map<String, ClusterProperties> _clusterPropertie = new HashMap<>();
  public Map<String, UriProperties> _uriProperties = new HashMap<>();


  public StaticLoadBalancerState()
  {
    _serviceProperties.put(TEST_SERVICE,
        new ServiceProperties(TEST_SERVICE, TEST_CLUSTER, TEST_SERVICE_PATH, TEST_SERVICE_STRATEGIES_LIST,
            TEST_SERVICE_LB_STRATEGY_PROPERTIES, TEST_SERVICE_TRANSPORT_CLIENT_PROPERTIES,
            TEST_SERVICE_DEGRADER_PROPERTIES, TEST_SERVICE_PRIORITIZED_SCHEMES, TEST_SERVICE_BANNED_URIS,
            TEST_SERVICE_META_PROPERTIES, TEST_SERVICE_BACKUP_REQUEST_PROPERTIES));

    _clusterPropertie.put(TEST_CLUSTER, new ClusterProperties(TEST_CLUSTER, TEST_SERVICE_PRIORITIZED_SCHEMES, TEST_CLUSTER_PROPERTIES,
                                                              TEST_CLUSTER_BANNED_URIS, TEST_CLUSTER_PARTITION_PROPERTIES,
                                                              TEST_CLUSTER_SSL_VALIDATION_STRINGS,
                                                              (Map<String, Object>)null, false));
    _uriProperties.put(TEST_CLUSTER, new UriProperties(TEST_CLUSTER, TEST_URIS_PARTITIONDESCRIPTIONS, TEST_URI_PROPERTIES));
  }

  /**
   * Since property objects are immutable, this function has to be called to refresh them when new properties are added.
   */
  public void refreshDefaultProperties()
  {
    _serviceProperties.replace(TEST_SERVICE,
        new ServiceProperties(TEST_SERVICE, TEST_CLUSTER, TEST_SERVICE_PATH, TEST_SERVICE_STRATEGIES_LIST,
            TEST_SERVICE_LB_STRATEGY_PROPERTIES, TEST_SERVICE_TRANSPORT_CLIENT_PROPERTIES,
            TEST_SERVICE_DEGRADER_PROPERTIES, TEST_SERVICE_PRIORITIZED_SCHEMES, TEST_SERVICE_BANNED_URIS,
            TEST_SERVICE_META_PROPERTIES, TEST_SERVICE_BACKUP_REQUEST_PROPERTIES));
    _clusterPropertie.replace(TEST_CLUSTER, new ClusterProperties(TEST_CLUSTER, TEST_SERVICE_PRIORITIZED_SCHEMES, TEST_CLUSTER_PROPERTIES,
                                                                  TEST_CLUSTER_BANNED_URIS, TEST_CLUSTER_PARTITION_PROPERTIES,
                                                                  TEST_CLUSTER_SSL_VALIDATION_STRINGS,
                                                                  (Map<String, Object>)null, false));
    _uriProperties.replace(TEST_CLUSTER, new UriProperties(TEST_CLUSTER, TEST_URIS_PARTITIONDESCRIPTIONS, TEST_URI_PROPERTIES));
  }

  @Override
  public boolean isListeningToCluster(String clusterName)
  {
    return true;
  }

  @Override
  public boolean isListeningToService(String serviceName)
  {
    return true;
  }

  @Override
  public void listenToService(String serviceName, LoadBalancerStateListenerCallback callback)
  {
    callback.done(LoadBalancerStateListenerCallback.SERVICE, serviceName);
  }

  @Override
  public void listenToCluster(String clusterName, LoadBalancerStateListenerCallback callback)
  {
    callback.done(LoadBalancerStateListenerCallback.CLUSTER, clusterName);
  }

  @Override
  public boolean stopListenToCluster(String clusterName)
  {
    return false;
  }

  @Override
  public void start(Callback<None> callback)
  {
    callback.onSuccess(None.none());
  }

  @Override
  public void shutdown(PropertyEventThread.PropertyEventShutdownCallback shutdown)
  {
    shutdown.done();
  }

  @Override
  public LoadBalancerStateItem<UriProperties> getUriProperties(String clusterName)
  {
    return new LoadBalancerStateItem<>(_uriProperties.get(clusterName), 1, 1);
  }

  @Override
  public LoadBalancerStateItem<ClusterProperties> getClusterProperties(String clusterName)
  {
    return new LoadBalancerStateItem<>(_clusterPropertie.get(clusterName), 1, 1);
  }

  @Override
  public LoadBalancerStateItem<FailoutProperties> getFailoutProperties(String clusterName)
  {
    return null;
  }

  @Override
  public LoadBalancerStateItem<PartitionAccessor> getPartitionAccessor(String clusterName)
  {
    return new LoadBalancerStateItem<>(DefaultPartitionAccessor.getInstance(), 1, 1);
  }

  @Override
  public LoadBalancerStateItem<ServiceProperties> getServiceProperties(String serviceName)
  {
    return new LoadBalancerStateItem<>(_serviceProperties.get(serviceName), 1, 1);
  }

  @Override
  public TrackerClient getClient(String serviceName, URI uri)
  {
    return null;
  }

  @Override
  public TransportClient getClient(String serviceName, String scheme)
  {
    return null;
  }

  @Override
  public LoadBalancerStrategy getStrategy(String serviceName, String scheme)
  {
    return TEST_STRATEGY;
  }

  @Override
  public List<SchemeStrategyPair> getStrategiesForService(String serviceName, List<String> prioritizedSchemes)
  {
    return Arrays.asList(new SchemeStrategyPair("http", TEST_STRATEGY), new SchemeStrategyPair("https", TEST_STRATEGY));
  }
}

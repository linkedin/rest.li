package com.linkedin.d2.balancer;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.util.clock.SettableClock;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Helper test class used for testing SimpleLoadBalancerTest's testGetPartitionMapping()
 */
public class PartitionedLoadBalancerTestState implements LoadBalancerState
{

  String _cluster;
  String _service;
  String _path;
  String _strategyName;
  Map<URI,Map<Integer, PartitionData>> _partitionDescriptions;
  List<SchemeStrategyPair> _orderedStrategies;
  PartitionAccessor _partitionAccessor;
  ConcurrentHashMap<URI, TrackerClient> _trackerClients;

  public PartitionedLoadBalancerTestState(String cluster, String service, String path, String strategyName,
                                   Map<URI,Map<Integer, PartitionData>> partitionDescriptions,
                                   List<SchemeStrategyPair> orderedStrategies,
                                   PartitionAccessor partitionAccessor)
  {
    _cluster = cluster;
    _service = service;
    _path = path;
    _strategyName = strategyName;
    _partitionDescriptions = partitionDescriptions;
    _orderedStrategies = orderedStrategies;
    _partitionAccessor = partitionAccessor;
    _trackerClients = new ConcurrentHashMap<URI, TrackerClient>();
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
    //do nothing
  }

  @Override
  public void listenToCluster(String clusterName, LoadBalancerStateListenerCallback callback)
  {
    //do nothing
  }

  @Override
  public void start(Callback<None> callback)
  {
    //do nothing
  }

  @Override
  public void shutdown(PropertyEventThread.PropertyEventShutdownCallback shutdown)
  {
    //do nothing
  }

  @Override
  public LoadBalancerStateItem<UriProperties> getUriProperties(String clusterName)
  {
    //this is used to get partitionId -> host uris
    UriProperties uriProperties = new UriProperties(_cluster, _partitionDescriptions);
    return new LoadBalancerStateItem<UriProperties>(uriProperties, 1, 1);
  }

  @Override
  public LoadBalancerStateItem<ClusterProperties> getClusterProperties(String clusterName)
  {
    List<String> prioritizedSchemes = new ArrayList<String>();
    prioritizedSchemes.add("http");
    ClusterProperties clusterProperties = new ClusterProperties(_cluster, prioritizedSchemes);
    return new LoadBalancerStateItem<ClusterProperties>(clusterProperties, 1, 1);
  }

  @Override
  public LoadBalancerStateItem<PartitionAccessor> getPartitionAccessor(String clusterName)
  {
    //this is used to get partitionId -> key mapping
    return new LoadBalancerStateItem<PartitionAccessor>(_partitionAccessor,1,1);
  }

  @Override
  public LoadBalancerStateItem<ServiceProperties> getServiceProperties(String serviceName)
  {
    ServiceProperties serviceProperties = new ServiceProperties(_service, _cluster, _path, Arrays.asList(_strategyName));
    return new LoadBalancerStateItem<ServiceProperties>(serviceProperties, 1, 1);
  }

  @Override
  public TrackerClient getClient(String serviceName, URI uri)
  {
    if (_partitionDescriptions.get(uri) != null)
    {
      // shorten the update interval to 20ms in order to increase the possibility of deadlock
      _trackerClients.putIfAbsent(uri, new TrackerClient(uri, _partitionDescriptions.get(uri), null, new SettableClock(), null, 20));

      return _trackerClients.get(uri);
    }
    return null;
  }

  @Override
  public TransportClient getClient(String serviceName, String scheme)
  {
    return null;  //not used
  }

  @Override
  public LoadBalancerStrategy getStrategy(String serviceName, String scheme)
  {
    return null;  //not used
  }

  @Override
  public List<SchemeStrategyPair> getStrategiesForService(String serviceName, List<String> prioritizedSchemes)
  {
    return _orderedStrategies;
  }
}

package com.linkedin.d2.balancer;


import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.clients.DegraderTrackerClientImpl;
import com.linkedin.d2.balancer.clients.RetryTrackerClient;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.clients.TrackerClientImpl;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.util.clock.SettableClock;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
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
  double _maxClientRequestRetryRatio;

  public PartitionedLoadBalancerTestState(String cluster, String service, String path, String strategyName,
                                   Map<URI,Map<Integer, PartitionData>> partitionDescriptions,
                                   List<SchemeStrategyPair> orderedStrategies,
                                   PartitionAccessor partitionAccessor)
  {
    this(cluster, service, path, strategyName, partitionDescriptions, orderedStrategies, partitionAccessor,
        HttpClientFactory.UNLIMITED_CLIENT_REQUEST_RETRY_RATIO);
  }

  public PartitionedLoadBalancerTestState(String cluster, String service, String path, String strategyName,
                                   Map<URI,Map<Integer, PartitionData>> partitionDescriptions,
                                   List<SchemeStrategyPair> orderedStrategies,
                                   PartitionAccessor partitionAccessor,
                                   double maxClientRequestRetryRatio)
  {
    _cluster = cluster;
    _service = service;
    _path = path;
    _strategyName = strategyName;
    _partitionDescriptions = partitionDescriptions;
    _orderedStrategies = orderedStrategies;
    _partitionAccessor = partitionAccessor;
    _trackerClients = new ConcurrentHashMap<>();
    _maxClientRequestRetryRatio = maxClientRequestRetryRatio;
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
    // trigger callback
    callback.done(LoadBalancerStateListenerCallback.SERVICE, null);
  }

  @Override
  public void listenToCluster(String clusterName, LoadBalancerStateListenerCallback callback)
  {
    // trigger callback
    callback.done(LoadBalancerStateListenerCallback.SERVICE, null);  }

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
    return new LoadBalancerStateItem<>(uriProperties, 1, 1);
  }

  @Override
  public LoadBalancerStateItem<ClusterProperties> getClusterProperties(String clusterName)
  {
    List<String> prioritizedSchemes = new ArrayList<>();
    prioritizedSchemes.add("http");
    ClusterProperties clusterProperties = new ClusterProperties(_cluster, prioritizedSchemes);
    return new LoadBalancerStateItem<>(clusterProperties, 1, 1);
  }

  @Override
  public LoadBalancerStateItem<PartitionAccessor> getPartitionAccessor(String clusterName)
  {
    //this is used to get partitionId -> key mapping
    return new LoadBalancerStateItem<>(_partitionAccessor,1,1);
  }

  @Override
  public LoadBalancerStateItem<ServiceProperties> getServiceProperties(String serviceName)
  {
    ServiceProperties serviceProperties = new ServiceProperties(serviceName, _cluster, _path,
        Collections.singletonList(_strategyName), Collections.emptyMap(),
        Collections.singletonMap(PropertyKeys.HTTP_MAX_CLIENT_REQUEST_RETRY_RATIO, _maxClientRequestRetryRatio),
        Collections.emptyMap(), Collections.emptyList(), Collections.emptySet());
    return new LoadBalancerStateItem<>(serviceProperties, 1, 1);
  }

  @Override
  public TrackerClient getClient(String serviceName, URI uri)
  {
    if (_partitionDescriptions.get(uri) != null)
    {
      if (serviceName.startsWith("retryService"))
      {
        _trackerClients.putIfAbsent(uri, new RetryTrackerClient(uri, _partitionDescriptions.get(uri), null));
      }
      else
      {
        // shorten the update interval to 20ms in order to increase the possibility of deadlock
        _trackerClients.putIfAbsent(uri, new DegraderTrackerClientImpl(uri, _partitionDescriptions.get(uri), null, new SettableClock(), null, 20, TrackerClientImpl.DEFAULT_ERROR_STATUS_PATTERN));
      }

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

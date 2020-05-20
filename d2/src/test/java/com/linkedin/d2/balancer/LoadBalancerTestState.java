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
import com.linkedin.d2.balancer.clients.TestClient;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategy;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadBalancerTestState implements LoadBalancerState
{
  public boolean getClient            = false;
  public boolean getClusterProperties = false;
  public boolean getServiceProperties = false;
  public boolean getStrategy          = false;
  public boolean getUriProperties     = false;
  public boolean isListeningToCluster = false;
  public boolean isListeningToService = false;
  public boolean listenToService      = false;
  public boolean listenToCluster      = false;
  public boolean shutdown             = false;
  public boolean getPartitionAccessor = false;

  @Override
  public TrackerClient getClient(String clusterName, URI uri)
  {
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(2);
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1));
    return (getClient) ? new TrackerClient(uri, partitionDataMap, new TestClient()) : null;
  }

  @Override
  public TransportClient getClient(String clusterName, String Scheme)
  {
    return (getClient) ? new TestClient() : null;
  }

  @Override
  public LoadBalancerStateItem<ClusterProperties> getClusterProperties(String clusterName)
  {
    return (getClusterProperties)
        ? new LoadBalancerStateItem<ClusterProperties>(new ClusterProperties("cluster-1"),
                                                       0,
                                                       0) : null;
  }

  @Override
  public LoadBalancerStateItem<PartitionAccessor> getPartitionAccessor(String clusterName)
  {
    return getPartitionAccessor
        ? new LoadBalancerStateItem<PartitionAccessor>(DefaultPartitionAccessor.getInstance(), 0, 0) : null;
  }

  @Override
  public LoadBalancerStateItem<ServiceProperties> getServiceProperties(String serviceName)
  {
    List<String> prioritizedSchemes = new ArrayList<String>();

    prioritizedSchemes.add("http");

    return (getServiceProperties)
        ? new LoadBalancerStateItem<ServiceProperties>(new ServiceProperties("service-1",
                                                                             "cluster-1",
                                                                             "/foo",
                                                                             Arrays.asList("rr"),
                                                                             Collections.<String, Object>emptyMap(),
                                                                             null,
                                                                             null,
                                                                             prioritizedSchemes,
                                                                             null),
                                                       0,
                                                       0) : null;
  }

  @Override
  public LoadBalancerStateItem<UriProperties> getUriProperties(String clusterName)
  {
    try
    {
      URI uri1 = URI.create("http://test.qa1.com:1234");
      URI uri2 = URI.create("http://test.qa2.com:2345");
      URI uri3 = URI.create("http://test.qa3.com:6789");

      Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
      partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
      Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>(3);
      uriData.put(uri1, partitionData);
      uriData.put(uri2, partitionData);
      uriData.put(uri3, partitionData);
      return (getUriProperties)
          ? new LoadBalancerStateItem<UriProperties>(new UriProperties("cluster-1", uriData),
                                                     0,
                                                     0) : null;
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public LoadBalancerStrategy getStrategy(String serviceName, String scheme)
  {
    return (getStrategy) ? new RandomLoadBalancerStrategy() : null;
  }

  @Override
  public boolean isListeningToCluster(String clusterName)
  {
    return isListeningToCluster;
  }

  @Override
  public boolean isListeningToService(String serviceName)
  {
    return isListeningToService;
  }

  @Override
  public void listenToCluster(String clusterName,
                              LoadBalancerStateListenerCallback callback)
  {
    if (listenToCluster)
    {
      callback.done(LoadBalancerStateListenerCallback.CLUSTER, clusterName);
    }
  }

  @Override
  public void listenToService(String serviceName,
                              LoadBalancerStateListenerCallback callback)
  {
    if (listenToService)
    {
      callback.done(LoadBalancerStateListenerCallback.SERVICE, serviceName);
    }
  }

  @Override
  public void start(Callback<None> callback)
  {
    callback.onSuccess(None.none());
  }

  @Override
  public void shutdown(PropertyEventShutdownCallback shutdown)
  {
    if (this.shutdown)
    {
      shutdown.done();
    }
  }

  public List<SchemeStrategyPair> getStrategiesForService(String serviceName,
                                                           List<String> prioritizedSchemes)
  {
    List<SchemeStrategyPair> orderedStrategies = new ArrayList<SchemeStrategyPair>(prioritizedSchemes.size());
    for (String scheme : prioritizedSchemes)
    {
      LoadBalancerStrategy strategy = getStrategy(serviceName, scheme);
      if (strategy != null)
      {
        orderedStrategies.add(new SchemeStrategyPair(
                                scheme,
                                getStrategy(serviceName, scheme))
        );
      }
    }
    return orderedStrategies;
  }
}

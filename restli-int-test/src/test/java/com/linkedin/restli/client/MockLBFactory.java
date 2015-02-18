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

package com.linkedin.restli.client;


import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.event.SynchronousExecutorService;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class MockLBFactory
{

  static SimpleLoadBalancer createLoadBalancer()
  {
    // define the load balancing strategies that we support (round robin, etc)
    Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();

    loadBalancerStrategyFactories.put("degrader",
                                      new DegraderLoadBalancerStrategyFactoryV3());

    Map<String, TransportClientFactory> clientFactories =
        new HashMap<String, TransportClientFactory>();

    clientFactories.put("http", new HttpClientFactory());

    SynchronousExecutorService executorService = new SynchronousExecutorService();
    MockStore<ServiceProperties> serviceRegistry = new MockStore<ServiceProperties>();
    MockStore<ClusterProperties> clusterRegistry = new MockStore<ClusterProperties>();
    MockStore<UriProperties> uriRegistry = new MockStore<UriProperties>();

    SimpleLoadBalancerState state =
        new SimpleLoadBalancerState(executorService,
                                    uriRegistry,
                                    clusterRegistry,
                                    serviceRegistry,
                                    clientFactories,
                                    loadBalancerStrategyFactories);

    state.listenToService("greetings", new LoadBalancerState.NullStateListenerCallback());
    state.listenToService("groups", new LoadBalancerState.NullStateListenerCallback());
    state.listenToCluster("testcluster", new LoadBalancerState.NullStateListenerCallback());
    state.listenToCluster("badcluster", new LoadBalancerState.NullStateListenerCallback());
    List<String> schemes = new ArrayList<String>();
    schemes.add("http");
    Map<String, Object> metadataProperties = new HashMap<String, Object>();
    metadataProperties.put(RestConstants.RESTLI_PROTOCOL_VERSION_PROPERTY,
                           AllProtocolVersions.BASELINE_PROTOCOL_VERSION.toString());
    serviceRegistry.put("greetings", new ServiceProperties("greetings", "testcluster", "/greetings",
                                                           Arrays.asList("degrader"),
                                                           Collections.<String, Object>emptyMap(),
                                                           null,
                                                           null,
                                                           schemes,
                                                           null,
                                                           metadataProperties));
    serviceRegistry.put("groups", new ServiceProperties("groups", "badcluster", "/groups",
                                                        Arrays.asList("degrader"),
                                                        Collections.<String, Object>emptyMap(),
                                                        null,
                                                        null,
                                                        schemes,
                                                        null,
                                                        metadataProperties));


    clusterRegistry.put("testcluster", new ClusterProperties("testcluster"));
    clusterRegistry.put("badcluster", new ClusterProperties("badcluster"));


    uriRegistry.put("testcluster", new UriProperties("testcluster", createUriData("http://localhost:1338")));
    uriRegistry.put("badcluster", new UriProperties("badcluster", createUriData("http://localhost:1337")));
    // create the load balancer
    return new SimpleLoadBalancer(state);
  }

  private static Map<URI, Map<Integer, PartitionData>> createUriData(String uriString)
  {
    URI uri = URI.create(uriString);
    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>(1);
    uriData.put(uri, partitionData);
    return uriData;
  }
}

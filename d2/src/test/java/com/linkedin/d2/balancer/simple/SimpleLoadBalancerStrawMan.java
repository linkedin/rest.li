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

package com.linkedin.d2.balancer.simple;

import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.strategies.random.RandomLoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.util.URIRequest;
import com.linkedin.d2.discovery.stores.mock.MockStore;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class SimpleLoadBalancerStrawMan
{
  public static void main(String[] args) throws URISyntaxException,
      ServiceUnavailableException
  {
    // define the load balancing strategies that we support (round robin, etc)
    Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();

    loadBalancerStrategyFactories.put("rr", new RandomLoadBalancerStrategyFactory());
    loadBalancerStrategyFactories.put("degrader",
                                      new DegraderLoadBalancerStrategyFactoryV3());

    // define the clients that we support (http, etc)
    Map<String, TransportClientFactory> clientFactories =
        new HashMap<String, TransportClientFactory>();

    clientFactories.put("http", new HttpClientFactory());

    // listen for service updates (could be a glu discovery client, zk discovery client,
    // config discovery client, etc)
    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
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


    // create the load balancer
    SimpleLoadBalancer loadBalancer = new SimpleLoadBalancer(state);

    final TransportClient tc = loadBalancer.getClient(new URIRequest("d2://browsemaps/52"),
                                                      new RequestContext());
    final Client c = new TransportClientAdapter(tc, true);
    c.restRequest(null);
  }
}

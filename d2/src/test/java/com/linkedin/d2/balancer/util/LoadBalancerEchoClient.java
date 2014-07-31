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

package com.linkedin.d2.balancer.util;


import com.linkedin.d2.balancer.clients.DynamicClient;
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
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.discovery.event.PropertyEventBus;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.file.FileStore;
import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperTogglingStore;
import com.linkedin.d2.jmx.JmxManager;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.util.NamedThreadFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoadBalancerEchoClient
{
  private final String   _hostPort;
  private final String[] _services;
  private final Random   _random;

  private static final String _basePath = "/d2";

  public static void main(String[] args) throws URISyntaxException,
      InterruptedException,
      ExecutionException,
      IOException,
      PropertyStoreException
  {
    String hostPort = args[0];
    String[] services = new String[args.length - 1];
    System.arraycopy(args, 1, services, 0, services.length);
    new LoadBalancerEchoClient(hostPort, services).startClient();
  }

  public LoadBalancerEchoClient(String hostPort, String... services)
  {
    _hostPort = hostPort;
    _services = services;
    _random = new Random();
  }

  public void startClient() throws URISyntaxException,
      InterruptedException,
      ExecutionException,
      IOException,
      PropertyStoreException
  {
    DynamicClient client = new DynamicClient(getLoadBalancer(_hostPort), null);

    for (;;)
    {
      int index = 0;
      if (_services.length > 1)
      {
        index = _random.nextInt(_services.length);
      }

      String service = _services[index];
      URI uri = URI.create("d2://" + service);

      RestRequest req =
          new RestRequestBuilder(uri).setEntity("hi there".getBytes("UTF-8")).build();

      try
      {
        Future<RestResponse> response = client.restRequest(req);
        String responseString = response.get().getEntity().asString("UTF-8");

        System.err.println(uri + " response: " + responseString);
      }
      catch (ExecutionException e)
      {
        System.err.println("future.get() failed for " + uri + ": " + e);
      }

      Thread.sleep(_random.nextInt(1000));
    }
  }

  public static SimpleLoadBalancer getLoadBalancer(String hostPort) throws IOException,
      PropertyStoreException
  {
    // zk stores
    ZooKeeperPermanentStore<ClusterProperties> zkClusterRegistry = null;
    ZooKeeperPermanentStore<ServiceProperties> zkServiceRegistry = null;
    ZooKeeperEphemeralStore<UriProperties> zkUriRegistry = null;

    ZKConnection zkClient = new ZKConnection(hostPort, 10000);

    zkClusterRegistry =
        new ZooKeeperPermanentStore<ClusterProperties>(zkClient,
                                                       new ClusterPropertiesJsonSerializer(),
                                                       _basePath+"/clusters");

    zkServiceRegistry =
        new ZooKeeperPermanentStore<ServiceProperties>(zkClient,
                                                       new ServicePropertiesJsonSerializer(),
                                                       _basePath+"/services");

    zkUriRegistry =
        new ZooKeeperEphemeralStore<UriProperties>(zkClient,
                                                   new UriPropertiesJsonSerializer(),
                                                   new UriPropertiesMerger(),
                                                   _basePath+"/uris");

    // fs stores
    File testDirectory =
        LoadBalancerUtil.createTempDirectory("lb-degrader-witih-file-store-large");

    testDirectory.deleteOnExit();

    new File(testDirectory + File.separator + "cluster").mkdir();
    new File(testDirectory + File.separator + "service").mkdir();
    new File(testDirectory + File.separator + "uri").mkdir();

    FileStore<ClusterProperties> fsClusterStore =
        new FileStore<ClusterProperties>(testDirectory + File.separator + "cluster",
                                         ".ini",
                                         new ClusterPropertiesJsonSerializer());

    FileStore<ServiceProperties> fsServiceStore =
        new FileStore<ServiceProperties>(testDirectory + File.separator + "service",
                                         ".ini",
                                         new ServicePropertiesJsonSerializer());

    FileStore<UriProperties> fsUriStore =
        new FileStore<UriProperties>(testDirectory + File.separator + "uri",
                                     ".ini",
                                     new UriPropertiesJsonSerializer());

    // chains
    PropertyEventThread thread = new PropertyEventThread("echo client event thread");
    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(
                        "echo client event thread"));

    // start up the world
    thread.start();

    PropertyEventBus<ServiceProperties> serviceBus =
        new PropertyEventBusImpl<ServiceProperties>(executorService, zkServiceRegistry);
    serviceBus.register(fsServiceStore);
    new ZooKeeperTogglingStore<ServiceProperties>(zkServiceRegistry,
                                                  fsServiceStore,
                                                  serviceBus,
                                                  true);

    PropertyEventBus<UriProperties> uriBus =
        new PropertyEventBusImpl<UriProperties>(executorService, zkUriRegistry);
    uriBus.register(fsUriStore);
    new ZooKeeperTogglingStore<UriProperties>(zkUriRegistry, fsUriStore, uriBus, true);

    PropertyEventBus<ClusterProperties> clusterBus =
        new PropertyEventBusImpl<ClusterProperties>(executorService, zkClusterRegistry);
    clusterBus.register(fsClusterStore);
    new ZooKeeperTogglingStore<ClusterProperties>(zkClusterRegistry,
                                                  fsClusterStore,
                                                  clusterBus,
                                                  true);

    Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> loadBalancerStrategyFactories =
        new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();

    // strategy and scheme factories
    loadBalancerStrategyFactories.put("degrader",
                                      new DegraderLoadBalancerStrategyFactoryV3());

    Map<String, TransportClientFactory> clientFactories =
        new HashMap<String, TransportClientFactory>();

    clientFactories.put("http", new HttpClientFactory());

    // create the state
    SimpleLoadBalancerState state =
        new SimpleLoadBalancerState(executorService,
                                    uriBus,
                                    clusterBus,
                                    serviceBus,
                                    clientFactories,
                                    loadBalancerStrategyFactories,
                                    null, null, false);

    SimpleLoadBalancer balancer = new SimpleLoadBalancer(state, 5, TimeUnit.SECONDS);

    new JmxManager().registerLoadBalancer("balancer", balancer)
                    .registerLoadBalancerState("state", state);

    return balancer;
  }

}

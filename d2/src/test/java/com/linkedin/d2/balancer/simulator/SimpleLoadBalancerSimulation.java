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

package com.linkedin.d2.balancer.simulator;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.LoadBalancerStateItem;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerTest;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerTest.DoNothingClientFactory;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategyFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyFactoryV3;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.d2.balancer.util.URIRequest;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.mock.MockStore;
import com.linkedin.d2.jmx.JmxManager;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * The SimpleLoadBalancerSimulation will try to simulate add/remove cluster/service calls
 * on the load balancer across multiple threads. A fixed number of operations (the
 * simulation step size) will be performed a fixed number of times (the total number of
 * steps).
 *
 * <br/>
 * <br/>
 *
 * After each step is performed, verification will be performed to ensure that the
 * simulation's view of the system is equivalent to that of the load balancer's.
 */
public class SimpleLoadBalancerSimulation
{
  // simulation state
  private ConcurrentLinkedQueue<String[]>[]                                        _queues;
  private Random                                                                   _random;
  private List<String>                                                             _possibleServices;
  private List<String>                                                             _possibleClusters;
  private List<String>                                                             _possiblePaths;
  private List<String>                                                             _possibleSchemes;
  private List<String>                                                             _possibleStrategies;
  private List<URI>                                                                _possibleUris;

  // load balancer state
  private Map<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>> _loadBalancerStrategyFactories;
  private LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>              _loadBalancerStrategyFactoryToTest;
  private Map<String, TransportClientFactory>                                      _clientFactories;
  private MockStore<ServiceProperties>                                             _serviceRegistry;
  private MockStore<UriProperties>                                                 _uriRegistry;
  private MockStore<ClusterProperties>                                             _clusterRegistry;
  private SimpleLoadBalancer                                                       _loadBalancer;
  private SimpleLoadBalancerState                                                  _state;
  private ScheduledExecutorService                                                 _executorService;
  private PropertyStoreFactory<ClusterProperties>                                  _clusterStoreFactory;
  private PropertyStoreFactory<ServiceProperties>                                  _serviceStoreFactory;
  private PropertyStoreFactory<UriProperties>                                      _uriStoreFactory;

  // verification state
  private Map<String, ServiceProperties>                                           _expectedServiceProperties;
  private Map<String, ClusterProperties>                                           _expectedClusterProperties;
  private Map<String, UriProperties>                                               _expectedUriProperties;
  private long                                                                     _totalMessages;
  private FileOutputStream                                                         _messageLog;

  public static void main(String[] args) throws Exception
  {
    SimpleLoadBalancerTest test = new SimpleLoadBalancerTest();

    test.doOneTimeSetUp();
    System.err.println(2);
    test.testLoadBalancerSimulationRandom();
    System.err.println(3);
    test.testLoadBalancerSimulationRandomLarge();
    System.err.println(4);
    test.testLoadBalancerSimulationDegrader();
    System.err.println(5);
    test.testLoadBalancerSimulationDegraderLarge();
    System.err.println(6);
    test.testLoadBalancerSimulationDegraderWithFileStore();
    System.err.println(7);
    test.testLoadBalancerSimulationDegraderWithFileStoreLarge();
    System.err.println(8);
    test.doOneTimeTearDown();

    new SimpleLoadBalancerSimulation(new DegraderLoadBalancerStrategyFactoryV3()).simulateMultithreaded(50,
                                                                                                                                                     1000000,
                                                                                                                                                     999999999);
  }

  public SimpleLoadBalancerSimulation(LoadBalancerStrategyFactory<? extends LoadBalancerStrategy> loadBalancerStrategyFactoryToTest)
  {
    this(loadBalancerStrategyFactoryToTest,
         new MockStoreFactory<ClusterProperties>(),
         new MockStoreFactory<ServiceProperties>(),
         new MockStoreFactory<UriProperties>());
  }

  public SimpleLoadBalancerSimulation(LoadBalancerStrategyFactory<? extends LoadBalancerStrategy> loadBalancerStrategyFactoryToTest,
                                      PropertyStoreFactory<ClusterProperties> clusterStoreFactory,
                                      PropertyStoreFactory<ServiceProperties> serviceStoreFactory,
                                      PropertyStoreFactory<UriProperties> uriStoreFactory)
  {
    _loadBalancerStrategyFactoryToTest = loadBalancerStrategyFactoryToTest;
    _clusterStoreFactory = clusterStoreFactory;
    _serviceStoreFactory = serviceStoreFactory;
    _uriStoreFactory = uriStoreFactory;

    reset();
  }

  /**
   * Reset the entire state of the simulation.
   *
   */
  public void reset()
  {
    // simulation state
    _random = new Random();
    _possibleServices = Collections.synchronizedList(new ArrayList<String>());
    _possibleClusters = Collections.synchronizedList(new ArrayList<String>());
    _possiblePaths = Collections.synchronizedList(new ArrayList<String>());
    _possibleSchemes = Collections.synchronizedList(new ArrayList<String>());
    _possibleStrategies = Collections.synchronizedList(new ArrayList<String>());
    _possibleUris = Collections.synchronizedList(new ArrayList<URI>());

    // load balancer state

    _executorService = Executors.newSingleThreadScheduledExecutor();;

    // pretend that these are zk stores
    _serviceRegistry = new MockStore<ServiceProperties>();
    _uriRegistry = new MockStore<UriProperties>();
    _clusterRegistry = new MockStore<ClusterProperties>();

    _loadBalancerStrategyFactories =
        new HashMap<String, LoadBalancerStrategyFactory<? extends LoadBalancerStrategy>>();
    _clientFactories = new HashMap<String, TransportClientFactory>();
    _state =
        new SimpleLoadBalancerState(_executorService,
                                    _uriRegistry,
                                    _clusterRegistry,
                                    _serviceRegistry,
                                    _clientFactories,
                                    _loadBalancerStrategyFactories);
    _loadBalancer = new SimpleLoadBalancer(_state, 10, TimeUnit.SECONDS);

    FutureCallback<None> callback = new FutureCallback<None>();
    _loadBalancer.start(callback);
    try
    {
      callback.get();
    }
    catch (Exception e)
    {
      throw new RuntimeException("Balancer start failed", e);
    }

    // verification state
    _expectedServiceProperties = new ConcurrentHashMap<String, ServiceProperties>();
    _expectedClusterProperties = new ConcurrentHashMap<String, ClusterProperties>();
    _expectedUriProperties = new ConcurrentHashMap<String, UriProperties>();
    _totalMessages = 0;

    // state setup
    // TODO parameterize this
    for (int i = 0; i < 10; ++i)
    {
      _possibleServices.add("service-" + i);
      _possibleClusters.add("cluster-" + i);
      _possiblePaths.add("/some/path/" + i);
      _possibleSchemes.add("scheme" + i % 3);
      _possibleStrategies.add("strategy-" + i);
      _clientFactories.put("scheme" + i % 2, new DoNothingClientFactory());
      _loadBalancerStrategyFactories.put("strategy-" + i,
                                         _loadBalancerStrategyFactoryToTest);
    }

    for (int i = 0; i < 1000; ++i)
    {
      _possibleUris.add(URI.create(random(_possibleSchemes) + "://host" + i % 100 + ":"
          + (1000 + _random.nextInt(1000)) + random(_possiblePaths)));
    }

    // add bad stuff
    // add a bad scheme to prioritized schemes
    _possibleSchemes.add("BAD_PRIORITIZED_SCHEME");

    // add a bad scheme to possible uris
    _possibleUris.add(URI.create("BADSCHEME://host1001:" + (1000 + _random.nextInt(1000))
        + random(_possiblePaths)));

    // register jmx goodies
    new JmxManager().registerLoadBalancer("SimpleLoadBalancer", _loadBalancer)
                    .registerLoadBalancerState("SimpleLoadBalancerState", _state);
  }

  /**
   * Run the simulation from a play-back text file. The format of the text file is the
   * same as the way the random messages are generated in getRandomMessage().
   *
   * @param threads
   *          Number of threads to run.
   * @param simulationStepSize
   *          Number of operations to perform during each step.
   * @param playbackStream
   *          The input stream of messages to play back.
   * @throws IOException
   * @throws ServiceUnavailableException
   */
  public void simulateMultithreaded(int threads,
                                    int simulationStepSize,
                                    InputStream playbackStream) throws IOException,
      ServiceUnavailableException
  {
    BufferedReader br = new BufferedReader(new InputStreamReader(playbackStream));
    String line = "";

    initQueues(threads);

    int lineCount = 0;
    while (line != null)
    {
      // load simulation steps into queue
      for (int i = 0; i < simulationStepSize && line != null; ++i)
      {
        line = br.readLine();

        if (line != null)
        {
          lineCount++;
          String[] message = line.split(" ");
          _queues[getThreadId(message, threads)].add(message);
        }
      }

      // simulate
      runSimulation(threads);

      // now validate that state is as we expect
      verifyState();
      reset();
    }

    try
    {
      final CountDownLatch latch = new CountDownLatch(1);
      PropertyEventShutdownCallback callback = new PropertyEventShutdownCallback()
      {
        @Override
        public void done()
        {
          latch.countDown();
        }
      };

      _state.shutdown(callback);

      if (!latch.await(60, TimeUnit.SECONDS))
      {
        fail("unable to shutdown state");
      }


    }
    catch (InterruptedException e)
    {
      fail("unable to shutdown");
    }
  }

  public void simulateMultithreaded(int threads, int simulationStepSize, int totalSteps) throws ServiceUnavailableException
  {
    simulateMultithreaded(threads, simulationStepSize, totalSteps, null);
  }

  /**
   * Run the simulation using randomly generated messages.
   *
   * @param threads
   *          Number of threads to run.
   * @param simulationStepSize
   *          Number of operations to perform during each step.
   * @param totalSteps
   *          The number of steps.
   * @throws ServiceUnavailableException
   */
  public void simulateMultithreaded(int threads,
                                    int simulationStepSize,
                                    int totalSteps,
                                    String messageLogPath) throws ServiceUnavailableException
  {
    initQueues(threads);
    initMessageLog(messageLogPath);

    for (int step = 0; step < totalSteps; ++step)
    {
      try
      {
        // load simulation steps into queue
        for (int i = 0; i < simulationStepSize; ++i)
        {
          String[] message = getRandomMessage();

          if (_messageLog != null)
          {
            _messageLog.write((LoadBalancerUtil.join(Arrays.asList(message), " ") + "\n").getBytes("UTF-8"));
          }

          _queues[getThreadId(message, threads)].add(message);
          ++_totalMessages;
        }
      }
      catch (UnsupportedEncodingException e)
      {
        fail("unable to encode message properly");
      }
      catch (IOException e)
      {
        fail("unable to write to file");
      }

      // simulate
      long start = System.currentTimeMillis();
      runSimulation(threads);
      System.err.println(simulationStepSize + " in "
          + (System.currentTimeMillis() - start) + "ms (" + _totalMessages + " total)");

      // now validate that state is as we expect
      verifyState();
      reset();
    }

    try
    {
      final CountDownLatch latch = new CountDownLatch(1);
      PropertyEventShutdownCallback callback = new PropertyEventShutdownCallback()
      {
        @Override
        public void done()
        {
          latch.countDown();
        }
      };

      _state.shutdown(callback);

      if (!latch.await(60, TimeUnit.SECONDS))
      {
        fail("unable to shutdown state");
      }


    }
    catch (InterruptedException e)
    {
      fail("interrupted during state and thread shutdown");
    }
  }

  /**
   * Start the threads that actually run the simulation for a single step, and wait for
   * the threads to finish.
   *
   * @param threads
   *          Number of threads to start.
   */
  public void runSimulation(int threads)
  {
    ExecutorService pool = Executors.newFixedThreadPool(threads);

    // now run simulation on these steps
    for (int i = 0; i < threads; ++i)
    {
      final int id = i;

      pool.execute(new Thread()
      {
        @Override
        public void run()
        {
          simulateSingleThreaded(id);
        }
      });
    }

    try
    {
      pool.shutdown();

      if (!pool.awaitTermination(300, TimeUnit.SECONDS))
      {
        fail("unable to stop the simulation threads. something is probably hanging.");
      }
    }
    catch (InterruptedException e)
    {
      fail("interrupted during shutdown.");
    }
  }

  /**
   * Handle all messages for this thread's queue. By having separate queues for each
   * thread, we can specifically send messages to only a certain thread. This allows us to
   * perform add/removes for the same cluster in the same thread.
   *
   * <br/>
   * <br/>
   *
   * Although this is not completely multi-threaded, it is required so that we don't have
   * multiple threads updating the simulator's view of reality at the same time.
   *
   * @param id
   *          The id of this thread's queue.
   */
  public void simulateSingleThreaded(int id)
  {
    int msgCount = 0;
    String[] message = null;

    try
    {
      while ((message = _queues[id].poll()) != null)
      {
        msgCount++;
        if (message[0].equals("call"))
        {
          call(message[1]);
        }
        else if (message[0].equals("add_cluster"))
        {
          addCluster(message[1],
                     Arrays.asList(message[2].split(",")),
                     stringToUris(message.length > 3 ? message[3] : ""));
        }
        else if (message[0].equals("remove_cluster"))
        {
          removeCluster(message[1]);
        }
        else if (message[0].equals("add_service"))
        {
          addService(message[1], message[2], message[3], message[4]);
        }
        else if (message[0].equals("remove_service"))
        {
          removeService(message[1]);
        }
        else if (message[0].equals("increment_time"))
        {
          incrementTime(Long.parseLong(message[1]));
        }
      }
    }
    catch (Exception e)
    {
      StringBuilder b = new StringBuilder();
      b.append("[");
      for (String s : message)
      {
        b.append(s);
        b.append(',');
      }
      b.append(']');
      System.err.println("Bad message was " + b.toString());
      e.printStackTrace();

    }
    System.err.println("Thread " + id + " finished, queue is " + _queues[id].size());
  }

  /**
   * Compare the simulator's view of reality with the load balancer's. This method should
   * be called after every step is performed and all threads have finished.
   */
  public void verifyState()
  {
    // verify that we consumed all messages before we do anything
    for (int i = 0; i < _queues.length; ++i)
    {
      if (_queues[i].size() > 0)
      {
        fail("there were messages left in the queue. all messages should have been consumed during this simulation step.");
      }
    }

    // verify that all clients have been shut down
    for (Map.Entry<String,TransportClientFactory> e : _clientFactories.entrySet())
    {
      DoNothingClientFactory factory = (DoNothingClientFactory)e.getValue();
      if (factory.getRunningClientCount() != 0)
      {
        fail("Not all clients were shut down from factory " + e.getKey());
      }
    }

    try
    {
      final CountDownLatch latch = new CountDownLatch(1);
      PropertyEventShutdownCallback callback = new PropertyEventShutdownCallback()
      {
        @Override
        public void done()
        {
          latch.countDown();
        }
      };

      _state.shutdown(callback);

      if (!latch.await(60, TimeUnit.SECONDS))
      {
        fail("unable to shutdown state");
      }


    }
    catch (InterruptedException e)
    {
      fail("unable to shutdown state in verifyState.");
    }

    // New load balancer with no timeout; the code below checks for services that don't
    // exist,
    // and a load balancer with non-zero timeout will just timeout waiting for them to be
    // registered, which will never happen because the PropertyEventThread is shut down.
    _loadBalancer = new SimpleLoadBalancer(_state, 0, TimeUnit.SECONDS);
    // verify services are as we expect
    for (String possibleService : _possibleServices)
    {
      // check for isRegistered because we may have added the service, but
      // never called it, in which case the load balancer wouldn't know
      // about it
      if (!_expectedServiceProperties.containsKey(possibleService)
          || !_state.isListeningToService(possibleService))
      {
        LoadBalancerStateItem<ServiceProperties> serviceItem =
            _state.getServiceProperties(possibleService);
        assertTrue(serviceItem == null || serviceItem.getProperty() == null);
      }
      else
      {
        ServiceProperties serviceProperties =
            _expectedServiceProperties.get(possibleService);
        ClusterProperties clusterProperties =
            _expectedClusterProperties.get(serviceProperties.getClusterName());
        UriProperties uriProperties =
            _expectedUriProperties.get(serviceProperties.getClusterName());

        assertEquals(_state.getServiceProperties(possibleService).getProperty(),
                     serviceProperties);

        // verify round robin'ing of the hosts for this service
        for (int i = 0; i < 100; ++i)
        {
          try
          {
            // this call will queue up messages if we're not listening to the service, but
            // it's ok, because all of the messengers have been stopped.
            final TransportClient client =
                _loadBalancer.getClient(new URIRequest("d2://" + possibleService +
                    random(_possiblePaths)), new RequestContext());

            // if we didn't receive service unavailable, we should
            // get a client back
            assertNotNull(client);
          }
          catch (ServiceUnavailableException e)
          {
            if (uriProperties != null && clusterProperties != null)
            {
              // only way to get here is if the prioritized
              // schemes could find no available uris in the
              // cluster. let's see if we can find a URI that
              // matches a prioritized scheme in the cluster.
              Set<String> schemes = new HashSet<String>();

              for (URI uri : uriProperties.Uris())
              {
                schemes.add(uri.getScheme());
              }

              for (String scheme : clusterProperties.getPrioritizedSchemes())
              {
                // if we got null, then we must not have a
                // matching scheme, or we must be missing a
                // client factory

                // TODO if you're not using round robin
                // strategy, it's possible that the strategy
                // could return null (even though a factory is
                // available), but i am not checking for this at
                // the moment. See SimpleLoadBalancer.java to trace
                // the code.
                if (schemes.contains(scheme) && _clientFactories.containsKey(scheme))
                {
                  break;
                }

                assertFalse(schemes.contains(scheme)
                                && _clientFactories.containsKey(scheme),
                            "why couldn't a client be found for schemes "
                                + clusterProperties.getPrioritizedSchemes()
                                + " with URIs: " + uriProperties.Uris());
              }
            }
          }
        }
      }
    }

    // verify clusters are as we expect
    for (String possibleCluster : _possibleClusters)
    {
      LoadBalancerStateItem<ClusterProperties> clusterItem =
          _state.getClusterProperties(possibleCluster);

      if (!_expectedClusterProperties.containsKey(possibleCluster)
          || !_state.isListeningToCluster(possibleCluster))
      {
        assertTrue(clusterItem == null || clusterItem.getProperty() == null,
                   "cluster item for " + possibleCluster + " is not null: " + clusterItem);
      }
      else
      {
        assertNotNull(clusterItem, "Item for cluster " + possibleCluster
            + " should not be null, listening: "
            + _state.isListeningToCluster(possibleCluster) + ", keys: "
            + _expectedClusterProperties.keySet());
        assertEquals(clusterItem.getProperty(),
                     _expectedClusterProperties.get(possibleCluster));
      }
    }

    // verify uris are as we expect
    for (String possibleCluster : _possibleClusters)
    {
      LoadBalancerStateItem<UriProperties> uriItem =
          _state.getUriProperties(possibleCluster);

      if (!_expectedUriProperties.containsKey(possibleCluster)
          || !_state.isListeningToCluster(possibleCluster))
      {
        assertTrue(uriItem == null || uriItem.getProperty() == null);
      }
      else
      {
        assertNotNull(uriItem);
        assertEquals(uriItem.getProperty(), _expectedUriProperties.get(possibleCluster));
      }
    }
  }

  // load balancer simulation
  public void call(String uri)
  {
    try
    {
      _loadBalancer.getClient(new URIRequest(uri), new RequestContext());
    }
    catch (ServiceUnavailableException e)
    {
    }
  }

  // backoff simulation
  public void incrementTime(long ms)
  {
    // do nothing
  }

  // service simulation
  public void addService(String serviceName,
                         String clusterName,
                         String path,
                         String loadBalancerStrategyName)
  {
    ServiceProperties serviceProperties =
      new ServiceProperties(serviceName, clusterName, path, Arrays.asList(loadBalancerStrategyName));

    _expectedServiceProperties.put(serviceName, serviceProperties);
    _serviceRegistry.put(serviceName, serviceProperties);
  }

  public void removeService(String serviceName)
  {
    _expectedServiceProperties.remove(serviceName);
    _serviceRegistry.remove(serviceName);
  }

  // cluster simulation
  public void addCluster(String clusterName,
                         List<String> prioritizedSchemes,
                         List<URI> uris)
  {
    ClusterProperties clusterProperties =
        new ClusterProperties(clusterName, prioritizedSchemes);

    // weight the uris randomly between 1 and 2
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>();

    for (URI uri : uris)
    {
      Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
      partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d + _random.nextDouble()));
      uriData.put(uri, partitionData);
    }

    UriProperties uriProperties = new UriProperties(clusterName, uriData);

    _expectedClusterProperties.put(clusterName, clusterProperties);
    _expectedUriProperties.put(clusterName, uriProperties);
    _clusterRegistry.put(clusterName, clusterProperties);
    _uriRegistry.put(clusterName, uriProperties);
  }

  public void removeCluster(String clusterName)
  {
    _expectedClusterProperties.remove(clusterName);
    _expectedUriProperties.remove(clusterName);
    _clusterRegistry.remove(clusterName);
    _uriRegistry.remove(clusterName);
  }

  // helpers
  public List<URI> stringToUris(String urisString)
  {
    List<URI> uris = new ArrayList<URI>();

    if (urisString.length() > 0)
    {
      for (String uri : urisString.split("\\,"))
      {
        uris.add(URI.create(uri));
      }
    }

    return uris;
  }

  public String[] getRandomMessage()
  {
    double random = _random.nextDouble();

    if (random < 0.9d)
    {
      return new String[] { "call", randomServiceUrn().toString() };
    }
    else if (random < 0.96d)
    {
      return new String[] { "increment_time", _random.nextInt(100000) + "" };
    }
    else if (random < 0.97d)
    {
      String clusterName = random(_possibleClusters);
      String prioritizedSchemes = "";
      String uris = "";

      for (int i = 0; i < _random.nextInt(_possibleSchemes.size()); ++i)
      {
        prioritizedSchemes += random(_possibleSchemes) + ",";
      }

      for (int i = 0; i < _random.nextInt(_possibleUris.size()); ++i)
      {
        uris += random(_possibleUris) + ",";
      }

      if (prioritizedSchemes.length() > 0)
      {
        prioritizedSchemes =
            prioritizedSchemes.substring(0, prioritizedSchemes.length() - 1);
      }

      if (uris.length() > 0)
      {
        uris = uris.substring(0, uris.length() - 1);
      }

      return new String[] { "add_cluster", clusterName, prioritizedSchemes, uris };
    }
    else if (random < 0.98d)
    {
      return new String[] { "remove_cluster", random(_possibleClusters) };
    }
    else if (random < 0.99d)
    {
      String serviceName = random(_possibleServices);
      String clusterName = random(_possibleClusters);
      String path = random(_possiblePaths);
      String loadBalancerStrategyName = random(_possibleStrategies);

      return new String[] { "add_service", serviceName, clusterName, path,
          loadBalancerStrategyName };
    }
    else
    {
      return new String[] { "remove_service", random(_possibleServices) };
    }
  }

  public URI randomServiceUrn()
  {
    return URI.create("d2://" + random(_possibleServices) + random(_possiblePaths));
  }

  public <T> T random(List<T> possible)
  {
    return possible.get(_random.nextInt(possible.size()));
  }

  public int getThreadId(String[] message, int numThreads)
  {
    // force all add/removes to the same thread so we don't run into synch
    // problems with the simulator (don't have to worry about add/remove
    // ordering across threads)
    if (message[0].equals("add_cluster") || message[0].equals("remove_cluster")
        || message[0].equals("add_service") || message[0].equals("remove_service"))
    {
      return Math.abs(message[1].hashCode()) % numThreads;
    }

    // otherwise just send the message to any queue
    return _random.nextInt(numThreads);
  }

  @SuppressWarnings({"unchecked","rawtypes"})
  public void initQueues(int queues)
  {
    // TODO make this typed properly
    _queues = new ConcurrentLinkedQueue[queues];

    for (int i = 0; i < queues; ++i)
    {
      _queues[i] = new ConcurrentLinkedQueue<String[]>();
    }
  }

  public void initMessageLog(String messageLogPath)
  {
    try
    {
      if (_messageLog != null)
      {
        _messageLog.close();
      }

      if (messageLogPath != null)
      {
        _messageLog = new FileOutputStream(new File(messageLogPath));
      }
    }
    catch (Exception e)
    {
      fail("unable to open log file: " + messageLogPath);
    }
  }

  public static class MockStoreFactory<T> implements PropertyStoreFactory<T>
  {
    @Override
    public PropertyStore<T> getStore()
    {
      return new MockStore<T>();
    }
  }

  public interface PropertyStoreFactory<T>
  {
    PropertyStore<T> getStore();
  }
}

package com.linkedin.d2.loadbalancer;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.D2BaseTest;
import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerStateTest;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.relative.TrackerClientState;
import com.linkedin.d2.balancer.util.LoadBalancerClientCli;
import com.linkedin.d2.balancer.util.LoadBalancerEchoServer;
import com.linkedin.d2.discovery.stores.zk.ZKServer;
import com.linkedin.d2.discovery.stores.zk.ZKTestUtil;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.util.NamedThreadFactory;
import com.linkedin.test.util.retry.ThreeRetries;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


/**
 * An end-to-end integration test on Dynamic Discovery and load balancing behavior.
 *
 * The test starts a local ZooKeeper server instance that has a group of {@link LoadBalancerEchoServer}s announce
 * to it. The D2 cluster and service properties are defined in the d2_config_example.json file under the resource
 * folder, and is also deployed to the local ZooKeeper server using the {@link LoadBalancerClientCli} tool.
 *
 * It then creates a {@link DynamicClient} that connects to the local ZooKeeper instance and sends Rest request to
 * the downstream cluster using a {@link ScheduledThreadPoolExecutor}.
 *
 * With different test setups, we are able to simulate different production scenarios, including even load distribution,
 * D2 weight changes, host mark down/up, etc. And we can verify that the traffic distribution and the internal of D2
 * load balancer state are what we expected.
 */
public class TestDynamicClient extends D2BaseTest {
  private static final String D2_CONFIG_FILE = "d2_config_example.json";
  private static final String ZK_HOST = "127.0.0.1";
  private static final int ECHO_SERVER_PORT_START = 2851;
  private static final int NUMBER_OF_HOSTS = 5;
  private static final int NUMBER_OF_THREADS = 10;
  private static final int TEST_DURATION_IN_MS = 10000;
  private static final double TOLERANCE = 0.15;

  private LoadBalancerClientCli _cli;
  private ArrayList<LoadBalancerEchoServer> _echoServers;
  private int _zkPort;
  private String _zkUriString;

  private SimpleLoadBalancerState _state;
  private DynamicClient _client;

  @BeforeTest
  public void setup() throws Exception
  {
    // Start ZK Server
    ZKServer zkServer = ZKTestUtil.startZKServer();
    _zkPort = zkServer.getPort();
    String zkHosts = ZK_HOST + ":" + _zkPort;
    _zkUriString = "zk://" + zkHosts;

    // Register D2 clusters/services
    URL d2Config = getClass().getClassLoader().getResource(D2_CONFIG_FILE);
    if (d2Config != null) {
      LoadBalancerClientCli.runDiscovery(zkHosts, "/d2", new File(d2Config.toURI()));
    }

    // Set up SimpleLoadBalancerState and D2 Client
    _cli = new LoadBalancerClientCli(zkHosts, "/d2");
  }

  @BeforeMethod
  public void init() throws Exception
  {
    // Bring up all echo servers
    if (_echoServers != null)
    {
      stopAllEchoServers(_echoServers);
    }
    startEchoServers(NUMBER_OF_HOSTS);
    assertAllEchoServersRunning(_echoServers);
    assertAllEchoServersRegistered(_cli.getZKClient(), _zkUriString, _echoServers);

    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("D2 PropertyEventExecutor"));
    _state = LoadBalancerClientCli.createSimpleLoadBalancerState(_cli.getZKClient(), _zkUriString, "/d2", executor);
    SimpleLoadBalancer balancer = new SimpleLoadBalancer(_state, 5, TimeUnit.SECONDS, executor);
    _client = new DynamicClient(balancer, null);

    // Start the load balancer
    FutureCallback<None> callback = new FutureCallback<>();
    balancer.start(callback);
    callback.get(5, TimeUnit.SECONDS);
  }

  /**
   * Given that all the downstream hosts in the cluster are healthy and have a uniform weight,
   * the requests sending from the clients should result in an even distribution. The total call count
   * received by a single server should not deviate by more than 15% of the average.
   */
  @Test(retryAnalyzer = ThreeRetries.class)
  public void testBalancedLoadDistribution()
  {
    SimpleLoadBalancerStateTest.TestListener listener = new SimpleLoadBalancerStateTest.TestListener();
    _state.register(listener);

    URI uri = URI.create("d2://" + "service-1_1");
    Map<Integer, Long> latencyMap = getUniformLatencyMap(5);

    // Use one request to trigger load balancer state update
    RestRequest trigger =
        new RestRequestBuilder(uri).setEntity(latencyMapToRequestEntity(latencyMap)).build();
    try
    {
      _client.restRequest(trigger, new RequestContext()).get();
      assertTrue(listener.strategy instanceof RelativeLoadBalancerStrategy);
      RelativeLoadBalancerStrategy strategy = (RelativeLoadBalancerStrategy) listener.strategy;
      Map<URI, Integer> pointsMap = strategy.getPartitionState(0).getPointsMap();
      assertEquals(pointsMap.size(), NUMBER_OF_HOSTS);
      for (int point : pointsMap.values())
      {
        assertEquals(point, 100);
      }

      Map<TrackerClient, TrackerClientState> stateMap = strategy.getPartitionState(0).getTrackerClientStateMap();
      assertEquals(stateMap.size(), NUMBER_OF_HOSTS);
      for (TrackerClientState state : stateMap.values())
      {
        assertEquals(state.getHealthScore(), 1.0);
        assertFalse(state.isUnhealthy());
      }
    } catch (InterruptedException | ExecutionException e)
    {
      throw new RuntimeException("Failed the test because thread was interrupted");
    }

    Map<String, Integer> distributionMap = sendD2Requests(uri, NUMBER_OF_THREADS, 1, TEST_DURATION_IN_MS, latencyMap);
    double mean = distributionMap.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    double delta = TOLERANCE * mean;
    for (int count : distributionMap.values())
    {
      assertTrue(Math.abs(count - mean) <= delta);
    }
  }

  /**
   * After the hosts are up and running, one can change the D2 weight of a host through JMX.
   * The D2 weight change involves marking down the host, and re-marking up with the updated weight. The change
   * in the /d2/uris ZNode should be propagate to the client immediately (< 1s when ZK is healthy).
   *
   * The host will not be enrolled in the slow start program. Instead, it will be marked as doNotSlowStart and
   * has an initial point of (d2_weight * 100). In this test case, a D2 weight of 0.5 will result in 50 points
   * in the hash ring. And the host will receive half of the traffic of the other hosts, (with a tolerance of 15%).
   *
   * After the update event is received by the ZK event subscriber. One request is required to actually trigger the
   * load balancer state and hash ring changes.
   */
  @Test(retryAnalyzer = ThreeRetries.class)
  public void testD2WeightLessThanOne()
  {
    SimpleLoadBalancerStateTest.TestListener listener = new SimpleLoadBalancerStateTest.TestListener();
    _state.register(listener);

    URI uri = URI.create("d2://" + "service-1_1");
    Map<Integer, Long> latencyMap = getUniformLatencyMap(5);

    // Use one request to trigger load balancer state update
    RestRequest trigger =
        new RestRequestBuilder(uri).setEntity(latencyMapToRequestEntity(latencyMap)).build();
    try
    {
      _client.restRequest(trigger, new RequestContext()).get();
    } catch (InterruptedException | ExecutionException e)
    {
      throw new RuntimeException("Failed the test because thread was interrupted");
    }

    try {
      // Change the D2 weight of server:2851 to 0.5
      invokeD2ChangeWeightJmx(new ObjectName("com.linkedin.d2:type=\"server:2851\""), 0.5);
      // Wait 5ms for the change to propagate
      Thread.sleep(5);
    } catch (Exception e) {
      fail("Failed to invoke d2 weight change jmx", e);
    }

    // Send one trigger request again and verify the hash ring changes
    try
    {
      _client.restRequest(trigger, new RequestContext()).get();
      assertTrue(listener.strategy instanceof RelativeLoadBalancerStrategy);
      RelativeLoadBalancerStrategy strategy = (RelativeLoadBalancerStrategy) listener.strategy;
      Map<URI, Integer> pointsMap = strategy.getPartitionState(0).getPointsMap();
      assertEquals(pointsMap.size(), NUMBER_OF_HOSTS);
      for (Map.Entry<URI, Integer> entry : pointsMap.entrySet())
      {
        int points = entry.getValue();
        // Only the single host that has weight changed should receive 50 points, all others should receive 100 points
        if (entry.getKey().equals(URI.create("http://127.0.0.1:2851/cluster-1")))
        {
          assertEquals(points, 50);
        } else
        {
          assertEquals(points, 100);
        }
      }

      Map<TrackerClient, TrackerClientState> stateMap = strategy.getPartitionState(0).getTrackerClientStateMap();
      assertEquals(stateMap.size(), NUMBER_OF_HOSTS);
      for (TrackerClientState state : stateMap.values())
      {
        assertEquals(state.getHealthScore(), 1.0);
        assertFalse(state.isUnhealthy());
      }
    } catch (InterruptedException | ExecutionException e)
    {
      throw new RuntimeException("Failed the test because thread was interrupted");
    }

    Map<String, Integer> distributionMap = sendD2Requests(uri, NUMBER_OF_THREADS, 1, TEST_DURATION_IN_MS, latencyMap);
    double sum = distributionMap.values().stream().mapToInt(Integer::intValue).sum();
    double totalWeight = 0.5 + (NUMBER_OF_HOSTS - 1) * 1.0;
    for (Map.Entry<String, Integer> entry : distributionMap.entrySet())
    {
      // The single host that has the weight changed should receive (0.5 / totalWeight) of total traffic
      if (entry.getKey().equals("2851"))
      {
        assertTrue(Math.abs(entry.getValue() - sum * 0.5 / totalWeight) <= TOLERANCE * sum * 0.5 / totalWeight);
      }
      else
      {
        assertTrue(Math.abs(entry.getValue() - sum / totalWeight) <= TOLERANCE * sum / totalWeight);
      }
    }
  }

  /**
   * Similar to the test case above, a D2 weight of 2.0 will result in 200 points in the hash ring.
   * And the host will receive twice the traffic of the other hosts (with a tolerance of 15%).
   *
   * And if we further increase the weight to 4.0. The host will receive 4x the traffic of the other hosts
   * (with a tolerance of 15%).
   */
  @Test(retryAnalyzer = ThreeRetries.class)
  public void testD2WeightGreaterThanOne()
  {
    SimpleLoadBalancerStateTest.TestListener listener = new SimpleLoadBalancerStateTest.TestListener();
    _state.register(listener);

    URI uri = URI.create("d2://" + "service-1_1");
    Map<Integer, Long> latencyMap = getUniformLatencyMap(5);

    // Use one request to trigger load balancer state update
    RestRequest trigger =
        new RestRequestBuilder(uri).setEntity(latencyMapToRequestEntity(latencyMap)).build();
    try
    {
      _client.restRequest(trigger, new RequestContext()).get();
    } catch (InterruptedException | ExecutionException e)
    {
      throw new RuntimeException("Failed the test because thread was interrupted");
    }

    try {
      // Change the D2 weight of server:2851 to 2.0
      invokeD2ChangeWeightJmx(new ObjectName("com.linkedin.d2:type=\"server:2851\""), 2);
      // Wait 5ms for the change to propagate
      Thread.sleep(5);
    } catch (Exception e) {
      fail("Failed to invoke d2 weight change jmx", e);
    }

    // Send one trigger request again and verify the hash ring changes
    try
    {
      _client.restRequest(trigger, new RequestContext()).get();
      assertTrue(listener.strategy instanceof RelativeLoadBalancerStrategy);
      RelativeLoadBalancerStrategy strategy = (RelativeLoadBalancerStrategy) listener.strategy;
      Map<URI, Integer> pointsMap = strategy.getPartitionState(0).getPointsMap();
      assertEquals(pointsMap.size(), NUMBER_OF_HOSTS);
      for (Map.Entry<URI, Integer> entry : pointsMap.entrySet())
      {
        int points = entry.getValue();
        // Only the single host that has weight changed should receive 200 points, all others should receive 100 points
        if (entry.getKey().equals(URI.create("http://127.0.0.1:2851/cluster-1")))
        {
          assertEquals(points, 200);
        } else
        {
          assertEquals(points, 100);
        }
      }

      Map<TrackerClient, TrackerClientState> stateMap = strategy.getPartitionState(0).getTrackerClientStateMap();
      assertEquals(stateMap.size(), NUMBER_OF_HOSTS);
      for (TrackerClientState state : stateMap.values())
      {
        assertEquals(state.getHealthScore(), 1.0);
        assertFalse(state.isUnhealthy());
      }
    } catch (InterruptedException | ExecutionException e)
    {
      throw new RuntimeException("Failed the test because thread was interrupted");
    }

    Map<String, Integer> distributionMap = sendD2Requests(uri, NUMBER_OF_THREADS, 1, TEST_DURATION_IN_MS, latencyMap);
    double sum = distributionMap.values().stream().mapToInt(Integer::intValue).sum();
    double totalWeight = 2.0 + (NUMBER_OF_HOSTS - 1) * 1.0;
    System.out.println(distributionMap);
    for (Map.Entry<String, Integer> entry : distributionMap.entrySet())
    {
      // The single host that has the weight changed should receive (2.0 / totalWeight) of total traffic
      if (entry.getKey().equals("2851"))
      {
        assertTrue(Math.abs(entry.getValue() - sum * 2.0 / totalWeight) <= TOLERANCE * sum * 2.0 / totalWeight);
      }
      else
      {
        assertTrue(Math.abs(entry.getValue() - sum / totalWeight) <= TOLERANCE * sum / totalWeight);
      }
    }

    try {
      // Change the D2 weight of server:2851 to 4.0
      invokeD2ChangeWeightJmx(new ObjectName("com.linkedin.d2:type=\"server:2851\""), 4);
      // Wait 5ms for the change to propagate
      Thread.sleep(5);
    } catch (Exception e) {
      fail("Failed to invoke d2 weight change jmx", e);
    }

    distributionMap = sendD2Requests(uri, NUMBER_OF_THREADS, 1, TEST_DURATION_IN_MS, latencyMap);
    sum = distributionMap.values().stream().mapToInt(Integer::intValue).sum();
    totalWeight = 4.0 + (NUMBER_OF_HOSTS - 1) * 1.0;
    System.out.println(distributionMap);
    for (Map.Entry<String, Integer> entry : distributionMap.entrySet())
    {
      // The single host that has the weight changed should receive (4.0 / totalWeight) of total traffic
      if (entry.getKey().equals("2851"))
      {
        assertTrue(Math.abs(entry.getValue() - sum * 4.0 / totalWeight) <= TOLERANCE * sum * 4.0 / totalWeight);
      }
      else
      {
        assertTrue(Math.abs(entry.getValue() - sum / totalWeight) <= TOLERANCE * sum / totalWeight);
      }
    }
  }

  /**
   * When a host is marked down (e.g. due to re-deployment), it removes itself from the /d2/uris ZNode and
   * propagate the new data to the D2 clients immediately (<1s when ZK is healthy). One request is required
   * to actually trigger the load balancer state and hash ring changes.
   *
   * When a host is marked up, it adds itself to the /d2/uris ZNode and propagate the new data to the D2 clients
   * immediately (<1s when ZK is healthy). One request is required to actually trigger the load balancer state and h
   * ash ring changes. The new host is then enrolled in the fast recovery program, starting with a point of 1.
   *
   * If there's no request sent to the new host, it will double its weight. Fast recovery stops until either of the
   * two conditions are satisfied:
   *   1. The host start receiving traffic and is considered unhealthy again.
   *   2. The host start receiving traffic and has a health score > 0.5.
   * The host will then be kicked out of the recovery program and continue to recover/degrade using normal up/downStep.
   */
  @Test(retryAnalyzer = ThreeRetries.class)
  public void testHostMarkDownAndMarkUp()
  {
    SimpleLoadBalancerStateTest.TestListener listener = new SimpleLoadBalancerStateTest.TestListener();
    _state.register(listener);

    URI uri = URI.create("d2://" + "service-1_1");
    Map<Integer, Long> latencyMap = getUniformLatencyMap(5);

    // Use one request to trigger load balancer state update
    RestRequest trigger =
        new RestRequestBuilder(uri).setEntity(latencyMapToRequestEntity(latencyMap)).build();
    try
    {
      _client.restRequest(trigger, new RequestContext()).get();
    } catch (InterruptedException | ExecutionException e)
    {
      throw new RuntimeException("Failed the test because thread was interrupted");
    }

    try {
      // Mark down server:2851
      invokeMarkDownJmx(new ObjectName("com.linkedin.d2:type=\"server:2851\""));
      // Wait 5ms for the change to propagate
      Thread.sleep(5);
    } catch (Exception e) {
      fail("Failed to invoke d2 weight change jmx", e);
    }

    // Send one trigger request again and verify the hash ring changes
    try
    {
      _client.restRequest(trigger, new RequestContext()).get();
      assertTrue(listener.strategy instanceof RelativeLoadBalancerStrategy);
      RelativeLoadBalancerStrategy strategy = (RelativeLoadBalancerStrategy) listener.strategy;
      Map<URI, Integer> pointsMap = strategy.getPartitionState(0).getPointsMap();
      assertEquals(pointsMap.size(), NUMBER_OF_HOSTS - 1);
      for (int point : pointsMap.values())
      {
        assertEquals(point, 100);
      }

      Map<TrackerClient, TrackerClientState> stateMap = strategy.getPartitionState(0).getTrackerClientStateMap();
      assertEquals(stateMap.size(), NUMBER_OF_HOSTS - 1);
      for (TrackerClientState state : stateMap.values())
      {
        assertEquals(state.getHealthScore(), 1.0);
        assertFalse(state.isUnhealthy());
      }
    } catch (InterruptedException | ExecutionException e)
    {
      throw new RuntimeException("Failed the test because thread was interrupted");
    }

    try {
      // Mark up server:2851
      invokeMarkUpJmx(new ObjectName("com.linkedin.d2:type=\"server:2851\""));
      // Wait 5ms for the change to propagate
      Thread.sleep(5);
    } catch (Exception e) {
      fail("Failed to invoke d2 weight change jmx", e);
    }

    // Verify recovery status
    try
    {
      _client.restRequest(trigger, new RequestContext()).get();
      assertTrue(listener.strategy instanceof RelativeLoadBalancerStrategy);
      RelativeLoadBalancerStrategy strategy = (RelativeLoadBalancerStrategy) listener.strategy;
      Map<URI, Integer> pointsMap = strategy.getPartitionState(0).getPointsMap();
      assertEquals(pointsMap.size(), NUMBER_OF_HOSTS);
      for (Map.Entry<URI, Integer> entry : pointsMap.entrySet())
      {
        int points = entry.getValue();
        // The host that was marked up again will enroll in the recovery program, starting from point 1
        if (entry.getKey().equals(URI.create("http://127.0.0.1:2851/cluster-1")))
        {
          assertEquals(points, 1);
        } else
        {
          assertEquals(points, 100);
        }
      }
      Thread.sleep(5100);
      // Even when there's no request sent, the newly added host will recover on fast-recovery program (by doubling its weight)
      // Therefore, after 5000ms = 5 update intervals, the host will get 1 * 2 ^ 5 = 32 points
      assertEquals((int) strategy.getPartitionState(0).getPointsMap().get(URI.create("http://127.0.0.1:2851/cluster-1")), 32);

      sendD2Requests(uri, 10, 1, 5000, latencyMap);
      pointsMap = strategy.getPartitionState(0).getPointsMap();
      for (Map.Entry<URI, Integer> entry : pointsMap.entrySet())
      {
        int points = entry.getValue();
        // The host should gradually recover after receiving traffic. With 5 update intervals, it should receive a point between 50 and 100
        if (entry.getKey().equals(URI.create("http://127.0.0.1:2851/cluster-1")))
        {
          assertTrue(points >= 50 && points < 100);
        } else
        {
          assertEquals(points, 100);
        }
      }
    } catch (InterruptedException | ExecutionException e)
    {
      throw new RuntimeException("Failed the test because thread was interrupted");
    }
  }

  private Map<String, Integer> sendD2Requests(URI uri, int numberOfThreads, long scheduledInterval, long duration,
      Map<Integer, Long> latencyMap)
  {
    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(numberOfThreads);
    ConcurrentMap<String, Integer> distributionMap = new ConcurrentHashMap<>();

    ScheduledFuture<?> future = executorService.scheduleAtFixedRate(() ->
    {
      RestRequest request =
          new RestRequestBuilder(uri).setEntity(latencyMapToRequestEntity(latencyMap)).build();
      Future<RestResponse> response = _client.restRequest(request, new RequestContext());
      try
      {
        String responseString = response.get().getEntity().asString(StandardCharsets.UTF_8);
        // We use the port number to distinguish the server hosts
        String[] split = responseString.split(":");
        String serverID = split[split.length - 1];
        distributionMap.put(serverID, distributionMap.getOrDefault(serverID, 0) + 1);
      } catch (InterruptedException | ExecutionException e)
      {
        throw new RuntimeException("Failed the test because thread was interrupted");
      }
    }, 0, scheduledInterval, TimeUnit.MILLISECONDS);

    try {
      future.get(duration, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      future.cancel(false);
    } catch (ExecutionException | InterruptedException e) {
      throw new RuntimeException("Failed the test because thread was interrupted");
    }

    executorService.shutdownNow();

    return distributionMap;
  }

  private void invokeD2ChangeWeightJmx(ObjectName name, double weight)
      throws ReflectionException, InstanceNotFoundException, MBeanException,
             AttributeNotFoundException, InvalidAttributeValueException
  {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    server.setAttribute(name, new Attribute("Weight", weight));

    String opChangeWeight = "changeWeight";
    server.invoke(name, opChangeWeight, new Object[]{true}, new String[]{"boolean"});
  }

  private void invokeMarkDownJmx(ObjectName name)
      throws ReflectionException, InstanceNotFoundException, MBeanException
  {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    String opMarkDown = "markDown";
    server.invoke(name, opMarkDown, null, null);
  }

  private void invokeMarkUpJmx(ObjectName name)
      throws ReflectionException, InstanceNotFoundException, MBeanException
  {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    String opMarkUp = "markUp";
    server.invoke(name, opMarkUp, null, null);
  }

  private void startEchoServers(int numHosts) throws Exception
  {
    _echoServers = new ArrayList<>();

    for (int i = 0; i < numHosts; i++) {
      _echoServers.add(
          startEchoServer(ZK_HOST, _zkPort, ECHO_SERVER_HOST, ECHO_SERVER_PORT_START + i,
              "cluster-1", null, true, "service-1_1", "service-1_2", "service-1_3"));
    }
  }

  private Map<Integer, Long> getUniformLatencyMap(long latencyMs)
  {
    Map<Integer, Long> latencyMap = new HashMap<>();
    for (int i = 0; i < NUMBER_OF_HOSTS; i++)
    {
      latencyMap.put(ECHO_SERVER_PORT_START + i, latencyMs);
    }
    return latencyMap;
  }

  private byte[] latencyMapToRequestEntity(Map<Integer, Long> latencyMap)
  {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<Integer, Long> key : latencyMap.entrySet())
    {
      sb.append("PORT=").append(key.getKey()).append(",LATENCY=").append(key.getValue()).append(';');
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }
}
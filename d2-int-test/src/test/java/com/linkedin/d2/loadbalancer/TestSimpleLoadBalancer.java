package com.linkedin.d2.loadbalancer;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.D2BaseTest;
import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.util.LoadBalancerClientCli;
import com.linkedin.d2.balancer.util.LoadBalancerEchoServer;
import com.linkedin.d2.discovery.stores.zk.ZKServer;
import com.linkedin.d2.discovery.stores.zk.ZKTestUtil;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.util.NamedThreadFactory;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


@Test(groups = {"d2integration"})
public class TestSimpleLoadBalancer extends D2BaseTest {
  private static final String D2_CONFIG_FILE = "d2_config_example.json";
  private static final String ZK_HOST = "127.0.0.1";
  private static final int ECHO_SERVER_PORT_START = 2351;
  private static final int NUMBER_OF_THREADS = 10;
  private static final int NUMBER_OF_REQUESTS_PER_THREAD = 1000;

  private ArrayList<LoadBalancerEchoServer> _echoServers;
  private int _zkPort;
  private String _zkHosts;
  private String _zkUriString;
  private ZKServer _zkServer;
  private LoadBalancerClientCli _cli;

  private SimpleLoadBalancerState _state;
  private SimpleLoadBalancer _balancer;
  private DynamicClient _client;

  @BeforeTest
  public void setup() throws IOException, Exception {
    // Start ZK Server
    _zkServer = ZKTestUtil.startZKServer();
    _zkPort = _zkServer.getPort();
    _zkHosts = ZK_HOST + ":" + _zkPort;
    _zkUriString = "zk://" + _zkHosts;

    // Register D2 clusters/services
    URL d2Config = getClass().getClassLoader().getResource(D2_CONFIG_FILE);
    if (d2Config != null) {
      LoadBalancerClientCli.runDiscovery(_zkHosts, "/d2", new File(d2Config.toURI()));
    }

    // Set up SimpleLoadBalancerState and D2 Client
    _cli = new LoadBalancerClientCli(_zkHosts, "/d2");
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("D2 PropertyEventExecutor"));
    _state = LoadBalancerClientCli.createSimpleLoadBalancerState(_cli.getZKClient(), _zkUriString, "/d2", executor);
    _balancer = new SimpleLoadBalancer(_state, 5, TimeUnit.SECONDS, executor);
    _client = new DynamicClient(_balancer, null);

    // Start the load balancer
    FutureCallback<None> callback = new FutureCallback<>();
    _balancer.start(callback);
    callback.get(5, TimeUnit.SECONDS);

    // Bring up all echo servers
    startEchoServers(5);
    assertAllEchoServersRunning(_echoServers);
    assertAllEchoServersRegistered(_cli.getZKClient(), _zkUriString, _echoServers);
  }

  @Test
  public void testBalancedLoadDistribution() {
    ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    CountDownLatch startLatch = new CountDownLatch(NUMBER_OF_THREADS);
    CountDownLatch exitLatch = new CountDownLatch(NUMBER_OF_THREADS);
    List<Future<Boolean>> futures = new ArrayList<>();
    ConcurrentMap<String, Integer> distributionMap = new ConcurrentHashMap<>();

    URI uri = URI.create("d2://" + "service-1_1");

    for (int i = 0; i < NUMBER_OF_THREADS; i++)
    {
      Future<Boolean> future = executorService.submit(() -> {
        startLatch.countDown();
        try
        {
          //wait until all threads are ready so they can run together
          startLatch.await();
        }
        catch (InterruptedException e)
        {
          throw new RuntimeException("Failed the test because thread was interrupted");
        }

        for (int j = 0; j < NUMBER_OF_REQUESTS_PER_THREAD; j++)
        {
          RestRequest request =
              new RestRequestBuilder(uri).setEntity(generateMessage(_zkUriString).getBytes(StandardCharsets.UTF_8)).build();
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
        }

        exitLatch.countDown();
        try
        {
          if (!exitLatch.await(5, TimeUnit.SECONDS))
          {
            throw new RuntimeException("Failed the test because we waited longer than 5 seconds");
          }
        }
        catch (InterruptedException e)
        {
          throw new RuntimeException("Failed the test because thread was interrupted");
        }
      }, true);
      futures.add(future);
    }

    for (Future<Boolean> future : futures)
    {
      try
      {
        assertTrue(future.get());
      }
      catch (Exception e)
      {
        fail("something is failing", e);
      }
    }
    executorService.shutdownNow();
    System.out.println(distributionMap);
  }

  private void startEchoServers(int numHosts) throws Exception {
    _echoServers = new ArrayList<>();

    for (int i = 0; i < numHosts; i++) {
      _echoServers.add(
          startEchoServer(ZK_HOST, _zkPort, ECHO_SERVER_HOST, ECHO_SERVER_PORT_START + i, "cluster-1", "service-1_1",
              "service-1_2", "service-1_3"));
    }
  }
}
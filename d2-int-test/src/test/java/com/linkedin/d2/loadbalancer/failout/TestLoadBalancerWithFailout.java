package com.linkedin.d2.loadbalancer.failout;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.D2BaseTest;
import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.clusterfailout.FailoutConfigProviderFactory;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.ClusterPropertiesJsonSerializer;
import com.linkedin.d2.balancer.properties.ClusterStoreProperties;
import com.linkedin.d2.balancer.properties.FailoutProperties;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancer;
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import com.linkedin.d2.balancer.util.LoadBalancerClientCli;
import com.linkedin.d2.balancer.util.LoadBalancerEchoServer;
import com.linkedin.d2.discovery.stores.zk.ZKServer;
import com.linkedin.d2.discovery.stores.zk.ZKTestUtil;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.util.NamedThreadFactory;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class TestLoadBalancerWithFailout extends D2BaseTest
{
  private static final String D2_CONFIG_FILE = "d2_config_example.json";
  private static final String ZK_HOST = "127.0.0.1";
  private static final int ECHO_SERVER_PORT_START = 2851;
  private static final int NUMBER_OF_HOSTS = 5;

  private LoadBalancerClientCli _cli;
  private List<LoadBalancerEchoServer> _echoServers;
  private int _zkPort;
  private String _zkUriString;

  private SimpleLoadBalancerState _state;
  private SimpleLoadBalancer _loadBalancer;
  private FailoutConfigProviderFactory _failoutConfigProviderFactory;

  @BeforeTest
  public void setup()
    throws Exception
  {
    // Start ZK Server
    ZKServer zkServer = ZKTestUtil.startZKServer();
    _zkPort = zkServer.getPort();
    String zkHosts = ZK_HOST + ":" + _zkPort;
    _zkUriString = "zk://" + zkHosts;

    // Register D2 clusters/services
    URL d2Config = getClass().getClassLoader().getResource(D2_CONFIG_FILE);
    if (d2Config != null)
    {
      LoadBalancerClientCli.runDiscovery(zkHosts, "/d2", new File(d2Config.toURI()));
    }

    // Set up SimpleLoadBalancerState and D2 Client
    _cli = new LoadBalancerClientCli(zkHosts, "/d2");
  }

  @BeforeMethod
  public void init()
    throws Exception
  {
    // Bring up all echo servers
    if (_echoServers != null)
    {
      stopAllEchoServers(_echoServers);
    }
    startEchoServers(NUMBER_OF_HOSTS);
    assertAllEchoServersRunning(_echoServers);
    assertAllEchoServersRegistered(_cli.getZKClient(), _zkUriString, _echoServers);

    _failoutConfigProviderFactory = new MockFailoutConfigProviderFactory();

    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("D2 PropertyEventExecutor"));
    _state = LoadBalancerClientCli.createSimpleLoadBalancerState(_cli.getZKClient(), _zkUriString, "/d2", executor);
    _loadBalancer = new SimpleLoadBalancer(_state, 5, TimeUnit.SECONDS, executor, _failoutConfigProviderFactory);

    // Start the load balancer
    FutureCallback<None> callback = new FutureCallback<>();
    _loadBalancer.start(callback);
    callback.get(5, TimeUnit.SECONDS);

    _loadBalancer.listenToCluster("cluster-1", true, new LoadBalancerState.NullStateListenerCallback());

    DynamicClient client = new DynamicClient(_loadBalancer, null);
    URI uri = URI.create("d2://" + "service-1_1");

    // Use one request to trigger load balancer state update
    RestRequest trigger = new RestRequestBuilder(uri).build();
    try
    {
      client.restRequest(trigger, new RequestContext()).get();
    }
    catch (InterruptedException | ExecutionException e)
    {
      throw new RuntimeException("Failed the test because thread was interrupted");
    }
  }

  @Test
  public void testFailout()
    throws ExecutionException, InterruptedException, TimeoutException
  {
    assertNull(_loadBalancer.getFailoutConfig("cluster-1"), "No failout config should exist");

    final ClusterProperties originalProperties = _state.getClusterProperties("cluster-1").getProperty();
    // Inserts dummy failout config
    final ClusterStoreProperties propertiesWithFailout =
      new ClusterStoreProperties(originalProperties, null, null, new FailoutProperties(Collections.emptyList(), Collections.emptyList()));

    writeClusterProperties(propertiesWithFailout);

    waitForFailoutPropertyUpdate(true);
    assertNotNull(_loadBalancer.getFailoutConfig("cluster-1"));

    // Removes the failout config
    final ClusterStoreProperties propertiesWithoutFailout = new ClusterStoreProperties(originalProperties, null, null, null);
    writeClusterProperties(propertiesWithoutFailout);

    waitForFailoutPropertyUpdate(false);
    assertNull(_loadBalancer.getFailoutConfig("cluster-1"));
  }

  private void writeClusterProperties(ClusterStoreProperties propertiesWithFailout)
    throws InterruptedException, ExecutionException, TimeoutException
  {
    FutureCallback<None> callback = new FutureCallback<>();
    _cli.getZKClient().setDataUnsafe("/d2/clusters/cluster-1", new ClusterPropertiesJsonSerializer().toBytes(propertiesWithFailout), callback);
    callback.get(5, TimeUnit.SECONDS);
  }

  private void waitForFailoutPropertyUpdate(boolean shouldHaveFailoutProperties)
    throws InterruptedException
  {
    // Wait up to 3 seconds for the subscriber to pick up the change.
    for (int i = 0; i < 30; i++)
    {
      final boolean hasFailoutProperties = _loadBalancer.getFailoutConfig("cluster-1") != null;
      if (hasFailoutProperties != shouldHaveFailoutProperties)
      {
        Thread.sleep(100);
      }
      else
      {
        return;
      }
    }
  }

  private void startEchoServers(int numHosts)
    throws Exception
  {
    _echoServers = new ArrayList<>();

    for (int i = 0; i < numHosts; i++)
    {
      _echoServers.add(
        startEchoServer(ZK_HOST, _zkPort, ECHO_SERVER_HOST, ECHO_SERVER_PORT_START + i, "cluster-1", null, true, "service-1_1", "service-1_2",
                        "service-1_3"));
    }
  }
}

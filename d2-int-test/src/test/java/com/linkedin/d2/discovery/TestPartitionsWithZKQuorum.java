package com.linkedin.d2.discovery;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.util.LoadBalancerClientCli;
import com.linkedin.d2.balancer.util.LoadBalancerEchoServer;
import com.linkedin.d2.D2BaseTest;
import com.linkedin.d2.quorum.ZKQuorum;
import com.linkedin.d2.discovery.stores.PropertyStoreException;

@Test (groups = {"d2integration"})
public class TestPartitionsWithZKQuorum extends D2BaseTest
{
  private List<LoadBalancerEchoServer>  _echoServers;
  private String[]                      _zkHosts;
  private ZKQuorum                      _quorum;
  private LoadBalancerClientCli         _cli;
  DynamicClient                         _client;
  private String                        _zkUriString;

  public void setup() throws IOException, Exception
  {
    // Start _quorum
    _quorum = new ZKQuorum(QUORUM_SIZE);
    _quorum.startAll();

    _zkUriString = "zk://"+_quorum.getHosts();
    _zkHosts = _quorum.getHosts().split(",");
    _quorum.assertAllPeersUp();
  }

  @AfterMethod
  public void teardownMethod() throws Exception
  {
    teardown();
  }

  @AfterTest
  public void teardownTest() throws Exception
  {
    teardown();
  }

  @Test
  public void testRegisterUnregisterDefaultPartitionEchoServers() throws IOException,
                                                URISyntaxException,
                                                PropertyStoreException,
                                                ExecutionException,
                                                TimeoutException,
                                                InterruptedException,
                                                Exception
  {
    _echoServers = new ArrayList<LoadBalancerEchoServer>();
    setup();
    _cli = new LoadBalancerClientCli(_quorum.getHosts(), "/d2");
    assertEquals(_cli.runDiscovery(_quorum.getHosts(), "/d2", D2_CONFIG_DEFAULT_PARTITION_DATA), 0);
    _client = _cli.createZKFSTogglingLBClient(_quorum.getHosts(), "/d2", null);
    // Echo servers startup
    startDefaultPartitionEchoServers();
    assertAllEchoServersRunning(_echoServers);
    assertAllEchoServersRegistered(_cli.getZKClient(), _zkUriString, _echoServers);
    assertQuorumProcessAllRequests(D2_CONFIG_DEFAULT_PARTITION_DATA);

    // Markdown echo servers
    stopAllEchoServers(_echoServers);
    assertAllEchoServersUnregistered(_cli.getZKClient(), _zkUriString, _echoServers);
  }

  @Test
  public void testRegisterUnregisterCustomPartitionEchoServers() throws IOException,
                                                URISyntaxException,
                                                PropertyStoreException,
                                                ExecutionException,
                                                TimeoutException,
                                                InterruptedException,
                                                Exception
  {
    _echoServers = new ArrayList<LoadBalancerEchoServer>();
    setup();
    _cli = new LoadBalancerClientCli(_quorum.getHosts(), "/d2");
    assertEquals(_cli.runDiscovery(_quorum.getHosts(), "/d2", D2_CONFIG_CUSTOM_PARTITION_DATA), 0);
    _client = _cli.createZKFSTogglingLBClient(_quorum.getHosts(), "/d2", null);
    // Echo servers startup
    Map<Integer, Double> partitionWeight = new HashMap<Integer, Double>();
    partitionWeight.put(new Integer(1), new Double(1.0d));
    startCustomPartitionEchoServers(partitionWeight);
    assertAllEchoServersRegistered(_cli.getZKClient(), _zkUriString, _echoServers);
    assertQuorumProcessAllRequests(D2_CONFIG_CUSTOM_PARTITION_DATA);

    // Markdown echo servers
    stopAllEchoServers(_echoServers);
    assertAllEchoServersUnregistered(_cli.getZKClient(), _zkUriString, _echoServers);
  }

  @Test
  public void testRegisterUnregisterAllEchoServers() throws IOException,
                                                URISyntaxException,
                                                PropertyStoreException,
                                                ExecutionException,
                                                TimeoutException,
                                                InterruptedException,
                                                Exception
  {
    _echoServers = new ArrayList<LoadBalancerEchoServer>();
    setup();
    _cli = new LoadBalancerClientCli(_quorum.getHosts(), "/d2");
    assertEquals(_cli.runDiscovery(_quorum.getHosts(), "/d2", D2_CONFIG_DATA), 0);
    _client = _cli.createZKFSTogglingLBClient(_quorum.getHosts(), "/d2", null);
    // Echo servers startup
    Map<Integer, Double> partitionWeight = new HashMap<Integer, Double>();
    partitionWeight.put(new Integer(1), new Double(1.0d));
    startAllEchoServers(partitionWeight);
    assertAllEchoServersRegistered(_cli.getZKClient(), _zkUriString, _echoServers);
    assertQuorumProcessAllRequests(D2_CONFIG_DATA);

    // Markdown echo servers
    stopAllEchoServers(_echoServers);
    assertAllEchoServersUnregistered(_cli.getZKClient(), _zkUriString, _echoServers);
  }

  private void assertQuorumProcessAllRequests(String clustersData) throws Exception
  {
    assertQuorumProcessAllRequests(1, clustersData, _zkUriString, _cli, _client, null);
  }

  private void startDefaultPartitionEchoServers() throws Exception
  {
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT1_1, "cluster-1", "service-1_1", "service-1_2", "service-1_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT1_2, "cluster-1", "service-1_1", "service-1_2", "service-1_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT2_1, "cluster-2", "service-2_1", "service-2_2", "service-2_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT2_2, "cluster-2", "service-2_1", "service-2_2", "service-2_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT3, "cluster-3", "service-3_1", "service-3_2", "service-3_3" ));
  }

  private void startCustomPartitionEchoServers(Map<Integer, Double> partitionWeight) throws Exception
  {
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT4, "cluster-4", partitionWeight, "service-4_11", "service-4_12", "service-4_13" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT5, "cluster-4", partitionWeight, "service-4_11", "service-4_12", "service-4_13" ));
  }

  private void startAllEchoServers(Map<Integer, Double> partitionWeight) throws Exception
  {
    startDefaultPartitionEchoServers();
    startCustomPartitionEchoServers(partitionWeight);
  }

  private void teardown() throws Exception
  {
    try
    {
    _quorum.shutdownAll(true);
    }
    catch (Exception e)
    {
    }
    try
    {
      _client.shutdown();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    try
    {
      _cli.shutdown();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    try
    {
      stopAllEchoServers(_echoServers);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}

package com.linkedin.d2.discovery.quorum;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.util.LoadBalancerClientCli;
import com.linkedin.d2.balancer.util.LoadBalancerEchoServer;
import com.linkedin.d2.D2BaseTest;
import com.linkedin.d2.discovery.stores.PropertyStoreException;

@Test (groups = {"integration"})
public class TestPartitionsWithZKQuorum extends D2BaseTest
{
  private static final Logger _log = LoggerFactory.getLogger(TestZKQuorum.class);
  private List<LoadBalancerEchoServer> _echoServers;
  private String[] _zkHosts;
  private ZKQuorum _quorum;
  private LoadBalancerClientCli _cli;
  DynamicClient _client;
  private String _zkUriString;

  public void setup() throws IOException, Exception
  {
    // Start _quorum
    _quorum = new ZKQuorum(QUORUM_SIZE);
    _quorum.startAll();

    _zkUriString = "zk://"+_quorum.getHosts();
    _zkHosts = _quorum.getHosts().split(",");
    assertAllPeersAreRunning(_quorum);
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

    Map<String, List<String>> clustersData = generateClusterData(new String[] {"1","2","3","4","5"}, 0);
    runDiscovery(_quorum.getHosts(), clustersData);

    _cli = new LoadBalancerClientCli(_quorum.getHosts(), "/d2");
    _client = _cli.createTogglingLBClient(_cli.getZKClient(), "zk://"+_quorum.getHosts(), "/d2", null);
    // Echo servers startup
    startDefaultPartitionEchoServers();
    assertAllEchoServersRunning(_echoServers);
    assertAllEchoServersRegistered(_cli.getZKClient(), _zkUriString, _echoServers);
    assertQuorumProcessAllRequests(clustersData);

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

    Map<String, List<String>> clustersData = generateClusterData(new String[] {"6"}, 10);
    runDiscovery(_quorum.getHosts(), clustersData, generatePartitionProperties("service-6_(\\d+)", 0, 10, 10, "RANGE"));

    _cli = new LoadBalancerClientCli(_quorum.getHosts(), "/d2");
    //_client = _cli.createClient(_cli.getZKClient(), "zk://"+_quorum.getHosts(), "/d2", "service-1_1");
    _client = _cli.createTogglingLBClient(_cli.getZKClient(), "zk://"+_quorum.getHosts(), "/d2", null);
    // Echo servers startup
    Map<Integer, Double> partitionWeight = new HashMap<Integer, Double>();
    partitionWeight.put(new Integer(1), new Double(1.0d));
    startCustomPartitionEchoServers(partitionWeight);
    assertAllEchoServersRegistered(_cli.getZKClient(), _zkUriString, _echoServers);
    assertQuorumProcessAllRequests(clustersData);

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
    
    Map<String, List<String>> clusterData = generateClusterData(new String[] {"1","2","3","4","5"}, 0);
    Map<String, List<String>> customClusterData = generateClusterData(new String[] {"6"}, 10);
    runDiscovery(_quorum.getHosts(), clusterData, customClusterData, generatePartitionProperties("service-6_(\\d+)", 0, 10, 10, "RANGE"));
    
    _cli = new LoadBalancerClientCli(_quorum.getHosts(), "/d2");
    //_client = _cli.createClient(_cli.getZKClient(), "zk://"+_quorum.getHosts(), "/d2", "service-1_1");
    _client = _cli.createTogglingLBClient(_cli.getZKClient(), "zk://"+_quorum.getHosts(), "/d2", null);
    // Echo servers startup
    Map<Integer, Double> partitionWeight = new HashMap<Integer, Double>();
    partitionWeight.put(new Integer(1), new Double(1.0d));
    startAllEchoServers(partitionWeight);
    assertAllEchoServersRegistered(_cli.getZKClient(), _zkUriString, _echoServers);
    clusterData.putAll(customClusterData);
    assertQuorumProcessAllRequests(clusterData);

    // Markdown echo servers
    stopAllEchoServers(_echoServers);
    assertAllEchoServersUnregistered(_cli.getZKClient(), _zkUriString, _echoServers);
  }

  private void assertQuorumProcessAllRequests(Map<String, List<String>> clustersData) throws Exception
  {
    assertQuorumProcessAllRequests(1, clustersData, _zkUriString, _cli, _client, null);
  }

  private void startDefaultPartitionEchoServers() throws Exception
  {
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT1_1, "cluster-1", "service-1_1", "service-1_2", "service-1_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT1_2, "cluster-1", "service-1_1", "service-1_2", "service-1_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT2_1, "cluster-2", "service-2_1", "service-2_2", "service-2_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT2_2, "cluster-2", "service-2_1", "service-2_2", "service-2_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT3_1, "cluster-3", "service-3_1", "service-3_2", "service-3_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT3_2, "cluster-3", "service-3_1", "service-3_2", "service-3_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT4, "cluster-4", "service-4_1", "service-4_2", "service-4_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT5, "cluster-5", "service-5_1", "service-5_2", "service-5_3" ));
  }
  
  private void startCustomPartitionEchoServers(Map<Integer, Double> partitionWeight) throws Exception
  {
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT6, "cluster-6", partitionWeight, "service-6_11", "service-6_12", "service-6_13" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT7, "cluster-6", partitionWeight, "service-6_11", "service-6_12", "service-6_13" ));
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
      _cli.shutdown();
    }
    catch (Exception e)
    {
      e.printStackTrace();
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
      stopAllEchoServers(_echoServers);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}

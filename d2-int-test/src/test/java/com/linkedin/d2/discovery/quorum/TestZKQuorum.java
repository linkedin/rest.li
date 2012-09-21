package com.linkedin.d2.discovery.quorum;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

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

@Test (groups = {"d2integration"})
public class TestZKQuorum extends D2BaseTest
{
  private static final Logger _log = LoggerFactory.getLogger(TestZKQuorum.class);
  private List<LoadBalancerEchoServer> _echoServers;
  private Map<String, List<String>> _defaultClusterData;
  private Map<String, List<String>> _customClusterData;
  private Map<String, List<String>> _clusterData;
  private String[] _zkHosts;
  private ZKQuorum _quorum;
  private LoadBalancerClientCli _cli;
  private DynamicClient _client;
  private String _zkUriString;

  {
    _defaultClusterData = generateClusterData(new String[] {"1","2","3","4","5"}, 0);
    _customClusterData = generateClusterData(new String[] {"6"}, 10);
    _clusterData = _defaultClusterData;
    _clusterData.putAll(_customClusterData);
  }
  
  public void setup() throws IOException, Exception
  {
    // Start _quorum
    _quorum = new ZKQuorum(QUORUM_SIZE);
    _quorum.startAll();

    _zkUriString = "zk://"+_quorum.getHosts();
    _zkHosts = _quorum.getHosts().split(",");
    assertAllPeersAreRunning(_quorum);
    // Register clusters/services with zookeeper _quorum
    runDiscovery(_quorum.getHosts(), _defaultClusterData, _customClusterData, generatePartitionProperties("service-6_(\\d+)", 0, 10, 10, "RANGE"));
    // Echo servers startup
    startAllEchoServers();
    // Get LoadBalancer Client
    _cli = new LoadBalancerClientCli(_quorum.getHosts(), "/d2");
    assertAllEchoServersRunning(_echoServers);
    _client = _cli.createTogglingLBClient(_cli.getZKClient(), "zk://"+_quorum.getHosts(), "/d2", null);
    assertAllEchoServersRegistered(_cli.getZKClient(), _zkUriString, _echoServers);
    assertQuorumProcessAllRequests(_clusterData);
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

  //D2 With _quorum Follower Peer shutdown, restart

  @Test
  public void testD2WithQuorumFollowerPeerServerDown() throws IOException,
                                                URISyntaxException,
                                                PropertyStoreException,
                                                ExecutionException,
                                                TimeoutException,
                                                InterruptedException,
                                                Exception
  {
    setup();
    // Shutdown zk quorum follower server (non-leader member of zk _quorum)
    ZKPeer follower = _quorum.getQuorumFollower();
    follower.shutdownPeerZkServer();
    assertFalse(follower.getZKServer().isRunning(), "Quorum Peer #"+follower.getId()+"/port "+follower.getClientPort()+" server has not shut down.");
    assertQuorumProcessAllRequests(_clusterData);
    // Restart follower zk server
    follower.startupPeerZkServer();
    // Assert requests are processed while follower is restarting
    long start = System.currentTimeMillis();
    while (follower.getZKServer() == null && System.currentTimeMillis() < start + TIMEOUT)
    {
      assertQuorumProcessAllRequests(_clusterData);
    }
    assertAllPeersAreRunning(_quorum);
    // Assert requests are processed after follower restarted
    assertQuorumProcessAllRequests(_clusterData);
  }

  @Test
  public void testD2WithQuorumFollowerPeerDown() throws IOException,
                                                URISyntaxException,
                                                PropertyStoreException,
                                                ExecutionException,
                                                TimeoutException,
                                                InterruptedException,
                                                Exception
  {
    setup();
    assertQuorumProcessAllRequests(_clusterData);
    // Shutdown zk _quorum follower peer (non-leader member of a zk _quorum)
    ZKPeer follower = _quorum.getQuorumFollower();
    follower.shutdownPeer();
    // Assert requests are processed while follower goes down
    long start = System.currentTimeMillis();
    while (follower.getZKServer() != null && System.currentTimeMillis() < start + TIMEOUT)
    {
      assertQuorumProcessAllRequests(_clusterData);
    }
    assertFalse(follower.isRunning(), "_quorum Peer #"+follower.getId()+"/port "+follower.getClientPort()+" server has not shut down.");
    // Restart peer
    _quorum.restart(follower.getId());
    // Assert requests processed while follower is restarting
    start = System.currentTimeMillis();
    while ((_quorum.getPeerActiveServer(follower.getId()) == null || ! _quorum.getPeerActiveServer(follower.getId()).isRunning()) && System.currentTimeMillis() < start + TIMEOUT)
    {
      assertQuorumProcessAllRequests(_clusterData);
    }
    // Assert requests are processed after peer restart
    assertQuorumProcessAllRequests(_clusterData);
  }

  @Test
  public void testD2WithQuorumFollowerServerKilled() throws IOException,
                                                URISyntaxException,
                                                PropertyStoreException,
                                                ExecutionException,
                                                TimeoutException,
                                                InterruptedException,
                                                Exception
  {
    setup();
    // Kill quorum peer follower zk server (non-leader member of a zk _quorum)
    ZKPeer follower = _quorum.getQuorumFollower();
    follower.killPeerZkServer();
    long start = System.currentTimeMillis();
    while(follower.getZKServer().isRunning() && System.currentTimeMillis() < start + TIMEOUT)
    {
      assertQuorumProcessAllRequests(_clusterData);
    }  
    assertFalse(follower.getZKServer().isRunning(), "quorum Peer #"+follower.getId()+"/port "+follower.getClientPort()+" server has not shut down.");

    // Restart peer zk server
    follower.startupPeerZkServer();
    // Assert requests are processed while follower is restarting
    start = System.currentTimeMillis();
    while (follower.getZKServer() == null && System.currentTimeMillis() < start + TIMEOUT)
    {
      assertQuorumProcessAllRequests(_clusterData);
    }
    assertAllPeersAreRunning(_quorum);
    assertQuorumProcessAllRequests(_clusterData);
 }

  // D2 With _quorum Leader Peer shutdown, restart

  @Test
  public void testD2WithQuorumLeaderPeerServerDown() throws IOException,
                                                URISyntaxException,
                                                PropertyStoreException,
                                                ExecutionException,
                                                TimeoutException,
                                                InterruptedException,
                                                Exception
  {
    setup();
    // Shutdown quorum leader zk server
    ZKPeer peer = _quorum.getQuorumLeader();
    peer.shutdownPeerZkServer();
    assertFalse(peer.getZKServer().isRunning(), "_quorum Peer #"+peer.getId()+"/port "+peer.getClientPort()+" server has not shut down.");
    assertQuorumProcessAllRequests(_clusterData);
    // Restart leader zk server
    peer.startupPeerZkServer();
    // Assert requests are processed while peer is restarting
    long start = System.currentTimeMillis();
    while (peer.getZKServer() == null && System.currentTimeMillis() < start + TIMEOUT)
    {
      assertQuorumProcessAllRequests(_clusterData);
    }
    assertAllPeersAreRunning(_quorum);
    // After restart
    assertQuorumProcessAllRequests(_clusterData);
  }

  @Test
  public void testD2WithQuorumLeaderPeerDown() throws IOException,
                                                URISyntaxException,
                                                PropertyStoreException,
                                                ExecutionException,
                                                TimeoutException,
                                                InterruptedException,
                                                Exception
  {
    setup();
     // Shutdown zk _quorum leader peer
    ZKPeer peer = _quorum.getQuorumLeader();
    int peerId = peer.getId();
    peer.shutdownPeer();
    // Verify requests are processed while leader goes down
    long start = System.currentTimeMillis();
    while (_quorum.getQuorumLeader() != null && _quorum.getQuorumLeader().equals(peer) && System.currentTimeMillis() < (start + TIMEOUT))
    {
      assertQuorumProcessAllRequests(_clusterData);
    }
    
    if (_quorum.getQuorumLeader() == null)
    {
      Thread.sleep(800); // wait while zookeeper is establishing a new leader
    }
    assertTrue(! _quorum.getQuorumLeader().equals(peer), "No new _quorum leader was established. Old leader id=" + peerId + " " + peer.getPeerName() + ". New leader id=" + _quorum.getQuorumLeader().getId() + " " + _quorum.getQuorumLeader().getPeerName());
    // Restart leader peer
    _quorum.restart(peerId);
    start = System.currentTimeMillis();
    peer = _quorum.get(peerId);
    // Verify requests are processed while peer is restarting
    while (System.currentTimeMillis() < (start + TIMEOUT) && ( peer.getZKServer() == null || ! _quorum.areAllPeersUp() || ! peer.getZKServer().isRunning()))
    {
      assertQuorumProcessAllRequests(_clusterData);
    }
    // After restart
    assertQuorumProcessAllRequests(_clusterData);
  }

  @Test
  public void testD2WithQuorumLeaderServerKilled() throws IOException,
                                                URISyntaxException,
                                                PropertyStoreException,
                                                ExecutionException,
                                                TimeoutException,
                                                InterruptedException,
                                                Exception
  {
    setup();
    // Kill _quorum leader peer zk server
    ZKPeer peer = _quorum.getQuorumLeader();
    peer.killPeerZkServer();
    assertQuorumProcessAllRequests(_clusterData);
    assertFalse(peer.getZKServer().isRunning(),"_quorum Peer #"+peer.getId()+"/port "+peer.getClientPort()+" server has not shut down.");

    // Restart peer zk server
    peer.startupPeerZkServer();
    // Assert requests are processed while peer is restarting
    long start = System.currentTimeMillis();
    while (peer.getZKServer() == null && System.currentTimeMillis() < start + TIMEOUT)
    {
      assertQuorumProcessAllRequests(_clusterData);
    }
    assertAllPeersAreRunning(_quorum);
    // After restart
    assertQuorumProcessAllRequests(_clusterData);
  }

  @Test
  public void testD2WithQuorumPeerAdded() throws IOException, Exception
  {
    setup();
    // Add new peer to ZK _quorum
    int newPeerId = _quorum.addPeer();
    _quorum.start(newPeerId);
    assertQuorumProcessAllRequests(_clusterData);

    // Remove peer
    _quorum.get(newPeerId).shutdown(true);
    assertQuorumProcessAllRequests(_clusterData);
  }

  private void assertQuorumProcessAllRequests(Map<String, List<String>> clustersData) throws Exception
  {
    assertQuorumProcessAllRequests(1, clustersData, _zkUriString, _cli, _client, null);
  }
  
  private void startAllEchoServers() throws Exception
  {
    _echoServers = new ArrayList<LoadBalancerEchoServer>();
    Map<Integer, Double> partitionWeight = new HashMap<Integer, Double>();
    partitionWeight.put(new Integer(1), new Double(1.0d));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT1_1, "cluster-1", "service-1_1", "service-1_2", "service-1_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT1_2, "cluster-1", "service-1_1", "service-1_2", "service-1_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT2_1, "cluster-2", "service-2_1", "service-2_2", "service-2_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT2_2, "cluster-2", "service-2_1", "service-2_2", "service-2_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT3_1, "cluster-3", "service-3_1", "service-3_2", "service-3_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT3_2, "cluster-3", "service-3_1", "service-3_2", "service-3_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT4, "cluster-4", "service-4_1", "service-4_2", "service-4_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT5, "cluster-5", "service-5_1", "service-5_2", "service-5_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT6, "cluster-6", partitionWeight, "service-6_11", "service-6_12", "service-6_13" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT7, "cluster-6", partitionWeight, "service-6_11", "service-6_12", "service-6_13" ));
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

    stopAllEchoServers(_echoServers);
  }
}

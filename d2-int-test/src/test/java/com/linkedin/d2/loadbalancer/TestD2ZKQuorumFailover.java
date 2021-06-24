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

package com.linkedin.d2.loadbalancer;


import com.linkedin.d2.D2BaseTest;
import com.linkedin.d2.balancer.clients.DynamicClient;
import com.linkedin.d2.balancer.util.LoadBalancerClientCli;
import com.linkedin.d2.balancer.util.LoadBalancerEchoServer;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.d2.quorum.ZKPeer;
import com.linkedin.d2.quorum.ZKQuorum;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@Test (groups = {"d2integration"})
public class TestD2ZKQuorumFailover extends D2BaseTest
{
  private static final Logger _log = LoggerFactory.getLogger(TestD2ZKQuorumFailover.class);

  private static final int              QUORUM_SIZE = 9;
  private List<LoadBalancerEchoServer>  _echoServers;
  private String[]                      _zkHosts;
  private ZKQuorum                      _quorum;
  private LoadBalancerClientCli         _cli;
  private DynamicClient                 _client;
  private String                        _zkUriString;

  private void setup() throws IOException, Exception
  {
    // Start _quorum
    _quorum = new ZKQuorum(QUORUM_SIZE);
    _quorum.startAll();
    _quorum.assertAllPeersUp();
    _zkUriString = "zk://"+_quorum.getHosts();
    _zkHosts = _quorum.getHosts().split(",");

    // Register clusters/services with zookeeper _quorum
    LoadBalancerClientCli.runDiscovery(_quorum.getHosts(), "/d2", D2_CONFIG_DATA);

    // Echo servers startup
    startAllEchoServers();
    assertAllEchoServersRunning(_echoServers);
    // Get LoadBalancer Client
    _cli = new LoadBalancerClientCli(_quorum.getHosts(), "/d2");
    _client = _cli.createZKFSTogglingLBClient(_quorum.getHosts(), "/d2", null);
    assertAllEchoServersRegistered(_cli.getZKClient(), _zkUriString, _echoServers);
    assertQuorumProcessAllRequests(D2_CONFIG_DATA);
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

  public void testD2WithQuorumFollowerPeerServerDown() throws Exception
  {
    setup();
    // Shutdown zk quorum follower server (non-leader member of zk _quorum)
    ZKPeer follower = _quorum.getQuorumFollower();
    follower.shutdownPeerZkServer();
    assertFalse(follower.getZKServer().isRunning(), "Quorum Peer #"+follower.getId()+"/port "+follower.getClientPort()+" server has not shut down.");
    assertQuorumProcessAllRequests(D2_CONFIG_DATA);
    // Restart follower zk server
    follower.startupPeerZkServer();
    // Assert requests are processed while follower is restarting
    long start = System.currentTimeMillis();
    while (follower.getZKServer() == null && System.currentTimeMillis() < start + TIMEOUT)
    {
      assertQuorumProcessAllRequests(D2_CONFIG_DATA);
    }
    _quorum.assertAllPeersUp();
    // Assert requests are processed after follower restarted
    assertQuorumProcessAllRequests(D2_CONFIG_DATA);
  }

  public void testD2WithQuorumFollowerPeerDown() throws Exception
  {
    setup();
    // Shutdown zk _quorum follower peer (non-leader member of a zk _quorum)
    ZKPeer follower = _quorum.getQuorumFollower();
    follower.shutdownPeer();
    // Assert requests are processed while follower goes down
    long start = System.currentTimeMillis();
    while (follower.getZKServer() != null && System.currentTimeMillis() < start + TIMEOUT)
    {
      assertQuorumProcessAllRequests(D2_CONFIG_DATA);
    }
    assertFalse(follower.isRunning(), "_quorum Peer #"+follower.getId()+"/port "+follower.getClientPort()+" server has not shut down.");
    // Restart peer
    _quorum.restart(follower.getId());
    // Assert requests processed while follower is restarting
    start = System.currentTimeMillis();
    while ((_quorum.getPeerZKServer(follower.getId()) == null || ! _quorum.getPeerZKServer(follower.getId()).isRunning()) && System.currentTimeMillis() < start + TIMEOUT)
    {
      assertQuorumProcessAllRequests(D2_CONFIG_DATA);
    }
    // Assert requests are processed after peer restart
    assertQuorumProcessAllRequests(D2_CONFIG_DATA);
  }

  public void testD2WithQuorumFollowerServerKilled() throws Exception
  {
    setup();
    // Kill quorum peer follower zk server
    ZKPeer follower = _quorum.getQuorumFollower();
    // Simulate zk server crash
    follower.killPeerZkServer();
    assertQuorumProcessAllRequests(D2_CONFIG_DATA);
    assertFalse(follower.getZKServer().isRunning(), "quorum Peer #"+follower.getId()+"/port "+follower.getClientPort()+" server has not shut down.");
    assertQuorumProcessAllRequests(D2_CONFIG_DATA);
  }

  // D2 With _quorum Leader Peer shutdown, restart

  public void testD2WithQuorumLeaderPeerServerDown() throws Exception
  {
    setup();
    // Shutdown quorum leader zk server
    ZKPeer peer = _quorum.getQuorumLeader();
    if (peer != null)
    {
      peer.shutdownPeerZkServer();
      assertFalse(peer.getZKServer().isRunning(), "_quorum Peer #"+peer.getId()+"/port "+peer.getClientPort()+" server has not shut down.");
      assertQuorumProcessAllRequests(D2_CONFIG_DATA);
      // Restart leader zk server
      peer.startupPeerZkServer();
      // Assert requests are processed while peer is restarting
      long start = System.currentTimeMillis();
      while (peer.getZKServer() == null && System.currentTimeMillis() < start + TIMEOUT)
      {
        assertQuorumProcessAllRequests(D2_CONFIG_DATA);
      }
      _quorum.assertAllPeersUp();
      // After restart
      assertQuorumProcessAllRequests(D2_CONFIG_DATA);
    }
    else
    {
      fail("Quorum is unable to identify zk leader. No tests were executed.");
    }
  }

  public void testD2WithQuorumLeaderPeerDown() throws Exception
  {
    setup();
    assertQuorumProcessAllRequests(D2_CONFIG_DATA);
    // Shutdown zk _quorum leader peer
    ZKPeer peer = _quorum.getQuorumLeader();
    if (peer != null)
    {
      int peerId = peer.getId();
      peer.shutdownPeer();
      // Verify requests are processed while leader goes down
      long start = System.currentTimeMillis();
      while (_quorum.getQuorumLeader() != null && _quorum.getQuorumLeader().equals(peer) && System.currentTimeMillis() < (start + TIMEOUT))
      {
        assertQuorumProcessAllRequests(D2_CONFIG_DATA);
      }
      start = System.currentTimeMillis();
      while (_quorum.getQuorumLeader() == null && System.currentTimeMillis() < (start + TIMEOUT))
      {
        assertQuorumProcessAllRequests(D2_CONFIG_DATA); // wait while zookeeper is establishing a new leader
      }
      if (_quorum.getQuorumLeader() == null)
      {
        _quorum.restartAll();
      }
      else
      {
         assertTrue(! _quorum.getQuorumLeader().equals(peer), "No new _quorum leader was established. New leader id=" + _quorum.getQuorumLeader().getId());
        // Restart leader peer
        _quorum.restart(peerId);
      }
      start = System.currentTimeMillis();
      peer = _quorum.get(peerId);
      // Verify requests are processed while peer is restarting
      while (System.currentTimeMillis() < (start + TIMEOUT) && ( peer.getZKServer() == null || ! _quorum.areAllPeersUp() || ! peer.getZKServer().isRunning()))
      {
        assertQuorumProcessAllRequests(D2_CONFIG_DATA);
      }
      // After restart
      assertQuorumProcessAllRequests(D2_CONFIG_DATA);
    }
    else
    {
      fail("Quorum is unable to identify zk leader. No tests were executed.");
    }
  }

  public void testD2WithQuorumLeaderServerKilled() throws Exception
  {
    setup();
    // Kill _quorum leader peer zk server
    ZKPeer peer = _quorum.getQuorumLeader();
    if (peer != null)
    {
      peer.killPeerZkServer();
      assertQuorumProcessAllRequests(D2_CONFIG_DATA);
      assertFalse(peer.getZKServer().isRunning(),"_quorum Peer #"+peer.getId()+"/port "+peer.getClientPort()+" server has not shut down.");
      // Restart peer zk server
      peer.startupPeerZkServer();
      // Assert requests are processed while peer is restarting
      long start = System.currentTimeMillis();
      while (peer.getZKServer() == null && System.currentTimeMillis() < start + TIMEOUT)
      {
        assertQuorumProcessAllRequests(D2_CONFIG_DATA);
      }
      _quorum.assertAllPeersUp();
      // After restart
      assertQuorumProcessAllRequests(D2_CONFIG_DATA);
    }
    else
    {
      fail("Quorum unable to identify zk leader. No tests were executed.");
    }
  }

  public void testD2WithQuorumPeerAdded() throws Exception
  {
    setup();
    // Add new peer to ZK _quorum
    int newPeerId = _quorum.addPeer();
    _quorum.start(newPeerId);
    assertQuorumProcessAllRequests(D2_CONFIG_DATA);

    // Remove peer
    _quorum.get(newPeerId).shutdown(true);
    assertQuorumProcessAllRequests(D2_CONFIG_DATA);
  }

  private void assertQuorumProcessAllRequests(String clustersData) throws Exception
  {
    _quorum.printPeersStats();
    assertQuorumProcessAllRequests(1, clustersData, _zkUriString, _cli, _client, null);
  }

  private void startAllEchoServers() throws Exception
  {
    _echoServers = new ArrayList<>();
    Map<Integer, Double> partitionWeight = new HashMap<>();
    partitionWeight.put(Integer.valueOf(1), Double.valueOf(1.0d));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT1_1, "cluster-1", "service-1_1", "service-1_2", "service-1_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT1_2, "cluster-1", "service-1_1", "service-1_2", "service-1_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT2_1, "cluster-2", "service-2_1", "service-2_2", "service-2_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT2_2, "cluster-2", "service-2_1", "service-2_2", "service-2_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT3, "cluster-3", "service-3_1", "service-3_2", "service-3_3" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT4, "cluster-4", partitionWeight, "service-4_11", "service-4_12", "service-4_13" ));
    _echoServers.add(startEchoServer(getHost(_zkHosts[0]), getPort(_zkHosts[0]), ECHO_SERVER_HOST, ECHO_SERVER_PORT5, "cluster-4", partitionWeight, "service-4_11", "service-4_12", "service-4_13" ));
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
      LoadBalancerUtil.syncShutdownClient(_client, _log);
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
    }
    try
    {
      stopAllEchoServers(_echoServers);
    }
    catch (Exception e)
    {
    }
    cleanupTempDir();
  }
}

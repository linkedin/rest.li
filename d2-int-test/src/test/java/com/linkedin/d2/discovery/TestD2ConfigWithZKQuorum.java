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

package com.linkedin.d2.discovery;

import static org.testng.Assert.assertEquals;
import java.util.Iterator;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import com.linkedin.d2.D2BaseTest;
import com.linkedin.d2.quorum.ZKPeer;
import com.linkedin.d2.quorum.ZKQuorum;
import com.linkedin.d2.balancer.util.LoadBalancerClientCli;

@Test (groups = {"d2integration"})
public class TestD2ConfigWithZKQuorum extends D2BaseTest
{
  private static final String       HOST = "127.0.0.1";
  private static final int          QUORUM_SIZE = 9;
  private ZKQuorum                  _quorum;

  private void setup() throws Exception
  {
    // Start _quorum
    _quorum = new ZKQuorum(QUORUM_SIZE);
    _quorum.startAll();
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

  public void testD2ConfigWithZKQuorumV3DegraderDefaultPartitionAllValidHosts() throws Exception
  {
    setup();
    assertEquals(LoadBalancerClientCli.runDiscovery(_quorum.getHosts(), "/d2", D2_CONFIG_DEFAULT_PARTITION_DATA), 0);
  }

  public void testD2ConfigWithZKQuorumV3DegraderDefaultPartitionFollowerPeerDown() throws Exception
  {
    setup();
    String connectionString = _quorum.getHosts();
    ZKPeer follower = _quorum.getQuorumFollower();
    follower.shutdownPeer();
    assertEquals(LoadBalancerClientCli.runDiscovery(connectionString, "/d2", D2_CONFIG_DEFAULT_PARTITION_DATA), 0);
  }

  public void testD2ConfigWithZKQuorumV3DegraderDefaultPartitionLeaderPeerDown() throws Exception
  {
    setup();
    String connectionString = _quorum.getHosts();
    ZKPeer leader = _quorum.getQuorumLeader();
    leader.shutdownPeer();
    assertEquals(LoadBalancerClientCli.runDiscovery(connectionString, "/d2", D2_CONFIG_DEFAULT_PARTITION_DATA), 0);
  }

  public void testD2ConfigWithZKQuorumV3DegraderDefaultPartitionMultiplePeersDown() throws Exception
  {
    setup();
    String connectionString = _quorum.getHosts();
    int iterations = _quorum.size() - 5;
    Iterator<Integer> it = _quorum.iterator();
    for (int i = 0; i < iterations; i++)
    {
      _quorum.shutdown(it.next());
    }
    assertEquals(LoadBalancerClientCli.runDiscovery(connectionString, "/d2", D2_CONFIG_DEFAULT_PARTITION_DATA), 0);
  }

  public void testD2ConfigWithZKQuorumV3DegraderCustomPartitionAllValidHosts() throws Exception
  {
    setup();
    assertEquals(LoadBalancerClientCli.runDiscovery(_quorum.getHosts(), "/d2", D2_CONFIG_CUSTOM_PARTITION_DATA), 0);
  }

  public void testD2ConfigWithZKQuorumV3DegraderCustomPartitionAllValidHosts2() throws Exception
  {
    setup();
    assertEquals(LoadBalancerClientCli.runDiscovery(_quorum.getHosts(), "/d2", D2_CONFIG_CUSTOM_PARTITION_DATA2), 0);
  }

  public void testD2ConfigWithZKQuorumV3DegraderCustomPartitionFollowerPeerDown() throws Exception
  {
    setup();
    String connectionString = _quorum.getHosts();
    ZKPeer follower = _quorum.getQuorumFollower();
    follower.shutdownPeer();
    assertEquals(LoadBalancerClientCli.runDiscovery(connectionString, "/d2", D2_CONFIG_DEFAULT_PARTITION_DATA), 0);
  }

  public void testD2ConfigWithZKQuorumV3DegraderCustomPartitionMultiplePeersDown() throws Exception
  {
    setup();
    String connectionString = _quorum.getHosts();
    int iterations = _quorum.size() - 5;
    Iterator<Integer> it = _quorum.iterator();
    for (int i = 0; i < iterations; i++)
    {
      _quorum.shutdown(it.next());
    }
    assertEquals(LoadBalancerClientCli.runDiscovery(connectionString, "/d2", D2_CONFIG_DEFAULT_PARTITION_DATA), 0);
  }

  public void testD2ConfigWithZKQuorumV3DegraderCustomPartitionLeaderPeerDown() throws Exception
  {
    setup();
    String connectionString = _quorum.getHosts();
    ZKPeer leader = _quorum.getQuorumLeader();
    leader.shutdownPeer();
    assertEquals(LoadBalancerClientCli.runDiscovery(connectionString, "/d2", D2_CONFIG_DEFAULT_PARTITION_DATA), 0);
  }

  public void testD2ConfigWithZKQuorumV3DegraderDefaultPartitionInvalidHost() throws Exception
  {
    setup();
    String connectionString = _quorum.getHosts() + "," + HOST + ":9999";
    assertEquals(LoadBalancerClientCli.runDiscovery(connectionString, "/d2", D2_CONFIG_DEFAULT_PARTITION_DATA), 0);
  }

  public void testD2ConfigWithZKQuorumV3DegraderDefaultPartitionNullHost() throws Exception
  {
    setup();
    String connectionString = _quorum.getHosts() + ",,";
    assertEquals(LoadBalancerClientCli.runDiscovery(connectionString, "/d2", D2_CONFIG_DEFAULT_PARTITION_DATA), 0);
  }

  public void testD2ConfigWithZKQuorumV3DegraderCustomPartitionNullHost() throws Exception
  {
    setup();
    String connectionString = _quorum.getHosts() + ",";
    assertEquals(LoadBalancerClientCli.runDiscovery(connectionString, "/d2", D2_CONFIG_DEFAULT_PARTITION_DATA), 0);
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
    cleanupTempDir();
  }
}

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

package com.linkedin.d2.quorum;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeer.LearnerType;
import org.apache.zookeeper.server.quorum.QuorumPeer.QuorumServer;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.linkedin.d2.discovery.stores.zk.ZKTestUtil;

public class ZKQuorum
{
  private static final Logger         _log = LoggerFactory.getLogger(ZKQuorum.class);

  public static final int             CONNECTION_TIMEOUT = 10000;
  private static final String         HOST = "127.0.0.1";
  private Map<Integer, ZKPeer>        _peers;
  private Map<Long, QuorumServer>     _peersView;
  private  int                        _peerCount;
  private String                      _hosts;

  /**
   * Initializes n quorum peers which will form a ZooKeeper ensemble.
   * @param n - number of peers in the ensemble ( for test stability, set peer number in a quorum to 7+ (7, 9 or 11) )
   */

  public ZKQuorum(int ttlPeersCount) throws IOException, Exception
  {
    _peers = new HashMap<Integer, ZKPeer>();
    _peersView = new HashMap<Long, QuorumServer>();
    _peerCount = ttlPeersCount;
    _hosts = "";

    if (ttlPeersCount < 7)
    {
      throw new Exception("ZK Quorum Failure. Too few peers in the quorum:" + ttlPeersCount + ". For test stability, increase number of quorum peers (recommended number - 7 or 9 or 11).");
    }

    // Create peer views and list of hosts
    for (int i = 1; i <= _peerCount; ++i)
    {
      createNewPeerData(i);
    }

    for (int i = 1; i <= _peerCount; ++i)
    {
      setQuorumPeer(i);
    }
  }

  private void createNewPeerData(int id)
  {
    int clientPort = ZKTestUtil.getRandomPort();
    int quorumPort = ZKTestUtil.getRandomPort() + 1000;
    int electionPort = ZKTestUtil.getRandomPort() + 1001;
    ZKPeer zkpeer = new ZKPeer(id, ZKTestUtil.createTempDir("zkdata"+id), ZKTestUtil.createTempDir("zklog"+id), HOST, clientPort, quorumPort, electionPort);
    _peers.put(id, zkpeer);
    _peersView.put(Long.valueOf(id), new QuorumServer(id, HOST, quorumPort, electionPort, LearnerType.PARTICIPANT));

    _log.info("Created peer #" + id + " with ports:" + clientPort + "/" + quorumPort + "/" + electionPort + "  peer server addr:"+_peersView.get(Long.valueOf(id)).addr+"  peer server electionAddr:"+_peersView.get(Long.valueOf(id)).electionAddr);
  }

  private void setPeer(int id) throws Exception
  {
    try
    {
      _peers.get(id).setQuorumPeer(_peerCount, _peersView);
    }
    catch (java.net.BindException e)
    {
      _log.info("Removing peer#" + getQuorumPeerPortsInfo(id) + " from peers. Got BindException", e);
      recreatePeer(id);
    }
    _hosts += ((_hosts.length() > 0) ? "," : "") + HOST + ":" + _peers.get(id).getClientPort();
  }

  private void setQuorumPeer(int id)  throws Exception
  {
    setPeer(id);
  }

  public int addPeer() throws Exception
  {
    createNewPeerData(++_peerCount);
    setQuorumPeer(_peerCount);

    return _peers.size();
  }

  public ZKPeer get(int id)
  {
    return _peers.get(id);
  }

  public QuorumPeer getQuorumPeer(int id)
  {
    if (isExists(id))
    {
      return _peers.get(id).getPeer();
    }
    return null;
  }

  public int getPeerCount()
  {
    return _peers.size();
  }

  public ZooKeeperServer getPeerZKServer(int id)
  {
    if (isExists(id) && _peers.get(id).getPeer() != null)
    {
      return _peers.get(id).getZKServer();
    }
    return null;
  }

  public String getHosts()
  {
    return _hosts;
  }

  public ZKPeer getQuorumLeader()
  {
    for (Integer id : _peers.keySet())
    {
      if (_peers.get(id).getPeerType().equals("leader"))
      {
        return _peers.get(id);
      }
    }

    return null;
  }

  public ZKPeer getQuorumFollower()
  {
    for (Integer id : _peers.keySet())
    {
      if (_peers.get(id).getPeerType().equals("follower"))
      {
        return _peers.get(id);
      }
    }

    return null;
  }

  private String getQuorumPeerPortsInfo(int id)
  {
    if (isExists(id))
    {
      return _peers.get(id).getPeerPortsInfo();
    }
    return "";
  }

  public boolean isAlive(int id)
  {
    if (isExists(id))
    {
      return _peers.get(id).isAlive();
    }
    return false;
  }

  public Iterator<Integer> iterator()
  {
    return _peers.keySet().iterator();
  }

  public int size()
  {
    return _peers.size();
  }

  public void start(int id) throws Exception
  {
    try
    {
      if (isExists(id))
      {
        _peers.get(id).start();
      }
    }
    catch (java.net.BindException e)
    {
      recreatePeer(id);
    }
  }

  private boolean isExists(int id)
  {
    if (_peers.get(id) != null)
    {
      return true;
    }
    return false;
  }

  public void restart(int id) throws IOException, Exception
  {
    _log.info("Restarting peer #" + getQuorumPeerPortsInfo(id));
    _peers.get(id).shutdown(false);
    setQuorumPeer(id);
    _peers.get(id).start();
  }

  public void restartPeersInTerminatedState() throws Exception
  {
    for (int i=1; i <= _peerCount; i++)
    {
      if (isExists(i))
      {
        if (_peers.get(i).getPeerState().equals("TERMINATED"))
        {
          restart(i);
        }
      }
    }
    waitForAllPeersUp();
  }

  public void startAll() throws IOException, Exception
  {
    for (int id=1; id <= _peerCount; id++)
    {
      if (isExists(id))
      {
        start(id);
        _log.info("Started Quorum peer #" + getQuorumPeerPortsInfo(id));
      }
    }

    waitForAllPeersUp();
  }

  public void waitForAllPeersUp() throws Exception
  {
    for (int id=1; id <= _peerCount; id++)
    {
      if (isExists(id))
      {
        _peers.get(id).waitForServerUp(CONNECTION_TIMEOUT);
      }
    }
    removeFailedPeers();
    assertAllPeersUp();
  }

  private void removeFailedPeers() throws Exception
  {
    for (int id=1; id <= _peerCount; id++)
    {
      if (isExists(id))
      {
        if (_peers.get(id).getPeerState().equals("TERMINATED") || _peers.get(id).getZKServer() == null ||  ! _peers.get(id).isZKServerRunning())
        {
          // Remove failed peer
          _peers.get(id).printPeerStats();
          deletePeer(id);
        }
      }
    }
    if (_peers.size() % 2 == 0 && _peers.size() > 5) // min recommended quorum size is 5
    {
      deletePeer(Collections.min(_peers.keySet()));
    }
  }

  public void assertAllPeersUp()
  {
    printPeersStats();
    for (int id=1; id <= _peerCount; id++)
    {
      if (_peers.containsKey(id))
      {
        assertNotNull(_peers.get(id), "ZK Quorum Failure. Peer #" + id + " is null.");
        assertNotNull(_peers.get(id).getZKServer(), "ZK Quorum Failure. Null ZK Server for Peer #" + getQuorumPeerPortsInfo(id) + ".");
        assertTrue(_peers.get(id).getZKServer().isRunning(), "ZK Quorum Failure. ZK Server is not running. Peer #" + getQuorumPeerPortsInfo(id));
      }
    }
  }

  public boolean areAllPeersUp()
  {
    for (Integer id : _peers.keySet())
    {
      if (_peers.get(id).getZKServer() == null)
      {
        return false;
      }
      else if (!_peers.get(id).getZKServer().isRunning())
      {
        return false;
      }
    }
    return true;
  }

  public void restartAll() throws Exception
  {
    for (int i=1; i <= _peerCount; i++)
    {
      if (isExists(i))
      {
        restart(_peers.get(i).getId());
      }
    }
    waitForAllPeersUp();
  }

  public void shutdown(int id) throws Exception
  {
    shutdown(id, false);
  }

  public void shutdown(int id, boolean removeDir) throws Exception
  {
    if (removeDir)
    {
      deletePeer(id);
    }
    else
    {
      if (_peers.get(id) != null)
      {
        _peers.get(id).shutdown(removeDir);
      }
    }
  }

  public void shutdownAll(boolean removeDirectories) throws Exception
  {
    // Shutdown leader first
    ZKPeer p = getQuorumLeader();
    if (p != null)
    {
      try
      {
        p.shutdown(true);
      }
      catch (Exception e)
      {
        _log.warn("Failed to shutdown peer #"+ getQuorumPeerPortsInfo(p.getId()), e);
      }
    }

    for (int id=1; id <= _peerCount; id++)
    {
      if (isExists(id))
      {
        try
        {
          shutdown(id, removeDirectories);
        }
        catch (Exception e)
        {
          _log.warn("Failed to shutdown peer #" + getQuorumPeerPortsInfo(id), e);
        }
      }
    }
    Thread.sleep(600); // give zk extra time to complete shutdown
  }

  public void deletePeer(int id) throws Exception
  {
    if (isExists(id))
    {
      _log.info("Deleting peer #" + getQuorumPeerPortsInfo(id));
      int clientPort = _peers.get(id).getClientPort();
      _peers.get(id).shutdown(true);
      _peers.get(id).waitForServerDown(900);
      removePeerFromQuorumPeersMap(id, clientPort);
    }
  }

  private void recreatePeer(int id) throws Exception
  {
    _log.info("Recreating peer#" + getQuorumPeerPortsInfo(id));
    _peers.get(id).shutdownPeer();
    _peers.remove(id);
    _peersView.remove(id);
    // Generate new peer
    createNewPeerData(id);
    setQuorumPeer(id);
    _log.info("Recreated peer#" + getQuorumPeerPortsInfo(id));
  }

  public void closeAllPeers()
  {
    for (int id=1; id <= _peerCount; id++)
    {
      if (isExists(id))
      {
        closePeer(id);
      }
    }
  }

  public void closePeer(int id)
  {
    try
    {
      if (_peers.get(id) != null)
      {
        _peers.get(id).close();
      }
    }
    catch (Exception e)
    {
      _log.info("Failed to close peer #" + id, e);
    }
  }

  private void removePeerFromQuorumPeersMap(int id, int clientPort)
  {
    _log.debug("Removing failed peer #" + id);
    _peers.remove(id);
    _peersView.remove(id);
    String failedZk = HOST + ":" + clientPort;
    _hosts = _hosts.replaceAll("[,]*"+failedZk+"[,]*", ((_hosts.indexOf(failedZk) > 0 && _hosts.indexOf(failedZk) + failedZk.length() < _hosts.length()) ? "," : ""));
  }

  public void printPeersStats()
  {
    for (Integer id : _peers.keySet())
    {
      _peers.get(id).printPeerStats();
    }
  }
}

package com.linkedin.d2.discovery.quorum;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
  private static final Logger _log = LoggerFactory.getLogger(ZKQuorum.class);

  public static final int             CONNECTION_TIMEOUT = 10000;
  private Map<Integer, ZKPeer>        _peers;
  private Map<Long, QuorumServer>     _peersView;
  private  int                        _peerCount;
  private String                      _hosts;

  /**
   * Initializes n quorum peers which will form a ZooKeeper ensemble.
   * @param n - number of peers in the ensemble
   */

  public ZKQuorum(int ttlPeersCount) throws IOException
  {
    _peers = new HashMap<Integer, ZKPeer>();
    _peersView = new HashMap<Long, QuorumServer>();
    _peerCount = ttlPeersCount;
    _hosts = "";

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
    ZKPeer zkpeer = new ZKPeer(id, ZKTestUtil.createTempDir("zkdata"+id), ZKTestUtil.createTempDir("zklog"+id), "127.0.0.1", clientPort);
    _peers.put(id, zkpeer);
    _peersView.put(Long.valueOf(id), new QuorumServer(id,
                                                     new InetSocketAddress("127.0.0.1", clientPort + 1000),
                                                     new InetSocketAddress("127.0.0.1", ZKTestUtil.getRandomPort() + 1001),
                                                     LearnerType.PARTICIPANT));

    _log.info("Created peer with port:"+clientPort+"  peer server addr:"+_peersView.get(Long.valueOf(id)).addr+"  peer server electionAddr:"+_peersView.get(Long.valueOf(id)).electionAddr);
  }

  private void setPeer(int id) throws IOException
  {
    _peers.get(id).setQuorumPeer(_peerCount, _peersView);
    _hosts += ((_hosts.length() > 0) ? "," : "") + "127.0.0.1:" + _peers.get(id).getClientPort();
  }

  private void setQuorumPeer(int id)  throws IOException
  {
    setPeer(id);
  }

  public int addPeer() throws IOException
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
    return _peers.get(id).getPeer();
  }

  public int getPeerCount()
  {
    return _peers.size();
  }

  public ZooKeeperServer getPeerActiveServer(int id)
  {
    return _peers.get(id).getPeer().getActiveServer();
  }

  public String getHosts()
  {
    return _hosts;
  }

  public ZKPeer getQuorumLeader()
  {
    for (Integer i : _peers.keySet())
    {
      if (_peers.get(i).getPeer().leader != null)
      {
        return _peers.get(i);
      }
    }

    return null;
  }

  public ZKPeer getQuorumFollower()
  {
    for (Integer i : _peers.keySet())
    {
      if (_peers.get(i).getPeer().follower != null)
      {
        return _peers.get(i);
      }
    }

    return null;
  }

  public boolean isAlive(int id)
  {
    return _peers.get(id).getPeer().isAlive();
  }

  public void start(int id) throws IOException, Exception
  {
    try
    {
      _log.debug("Starting peer #"+_peers.get(id)+"/port "+_peers.get(id).getClientPort()+" " + _peers.get(id).getPeerName());
      _peers.get(id).start();
    }
    catch (java.net.BindException e)
    {
      _log.info("Removing peer#"+_peers.get(id)+"/port "+_peers.get(id).getClientPort()+" " + _peers.get(id).getPeerName() + " from peers. Got BindException"+e);
      e.printStackTrace();
      _peers.get(id).shutdownPeer();
      _peers.remove(id);
      _peersView.remove(id);
      // Generate new peer
      createNewPeerData(id);
      setQuorumPeer(id);
    }
  }

  public void restart(int id) throws IOException, Exception
  {
    _log.info("Restarting peer #"+_peers.get(id)+"/port "+_peers.get(id).getClientPort()+" " + _peers.get(id).getPeerName());
    _peers.get(id).shutdown(false);
    setQuorumPeer(id);
    _peers.get(id).start();
  }

  public void startAll() throws IOException, Exception
  {
    for (Integer i : _peers.keySet())
    {
      start(i);
      _log.info("Started Quorum peer #"+_peers.get(i)+"/port "+_peers.get(i).getClientPort()+" " + _peers.get(i).getPeerName());
    }

    waitForAllPeersUp();
  }

  public void waitForAllPeersUp() throws Exception
  {
    for (Integer i : _peers.keySet())
    {
      if (! _peers.get(i).waitForServerUp(CONNECTION_TIMEOUT)) // && _peers.get(i).getPeer().getActiveServer() != null && _peers.get(i).getPeer().getActiveServer().isRunning())
      {
        recreatePeer(i);
      }
    }
    assertAllPeersUp();
  }

  public void assertAllPeersUp()
  {
    for (Integer i : _peers.keySet())
    {
      assertTrue(_peers.get(i).waitForServerUp(CONNECTION_TIMEOUT), "Quorum peer #"+_peers.get(i).getId()+"/ZK server (port "+_peers.get(i).getClientPort()+") failed to start.");
      assertNotNull(_peers.get(i).getZKServer());
      assertTrue(_peers.get(i).getZKServer().isRunning());
    }
  }

  public boolean areAllPeersUp()
  {
    for (Integer i : _peers.keySet())
    {
      if (_peers.get(i).getZKServer() == null)
      {
        return false;
      }
      else if (!_peers.get(i).getZKServer().isRunning())
      {
        return false;
      }
    }
    return true;
  }

  public void restartAll() throws Exception
  {
    for (Integer i : _peers.keySet())
    {
      restart(i);
    }
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
    Set<Integer> ids = _peers.keySet(); 
    for (Integer id : ids)
    {
      try
      {
        shutdown(id, removeDirectories);
        _log.error("\n\nSHUTDOWN ALL removed peer #"+id);
      }
      catch (Exception e)
      {
        _log.error("Failed to shutdown peer #"+_peers.get(id)+"/port "+_peers.get(id).getClientPort()+" " + _peers.get(id).getPeerName());
        e.printStackTrace();
      }
    }
    closeAllPeers();
  }

  public void deletePeer(int peerId) throws Exception
  {
    if (_peers.containsKey(peerId))
    {
      _log.error("Deleting #"+peerId);
      if (_peers.get(peerId) != null)
      {
        _peers.get(peerId).shutdown(true);
        _peers.get(peerId).waitForServerDown(CONNECTION_TIMEOUT);
      }
    }
  }
  
  public void closeAllPeers()
  {
    for (Integer id : _peers.keySet())
    {
      closePeer(id);
    }
  }
  
  public void closePeer(int id)
  {
    try
    {
      _peers.get(id).close();
      _peers.remove(id);
    }
    catch (Exception e)
    {
      // do nothing
      e.printStackTrace();
    }
  }

  private int recreatePeer(int peerId) throws Exception
  {
    deletePeer(peerId);
    // Create new peer
    int newId = addPeer();
    _peers.get(newId).start();
    _log.info("Quorum peer #" + peerId + "/ZK server (port " + _peers.get(peerId).getClientPort() + ") failed to start. Creating new peer with id=" + newId);
    return newId;
  }
}

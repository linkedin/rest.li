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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.Election;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeer.LearnerType;
import org.apache.zookeeper.server.quorum.QuorumPeer.QuorumServer;
import org.apache.zookeeper.server.quorum.flexible.QuorumMaj;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author aclayton
 */

public class ZKPeer
{
  private static final Logger _log               = LoggerFactory.getLogger(ZKPeer.class);
  private static int           TIMEOUT           = 6000;

  private static int          _tickTime          = 3000;
  private static int          _initLimit         = 10;
  private static int          _syncLimit         = 5;
  private static int          _electionAlg       = 3;
  private static int          _minSessionTimeout = -1;
  private static int          _maxSessionTimeout = -1;
  private static int          _maxClientCnxns    = 100;

  private int                   _id;
  private QuorumPeer            _peer;
  private File                  _dataDir;
  private File                  _logDir;
  private String                _host;
  private int                   _clientPort;
  private int                   _electionPort;
  private int                   _quorumPort;

  public ZKPeer(int id, File dataDir, File logDir, String host, int clientPort, int quorumPort, int electionPort)
  {
    _id = id;
    _dataDir = dataDir;
    _logDir = logDir;
    _host = host;
    _clientPort = clientPort;
    _quorumPort = quorumPort;
    _electionPort = electionPort;
  }

  public void setQuorumPeer() throws IOException
  {
    setQuorumPeer(_peer.getView().size(), _peer.getView(), new FileTxnSnapLog(_logDir, _dataDir));
  }

  public void setQuorumPeer(int peersCount, Map<Long, QuorumServer> peersView) throws IOException
  {
    setQuorumPeer(peersCount, peersView, new FileTxnSnapLog(_logDir, _dataDir));
  }

  public void setQuorumPeer(int peersCount,
                            Map<Long, QuorumServer> peersView,
                            FileTxnSnapLog fts) throws IOException
  {
    NIOServerCnxn.Factory cnxnFactory =
          new NIOServerCnxn.Factory(new InetSocketAddress("127.0.0.1", _clientPort), _maxClientCnxns);

    _peer = new QuorumPeer();
    _peer.setClientPortAddress(new InetSocketAddress("127.0.0.1", _clientPort));
    _peer.setTxnFactory(fts);
    _peer.setQuorumPeers(peersView);
    _peer.setElectionType(_electionAlg);
    _peer.setMyid(_id);
    _peer.setTickTime(_tickTime);
    _peer.setMinSessionTimeout(_minSessionTimeout);
    _peer.setMaxSessionTimeout(_maxSessionTimeout);
    _peer.setInitLimit(_initLimit);
    _peer.setSyncLimit(_syncLimit);
    _peer.setQuorumVerifier(new QuorumMaj(peersCount));
    _peer.setCnxnFactory(cnxnFactory);
    _peer.setZKDatabase(new ZKDatabase(_peer.getTxnFactory()));
    _peer.setPeerType(LearnerType.PARTICIPANT);

  }

  public int getId()
  {
    return _id;
  }

  public int getClientPort()
  {
    return _clientPort;
  }

  public int getQuorumPort()
  {
    return _quorumPort;
  }

  public int getElectionPort()
  {
    return _electionPort;
  }

  public ZooKeeperServer getZKServer()
  {
    return _peer.getActiveServer();
  }

  public String getZKServerState()
  {
    if (_peer.getActiveServer() != null)
    {
      return _peer.getActiveServer().getState();
    }
    return "NA";
  }

  public boolean isZKServerRunning()
  {
    if (_peer.getActiveServer() != null)
    {
      return _peer.getActiveServer().isRunning();
    }
    return false;
  }

  public QuorumPeer getPeer()
  {
    return _peer;
  }

  public File getPeerDataDirectory()
  {
    return _peer.getTxnFactory().getDataDir();
  }

  public File getPeerSnapshotDirectory()
  {
    return _peer.getTxnFactory().getSnapDir();
  }

  public FileTxnSnapLog getPeerTxnFactory()
  {
    return _peer.getTxnFactory();
  }

  public String getPeerName()
  {
    return _peer.getName();
  }

  public String getPeerState()
  {
    return _peer.getServerState();
  }

  public String getPeerPortsInfo()
  {
    if (_peer != null)
    {
      return  _id + ". Ports: " + _clientPort + "/" + _quorumPort + "/" + _electionPort + " " + _peer.getName();
    }
    return null;
  }

  public String getPeerType()
  {
    if (_peer.leader != null)
    {
      return "leader";
    }
    else if (_peer.follower != null)
    {
      return "follower";
    }
    else if (_peer.observer != null)
    {
      return "observer";
    }
    return "NA";
  }

  public Map<Long, QuorumServer> getPeerView()
  {
    return _peer.getView();
  }

  public boolean isRunning()
  {
    return _peer.isRunning();
  }

  public boolean isAlive()
  {
    return _peer.isAlive();
  }

  public void setPort(int port)
  {
    _clientPort = port;
  }

  public boolean start() throws IOException
  {
    _log.info("Starting Peer #" +getPeerPortsInfo());
    _peer.start();

    if (isAlive() || isRunning())
    {
      return true;
    }
    else
    {
      return false;
    }
  }

  public void shutdownPeer()
  {
    String msg = "Peer #" + getPeerPortsInfo();
    _log.info("Shutting down " + msg);
    _peer.shutdown();
    assertTrue(waitForServerDown(TIMEOUT), msg + " has not shut down.");
  }

  public void shutdownPeerZkServer()
  {
    _log.info("Shutting down peer zk ActiveServer. Peer #" + getPeerPortsInfo());
    _peer.getActiveServer().shutdown();
  }

  public void startupPeerZkServer() throws Exception
  {
    _log.info("Starting peer zk ActiveServer. Peer #" + getPeerPortsInfo());
    _peer.getActiveServer().startup();
  }

  public void shutdown(boolean removeDirectories) throws IOException
  {
    try
    {
      String peerType = "";
      if (_peer.leader != null)
      {
        peerType = "leader";
      }
      _log.info("Shutting down quorum " + peerType + " peer #" + getPeerPortsInfo());
      try
      {
        _peer.shutdown();
      }
      catch (Exception e)
      {
        _log.debug("Exception during peer " + _peer.getName() + " shutdown.", e);
      }
      Election e = _peer.getElectionAlg();
      if (e != null)
      {
        _log.debug("Shutting down " + _peer.getName() + " leader election ");
        e.shutdown();
      }
      else
      {
        _log.debug("No election available to shutdown ");
      }
      _log.debug("Waiting for " + _peer.getName() + " to exit thread");
      _peer.join(60000);
      if (_peer.isAlive())
      {
        fail("QP failed to shutdown in 60 seconds: " + _peer.getName());
      }

      if (removeDirectories)
      {
        FileUtils.deleteDirectory(_dataDir);
        FileUtils.deleteDirectory(_logDir);
      }
    }
    catch (InterruptedException e)
    {
      _log.debug("Shutdown interrupted: " + _peer.getName(), e);
    }
    try
    {
      waitForServerDown(TIMEOUT);
    }
    catch (Exception e)
    {
    }
  }

  public void killQuorumPeer()
  {
    try
    {
      if (_peer != null)
      {
        _log.info("Killing quorum peer #" + getPeerPortsInfo());

        Field cnxnFactoryField = QuorumPeer.class.getDeclaredField("cnxnFactory");
        cnxnFactoryField.setAccessible(true);
        NIOServerCnxn.Factory cnxnFactory =
            (NIOServerCnxn.Factory) cnxnFactoryField.get(_peer);
        cnxnFactory.shutdown();

        Field ssField = cnxnFactory.getClass().getDeclaredField("ss");
        ssField.setAccessible(true);
        ServerSocketChannel ss = (ServerSocketChannel) ssField.get(cnxnFactory);
        ss.close();
      }
      close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  public void killPeerZkServer()
  {
    ZooKeeperServer zserver = _peer.getActiveServer();

    try
    {
      _log.info("Killing quorum zk server. Peer #" + getPeerPortsInfo());
      Field cnxnFactoryField = ZooKeeperServer.class.getDeclaredField("serverCnxnFactory");
      cnxnFactoryField.setAccessible(true);

      NIOServerCnxn.Factory cnxnFactory =
          (NIOServerCnxn.Factory) cnxnFactoryField.get(zserver);
      cnxnFactory.shutdown();

      Field ssField = cnxnFactory.getClass().getDeclaredField("ss");
      ssField.setAccessible(true);
      ServerSocketChannel ss = (ServerSocketChannel) ssField.get(cnxnFactory);
      ss.close();
      close();
    }
    catch (Exception e)
    {
      // do nothing - this method is only for testing
    }
  }

  public void close() throws Exception
  {
    _log.info("Closing quorum peer #" + getPeerPortsInfo());
    try
    {
      ZooKeeperServer zkServer = _peer.getActiveServer();
      if (zkServer != null)
      {
        ZKDatabase zkDb = zkServer.getZKDatabase();
        if (zkDb != null)
        {
          // make ZK server close its log files
          zkDb.close();
        }
      }
    }
    catch (Exception e)
    {
      _log.debug("Failed to close peer #" + getPeerPortsInfo(), e);
    }
  }

  public String printPeerStats()
  {
   StringBuffer sb = new StringBuffer();
    if (_peer != null)
    {
      sb.append("\n======================= Peer #" + getPeerPortsInfo() + " Stats ===================");
      sb.append("\nPeer Type:");
      sb.append(getPeerType());
      sb.append(", Peer State: ");
      sb.append(getPeerState());
      sb.append(", Peer IsRunning: ");
      sb.append(isRunning());
      sb.append(", Peer isAlive: ");
      sb.append(isAlive());
      if (_peer.getActiveServer() != null)
      {
       sb.append("\nZK Server State: ");
       sb.append(getZKServerState());
       sb.append(", K Server IsRunning: ");
       sb.append(isZKServerRunning() + "\n");
      }
      _log.debug(sb.toString());
    }

    return sb.toString();
  }

  public boolean waitForServerUp(long timeout)
  {
    long start = System.currentTimeMillis();
    long end = start + timeout;
    while (true)
    {
      try
      {
        _log.info("............... Waiting for ZK peer to come up. Sending 'srvr' command to peer #" + getPeerPortsInfo() );
        String result = sendCommand(_host, _clientPort, "srvr");
        if (result.startsWith("Zookeeper version:"))
        {
          return true;
        }
      }
      catch (IOException e)
      {
        // ignore as this is expected
      }

      if (System.currentTimeMillis() > end)
      {
        if (_peer.getActiveServer() != null && _peer.getActiveServer().isRunning())
        {
          return true;
        }
        else
        {
          return false;
        }
      }
      try
      {
        Thread.sleep(250);
      }
      catch (InterruptedException e)
      {
        // ignore
      }
    }
  }

  public boolean waitForServerDown(long timeout)
  {
    long start = System.currentTimeMillis();
    while (true)
    {
      try
      {
        _log.info("---------------- Waiting for ZK peer to shutdown. Sending 'srvr' command to peer #" + getPeerPortsInfo());
        sendCommand(_host, _clientPort, "srvr");
      }
      catch (IOException e)
      {
        _log.info("GOT IO EXCEPTION. SERVER " + _host + ":" + _clientPort + " IS DOWN.");
        return true;
      }

      if (_peer.getActiveServer() == null || ! _peer.getActiveServer().isRunning())
      {
        return true;
      }

      if (System.currentTimeMillis() > (start + timeout))
      {
        break;
      }
      try
      {
        Thread.sleep(250);
      }
      catch (InterruptedException e)
      {
        // ignore
      }
    }
    return false;
  }

  /**
   * Send the 4letterword
   *
   * @param host- destination host
   * @param port- destination port
   * @param cmd- the 4letterword (stat, srvr,etc. - see
   *          http://zookeeper.apache.org/doc/r3.3.3/zookeeperAdmin.html#sc_zkCommands)
   * @return
   * @throws IOException
   */
  public static String sendCommand(String host, int port, String cmd) throws IOException
  {
    // NOTE: ignore CancelledKeyException in logs caused by
    // https://issues.apache.org/jira/browse/ZOOKEEPER-1237
    Socket sock = new Socket(host, port);
    BufferedReader reader = null;
    try
    {
      OutputStream outstream = sock.getOutputStream();
      outstream.write(cmd.getBytes());
      outstream.flush();
      sock.shutdownOutput();

      reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null)
      {
        sb.append(line + "\n");
      }
      return sb.toString();
    }
    finally
    {
      sock.close();
      if (reader != null)
      {
        reader.close();
      }
    }
  }

}

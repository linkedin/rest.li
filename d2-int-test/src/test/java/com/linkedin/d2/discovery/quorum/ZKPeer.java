package com.linkedin.d2.discovery.quorum;

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
  private int                   _clientPort;
  private String                _host;

  public ZKPeer(int id, File dataDir, File logDir, String host, int clientPort)
  {
    _id = id;
    _dataDir = dataDir;
    _logDir = logDir;
    _host = host;
    _clientPort = clientPort;
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

  public ZooKeeperServer getZKServer()
  {
    return _peer.getActiveServer();
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

  public void start() throws IOException
  {
    _peer.start();
  }

  public void shutdownPeer()
  {
    String msg = "Peer #"+_peer.getId()+"/port "+_peer.getClientPort()+" " + _peer.getName();
    _log.debug("Shutting down " + msg);
    _peer.shutdown();
    assertTrue(waitForServerDown(TIMEOUT), msg + " has not shut down.");
  }

  public void shutdownPeerZkServer()
  {
    _log.debug("Shutting down zk ActiveServer. Peer #"+_peer.getId()+"/port "+_peer.getClientPort()+" " + _peer.getName());
    _peer.getActiveServer().shutdown();
  }
  
  public void startupPeerZkServer() throws Exception
  {
    _log.debug("Starting zk ActiveServer. Peer #"+_peer.getId()+"/port " + _peer.getClientPort());
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
      
      _log.info("Shutting down quorum " + peerType + " peer #"+_peer.getId()+"/port "+_peer.getClientPort()+" " + _peer.getName());
      _peer.shutdown();
      Election e = _peer.getElectionAlg();
      if (e != null)
      {
        _log.info("Shutting down leader election ");
        e.shutdown();
      }
      else
      {
        _log.info("No election available to shutdown ");
      }
      _log.info("Waiting for " + _peer.getName() + " to exit thread");
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
      _log.debug("QP interrupted: " + _peer.getName(), e);
    }
    waitForServerDown(TIMEOUT);
  }

  public void killQuorumPeer()
  {
    try
    {
      if (_peer != null)
      {
        _log.info("Killing quorum peer #"+_peer.getId()+"/port "+_peer.getClientPort()+" " + _peer.getName());
        
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
      _log.info("Killing quorum zk server peer #"+_peer.getId()+"/port "+_peer.getClientPort()+" " + _peer.getName());
      Field cnxnFactoryField =
          ZooKeeperServer.class.getDeclaredField("serverCnxnFactory");
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
      e.printStackTrace(); // just ignore - this class is only for testing
    }
  }

  public void close() throws IOException
  {
    try
    {
      _log.info("Closing quorum zk server peer #"+_peer.getId()+"/port "+_peer.getClientPort()+" " + _peer.getName());
      ZooKeeperServer zkServer = _peer.getActiveServer();
      _log.info("got peer zk server:"+zkServer);
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
      _log.debug("\n\n\n\n#############Failed to close peer #"+_peer.getId()+"/port "+_peer.getClientPort()+" " + _peer.getName());
      e.printStackTrace();
    }
  }

  public boolean waitForServerUp(long timeout)
  {
    long start = System.currentTimeMillis();
    while (true)
    {
      try
      {
        _log.info("............... Waiting for ZK peer to come up. Sending 'srvr' command to peer #"+getId()+" port:"+getClientPort());
        String result = sendCommand(_host, _clientPort, "srvr");
        if (result.startsWith("Zookeeper version:"))
        {
          return true;
        }
      }
      catch (IOException e)
      {
        // ignore as this is expected
        _log.info("ZK Peer server " + _host + ":" + _clientPort + " not up yet " + e);
      }

      if (System.currentTimeMillis() > (start + timeout))
      {
        if (_peer.getActiveServer() != null && _peer.getActiveServer().isRunning())
        {
          return true;
        }
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

  public boolean waitForServerDown(long timeout)
  {
    long start = System.currentTimeMillis();
    while (true)
    {
      try
      {
        _log.info("---------------- Waiting for ZK peer to shutdown. Sending 'srvr' command to peer #"+getId()+" port:"+getClientPort());
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

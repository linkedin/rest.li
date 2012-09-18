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

/**
 * $Id: $
 */

package com.linkedin.d2.discovery.stores.zk;

import org.apache.commons.io.FileUtils;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerMain;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * Very simple wrapper around ZooKeeper server, intended only for TEST use.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ZKServer
{
  private final ZooKeeperServer _zk;
  private final NIOServerCnxn.Factory _factory;
  private final File _dataDir;
  private final File _logDir;
  private final boolean _erase;

  private final CountDownLatch  _latch = new CountDownLatch(1);

  /**
   * Create a ZK server with automatically determined port, log and data dirs, which will be
   * erased upon shutdown.
   * @throws IOException
   */
  public ZKServer() throws IOException
  {
    this(0);
  }

  /**
   * Create a ZK server with automatically determined log and data dirs, which will be
   * erased upon shutdown.
   * @param port The port to listen on
   * @throws IOException
   */
  public ZKServer(int port) throws IOException
  {
    this(ZKTestUtil.createTempDir("data"), ZKTestUtil.createTempDir("log"), port, true);
  }

  /**
   * Create a ZK server with specified data and log dirs.
   * @param dataDir
   * @param logDir
   * @param port
   * @param erase if true, dataDir and logDir will be erased when shutdown() is called
   * @throws IOException
   */
  public ZKServer(File dataDir, File logDir, int port, boolean erase) throws IOException
  {
    _dataDir = dataDir;
    _logDir = logDir;
    _zk = new ZooKeeperServer(dataDir, logDir, 5000);
    _factory = new NIOServerCnxn.Factory(new InetSocketAddress(port));
    _erase = erase;

  }

  public int getPort()
  {
    return _factory.getLocalPort();
  }

  public void startup() throws IOException, InterruptedException
  {
    ensureDir(_dataDir);
    ensureDir(_logDir);
    _zk.startup();
    _factory.startup(_zk);
  }

  public void shutdown() throws IOException
  {
    shutdown(_erase);
  }

  public void shutdown(boolean erase) throws IOException
  {
    _factory.shutdown();
    try
    {
      _factory.join();
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
    _zk.shutdown();
    if (erase)
    {
      FileUtils.deleteDirectory(_dataDir);
      FileUtils.deleteDirectory(_logDir);
    }
  }

  private static void ensureDir(File dir) throws IOException
  {
    if (dir.exists() && !dir.isDirectory())
    {
      throw new IOException("Not a directory: " + dir.getAbsolutePath());
    }
  }

}

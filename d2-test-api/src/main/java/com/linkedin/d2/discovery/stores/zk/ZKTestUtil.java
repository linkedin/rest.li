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

package com.linkedin.d2.discovery.stores.zk;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.testng.Assert;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Steven Ihde
 */
public class ZKTestUtil
{
  private static final int TEMP_DIR_ATTEMPTS = 10;

  private ZKTestUtil()
  {

  }

  public static ZKConnection getConnection(String connectString, int timeout)
  {
    ZKConnection conn = new ZKConnection(connectString, timeout, false, true);
    try
    {
      conn.start();
      conn.waitForState(Watcher.Event.KeeperState.SyncConnected, timeout, TimeUnit.MILLISECONDS);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }

    return conn;
  }

  public static ZKServer startZKServer() throws  InterruptedException
  {
    ZKServer zkServer = null;
    try
    {
      zkServer = new ZKServer();
      zkServer.startup();
    }
    catch (IOException e)
    {
      Assert.fail("unable to instantiate real zk server.");
      e.printStackTrace();
    }

    return zkServer;
  }

  public static void expireSession(String connectString, ZooKeeper zk, long timeout, TimeUnit timeoutUnit)
          throws IOException, TimeoutException, InterruptedException
  {
    expireSession(connectString, zk.getSessionTimeout(), zk.getSessionId(), zk.getSessionPasswd(), timeout, timeoutUnit);
  }

  /**
   * This may not work correctly with a quorum, but it should work correctly with a single server.
   * @param connectString
   * @param sessionTimeout
   * @param sessionId
   * @param sessionPasswd
   * @param timeout
   * @param timeoutUnit
   * @throws IOException
   * @throws TimeoutException
   * @throws InterruptedException
   */
  public static void expireSession(String connectString, int sessionTimeout, long sessionId, byte[] sessionPasswd,
                                   long timeout, TimeUnit timeoutUnit)
          throws IOException, TimeoutException, InterruptedException
  {
    WaiterWatcher w = new WaiterWatcher();
    ZooKeeper zk = new VanillaZooKeeperAdapter(connectString, sessionTimeout, w, sessionId, sessionPasswd);

    // NB, we must wait for SyncConnected before calling close(), otherwise it may not actually kill the session
    w.waitForConnected(timeout, timeoutUnit);
    zk.close();
  }

  /**
   * Waits for the connection to re-establish after a session expire is triggered
   *
   * @param oldZKSessionId session id before the session is expired
   * @param zkPersistentConnection
   * @param timeout
   * @param timeoutUnit
   * @throws IOException
   * @throws TimeoutException
   * @throws InterruptedException
   */
  public static void waitForNewSessionEstablished(long oldZKSessionId, ZKPersistentConnection zkPersistentConnection, long timeout, TimeUnit timeoutUnit) throws IOException, TimeoutException, InterruptedException
  {
    Date deadline = new Date(System.currentTimeMillis() + timeoutUnit.toMillis(timeout));
    while (zkPersistentConnection.getZooKeeper().getSessionId() == oldZKSessionId
      && deadline.getTime() > System.currentTimeMillis())
    {
      Thread.sleep(100);
    }
    long remainingTime = deadline.getTime() - System.currentTimeMillis();
    zkPersistentConnection.getZKConnection().waitForState(Watcher.Event.KeeperState.SyncConnected, remainingTime, TimeUnit.MILLISECONDS);
  }

  private static class WaiterWatcher implements Watcher
  {
    private final Lock _lock = new ReentrantLock();
    private final Condition _connected = _lock.newCondition();
    private Event.KeeperState _state;

    @Override
    public void process(WatchedEvent watchedEvent)
    {
      Event.KeeperState state = watchedEvent.getState();
      if (state != null)
      {
        _lock.lock();
        try
        {
          _state = state;
          if (state == Event.KeeperState.SyncConnected)
          {
            _connected.signalAll();
          }
        }
        finally
        {
          _lock.unlock();
        }
      }
    }

    public void waitForConnected(long timeout, TimeUnit timeoutUnit)
            throws InterruptedException, TimeoutException
    {

      final Date deadline = new Date(System.currentTimeMillis() + timeoutUnit.toMillis(timeout));
      _lock.lock();
      try
      {
        while (_state != Event.KeeperState.SyncConnected)
        {
          if (!_connected.awaitUntil(deadline))
          {
            throw new TimeoutException("Timeout expired, state was " + _state);
          }
        }
      }
      finally
      {
        _lock.unlock();
      }
    }
  }

  public static File createTempDir(String suffix)
  {
    File parent = new File(System.getProperty("java.io.tmpdir"));
    String baseName = System.currentTimeMillis()+".";

    for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++)
    {
      File tempDir = new File(parent,baseName + counter + "." + suffix);
      if (tempDir.mkdir())
      {
        return tempDir;
      }
    }
    throw new IllegalStateException("Failed to create directory within "
        + TEMP_DIR_ATTEMPTS + " attempts (tried "
        + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
  }

  public static int getRandomPort()
  {
    ServerSocket server = null;
    try
    {
      server = new ServerSocket(0);
      return server.getLocalPort();
    }
    catch ( IOException e )
    {
      throw new Error(e);
    }
    finally
    {
      if ( server != null )
      {
        try
        {
          server.close();
        }
        catch ( IOException ignore )
        {
        }
      }
    }
  }
}

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

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TestZKPersistentConnection
{

  @Test
  public void testExpiration()
          throws IOException, InterruptedException, KeeperException, TimeoutException
  {
    ZKServer server = new ZKServer();
    server.startup();

    try
    {
      final int PORT = server.getPort();

      TestListener listener = new TestListener();
      ZKPersistentConnection c = new ZKPersistentConnection(
        "localhost:" + PORT, 15000, Collections.singleton(listener));

      long count = listener.getCount();
      c.start();

      listener.waitForEvent(count, ZKPersistentConnection.Event.SESSION_ESTABLISHED, 30, TimeUnit.SECONDS);

      final String TEST_NODE = "/testnode";
      final byte[] TEST_DATA = "Testing".getBytes();
      c.getZooKeeper().create(TEST_NODE, TEST_DATA, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

      count = listener.getCount();

      // Duplicate the connection and close it to generate an expired event
      ZKTestUtil.expireSession("localhost:" + PORT, c.getZooKeeper(), 30, TimeUnit.SECONDS);

      // assert we get syncconnected
      listener.waitForEvent(count, ZKPersistentConnection.Event.SESSION_ESTABLISHED, 30, TimeUnit.SECONDS);

      byte[] data = c.getZooKeeper().getData(TEST_NODE, false, null);

      Assert.assertEquals(data, TEST_DATA);
      c.shutdown();
    }
    finally
    {
      server.shutdown();
    }
  }


  @Test
  public void testWaitForNewSessionEstablished()
    throws IOException, InterruptedException, KeeperException, TimeoutException
  {
    ZKServer server = new ZKServer();
    server.startup();

    try
    {
      final int PORT = server.getPort();

      TestListener listener = new TestListener();
      ZKPersistentConnection c = new ZKPersistentConnection(
        "localhost:" + PORT, 15000, Collections.singleton(listener));

      long count = listener.getCount();
      c.start();

      listener.waitForEvent(count, ZKPersistentConnection.Event.SESSION_ESTABLISHED, 30, TimeUnit.SECONDS);

      // value of previous session id
      long oldSessionId = c.getZooKeeper().getSessionId();

      ZKTestUtil.expireSession("localhost:" + PORT, c.getZooKeeper(), 30, TimeUnit.SECONDS);

      ZKTestUtil.waitForNewSessionEstablished(oldSessionId, c, 5, TimeUnit.SECONDS);
      c.shutdown();
    }
    finally
    {
      server.shutdown();
    }
  }

  private static class TestListener implements ZKPersistentConnection.EventListener
  {
    private final Lock _lock = new ReentrantLock();
    private final Condition _stateChanged = _lock.newCondition();

    private ZKPersistentConnection.Event _state;
    private long _count = 0;

    @Override
    public void notifyEvent(ZKPersistentConnection.Event event)
    {
      _lock.lock();
      try
      {
        _state = event;
        _count++;
        _stateChanged.signalAll();
      }
      finally
      {
        _lock.unlock();
      }
    }

    public long getCount()
    {
      _lock.lock();
      try
      {
        return _count;
      }
      finally
      {
        _lock.unlock();
      }
    }

    public void waitForEvent(long count, ZKPersistentConnection.Event event, long timeout,
                             TimeUnit timeoutUnit)
            throws TimeoutException, InterruptedException
    {
      Date deadline = new Date(System.currentTimeMillis() + timeoutUnit.toMillis(timeout));
      _lock.lock();
      try
      {
        while (_count <= count || (event != null && _state != event))
        {
          if (!_stateChanged.awaitUntil(deadline))
          {
            throw new TimeoutException("Timed out waiting for " + event + " > " + count + "; currently " + _state + " and " + _count);
          }
        }
      }
      finally
      {
        _lock.unlock();
      }
    }
  }
}

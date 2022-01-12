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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.linkedin.pegasus.org.apache.zookeeper.CreateMode;
import com.linkedin.pegasus.org.apache.zookeeper.KeeperException;
import com.linkedin.pegasus.org.apache.zookeeper.ZooDefs;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.testng.Assert.fail;

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
        new ZKConnectionBuilder("localhost:" + PORT).setTimeout(15000));
      c.addListeners(Collections.singletonList(listener));

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
        new ZKConnectionBuilder("localhost:" + PORT).setTimeout(15000));
      c.addListeners(Collections.singletonList(listener));

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


  @Test
  public void testAddListenersBeforeStart()
    throws IOException, InterruptedException, KeeperException, TimeoutException
  {
    ZKServer server = new ZKServer();
    server.startup();

    try
    {
      final int PORT = server.getPort();

      TestListener listener = new TestListener();
      TestListener listener2 = new TestListener();
      ZKPersistentConnection c = new ZKPersistentConnection(
        new ZKConnectionBuilder("localhost:" + PORT).setTimeout(15000));
      c.addListeners(Collections.singletonList(listener));
      c.addListeners(Collections.singleton(listener2));
      long count = listener.getCount();
      long count2 = listener2.getCount();
      c.start();

      listener.waitForEvent(count, ZKPersistentConnection.Event.SESSION_ESTABLISHED, 30, TimeUnit.SECONDS);
      listener.waitForEvent(count2, ZKPersistentConnection.Event.SESSION_ESTABLISHED, 30, TimeUnit.SECONDS);

      c.shutdown();
    }
    finally
    {
      server.shutdown();
    }
  }


  @Test
  public void testFailureAddListenersAfterStart()
    throws IOException, InterruptedException, KeeperException, TimeoutException
  {
    ZKServer server = new ZKServer();
    server.startup();
    ZKPersistentConnection c = null;
    try
    {
      final int PORT = server.getPort();

      TestListener listener = new TestListener();
      TestListener listener2 = new TestListener();
      c = new ZKPersistentConnection(
        new ZKConnectionBuilder("localhost:" + PORT).setTimeout(15000));
      c.addListeners(Collections.singletonList(listener));

      c.start();
      c.addListeners(Collections.singleton(listener2));

      fail("Adding a listener after start should fail");
    }
    catch (IllegalStateException e)
    {
      // success
    }
    finally
    {
      // it should have always a value
      c.shutdown();
      server.shutdown();
    }
  }

  @Test
  public void testMultipleUsersOnSingleConnection() throws Exception {
    int port = 2120;
    int numUsers = 10;
    Random random = new Random();
    ZKServer server = new ZKServer(port);
    server.startup();
    ZKPersistentConnection c =
        new ZKPersistentConnection(new ZKConnectionBuilder("localhost:" + port).setTimeout(15000));
    ExecutorService executor = Executors.newFixedThreadPool(numUsers);
    AtomicInteger notificationCount = new AtomicInteger(0);

    for (int i = 0; i < numUsers; i++) {
      ZKPersistentConnection.EventListener listener = new ZKPersistentConnection.EventListener() {
        @Override
        public void notifyEvent(ZKPersistentConnection.Event event) {
          notificationCount.getAndIncrement();
        }
      };
      c.addListeners(Collections.singletonList(listener));
      c.incrementShareCount();
    }

    FutureCallback<None> callback = new FutureCallback<>();
    Callback<None> multiCallback = Callbacks.countDown(callback, numUsers);
    for (int i = 0; i < numUsers; i++) {
      final int userIndex = i;
      executor.submit(new Runnable() {
        @Override
        public void run() {
          try {
            // start after indeterminate delay to simulate interleaved startup and shutdown
            Thread.sleep(Math.abs(random.nextInt()) % 100);
            c.start();

            //test live connection
            c.getZooKeeper().exists("/test", false);

            c.shutdown();
            multiCallback.onSuccess(None.none());
          } catch (Exception e) {
            multiCallback.onError(e);
          }
        }
      });
    }

    callback.get(5000, TimeUnit.MILLISECONDS);
    Assert.assertTrue(notificationCount.get() == 10);
    Assert.assertTrue(c.isConnectionStopped());
    server.shutdown();
    executor.shutdown();
  }

  @Test
  public void testNormalUsercaseWithoutSharing() throws IOException, InterruptedException, KeeperException
  {
    int port = 2120;
    int numUsers = 10;
    Random random = new Random();
    ZKServer server = new ZKServer(port);
    server.startup();

    ZKConnectionBuilder builder = new ZKConnectionBuilder("localhost:" + port);
    builder.setTimeout(15000);
    ZKPersistentConnection connection = new ZKPersistentConnection(builder);

    connection.start();
    Assert.assertTrue(connection.isConnectionStarted());

    connection.getZooKeeper().exists("/test", false);

    connection.shutdown();
    Assert.assertTrue(connection.isConnectionStopped());
    server.shutdown();
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

/*
   Copyright (c) 2017 LinkedIn Corp.

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

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.discovery.event.PropertyEventBusImpl;
import com.linkedin.d2.discovery.event.PropertyEventSubscriber;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class ZooKeeperConnectionAwareStoreTest
{

  private static final String TEST_ZK_PROP_NAME = "testProp";

  protected ZKServer _zkServer;
  protected static final int PORT = 11722;

  @BeforeSuite
  public void doOneTimeSetUp() throws InterruptedException
  {
    try
    {
      _zkServer = new ZKServer(PORT);
      _zkServer.startup();
    } catch (IOException e)
    {
      fail("unable to instantiate real zk server on port " + PORT);
    }
  }

  @AfterSuite
  public void doOneTimeTearDown() throws IOException
  {
    _zkServer.shutdown();
  }

  @Test
  public void testRereadFromBusAfterExpiration()
      throws InterruptedException, IOException, PropertyStoreException, ExecutionException, TimeoutException
  {
    ZooKeeperConnectionAwareStore<String, ZooKeeperEphemeralStore<String>> store = ZkStoreTestOnlyUtil.getZKAwareStore(PORT);

    PropertyEventBusImpl<String> propertyEventBus = new PropertyEventBusImpl<>(Executors.newSingleThreadExecutor());
    store.setBusImpl(propertyEventBus);

    // the first time it is written, the this countdown will go down
    CountDownLatch initializedLatch = new CountDownLatch(1);

    // when reconnection happens, the properties will be re-registered again under the current implementation
    // and the add callback will be called
    CountDownLatch addLatch = new CountDownLatch(1);

    // we could move this three statements below registration, but then we should change the logic
    // of the Subscriber: if you register before any value is in, you'll get a null value before in onInitialize
    // and subsequently values in onAdd. Therefore moving those need a refactor of the subscriber
    ZKPersistentConnection zkPersistentConnection = ZkStoreTestOnlyUtil.getZkPersistentConnection(PORT);
    ZooKeeperEphemeralStore<String> writerStore = ZkStoreTestOnlyUtil.getZooKeeperEphemeralStore(PORT);
    writerStore.put(TEST_ZK_PROP_NAME, "randomValue");

    propertyEventBus.register(Collections.singleton(TEST_ZK_PROP_NAME), new PropertyEventSubscriber<String>()
    {
      @Override
      public void onInitialize(String propertyName, String propertyValue)
      {
        if (propertyName != null)
        {
          initializedLatch.countDown();
        }
      }

      @Override
      public void onAdd(String propertyName, String propertyValue)
      {
        if (propertyName != null)
        {
          addLatch.countDown();
        }
      }

      @Override
      public void onRemove(String propertyName)
      {
      }
    });

    initializedLatch.await(5, TimeUnit.SECONDS);
    if (initializedLatch.getCount() != 0)
    {
      fail("Initialized not received");
    }

    if (addLatch.getCount() == 0)
    {
      fail("This should have not been invoked yet");
    }

    // value of previous session id
    long oldSessionId = zkPersistentConnection.getZooKeeper().getSessionId();

    ZKTestUtil.expireSession("localhost:" + PORT, zkPersistentConnection.getZooKeeper(), 30, TimeUnit.SECONDS);
    ZKTestUtil.waitForNewSessionEstablished(oldSessionId, zkPersistentConnection, 5, TimeUnit.SECONDS);

    // when connection gets restarted, the properties are fetched again and re-registered on the bus
    addLatch.await(5, TimeUnit.SECONDS);
    if (addLatch.getCount() != 0)
    {
      fail("Reading last value after expiration not happened");
    }

    // shutting down
    final FutureCallback<None> shutdownCallback = new FutureCallback<>();
    store.shutdown(shutdownCallback);
    shutdownCallback.get(5, TimeUnit.SECONDS);
  }

  @Test
  public void testShutdown()
      throws InterruptedException, IOException, PropertyStoreException, ExecutionException, TimeoutException
  {
    ZooKeeperConnectionAwareStore<String, ZooKeeperEphemeralStore<String>> store = ZkStoreTestOnlyUtil.getZKAwareStore(PORT);

    final FutureCallback<None> callback = new FutureCallback<>();
    store.shutdown(callback);
    try
    {
      callback.get(5, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e)
    {
      fail("unable to shut down store");
    }
  }
}
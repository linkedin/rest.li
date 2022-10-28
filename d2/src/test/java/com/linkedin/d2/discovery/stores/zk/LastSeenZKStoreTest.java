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
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.testng.annotations.Test;

import static org.testng.Assert.fail;


/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class LastSeenZKStoreTest
{
  protected ZKServer _zkServer;
  protected static final int PORT = 11721;

  private static final String TEST_ZK_PROP_NAME = "testProp";

  /**
   * The test aims at
   * 1) write data in the FS store
   * 2) Shutdown the ZKServer and check if a new LastSeenZKStore will read data from disk
   * 3) Restart ZKServer and see if this LastSeenZKStore which could never access to disk will retrieve latest
   *    information from there
   */
  @Test
  public void testLastSeenLifeCycle()
      throws InterruptedException, ExecutionException, TimeoutException, IOException, PropertyStoreException
  {
    createZKServer();
    // Fill the store with data
    File dataPath = ZKTestUtil.createTempDir("randomFileDataPath");
    LastSeenZKStore<String> store = ZkStoreTestOnlyUtil.getLastSeenZKStore(dataPath.getPath(), PORT);

    ZooKeeperEphemeralStore<String> storeWriter = ZkStoreTestOnlyUtil.getZooKeeperEphemeralStore(PORT);
    storeWriter.put(TEST_ZK_PROP_NAME, "randomData");

    PropertyEventBusImpl<String>  propertyEventBus = new PropertyEventBusImpl<>(Executors.newSingleThreadExecutor());
    propertyEventBus.setPublisher(store);

    CountDownLatch initializedLatch = new CountDownLatch(1);
    propertyEventBus.register(Collections.singleton(TEST_ZK_PROP_NAME), new LatchSubscriber(initializedLatch, null));
    initializedLatch.await(5, TimeUnit.SECONDS);
    if (initializedLatch.getCount() != 0)
    {
      fail("Initialized not received");
    }

    // stopping ZK without removing data. This make ZK unreachable

    _zkServer.shutdown(false);

    // create new last seen, without ZK Connection, and see if it fetches from the server

    store = ZkStoreTestOnlyUtil.getLastSeenZKStore(dataPath.getPath(), PORT);

    propertyEventBus = new PropertyEventBusImpl<>(Executors.newSingleThreadExecutor());
    propertyEventBus.setPublisher(store);

    CountDownLatch initializedLatch2 = new CountDownLatch(1);
    CountDownLatch addLatch2 = new CountDownLatch(1);
    propertyEventBus.register(Collections.singleton(TEST_ZK_PROP_NAME),
        new LatchSubscriber(initializedLatch2, addLatch2));

    initializedLatch2.await(5, TimeUnit.SECONDS);
    if (initializedLatch2.getCount() != 0)
    {
      fail("Initialized not received");
    }

    if (addLatch2.getCount() != 1)
    {
      fail("The add latch should have not been invoked yet");
    }

    // restart ZK and see if it reads the most updated value, the most updated value in this case is identical
    _zkServer.restart();

    addLatch2.await(50, TimeUnit.SECONDS);
    if (addLatch2.getCount() != 0)
    {
      fail("When ZK restarted we didn't read the most updated value from ZK");
    }

    // shutting everything down
    final FutureCallback<None> shutdownCallback = new FutureCallback<>();
    store.shutdown(shutdownCallback);
    shutdownCallback.get(5, TimeUnit.SECONDS);

    final FutureCallback<None> shutdownCallback2 = new FutureCallback<>();
    storeWriter.shutdown(shutdownCallback2);
    shutdownCallback2.get(5, TimeUnit.SECONDS);

    _zkServer.shutdown();
  }

  // ####################### ZK server #######################

  private class LatchSubscriber implements PropertyEventSubscriber<String>
  {
    private CountDownLatch _initializedLatch;
    private CountDownLatch _addLatch;

    private LatchSubscriber(CountDownLatch initializedLatch, CountDownLatch addLatch)
    {
      _initializedLatch = initializedLatch;
      _addLatch = addLatch;
    }

    @Override
    public void onInitialize(String propertyName, String propertyValue)
    {
      if (propertyValue != null && _initializedLatch != null)
      {
        _initializedLatch.countDown();
      }
    }

    @Override
    public void onAdd(String propertyName, String propertyValue)
    {
      if (propertyValue != null && _addLatch != null)
      {
        _addLatch.countDown();
      }
    }

    @Override
    public void onRemove(String propertyName)
    {
    }
  }

  public void createZKServer() throws InterruptedException
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
}
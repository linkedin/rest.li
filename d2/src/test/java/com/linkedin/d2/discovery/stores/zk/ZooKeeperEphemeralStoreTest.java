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

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.PropertyStringMerger;
import com.linkedin.d2.discovery.stores.PropertyStringSerializer;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class ZooKeeperEphemeralStoreTest
{
  protected ZKServer _zkServer;
  protected File     _dataPath;
  protected File     _logPath;
  protected int      _port;
  private final AtomicReference<String> _clusterInCallback = new AtomicReference<>();
  private final AtomicReference<String> _nodePathInCallback = new AtomicReference<>();
  private final AtomicReference<String> _dataInCallback = new AtomicReference<>();

  @BeforeSuite
  public void doOneTimeSetUp() throws InterruptedException
  {
    _port = 11720;

    try
    {
      _zkServer = new ZKServer(_port);
      _zkServer.startup();
    }
    catch (IOException e)
    {
      fail("unable to instantiate real zk server on port " + _port);
    }
  }

  @AfterSuite
  public void doOneTimeTearDown() throws IOException
  {
    _zkServer.shutdown();
  }

  public static void main(String[] args) throws Exception
  {
    ZooKeeperEphemeralStoreTest test = new ZooKeeperEphemeralStoreTest();

    test.doOneTimeSetUp();
    test.testPutGetRemovePartial();
    test.doOneTimeTearDown();
  }

  public ZooKeeperEphemeralStore<String> getStore()
          throws IOException, PropertyStoreException, InterruptedException, ExecutionException
  {
    ZKConnection client = new ZKConnection("localhost:" + _port, 5000);
    client.start();


    ZooKeeperEphemeralStore<String> store = new ZooKeeperEphemeralStore<>(
            client,
            new PropertyStringSerializer(),
            new PropertyStringMerger(),
            "/test-path",
            false,
            true);
    FutureCallback<None> callback = new FutureCallback<>();
    store.start(callback);
    callback.get();
    return store;
  }

  @Test(groups = { "small", "back-end" })
  public void testPutGetRemovePartial()
          throws InterruptedException, IOException, PropertyStoreException, ExecutionException
  {
    ZooKeeperEphemeralStore<String> store = getStore();
    store.setZnodePathAndDataCallback(((cluster, nodePath, data) -> {
      _clusterInCallback.set(cluster);
      _nodePathInCallback.set(nodePath);
      _dataInCallback.set(data);
    }));

    String service_1 = "service-1";
    String path_1 = "/test-path/" + service_1 + "/ephemoral-0000000000";
    String data_1 = "1";
    String path_2 = "/test-path/" + service_1 + "/ephemoral-0000000001";
    String data_2 = "2";

    String service_2 = "service-2";
    String data_3 = "3";
    String path_3 = "/test-path/" + service_2 + "/ephemoral-0000000000";

    store.put(service_1, data_1);
    verifyClusterPathAndDataInCallback(service_1, path_1, data_1);
    store.put(service_1, data_2);
    verifyClusterPathAndDataInCallback(service_1, path_2, data_2);
    store.put(service_2, data_3);
    verifyClusterPathAndDataInCallback(service_2, path_3, data_3);

    assertTrue(store.get(service_1).equals("1,2")
        || store.get(service_1).equals("2,1"));
    assertEquals(store.get(service_2), data_3);
    assertNull(store.get("service-3"));

    store.removePartial(service_1, data_2);
    assertEquals(store.get(service_1), data_1);

    store.remove(service_2);
    assertNull(store.get(service_2));

    final FutureCallback<None> callback = new FutureCallback<>();
    store.shutdown(callback);
    try
    {
      callback.get(5, TimeUnit.SECONDS);
    }
    catch (InterruptedException | ExecutionException | TimeoutException e)
    {
      fail("unable to shut down store");
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testShutdown()
          throws InterruptedException, IOException, PropertyStoreException, ExecutionException
  {
    PropertyStore<String> store = getStore();

    final FutureCallback<None> callback = new FutureCallback<>();
    store.shutdown(callback);
    try
    {
      callback.get(5, TimeUnit.SECONDS);
    }
    catch (InterruptedException | ExecutionException | TimeoutException e)
    {
      fail("unable to shut down store");
    }
  }

  private void verifyClusterPathAndDataInCallback(String cluster, String path, String data) {
    assertEquals(_clusterInCallback.get(), cluster);
    assertEquals(_nodePathInCallback.get(), path);
    assertEquals(_dataInCallback.get(), data);
  }
}

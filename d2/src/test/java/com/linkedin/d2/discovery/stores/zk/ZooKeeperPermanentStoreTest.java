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

import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.PropertyStoreTest;
import com.linkedin.d2.discovery.stores.PropertyStringSerializer;

import java.io.IOException;

public class ZooKeeperPermanentStoreTest extends PropertyStoreTest
{
  protected static final int PORT = 5000;

  protected ZKServer _zkServer;

  @BeforeMethod
  public void setupServer() throws IOException, InterruptedException
  {
    _zkServer = new ZKServer(PORT);
    _zkServer.startup();
  }

  @AfterMethod
  public void cleanupServer() throws IOException
  {
    _zkServer.shutdown();
  }

  @Override
  public PropertyStore<String> getStore() throws PropertyStoreException
  {
    try
    {
      ZKConnection client = new ZKConnection("localhost:" + PORT, 30000);
      client.start();

      ZooKeeperPermanentStore<String> store = new ZooKeeperPermanentStore<String>(
              client,
              new PropertyStringSerializer(),
              "/test-path");
      FutureCallback<None> callback = new FutureCallback<None>();
      store.start(callback);
      callback.get();
      return store;
    }
    catch (Exception e)
    {
      throw new PropertyStoreException(e);
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testEmpty()
  {
  }
}

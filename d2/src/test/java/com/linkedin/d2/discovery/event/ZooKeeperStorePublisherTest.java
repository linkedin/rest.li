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

package com.linkedin.d2.discovery.event;

import com.linkedin.d2.discovery.stores.zk.ZKConnection;
import com.linkedin.d2.discovery.stores.zk.ZKServer;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperStore;
import com.linkedin.pegasus.org.apache.zookeeper.Watcher;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.fail;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public abstract class ZooKeeperStorePublisherTest extends PublisherTest
{
  private final int PORT = 11725;
  private final String CONNECT = "localhost:" + PORT;
  private final int TIMEOUT = 30;

  private ZKServer _zkServer;

  @Override
  protected PropertyEventPublisher<String> getPublisher()
  {
    return getStore();
  }

  @Override
  protected abstract ZooKeeperStore<String> getStore();

  @BeforeMethod
  public void doSetup() throws InterruptedException
  {
    try
    {
      _zkServer = new ZKServer(PORT);
      _zkServer.startup();
    }
    catch (IOException e)
    {
      fail("unable to instantiate real zk server on port " + PORT);
    }
  }

  @AfterMethod
  public void doTeardown() throws IOException
  {
    _zkServer.shutdown();
  }

  protected ZKConnection getConnection()
  {
    ZKConnection conn = new ZKConnection(CONNECT, TIMEOUT * 1000 /* in milliseconds */);
    try
    {
      conn.start();
      conn.waitForState(Watcher.Event.KeeperState.SyncConnected, TIMEOUT, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
    return conn;
  }

}

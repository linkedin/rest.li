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

import com.linkedin.d2.discovery.stores.PropertyStringSerializer;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPermanentStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperStore;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ZooKeeperPermanentStorePublisherTest extends ZooKeeperStorePublisherTest
{
  @Override
  protected ZooKeeperStore<String> getStore()
  {
    ZooKeeperPermanentStore<String> store = new ZooKeeperPermanentStore<String>(
            getConnection(), new PropertyStringSerializer(), "/testing/testPath");
    try
    {
      FutureCallback<None> callback = new FutureCallback<None>();
      store.start(callback);
      callback.get(30, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      throw new RuntimeException(e);
    }
    return store;
  }

  @Test
  public void testNothing()
  {
    // Get TestNG to notice us
  }
}

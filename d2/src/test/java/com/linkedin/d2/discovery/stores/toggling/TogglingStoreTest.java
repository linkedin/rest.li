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

package com.linkedin.d2.discovery.stores.toggling;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEventShutdownCallback;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.PropertyStoreTest;
import com.linkedin.d2.discovery.stores.mock.MockStore;

public class TogglingStoreTest extends PropertyStoreTest
{
  @Override
  public TogglingStore<String> getStore()
  {
    return new TogglingStore<String>(new MockStore<String>());
  }

  @Test(groups = { "small", "back-end" })
  public void testPutGetDisabled() throws PropertyStoreException
  {
    TogglingStore<String> store = getStore();

    store.setEnabled(false);

    assertNull(store.get("test"));

    store.put("test", "exists");

    assertNull(store.get("test"));
  }

  @Test(groups = { "small", "back-end" })
  public void testPutRemoveDisabled() throws PropertyStoreException
  {
    TogglingStore<String> store = getStore();

    store.setEnabled(false);

    assertNull(store.get("test"));

    store.put("test", "exists");

    assertNull(store.get("test"));

    store.remove("test");
    store.remove("empty");

    assertNull(store.get("test"));
    assertNull(store.get("empty"));
  }

  @Test(groups = { "small", "back-end" })
  public void testShutdownDisabled() throws InterruptedException
  {
    TogglingStore<String> store = getStore();

    store.setEnabled(false);

    final CountDownLatch latch = new CountDownLatch(1);

    store.shutdown(new PropertyEventShutdownCallback()
    {
      @Override
      public void done()
      {
        latch.countDown();
      }
    });

    if (!latch.await(5, TimeUnit.SECONDS))
    {
      fail("unable to shut down store");
    }
  }
}

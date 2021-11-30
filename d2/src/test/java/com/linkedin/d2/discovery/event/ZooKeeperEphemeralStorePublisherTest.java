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

import com.linkedin.d2.discovery.stores.PropertyStore;
import com.linkedin.d2.discovery.stores.PropertyStoreException;
import com.linkedin.d2.discovery.stores.PropertyStringSerializer;
import com.linkedin.d2.discovery.stores.PropertyStringMerger;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperEphemeralStore;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperPropertyMerger;
import com.linkedin.d2.discovery.stores.zk.ZooKeeperStore;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class ZooKeeperEphemeralStorePublisherTest extends ZooKeeperStorePublisherTest
{
  private final ZooKeeperPropertyMerger<String> _merger = new PropertyStringMerger();
  @Override
  protected ZooKeeperStore<String> getStore()
  {
    ZooKeeperEphemeralStore<String> store = new ZooKeeperEphemeralStore<>(
            getConnection(), new PropertyStringSerializer(),
            _merger, "/testing/testPath",
            false, true);
    try
    {
      FutureCallback<None> callback = new FutureCallback<>();
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

  @Test
  public void testMultiNewProperties() throws PropertyStoreException, TimeoutException, InterruptedException
  {
    final String KEY = "someKey";
    final String VALUE_1 = "someValue1";
    final String VALUE_2 = "someValue2";
    final String VALUE_3 = "someValue3";

    PropertyEventPublisher<String> pub = getPublisher();

    MockBusSink bus = new MockBusSink();

    pub.setBus(bus);

    // Publisher should publish an initial null
    pub.startPublishing(KEY);
    bus.awaitInit(KEY, null, BUS_UPDATE_TIMEOUT, TimeUnit.SECONDS);

    // After updating, publisher should publish the new value
    PropertyStore<String> store = getStore();
    store.put(KEY, VALUE_1);
    store.put(KEY, VALUE_2);
    bus.awaitAdd(KEY, _merger.merge(KEY, Arrays.asList(VALUE_1, VALUE_2)), BUS_UPDATE_TIMEOUT, TimeUnit.SECONDS);

    store.put(KEY, VALUE_3);
    bus.awaitAdd(KEY, _merger.merge(KEY, Arrays.asList(VALUE_1, VALUE_2, VALUE_3)), BUS_UPDATE_TIMEOUT, TimeUnit.SECONDS);
  }

  @Test
  public void testMultiExistingProperties()
      throws PropertyStoreException, TimeoutException, InterruptedException
  {
    final String KEY = "someKey";
    final String VALUE_1 = "someValue1";
    final String VALUE_2 = "someValue2";
    final String VALUE_3 = "someValue3";

    PropertyStore<String> store = getStore();
    store.put(KEY, VALUE_1);
    store.put(KEY, VALUE_2);
    store.put(KEY, VALUE_3);
    Assert.assertEquals(store.get(KEY), _merger.merge(KEY, Arrays.asList(VALUE_1, VALUE_2, VALUE_3)));

    MockBusSink bus = new MockBusSink();

    PropertyEventPublisher<String> pub = getPublisher();
    pub.setBus(bus);

    pub.startPublishing(KEY);
    bus.awaitInit(KEY, _merger.merge(KEY, Arrays.asList(VALUE_1, VALUE_2, VALUE_3)), BUS_UPDATE_TIMEOUT, TimeUnit.SECONDS);

    ((ZooKeeperEphemeralStore<String>)store).removePartial(KEY, VALUE_2);
    bus.awaitAdd(KEY, _merger.merge(KEY, Arrays.asList(VALUE_1, VALUE_3)), BUS_UPDATE_TIMEOUT, TimeUnit.SECONDS);

    store.remove(KEY);
    bus.awaitRemove(KEY, BUS_UPDATE_TIMEOUT, TimeUnit.SECONDS);
  }

  @Test
  public void testPublishNullProperty()
      throws TimeoutException, InterruptedException, PropertyStoreException {
    final String KEY = "someKey";
    final String VALUE = "someValue";

    PropertyStore<String> store = getStore();
    MockBusSink bus = new MockBusSink();
    PropertyEventPublisher<String> pub = getPublisher();
    pub.setBus(bus);

    pub.startPublishing(KEY);
    bus.awaitInit(KEY, null, BUS_UPDATE_TIMEOUT, TimeUnit.SECONDS);

    store.put(KEY, VALUE);
    bus.awaitAdd(KEY, VALUE, BUS_UPDATE_TIMEOUT, TimeUnit.SECONDS);

    ((ZooKeeperEphemeralStore<String>)store).removePartial(KEY, VALUE);
    bus.awaitAdd(KEY, null, BUS_UPDATE_TIMEOUT, TimeUnit.SECONDS);

  }
}

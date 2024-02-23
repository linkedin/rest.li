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

package com.linkedin.d2.discovery.event;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.linkedin.d2.discovery.stores.mock.MockStore;
import com.linkedin.d2.discovery.stores.util.StoreEventPublisher;
import org.testng.annotations.Test;

import com.linkedin.d2.discovery.event.PropertyEventBus;

public abstract class PropertyEventBusTest
{
  public abstract PropertyEventBus<String> getBus();

  public abstract void put(PropertyEventBus<String> registry,
                           String listenTo,
                           String discoveryProperties);

  public abstract void remove(PropertyEventBus<String> registry, String listenTo);

  @Test(groups = { "small", "back-end" })
  public void testRegister() throws InterruptedException
  {
    PropertyEventBus<String> bus = getBus();
    PropertyEventTestSubscriber listener = new PropertyEventTestSubscriber();
    Set<String> listenTos = new HashSet<>();

    listenTos.add("test");
    listenTos.add("test2");

    // test init
    put(bus, "test", "exists");

    bus.register(listenTos, listener);

    assertEquals(listener.properties.get("init-test"), "exists");
    assertNull(listener.properties.get("init-test2"));

    // test remove
    remove(bus, "test");

    // wait for the listener to get the response, in case this registry is async
    for (int i = 0; i < 5 && listener.properties.get("init-test") != null; ++i)
    {
      Thread.sleep(500);
    }

    assertNull(listener.properties.get("init-test"));

    // test add
    put(bus, "test2", "exists");

    // wait for the listener to get the response, in case this registry is async
    for (int i = 0; i < 5 && listener.properties.get("add-test2") == null; ++i)
    {
      Thread.sleep(500);
    }

    assertEquals(listener.properties.get("add-test2"), "exists");

    remove(bus, "test2");
  }

  @Test(groups = { "small", "back-end" })
  public void testUnregister() throws InterruptedException
  {
    PropertyEventBus<String> bus = getBus();
    PropertyEventTestSubscriber listener = new PropertyEventTestSubscriber();
    Set<String> listenTos = new HashSet<>();

    listenTos.add("test");
    listenTos.add("test2");

    put(bus, "test", "exists");

    bus.register(listenTos, listener);

    assertEquals(listener.properties.get("init-test"), "exists");
    assertNull(listener.properties.get("init-test2"));

    bus.unregister(listenTos, listener);

    // do some stuff
    remove(bus, "test");
    put(bus, "test2", "exists");

    // wait for the listener to get the response, in case this registry is async
    for (int i = 0; i < 10
        && (listener.properties.get("init-test2") == null || listener.properties.get("init-test2") != null); ++i)
    {
      Thread.sleep(500);
    }

    // verify that the listener didn't get the events
    assertEquals(listener.properties.get("init-test"), "exists");
    assertNull(listener.properties.get("init-test2"));
  }

  @Test(groups = { "small", "back-end" })
  public void testDoubleUnregister() throws InterruptedException
  {
    PropertyEventBus<String> bus = getBus();
    PropertyEventTestSubscriber listener1 = new PropertyEventTestSubscriber();
    PropertyEventTestSubscriber listener2 = new PropertyEventTestSubscriber();
    Set<String> listenTos = new HashSet<>();

    listenTos.add("dtest");
    listenTos.add("dtest2");

    put(bus, "dtest", "exists");

    bus.register(listenTos, listener1);
    bus.register(listenTos, listener2);

    assertEquals(listener1.properties.get("init-dtest"), "exists");
    assertNull(listener1.properties.get("init-dtest2"));
    assertEquals(listener2.properties.get("init-dtest"), "exists");
    assertNull(listener2.properties.get("init-dtest2"));

    bus.unregister(listenTos, listener1);

    // do some stuff
    remove(bus, "dtest");
    put(bus, "dtest2", "exists");

    // wait for the listener to get the response, in case this registry is async
    for (int i = 0; i < 10
        && (listener1.properties.get("init-dtest2") != null
            || listener1.properties.get("init-dtest") == null
            || listener2.properties.get("add-dtest2") == null
            || listener2.properties.get("add-dtest") != null || listener2.properties.get("init-dtest") != null); ++i)
    {
      Thread.sleep(500);
    }

    // verify that listener1 didn't get the events
    assertEquals(listener1.properties.get("init-dtest"), "exists");
    assertNull(listener1.properties.get("init-dtest2"));

    // but listener2 did
    assertNull(listener2.properties.get("init-dtest"));
    assertNull(listener2.properties.get("add-dtest"));
    assertEquals(listener2.properties.get("add-dtest2"), "exists");

    bus.unregister(listenTos, listener2);

    // now check that unregistering all works properly
    put(bus, "dtest", "exists");
    remove(bus, "dtest2");

    // wait for the listener to get the response, in case this registry is async
    for (int i = 0; i < 10
        && (listener1.properties.get("init-dtest2") != null
            || listener1.properties.get("init-dtest") == null
            || listener2.properties.get("add-dtest2") == null
            || listener2.properties.get("add-dtest") != null || listener2.properties.get("init-dtest") != null); ++i)
    {
      Thread.sleep(500);
    }

    // verify that nothing changed
    assertEquals(listener1.properties.get("init-dtest"), "exists");
    assertNull(listener1.properties.get("init-dtest2"));
    assertNull(listener2.properties.get("init-dtest"));
    assertNull(listener2.properties.get("add-dtest"));
    assertEquals(listener2.properties.get("add-dtest2"), "exists");
  }

  @Test(groups = { "small", "back-end" })
  public void testUnregisterRemovesSingleListener() throws InterruptedException
  {
    PropertyEventBus<String> bus = getBus();
    PropertyEventTestSubscriber listener1 = new PropertyEventTestSubscriber();

    put(bus, "dtest", "exists");

    Set<String> listenTos = new HashSet<>();

    listenTos.add("dtest");
    bus.register(listenTos, listener1);
    bus.register(listenTos, listener1);

    assertEquals(listener1.properties.get("init-dtest"), "exists");

    // Unregister once. We should still have a listener listening to changes
    bus.unregister(listenTos, listener1);
    put(bus, "dtest", "new-value");

    // wait for the listener to get the response, in case this registry is async
    for (int i = 0; i < 10 && listener1.properties.get("init-dtest") == null; ++i)
    {
      Thread.sleep(500);
    }

    // Verify property change is observed
    assertEquals(listener1.properties.get("add-dtest"), "new-value");

    // Unregister the second listener. listener1 should no longer be getting updates
    bus.unregister(listenTos, listener1);

    // Register listener2 to make sure updates are being propagated.
    PropertyEventTestSubscriber listener2 = new PropertyEventTestSubscriber();
    bus.register(listenTos, listener2);

    put(bus, "dtest", "latest-value");

    // wait for the listener to get the response, in case this registry is async
    for (int i = 0; i < 10 && listener2.properties.get("add-dtest") == null; ++i)
    {
      Thread.sleep(500);
    }

    assertEquals(listener1.properties.get("add-dtest"), "new-value");
    assertEquals(listener2.properties.get("add-dtest"), "latest-value");
  }

  @Test
  public void testMaintainRegistration()
  {
    PropertyEventBus<String> bus = getBus();

    PropertyEventTestSubscriber subscriber = new PropertyEventTestSubscriber();

    final String TEST_PROP = "testProp";
    final String TEST_VALUE = "testValue";

    bus.register(Collections.singleton(TEST_PROP), subscriber);

    // Should have received init with a null value, because initial publisher has no data

    assertEquals(subscriber.properties.get("init-" + TEST_PROP), null);
    assertTrue(subscriber.properties.containsKey("init-" + TEST_PROP));

    // Now, switch to a new publisher that does have a value for this property; the
    // subscription should be maintained, and clients should receive an update.
    MockStore<String> newStore = new MockStore<>();
    newStore.put(TEST_PROP, TEST_VALUE);

    bus.setPublisher(new StoreEventPublisher<>(newStore));

    // Now, should have received an update with the new value.
    assertEquals(subscriber.properties.get("add-" + TEST_PROP), TEST_VALUE);

  }
}

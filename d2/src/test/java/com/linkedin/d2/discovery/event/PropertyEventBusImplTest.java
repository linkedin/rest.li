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

import com.linkedin.d2.discovery.stores.mock.MockStore;


import java.util.concurrent.ScheduledExecutorService;
import org.testng.annotations.Test;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class PropertyEventBusImplTest extends PropertyEventBusTest
{
  @SuppressWarnings("unchecked")
  @Override
  public PropertyEventBus<String> getBus()
  {
    // TODO rewrite tests in the parent class so they work with either sync or async, and
    // test both modes of operation.
    ScheduledExecutorService executorService = new SynchronousExecutorService();
    PropertyEventPublisher<String> publisher = new MockStore<String>();
    PropertyEventBus<String> bus = new PropertyEventBusImpl<String>(executorService, publisher);
    return bus;
  }

  @Override
  public void put(PropertyEventBus<String> registry,
                  String listenTo,
                  String discoveryProperties)
  {
    getStore(registry).put(listenTo, discoveryProperties);
  }

  @Override
  public void remove(PropertyEventBus<String> registry, String listenTo)
  {
    getStore(registry).remove(listenTo);
  }

  private MockStore<String> getStore(PropertyEventBus<String> registry)
  {
    PropertyEventBusImpl<String> bus = (PropertyEventBusImpl<String>) registry;
    return (MockStore<String>) bus.getPublisher();
  }

  @Test
  public void testNothing()
  {
    // this gets Gradle/TestNG to notice this class and run the tests in the superclass
  }
}

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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.testng.annotations.Test;

import com.linkedin.d2.discovery.event.PropertyEventThread;
import com.linkedin.d2.discovery.event.PropertyEventThread.PropertyEvent;

public class PropertyEventThreadTest
{
  @Test(groups = { "small", "back-end" })
  public void testUnstartedThread()
  {
    PropertyEventThread thread = new PropertyEventThread("test", 1001);

    assertEquals(thread.getRemainingCapacity(), 1001);
    assertEquals(thread.getQueuedMessageCount(), 0);
    assertFalse(thread.isAlive());
    assertTrue(thread.isDaemon());

    //assertFalse(thread.send(null));
    //assertFalse(thread.send(new PropertyTestEvent("donothing")));
  }

  @Test(groups = { "small", "back-end" })
  public void testThread() throws InterruptedException
  {
    PropertyEventThread thread = new PropertyEventThread("test");
    PropertyTestEvent testEvent = new PropertyTestEvent("counter");

    // Test doesn't make sense with hacked PropertyEventThread
    //assertFalse(thread.send(testEvent));

    thread.start();

    assertTrue(thread.send(testEvent));
    assertTrue(thread.send(testEvent));

    thread.interrupt();
    thread.join(0);

    // Also doesn't make sense with hack
    //assertFalse(thread.send(testEvent));
    assertEquals(testEvent.getCount(), 2);
  }

  public class PropertyTestEvent extends PropertyEvent
  {
    private final AtomicLong _count;

    public PropertyTestEvent(String d)
    {
      super(d);

      _count = new AtomicLong(0);
    }

    @Override
    public void innerRun()
    {
      _count.incrementAndGet();
    }

    public long getCount()
    {
      return _count.get();
    }
  }
}

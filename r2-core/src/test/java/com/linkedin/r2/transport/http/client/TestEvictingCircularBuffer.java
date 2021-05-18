/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SettableClock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.IntStream;
import org.junit.Assert;
import org.testng.annotations.Test;


public class TestEvictingCircularBuffer
{
  private static final int TEST_TIMEOUT = 3000;
  private static final int TEST_CAPACITY = 5;
  private static final int TEST_TTL = 5;
  private static final ChronoUnit TEST_TTL_UNIT = ChronoUnit.SECONDS;
  private static final SettableClock TEST_CLOCK = new SettableClock();

  @Test
  public void testGettersAfterInstantiateSimple()
  {
    EvictingCircularBuffer buffer = new EvictingCircularBuffer(TEST_CAPACITY, TEST_TTL, TEST_TTL_UNIT);
    Assert.assertEquals(buffer.getCapacity(), TEST_CAPACITY);
    Assert.assertEquals(buffer.getTtl().getSeconds(), TEST_TTL);
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testCreatePutGetRepeatInOrder()
  {
    Callback<None> callback = new FutureCallback<>();
    Callback<None> callbackAlso = new FutureCallback<>();
    EvictingCircularBuffer buffer = getBuffer();
    buffer.put(callback);
    Assert.assertSame(buffer.get(), callback);
    Assert.assertSame(buffer.get(), callback);
    buffer.put(callbackAlso);
    Assert.assertSame(buffer.get(), callbackAlso);
    Assert.assertSame(buffer.get(), callback);
    Assert.assertSame(buffer.get(), callbackAlso);
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testTtlPurge()
  {
    Callback<None> callback = new FutureCallback<>();
    EvictingCircularBuffer buffer = getBuffer();
    buffer.put(callback);
    Assert.assertSame(buffer.get(), callback);
    TEST_CLOCK.addDuration(5001);
    try
    {
      buffer.get();
    }
    catch (NoSuchElementException ex)
    {
      // get
    }
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testParallelPutGet()
  {
    CyclicBarrier floodgate = new CyclicBarrier(9);
    Callback<None> callback = new FutureCallback<>();
    EvictingCircularBuffer buffer = getBuffer();

    buffer.put(callback);

    for (int i = 0; i < 4; i++)
    {
      new Thread(() -> {
        try
        {
          floodgate.await();
        }
        catch (InterruptedException | BrokenBarrierException ignored) {}
        buffer.put(new FutureCallback<>());
      }).start();
    }

    for (int i = 0; i < 5; i++)
    {
      new Thread(() -> {
        try
        {
          floodgate.await();
        }
        catch (InterruptedException | BrokenBarrierException ignored) {}
        buffer.get();
      }).start();
    }

    ArrayList<Callback<None>> results = new ArrayList<>();
    IntStream.range(0, 5).forEach(x -> results.add(buffer.get()));
    Assert.assertTrue(results.contains(callback));
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testSetCapacityAfterCreate()
  {
    EvictingCircularBuffer buffer = getBuffer();
    buffer.put(new FutureCallback<>());
    buffer.setCapacity(9001);
    try
    {
      buffer.get();
    }
    catch (NoSuchElementException ex)
    {
      // buffer clears after resize by design
    }
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testSetTtlAfterCreate()
  {
    EvictingCircularBuffer buffer = getBuffer();
    Callback<None> callback = new FutureCallback<>();
    buffer.put(callback);
    buffer.setTtl(9001, ChronoUnit.MILLIS);
    TEST_CLOCK.addDuration(8000);
    Assert.assertSame(buffer.get(), callback);
    TEST_CLOCK.addDuration(1002);
    try
    {
      buffer.get();
    }
    catch (NoSuchElementException ex)
    {
      // expired ttl
    }
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testIllegalTtlAndCapacityArguments()
  {
    EvictingCircularBuffer buffer = getBuffer();

    try
    {
      buffer.setTtl(0, TEST_TTL_UNIT);
    }
    catch (IllegalArgumentException ex)
    {
      // TTL can't be less than 1.
    }

    try
    {
      buffer.setTtl(1, null);
    }
    catch (IllegalArgumentException ex)
    {
      // TTL unit can't be null
    }

    try
    {
      buffer.setCapacity(0);
    }
    catch (IllegalArgumentException ex)
    {
      // we can always do puts on EvictingCircularBuffer, so capacity should never be less than 1.
    }
  }

  public static EvictingCircularBuffer getBuffer()
  {
    return getBuffer(TEST_CLOCK);
  }

  public static EvictingCircularBuffer getBuffer(Clock clock)
  {
    return new EvictingCircularBuffer(TEST_CAPACITY, TEST_TTL, TEST_TTL_UNIT, clock);
  }
}

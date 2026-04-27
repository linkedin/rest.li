/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client.ratelimiter;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.MultiCallback;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.AsyncRateLimiter;
import com.linkedin.r2.transport.http.client.SmoothRateLimiter;
import com.linkedin.test.util.ClockedExecutor;
import com.linkedin.util.clock.Clock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class TestSmoothRateLimiter extends BaseTestSmoothRateLimiter
{
  private static final String RATE_LIMITER_NAME_TEST = "test";

  @Test(timeOut = TEST_TIMEOUT)
  public void testUnlimitedBurstRate()
  {
    Rate rate = new Rate(3, 1000, UNLIMITED_BURST);
    assertEquals(rate.getEvents(), 3);
    assertEquals(rate.getPeriod(), 1000);
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testLowBurstRate()
  {
    Rate rate = new Rate(3, 1000, 1);
    assertEquals(rate.getEvents(), 1);
    assertEquals(rate.getPeriod(), 333);
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testBurstRateInOneMillisecond()
  {
    // this should be now supported
    // it should just ending up generating 5 events every ms. If we are refreshing every ms,
    // we cannot really say that it is `bursting`
    new Rate(50, 10, 1);
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testSubmitExceedsMaxBuffered()
  {
    SmoothRateLimiter rateLimiter =
      new SmoothRateLimiter(_scheduledExecutorService, _executor, _clock, _queue, 0, SmoothRateLimiter.BufferOverflowMode.DROP,
                            RATE_LIMITER_NAME_TEST);
    rateLimiter.setRate(ONE_PERMIT_PER_PERIOD, ONE_SECOND_PERIOD, UNLIMITED_BURST);

    FutureCallback<None> callback = new FutureCallback<>();
    try
    {
      rateLimiter.submit(callback);
    }
    catch (RejectedExecutionException e)
    {
      Assert.assertFalse("The tasks should have been rejected and not run", callback.isDone());
      // success, the exception has been thrown as expected!
      return;
    }
    Assert.fail("It should have thrown a RejectedExecutionException");
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testSubmitExceedsMaxBufferedButNoReject()
    throws InterruptedException, ExecutionException, TimeoutException
  {
    SmoothRateLimiter rateLimiter =
      new SmoothRateLimiter(_scheduledExecutorService, _executor, _clock, _queue, 0, SmoothRateLimiter.BufferOverflowMode.SCHEDULE_WITH_WARNING,
                            RATE_LIMITER_NAME_TEST);
    rateLimiter.setRate(ONE_PERMIT_PER_PERIOD, ONE_SECOND_PERIOD, UNLIMITED_BURST);

    int numberOfTasks = 100;

    FutureCallback<None> callback = new FutureCallback<>();

    Callback<None> callbacks = new MultiCallback(callback, numberOfTasks);

    for (int i = 0; i < numberOfTasks; i++)
    {
      try
      {
        rateLimiter.submit(callbacks);
      }
      catch (RejectedExecutionException e)
      {
        Assert.fail("It should have just run a task and not throw a RejectedExecutionException");
      }
    }
    callback.get(5, TimeUnit.SECONDS);
    Assert.assertTrue("The tasks should run", callback.isDone());
  }

  /**
   * Verifies that nanosecond period tracking produces accurate dispatch rates for fractional
   * millisecond periods.
   *
   * <p>At 750 QPS with burst=1, the Rate constructor computes an internal period of
   * 1000/750 = 1.333ms. Previously, {@link Rate#getPeriod()} rounded this to 1ms, causing the
   * event loop to dispatch at ~1000 QPS (+33% error). With nanosecond tracking via
   * {@link Rate#getPeriodNanos()}, the period is represented as 1,333,333ns and the dispatch
   * rate matches the target.</p>
   */
  @Test(timeOut = 10000)
  public void testFractionalPeriodAccuracy()
  {
    ClockedExecutor executor = new ClockedExecutor();
    SmoothRateLimiter rateLimiter = new SmoothRateLimiter(
        executor, executor, executor, _queue, Integer.MAX_VALUE,
        SmoothRateLimiter.BufferOverflowMode.DROP, RATE_LIMITER_NAME_TEST);

    int targetQps = 750;
    int burst = 1;
    rateLimiter.setRate(targetQps, ONE_SECOND_PERIOD, burst);

    AtomicInteger dispatchCount = new AtomicInteger(0);

    for (int i = 0; i < targetQps * 4; i++)
    {
      rateLimiter.submit(new Callback<None>()
      {
        @Override
        public void onError(Throwable e) { }

        @Override
        public void onSuccess(None result)
        {
          dispatchCount.incrementAndGet();
        }
      });
    }

    int durationSeconds = 3;
    executor.runFor(ONE_SECOND_PERIOD * durationSeconds);

    int totalDispatches = dispatchCount.get();
    int expectedTotal = targetQps * durationSeconds;
    double errorPercent = 100.0 * Math.abs(totalDispatches - expectedTotal) / expectedTotal;

    // Without nanosecond tracking, getPeriod() rounds 1.333ms to 1ms, yielding ~1000 QPS (+33% error).
    // With nanosecond tracking, getPeriodNanos() = 1,333,333ns and the rate is accurate within 5%.
    assertTrue(errorPercent < 5.0,
        "Dispatched " + totalDispatches + " in " + durationSeconds + "s, expected ~" + expectedTotal
            + " (error " + String.format("%.1f", errorPercent) + "%)");
  }

  protected AsyncRateLimiter getRateLimiter(ScheduledExecutorService executorService, ExecutorService executor, Clock clock)
  {
    return new SmoothRateLimiter(executorService, executor, clock, _queue, MAX_BUFFERED_CALLBACKS, SmoothRateLimiter.BufferOverflowMode.DROP,
                                 RATE_LIMITER_NAME_TEST);
  }
}

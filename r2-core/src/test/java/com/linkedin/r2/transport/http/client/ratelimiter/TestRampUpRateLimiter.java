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
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.AsyncRateLimiter;
import com.linkedin.r2.transport.http.client.SmoothRateLimiter;
import com.linkedin.test.util.ClockedExecutor;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestRampUpRateLimiter extends TestSmoothRateLimiter
{
  private static final int MINIMUM_BURST = 1;
  private static final String RATE_LIMITER_NAME_TEST = "test";

  @DataProvider(name = "targetRamp")
  public Object[][] multiplePartsDataSource()
  {

    return new Object[][]
      {
        {1, 0.1f},
        {5, 0.2f},
        {10, 0.5f},
        {100, 0.1f},
        {100, 0.2f},
        {100, 0.5f},
        {100, 1},
        {100, 2},
        {100, 5},
        {100, 20},
        {100, 50},
        {100, 70},
        {100, 150},
        {100, 150000}
      };
  }

  @Test(dataProvider = "targetRamp", timeOut = TEST_TIMEOUT * 1000)
  public void testRampUp(int targetPermitsPerPeriod, float rampUp)
  {
    boolean useRampUpMethod = false;
    for (int k = 0; k < 2; k++, useRampUpMethod = true)
    {
      _queue.clear();
      ClockedExecutor clockedExecutor = new ClockedExecutor();

      RampUpRateLimiter rateLimiter = new RampUpRateLimiterImpl(new SmoothRateLimiter(
        clockedExecutor, clockedExecutor, clockedExecutor, _queue, Integer.MAX_VALUE, SmoothRateLimiter.BufferOverflowMode.DROP,
        RATE_LIMITER_NAME_TEST), clockedExecutor);

      rateLimiter.setRate(0, 1, MINIMUM_BURST, rampUp);
      rateLimiter.setRate(targetPermitsPerPeriod, ONE_SECOND_PERIOD, MINIMUM_BURST, rampUp);

      if (useRampUpMethod)
      {
        // issue close to 0 permits to have a successful ramp up afterwards
        rateLimiter.setRate(0, 1, MINIMUM_BURST, rampUp);

        rateLimiter.setRate(targetPermitsPerPeriod, ONE_SECOND_PERIOD, MINIMUM_BURST, rampUp);
      }

      AtomicInteger time = new AtomicInteger(0);
      AtomicInteger count = new AtomicInteger(0);

      List<Integer> completionsPerSecond = new ArrayList<>();

      int secondsToReachTargetState = (int) Math.ceil(targetPermitsPerPeriod / rampUp);


      IntStream.range(0, (int) (rampUp * secondsToReachTargetState * (secondsToReachTargetState + 1))).forEach(i -> {
        rateLimiter.submit(new Callback<None>()
        {
          @Override
          public void onError(Throwable e)
          {
            throw new RuntimeException(e);
          }

          @Override
          public void onSuccess(None result)
          {
            // counting how many tasks per second we are receiving.
            if (clockedExecutor.millis() - time.get() >= ONE_SECOND_PERIOD)
            {
              time.set(((int) (clockedExecutor.millis() / 1000) * 1000));
              completionsPerSecond.add(count.get());
              count.set(1);
            }
            else
            {
              count.incrementAndGet();
            }
          }
        });
      });

      // run the clock only for the exact amount of time that is necessary to reach the stable state
      clockedExecutor.runFor((long) ((secondsToReachTargetState + 2) * 1000));

      long countAboveMaxTarget = 0;
      long countAtTarget = 0;
      long countBelowTarget = 0;

      for (Integer i : completionsPerSecond)
      {
        if (i > targetPermitsPerPeriod) countAboveMaxTarget++;
        if (i == targetPermitsPerPeriod) countAtTarget++;
        if (i < targetPermitsPerPeriod) countBelowTarget++;
      }

      assertEquals(countAboveMaxTarget, 0, "It should never go above the target QPS");
      assertTrue(countAtTarget > 0, "There should be at least one at the target QPS since it should reach the stable state after a while");

      long actualStepsToTarget = (countBelowTarget + 1)
        // we want to account for the first seconds in which no task will return if the rampUp<1
        + (rampUp < 1 ? (long) (1 / rampUp) - 1 : 0);
      // using countABelowTarget+1, because the one from the last number to the target is never counted
      assertTrue(actualStepsToTarget >= secondsToReachTargetState * 0.9 && actualStepsToTarget <= Math.ceil(secondsToReachTargetState * 1.1),
        "There should be at least " + secondsToReachTargetState * 0.9 + " steps to get to the target and no more than " + Math.ceil(secondsToReachTargetState * 1.1) + ". Found: " +
          actualStepsToTarget + ".");

    }
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testRampDownImmediately()
  {
    ClockedExecutor clockedExecutor = new ClockedExecutor();

    RampUpRateLimiter rateLimiter = new RampUpRateLimiterImpl(new SmoothRateLimiter(
      clockedExecutor, clockedExecutor, clockedExecutor, _queue, Integer.MAX_VALUE, SmoothRateLimiter.BufferOverflowMode.DROP, RATE_LIMITER_NAME_TEST), clockedExecutor);
    rateLimiter.setRate(1000d, ONE_SECOND_PERIOD, MINIMUM_BURST);

    List<FutureCallback<None>> callbacks = new ArrayList<>();
    IntStream.range(0, 1002).forEach(i -> {
      FutureCallback<None> callback = new FutureCallback<>();
      rateLimiter.submit(callback);
      callbacks.add(callback);
    });

    // -1 because if it passes a full second, the new batch of permits will be issued
    clockedExecutor.runFor(ONE_SECOND_PERIOD - 1);
    IntStream.range(0, 1000).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(1000, 1002).forEach(i -> assertFalse(callbacks.get(i).isDone(), i + " should not have been executed"));

    rateLimiter.setRate(1, ONE_SECOND_PERIOD, 1, Integer.MAX_VALUE);
    clockedExecutor.runFor(ONE_SECOND_PERIOD);

    IntStream.range(1000, 1001).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(1001, 1002).forEach(i -> assertFalse(callbacks.get(i).isDone(), i + " should not have been executed"));

    clockedExecutor.runFor(ONE_SECOND_PERIOD);
    IntStream.range(1001, 1002).forEach(i -> assertTrue(callbacks.get(i).isDone()));
  }

  protected AsyncRateLimiter getRateLimiter(ScheduledExecutorService executorService, ExecutorService executor, Clock clock)
  {
    return new RampUpRateLimiterImpl(new SmoothRateLimiter(executorService, executor, clock, _queue, MAX_BUFFERED_CALLBACKS, SmoothRateLimiter.BufferOverflowMode.DROP,
                                                           RATE_LIMITER_NAME_TEST), executorService);
  }
}

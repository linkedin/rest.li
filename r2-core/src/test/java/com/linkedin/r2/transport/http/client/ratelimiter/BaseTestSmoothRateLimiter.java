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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.AsyncRateLimiter;
import com.linkedin.test.util.ClockedExecutor;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SettableClock;
import com.linkedin.util.clock.SystemClock;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;


public abstract class BaseTestSmoothRateLimiter
{
  protected static final int TEST_TIMEOUT = 3000;
  private static final int TEST_TIMEOUT_LONG = 10000;
  protected static final int MAX_BUFFERED_CALLBACKS = 1024;
  protected static final double ONE_PERMIT_PER_PERIOD = 1;
  protected static final long ONE_SECOND_PERIOD = TimeUnit.SECONDS.toMillis(1);
  private static final long ONE_MILLISECOND_PERIOD = TimeUnit.MILLISECONDS.toMillis(1);
  protected static final int UNLIMITED_BURST = Integer.MAX_VALUE;
  private static final double UNLIMITED_PERMITS = Integer.MAX_VALUE;
  private static final int CONCURRENT_THREADS = 32;
  private static final int CONCURRENT_SUBMITS = 1024;

  protected ScheduledExecutorService _scheduledExecutorService;
  protected ExecutorService _executor;
  protected final Clock _clock = SystemClock.instance();
  protected Queue<Callback<None>> _queue;

  @BeforeClass
  public void doBeforeClass()
  {
    _scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    _executor = Executors.newCachedThreadPool();
  }

  @AfterClass
  public void doAfterClass()
  {
    _scheduledExecutorService.shutdown();
    _executor.shutdown();
  }

  @BeforeMethod
  public void doBeforeMethod()
  {
    _queue = new ConcurrentLinkedQueue<>();
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testSubmitWithinPermits() throws Exception
  {
    AsyncRateLimiter rateLimiter = getRateLimiter(_scheduledExecutorService, _executor, _clock);

    rateLimiter.setRate(ONE_PERMIT_PER_PERIOD, ONE_SECOND_PERIOD, UNLIMITED_BURST);

    FutureCallback<None> callback = new FutureCallback<>();
    rateLimiter.submit(callback);

    callback.get();
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testMultiSubmitWithinPermits() throws Exception
  {
    SettableClock clock = new SettableClock();
    AsyncRateLimiter rateLimiter = getRateLimiter(_scheduledExecutorService, _executor, clock);

    rateLimiter.setRate(128d, ONE_SECOND_PERIOD, UNLIMITED_BURST);

    List<FutureCallback<None>> callbacks = new ArrayList<>();
    for (int i = 0; i < 128; i++)
    {
      FutureCallback<None> callback = new FutureCallback<>();
      callbacks.add(callback);
      rateLimiter.submit(callback);
    }

    for (int i = 0; i < callbacks.size(); i++)
    {
      callbacks.get(i).get();
    }
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testSubmitExceedsPermits() throws Exception
  {
    ClockedExecutor clockedExecutor = new ClockedExecutor();
    AsyncRateLimiter rateLimiter = getRateLimiter(clockedExecutor, clockedExecutor, clockedExecutor);

    rateLimiter.setRate(ONE_PERMIT_PER_PERIOD, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);

    List<FutureCallback<None>> callbacks = new ArrayList<>();
    IntStream.range(0, 5).forEach(i -> {
      FutureCallback<None> callback = new FutureCallback<>();
      rateLimiter.submit(callback);
      callbacks.add(callback);
    });
    // trigger task to run them until current time
    clockedExecutor.runFor(0);

    // We have one permit to begin with so the first task should run immediate and left with 4 pending
    callbacks.get(0).get();
    IntStream.range(0, 1).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(1, 5).forEach(i -> assertFalse(callbacks.get(i).isDone()));

    // We increment the clock by one period and one more permit should have been issued
    clockedExecutor.runFor(ONE_MILLISECOND_PERIOD);
    callbacks.get(1).get();
    IntStream.range(0, 2).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(2, 5).forEach(i -> assertFalse(callbacks.get(i).isDone()));

    clockedExecutor.runFor(ONE_MILLISECOND_PERIOD);
    callbacks.get(2).get();
    IntStream.range(0, 3).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(3, 5).forEach(i -> assertFalse(callbacks.get(i).isDone()));

    clockedExecutor.runFor(ONE_MILLISECOND_PERIOD);
    callbacks.get(3).get();
    IntStream.range(0, 4).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(4, 5).forEach(i -> assertFalse(callbacks.get(i).isDone()));

    clockedExecutor.runFor(ONE_MILLISECOND_PERIOD);
    callbacks.get(4).get();
    IntStream.range(0, 5).forEach(i -> assertTrue(callbacks.get(i).isDone()));
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testSetRate() throws Exception
  {
    ClockedExecutor clockedExecutor = new ClockedExecutor();
    AsyncRateLimiter rateLimiter = getRateLimiter(clockedExecutor, clockedExecutor, clockedExecutor);

    rateLimiter.setRate(ONE_PERMIT_PER_PERIOD, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);

    List<FutureCallback<None>> callbacks = new ArrayList<>();
    IntStream.range(0, 5).forEach(i -> {
      FutureCallback<None> callback = new FutureCallback<>();
      rateLimiter.submit(callback);
      callbacks.add(callback);
    });
    // trigger task to run them until current time
    clockedExecutor.runFor(0);

    // We have one permit to begin with so the first task should run immediate and left with four pending
    callbacks.get(0).get();
    IntStream.range(0, 1).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(1, 5).forEach(i -> assertFalse(callbacks.get(i).isDone(), i + " should not have been executed"));

    clockedExecutor.runFor(ONE_MILLISECOND_PERIOD);

    // We set the permit rate to two per period and increment the clock by one millisecond. We expect two
    // more callbacks to be invoked at the next permit issuance
    rateLimiter.setRate(2d, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);
    clockedExecutor.runFor(0);
    callbacks.get(1).get();
    callbacks.get(2).get();
    IntStream.range(0, 3).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(3, 5).forEach(i -> assertFalse(callbacks.get(i).isDone(), i + " should not have been executed"));

    // We set the permit rate back to one per period and increment the clock by one millisecond. We expect
    // only one more callbacks to be invoked at the next permit issuance
    rateLimiter.setRate(1d, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);
    clockedExecutor.runFor(ONE_MILLISECOND_PERIOD);
    callbacks.get(3).get();
    IntStream.range(0, 4).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(4, 5).forEach(i -> assertFalse(callbacks.get(i).isDone(), i + " should not have been executed"));

    // We set the permit rate to two per period again and increment the clock by one millisecond. We expect
    // only one more callbacks to be invoked at the next permit issuance because only one is left
    rateLimiter.setRate(2d, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);
    clockedExecutor.runFor(ONE_MILLISECOND_PERIOD);
    callbacks.get(4).get();
    IntStream.range(0, 5).forEach(i -> assertTrue(callbacks.get(i).isDone()));
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testCancelAll() throws Exception
  {
    SettableClock clock = new SettableClock();
    AsyncRateLimiter rateLimiter = getRateLimiter(_scheduledExecutorService, _executor, clock);
    rateLimiter.setRate(ONE_PERMIT_PER_PERIOD, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);

    List<FutureCallback<None>> callbacks = new ArrayList<>();
    IntStream.range(0, 5).forEach(i -> {
      FutureCallback<None> callback = new FutureCallback<>();
      rateLimiter.submit(callback);
      callbacks.add(callback);
    });

    // We have one permit to begin with so the first task should run immediate and left with four pending
    callbacks.get(0).get();
    IntStream.range(0, 1).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(1, 5).forEach(i -> assertFalse(callbacks.get(i).isDone()));

    // We cancel all pending callbacks and increment clock by one period. All pending callbacks should be invoked.
    Throwable throwable = new Throwable();
    rateLimiter.cancelAll(throwable);
    clock.addDuration(ONE_MILLISECOND_PERIOD);
    AtomicInteger errorInvocations = new AtomicInteger();
    IntStream.range(1, 5).forEach(i -> {
      try
      {
        callbacks.get(i).get();
      }
      catch (Exception e)
      {
        assertSame(e.getCause(), throwable);
        errorInvocations.incrementAndGet();
      }
    });
    assertEquals(errorInvocations.get(), 4);
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testCancelAllTwice()
  {
    AsyncRateLimiter rateLimiter = getRateLimiter(_scheduledExecutorService, _executor, _clock);
    rateLimiter.setRate(ONE_PERMIT_PER_PERIOD, ONE_SECOND_PERIOD, UNLIMITED_BURST);

    rateLimiter.cancelAll(new Throwable());
    rateLimiter.cancelAll(new Throwable());
  }

  @Test(timeOut = TEST_TIMEOUT_LONG)
  public void testConcurrentSubmits() throws Exception
  {
    Executor executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
    AsyncRateLimiter rateLimiter = getRateLimiter(_scheduledExecutorService, this._executor, _clock);
    rateLimiter.setRate(UNLIMITED_PERMITS, ONE_SECOND_PERIOD, UNLIMITED_BURST);

    CountDownLatch countDownLatch = new CountDownLatch(CONCURRENT_SUBMITS);
    LongAdder successCount = new LongAdder();
    LongAdder failureCount = new LongAdder();
    for (int i = 0; i < CONCURRENT_SUBMITS; i++)
    {
      executor.execute(() ->
        rateLimiter.submit(new Callback<None>()
        {
          @Override
          public void onError(Throwable e)
          {
            failureCount.increment();
            countDownLatch.countDown();
          }

          @Override
          public void onSuccess(None result)
          {
            successCount.increment();
            countDownLatch.countDown();
          }
        })
      );
    }

    countDownLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS);
    Assert.assertEquals(successCount.longValue(), CONCURRENT_SUBMITS);
    Assert.assertEquals(failureCount.longValue(), 0L);
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testSetRateInstantaneous()
  {
    ClockedExecutor clockedExecutor = new ClockedExecutor();
    AsyncRateLimiter rateLimiter = getRateLimiter(clockedExecutor, clockedExecutor, clockedExecutor);

    List<FutureCallback<None>> callbacks = new ArrayList<>();
    IntStream.range(0, 10).forEachOrdered(i -> {
      FutureCallback<None> callback = new FutureCallback<>();
      rateLimiter.submit(callback);
      callbacks.add(callback);
    });

    // the last set should take immediately effect, and therefore at ms 0, we should have 3 permits available
    rateLimiter.setRate(0d, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);
    rateLimiter.setRate(1d, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);
    rateLimiter.setRate(2d, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);
    rateLimiter.setRate(3d, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);

    // trigger task to run them until current time
    clockedExecutor.runFor(0);

    // We have one permit to begin with so the first task should run immediate and left with four pending
    IntStream.range(0, 3).forEach(i -> assertTrue(callbacks.get(i).isDone(), i + " should have been executed " + callbacks.get(i)));
    IntStream.range(3, 10).forEach(i -> assertFalse(callbacks.get(i).isDone(), i + " should not have been executed"));

    clockedExecutor.runFor(ONE_MILLISECOND_PERIOD);
    IntStream.range(3, 6).forEach(i -> assertTrue(callbacks.get(i).isDone(), i + " should have been executed " + callbacks.get(i)));
    IntStream.range(6, 10).forEach(i -> assertFalse(callbacks.get(i).isDone(), i + " should not have been executed"));
  }

  protected abstract AsyncRateLimiter getRateLimiter(ScheduledExecutorService executorService, ExecutorService executor, Clock clock);

}

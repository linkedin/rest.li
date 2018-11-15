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

package com.linkedin.r2.transport.http.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SettableClock;
import com.linkedin.util.clock.SystemClock;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestSmoothRateLimiter
{
  private static final int TEST_TIMEOUT = 1000;
  private static final int TEST_TIMEOUT_LONG = 10000;
  private static final int MAX_BUFFERED_CALLBACKS = 1024;
  private static final int ONE_PERMIT_PER_PERIOD = 1;
  private static final long ONE_SECOND_PERIOD = TimeUnit.SECONDS.toMillis(1);
  private static final long ONE_MILLISECOND_PERIOD = TimeUnit.MILLISECONDS.toMillis(1);
  private static final int UNLIMITED_BURST = Integer.MAX_VALUE;
  private static final int UNLIMITED_PERMITS = Integer.MAX_VALUE;
  private static final int CONCURRENT_THREADS = 32;
  private static final int CONCURRENT_SUBMITS = 1024;

  private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
  private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
  private static final Clock CLOCK = SystemClock.instance();
  private static final Queue<Callback<None>> QUEUE = new ConcurrentLinkedDeque<>();

  @AfterClass
  public void doAfterClass()
  {
    SCHEDULER.shutdown();
    EXECUTOR.shutdown();
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testSubmitWithinPermits() throws Exception
  {
    SmoothRateLimiter rateLimiter = new SmoothRateLimiter(
        SCHEDULER, EXECUTOR, CLOCK, QUEUE, MAX_BUFFERED_CALLBACKS,
        ONE_PERMIT_PER_PERIOD, ONE_SECOND_PERIOD, UNLIMITED_BURST);

    FutureCallback<None> callback = new FutureCallback<>();
    rateLimiter.submit(callback);

    callback.get();
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testUnlimitedBurstRate() throws Exception
  {
    SmoothRateLimiter.Rate rate = new SmoothRateLimiter.Rate(3, 1000, UNLIMITED_BURST);
    assertEquals(rate.getEvents(), 3);
    assertEquals(rate.getPeriod(), 1000);
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testLowBurstRate() throws Exception
  {
    SmoothRateLimiter.Rate rate = new SmoothRateLimiter.Rate(3, 1000, 1);
    assertEquals(rate.getEvents(), 1);
    assertEquals(rate.getPeriod(), 333);
  }

  @Test(timeOut = TEST_TIMEOUT, expectedExceptions = IllegalArgumentException.class)
  public void testUnsatisfiableBurstRate() throws Exception
  {
    new SmoothRateLimiter.Rate(50, 10, 1);
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testMultiSubmitWithinPermits() throws Exception
  {
    SettableClock clock = new SettableClock();
    SmoothRateLimiter rateLimiter = new SmoothRateLimiter(
        SCHEDULER, EXECUTOR, clock, QUEUE, MAX_BUFFERED_CALLBACKS,
        128, ONE_SECOND_PERIOD, UNLIMITED_BURST);

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
    SettableClock clock = new SettableClock();
    SmoothRateLimiter rateLimiter = new SmoothRateLimiter(
        SCHEDULER, EXECUTOR, clock, QUEUE, MAX_BUFFERED_CALLBACKS,
        ONE_PERMIT_PER_PERIOD, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);

    List<FutureCallback<None>> callbacks = new ArrayList<>();
    IntStream.range(0, 5).forEach(i -> {
      FutureCallback<None> callback = new FutureCallback<>();
      rateLimiter.submit(callback);
      callbacks.add(callback);
    });

    // We have one permit to begin with so the first task should run immediate and left with 4 pending
    callbacks.get(0).get();
    IntStream.range(0, 1).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(1, 5).forEach(i -> assertFalse(callbacks.get(i).isDone()));

    // We increment the clock by one period and one more permit should have been issued
    clock.addDuration(ONE_MILLISECOND_PERIOD);
    callbacks.get(1).get();
    IntStream.range(0, 2).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(2, 5).forEach(i -> assertFalse(callbacks.get(i).isDone()));

    clock.addDuration(ONE_MILLISECOND_PERIOD);
    callbacks.get(2).get();
    IntStream.range(0, 3).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(3, 5).forEach(i -> assertFalse(callbacks.get(i).isDone()));

    clock.addDuration(ONE_MILLISECOND_PERIOD);
    callbacks.get(3).get();
    IntStream.range(0, 4).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(4, 5).forEach(i -> assertFalse(callbacks.get(i).isDone()));

    clock.addDuration(ONE_MILLISECOND_PERIOD);
    callbacks.get(4).get();
    IntStream.range(0, 5).forEach(i -> assertTrue(callbacks.get(i).isDone()));
  }

  @Test(timeOut = TEST_TIMEOUT, expectedExceptions = RejectedExecutionException.class)
  public void testSubmitExceedsMaxBuffered()
  {
    SmoothRateLimiter rateLimiter = new SmoothRateLimiter(
        SCHEDULER, EXECUTOR, CLOCK, QUEUE, 0,
        ONE_PERMIT_PER_PERIOD, ONE_SECOND_PERIOD, UNLIMITED_BURST);

    FutureCallback<None> callback = new FutureCallback<>();
    rateLimiter.submit(callback);
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testSetRate() throws Exception
  {
    SettableClock clock = new SettableClock();
    SmoothRateLimiter rateLimiter = new SmoothRateLimiter(
        SCHEDULER, EXECUTOR, clock, QUEUE, MAX_BUFFERED_CALLBACKS,
        ONE_PERMIT_PER_PERIOD, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);

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

    // We set the permit rate to two per period and increment the clock by one millisecond. We expect two
    // more callbacks to be invoked at the next permit issuance
    rateLimiter.setRate(2, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);
    clock.addDuration(ONE_MILLISECOND_PERIOD);
    callbacks.get(1).get();
    callbacks.get(2).get();
    IntStream.range(0, 3).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(3, 5).forEach(i -> assertFalse(callbacks.get(i).isDone()));

    // We set the permit rate back to one per period and increment the clock by one millisecond. We expect
    // only one more callbacks to be invoked at the next permit issuance
    rateLimiter.setRate(1, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);
    clock.addDuration(ONE_MILLISECOND_PERIOD);
    callbacks.get(3).get();
    IntStream.range(0, 4).forEach(i -> assertTrue(callbacks.get(i).isDone()));
    IntStream.range(4, 5).forEach(i -> assertFalse(callbacks.get(i).isDone()));

    // We set the permit rate to two per period again and increment the clock by one millisecond. We expect
    // only one more callbacks to be invoked at the next permit issuance because only one is left
    rateLimiter.setRate(2, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);
    clock.addDuration(ONE_MILLISECOND_PERIOD);
    callbacks.get(4).get();
    IntStream.range(0, 5).forEach(i -> assertTrue(callbacks.get(i).isDone()));
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testCancelAll() throws Exception
  {
    SettableClock clock = new SettableClock();
    SmoothRateLimiter rateLimiter = new SmoothRateLimiter(
        SCHEDULER, EXECUTOR, clock, QUEUE, MAX_BUFFERED_CALLBACKS,
        ONE_PERMIT_PER_PERIOD, ONE_MILLISECOND_PERIOD, UNLIMITED_BURST);

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
    SmoothRateLimiter rateLimiter = new SmoothRateLimiter(
        SCHEDULER, EXECUTOR, CLOCK, QUEUE, MAX_BUFFERED_CALLBACKS,
        ONE_PERMIT_PER_PERIOD, ONE_SECOND_PERIOD, UNLIMITED_BURST);
    rateLimiter.cancelAll(new Throwable());
    rateLimiter.cancelAll(new Throwable());
  }

  @Test(timeOut = TEST_TIMEOUT_LONG)
  public void testConcurrentSubmits() throws Exception
  {
    Executor executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
    SmoothRateLimiter rateLimiter = new SmoothRateLimiter(
        SCHEDULER, EXECUTOR, CLOCK, QUEUE, MAX_BUFFERED_CALLBACKS,
        UNLIMITED_PERMITS, ONE_SECOND_PERIOD, UNLIMITED_BURST);

    CountDownLatch countDownLatch = new CountDownLatch(CONCURRENT_SUBMITS);
    LongAdder successCount = new LongAdder();
    LongAdder failureCount = new LongAdder();
    for (int i = 0; i < CONCURRENT_SUBMITS; i++) {
      executor.execute(() ->
        rateLimiter.submit(new Callback<None>() {
          @Override
          public void onError(Throwable e) {
            failureCount.increment();
            countDownLatch.countDown();
          }

          @Override
          public void onSuccess(None result) {
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
}

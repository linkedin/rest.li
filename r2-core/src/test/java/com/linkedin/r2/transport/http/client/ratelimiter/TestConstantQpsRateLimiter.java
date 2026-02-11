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

package com.linkedin.r2.transport.http.client.ratelimiter;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.ConstantQpsRateLimiter;
import com.linkedin.r2.transport.http.client.EvictingCircularBuffer;
import com.linkedin.r2.transport.http.client.TestEvictingCircularBuffer;
import com.linkedin.test.util.ClockedExecutor;
import com.linkedin.test.util.retry.ThreeRetries;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.NoSuchElementException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.junit.Assert.fail;
import com.linkedin.util.clock.SettableClock;


public class TestConstantQpsRateLimiter
{
  private static final int TEST_TIMEOUT = 3000;
  private static final float TEST_QPS = 5;
  private static final float TEST_LOW_FRACTIONAL_QPS = 0.05f;
  private static final int ONE_SECOND = 1000;
  private static final int TEST_NUM_CYCLES = 100;
  private static final int UNLIMITED_BURST = Integer.MAX_VALUE;
  private static final int LARGE_TEST_NUM_REPLICAS = 400;
  private static final int LARGE_TEST_INBOUND_QPS_PER_REPLICA = 10;
  private static final int LARGE_TEST_MAX_BURST_MULTIPLE = 3;
  private static final int LARGE_TEST_MAX_BURST_FREQUENCY_COUNT = 5;
  private static final int LARGE_TEST_MAX_ZERO_FREQUENCY_COUNT = 5;
  private static final float LARGE_TEST_QUERY_VOLUME_CONSISTENCY_CONFIDENCE = 0.99f;


  @Test(timeOut = TEST_TIMEOUT)
  public void submitOnceGetMany()
  {
    ClockedExecutor executor = new ClockedExecutor();
    ClockedExecutor circularBufferExecutor = new ClockedExecutor();
    ConstantQpsRateLimiter rateLimiter =
        new ConstantQpsRateLimiter(executor, executor, executor, TestEvictingCircularBuffer.getBuffer(circularBufferExecutor));

    rateLimiter.setRate(TEST_QPS, ONE_SECOND, UNLIMITED_BURST);
    rateLimiter.setBufferCapacity(1);

    TattlingCallback<None> tattler = new TattlingCallback<>(executor);
    rateLimiter.submit(tattler);
    executor.runFor(ONE_SECOND * TEST_NUM_CYCLES);
    Assert.assertTrue(tattler.getInteractCount() > 1);
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void lowNonWholeRate()
  {
    for (int i = 0; i < TEST_NUM_CYCLES; i++)
    {
      ClockedExecutor executor = new ClockedExecutor();
      ClockedExecutor circularBufferExecutor = new ClockedExecutor();
      ConstantQpsRateLimiter rateLimiter =
          new ConstantQpsRateLimiter(executor, executor, executor, TestEvictingCircularBuffer.getBuffer(circularBufferExecutor));
      rateLimiter.setRate(TEST_LOW_FRACTIONAL_QPS, ONE_SECOND, UNLIMITED_BURST);
      rateLimiter.setBufferCapacity(1);
      TattlingCallback<None> tattler = new TattlingCallback<>(executor);
      rateLimiter.submit(tattler);
      // run for enough time such that 3 queries are sent
      executor.runFor((int) (((ONE_SECOND / TEST_LOW_FRACTIONAL_QPS) * 3) - 1));
      Assert.assertTrue(tattler.getInteractCount() == 3);
    }
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void eventLoopStopsWhenTtlExpiresAllRequests()
  {
    ClockedExecutor executor = new ClockedExecutor();
    ConstantQpsRateLimiter rateLimiter =
        new ConstantQpsRateLimiter(executor, executor, executor, TestEvictingCircularBuffer.getBuffer(executor));

    rateLimiter.setRate(TEST_QPS, ONE_SECOND, UNLIMITED_BURST);
    rateLimiter.setBufferTtl(ONE_SECOND - 1, ChronoUnit.MILLIS);
    TattlingCallback<None> tattler = new TattlingCallback<>(executor);
    rateLimiter.submit(tattler);
    executor.runFor(ONE_SECOND * TEST_NUM_CYCLES);
    Assert.assertSame(tattler.getInteractCount(), (int) TEST_QPS);
    long prevTaskCount = executor.getExecutedTaskCount();
    executor.runFor(ONE_SECOND * TEST_NUM_CYCLES);
    // EventLoop continues by scheduling itself at the end. If executed task count remains the same,
    // then EventLoop hasn't re-scheduled itself.
    Assert.assertSame(executor.getExecutedTaskCount(), prevTaskCount);
  }

  @Test
  public void ensureRandomButConstantRate()
  {
    ClockedExecutor executor = new ClockedExecutor();
    ClockedExecutor circularBufferExecutor = new ClockedExecutor();
    ConstantQpsRateLimiter rateLimiter =
        new ConstantQpsRateLimiter(executor, executor, executor, TestEvictingCircularBuffer.getBuffer(circularBufferExecutor));
    rateLimiter.setRate(200d, ONE_SECOND, 1);
    rateLimiter.setBufferCapacity(1);
    TattlingCallback<None> tattler = new TattlingCallback<>(executor);
    rateLimiter.submit(tattler);
    executor.runFor(ONE_SECOND * TEST_NUM_CYCLES);
    long prevTime = 0;
    List<Long> timeDeltas = new ArrayList<>();
    for (Long stamp : tattler.getOccurrences())
    {
      timeDeltas.add(stamp - prevTime);
      prevTime = stamp;
    }
    // Ensure variance up to 10 possible time deltas given a rate of 200 requests per second
    Set<Long> uniqueTimeDeltas = new HashSet<>(timeDeltas);
    assert(uniqueTimeDeltas.size() > 8 && uniqueTimeDeltas.size() < 11);
  }

  @Test(retryAnalyzer = ThreeRetries.class) // Known to be flaky in CI
  public void testLowRateHighlyParallelConsistentRandomness()
  {
    // Simulate a large production cluster dispatching a very low rate of traffic.
    // This test verifies that the resulting qps from a distributed collection of dispatchers
    // follows a predictable pattern within the defined tolerances.
    int maxBurstFailCount = 0;
    int burstFreqFailCount = 0;
    int zeroFreqFailCount = 0;
    for (int n = 0; n < TEST_NUM_CYCLES; n++)
    {
      // Set simulated test time such that each replica sends exactly one request.
      int totalRuntime = (int) (ONE_SECOND / (TEST_QPS / LARGE_TEST_NUM_REPLICAS));
      List<Long> queryTimes = new ArrayList<>();
      for (int i = 0; i < LARGE_TEST_NUM_REPLICAS; i++)
      {
        ClockedExecutor executor = new ClockedExecutor();
        ConstantQpsRateLimiter rateLimiter =
            new ConstantQpsRateLimiter(executor, executor, executor, TestEvictingCircularBuffer.getBuffer(executor));
        rateLimiter.setBufferTtl(Integer.MAX_VALUE, ChronoUnit.DAYS);
        rateLimiter.setBufferCapacity(1);
        // Split an already low TEST_QPS across a large number of replicas
        rateLimiter.setRate(TEST_QPS / LARGE_TEST_NUM_REPLICAS, ONE_SECOND, 1);
        TattlingCallback<None> tattler = new TattlingCallback<>(executor);
        rateLimiter.submit(tattler);

        // Each test replica receives 10 qps, but sends 1 request very infrequently due to the low
        // target rate shared across the large cluster.
        // Intermix inbound queries while running clock at the defined rate
        for (int x = 0; x < totalRuntime; x = x + ONE_SECOND / LARGE_TEST_INBOUND_QPS_PER_REPLICA)
        {
          // ensure that calling setRate before submitting a new callback does not detrimentally affect random distribution
          rateLimiter.setRate(TEST_QPS / LARGE_TEST_NUM_REPLICAS, ONE_SECOND, 1);
          rateLimiter.submit(tattler);
          executor.runFor(ONE_SECOND / LARGE_TEST_INBOUND_QPS_PER_REPLICA);
        }
        for (Long stamp : tattler.getOccurrences())
        {
          // totalRuntime includes 1ms of the next window. Exclude any query occurring on the first ms from the next window.
          // Prefer this over making totalRuntime 1ms shorter since it keeps the math clean
          if (stamp != totalRuntime) {
            queryTimes.add(stamp);
          }
        }
      }
      // each replica should have only sent one request
      assert (queryTimes.size() == LARGE_TEST_NUM_REPLICAS);
      int[] queriesPerBucketedSecond = new int[totalRuntime / ONE_SECOND];
      for (Long stamp : queryTimes)
      {
        int idx = (int) (stamp / ONE_SECOND);
        queriesPerBucketedSecond[idx]++;
      }
      // ensure the cluster sent an average of the TEST_QPS
      assert (Arrays.stream(queriesPerBucketedSecond).average().getAsDouble() == TEST_QPS);

      // Variability of query volume is expected in production, but make sure it stays in check
      // Ensure our bursts in queries in a given second aren't too high
      if (Arrays.stream(queriesPerBucketedSecond).max().getAsInt() > TEST_QPS * LARGE_TEST_MAX_BURST_MULTIPLE)
      {
        maxBurstFailCount++;
      };
      // Make sure though that we don't see too many seconds with high query volume
      if (Arrays.stream(queriesPerBucketedSecond).filter(
          a -> a > TEST_QPS * LARGE_TEST_MAX_BURST_MULTIPLE * 0.67).count() > LARGE_TEST_MAX_BURST_FREQUENCY_COUNT)
      {
        burstFreqFailCount++;
      }
      // Make sure we don't have too many cases of sending zero qps.
      if (Arrays.stream(queriesPerBucketedSecond).filter(a -> a == 0).count() > LARGE_TEST_MAX_ZERO_FREQUENCY_COUNT)
      {
        zeroFreqFailCount++;
      }
    }
    // Query volume stability assertions should be true within the defined confidence value
    int acceptableFailCount =
        Math.round((TEST_NUM_CYCLES * (1 - LARGE_TEST_QUERY_VOLUME_CONSISTENCY_CONFIDENCE)));
    assert(maxBurstFailCount <= acceptableFailCount);
    assert(burstFreqFailCount <= acceptableFailCount);
    assert(zeroFreqFailCount <= acceptableFailCount);
  }

  @Test(timeOut = TEST_TIMEOUT)
  /**
   * This simulates the shutdown behavior of ConstantQpsDarkClusterStrategy,
   * to verify that the buffer is cleared immediately upon shutdown.
   */
  public void shutdownClearsBufferImmediately()
  {
    ClockedExecutor executor = new ClockedExecutor();
    // Use a separate clock for the circular buffer so TTL doesn’t auto-expire unless we advance it.
    ClockedExecutor circularBufferExecutor = new ClockedExecutor();
    EvictingCircularBuffer buffer = TestEvictingCircularBuffer.getBuffer(circularBufferExecutor);
    ConstantQpsRateLimiter rateLimiter =
        new ConstantQpsRateLimiter(executor, executor, executor, buffer);

    // Ensure TTL is long so callbacks would normally remain unless explicitly cleared.
    rateLimiter.setBufferTtl(Integer.MAX_VALUE, ChronoUnit.DAYS);
    rateLimiter.setBufferCapacity(10);

    // Submit several no-op callbacks
    for (int i = 0; i < 10; i++)
    {
      rateLimiter.submit(new Callback<None>()
      {
        @Override
        public void onError(Throwable e) { }

        @Override
        public void onSuccess(None result) { }
      });
    }

    // Sanity: buffer should have something retrievable before cancelAll
    Assert.assertNotNull(buffer.get());

    // Invoke shutdown-like behavior: clear buffer immediately
    rateLimiter.clear();

    // Run the executor briefly to allow the event loop to process
    executor.runFor(ONE_SECOND);

    // Verify the buffer is cleared immediately (i.e., not waiting for TTL to expire)
    boolean threw = false;
    try
    {
      buffer.get();
    }
    catch (NoSuchElementException ex)
    {
      threw = true;
    }
    Assert.assertTrue("Expected the buffer to be empty immediately after clear()", threw);
  }

  /**
   * Verifies that rates with non-integer millisecond periods dispatch accurately.
   * <p>At 1729 QPS with burst=2 the internal period is 1000*2/1729 = 1.156ms.
   * With fractional period tracking the observed rate should be close  to 1729/s.</p>
   */
  @Test(timeOut = TEST_TIMEOUT)
  public void testFractionalPeriodRateAccuracy()
  {
    ClockedExecutor executor = new ClockedExecutor();
    ClockedExecutor circularBufferExecutor = new ClockedExecutor();
    ConstantQpsRateLimiter rateLimiter =
            new ConstantQpsRateLimiter(executor, executor, executor, TestEvictingCircularBuffer.getBuffer(circularBufferExecutor));

    int targetQps = 1729;
    int burst = (int) Math.max(1, Math.ceil((double) targetQps / ONE_SECOND));
    rateLimiter.setRate(targetQps, ONE_SECOND, burst);
    rateLimiter.setBufferCapacity(1);

    TattlingCallback<None> tattler = new TattlingCallback<>(executor);
    rateLimiter.submit(tattler);

    int durationSeconds = 5;
    executor.runFor(ONE_SECOND * durationSeconds);

    int totalDispatches = tattler.getInteractCount();
    int expectedTotal = targetQps * durationSeconds;               // 7500
    double errorPercent = 100.0 * Math.abs(totalDispatches - expectedTotal) / expectedTotal;
    double maxErrorPercent = 5.0; // locally experiments show error with fix should be under 1%, but allow some buffer for CI variability
    Assert.assertTrue(String.format("Expected total dispatches to be within %f%% of expected total, but was %d vs %d",
            maxErrorPercent, totalDispatches, expectedTotal), errorPercent < maxErrorPercent);
  }


  /**
   * Verifies that rates with non-integer millisecond periods dispatch accurately.
   * <p>At 2595 QPS with burst=2 the internal period is 1000*2/2595 = 0.771ms.
   * With fractional period tracking the observed rate should be close to 2595/s.</p>
   */
  @Test(timeOut = TEST_TIMEOUT)
  public void testFractionalPeriodRateAccuracy_higher_qps()
  {
    ClockedExecutor executor = new ClockedExecutor();
    ClockedExecutor circularBufferExecutor = new ClockedExecutor();
    ConstantQpsRateLimiter rateLimiter =
            new ConstantQpsRateLimiter(executor, executor, executor, TestEvictingCircularBuffer.getBuffer(circularBufferExecutor));

    int targetQps = 2595;
    int burst = (int) Math.max(1, Math.ceil((double) targetQps / ONE_SECOND));
    rateLimiter.setRate(targetQps, ONE_SECOND, burst);
    rateLimiter.setBufferCapacity(1);

    TattlingCallback<None> tattler = new TattlingCallback<>(executor);
    rateLimiter.submit(tattler);

    int durationSeconds = 5;
    executor.runFor(ONE_SECOND * durationSeconds);

    int totalDispatches = tattler.getInteractCount();
    int expectedTotal = targetQps * durationSeconds;               // 7500
    double errorPercent = 100.0 * Math.abs(totalDispatches - expectedTotal) / expectedTotal;
    double maxErrorPercent = 5.0; // locally experiments show error with fix should be under 1%, but allow some buffer for CI variability
    Assert.assertTrue(String.format("Expected total dispatches to be within %f%% of expected total, but was %d vs %d",
            maxErrorPercent, totalDispatches, expectedTotal), errorPercent < maxErrorPercent);
  }

  private static class TattlingCallback<T> implements Callback<T>
  {
    private AtomicInteger _interactCount = new AtomicInteger();
    private List<Long> _occurrences = new ArrayList<>();
    private ClockedExecutor _clock;

    TattlingCallback(ClockedExecutor clock)
    {
      _clock = clock;
    }

    @Override
    public void onError(Throwable e) {}

    @Override
    public void onSuccess(T result)
    {
      _interactCount.incrementAndGet();
      _occurrences.add(_clock.currentTimeMillis());
    }

    public int getInteractCount()
    {
      return _interactCount.intValue();
    }

    public List<Long> getOccurrences()
    {
      return _occurrences;
    }
  }

  @DataProvider(name = "rateValuesForShutdownPendingSchedule")
  public Object[][] rateValuesForShutdownPendingSchedule()
  {
    // null => don't call setRate (exercise the default zero-rate behavior)
    return new Object[][]
        {
            {null}, // Dont set rate, this leaves it in the default state
            {0d},
            {1d},
            {10d}
        };
  }

  @Test(dataProvider = "rateValuesForShutdownPendingSchedule", timeOut = TEST_TIMEOUT)
  public void shutdownShouldCancelPendingEventLoopScheduleImpl(Double permitsPerPeriod)
  {
    CapturingScheduledExecutor scheduler = new CapturingScheduledExecutor();

    // Use a deterministic clock so we can advance time to when the scheduled tick should run.
    SettableClock clock = new SettableClock(0);
    EvictingCircularBuffer buffer = new EvictingCircularBuffer(1, Integer.MAX_VALUE, ChronoUnit.DAYS, clock);
    ConstantQpsRateLimiter rateLimiter = new ConstantQpsRateLimiter(scheduler, Runnable::run, clock, buffer);

    rateLimiter.submit(new Callback<None>()
    {
      @Override
      public void onError(Throwable e) { }

      @Override
      public void onSuccess(None result) { }
    });

    // Optionally change the rate AFTER submitting so the first event-loop run happens before any
    // updateWithNewRate runnable is processed. This avoids flakiness from the internal Random-based delay.
    if (permitsPerPeriod != null)
    {
      rateLimiter.setRate(permitsPerPeriod, ONE_SECOND, 1);
    }

    // Drive exactly one immediate runnable (the event loop enqueued via submit()).
    scheduler.runNextImmediate();
    Assert.assertEquals("Expected a pending scheduled event-loop task", 1, scheduler.getScheduledCount());

    long nextDelayMs = scheduler.peekNextScheduledDelayMs();

    rateLimiter.stop();
    rateLimiter.clear();

    // Drain remaining immediate tasks (e.g., updateWithNewRate if we called setRate). Since we've stopped execution,
    // none of these tasks should schedule additional delayed ticks.
    scheduler.runAllImmediate();
    Assert.assertEquals("Expected the already-scheduled event-loop tick to remain pending after clear()",
        1, scheduler.getScheduledCount());

    // Advance time to when the delayed tick should run, then execute it. With an empty buffer, the event loop should
    // pause and should not schedule another delayed tick.
    clock.addDuration(nextDelayMs + 1);
    scheduler.runNextScheduled();
    scheduler.runAllImmediate();
    Assert.assertEquals("Expected no additional scheduled event-loop tasks after the delayed tick runs on an empty buffer",
        0, scheduler.getScheduledCount());
  }

  /**
   * Deterministic ScheduledExecutorService used to verify whether SmoothRateLimiter leaves a pending scheduled task behind.
   * This avoids racey assertions against a real executor while still exercising the schedule/cancel paths.
   */
  private static final class CapturingScheduledExecutor implements java.util.concurrent.ScheduledExecutorService
  {
    private final Deque<Runnable> _immediate = new ArrayDeque<>();
    private final Deque<CapturedScheduledFuture> _scheduled = new ArrayDeque<>();
    private volatile boolean _shutdown = false;

    int getScheduledCount()
    {
      return _scheduled.size();
    }

    long peekNextScheduledDelayMs()
    {
      CapturedScheduledFuture f = _scheduled.peekFirst();
      if (f == null)
      {
        throw new IllegalStateException("No scheduled tasks");
      }
      return f.getDelayMs();
    }

    void runNextScheduled()
    {
      CapturedScheduledFuture f = _scheduled.pollFirst();
      if (f == null)
      {
        return;
      }
      if (!f.isCancelled())
      {
        f.runCommand();
      }
    }

    void runAllImmediate()
    {
      Runnable r;
      while ((r = _immediate.pollFirst()) != null)
      {
        r.run();
      }
    }

    void runNextImmediate()
    {
      Runnable r = _immediate.pollFirst();
      if (r != null)
      {
        r.run();
      }
    }

    private void removeScheduled(CapturedScheduledFuture f)
    {
      _scheduled.remove(f);
    }

    @Override
    public void execute(Runnable command)
    {
      if (_shutdown)
      {
        throw new java.util.concurrent.RejectedExecutionException("scheduler is shutdown");
      }
      _immediate.addLast(command);
    }

    @Override
    public java.util.concurrent.ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
    {
      if (_shutdown)
      {
        throw new java.util.concurrent.RejectedExecutionException("scheduler is shutdown");
      }
      CapturedScheduledFuture future = new CapturedScheduledFuture(this, command, unit.toMillis(delay));
      _scheduled.addLast(future);
      return future;
    }

    // Unused ScheduledExecutorService APIs for this test.
    @Override
    public <V> java.util.concurrent.ScheduledFuture<V> schedule(java.util.concurrent.Callable<V> callable, long delay, TimeUnit unit)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown()
    {
      _shutdown = true;
      _immediate.clear();
      _scheduled.clear();
    }

    @Override
    public java.util.List<Runnable> shutdownNow()
    {
      shutdown();
      return java.util.Collections.emptyList();
    }

    @Override
    public boolean isShutdown()
    {
      return _shutdown;
    }

    @Override
    public boolean isTerminated()
    {
      return _shutdown;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
    {
      return _shutdown;
    }

    @Override
    public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> java.util.concurrent.Future<T> submit(Runnable task, T result)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<?> submit(Runnable task)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> java.util.List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks,
                                                                        long timeout, TimeUnit unit)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks)
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, TimeUnit unit)
    {
      throw new UnsupportedOperationException();
    }

    private static final class CapturedScheduledFuture implements java.util.concurrent.ScheduledFuture<Object>
    {
      private final CapturingScheduledExecutor _owner;
      private final Runnable _command;
      private final long _delayMs;
      private volatile boolean _cancelled = false;

      CapturedScheduledFuture(CapturingScheduledExecutor owner, Runnable command, long delayMs)
      {
        _owner = owner;
        _command = command;
        _delayMs = delayMs;
      }

      long getDelayMs()
      {
        return _delayMs;
      }

      void runCommand()
      {
        _command.run();
      }

      @Override
      public long getDelay(TimeUnit unit)
      {
        return unit.convert(_delayMs, TimeUnit.MILLISECONDS);
      }

      @Override
      public int compareTo(java.util.concurrent.Delayed o)
      {
        return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
      }

      @Override
      public boolean cancel(boolean mayInterruptIfRunning)
      {
        if (_cancelled)
        {
          return false;
        }
        _cancelled = true;
        _owner.removeScheduled(this);
        return true;
      }

      @Override
      public boolean isCancelled()
      {
        return _cancelled;
      }

      @Override
      public boolean isDone()
      {
        return _cancelled;
      }

      @Override
      public Object get()
      {
        throw new UnsupportedOperationException();
      }

      @Override
      public Object get(long timeout, TimeUnit unit)
      {
        throw new UnsupportedOperationException();
      }
    }
  }
}

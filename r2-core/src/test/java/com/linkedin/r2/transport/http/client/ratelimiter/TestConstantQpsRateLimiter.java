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
import com.linkedin.r2.transport.http.client.TestEvictingCircularBuffer;
import com.linkedin.test.util.ClockedExecutor;
import com.linkedin.test.util.retry.ThreeRetries;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.testng.annotations.Test;


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
}

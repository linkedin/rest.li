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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
  private static final int ONE_SECOND = 1000;
  private static final int TEST_NUM_CYCLES = 10;
  private static final int UNLIMITED_BURST = Integer.MAX_VALUE;


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
    ClockedExecutor executor = new ClockedExecutor();
    ClockedExecutor circularBufferExecutor = new ClockedExecutor();
    ConstantQpsRateLimiter rateLimiter =
        new ConstantQpsRateLimiter(executor, executor, executor, TestEvictingCircularBuffer.getBuffer(circularBufferExecutor));
    rateLimiter.setRate(0.05d, ONE_SECOND, UNLIMITED_BURST);
    rateLimiter.setBufferCapacity(1);
    TattlingCallback<None> tattler = new TattlingCallback<>(executor);
    rateLimiter.submit(tattler);
    executor.runFor(59000);
    Assert.assertTrue(tattler.getInteractCount() == 3);
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void eventLoopStopsWhenTtlExpiresAllRequests()
  {
    ClockedExecutor executor = new ClockedExecutor();
    ConstantQpsRateLimiter rateLimiter =
        new ConstantQpsRateLimiter(executor, executor, executor, TestEvictingCircularBuffer.getBuffer(executor));

    rateLimiter.setRate(TEST_QPS, ONE_SECOND, UNLIMITED_BURST);
    rateLimiter.setBufferTtl(999, ChronoUnit.MILLIS);
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
    ConstantQpsRateLimiter rateLimiter =
        new ConstantQpsRateLimiter(executor, executor, executor, TestEvictingCircularBuffer.getBuffer(executor));
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
    assert(uniqueTimeDeltas.size() > 5 && uniqueTimeDeltas.size() < 11);
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
    public void onSuccess(T result) {
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

package com.linkedin.r2.transport.http.client.ratelimiter;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.ConstantQpsRateLimiter;
import com.linkedin.r2.transport.http.client.TestEvictingCircularBuffer;
import com.linkedin.test.util.ClockedExecutor;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.testng.annotations.Test;


public class TestConstantQpsRateLimiter {
  private static final int TEST_TIMEOUT = 3000;
  private static final float TEST_QPS = 5;
  private static final int ONE_SECOND = 1000;
  private static final int TEST_NUM_CYCLES = 10;
  private static final int UNLIMITED_BURST = Integer.MAX_VALUE;


  @Test(timeOut = TEST_TIMEOUT)
  public void submitOnceGetMany()
  {
    ClockedExecutor executor = new ClockedExecutor();
    ConstantQpsRateLimiter rateLimiter =
        new ConstantQpsRateLimiter(executor, executor, executor, TestEvictingCircularBuffer.getBuffer(executor));

    rateLimiter.setRate(TEST_QPS, ONE_SECOND, UNLIMITED_BURST);
    rateLimiter.setBufferCapacity(1);

    TattlingCallback<None> tattler = new TattlingCallback<>();
    rateLimiter.submit(tattler);
    executor.runFor(ONE_SECOND * TEST_NUM_CYCLES);
    Assert.assertTrue(tattler.getInteractCount() > 1);
  }

  @Test
  public void eventLoopStopsWhenTtlExpiresAllRequests()
  {
    ClockedExecutor executor = new ClockedExecutor();
    ConstantQpsRateLimiter rateLimiter =
        new ConstantQpsRateLimiter(executor, executor, executor, TestEvictingCircularBuffer.getBuffer(executor));

    rateLimiter.setRate(TEST_QPS, ONE_SECOND, UNLIMITED_BURST);
    rateLimiter.setBufferTtl(999, ChronoUnit.MILLIS);
    TattlingCallback<None> tattler = new TattlingCallback<>();
    rateLimiter.submit(tattler);
    executor.runFor(ONE_SECOND * TEST_NUM_CYCLES);
    Assert.assertSame(tattler.getInteractCount(), (int) TEST_QPS);
    long prevTaskCount = executor.getExecutedTaskCount();
    executor.runFor(ONE_SECOND * TEST_NUM_CYCLES);
    // EventLoop continues by scheduling itself at the end. If executed task count remains the same,
    // then EventLoop hasn't re-scheduled itself.
    Assert.assertSame(executor.getExecutedTaskCount(), prevTaskCount);
  }

  private static class TattlingCallback<T> implements Callback<T>
  {
    private  AtomicInteger _interactCount = new AtomicInteger();

    @Override
    public void onError(Throwable e) {}

    @Override
    public void onSuccess(T result) {
      _interactCount.incrementAndGet();
    }

    public int getInteractCount()
    {
      return _interactCount.intValue();
    }
  }
}

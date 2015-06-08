package test.r2.transport.http.client;

import com.linkedin.common.callback.SimpleCallback;
import com.linkedin.r2.transport.http.client.ExponentialBackOffRateLimiter;
import com.linkedin.r2.transport.http.client.RateLimiter;
import com.linkedin.r2.transport.http.client.RateLimiter.Task;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Ang Xu
 * @version $Revision: $
 */
public class TestRateLimiter
{
  private final long MAXPERIOD = 600;

  private ScheduledExecutorService _executor;

  @BeforeClass
  public void setUp()
  {
    _executor = Executors.newSingleThreadScheduledExecutor();
  }

  @AfterClass
  public void tearDown()
  {
    _executor.shutdown();
  }

  @Test
  public void testSimple() throws Exception
  {
    final int total = 10;

    // NB on Solaris x86 there seems to be an extra 10ms that gets added to the period; need
    // to figure this out.  For now set the period high enough that period + 10 will be within
    // the tolerance.
    final int period = 100;

    final CountDownLatch latch = new CountDownLatch(total);
    final Task incr = new Task()
    {
      @Override
      public void run(SimpleCallback doneCallback)
      {
        latch.countDown();
        doneCallback.onDone();
      }
    };


    RateLimiter limiter = new ExponentialBackOffRateLimiter(period, period, period, _executor);
    limiter.setPeriod(period);
    long start = System.currentTimeMillis();
    long lowTolerance = (total * period) * 4 / 5;
    long highTolerance = (total * period) * 5 / 4;
    for (int i = 0; i < total * period; i++)
    {
      limiter.submit(incr);
    }
    Assert.assertTrue(latch.await(highTolerance, TimeUnit.MILLISECONDS),
        "Should have finished within " + highTolerance + "ms");
    long t = System.currentTimeMillis() - start;
    Assert.assertTrue(t > lowTolerance, "Should have finished after " + lowTolerance + "ms (took " + t + ")");
  }

  @Test
  public void testMaxRunningTasks() throws Exception
  {
    final int total = 20;
    final int maxRunning = 5;
    final int period = 100;

    final Random rand = new Random();
    final CountDownLatch latch = new CountDownLatch(total);
    final AtomicInteger totalStarted = new AtomicInteger();
    final AtomicInteger totalFinished = new AtomicInteger();


    final Task r = new Task()
    {
      @Override
      public void run(final SimpleCallback callback)
      {
        totalStarted.incrementAndGet();

        int delay = period + rand.nextInt(period);
        _executor.schedule(new Runnable()
        {
          @Override
          public void run()
          {
            totalFinished.incrementAndGet();
            callback.onDone();
          }
        }, delay, TimeUnit.MILLISECONDS);
        latch.countDown();
      }
    };


    RateLimiter limiter = new ExponentialBackOffRateLimiter(period, period, period, _executor, maxRunning);
    limiter.setPeriod(period);
    for (int i = 0; i < total; ++i)
    {
      limiter.submit(r);
    }

    // check the current number of concurrent tasks every 100ms.
    for (int i = 0; i < total * 2; ++i)
    {
      int currentRunning = totalStarted.get() - totalFinished.get();
      Assert.assertTrue(currentRunning <= maxRunning,
          "Should have less than " + maxRunning + " concurrent tasks");
      Thread.sleep(period);
    }

    Assert.assertTrue(latch.await(30, TimeUnit.SECONDS));
    Assert.assertEquals(total, totalStarted.get());
  }

  @Test
  public void testSetPeriod() throws Exception
  {
    final RateLimiter rl = new ExponentialBackOffRateLimiter(0, MAXPERIOD, 20, _executor);

    final ScheduledFuture<?> future = _executor.scheduleAtFixedRate(new Runnable()
    {
      private int _period = 300;

      @Override
      public void run()
      {
        rl.setPeriod(_period);
        _period += 50;
      }
    }, 0, 100, TimeUnit.MILLISECONDS);

    RateLimiterRunner runner = new RateLimiterRunner(System.currentTimeMillis());
    rl.submit(runner);

    Assert.assertTrue(runner.getElapsedTime() <= MAXPERIOD, "Elapsed Time exceed MAX Period");
    future.cancel(false);
  }

  private class RateLimiterRunner implements Task
  {
    private long _startTime;
    private long _elapsedTime;
    private final CountDownLatch _count;

    public RateLimiterRunner(long start)
    {
      _startTime = start;
      _count = new CountDownLatch(1);
    }

    @Override
    public void run(SimpleCallback callback)
    {
      _elapsedTime = System.currentTimeMillis() - _startTime;
      _count.countDown();
      callback.onDone();
    }

    public long getElapsedTime() throws Exception
    {
      if (!_count.await(MAXPERIOD*2, TimeUnit.MILLISECONDS))
        throw new Exception("CountDownLatch Timeout");
      else
        return _elapsedTime;
    }
  }
}

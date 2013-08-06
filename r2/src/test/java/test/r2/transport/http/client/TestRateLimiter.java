package test.r2.transport.http.client;

import com.linkedin.r2.transport.http.client.RateLimiter;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author Ang Xu
 * @version $Revision: $
 */
public class TestRateLimiter {
  private ScheduledExecutorService _executor = Executors.newSingleThreadScheduledExecutor();
  private final long MAXPERIOD = 600;

  @AfterClass
  public void stopExecutor()
  {
    _executor.shutdown();
  }

  @Test
  public void testSetPeriod() {
    final RateLimiter rl = new RateLimiter(0,
                                     MAXPERIOD,
                                     20,
                                     _executor);


    _executor.scheduleAtFixedRate(new Runnable() {
      private int _period = 300;

      @Override
      public void run() {
        rl.setPeriod(_period);
        _period += 50;
      }
    }, 0, 100, TimeUnit.MILLISECONDS);

    RateLimiterRunner runner = new RateLimiterRunner(System.currentTimeMillis());
    rl.submit(runner);

    try {
      Assert.assertTrue(runner.getElapsedTime() <= MAXPERIOD, "Elapsed Time exceed MAX Period");
    } catch (Exception e) {
      Assert.fail("Unexpected failure", e);
    }
  }

  private class RateLimiterRunner implements Runnable {

    private long _startTime;
    private long _elapsedTime;
    private final CountDownLatch _count;

    public RateLimiterRunner(long start) {
      _startTime = start;
      _count = new CountDownLatch(1);
    }

    @Override
    public void run() {
      _elapsedTime = System.currentTimeMillis() - _startTime;
      _count.countDown();
    }

    public long getElapsedTime() throws Exception {
      if (!_count.await(MAXPERIOD*2, TimeUnit.MILLISECONDS))
        throw new Exception("CountDownLatch Timeout");
      else
        return _elapsedTime;
    }
  }
}

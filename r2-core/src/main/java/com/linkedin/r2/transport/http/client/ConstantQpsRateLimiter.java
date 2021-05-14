package com.linkedin.r2.transport.http.client;

import com.linkedin.r2.transport.http.client.ratelimiter.ConsumptionTracker;
import com.linkedin.util.clock.Clock;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;


public class ConstantQpsRateLimiter extends SmoothRateLimiter {
  private final EvictingCircularBuffer _evictingCircularBuffer;

  public ConstantQpsRateLimiter(
      ScheduledExecutorService scheduler, Executor executor, Clock clock, EvictingCircularBuffer storedCallbacks)
  {
    super(scheduler, executor, clock, storedCallbacks, BufferOverflowMode.NONE, "ConstantQpsRateLimiter", new UnlimitedConsumptionTracker());
    _evictingCircularBuffer = storedCallbacks;
  }

  public void setBufferCapacity(int capacity)
  {
    _evictingCircularBuffer.setCapacity(capacity);
  }

  public void setBufferTtl(int ttl, ChronoUnit ttlUnit)
  {
    _evictingCircularBuffer.setTtl(ttl, ttlUnit);
  }


  private static class UnlimitedConsumptionTracker implements ConsumptionTracker
  {
    private final AtomicBoolean _paused = new AtomicBoolean(true);

    public int getPending()
    {
      return 1;
    }

    public boolean getPausedAndIncrement()
    {
      return _paused.getAndSet(false);
    }

    public boolean decrementAndGetPaused()
    {
      return _paused.get();
    }

    public void pauseConsumption()
    {
      _paused.set(true);
    }

    public boolean isPaused()
    {
      return _paused.get();
    }

    public int getMaxBuffered()
    {
      return Integer.MAX_VALUE;
    }
  }
}
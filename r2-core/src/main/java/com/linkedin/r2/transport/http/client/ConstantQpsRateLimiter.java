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

package com.linkedin.r2.transport.http.client;

import com.linkedin.r2.transport.http.client.ratelimiter.RateLimiterExecutionTracker;
import com.linkedin.util.clock.Clock;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A {@link SmoothRateLimiter} that never rejects new callbacks, and continues to execute callbacks as long as the underlying
 * {@link EvictingCircularBuffer} has callbacks to supply. This rate-limiter should only be used in cases where the user
 * demands a constant rate of callback execution, and it's not important that all callbacks are executed, or executed only once.
 *
 * Rest.li's original use case for this rate-limiter is to supply Dark Clusters with a steady stream of request volume for
 * testing purposes under a given load.
 */
public class ConstantQpsRateLimiter extends SmoothRateLimiter
{
  private final EvictingCircularBuffer _evictingCircularBuffer;

  public ConstantQpsRateLimiter(
      ScheduledExecutorService scheduler, Executor executor, Clock clock, EvictingCircularBuffer callbackBuffer)
  {
    super(scheduler, executor, clock, callbackBuffer, BufferOverflowMode.NONE, "ConstantQpsRateLimiter", new UnboundedRateLimiterExecutionTracker());
    _evictingCircularBuffer = callbackBuffer;
  }

  /**
   * Sets the underlying {@link EvictingCircularBuffer} size, which controls the maximum number of callbacks to store in memory concurrently.
   * @param capacity
   */
  public void setBufferCapacity(int capacity)
  {
    _evictingCircularBuffer.setCapacity(capacity);
  }

  /**
   * Sets the underlying {@link EvictingCircularBuffer} ttl, which controls how long a request can exist in the buffer
   * until it is no longer available.
   * @param ttl
   * @param ttlUnit
   */
  public void setBufferTtl(int ttl, ChronoUnit ttlUnit)
  {
    _evictingCircularBuffer.setTtl(ttl, ttlUnit);
  }


  private static class UnboundedRateLimiterExecutionTracker implements RateLimiterExecutionTracker
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

    public void pauseExecution()
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

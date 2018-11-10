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
import com.linkedin.common.util.None;
import com.linkedin.util.ArgumentUtil;
import com.linkedin.util.clock.Clock;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple {@link AsyncRateLimiter} implementation that re-issues permits at every specified period of time.
 * A submitted callback's #onError is invoked with {@link RejectedExecutionException} if the current buffered
 * callbacks exceeds to the maximum allowed by the implementation.
 *
 * @author Sean Sheng
 */
public class SmoothRateLimiter implements AsyncRateLimiter
{
  private static final Logger LOG = LoggerFactory.getLogger(SmoothRateLimiter.class);

  private final Executor _executor;
  private final ScheduledExecutorService _scheduler;
  private final AtomicReference<Rate> _rate;
  private final EventLoop _eventLoop;
  private final int _maxBuffered;
  private final Queue<Callback<None>> _pendingCallbacks;

  private final AtomicInteger _pendingCount = new AtomicInteger(0);
  private final AtomicReference<Throwable> _invocationError = new AtomicReference<>(null);

  /**
   * Constructs a new instance of {@link SmoothRateLimiter}.
   *
   * @param scheduler Scheduler used to execute the internal non-blocking event loop
   * @param executor Executes the tasks for invoking #onSuccess and #onError (only during #callAll)
   * @param clock Clock implementation that supports getting the current time accurate to milliseconds
   * @param maxBuffered Maximum number of tasks kept in the queue before execution
   * @param permitsPerPeriod Number of permits to run tasks per period of time
   * @param periodMilliseconds Period of time in milliseconds used to calculate {@code permitsPerPeriod}
   * @param burst Maximum number of permits can be issued at a time, see {@link Rate}
   */
  public SmoothRateLimiter(ScheduledExecutorService scheduler, Executor executor, Clock clock,
      Queue<Callback<None>> pendingCallbacks,  int maxBuffered, int permitsPerPeriod, long periodMilliseconds, int burst)
  {
    ArgumentUtil.ensureNotNull(scheduler, "scheduler");
    ArgumentUtil.ensureNotNull(executor, "executor");
    ArgumentUtil.ensureNotNull(clock, "clock");
    ArgumentUtil.checkArgument(maxBuffered >= 0, "maxBuffered");
    ArgumentUtil.checkArgument(permitsPerPeriod > 0, "permitsPerPeriod");
    ArgumentUtil.checkArgument(periodMilliseconds > 0, "periodMilliseconds");
    ArgumentUtil.checkArgument(burst > 0, "burst");

    _scheduler = scheduler;
    _executor = executor;
    _pendingCallbacks = pendingCallbacks;
    _maxBuffered = maxBuffered;

    _rate = new AtomicReference<>(new Rate(permitsPerPeriod, periodMilliseconds, burst));
    _eventLoop = new EventLoop(clock);
  }

  @Override
  public void submit(Callback<None> callback) throws RejectedExecutionException
  {
    ArgumentUtil.ensureNotNull(callback, "callback");

    if (_pendingCount.get() >= _maxBuffered)
    {
      throw new RejectedExecutionException("Cannot submit callback because the buffer is full at " + _maxBuffered);
    }

    _pendingCallbacks.offer(callback);
    if (_pendingCount.getAndIncrement() == 0)
    {
      _scheduler.execute(_eventLoop::loop);
    }
  }

  @Override
  public void setRate(int permitsPerPeriod, long period, int burst)
  {
    ArgumentUtil.checkArgument(permitsPerPeriod > 0, "permitsPerPeriod");
    ArgumentUtil.checkArgument(period > 0, "period");
    ArgumentUtil.checkArgument(burst > 0, "burst");

    _rate.set(new Rate(permitsPerPeriod, period, burst));
  }

  @Override
  public void cancelAll(Throwable throwable)
  {
    ArgumentUtil.ensureNotNull(throwable, "throwable");

    // Sets the invocation error to the given throwable. If there are pending callbacks in the queue,
    // we will invoke #onError to all the left over callbacks with the given throwable
    if (!_invocationError.compareAndSet(null, throwable))
    {
      LOG.error("Method cancelAll should only be invoked once.", new IllegalStateException());
      return;
    }

    // Sets unlimited permits issuance because we do not rate limit invocations of #onError
    _rate.set(Rate.MAX_VALUE);
  }

  /**
   * An immutable implementation of rate as number of events per period of time in milliseconds.
   * In addition, a {@code burst} parameter is used to indicate the maximum number of permits can
   * be issued at a time. To satisfy the burst requirement, {@code period} might adjusted if
   * necessary. The minimal period is one millisecond. If the specified events per period cannot
   * satisfy the burst, an {@link IllegalArgumentException} will be thrown.
   */
  static class Rate
  {
    static final Rate MAX_VALUE = new Rate(Integer.MAX_VALUE, 1, Integer.MAX_VALUE);

    private final int _events;
    private final long _period;

    /**
     * Constructs a new instance of Rate.
     * @param events Number of events per period.
     * @param period Time period length in milliseconds.
     * @param burst Maximum number of events allowed simultaneously.
     */
    Rate(int events, long period, int burst)
    {
      if (burst < events)
      {
        long newPeriod = Math.round(period * burst / (events * 1.0D));
        if (newPeriod == 0)
        {
          String message = String.format(
              "Configured rate of %d events per %d ms cannot satisfy the requirement of %d burst events at a time",
              events, period, burst);
          throw new IllegalArgumentException(message);
        }

        _events = burst;
        _period = newPeriod;
      }
      else
      {
        _events = events;
        _period = period;
      }
    }

    int getEvents() {
      return _events;
    }

    /**
     * Gets period in Milliseconds.
     * @return Period in milliseconds.
     */
    long getPeriod()
    {
      return _period;
    }
  }

  /**
   * A event loop implementation that dispatches and executes {@link Callback}s from the queue based
   * on available permits. If permits are exhausted, the event loop will reschedule itself to run
   * at the next permit issuance time. If the callback queue is exhausted, the event loop will exit
   * and need to be restarted externally.
   *
   * Event loop is meant to be run in a single-threaded setting.
   */
  private class EventLoop
  {
    private final Clock _clock;

    private long _permitTime;
    private int _permitCount;

    EventLoop(Clock clock)
    {
      _clock = clock;
      _permitTime = _clock.currentTimeMillis();
      _permitCount = _rate.get().getEvents();
    }

    public void loop()
    {
      // Checks if permits should be refreshed
      long now = _clock.currentTimeMillis();
      Rate rate = _rate.get();
      if (now - _permitTime >= rate.getPeriod())
      {
        _permitTime = now;
        _permitCount = rate.getEvents();
      }

      int permitCount = _permitCount;
      for (int i = 0; i < permitCount; i++)
      {
        Callback<None> callback = _pendingCallbacks.poll();
        Runnable task = () -> {
          Throwable throwable = _invocationError.get();
          if (throwable == null)
          {
            callback.onSuccess(None.none());
          }
          else
          {
            callback.onError(throwable);
          }
        };
        try
        {
          _executor.execute(task);
        }
        catch (Throwable e)
        {
          // Invoke the callback on the current thread as the last resort. Executing the callback
          // on the current thread also prevents the scheduler from polling another callback while the
          // executor is busy.
          try
          {
            LOG.warn("Task is run on the scheduler thread instead of executor.", e);
            task.run();
          }
          catch (Throwable throwable)
          {
            // Log error because we cannot not recover at this point
            throwable.addSuppressed(e);
            LOG.error("Unexpected exception while executing a callback.", throwable);
            return;
          }
        }
        finally
        {
          _permitCount--;
          if (_pendingCount.decrementAndGet() == 0)
          {
            return;
          }
        }
      }

      // Exhausted all permits in the current time and schedules event loop to run later when
      // more permits are supposed to be issued.
      try
      {
        _scheduler.schedule(this::loop, Math.max(0, _permitTime + rate.getPeriod() - _clock.currentTimeMillis()),
            TimeUnit.MILLISECONDS);
      }
      catch (Throwable throwable)
      {
        LOG.error("Unexpected exception while scheduling the event loop.", throwable);
      }
    }
  }
}

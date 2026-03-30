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
import com.linkedin.r2.transport.http.client.ratelimiter.CallbackBuffer;
import com.linkedin.r2.transport.http.client.ratelimiter.RateLimiterExecutionTracker;
import com.linkedin.r2.transport.http.client.ratelimiter.SimpleCallbackBuffer;
import com.linkedin.r2.transport.http.client.ratelimiter.Rate;
import com.linkedin.util.ArgumentUtil;
import com.linkedin.util.RateLimitedLogger;
import com.linkedin.util.clock.Clock;
import java.util.NoSuchElementException;
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
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class SmoothRateLimiter implements AsyncRateLimiter
{
  private static final Logger LOG = LoggerFactory.getLogger(SmoothRateLimiter.class);
  private static final String RATE_LIMITER_NAME_UNDEFINED = "undefined";

  private final Executor _executor;
  private final ScheduledExecutorService _scheduler;
  private final BufferOverflowMode _bufferOverflowMode;
  private final String _rateLimiterName;
  private volatile Rate _rate = Rate.ZERO_VALUE;
  private final EventLoop _eventLoop;
  private final CallbackBuffer _pendingCallbacks;

  /**
   * Controls the permit-tracking strategy used by the {@link EventLoop}.
   *
   * <p>When {@code true}, the EventLoop uses fractional (double-precision) arithmetic for period
   * tracking and permit accumulation, eliminating millisecond quantization errors at rates where
   * {@code period * burst / events} yields a non-integer millisecond value.</p>
   *
   * <p>When {@code false} (the default), the EventLoop uses integer-rounded values from
   * {@link Rate#getEvents()} and {@link Rate#getPeriod()}.</p>
   */
  private final boolean _enablePrecisePeriodTracking;

  protected final RateLimiterExecutionTracker _executionTracker;
  private final AtomicReference<Throwable> _invocationError = new AtomicReference<>(null);

  private final static Long OVER_BUFFER_RATELIMITEDLOG_RATE_MS = 60000L;
  private final Logger _rateLimitedLoggerOverBuffer;

  public enum BufferOverflowMode
  {
    /**
     * Drop the request with RejectedException
     */
    DROP,
    /**
     * Enqueue the request and run at least one to avoid the overflow
     */
    SCHEDULE_WITH_WARNING,
    /**
     * Used for buffers that cannot overflow
     */
    NONE
  }

  public SmoothRateLimiter(ScheduledExecutorService scheduler, Executor executor, Clock clock, Queue<Callback<None>> pendingCallbacks,
                           int maxBuffered, BufferOverflowMode bufferOverflowMode, String rateLimiterName)
  {
    this(scheduler, executor, clock, pendingCallbacks, maxBuffered, bufferOverflowMode, rateLimiterName, false);
  }

  public SmoothRateLimiter(ScheduledExecutorService scheduler, Executor executor, Clock clock, Queue<Callback<None>> pendingCallbacks,
                           int maxBuffered, BufferOverflowMode bufferOverflowMode, String rateLimiterName,
                           boolean enablePrecisePeriodTracking)
  {
    this(scheduler, executor, clock, new SimpleCallbackBuffer(pendingCallbacks), bufferOverflowMode, rateLimiterName,
        new BoundedRateLimiterExecutionTracker(maxBuffered), enablePrecisePeriodTracking);
  }

  /**
   * Constructs a new instance of {@link SmoothRateLimiter}.
   * The default rate is 0, no requests will be processed until the rate is changed
   *
   * @param scheduler          Scheduler used to execute the internal non-blocking event loop. MUST be single-threaded
   * @param executor           Executes the tasks for invoking #onSuccess and #onError (only during #callAll)
   * @param clock              Clock implementation that supports getting the current time accurate to milliseconds
   * @param pendingCallbacks   THREAD SAFE and NON-BLOCKING implementation of callback queue
   * @param bufferOverflowMode just what to do if the max buffer is reached. In many applications blindly
   *                           dropping the request might not be backward compatible
   * @param rateLimiterName    Name assigned for logging purposes
   * @param executionTracker   Adjusts the behavior of the rate limiter based on policies/state of RateLimiterExecutionTracker
   */
  SmoothRateLimiter(ScheduledExecutorService scheduler, Executor executor, Clock clock, CallbackBuffer pendingCallbacks,
                    BufferOverflowMode bufferOverflowMode, String rateLimiterName, RateLimiterExecutionTracker executionTracker)
  {
    this(scheduler, executor, clock, pendingCallbacks, bufferOverflowMode, rateLimiterName, executionTracker, false);
  }

  /**
   * Constructs a new instance of {@link SmoothRateLimiter} with an option to enable precise
   * period tracking for improved rate accuracy at sub-millisecond periods.
   *
   * @param scheduler              Scheduler used to execute the internal non-blocking event loop. MUST be single-threaded
   * @param executor               Executes the tasks for invoking #onSuccess and #onError (only during #callAll)
   * @param clock                  Clock implementation that supports getting the current time accurate to milliseconds
   * @param pendingCallbacks       THREAD SAFE and NON-BLOCKING implementation of callback queue
   * @param bufferOverflowMode     What to do if the max buffer is reached
   * @param rateLimiterName        Name assigned for logging purposes
   * @param executionTracker       Adjusts the behavior of the rate limiter based on policies/state of RateLimiterExecutionTracker
   * @param enablePrecisePeriodTracking   When {@code true}, uses double-precision period and permit tracking
   *                               to eliminate millisecond quantization errors. When {@code false}, uses
   *                               integer-rounded period and permit values. Defaults to {@code false}.
   */
  SmoothRateLimiter(ScheduledExecutorService scheduler, Executor executor, Clock clock, CallbackBuffer pendingCallbacks,
                    BufferOverflowMode bufferOverflowMode, String rateLimiterName, RateLimiterExecutionTracker executionTracker,
                    boolean enablePrecisePeriodTracking)
  {
    ArgumentUtil.ensureNotNull(scheduler, "scheduler");
    ArgumentUtil.ensureNotNull(executor, "executor");
    ArgumentUtil.ensureNotNull(clock, "clock");

    _scheduler = scheduler;
    _executor = executor;
    _pendingCallbacks = pendingCallbacks;
    _bufferOverflowMode = bufferOverflowMode;
    _rateLimiterName = rateLimiterName;
    _executionTracker = executionTracker;
    _enablePrecisePeriodTracking = enablePrecisePeriodTracking;

    _eventLoop = new EventLoop(clock);
    _rateLimitedLoggerOverBuffer = new RateLimitedLogger(LOG, OVER_BUFFER_RATELIMITEDLOG_RATE_MS, clock);
  }

  @Deprecated
  public SmoothRateLimiter(ScheduledExecutorService scheduler, Executor executor, Clock clock, Queue<Callback<None>> pendingCallbacks,
                           int maxBuffered)
  {
    this(scheduler, executor, clock, pendingCallbacks, maxBuffered, BufferOverflowMode.DROP, RATE_LIMITER_NAME_UNDEFINED);
  }

  /**
   * @deprecated use SmoothRateLimiter + setRate instead
   */
  @Deprecated
  public SmoothRateLimiter(ScheduledExecutorService scheduler, Executor executor, Clock clock, Queue<Callback<None>> pendingCallbacks,
                           int maxBuffered, int permitsPerPeriod, long periodMilliseconds, int burst)
  {
    this(scheduler, executor, clock, pendingCallbacks, maxBuffered);

    ArgumentUtil.checkArgument(permitsPerPeriod >= 0, "permitsPerPeriod");
    ArgumentUtil.checkArgument(periodMilliseconds > 0, "periodMilliseconds");
    ArgumentUtil.checkArgument(burst > 0, "burst");
    setRate((double) permitsPerPeriod, periodMilliseconds, burst);
  }

  @Override
  public void submit(Callback<None> callback) throws RejectedExecutionException
  {
    ArgumentUtil.ensureNotNull(callback, "callback");

    if (_executionTracker.getPending() >= _executionTracker.getMaxBuffered())
    {
      if (_bufferOverflowMode == BufferOverflowMode.DROP)
      {
        throw new RejectedExecutionException(
            String.format("PEGA_2000: Cannot submit callback because the buffer is full at %d tasks for ratelimiter: %s",
                _executionTracker.getMaxBuffered(), _rateLimiterName));
      }
      else
      {
        _rateLimitedLoggerOverBuffer.error(String.format(
            "PEGA_2001: the buffer is full at %d tasks for ratelimiter: %s. Executing a request immediately to avoid overflowing and dropping the task.",
            _executionTracker.getMaxBuffered(), _rateLimiterName));
      }
    }

    _pendingCallbacks.put(callback);
    if (_executionTracker.getPausedAndIncrement())
    {
      _scheduler.execute(_eventLoop::loop);
    }
  }

  @Override
  public Rate getRate()
  {
    return _rate;
  }

  @Override
  public void setRate(double permitsPerPeriod, long periodMilliseconds, int burst)
  {
    ArgumentUtil.checkArgument(permitsPerPeriod >= 0, "permitsPerPeriod");
    ArgumentUtil.checkArgument(periodMilliseconds > 0, "periodMilliseconds");
    ArgumentUtil.checkArgument(burst > 0, "burst");

    Rate newRate = new Rate(permitsPerPeriod, periodMilliseconds, burst);
    if (!_rate.equals(newRate))
    {
      _rate = newRate;
      _scheduler.execute(_eventLoop::updateWithNewRate);
    }
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
    setRate(Rate.MAX_VALUE.getEventsRaw(), Rate.MAX_VALUE.getPeriod(), Rate.MAX_VALUE.getEvents());
  }

  @Override
  public int getPendingTasksCount(){
    return _executionTracker.getPending();
  }

  /**
   * A single-threaded event loop that dispatches {@link Callback}s from the pending queue at
   * the configured rate. Supports two permit-tracking strategies controlled by
   * {@link #_enablePrecisePeriodTracking}:
   *
   * <ul>
   *   <li><b>Classic mode</b> ({@code enablePrecisePeriodTracking=false}, the default) &mdash;
   *       period and event counts come from {@link Rate#getPeriod()} / {@link Rate#getEvents()}
   *       (integer-rounded). The period boundary is snapped to {@code now} on each refresh.</li>
   *   <li><b>Precise mode</b> ({@code enablePrecisePeriodTracking=true}) &mdash;
   *       period and event counts come from {@link Rate#getPeriodRaw()} / {@link Rate#getEventsRaw()}
   *       (double-precision). The period boundary advances by the exact fractional period so
   *       sub-millisecond remainders accumulate across refreshes. For example, at 750&nbsp;QPS
   *       with burst=1 the period is 1.333&thinsp;ms and the boundary advances as
   *       0&nbsp;&rarr;&nbsp;1.333&nbsp;&rarr;&nbsp;2.666&nbsp;&rarr;&nbsp;4.0, producing exactly
   *       3&nbsp;dispatches per 4&thinsp;ms (=&nbsp;750/s).</li>
   * </ul>
   *
   * <p>If permits are exhausted the loop reschedules itself for the next period boundary.
   * If the callback queue is drained the loop exits and is restarted by
   * {@link #submit(Callback)}. If the buffer exceeds the configured max, at least one dispatch
   * is forced to prevent a leak.</p>
   */
  private class EventLoop
  {
    private final Clock _clock;

    /** Period boundary timestamp; advanced each refresh (fractional in fractional mode). */
    private double _permitTime;

    /** Permits remaining in the current period. */
    private double _permitAvailableCount;

    /** Total permits granted at the start of the current period (used to track consumption across rate changes). */
    private double _permitsInTimeFrame;

    /** Absolute time of the next already-scheduled tick (prevents duplicate scheduling). */
    private long _nextScheduled;

    /** Earliest wall-clock time at which the next dispatch is allowed (execution-delay support). */
    private long _delayUntil;

    EventLoop(Clock clock)
    {
      _clock = clock;
      _permitTime = _clock.currentTimeMillis();
      Rate rate = _rate;
      _permitAvailableCount = _enablePrecisePeriodTracking ? rate.getEventsRaw() : rate.getEvents();
      _permitsInTimeFrame = _enablePrecisePeriodTracking ? rate.getEventsRaw() : rate.getEvents();
    }

    /**
     * Called when the rate changes via {@link #setRate}. Preserves the number of permits already
     * consumed in the current period so the transition does not over- or under-grant.
     */
    private void updateWithNewRate()
    {
      Rate rate = _rate;

      double events = _enablePrecisePeriodTracking ? rate.getEventsRaw() : rate.getEvents();
      double consumed = _permitsInTimeFrame - _permitAvailableCount;
      _permitAvailableCount = Math.max(events - consumed, 0);
      _permitsInTimeFrame = events;
      long now = _clock.currentTimeMillis();
      long timeSinceLastPermit = now - (long) _permitTime;
      _delayUntil = now + Math.max(0, (_executionTracker.getNextExecutionDelay(_rate) - timeSinceLastPermit));

      loop();
    }

    public void loop()
    {
      long now = _clock.currentTimeMillis();
      Rate rate = _rate;

      double period = _enablePrecisePeriodTracking ? rate.getPeriodRaw() : rate.getPeriod();
      double events = _enablePrecisePeriodTracking ? rate.getEventsRaw() : rate.getEvents();

      // Refresh permits when the current period has elapsed.
      if (now - _permitTime >= period)
      {
        if (_enablePrecisePeriodTracking)
        {
          // Advance by exact fractional period so sub-millisecond remainders accumulate.
          _permitTime += period;
        }
        else
        {
          // Classic: snap to wall-clock time.
          _permitTime = now;
        }
        _permitAvailableCount = events;
        _permitsInTimeFrame = events;
        _delayUntil = now + _executionTracker.getNextExecutionDelay(_rate);
      }

      if (_executionTracker.isPaused())
      {
        return;
      }

      // Buffer overflow safeguard: force at least one dispatch to avoid a leak.
      if (_executionTracker.getPending() > _executionTracker.getMaxBuffered())
      {
        if (_enablePrecisePeriodTracking)
        {
          _permitAvailableCount = Math.max(_permitAvailableCount, 1.0);
        }
        else
        {
          _permitAvailableCount++;
        }
      }

      if (_permitAvailableCount > 0 && _delayUntil <= now)
      {
        _delayUntil = now + _executionTracker.getNextExecutionDelay(_rate);
        _permitAvailableCount--;
        Callback<None> callback = null;
        try
        {
          callback = _pendingCallbacks.get();
          _executor.execute(new Task(callback, _invocationError.get()));
        }
        catch (NoSuchElementException ex)
        {
          _executionTracker.pauseExecution();
        }
        catch (Throwable e)
        {
          // Last resort: invoke onError on the scheduler thread to avoid losing the callback
          if (callback == null)
          {
            LOG.error("Unrecoverable exception occurred while executing a null callback in executor.", e);
          }
          else
          {
            LOG.warn("Unexpected exception while executing a callback in executor. Invoking callback with scheduler.", e);
            callback.onError(e);
          }
        }
        finally
        {
          if (!_executionTracker.decrementAndGetPaused())
          {
            _scheduler.execute(this::loop);
          }
        }
      }
      else
      {
        try
        {
          // Reschedule: either wait for the execution delay or the next period boundary.
          long nextRunRelativeTime;
          if (_permitAvailableCount > 0)
          {
            nextRunRelativeTime = _delayUntil - now;
          }
          else if (_enablePrecisePeriodTracking)
          {
            // Round up so the scheduler never fires before the fractional boundary.
            nextRunRelativeTime = Math.max(1, (long) Math.ceil(_permitTime + period - now));
          }
          else
          {
            nextRunRelativeTime = Math.max(0, (long) (_permitTime + period) - now);
          }
          long nextRunAbsolute = now + nextRunRelativeTime;
          if (_nextScheduled > nextRunAbsolute || _nextScheduled <= now)
          {
            _nextScheduled = nextRunAbsolute;
            _scheduler.schedule(this::loop, nextRunRelativeTime, TimeUnit.MILLISECONDS);
          }
        }
        catch (Throwable throwable)
        {
          LOG.error("An unrecoverable exception occurred while scheduling the event loop causing the rate limiter"
              + "to stop processing submitted tasks.", throwable);
        }
      }
    }
  }

  /**
   * An implementation of {@link Runnable} that invokes the given {@link Callback}. If a
   * {@link Throwable} is provided, Callback#onError is invoked. Otherwise, Callback#onSuccess
   * is invoked.
   */
  private static class Task implements Runnable
  {
    private final Callback<None> _callback;
    private final Throwable _invocationError;

    public Task(Callback<None> callback, Throwable invocationError)
    {
      ArgumentUtil.notNull(callback, "callback");

      _callback = callback;
      _invocationError = invocationError;
    }

    @Override
    public void run()
    {
      try
      {
        if (_invocationError == null)
        {
          _callback.onSuccess(None.none());
        }
        else
        {
          _callback.onError(_invocationError);
        }
      }
      catch (Throwable throwable)
      {
        _callback.onError(throwable);
      }
    }
  }

  private static class BoundedRateLimiterExecutionTracker implements RateLimiterExecutionTracker
  {
    private final AtomicInteger _pendingCount = new AtomicInteger(0);
    private final int _maxBuffered;

    public BoundedRateLimiterExecutionTracker(int maxBuffered)
    {
      ArgumentUtil.checkArgument(maxBuffered >= 0, "maxBuffered");

      _maxBuffered = maxBuffered;
    }

    @Override
    public void stopExecution()
    {
      LOG.warn("Method stopExecution is not implemented in BoundedRateLimiterExecutionTracker.");
    }

    public boolean getPausedAndIncrement()
    {
      return _pendingCount.getAndIncrement() == 0;
    }

    public boolean decrementAndGetPaused()
    {
      return _pendingCount.updateAndGet(i -> i > 0 ? i - 1 : i) == 0;
    }

    public boolean isPaused()
    {
      // if all the tasks have been previously consumed, there is no need for continuing execution
      return _pendingCount.get() == 0;
    }

    public void pauseExecution()
    {
      _pendingCount.set(0);
    }

    public int getPending()
    {
      return _pendingCount.get();
    }

    public int getMaxBuffered()
    {
      return _maxBuffered;
    }
  }
}

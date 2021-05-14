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

package com.linkedin.darkcluster.util;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.AsyncRateLimiter;
import com.linkedin.r2.transport.http.client.ratelimiter.Rate;
import com.linkedin.util.ArgumentUtil;
import com.linkedin.util.clock.Clock;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple {@link AsyncRateLimiter} implementation that re-issues permits at every specified period of time.
 * A submitted callback's #onError is invoked with {@link RejectedExecutionException} if the current buffered
 * callbacks exceeds to the maximum allowed by the implementation. Based heavily on SmoothRateLimiter by Sean Sheng.
 *
 * @author Lester Haynes
 * @author Sean Sheng
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class GuaranteedRateLimiter implements AsyncRateLimiter
{
  private static final Logger LOG = LoggerFactory.getLogger(GuaranteedRateLimiter.class);

  private final Executor _executor;
  private final ScheduledExecutorService _scheduler;
  private volatile Rate _rate = Rate.ZERO_VALUE;
  private final EventLoop _eventLoop;
  private final ExpiringCircularBuffer<Callback<None>> _storedCallbacks;

  private final AtomicReference<Throwable> _invocationError = new AtomicReference<>(null);

  /**
   * Constructs a new instance of {@link GuaranteedRateLimiter}.
   * The default rate is 0, no requests will be processed until the rate is changed
   *
   * @param scheduler        Scheduler used to execute the internal non-blocking event loop. MUST be single-threaded
   * @param executor         Executes the tasks for invoking #onSuccess and #onError (only during #callAll)
   * @param clock            Clock implementation that supports getting the current time accurate to milliseconds
   * @param storedCallbacks  ExpiringCircularBuffer in which to cache requests for a stable guaranteed rate of replay
   */
  public GuaranteedRateLimiter(@Nonnull ScheduledExecutorService scheduler,
                               @Nonnull Executor executor,
                               @Nonnull Clock clock,
                               @Nonnull ExpiringCircularBuffer<Callback<None>> storedCallbacks)
  {
    _scheduler = scheduler;
    _executor = executor;
    _storedCallbacks = storedCallbacks;
    _eventLoop = new EventLoop(clock);
  }

  @Override
  public void submit(Callback<None> callback)
  {
    ArgumentUtil.ensureNotNull(callback, "callback");

    _storedCallbacks.offer(callback);

    if (!_eventLoop.isRunning())
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

    _rate = new Rate(permitsPerPeriod, periodMilliseconds, burst);
    _scheduler.execute(_eventLoop::updateWithNewRate);
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

  /**
   * A event loop implementation that dispatches and executes {@link Callback}s from the queue based
   * on available permits. If permits are exhausted, the event loop will reschedule itself to run
   * at the next permit issuance time. If the callback queue is exhausted, the event loop will exit
   * and need to be restarted externally.
   *
   * If there are more tasks than the max in the buffer, they'll be immediately executed to align with the limit
   * <p>
   * Event loop is meant to be run in a single-threaded setting.
   */
  private class EventLoop
  {
    private final Clock _clock;

    private long _permitTime;
    private int _permitAvailableCount;
    private int _permitsInTimeFrame;
    private long _nextScheduled;
    private boolean _running;

    EventLoop(Clock clock)
    {
      _clock = clock;
      _permitTime = _clock.currentTimeMillis();
      Rate rate = _rate;
      _permitAvailableCount = rate.getEvents();
      _permitsInTimeFrame = rate.getEvents();
    }

    private void updateWithNewRate()
    {
      Rate rate = _rate;

      // if we already used some permits in the current period, we want to use just the possible remaining ones
      // before entering the next period
      _permitAvailableCount = Math.max(rate.getEvents() - (_permitsInTimeFrame - _permitAvailableCount), 0);
      _permitsInTimeFrame = rate.getEvents();

      loop();
    }

    public void loop()
    {
      // Checks if permits should be refreshed
      long now = _clock.currentTimeMillis();
      Rate rate = _rate;
      _running = true;
      if (now - _permitTime >= rate.getPeriod())
      {
        _permitTime = now;
        _permitAvailableCount = rate.getEvents();
        _permitsInTimeFrame = rate.getEvents();
      }

      if (_permitAvailableCount > 0)
      {
        _permitAvailableCount--;
        Callback<None> callback = null;
        try
        {
          callback = _storedCallbacks.element();
          _executor.execute(new Task(callback, _invocationError.get()));
        }
        catch (NoSuchElementException e)
        {
          _running = false;
        }
        catch (Throwable e)
        {
          // Invoke the callback#onError on the current thread as the last resort. Executing the callback on the
          // current thread also prevents the scheduler from polling another callback while the executor is busy.
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
        finally {
          if (_running)
          {
            _scheduler.execute(this::loop);
          }
        }
      }
      else
      {
        try
        {
          // avoids executing too many duplicate tasks
          long nextRunRelativeTime = Math.max(0, _permitTime + rate.getPeriod() - _clock.currentTimeMillis());
          long nextRunAbsolute = _clock.currentTimeMillis() + nextRunRelativeTime;
          if (_nextScheduled > nextRunAbsolute || _nextScheduled <= _clock.currentTimeMillis())
          {
            _nextScheduled = nextRunAbsolute;

            _scheduler.schedule(this::loop, nextRunRelativeTime,
                TimeUnit.MILLISECONDS);
          }
        }
        catch (Throwable throwable)
        {
          LOG.error("An unrecoverable exception occurred while scheduling the event loop causing the rate limiter"
              + "to stop processing submitted tasks.", throwable);
        }
      }
    }

    public boolean isRunning()
    {
      return _running;
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
}

/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.r2.transport.http.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.common.callback.SimpleCallback;

/**
* @author Steven Ihde
* @version $Revision: $
*/
public class ExponentialBackOffRateLimiter implements RateLimiter
{
  private static final Logger LOG = LoggerFactory.getLogger(ExponentialBackOffRateLimiter.class);
  private final ScheduledExecutorService _executor;
  private final long _minPeriod;
  private final long _initialIncrement;
  private final long _maxPeriod;
  private final int _maxRunningTasks;
  private final Queue<Task> _pending = new LinkedList<Task>();
  private long _period;
  private int  _runningTasks;
  private ScheduledFuture<?> _task;

  private final SimpleCallback _doneCallback = new SimpleCallback()
  {
    @Override
    public void onDone()
    {
      synchronized (ExponentialBackOffRateLimiter.this)
      {
        _runningTasks --;
        schedule();
      }
    }
  };

  private final Runnable _doit = new Runnable()
  {
    @Override
    public void run()
    {
      Task t = null;
      synchronized (ExponentialBackOffRateLimiter.this)
      {
        _task = null;
        if (_runningTasks < _maxRunningTasks && !_pending.isEmpty())
        {
          _runningTasks ++;
          t = _pending.poll();
        }
        schedule();
      }
      if (t != null)
      {
        try
        {
          if (LOG.isDebugEnabled())
          {
            LOG.debug("Running rate limited task at {} with period {}", System.currentTimeMillis(), _period);
          }
          t.run(_doneCallback);
        }
        catch (Exception e)
        {
          LOG.error("Uncaught exception while running rate-limited task", e);
        }
      }
    }
  };

  /**
   * Construct a new instance using the specified parameters.
   *
   * @param minPeriod minimum period.
   * @param maxPeriod maximum period.
   * @param initialIncrement initial increment.
   * @param executor executor pool.
   */
  public ExponentialBackOffRateLimiter(long minPeriod, long maxPeriod, long initialIncrement,
      ScheduledExecutorService executor)
  {
    this(minPeriod, maxPeriod, initialIncrement, executor, Integer.MAX_VALUE);
  }

  /**
   * Construct a new instance using the specified parameters.
   *
   * @param minPeriod minimum period.
   * @param maxPeriod maximum period.
   * @param initialIncrement initial increment.
   * @param executor executor pool.
   * @param maxRunningTasks maximum number of tasks the ExponentialBackOffRateLimiter is allowed to run concurrently.
   */
  public ExponentialBackOffRateLimiter(long minPeriod, long maxPeriod, long initialIncrement,
      ScheduledExecutorService executor, int maxRunningTasks)
  {
    _minPeriod = minPeriod;
    _maxPeriod = maxPeriod;
    _initialIncrement = initialIncrement;
    _executor = executor;
    _maxRunningTasks = maxRunningTasks;
  }

  public void setPeriod(long ms)
  {
    Long previous = null;
    ms = Math.min(_maxPeriod, Math.max(_minPeriod, ms));
    synchronized (this)
    {
      if (ms != _period)
      {
        previous = _period;
        _period = ms;
        if (!_pending.isEmpty() && (_task == null || _task.cancel(false)))
        {
          long adjustedPeriod = _period;
          if (_task != null) {
            long elapsedTime = previous - _task.getDelay(TimeUnit.MILLISECONDS);
            adjustedPeriod = Math.max(_period - elapsedTime, 0);
            _task = null;
          }
          schedule(adjustedPeriod);
        }
      }
    }
    if (previous != null)
    {
      // Use previous to avoid logging in the synchronized block
      LOG.debug("Minimum period changed from {} to {}", previous, ms);
    }
  }

  /**
   * Increment the period.
   */
  public void incrementPeriod()
  {
    synchronized (this)
    {
      setPeriod(Math.min(_maxPeriod, (_period == 0 ? _initialIncrement : _period * 2)));
    }
  }

  @Override
  public void submit(Task t)
  {
    boolean runNow = false;
    synchronized (this)
    {
      if (_period == 0 && _pending.isEmpty() && _runningTasks < _maxRunningTasks)
      {
        _runningTasks ++;
        runNow = true;
      }
      else
      {
        _pending.add(t);
        schedule();
      }
    }

    if (runNow)
    {
      t.run(_doneCallback);
    }
  }

  @Override
  public Collection<Task> cancelPendingTasks()
  {
    synchronized (this)
    {
      Collection<Task> cancelled = new ArrayList<Task>(_pending.size());
      for (Task item; (item = _pending.poll()) != null;)
      {
        cancelled.add(item);
      }
      return cancelled;
    }
  }

  /**
   * Schedule a rate-limit task if necessary. Lock must be acquired before calling this method!
   */
  private void schedule()
  {
    schedule(_period);
  }

  /**
   * Schedule a rate-limit task if necessary. Lock must be acquired before calling this method!
   *
   * @param delay  time to delay before running the next rate-limit task.
   *
   */
  private void schedule(long delay)
  {
    if (_runningTasks < _maxRunningTasks && !_pending.isEmpty()
          && _task == null)
    {
      _task = _executor.schedule(_doit, delay, TimeUnit.MILLISECONDS);
    }
  }


}

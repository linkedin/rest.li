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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author Steven Ihde
* @version $Revision: $
*/
public class RateLimiter
{
  private static final Logger LOG = LoggerFactory.getLogger(RateLimiter.class);
  private final ScheduledExecutorService _executor;
  private final long _minPeriod;
  private final long _initialIncrement;
  private final long _maxPeriod;
  private final Queue<Runnable> _pending = new LinkedList<Runnable>();
  private long _period;
  private ScheduledFuture _task;

  private final Runnable _doit = new Runnable()
  {
    @Override
    public void run()
    {
      Runnable r;
      synchronized (RateLimiter.this)
      {
        r = _pending.poll();
        if (_pending.isEmpty())
        {
          _task = null;
        }
        else
        {
          _task = _executor.schedule(_doit, _period, TimeUnit.MILLISECONDS);
        }
      }
      if (r != null)
      {
        try
        {
          if (LOG.isDebugEnabled())
          {
            LOG.debug("Running rate limited task at {} with period {}", System.currentTimeMillis(), _period);
          }
          r.run();
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
  public RateLimiter(long minPeriod,
                     long maxPeriod,
                     long initialIncrement,
                     ScheduledExecutorService executor)
  {
    _minPeriod = minPeriod;
    _maxPeriod = maxPeriod;
    _initialIncrement = initialIncrement;
    _executor = executor;
  }

  /**
   * Set the period.
   *
   * @param ms New value for the period, in milliseconds.
   */
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
          _task = _executor.schedule(_doit, _period, TimeUnit.MILLISECONDS);
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

  /**
   * Submit a new {@link Runnable}.
   *
   * @param r the {@link Runnable} to be executed.
   */
  public void submit(Runnable r)
  {
    boolean runNow = false;
    synchronized (this)
    {
      if (_period == 0 && _pending.isEmpty())
      {
        runNow = true;
      }
      else
      {
        _pending.add(r);
        if (_task == null)
        {
          _task = _executor.schedule(_doit, _period, TimeUnit.MILLISECONDS);
        }
      }
    }

    if (runNow)
    {
      r.run();
    }
  }
}

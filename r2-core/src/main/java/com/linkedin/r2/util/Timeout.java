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

package com.linkedin.r2.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A timeout that stores a reference.  If the reference is not retrieved within
 * the specified timeout, any timeout tasks specified by {@link #addTimeoutTask(Runnable)}
 * are executed.
 *
 * @author Steven Ihde
 * @version $Revision: $
 */

public class Timeout<T> implements TimeoutExecutor
{
  private static final Logger LOG = LoggerFactory.getLogger(Timeout.class);

  private final AtomicReference<T> _item;
  private final ScheduledFuture<?> _future;
  private final ClosableQueue<Runnable> _queue = new ClosableQueue<Runnable>();

  /**
   * Construct a new instance with the specified parameters.
   *
   * @param executor the {@link ScheduledExecutorService} to use for scheduling the timeout task
   * @param timeout the timeout delay, in the specified {@link TimeUnit}.
   * @param timeoutUnit the {@link TimeUnit} for the timeout parameter.
   * @param item the item to be retrieved.
   */
  public Timeout(ScheduledExecutorService executor, long timeout, TimeUnit timeoutUnit, T item)
  {
    if (item == null)
    {
      throw new NullPointerException();
    }
    _item = new AtomicReference<T>(item);
    _future = executor.schedule(new Runnable()
    {
      @Override
      public void run()
      {
        T item = _item.getAndSet(null);
        if (item != null)
        {
          List<Runnable> actions = _queue.close();
          if (actions.isEmpty())
          {
            LOG.warn("Timeout elapsed but no action was specified");
          }
          for (Runnable action : actions)
          {
            try
            {
              action.run();
            }
            catch (Exception e)
            {
              LOG.error("Failed to execute timeout action", e);
            }
          }
        }
      }
    }, timeout, timeoutUnit);
  }

  /**
   * Obtain the item from this Timeout instance.
   *
   * @return the item held by this Timeout, or null if the item has already been retrieved.
   */
  public T getItem()
  {
    T item = _item.getAndSet(null);
    if (item != null)
    {
      _future.cancel(false);
    }
    return item;
  }

  @Override
  public void addTimeoutTask(Runnable action)
  {
    if (!_queue.offer(action))
    {
      // If the queue was closed, the timeout has already occurred.
      action.run();
    }
  }
}

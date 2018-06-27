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

package com.linkedin.r2.util;

import com.linkedin.util.ArgumentUtil;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A timeout that stores a reference of an object and the action that must be executed if the reference
 * is not retrieved within the specified timeout.
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */

public class SingleTimeout<T>
{
  private static final Logger LOG = LoggerFactory.getLogger(SingleTimeout.class);

  private final AtomicReference<T> _item;
  private final ScheduledFuture<?> _future;

  /**
   * Construct a new instance with the specified parameters.
   *
   * @param executor the {@link ScheduledExecutorService} to use for scheduling the timeout task
   * @param timeout the timeout delay, in the specified {@link TimeUnit}.
   * @param timeoutUnit the {@link TimeUnit} for the timeout parameter.
   * @param item the item to be retrieved.
   * @param timeoutAction the action to be executed in case of timeout.
   */
  public SingleTimeout(ScheduledExecutorService executor, long timeout, TimeUnit timeoutUnit, T item, Runnable timeoutAction)
  {
    ArgumentUtil.ensureNotNull(item,"item");
    ArgumentUtil.ensureNotNull(timeoutAction,"timeoutAction");

    _item = new AtomicReference<>(item);
    _future = executor.schedule(() -> {
      T item1 = _item.getAndSet(null);
      if (item1 != null)
      {
        try
        {
          timeoutAction.run();
        } catch (Throwable e)
        {
          LOG.error("Failed to execute timeout action", e);
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
}

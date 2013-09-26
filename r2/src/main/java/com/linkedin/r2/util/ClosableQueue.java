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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides a mechanism to accumulate objects in a queue until the queue is closed,
 * at which point all the enqueued objects can be retrieved and no further objects can be
 * enqueued.
 *
 * The queue is optimized for the use case that it will be closed most of the time, so
 * testing whether the queue is closed or not is lock-free.  Specifically, calling
 * {@link #offer(Object)} after the queue is closed only reads a single atomic integer.
 *
 * A typical use is to record a list of waiters for some event; the queue is closed
 * when the event occurs.  At that point the waiters can be notified of the event,
 * and subsequent attempts to enqueue return false, indicating the event has already occurred.
 *
 * @author Steven Ihde
 * @version $Revision: $
 */
public class ClosableQueue<T>
{
  /**
   * -1: queue closed.
   * >= 0: queue still open, _count objects enqueued.
   */
  private final AtomicInteger _count = new AtomicInteger(0);

  // Could consider changing this to a ConcurrentLinkedQueue, and using a spin loop
  // in close instead of blocking via take().
  private final BlockingQueue<T> _queue = new LinkedBlockingQueue<T>();
  private final AtomicBoolean _closing = new AtomicBoolean(false);

  /**
   * Enqueues an object into the queue, if it is still open.
   *
   * @param obj the object to enqueue
   * @return true if the object was enqueued, false if the queue was closed
   */
  public boolean offer(T obj)
  {
    int count;
    while ((count = _count.get()) >= 0)
    {
      if (_count.compareAndSet(count, count + 1))
      {
        _queue.add(obj);
        return true;
      }
    }

    return false;
  }

  /**
   * Closes the queue and returns the items enqueued prior to closure.
   * No more items can be enqueued after calling this method, and offer() will return false.
   * It is the caller's responsibility to ensure that close() is called at most once
   *
   * @return the items enqueued prior to closure
   * @throws IllegalStateException if close() has been previously called
   */
  public List<T> close()
  {
    List<T> queue = ensureClosed();
    if (queue == null)
    {
      throw new IllegalStateException("Queue is already closed");
    }
    return queue;
  }

  public List<T> ensureClosed()
  {
    if (!_closing.compareAndSet(false, true))
    {
      return null;
    }
    boolean interrupted = false;
    int count = _count.get();
    List<T> members = new ArrayList<T>(count);
    while (count >= 0)
    {
      if (_count.compareAndSet(count, count - 1))
      {
        if (count-- > 0)
        {
          T removed = null;
          while (removed == null)
          {
            try
            {
              removed = _queue.take();
            }
            catch (InterruptedException e)
            {
              interrupted = true;
            }
          }
          members.add(removed);
        }
      }
      else
      {
        count = _count.get();
      }
    }
    if (interrupted)
    {
      Thread.currentThread().interrupt();
    }
    return members;
  }

  /**
   * Return true iff the queue is already closed.
   *
   * @return true iff the queue is already closed.
   */
  public boolean isClosed()
  {
    return _count.get() == -1;
  }
}

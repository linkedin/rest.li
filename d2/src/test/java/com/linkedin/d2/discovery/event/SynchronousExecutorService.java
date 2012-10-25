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

package com.linkedin.d2.discovery.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Dummy executor service whose purpose is to synchronously process a runnable in the current thread
 * so this will make the test easy to manage
 *
 * @author Oby Sumampouw <osumampouw@linkedin.com>
 */

public class SynchronousExecutorService extends AbstractExecutorService implements ScheduledExecutorService
{

  boolean _isShutDown = false;

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
  {
    synchronized (this)
    {
      if (delay < 0)
      {
        throw new IllegalArgumentException("Cannot use delay less than 0. Delay = " + delay);
      }

      if (!_isShutDown)
      {
        if (delay == 0)
        {
          command.run();
          return null;
        }
        try
        {
          if (TimeUnit.DAYS == unit)
          {
            delay = delay * 1000 * 24 * 3600;
            Thread.sleep(delay);
          }
          else if (TimeUnit.HOURS == unit)
          {
            delay = delay * 1000 * 3600;
            Thread.sleep(delay);
          }
          else if (TimeUnit.MINUTES == unit)
          {
            delay = delay * 1000 * 60;
            Thread.sleep(delay);
          }
          else if (TimeUnit.SECONDS == unit)
          {
            delay = delay * 1000;
            Thread.sleep(delay);
          }
          else if (TimeUnit.MILLISECONDS == unit)
          {
            Thread.sleep(delay);
          }
          else  if (TimeUnit.MICROSECONDS == unit)
          {
            long millisecondsDelay = delay / 1000;
            if (millisecondsDelay > 0)
            {
              Thread.sleep(millisecondsDelay);
            }
            else
            {
              int nanoDelay = (int)delay * 1000;
              Thread.sleep(0, nanoDelay);
            }
          }
          else if (TimeUnit.NANOSECONDS == unit)
          {
            long millisecondsDelay = delay / 1000000;
            if (millisecondsDelay > 0)
            {
              Thread.sleep(millisecondsDelay);
            }
            else
            {
              int nanoDelay = (int)delay;
              Thread.sleep(0, nanoDelay);
            }
          }
        }
        catch (InterruptedException e)
        {
          //not going to run the command if interrupted
          return null;
        }
        command.run();
      }
    }
    return null;
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdown()
  {
    synchronized (this)
    {
      _isShutDown = true;
    }
  }

  @Override
  public List<Runnable> shutdownNow()
  {
    synchronized (this)
    {
      _isShutDown = true;
    }
    return new ArrayList<Runnable>();
  }

  @Override
  public boolean isShutdown()
  {
    synchronized (this)
    {
      return _isShutDown;
    }
  }

  @Override
  public boolean isTerminated()
  {
    synchronized (this)
    {
      return _isShutDown;
    }
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit)
      throws InterruptedException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void execute(Runnable command)
  {
    synchronized (this)
    {
      if (!_isShutDown)
      {
        command.run();
      }
    }
  }
}

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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A Runnable wrapper with associated timeout.  If the TimeoutRunnable's {@link #run} method
 * is invoked before the timeout expires, the timeout is cancelled.  Otherwise, the wrapped
 * Runnable's run method will be invoked when the timeout expires.
 *
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TimeoutRunnable implements Runnable
{
  private static final Logger LOG = LoggerFactory.getLogger(TimeoutRunnable.class);

  private final Timeout<Runnable> _timeout;

  /**
   * Construct a new instance with the specified parameters.
   *
   * @param executor the {@link ScheduledExecutorService} to use for scheduling the timeout task
   * @param timeout the timeout delay, in the specified {@link TimeUnit}.
   * @param timeoutUnit the {@link TimeUnit} for the timeout parameter.
   * @param action the {@link Runnable} to be run when the timeout expires.
   * @param timeoutMessage a message to be logged when the timeout expires.
   */
  public TimeoutRunnable(ScheduledExecutorService executor, long timeout, TimeUnit timeoutUnit,
                         final Runnable action, final String timeoutMessage)
  {
    if (action == null)
    {
      throw new NullPointerException();
    }
    _timeout = new Timeout<Runnable>(executor, timeout, timeoutUnit, action);
    _timeout.addTimeoutTask(new Runnable()
    {
      @Override
      public void run()
      {
        LOG.info(timeoutMessage);
        action.run();
      }
    });
  }

  @Override
  public void run()
  {
    Runnable r = _timeout.getItem();
    if (r != null)
    {
      r.run();
    }
  }
}

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

import java.util.Collection;
import com.linkedin.common.callback.SimpleCallback;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */
public interface RateLimiter
{
  /**
   * Submit a new {@link Task}. The {@link Task} may be executed right away or
   * sometime later after the submission(rate-limiting).
   *
   * @param t the {@link Task} to be executed.
   */
  void submit(Task t);

  /**
   * Set the rate-limit period.
   *
   * @param ms New value for the period, in milliseconds.
   */
  void setPeriod(long ms);

  /**
   * Increment the period.
   */
  void incrementPeriod();

  /**
   * Cancel all pending {@link Task}s that are submitted to the {@link RateLimiter} but haven't
   * been executed.
   *
   * @return a {@link Collection} of {@link Task}s have been cancelled.
   */
  Collection<Task> cancelPendingTasks();

  /**
   * The minimum scheduling unit to apply rate-limiting logic.
   */
  public interface Task
  {
    void run(SimpleCallback callback);
  }
}

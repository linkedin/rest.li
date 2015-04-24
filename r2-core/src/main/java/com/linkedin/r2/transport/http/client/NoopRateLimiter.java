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
import java.util.Collections;
import com.linkedin.common.callback.SimpleCallback;

/**
 * A pass-through {@link RateLimiter} that doesn't apply any rate-limiting logic.
 *
 * @author Ang Xu
 * @version $Revision: $
 */
public class NoopRateLimiter implements RateLimiter
{
  private static final SimpleCallback NULL_CALLBACK = new SimpleCallback()
  {
    @Override
    public void onDone()
    {
    }
  };

  @Override
  public void submit(Task t)
  {
    t.run(NULL_CALLBACK);
  }

  @Override
  public void setPeriod(long ms)
  {
  }

  @Override
  public void incrementPeriod()
  {
  }

  @Override
  public Collection<Task> cancelPendingTasks()
  {
    return Collections.emptyList();
  }
}

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

package com.linkedin.d2.jmx;

import java.util.concurrent.ScheduledThreadPoolExecutor;


/**
 * Implementation of ScheduledThreadPoolExecutorJmxMBean
 *
 * @author Oby Sumampouw <osumampouw@linkedin.com>
 */
public class ScheduledThreadPoolExecutorJmx implements ScheduledThreadPoolExecutorJmxMBean
{
  private final ScheduledThreadPoolExecutor _executorService;

  public ScheduledThreadPoolExecutorJmx(ScheduledThreadPoolExecutor executorService)
  {
    _executorService = executorService;
  }

  @Override
  public int getQueuedMessageCount()
  {
    return _executorService.getQueue().size();
  }

  @Override
  public boolean isAlive()
  {
    return !_executorService.isShutdown();
  }
}

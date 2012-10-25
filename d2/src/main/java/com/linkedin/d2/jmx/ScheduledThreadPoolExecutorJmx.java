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

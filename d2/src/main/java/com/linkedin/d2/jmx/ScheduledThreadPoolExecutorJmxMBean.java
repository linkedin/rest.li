package com.linkedin.d2.jmx;


/**
 * JMX tools for accessing the executor service for LoadBalancer
 *
 * @author Oby Sumampouw <osumampouw@linkedin.com>
 */
public interface ScheduledThreadPoolExecutorJmxMBean
{
  int getQueuedMessageCount();

  boolean isAlive();
}

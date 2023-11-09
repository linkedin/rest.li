package com.linkedin.d2.xds.LoadBalanceTaskPool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class NewLoadBalancerTaskthreadPool {
  private ThreadPoolExecutor _threadPoolExecutor;
  private int _corePoolSize = 2;
  private int _maximumPoolSize = 3;
  private long _keepAliveTime = 200;
  private int _queueSize = 1000;

  public NewLoadBalancerTaskthreadPool() {
    RejectedExecutionHandler rejectedExecutionHandler = new NewBalanceTaskRejectedPolicy();
    _threadPoolExecutor = new ThreadPoolExecutor(_corePoolSize, _maximumPoolSize, _keepAliveTime, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<Runnable>(_queueSize), rejectedExecutionHandler);
  }

  public void execute(Runnable task) {
    _threadPoolExecutor.execute(task);
  }

  public void shutdown() {
    _threadPoolExecutor.shutdown();
  }
}
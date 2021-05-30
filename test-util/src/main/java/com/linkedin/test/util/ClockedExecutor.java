package com.linkedin.test.util;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simulated service executor and clock. For test only
 *
 * This class lacks in some implementations. It's in work in progress
 */
public class ClockedExecutor extends Clock implements ScheduledExecutorService
{
  private static final Logger LOG = LoggerFactory.getLogger(ClockedExecutor.class);

  private volatile long _currentTimeMillis = 0L;
  private volatile Boolean _stopped = true;
  private PriorityBlockingQueue<ClockedTask> _taskList = new PriorityBlockingQueue<>();

  public Future<Void> runFor(long duration)
  {
    return runUntil((duration <= 0 ? 0 : duration) + _currentTimeMillis);
  }

  public Future<Void> runUntil(long untilTime)
  {
    if (!_stopped)
    {
      throw new IllegalArgumentException("Already Started!");
    }
    if (_taskList.isEmpty())
    {
      return null;
    }
    _stopped = false;

    while (!_stopped && !_taskList.isEmpty() && (untilTime <= 0L || untilTime >= _currentTimeMillis))
    {
      ClockedTask task = _taskList.peek();
      long expectTime = task.getScheduledTime();

      if (expectTime > untilTime)
      {
        _currentTimeMillis = untilTime;
        break;
      }

      _taskList.remove();

      if (expectTime > _currentTimeMillis)
      {
        _currentTimeMillis = expectTime;
      }
      if (LOG.isDebugEnabled())
      {
        LOG.debug("Processing task " + task.toString() + " total {}, time {}", _taskList.size(), _currentTimeMillis);
      }
      task.run();
      if (task.repeatCount() > 0 && !task.isCancelled() && !_stopped)
      {
        task.reschedule(_currentTimeMillis);
        _taskList.add(task);
      }
    }
    _stopped = true;
    return null;
  }

  @Override
  public ScheduledFuture<Void> schedule(Runnable cmd, long delay, TimeUnit unit)
  {
    ClockedTask task = new ClockedTask("ScheduledTask", cmd, _currentTimeMillis + unit.toMillis(delay));
    _taskList.add(task);
    return task;
  }

  @Override
  public <Void> ScheduledFuture<Void> schedule(Callable<Void> callable, long delay, TimeUnit unit)
  {
    throw new IllegalArgumentException("Not supported yet!");
  }

  @Override
  public ScheduledFuture<Void> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
  {
    ClockedTask task =
      new ClockedTask("scheduleAtFixedRate", command, _currentTimeMillis + unit.toMillis(initialDelay), unit.toMillis(period), Long.MAX_VALUE);
    _taskList.add(task);
    return task;
  }

  @Override
  public ScheduledFuture<Void> scheduleWithFixedDelay(Runnable cmd, long initDelay, long interval, TimeUnit unit)
  {
    ClockedTask task =
      new ClockedTask("scheduledWithDelayTask", cmd, _currentTimeMillis + unit.toMillis(initDelay), unit.toMillis(interval), Long.MAX_VALUE);
    _taskList.add(task);
    return task;
  }

  public void scheduleWithRepeat(Runnable cmd, long initDelay, long interval, long repeatTimes)
  {
    ClockedTask task = new ClockedTask("scheduledWithRepeatTask", cmd, _currentTimeMillis + initDelay, interval, repeatTimes);
    _taskList.add(task);
  }

  @Override
  public void execute(Runnable cmd)
  {
    ClockedTask task = new ClockedTask("executeTask", cmd, _currentTimeMillis);
    _taskList.add(task);
  }

  @Override
  public void shutdown()
  {
    _stopped = true;
  }

  @Override
  public List<Runnable> shutdownNow()
  {
    _stopped = true;
    return Collections.emptyList();
  }

  @Override
  public boolean isShutdown()
  {
    return _stopped;
  }

  @Override
  public boolean isTerminated()
  {
    return _stopped && _taskList.isEmpty();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit)
  {
    runUntil(unit.convert(timeout, TimeUnit.MILLISECONDS));
    return true;
  }

  @Override
  public <T> Future<T> submit(Callable<T> task)
  {
    throw new IllegalArgumentException("Not supported yet!");
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result)
  {
    throw new IllegalArgumentException("Not supported yet!");
  }

  @Override
  public Future<?> submit(Runnable task)
  {
    if (task == null)
    {
      throw new NullPointerException();
    }
    RunnableFuture<Void> ftask = new FutureTask<>(() -> {
    }, null);
    // Simulation only: Run the task in current thread
    task.run();
    return ftask;
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
  {
    throw new IllegalArgumentException("Not supported yet!");
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
  {
    throw new IllegalArgumentException("Not supported yet!");
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
  {
    throw new IllegalArgumentException("Not supported yet!");
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
  {
    throw new IllegalArgumentException("Not supported yet!");
  }

  @Override
  public String toString()
  {
    return "ClockedExecutor [_currentTimeMillis: " + _currentTimeMillis + "_taskList:" + _taskList.stream().map(e -> e.toString())
      .collect(Collectors.joining(","));
  }

  @Override
  public ZoneId getZone() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Clock withZone(ZoneId zoneId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Instant instant() {
    return Instant.ofEpochMilli(_currentTimeMillis);
  }

  private class ClockedTask implements Runnable, ScheduledFuture<Void>
  {
    final private String _name;
    private long _expectTimeMillis;
    private long _interval;
    private Runnable _task;
    private long _repeatTimes;
    private CountDownLatch _done;
    private boolean _cancelled;

    ClockedTask(String name, Runnable task, long scheduledTime)
    {
      this(name, task, scheduledTime, 0l, 0l);
    }

    ClockedTask(String name, Runnable task, long scheduledTime, long interval, long repeat)
    {
      _name = name;
      _task = task;
      _expectTimeMillis = scheduledTime;
      _interval = interval;
      _repeatTimes = repeat;
      _done = new CountDownLatch(1);
      _cancelled = false;
    }

    @Override
    public void run()
    {
      if (!_cancelled)
      {
        _task.run();
        _done.countDown();
      }
    }

    long repeatCount()
    {
      return _repeatTimes;
    }

    long getScheduledTime()
    {
      return _expectTimeMillis;
    }

    void reschedule(long currentTime)
    {
      if (!_cancelled && currentTime >= _expectTimeMillis && _repeatTimes-- > 0)
      {
        _expectTimeMillis += (_interval - (currentTime - _expectTimeMillis));
        _done = new CountDownLatch(1);
      }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
      _cancelled = true;
      if (_done.getCount() > 0)
      {
        _done.countDown();
        return true;
      }
      return false;
    }

    @Override
    public boolean isCancelled()
    {
      return _cancelled;
    }

    @Override
    public boolean isDone()
    {
      return _done.getCount() == 0;
    }

    @Override
    public Void get()
      throws InterruptedException
    {
      _done.await();
      return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit)
      throws InterruptedException
    {
      _done.await(timeout, unit);
      return null;
    }

    @Override
    public long getDelay(TimeUnit unit)
    {
      return unit.convert(_expectTimeMillis - _currentTimeMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other)
    {
      return (int) (getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS));
    }

    @Override
    public String toString()
    {
      return "ClockedTask [_name=" + _name + "_expectedTime=" + _expectTimeMillis + "_repeatTimes=" + _repeatTimes + "_interval=" + _interval + "]";
    }
  }
}

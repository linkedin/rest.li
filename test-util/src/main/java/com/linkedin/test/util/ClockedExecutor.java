package com.linkedin.test.util;

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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.linkedin.util.clock.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simulated service executor and clock. For test only
 *
 * This class lacks in some implementations. It's in work in progress
 */
public class ClockedExecutor implements Clock, ScheduledExecutorService
{
  private static final Logger LOG = LoggerFactory.getLogger(ClockedExecutor.class);
  private static final long NANOS_PER_MILLI = 1_000_000L;

  private volatile long _currentTimeNanos = 0L;
  private volatile Boolean _stopped = true;
  private volatile long _taskCount = 0L;
  private PriorityBlockingQueue<ClockedTask> _taskList = new PriorityBlockingQueue<>();

  public Future<Void> runFor(long duration)
  {
    return runUntil((duration <= 0 ? 0 : duration) + getCurrentTimeMillis());
  }

  public Future<Void> runUntil(long untilTime)
  {
    return runUntilNanos(untilTime * NANOS_PER_MILLI);
  }

  private Future<Void> runUntilNanos(long untilTimeNanos)
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

    while (!_stopped && !_taskList.isEmpty() && (untilTimeNanos <= 0L || untilTimeNanos >= _currentTimeNanos))
    {
      ClockedTask task = _taskList.peek();
      long expectTime = task.getScheduledTime();

      if (expectTime > untilTimeNanos)
      {
        _currentTimeNanos = untilTimeNanos;
        break;
      }

      _taskList.remove();

      if (expectTime > _currentTimeNanos)
      {
        _currentTimeNanos = expectTime;
      }
      if (LOG.isDebugEnabled())
      {
        LOG.debug("Processing task " + task.toString() + " total {}, time {}", _taskList.size(), _currentTimeNanos);
      }
      task.run();
      _taskCount++;
      if (task.repeatCount() > 0 && !task.isCancelled() && !_stopped)
      {
        task.reschedule(_currentTimeNanos);
        _taskList.add(task);
      }
    }
    _stopped = true;
    return null;
  }

  public long getExecutedTaskCount()
  {
    return _taskCount;
  }

  @Override
  public ScheduledFuture<Void> schedule(Runnable cmd, long delay, TimeUnit unit)
  {
    ClockedTask task = new ClockedTask("ScheduledTask", cmd, _currentTimeNanos + unit.toNanos(delay));
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
      new ClockedTask("scheduleAtFixedRate", command, _currentTimeNanos + unit.toNanos(initialDelay), unit.toNanos(period), Long.MAX_VALUE);
    _taskList.add(task);
    return task;
  }

  @Override
  public ScheduledFuture<Void> scheduleWithFixedDelay(Runnable cmd, long initDelay, long interval, TimeUnit unit)
  {
    ClockedTask task =
      new ClockedTask("scheduledWithDelayTask", cmd, _currentTimeNanos + unit.toNanos(initDelay), unit.toNanos(interval), Long.MAX_VALUE);
    _taskList.add(task);
    return task;
  }

  public void scheduleWithRepeat(Runnable cmd, long initDelay, long interval, long repeatTimes)
  {
    ClockedTask task = new ClockedTask("scheduledWithRepeatTask", cmd, _currentTimeNanos + initDelay * NANOS_PER_MILLI, interval * NANOS_PER_MILLI, repeatTimes);
    _taskList.add(task);
  }

  @Override
  public void execute(Runnable cmd)
  {
    ClockedTask task = new ClockedTask("executeTask", cmd, _currentTimeNanos);
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
  public long currentTimeMillis()
  {
    return _currentTimeNanos / NANOS_PER_MILLI;
  }

  @Override
  public long currentTimeNanos()
  {
    return _currentTimeNanos;
  }

  @Override
  public String toString()
  {
    return "ClockedExecutor [_currentTimeMillis: " + currentTimeMillis() + "_taskList:" + _taskList.stream().map(e -> e.toString())
      .collect(Collectors.joining(","));
  }

  public long getCurrentTimeMillis()
  {
    return _currentTimeNanos / NANOS_PER_MILLI;
  }

  private class ClockedTask implements Runnable, ScheduledFuture<Void>
  {
    final private String _name;
    private long _expectTimeNanos;
    private long _intervalNanos;
    private Runnable _task;
    private long _repeatTimes;
    private CountDownLatch _done;
    private boolean _cancelled;

    ClockedTask(String name, Runnable task, long scheduledTimeNanos)
    {
      this(name, task, scheduledTimeNanos, 0L, 0L);
    }

    ClockedTask(String name, Runnable task, long scheduledTimeNanos, long intervalNanos, long repeat)
    {
      _name = name;
      _task = task;
      _expectTimeNanos = scheduledTimeNanos;
      _intervalNanos = intervalNanos;
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
      return _expectTimeNanos;
    }

    void reschedule(long currentTimeNanos)
    {
      if (!_cancelled && currentTimeNanos >= _expectTimeNanos && _repeatTimes-- > 0)
      {
        _expectTimeNanos += (_intervalNanos - (currentTimeNanos - _expectTimeNanos));
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
      return unit.convert(_expectTimeNanos - _currentTimeNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed other)
    {
      return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
    }

    @Override
    public String toString()
    {
      return "ClockedTask [_name=" + _name + "_expectedTime=" + _expectTimeNanos + "_repeatTimes=" + _repeatTimes + "_interval=" + _intervalNanos + "]";
    }
  }
}

package com.linkedin.d2.balancer.util;

import com.linkedin.common.callback.Function;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.testng.Assert.*;

/**
 * Miscellaneous code that's useful for testing.
 * Any of this code may throw AssertionError when something unexpected happens.
 */
public class TestHelper
{
  /**
   * Assert that actual and expected contain the same objects in the same order.
   */
  public static <T> void assertSameElements(Iterable<T> actual, Iterable<T> expected)
  {
    Iterator<T> e = expected.iterator();
    int index = 0;
    for (T a : actual)
    {
      assertTrue(e.hasNext(), "too long: " + actual + ".size > " + expected + ".size");
      assertSame(a, e.next(), "not same: actual[" + index + "]");
      ++index;
    }
    assertFalse(e.hasNext(), "too short: " + actual + ".size < " + expected + ".size");
  }

  /**
   * Partition from into subLists, each of size subListSize.
   */
  public static <T> List<List<T>> split(List<T> from, int subListSize)
  {
    List<List<T>> into = new ArrayList<List<T>>();
    for (int first = 0; first < from.size(); first += subListSize)
    {
      into.add(from.subList(first, Math.min(first + subListSize, from.size())));
    }
    return into;
  }

  public static <T> List<T> getAll(Collection<Future<T>> futures)
  {
    return getAll(futures, futures.size() * 10, TimeUnit.SECONDS); // plenty of time
  }

  public static <T> List<T> getAll(Iterable<Future<T>> futures, long timeout, TimeUnit unit)
  {
    List<T> all = new ArrayList<T>();
    final long deadline = System.nanoTime() + unit.toNanos(timeout);
    int f = 0;
    for (Future<T> future : futures)
    {
      try
      {
        all.add(future.get(deadline - System.nanoTime(), TimeUnit.NANOSECONDS));
      }
      catch (Exception e)
      {
        fail("index " + f, e);
      }
      ++f;
    }
    return all;
  }

  public static <T> List<Future<T>> concurrently(Collection<Callable<T>> calls)
  {
    final int numberOfCalls = calls.size();
    CountDownLatch ready = new CountDownLatch(numberOfCalls);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<T>> futures = new ArrayList<Future<T>>(numberOfCalls);
    {
      ExecutorService pool = newFixedDaemonPool(numberOfCalls);
      for (Callable<T> call : calls)
        futures.add(pool.submit(new PauseCallable<T>(1, ready, start, call)));
      assertEquals(futures.size(), numberOfCalls);
    }
    try
    {
      assertTrue(ready.await(numberOfCalls * 10, TimeUnit.SECONDS));
    }
    catch (InterruptedException e)
    {
      fail(e + "", e);
    }
    start.countDown(); // allow all threads to proceed
    return futures;
  }

  public static ExecutorService newFixedDaemonPool(int numberOfThreads)
  {
    return Executors.newFixedThreadPool(numberOfThreads, new DaemonFactory());
  }

  public static class DaemonFactory implements ThreadFactory
  {
    private static final AtomicLong factoryNumbers = new AtomicLong(0);
    private final long factoryNumber = factoryNumbers.incrementAndGet();
    private final AtomicLong threadNumbers = new AtomicLong(0);

    @Override
    public Thread newThread(Runnable target)
    {
      Thread thread = new Thread(target);
      thread.setDaemon(true);
      // Structured thread names are helpful for debugging.
      thread.setName("daemon-" + factoryNumber + "." + threadNumbers.incrementAndGet());
      return thread;
    }
  }

  /**
   * A Callable that pauses execution of its calling threads.
   */
  public static class PauseCallable<T> implements Callable<T>
  {
    private final long _pauseCall;
    private final CountDownLatch _paused;
    private final CountDownLatch _resume;
    private final Callable<T> _target;
    private final AtomicLong _calls = new AtomicLong(0);

    public PauseCallable(long pauseCall, CountDownLatch paused, CountDownLatch resume, Callable<T> target)
    {
      _pauseCall = pauseCall;
      _paused = paused;
      _resume = resume;
      _target = target;
    }

    /** The number of times this object has been called. */
    public long getCalls()
    {
      return _calls.get();
    }

    @Override
    public T call()
        throws Exception
    {
      if (_calls.incrementAndGet() >= _pauseCall)
      {
        _paused.countDown();
        _resume.await();
      }
      return _target.call();
    }
  }
}

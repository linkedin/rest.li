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
      assertTrue(e.hasNext(), actual + ".size > " + expected + ".size");
      assertSame(a, e.next(), actual + "[" + index + "]");
      ++index;
    }
    assertFalse(e.hasNext(), actual + ".size < " + expected + ".size");
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

  public static int max(Iterable<Integer> values)
  {
    int result = Integer.MIN_VALUE;
    for (Integer value : values)
      result = Math.max(result, value);
    return result;
  }

  /**
   * Call subject.map(i) concurrently, for i in 0 .. numberOfInputs-1. Run 3 threads for each input; make all the
   * threads call subject.map as close to concurrently as practical. This test is not repeatable, since the timing of
   * the threads' execution depends on the hardware and the thread scheduler.
   *
   * @return [subject.map(0) .. subject.map(numberOfInputs - 1)]
   */
  public static <O> List<O> concurrentCalls(Function<Integer, O> subject, int numberOfInputs)
  {
    int callsPerInput = 3;
    List<Callable<O>> calls = new ArrayList<Callable<O>>();
    for (int c = 0; c < callsPerInput; ++c)
      for (int i = 0; i < numberOfInputs; ++i)
        calls.add(new Call<Integer, O>(subject, i));
    List<List<O>> actual = split(getAll(concurrently(calls), numberOfInputs * 10, TimeUnit.SECONDS), numberOfInputs);
    assertEquals(actual.size(), callsPerInput);
    List<O> a0 = actual.get(0);
    assertEquals(a0.size(), numberOfInputs);
    for (int a = 1; a < actual.size(); ++a)
      assertSameElements(actual.get(a), a0);
    return a0;
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
        futures.add(pool.submit(new Coordinated<T>(ready, start, call)));
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
   * Call a Callable, after coordinating with other threads.
   */
  public static class Coordinated<T> implements Callable<T>
  {
    private final CountDownLatch _ready;
    private final CountDownLatch _start;
    private final Callable<T> _target;

    public Coordinated(CountDownLatch ready, CountDownLatch start, Callable<T> target)
    {
      _ready = ready;
      _start = start;
      _target = target;
    }

    @Override
    public T call()
        throws Exception
    {
      _ready.countDown();
      _start.await();
      return _target.call();
    }
  }

  public static class ToString<I> implements Function<I, String>
  {
    @Override
    public String map(I input)
    {
      return String.valueOf(input);
    }
  }

  /**
   * Call Function.map.
   */
  public static class Call<I, O> implements Callable<O>
  {
    private final Function<I, O> _function;
    private final I _input;

    public Call(Function<I, O> function, I input)
    {
      _function = function;
      _input = input;
    }

    @Override
    public O call()
        throws InterruptedException
    {
      return _function.map(_input);
    }
  }

  /**
   * A Function that pauses execution of its calling threads.
   */
  public static class PauseFunction<T> implements Function<T, T>
  {
    final AtomicInteger _calls = new AtomicInteger(0);
    final CountDownLatch _paused = new CountDownLatch(1);
    final CountDownLatch _resume = new CountDownLatch(1);

    @Override
    public T map(T input)
    {
      if (_calls.incrementAndGet() == 1)
      {
        _paused.countDown();
      }
      try
      {
        _resume.await();
      }
      catch (Exception e)
      {
        fail(e + "", e);
      }
      return input;
    }
  }
}

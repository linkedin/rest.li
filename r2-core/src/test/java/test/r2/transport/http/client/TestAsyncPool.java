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

package test.r2.transport.http.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.stats.LongTracking;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.AsyncPoolImpl;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.ExponentialBackOffRateLimiter;
import com.linkedin.r2.transport.http.client.NoopRateLimiter;
import com.linkedin.r2.transport.http.client.ObjectCreationTimeoutException;
import com.linkedin.r2.transport.http.client.PoolStats;
import com.linkedin.r2.util.Cancellable;
import com.linkedin.test.util.AssertionMethods;
import com.linkedin.test.util.ClockedExecutor;
import com.linkedin.test.util.retry.SingleRetry;
import com.linkedin.util.clock.SettableClock;
import java.time.Duration;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TestAsyncPool
{
  private static final long SAMPLING_DURATION_INCREMENT = Duration.ofMinutes(2).toMillis();

  private ScheduledExecutorService _executor = Executors.newSingleThreadScheduledExecutor();

  @AfterClass
  public void stopExecutor()
  {
    _executor.shutdown();
  }

  @Test
  public void testMustStart() throws TimeoutException, InterruptedException
  {
    AsyncPool<Object> pool = new AsyncPoolImpl<Object>("object pool",
                                                       new SynchronousLifecycle(),
                                                       1,
                                                       100,
                                                       _executor
                                                       );
    FutureCallback<Object> cb = new FutureCallback<Object>();
    pool.get(cb);
    try
    {
      cb.get(30, TimeUnit.SECONDS);
      Assert.fail("Get succeeded on pool not yet started");
    }
    catch (ExecutionException e)
    {
      // This is what we expect
    }
  }

  @Test
  public void testCreate()
  {
    AsyncPool<Object> pool = new AsyncPoolImpl<Object>("object pool",
                                                       new SynchronousLifecycle(),
                                                       1,
                                                       100,
                                                       _executor
                                                       );
    pool.start();
    FutureCallback<Object> cb = new FutureCallback<Object>();
    pool.get(cb);
    try
    {
      Object o = cb.get();
      Assert.assertNotNull(o);
    }
    catch (Exception e)
    {
      Assert.fail("Could not get object from pool", e);
    }

  }

  @Test
  public void testMaxSize()
  {
    final int ITERATIONS = 1000;
    final int THREADS = 100;
    final int POOL_SIZE = 25;
    final int DELAY = 1;
    SynchronousLifecycle lifecycle = new SynchronousLifecycle();
    final AsyncPool<Object> pool = new AsyncPoolImpl<Object>("object pool",
                                                             lifecycle,
                                                             POOL_SIZE,
                                                             100,
                                                             _executor
                                                             );
    pool.start();

    Runnable r = new Runnable()
    {
      @Override
      public void run()
      {
        for (int i = 0; i < ITERATIONS; i++)
        {
          FutureCallback<Object> cb = new FutureCallback<Object>();
          pool.get(cb);
          try
          {
            Object o = cb.get();
            if (DELAY > 0)
            {
              Thread.sleep(DELAY);
            }
            pool.put(o);
          }
          catch (Exception e)
          {
            Assert.fail("Unexpected failure", e);
          }
        }
      }
    };
    List<Thread> threads = new ArrayList<Thread>(THREADS);
    for (int i = 0; i < THREADS; i++)
    {
      Thread t = new Thread(r);
      t.start();
      threads.add(t);
    }
    for (Thread t : threads)
    {
      try
      {
        t.join();
      }
      catch (InterruptedException e)
      {
        Assert.fail("Unexpected interruption", e);
      }
    }
    Assert.assertTrue(lifecycle.getHighWaterMark() <= POOL_SIZE, "High water mark exceeded " + POOL_SIZE);
  }

  @Test
  public void testShutdown()
  {
    final int POOL_SIZE = 25;
    final int CHECKOUT = POOL_SIZE;
    SynchronousLifecycle lifecycle = new SynchronousLifecycle();
    final AsyncPool<Object> pool = new AsyncPoolImpl<Object>("object pool",
                                                             lifecycle,
                                                             POOL_SIZE,
                                                             100,
                                                             _executor
                                                             );
    pool.start();

    List<Object> objects = new ArrayList<Object>(CHECKOUT);
    for (int i = 0; i < CHECKOUT; i++)
    {
      FutureCallback<Object> cb = new FutureCallback<Object>();
      pool.get(cb);

      try
      {
        Object o = cb.get();
        Assert.assertNotNull(o);
        objects.add(o);
      }
      catch (Exception e)
      {
        Assert.fail("unexpected error", e);
      }
    }
    FutureCallback<None> shutdown = new FutureCallback<None>();
    pool.shutdown(shutdown);

    for (Object o : objects)
    {
      Assert.assertFalse(shutdown.isDone(), "Pool shutdown with objects checked out");
      pool.put(o);
    }

    try
    {
      shutdown.get();
    }
    catch (Exception e)
    {
      Assert.fail("unexpected error", e);
    }
  }

  /**
   * Tests {@link AsyncPool}'s shutdown sequence is properly triggered when outstanding
   * waiters cancel the previous get calls.
   */
  @Test
  public void testCancelTriggerShutdown() throws Exception
  {
    SynchronousLifecycle lifecycle = new SynchronousLifecycle();
    AsyncPool<Object> pool = new AsyncPoolImpl<Object>("object pool", lifecycle, 1, 100, _executor);
    pool.start();

    FutureCallback<Object> callback1 = new FutureCallback<>();
    Cancellable cancellable1 = pool.get(callback1);

    FutureCallback<Object> callback2 = new FutureCallback<>();
    Cancellable cancellable2 = pool.get(callback2);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);

    // Disposes the previously checked out object. The pool now has no outstanding checkouts but waiter
    // size is still one due to the second #get call above.
    pool.dispose(callback1.get(5, TimeUnit.SECONDS));

    // Caller cancels the second #get call. The pool should be in the right condition and initiate shutdown.
    cancellable2.cancel();

    // Pool should shutdown successfully without the callback timeout
    shutdownCallback.get(5, TimeUnit.SECONDS);
  }

  @Test
  public void testLRU() throws Exception
  {
    final int POOL_SIZE = 25;
    final int GET = 15;
    SynchronousLifecycle lifecycle = new SynchronousLifecycle();
    final AsyncPool<Object> pool = new AsyncPoolImpl<Object>("object pool",
        lifecycle, POOL_SIZE, 1000, _executor, _executor, Integer.MAX_VALUE, AsyncPoolImpl.Strategy.LRU, 0);

    pool.start();

    ArrayList<Object> objects = new ArrayList<Object>();

    for(int i = 0; i < GET; i++)
    {
      FutureCallback<Object>cb = new FutureCallback<Object>();
      pool.get(cb);
      objects.add(cb.get());
    }

    // put the objects back
    for(int i = 0; i < GET; i++)
    {
      pool.put(objects.get(i));
    }

    // we should get the same objects back in FIFO order
    for(int i = 0; i < GET; i++)
    {
      FutureCallback<Object> cb = new FutureCallback<Object>();
      pool.get(cb);
      Assert.assertEquals(cb.get(), objects.get(i));
    }
  }

  @Test
  public void testMinSize() throws Exception
  {
    final int POOL_SIZE = 25;
    final int MIN_SIZE = 15;
    final int GET = 20;
    final int DELAY = 1200;

    // Test every strategy
    for(AsyncPoolImpl.Strategy strategy : AsyncPoolImpl.Strategy.values()) {

      SynchronousLifecycle lifecycle = new SynchronousLifecycle();
      final AsyncPool<Object> pool = new AsyncPoolImpl<Object>("object pool",
          lifecycle, POOL_SIZE, 100, _executor, _executor, Integer.MAX_VALUE, strategy, MIN_SIZE);

      pool.start();

      Assert.assertEquals(lifecycle.getLive(), MIN_SIZE);

      ArrayList<Object> objects = new ArrayList<Object>();

      for(int i = 0; i < GET; i++)
      {
        FutureCallback<Object>cb = new FutureCallback<Object>();
        pool.get(cb);
        objects.add(cb.get());
      }
      Assert.assertEquals(lifecycle.getLive(), GET);
      for(int i = 0; i < GET; i++)
      {
        pool.put(objects.remove(objects.size()-1));
      }

      Thread.sleep(DELAY);

      Assert.assertEquals(lifecycle.getLive(), MIN_SIZE);
    }
  }

  @Test(retryAnalyzer = SingleRetry.class)
  public void testGetStats() throws Exception
  {
    final int POOL_SIZE = 25;
    final int MIN_SIZE = 0;
    final int MAX_WAITER_SIZE = Integer.MAX_VALUE;
    final SettableClock clock = new SettableClock();
    final LongTracking waitTimeTracker = new LongTracking();

    final int GET = 20;
    final int PUT_GOOD = 2;
    final int PUT_BAD = 3;
    final int DISPOSE = 4;
    final int TIMEOUT = 100;
    final int WAITER_TIMEOUT = 200;
    final int DELAY = 1200;

    final UnreliableLifecycle lifecycle = new UnreliableLifecycle();
    final AsyncPool<AtomicBoolean> pool = new AsyncPoolImpl<AtomicBoolean>(
        "object pool", lifecycle, POOL_SIZE, TIMEOUT, WAITER_TIMEOUT, _executor, MAX_WAITER_SIZE, AsyncPoolImpl.Strategy.MRU,
        MIN_SIZE, new NoopRateLimiter(), clock, waitTimeTracker);
    PoolStats stats;
    final List<AtomicBoolean> objects = new ArrayList<AtomicBoolean>();

    pool.start();

    // test values at initialization
    stats = pool.getStats();
    Assert.assertEquals(stats.getTotalCreated(), 0);
    Assert.assertEquals(stats.getTotalDestroyed(), 0);
    Assert.assertEquals(stats.getTotalCreateErrors(), 0);
    Assert.assertEquals(stats.getTotalDestroyErrors(), 0);
    Assert.assertEquals(stats.getCheckedOut(), 0);
    Assert.assertEquals(stats.getTotalTimedOut(), 0);
    Assert.assertEquals(stats.getTotalWaiterTimedOut(), 0);
    Assert.assertEquals(stats.getTotalBadDestroyed(), 0);
    Assert.assertEquals(stats.getMaxPoolSize(), POOL_SIZE);
    Assert.assertEquals(stats.getMinPoolSize(), 0);
    Assert.assertEquals(stats.getPoolSize(), 0);
    Assert.assertEquals(stats.getSampleMaxCheckedOut(), 0);
    Assert.assertEquals(stats.getSampleMaxPoolSize(), 0);

    // do a few gets
    for(int i = 0; i < GET; i++)
    {
      FutureCallback<AtomicBoolean> cb = new FutureCallback<AtomicBoolean>();
      pool.get(cb);
      AtomicBoolean obj = cb.get();
      objects.add(obj);
    }
    clock.addDuration(SAMPLING_DURATION_INCREMENT);
    stats = pool.getStats();
    Assert.assertEquals(stats.getTotalCreated(), GET);
    Assert.assertEquals(stats.getTotalDestroyed(), 0);
    Assert.assertEquals(stats.getTotalCreateErrors(), 0);
    Assert.assertEquals(stats.getTotalDestroyErrors(), 0);
    Assert.assertEquals(stats.getCheckedOut(), GET);
    Assert.assertEquals(stats.getTotalTimedOut(), 0);
    Assert.assertEquals(stats.getTotalBadDestroyed(), 0);
    Assert.assertEquals(stats.getMaxPoolSize(), POOL_SIZE);
    Assert.assertEquals(stats.getMinPoolSize(), 0);
    Assert.assertEquals(stats.getPoolSize(), GET);
    Assert.assertEquals(stats.getSampleMaxCheckedOut(), GET);
    Assert.assertEquals(stats.getSampleMaxPoolSize(), GET);

    // do some puts with good objects
    for(int i = 0; i < PUT_GOOD; i++)
    {
      AtomicBoolean obj = objects.remove(objects.size()-1);
      pool.put(obj);
    }
    clock.addDuration(SAMPLING_DURATION_INCREMENT);
    stats = pool.getStats();
    Assert.assertEquals(stats.getTotalCreated(), GET);
    Assert.assertEquals(stats.getTotalDestroyed(), 0);
    Assert.assertEquals(stats.getTotalCreateErrors(), 0);
    Assert.assertEquals(stats.getTotalDestroyErrors(), 0);
    Assert.assertEquals(stats.getCheckedOut(), GET - PUT_GOOD);
    Assert.assertEquals(stats.getTotalTimedOut(), 0);
    Assert.assertEquals(stats.getTotalBadDestroyed(), 0);
    Assert.assertEquals(stats.getMaxPoolSize(), POOL_SIZE);
    Assert.assertEquals(stats.getMinPoolSize(), 0);
    Assert.assertEquals(stats.getPoolSize(), GET);
    Assert.assertEquals(stats.getSampleMaxCheckedOut(), GET);
    Assert.assertEquals(stats.getSampleMaxPoolSize(), GET);

    // do some puts with bad objects
    for(int i = 0; i < PUT_BAD; i++)
    {
      AtomicBoolean obj = objects.remove(objects.size()-1);
      obj.set(false); // invalidate the object
      pool.put(obj);
    }
    clock.addDuration(SAMPLING_DURATION_INCREMENT);
    stats = pool.getStats();
    Assert.assertEquals(stats.getTotalCreated(), GET);
    Assert.assertEquals(stats.getTotalDestroyed(), PUT_BAD);
    Assert.assertEquals(stats.getTotalCreateErrors(), 0);
    Assert.assertEquals(stats.getTotalDestroyErrors(), 0);
    Assert.assertEquals(stats.getCheckedOut(), GET - PUT_GOOD - PUT_BAD);
    Assert.assertEquals(stats.getTotalTimedOut(), 0);
    Assert.assertEquals(stats.getTotalBadDestroyed(), PUT_BAD);
    Assert.assertEquals(stats.getMaxPoolSize(), POOL_SIZE);
    Assert.assertEquals(stats.getMinPoolSize(), 0);
    Assert.assertEquals(stats.getPoolSize(), GET - PUT_BAD);
    Assert.assertEquals(stats.getSampleMaxCheckedOut(), GET - PUT_GOOD);
    Assert.assertEquals(stats.getSampleMaxPoolSize(), GET);

    // do some disposes
    for(int i = 0; i < DISPOSE; i++)
    {
      AtomicBoolean obj = objects.remove(objects.size() - 1);
      pool.dispose(obj);
    }
    clock.addDuration(SAMPLING_DURATION_INCREMENT);
    stats = pool.getStats();
    Assert.assertEquals(stats.getTotalCreated(), GET);
    Assert.assertEquals(stats.getTotalDestroyed(), PUT_BAD + DISPOSE);
    Assert.assertEquals(stats.getTotalCreateErrors(), 0);
    Assert.assertEquals(stats.getTotalDestroyErrors(), 0);
    Assert.assertEquals(stats.getCheckedOut(), GET - PUT_GOOD - PUT_BAD - DISPOSE);
    Assert.assertEquals(stats.getTotalTimedOut(), 0);
    Assert.assertEquals(stats.getTotalBadDestroyed(), PUT_BAD + DISPOSE);
    Assert.assertEquals(stats.getMaxPoolSize(), POOL_SIZE);
    Assert.assertEquals(stats.getMinPoolSize(), 0);
    Assert.assertEquals(stats.getPoolSize(), GET - PUT_BAD - DISPOSE);
    Assert.assertEquals(stats.getSampleMaxCheckedOut(), GET - PUT_GOOD - PUT_BAD);
    Assert.assertEquals(stats.getSampleMaxPoolSize(), GET - PUT_BAD);

    // wait for a reap -- should destroy the PUT_GOOD objects
    Thread.sleep(DELAY);

    clock.addDuration(SAMPLING_DURATION_INCREMENT);
    stats = pool.getStats();
    Assert.assertEquals(stats.getTotalCreated(), GET);
    Assert.assertEquals(stats.getTotalDestroyed(), PUT_GOOD + PUT_BAD + DISPOSE);
    Assert.assertEquals(stats.getTotalCreateErrors(), 0);
    Assert.assertEquals(stats.getTotalDestroyErrors(), 0);
    Assert.assertEquals(stats.getCheckedOut(), GET - PUT_GOOD - PUT_BAD - DISPOSE);
    Assert.assertEquals(stats.getTotalTimedOut(), PUT_GOOD);
    Assert.assertEquals(stats.getTotalBadDestroyed(), PUT_BAD + DISPOSE);
    Assert.assertEquals(stats.getMaxPoolSize(), POOL_SIZE);
    Assert.assertEquals(stats.getMinPoolSize(), 0);
    Assert.assertEquals(stats.getPoolSize(), GET - PUT_GOOD - PUT_BAD - DISPOSE);
    Assert.assertEquals(stats.getSampleMaxCheckedOut(), GET - PUT_GOOD - PUT_BAD - DISPOSE);
    Assert.assertEquals(stats.getSampleMaxPoolSize(), GET - PUT_BAD - DISPOSE);
  }

  @Test
  public void testGetStatsWithErrors() throws Exception
  {
    final int POOL_SIZE = 25;
    final int GET = 20;
    final int PUT_BAD = 5;
    final int DISPOSE = 7;
    final int CREATE_BAD = 9;
    final int TIMEOUT = 100;

    final UnreliableLifecycle lifecycle = new UnreliableLifecycle();
    final AsyncPool<AtomicBoolean> pool = new AsyncPoolImpl<AtomicBoolean>(
        "object pool", lifecycle, POOL_SIZE, TIMEOUT, _executor
    );
    PoolStats stats;
    final List<AtomicBoolean> objects = new ArrayList<AtomicBoolean>();

    pool.start();

    // do a few gets
    for(int i = 0; i < GET; i++)
    {
      FutureCallback<AtomicBoolean> cb = new FutureCallback<AtomicBoolean>();
      pool.get(cb);
      AtomicBoolean obj = cb.get();
      objects.add(obj);
    }

    // put and destroy some, with errors
    lifecycle.setFail(true);
    for(int i = 0; i < PUT_BAD; i++)
    {
      AtomicBoolean obj = objects.remove(objects.size() - 1);
      obj.set(false);
      pool.put(obj);
    }
    for(int i = 0; i < DISPOSE; i++)
    {
      AtomicBoolean obj = objects.remove(objects.size() - 1);
      pool.dispose(obj);
    }
    stats = pool.getStats();
    Assert.assertEquals(stats.getTotalDestroyed(), 0);
    Assert.assertEquals(stats.getTotalCreateErrors(), 0);
    Assert.assertEquals(stats.getTotalDestroyErrors(), PUT_BAD + DISPOSE);
    Assert.assertEquals(stats.getTotalBadDestroyed(), PUT_BAD + DISPOSE);

    // create some with errors
    for(int i = 0; i < CREATE_BAD; i++)
    {
      FutureCallback<AtomicBoolean> cb = new FutureCallback<AtomicBoolean>();
      try
      {
        pool.get(cb);
      }
      catch (Exception e)
      {
        // this error is expected
      }
    }
    stats = pool.getStats();
    Assert.assertEquals(stats.getCheckedOut(), GET - PUT_BAD - DISPOSE);
    Assert.assertEquals(stats.getTotalCreateErrors(), CREATE_BAD);
  }

  /**
   * Wait time percentile, average, and maximum tracking is deprecated in {@link AsyncPool} implementations.
   */
  @Test
  public void testWaitTimeStats() throws Exception
  {
    final int POOL_SIZE = 25;
    final int CHECKOUT = POOL_SIZE;
    final long DELAY = 100;
    final double DELTA = 0.1;
    DelayedLifecycle lifecycle = new DelayedLifecycle(DELAY);
    final AsyncPool<Object> pool = new AsyncPoolImpl<Object>("object pool",
        lifecycle,
        POOL_SIZE,
        100,
        _executor
    );
    pool.start();

    PoolStats stats;
    List<Object> objects = new ArrayList<Object>(CHECKOUT);
    for (int i = 0; i < CHECKOUT; i++)
    {
      FutureCallback<Object> cb = new FutureCallback<Object>();
      pool.get(cb);
      Object o = cb.get();
      objects.add(o);
    }

    stats = pool.getStats();
    Assert.assertEquals(stats.getWaitTimeAvg(), DELAY, DELTA * DELAY);
  }

  /***
   * This test case verifies that if more request object creation requests are submitted to the rate limiter, it only
   * creates the absolute required maximum (see below example)
   *
   *  Assumption: the channel pool max size is always bigger than the requested checkout size
   *
   *|----------A------------|---------------B---------------|---------------C--------------|-------------D--------------
   *  A = In Phase A , N number of object checkout request to the pool when there are no tasks pending in the
   *      rate limiter. A's Expected result = channel pool will create N number of new objects and check them out
   *  B = In Phase B, N number of object checkout request again sent to the channel pool when the pool has already
   *      checkout N number of objects, In this phase, the object creation inside the pool is blocked and the
   *      rate limiter will Queue the creation requests once it reached its maximum concurrency configured.
   *  C = Ih Phase C, N number of objects are returned to the pool which are created in Phase A, this will make
   *      the number of idle objects in the pool as N.
   *  D = In Phase D, All the object creation blocked in Phase B will get un blocked and create number of new objects
   *      that are equal to the rate limiter concurrency. When rate limiter executes the queued creation requests - it
   *      should ignore the creation requests as there are no object waiters in the pool and thus effectively only
   *      creating the absolute minimum required count (N+Concurrency)
   *
   * @param numberOfCheckouts the N number of checkout operations that will be performed in phase A & B
   * @param poolSize the maximum Object Pool Size
   * @param concurrency the maximum allowed concurrent object creation
   */
  @Test(dataProvider = "channelStateRandomDataProvider")
  public void testObjectsAreNotCreatedWhenThereAreNoWaiters(int numberOfCheckouts, int poolSize, int concurrency)
      throws Exception
  {
    CreationBlockableSynchronousLifecycle blockableObjectCreator =
        new CreationBlockableSynchronousLifecycle(numberOfCheckouts, concurrency);
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(500);
    ExponentialBackOffRateLimiter rateLimiter = new ExponentialBackOffRateLimiter(0, 5000,
        10, executor, concurrency);

    final AsyncPool<Object> pool = new AsyncPoolImpl<Object>("object pool",
        blockableObjectCreator,
        poolSize,
        Integer.MAX_VALUE,
        _executor,
        Integer.MAX_VALUE,
        AsyncPoolImpl.Strategy.MRU,
        0, rateLimiter
    );

    pool.start();

    // Phase A:Checking out object 'numberOfCheckout' times!
    List<Object> checkedOutObjects = performCheckout(numberOfCheckouts, pool);

    // Phase B:Blocking object creation and performing the checkout 'numberOfCheckout' times again
    blockableObjectCreator.blockCreation();
    Future<None> future = performUnblockingCheckout(numberOfCheckouts, numberOfCheckouts, pool);
    blockableObjectCreator.waitUntilAllBlocked();

    // Phase C:Returning the checkedOut objects from Phase A back to the object pool
    for (Object checkedOutObject : checkedOutObjects)
    {
      pool.put(checkedOutObject);
    }

    // Phase D:All the object creation in phase B gets unblocked now
    blockableObjectCreator.unblockCreation();
    try
    {
      // Wait for all object creation to be unblocked
      future.get(5, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      Assert.fail("Did not complete unblocked object creations on time, Unexpected interruption", e);
    }

    // Making sure the rate limiter pending tasks are submitted to the executor
    AssertionMethods.assertWithTimeout(5000, ()->
        Assert.assertEquals(rateLimiter.numberOfPendingTasks(),0,"Number of tasks has to drop to 0"));

    // Wait for all the tasks in the rate limiter executor to finish
    executor.shutdown();
    try
    {
      if (!executor.awaitTermination(10, TimeUnit.SECONDS))
      {
        Assert.fail("Executor took too long to shutdown");
      }
    }
    catch (Exception ex)
    {
      Assert.fail("Unexpected interruption while shutting down executor", ex);
    }

    // Verify all the expectations
    PoolStats stats = pool.getStats();
    Assert.assertEquals(stats.getTotalCreationIgnored(), numberOfCheckouts-concurrency);
    Assert.assertEquals(stats.getCheckedOut(), numberOfCheckouts);
    Assert.assertEquals(stats.getIdleCount(), concurrency);
    Assert.assertEquals(stats.getTotalCreated(), numberOfCheckouts+concurrency);
    Assert.assertEquals(stats.getPoolSize(), numberOfCheckouts+concurrency);
    Assert.assertEquals(stats.getTotalDestroyed(), 0);
    Assert.assertEquals(stats.getTotalBadDestroyed(), 0);
    Assert.assertEquals(stats.getTotalTimedOut(), 0);
  }

  /***
   * This test case verifies that the correct number of waiters are timed out while waiting for object from the pool
   *
   *     Assumption: the channel pool max size is always bigger than the requested checkout size
   *
   *|----------A------------|---------------B---------------|---------------C--------------|-------------D--------------
   *   A = In Phase A , N number of object checkout request to the pool when there are no tasks pending in the rate
   *       limiter. A's Expected result = channel pool will create N number of new objects and check them out
   *   B = In Phase B, O number of object checkout request again sent to the channel pool when the pool has already
   *       checkout N number of objects, In this phase, the object creation inside the pool is blocked
   *       and the rate limiter will Queue the creation requests once it reached its maximum concurrency configured.
   *   C = Ih Phase C, P number of objects are returned to the pool which are created in Phase A, this will make
   *       the number of waiter queue size to be O-P
   *   D = In Phase D, A delay will be introduced to timeout the waiters and all the O-P waiters should be timed out.
   *       After the delay the object creation will be unblocked and it should create aleast the concurrency number of
   *       objects even though the waiters are timedout.
   *
   * @param numberOfCheckoutsInPhaseA the N number of checkout operations that will be performed in phase A
   * @param numberOfCheckoutsInPhaseB the O number of checkout operations that will be performed in Phase B
   * @param numbOfObjectsToBeReturnedInPhaseC the numeber of objects returned in Phase C
   * @param poolSize size of the pool,
   * @param concurrency concurrency of the rate limiter
   */
  @Test(dataProvider = "waiterTimeoutDataProvider")
  public void testWaiterTimeout(int numberOfCheckoutsInPhaseA, int numberOfCheckoutsInPhaseB,
      int numbOfObjectsToBeReturnedInPhaseC,
      int poolSize, int concurrency, int waiterTimeout) throws Exception
  {
    CreationBlockableSynchronousLifecycle blockableObjectCreator =
        new CreationBlockableSynchronousLifecycle(numberOfCheckoutsInPhaseB, concurrency);
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(500);
    ExponentialBackOffRateLimiter rateLimiter = new ExponentialBackOffRateLimiter(0, 5000,
        10, executor, concurrency);

    ClockedExecutor clockedExecutor = new ClockedExecutor();

    final AsyncPool<Object> pool = new AsyncPoolImpl<Object>("object pool",
        blockableObjectCreator,
        poolSize,
        Integer.MAX_VALUE,
        waiterTimeout,
        clockedExecutor,
        Integer.MAX_VALUE,
        AsyncPoolImpl.Strategy.MRU,
        0, rateLimiter, clockedExecutor, new LongTracking()
    );

    pool.start();

    // Phase A : Checking out object 'numberOfCheckoutsInPhaseA' times !
    List<Object> checkedOutObjects = performCheckout(numberOfCheckoutsInPhaseA, pool);

    // Phase B : Blocking object creation and performing the checkout 'numberOfCheckoutsInPhaseB' times again
    blockableObjectCreator.blockCreation();
    Future<None> future = performUnblockingCheckout(numberOfCheckoutsInPhaseB,
        0, pool);

    blockableObjectCreator.waitUntilAllBlocked();

    // Phase C : Returning the checkedOut objects from Phase A back to the object pool
    for (int i = 0; i < numbOfObjectsToBeReturnedInPhaseC; i++)
    {
      pool.put(checkedOutObjects.remove(0));
    }

    clockedExecutor.runFor(waiterTimeout);

    // Phase D : All the object creation in phase B gets unblocked now
    blockableObjectCreator.unblockCreation();
    try
    {
      future.get(5, TimeUnit.SECONDS);
    }
    catch (Exception e)
    {
      Assert.fail("Did not complete unblocked object creations on time, Unexpected interruption", e);
    }

    // Making sure the rate limiter pending tasks are submitted to the executor
    AssertionMethods.assertWithTimeout(5000, () ->
        Assert.assertEquals(rateLimiter.numberOfPendingTasks(),0,"Number of tasks has to drop to 0"));

    executor.shutdown();

    try
    {
      if (!executor.awaitTermination(10, TimeUnit.SECONDS))
      {
        Assert.fail("Executor took too long to shutdown");
      }
    }
    catch (Exception ex)
    {
      Assert.fail("Unexpected interruption while shutting down executor", ex);
    }

    PoolStats stats = pool.getStats();
    Assert.assertEquals(stats.getTotalCreationIgnored(), numberOfCheckoutsInPhaseB - concurrency);
    Assert.assertEquals(stats.getCheckedOut(), numberOfCheckoutsInPhaseA);
    Assert.assertEquals(stats.getIdleCount(), concurrency);
    Assert.assertEquals(stats.getTotalCreated(), numberOfCheckoutsInPhaseA + concurrency);
    Assert.assertEquals(stats.getPoolSize(), numberOfCheckoutsInPhaseA + concurrency);
    Assert.assertEquals(stats.getTotalWaiterTimedOut(), numberOfCheckoutsInPhaseB - numbOfObjectsToBeReturnedInPhaseC);
  }


  @Test(dataProvider = "creationTimeoutDataProvider")
  public void testCreationTimeout(int poolSize, int concurrency) throws Exception
  {
    // this object creation life cycle simulate the creation limbo state
    ObjectCreatorThatNeverCreates objectCreatorThatNeverCreates = new ObjectCreatorThatNeverCreates();
    ClockedExecutor clockedExecutor = new ClockedExecutor();
    ExponentialBackOffRateLimiter rateLimiter = new ExponentialBackOffRateLimiter(0, 5000,
        10, clockedExecutor, concurrency);
    final AsyncPool<Object> pool = new AsyncPoolImpl<Object>("object pool",
        objectCreatorThatNeverCreates,
        poolSize,
        Integer.MAX_VALUE,
        Integer.MAX_VALUE,
        clockedExecutor,
        Integer.MAX_VALUE,
        AsyncPoolImpl.Strategy.MRU,
        0, rateLimiter, clockedExecutor, new LongTracking()
    );

    pool.start();

    List<FutureCallback<Object>> checkoutCallbacks = new ArrayList<>();

    // Lets try to checkout more than the max Pool Size times when the object creator is in limbo state
    for (int i = 0; i < poolSize * 2 ; i++) {
      FutureCallback<Object> cb = new FutureCallback<>();
      checkoutCallbacks.add(cb);

      // Reset the exponential back off due to creation timeout error
      rateLimiter.setPeriod(0);

      pool.get(cb);

      // run for the duration of default creation timeout
      // TODO: parameterize the creation duration when the default creation gets parameterized
      clockedExecutor.runFor(AsyncPoolImpl.DEFAULT_OBJECT_CREATION_TIMEOUT);
    }

    // drain all the pending tasks
    clockedExecutor.runFor(AsyncPoolImpl.DEFAULT_OBJECT_CREATION_TIMEOUT);

    // Make sure that all the creations are failed with CreationTimeout
    // since the object creator went to limbo state
    for(FutureCallback<Object> cb : checkoutCallbacks)
    {
      try
      {
        cb.get(100, TimeUnit.MILLISECONDS);
      }
      catch (Exception ex)
      {
        Assert.assertTrue(ex.getCause() instanceof ObjectCreationTimeoutException);
      }
    }

    // Lets make sure the channel pool stats are at expected state
    PoolStats stats = pool.getStats();
    // Lets make sure all the limbo creations are timed out as expected
    Assert.assertEquals(stats.getTotalCreateErrors(), poolSize * 2);

    // No checkout should have happened due to object creator in limbo
    Assert.assertEquals(stats.getCheckedOut(), 0);
    // No Idle objects in the pool
    Assert.assertEquals(stats.getIdleCount(), 0);

    // Lets make sure that all the slots in the pool are reclaimed even if the object creation is in limbo
    Assert.assertEquals(stats.getPoolSize(), 0);

    // Since the max pending creation request reached the max pool size,
    // we should have reached the maPool Size at least once
    Assert.assertEquals(stats.getMaxPoolSize(), poolSize);

    // Since no object is successfully created, expecting idle objects to be zero
    Assert.assertEquals(stats.getIdleCount(), 0);
  }

  @DataProvider
  public Object[][] channelStateRandomDataProvider()
  {
    // 500 represent a good sample for the randomized data.
    // This has been verified against 100K test cases in local
    int numberOfTestCases = 500;
    Random randomNumberGenerator = ThreadLocalRandom.current();

    Object[][] data = new Object[numberOfTestCases][3];
    for (int i = 0; i < numberOfTestCases; i++)
    {
      int checkout = randomNumberGenerator.nextInt(200)+1;
      int poolSize = randomNumberGenerator.nextInt(checkout)+checkout*2;
      int concurrency = randomNumberGenerator.nextInt(Math.min(checkout,499))+1;
      data[i][0] = checkout;
      data[i][1] = poolSize;
      data[i][2] = concurrency;
    }

    return data;
  }

  @DataProvider
  public Object[][] waiterTimeoutDataProvider()
  {
    // 500 represent a good sample for the randomized data.
    // This has been verified against 100K test cases in local
    int numberOfTestCases = 500;
    Random randomNumberGenerator = new Random();

    Object[][] data = new Object[numberOfTestCases][6];
    for (int i = 0; i < numberOfTestCases; i++)
    {
      int numberOfCheckoutsInPhaseA = randomNumberGenerator.nextInt(100)+1;
      int numberOfCheckoutsInPhaseB = randomNumberGenerator.nextInt(numberOfCheckoutsInPhaseA)+1;
      numberOfCheckoutsInPhaseB = Math.min(numberOfCheckoutsInPhaseA, numberOfCheckoutsInPhaseB);
      int numbOfObjectsToBeReturnedInPhaseC = randomNumberGenerator.nextInt(numberOfCheckoutsInPhaseB);
      int poolSize = randomNumberGenerator.nextInt(numberOfCheckoutsInPhaseA)+numberOfCheckoutsInPhaseA*2;
      int concurrency = randomNumberGenerator.nextInt(Math.min(numberOfCheckoutsInPhaseB,499))+1;
      int waiterTimeout = randomNumberGenerator.nextInt(AsyncPoolImpl.MAX_WAITER_TIMEOUT);
      waiterTimeout = Math.max(waiterTimeout, AsyncPoolImpl.MIN_WAITER_TIMEOUT);

      concurrency = Math.min(concurrency, numberOfCheckoutsInPhaseB);

      data[i][0] = numberOfCheckoutsInPhaseA;
      data[i][1] = numberOfCheckoutsInPhaseB;
      data[i][2] = numbOfObjectsToBeReturnedInPhaseC;
      data[i][3] = poolSize;
      data[i][4] = concurrency;
      data[i][5] = waiterTimeout;
    }

    return data;
  }

  @DataProvider
  public Object[][] creationTimeoutDataProvider()
  {
    // 500 represent a good sample for the randomized data.
    // This has been verified against 500K test cases in local
    int numberOfTestCases = 1000;
    Random randomNumberGenerator = new Random();

    Object[][] data = new Object[numberOfTestCases][2];
    for (int i = 0; i < numberOfTestCases; i++)
    {
      int poolSize = randomNumberGenerator.nextInt(200)+1;
      int concurrency = randomNumberGenerator.nextInt(poolSize)+1;
      concurrency = Math.min(poolSize, concurrency);

      data[i][0] = poolSize;
      data[i][1] = concurrency;
    }

    return data;
  }

  private List<Object> performCheckout(int numberOfCheckouts, AsyncPool<Object> pool)
  {
    List<Object> checkedOutObjects = new ArrayList<>(numberOfCheckouts);

    ScheduledExecutorService checkoutExecutor = Executors.newScheduledThreadPool(50);
    CountDownLatch checkoutLatch = new CountDownLatch(numberOfCheckouts);
    Runnable checkoutTask = getCheckoutTask(pool, checkedOutObjects, new Object(), checkoutLatch, new CountDownLatch(numberOfCheckouts));

    for (int i = 0; i < numberOfCheckouts; i++)
    {
      checkoutExecutor.execute(checkoutTask);
    }

    try
    {
      checkoutLatch.await(5, TimeUnit.SECONDS);
      checkoutExecutor.shutdownNow();
    }
    catch (Exception ex)
    {
      Assert.fail("Too long to perform checkout operation");
    }

    return checkedOutObjects;
  }

  private Future<None> performUnblockingCheckout(int numberOfCheckoutRequests, int numberOfCheckouts, AsyncPool<Object> pool)
  {
    ScheduledExecutorService checkoutExecutor = Executors.newScheduledThreadPool(500);

    CountDownLatch checkoutLatch = new CountDownLatch(numberOfCheckouts);
    CountDownLatch requestLatch = new CountDownLatch(numberOfCheckoutRequests);
    Runnable checkoutTask = getCheckoutTask(pool, new LinkedList<>(), new Object(), checkoutLatch,
        requestLatch);

    for (int i = 0; i < numberOfCheckoutRequests; i++)
    {
      checkoutExecutor.execute(checkoutTask);
    }

    try
    {
      requestLatch.await(5, TimeUnit.SECONDS);
    }
    catch (Exception ex)
    {
      Assert.fail("Too long to perform checkout operation");
    }

    return new DelayedFutureCallback<>(checkoutLatch, checkoutExecutor);
  }

  private class DelayedFutureCallback<T> extends FutureCallback<T>
  {
    private CountDownLatch _checkoutLatch;
    private ScheduledExecutorService _checkoutExecutor;

    public DelayedFutureCallback(CountDownLatch checkoutLatch, ScheduledExecutorService checkoutExecutor)
    {
      _checkoutLatch = checkoutLatch;
      _checkoutExecutor = checkoutExecutor;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      _checkoutLatch.await(timeout, unit);
      _checkoutExecutor.shutdownNow();
      return null;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
      throw new ExecutionException(new Exception("Not Implemented"));
    }
  }

  private Runnable getCheckoutTask(AsyncPool<Object> pool, List<Object> checkedOutObjects, Object sync, CountDownLatch latch,
      CountDownLatch requestLatch)
  {
    return new Runnable()
    {
      @Override
      public void run()
      {
        FutureCallback<Object> cb = new FutureCallback<>();
        pool.get(cb);
        requestLatch.countDown();
        try
        {
          Object checkedOutObject = cb.get();
          synchronized (sync)
          {
            checkedOutObjects.add(checkedOutObject);
          }
          latch.countDown();
        }
        catch (Exception e)
        {
          Assert.fail("Unexpected failure", e);
        }
      }
    };
  }

  public static class SynchronousLifecycle implements AsyncPool.Lifecycle<Object>
  {
    private int _live = 0;
    private int _highWaterMark = 0;

    @Override
    public synchronized void create(Callback<Object> callback)
    {
      _live++;
      if (_highWaterMark < _live)
      {
        _highWaterMark = _live;
      }
      callback.onSuccess(new Object());
    }

    @Override
    public boolean validateGet(Object obj)
    {
      return true;
    }

    @Override
    public boolean validatePut(Object obj)
    {
      return true;
    }

    @Override
    public synchronized void destroy(Object obj, boolean error, Callback<Object> callback)
    {
      _live--;
      callback.onSuccess(obj);
    }

    @Override
    public PoolStats.LifecycleStats getStats()
    {
      return null;
    }

    public int getHighWaterMark()
    {
      return _highWaterMark;
    }

    public int getLive()
    {
      return _live;
    }
  }

  public static class ObjectCreatorThatNeverCreates extends SynchronousLifecycle
  {
    @Override
    public void create(Callback<Object> callback)
    {
      // just don't call the callback to simulate the creation limbo state
    }
  }


  public static class CreationBlockableSynchronousLifecycle extends SynchronousLifecycle
  {
    private CountDownLatch _blockersDoneLatch;
    private int _totalBlockers;

    public CreationBlockableSynchronousLifecycle(int checkout, int concurrency) {
      _blockersDoneLatch = new CountDownLatch(checkout);
      _totalBlockers = concurrency;
    }

    private CountDownLatch _doneLatch = new CountDownLatch(0);

    public void unblockCreation()
    {
      _doneLatch.countDown();
    }

    public void blockCreation()
    {
      _doneLatch = new CountDownLatch(1);
      _blockersDoneLatch = new CountDownLatch(_totalBlockers);
    }

    public void waitUntilAllBlocked() throws InterruptedException
    {
      _blockersDoneLatch.await();
    }

    @Override
    public void create(Callback<Object> callback)
    {
      long latch;
      try
      {
        latch = _blockersDoneLatch.getCount();
        _blockersDoneLatch.countDown();
        _doneLatch.await();
      }
      catch (Exception ex)
      {
        latch = -1;
      }

      callback.onSuccess(latch);
    }
  }

  /*
   * Allows testing of "bad" objects and create/destroy errors.
   *
   * A "bad" object is represented by an AtomicBoolean with a false value.
   * A "good" object has a true value. setFail controls whether create/destroy
   * succeed or fail.
   */
  public static class UnreliableLifecycle implements AsyncPool.Lifecycle<AtomicBoolean>
  {
    private boolean _fail = false;

    @Override
    public synchronized void create(Callback<AtomicBoolean> callback)
    {
      if(_fail)
      {
        callback.onError(new Exception());
      }
      else
      {
        callback.onSuccess(new AtomicBoolean(true));
      }
    }

    @Override
    public boolean validateGet(AtomicBoolean obj)
    {
      return obj.get();
    }

    @Override
    public boolean validatePut(AtomicBoolean obj)
    {
      return obj.get();
    }

    @Override
    public synchronized void destroy(AtomicBoolean obj, boolean error, Callback<AtomicBoolean> callback)
    {
      if(_fail)
      {
        callback.onError(new Exception());
      }
      else
      {
        callback.onSuccess(obj);
      }
    }

    @Override
    public PoolStats.LifecycleStats getStats()
    {
      return null;
    }

    public synchronized void setFail(boolean fail)
    {
      _fail = fail;
    }
  }

  public static class DelayedLifecycle implements AsyncPool.Lifecycle<Object>
  {
    private final long _delay;

    public DelayedLifecycle(long delay)
    {
      _delay = delay;
    }

    @Override
    public synchronized void create(Callback<Object> callback)
    {
      try
      {
        Thread.sleep(_delay);
        callback.onSuccess(new Object());
      }
      catch (InterruptedException e)
      {
        callback.onError(e);
      }

    }

    @Override
    public boolean validateGet(Object obj)
    {
      return true;
    }

    @Override
    public boolean validatePut(Object obj)
    {
      return true;
    }

    @Override
    public synchronized void destroy(Object obj, boolean error, Callback<Object> callback)
    {
      callback.onSuccess(obj);
    }

    @Override
    public PoolStats.LifecycleStats getStats()
    {
      return null;
    }
  }
}

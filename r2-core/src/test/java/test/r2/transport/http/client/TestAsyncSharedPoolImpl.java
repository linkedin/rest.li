/*
   Copyright (c) 2016 LinkedIn Corp.

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
import com.linkedin.common.util.None;
import com.linkedin.data.template.BooleanArray;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.AsyncPoolLifecycleStats;
import com.linkedin.r2.transport.http.client.AsyncSharedPoolImpl;
import com.linkedin.r2.transport.http.client.NoopRateLimiter;
import com.linkedin.r2.transport.http.client.PoolStats;
import com.linkedin.r2.transport.http.client.RateLimiter;
import com.linkedin.r2.util.Cancellable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 * @version $Revision: $
 */
public class TestAsyncSharedPoolImpl
{
  private static final String POOL_NAME = "testAsyncSharedPoolImpl";
  private static final int NUMBER_OF_THREADS = 128;
  private static final int SHUTDOWN_TIMEOUT = 5;
  private static final int GET_TIMEOUT = 5;
  private static final int OPERATION_TIMEOUT = 5;
  private static final int GET_COUNT = 100;
  private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
  private static final long SHORT_POOL_TIMEOUT = 500;
  private static final long NO_POOL_TIMEOUT = 0;
  private static final Object ITEM = new Object();
  private static final int MAX_WAITERS = Integer.MAX_VALUE;
  private static final int NO_WAITER = 0;

  private static final Exception CREATE_ERROR = new Exception("Simulated create failure");

  private static final ScheduledThreadPoolExecutor SCHEDULER = new ScheduledThreadPoolExecutor(NUMBER_OF_THREADS);
  private static final LifecycleMock LIFECYCLE = new LifecycleMock();
  private static final RateLimiter LIMITER = new NoopRateLimiter();

  @BeforeSuite
  public void doBeforeSuite()
  {
  }

  @AfterSuite
  public void doAfterSuite()
  {
    SCHEDULER.shutdown();
  }

  @Test
  public void testGetName()
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    Assert.assertTrue(pool.getName().startsWith(POOL_NAME));
  }

  @Test
  public void testGetStats()
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    PoolStats stats = pool.getStats();
    Assert.assertNotNull(stats);
    Assert.assertEquals(stats.getMaxPoolSize(), 1);
    Assert.assertEquals(stats.getMinPoolSize(), 0);
    Assert.assertEquals(stats.getIdleCount(), 0);

    Assert.assertEquals(stats.getTotalDestroyErrors(), 0);
    Assert.assertEquals(stats.getTotalDestroyed(), 0);
    Assert.assertEquals(stats.getTotalTimedOut(), 0);
    Assert.assertEquals(stats.getTotalCreateErrors(), 0);
    Assert.assertEquals(stats.getTotalBadDestroyed(), 0);
    Assert.assertEquals(stats.getCheckedOut(), 0);
    Assert.assertEquals(stats.getTotalCreated(), 0);
    Assert.assertEquals(stats.getPoolSize(), 0);

    Assert.assertEquals(stats.getSampleMaxCheckedOut(), 0);
    Assert.assertEquals(stats.getSampleMaxPoolSize(), 0);

    Assert.assertEquals(stats.getWaitTime50Pct(), 0);
    Assert.assertEquals(stats.getWaitTime95Pct(), 0);
    Assert.assertEquals(stats.getWaitTime99Pct(), 0);
    Assert.assertEquals(stats.getWaitTimeAvg(), 0.0);
  }

  @Test
  public void testStartShutdownSucceeds() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>
        (POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();
    verifyStats(pool.getStats(), 0, 0, 0, 0, 0, 0, 0, 0, 0);

    FutureCallback<None> callback = new FutureCallback<>();
    pool.shutdown(callback);
    None none = callback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
    Assert.assertNotNull(none);
    Assert.assertSame(none, None.none());
    verifyStats(pool.getStats(), 0, 0, 0, 0, 0, 0, 0, 0, 0);
  }

  @Test
  public void testReaperNoPendingPut() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, SHORT_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback = new FutureCallback<>();
    pool.get(getCallback);
    pool.put(getCallback.get(GET_TIMEOUT, TIME_UNIT));

    // Waits for twice the timeout amount of time for reaper to kick-in
    Thread.sleep(SHORT_POOL_TIMEOUT * 2);

    verifyStats(pool.getStats(), 0, 0, 0, 1, 0, 0, 1, 0, 1);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testReaperWithPendingPut() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, SHORT_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback = new FutureCallback<>();
    pool.get(getCallback);
    Object item = getCallback.get(GET_TIMEOUT, TIME_UNIT);

    // Waits for twice the timeout amount of time for reaper to kick-in
    Thread.sleep(SHORT_POOL_TIMEOUT * 2);

    verifyStats(pool.getStats(), 1, 1, 0, 0, 0, 0, 1, 0, 0);

    pool.put(item);
    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test(expectedExceptions = ExecutionException.class)
  public void testShutdownBeforeStart() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    FutureCallback<None> callback = new FutureCallback<>();
    pool.shutdown(callback);
    callback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testShutdownWithPendingPut() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    // Get a item from the pool
    FutureCallback<Object> getCallback = new FutureCallback<>();
    pool.get(getCallback);
    Object item = getCallback.get(GET_TIMEOUT, TIME_UNIT);
    Assert.assertNotNull(item);

    // Shutdown while the item is still outstanding
    FutureCallback<None> callback = new FutureCallback<>();
    pool.shutdown(callback);

    verifyStats(pool.getStats(), 1, 1, 0, 0, 0, 0, 1, 0, 0);

    // Return the item back the to the pool
    pool.put(item);
    verifyStats(pool.getStats(), 1, 0, 1, 0, 0, 0, 1, 0, 0);

    callback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testShutdownWithMultiplePendingPut() throws Exception
  {
    final AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    final CountDownLatch latch = new CountDownLatch(GET_COUNT);
    final Collection<FutureCallback<Object>> getCallbacks = new ConcurrentLinkedQueue<>();
    IntStream.range(0, GET_COUNT).forEach(i -> SCHEDULER.execute(() -> {
      FutureCallback<Object> getCallback = new FutureCallback<>();
      pool.get(getCallback);
      getCallbacks.add(getCallback);
      latch.countDown();
    }));
    if (!latch.await(OPERATION_TIMEOUT, TIME_UNIT))
    {
      Assert.fail("Timeout waiting for get calls");
    }

    final Collection<Object> items = new ConcurrentLinkedQueue<>();
    getCallbacks.stream().forEach(callback -> {
      try
      {
        items.add(callback.get(GET_TIMEOUT, TIME_UNIT));
      } catch (Exception e)
      {
      }
    });

    Assert.assertEquals(items.size(), GET_COUNT);

    verifyStats(pool.getStats(), 1, GET_COUNT, 0, 0, 0, 0, 1, 0, 0);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);

    // Put items back to the pool
    items.stream().forEach(item -> SCHEDULER.execute(() -> pool.put(item)));

    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testShutdownWithMultiplePendingPutValidationFails() throws Exception
  {
    final LifecycleMock lifecycleMock = new LifecycleMock();
    lifecycleMock.setValidatePutSupplier(() -> false);
    final AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, lifecycleMock, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    final CountDownLatch latch = new CountDownLatch(GET_COUNT);
    final Collection<FutureCallback<Object>> getCallbacks = new ConcurrentLinkedQueue<>();
    IntStream.range(0, GET_COUNT).forEach(i -> SCHEDULER.execute(() -> {
      FutureCallback<Object> getCallback = new FutureCallback<>();
      pool.get(getCallback);
      getCallbacks.add(getCallback);
      latch.countDown();
    }));
    if (!latch.await(OPERATION_TIMEOUT, TIME_UNIT))
    {
      Assert.fail("Timeout waiting for get calls");
    }
    Assert.assertEquals(getCallbacks.size(), GET_COUNT);

    final Collection<Object> items = new ConcurrentLinkedQueue<>();
    getCallbacks.stream().forEach(callback -> {
      try
      {
        items.add(callback.get(GET_TIMEOUT, TIME_UNIT));
      } catch (Exception e)
      {
        e.printStackTrace();
      }
    });
    Assert.assertEquals(items.size(), GET_COUNT);

    verifyStats(pool.getStats(), 1, GET_COUNT, 0, 0, 0, 0, 1, 0, 0);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);

    // Put items back to the pool
    items.stream().forEach(item -> SCHEDULER.execute(() -> pool.put(item)));

    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testShutdownWithPendingDispose() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    // Get a item from the pool
    FutureCallback<Object> getCallback = new FutureCallback<>();
    pool.get(getCallback);
    Object item = getCallback.get(GET_TIMEOUT, TIME_UNIT);
    Assert.assertNotNull(item);

    // Shutdown while the item is still outstanding
    FutureCallback<None> callback = new FutureCallback<>();
    pool.shutdown(callback);
    verifyStats(pool.getStats(), 1, 1, 0, 0, 0, 0, 1, 0, 0);

    // Return the item back the to the pool
    pool.dispose(item);
    verifyStats(pool.getStats(), 0, 0, 0, 1, 0, 1, 1, 0, 0);

    callback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testShutdownWithMultiplePendingDispose() throws Exception
  {
    final AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    final CountDownLatch latch = new CountDownLatch(GET_COUNT);
    final Collection<FutureCallback<Object>> getCallbacks = new ConcurrentLinkedQueue<>();
    IntStream.range(0, GET_COUNT).forEach(i -> SCHEDULER.execute(() -> {
      FutureCallback<Object> getCallback = new FutureCallback<>();
      pool.get(getCallback);
      getCallbacks.add(getCallback);
      latch.countDown();
    }));
    if (!latch.await(OPERATION_TIMEOUT, TIME_UNIT))
    {
      Assert.fail("Timeout waiting for get calls");
    }

    final Collection<Object> items = new ConcurrentLinkedQueue<>();
    getCallbacks.stream().forEach(callback -> {
      try
      {
        items.add(callback.get(GET_TIMEOUT, TIME_UNIT));
      } catch (Exception e)
      {
      }
    });

    Assert.assertEquals(items.size(), GET_COUNT);
    verifyStats(pool.getStats(), 1, GET_COUNT, 0, 0, 0, 0, 1, 0, 0);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);

    // Put items back to the pool
    items.stream().forEach(item -> SCHEDULER.execute(() -> pool.dispose(item)));

    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testShutdownWithPendingDisposedItems() throws Exception
  {
    final AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback1 = new FutureCallback<>();
    FutureCallback<Object> getCallback2 = new FutureCallback<>();
    pool.get(getCallback1);
    pool.get(getCallback2);
    Object item1 = getCallback1.get(GET_TIMEOUT, TIME_UNIT);
    Object item2 = getCallback2.get(GET_TIMEOUT, TIME_UNIT);
    Assert.assertNotNull(item1);
    Assert.assertNotNull(item2);
    Assert.assertSame(item1, item2);
    verifyStats(pool.getStats(), 1, 2, 0, 0, 0, 0, 1, 0, 0);

    pool.dispose(item1);
    verifyStats(pool.getStats(), 0, 0, 0, 0, 0, 0, 1, 0, 0);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);

    // Put items back to the pool
    pool.dispose(item2);
    verifyStats(pool.getStats(), 0, 0, 0, 1, 0, 1, 1, 0, 0);

    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testCancelWaiters() throws Exception
  {
    final LifecycleMock lifecycle = new LifecycleMock();
    final CountDownLatch latch = new CountDownLatch(1);
    lifecycle.setCreateConsumer(callback -> {
      try
      {
        latch.await();
        callback.onSuccess(ITEM);
      } catch (Exception e)
      {
        callback.onError(e);
      }
    });

    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, lifecycle, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    final CountDownLatch getLatch = new CountDownLatch(GET_COUNT - 1);
    IntStream.range(0, GET_COUNT).forEach(i -> SCHEDULER.execute(() -> {
      pool.get(new FutureCallback<>());
      getLatch.countDown();
    }));
    if (!getLatch.await(GET_TIMEOUT, TIME_UNIT))
    {
      Assert.fail("Timed out awaiting for get");
    }

    Collection<Callback<Object>> waiters = pool.cancelWaiters();
    Assert.assertNotNull(waiters);
    Assert.assertEquals(waiters.size(), GET_COUNT);
    verifyStats(pool.getStats(), 0, 0, 0, 0, 0, 0, 0, 0, 0);

    latch.countDown();
    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testSingleGetItemSucceeds() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback = new FutureCallback<>();
    Cancellable cancellable = pool.get(getCallback);
    Assert.assertNotNull(cancellable);

    Object item = getCallback.get(GET_TIMEOUT, TIME_UNIT);
    Assert.assertNotNull(item);
    verifyStats(pool.getStats(), 1, 1, 0, 0, 0, 0, 1, 0, 0);

    pool.put(item);
    verifyStats(pool.getStats(), 1, 0, 1, 0, 0, 0, 1, 0, 0);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testMultipleGetItemSucceeds() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    final List<Object> items = new ArrayList<>(GET_COUNT);
    for (int i = 0; i < GET_COUNT; i++)
    {
      FutureCallback<Object> getCallback = new FutureCallback<>();
      Cancellable cancellable = pool.get(getCallback);

      // Operation should not be cancellable
      Assert.assertNotNull(cancellable);
      Assert.assertEquals(cancellable.cancel(), false);

      Object item = getCallback.get(GET_TIMEOUT, TIME_UNIT);
      Assert.assertNotNull(item);
      items.add(item);
    }

    // All items should essentially be the same instance
    Assert.assertEquals(items.size(), GET_COUNT);
    items.stream().forEach(item -> Assert.assertSame(item, items.get(0)));
    verifyStats(pool.getStats(), 1, GET_COUNT, 0, 0, 0, 0, 1, 0, 0);

    // Put items back to the pool
    IntStream.range(0, GET_COUNT).forEach(i -> pool.put(items.get(i)));

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testMultipleDisposeItemSucceeds() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    final List<Object> items = new ArrayList<>(GET_COUNT);
    for (int i = 0; i < GET_COUNT; i++)
    {
      FutureCallback<Object> getCallback = new FutureCallback<>();
      Cancellable cancellable = pool.get(getCallback);

      // Operation should not be cancellable
      Assert.assertNotNull(cancellable);
      Assert.assertEquals(cancellable.cancel(), false);

      Object item = getCallback.get(GET_TIMEOUT, TIME_UNIT);
      Assert.assertNotNull(item);
      items.add(item);
    }

    // All items should essentially be the same instance
    Assert.assertEquals(items.size(), GET_COUNT);
    items.stream().forEach(item -> Assert.assertSame(item, items.get(0)));
    verifyStats(pool.getStats(), 1, GET_COUNT, 0, 0, 0, 0, 1, 0, 0);

    // Put items back to the pool
    IntStream.range(0, GET_COUNT).forEach(i -> pool.dispose(items.get(i)));

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testMixedPutAndDisposeItemSucceeds() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    final List<Object> items = new ArrayList<>(GET_COUNT);
    for (int i = 0; i < GET_COUNT; i++)
    {
      FutureCallback<Object> getCallback = new FutureCallback<>();
      Cancellable cancellable = pool.get(getCallback);

      // Operation should not be cancellable
      Assert.assertNotNull(cancellable);
      Assert.assertEquals(cancellable.cancel(), false);

      Object item = getCallback.get(GET_TIMEOUT, TIME_UNIT);
      Assert.assertNotNull(item);
      items.add(item);
    }

    // All items should essentially be the same instance
    Assert.assertEquals(items.size(), GET_COUNT);
    items.stream().forEach(item -> Assert.assertSame(item, items.get(0)));
    verifyStats(pool.getStats(), 1, GET_COUNT, 0, 0, 0, 0, 1, 0, 0);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);

    // Put items back to the pool
    IntStream.range(0, GET_COUNT).forEach(i -> {
      if (i % 2 == 0)
      {
        pool.put(items.get(i));
      } else
      {
        pool.dispose(items.get(i));
      }
    });
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testGetOnSuccessCallbackThrows() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    CountDownLatch onSuccessLatch = new CountDownLatch(1);
    pool.get(new Callback<Object>()
    {
      @Override
      public void onSuccess(Object result)
      {
        onSuccessLatch.countDown();
        throw new RuntimeException();
      }

      @Override
      public void onError(Throwable e)
      {
      }
    });

    if (!onSuccessLatch.await(GET_TIMEOUT, TIME_UNIT))
    {
      Assert.fail("Callback onSuccess was not invoked");
    }
  }

  @Test
  public void testGetOnErrorCallbackThrows() throws Exception
  {
    final LifecycleMock lifecycle = new LifecycleMock();
    lifecycle.setCreateConsumer(callback -> callback.onError(new Throwable()));
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, lifecycle, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    CountDownLatch onSuccessLatch = new CountDownLatch(1);
    pool.get(new Callback<Object>()
    {
      @Override
      public void onSuccess(Object result)
      {
      }

      @Override
      public void onError(Throwable e)
      {
        onSuccessLatch.countDown();
        throw new RuntimeException();
      }
    });

    if (!onSuccessLatch.await(GET_TIMEOUT, TIME_UNIT))
    {
      Assert.fail("Callback Error was not invoked");
    }
  }

  @Test
  public void testGetItemCancelled() throws Exception
  {
    final LifecycleMock lifecycle = new LifecycleMock();
    final CountDownLatch createLatch = new CountDownLatch(1);
    lifecycle.setCreateConsumer(callback -> {
      try
      {
        createLatch.await();
        callback.onSuccess(ITEM);
      } catch (Exception e)
      {
        callback.onError(e);
      }
    });

    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, lifecycle, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    // Only one thread will perform the actual item creation task and the rest
    // will return immediately. Therefore we wait for GET_COUNT - 1 threads to complete.
    final CountDownLatch getLatch = new CountDownLatch(GET_COUNT - 1);
    final ConcurrentLinkedQueue<Cancellable> cancellables = new ConcurrentLinkedQueue<>();
    for (int i = 0; i < GET_COUNT; i++)
    {
      SCHEDULER.execute(() -> {
        cancellables.add(pool.get(new FutureCallback<>()));
        getLatch.countDown();
      });
    }

    if (!getLatch.await(GET_TIMEOUT, TIME_UNIT))
    {
      Assert.fail("Timed out awaiting for get");
    }
    Assert.assertEquals(cancellables.size(), GET_COUNT - 1);

    // Cancelling waiters should all succeed
    cancellables.stream().forEach(cancellable -> Assert.assertTrue(cancellable.cancel()));

    // Cancel the last waiter blocking item creation
    Assert.assertEquals(pool.cancelWaiters().size(), 1);

    createLatch.countDown();
    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test(expectedExceptions = ExecutionException.class)
  public void testGetItemCreateFails() throws Exception
  {
    final LifecycleMock lifecycle = new LifecycleMock();
    lifecycle.setCreateConsumer(callback -> callback.onError(new Exception("Simulated create failure")));
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, lifecycle, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback = new FutureCallback<>();
    Cancellable cancellable = pool.get(getCallback);
    Assert.assertNotNull(cancellable);
    verifyStats(pool.getStats(), 0, 0, 0, 0, 0, 0, 0, 1, 0);

    getCallback.get(GET_TIMEOUT, TIME_UNIT);
  }

  @Test(expectedExceptions = ExecutionException.class)
  public void testGetWithNoWaiter() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, NO_WAITER);
    pool.start();

    FutureCallback<Object> callback = new FutureCallback<>();
    Cancellable cancellable = pool.get(callback);

    Assert.assertNotNull(cancellable);
    Assert.assertFalse(cancellable.cancel());

    callback.get(GET_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testGetExceedMaxWaiters() throws Exception
  {
    final int maxWaiters = 5;
    final CountDownLatch latch = new CountDownLatch(1);
    final LifecycleMock lifecycle = new LifecycleMock();
    lifecycle.setCreateConsumer(callback -> {
      try
      {
        latch.await();
        callback.onSuccess(new Object());
      }
      catch (Exception e)
      {
        callback.onError(e);
      }
    });
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, lifecycle, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, maxWaiters);
    pool.start();

    CountDownLatch getLatch = new CountDownLatch(maxWaiters - 1);
    ConcurrentLinkedQueue<FutureCallback<Object>> callbacks = new ConcurrentLinkedQueue<>();
    for (int i = 0; i < maxWaiters; i++)
    {
      SCHEDULER.execute(() -> {
        FutureCallback<Object> callback = new FutureCallback<>();
        Cancellable cancellable = pool.get(callback);
        Assert.assertNotNull(cancellable);
        callbacks.add(callback);
        getLatch.countDown();
      });
    }

    getLatch.await(GET_TIMEOUT, TIME_UNIT);
    FutureCallback<Object> waiterCallback = new FutureCallback<>();
    Cancellable cancellable = pool.get(waiterCallback);
    Assert.assertNotNull(cancellable);
    Assert.assertFalse(cancellable.cancel());
    try
    {
      waiterCallback.get(GET_TIMEOUT, TIME_UNIT);
      Assert.fail("Callback should fail but did not");
    }
    catch (ExecutionException e)
    {
      // Exception is recoverable and expected
    }

    latch.countDown();
    callbacks.forEach(callback -> {
      try
      {
        Object item = callback.get();
        Assert.assertNotNull(item);
        pool.put(item);
      }
      catch (Exception e)
      {
        Assert.fail("Unexpected exception during #get()");
      }
    });

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);

  }

  @Test(expectedExceptions = ExecutionException.class)
  public void testGetItemBeforeStart() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    FutureCallback<Object> callback = new FutureCallback<>();
    pool.get(callback);
    callback.get(GET_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testValidateGetFails() throws Exception
  {
    final LifecycleMock lifecycle = new LifecycleMock();
    lifecycle.setValidateGetSupplier(() -> false);
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, lifecycle, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback1 = new FutureCallback<>();
    pool.get(getCallback1);
    Object item1 = getCallback1.get(GET_TIMEOUT, TIME_UNIT);
    Assert.assertNotNull(item1);
    verifyStats(pool.getStats(), 1, 1, 0, 0, 0, 0, 1, 0, 0);

    FutureCallback<Object> getCallback2 = new FutureCallback<>();
    pool.get(getCallback2);
    Object item2 = getCallback2.get(GET_TIMEOUT, TIME_UNIT);
    Assert.assertNotNull(item2);
    verifyStats(pool.getStats(), 1, 1, 0, 0, 0, 0, 2, 0, 0);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);

    pool.put(item1);
    verifyStats(pool.getStats(), 1, 1, 0, 1, 0, 1, 2, 0, 0);

    pool.put(item2);
    verifyStats(pool.getStats(), 1, 0, 1, 1, 0, 1, 2, 0, 0);

    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testValidatePutFails() throws Exception
  {
    final LifecycleMock lifecycle = new LifecycleMock();
    lifecycle.setValidatePutSupplier(() -> false);
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, lifecycle, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback = new FutureCallback<>();
    pool.get(getCallback);
    Object item = getCallback.get(GET_TIMEOUT, TIME_UNIT);
    Assert.assertNotNull(item);

    pool.put(item);
    verifyStats(pool.getStats(), 0, 0, 0, 1, 0, 1, 1, 0, 0);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testDisposeSucceeds() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback = new FutureCallback<>();
    pool.get(getCallback);
    Object item = getCallback.get(GET_TIMEOUT, TIME_UNIT);
    Assert.assertNotNull(item);

    pool.dispose(item);
    verifyStats(pool.getStats(), 0, 0, 0, 1, 0, 1, 1, 0, 0);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testDisposeWithPendingCheckouts() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback1 = new FutureCallback<>();
    FutureCallback<Object> getCallback2 = new FutureCallback<>();
    pool.get(getCallback1);
    pool.get(getCallback2);
    Object item1 = getCallback1.get(GET_TIMEOUT, TIME_UNIT);
    Object item2 = getCallback2.get(GET_TIMEOUT, TIME_UNIT);
    Assert.assertNotNull(item1);
    Assert.assertNotNull(item2);
    Assert.assertSame(item1, item2);
    verifyStats(pool.getStats(), 1, 2, 0, 0, 0, 0, 1, 0, 0);

    pool.dispose(item1);
    verifyStats(pool.getStats(), 0, 0, 0, 0, 0, 0, 1, 0, 0);

    pool.dispose(item2);
    verifyStats(pool.getStats(), 0, 0, 0, 1, 0, 1, 1, 0, 0);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testDisposeDestroyFails() throws Exception
  {
    final LifecycleMock lifecycle = new LifecycleMock();
    lifecycle.setDestroyConsumer(callback -> callback.onError(new Exception("Simulated destroy failure")));
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, lifecycle, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback = new FutureCallback<>();
    pool.get(getCallback);
    Object item = getCallback.get(GET_TIMEOUT, TIME_UNIT);
    Assert.assertNotNull(item);

    pool.dispose(item);
    verifyStats(pool.getStats(), 0, 0, 0, 0, 1, 1, 1, 0, 0);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test
  public void testPutDestroyedItem() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback1 = new FutureCallback<>();
    pool.get(getCallback1);
    Object item1 = getCallback1.get(GET_TIMEOUT, TIME_UNIT);

    FutureCallback<Object> getCallback2 = new FutureCallback<>();
    pool.get(getCallback2);
    Object item2 = getCallback2.get(GET_TIMEOUT, TIME_UNIT);

    Assert.assertSame(item1, item2);
    verifyStats(pool.getStats(), 1, 2, 0, 0, 0, 0, 1, 0, 0);

    pool.dispose(item1);
    verifyStats(pool.getStats(), 0, 0, 0, 0, 0, 0, 1, 0, 0);

    pool.put(item2);
    verifyStats(pool.getStats(), 0, 0, 0, 1, 0, 1, 1, 0, 0);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testPutItemMismatch() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback = new FutureCallback<>();
    pool.get(getCallback);
    getCallback.get(GET_TIMEOUT, TIME_UNIT);

    // Returns another item reference
    pool.put(new Object());
  }

  @Test
  public void testDisposeDestroyedItem() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback1 = new FutureCallback<>();
    pool.get(getCallback1);
    Object item1 = getCallback1.get(GET_TIMEOUT, TIME_UNIT);

    FutureCallback<Object> getCallback2 = new FutureCallback<>();
    pool.get(getCallback2);
    Object item2 = getCallback2.get(GET_TIMEOUT, TIME_UNIT);

    Assert.assertSame(item1, item2);
    verifyStats(pool.getStats(), 1, 2, 0, 0, 0, 0, 1, 0, 0);

    pool.dispose(item1);
    verifyStats(pool.getStats(), 0, 0, 0, 0, 0, 0, 1, 0, 0);

    pool.dispose(item2);
    verifyStats(pool.getStats(), 0, 0, 0, 1, 0, 1, 1, 0, 0);

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDestroyItemMismatch() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback = new FutureCallback<>();
    pool.get(getCallback);
    Object item = getCallback.get(GET_TIMEOUT, TIME_UNIT);

    // Disposes another item reference
    pool.dispose(new Object());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testExcessivePut() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback = new FutureCallback<>();
    pool.get(getCallback);
    Object item = getCallback.get(GET_TIMEOUT, TIME_UNIT);
    pool.put(item);

    // Excessive put
    pool.put(item);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testExcessiveDestroy() throws Exception
  {
    AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, LIFECYCLE, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    FutureCallback<Object> getCallback = new FutureCallback<>();
    pool.get(getCallback);
    Object item = getCallback.get(GET_TIMEOUT, TIME_UNIT);
    pool.dispose(item);

    // Excessive destroy of item
    pool.dispose(item);
  }

  @DataProvider(name = "lifecycles")
  public Object[][] lifecycleProvider()
  {
    Random random = new Random(System.currentTimeMillis());
    return new Object[][]
    {
        { new LifecycleMock() },
        { new LifecycleMock().setCreateConsumer(callback -> callback.onError(CREATE_ERROR)) },
        { new LifecycleMock().setValidateGetSupplier(() -> false) },
        { new LifecycleMock().setValidatePutSupplier(() -> false) },
        { new LifecycleMock()
            .setValidateGetSupplier(() -> false)
            .setValidatePutSupplier(() -> false) },
        { new LifecycleMock()
            .setCreateConsumer(callback -> callback.onError(CREATE_ERROR))
            .setValidateGetSupplier(() -> false)
            .setValidatePutSupplier(() -> false) },
        { new LifecycleMock()
            .setCreateConsumer(callback -> callback.onError(CREATE_ERROR))
            .setValidatePutSupplier(() -> false) },
        { new LifecycleMock()
            .setCreateConsumer(callback -> callback.onError(CREATE_ERROR))
            .setValidateGetSupplier(() -> false) },
        { new LifecycleMock()
            .setCreateConsumer(callback -> {
              if (random.nextBoolean())
              {
                callback.onSuccess(new Object());
              }
              else
              {
                callback.onError(new Exception("Simulated create failure"));
              }
            })
            .setValidateGetSupplier(() -> random.nextBoolean())
            .setValidatePutSupplier(() -> random.nextBoolean()) },
    };
  }

  @Test(dataProvider = "lifecycles")
  public void testMaximumConcurrency(AsyncPool.Lifecycle<Object> lifecycle) throws Exception
  {
    final AsyncSharedPoolImpl<Object> pool = new AsyncSharedPoolImpl<>(
        POOL_NAME, lifecycle, SCHEDULER, LIMITER, NO_POOL_TIMEOUT, MAX_WAITERS);
    pool.start();

    CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);
    IntStream.range(0, NUMBER_OF_THREADS).forEach(i -> SCHEDULER.execute(() -> {
      try
      {
        FutureCallback<Object> callback = new FutureCallback<>();
        Assert.assertNotNull(pool.get(callback));
        Object item = callback.get(GET_TIMEOUT, TIME_UNIT);
        Assert.assertNotNull(item);
        pool.put(item);
      }
      catch (Exception e)
      {
      }
      finally
      {
        latch.countDown();
      }
    }));

    if (!latch.await(OPERATION_TIMEOUT, TIME_UNIT))
    {
      Assert.fail("Timed out before tasks finish");
    }

    PoolStats stats = pool.getStats();
    System.err.println("Total Created: " + stats.getTotalCreated());

    FutureCallback<None> shutdownCallback = new FutureCallback<>();
    pool.shutdown(shutdownCallback);
    shutdownCallback.get(SHUTDOWN_TIMEOUT, TIME_UNIT);
  }

  private static void verifyStats(PoolStats stats, int poolSize, int checkedOut, int idles, int destroyed,
      int destroyErrors, int badDestroyed, int created, int createErrors, int timeout)
  {
    Assert.assertNotNull(stats);
    Assert.assertEquals(stats.getPoolSize(), poolSize);
    Assert.assertEquals(stats.getCheckedOut(), checkedOut);
    Assert.assertEquals(stats.getIdleCount(), idles);
    Assert.assertEquals(stats.getTotalDestroyed(), destroyed);
    Assert.assertEquals(stats.getTotalDestroyErrors(), destroyErrors);
    Assert.assertEquals(stats.getTotalBadDestroyed(), badDestroyed);
    Assert.assertEquals(stats.getTotalCreated(), created);
    Assert.assertEquals(stats.getTotalCreateErrors(), createErrors);
    Assert.assertEquals(stats.getTotalTimedOut(), timeout);
  }

  public static class LifecycleMock implements AsyncPool.Lifecycle<Object>
  {
    private final AsyncPoolLifecycleStats LIFECYCLE_STATS = new AsyncPoolLifecycleStats(0, 0, 0, 0);

    private Consumer<Callback<Object>> _createConsumer;
    private Consumer<Callback<Object>> _destroyConsumer;
    private BooleanSupplier _validateGetSupplier;
    private BooleanSupplier _validatePutSupplier;
    private Supplier<AsyncPoolLifecycleStats> _statsSupplier;

    public LifecycleMock()
    {
      _createConsumer = null;
      _destroyConsumer = null;
      _validateGetSupplier = () -> true;
      _validatePutSupplier = () -> true;
      _statsSupplier = () -> LIFECYCLE_STATS;
    }

    @Override
    public void create(Callback<Object> callback)
    {
      if (_createConsumer == null)
      {
        callback.onSuccess(new Object());
        return;
      }
      _createConsumer.accept(callback);
    }

    @Override
    public boolean validateGet(Object item)
    {
      return _validateGetSupplier.getAsBoolean();
    }

    @Override
    public boolean validatePut(Object item)
    {
      return _validatePutSupplier.getAsBoolean();
    }

    @Override
    public void destroy(Object item, boolean error, Callback<Object> callback)
    {
      if (_destroyConsumer == null)
      {
        callback.onSuccess(item);
        return;
      }
      _destroyConsumer.accept(callback);
    }

    @Override
    public PoolStats.LifecycleStats getStats()
    {
      return _statsSupplier.get();
    }

    public LifecycleMock setCreateConsumer(Consumer<Callback<Object>> createConsumer)
    {
      _createConsumer = createConsumer;
      return this;
    }

    public LifecycleMock setDestroyConsumer(Consumer<Callback<Object>> destroyConsumer)
    {
      _destroyConsumer = destroyConsumer;
      return this;
    }

    public LifecycleMock setValidateGetSupplier(BooleanSupplier validateGetSupplier)
    {
      _validateGetSupplier = validateGetSupplier;
      return this;
    }

    public LifecycleMock setValidatePutSupplier(BooleanSupplier validatePutSupplier)
    {
      _validatePutSupplier = validatePutSupplier;
      return this;
    }

    public LifecycleMock setStatsSupplier(Supplier<AsyncPoolLifecycleStats> statsSupplier)
    {
      _statsSupplier = statsSupplier;
      return this;
    }
  }
}

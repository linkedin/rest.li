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
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.AsyncPoolImpl;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.AsyncPoolStats;
import com.linkedin.r2.transport.http.client.PoolStats;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
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

  @Test
  public void testGetStats() throws Exception
  {
    final int POOL_SIZE = 25;
    final int GET = 20;
    final int PUT_GOOD = 2;
    final int PUT_BAD = 3;
    final int DISPOSE = 4;
    final int TIMEOUT = 100;
    final int DELAY = 1200;

    final UnreliableLifecycle lifecycle = new UnreliableLifecycle();
    final AsyncPool<AtomicBoolean> pool = new AsyncPoolImpl<AtomicBoolean>(
        "object pool", lifecycle, POOL_SIZE, TIMEOUT, _executor
    );
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
    // When the each create fails, it will retry and cancel the waiter,
    // resulting in a second create error.
    Assert.assertEquals(stats.getTotalCreateErrors(), 2*CREATE_BAD);
  }

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

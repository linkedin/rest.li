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

    public int getHighWaterMark()
    {
      return _highWaterMark;
    }

    public int getLive()
    {
      return _live;
    }
  }

}

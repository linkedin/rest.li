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

package test.r2.transport.http.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.PoolStats;
import com.linkedin.r2.transport.http.client.TimeoutAsyncPoolHandle;
import com.linkedin.r2.util.Cancellable;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;


public class TestTimeoutAsyncPoolHandle
{
  private static final int IMMEDIATE_TIMEOUT = 0;
  private static final int LONG_TIMEOUT = 30;
  private static final int OPERATION_TIMEOUT = 30;
  private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

  private final ScheduledExecutorService _scheduler = Executors.newSingleThreadScheduledExecutor();

  @AfterSuite
  public void doAfterSuites()
  {
    _scheduler.shutdown();
  }

  @Test
  public void testTimeout() throws Exception
  {
    FakePool<Object> pool = new FakePool<>();
    TimeoutAsyncPoolHandle<Object> handle =
        new TimeoutAsyncPoolHandle<>(pool, _scheduler, IMMEDIATE_TIMEOUT, TIME_UNIT, new Object());

    CountDownLatch latch = new CountDownLatch(1);
    handle.addTimeoutTask(() -> latch.countDown());
    latch.await(OPERATION_TIMEOUT, TIME_UNIT);

    Assert.assertEquals(pool.getPutCount(), 0);
    Assert.assertEquals(pool.getDisposeCount(), 1);
  }

  @Test
  public void testBadReleaseAfterTimeout() throws Exception
  {
    FakePool<Object> pool = new FakePool<>();
    TimeoutAsyncPoolHandle<Object> handle = new TimeoutAsyncPoolHandle<>(
        pool, _scheduler, IMMEDIATE_TIMEOUT, TIME_UNIT, new Object());

    CountDownLatch latch = new CountDownLatch(1);
    handle.addTimeoutTask(() -> latch.countDown());
    latch.await(OPERATION_TIMEOUT, TIME_UNIT);

    handle.error().release();
    Assert.assertEquals(pool.getPutCount(), 0);
    Assert.assertEquals(pool.getDisposeCount(), 1);
  }

  @Test
  public void testGoodReleaseAfterTimeout() throws Exception
  {
    FakePool<Object> pool = new FakePool<>();
    TimeoutAsyncPoolHandle<Object> handle = new TimeoutAsyncPoolHandle<>(
        pool, _scheduler, IMMEDIATE_TIMEOUT, TIME_UNIT, new Object());

    CountDownLatch latch = new CountDownLatch(1);
    handle.addTimeoutTask(() -> latch.countDown());
    latch.await(OPERATION_TIMEOUT, TIME_UNIT);

    handle.release();
    Assert.assertEquals(pool.getPutCount(), 0);
    Assert.assertEquals(pool.getDisposeCount(), 1);
  }

  @Test
  public void testBadReleaseBeforeTimeout() throws Exception
  {
    FakePool<Object> pool = new FakePool<>();
    TimeoutAsyncPoolHandle<Object> handle = new TimeoutAsyncPoolHandle<>(
        pool, _scheduler, LONG_TIMEOUT, TIME_UNIT, new Object());

    handle.error().release();
    Assert.assertEquals(pool.getPutCount(), 0);
    Assert.assertEquals(pool.getDisposeCount(), 1);
  }

  @Test
  public void testGoodReleaseBeforeTimeout() throws Exception
  {
    FakePool<Object> pool = new FakePool<>();
    TimeoutAsyncPoolHandle<Object> handle = new TimeoutAsyncPoolHandle<>(
        pool, _scheduler, LONG_TIMEOUT, TIME_UNIT, new Object());

    handle.release();
    Assert.assertEquals(pool.getPutCount(), 1);
    Assert.assertEquals(pool.getDisposeCount(), 0);
  }

  private class FakePool<T> implements AsyncPool<T>
  {
    private volatile int _putCount = 0;
    private volatile int _disposeCount = 0;

    public int getPutCount()
    {
      return _putCount;
    }

    public int getDisposeCount()
    {
      return _disposeCount;
    }

    @Override
    public String getName()
    {
      return null;
    }

    @Override
    public void start()
    {
    }

    @Override
    public void shutdown(Callback<None> callback)
    {
    }

    @Override
    public Collection<Callback<T>> cancelWaiters()
    {
      return null;
    }

    @Override
    public Cancellable get(Callback<T> callback)
    {
      return null;
    }

    @Override
    public void put(T obj)
    {
      _putCount += 1;
    }

    @Override
    public void dispose(T obj)
    {
      _disposeCount += 1;
    }

    @Override
    public PoolStats getStats()
    {
      return null;
    }
  }
}

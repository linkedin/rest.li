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

package com.linkedin.r2.transport.http.client;

import com.linkedin.r2.util.Timeout;
import com.linkedin.r2.util.TimeoutExecutor;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


/**
 * Wraps an {@link AsyncPool} object with an associated timeout. Provides an interface to return or
 * dispose the pool object by invoking #put or #dispose respectively. If either #put or #dispose method
 * is invoked prior to timeout expires, the timeout is cancelled. Otherwise, when timeout expires, the
 * wrapped item is disposed to the async pool and subsequent invocations to #put and #dispose become no-op.
 *
 * @author Sean Sheng
 * @param <T>
 */
public class TimeoutAsyncPoolHandle<T> implements AsyncPoolHandle<T>, TimeoutExecutor
{
  private final AsyncPool<T> _pool;
  private final Timeout<T> _timeout;

  public TimeoutAsyncPoolHandle(
      AsyncPool<T> pool, ScheduledExecutorService scheduler, long timeout, TimeUnit unit, T item)
  {
    _pool = pool;
    _timeout = new Timeout<>(scheduler, timeout, unit, item);
    _timeout.addTimeoutTask(() -> _pool.dispose(item));
  }

  @Override
  public void release()
  {
    doTimeoutAwareAction(_pool::put);
  }

  @Override
  public void dispose()
  {
    doTimeoutAwareAction(_pool::dispose);
  }

  @Override
  public AsyncPool<T> pool()
  {
    return _pool;
  }

  @Override
  public void addTimeoutTask(Runnable task)
  {
    _timeout.addTimeoutTask(task);
  }

  /**
   * Cancels timeout and executes the {@link Consumer} action on the pool object if timeout is
   * not expired. Otherwise, do nothing.
   *
   * @param action {@link Consumer} action to be executed
   */
  private void doTimeoutAwareAction(Consumer<T> action)
  {
    Optional.of(_timeout.getItem()).ifPresent(action);
  }
}
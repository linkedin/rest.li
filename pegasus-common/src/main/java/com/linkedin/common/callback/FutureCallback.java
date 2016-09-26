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

/* $Id$ */
package com.linkedin.common.callback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple Future that does not support cancellation.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class FutureCallback<T> implements Future<T>, Callback<T>
{
  private final AtomicReference<Result<T>> _result = new AtomicReference<Result<T>>();
  private final CountDownLatch _doneLatch = new CountDownLatch(1);

  @Override
  public boolean cancel(boolean mayInterruptIfRunning)
  {
    // cancellation is not supported
    return false;
  }

  @Override
  public boolean isCancelled()
  {

    // cancellation is not supported
    return false;
  }

  @Override
  public boolean isDone()
  {
    return _doneLatch.getCount() == 0;
  }

  @Override
  public T get() throws InterruptedException, ExecutionException
  {
    _doneLatch.await();
    return unwrapResult();
  }

  private T getRaw() throws Throwable
  {
    _doneLatch.await();
    return unwrapResultRaw();
  }

  @Override
  public T get(final long timeout, final TimeUnit unit) throws InterruptedException,
      ExecutionException,
      TimeoutException
  {
    if (!_doneLatch.await(timeout, unit))
    {
      throw new TimeoutException();
    }
    return unwrapResult();
  }

  private T getRaw(final long timeout, final TimeUnit unit) throws Throwable
  {
    if (!_doneLatch.await(timeout, unit))
    {
      throw new TimeoutException();
    }
    return unwrapResultRaw();
  }

  @Override
  public void onSuccess(final T t)
  {
    safeSetValue(Result.createSuccess(t));
    _doneLatch.countDown();
  }

  @Override
  public void onError(final Throwable e)
  {
    Throwable error = e != null ? e : new NullPointerException("Null error is passed to onError!");
    safeSetValue(Result.<T>createError(error));
    _doneLatch.countDown();
  }

  private void safeSetValue(final Result<T> result)
  {
    assert result != null;

    if (!_result.compareAndSet(null, result))
    {
      throw new IllegalStateException("Callback already invoked. Value will not be changed. "
          + "Proposed value: "
          + result.getValue()
          + ". Original value: "
          + _result.get().getValue());
    }
  }

  private T unwrapResult() throws ExecutionException
  {
    try
    {
      return unwrapResultRaw();
    }
    catch (Throwable e)
    {
      throw new ExecutionException(e);
    }
  }

  private T unwrapResultRaw() throws Throwable
  {
    final Result<T> result = _result.get();
    assert result != null;

    if (result.isSuccess())
    {
      return result.getResult();
    }

    throw result.getError();
  }

  private static final class Result<T>
  {
    private final boolean _isSuccess;
    private final T _result;
    private final Throwable _ex;

    public static <T> Result<T> createSuccess(final T t)
    {
      return new Result<T>(t, null, true);
    }

    public static <T> Result<T> createError(final Throwable e)
    {
      return new Result<T>(null, e, false);
    }

    private Result(final T result, final Throwable ex, final boolean isSuccess)
    {
      _result = result;
      _ex = ex;
      _isSuccess = isSuccess;
    }

    public T getResult()
    {
      return _result;
    }

    public Throwable getError()
    {
      return _ex;
    }

    public boolean isSuccess()
    {
      return _isSuccess;
    }

    public Object getValue()
    {
      return isSuccess() ? _result : _ex;
    }
  }
}

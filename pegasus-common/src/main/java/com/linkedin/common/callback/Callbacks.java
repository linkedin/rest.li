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

import com.linkedin.common.util.None;

/**
 * A factory for {@link Callback}s.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class Callbacks
{
  @SuppressWarnings("rawtypes")
  private static final NullCallback NULL_CALLBACK = new NullCallback();

  private Callbacks()
  {
  }

  /**
   * Returns a callback that does nothing. Useful when the caller does not need a
   * notification of completion.
   *
   * @param <T>
   *          the type of the response
   * @return the empty callback
   */
  @SuppressWarnings("unchecked")
  public static <T> Callback<T> empty()
  {
    return NULL_CALLBACK;
  }

  /**
   * Converts a simple callback into a regular callback that can be used with most
   * asynchronous operations that take a callback.
   *
   * @param simpleCallback
   *          the callback to adapt
   * @return a regular {@link Callback}
   */
  public static <T> Callback<T> adaptSimple(final SimpleCallback simpleCallback)
  {
    return new SimpleCallbackAdapter<T>(simpleCallback);
  }

  /**
   * Returns a new callback which defers invocation of another callback until a specified
   * number of onSuccess() or onError() calls to the new callback have occured.
   *
   * @param callback
   *          the underlying callback
   * @param count
   *          the number of combined onSuccess()/onError() calls that must be invoked on
   *          the new callback before the underlying callback's onSuccess (if all were
   *          successful) or onError (if any errors occurred) method is invoked. If count
   *          is zero, the underlying callback's onSuccess will be invoked immediately on
   *          the calling thread.
   * @return the new callback
   */
  public static Callback<None> countDown(final Callback<None> callback, final int count)
  {
    if (count == 0)
    {
      callback.onSuccess(None.none());
      return empty();
    }
    return new MultiCallback(callback, count);
  }

  /**
   * Creates a new callback with the following behavior:
   *
   * <ul>
   * <li>{@code onSuccess} invoked - takes the argument, passes it to {@code fun}, and
   * passes that result to {@code callback.onSuccess}. If an error is thrown from
   * {@code fun} then {@code callback.onError} is invoked with the error.</li>
   * <li>{@code onError} invoked - simply invokes {@code callback.onError} with the error.
   * </li>
   * </ul>
   */
  public static <IN, OUT> Callback<IN> mapValue(final Callback<OUT> callback,
                                                final Function<IN, OUT> fun)
  {
    return new Callback<IN>()
    {
      @Override
      public void onError(final Throwable error)
      {
        callback.onError(error);
      }

      @Override
      public void onSuccess(final IN in)
      {
        final OUT out;
        try
        {
          out = fun.map(in);
        }
        catch (Exception e)
        {
          callback.onError(e);
          return;
        }
        catch (Throwable t)
        {
          // TODO: remove unnecessary wrap and catch clause when Callback allows Throwable
          callback.onError(new Exception(t));
          return;
        }
        callback.onSuccess(out);
      }
    };
  }

  private static class NullCallback<T> implements Callback<T>
  {
    @Override
    public void onSuccess(final Object o)
    {
    }

    @Override
    public void onError(final Throwable e)
    {
    }
  }

  private static class SimpleCallbackAdapter<T> implements Callback<T>
  {
    private final SimpleCallback _callback;

    public SimpleCallbackAdapter(final SimpleCallback simpleCallback)
    {
      _callback = simpleCallback;
    }

    @Override
    public void onSuccess(final T t)
    {
      _callback.onDone();
    }

    @Override
    public void onError(final Throwable e)
    {
      _callback.onDone();
    }
  }
}

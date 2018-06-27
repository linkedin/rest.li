/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.util.SingleTimeout;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * A Callback wrapper with associated timeout. If the TimeoutCallback's onSuccess or onError
 * method is invoked before the timeout expires, the timeout is cancelled.  Otherwise, when
 * the timeout expires, the wrapped Callback's onError method is invoked with a
 * {@link TimeoutException}.
 *
 * @author Steven Ihde
 */

public class TimeoutCallback<T> implements Callback<T>
{
  private final SingleTimeout<Callback<T>> _timeout;

  public TimeoutCallback(ScheduledExecutorService executor, long timeout, TimeUnit timeoutUnit,
                               final Callback<T> callback)
  {
    this(executor, timeout, timeoutUnit, callback, null);
  }

  /**
   * Construct a new instance.
   *
   * @param executor the {@link ScheduledExecutorService} used to schedule the timeout
   * @param timeout the timeout delay, in the specified {@link TimeUnit}.
   * @param timeoutUnit the {@link TimeUnit} for the timeout parameter.
   * @param callback the {@link Callback} to be invoked on success or error.
   * @param timeoutMessage the message to be included in the {@link TimeoutException} if a
   *                       timeout occurs.
   */
  public TimeoutCallback(ScheduledExecutorService executor, long timeout, TimeUnit timeoutUnit,
                               final Callback<T> callback, final String timeoutMessage)
  {
    _timeout = new SingleTimeout<>(executor, timeout, timeoutUnit, callback, () -> callback.onError(
      new TimeoutException(
        "Exceeded request timeout of " + timeoutUnit.toMillis(timeout) + "ms: " + timeoutMessage)));
  }

  @Override
  public void onSuccess(T t)
  {
    Callback<T> callback = _timeout.getItem();
    if (callback != null)
    {
      callback.onSuccess(t);
    }
  }

  @Override
  public void onError(Throwable e)
  {
    Callback<T> callback = _timeout.getItem();
    if (callback != null)
    {
      callback.onError(e);
    }
  }
}

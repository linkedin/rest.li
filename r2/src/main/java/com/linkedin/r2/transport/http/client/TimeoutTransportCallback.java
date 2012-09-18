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

package com.linkedin.r2.transport.http.client;

import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.util.Timeout;
import com.linkedin.r2.util.TimeoutExecutor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A TransportCallback wrapper with associated timeout.  If the TimeoutTransportCallback's
 * onResponse method is invoked before the timeout expires, the timeout is cancelled.
 * Otherwise, when the timeout expires, the wrapped TransportCallback's onResponse method is
 * invoked with a {@link java.util.concurrent.TimeoutException}.
 *
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TimeoutTransportCallback<T> implements TransportCallback<T>, TimeoutExecutor
{
  private final Timeout<TransportCallback<T>> _timeout;

  /**
   * Construct a new instance using the specified parameters.
   *
   * @param scheduler the {@link ScheduledExecutorService} used to schedule the timeout
   * @param timeout the timeout delay, in the specified {@link TimeUnit}.
   * @param timeoutUnit the {@link TimeUnit} for the timeout parameter.
   * @param callback the {@link TransportCallback} to be invoked on success or error.
   * @param timeoutMessage the message to be included in the {@link TimeoutException} if a
   *                       timeout occurs.
   */
  public TimeoutTransportCallback(ScheduledExecutorService scheduler,
                                  long timeout,
                                  TimeUnit timeoutUnit,
                                  final TransportCallback<T> callback,
                                  final String timeoutMessage)
  {
    _timeout = new Timeout<TransportCallback<T>>(scheduler, timeout, timeoutUnit, callback);
    _timeout.addTimeoutTask(new Runnable()
    {
      @Override
      public void run()
      {
        callback.onResponse(TransportResponseImpl.<T>error(new TimeoutException(timeoutMessage)));
      }
    });
  }

  @Override
  public void onResponse(TransportResponse<T> response)
  {
    TransportCallback<T> callback = _timeout.getItem();
    if (callback != null)
    {
      callback.onResponse(response);
    }
  }

  @Override
  public void addTimeoutTask(Runnable task)
  {
    _timeout.addTimeoutTask(task);
  }
}

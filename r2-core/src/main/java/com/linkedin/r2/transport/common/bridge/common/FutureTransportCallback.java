/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.transport.common.bridge.common;

import com.linkedin.common.callback.FutureCallback;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Simple future {@link TransportCallback} that does not support cancellation.
 *
 * @author Sean Sheng
 * @param <T>
 */
public class FutureTransportCallback<T> implements Future<TransportResponse<T>>, TransportCallback<T>
{
  private final FutureCallback<TransportResponse<T>> _futureCallback = new FutureCallback<>();

  @Override
  public void onResponse(TransportResponse<T> response)
  {
    _futureCallback.onSuccess(response);
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return _futureCallback.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return _futureCallback.isCancelled();
  }

  @Override
  public boolean isDone() {
    return _futureCallback.isDone();
  }

  @Override
  public TransportResponse<T> get() throws InterruptedException, ExecutionException {
    return _futureCallback.get();
  }

  @Override
  public TransportResponse<T> get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return _futureCallback.get(timeout, unit);
  }
}

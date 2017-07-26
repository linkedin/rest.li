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

package com.linkedin.r2.transport.http.client;

import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A TransportCallback wrapper that ensure onTransport being called only once
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class InvokedOnceTransportCallback<T> implements TransportCallback<T>
{
  private final AtomicReference<TransportCallback<T>> _callbackRef;

  public InvokedOnceTransportCallback(final TransportCallback<T> callback)
  {
    _callbackRef = new AtomicReference<>(callback);
  }

  @Override
  public void onResponse(TransportResponse<T> response)
  {
    TransportCallback<T> callback = _callbackRef.getAndSet(null);
    if (callback != null)
    {
      callback.onResponse(response);
    }
  }
}

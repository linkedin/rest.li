/*
   Copyright (c) 2015 LinkedIn Corp.

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A TransportCallback wrapper which ensures the #onResponse() method of the
 * wrapped callback is always invoked by the dedicated {@link ExecutorService}.
 *
 * @author Ang Xu
 * @version $Revision: $
 */
public class ExecutionCallback<T> implements TransportCallback<T>
{
  private static final Logger LOG = LoggerFactory.getLogger(ExecutionCallback.class);

  private final ExecutorService _executor;
  private AtomicReference<TransportCallback<T>>  _callbackRef;

  /**
   * Construct a new instance.
   *
   * @param executor the {@link ExecutorService} used to execute the given {@link TransportCallback}.
   * @param callback the {@link TransportCallback} to be invoked on success or error.
   */
  public ExecutionCallback(ExecutorService executor, TransportCallback<T> callback)
  {
    _executor = executor;
    _callbackRef = new AtomicReference<TransportCallback<T>>(callback);
  }

  @Override
  public void onResponse(final TransportResponse<T> response)
  {
    _executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        TransportCallback<T> callback = _callbackRef.getAndSet(null);
        if (callback != null)
        {
          callback.onResponse(response);
        }
        else
        {
          LOG.warn("Received response {} while _callback is null. Ignored.", response.getResponse());
        }
      }
    });
  }
}

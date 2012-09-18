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
package com.linkedin.r2.transport.common.bridge.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TransportCallbackAdapter<T> implements TransportCallback<T>
{
  private Callback<T> _callback;

  /**
   * Construct a new instance which delegates to the specified {@link Callback}.
   *
   * @param callback {@link Callback} to use as a delegate.
   */
  public TransportCallbackAdapter(Callback<T> callback)
  {
    _callback = callback;
  }

  @Override
  public void onResponse(TransportResponse<T> response)
  {
    if (response.hasError())
    {
      _callback.onError(response.getError());
      return;
    }

    _callback.onSuccess(response.getResponse());
  }
}

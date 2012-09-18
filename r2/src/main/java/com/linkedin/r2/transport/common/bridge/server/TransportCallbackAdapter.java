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
package com.linkedin.r2.transport.common.bridge.server;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class TransportCallbackAdapter<T> implements Callback<T>
{
  private final TransportCallback<T> _callback;

  /**
   * Construct a new instance which adapts the specified {@link TransportCallback}.
   *
   * @param callback the {@link TransportCallback} to be adapted.
   */
  public TransportCallbackAdapter(TransportCallback<T> callback)
  {
    _callback = callback;
  }

  @Override
  public void onSuccess(T res)
  {
    final Map<String, String> wireAttrs = new HashMap<String, String>();
    _callback.onResponse(TransportResponseImpl.success(res, wireAttrs));
  }

  @Override
  public void onError(Throwable e)
  {
    final Map<String, String> wireAttrs = new HashMap<String, String>();
    _callback.onResponse(TransportResponseImpl.<T>error(e, wireAttrs));
  }
}

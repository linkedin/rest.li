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

package com.linkedin.r2.transport.common.bridge.common;

import com.linkedin.r2.message.Request;
import com.linkedin.r2.transport.http.client.AsyncPoolHandle;
import com.linkedin.util.ArgumentUtil;


/**
 * Simple wrapper of an R2 {@link Request} implementation and a {@link TransportCallback}
 *
 * @param <R> An implementation of R2 {@link Request}
 * @param <C> An implementation of {@link TransportCallback}
 * @param <H> An implementation of {@link AsyncPoolHandle}
 */
public class RequestWithCallback<R extends Request, C extends TransportCallback<?>, H extends AsyncPoolHandle<?>>
{
  private final R _request;
  private final C _callback;
  private final H _handle;

  public RequestWithCallback(R request, C callback, H handle)
  {
    ArgumentUtil.notNull(request, "request");
    ArgumentUtil.notNull(callback, "callback");
    ArgumentUtil.notNull(handle, "handle");

    _request = request;
    _callback = callback;
    _handle = handle;
  }

  public R request()
  {
    return _request;
  }

  public C callback()
  {
    return _callback;
  }

  public H handle()
  {
    return _handle;
  }
}

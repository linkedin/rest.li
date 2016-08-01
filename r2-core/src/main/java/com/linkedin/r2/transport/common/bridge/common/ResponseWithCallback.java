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

import com.linkedin.r2.message.Response;
import com.linkedin.util.ArgumentUtil;


/**
 * Simple wrapper of an R2 {@link Response} implementation and a {@link TransportCallback}
 *
 * @param <R> An implementation of R2 {@link Response}
 * @param <C> An implementation of {@link TransportCallback}
 */
public class ResponseWithCallback<R extends Response, C extends TransportCallback<?>>
{
  private final R _response;
  private final C _callback;

  public ResponseWithCallback(R response, C callback)
  {
    ArgumentUtil.notNull(response, "response");
    ArgumentUtil.notNull(callback, "callback");

    _response = response;
    _callback = callback;
  }

  public R response()
  {
    return _response;
  }

  public C callback()
  {
    return _callback;
  }
}

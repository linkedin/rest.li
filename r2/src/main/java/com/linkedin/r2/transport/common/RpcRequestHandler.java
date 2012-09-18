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
package com.linkedin.r2.transport.common;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcResponse;

/**
 * A request handler for {@link RpcRequest}s. Request handlers can act either as dispatchers to
 * other request handlers or as the final request handler (i.e. service). Dispatchers can be
 * composed to provide more complicated routing decisions.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface RpcRequestHandler
{
  /**
   * Handles the supplied request and notifies the supplied callback upon completion.<p/>
   *
   * @param request the request to process
   * @param callback the callback to notify when request processing has completed
   */
  void handleRequest(RpcRequest request, Callback<RpcResponse> callback);
}

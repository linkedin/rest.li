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
package com.linkedin.r2.filter.message.rpc;

import com.linkedin.r2.filter.Filter;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcResponse;

import java.util.Map;

/**
 * A filter that processes {@link RpcRequest}s.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface RpcRequestFilter extends Filter
{

  /**
   * Method to be invoked for each {@link RpcRequest} message.
   *
   * @param req the {@link RpcRequest} message.
   * @param requestContext the {@link RequestContext} of the request.
   * @param wireAttrs the wire attributes of the request.
   * @param nextFilter the next filter in the chain.  Concrete implementations should invoke
   *                   {@link NextFilter#onRequest} to continue the filter chain.
   */
  void onRpcRequest(RpcRequest req,
                    RequestContext requestContext,
                    Map<String, String> wireAttrs,
                    NextFilter<RpcRequest, RpcResponse> nextFilter);
}

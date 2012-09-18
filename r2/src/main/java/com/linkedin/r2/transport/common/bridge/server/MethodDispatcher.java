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
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.transport.common.RpcRequestHandler;
import com.linkedin.r2.util.URIUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * A dispatcher that dispatches based on the last path segment of the URI (as defined by RFC 2396).
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class MethodDispatcher implements RpcRequestHandler
{
  private final Map<String, RpcRequestHandler> _methods;

  public static class Builder
  {
    private final Map<String, RpcRequestHandler> _methods = new HashMap<String, RpcRequestHandler>();

    /**
     * Add a method mapping to this Builder instance.
     *
     * @param methodName the method name to be mapped.
     * @param handler the handler for the specified method.
     * @return the current builder instance (fluent interface pattern).
     */
    public Builder addMethod(String methodName, RpcRequestHandler handler)
    {
      _methods.put(methodName, handler);
      return this;
    }

    /**
     * Build a new {@link MethodDispatcher} using this Builder's settings.
     *
     * @return a new {@link MethodDispatcher} using this Builder's settings.
     */
    public MethodDispatcher build()
    {
      return new MethodDispatcher(new HashMap<String, RpcRequestHandler>(_methods));
    }
  }

  private MethodDispatcher(Map<String, RpcRequestHandler> methods)
  {
    _methods = methods;
  }

  @Override
  public void handleRequest(RpcRequest req, Callback<RpcResponse> callback)
  {
    final String[] pathSegs = URIUtil.tokenizePath(req.getURI().getPath());
    if (pathSegs.length == 0)
    {
      callback.onError(new RuntimeException("URI is missing path information for dispatch: " + req.getURI()));
      return;
    }

    final String methodName = pathSegs[pathSegs.length - 1];
    final RpcRequestHandler method = _methods.get(methodName);
    if (method == null)
    {
      callback.onError(new RuntimeException("No method handler registered for: " + methodName));
      return;
    }

    method.handleRequest(req, callback);
  }
}

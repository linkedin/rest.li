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
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.common.util.None;

import java.util.concurrent.Future;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface Client
{
  /**
   * Asynchronously issues the given request and returns a {@link Future} that can be used to wait
   * for the response.
   *
   *
   * @param request the request to issue
   * @return a future to wait for the response
   */
  Future<RestResponse> restRequest(RestRequest request);

  /**
   * Asynchronously issues the given request and returns a {@link Future} that can be used to wait
   * for the response.
   *
   *
   * @param request the request to issue
   * @param requestContext context for the request
   * @return a future to wait for the response
   */
  Future<RestResponse> restRequest(RestRequest request, RequestContext requestContext);

  /**
   * Asynchronously issues the given request. The given callback is invoked when the response is
   * received. This event driven approach is typically more complicated to use and is appropriate
   * for building other abstractions, such as a DAG based resolver.
   *
   * @param request the request to issue
   * @param callback the callback to invoke with the response
   */
  void restRequest(RestRequest request, Callback<RestResponse> callback);

  /**
   * Asynchronously issues the given request. The given callback is invoked when the response is
   * received. This event driven approach is typically more complicated to use and is appropriate
   * for building other abstractions, such as a DAG based resolver.
   *
   * @param request the request to issue
   * @param requestContext context for the request
   * @param callback the callback to invoke with the response
   */
  void restRequest(RestRequest request, RequestContext requestContext,
                   Callback<RestResponse> callback);

  /**
   * Asynchronously issues the given request and returns a {@link Future} that can be used to wait
   * for the response.
   *
   *
   * @param request the request to issue
   * @return a future to wait for the response
   */
  Future<RpcResponse> rpcRequest(RpcRequest request);

  /**
   * Asynchronously issues the given request and returns a {@link Future} that can be used to wait
   * for the response.
   *
   *
   * @param request the request to issue
   * @param requestContext context for the request
   * @return a future to wait for the response
   */
  Future<RpcResponse> rpcRequest(RpcRequest request, RequestContext requestContext);

  /**
   * Asynchronously issues the given request. The given callback is invoked when the response is
   * received. This event driven approach is typically more complicated to use and is appropriate
   * for building other abstractions, such as a DAG based resolver.
   *
   * @param request the request to issue
   * @param callback the callback to invoke with the response
   */
  void rpcRequest(RpcRequest request, Callback<RpcResponse> callback);

  /**
   * Asynchronously issues the given request. The given callback is invoked when the response is
   * received. This event driven approach is typically more complicated to use and is appropriate
   * for building other abstractions, such as a DAG based resolver.
   *
   * @param request the request to issue
   * @param requestContext context for the request
   * @param callback the callback to invoke with the response
   */
  void rpcRequest(RpcRequest request, RequestContext requestContext, Callback<RpcResponse> callback);

  /**
   * Starts asynchronous shutdown of the client. This method should block minimally, if at all.
   *
   * @param callback a callback to invoke when the shutdown is complete
   */
  void shutdown(Callback<None> callback);
}
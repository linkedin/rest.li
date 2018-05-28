/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.restli.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.client.multiplexer.MultiplexedRequest;
import com.linkedin.restli.client.multiplexer.MultiplexedResponse;


/**
 * Rest.li client interface with overloading methods for sending Rest.li {@link Request}
 *
 * @author Sean Sheng
 */
public interface Client
{
  /**
   * Resource name of {@link MultiplexedRequest}
   */
  String MULTIPLEXER_RESOURCE = "mux";

  /**
   * Batching strategy for partition and sticky routine support
   */
  String SCATTER_GATHER_STRATEGY = "SCATTER_GATHER_STRATEGY";

  /**
   * Shuts down the underlying {@link com.linkedin.r2.transport.common.Client} which this RestClient wraps.
   * @param callback
   */
  void shutdown(Callback<None> callback);

  /**
   * Sends a type-bound REST request, returning a future.
   *
   *
   * @param request to send
   * @param requestContext context for the request
   * @return response future
   */
  <T> ResponseFuture<T> sendRequest(Request<T> request, RequestContext requestContext);

  /**
   * Sends a type-bound REST request, returning a future.
   *
   *
   * @param request to send
   * @param requestContext context for the request
   * @param errorHandlingBehavior error handling behavior
   * @return response future
   */
  <T> ResponseFuture<T> sendRequest(Request<T> request, RequestContext requestContext,
      ErrorHandlingBehavior errorHandlingBehavior);

  /**
   * Sends a type-bound REST request, returning a future.
   *
   *
   * @param requestBuilder to invoke {@link RequestBuilder#build()} on to obtain the request
   *                       to send.
   * @param requestContext context for the request
   * @return response future
   */
  <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder, RequestContext requestContext);

  /**
   * Sends a type-bound REST request, returning a future.
   *
   *
   * @param requestBuilder to invoke {@link RequestBuilder#build()} on to obtain the request
   *                       to send.
   * @param requestContext context for the request
   * @param errorHandlingBehavior error handling behavior
   * @return response future
   */
  <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder, RequestContext requestContext,
      ErrorHandlingBehavior errorHandlingBehavior);

  /**
   * Sends a type-bound REST request using a callback.
   *
   * @param request to send
   * @param requestContext context for the request
   * @param callback to call on request completion. In the event of an error, the callback
   *                 will receive a {@link com.linkedin.r2.RemoteInvocationException}. If a valid
   *                 error response was received from the remote server, the callback will receive
   *                 a {@link RestLiResponseException} containing the error details.
   */
  <T> void sendRequest(Request<T> request, RequestContext requestContext, Callback<Response<T>> callback);

  /**
   * Sends a type-bound REST request using a callback.
   *
   * @param requestBuilder to invoke {@link RequestBuilder#build()} on to obtain the request
   *                       to send.
   * @param requestContext context for the request
   * @param callback to call on request completion. In the event of an error, the callback
   *                 will receive a {@link com.linkedin.r2.RemoteInvocationException}. If a valid
   *                 error response was received from the remote server, the callback will receive
   *                 a {@link RestLiResponseException} containing the error details.
   */
  <T> void sendRequest(RequestBuilder<? extends Request<T>> requestBuilder, RequestContext requestContext,
      Callback<Response<T>> callback);

  /**
   * Sends a type-bound REST request, returning a future
   * @param request to send
   * @return response future
   */
  <T> ResponseFuture<T> sendRequest(Request<T> request);

  /**
   * Sends a type-bound REST request, returning a future
   * @param request to send
   * @param errorHandlingBehavior error handling behavior
   * @return response future
   */
  <T> ResponseFuture<T> sendRequest(Request<T> request, ErrorHandlingBehavior errorHandlingBehavior);

  /**
   * Sends a type-bound REST request, returning a future
   *
   * @param requestBuilder to invoke {@link RequestBuilder#build()} on to obtain the request
   *                       to send.
   * @return response future
   */
  <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder);

  /**
   * Sends a type-bound REST request, returning a future
   *
   * @param requestBuilder to invoke {@link RequestBuilder#build()} on to obtain the request
   *                       to send.
   * @param errorHandlingBehavior error handling behavior
   * @return response future
   */
  <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder,
      ErrorHandlingBehavior errorHandlingBehavior);

  /**
   * Sends a type-bound REST request using a callback.
   *
   * @param request to send
   * @param callback to call on request completion. In the event of an error, the callback
   *                 will receive a {@link com.linkedin.r2.RemoteInvocationException}. If a valid
   *                 error response was received from the remote server, the callback will receive
   *                 a {@link RestLiResponseException} containing the error details.
   */
  <T> void sendRequest(Request<T> request, Callback<Response<T>> callback);

  /**
   * Sends a type-bound REST request using a callback.
   *
   * @param requestBuilder to invoke {@link RequestBuilder#build()} on to obtain the request
   *                       to send.
   * @param callback to call on request completion. In the event of an error, the callback
   *                 will receive a {@link com.linkedin.r2.RemoteInvocationException}. If a valid
   *                 error response was received from the remote server, the callback will receive
   *                 a {@link RestLiResponseException} containing the error details.
   */
  <T> void sendRequest(RequestBuilder<? extends Request<T>> requestBuilder, Callback<Response<T>> callback);

  /**
   * Sends a multiplexed request. Responses are provided to individual requests' callbacks.
   *
   * The request is sent using the protocol version 2.0.
   *
   * @param multiplexedRequest the request to send.
   */
  void sendRequest(MultiplexedRequest multiplexedRequest);

  /**
   * Sends a multiplexed request. Responses are provided to individual requests' callbacks. After all responses are
   * received the given aggregated callback is invoked.
   *
   * The request is sent using the protocol version 2.0.
   *
   * @param multiplexedRequest  the multiplexed request to send.
   * @param callback the aggregated response callback.
   */
  void sendRequest(MultiplexedRequest multiplexedRequest, Callback<MultiplexedResponse> callback);

  /**
   * Sends a multiplexed request. Responses are provided to individual requests' callbacks. After all responses are
   * received the given aggregated callback is invoked.
   *
   * The request is sent using the protocol version 2.0.
   *
   * @param multiplexedRequest  the multiplexed request to send.
   * @param requestContext context for the request
   * @param callback the aggregated response callback.
   */
  void sendRequest(MultiplexedRequest multiplexedRequest, RequestContext requestContext,
      Callback<MultiplexedResponse> callback);
}

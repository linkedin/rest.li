/*
   Copyright (c) 2018 LinkedIn Corp.

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
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.client.multiplexer.MultiplexedRequest;
import com.linkedin.restli.client.multiplexer.MultiplexedResponse;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * The purpose of this class is to be used when transitioning from RestClient to Client. It can be difficult to change
 * long-standing code that uses RestClient all over its API or is no longer maintained. Using the ForwardingRestClient
 * allows users to get the same benefits as using a Client for the majority of methods.
 *
 * If you are considering using this class, strongly consider using {@link Client} instead. This class is a shim for
 * RestClient compatibility and is not intended for any new development.
 *
 * Forwards all calls from RestClient to a Client delegate. A RestClient delegate is also provided for cases where
 * RestClient-only methods are called. If a RestClient-only method is called, but the RestClient was not supplied, an
 * {@link UnsupportedOperationException} will be thrown. In future versions, RestClient will be changing its public API
 * to the same as Client and the fallback RestClient constructor will be removed.
 *
 * @author Gil Cottle
 */
public class ForwardingRestClient extends RestClient implements Client {

  private final Client _client;
  private final RestClient _restClient;

  /**
   * @param client delegate for all Client calls
   */
  public ForwardingRestClient(@Nonnull Client client) {
    this(client, null);
  }

  /**
   * Using this constructor is deprecated, but provided for the use-cases where callers still depend on deprecated
   * RestClient-only API methods.
   *
   * @param client Client to delegate all overlapping Client calls
   * @param restClientFallback RestClient to use for RestClient-only methods. See class description for details.
   * @deprecated this constructor will be removed in the future after changing the RestClient API to match that of
   * Client. Use {@link #ForwardingRestClient(Client)} if possible.
   */
  @Deprecated
  public ForwardingRestClient(@Nonnull Client client, @Nullable RestClient restClientFallback) {
    super(null, null);
    _client = client;
    _restClient = restClientFallback;
  }

  // RestClient only method
  @Deprecated
  @Override
  public String getURIPrefix() {
    if (_restClient == null) {
      throw new UnsupportedOperationException("getURIPrefix is not supported by the ForwardingRestClient");
    }
    return _restClient.getURIPrefix();
  }

  // RestClient only method
  @Deprecated
  @Override
  public <T> void sendRestRequest(final Request<T> request, RequestContext requestContext,
      Callback<RestResponse> callback) {
    if (_restClient == null) {
      throw new UnsupportedOperationException("sendRestRequest is not supported by the ForwardingRestClient");
    }
    _restClient.sendRestRequest(request, requestContext, callback);
  }

  @Override
  public void shutdown(Callback<None> callback) {
    _client.shutdown(callback);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(Request<T> request, RequestContext requestContext) {
    return _client.sendRequest(request, requestContext);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(Request<T> request, RequestContext requestContext,
      ErrorHandlingBehavior errorHandlingBehavior) {
    return _client.sendRequest(request, requestContext, errorHandlingBehavior);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder,
      RequestContext requestContext) {
    return _client.sendRequest(requestBuilder, requestContext);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder,
      RequestContext requestContext, ErrorHandlingBehavior errorHandlingBehavior) {
    return _client.sendRequest(requestBuilder, requestContext, errorHandlingBehavior);
  }

  @Override
  public <T> void sendRequest(final Request<T> request, final RequestContext requestContext,
      final Callback<Response<T>> callback) {
    _client.sendRequest(request, requestContext, callback);
  }

  @Override
  public <T> void sendRequest(final RequestBuilder<? extends Request<T>> requestBuilder, RequestContext requestContext,
      Callback<Response<T>> callback) {
    _client.sendRequest(requestBuilder, requestContext, callback);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(Request<T> request) {
    return _client.sendRequest(request);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(Request<T> request, ErrorHandlingBehavior errorHandlingBehavior) {
    return _client.sendRequest(request, errorHandlingBehavior);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder) {
    return _client.sendRequest(requestBuilder);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder,
      ErrorHandlingBehavior errorHandlingBehavior) {
    return _client.sendRequest(requestBuilder, errorHandlingBehavior);
  }

  @Override
  public <T> void sendRequest(final Request<T> request, Callback<Response<T>> callback) {
    _client.sendRequest(request, callback);
  }

  @Override
  public <T> void sendRequest(final RequestBuilder<? extends Request<T>> requestBuilder,
      Callback<Response<T>> callback) {
    _client.sendRequest(requestBuilder, callback);
  }

  @Override
  public void sendRequest(MultiplexedRequest multiplexedRequest) {
    _client.sendRequest(multiplexedRequest);
  }

  @Override
  public void sendRequest(MultiplexedRequest multiplexedRequest, Callback<MultiplexedResponse> callback) {
    _client.sendRequest(multiplexedRequest, callback);
  }

  @Override
  public void sendRequest(MultiplexedRequest multiplexedRequest, RequestContext requestContext,
      Callback<MultiplexedResponse> callback) {
    _client.sendRequest(multiplexedRequest, requestContext, callback);
  }
}

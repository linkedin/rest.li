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
import com.linkedin.common.callback.Callbacks;
import com.linkedin.common.util.None;
import com.linkedin.r2.disruptor.DisruptContext;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.client.multiplexer.MultiplexedRequest;
import com.linkedin.restli.client.multiplexer.MultiplexedResponse;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.disruptor.DisruptRestController;
import com.linkedin.util.ArgumentUtil;

import static com.linkedin.r2.disruptor.DisruptContext.addDisruptContextIfNotPresent;


/**
 * Decorator Rest.li {@link Client} implementation that evaluates each {@link Request}
 * against a provided {@link DisruptRestController} instance and writes the evaluated
 * {@link DisruptContext} into the {@link RequestContext} object associated the request.
 * The #sendRequest operation is eventually delegated to the decorated Rest.li {@link Client}.
 *
 * @author Sean Sheng
 */
public class DisruptRestClient implements Client
{
  private final Client _client;
  private final DisruptRestController _controller;

  public DisruptRestClient(Client client, DisruptRestController controller)
  {
    ArgumentUtil.notNull(client, "client");
    ArgumentUtil.notNull(controller, "controller");

    _client = client;
    _controller = controller;
  }

  @Override
  public void shutdown(Callback<None> callback)
  {
    _client.shutdown(callback);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(Request<T> request, RequestContext requestContext)
  {
    doEvaluateDisruptContext(request, requestContext);
    return _client.sendRequest(request, requestContext);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(Request<T> request, RequestContext requestContext,
      ErrorHandlingBehavior errorHandlingBehavior)
  {
    doEvaluateDisruptContext(request, requestContext);
    return _client.sendRequest(request, requestContext, errorHandlingBehavior);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder,
      RequestContext requestContext)
  {
    Request<T> request = requestBuilder.build();
    doEvaluateDisruptContext(request, requestContext);
    return _client.sendRequest(request, requestContext);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder,
      RequestContext requestContext, ErrorHandlingBehavior errorHandlingBehavior)
  {
    Request<T> request = requestBuilder.build();
    doEvaluateDisruptContext(request, requestContext);
    return _client.sendRequest(request, requestContext, errorHandlingBehavior);
  }

  @Override
  public <T> void sendRequest(Request<T> request, RequestContext requestContext, Callback<Response<T>> callback)
  {
    doEvaluateDisruptContext(request, requestContext);
    _client.sendRequest(request, requestContext, callback);
  }

  @Override
  public <T> void sendRequest(RequestBuilder<? extends Request<T>> requestBuilder, RequestContext requestContext,
      Callback<Response<T>> callback)
  {
    Request<T> request = requestBuilder.build();
    doEvaluateDisruptContext(request, requestContext);
    _client.sendRequest(request, requestContext, callback);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(Request<T> request)
  {
    RequestContext requestContext = new RequestContext();
    doEvaluateDisruptContext(request, requestContext);
    return _client.sendRequest(request, requestContext);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(Request<T> request, ErrorHandlingBehavior errorHandlingBehavior)
  {
    RequestContext requestContext = new RequestContext();
    doEvaluateDisruptContext(request, requestContext);
    return _client.sendRequest(request, requestContext, errorHandlingBehavior);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder)
  {
    Request<T> request = requestBuilder.build();
    RequestContext requestContext = new RequestContext();
    doEvaluateDisruptContext(request, requestContext);
    return _client.sendRequest(request, requestContext);
  }

  @Override
  public <T> ResponseFuture<T> sendRequest(RequestBuilder<? extends Request<T>> requestBuilder,
      ErrorHandlingBehavior errorHandlingBehavior)
  {
    Request<T> request = requestBuilder.build();
    RequestContext requestContext = new RequestContext();
    doEvaluateDisruptContext(request, requestContext);
    return _client.sendRequest(request, requestContext, errorHandlingBehavior);
  }

  @Override
  public <T> void sendRequest(Request<T> request, Callback<Response<T>> callback)
  {
    RequestContext requestContext = new RequestContext();
    doEvaluateDisruptContext(request, requestContext);
    _client.sendRequest(request, requestContext, callback);
  }

  @Override
  public <T> void sendRequest(RequestBuilder<? extends Request<T>> requestBuilder, Callback<Response<T>> callback)
  {
    Request<T> request = requestBuilder.build();
    RequestContext requestContext = new RequestContext();
    doEvaluateDisruptContext(request, requestContext);
    _client.sendRequest(request, requestContext, callback);
  }

  @Override
  public void sendRequest(MultiplexedRequest multiplexedRequest)
  {
    RequestContext requestContext = new RequestContext();
    doEvaluateDisruptContext(requestContext);
    _client.sendRequest(multiplexedRequest, requestContext, Callbacks.empty());
  }

  @Override
  public void sendRequest(MultiplexedRequest multiplexedRequest, Callback<MultiplexedResponse> callback)
  {
    RequestContext requestContext = new RequestContext();
    doEvaluateDisruptContext(requestContext);
    _client.sendRequest(multiplexedRequest, requestContext, callback);
  }

  @Override
  public void sendRequest(MultiplexedRequest multiplexedRequest, RequestContext requestContext,
      Callback<MultiplexedResponse> callback)
  {
    doEvaluateDisruptContext(requestContext);
    _client.sendRequest(multiplexedRequest, requestContext, callback);
  }

  /**
   * Evaluates if a {@link MultiplexedRequest} should be disrupted, against the {@link DisruptRestController}
   * and store the corresponding {@link DisruptContext} into the {@link RequestContext}. However,
   * if disrupt source is already set in the ReuqestContext, the method does not evaluate further.
   *
   * @param requestContext Context of the request
   * @param <T> Request template
   */
  private <T> void doEvaluateDisruptContext(RequestContext requestContext)
  {
    addDisruptContextIfNotPresent(requestContext, _controller.getClass(),
        () -> _controller.getDisruptContext(MULTIPLEXER_RESOURCE));
  }

  /**
   * Evaluates if a {@link Request} should be disrupted, against the {@link DisruptRestController}
   * and store the corresponding {@link DisruptContext} into the {@link RequestContext}. However,
   * if disrupt source is already set in the ReuqestContext, the method does not evaluate further.
   *
   * @param request Request
   * @param requestContext Context associated with the request
   * @param <T> Request template
   */
  private <T> void doEvaluateDisruptContext(Request<T> request, RequestContext requestContext)
  {
    addDisruptContextIfNotPresent(requestContext, _controller.getClass(), () -> {
      final ResourceMethod method = request.getMethod();
      final String resource = request.getBaseUriTemplate();
      final String name = request.getMethodName();
      if (name == null)
      {
        return _controller.getDisruptContext(resource, method);
      }
      else
      {
        return _controller.getDisruptContext(resource, method, name);
      }
    });
  }
}

package com.linkedin.r2.transport.common;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;

/**
 * An adapter for adapting RestRequestHandler to StreamRequestHandler. The adapter would convert StreamRequest to
 * RestRequest before invoking the handleRequest method, and convert RestResponse to StreamResponse in the callback.
 *
 * @author Zhenkai Zhu
 */
public class StreamRequestHandlerAdapter implements StreamRequestHandler
{
  private final RestRequestHandler _restRequestHandler;

  public StreamRequestHandlerAdapter(RestRequestHandler restRequestHandler)
  {
    _restRequestHandler = restRequestHandler;
  }

  @Override
  public void handleRequest(StreamRequest request, final RequestContext requestContext, final Callback<StreamResponse> callback)
  {
    Messages.toRestRequest(request, new Callback<RestRequest>()
    {
      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }

      @Override
      public void onSuccess(RestRequest restRequest)
      {
        _restRequestHandler.handleRequest(restRequest, requestContext, Messages.toRestCallback(callback));
      }
    });
  }
}

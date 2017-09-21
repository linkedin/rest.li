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

package com.linkedin.restli.server;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandler;


/**
 * This is a handler for handling non-resource requests. For example, documentation request, multiplexed request,
 * and debug request are non-resource requests. The {@link RestLiServer} can have more than one of these request
 * handlers configured to it. Whenever a request comes in, <code>RestLiServer</code> will test if the request is one of
 * those non-resource requests and have the matching request handler handle the request.
 *
 * @author xma
 */
public interface RequestHandler extends RestRequestHandler, StreamRequestHandler
{
  /**
   * Tests whether or not the given request should be handled by this request handler.
   */
  boolean shouldHandle(Request request);

  /**
   * Handles the {@link StreamRequest}. The default implementation adapts the <code>StreamRequest</code> to a
   * {@link RestRequest} and use the {@link RestRequestHandler} implementation.
   */
  default void handleRequest(StreamRequest request, RequestContext requestContext, Callback<StreamResponse> callback)
  {
    Messages.toRestRequest(request, new Callback<RestRequest>()
    {
      @Override
      public void onError(Throwable e)
      {
        callback.onError(e);
      }

      @Override
      public void onSuccess(RestRequest result)
      {
        handleRequest(result, requestContext, Messages.toRestCallback(callback));
      }
    });
  }
}

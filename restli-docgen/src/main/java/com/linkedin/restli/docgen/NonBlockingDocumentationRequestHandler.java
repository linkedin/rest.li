/*
   Copyright (c) 2022 LinkedIn Corp.

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

package com.linkedin.restli.docgen;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.server.RestLiDocumentationRequestHandler;
import com.linkedin.restli.server.RestLiServiceException;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Non-blocking extension of the default {@link RestLiDocumentationRequestHandler} that is needed in special use cases.
 * This implementation blocks on the request thread that lazily initializes the renderers, but refuses to block
 * subsequent request threads, failing the requests instead.
 *
 * The advantage of this approach is that the request thread pool may avoid exhaustion in the case of lengthy initialization.
 * The downside is that requests sent during initialization will fail, which may cause client problems and confusion.
 *
 * @author Evan Williams
 */
public class NonBlockingDocumentationRequestHandler extends DefaultDocumentationRequestHandler
{
  private final AtomicBoolean _shouldInitialize = new AtomicBoolean(true);

  @Override
  public void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
  {
    // The first request thread should perform the initialization and render the response
    if (_shouldInitialize.getAndSet(false))
    {
      super.handleRequest(request, requestContext, callback);
    }
    // For subsequent requests sent during initialization, immediately return a failure response
    else if (!isInitialized())
    {
      callback.onError(new RestLiServiceException(HttpStatus.S_503_SERVICE_UNAVAILABLE, "Documentation renderers have not yet been initialized."));
    }
    // For all requests received after initialization has completed, render the response
    else
    {
      super.handleRequest(request, requestContext, callback);
    }
  }
}

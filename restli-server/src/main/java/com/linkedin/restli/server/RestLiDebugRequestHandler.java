/*
   Copyright (c) 2014 LinkedIn Corp.

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
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;


/**
 * The interface for Rest.li debug request handlers. Debug request handlers are registered with {@link RestLiServer}
 * through {@link RestLiConfig}. Every debug request handler has a handler id which determines the uri
 * sub-path that {@link RestLiServer} will route the requests to that debug request handler.
 * The Rest.li requests which have a "/__debug/<Debug request handler id>" appended to their path will be routed to
 * the corresponding debug request handler. At that point, the debug request handler gets a chance
 * to inspect the request, modify it, execute it through normal Rest.li method invocation pipeline, get the response
 * and shape it in any way it determines.
 *
 * @see RestLiDebugRequestHandler#getHandlerId()
 * @see RestLiDebugRequestHandler#handleRequest(RestRequest, RequestContext, ResourceDebugRequestHandler, Callback)
 */
public interface RestLiDebugRequestHandler
{
  /**
   * Handles a debug request. The implementation of this method can optionally execute the request through
   * the {@code resourceDebugRequestHandler} parameter which would execute it through the normal rest.li method
   * invocation pipeline.
   *
   * It is the responsibility of this debug request handler to also absorb any incoming request attachments provided.
   *
   * The implementation of this method is responsible for invoking
   * the right method on the {@code callback} parameter to return a response.
   * @param request The debug request.
   * @param context The request context.
   * @param resourceDebugRequestHandler The resource request handler for executing a debug request as a regular request.
   * @param callback The callback to be invoked with a response at the end of the execution.
   */
  void handleRequest(final RestRequest request,
      final RequestContext context,
      final ResourceDebugRequestHandler resourceDebugRequestHandler,
      final Callback<RestResponse> callback);

  /**
   * Gets the handler id for this debug request handler. The handler id uniquely identifies the debug request handler
   * in a {@link RestLiServer}. It also determines the path segment for the debug requests that will be routed to this
   * debug request handler. It corresponds to the path segment after the path segment "__debug".
   * @return The handler id.
   */
  String getHandlerId();

  /**
   * The interface for resource debug request handler. A resource debug request handler transforms a debug request into
   * the original Rest.li request that it represents and executes it. A debug request handler can use a resource
   * debug request handler to execute a debug request as if it is a regular Rest.li request.
   */
  public interface ResourceDebugRequestHandler
  {
    /**
     * Handles a debug request as if it is the Rest.li request it represents.
     * @param request The debug request
     * @param requestContext The request context
     * @param callback The callback which will be invoked with the result of the request execution.
     */
    void handleRequest(final RestRequest request,
                       final RequestContext requestContext,
                       final Callback<RestResponse> callback);
  }
}
/*
    Copyright (c) 2019 LinkedIn Corp.

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
import com.linkedin.restli.internal.server.response.RestLiResponse;


/**
 * A request handler for {@link RestRequest}s that returns RestLiResponse as the result.
 *
 * @see com.linkedin.r2.transport.common.RestRequestHandler
 */
public interface RestToRestLiRequestHandler
{
  /**
   * Handles the supplied request and notifies the supplied callback upon completion.
   *
   * <p>
   * If this is a dispatcher, as defined in the class documentation, then this method should return
   * {@link com.linkedin.r2.message.rest.RestStatus#NOT_FOUND} if no handler can be found for the
   * request.
   *
   * @param request The fully-buffered request to process.
   * @param requestContext {@link RequestContext} context for the request
   * @param callback The callback to notify when request processing has completed. When callback with an error, use
   *                 {@link com.linkedin.r2.message.rest.RestException} to provide custom response status code,
   *                 headers, and response body.
   *
   * @see com.linkedin.r2.transport.common.RestRequestHandler#handleRequest(RestRequest, RequestContext, Callback)
   */
  void handleRequestWithRestLiResponse(RestRequest request, RequestContext requestContext, Callback<RestLiResponse> callback);
}

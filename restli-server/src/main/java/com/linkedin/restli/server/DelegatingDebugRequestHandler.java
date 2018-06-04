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
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.util.URIUtil;


/**
 * A handler for debug request. It delegates the handling to the underlying {@link RestLiDebugRequestHandler}.
 *
 * @author xma
 */
class DelegatingDebugRequestHandler implements NonResourceRequestHandler
{
  static final String DEBUG_PATH_SEGMENT = "__debug";

  private final RestLiDebugRequestHandler _delegate;
  private final RestRestLiServer _restLiServer;

  DelegatingDebugRequestHandler(RestLiDebugRequestHandler delegate, RestRestLiServer restLiServer)
  {
    _delegate = delegate;
    _restLiServer = restLiServer;
  }

  @Override
  public boolean shouldHandle(Request request)
  {
    // Typically, a debug request should have the following pattern in its URI path
    // <base URI>/__debug/<debug handler ID>/<Rest.li request path>
    String[] pathSegments = URIUtil.tokenizePath(request.getURI().getPath());
    String debugHandlerId = null;

    for (int i = 0; i < pathSegments.length; ++i)
    {
      String pathSegment = pathSegments[i];
      if (pathSegment.equals(DEBUG_PATH_SEGMENT))
      {
        if (i < pathSegments.length - 1)
        {
          debugHandlerId = pathSegments[i + 1];
        }
        break;
      }
    }

    return _delegate.getHandlerId().equals(debugHandlerId);
  }

  @Override
  public void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
  {
    _delegate.handleRequest(request,
        requestContext,
        new ResourceDebugRequestHandlerImpl(),
        callback);
  }

  private class ResourceDebugRequestHandlerImpl implements RestLiDebugRequestHandler.ResourceDebugRequestHandler
  {
    @Override
    public void handleRequest(final RestRequest request,
        final RequestContext requestContext,
        final Callback<RestResponse> callback)
    {
      // Create a new request at this point from the debug request by removing the path suffix
      // starting with "__debug".
      String fullPath = request.getURI().getPath();
      int debugSegmentIndex = fullPath.indexOf(DEBUG_PATH_SEGMENT);

      RestRequestBuilder requestBuilder = new RestRequestBuilder(request);

      UriBuilder uriBuilder = UriBuilder.fromUri(request.getURI());
      uriBuilder.replacePath(request.getURI().getPath().substring(0, debugSegmentIndex - 1));
      requestBuilder.setURI(uriBuilder.build());

      _restLiServer.handleResourceRequest(requestBuilder.build(), requestContext, callback);
    }
  }
}

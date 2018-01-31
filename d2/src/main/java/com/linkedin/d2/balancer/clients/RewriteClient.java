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

package com.linkedin.d2.balancer.clients;

import com.google.common.annotations.VisibleForTesting;
import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.util.URIRewriter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import java.util.Map;

/**
 * Convert/send restli requests to transportClient compatible requests. The conversion is done through
 * provided URIRewriter.
 */

public class RewriteClient implements TransportClient
{
  private final TransportClient _transportClient;
  private final URIRewriter _uriRewriter;

  public RewriteClient(TransportClient transportClient, URIRewriter URIRewriter)
  {
    _uriRewriter = URIRewriter;
    _transportClient = transportClient;
  }

  /**
   * Asynchronously issues the given request. The given callback is invoked when the response is
   * received.
   *
   * @param request the request to issue
   * @param requestContext context for the request
   * @param wireAttrs attributes that should be sent over the wire to the server
   * @param callback the callback to invoke with the response
   */
  @Override
  public void restRequest(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
      TransportCallback<RestResponse> callback)
  {
    _transportClient.restRequest(rewriteRequest(request), requestContext, wireAttrs, callback);
  }

  /**
   * Asynchronously issues the given request. The given callback is invoked when the response is
   * received.
   *
   * Any implementation that wants to support streaming MUST override this method.
   *
   * @param request the request to issue
   * @param requestContext context for the request
   * @param wireAttrs attributes that should be sent over the wire to the server
   * @param callback the callback to invoke with the response
   */
  @Override
  public void streamRequest(StreamRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
      TransportCallback<StreamResponse> callback)
  {
    _transportClient.streamRequest(rewriteRequest(request), requestContext, wireAttrs, callback);
  }

  /**
   * Starts asynchronous shutdown of the client. This method should block minimally, if at all.
   *
   * @param callback a callback that will be invoked once the shutdown is complete
   */
  @Override
  public void shutdown(Callback<None> callback)
  {
    _transportClient.shutdown(callback);
  }

  @VisibleForTesting
  public TransportClient getDecoratedClient()
  {
    return _transportClient;
  }

  private RestRequest rewriteRequest(RestRequest request)
  {
    return request.builder().setURI(_uriRewriter.rewriteURI(request.getURI())).build();
  }

  private StreamRequest rewriteRequest(StreamRequest request)
  {
    return request.builder().setURI(_uriRewriter.rewriteURI(request.getURI())).build(request.getEntityStream());
  }


}

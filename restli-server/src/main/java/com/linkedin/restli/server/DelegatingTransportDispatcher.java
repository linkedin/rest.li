/*
   Copyright (c) 2012 LinkedIn Corp.

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


import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandlerAdapter;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.common.bridge.server.TransportCallbackAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;

import java.util.Map;

/**
 * Delegates dispatch to a single {@link RestRequestHandler}.
 *
 * @author dellamag
 */
public class DelegatingTransportDispatcher implements TransportDispatcher
{
  // for the streaming & rest-over-stream code path
  private final StreamRequestHandler _streamHandler;
  // for the legacy code path
  private final RestRequestHandler _restHandler;

  /**
   * @deprecated Please use {@link #DelegatingTransportDispatcher(RestRequestHandler, StreamRequestHandler)}
   */
  @Deprecated
  public DelegatingTransportDispatcher(final RestRequestHandler handler)
  {
    this(handler, new StreamRequestHandlerAdapter(handler));
  }

  public DelegatingTransportDispatcher(final RestRequestHandler restRequestHandler, final StreamRequestHandler streamRequestHandler)
  {
    _restHandler = restRequestHandler;
    _streamHandler = streamRequestHandler;
  }

  @Override
  public void handleRestRequest(RestRequest req, Map<String, String> wireAttrs,
                                  RequestContext requestContext, TransportCallback<RestResponse> callback)
  {
    try
    {
      _restHandler.handleRequest(req, requestContext, new TransportCallbackAdapter<RestResponse>(callback));
    }
    catch (Exception e)
    {
      callback.onResponse(TransportResponseImpl.<RestResponse>error(RestException.forError(RestStatus.INTERNAL_SERVER_ERROR, e)));
    }
  }

  @Override
  public void handleStreamRequest(final StreamRequest req,
                                final Map<String, String> wireAttrs,
                                final RequestContext requestContext,
                                final TransportCallback<StreamResponse> callback)
  {
    try
    {
      _streamHandler.handleRequest(req, requestContext, new TransportCallbackAdapter<StreamResponse>(callback));
    }
    catch (Exception e)
    {
      final Exception ex = RestException.forError(RestStatus.INTERNAL_SERVER_ERROR, e);
      callback.onResponse(TransportResponseImpl.<StreamResponse>error(ex));
    }
  }
}

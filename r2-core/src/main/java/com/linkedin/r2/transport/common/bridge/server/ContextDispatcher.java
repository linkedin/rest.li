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

/* $Id$ */
package com.linkedin.r2.transport.common.bridge.server;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.DrainReader;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandlerAdapter;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.util.URIUtil;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * A dispatcher that uses the first path segment (as defined in RFC 2396) to dispatch to another
 * request handler that manages that context.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class ContextDispatcher implements TransportDispatcher
{
  private static final StreamRequestHandler DEFAULT_STREAM_HANDLER = new StreamRequestHandler() {
    @Override
    public void handleRequest(StreamRequest req, RequestContext requestContext, Callback<StreamResponse> callback)
    {
      final RestResponse response =
              RestStatus.responseForStatus(RestStatus.NOT_FOUND, "No resource for URI: " + req.getURI());
      callback.onSuccess(Messages.toStreamResponse(response));
      req.getEntityStream().setReader(new DrainReader());
    }
  };

  private static final RestRequestHandler DEFAULT_REST_HANDLER = new RestRequestHandler()
  {
    @Override
    public void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
    {
      callback.onSuccess(RestStatus.responseForStatus(RestStatus.NOT_FOUND, "No resource for URI: " + request.getURI()));
    }
  };

  private final Map<String, StreamRequestHandler> _streamHandlers;
  private final Map<String, RestRequestHandler> _restHandlers;

  /**
   * Construct a new instance with the specified dispatcher maps.
   *
   * @param restDispatcher a map from path to {@link RestRequestHandler}.  REST requests whose first
   *                       path segment matches the map key will be dispatched to the respective
   *                       handler.
   */
  public ContextDispatcher(Map<String, RestRequestHandler> restDispatcher)
  {
    _streamHandlers = new HashMap<>();
    for (Map.Entry<String, RestRequestHandler> entry : restDispatcher.entrySet())
    {
      _streamHandlers.put(entry.getKey(), new StreamRequestHandlerAdapter(entry.getValue()));
    }

    _restHandlers = new HashMap<>(restDispatcher);
  }

  @Override
  public void handleRestRequest(RestRequest req, Map<String, String> wireAttrs,
                                  RequestContext requestContext, TransportCallback<RestResponse> callback)
  {
    final RestRequestHandler handler = getHandler(req.getURI(), _restHandlers, DEFAULT_REST_HANDLER);
    try
    {
      handler.handleRequest(req, requestContext, new TransportCallbackAdapter<>(callback));
    }
    catch (Exception e)
    {
      callback.onResponse(TransportResponseImpl.<RestResponse>error(RestException.forError(RestStatus.INTERNAL_SERVER_ERROR, e)));
    }
  }

  @Override
  public void handleStreamRequest(StreamRequest req, Map<String, String> wireAttrs,
                                RequestContext requestContext,
                                TransportCallback<StreamResponse> callback)
  {
    final StreamRequestHandler handler = getHandler(req.getURI(), _streamHandlers,
        DEFAULT_STREAM_HANDLER);

    try
    {
      handler.handleRequest(req, requestContext, new TransportCallbackAdapter<>(callback));
    }
    catch (Exception e)
    {
      final Exception ex = RestException.forError(RestStatus.INTERNAL_SERVER_ERROR, e);
      callback.onResponse(TransportResponseImpl.<StreamResponse>error(ex));
    }
  }

  private <T> T getHandler(URI uri, Map<String, T> handlers, T defaultHandler)
  {
    final String path = uri.getPath();
    if (path == null)
    {
      return defaultHandler;
    }

    final String[] segs = URIUtil.tokenizePath(path);
    if (segs.length < 1)
    {
      return defaultHandler;
    }

    final T handler = handlers.get(segs[0]);
    return handler != null ? handler : defaultHandler;
  }
}

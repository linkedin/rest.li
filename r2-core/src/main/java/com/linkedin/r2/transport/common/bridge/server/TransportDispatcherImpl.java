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
package com.linkedin.r2.transport.common.bridge.server;

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
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @author Zhenkai Zhu
 */
/* package private */ final class TransportDispatcherImpl implements TransportDispatcher
{
  private final Map<URI, StreamRequestHandler> _streamHandlers;
  private final Map<URI, RestRequestHandler> _restHandlers;

  /* package private */ TransportDispatcherImpl(Map<URI, RestRequestHandler> restHandlers, Map<URI, StreamRequestHandler> streamHandlers)
  {
    _streamHandlers = streamHandlers == null ? Collections.<URI, StreamRequestHandler>emptyMap() : new HashMap<URI, StreamRequestHandler>(streamHandlers);
    _restHandlers = restHandlers == null ? Collections.<URI, RestRequestHandler>emptyMap() : new HashMap<URI, RestRequestHandler>(restHandlers);
  }

  @Override
  public void handleRestRequest(RestRequest req, Map<String, String> wireAttrs,
                                  RequestContext requestContext, TransportCallback<RestResponse> callback)
  {
    final URI address = req.getURI();
    RestRequestHandler handler = _restHandlers.get(address);
    if (handler == null)
    {
      callback.onResponse(TransportResponseImpl.success(RestStatus.responseForStatus(RestStatus.NOT_FOUND, "No resource for URI:" + address)));
      return;
    }

    try
    {
      handler.handleRequest(req, requestContext, new TransportCallbackAdapter<RestResponse>(callback));
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
    final URI address = req.getURI();
    final StreamRequestHandler handler = _streamHandlers.get(address);

    if (handler == null)
    {
      final RestResponse response =
          RestStatus.responseForStatus(RestStatus.NOT_FOUND, "No resource for URI: " + address);
      callback.onResponse(TransportResponseImpl.success(Messages.toStreamResponse(response)));
      req.getEntityStream().setReader(new DrainReader());
      return;
    }

    try
    {
      handler.handleRequest(req, requestContext, new TransportCallbackAdapter<StreamResponse>(callback));
    }
    catch (Exception e)
    {
      final Exception ex = RestException.forError(RestStatus.INTERNAL_SERVER_ERROR, e);
      callback.onResponse(TransportResponseImpl.<StreamResponse>error(ex));
    }
  }

}

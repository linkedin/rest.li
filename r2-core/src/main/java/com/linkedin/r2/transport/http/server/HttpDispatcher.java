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
package com.linkedin.r2.transport.http.server;


import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.MessageType;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.common.HttpBridge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class HttpDispatcher
{
  private final TransportDispatcher _dispatcher;

  /**
   * Construct a new instance which delegates to the specified dispatcher.
   *
   * @param dispatcher the {@link TransportDispatcher} to which requests are delegated.
   */
  public HttpDispatcher(TransportDispatcher dispatcher)
  {
    _dispatcher = dispatcher;
  }

  /**
   * handle a {@link RestRequest}.
   * @see TransportDispatcher#handleRestRequest
   *
   * @param req the request to be handled.
   * @param callback the callback to be invoked with the response or error.
   */
  public void handleRequest(RestRequest req,
                            TransportCallback<RestResponse> callback)
  {
    handleRequest(req, new RequestContext(), callback);
  }

  /**
   * handle a {@link RestRequest} using the given request context.
   * @see TransportDispatcher#handleRestRequest
   *
   * @param req the request to be handled.
   * @param context the request context.
   * @param callback the callback to be invoked with the response or error.
   */
  public void handleRequest(RestRequest req,
                            RequestContext context,
                            TransportCallback<RestResponse> callback)
  {
    final Map<String, String> headers = new HashMap<String, String>(req.getHeaders());
    final Map<String, String> wireAttrs = WireAttributeHelper.removeWireAttributes(headers);

    try
    {
      MessageType.Type msgType = MessageType.getMessageType(wireAttrs, MessageType.Type.REST);
      switch (msgType)
      {
        default:
        case REST:
          _dispatcher.handleRestRequest(HttpBridge.toRestRequest(req, headers),
                                        wireAttrs,
                                        context, HttpBridge.httpToRestCallback(callback)
          );
      }
    }
    catch (Exception e)
    {
      callback.onResponse(TransportResponseImpl.<RestResponse>error(e, Collections.<String, String>emptyMap()));
    }
  }
}

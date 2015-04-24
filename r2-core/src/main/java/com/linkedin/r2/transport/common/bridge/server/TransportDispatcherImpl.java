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


import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;

import java.net.URI;
import java.util.Map;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
/* package private */ class TransportDispatcherImpl implements TransportDispatcher
{
  private final Map<URI, RestRequestHandler> _restHandlers;

  /* package private */ TransportDispatcherImpl(Map<URI, RestRequestHandler> restDispatcher)
  {
    _restHandlers = restDispatcher;
  }

  @Override
  public void handleRestRequest(RestRequest req, Map<String, String> wireAttrs,
                                RequestContext requestContext,
                                TransportCallback<RestResponse> callback)
  {
    final URI address = req.getURI();
    final RestRequestHandler handler = _restHandlers.get(address);

    if (handler == null)
    {
      final RestResponse response =
              RestStatus.responseForStatus(RestStatus.NOT_FOUND, "No resource for URI: " + address);
      callback.onResponse(TransportResponseImpl.success(response));
      return;
    }

    try
    {
      handler.handleRequest(req, requestContext, new TransportCallbackAdapter<RestResponse>(callback));
    }
    catch (Exception e)
    {
      final Exception ex = RestException.forError(RestStatus.INTERNAL_SERVER_ERROR, e);
      callback.onResponse(TransportResponseImpl.<RestResponse>error(ex));
    }
  }
}

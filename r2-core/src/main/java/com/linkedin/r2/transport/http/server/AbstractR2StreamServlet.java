/*
   Copyright (c) 2015 LinkedIn Corp.

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
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @author Zhenkai Zhu
 */
public abstract class AbstractR2StreamServlet extends HttpServlet
{
  private static final long   serialVersionUID = 0L;

  private final long _ioHandlerTimeout;

  protected abstract HttpDispatcher getDispatcher();

  public AbstractR2StreamServlet(long ioHandlerTimeout)
  {
    _ioHandlerTimeout = ioHandlerTimeout;
  }

  @Override
  protected void service(final HttpServletRequest req, final HttpServletResponse resp)
          throws ServletException, IOException
  {
    final SyncIOHandler ioHandler = new SyncIOHandler(req.getInputStream(), resp.getOutputStream(), req.getProtocol(),
        req.getRemoteAddr(), 2, _ioHandlerTimeout);

    RequestContext requestContext = ServletHelper.readRequestContext(req);

    StreamRequest streamRequest;

    try
    {
      streamRequest = ServletHelper.readFromServletRequest(req, ioHandler);
    }
    catch (URISyntaxException e)
    {
      ServletHelper.writeToServletError(resp, RestStatus.BAD_REQUEST, e.toString());
      return;
    }


    TransportCallback<StreamResponse> callback = new TransportCallback<StreamResponse>()
    {
      @Override
      public void onResponse(TransportResponse<StreamResponse> response)
      {
        StreamResponse streamResponse = ServletHelper.writeResponseHeadersToServletResponse(response, resp);
        streamResponse.getEntityStream().setReader(ioHandler);
      }
    };

    getDispatcher().handleRequest(streamRequest, requestContext, callback);

    ioHandler.loop();
  }
}

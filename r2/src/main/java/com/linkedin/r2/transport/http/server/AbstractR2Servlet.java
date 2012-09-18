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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;

/**
 * @author Steven Ihde
 * @author Chris Pettitt
 * @author Fatih Emekci
 * @version $Revision$
 */
public abstract class AbstractR2Servlet extends HttpServlet
{
  private static final long serialVersionUID = 0L;

  /**
   * Initialize the servlet using Jetty continuations for async support.
   *
   * Not supported until we upgrade to Servlet 3 async / Jetty 8
   *
   * @param useContinuations whether to use Continuations
   * @param timeOut timeout to suspend the thread while waiting for response (ignored if
   *          useContinuations is false)
   * @param timeOutDelta if woken before timeOut - timeoutDelta and no response is
   *          available, will sleep again assuming spurious wakeup (ignored if
   *          useContinuations is false)
   */
  public AbstractR2Servlet(boolean useContinuations, int timeOut, int timeOutDelta)
  {
    if (useContinuations)
    {
      throw new UnsupportedOperationException("Asynchronous continuations not supported");
    }
  }

  /**
   * Construct a new instance using synchronous servlet support.
   */
  public AbstractR2Servlet()
  {
    this(false, -1, -1);
  }

  protected abstract HttpDispatcher getDispatcher();

  @Override
  protected void service(final HttpServletRequest req, final HttpServletResponse resp)
          throws ServletException, IOException
  {
    serviceNoContinuation(req, resp);
  }

  private void serviceNoContinuation(final HttpServletRequest req, final HttpServletResponse resp)
          throws ServletException, IOException
  {
    RestRequest restRequest;
    try
    {
      restRequest = readFromServletRequest(req);
    }
    catch (URISyntaxException e)
    {
      RestResponse restResponse = RestStatus.responseForError(RestStatus.BAD_REQUEST, e);
      writeToServletResponse(TransportResponseImpl.success(restResponse), resp);
      return;
    }

    final AtomicReference<TransportResponse<RestResponse>> result =
        new AtomicReference<TransportResponse<RestResponse>>();
    final CountDownLatch latch = new CountDownLatch(1);
    TransportCallback<RestResponse> callback = new TransportCallback<RestResponse>()
    {
      @Override
      public void onResponse(TransportResponse<RestResponse> response)
      {
        result.set(response);
        latch.countDown();
      }
    };
    getDispatcher().handleRequest(restRequest, callback);
    try
    {
      latch.await();
    }
    catch (InterruptedException e)
    {
      throw new ServletException("Interrupted!", e);
    }

    writeToServletResponse(result.get(), resp);
  }

  private RestRequest readFromServletRequest(HttpServletRequest req) throws IOException,
      ServletException,
      URISyntaxException
  {
    StringBuilder sb = new StringBuilder();
    String pathInfo = req.getPathInfo();
    if (pathInfo != null)
    {
      sb.append(req.getPathInfo());
    }
    else
    {
      // We prefer to keep servlet mapping trivial with R2 and have R2
      // TransportDispatchers make most of the routing decisions based on the 'pathInfo'
      // and query parameters in the URI.
      // If pathInfo is null, it's highly likely that the servlet was mapped to an exact
      // path or to a file extension, making such R2-based services too reliant on the
      // servlet container for routing
      throw new ServletException("R2 servlet should only be mapped via wildcard path mapping e.g. /r2/*. "
          + "Exact path matching (/r2) and file extension mappings (*.r2) are currently not supported");
    }

    String query = req.getQueryString();
    if (query != null)
    {
      sb.append('?');
      sb.append(query);
    }

    URI uri = new URI(sb.toString());

    RestRequestBuilder rb = new RestRequestBuilder(uri);
    rb.setMethod(req.getMethod());

    for (Enumeration<?> headerNames = req.getHeaderNames(); headerNames.hasMoreElements();)
    {
      // TODO multi-valued headers
      String headerName = (String) headerNames.nextElement();
      rb.setHeader(headerName, req.getHeader(headerName));
    }
    int length = req.getContentLength();
    if (length >= 0)
    {
      InputStream in = req.getInputStream();
      byte[] buf = new byte[length];
      int offset = 0;
      for (int r; offset < length && (r = in.read(buf, offset, length - offset)) != -1; offset += r)
      {

      }
      rb.setEntity(buf);
    }
    return rb.build();

  }

  private void writeToServletResponse(TransportResponse<RestResponse> response,
                                      HttpServletResponse resp)
          throws IOException
  {
    Map<String, String> wireAttrs = response.getWireAttributes();
    for (Map.Entry<String, String> e : WireAttributeHelper.toWireAttributes(wireAttrs)
                                                          .entrySet())
    {
      resp.setHeader(e.getKey(), e.getValue());
    }

    RestResponse restResponse = null;
    if (response.hasError())
    {
      Throwable e = response.getError();
      if (e instanceof RestException)
      {
        restResponse = ((RestException) e).getResponse();
      }
      if (restResponse == null)
      {
        restResponse = RestStatus.responseForError(RestStatus.INTERNAL_SERVER_ERROR, e);
      }
    }
    else
    {
      restResponse = response.getResponse();
    }

    resp.setStatus(restResponse.getStatus());
    Map<String, String> headers = restResponse.getHeaders();
    for (Map.Entry<String, String> e : headers.entrySet())
    {
      // TODO multi-valued headers
      resp.setHeader(e.getKey(), e.getValue());
    }
    final ByteString entity = restResponse.getEntity();
    entity.write(resp.getOutputStream());

    resp.getOutputStream().close();
  }
}

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


import java.net.URISyntaxException;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Abstract Async R2 Servlet that supports streaming. Any servlet deriving from this class can only be used with
 * containers supporting Servlet API 3.0 or greater.
 *
 * @author Zhenkai Zhu
 */
@SuppressWarnings("serial")
public abstract class AbstractAsyncR2StreamServlet extends HttpServlet
{
  private static final String ASYNC_IOEXCEPTION = "AsyncIOException";
  private static final Logger LOG = LoggerFactory.getLogger(AbstractAsyncR2StreamServlet.class.getName());
  private static final int MAX_BUFFERED_CHUNKS = 3;

  // servlet async context timeout in ms.
  private final long _timeout;

  protected abstract HttpDispatcher getDispatcher();

  /**
   * Initialize the servlet, optionally using servlet-api-3.0 async API, if supported
   * by the container. The latter is checked later in init()
   */
  public AbstractAsyncR2StreamServlet(long timeout)
  {
    _timeout = timeout;
  }

  @Override
  public void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
                                                                                                 IOException
  {
    final AsyncContext ctx = req.startAsync(req, resp);
    ctx.setTimeout(_timeout);

    final WrappedAsyncContext wrappedCtx = new WrappedAsyncContext(ctx);

    final AsyncEventIOHandler ioHandler =
        new AsyncEventIOHandler(req.getInputStream(), resp.getOutputStream(), wrappedCtx, MAX_BUFFERED_CHUNKS);

    final RequestContext requestContext = ServletHelper.readRequestContext(req);

    final StreamRequest streamRequest;

    try
    {
      streamRequest = ServletHelper.readFromServletRequest(req, ioHandler);
    }
    catch (URISyntaxException e)
    {
      ServletHelper.writeToServletError(resp, RestStatus.BAD_REQUEST, e.toString());
      wrappedCtx.complete();
      return;
    }

    final AtomicBoolean startedResponding = new AtomicBoolean(false);

    ctx.addListener(new AsyncListener()
    {
      @Override
      public void onTimeout(AsyncEvent event) throws IOException
      {
        LOG.error("Server timeout for request: " + formatURI(req.getRequestURI()));
        if (startedResponding.compareAndSet(false, true))
        {
          LOG.info("Returning server timeout response");
          ServletHelper.writeToServletError(resp, RestStatus.INTERNAL_SERVER_ERROR, "Server timeout");
        }
        else
        {
          req.setAttribute(ASYNC_IOEXCEPTION, new ServletException("Server timeout"));
        }
        ioHandler.exitLoop();
        wrappedCtx.complete();
      }

      @Override
      public void onStartAsync(AsyncEvent event) throws IOException
      {
        // Nothing to do here
      }

      @Override
      public void onError(AsyncEvent event) throws IOException
      {
        LOG.error("Server error for request: " + formatURI(req.getRequestURI()));
        if (startedResponding.compareAndSet(false, true))
        {
          LOG.info("Returning server error response");
          ServletHelper.writeToServletError(resp, RestStatus.INTERNAL_SERVER_ERROR, "Server error");
        }
        else
        {
          req.setAttribute(ASYNC_IOEXCEPTION, new ServletException("Server error"));
        }
        ioHandler.exitLoop();
        wrappedCtx.complete();
      }

      @Override
      public void onComplete(AsyncEvent event) throws IOException
      {
        Object exception = req.getAttribute(ASYNC_IOEXCEPTION);
        if (exception != null)
        {
          throw new IOException((Throwable)exception);
        }
      }
    });

    final TransportCallback<StreamResponse> callback = new TransportCallback<StreamResponse>()
    {
      @Override
      public void onResponse(final TransportResponse<StreamResponse> response)
      {
        if (startedResponding.compareAndSet(false, true))
        {
          ctx.start(new Runnable()
          {
            @Override
            public void run()
            {
              try
              {
                StreamResponse streamResponse = ServletHelper.writeResponseHeadersToServletResponse(response, resp);
                streamResponse.getEntityStream().setReader(ioHandler);
                ioHandler.loop();
              }
              catch (Exception e)
              {
                req.setAttribute(ASYNC_IOEXCEPTION, e);
                wrappedCtx.complete();
              }
            }
          });
        }
        else
        {
          LOG.error("Dropped a response; this is mostly like because that AsyncContext timeout or error had already happened");
        }
      }
    };

    // we have to use a new thread and let this thread return to pool. otherwise the timeout won't start
    ctx.start(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          getDispatcher().handleRequest(streamRequest, requestContext, callback);
          ioHandler.loop();
        }
        catch (Exception e)
        {
          req.setAttribute(ASYNC_IOEXCEPTION, e);
          wrappedCtx.complete();
        }
      }
    });

  }

  public long getTimeout()
  {
    return _timeout;
  }

  /* package private */static class WrappedAsyncContext
  {

    private final AtomicBoolean _completed = new AtomicBoolean(false);
    private final AsyncContext _ctx;

    WrappedAsyncContext(AsyncContext ctx)
    {
      _ctx = ctx;
    }

    void complete()
    {
      if (_completed.compareAndSet(false, true))
      {
        _ctx.complete();
      }
    }
  }

  private String formatURI(String uriText)
  {
    int queryStringIndex = uriText.lastIndexOf('?');
    return (queryStringIndex >= 0) ? uriText.substring(0, queryStringIndex) : uriText;
  }
}

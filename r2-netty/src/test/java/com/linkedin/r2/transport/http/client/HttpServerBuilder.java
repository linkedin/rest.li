/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;


/**
 * A Jetty implementation of HTTP/2 server that supports both h2c protocol through upgrade.
 */
public class HttpServerBuilder
{
  private static final int HTTP_PORT = 8080;
  private static final int RESPONSE_LATCH_TIMEOUT = 30;
  private static final TimeUnit RESPONSE_LATCH_TIMEUNIT = TimeUnit.SECONDS;
  private static final String HEADER_NAME = "X-DUMMY-HEADER";

  private int _responseSize = 0;
  private int _headerSize = 0;
  private int _status = 200;
  private int _minThreads = 0;
  private int _maxThreads = 150;
  private long _idleTimeout = 30000;
  private CountDownLatch _responseLatch = null;
  private Consumer<Throwable> _exceptionListener = null;

  // HTTP/2 settings
  private int _maxConcurrentStreams = 256;
  private int _initialSessionRecvWindow = 64 * 1024;
  private int _initialStreamRecvWindow = 256 * 64 * 1024;

  public HttpServerBuilder status(int status)
  {
    _status = status;
    return this;
  }

  public HttpServerBuilder headerSize(int headerSize)
  {
    _headerSize = headerSize;
    return this;
  }

  public HttpServerBuilder responseSize(int responseSize)
  {
    _responseSize = responseSize;
    return this;
  }

  public HttpServerBuilder idleTimeout(long idleTimeout)
  {
    _idleTimeout = idleTimeout;
    return this;
  }

  public HttpServerBuilder responseLatch(CountDownLatch responseLatch)
  {
    _responseLatch = responseLatch;
    return this;
  }

  public HttpServerBuilder exceptionListener(Consumer<Throwable> exceptionListener)
  {
    _exceptionListener = exceptionListener;
    return this;
  }

  public HttpServerBuilder minThreads(int minThreads)
  {
    _minThreads = minThreads;
    return this;
  }

  public HttpServerBuilder maxThreads(int maxThreads)
  {
    _maxThreads = maxThreads;
    return this;
  }

  public HttpServerBuilder maxConcurrentStreams(int maxConcurrentStreams)
  {
    _maxConcurrentStreams = maxConcurrentStreams;
    return this;
  }

  public HttpServerBuilder initialSessionRecvWindow(int initialSessionRecvWindow)
  {
    _initialSessionRecvWindow = initialSessionRecvWindow;
    return this;
  }

  public HttpServerBuilder initialStreamRecvWindow(int initialStreamRecvWindow)
  {
    _initialStreamRecvWindow = initialStreamRecvWindow;
    return this;
  }

  public Server build()
  {
    Server server = new Server(new QueuedThreadPool(_maxThreads, _minThreads));

    // HTTP Configuration
    HttpConfiguration configuration = new HttpConfiguration();
    configuration.setSendXPoweredBy(true);
    configuration.setSendServerVersion(true);
    configuration.setSendXPoweredBy(false);
    configuration.setSendServerVersion(false);
    configuration.setSendDateHeader(false);

    // HTTP connection factory
    HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(configuration);

    // HTTP/2 clear text connection factory
    HTTP2CServerConnectionFactory h2cConnectionFactory = new HTTP2CServerConnectionFactory(configuration);
    h2cConnectionFactory.setMaxConcurrentStreams(_maxConcurrentStreams);
    h2cConnectionFactory.setInitialStreamRecvWindow(_initialStreamRecvWindow);
    h2cConnectionFactory.setInitialSessionRecvWindow(_initialSessionRecvWindow);

    // HTTP Connector
    ServerConnector http = new ServerConnector(
        server,
        httpConnectionFactory,
        h2cConnectionFactory);
    http.setIdleTimeout(_idleTimeout);
    http.setPort(HTTP_PORT);
    server.addConnector(http);

    ServletContextHandler handler = new ServletContextHandler(server, "");
    handler.addServlet(new ServletHolder(new HttpServlet()
    {
      private static final long serialVersionUID = 0;

      @Override
      protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
      {
        try
        {
          awaitLatch();
          readEntity(req.getReader());
          prepareResponse(resp);
        }
        catch (Exception e)
        {
          _exceptionListener.accept(e);
          throw e;
        }
      }

      private void prepareResponse(HttpServletResponse resp) throws IOException
      {
        addStatus(resp);
        addHeader(resp);
        addContent(resp);
      }

      private void addStatus(HttpServletResponse resp) throws IOException
      {
        resp.setStatus(_status);
      }

      private void addHeader(HttpServletResponse resp) throws IOException
      {
        if (_headerSize <= 0)
        {
          return;
        }
        int valueSize = _headerSize - HEADER_NAME.length();
        char[] headerValue = new char[valueSize];
        Arrays.fill(headerValue, 'a');
        resp.addHeader(HEADER_NAME, new String(headerValue));
      }

      private void addContent(HttpServletResponse resp) throws IOException
      {
        if (_responseSize <= 0)
        {
          return;
        }
        char[] content = new char[_responseSize];
        Arrays.fill(content, 'a');
        resp.getWriter().write(content);
      }

      private void awaitLatch()
      {
        if (_responseLatch != null)
        {
          try
          {
            _responseLatch.await(RESPONSE_LATCH_TIMEOUT, RESPONSE_LATCH_TIMEUNIT);
          }
          catch (InterruptedException e)
          {
          }
        }
      }

      private void readEntity(BufferedReader reader) throws IOException
      {
        while (true)
        {
          char[] bytes = new char[8192];
          int read = reader.read(bytes);
          if (read < 0)
          {
            break;
          }
        }
      }
    }), "/*");

    return server;
  }
}
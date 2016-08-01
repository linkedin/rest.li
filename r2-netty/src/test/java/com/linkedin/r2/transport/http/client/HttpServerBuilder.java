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
  private long _idleTimeout = 30000;
  private CountDownLatch _responseLatch = null;

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

  public Server build()
  {
    Server server = new Server();

    // HTTP Configuration
    HttpConfiguration configuration = new HttpConfiguration();
    configuration.setSendXPoweredBy(true);
    configuration.setSendServerVersion(true);
    configuration.setSendXPoweredBy(false);
    configuration.setSendServerVersion(false);
    configuration.setSendDateHeader(false);

    // HTTP Connector
    ServerConnector http = new ServerConnector(
        server,
        new HttpConnectionFactory(configuration),
        new HTTP2CServerConnectionFactory(configuration));
    http.setIdleTimeout(_idleTimeout);
    http.setPort(HTTP_PORT);
    server.addConnector(http);

    ServletContextHandler handler = new ServletContextHandler(server, "");
    handler.addServlet(new ServletHolder(new HttpServlet()
    {
      private static final long serialVersionUID = 0;

      @Override
      protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
      {
        awaitLatch();
        readEntity(req.getReader());

        addStatus(resp);
        addHeader(resp);
        addContent(resp);
      }

      @Override
      protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
      {
        awaitLatch();
        readEntity(req.getReader());

        addStatus(resp);
        addHeader(resp);
        addContent(resp);
      }

      @Override
      protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
          throws ServletException, IOException
      {
        awaitLatch();
        readEntity(req.getReader());

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
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

/**
 * $Id: $
 */

package com.linkedin.r2.transport.http.server;


import javax.servlet.http.HttpServlet;
import java.io.IOException;

import com.linkedin.r2.filter.R2Constants;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class HttpJettyServer implements HttpServer
{
  protected final int _port;
  protected final int _threadPoolSize;
  protected final String _contextPath;
  protected final HttpServlet _servlet;

  protected Server _server;

  public enum ServletType {RAP, ASYNC_EVENT}

  public HttpJettyServer(int port, HttpDispatcher dispatcher, boolean restOverStream)
  {
    this(port, createServlet(dispatcher, ServletType.RAP, 0, restOverStream));
  }

  public HttpJettyServer(int port,
      String contextPath,
      int threadPoolSize,
      HttpDispatcher dispatcher)
  {
    this(port, contextPath, threadPoolSize, dispatcher, ServletType.RAP, 0, R2Constants.DEFAULT_REST_OVER_STREAM);
  }

  public HttpJettyServer(int port,
      String contextPath,
      int threadPoolSize,
      HttpDispatcher dispatcher,
      boolean restOverStream)
  {
    this(port, contextPath, threadPoolSize, dispatcher, ServletType.RAP, 0, restOverStream);
  }

  public HttpJettyServer(int port,
      String contextPath,
      int threadPoolSize,
      HttpDispatcher dispatcher,
      ServletType type,
      int asyncTimeout)
  {
    this(port, contextPath, threadPoolSize, createServlet(dispatcher, type, asyncTimeout, R2Constants.DEFAULT_REST_OVER_STREAM));
  }

  public HttpJettyServer(int port,
      String contextPath,
      int threadPoolSize,
      HttpDispatcher dispatcher,
      ServletType type,
      int asyncTimeout,
      boolean restOverStream)
  {
    this(port, contextPath, threadPoolSize, createServlet(dispatcher, type, asyncTimeout, restOverStream));
  }

  public HttpJettyServer(int port, HttpServlet servlet)
  {
    this(port,
        HttpServerFactory.DEFAULT_CONTEXT_PATH,
        HttpServerFactory.DEFAULT_THREAD_POOL_SIZE,
        servlet);
  }

  public HttpJettyServer(int port, String contextPath, int threadPoolSize, HttpServlet servlet)
  {
    _port = port;
    _contextPath = contextPath;
    _threadPoolSize = threadPoolSize;
    _servlet = servlet;
  }

  @Override
  public void start() throws IOException
  {
    _server = new Server(new QueuedThreadPool(_threadPoolSize));
    _server.setConnectors(getConnectors(_server));

    ServletContextHandler root =
        new ServletContextHandler(_server, _contextPath, ServletContextHandler.SESSIONS);
    root.addServlet(new ServletHolder(_servlet), "/*");

    try
    {
      _server.start();
    }
    catch (Exception e)
    {
      throw new IOException("Failed to start Jetty", e);
    }
  }

  @Override
  public void stop() throws IOException
  {
    if (_server != null)
    {
      try
      {
        _server.stop();
      }
      catch (Exception e)
      {
        throw new IOException("Failed to stop Jetty", e);
      }
    }

  }

  @Override
  public void waitForStop() throws InterruptedException
  {
    _server.join();
  }

  protected Connector[] getConnectors(Server server)
  {
    HttpConfiguration configuration = new HttpConfiguration();
    ServerConnector connector = new ServerConnector(
        server,
        new HttpConnectionFactory(configuration, HttpCompliance.RFC2616));
    connector.setPort(_port);
    return new Connector[] { connector };
  }

  private static HttpServlet createServlet(HttpDispatcher dispatcher, ServletType type, int timeout, boolean restOverStream)
  {
    HttpServlet httpServlet;
    switch (type)
    {
      case ASYNC_EVENT:
        httpServlet = restOverStream ? new AsyncR2StreamServlet(dispatcher, timeout) : new AsyncR2Servlet(dispatcher, timeout);
        break;
      default:
        httpServlet = restOverStream ? new RAPStreamServlet(dispatcher) : new RAPServlet(dispatcher);
    }

    return httpServlet;
  }
}
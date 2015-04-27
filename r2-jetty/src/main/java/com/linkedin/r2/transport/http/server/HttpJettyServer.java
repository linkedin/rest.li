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

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class HttpJettyServer implements HttpServer
{
  private final int         _port;
  private final String      _contextPath;
  private final int         _threadPoolSize;
  private Server            _server;
  private final HttpServlet _servlet;

  public HttpJettyServer(int port, HttpDispatcher dispatcher)
  {
    this(port, new RAPServlet(dispatcher));
  }

  public HttpJettyServer(int port,
                         String contextPath,
                         int threadPoolSize,
                         HttpDispatcher dispatcher,
                         boolean useAsync,
                         int asyncTimeOut)
  {
    this(port, contextPath, threadPoolSize,
         useAsync ? new AsyncR2Servlet(dispatcher, asyncTimeOut) :
                    new RAPServlet(dispatcher));
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
    _server = new Server();
    _server.setConnectors(getConnectors());
    _server.setThreadPool(new QueuedThreadPool(_threadPoolSize));
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


  protected Connector[] getConnectors()
  {
    SelectChannelConnector connector = new SelectChannelConnector();
    connector.setPort(_port);
    return new Connector[] { connector };
  }
}

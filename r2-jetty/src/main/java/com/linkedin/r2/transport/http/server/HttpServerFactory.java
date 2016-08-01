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

import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.transport.FilterChainDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;

import javax.servlet.http.HttpServlet;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class HttpServerFactory
{
  public static final String  DEFAULT_CONTEXT_PATH          = "/";
  public static final int     DEFAULT_THREAD_POOL_SIZE      = 512;
  public static final int     DEFAULT_ASYNC_TIMEOUT         = 30000;
  public static final HttpJettyServer.ServletType DEFAULT_SERVLET_TYPE = HttpJettyServer.ServletType.RAP;

  private final FilterChain _filters;
  private final HttpJettyServer.ServletType _servletType;

  public HttpServerFactory()
  {
    this(DEFAULT_SERVLET_TYPE);
  }

  public HttpServerFactory(HttpJettyServer.ServletType servletType)
  {
    this(FilterChains.empty(), servletType);
  }

  public HttpServerFactory(FilterChain filters)
  {
    this(filters, DEFAULT_SERVLET_TYPE);
  }

  public HttpServerFactory(FilterChain filters, HttpJettyServer.ServletType servletType)
  {
    _filters = filters;
    _servletType = servletType;
  }

  public HttpServer createServer(int port, TransportDispatcher transportDispatcher)
  {
    return createServer(port,
        DEFAULT_CONTEXT_PATH,
        DEFAULT_THREAD_POOL_SIZE,
        transportDispatcher, R2Constants.DEFAULT_REST_OVER_STREAM);
  }

  public HttpServer createServer(int port, TransportDispatcher transportDispatcher, boolean restOverStream)
  {
    return createServer(port,
        DEFAULT_CONTEXT_PATH,
        DEFAULT_THREAD_POOL_SIZE,
        transportDispatcher, restOverStream);
  }

  public HttpServer createServer(int port,
                                 String contextPath,
                                 int threadPoolSize,
                                 TransportDispatcher transportDispatcher)
  {
    return createServer(port, contextPath, threadPoolSize, transportDispatcher, _servletType, DEFAULT_ASYNC_TIMEOUT, R2Constants.DEFAULT_REST_OVER_STREAM);
  }

  public HttpServer createServer(int port,
                                 String contextPath,
                                 int threadPoolSize,
                                 TransportDispatcher transportDispatcher, boolean restOverStream)
  {
    return createServer(port, contextPath, threadPoolSize, transportDispatcher, _servletType, DEFAULT_ASYNC_TIMEOUT, restOverStream);
  }

  public HttpServer createServer(int port,
                                 String contextPath,
                                 int threadPoolSize,
                                 TransportDispatcher transportDispatcher,
                                 HttpJettyServer.ServletType servletType,
                                 int asyncTimeOut)
  {
    return createServer(port, contextPath, threadPoolSize, transportDispatcher, servletType, asyncTimeOut, R2Constants.DEFAULT_REST_OVER_STREAM);
  }

  public HttpServer createServer(int port,
                                 String contextPath,
                                 int threadPoolSize,
                                 TransportDispatcher transportDispatcher,
                                 HttpJettyServer.ServletType servletType,
                                 int asyncTimeOut,
                                 boolean restOverStream)
  {
    final TransportDispatcher filterDispatcher =
        new FilterChainDispatcher(transportDispatcher,  _filters);
    final HttpDispatcher dispatcher = new HttpDispatcher(filterDispatcher);
    return new HttpJettyServer(port,
                               contextPath,
                               threadPoolSize,
                               dispatcher,
                               servletType,
                               asyncTimeOut,
                               restOverStream);
  }

  public HttpServer createHttpsServer(int port,
                                      int sslPort,
                                      String keyStore,
                                      String keyStorePassword,
                                      TransportDispatcher transportDispatcher,
                                      HttpJettyServer.ServletType servletType)
  {
    return createHttpsServer(port, sslPort, keyStore, keyStorePassword, DEFAULT_CONTEXT_PATH, DEFAULT_THREAD_POOL_SIZE,
        transportDispatcher, servletType, DEFAULT_ASYNC_TIMEOUT, R2Constants.DEFAULT_REST_OVER_STREAM);
  }

  public HttpServer createHttpsServer(int port,
      int sslPort,
      String keyStore,
      String keyStorePassword,
      TransportDispatcher transportDispatcher,
      HttpJettyServer.ServletType servletType,
      boolean restOverStream)
  {
    return createHttpsServer(port, sslPort, keyStore, keyStorePassword, DEFAULT_CONTEXT_PATH, DEFAULT_THREAD_POOL_SIZE,
        transportDispatcher, servletType, DEFAULT_ASYNC_TIMEOUT, restOverStream);
  }

  public HttpServer createHttpsServer(int port,
                                      int sslPort,
                                      String keyStore,
                                      String keyStorePassword,
                                      String contextPath,
                                      int threadPoolSize,
                                      TransportDispatcher transportDispatcher,
                                      HttpJettyServer.ServletType servletType,
                                      int asyncTimeOut)
  {
    return createHttpsServer(port, sslPort, keyStore, keyStorePassword, contextPath, threadPoolSize, transportDispatcher, servletType, asyncTimeOut, R2Constants.DEFAULT_REST_OVER_STREAM);
  }

  public HttpServer createHttpsServer(int port,
                                      int sslPort,
                                      String keyStore,
                                      String keyStorePassword,
                                      String contextPath,
                                      int threadPoolSize,
                                      TransportDispatcher transportDispatcher,
                                      HttpJettyServer.ServletType servletType,
                                      int asyncTimeOut,
                                      boolean restOverStream)
  {
    final TransportDispatcher filterDispatcher =
      new FilterChainDispatcher(transportDispatcher, _filters);
    final HttpDispatcher dispatcher = new HttpDispatcher(filterDispatcher);
    return new HttpsJettyServer(port,
                                sslPort,
                                keyStore,
                                keyStorePassword,
                                contextPath,
                                threadPoolSize,
                                dispatcher,
                                servletType,
                                asyncTimeOut,
                                restOverStream);
  }

  public HttpServer createH2cServer(int port, TransportDispatcher transportDispatcher, boolean restOverStream)
  {
    return createH2cServer(port, DEFAULT_CONTEXT_PATH, DEFAULT_THREAD_POOL_SIZE, transportDispatcher, restOverStream);
  }

  public HttpServer createH2cServer(int port,
      String contextPath,
      int threadPoolSize,
      TransportDispatcher transportDispatcher,
      boolean restOverStream)
  {
    final TransportDispatcher filterDispatcher = new FilterChainDispatcher(transportDispatcher,  _filters);
    final HttpDispatcher dispatcher = new HttpDispatcher(filterDispatcher);
    return new H2cJettyServer(
        port,
        contextPath,
        threadPoolSize,
        dispatcher,
        restOverStream);
  }

  public HttpServer createServer(int port, TransportDispatcher transportDispatcher, int timeout, boolean restOverStream)
  {
    return createServer(port, DEFAULT_CONTEXT_PATH, DEFAULT_THREAD_POOL_SIZE, transportDispatcher, _servletType, timeout, restOverStream);
  }

  public HttpServer createRAPServer(int port, TransportDispatcher transportDispatcher, int timeout, boolean restOverStream)
  {
    final TransportDispatcher filterDispatcher =
        new FilterChainDispatcher(transportDispatcher,  _filters);
    HttpServlet httpServlet = restOverStream ? new RAPStreamServlet(filterDispatcher, timeout) : new RAPServlet(filterDispatcher);
    return new HttpJettyServer(port, httpServlet);
  }
}

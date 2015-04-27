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
import com.linkedin.r2.filter.transport.FilterChainDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class HttpServerFactory
{
  public static final String  DEFAULT_CONTEXT_PATH          = "/";
  public static final int     DEFAULT_THREAD_POOL_SIZE      = 512;
  public static final int     DEFAULT_ASYNC_TIMEOUT         = 5000;
  public static final boolean DEFAULT_USE_ASYNC_SERVLET_API = false;

  private final FilterChain _filters;

  public HttpServerFactory()
  {
    this(FilterChains.empty());
  }

  public HttpServerFactory(FilterChain filters)
  {
    _filters = filters;
  }

  public HttpServer createServer(int port, TransportDispatcher transportDispatcher)
  {
    return createServer(port,
                        DEFAULT_CONTEXT_PATH,
                        DEFAULT_THREAD_POOL_SIZE,
                        transportDispatcher,
                        DEFAULT_USE_ASYNC_SERVLET_API,
                        DEFAULT_ASYNC_TIMEOUT);
  }

  public HttpServer createServer(int port,
                                 String contextPath,
                                 int threadPoolSize,
                                 TransportDispatcher transportDispatcher)
  {
    return createServer(port,
                        contextPath,
                        threadPoolSize,
                        transportDispatcher,
                        DEFAULT_USE_ASYNC_SERVLET_API,
                        DEFAULT_ASYNC_TIMEOUT);
  }

  public HttpServer createServer(int port,
                                 String contextPath,
                                 int threadPoolSize,
                                 TransportDispatcher transportDispatcher,
                                 boolean useAsyncServletApi,
                                 int asyncTimeOut)
  {
    final TransportDispatcher filterDispatcher =
        new FilterChainDispatcher(transportDispatcher, _filters);
    final HttpDispatcher dispatcher = new HttpDispatcher(filterDispatcher);
    return new HttpJettyServer(port,
                               contextPath,
                               threadPoolSize,
                               dispatcher,
                               useAsyncServletApi,
                               asyncTimeOut);
  }

  public HttpServer createHttpsServer(int port,
      int sslPort,
      String keyStore,
      String keyStorePassword,
      TransportDispatcher transportDispatcher)
  {
    return createHttpsServer(port,
                             sslPort,
                             keyStore,
                             keyStorePassword,
                             DEFAULT_CONTEXT_PATH,
                             DEFAULT_THREAD_POOL_SIZE,
                             transportDispatcher,
                             DEFAULT_USE_ASYNC_SERVLET_API,
                             DEFAULT_ASYNC_TIMEOUT);
  }

  public HttpServer createServer(int port,
                                 int sslPort,
                                 String keyStore,
                                 String keyStorePassword,
                                 String contextPath,
                                 int threadPoolSize,
                                 TransportDispatcher transportDispatcher)
  {
    return createHttpsServer(port,
                             sslPort,
                             keyStore,
                             keyStorePassword,
                             contextPath,
                             threadPoolSize,
                             transportDispatcher,
                             DEFAULT_USE_ASYNC_SERVLET_API,
                             DEFAULT_ASYNC_TIMEOUT);
  }

  public HttpServer createHttpsServer(int port,
                                      int sslPort,
                                      String keyStore,
                                      String keyStorePassword,
                                      String contextPath,
                                      int threadPoolSize,
                                      TransportDispatcher transportDispatcher,
                                      boolean useAsyncServletApi,
                                      int asyncTimeOut)
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
                                useAsyncServletApi,
                                asyncTimeOut);
  }

}

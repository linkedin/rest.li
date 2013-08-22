/*
   Copyright (c) 2013 LinkedIn Corp.

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
 * Creates Netty backed Http servers.
 *
 * @author Chris Pettitt
 * @author Joe Betz
 * @version $Revision$
 */
public class HttpNettyServerFactory
{
  public static final int DEFAULT_THREAD_POOL_SIZE = 256;
  private final FilterChain _filters;

  public HttpNettyServerFactory()
  {
    this(FilterChains.empty());
  }

  public HttpNettyServerFactory(FilterChain filters)
  {
    _filters = filters;
  }

  public HttpServer createServer(int port, TransportDispatcher transportDispatcher)
  {
    return createServer(port, DEFAULT_THREAD_POOL_SIZE, transportDispatcher);
  }

  public HttpServer createServer(int port, int threadPoolSize, TransportDispatcher transportDispatcher)
  {
    final TransportDispatcher filterDispatcher = new FilterChainDispatcher(transportDispatcher, _filters);
    final HttpDispatcher dispatcher = new HttpDispatcher(filterDispatcher);
    return new HttpNettyServer(port, threadPoolSize, dispatcher);
  }
}

/*
   Copyright (c) 2019 LinkedIn Corp.

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

package test.r2.integ.clientserver.providers.server;

import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.server.HttpServerFactory;


/**
 * @author Nizar Mankulangara
 */
public class ServerCreationContext
{
  private final int _port;
  private final FilterChain _filterChain;
  private final TransportDispatcher _transportDispatcher;
  private final String _contextPath;
  private final int _threadPoolSize;
  private final int _serverTimeout;

  public ServerCreationContext(FilterChain filterChain, int port, TransportDispatcher dispatcher)
  {
    this(filterChain, port, dispatcher, HttpServerFactory.DEFAULT_ASYNC_TIMEOUT);
  }

  public ServerCreationContext(FilterChain filterChain, int port, TransportDispatcher dispatcher, int serverTimeout)
  {
    _port = port;
    _filterChain = filterChain;
    _transportDispatcher = dispatcher;
    _contextPath = HttpServerFactory.DEFAULT_CONTEXT_PATH;
    _threadPoolSize = HttpServerFactory.DEFAULT_THREAD_POOL_SIZE;
    _serverTimeout = serverTimeout;
  }

  public int getPort()
  {
    return _port;
  }

  public FilterChain getFilterChain()
  {
    return _filterChain;
  }

  public TransportDispatcher getTransportDispatcher()
  {
    return _transportDispatcher;
  }

  public String getContextPath()
  {
    return _contextPath;
  }

  public int getThreadPoolSize()
  {
    return _threadPoolSize;
  }

  public int getServerTimeout()
  {
    return _serverTimeout;
  }
}

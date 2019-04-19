/*
   Copyright (c) 2018 LinkedIn Corp.

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
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.Server;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.server.HttpJettyServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;


public class Http2JettyServerProvider implements ServerProvider
{
  private final HttpJettyServer.ServletType _servletType;
  private final boolean _serverROS;

  public Http2JettyServerProvider(HttpJettyServer.ServletType servletType, boolean serverROS)
  {
    _servletType = servletType;
    _serverROS = serverROS;
  }

  public Http2JettyServerProvider(boolean serverROS)
  {
    this(HttpServerFactory.DEFAULT_SERVLET_TYPE, serverROS);
  }

  @Override
  public Server createServer(FilterChain filters, int port)
  {
    return Bootstrap.createH2cServer(port, filters, _serverROS);
  }

  @Override
  public Server createServer(FilterChain filters, int port, TransportDispatcher dispatcher)
  {
    return Bootstrap.createH2cServer(port, filters, _serverROS, dispatcher);
  }

  @Override
  public Server createServer(ServerCreationContext context)
  {
    return new HttpServerFactory(context.getFilterChain()).createH2cServer(context.getPort(), context.getContextPath(),
        context.getThreadPoolSize(), context.getTransportDispatcher(), _servletType,
        context.getServerTimeout(), _serverROS);
  }

  @Override
  public String toString()
  {
    return "[" + getClass().getName() + ", stream=" + _serverROS + "]";
  }
}

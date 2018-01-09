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

package com.linkedin.r2.transport.http.server;

import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.transport.FilterChainDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 *  Convenient class for building {@link HttpNettyServer} with reasonable default configs.
 *  In order to build a {@link HttpNettyServer}, the following methods need to be called:
 *  {@link HttpNettyServerBuilder#_transportDispatcher} and {@link HttpNettyServerBuilder#_filters}.
 *
 *  If the port is not set by calling {@link HttpNettyServerBuilder#_port}, a default value
 *  will be used: {@link R2Constants#DEFAULT_NETTY_HTTP_SERVER_PORT}.
 */
public class HttpNettyServerBuilder
{
  // The following fields are required.
  private TransportDispatcher _transportDispatcher = null;
  private FilterChain _filters = null;

  // The following fields have default values.
  private int _port = R2Constants.DEFAULT_NETTY_HTTP_SERVER_PORT;
  private int _threadPoolSize = HttpNettyServerFactory.DEFAULT_THREAD_POOL_SIZE;
  private boolean _restOverStream = R2Constants.DEFAULT_REST_OVER_STREAM;

  // The following fields are optional.
  private SSLContext _sslContext = null;
  private SSLParameters _sslParameters = null;

  public HttpNettyServerBuilder filters(FilterChain filters)
  {
    _filters = filters;
    return this;
  }

  public HttpNettyServerBuilder port(int port)
  {
    _port = port;
    return this;
  }

  public HttpNettyServerBuilder threadPoolSize(int threadPoolSize)
  {
    _threadPoolSize = threadPoolSize;
    return this;
  }

  public HttpNettyServerBuilder transportDispatcher(TransportDispatcher dispatcher)
  {
    _transportDispatcher = dispatcher;
    return this;
  }

  public HttpNettyServerBuilder _restOverStream(boolean restOverStream)
  {
    _restOverStream = restOverStream;
    return this;
  }

  public HttpNettyServerBuilder sslContext(SSLContext sslContext)
  {
    _sslContext = sslContext;
    return this;
  }

  public HttpNettyServerBuilder sslParameters(SSLParameters sslParameters)
  {
    _sslParameters = sslParameters;
    return this;
  }

  public HttpNettyServer build()
  {
    validateParameters();
    final TransportDispatcher filterDispatcher = new FilterChainDispatcher(_transportDispatcher, _filters);
    final HttpDispatcher dispatcher = new HttpDispatcher(filterDispatcher);
    return new HttpNettyServer(_port, _threadPoolSize, dispatcher, _sslContext, _sslParameters);
  }

  private void validateParameters()
  {
    final String errorMsg = " is required by HttpNettyServerBuilder, however it was not set.";
    if (_transportDispatcher == null)
    {
      throw new NullPointerException("transportDispatcher" + errorMsg);
    }
    if (_filters == null)
    {
      throw new NullPointerException("filters" + errorMsg);
    }
  }
}

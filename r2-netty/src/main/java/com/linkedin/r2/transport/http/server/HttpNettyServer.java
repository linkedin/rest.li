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

import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.util.NamedThreadFactory;
import com.linkedin.pegasus.io.netty.bootstrap.ServerBootstrap;
import com.linkedin.pegasus.io.netty.channel.nio.NioEventLoopGroup;
import com.linkedin.pegasus.io.netty.channel.socket.nio.NioServerSocketChannel;
import com.linkedin.pegasus.io.netty.util.concurrent.DefaultEventExecutorGroup;
import com.linkedin.pegasus.io.netty.util.concurrent.EventExecutorGroup;
import java.net.InetSocketAddress;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;


/**
 *
 * @author Steven Ihde
 * @author Ang Xu
 */

/* package private */ class HttpNettyServer implements HttpServer
{
  private final int _port;
  private final int _threadPoolSize;
  private final HttpDispatcher _dispatcher;
  private final boolean _restOverStream;
  private final SSLContext _sslContext;
  private final SSLParameters _sslParameters;
  private final int _startupTimeoutMillis;

  private NioEventLoopGroup _bossGroup;
  private NioEventLoopGroup _workerGroup;
  private EventExecutorGroup _eventExecutors;

  public HttpNettyServer(int port, int threadPoolSize, HttpDispatcher dispatcher)
  {
    this(port, threadPoolSize, dispatcher, R2Constants.DEFAULT_REST_OVER_STREAM);
  }

  public HttpNettyServer(int port, int threadPoolSize, HttpDispatcher dispatcher,
      SSLContext sslContext, SSLParameters sslParameters)
  {
    this(port, threadPoolSize, dispatcher, R2Constants.DEFAULT_REST_OVER_STREAM, sslContext, sslParameters);
  }

  public HttpNettyServer(int port, int threadPoolSize, HttpDispatcher dispatcher, boolean restOverStream)
  {
    this(port, threadPoolSize, dispatcher, restOverStream, null,null);
  }

  public HttpNettyServer(int port, int threadPoolSize, HttpDispatcher dispatcher, boolean restOverStream,
      SSLContext sslContext, SSLParameters sslParameters)
  {
    this(port, threadPoolSize, dispatcher, restOverStream, sslContext, sslParameters, 10000);
  }

  public HttpNettyServer(int port, int threadPoolSize, HttpDispatcher dispatcher, boolean restOverStream,
                         SSLContext sslContext, SSLParameters sslParameters, int startupTimeoutMillis)
  {
    _port = port;
    _threadPoolSize = threadPoolSize;
    _dispatcher = dispatcher;
    _restOverStream = restOverStream;
    _sslContext = sslContext;
    _sslParameters = sslParameters;
    _startupTimeoutMillis = startupTimeoutMillis;
  }

  @Override
  public void start()
  {
    _eventExecutors =  new DefaultEventExecutorGroup(_threadPoolSize);
    _bossGroup = new NioEventLoopGroup(1, new NamedThreadFactory("R2 Nio Boss"));
    _workerGroup = new NioEventLoopGroup(0, new NamedThreadFactory("R2 Nio Worker"));

    final HttpNettyServerPipelineInitializer pipelineInitializer = new HttpNettyServerPipelineInitializer(
        _dispatcher, _eventExecutors, _sslContext, _sslParameters, _restOverStream);
    ServerBootstrap bootstrap = new ServerBootstrap()
                                      .group(_bossGroup, _workerGroup)
                                      .channel(NioServerSocketChannel.class)
                                      .childHandler(pipelineInitializer);
    bootstrap.bind(new InetSocketAddress(_port)).awaitUninterruptibly(_startupTimeoutMillis);
  }

  @Override
  public void stop()
  {
    // shut down Netty thread pool and close all channels associated with.
    try
    {
      _bossGroup.shutdownGracefully().sync();
    }
    catch(Exception ex)
    {
      // Do nothing
    }

    try
    {
      _workerGroup.shutdownGracefully().sync();
    }
    catch(Exception ex)
    {
      // Do nothing
    }

    try
    {
      _eventExecutors.shutdownGracefully().sync();
    }
    catch(Exception ex)
    {
      // Do nothing
    }
  }

  @Override
  public void waitForStop() throws InterruptedException
  {
    _bossGroup.terminationFuture().await();
    _workerGroup.terminationFuture().await();
    _eventExecutors.terminationFuture().await();
  }
}

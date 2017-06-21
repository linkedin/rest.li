/*
   Copyright (c) 2015 LinkedIn Corp.

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

import com.linkedin.r2.transport.http.client.common.ChannelPoolManager;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManagerFactory;
import com.linkedin.r2.transport.http.client.common.ChannelPoolManagerKeyBuilder;
import com.linkedin.r2.transport.http.client.rest.HttpNettyClient;
import com.linkedin.r2.transport.http.client.stream.http.HttpNettyStreamClient;
import com.linkedin.r2.transport.http.client.stream.http2.Http2NettyStreamClient;
import io.netty.channel.nio.NioEventLoopGroup;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;


/**
 * Convenient class for building {@link HttpNettyStreamClient} with reasonable default configs.
 *
 * @author Ang Xu
 * @author Francesco Capponi
 * @version $Revision: $
 */
class HttpClientBuilder
{

  private final ChannelPoolManagerKeyBuilder _channelPoolManagerKeyBuilder;
  private ExecutorService _callbackExecutors = null;
  private long _shutdownTimeout = 5000;
  private long _requestTimeout = 10000;
  private AbstractJmxManager _jmxManager = AbstractJmxManager.NULL_JMX_MANAGER;
  private final NioEventLoopGroup _eventLoopGroup;
  private final ScheduledExecutorService _scheduler;


  public HttpClientBuilder(NioEventLoopGroup eventLoopGroup, ScheduledExecutorService scheduler)
  {
    _eventLoopGroup = eventLoopGroup;
    _scheduler = scheduler;
    _channelPoolManagerKeyBuilder = new ChannelPoolManagerKeyBuilder();
  }

  public HttpClientBuilder setCallbackExecutors(ExecutorService callbackExecutors)
  {
    _callbackExecutors = callbackExecutors;
    return this;
  }

  /**
   * @param requestTimeout Timeout, in ms, to get a connection from the pool or create one
   */
  public HttpClientBuilder setRequestTimeout(long requestTimeout)
  {
    _requestTimeout = requestTimeout;
    setGracefulShutdownTimeout((int) _requestTimeout);
    return this;
  }

  /**
   * @param shutdownTimeout Timeout, in ms, the client should wait after shutdown is
   *                        initiated before terminating outstanding requests
   */
  public HttpClientBuilder setShutdownTimeout(long shutdownTimeout)
  {
    _shutdownTimeout = shutdownTimeout;
    return this;
  }

  /**
   * @param jmxManager A management class that is aware of the creation/shutdown event
   *                   of the underlying {@link ChannelPoolManager}
   */
  public HttpClientBuilder setJmxManager(AbstractJmxManager jmxManager)
  {
    _jmxManager = jmxManager;
    return this;
  }

  private ChannelPoolManagerFactory getChannelPoolManagerFactory()
  {
    return new ChannelPoolManagerFactory(_eventLoopGroup, _scheduler, _channelPoolManagerKeyBuilder.build());
  }

  public HttpNettyStreamClient buildStreamClient()
  {
    return new HttpNettyStreamClient(
      _eventLoopGroup,
      _scheduler,
      _requestTimeout,
      _shutdownTimeout,
      _callbackExecutors,
      _jmxManager,
      getChannelPoolManagerFactory().buildStream()
    );
  }

  public HttpNettyClient buildRestClient()
  {
    return new HttpNettyClient(
      _eventLoopGroup,
      _scheduler,
      _requestTimeout,
      _shutdownTimeout,
      _callbackExecutors,
      _jmxManager,
      getChannelPoolManagerFactory().buildRest()
    );
  }

  public Http2NettyStreamClient buildHttp2StreamClient()
  {
    return new Http2NettyStreamClient(
      _eventLoopGroup,
      _scheduler,
      _requestTimeout,
      _shutdownTimeout,
      _callbackExecutors,
      _jmxManager,
      getChannelPoolManagerFactory().buildHttp2Stream());
  }

  // Delegating parameters

  public HttpClientBuilder setSSLContext(SSLContext sslContext)
  {
    _channelPoolManagerKeyBuilder.setSSLContext(sslContext);
    return this;
  }

  public HttpClientBuilder setSSLParameters(SSLParameters sslParameters)
  {
    _channelPoolManagerKeyBuilder.setSSLParameters(sslParameters);
    return this;
  }

  public HttpClientBuilder setGracefulShutdownTimeout(int gracefulShutdownTimeout)
  {
    _channelPoolManagerKeyBuilder.setGracefulShutdownTimeout(gracefulShutdownTimeout);
    return this;
  }

  public HttpClientBuilder setIdleTimeout(long idleTimeout)
  {
    _channelPoolManagerKeyBuilder.setIdleTimeout(idleTimeout);
    return this;
  }


  public HttpClientBuilder setMaxHeaderSize(int maxHeaderSize)
  {
    _channelPoolManagerKeyBuilder.setMaxHeaderSize(maxHeaderSize);
    return this;
  }

  public HttpClientBuilder setMaxChunkSize(int maxChunkSize)
  {
    _channelPoolManagerKeyBuilder.setMaxChunkSize(maxChunkSize);
    return this;
  }

  public HttpClientBuilder setMaxResponseSize(long maxResponseSize)
  {
    _channelPoolManagerKeyBuilder.setMaxResponseSize(maxResponseSize);
    return this;
  }

  public HttpClientBuilder setMaxPoolSize(int maxPoolSize)
  {
    _channelPoolManagerKeyBuilder.setMaxPoolSize(maxPoolSize);
    return this;
  }

  public HttpClientBuilder setMinPoolSize(int minPoolSize)
  {
    _channelPoolManagerKeyBuilder.setMinPoolSize(minPoolSize);
    return this;
  }

  public HttpClientBuilder setMaxConcurrentConnectionInitializations(int maxConcurrentConnectionInitializations)
  {
    _channelPoolManagerKeyBuilder.setMaxConcurrentConnectionInitializations(maxConcurrentConnectionInitializations);
    return this;
  }

  public HttpClientBuilder setPoolWaiterSize(int poolWaiterSize)
  {
    _channelPoolManagerKeyBuilder.setPoolWaiterSize(poolWaiterSize);
    return this;
  }

  public HttpClientBuilder setStrategy(AsyncPoolImpl.Strategy strategy)
  {
    _channelPoolManagerKeyBuilder.setStrategy(strategy);
    return this;
  }

  public HttpClientBuilder setTcpNoDelay(boolean tcpNoDelay)
  {
    _channelPoolManagerKeyBuilder.setTcpNoDelay(tcpNoDelay);
    return this;
  }

}

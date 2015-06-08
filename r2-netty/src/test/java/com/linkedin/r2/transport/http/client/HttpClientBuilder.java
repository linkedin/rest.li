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

import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;


/**
 * Convenient class for building {@link HttpNettyStreamClient} with reasonable default configs.
 *
 * @author Ang Xu
 * @version $Revision: $
 */
class HttpClientBuilder
{
  private final NioEventLoopGroup _eventLoopGroup;
  private final ScheduledExecutorService _scheduler;

  private ExecutorService _callbackExecutors = null;
  private SSLContext _sslContext = null;
  private SSLParameters _sslParameters = null;
  private long _requestTimeout = 10000;
  private long _shutdownTimeout = 5000;
  private long _idleTimeout = 25000;
  private int _maxHeaderSize = 8192;
  private int _maxChunkSize = 8192;
  private int _maxResponseSize = 1024 * 1024 * 2;
  private String _name = "noNameSpecifiedClient";
  private int _maxPoolSize = 200;
  private int _minPoolSize = 0;
  private int _maxConcurrentConnections = Integer.MAX_VALUE;
  private int _poolWaiterSize = Integer.MAX_VALUE;
  private AsyncPoolImpl.Strategy _strategy = AsyncPoolImpl.Strategy.MRU;
  private AbstractJmxManager _jmxManager = AbstractJmxManager.NULL_JMX_MANAGER;
  private boolean _tcpNoDelay = true;


  public HttpClientBuilder(NioEventLoopGroup eventLoopGroup, ScheduledExecutorService scheduler)
  {
    _eventLoopGroup = eventLoopGroup;
    _scheduler = scheduler;
  }

  public HttpClientBuilder setCallbackExecutors(ExecutorService callbackExecutors)
  {
    _callbackExecutors = callbackExecutors;
    return this;
  }

  public HttpClientBuilder setSSLContext(SSLContext sslContext)
  {
    _sslContext = sslContext;
    return this;
  }

  public HttpClientBuilder setSSLParameters(SSLParameters sslParameters)
  {
    _sslParameters = sslParameters;
    return this;
  }

  public HttpClientBuilder setRequestTimeout(long requestTimeout)
  {
    _requestTimeout = requestTimeout;
    return this;
  }

  public HttpClientBuilder setShutdownTimeout(long shutdownTimeout)
  {
    _shutdownTimeout = shutdownTimeout;
    return this;
  }

  public HttpClientBuilder setIdleTimeout(long idleTimeout)
  {
    _idleTimeout = idleTimeout;
    return this;
  }

  public HttpClientBuilder setMaxHeaderSize(int maxHeaderSize)
  {
    _maxHeaderSize = maxHeaderSize;
    return this;
  }

  public HttpClientBuilder setMaxChunkSize(int maxChunkSize)
  {
    _maxChunkSize = maxChunkSize;
    return this;
  }

  public HttpClientBuilder setMaxResponseSize(int maxResponseSize)
  {
    _maxResponseSize = maxResponseSize;
    return this;
  }

  public HttpClientBuilder setClientName(String name)
  {
    _name = name;
    return this;
  }

  public HttpClientBuilder setMaxPoolSize(int maxPoolSize)
  {
    _maxPoolSize = maxPoolSize;
    return this;
  }

  public HttpClientBuilder setMinPoolSize(int minPoolSize)
  {
    _minPoolSize = minPoolSize;
    return this;
  }

  public void setMaxConcurrentConnections(int maxConcurrentConnections) {
    _maxConcurrentConnections = maxConcurrentConnections;
  }

  public HttpClientBuilder setPoolWaiterSize(int poolWaiterSize)
  {
    _poolWaiterSize = poolWaiterSize;
    return this;
  }

  public HttpClientBuilder setStrategy(AsyncPoolImpl.Strategy strategy)
  {
    _strategy = strategy;
    return this;
  }

  public HttpClientBuilder setJmxManager(AbstractJmxManager jmxManager)
  {
    _jmxManager = jmxManager;
    return this;
  }

  public HttpClientBuilder setTcpNoDelay(boolean tcpNoDelay)
  {
    _tcpNoDelay = tcpNoDelay;
    return this;
  }

  public HttpNettyStreamClient buildStream()
  {
    return new HttpNettyStreamClient(_eventLoopGroup,
                               _scheduler,
                               _maxPoolSize,
                               _requestTimeout,
                               _idleTimeout,
                               _shutdownTimeout,
                               _maxResponseSize,
                               _sslContext,
                               _sslParameters,
                               _callbackExecutors,
                               _poolWaiterSize,
                               _name,
                               _jmxManager,
                               _strategy,
                               _minPoolSize,
                               _maxHeaderSize,
                               _maxChunkSize,
                               _maxConcurrentConnections,
                               _tcpNoDelay);

  }

  public HttpNettyClient buildRest()
  {
    return new HttpNettyClient(_eventLoopGroup,
        _scheduler,
        _maxPoolSize,
        _requestTimeout,
        _idleTimeout,
        _shutdownTimeout,
        _maxResponseSize,
        _sslContext,
        _sslParameters,
        _callbackExecutors,
        _poolWaiterSize,
        _name,
        _jmxManager,
        _strategy,
        _minPoolSize,
        _maxHeaderSize,
        _maxChunkSize,
        _maxConcurrentConnections);

  }

}

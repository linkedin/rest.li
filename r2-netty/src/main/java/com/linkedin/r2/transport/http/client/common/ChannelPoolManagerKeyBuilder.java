/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client.common;

import com.linkedin.r2.transport.http.client.AsyncPoolImpl;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.pegasus.io.netty.util.internal.ObjectUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;


/**
 * Convenient class for building {@link ChannelPoolManagerKey} with reasonable default configs.
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class ChannelPoolManagerKeyBuilder
{
  private SSLContext _sslContext = null;
  private SSLParameters _sslParameters = null;
  private int _gracefulShutdownTimeout = 30000; // default value in netty
  private long _idleTimeout = HttpClientFactory.DEFAULT_IDLE_TIMEOUT;
  private long _sslIdleTimeout = HttpClientFactory.DEFAULT_SSL_IDLE_TIMEOUT;
  private int _maxHeaderSize = HttpClientFactory.DEFAULT_MAX_HEADER_SIZE;
  private int _maxChunkSize = HttpClientFactory.DEFAULT_MAX_CHUNK_SIZE;
  private long _maxResponseSize = HttpClientFactory.DEFAULT_MAX_RESPONSE_SIZE;
  private int _maxPoolSize = HttpClientFactory.DEFAULT_POOL_SIZE;
  private int _minPoolSize = HttpClientFactory.DEFAULT_POOL_MIN_SIZE;
  private int _maxConcurrentConnectionInitializations = HttpClientFactory.DEFAULT_MAX_CONCURRENT_CONNECTIONS;
  private int _poolWaiterSize = HttpClientFactory.DEFAULT_POOL_WAITER_SIZE;
  private AsyncPoolImpl.Strategy _strategy = HttpClientFactory.DEFAULT_POOL_STRATEGY;
  private boolean _tcpNoDelay = HttpClientFactory.DEFAULT_TCP_NO_DELAY;
  private String _poolStatsNamePrefix = HttpClientFactory.DEFAULT_POOL_STATS_NAME_PREFIX;

  /**
   * @param sslContext {@link SSLContext}
   */
  public ChannelPoolManagerKeyBuilder setSSLContext(SSLContext sslContext)
  {
    _sslContext = sslContext;
    return this;
  }

  /**
   * @param sslParameters {@link SSLParameters}with overloaded construct
   */
  public ChannelPoolManagerKeyBuilder setSSLParameters(SSLParameters sslParameters)
  {
    _sslParameters = sslParameters;
    return this;
  }

  /**
   * @param gracefulShutdownTimeout Graceful shutdown timeout dictates the amount of time an HTTP/2 connection waits
   *                                for existing streams to complete before shutting down the connection, by either
   *                                connection error or intentional connection close.
   *                                The suggested value is about the request timeout because there is no point to wait
   *                                any further if the request has already timed out.
   *                                The default Netty value is 30s.
   */
  public ChannelPoolManagerKeyBuilder setGracefulShutdownTimeout(int gracefulShutdownTimeout)
  {
    ObjectUtil.checkPositiveOrZero(gracefulShutdownTimeout, "gracefulShutdownTimeout");
    _gracefulShutdownTimeout = gracefulShutdownTimeout;
    return this;
  }

  /**
   * @param idleTimeout Interval after which idle connections will be automatically closed
   */
  public ChannelPoolManagerKeyBuilder setIdleTimeout(long idleTimeout)
  {
    ObjectUtil.checkPositive(idleTimeout, "idleTimeout");
    _idleTimeout = idleTimeout;
    return this;
  }

  /**
   * @param sslIdleTimeout Interval after which idle connections will be automatically closed
   */
  public ChannelPoolManagerKeyBuilder setSslIdleTimeout(long sslIdleTimeout)
  {
    ObjectUtil.checkPositive(sslIdleTimeout, "sslIdleTimeout");
    _sslIdleTimeout = sslIdleTimeout;
    return this;
  }

  /**
   * @param maxHeaderSize Maximum size of all HTTP headers
   */
  public ChannelPoolManagerKeyBuilder setMaxHeaderSize(int maxHeaderSize)
  {
    ObjectUtil.checkPositive(maxHeaderSize, "maxHeaderSize");
    _maxHeaderSize = maxHeaderSize;
    return this;
  }

  /**
   * @param maxChunkSize Maximum size of a HTTP chunk
   */
  public ChannelPoolManagerKeyBuilder setMaxChunkSize(int maxChunkSize)
  {
    ObjectUtil.checkPositive(maxChunkSize, "maxChunkSize");
    _maxChunkSize = maxChunkSize;
    return this;
  }

  /**
   * @param maxResponseSize Maximum size of a HTTP response
   */
  public ChannelPoolManagerKeyBuilder setMaxResponseSize(long maxResponseSize)
  {
    ObjectUtil.checkPositive(maxResponseSize, "maxResponseSize");
    _maxResponseSize = maxResponseSize;
    return this;
  }

  /**
   * @param maxPoolSize maximum size for each pool for each host. Http and Https have different pools
   */
  public ChannelPoolManagerKeyBuilder setMaxPoolSize(int maxPoolSize)
  {
    ObjectUtil.checkPositive(maxPoolSize, "maxPoolSize");
    _maxPoolSize = maxPoolSize;
    return this;
  }

  /**
   * @param minPoolSize minimum size for each pool for each host
   */
  public ChannelPoolManagerKeyBuilder setMinPoolSize(int minPoolSize)
  {
    ObjectUtil.checkPositiveOrZero(minPoolSize, "minPoolSize");
    _minPoolSize = minPoolSize;
    return this;
  }

  /**
   * In case of failure, this is the maximum number or connection that can be retried to establish at the same time
   */
  public ChannelPoolManagerKeyBuilder setMaxConcurrentConnectionInitializations(int maxConcurrentConnectionInitializations)
  {
    ObjectUtil.checkPositive(maxConcurrentConnectionInitializations, "maxConcurrentConnectionInitializations");
    _maxConcurrentConnectionInitializations = maxConcurrentConnectionInitializations;
    return this;
  }

  /**
   * PoolWaiterSize is the max # of concurrent waiters for getting a connection/stream from the AsyncPool
   */
  public ChannelPoolManagerKeyBuilder setPoolWaiterSize(int poolWaiterSize)
  {
    ObjectUtil.checkPositiveOrZero(poolWaiterSize, "poolWaiterSize");
    _poolWaiterSize = poolWaiterSize;
    return this;
  }

  /**
   * @param strategy The strategy used to return pool objects
   */
  public ChannelPoolManagerKeyBuilder setStrategy(AsyncPoolImpl.Strategy strategy)
  {
    ObjectUtil.checkNotNull(strategy, "strategy");
    _strategy = strategy;
    return this;
  }

  /**
   * @param poolStatsNamePrefix The name prefix before the hash of properties
   */
  public ChannelPoolManagerKeyBuilder setPoolStatsNamePrefix(String poolStatsNamePrefix)
  {
    ObjectUtil.checkNotNull(poolStatsNamePrefix, "poolStatsNamePrefix");
    _poolStatsNamePrefix = poolStatsNamePrefix;
    return this;
  }

  /**
   * @param tcpNoDelay flag to enable/disable Nagle's algorithm
   */
  public ChannelPoolManagerKeyBuilder setTcpNoDelay(boolean tcpNoDelay)
  {
    _tcpNoDelay = tcpNoDelay;
    return this;
  }

  public ChannelPoolManagerKey build()
  {
    return new ChannelPoolManagerKey(_sslContext, _sslParameters, _gracefulShutdownTimeout, _idleTimeout, _sslIdleTimeout,
      _maxHeaderSize, _maxChunkSize, _maxResponseSize, _maxPoolSize, _minPoolSize, _maxConcurrentConnectionInitializations,
      _poolWaiterSize, _strategy, _tcpNoDelay, _poolStatsNamePrefix);
  }
}

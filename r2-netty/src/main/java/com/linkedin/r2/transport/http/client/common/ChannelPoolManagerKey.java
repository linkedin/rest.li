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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;


/**
 * Class to store transport properties to create a channel pool manager
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class ChannelPoolManagerKey
{
  private final SSLContext _sslContext;
  private final SSLParameters _sslParameters;

  private final int _gracefulShutdownTimeout;
  private final long _idleTimeout;
  private final long _sslIdleTimeout;
  private final int _maxHeaderSize;
  private final int _maxChunkSize;
  private final long _maxResponseSize;
  private final int _maxPoolSize;
  private final int _minPoolSize;
  private final int _maxConcurrentConnectionInitializations;
  private final int _poolWaiterSize;
  private final AsyncPoolImpl.Strategy _strategy;
  private final boolean _tcpNoDelay;
  private final String _poolStatsNamePrefix;
  private final String _udsAddress;

  @Deprecated
  public ChannelPoolManagerKey(SSLContext sslContext, SSLParameters sslParameters, int gracefulShutdownTimeout,
      long idleTimeout, long sslIdleTimeout, int maxHeaderSize, int maxChunkSize,
      long maxResponseSize, int maxPoolSize, int minPoolSize,
      int maxConcurrentConnectionInitializations, int poolWaiterSize, AsyncPoolImpl.Strategy strategy,
      boolean tcpNoDelay, String poolStatsNamePrefix) {
    this(sslContext, sslParameters, gracefulShutdownTimeout, idleTimeout, sslIdleTimeout, maxHeaderSize, maxChunkSize,
        maxResponseSize, maxPoolSize, minPoolSize, maxConcurrentConnectionInitializations, poolWaiterSize, strategy,
        tcpNoDelay, poolStatsNamePrefix, null);
  }

  public ChannelPoolManagerKey(SSLContext sslContext, SSLParameters sslParameters, int gracefulShutdownTimeout,
                               long idleTimeout, long sslIdleTimeout, int maxHeaderSize, int maxChunkSize,
                               long maxResponseSize, int maxPoolSize, int minPoolSize,
                               int maxConcurrentConnectionInitializations, int poolWaiterSize, AsyncPoolImpl.Strategy strategy,
                               boolean tcpNoDelay, String poolStatsNamePrefix, String udsAddress)
  {
    _sslContext = sslContext;
    _sslParameters = sslParameters;
    _gracefulShutdownTimeout = gracefulShutdownTimeout;
    _idleTimeout = idleTimeout;
    _sslIdleTimeout = sslIdleTimeout;
    _maxHeaderSize = maxHeaderSize;
    _maxChunkSize = maxChunkSize;
    _maxResponseSize = maxResponseSize;
    _maxPoolSize = maxPoolSize;
    _minPoolSize = minPoolSize;
    _maxConcurrentConnectionInitializations = maxConcurrentConnectionInitializations;
    _poolWaiterSize = poolWaiterSize;
    _strategy = strategy;
    _tcpNoDelay = tcpNoDelay;
    _poolStatsNamePrefix = poolStatsNamePrefix;
    _udsAddress = udsAddress;
  }

  /**
   * Helper for equals and uniqueKeyBasedOnProperties that returns true if the sslContext and sslParameter have been set to avoid the hash
   * being dependent on the memory address of the two variables
   */
  private boolean isSsl()
  {
    return _sslContext != null && _sslParameters != null;
  }

  /**
   * The name is determined by an hash on the transport properties
   */
  public String getName()
  {
    return _poolStatsNamePrefix + " " + String.valueOf(uniqueKeyBasedOnProperties()) + " " + (isSsl() ? "SSL" : "");
  }

  /**
   * Composed by all the transport client properties that identify uniquely the ChannelPoolManager.
   * The implementation of this function is the standard one to determine an hashCode.
   */
  private int uniqueKeyBasedOnProperties()
  {
    int result = _gracefulShutdownTimeout;
    result = 31 * result + (int) (_idleTimeout ^ (_idleTimeout >>> 32));
    result = 31 * result + (int) (_sslIdleTimeout ^ (_sslIdleTimeout >>> 32));
    result = 31 * result + _maxHeaderSize;
    result = 31 * result + _maxChunkSize;
    result = 31 * result + (int) (_maxResponseSize ^ (_maxResponseSize >>> 32));
    result = 31 * result + _maxPoolSize;
    result = 31 * result + _minPoolSize;
    result = 31 * result + _maxConcurrentConnectionInitializations;
    result = 31 * result + _poolWaiterSize;
    result = 31 * result + (_strategy != null ? _strategy.toString().hashCode() : 0);
    result = 31 * result + (_tcpNoDelay ? 1 : 0);
    result = 31 * result + (isSsl() ? 1 : 0);
    result = 31 * result + (_poolStatsNamePrefix != null ? _poolStatsNamePrefix.hashCode() : 0);
    return result;
  }

  public SSLContext getSslContext()
  {
    return _sslContext;
  }

  public SSLParameters getSslParameters()
  {
    return _sslParameters;
  }

  public int getGracefulShutdownTimeout()
  {
    return _gracefulShutdownTimeout;
  }

  /**
   * @return idleTimeout if the connection is NOT Ssl, otherwise it returns sslIdleTimeout
   */
  public long getIdleTimeout()
  {
    return isSsl() ? _sslIdleTimeout : _idleTimeout;
  }

  public int getMaxHeaderSize()
  {
    return _maxHeaderSize;
  }

  public int getMaxChunkSize()
  {
    return _maxChunkSize;
  }

  public long getMaxResponseSize()
  {
    return _maxResponseSize;
  }

  public int getMaxPoolSize()
  {
    return _maxPoolSize;
  }

  public int getMinPoolSize()
  {
    return _minPoolSize;
  }

  public int getMaxConcurrentConnectionInitializations()
  {
    return _maxConcurrentConnectionInitializations;
  }

  public int getPoolWaiterSize()
  {
    return _poolWaiterSize;
  }

  public AsyncPoolImpl.Strategy getStrategy()
  {
    return _strategy;
  }

  public boolean isTcpNoDelay()
  {
    return _tcpNoDelay;
  }

  public String getPoolStatsNamePrefix()
  {
    return _poolStatsNamePrefix;
  }

  public String getUdsAddress() {
    return _udsAddress;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (!(o instanceof ChannelPoolManagerKey)) return false;

    ChannelPoolManagerKey that = (ChannelPoolManagerKey) o;

    if (_gracefulShutdownTimeout != that._gracefulShutdownTimeout) return false;
    if (_idleTimeout != that._idleTimeout) return false;
    if (_maxHeaderSize != that._maxHeaderSize) return false;
    if (_maxChunkSize != that._maxChunkSize) return false;
    if (_maxResponseSize != that._maxResponseSize) return false;
    if (_maxPoolSize != that._maxPoolSize) return false;
    if (_minPoolSize != that._minPoolSize) return false;
    if (_maxConcurrentConnectionInitializations != that._maxConcurrentConnectionInitializations) return false;
    if (_poolWaiterSize != that._poolWaiterSize) return false;
    if (_tcpNoDelay != that._tcpNoDelay) return false;
    if (isSsl() != that.isSsl()) return false;
    if (_strategy != that._strategy) return false;
    return _poolStatsNamePrefix != null ? _poolStatsNamePrefix.equals(that._poolStatsNamePrefix) : that._poolStatsNamePrefix == null;
  }

  @Override
  public int hashCode()
  {
    return uniqueKeyBasedOnProperties();
  }
}

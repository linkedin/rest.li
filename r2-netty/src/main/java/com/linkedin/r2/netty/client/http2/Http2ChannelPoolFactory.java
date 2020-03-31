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

package com.linkedin.r2.netty.client.http2;

import com.linkedin.common.stats.NoopLongTracker;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.AsyncPoolImpl;
import com.linkedin.r2.transport.http.client.NoopRateLimiter;
import com.linkedin.r2.transport.http.client.common.ChannelPoolFactory;
import com.linkedin.r2.transport.http.client.common.ChannelPoolLifecycle;
import com.linkedin.util.clock.SystemClock;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.SocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * Factory class to produce {@link AsyncPool}&#060;{@link Channel}&#062; for Http2 channels
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public class Http2ChannelPoolFactory implements ChannelPoolFactory
{
  private final long _idleTimeout;
  private final long _maxContentLength;
  private final int _maxPoolWaiterSize;
  private final int _maxPoolSize;
  private final int _minPoolSize;
  private final boolean _tcpNoDelay;
  private final boolean _ssl;
  private final Bootstrap _bootstrap;
  private final ChannelGroup _allChannels;
  private final ScheduledExecutorService _scheduler;
  private final AsyncPoolImpl.Strategy _strategy;

  public Http2ChannelPoolFactory(
      ScheduledExecutorService scheduler,
      EventLoopGroup eventLoopGroup,
      ChannelGroup channelGroup,
      AsyncPoolImpl.Strategy strategy,
      SSLContext sslContext,
      SSLParameters sslParameters,
      int maxPoolSize,
      int minPoolSize,
      int maxPoolWaiterSize,
      int maxInitialLineLength,
      int maxHeaderSize,
      int maxChunkSize,
      long idleTimeout,
      long maxContentLength,
      boolean tcpNoDelay,
      boolean enableSSLSessionResumption,
      int connectTimeout,
      int sslHandShakeTimeout)
  {
    final ChannelInitializer<NioSocketChannel> initializer = new Http2ChannelInitializer(
        sslContext, sslParameters, maxInitialLineLength, maxHeaderSize, maxChunkSize, maxContentLength,
        enableSSLSessionResumption, sslHandShakeTimeout);

    _scheduler = scheduler;
    _allChannels = channelGroup;
    _strategy = strategy;
    _maxPoolSize = maxPoolSize;
    _minPoolSize = minPoolSize;
    _maxPoolWaiterSize = maxPoolWaiterSize;
    _idleTimeout = idleTimeout;
    _maxContentLength = maxContentLength;
    _tcpNoDelay = tcpNoDelay;

    _bootstrap = new Bootstrap().
        group(eventLoopGroup).
        channel(NioSocketChannel.class).
        option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout).
        handler(initializer);
    _ssl = sslContext != null && sslParameters != null;
  }

  @Override
  public AsyncPool<Channel> getPool(SocketAddress address)
  {
    return new AsyncPoolImpl<>(
        address.toString(),
        new Http2ChannelLifecycle(
            address,
            _scheduler,
            SystemClock.instance(),
            _allChannels,
            _ssl,
            _maxContentLength,
            _idleTimeout,
            new ChannelPoolLifecycle(
                address,
                _bootstrap,
                _allChannels,
                _tcpNoDelay
            )),
        _maxPoolSize,
        _idleTimeout,
        _scheduler,
        _maxPoolWaiterSize,
        _strategy,
        _minPoolSize,
        new NoopRateLimiter(),
        SystemClock.instance(),
        NoopLongTracker.instance());
  }
}

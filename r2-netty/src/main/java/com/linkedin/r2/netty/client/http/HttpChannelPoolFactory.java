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

package com.linkedin.r2.netty.client.http;

import com.linkedin.common.stats.NoopLongTracker;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.AsyncPoolImpl;
import com.linkedin.r2.transport.http.client.ExponentialBackOffRateLimiter;
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
 * Factory class to produce {@link AsyncPool}&#060;{@link Channel}&#062; for Http Channels
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public class HttpChannelPoolFactory implements ChannelPoolFactory
{
  private final long _idleTimeout;
  private final int _maxPoolWaiterSize;
  private final int _maxPoolSize;
  private final int _minPoolSize;
  private final int _maxConcurrentConnectionInitializations;
  private final boolean _tcpNoDelay;
  private final Bootstrap _bootstrap;
  private final ChannelGroup _allChannels;
  private final ScheduledExecutorService _scheduler;
  private final AsyncPoolImpl.Strategy _strategy;
  private int _channelPoolWaiterTimeout;

  public HttpChannelPoolFactory(
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
      int maxConcurrentConnectionInitializations,
      long idleTimeout,
      long maxContentLength,
      boolean tcpNoDelay,
      boolean enableSSLSessionResumption,
      int channelPoolWaiterTimeout,
      int connectTimeout,
      int sslHandShakeTimeout)
  {
    ChannelInitializer<NioSocketChannel> initializer = new HttpChannelInitializer(sslContext, sslParameters,
        maxInitialLineLength, maxHeaderSize, maxChunkSize, maxContentLength, enableSSLSessionResumption, sslHandShakeTimeout);

    _scheduler = scheduler;
    _allChannels = channelGroup;
    _strategy = strategy;
    _maxPoolSize = maxPoolSize;
    _minPoolSize = minPoolSize;
    _maxPoolWaiterSize = maxPoolWaiterSize;
    _maxConcurrentConnectionInitializations = maxConcurrentConnectionInitializations;
    _idleTimeout = idleTimeout;
    _tcpNoDelay = tcpNoDelay;
    _channelPoolWaiterTimeout = channelPoolWaiterTimeout;

    _bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioSocketChannel.class).
        option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout).handler(initializer);
  }

  @Override
  public AsyncPool<Channel> getPool(SocketAddress address)
  {
    return new AsyncPoolImpl<>(address.toString(),
        new ChannelPoolLifecycle(address,
            _bootstrap,
            _allChannels,
            _tcpNoDelay),
        _maxPoolSize,
        _idleTimeout,
        _channelPoolWaiterTimeout,
        _scheduler,
        _maxPoolWaiterSize,
        _strategy,
        _minPoolSize,
        new ExponentialBackOffRateLimiter(0,
            ChannelPoolLifecycle.MAX_PERIOD_BEFORE_RETRY_CONNECTIONS,
            ChannelPoolLifecycle.INITIAL_PERIOD_BEFORE_RETRY_CONNECTIONS,
            _scheduler,
            _maxConcurrentConnectionInitializations),
        SystemClock.instance(),
        NoopLongTracker.instance()
    );
  }
}

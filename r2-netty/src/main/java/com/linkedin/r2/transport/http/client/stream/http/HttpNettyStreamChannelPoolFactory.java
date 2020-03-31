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

package com.linkedin.r2.transport.http.client.stream.http;

import com.linkedin.common.stats.NoopLongTracker;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.AsyncPoolImpl;
import com.linkedin.r2.transport.http.client.common.ChannelPoolFactory;
import com.linkedin.r2.transport.http.client.common.ChannelPoolLifecycle;
import com.linkedin.r2.transport.http.client.ExponentialBackOffRateLimiter;
import com.linkedin.r2.transport.http.client.stream.http2.Http2NettyStreamClient;
import com.linkedin.util.clock.SystemClock;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.SocketAddress;
import java.util.concurrent.ScheduledExecutorService;

/**
 * It generates Pools of Channels for {@link Http2NettyStreamClient}
 */
public class HttpNettyStreamChannelPoolFactory implements ChannelPoolFactory
{
  private final Bootstrap _bootstrap;
  private final int _maxPoolSize;
  private final long _idleTimeout;
  private final int _maxPoolWaiterSize;
  private final AsyncPoolImpl.Strategy _strategy;
  private final int _minPoolSize;
  private final boolean _tcpNoDelay;
  private final ChannelGroup _allChannels;
  private final ScheduledExecutorService _scheduler;
  private final int _maxConcurrentConnectionInitializations;
  private final int _channelPoolWaiterTimeout;

  public HttpNettyStreamChannelPoolFactory(int maxPoolSize,
                                           long idleTimeout,
                                           int maxPoolWaiterSize,
                                           AsyncPoolImpl.Strategy strategy,
                                           int minPoolSize,
                                           boolean tcpNoDelay,
                                           ScheduledExecutorService scheduler,
                                           int maxConcurrentConnectionInitializations,
                                           SSLContext sslContext,
                                           SSLParameters sslParameters,
                                           int maxHeaderSize,
                                           int maxChunkSize,
                                           long maxResponseSize,
                                           boolean enableSSLSessionResumption,
                                           EventLoopGroup eventLoopGroup,
                                           ChannelGroup channelGroup,
                                           int channelPoolWaiterTimeout,
                                           int connectTimeout,
                                           int sslHandShakeTimeout)
  {
    ChannelInitializer<NioSocketChannel> initializer =
      new RAPStreamClientPipelineInitializer(sslContext, sslParameters, maxHeaderSize, maxChunkSize, maxResponseSize,
          enableSSLSessionResumption, sslHandShakeTimeout);

    Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup)
      .channel(NioSocketChannel.class)
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
      .handler(initializer);

    _bootstrap = bootstrap;
    _maxPoolSize = maxPoolSize;
    _idleTimeout = idleTimeout;
    _maxPoolWaiterSize = maxPoolWaiterSize;
    _strategy = strategy;
    _minPoolSize = minPoolSize;
    _tcpNoDelay = tcpNoDelay;
    _allChannels = channelGroup;
    _scheduler = scheduler;
    _maxConcurrentConnectionInitializations = maxConcurrentConnectionInitializations;
    _channelPoolWaiterTimeout = channelPoolWaiterTimeout;
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

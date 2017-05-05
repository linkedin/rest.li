package com.linkedin.r2.transport.http.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.SocketAddress;
import java.util.concurrent.ScheduledExecutorService;

class HttpNettyStreamChannelPoolFactory implements ChannelPoolFactory
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

  HttpNettyStreamChannelPoolFactory(int maxPoolSize,
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
                                        EventLoopGroup eventLoopGroup,
                                        ChannelGroup channelGroup)
  {
    ChannelInitializer<NioSocketChannel> initializer =
      new RAPClientPipelineInitializer(sslContext, sslParameters, maxHeaderSize, maxChunkSize, maxResponseSize);

    Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup)
      .channel(NioSocketChannel.class)
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
  }

  @Override
  public AsyncPool<Channel> getPool(SocketAddress address)
  {
    return new AsyncPoolImpl<>(address.toString() + " HTTP connection pool",
      new ChannelPoolLifecycle(address,
        _bootstrap,
        _allChannels,
        _tcpNoDelay),
      _maxPoolSize,
      _idleTimeout,
      _scheduler,
      _maxPoolWaiterSize,
      _strategy,
      _minPoolSize,
      new ExponentialBackOffRateLimiter(0,
        ChannelPoolLifecycle.MAX_PERIOD_BEFORE_RETRY_CONNECTIONS,
        ChannelPoolLifecycle.INITIAL_PERIOD_BEFORE_RETRY_CONNECTIONS,
        _scheduler,
        _maxConcurrentConnectionInitializations)
    );
  }
}

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

package com.linkedin.r2.transport.http.client.rest;

import com.linkedin.common.stats.NoopLongTracker;
import com.linkedin.r2.netty.common.SslHandlerUtil;
import com.linkedin.r2.netty.handler.common.SessionResumptionSslHandler;
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
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import java.net.SocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;


/**
 * it generates Pools of Channels for {@link HttpNettyClient}
 */
public class HttpNettyChannelPoolFactory implements ChannelPoolFactory
{
  private final Bootstrap _bootstrap;
  private final int _maxPoolSize;
  private final long _idleTimeout;
  private final int _maxPoolWaiterSize;
  private final AsyncPoolImpl.Strategy _strategy;
  private final int _minPoolSize;
  private final ChannelGroup _allChannels;
  private final ScheduledExecutorService _scheduler;
  private final int _maxConcurrentConnectionInitializations;
  private final int _channelPoolWaiterTimeout;

  public HttpNettyChannelPoolFactory(int maxPoolSize, long idleTimeout, int maxPoolWaiterSize, AsyncPoolImpl.Strategy strategy,
                                     int minPoolSize, EventLoopGroup eventLoopGroup, SSLContext sslContext, SSLParameters sslParameters, int maxHeaderSize,
                                     int maxChunkSize, int maxResponseSize, ScheduledExecutorService scheduler, int maxConcurrentConnectionInitializations,
                                     boolean enableSSLSessionResumption, ChannelGroup allChannels, int channelPoolWaiterTimeout,
                                     int connectTimeout, int sslHandShakeTimeout)
  {

    _allChannels = allChannels;
    _scheduler = scheduler;
    _maxConcurrentConnectionInitializations = maxConcurrentConnectionInitializations;
    _channelPoolWaiterTimeout = channelPoolWaiterTimeout;
    Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup)
      .channel(NioSocketChannel.class)
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
      .handler(new HttpClientPipelineInitializer(sslContext, sslParameters, maxHeaderSize, maxChunkSize, maxResponseSize,
          enableSSLSessionResumption, sslHandShakeTimeout));

    _bootstrap = bootstrap;
    _maxPoolSize = maxPoolSize;
    _idleTimeout = idleTimeout;
    _maxPoolWaiterSize = maxPoolWaiterSize;
    _strategy = strategy;
    _minPoolSize = minPoolSize;
  }

  @Override
  public AsyncPool<Channel> getPool(SocketAddress address)
  {
    return new AsyncPoolImpl<>(address.toString(),
      new ChannelPoolLifecycle(address,
        _bootstrap,
        _allChannels,
        false),
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

  static class HttpClientPipelineInitializer extends ChannelInitializer<NioSocketChannel>
  {
    private final SSLContext _sslContext;
    private final SSLParameters _sslParameters;

    private final ChannelPoolHandler _handler = new ChannelPoolHandler();
    private final RAPResponseHandler _responseHandler = new RAPResponseHandler();

    private final int _maxHeaderSize;
    private final int _maxChunkSize;
    private final int _maxResponseSize;
    private final boolean _enableSSLSessionResumption;
    private final int _sslSessionTimeout;

    /**
     * Creates new instance. If sslParameters is present the PipelineInitializer
     * will produce channels that support only https connections
     *  @param sslContext {@link SSLContext} to be used for TLS-enabled channel pipeline.
     * @param sslParameters {@link SSLParameters} to configure {@link SSLEngine}s created
     *          from sslContext. This is somewhat redundant to
     *          SSLContext.getDefaultSSLParameters(), but those turned out to be
     *          exceedingly difficult to configure, so we can't pass all desired
     * @param maxHeaderSize
     * @param maxChunkSize
     * @param maxResponseSize
     * @param enableSSLSessionResumption
     */
    public HttpClientPipelineInitializer(SSLContext sslContext, SSLParameters sslParameters, int maxHeaderSize,
                                         int maxChunkSize, int maxResponseSize, boolean enableSSLSessionResumption,
                                         int sslSessionTimeout)
    {
      _maxHeaderSize = maxHeaderSize;
      _maxChunkSize = maxChunkSize;
      _maxResponseSize = maxResponseSize;
      _enableSSLSessionResumption = enableSSLSessionResumption;
      _sslSessionTimeout = sslSessionTimeout;
      SslHandlerUtil.validateSslParameters(sslContext, sslParameters);
      _sslContext = sslContext;
      _sslParameters = sslParameters;
    }

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception
    {
      if (_sslContext != null)
      {
        ch.pipeline().addLast(SessionResumptionSslHandler.PIPELINE_SESSION_RESUMPTION_HANDLER,
          new SessionResumptionSslHandler(_sslContext, _sslParameters, _enableSSLSessionResumption, _sslSessionTimeout));
      }
      ch.pipeline().addLast("codec", new HttpClientCodec(4096, _maxHeaderSize, _maxChunkSize));
      ch.pipeline().addLast("dechunker", new HttpObjectAggregator(_maxResponseSize));
      ch.pipeline().addLast("rapiCodec", new RAPClientCodec());
      // the response handler catches the exceptions thrown by other layers. By consequence no handlers that throw exceptions
      // should be after this one, otherwise the exception won't be caught and managed by R2
      ch.pipeline().addLast("responseHandler", _responseHandler);
      ch.pipeline().addLast("channelManager", _handler);
    }
  }
}

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

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.netty.client.http.HttpChannelPoolFactory;
import com.linkedin.r2.netty.client.http2.Http2ChannelPoolFactory;
import com.linkedin.r2.transport.http.client.rest.HttpNettyChannelPoolFactory;
import com.linkedin.r2.transport.http.client.stream.http.HttpNettyStreamChannelPoolFactory;
import com.linkedin.r2.transport.http.client.stream.http2.Http2NettyStreamChannelPoolFactory;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Factory class to create the right instance of {@link ChannelPoolManagerImpl} given a set of transport properties
 * {@link ChannelPoolManagerKey}.
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class ChannelPoolManagerFactoryImpl implements ChannelPoolManagerFactory
{
  private static final Logger LOG = LoggerFactory.getLogger(ChannelPoolManagerFactoryImpl.class);

  /**
   * Maximum initial HTTP/1.1 line length (e.g. "GET / HTTP/1.0" or "HTTP/1.0 200 OK"),
   *  It can be made configurable if right requirement presents.
   *  If the length of the initial line exceeds this value, a TooLongFrameException will be raised.
   *  Since Http do not define a standard limit on this and different servers support different values for the
   *  initial header line,  we are using 4096 default value as most of the servers support 4096 and above
   */
  private static final int MAX_INITIAL_LINE_LENGTH = 4096;

  private final NioEventLoopGroup _eventLoopGroup;
  private final ScheduledExecutorService _scheduler;
  private final boolean _enableSSLSessionResumption;
  private final boolean _usePipelineV2;
  private final int _channelPoolWaiterTimeout;
  private final int _connectTimeout;
  private final int _sslHandShakeTimeout;

  /**
   * @param eventLoopGroup The NioEventLoopGroup; it is the caller's responsibility to
   *                       shut it down
   * @param scheduler      An executor; it is the caller's responsibility to shut it down
   * @param usePipelineV2 build channel pool manager for the new Netty pipeline.
   * @param enableSSLSessionResumption Enable reuse of Ssl Session.
   * @param usePipelineV2 Use unified new code.
   */
  public ChannelPoolManagerFactoryImpl(NioEventLoopGroup eventLoopGroup, ScheduledExecutorService scheduler,
      boolean enableSSLSessionResumption, boolean usePipelineV2, int channelPoolWaiterTimeout,
      int connectTimeout, int sslHandShakeTimeout)
  {
    _eventLoopGroup = eventLoopGroup;
    _scheduler = scheduler;
    _enableSSLSessionResumption = enableSSLSessionResumption;
    _usePipelineV2 = usePipelineV2;
    _channelPoolWaiterTimeout = channelPoolWaiterTimeout;
    _connectTimeout = connectTimeout;
    _sslHandShakeTimeout = sslHandShakeTimeout;
  }

  @Override
  public ChannelPoolManager buildRest(ChannelPoolManagerKey channelPoolManagerKey)
  {
    DefaultChannelGroup channelGroup = new DefaultChannelGroup("R2 client channels", _eventLoopGroup.next());

    // Logs a warning if the configured max response size exceeds the maximum integer value. Only the lower 32-bit
    // of the long will be taken during the cast, potentially setting erroneous max response size.
    if (channelPoolManagerKey.getMaxResponseSize() > Integer.MAX_VALUE)
    {
      LOG.warn("The configured max response size {} has exceeded the max value allowed {} for the HTTP Rest client. "
          + "Consider using the streaming implementation instead.",
        channelPoolManagerKey.getMaxResponseSize(), Integer.MAX_VALUE);
    }

    return new ChannelPoolManagerImpl(
      new HttpNettyChannelPoolFactory(
        channelPoolManagerKey.getMaxPoolSize(),
        channelPoolManagerKey.getIdleTimeout(),
        channelPoolManagerKey.getPoolWaiterSize(),
        channelPoolManagerKey.getStrategy(),
        channelPoolManagerKey.getMinPoolSize(),
        _eventLoopGroup,
        channelPoolManagerKey.getSslContext(),
        channelPoolManagerKey.getSslParameters(),
        channelPoolManagerKey.getMaxHeaderSize(),
        channelPoolManagerKey.getMaxChunkSize(),
        (int) channelPoolManagerKey.getMaxResponseSize(),
        _scheduler,
        channelPoolManagerKey.getMaxConcurrentConnectionInitializations(),
        _enableSSLSessionResumption,
        channelGroup,
        _channelPoolWaiterTimeout,
        _connectTimeout,
        _sslHandShakeTimeout),
      channelPoolManagerKey.getName(),
      channelGroup,
      _scheduler);
  }

  @Override
  public ChannelPoolManager buildStream(ChannelPoolManagerKey channelPoolManagerKey)
  {
    DefaultChannelGroup channelGroup = new DefaultChannelGroup("R2 client channels", _eventLoopGroup.next());
    ChannelPoolFactory channelPoolFactory;
    if (_usePipelineV2)
    {
      channelPoolFactory = new HttpChannelPoolFactory(
          _scheduler,
          _eventLoopGroup,
          channelGroup,
          channelPoolManagerKey.getStrategy(),
          channelPoolManagerKey.getSslContext(),
          channelPoolManagerKey.getSslParameters(),
          channelPoolManagerKey.getMaxPoolSize(),
          channelPoolManagerKey.getMinPoolSize(),
          channelPoolManagerKey.getPoolWaiterSize(),
          MAX_INITIAL_LINE_LENGTH,
          channelPoolManagerKey.getMaxHeaderSize(),
          channelPoolManagerKey.getMaxChunkSize(),
          channelPoolManagerKey.getMaxConcurrentConnectionInitializations(),
          channelPoolManagerKey.getIdleTimeout(),
          channelPoolManagerKey.getMaxResponseSize(),
          channelPoolManagerKey.isTcpNoDelay(),
          _enableSSLSessionResumption,
          _channelPoolWaiterTimeout,
          _connectTimeout,
          _sslHandShakeTimeout);
    }
    else
    {
      channelPoolFactory = new HttpNettyStreamChannelPoolFactory(
          channelPoolManagerKey.getMaxPoolSize(),
          channelPoolManagerKey.getIdleTimeout(),
          channelPoolManagerKey.getPoolWaiterSize(),
          channelPoolManagerKey.getStrategy(),
          channelPoolManagerKey.getMinPoolSize(),
          channelPoolManagerKey.isTcpNoDelay(),
          _scheduler,
          channelPoolManagerKey.getMaxConcurrentConnectionInitializations(),
          channelPoolManagerKey.getSslContext(),
          channelPoolManagerKey.getSslParameters(),
          channelPoolManagerKey.getMaxHeaderSize(),
          channelPoolManagerKey.getMaxChunkSize(),
          channelPoolManagerKey.getMaxResponseSize(),
          _enableSSLSessionResumption,
          _eventLoopGroup,
          channelGroup,
          _channelPoolWaiterTimeout,
          _connectTimeout,
          _sslHandShakeTimeout);
    }
    return new ChannelPoolManagerImpl(
        channelPoolFactory,
        channelPoolManagerKey.getName() + "-Stream",
        channelGroup,
        _scheduler);
  }

  @Override
  public ChannelPoolManager buildHttp2Stream(ChannelPoolManagerKey channelPoolManagerKey)
  {
    DefaultChannelGroup channelGroup = new DefaultChannelGroup("R2 client channels", _eventLoopGroup.next());
    ChannelPoolFactory channelPoolFactory;

    if (_usePipelineV2)
    {
      channelPoolFactory = new Http2ChannelPoolFactory(
          _scheduler,
          _eventLoopGroup,
          channelGroup,
          channelPoolManagerKey.getStrategy(),
          channelPoolManagerKey.getSslContext(),
          channelPoolManagerKey.getSslParameters(),
          channelPoolManagerKey.getMaxPoolSize(),
          channelPoolManagerKey.getMinPoolSize(),
          channelPoolManagerKey.getPoolWaiterSize(),
          MAX_INITIAL_LINE_LENGTH,
          channelPoolManagerKey.getMaxHeaderSize(),
          channelPoolManagerKey.getMaxChunkSize(),
          channelPoolManagerKey.getIdleTimeout(),
          channelPoolManagerKey.getMaxResponseSize(),
          channelPoolManagerKey.isTcpNoDelay(),
          _enableSSLSessionResumption,
          _connectTimeout,
          _sslHandShakeTimeout);
    }
    else
    {
      channelPoolFactory = new Http2NettyStreamChannelPoolFactory(
          channelPoolManagerKey.getIdleTimeout(),
          channelPoolManagerKey.getPoolWaiterSize(),
          channelPoolManagerKey.getMinPoolSize(),
          channelPoolManagerKey.isTcpNoDelay(),
          _scheduler,
          channelPoolManagerKey.getSslContext(),
          channelPoolManagerKey.getSslParameters(),
          channelPoolManagerKey.getGracefulShutdownTimeout(),
          channelPoolManagerKey.getMaxHeaderSize(),
          channelPoolManagerKey.getMaxChunkSize(),
          channelPoolManagerKey.getMaxResponseSize(),
          _enableSSLSessionResumption,
          _eventLoopGroup,
          channelGroup,
          _connectTimeout,
          _sslHandShakeTimeout);
    }

    return new ChannelPoolManagerImpl(
      channelPoolFactory,
      channelPoolManagerKey.getName() + "-HTTP/2-Stream",
      channelGroup,
      _scheduler);
  }

  /**
   * The standard {@link ChannelPoolManagerFactoryImpl} is stateless, and doesn't need to do any operation at shutdown
   */
  @Override
  public void shutdown(Callback<None> callback)
  {
    callback.onSuccess(None.none());
  }
}

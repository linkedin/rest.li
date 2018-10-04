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
import com.linkedin.r2.transport.http.client.rest.HttpNettyChannelPoolFactory;
import com.linkedin.r2.transport.http.client.stream.http.HttpNettyStreamChannelPoolFactory;
import com.linkedin.r2.transport.http.client.stream.http2.Http2NettyStreamChannelPoolFactory;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;


/**
 * Factory class to create the right instance of {@link ChannelPoolManagerImpl} given a set of transport properties
 * {@link ChannelPoolManagerKey}.
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class ChannelPoolManagerFactoryImpl implements ChannelPoolManagerFactory
{
  private static final Logger LOG = LoggerFactory.getLogger(ChannelPoolManagerFactoryImpl.class);

  private final NioEventLoopGroup _eventLoopGroup;
  private final ScheduledExecutorService _scheduler;
  private final boolean _enableSSLSessionResumption;

  /**
   * @param eventLoopGroup The NioEventLoopGroup; it is the caller's responsibility to
   *                       shut it down
   * @param scheduler      An executor; it is the caller's responsibility to shut it down
   * @param enableSSLSessionResumption
   */
  public ChannelPoolManagerFactoryImpl(NioEventLoopGroup eventLoopGroup, ScheduledExecutorService scheduler, boolean enableSSLSessionResumption)
  {
    _eventLoopGroup = eventLoopGroup;
    _scheduler = scheduler;
    _enableSSLSessionResumption = enableSSLSessionResumption;
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
        channelGroup),
      channelPoolManagerKey.getName(),
      channelGroup,
      _scheduler);
  }

  @Override
  public ChannelPoolManager buildStream(ChannelPoolManagerKey channelPoolManagerKey)
  {
    DefaultChannelGroup channelGroup = new DefaultChannelGroup("R2 client channels", _eventLoopGroup.next());

    return new ChannelPoolManagerImpl(
      new HttpNettyStreamChannelPoolFactory(
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
        channelGroup
      ),
      channelPoolManagerKey.getName() + "-Stream",
      channelGroup,
      _scheduler);
  }

  @Override
  public ChannelPoolManager buildHttp2Stream(ChannelPoolManagerKey channelPoolManagerKey)
  {
    DefaultChannelGroup channelGroup = new DefaultChannelGroup("R2 client channels", _eventLoopGroup.next());

    return new ChannelPoolManagerImpl(
      new Http2NettyStreamChannelPoolFactory(
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
        channelGroup),
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

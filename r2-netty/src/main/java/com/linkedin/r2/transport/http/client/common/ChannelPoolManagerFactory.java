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

import com.linkedin.r2.transport.http.client.rest.HttpNettyChannelPoolFactory;
import com.linkedin.r2.transport.http.client.stream.http.HttpNettyStreamChannelPoolFactory;
import com.linkedin.r2.transport.http.client.stream.http2.Http2NettyStreamChannelPoolFactory;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Factory class to create the right instance of {@link ChannelPoolManager} given a set of transport properties
 * {@link ChannelPoolManagerKey}.
 *
 * @author Francesco Capponi
 */
public class ChannelPoolManagerFactory
{
  private static final Logger LOG = LoggerFactory.getLogger(ChannelPoolManagerFactory.class);

  private final NioEventLoopGroup _eventLoopGroup;
  private final ScheduledExecutorService _scheduler;
  private final ChannelPoolManagerKey _channelPoolManagerKey;

  /**
   * @param eventLoopGroup        The NioEventLoopGroup; it is the caller's responsibility to
   *                              shut it down
   * @param scheduler             An executor; it is the caller's responsibility to shut it down
   * @param channelPoolManagerKey An object composed by all the transport client properties
   *                              to initialize the current client
   */
  public ChannelPoolManagerFactory(NioEventLoopGroup eventLoopGroup, ScheduledExecutorService scheduler, ChannelPoolManagerKey channelPoolManagerKey)
  {
    _eventLoopGroup = eventLoopGroup;
    _scheduler = scheduler;
    _channelPoolManagerKey = channelPoolManagerKey;
  }

  public ChannelPoolManager buildRest()
  {
    DefaultChannelGroup channelGroup = new DefaultChannelGroup("R2 client channels", _eventLoopGroup.next());

    // Logs a warning if the configured max response size exceeds the maximum integer value. Only the lower 32-bit
    // of the long will be taken during the cast, potentially setting erroneous max response size.
    if (_channelPoolManagerKey.getMaxResponseSize() > Integer.MAX_VALUE)
    {
      LOG.warn("The configured max response size {} has exceeded the max value allowed {} for the HTTP Rest client. "
          + "Consider using the streaming implementation instead.",
          _channelPoolManagerKey.getMaxResponseSize(), Integer.MAX_VALUE);
    }

    return new ChannelPoolManager(
      new HttpNettyChannelPoolFactory(
        _channelPoolManagerKey.getMaxPoolSize(),
        _channelPoolManagerKey.getIdleTimeout(),
        _channelPoolManagerKey.getPoolWaiterSize(),
        _channelPoolManagerKey.getStrategy(),
        _channelPoolManagerKey.getMinPoolSize(),
        _eventLoopGroup,
        _channelPoolManagerKey.getSslContext(),
        _channelPoolManagerKey.getSslParameters(),
        _channelPoolManagerKey.getMaxHeaderSize(),
        _channelPoolManagerKey.getMaxChunkSize(),
        (int) _channelPoolManagerKey.getMaxResponseSize(),
        _scheduler,
        _channelPoolManagerKey.getMaxConcurrentConnectionInitializations(),
        channelGroup),
      _channelPoolManagerKey.getName(),
      channelGroup);
  }

  public ChannelPoolManager buildStream()
  {
    DefaultChannelGroup channelGroup = new DefaultChannelGroup("R2 client channels", _eventLoopGroup.next());

    return new ChannelPoolManager(
      new HttpNettyStreamChannelPoolFactory(
        _channelPoolManagerKey.getMaxPoolSize(),
        _channelPoolManagerKey.getIdleTimeout(),
        _channelPoolManagerKey.getPoolWaiterSize(),
        _channelPoolManagerKey.getStrategy(),
        _channelPoolManagerKey.getMinPoolSize(),
        _channelPoolManagerKey.isTcpNoDelay(),
        _scheduler,
        _channelPoolManagerKey.getMaxConcurrentConnectionInitializations(),
        _channelPoolManagerKey.getSslContext(),
        _channelPoolManagerKey.getSslParameters(),
        _channelPoolManagerKey.getMaxHeaderSize(),
        _channelPoolManagerKey.getMaxChunkSize(),
        _channelPoolManagerKey.getMaxResponseSize(),
        _eventLoopGroup,
        channelGroup),
      _channelPoolManagerKey.getName() + "-Stream",
      channelGroup);
  }

  public ChannelPoolManager buildHttp2Stream()
  {
    DefaultChannelGroup channelGroup = new DefaultChannelGroup("R2 client channels", _eventLoopGroup.next());

    return new ChannelPoolManager(
      new Http2NettyStreamChannelPoolFactory(
        _channelPoolManagerKey.getIdleTimeout(),
        _channelPoolManagerKey.getPoolWaiterSize(),
        _channelPoolManagerKey.isTcpNoDelay(),
        _scheduler,
        _channelPoolManagerKey.getSslContext(),
        _channelPoolManagerKey.getSslParameters(),
        _channelPoolManagerKey.getGracefulShutdownTimeout(),
        _channelPoolManagerKey.getMaxHeaderSize(),
        _channelPoolManagerKey.getMaxChunkSize(),
        _channelPoolManagerKey.getMaxResponseSize(),
        _eventLoopGroup,
        channelGroup),
      _channelPoolManagerKey.getName() + "-HTTP/2-Stream",
      channelGroup);
  }
}

/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.r2.transport.http.client.common;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.RetriableRequestException;
import com.linkedin.r2.netty.common.SslHandlerUtil;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.r2.transport.http.client.AsyncPoolLifecycleStats;
import com.linkedin.r2.transport.http.client.PoolStats;
import com.linkedin.r2.transport.http.client.stream.http.HttpNettyStreamClient;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SystemClock;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import java.net.ConnectException;
import java.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
* @author Steven Ihde
* @version $Revision: $
*/
public class ChannelPoolLifecycle implements AsyncPool.Lifecycle<Channel>
{
  private static final Logger LOG = LoggerFactory.getLogger(ChannelPoolLifecycle.class);

  public static final AttributeKey<Long> CHANNEL_CREATION_TIME_KEY = AttributeKey.valueOf("channelCreationTime");

  /**
   * Maximum period in ms between retries for creating a channel in back-off policies
   */
  public static final int MAX_PERIOD_BEFORE_RETRY_CONNECTIONS = 5000;

  /**
   * When back-off policies are triggered in channel creation for the first time, this is the amount in ms to wait
   * before a second attempt
   */
  public static final int INITIAL_PERIOD_BEFORE_RETRY_CONNECTIONS = 100;

  /**
   * The default channel pool lifecycle stats returned when getStats() is called. Detailed stats is no longer
   * being tracked for performance reasons.
   */
  private static final AsyncPoolLifecycleStats DEFAULT_LIFECYCLE_STATS = new AsyncPoolLifecycleStats(0D, 0L, 0L, 0L);

  private final Clock _clock = SystemClock.instance();
  public final static String CHANNELPOOL_SSL_CALLBACK_HANDLER = "channelPoolSslCallbackHandler";

  private final SocketAddress _remoteAddress;
  private final Bootstrap _bootstrap;
  private final ChannelGroup _channelGroup;
  private final boolean _tcpNoDelay;

  public ChannelPoolLifecycle(SocketAddress address, Bootstrap bootstrap, ChannelGroup channelGroup, boolean tcpNoDelay)
  {
    _remoteAddress = address;
    _bootstrap = bootstrap;
    _channelGroup = channelGroup;
    _tcpNoDelay = tcpNoDelay;
  }

  @Override
  public void create(final Callback<Channel> channelCallback)
  {
    _bootstrap.connect(_remoteAddress).addListener((ChannelFutureListener) channelFuture -> {
      if (!channelFuture.isSuccess())
      {
        onError(channelCallback, channelFuture);
        return;
      }

      Channel c = channelFuture.channel();
      c.attr(CHANNEL_CREATION_TIME_KEY).set(_clock.currentTimeMillis());

      if (_tcpNoDelay)
      {
        c.config().setOption(ChannelOption.TCP_NODELAY, true);
      }
      _channelGroup.add(c);

      if (c.pipeline().get(SslHandlerUtil.PIPELINE_SSL_HANDLER) == null)
      {
        channelCallback.onSuccess(c);
        return;
      }

      c.pipeline().addAfter(SslHandlerUtil.PIPELINE_SSL_HANDLER, CHANNELPOOL_SSL_CALLBACK_HANDLER, new ChannelDuplexHandler()
      {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
        {
          if(evt == SslHandshakeCompletionEvent.SUCCESS){
            channelCallback.onSuccess(c);
            c.pipeline().remove(CHANNELPOOL_SSL_CALLBACK_HANDLER);
          }
          ctx.fireUserEventTriggered(evt);
        }
      });
    });
  }

  private void onError(Callback<Channel> channelCallback, Future<?> channelFuture)
  {
    Throwable cause = channelFuture.cause();
    LOG.error("Failed to create channel, remote={}", _remoteAddress, cause);
    if (cause instanceof ConnectException)
    {
      channelCallback.onError(new RetriableRequestException(cause));
    }
    else
    {
      channelCallback.onError(HttpNettyStreamClient.toException(cause));
    }
  }

  @Override
  public boolean validateGet(Channel c)
  {
    return c.isActive();
  }

  @Override
  public boolean validatePut(Channel c)
  {
    return c.isActive();
  }

  @Override
  public void destroy(final Channel channel, final boolean error, final Callback<Channel> channelCallback)
  {
    if (channel.isOpen())
    {
      channel.close().addListener((ChannelFutureListener) channelFuture -> {
        if (channelFuture.isSuccess())
        {
          channelCallback.onSuccess(channelFuture.channel());
        }
        else
        {
          final Throwable cause = channelFuture.cause();
          LOG.error("Failed to destroy channel, remote={}", _remoteAddress, cause);
          channelCallback.onError(HttpNettyStreamClient.toException(cause));
        }
      });
    }
    else
    {
      channelCallback.onSuccess(channel);
    }
  }

  @Override
  public PoolStats.LifecycleStats getStats()
  {
    return DEFAULT_LIFECYCLE_STATS;
  }
}

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

package com.linkedin.r2.netty.handler.common;

import com.linkedin.r2.netty.common.ChannelPipelineEvent;
import com.linkedin.r2.netty.common.NettyChannelAttributes;
import com.linkedin.r2.netty.common.ShutdownTimeoutException;
import com.linkedin.r2.transport.http.client.AsyncPool;
import com.linkedin.pegasus.io.netty.channel.Channel;
import com.linkedin.pegasus.io.netty.channel.ChannelHandlerContext;
import com.linkedin.pegasus.io.netty.channel.ChannelInboundHandler;
import com.linkedin.pegasus.io.netty.channel.ChannelInboundHandlerAdapter;
import com.linkedin.pegasus.io.netty.channel.pool.ChannelPool;

/**
 * An implementation of {@link ChannelInboundHandler} that returns or disposes the
 * {@link Channel} to the channel {@link AsyncPool} upon receiving response completion,
 * exception, or channel inactive events. The behavior upon response completion
 * event is configurable.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public class ChannelLifecycleHandler extends ChannelInboundHandlerAdapter
{
  private final boolean _recycle;

  public ChannelLifecycleHandler(boolean recycle)
  {
    _recycle = recycle;
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx)
  {
    tryDisposeChannel(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
  {
    if (isChannelRecyclableException(cause))
    {
      tryReturnChannel(ctx);
    }
    else
    {
      tryDisposeChannel(ctx);
    }
  }

  private boolean isChannelRecyclableException(Throwable cause)
  {
    return _recycle && cause instanceof ShutdownTimeoutException;
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
  {
    if (ChannelPipelineEvent.RESPONSE_COMPLETE == evt)
    {
      if (_recycle)
      {
        tryReturnChannel(ctx);
      }
      else
      {
        tryDisposeChannel(ctx);
      }
    }
    ctx.fireUserEventTriggered(evt);
  }

  /**
   * Attempts to the dispose the {@link Channel} to the {@link ChannelPool}. Disposes only
   * if the channel hasn't been previously returned or disposed, guaranteed by #getAndSet(null).
   * @param ctx Channel handler context
   */
  private void tryDisposeChannel(ChannelHandlerContext ctx)
  {
    final AsyncPool<Channel> pool = ctx.channel().attr(NettyChannelAttributes.CHANNEL_POOL).getAndSet(null);
    if (pool != null)
    {
      pool.dispose(ctx.channel());
    }
  }

  /**
   * Attempts to the return the {@link Channel} to the {@link ChannelPool}. Return only
   * if the channel hasn't been previously returned or disposed, guaranteed by #getAndSet(null).
   * @param ctx Channel handler context
   */
  private void tryReturnChannel(ChannelHandlerContext ctx)
  {
    final AsyncPool<Channel> pool = ctx.channel().attr(NettyChannelAttributes.CHANNEL_POOL).getAndSet(null);
    if (pool != null)
    {
      pool.put(ctx.channel());
    }
  }
}

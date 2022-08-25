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
import com.linkedin.r2.netty.common.StreamingTimeout;
import com.linkedin.r2.netty.common.NettyChannelAttributes;
import com.linkedin.pegasus.io.netty.channel.ChannelHandlerContext;
import com.linkedin.pegasus.io.netty.channel.ChannelInboundHandler;
import com.linkedin.pegasus.io.netty.channel.ChannelInboundHandlerAdapter;
import com.linkedin.pegasus.io.netty.channel.ChannelPipeline;
import java.util.concurrent.ScheduledFuture;

/**
 * An implementation of {@link ChannelInboundHandler} that is responsible for cancelling
 * the request timeout {@link ScheduledFuture} upon response completion, exception,
 * or channel inactive events.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public class CancelTimeoutHandler extends ChannelInboundHandlerAdapter
{
  @Override
  public void channelInactive(ChannelHandlerContext ctx)
  {
    tryCancelTimeout(ctx);
    ctx.fireChannelInactive();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
  {
    tryCancelTimeout(ctx);
    ctx.fireExceptionCaught(cause);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
  {
    if (ChannelPipelineEvent.RESPONSE_COMPLETE == evt)
    {
      tryCancelTimeout(ctx);
    }
    ctx.fireUserEventTriggered(evt);
  }

  /**
   * Gets the timeout {@link ScheduledFuture} from channel attributes and attempts to cancel. Cancel
   * only if the timeout future has not been previously cancelled, guaranteed by #getAndSet(null),
   * or is not already done, by checking #isDone on the future.
   * @param ctx Channel handler context
   */
  private void tryCancelTimeout(ChannelHandlerContext ctx)
  {
    ScheduledFuture<ChannelPipeline> timeout = ctx.channel().attr(NettyChannelAttributes.TIMEOUT_FUTURE).getAndSet(null);
    if (timeout != null && !timeout.isDone())
    {
      timeout.cancel(false);
    }

    StreamingTimeout streamTimeout = ctx.channel().attr(NettyChannelAttributes.STREAMING_TIMEOUT_FUTURE).getAndSet(null);
    if (streamTimeout != null)
    {
      streamTimeout.cancel();
    }
  }
}

package com.linkedin.r2.netty.common;

import com.linkedin.r2.netty.common.ChannelPoolLifecycle;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;


/**
 * Implementation of {@link ChannelFutureListener} that listens for failures, wrap the caught
 * throwable with {@link ChannelException}, and logs additional information about the channel
 * before forwarding the wrapped exception to the {@link ChannelPipeline}.
 */
public class ErrorChannelFutureListener implements ChannelFutureListener
{
  @Override
  public void operationComplete(ChannelFuture future) throws Exception
  {
    if (!future.isSuccess())
    {
      Channel channel = future.channel();
      Long createTime = channel.attr(ChannelPoolLifecycle.CHANNEL_CREATION_TIME_KEY).get();
      String message = String.format(
          "Channel %s encountered exception on write and flush, remote=%s, createTime=%s",
          channel.id(), channel.remoteAddress(), createTime);
      channel.pipeline().fireExceptionCaught(new ChannelException(message, future.cause()));
    }
  }
}

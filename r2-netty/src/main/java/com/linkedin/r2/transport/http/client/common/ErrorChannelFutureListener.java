package com.linkedin.r2.transport.http.client.common;

import com.linkedin.pegasus.io.netty.channel.Channel;
import com.linkedin.pegasus.io.netty.channel.ChannelException;
import com.linkedin.pegasus.io.netty.channel.ChannelFuture;
import com.linkedin.pegasus.io.netty.channel.ChannelFutureListener;
import com.linkedin.pegasus.io.netty.channel.ChannelPipeline;


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

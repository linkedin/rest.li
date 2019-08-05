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

import com.linkedin.common.callback.Callback;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.r2.netty.common.NettyChannelAttributes;
import com.linkedin.r2.netty.entitystream.StreamReader;
import com.linkedin.r2.netty.entitystream.StreamWriter;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.http.client.stream.OrderedEntityStreamReader;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

/**
 * Implementation of {@link ChannelDuplexHandler} that is responsible for sending {@link StreamRequest},
 * receiving {@link StreamResponseBuilder} and response entity in the form of {@link ByteString}s.
 *
 * This handler also integrates with R2 entity streaming with the help of {@link StreamReader} and
 * {@link StreamWriter}.
 *
 * The implementation guarantees the user {@link Callback} is invoked at most once
 * upon receiving response headers, exception, or channel inactive events. Together with timeout
 * {@link ScheduledFuture}, the implementation can also guarantee the callback is invoked eventually.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
@Sharable
public class ClientEntityStreamHandler extends ChannelDuplexHandler
{
  private final long _maxContentLength;

  public ClientEntityStreamHandler(long maxContentLength)
  {
    _maxContentLength = maxContentLength;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
  {
    if (msg instanceof StreamRequest)
    {
      StreamRequest request = (StreamRequest) msg;

      // Sets reader after the headers have been flushed on the channel
      OrderedEntityStreamReader orderedReader = new OrderedEntityStreamReader(ctx, new StreamReader(ctx));
      ctx.write(request).addListener(future -> request.getEntityStream().setReader(orderedReader));
    }
    else
    {
      ctx.write(msg);
    }
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg)
  {
    if (msg instanceof StreamResponseBuilder)
    {
      final StreamResponseBuilder builder = (StreamResponseBuilder) msg;

      final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      headers.putAll(builder.getHeaders());
      final Map<String, String> wireAttrs = WireAttributeHelper.removeWireAttributes(headers);

      final StreamWriter writer = new StreamWriter(ctx, _maxContentLength);
      ctx.channel().attr(NettyChannelAttributes.RESPONSE_WRITER).set(writer);

      final StreamResponse response = builder.unsafeSetHeaders(headers).build(EntityStreams.newEntityStream(writer));

      final TransportCallback<StreamResponse> callback = ctx.channel().attr(NettyChannelAttributes.RESPONSE_CALLBACK).getAndSet(null);
      if (callback != null)
      {
        callback.onResponse(TransportResponseImpl.success(response, wireAttrs));
      }
    }
    else if (msg instanceof ByteString)
    {
      final StreamWriter writer = msg == StreamWriter.EOF ?
          ctx.channel().attr(NettyChannelAttributes.RESPONSE_WRITER).getAndSet(null) :
          ctx.channel().attr(NettyChannelAttributes.RESPONSE_WRITER).get();
      if (writer != null)
      {
        writer.onDataAvailable((ByteString) msg);
      }
    }
    else
    {
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx)
  {
    tryInvokeCallbackWithError(ctx, ClosedChannelException::new);
    tryNotifyWriterWithError(ctx, ClosedChannelException::new);
    ctx.fireChannelInactive();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
  {
    tryInvokeCallbackWithError(ctx, () -> cause);
    tryNotifyWriterWithError(ctx, () -> cause);
    ctx.fireExceptionCaught(cause);
  }

  /**
   * Attempts to invoke {@link Callback} with the given {@link Throwable}. Callback can be invoked
   * at most once guaranteed by channel attributes #getAndSet(null).
   * @param ctx Channel handler context
   * @param causeSupplier Supplies throwable used to invoke the callback
   */
  private void tryInvokeCallbackWithError(ChannelHandlerContext ctx, Supplier<Throwable> causeSupplier)
  {
    final TransportCallback<StreamResponse> callback = ctx.channel().attr(NettyChannelAttributes.RESPONSE_CALLBACK).getAndSet(null);
    if (callback != null)
    {
      callback.onResponse(TransportResponseImpl.error(causeSupplier.get()));
    }
  }

  /**
   * Attempts to notify {@link Writer} with the given {@link Throwable}. Writer can be notified
   * at most once guaranteed by channel attributes #getAndSet(null)
   * @param ctx Channel handler context
   * @param causeSupplier Supplies throwable used to invoke the callback
   */
  private void tryNotifyWriterWithError(ChannelHandlerContext ctx, Supplier<Throwable> causeSupplier)
  {
    final StreamWriter writer = ctx.channel().attr(NettyChannelAttributes.RESPONSE_WRITER).getAndSet(null);
    if (writer != null)
    {
      writer.onError(causeSupplier.get());
    }
  }
}

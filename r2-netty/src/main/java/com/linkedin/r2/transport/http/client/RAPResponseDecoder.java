/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client;


import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.r2.util.Timeout;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ClosedChannelException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static io.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static io.netty.handler.codec.http.HttpHeaders.isTransferEncodingChunked;
import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.removeTransferEncodingChunked;

/**
 * This Decoder decodes chunked Netty responses into StreamResponse.
 *
 * @author Zhenkai Zhu
 */

/* package private */ class RAPResponseDecoder extends SimpleChannelInboundHandler<HttpObject>
{
  private static final Logger LOG = LoggerFactory.getLogger(RAPResponseDecoder.class);

  public static final AttributeKey<Timeout<None>> TIMEOUT_ATTR_KEY
      = AttributeKey.valueOf("TimeoutExecutor");
  private static final FullHttpResponse CONTINUE =
      new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER);

  private static final int BUFFER_HIGH_WATER_MARK = 3 * R2Constants.DEFAULT_DATA_CHUNK_SIZE;
  private static final int BUFFER_LOW_WATER_MARK = R2Constants.DEFAULT_DATA_CHUNK_SIZE;

  private final long _maxContentLength;

  private TimeoutBufferedWriter _chunkedMessageWriter;
  boolean _shouldCloseConnection;

  RAPResponseDecoder(long maxContentLength)
  {
    _maxContentLength = maxContentLength;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, HttpObject msg) throws Exception
  {
    if (msg instanceof HttpResponse)
    {
      HttpResponse m = (HttpResponse) msg;
      _shouldCloseConnection = !HttpUtil.isKeepAlive(m);

      if (HttpUtil.is100ContinueExpected(m))
      {
        ctx.writeAndFlush(CONTINUE).addListener(new ChannelFutureListener()
        {
          @Override
          public void operationComplete(ChannelFuture future)
              throws Exception
          {
            if (!future.isSuccess())
            {
              ctx.fireExceptionCaught(future.cause());
            }
          }
        });
      }
      if (!m.decoderResult().isSuccess())
      {
        ctx.fireExceptionCaught(m.decoderResult().cause());
        return;
      }
      // remove chunked encoding.
      if (HttpUtil.isTransferEncodingChunked(m))
      {
        HttpUtil.setTransferEncodingChunked(m, false);
      }

      Timeout<None> timeout = ctx.channel().attr(TIMEOUT_ATTR_KEY).getAndRemove();
      if (timeout == null)
      {
        LOG.debug("dropped a response after channel inactive or exception had happened.");
        return;
      }

      final TimeoutBufferedWriter writer = new TimeoutBufferedWriter(ctx, _maxContentLength,
          BUFFER_HIGH_WATER_MARK, BUFFER_LOW_WATER_MARK, timeout);
      EntityStream entityStream = EntityStreams.newEntityStream(writer);
      _chunkedMessageWriter = writer;
      StreamResponseBuilder builder = new StreamResponseBuilder();
      builder.setStatus(m.status().code());

      for (Map.Entry<String, String> e : m.headers())
      {
        String key = e.getKey();
        String value = e.getValue();
        if (key.equalsIgnoreCase(HttpConstants.RESPONSE_COOKIE_HEADER_NAME))
        {
          builder.addCookie(value);
        }
        else
        {
          builder.unsafeAddHeaderValue(key, value);
        }
      }

      ctx.fireChannelRead(builder.build(entityStream));
    }
    else if (msg instanceof HttpContent)
    {
      HttpContent chunk = (HttpContent) msg;
      TimeoutBufferedWriter currentWriter = _chunkedMessageWriter;
      // Sanity check
      if (currentWriter == null)
      {
        throw new IllegalStateException(
            "received " + HttpContent.class.getSimpleName() +
                " without " + HttpResponse.class.getSimpleName());
      }

      if (!chunk.decoderResult().isSuccess())
      {
        this.exceptionCaught(ctx, chunk.decoderResult().cause());
      }

      currentWriter.processHttpChunk(chunk);

      if (chunk instanceof LastHttpContent)
      {
        _chunkedMessageWriter = null;
        if (_shouldCloseConnection)
        {
          ctx.fireChannelRead(ChannelPoolStreamHandler.CHANNEL_DESTROY_SIGNAL);
        }
        else
        {
          ctx.fireChannelRead(ChannelPoolStreamHandler.CHANNEL_RELEASE_SIGNAL);
        }
      }
    }
    else
    {
      // something must be wrong, but let's proceed so that
      // handler after us has a chance to process it.
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    Timeout<None> timeout = ctx.channel().attr(TIMEOUT_ATTR_KEY).getAndRemove();
    if (timeout != null)
    {
      timeout.getItem();
    }
    if (_chunkedMessageWriter != null)
    {
      _chunkedMessageWriter.fail(new ClosedChannelException());
      _chunkedMessageWriter = null;
    }
    ctx.fireChannelInactive();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    Timeout<None> timeout = ctx.channel().attr(TIMEOUT_ATTR_KEY).getAndRemove();
    if (timeout != null)
    {
      timeout.getItem();
    }
    if (_chunkedMessageWriter != null)
    {
      _chunkedMessageWriter.fail(cause);
      _chunkedMessageWriter = null;
    }
    ctx.fireExceptionCaught(cause);
  }

  /**
   * A buffered writer that stops reading from socket if buffered bytes is larger than high water mark
   * and resumes reading from socket if buffered bytes is smaller than low water mark.
   */
  private class TimeoutBufferedWriter implements Writer
  {
    private final ChannelHandlerContext _ctx;
    private final long _maxContentLength;
    private final int _highWaterMark;
    private final int _lowWaterMark;
    private WriteHandle _wh;
    private boolean _lastChunkReceived;
    private int _totalBytesWritten;
    private int _bufferedBytes;
    private final List<ByteString> _buffer;
    private final Timeout<None> _timeout;
    private volatile Throwable _failureBeforeInit;

    TimeoutBufferedWriter(final ChannelHandlerContext ctx, long maxContentLength,
                          int highWaterMark, int lowWaterMark,
                          Timeout<None> timeout)
    {
      _ctx = ctx;
      _maxContentLength = maxContentLength;
      _highWaterMark = highWaterMark;
      _lowWaterMark = lowWaterMark;
      _failureBeforeInit = null;
      _lastChunkReceived = false;
      _totalBytesWritten = 0;
      _bufferedBytes = 0;
      _buffer = new LinkedList<ByteString>();

      // schedule a timeout to close the channel and inform use
      Runnable timeoutTask = new Runnable()
      {
        @Override
        public void run()
        {
          _ctx.executor().execute(new Runnable()
          {
            @Override
            public void run()
            {
              final Exception ex = new TimeoutException("Timeout while receiving the response entity.");
              fail(ex);
              ctx.fireExceptionCaught(ex);
            }
          });
        }
      };
      _timeout = timeout;
      _timeout.addTimeoutTask(timeoutTask);
    }

    @Override
    public void onInit(WriteHandle wh)
    {
      _wh = wh;
    }

    @Override
    public void onWritePossible()
    {
      if (_failureBeforeInit != null)
      {
        fail(_failureBeforeInit);
        return;
      }

      if (_ctx.executor().inEventLoop())
      {
        doWrite();
      }
      else
      {
        _ctx.executor().execute(new Runnable()
        {
          @Override
          public void run()
          {
            doWrite();
          }
        });
      }
    }

    @Override
    public void onAbort(Throwable ex)
    {
      _timeout.getItem();
      _ctx.fireExceptionCaught(ex);
    }

    public void processHttpChunk(HttpContent chunk) throws TooLongFrameException
    {
      if (chunk.content().readableBytes() + _totalBytesWritten > _maxContentLength)
      {
        TooLongFrameException ex = new TooLongFrameException("HTTP content length exceeded " + _maxContentLength +
            " bytes.");
        fail(ex);
        _chunkedMessageWriter = null;
        throw ex;
      }
      else
      {
        if (chunk.content().isReadable())
        {
          ByteBuf rawData = chunk.content();
          InputStream is = new ByteBufInputStream(rawData);
          final ByteString data;
          try
          {
            data = ByteString.read(is, rawData.readableBytes());
          }
          catch (IOException ex)
          {
            fail(ex);
            return;
          }
          _buffer.add(data);
          _bufferedBytes += data.length();
          if (_bufferedBytes > _highWaterMark && _ctx.channel().config().isAutoRead())
          {
            // stop reading from socket because we buffered too much
            _ctx.channel().config().setAutoRead(false);
          }
        }
        if (chunk instanceof LastHttpContent)
        {
          _lastChunkReceived = true;
          _timeout.getItem();
        }
        if (_wh != null)
        {
          doWrite();
        }
      }
    }

    public void fail(Throwable ex)
    {
      _timeout.getItem();
      if (_wh != null)
      {
        _wh.error(new RemoteInvocationException(ex));
      }
      else
      {
        _failureBeforeInit = ex;
      }
    }

    private void doWrite()
    {
      while(_wh.remaining() > 0)
      {
        if (!_buffer.isEmpty())
        {
          ByteString data = _buffer.remove(0);
          _wh.write(data);
          _bufferedBytes -= data.length();
          _totalBytesWritten += data.length();
          if (!_ctx.channel().config().isAutoRead() && _bufferedBytes < _lowWaterMark)
          {
            // resume reading from socket
            _ctx.channel().config().setAutoRead(true);
          }
        }
        else
        {
          if (_lastChunkReceived)
          {
            _wh.done();
          }
          break;
        }
      }
    }
  }
}

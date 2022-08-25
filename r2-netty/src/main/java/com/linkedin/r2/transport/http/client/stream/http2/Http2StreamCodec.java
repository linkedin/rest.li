/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client.stream.http2;

import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.netty.common.NettyRequestAdapter;
import com.linkedin.r2.transport.common.bridge.common.RequestWithCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.http.client.AsyncPoolHandle;
import com.linkedin.r2.transport.http.client.TimeoutAsyncPoolHandle;
import com.linkedin.r2.transport.http.client.stream.OrderedEntityStreamReader;
import com.linkedin.pegasus.io.netty.buffer.ByteBuf;
import com.linkedin.pegasus.io.netty.buffer.Unpooled;
import com.linkedin.pegasus.io.netty.channel.Channel;
import com.linkedin.pegasus.io.netty.channel.ChannelFuture;
import com.linkedin.pegasus.io.netty.channel.ChannelHandlerContext;
import com.linkedin.pegasus.io.netty.channel.ChannelPromise;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2ConnectionDecoder;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2ConnectionEncoder;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2ConnectionHandler;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2Error;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2Exception;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2Headers;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2Settings;
import java.util.Collections;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Encodes {@link StreamRequest} and {@link RestRequest} to HTTP/2 frames and decodes HTTP/2
 * frames to StreamRequest and RestRequest. Http/2 stream level errors should cause only the
 * stream to be reset, not the entire connection. As a result, errors specific to a stream
 * should not result in throwing non HTTP/2 stream exceptions in this codec.
 */
class Http2StreamCodec extends Http2ConnectionHandler
{
  private static final Logger LOG = LoggerFactory.getLogger(Http2StreamCodec.class);
  public static final String PIPELINE_HTTP2_CODEC_HANDLER = "http2Handler";

  private static final int NO_PADDING = 0;
  private static final int NO_DATA = 0;
  private static final boolean NOT_END_STREAM = false;
  private static final boolean END_STREAM = true;

  Http2StreamCodec(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings)
  {
    super(decoder, encoder, initialSettings);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    if (!(msg instanceof RequestWithCallback))
    {
      ctx.write(msg, promise);
      return;
    }

    Request request = ((RequestWithCallback)msg).request();
    Http2ConnectionEncoder encoder = encoder();
    int streamId = connection().local().incrementAndGetNextStreamId();
    final ChannelFuture headersFuture;
    if (request instanceof StreamRequest)
    {
      final StreamRequest streamRequest = (StreamRequest) request;
      final Http2Headers http2Headers = NettyRequestAdapter.toHttp2Headers(streamRequest);
      final BufferedReader bufferedReader = new BufferedReader(ctx, encoder, streamId, ((RequestWithCallback) msg).handle());
      final OrderedEntityStreamReader reader = new OrderedEntityStreamReader(ctx, bufferedReader);
      streamRequest.getEntityStream().setReader(reader);
      LOG.debug("Sent HTTP/2 HEADERS frame, stream={}, end={}, headers={}, padding={}bytes",
          new Object[] { streamId, NOT_END_STREAM, http2Headers.size(), NO_PADDING});
      headersFuture = encoder.writeHeaders(ctx, streamId, http2Headers, NO_PADDING, NOT_END_STREAM, promise);
      headersFuture.addListener(future -> {
        if (future.isSuccess())
        {
          reader.request(BufferedReader.MAX_BUFFERED_CHUNKS);
        }
      });
    }
    else if (request instanceof RestRequest)
    {
      final RestRequest restRequest = (RestRequest) request;
      final Http2Headers headers = NettyRequestAdapter.toHttp2Headers(restRequest);
      LOG.debug("Sent HTTP/2 HEADERS frame, stream={}, end={}, headers={}, padding={}bytes",
          new Object[] { streamId, NOT_END_STREAM, headers.size(), NO_PADDING});
      headersFuture = encoder.writeHeaders(ctx, streamId, headers, NO_PADDING, NOT_END_STREAM, promise);
      headersFuture.addListener(future -> {
        if (future.isSuccess())
        {
          final ByteBuf data = Unpooled.wrappedBuffer(restRequest.getEntity().asByteBuffer());
          LOG.debug("Sent HTTP/2 DATA frame, stream={}, end={}, data={}bytes, padding={}bytes",
              new Object[]{streamId, END_STREAM, data.readableBytes(), NO_PADDING});
          encoder.writeData(ctx, streamId, data, NO_PADDING, END_STREAM, ctx.newPromise());
          ctx.channel().flush();
        }
      });
    }
    else
    {
      // Release the handle to put the channel back to the pool
      ((RequestWithCallback) msg).handle().release();
      throw new IllegalArgumentException("Request is neither StreamRequest or RestRequest");
    }

    final TransportCallback<?> callback = ((RequestWithCallback)msg).callback();
    @SuppressWarnings("unchecked")
    final TimeoutAsyncPoolHandle<Channel> handle = (TimeoutAsyncPoolHandle<Channel>) ((RequestWithCallback) msg).handle();

    headersFuture.addListener(future -> {
      if (future.isSuccess())
      {
        // Sets TransportCallback as a stream property to be retrieved later
        Http2PipelinePropertyUtil.set(
            ctx, connection(), streamId, Http2ClientPipelineInitializer.CALLBACK_ATTR_KEY, callback);

        // Sets AsyncPoolHandle as a stream property to be retrieved later
        Http2PipelinePropertyUtil.set(
            ctx, connection(), streamId, Http2ClientPipelineInitializer.CHANNEL_POOL_HANDLE_ATTR_KEY, handle);

        // Sets a timeout task to reset stream
        // Channel pool handle is also released at timeout
        handle.addTimeoutTask(() -> {
          LOG.debug("Reset stream upon timeout, stream={}", streamId);
          resetStream(ctx, streamId, Http2Error.CANCEL.code(), ctx.newPromise());
          ctx.flush();
        });
      }
      else
      {
        // Invokes callback onResponse with the error thrown during write header or data
        callback.onResponse(TransportResponseImpl.error(future.cause()));

        // Releases the handle to put the channel back to the pool
        handle.release();

        // Resets the stream if a stream is created after we sent header
        if (connection().stream(streamId) != null)
        {
          LOG.debug("Reset stream upon timeout, stream={}", streamId);
          resetStream(ctx, streamId, Http2Error.CANCEL.code(), ctx.newPromise());
          ctx.flush();
        }
      }
    });
  }

  @Override
  protected void onStreamError(ChannelHandlerContext ctx, boolean outbound, Throwable cause, Http2Exception.StreamException streamException)
  {
    final int streamId = streamException.streamId();

    // Logs the full exception here
    final String message = String.format(
        "HTTP/2 stream encountered an exception, stream=%d, remote=%s, channel=%s",
        streamId, ctx.channel().remoteAddress(), ctx.channel().id());
    LOG.error(message, cause);
    try
    {
      doOnStreamError(ctx, streamId, cause);
    }
    finally
    {
      super.onStreamError(ctx, outbound, cause, streamException);
    }
  }

  @Override
  protected void onConnectionError(ChannelHandlerContext ctx, boolean outbound, Throwable cause, Http2Exception connectionError)
  {
    // Logs the full exception here
    final String message = String.format(
        "HTTP/2 connection encountered an exception, streamCount=%d, remote=%s, channel=%s",
        connection().numActiveStreams(), ctx.channel().remoteAddress(), ctx.channel().id());
    LOG.error(message, cause);
    try
    {
      connection().forEachActiveStream(stream -> {
        resetStream(ctx, stream.id(), Http2Error.CANCEL.code(), ctx.newPromise());
        doOnStreamError(ctx, stream.id(), cause);
        return true;
      });
      ctx.flush();
    }
    catch (Http2Exception e)
    {
      LOG.error("Encountered exception while invoking request callbacks with errors", e);
    }
    finally
    {
      super.onConnectionError(ctx, outbound, cause, connectionError);
    }
  }

  /**
   * If present, invokes the associated {@link TransportCallback} with error and releases the {@link Channel}
   * when an HTTP/2 stream encounters an error.
   *
   * @param ctx ChannelHandlerContext
   * @param streamId Stream ID
   * @param cause Cause of the error
   */
  private void doOnStreamError(ChannelHandlerContext ctx, int streamId, Throwable cause)
  {
    // Invokes the call back with error
    final TransportCallback<StreamResponse> callback = Http2PipelinePropertyUtil.remove(
        ctx, connection(), streamId, Http2ClientPipelineInitializer.CALLBACK_ATTR_KEY);
    if (callback != null)
    {
      callback.onResponse(TransportResponseImpl.<StreamResponse>error(cause, Collections.<String, String>emptyMap()));
    }

    // Signals to release the channel back to the pool
    final TimeoutAsyncPoolHandle<Channel> handle = Http2PipelinePropertyUtil.remove(
        ctx, connection(), streamId, Http2ClientPipelineInitializer.CHANNEL_POOL_HANDLE_ATTR_KEY);
    Optional.ofNullable(handle).ifPresent(TimeoutAsyncPoolHandle::release);
  }

  /**
   * A reader that has pipelining/buffered reading
   *
   * Buffering is actually done by Netty; we just enforce the upper bound of the buffering
   */
  private class BufferedReader implements Reader
  {
    private static final int MAX_BUFFERED_CHUNKS = 10;

    // this threshold is to mitigate the effect of the inter-play of Nagle's algorithm & Delayed ACK
    // when sending requests with small entity
    private static final int FLUSH_THRESHOLD = R2Constants.DEFAULT_DATA_CHUNK_SIZE;

    private final int _streamId;
    private final ChannelHandlerContext _ctx;
    private final Http2ConnectionEncoder _encoder;
    private final AsyncPoolHandle<?> _poolHandle;
    private volatile ReadHandle _readHandle;
    private int _notFlushedBytes;
    private int _notFlushedChunks;

    BufferedReader(ChannelHandlerContext ctx, Http2ConnectionEncoder encoder, int streamId, AsyncPoolHandle<?> poolHandle)
    {
      _streamId = streamId;
      _ctx = ctx;
      _encoder = encoder;
      _poolHandle = poolHandle;
      _notFlushedBytes = 0;
      _notFlushedChunks = 0;
    }

    @Override
    public void onInit(ReadHandle rh)
    {
      _readHandle = rh;
    }

    @Override
    public void onDataAvailable(final ByteString data)
    {
      ByteBuf content = Unpooled.wrappedBuffer(data.asByteBuffer());
      _encoder.writeData(_ctx, _streamId, content, NO_PADDING, NOT_END_STREAM, _ctx.channel().newPromise())
          .addListener(future -> _readHandle.request(1));
      LOG.debug("Sent HTTP/2 DATA frame, stream={}, end={}, data={}bytes, padding={}bytes",
          new Object[] { _streamId, NOT_END_STREAM, content.readableBytes(), NO_PADDING });
      _notFlushedBytes += data.length();
      _notFlushedChunks++;
      if (_notFlushedBytes >= FLUSH_THRESHOLD || _notFlushedChunks == MAX_BUFFERED_CHUNKS)
      {
        _ctx.channel().flush();
        _notFlushedBytes = 0;
        _notFlushedChunks = 0;
      }
    }

    @Override
    public void onDone()
    {
      _encoder.writeData(_ctx, _streamId, Unpooled.EMPTY_BUFFER, NO_PADDING, END_STREAM, _ctx.channel().newPromise());
      LOG.debug("Sent HTTP/2 DATA frame, stream={}, end={}, data={}bytes, padding={}bytes",
          new Object[] { _streamId, END_STREAM, NO_DATA, NO_PADDING });
      _ctx.channel().flush();
    }

    @Override
    public void onError(Throwable cause)
    {
      resetStream(_ctx, _streamId, Http2Error.CANCEL.code(), _ctx.newPromise());

      // Releases the handle to put the channel back to the pool
      _poolHandle.release();
    }
  }
}

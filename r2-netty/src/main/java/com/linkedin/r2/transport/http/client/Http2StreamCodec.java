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

/**
 * $Id: $
 */

package com.linkedin.r2.transport.http.client;

import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.transport.common.bridge.common.RequestWithCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.concurrent.PromiseCombiner;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http2.Http2CodecUtil.getEmbeddedHttp2Exception;
import static io.netty.handler.codec.http2.Http2Exception.isStreamError;


/**
 * Encodes {@link StreamRequest} and {@link RestRequest} to HTTP/2 frames and decodes HTTP/2
 * frames to StreamRequest and RestRequest. Http/2 stream level errors should cause only the
 * stream to be reset, not the entire connection. As a result, errors specific to a stream
 * should not result in throwing non HTTP/2 stream exceptions in this codec.
 */
class Http2StreamCodec extends Http2ConnectionHandler
{
  private static final Logger LOG = LoggerFactory.getLogger(Http2StreamCodec.class);

  private static final int NO_PADDING = 0;
  private static final int NO_DATA = 0;
  private static final boolean NOT_END_STREAM = false;
  private static final boolean END_STREAM = true;

  protected Http2StreamCodec(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
      Http2Settings initialSettings)
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
    if (request instanceof StreamRequest)
    {
      LOG.debug("Writing StreamRequest...");
      StreamRequest streamRequest = (StreamRequest) request;
      Http2Headers http2Headers = NettyRequestAdapter.toHttp2Headers(streamRequest);
      BufferedReader reader = new BufferedReader(ctx, encoder, streamId, ((RequestWithCallback) msg).handle());
      streamRequest.getEntityStream().setReader(reader);
      encoder.writeHeaders(ctx, streamId, http2Headers, NO_PADDING, NOT_END_STREAM, promise)
          .addListener(future -> reader.request());
      LOG.debug("Sent HTTP/2 HEADERS frame, stream={}, end={}, headers={}, padding={}bytes",
          new Object[] { streamId, NOT_END_STREAM, http2Headers.size(), NO_PADDING});
    }
    else if (request instanceof RestRequest)
    {
      LOG.debug("Writing RestRequest...");
      PromiseCombiner promiseCombiner = new PromiseCombiner();
      ChannelPromise headersPromise = ctx.channel().newPromise();
      ChannelPromise dataPromise = ctx.channel().newPromise();
      promiseCombiner.add(headersPromise);
      promiseCombiner.add(dataPromise);
      promiseCombiner.finish(promise);

      RestRequest restRequest = (RestRequest) request;
      Http2Headers headers = NettyRequestAdapter.toHttp2Headers(restRequest);
      encoder.writeHeaders(ctx, streamId, headers, NO_PADDING, NOT_END_STREAM, headersPromise);
      LOG.debug("Sent HTTP/2 HEADERS frame, stream={}, end={}, headers={}, padding={}bytes",
          new Object[] { streamId, NOT_END_STREAM, headers.size(), NO_PADDING});
      ByteBuf data = Unpooled.wrappedBuffer(restRequest.getEntity().asByteBuffer());
      encoder.writeData(ctx, streamId, data, NO_PADDING, END_STREAM, dataPromise);
      LOG.debug("Sent HTTP/2 DATA frame, stream={}, end={}, data={}bytes, padding={}bytes",
          new Object[] { streamId, END_STREAM, data.readableBytes(), NO_PADDING});
    }
    else
    {
      // Request type is not supported. Returns channel back to the pool and throws exception.
      ctx.fireChannelRead(((RequestWithCallback) msg).handle());
      throw new IllegalArgumentException("Request is neither StreamRequest or RestRequest");
    }

    // Sets TransportCallback as a stream property to be retrieved later
    TransportCallback<?> callback = ((RequestWithCallback)msg).callback();
    Http2Connection.PropertyKey callbackKey =
        ctx.channel().attr(Http2ClientPipelineInitializer.CALLBACK_ATTR_KEY).get();
    connection().stream(streamId).setProperty(callbackKey, callback);

    // Sets AsyncPoolHandle as a stream property to be retrieved later
    AsyncPoolHandle<?> handle = ((RequestWithCallback)msg).handle();
    Http2Connection.PropertyKey handleKey =
        ctx.channel().attr(Http2ClientPipelineInitializer.CHANNEL_POOL_HANDLE_ATTR_KEY).get();
    connection().stream(streamId).setProperty(handleKey, handle);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    super.exceptionCaught(ctx, cause);
    onError(ctx, cause);
  }

  @Override
  public void onError(ChannelHandlerContext ctx, Throwable cause)
  {
    super.onError(ctx, cause);
    Http2Exception http2Exception = getEmbeddedHttp2Exception(cause);
    if (http2Exception == null)
    {
      doHandleConnectionException(ctx, cause);
    }
    else
    {
      if (http2Exception instanceof Http2Exception.StreamException)
      {
        Http2Exception.StreamException streamException = (Http2Exception.StreamException) http2Exception;
        doHandleStreamException(connection().stream(streamException.streamId()), ctx, streamException);
      }
      else if (http2Exception instanceof Http2Exception.CompositeStreamException)
      {
        Http2Exception.CompositeStreamException compositException = (Http2Exception.CompositeStreamException) http2Exception;
        for (Http2Exception.StreamException streamException : compositException)
        {
          doHandleStreamException(connection().stream(streamException.streamId()), ctx, streamException);
        }
      }
      else
      {
        doHandleConnectionException(ctx, http2Exception);
      }
    }
  }

  private void doHandleStreamException(Http2Stream stream, ChannelHandlerContext ctx, Throwable cause)
  {
    Http2Connection.PropertyKey callbackKey =
        ctx.channel().attr(Http2ClientPipelineInitializer.CALLBACK_ATTR_KEY).get();
    Http2Connection.PropertyKey handleKey =
        ctx.channel().attr(Http2ClientPipelineInitializer.CHANNEL_POOL_HANDLE_ATTR_KEY).get();

    // Invokes the call back with error
    TimeoutTransportCallback<StreamResponse> callback = stream.removeProperty(callbackKey);
    if (callback != null)
    {
      callback.onResponse(TransportResponseImpl.<StreamResponse>error(cause, Collections.<String, String>emptyMap()));
    }

    // Signals to dispose the channel back to the pool
    TimeoutAsyncPoolHandle<Channel> handle = stream.removeProperty(handleKey);
    if (handle != null)
    {
      ctx.fireChannelRead(handle.error());
    }
  }

  private void doHandleConnectionException(ChannelHandlerContext ctx, Throwable cause)
  {
    try
    {
      connection().forEachActiveStream(stream -> {
        doHandleStreamException(stream, ctx, cause);
        return true;
      });
    }
    catch (Http2Exception e)
    {
      LOG.error("Encountered exception while invoking request callbacks with errors", e);
      super.onError(ctx, cause);
    }
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
        flush();
        _notFlushedBytes = 0;
        _notFlushedChunks = 0;
      }
    }

    @Override
    public void onDone()
    {
      _encoder.writeData(_ctx, _streamId, Unpooled.EMPTY_BUFFER, NO_PADDING, END_STREAM, _ctx.channel().voidPromise());
      LOG.debug("Sent HTTP/2 DATA frame, stream={}, end={}, data={}bytes, padding={}bytes",
          new Object[] { _streamId, END_STREAM, NO_DATA, NO_PADDING });
      flush();
    }

    @Override
    public void onError(Throwable cause)
    {
      resetStream(_ctx, _streamId, Http2Error.CANCEL.code(), _ctx.newPromise());

      // Signals Http2ChannelPoolHandler to return channel back to the async pool assuming the channel is still good
      _ctx.fireChannelRead(_poolHandle);
    }

    private void request()
    {
      _readHandle.request(MAX_BUFFERED_CHUNKS);
    }

    private void flush()
    {
      try
      {
        _encoder.flowController().writePendingBytes();
      }
      catch (Http2Exception e)
      {
        Http2StreamCodec.this.onError(_ctx, e);
      }
      finally
      {
        _ctx.flush();
      }
    }
  }
}

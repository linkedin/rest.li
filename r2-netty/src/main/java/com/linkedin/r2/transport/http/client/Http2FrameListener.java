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

import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.r2.transport.common.bridge.common.ResponseWithCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.r2.util.Timeout;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2EventAdapter;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2LifecycleManager;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2Stream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Listens to HTTP/2 frames and assembles {@link com.linkedin.r2.message.stream.StreamRequest}
 * and its {@link com.linkedin.r2.message.stream.entitystream.EntityStream}. Http/2 stream level
 * errors should cause only the stream to be reset, not the entire connection. As a result, errors
 * specific to a stream should not result in throwing non HTTP/2 stream exceptions in this codec.
 */
public class Http2FrameListener extends Http2EventAdapter
{
  public enum FrameEvent
  {
    SETTINGS_FRAME_RECEIVED
  }

  private static final Logger LOG = LoggerFactory.getLogger(Http2FrameListener.class);

  private final ScheduledExecutorService _scheduler;
  private final Http2Connection _connection;
  private final Http2Connection.PropertyKey _writerKey;
  private final Http2LifecycleManager _lifecycleManager;
  private final long _maxContentLength;
  private final long _streamingTimeout;

  public Http2FrameListener(ScheduledExecutorService scheduler, Http2Connection connection,
      Http2LifecycleManager lifecycleManager, long maxContentLength, long streamingTimeout)
  {
    _scheduler = scheduler;
    _connection = connection;
    _writerKey = connection.newKey();
    _lifecycleManager = lifecycleManager;
    _maxContentLength = maxContentLength;
    _streamingTimeout = streamingTimeout;
  }

  @Override
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency,
      short weight, boolean exclusive, int padding, boolean endStream) throws Http2Exception {
    onHeadersRead(ctx, streamId, headers, padding, endStream);
  }

  @Override
  public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding,
      boolean endOfStream) throws Http2Exception
  {
    LOG.debug("Received HTTP/2 HEADERS frame, stream={}, end={}, headers={}, padding={}bytes",
        new Object[]{streamId, endOfStream, headers.size(), padding});
    // Ignores response for the upgrade request
    if (streamId == Http2CodecUtil.HTTP_UPGRADE_STREAM_ID)
    {
      return;
    }

    final StreamResponseBuilder builder = new StreamResponseBuilder();
    // Process HTTP/2 pseudo headers
    if (headers.status() != null)
    {
      builder.setStatus(Integer.parseInt(headers.status().toString()));
    }
    if (headers.authority() != null)
    {
      builder.addHeaderValue(HttpHeaderNames.HOST.toString(), headers.authority().toString());
    }

    // Process other HTTP headers
    for (Map.Entry<CharSequence, CharSequence> header : headers)
    {
      if (Http2Headers.PseudoHeaderName.isPseudoHeader(header.getKey()))
      {
        // Do no set HTTP/2 pseudo headers to response
        continue;
      }

      final String key = header.getKey().toString();
      final String value = header.getValue().toString();
      if (key.equalsIgnoreCase(HttpConstants.RESPONSE_COOKIE_HEADER_NAME))
      {
        builder.addCookie(value);
      }
      else
      {
        builder.unsafeAddHeaderValue(key, value);
      }
    }

    // Gets async pool handle from stream properties
    Http2Connection.PropertyKey handleKey =
        ctx.channel().attr(Http2ClientPipelineInitializer.CHANNEL_POOL_HANDLE_ATTR_KEY).get();
    TimeoutAsyncPoolHandle<?> handle = _connection.stream(streamId).removeProperty(handleKey);
    if (handle == null)
    {
      _lifecycleManager.onError(ctx, Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR,
          "No channel pool handle is associated with this stream", streamId));
      return;
    }

    final StreamResponse response;
    if (endOfStream)
    {
      response = builder.build(EntityStreams.emptyStream());
      ctx.fireChannelRead(handle);
    }
    else
    {
      // Associate an entity stream writer to the HTTP/2 stream
      final TimeoutBufferedWriter writer = new TimeoutBufferedWriter(ctx, streamId, _maxContentLength, handle);
      if (_connection.stream(streamId).setProperty(_writerKey, writer) != null)
      {
        _lifecycleManager.onError(ctx, Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR,
            "Another writer has already been associated with current stream ID", streamId));
        return;
      }

      // Prepares StreamResponse for the channel pipeline
      EntityStream entityStream = EntityStreams.newEntityStream(writer);
      response = builder.build(entityStream);
    }

    // Gets callback from stream properties
    Http2Connection.PropertyKey callbackKey =
        ctx.channel().attr(Http2ClientPipelineInitializer.CALLBACK_ATTR_KEY).get();
    TransportCallback<?> callback = _connection.stream(streamId).removeProperty(callbackKey);
    if (callback != null)
    {
      ctx.fireChannelRead(new ResponseWithCallback<Response, TransportCallback<?>>(response, callback));
    }
  }

  @Override
  public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
      throws Http2Exception
  {
    LOG.debug("Received HTTP/2 DATA frame, stream={}, end={}, data={}bytes, padding={}bytes",
        new Object[]{streamId, endOfStream, data.readableBytes(), padding});
    // Ignores response for the upgrade request
    if (streamId == Http2CodecUtil.HTTP_UPGRADE_STREAM_ID)
    {
      return data.readableBytes() + padding;
    }

    final TimeoutBufferedWriter writer = _connection.stream(streamId).getProperty(_writerKey);
    if (writer == null)
    {
      throw new IllegalStateException("No writer is associated with current stream ID " + streamId);
    }
    writer.onDataRead(data, endOfStream);
    if (endOfStream)
    {
      _connection.stream(streamId).removeProperty(_writerKey);
    }
    return padding;
  }

  @Override
  public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception
  {
    LOG.debug("Received HTTP/2 RST_STREAM frame, stream={}, error={}", streamId, Http2Error.valueOf(errorCode));
  }

  @Override
  public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) throws Http2Exception
  {
    LOG.debug("Received HTTP/2 SETTINGS frame, settings={}", settings);
    ctx.fireUserEventTriggered(FrameEvent.SETTINGS_FRAME_RECEIVED);
  }

  @Override
  public void onSettingsAckRead(ChannelHandlerContext ctx) throws Http2Exception
  {
    LOG.debug("Received HTTP/2 SETTINGS_ACK frame");
  }

  @Override
  public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) throws Http2Exception {
    LOG.debug("Received HTTP/2 WINDOW_UPDATE frame, stream={}, increment={}", streamId, windowSizeIncrement);
  }

  /**
   * A buffered writer that stops reading from socket if buffered bytes is larger than high water mark
   * and resumes reading from socket if buffered bytes is smaller than low water mark.
   */
  class TimeoutBufferedWriter implements Writer
  {
    private final ChannelHandlerContext _ctx;
    private final int _streamId;
    private final long _maxContentLength;
    private final TimeoutAsyncPoolHandle<?> _poolHandle;
    private WriteHandle _wh;
    private boolean _lastChunkReceived;
    private int _totalBytesWritten;
    private final Queue<ByteString> _buffer;
    private final Timeout<None> _timeout;
    private volatile Throwable _failureBeforeInit;

    TimeoutBufferedWriter(final ChannelHandlerContext ctx, int streamId, long maxContentLength,
        TimeoutAsyncPoolHandle<?> poolHandle)
    {
      _ctx = ctx;
      _streamId = streamId;
      _maxContentLength = maxContentLength;
      _poolHandle = poolHandle;
      _failureBeforeInit = null;
      _lastChunkReceived = false;
      _totalBytesWritten = 0;
      _buffer = new LinkedList<>();

      // schedule a timeout to set the stream and inform use
      _timeout = new Timeout<>(_scheduler, _streamingTimeout, TimeUnit.MILLISECONDS, None.none());
      _timeout.addTimeoutTask(() -> _ctx.executor().execute(()
          -> {
        final Exception cause = new TimeoutException("Timeout while receiving the response entity.");
        doFail(cause);
      }));
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
        doFail(_failureBeforeInit);
        return;
      }

      if (_ctx.executor().inEventLoop())
      {
        doWrite();
      }
      else
      {
        _ctx.executor().execute(this::doWrite);
      }
    }

    @Override
    public void onAbort(Throwable ex)
    {
      doReset();

      // Signals Http2ChannelPoolHandler to return channel back to the async pool
      _ctx.fireChannelRead(_poolHandle);
    }

    public void onDataRead(ByteBuf data, boolean end) throws TooLongFrameException
    {
      if (data.readableBytes() + _totalBytesWritten > _maxContentLength)
      {
        doFail(new TooLongFrameException("HTTP content length exceeded " + _maxContentLength + " bytes."));
      }
      else
      {
        if (data.isReadable())
        {
          final InputStream is = new ByteBufInputStream(data);
          final ByteString bytes;
          try
          {
            bytes = ByteString.read(is, data.readableBytes());
          }
          catch (IOException ex)
          {
            doFail(ex);
            return;
          }
          _buffer.add(bytes);
        }
        if (end)
        {
          _lastChunkReceived = true;
        }
        if (_wh != null)
        {
          doWrite();
        }
      }
    }

    private void doFail(Throwable cause)
    {
      doReset();

      if (_wh != null)
      {
        _wh.error(new RemoteInvocationException(cause));
      }
      else
      {
        _failureBeforeInit = cause;
      }

      // Signals Http2ChannelPoolHandler to return channel back to the async pool
      _ctx.fireChannelRead(_poolHandle.error());
    }

    private void doReset()
    {
      // Cancels streaming timeout
      _timeout.getItem();
      // Resets and closes the stream
      _lifecycleManager.resetStream(_ctx, _streamId, Http2Error.CANCEL.code(), _ctx.newPromise());
    }

    private void doWrite()
    {
      while(_wh.remaining() > 0)
      {
        if (!_buffer.isEmpty())
        {
          final ByteString bytes = _buffer.poll();
          _wh.write(bytes);
          _totalBytesWritten += bytes.length();
          try
          {
            Http2Stream stream = _connection.stream(_streamId);
            _connection.local().flowController().consumeBytes(stream, bytes.length());
          }
          catch (Http2Exception e)
          {
            doFail(e);
            return;
          }
          finally
          {
            _ctx.flush();
          }
        }
        else
        {
          if (_lastChunkReceived)
          {
            // Signals Http2ChannelPoolHandler to return channel back to the async pool
            _ctx.fireChannelRead(_poolHandle);
            _wh.done();
            _timeout.getItem();
          }
          break;
        }
      }
    }
  }
}

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
import com.linkedin.pegasus.io.netty.buffer.ByteBuf;
import com.linkedin.pegasus.io.netty.buffer.ByteBufInputStream;
import com.linkedin.pegasus.io.netty.channel.ChannelHandlerContext;
import com.linkedin.pegasus.io.netty.handler.codec.TooLongFrameException;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2CodecUtil;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2Connection;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2Error;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2EventAdapter;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2Exception;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2Headers;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2LifecycleManager;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2Settings;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2Stream;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.r2.netty.handler.http2.Http2MessageDecoders;
import com.linkedin.r2.transport.common.bridge.common.ResponseWithCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.http.client.TimeoutAsyncPoolHandle;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Listens to HTTP/2 frames and assembles {@link com.linkedin.r2.message.stream.StreamRequest}
 * and its {@link com.linkedin.r2.message.stream.entitystream.EntityStream}. Http/2 stream level
 * errors should cause only the stream to be reset, not the entire connection. As a result, errors
 * specific to a stream should not result in throwing non HTTP/2 stream exceptions in this codec.
 */
class Http2FrameListener extends Http2EventAdapter
{
  public enum FrameEvent
  {
    /**
     * An event indicating both SETTING and SETTING_ACK are received.
     */
    SETTINGS_COMPLETE
  }

  private static final Logger LOG = LoggerFactory.getLogger(Http2FrameListener.class);

  private final Http2Connection _connection;
  private final Http2Connection.PropertyKey _writerKey;
  private final Http2LifecycleManager _lifecycleManager;
  private final long _maxContentLength;
  private final int _connectionWindowSizeDelta;

  private boolean _settingsReceived = false;
  private boolean _settingsAckReceived = false;
  private boolean _settingsCompleteEventFired = false;

  public Http2FrameListener(Http2Connection connection, Http2LifecycleManager lifecycleManager, long maxContentLength,
      int initialConnectionWindowSize)
  {
    if (initialConnectionWindowSize < Http2CodecUtil.DEFAULT_WINDOW_SIZE)
    {
      throw new IllegalArgumentException("Initial connection window size should be greater than or equal"
          + " to the default window size " + Http2CodecUtil.DEFAULT_WINDOW_SIZE);
    }

    _connection = connection;
    _writerKey = connection.newKey();
    _lifecycleManager = lifecycleManager;
    _maxContentLength = maxContentLength;
    _connectionWindowSizeDelta = initialConnectionWindowSize - Http2CodecUtil.DEFAULT_WINDOW_SIZE;
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

    // Refactored duplicate code to new code pipeline.
    final StreamResponseBuilder builder = Http2MessageDecoders.ResponseDecoder.buildStreamResponse(headers);

    // Gets async pool handle from stream properties
    TimeoutAsyncPoolHandle<?> timeoutHandle =
        Http2PipelinePropertyUtil.remove(ctx, _connection, streamId, Http2ClientPipelineInitializer.CHANNEL_POOL_HANDLE_ATTR_KEY);

    if (timeoutHandle == null)
    {
      _lifecycleManager.onError(ctx, false, Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR,
          "No channel pool handle is associated with this stream", streamId));
      return;
    }

    final StreamResponse response;
    if (endOfStream)
    {
      response = builder.build(EntityStreams.emptyStream());

      // Release the handle to put the channel back to the pool
      timeoutHandle.release();
    }
    else
    {
      // Associate an entity stream writer to the HTTP/2 stream
      final TimeoutBufferedWriter writer = new TimeoutBufferedWriter(ctx, streamId, _maxContentLength, timeoutHandle);
      if (_connection.stream(streamId).setProperty(_writerKey, writer) != null)
      {
        _lifecycleManager.onError(ctx, false, Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR,
            "Another writer has already been associated with current stream ID", streamId));
        return;
      }

      // Prepares StreamResponse for the channel pipeline
      EntityStream entityStream = EntityStreams.newEntityStream(writer);
      response = builder.build(entityStream);
    }

    // Gets callback from stream properties
    TransportCallback<?> callback =
        Http2PipelinePropertyUtil.remove(ctx, _connection, streamId, Http2ClientPipelineInitializer.CALLBACK_ATTR_KEY);
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

    // Increase the connection flow control window size by sending the delta as a window update
    _connection.local().flowController().incrementWindowSize(_connection.connectionStream(), _connectionWindowSizeDelta);

    _settingsReceived = true;
    checkAndTriggerSettingsCompleteEvent(ctx);
  }

  @Override
  public void onSettingsAckRead(ChannelHandlerContext ctx) throws Http2Exception
  {
    LOG.debug("Received HTTP/2 SETTINGS_ACK frame");
    _settingsAckReceived = true;
    checkAndTriggerSettingsCompleteEvent(ctx);
  }

  /**
   * Checks if conditions are met for triggering the SETTINGS_COMPLETE event.
   *
   * @param ctx
   */
  private void checkAndTriggerSettingsCompleteEvent(ChannelHandlerContext ctx)
  {
    // Ensures SETTINGS_COMPLETE event is fired at most once
    if (_settingsReceived && _settingsAckReceived && !_settingsCompleteEventFired)
    {
      ctx.fireUserEventTriggered(FrameEvent.SETTINGS_COMPLETE);
      _settingsCompleteEventFired = true;
    }
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
    private final TimeoutAsyncPoolHandle<?> _timeoutPoolHandle;
    private WriteHandle _wh;
    private boolean _lastChunkReceived;
    private long _totalBytesWritten;
    private final Queue<ByteString> _buffer;
    private volatile Throwable _failureBeforeInit;

    TimeoutBufferedWriter(final ChannelHandlerContext ctx, int streamId, long maxContentLength,
                          TimeoutAsyncPoolHandle<?> timeoutPoolHandle)
    {
      _ctx = ctx;
      _streamId = streamId;
      _maxContentLength = maxContentLength;
      _timeoutPoolHandle = timeoutPoolHandle;
      _failureBeforeInit = null;
      _lastChunkReceived = false;
      _totalBytesWritten = 0;
      _buffer = new LinkedList<>();

      // schedule a timeout to set the stream and inform use
      _timeoutPoolHandle.addTimeoutTask(() -> _ctx.executor().execute(() -> {
        final String message = String.format(
            "Timeout while receiving the response entity, stream=%d, remote=%s",
            streamId, ctx.channel().remoteAddress());
        doResetAndNotify(new TimeoutException(message));
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
        doResetAndNotify(_failureBeforeInit);
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
    }

    public void onDataRead(ByteBuf data, boolean end) throws TooLongFrameException
    {
      if (data.readableBytes() + _totalBytesWritten > _maxContentLength)
      {
        doResetAndNotify(new TooLongFrameException("HTTP content length exceeded " + _maxContentLength + " bytes."));
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
            doResetAndNotify(ex);
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

    private void doResetAndNotify(Throwable cause)
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
    }

    private void doReset()
    {
      // Resets and closes the stream
      _lifecycleManager.resetStream(_ctx, _streamId, Http2Error.CANCEL.code(), _ctx.newPromise());
      _ctx.flush();

      // Releases the handle to put the channel back to the pool
      _timeoutPoolHandle.release();
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
            doResetAndNotify(e);
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
            _wh.done();

            // Release the handle to put the channel back to the pool
            _timeoutPoolHandle.release();
          }
          break;
        }
      }
    }
  }
}

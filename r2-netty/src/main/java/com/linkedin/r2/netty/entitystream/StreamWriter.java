/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.r2.netty.entitystream;

import com.linkedin.data.ByteString;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.r2.netty.common.ChannelPipelineEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http2.Http2StreamChannel;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entity stream {@link Writer} implementation that receives data from the Netty pipeline
 * and writes to the entity stream.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public class StreamWriter extends ChannelInboundHandlerAdapter implements Writer
{
  private static final Logger LOG = LoggerFactory.getLogger(StreamWriter.class);
  /**
   * The static instance of {@link ByteString} represents the end-of-file of the writer.
   */
  public static final ByteString EOF = ByteString.copy(new byte[0]);

  /**
   * Maximum number of bytes buffered before disabling {@link Channel}'s auto read.
   */
  private static final int BUFFER_HIGH_WATER_MARK = 3 * R2Constants.DEFAULT_DATA_CHUNK_SIZE;

  /**
   * Minimum number of bytes buffered before re-enabling {@link Channel}'s auto read.
   */
  private static final int BUFFER_LOW_WATER_MARK = R2Constants.DEFAULT_DATA_CHUNK_SIZE;

  private final ChannelHandlerContext _ctx;
  private final List<ByteString> _buffer = new LinkedList<>();
  private final long _maxContentLength;

  private long _totalBytesWritten = 0L;
  private int _bufferedBytes = 0;
  private boolean _errorRaised = false;

  private volatile WriteHandle _wh;
  private volatile Throwable _failureBeforeInit;

  public StreamWriter(ChannelHandlerContext ctx, long maxContentLength)
  {
    _ctx = ctx;
    _maxContentLength = maxContentLength;
  }

  /**
   * Notifies the writer that bytes are available from the {@link ChannelPipeline}.
   * @param data Available bytes from the channel pipeline.
   */
  public void onDataAvailable(ByteString data)
  {
    if (data.length() + _totalBytesWritten > _maxContentLength)
    {
      onError(new TooLongFrameException("HTTP content length exceeded " + _maxContentLength + " bytes."));
      return;
    }

    _totalBytesWritten += data.length();

    _buffer.add(data);
    _bufferedBytes += data.length();

    if (_bufferedBytes > BUFFER_HIGH_WATER_MARK && _ctx.channel().config().isAutoRead())
    {
      _ctx.channel().config().setAutoRead(false);
    }

    if (_wh != null)
    {
      doWrite();
    }
  }

  /**
   * Notifies the writer that a {@link ChannelPipeline} error is encountered. Only the first invocation
   * is raised and the subsequent invocations are ignored.
   *
   * @param throwable error encountered by the channel pipeline.
   */
  public void onError(Throwable throwable)
  {
    if (_wh == null)
    {
      _failureBeforeInit = throwable;
    }
    else
    {
      if (!_errorRaised)
      {
        _wh.error(new RemoteInvocationException(throwable));
        _errorRaised = true;
      }
    }
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
      onError(_failureBeforeInit);
      return;
    }

    // Ensure #doWrite is invoked asynchronously by the event loop thread
    // instead of the caller thread to prevent stack overflow
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
  public void onAbort(Throwable throwable)
  {
    LOG.error("onAbort: " + throwable.toString());
    _ctx.fireExceptionCaught(throwable);
  }

  /**
   * Attempts to write to the entity stream remaining chunks are available. Method must be executed
   * by the {@link ChannelHandlerContext}'s executor.
   */
  private void doWrite()
  {
    while (_wh.remaining() > 0)
    {
      if (_buffer.isEmpty())
      {
        break;
      }

      ByteString data = _buffer.remove(0);
      if (data == EOF)
      {
        _wh.done();
        _ctx.fireUserEventTriggered(ChannelPipelineEvent.RESPONSE_COMPLETE);
        return;
      }

      _wh.write(data);
      _bufferedBytes -= data.length();
      if (!_ctx.channel().config().isAutoRead() && _bufferedBytes < BUFFER_LOW_WATER_MARK)
      {
        _ctx.channel().config().setAutoRead(true);
      }
    }
  }
}

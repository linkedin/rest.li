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

package com.linkedin.r2.netty.entitystream;

import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import io.netty.channel.ChannelHandlerContext;

/**
 * Entity stream {@link Reader} implementation that reads from the entity stream
 * and writes to the Netty pipeline.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public class StreamReader implements Reader
{
  public static final ByteString EOF = ByteString.copy(new byte[0]);

  /**
   * Number of data chunks to request after the current one is flushed. Specifying
   * a value of one indicates a steady uniform stream, while a value greater than
   * one indicates an accelerated stream.
   */
  private static final int REQUEST_CHUNKS = 1;

  private static final int MAX_BUFFERED_CHUNKS = 8;

  /**
   * This threshold is to mitigate the effect of the inter-play of Nagle's algorithm
   * & Delayed ACK when sending requests with small entity.
   */
  private static final int FLUSH_THRESHOLD = R2Constants.DEFAULT_DATA_CHUNK_SIZE;

  private final ChannelHandlerContext _ctx;

  private int _notFlushedBytes;
  private int _notFlushedChunks;

  private volatile ReadHandle _rh;

  public StreamReader(ChannelHandlerContext ctx)
  {
    _ctx = ctx;
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _rh = rh;
    _rh.request(MAX_BUFFERED_CHUNKS);
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    // Additional chunks will not be requested until flush() is called and the data is actually written to socket
    _ctx.write(data).addListener(future -> _rh.request(REQUEST_CHUNKS));

    _notFlushedBytes += data.length();
    _notFlushedChunks++;
    if (_notFlushedBytes >= FLUSH_THRESHOLD || _notFlushedChunks == MAX_BUFFERED_CHUNKS)
    {
      _ctx.flush();
      _notFlushedBytes = 0;
      _notFlushedChunks = 0;
    }
  }

  @Override
  public void onDone()
  {
    _ctx.writeAndFlush(EOF);
  }

  @Override
  public void onError(Throwable e)
  {
    _ctx.fireExceptionCaught(e);
  }
}

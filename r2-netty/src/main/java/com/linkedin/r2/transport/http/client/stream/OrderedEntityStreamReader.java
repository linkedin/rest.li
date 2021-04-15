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

package com.linkedin.r2.transport.http.client.stream;

import com.linkedin.data.ByteString;
import com.linkedin.pegasus.io.netty.channel.ChannelHandlerContext;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;


/**
 * A Reader wrapper which ensures the reader callbacks are executed in the order they called by the writer
 * wrapped reader callbacks are queued to be invoked by the dedicated single threaded {@link io.netty.channel.EventLoop}
 *
 * @author Nizar Mankulangara
 */
public class OrderedEntityStreamReader implements Reader
{
  private final ChannelHandlerContext _ctx;
  private final Reader _reader;
  private ReadHandle _rh;

  /**
   * Construct a new instance.
   *
   * @param ctx the {@link ChannelHandlerContext} to retrieve the right {@link io.netty.channel.EventLoop} Executor
   * @param reader the underlying {@link Reader} whose callbacks execution needs to be ordered
   */
  public OrderedEntityStreamReader(ChannelHandlerContext ctx, Reader reader)
  {
    _ctx = ctx;
    _reader = reader;
  }

  private void addToEventLoop(Runnable r)
  {
    _ctx.executor().execute(r);
  }

  @Override
  public void onInit(ReadHandle rh)
  {
    _rh = rh;
    addToEventLoop(()->_reader.onInit(rh));
  }

  @Override
  public void onDataAvailable(ByteString data)
  {
    addToEventLoop(()->_reader.onDataAvailable(data));
  }

  @Override
  public void onDone()
  {
    addToEventLoop(_reader::onDone);
  }

  @Override
  public void onError(Throwable e)
  {
    addToEventLoop(()->_reader.onError(e));
  }

  public void request(int maximumChunks)
  {
    _rh.request(maximumChunks);
  }
}

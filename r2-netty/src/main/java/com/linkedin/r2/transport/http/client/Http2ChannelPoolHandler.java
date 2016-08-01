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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


/**
 * Handles {@link AsyncPoolHandle} and releases the wrapped channel back to the {@link AsyncPool}.
 *
 * This handler does not call super because it expects to be the last handler in the pipeline,
 * to ensure that every other handler has had a chance to process the event and finish with
 * the channel.
 *
 * The handler operates as a singleton (it can be a member of multiple pipelines). It expectsHttp2ClientPipelineInitializer
 * that the channel's attachment will be an AsyncPool&lt;Channel&gt; to which the channel belongs.
 */
@ChannelHandler.Sharable
class Http2ChannelPoolHandler extends ChannelInboundHandlerAdapter
{
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    if (msg instanceof AsyncPoolHandle)
    {
      // Returns channel back to the async pool
      AsyncPoolHandle<?> handle = (AsyncPoolHandle<?>)msg;
      handle.release();
    }
  }
}

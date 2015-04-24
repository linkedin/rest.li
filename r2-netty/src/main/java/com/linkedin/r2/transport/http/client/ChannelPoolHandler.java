/*
   Copyright (c) 2012 LinkedIn Corp.

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


import com.linkedin.r2.message.rest.RestResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

import java.util.List;


/**
 * Listens for upstream events affecting the state of the channel as it relates to the pool.
 * This handler does not call super because it expects to be the last handler in the pipeline,
 * to ensure that every other handler has had a chance to process the event and finish with
 * the channel.
 *
 * Basically, the handler's job is to return the channel to the pool, or ask the pool to
 * dispose of the channel, after the response is received or after an error occurs.
 *
 * The handler operates as a singleton (it can be a member of multiple pipelines). It expects
 * that the channel's attachment will be an AsyncPool&lt;Channel&gt; to which the channel belongs.
 */
@ChannelHandler.Sharable
class ChannelPoolHandler extends ChannelInboundHandlerAdapter
{
  public static final AttributeKey<AsyncPool<Channel>> CHANNEL_POOL_ATTR_KEY
      = AttributeKey.valueOf("ChannelPool");

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    AsyncPool<Channel> pool = ctx.channel().attr(CHANNEL_POOL_ATTR_KEY).getAndRemove();
    if (pool != null)
    {
      RestResponse restResponse = (RestResponse) msg;
      List<String> connectionTokens = restResponse.getHeaderValues("connection");
      if (connectionTokens != null)
      {
        for (String token: connectionTokens)
        {
          if ("close".equalsIgnoreCase(token))
          {
            pool.dispose(ctx.channel());
            return;
          }
        }
      }
      pool.put(ctx.channel());
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    AsyncPool<Channel> pool = ctx.channel().attr(CHANNEL_POOL_ATTR_KEY).getAndRemove();
    if (pool != null)
    {
      // TODO do all exceptions mean we should get rid of the channel?
      pool.dispose(ctx.channel());
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception
  {
    AsyncPool<Channel> pool = ctx.channel().attr(CHANNEL_POOL_ATTR_KEY).getAndRemove();
    if (pool != null)
    {
      pool.dispose(ctx.channel());
    }
  }
}

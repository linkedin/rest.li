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

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

/**
 * Listens for upstream events affecting the state of the channel as it relates to the pool.
 * This handler does not call super because it expects to be the last handler in the pipeline,
 * to ensure that every other handler has had a chance to process the event and finish with
 * the channel.
 *
 * Basically, the handler's job is to return the channel to the pool, or ask the pool to
 * dispose of the channel, after the response is received or after an error occurs.
 *
 * The handler operates as a singleton (it can be a member of multiple pipelines).  The handler
 * expects that its {@link ChannelHandlerContext} will be an object of type
 * AsyncPool&lt;Channel&gt;.
 */
class ChannelPoolHandler extends UpstreamHandlerWithAttachment<AsyncPool<Channel>>
{
  /**
   * Construct a new instance.
   */
  public ChannelPoolHandler()
  {
  }

  @Override
  public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception
  {
    AsyncPool<Channel> pool = removeAttachment(ctx);
    if (pool != null)
    {
      pool.put(e.getChannel());
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
  {
    AsyncPool<Channel> pool = removeAttachment(ctx);
    if (pool != null)
    {
      // TODO do all exceptions mean we should get rid of the channel?
      pool.dispose(e.getChannel());
    }
  }

  @Override
  public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
  {
    AsyncPool<Channel> pool = removeAttachment(ctx);
    if (pool != null)
    {
      pool.dispose(e.getChannel());
    }
  }
}

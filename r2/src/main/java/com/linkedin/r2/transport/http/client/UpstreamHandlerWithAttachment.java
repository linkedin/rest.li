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

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */
public class UpstreamHandlerWithAttachment<T> extends SimpleChannelUpstreamHandler
{
  /**
   * Remove any attachment from the {@link ChannelHandlerContext}.
   *
   * @param ctx the {@link ChannelHandlerContext} for the attachment.
   * @return the attachment which was removed, or null if no attachment exists.
   */
  public T removeAttachment(ChannelHandlerContext ctx)
  {
    @SuppressWarnings("unchecked")
    T attachment = (T)ctx.getAttachment();
    if (attachment != null)
    {
      ctx.setAttachment(null);
    }
    return attachment;
  }
}

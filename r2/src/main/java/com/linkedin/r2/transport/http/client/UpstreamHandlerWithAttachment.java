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

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */
public class UpstreamHandlerWithAttachment<T> extends SimpleChannelUpstreamHandler
{
  /**
   * Set attachment to the {@link ChannelHandlerContext}. We wrap the actual attachment
   * with an {@link AtomicReference} to make the subsequent removeAttachment() an atomic
   * operation.
   *
   * @param ctx the {@link ChannelHandlerContext} for the attachment.
   * @param attachment the attachment to set
   */
  public void setAttachment(ChannelHandlerContext ctx, T attachment)
  {
    AtomicReference<T> atomicRef = new AtomicReference<T>(attachment);
    ctx.setAttachment(atomicRef);
  }

  /**
   * Remove any attachment from the {@link ChannelHandlerContext}. It is safe to call this
   * method in multiple threads since the actual attachment can be returned only once.
   *
   * @param ctx the {@link ChannelHandlerContext} for the attachment.
   * @return the attachment which was removed, or null if no attachment exists.
   */
  public T removeAttachment(ChannelHandlerContext ctx)
  {
    @SuppressWarnings("unchecked")
    AtomicReference<T> atomicRef = (AtomicReference<T>)ctx.getAttachment();
    T attachment = null;
    if (atomicRef != null)
    {
      attachment = atomicRef.getAndSet(null);
    }
    return attachment;
  }
}

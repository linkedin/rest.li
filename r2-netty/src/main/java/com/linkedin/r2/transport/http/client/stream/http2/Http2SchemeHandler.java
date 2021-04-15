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

package com.linkedin.r2.transport.http.client.stream.http2;

import com.linkedin.pegasus.io.netty.channel.ChannelHandlerContext;
import com.linkedin.pegasus.io.netty.channel.ChannelOutboundHandlerAdapter;
import com.linkedin.pegasus.io.netty.channel.ChannelPromise;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.transport.common.bridge.common.RequestWithCallback;
import java.net.URI;


/**
 * A handler that enforces the scheme of every request. Throws {@link java.lang.IllegalStateException}
 * if the scheme of incoming request does not comply with the desired one in the handler.
 */
class Http2SchemeHandler extends ChannelOutboundHandlerAdapter
{
  private final String _scheme;

  public Http2SchemeHandler(String scheme)
  {
    _scheme = scheme;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    if (!(msg instanceof RequestWithCallback))
    {
      ctx.write(msg, promise);
      return;
    }

    Request request = ((RequestWithCallback)msg).request();
    URI uri = request.getURI();
    String scheme = uri.getScheme();

    if (!scheme.equalsIgnoreCase(_scheme))
    {
      // Specified scheme does not match the existing scheme for the pipeline. Returns channel back to the pool
      // and throws exception to the caller.
      ((RequestWithCallback)msg).handle().release();
      throw new IllegalStateException(
          String.format("Cannot switch scheme from %s to %s for %s", _scheme, scheme, ctx.channel().remoteAddress()));
    }

    ctx.write(msg, promise);
  }
}

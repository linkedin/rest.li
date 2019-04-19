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

package com.linkedin.r2.netty.handler.common;

import com.linkedin.r2.message.stream.StreamRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.net.URI;

/**
 * A handler that enforces the scheme of every request. Fires {@link IllegalStateException}
 * if the scheme of incoming request does not comply with the desired one in the handler.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public class SchemeHandler extends ChannelOutboundHandlerAdapter
{
  private final String _scheme;

  public SchemeHandler(String scheme)
  {
    _scheme = scheme;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
  {
    if (msg instanceof StreamRequest)
    {
      StreamRequest request = (StreamRequest) msg;
      URI uri = request.getURI();
      String scheme = uri.getScheme();

      if (!scheme.equalsIgnoreCase(_scheme))
      {
        // Specified scheme does not match the existing scheme for the pipeline. Returns channel back to the pool
        // and throws exception to the caller.
        ctx.fireExceptionCaught(new IllegalStateException(String.format(
            "Cannot switch scheme from %s to %s, remote=%s", _scheme, scheme, ctx.channel().remoteAddress())));
        return;
      }
    }

    ctx.write(msg, promise);
  }
}

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

package com.linkedin.r2.netty.handler.http2;

import com.linkedin.pegasus.io.netty.channel.ChannelHandlerContext;
import com.linkedin.pegasus.io.netty.channel.ChannelPromise;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2MultiplexHandler;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2Settings;
import com.linkedin.pegasus.io.netty.handler.ssl.ApplicationProtocolNames;
import com.linkedin.pegasus.io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import com.linkedin.pegasus.io.netty.handler.ssl.SslHandler;
import java.nio.channels.ClosedChannelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty handler to configure a {@link io.netty.channel.ChannelPipeline} upon successful ALPN
 *  to H2 by {@link SslHandler}. If the ALPN is not resulted in H2 - the ALPN promise is marked is failed and
 *  that will notify the ALPN promise listener present in channel lifecycle.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public class Http2AlpnHandler extends ApplicationProtocolNegotiationHandler
{
  private static Logger LOG = LoggerFactory.getLogger(Http2AlpnHandler.class);

  private final ChannelPromise _alpnPromise;
  private final Http2Settings _http2Settings;

  /**
   * @param alpnPromise - The {@link ChannelPromise} created to track the status of ALPN. This handler {@link Http2AlpnHandler}
   *                    is not responsible for setting up the required call backs. This is expected to be setup by the
   *                    pipeline bootstrap code. This handler is only responsible for marking success or failure in
   *                    ALPN stage.
   * @param http2Settings - Http2 settings
   */
  public Http2AlpnHandler(ChannelPromise alpnPromise, Http2Settings http2Settings)
  {
    super(ApplicationProtocolNames.HTTP_1_1);
    _alpnPromise = alpnPromise;
    _http2Settings = http2Settings;
  }

  @Override
  protected void configurePipeline(ChannelHandlerContext ctx, String protocol)
  {
    switch (protocol)
    {
      case ApplicationProtocolNames.HTTP_2:
        ctx.pipeline().addLast(Http2FrameCodecBuilder
            .forClient()
            .initialSettings(_http2Settings)
            .build());
        ctx.pipeline().addLast(new Http2MultiplexHandler(new UnsupportedHandler()));
        _alpnPromise.setSuccess();
        break;
      default:
        _alpnPromise.setFailure(new IllegalStateException("Unsupported protocol '" + protocol + "' is negotiated."));
    }
  }

  @Override
  protected void handshakeFailure(ChannelHandlerContext ctx, Throwable cause)
  {
    trySetAlpnFailure(cause);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx)
  {
    LOG.error("******** Http2AlpnHandler inactive " + ctx.channel() + " ********");
    trySetAlpnFailure(new ClosedChannelException());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
  {
    trySetAlpnFailure(cause);
  }

  private void trySetAlpnFailure(Throwable cause)
  {
    if (!_alpnPromise.isDone())
    {
      _alpnPromise.setFailure(new IllegalStateException("HTTP/2 ALPN failed", cause));
    }
  }
}

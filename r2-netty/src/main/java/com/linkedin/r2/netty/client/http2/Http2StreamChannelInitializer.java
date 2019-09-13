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

package com.linkedin.r2.netty.client.http2;

import com.linkedin.r2.netty.handler.common.CancelTimeoutHandler;
import com.linkedin.r2.netty.handler.common.ChannelLifecycleHandler;
import com.linkedin.r2.netty.handler.common.ClientEntityStreamHandler;
import com.linkedin.r2.netty.handler.common.SchemeHandler;
import com.linkedin.r2.netty.handler.http2.Http2MessageDecoders;
import com.linkedin.r2.netty.handler.http2.Http2MessageEncoders;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpScheme;

/**
 * Netty handler to setup the Http2 Stream Channel pipeline
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
class Http2StreamChannelInitializer extends ChannelInitializer<Channel>
{
  /**
   * HTTP/2 stream channels are not recyclable and should be disposed upon completion.
   */
  private static final boolean CHANNEL_RECYCLE = false;

  private final boolean _ssl;
  private final long _maxContentLength;

  public Http2StreamChannelInitializer(boolean ssl, long maxContentLength)
  {
    _ssl = ssl;
    _maxContentLength = maxContentLength;
  }

  @Override
  protected void initChannel(Channel channel)
  {
    channel.pipeline().addLast("outboundRestRequestEncoder", Http2MessageEncoders.newRestRequestEncoder());
    channel.pipeline().addLast("outboundStreamDataEncoder", Http2MessageEncoders.newDataEncoder());
    channel.pipeline().addLast("outboundStreamRequestEncoder", Http2MessageEncoders.newStreamRequestEncoder());
    channel.pipeline().addLast("inboundDataDecoder", Http2MessageDecoders.newDataDecoder());
    channel.pipeline().addLast("inboundRequestDecoder", Http2MessageDecoders.newResponseDecoder());
    channel.pipeline().addLast("schemeHandler", new SchemeHandler(_ssl ? HttpScheme.HTTPS.toString() : HttpScheme.HTTP.toString()));
    channel.pipeline().addLast("streamDuplexHandler", new ClientEntityStreamHandler(_maxContentLength));
    channel.pipeline().addLast("timeoutHandler", new CancelTimeoutHandler());
    channel.pipeline().addLast("channelPoolHandler", new ChannelLifecycleHandler(CHANNEL_RECYCLE));
  }
}

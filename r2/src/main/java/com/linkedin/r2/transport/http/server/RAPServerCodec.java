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

package com.linkedin.r2.transport.http.server;


import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.http.common.HttpConstants;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.net.URI;
import java.util.List;
import java.util.Map;


/**
* @author Steven Ihde
* @version $Revision: $
*/
class RAPServerCodec extends ChannelDuplexHandler
{
  private final RAPResponseEncoder _encoder = new RAPResponseEncoder();
  private final RAPRequestDecoder _decoder = new RAPRequestDecoder();

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    _decoder.channelRead(ctx, msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    _encoder.write(ctx, msg, promise);
  }

  private class RAPRequestDecoder extends MessageToMessageDecoder<FullHttpRequest>
  {
    @Override
    protected void decode(ChannelHandlerContext ctx, FullHttpRequest nettyRequest, List<Object> out)
        throws Exception
    {
      if (nettyRequest.getDecoderResult().isFailure())
      {
        ctx.fireExceptionCaught(nettyRequest.getDecoderResult().cause());
        return;
      }

      URI uri = new URI(nettyRequest.getUri());
      RestRequestBuilder builder = new RestRequestBuilder(uri);
      builder.setMethod(nettyRequest.getMethod().name());
      for (Map.Entry<String, String> e : nettyRequest.headers())
      {
        if (e.getKey().equalsIgnoreCase(HttpConstants.REQUEST_COOKIE_HEADER_NAME))
        {
          builder.addCookie(e.getValue());
        }
        else
        {
          builder.unsafeAddHeaderValue(e.getKey(), e.getValue());
        }
      }
      ByteBuf buf = nettyRequest.content();
      if (buf != null)
      {
        ByteString entity = ByteString.read(new ByteBufInputStream(buf), buf.readableBytes());
        builder.setEntity(entity);
      }
      out.add(builder.build());
    }
  }

  private class RAPResponseEncoder extends MessageToMessageEncoder<RestResponse>
  {
    protected void encode(ChannelHandlerContext ctx, RestResponse response, List<Object> out)
        throws Exception
    {
      final ByteString entity = response.getEntity();
      ByteBuf content = Unpooled.wrappedBuffer(entity.asByteBuffer());

      HttpResponse nettyResponse =
          new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(response.getStatus()), content);

      for (Map.Entry<String, String> e : response.getHeaders().entrySet())
      {
        nettyResponse.headers().set(e.getKey(), e.getValue());
      }

      nettyResponse.headers().set(HttpConstants.RESPONSE_COOKIE_HEADER_NAME, response.getCookies());
      nettyResponse.headers().set(HttpHeaders.Names.CONTENT_LENGTH, entity.length());

      out.add(nettyResponse);
    }
  }
}

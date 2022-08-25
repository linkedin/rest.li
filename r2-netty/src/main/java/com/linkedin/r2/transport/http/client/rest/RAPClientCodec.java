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

package com.linkedin.r2.transport.http.client.rest;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.netty.common.NettyRequestAdapter;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.pegasus.io.netty.buffer.ByteBuf;
import com.linkedin.pegasus.io.netty.buffer.ByteBufInputStream;
import com.linkedin.pegasus.io.netty.buffer.Unpooled;
import com.linkedin.pegasus.io.netty.channel.ChannelDuplexHandler;
import com.linkedin.pegasus.io.netty.channel.ChannelHandlerContext;
import com.linkedin.pegasus.io.netty.channel.ChannelPromise;
import com.linkedin.pegasus.io.netty.handler.codec.MessageToMessageDecoder;
import com.linkedin.pegasus.io.netty.handler.codec.MessageToMessageEncoder;
import com.linkedin.pegasus.io.netty.handler.codec.http.DefaultFullHttpRequest;
import com.linkedin.pegasus.io.netty.handler.codec.http.FullHttpRequest;
import com.linkedin.pegasus.io.netty.handler.codec.http.FullHttpResponse;
import com.linkedin.pegasus.io.netty.handler.codec.http.HttpHeaderNames;
import com.linkedin.pegasus.io.netty.handler.codec.http.HttpMethod;
import com.linkedin.pegasus.io.netty.handler.codec.http.HttpVersion;
import java.net.URL;
import java.util.List;
import java.util.Map;


/**
 * @author Steven Ihde
 * @author Ang Xu
 * @version $Revision: $
 */
class RAPClientCodec extends ChannelDuplexHandler
{
  private final RAPRequestEncoder _encoder = new RAPRequestEncoder();
  private final RAPResponseDecoder _decoder = new RAPResponseDecoder();

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    _decoder.channelRead(ctx, msg);
  }

  @Override
  public void write(final ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    _encoder.write(ctx, msg, promise);
  }

  private class RAPRequestEncoder extends MessageToMessageEncoder<RestRequest>
  {
    @Override
    protected void encode(ChannelHandlerContext ctx, RestRequest request, List<Object> out)
        throws Exception
    {
      HttpMethod nettyMethod = HttpMethod.valueOf(request.getMethod());
      URL url = new URL(request.getURI().toString());
      String path = url.getFile();
      // RFC 2616, section 5.1.2:
      //   Note that the absolute path cannot be empty; if none is present in the original URI,
      //   it MUST be given as "/" (the server root).
      if (path.isEmpty())
      {
        path = "/";
      }
      ByteString entity = request.getEntity();
      ByteBuf content = Unpooled.wrappedBuffer(entity.asByteBuffer());
      FullHttpRequest nettyRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, nettyMethod, path, content);

      NettyRequestAdapter.setHttpHeadersAndCookies(request, url, nettyRequest);

      nettyRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, entity.length());

      out.add(nettyRequest);
    }
  }

  private class RAPResponseDecoder extends MessageToMessageDecoder<FullHttpResponse>
  {
    @Override
    protected void decode(ChannelHandlerContext ctx, FullHttpResponse nettyResponse, List<Object> out)
        throws Exception
    {
      // Weird weird... Netty won't throw up, instead, it'll return a partially decoded response
      // if there is a decoding error.
      if (nettyResponse.decoderResult().isFailure())
      {
        ctx.fireExceptionCaught(nettyResponse.decoderResult().cause());
        return;
      }

      RestResponseBuilder builder = new RestResponseBuilder();
      builder.setStatus(nettyResponse.status().code());

      for (Map.Entry<String, String> e : nettyResponse.headers())
      {
        if (e.getKey().equalsIgnoreCase(HttpConstants.RESPONSE_COOKIE_HEADER_NAME))
        {
          builder.addCookie(e.getValue());
        }
        else
        {
          builder.unsafeAddHeaderValue(e.getKey(), e.getValue());
        }
      }

      ByteBuf buf = nettyResponse.content();
      ByteString entity = ByteString.read(new ByteBufInputStream(buf), buf.readableBytes());
      builder.setEntity(entity);
      /**
       * Note: no need to release the incoming {@link ByteBuf} because {@link MessageToMessageDecoder}
       * automatically does it for us.
       */
      out.add(builder.build());
    }
  }
}

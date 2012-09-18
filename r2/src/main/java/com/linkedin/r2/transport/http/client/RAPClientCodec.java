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

import java.net.URL;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponseBuilder;

/**
* @author Steven Ihde
* @version $Revision: $
*/
class RAPClientCodec implements ChannelUpstreamHandler, ChannelDownstreamHandler
{
  private final RAPRequestEncoder _encoder = new RAPRequestEncoder();
  private final RAPResponseDecoder _decoder = new RAPResponseDecoder();

  @Override
  public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception
  {
    _encoder.handleDownstream(ctx, e);
  }

  @Override
  public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception
  {
    _decoder.handleUpstream(ctx, e);
  }

  private class RAPRequestEncoder extends OneToOneEncoder
  {
    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception
    {
      RestRequest request = (RestRequest) msg;

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

      HttpRequest nettyRequest =
          new DefaultHttpRequest(HttpVersion.HTTP_1_1, nettyMethod, path);

      nettyRequest.setHeader(HttpHeaders.Names.HOST, url.getAuthority());
      for (Map.Entry<String, String> e : request.getHeaders().entrySet())
      {
        nettyRequest.setHeader(e.getKey(), e.getValue());
      }

      final ByteString entity = request.getEntity();
      ChannelBuffer buf = ChannelBuffers.wrappedBuffer(entity.asByteBuffer());
      nettyRequest.setContent(buf);
      nettyRequest.setHeader(HttpHeaders.Names.CONTENT_LENGTH, entity.length());

      return nettyRequest;
    }
  }

  private class RAPResponseDecoder extends OneToOneDecoder
  {
    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception
    {
      HttpResponse nettyResponse = (HttpResponse) msg;

      RestResponseBuilder builder = new RestResponseBuilder();

      HttpResponseStatus status = nettyResponse.getStatus();
      builder.setStatus(status.getCode());

      for (Map.Entry<String, String> e : nettyResponse.getHeaders())
      {
        builder.unsafeAddHeaderValue(e.getKey(), e.getValue());
      }

      ChannelBuffer buf = nettyResponse.getContent();
      byte[] array = new byte[buf.readableBytes()];
      buf.readBytes(array);
      builder.setEntity(array);

      return builder.build();
    }
  }
}

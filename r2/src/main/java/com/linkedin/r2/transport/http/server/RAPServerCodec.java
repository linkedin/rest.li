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

import java.net.URI;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;

/**
* @author Steven Ihde
* @version $Revision: $
*/
class RAPServerCodec implements ChannelUpstreamHandler, ChannelDownstreamHandler
{
  private final RAPResponseEncoder _encoder = new RAPResponseEncoder();
  private final RAPRequestDecoder _decoder = new RAPRequestDecoder();

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

  private class RAPRequestDecoder extends OneToOneDecoder
  {
    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception
    {
      HttpRequest nettyRequest = (HttpRequest) msg;
      URI uri = new URI(nettyRequest.getUri());
      RestRequestBuilder builder = new RestRequestBuilder(uri);
      builder.setMethod(nettyRequest.getMethod().getName());
      for (Map.Entry<String, String> e : nettyRequest.getHeaders())
      {
        builder.unsafeAddHeaderValue(e.getKey(), e.getValue());
      }
      ChannelBuffer buf = nettyRequest.getContent();
      if (buf != null)
      {
        if (buf.hasArray())
        {
          // TODO make a copy?
          builder.setEntity(buf.array());
        }
      }

      return builder.build();
    }
  }

  private class RAPResponseEncoder extends OneToOneEncoder
  {
    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception
    {
      RestResponse response = (RestResponse) msg;

      HttpResponse nettyResponse =
          new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                                  HttpResponseStatus.valueOf(response.getStatus()));

      for (Map.Entry<String, String> e : response.getHeaders().entrySet())
      {
        nettyResponse.setHeader(e.getKey(), e.getValue());
      }
      final ByteString entity = response.getEntity();
      ChannelBuffer buf = ChannelBuffers.wrappedBuffer(entity.asByteBuffer());
      nettyResponse.setContent(buf);
      nettyResponse.setHeader(HttpHeaders.Names.CONTENT_LENGTH, entity.length());

      return nettyResponse;
    }
  }
}

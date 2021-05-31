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

import com.linkedin.data.ByteString;
import com.linkedin.pegasus.io.netty.buffer.ByteBufInputStream;
import com.linkedin.pegasus.io.netty.channel.ChannelHandler;
import com.linkedin.pegasus.io.netty.channel.ChannelHandler.Sharable;
import com.linkedin.pegasus.io.netty.channel.ChannelHandlerContext;
import com.linkedin.pegasus.io.netty.handler.codec.MessageToMessageDecoder;
import com.linkedin.pegasus.io.netty.handler.codec.http.HttpHeaderNames;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2DataFrame;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2Headers;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2HeadersFrame;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.netty.entitystream.StreamWriter;
import com.linkedin.r2.transport.http.common.HttpConstants;
import java.util.List;
import java.util.Map;

/**
 * Inbound {@link ChannelHandler} implementation that decodes {@link Http2HeadersFrame} and
 * {@link Http2DataFrame} into {@link StreamResponseBuilder} and {@link ByteString}.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public class Http2MessageDecoders
{
  public static ResponseDecoder newResponseDecoder()
  {
    return new ResponseDecoder();
  }

  public static DataDecoder newDataDecoder()
  {
    return new DataDecoder();
  }

  @Sharable
  public static class ResponseDecoder extends MessageToMessageDecoder<Http2HeadersFrame>
  {
    private ResponseDecoder()
    {
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Http2HeadersFrame frame, List<Object> out)
    {
      final Http2Headers headers = frame.headers();

      final StreamResponseBuilder builder = buildStreamResponse(headers);

      out.add(builder);
      if (frame.isEndStream())
      {
        out.add(StreamWriter.EOF);
      }
    }

    /**
     * Create a StreamResponseBuilder that has all the http/2 headers and cookies setup in it.
     *
     * @param headers http/2 response headers
     * @return StreamResponseBuilder with all the cookies and headers setup
     */
    public static StreamResponseBuilder buildStreamResponse(Http2Headers headers)
    {
      final StreamResponseBuilder builder = new StreamResponseBuilder();

      processPsuedoHttp2Headers(builder, headers);

      processOtherHttp2HeadersAndCookies(builder, headers);

      return builder;
    }

    /**
     * Add Headers and Cookies to the StreamResponseBuilder.
     * All the Http/2 Pseudo Headers will be ignored
     */
    private static void processOtherHttp2HeadersAndCookies(StreamResponseBuilder builder, Http2Headers headers)
    {
      for (Map.Entry<CharSequence, CharSequence> header : headers)
      {
        if (Http2Headers.PseudoHeaderName.isPseudoHeader(header.getKey()))
        {
          // Do no set HTTP/2 pseudo headers to response
          continue;
        }

        final String key = header.getKey().toString();
        final String value = header.getValue().toString();

        if (key.equalsIgnoreCase(HttpConstants.RESPONSE_COOKIE_HEADER_NAME))
        {
          builder.addCookie(value);
        }
        else
        {
          builder.unsafeAddHeaderValue(key, value);
        }
      }
    }

    /**
     * Update the Status and Host details from Http/2 Psuedo Headers
     */
    private static void processPsuedoHttp2Headers(StreamResponseBuilder builder, Http2Headers headers)
    {
      if (headers.status() != null)
      {
        builder.setStatus(Integer.parseInt(headers.status().toString()));
      }
      if (headers.authority() != null)
      {
        builder.addHeaderValue(HttpHeaderNames.HOST.toString(), headers.authority().toString());
      }
    }
  }

  @Sharable
  public static class DataDecoder extends MessageToMessageDecoder<Http2DataFrame>
  {
    private DataDecoder()
    {
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Http2DataFrame frame, List<Object> out) throws Exception
    {
      if (frame.content().isReadable())
      {
        out.add(ByteString.read(new ByteBufInputStream(frame.content()), frame.content().readableBytes()));
      }
      if (frame.isEndStream())
      {
        out.add(StreamWriter.EOF);
      }
    }
  }
}

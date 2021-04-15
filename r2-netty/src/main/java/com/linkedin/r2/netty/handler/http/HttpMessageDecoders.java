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

package com.linkedin.r2.netty.handler.http;

import com.linkedin.data.ByteString;
import com.linkedin.pegasus.io.netty.buffer.ByteBufInputStream;
import com.linkedin.pegasus.io.netty.channel.ChannelHandler;
import com.linkedin.pegasus.io.netty.channel.ChannelHandler.Sharable;
import com.linkedin.pegasus.io.netty.channel.ChannelHandlerContext;
import com.linkedin.pegasus.io.netty.handler.codec.MessageToMessageDecoder;
import com.linkedin.pegasus.io.netty.handler.codec.http.HttpContent;
import com.linkedin.pegasus.io.netty.handler.codec.http.HttpResponse;
import com.linkedin.pegasus.io.netty.handler.codec.http.HttpUtil;
import com.linkedin.pegasus.io.netty.handler.codec.http.LastHttpContent;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.netty.entitystream.StreamWriter;
import com.linkedin.r2.transport.http.common.HttpConstants;
import java.util.List;
import java.util.Map;

/**
 * Inbound {@link ChannelHandler} implementation that decodes {@link HttpResponse} and {@link HttpContent}
 * into {@link StreamResponseBuilder} and {@link ByteString}.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public class HttpMessageDecoders
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
  public static class ResponseDecoder extends MessageToMessageDecoder<HttpResponse>
  {
    private ResponseDecoder()
    {
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpResponse response, List<Object> out)
    {
      if (!response.decoderResult().isSuccess())
      {
        ctx.fireExceptionCaught(response.decoderResult().cause());
        return;
      }

      // Remove chunked encoding.
      if (HttpUtil.isTransferEncodingChunked(response))
      {
        HttpUtil.setTransferEncodingChunked(response, false);
      }

      out.add(buildStreamResponse(response));
    }

    public static StreamResponseBuilder buildStreamResponse(HttpResponse response)
    {
      StreamResponseBuilder builder = new StreamResponseBuilder();
      builder.setStatus(response.status().code());

      for (Map.Entry<String, String> entry : response.headers())
      {
        String key = entry.getKey();
        String value = entry.getValue();
        if (key.equalsIgnoreCase(HttpConstants.RESPONSE_COOKIE_HEADER_NAME))
        {
          builder.addCookie(value);
        }
        else
        {
          builder.unsafeAddHeaderValue(key, value);
        }
      }

      return builder;
    }
  }

  @Sharable
  public static class DataDecoder extends MessageToMessageDecoder<HttpContent>
  {
    private DataDecoder()
    {
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, HttpContent chunk, List<Object> out) throws Exception
    {
      if (!chunk.decoderResult().isSuccess())
      {
        ctx.fireExceptionCaught(chunk.decoderResult().cause());
      }

      if (chunk.content().isReadable())
      {
        out.add(ByteString.read(new ByteBufInputStream(chunk.content()), chunk.content().readableBytes()));
      }

      if (chunk instanceof LastHttpContent)
      {
        out.add(StreamWriter.EOF);
      }
    }
  }
}

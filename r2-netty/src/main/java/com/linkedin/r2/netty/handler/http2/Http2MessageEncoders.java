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
import com.linkedin.pegasus.io.netty.buffer.ByteBuf;
import com.linkedin.pegasus.io.netty.buffer.Unpooled;
import com.linkedin.pegasus.io.netty.channel.ChannelHandler;
import com.linkedin.pegasus.io.netty.channel.ChannelHandlerContext;
import com.linkedin.pegasus.io.netty.handler.codec.MessageToMessageEncoder;
import com.linkedin.pegasus.io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import com.linkedin.pegasus.io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2DataFrame;
import com.linkedin.pegasus.io.netty.handler.codec.http2.Http2HeadersFrame;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.netty.common.NettyRequestAdapter;
import com.linkedin.r2.netty.entitystream.StreamReader;

import java.util.List;

/**
 * Outbound {@link ChannelHandler} implementations that encodes {@link StreamRequest} and request
 * entity in the form of {@link ByteString} into {@link Http2HeadersFrame} and {@link Http2DataFrame}.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public final class Http2MessageEncoders
{
  private static final boolean END_OF_STREAM = true;

  public static StreamRequestEncoder newStreamRequestEncoder()
  {
    return new StreamRequestEncoder();
  }

  public static RestRequestEncoder newRestRequestEncoder()
  {
    return new RestRequestEncoder();
  }

  public static DataEncoder newDataEncoder()
  {
    return new DataEncoder();
  }

  public static class StreamRequestEncoder extends MessageToMessageEncoder<StreamRequest>
  {
    private StreamRequestEncoder()
    {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, StreamRequest request, List<Object> out) throws Exception
    {
      out.add(new DefaultHttp2HeadersFrame(NettyRequestAdapter.toHttp2Headers(request)));
    }
  }

  public static class RestRequestEncoder extends MessageToMessageEncoder<RestRequest>
  {
    private RestRequestEncoder()
    {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, RestRequest request, List<Object> out) throws Exception
    {
      out.add(new DefaultHttp2HeadersFrame(NettyRequestAdapter.toHttp2Headers(request)));
      ByteBuf content = Unpooled.wrappedBuffer(request.getEntity().asByteBuffer());
      out.add(new DefaultHttp2DataFrame(content, true));
    }
  }

  public static class DataEncoder extends MessageToMessageEncoder<ByteString>
  {
    private DataEncoder()
    {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteString data, List<Object> out)
    {
      if (StreamReader.EOF == data)
      {
        out.add(new DefaultHttp2DataFrame(Unpooled.EMPTY_BUFFER, END_OF_STREAM));
      }
      else
      {
        out.add(new DefaultHttp2DataFrame(Unpooled.wrappedBuffer(data.asByteBuffer())));
      }
    }
  }
}

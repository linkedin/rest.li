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
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.netty.common.NettyRequestAdapter;
import com.linkedin.r2.netty.entitystream.StreamReader;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import java.util.List;

/**
 * Outbound {@link ChannelHandler} implementations that encodes {@link StreamRequest} and request
 * entity in the form of {@link ByteString} into {@link HttpRequest} and {@link HttpContent}.
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
public class HttpMessageEncoders
{
  public static RequestEncoder newRequestEncoder()
  {
    return new RequestEncoder();
  }

  public static DataEncoder newDataEncoder()
  {
    return new DataEncoder();
  }

  public static class RequestEncoder extends MessageToMessageEncoder<StreamRequest>
  {
    private RequestEncoder()
    {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, StreamRequest request, List<Object> out) throws Exception
    {
      out.add(NettyRequestAdapter.toNettyRequest(request));
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
        out.add(LastHttpContent.EMPTY_LAST_CONTENT);
      }
      else
      {
        out.add(new DefaultHttpContent(Unpooled.wrappedBuffer(data.asByteBuffer())));
      }
    }
  }
}

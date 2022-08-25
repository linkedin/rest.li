/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client.stream.http2;

import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.common.ResponseWithCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.pegasus.io.netty.channel.ChannelHandler;
import com.linkedin.pegasus.io.netty.channel.ChannelHandlerContext;
import com.linkedin.pegasus.io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Netty pipeline handler which takes a complete received message and invokes the user-specified callback.
 *
 * Note that an instance of this class needs to be stateless, since a single instance is used in multiple
 * {@link com.linkedin.pegasus.io.netty.channel.ChannelPipeline}s simultaneously. The user specified callback is
 * expected to be pass in through a {@link com.linkedin.r2.transport.common.bridge.common.ResponseWithCallback} as a
 * {@link com.linkedin.r2.transport.http.client.TimeoutTransportCallback}
 *
 * @author Sean Sheng
 */
@ChannelHandler.Sharable
class Http2StreamResponseHandler extends ChannelInboundHandlerAdapter
{
  private static Logger LOG = LoggerFactory.getLogger(Http2StreamResponseHandler.class);

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
  {
    if (msg instanceof ResponseWithCallback)
    {
      @SuppressWarnings("unchecked")
      ResponseWithCallback<StreamResponse, TransportCallback<StreamResponse>> responseWithCallback =
          (ResponseWithCallback<StreamResponse, TransportCallback<StreamResponse>>) msg;
      StreamResponse response = responseWithCallback.response();
      TransportCallback<StreamResponse> callback = responseWithCallback.callback();

      Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      headers.putAll(response.getHeaders());

      Map<String, String> wireAttrs = WireAttributeHelper.removeWireAttributes(headers);
      StreamResponse newResponse = new StreamResponseBuilder(response)
          .unsafeSetHeaders(headers)
          .build(response.getEntityStream());

      LOG.debug("{}: handling a response", ctx.channel().remoteAddress());
      callback.onResponse(TransportResponseImpl.success(newResponse, wireAttrs));
    }

    ctx.fireChannelRead(msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    LOG.error("Pipeline encountered an unexpected exception", cause);
  }
}

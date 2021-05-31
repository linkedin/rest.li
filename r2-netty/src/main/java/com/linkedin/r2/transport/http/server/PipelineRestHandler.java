/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.server;

import com.linkedin.pegasus.io.netty.channel.Channel;
import com.linkedin.pegasus.io.netty.channel.ChannelHandlerContext;
import com.linkedin.pegasus.io.netty.channel.SimpleChannelInboundHandler;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class PipelineRestHandler extends SimpleChannelInboundHandler<RestRequest>
{
  private static final Logger LOG = LoggerFactory.getLogger(PipelineRestHandler.class);
  private final HttpDispatcher _dispatcher;

  PipelineRestHandler(HttpDispatcher dispatcher)
  {
    _dispatcher = dispatcher;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, RestRequest request) throws Exception
  {
    final Channel ch = ctx.channel();
    TransportCallback<RestResponse> writeResponseCallback = new TransportCallback<RestResponse>()
    {
      @Override
      public void onResponse(TransportResponse<RestResponse> response)
      {
        final RestResponseBuilder responseBuilder;
        if (response.hasError())
        {
          // This onError is only getting called in cases where:
          // (1) the exception was thrown by the handleRequest() method, and the upper layer
          // dispatcher did not catch the exception or caught it and passed it here without
          // turning it into a Response, or
          // (2) the HttpBridge-installed callback's onError declined to convert the exception to a
          // response and passed it along to here.
          responseBuilder =
              new RestResponseBuilder(RestStatus.responseForError(RestStatus.INTERNAL_SERVER_ERROR, response.getError()));
        }
        else
        {
          responseBuilder = new RestResponseBuilder(response.getResponse());
        }

        responseBuilder
            .unsafeOverwriteHeaders(WireAttributeHelper.toWireAttributes(response.getWireAttributes()))
            .build();

        ch.writeAndFlush(responseBuilder.build());
      }
    };
    try
    {
      _dispatcher.handleRequest(request, writeResponseCallback);
    }
    catch (Exception ex)
    {
      writeResponseCallback.onResponse(TransportResponseImpl.<RestResponse> error(ex, Collections.<String, String> emptyMap()));
    }
  }
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    LOG.error("Exception caught on channel: " + ctx.channel().remoteAddress(), cause);
    ctx.close();
  }
}

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

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class PipelineStreamHandler extends SimpleChannelInboundHandler<RestRequest>
{
  private static final Logger LOG = LoggerFactory.getLogger(PipelineStreamHandler.class);
  private final HttpDispatcher _dispatcher;

  PipelineStreamHandler(HttpDispatcher dispatcher)
  {
    _dispatcher = dispatcher;
  }

  private void writeError(Channel ch, TransportResponse<StreamResponse> response, Throwable ex)
  {
    RestResponseBuilder responseBuilder =
        new RestResponseBuilder(RestStatus.responseForError(RestStatus.INTERNAL_SERVER_ERROR, ex))
            .unsafeOverwriteHeaders(WireAttributeHelper.toWireAttributes(response.getWireAttributes()));

    ch.writeAndFlush(responseBuilder.build());
  }

  private void writeResponse(Channel ch, TransportResponse<StreamResponse> response,  RestResponse restResponse)
  {
    RestResponseBuilder responseBuilder = restResponse.builder()
        .unsafeOverwriteHeaders(WireAttributeHelper.toWireAttributes(response.getWireAttributes()));

    ch.writeAndFlush(responseBuilder.build());
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, RestRequest request) throws Exception
  {
    final Channel ch = ctx.channel();
    TransportCallback<StreamResponse> writeResponseCallback = new TransportCallback<StreamResponse>()
    {
      @Override
      public void onResponse(final TransportResponse<StreamResponse> response)
      {

        if (response.hasError())
        {
          // This onError is only getting called in cases where:
          // (1) the exception was thrown by the handleRequest() method, and the upper layer
          // dispatcher did not catch the exception or caught it and passed it here without
          // turning it into a Response, or
          // (2) the HttpBridge-installed callback's onError declined to convert the exception to a
          // response and passed it along to here.
          writeError(ch, response, response.getError());
        }
        else
        {
          Messages.toRestResponse(response.getResponse(), new Callback<RestResponse>()
          {
            @Override
            public void onError(Throwable e)
            {
              writeError(ch, response, e);
            }

            @Override
            public void onSuccess(RestResponse result)
            {
              writeResponse(ch, response, result);
            }
          });
        }
      }
    };
    try
    {
      _dispatcher.handleRequest(Messages.toStreamRequest(request), writeResponseCallback);
    }
    catch (Exception ex)
    {
      writeResponseCallback.onResponse(TransportResponseImpl.<StreamResponse> error(ex,
          Collections.<String, String> emptyMap()));
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    LOG.error("Exception caught on channel: " + ctx.channel().remoteAddress(), cause);
    ctx.close();
  }
}

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


import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;

import com.linkedin.r2.util.NamedThreadFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import java.net.InetSocketAddress;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO: Do we still need this?
 *
 * @author Steven Ihde
 * @author Ang Xu
 * @version $Revision: $
 */

/* package private */ class HttpNettyServer implements HttpServer
{
  private static final Logger LOG = LoggerFactory.getLogger(HttpNettyServer.class);

  private final int _port;
  private final int _threadPoolSize;
  private final HttpDispatcher _dispatcher;

  private NioEventLoopGroup _bossGroup;
  private NioEventLoopGroup _workerGroup;
  private EventExecutorGroup _eventExecutors;

  public HttpNettyServer(int port, int threadPoolSize, HttpDispatcher dispatcher)
  {
    _port = port;
    _threadPoolSize = threadPoolSize;
    _dispatcher = dispatcher;
  }

  @Override
  public void start()
  {
    _eventExecutors = new DefaultEventExecutorGroup(_threadPoolSize);
    _bossGroup = new NioEventLoopGroup(1, new NamedThreadFactory("R2 Nio Boss"));
    _workerGroup = new NioEventLoopGroup(0, new NamedThreadFactory("R2 Nio Worker"));

    ServerBootstrap bootstrap = new ServerBootstrap()
                                      .group(_bossGroup, _workerGroup)
                                      .channel(NioServerSocketChannel.class)
                                      .childHandler(new ChannelInitializer<NioSocketChannel>()
                                      {
                                        @Override
                                        protected void initChannel(NioSocketChannel ch)
                                            throws Exception
                                        {
                                          ch.pipeline().addLast("decoder", new HttpRequestDecoder());
                                          ch.pipeline().addLast("aggregator", new HttpObjectAggregator(1048576));
                                          ch.pipeline().addLast("encoder", new HttpResponseEncoder());
                                          ch.pipeline().addLast("rapi", new RAPServerCodec());
                                          ch.pipeline().addLast(_eventExecutors, "handler", new Handler());
                                        }
                                      });

    bootstrap.bind(new InetSocketAddress(_port));
  }

  @Override
  public void stop()
  {
    System.out.println("Shutting down");
    // shut down Netty thread pool and close all channels associated with.
    _bossGroup.shutdownGracefully();
    _workerGroup.shutdownGracefully();
  }

  @Override
  public void waitForStop() throws InterruptedException
  {
    _bossGroup.terminationFuture().await();
    _workerGroup.terminationFuture().await();

  }

  private class Handler extends SimpleChannelInboundHandler<RestRequest>
  {
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

}

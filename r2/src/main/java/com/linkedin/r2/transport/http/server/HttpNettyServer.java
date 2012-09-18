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

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.transport.common.WireAttributeHelper;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;

/**
 * TODO: Do we still need this?
 *
 * TODO: for now we use com.linkedin.r2.api.message for requests and responses. We may want
 * constructs specific to this transport.
 *
 * @author Steven Ihde
 * @version $Revision: $
 */

/* package private */ class HttpNettyServer implements HttpServer
{
  private ServerBootstrap _bootstrap;
  private final ChannelGroup _allChannels = new DefaultChannelGroup("RAP server channels");

  private final ExecutionHandler _executionHandler = new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(256, 0, 0));

  private final int _port;
  private final HttpDispatcher _dispatcher;

  public HttpNettyServer(int port, HttpDispatcher dispatcher)
  {
    _port = port;
    _dispatcher = dispatcher;
  }

  @Override
  public void start()
  {
    ChannelFactory factory =
          new NioServerSocketChannelFactory(
                  Executors.newCachedThreadPool(),
                  Executors.newFixedThreadPool(256)
//                  Executors.newCachedThreadPool()
          );

    _bootstrap = new ServerBootstrap(factory);
    _bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      @Override
      public ChannelPipeline getPipeline() throws Exception
      {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("rapi", new RAPServerCodec());
        pipeline.addLast("execution", _executionHandler);
        pipeline.addLast("handler", new Handler());
        return pipeline;
      }
    });
    _bootstrap.bind(new InetSocketAddress(_port));
  }

  // TODO: can we make shutdown asynchronous?
  @Override
  public void stop()
  {
    System.out.println("Shutting down");
    ChannelGroupFuture shutdown = _allChannels.disconnect();
    shutdown.awaitUninterruptibly();
    _bootstrap.releaseExternalResources();
    _executionHandler.releaseExternalResources();
  }

  @Override
  public void waitForStop() throws InterruptedException
  {
    // Cheat and delegate to stop for now
    stop();
  }

  private class Handler extends SimpleChannelUpstreamHandler
  {
    // By virtue of being upstream  from the ExecutionHandler, all events in this handler will
    // be handled on a separate thread so it is safe to block, etc.
    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) throws Exception
    {
      final Channel ch = e.getChannel();
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

          ch.write(responseBuilder.build());
        }
      };
      RestRequest request = (RestRequest) e.getMessage();
      try
      {
        _dispatcher.handleRequest(request, writeResponseCallback);
      }
      catch (Exception ex)
      {
        writeResponseCallback.onResponse(TransportResponseImpl.<RestResponse> error(ex,
                                                                                    Collections.<String, String> emptyMap()));
      }
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
    {
      _allChannels.add(ctx.getChannel());
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
    {
      _allChannels.remove(ctx.getChannel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
    {
      // TODO close channel, etc.
      e.getCause().printStackTrace();
    }
  }

}

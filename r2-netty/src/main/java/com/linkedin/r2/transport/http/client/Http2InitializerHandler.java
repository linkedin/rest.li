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

package com.linkedin.r2.transport.http.client;

import com.linkedin.r2.message.Request;
import com.linkedin.r2.transport.common.bridge.common.RequestWithCallback;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpClientUpgradeHandler;
import io.netty.handler.codec.http.HttpScheme;
import io.netty.handler.codec.http2.Http2ClientUpgradeCodec;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslHandler;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;


/**
 * Initializes the pipeline upon receiving the first request to #write. Based on the HTTP scheme of
 * the first request, a pipeline setup is selected.
 *
 * If the selected scheme is HTTP, HTTP/2 over TCP through protocol upgrade (h2c) pipeline is setup.
 * Else HTTP/2 over TLS through ALPN (h2) pipeline is setup.
 */
class Http2InitializerHandler extends ChannelOutboundHandlerAdapter
{
  private static final int MAX_CLIENT_UPGRADE_CONTENT_LENGTH = Integer.MAX_VALUE;
  private static final int MAX_INITIAL_LINE_LENGTH = 4096;
  private static final boolean IS_CLIENT = true;

  private final int _maxHeaderSize;
  private final int _maxChunkSize;
  private final long _maxResponseSize;
  private final long _streamingTimeout;
  private final long _gracefulShutdownTimeout;
  private final Http2Connection _connection;
  private final ScheduledExecutorService _scheduler;
  private final SSLContext _sslContext;
  private final SSLParameters _sslParameters;

  private boolean _setupComplete = false;

  public Http2InitializerHandler(int maxHeaderSize, int maxChunkSize, long maxResponseSize, long streamingTimeout,
      ScheduledExecutorService scheduler, Http2Connection connection, SSLContext sslContext, SSLParameters sslParameters)
  {
    _maxHeaderSize = maxHeaderSize;
    _maxChunkSize = maxChunkSize;
    _maxResponseSize = maxResponseSize;
    _streamingTimeout = streamingTimeout;
    // Graceful shutdown timeout dictates the amount of time an HTTP/2 connection waits for existing streams
    // to complete before shutting down the connection, by either connection error or intentional connection close.
    // Sets graceful shutdown timeout to the same value as streaming timeout because there is no point to wait
    // any further if the request has already timed out.
    _gracefulShutdownTimeout = streamingTimeout;
    _scheduler = scheduler;
    _connection = connection;
    _sslContext = sslContext;
    _sslParameters = sslParameters;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    if (!(msg instanceof RequestWithCallback))
    {
      ctx.write(msg, promise);
      return;
    }

    if (_setupComplete)
    {
      ctx.write(msg);
    }
    else
    {
      Request request = ((RequestWithCallback) msg).request();
      URI uri = request.getURI();
      String scheme = uri.getScheme();
      if (scheme.equalsIgnoreCase(HttpScheme.HTTPS.toString()))
      {
        configureHttpsPipeline(ctx);
      }
      else if (scheme.equalsIgnoreCase(HttpScheme.HTTP.toString()))
      {
        configureHttpPipeline(ctx, request);
      }
      else
      {
        throw new IllegalArgumentException("Invalid scheme " + scheme + ", expected either http or https");
      }
      ctx.write(msg);
    }
  }

  /**
   * Sets up HTTP/2 over TCP through protocol upgrade (h2c) pipeline
   */
  private void configureHttpPipeline(ChannelHandlerContext ctx, Request request) throws Exception
  {
    Http2StreamCodec http2Codec = new Http2StreamCodecBuilder()
        .connection(_connection)
        .maxContentLength(_maxResponseSize)
        .gracefulShutdownTimeoutMillis(_gracefulShutdownTimeout)
        .streamingTimeout(_streamingTimeout)
        .scheduler(_scheduler)
        .build();
    HttpClientCodec sourceCodec = new HttpClientCodec(MAX_INITIAL_LINE_LENGTH, _maxHeaderSize, _maxChunkSize);
    Http2ClientUpgradeCodec targetCodec = new Http2ClientUpgradeCodec(http2Codec);
    HttpClientUpgradeHandler upgradeCodec = new HttpClientUpgradeHandler(
        sourceCodec, targetCodec, MAX_CLIENT_UPGRADE_CONTENT_LENGTH);
    Http2SchemeHandler schemeHandler = new Http2SchemeHandler(HttpScheme.HTTP.toString());

    String host = request.getURI().getAuthority();
    int port = request.getURI().getPort();
    String path = request.getURI().getPath();

    Http2UpgradeHandler upgradeHandler = new Http2UpgradeHandler(host, port, path);
    Http2StreamResponseHandler responseHandler = new Http2StreamResponseHandler();
    Http2ChannelPoolHandler channelPoolHandler = new Http2ChannelPoolHandler();

    ctx.pipeline().addBefore(ctx.name(), "sourceCodec", sourceCodec);
    ctx.pipeline().addBefore(ctx.name(), "upgradeCodec", upgradeCodec);
    ctx.pipeline().addBefore(ctx.name(), "upgradeHandler", upgradeHandler);
    ctx.pipeline().addBefore(ctx.name(), "schemeHandler", schemeHandler);
    ctx.pipeline().addBefore(ctx.name(), "responseHandler", responseHandler);
    ctx.pipeline().addBefore(ctx.name(), "channelHandler", channelPoolHandler);

    _setupComplete = true;
  }

  /**
   * Sets up HTTP/2 over TLS through ALPN (h2) pipeline
   */
  private void configureHttpsPipeline(ChannelHandlerContext ctx) throws Exception
  {
    JdkSslContext context = new JdkSslContext(
        _sslContext,
        IS_CLIENT,
        Arrays.asList(_sslParameters.getCipherSuites()),
        IdentityCipherSuiteFilter.INSTANCE,
        new ApplicationProtocolConfig(
            ApplicationProtocolConfig.Protocol.ALPN,
            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
            ApplicationProtocolNames.HTTP_2,
            ApplicationProtocolNames.HTTP_1_1),
        _sslParameters.getNeedClientAuth() ? ClientAuth.REQUIRE : ClientAuth.OPTIONAL);
    SslHandler sslHandler = context.newHandler(ctx.alloc());

    Http2StreamCodec http2Codec = new Http2StreamCodecBuilder()
        .connection(_connection)
        .maxContentLength(_maxResponseSize)
        .gracefulShutdownTimeoutMillis(_gracefulShutdownTimeout)
        .streamingTimeout(_streamingTimeout)
        .scheduler(_scheduler)
        .build();

    Http2AlpnHandler alpnHandler = new Http2AlpnHandler(sslHandler, http2Codec);
    Http2SchemeHandler schemeHandler = new Http2SchemeHandler(HttpScheme.HTTPS.toString());
    Http2StreamResponseHandler responseHandler = new Http2StreamResponseHandler();
    Http2ChannelPoolHandler channelPoolHandler = new Http2ChannelPoolHandler();

    ctx.pipeline().addBefore(ctx.name(), "alpnHandler", alpnHandler);
    ctx.pipeline().addBefore(ctx.name(), "schemeHandler", schemeHandler);
    ctx.pipeline().addBefore(ctx.name(), "responseHandler", responseHandler);
    ctx.pipeline().addBefore(ctx.name(), "channelHandler", channelPoolHandler);

    _setupComplete = true;
  }
}

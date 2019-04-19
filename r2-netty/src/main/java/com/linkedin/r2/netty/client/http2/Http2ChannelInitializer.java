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

package com.linkedin.r2.netty.client.http2;

import com.linkedin.r2.netty.common.NettyChannelAttributes;
import com.linkedin.r2.netty.handler.common.CancelTimeoutHandler;
import com.linkedin.r2.netty.handler.common.CertificateHandler;
import com.linkedin.r2.netty.handler.common.ChannelLifecycleHandler;
import com.linkedin.r2.netty.handler.common.ClientEntityStreamHandler;
import com.linkedin.r2.netty.handler.common.SchemeHandler;
import com.linkedin.r2.netty.handler.common.SessionResumptionSslHandler;
import com.linkedin.r2.netty.handler.common.SslHandshakeTimingHandler;
import com.linkedin.r2.netty.handler.http2.Http2AlpnHandler;
import com.linkedin.r2.netty.handler.http2.Http2MessageDecoders;
import com.linkedin.r2.netty.handler.http2.Http2MessageEncoders;
import com.linkedin.r2.netty.handler.http2.Http2ProtocolUpgradeHandler;
import com.linkedin.r2.netty.handler.http2.UnsupportedHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpClientUpgradeHandler;
import io.netty.handler.codec.http2.Http2ClientUpgradeCodec;
import io.netty.handler.codec.http2.Http2MultiplexCodec;
import io.netty.handler.codec.http2.Http2MultiplexCodecBuilder;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;


/**
 * HTTP/2 implementation of {@link ChannelInitializer}. HTTP/2 channel pipeline initialization
 * takes the follow steps based on whether the channel is SSL(TLS) or clear text.
 *
 * During SSL channel initialization, the channel pipeline is first configured with
 * {@link SessionResumptionSslHandler} and {@link Http2AlpnHandler} to perform application level
 * protocol negotiation. If SSL handshake or ALPN failed to negotiate HTTP/2, appropriate failure
 * exception will be set to the initialization {@link ChannelPromise}. If SSL handshake and ALPN
 * succeed, {@link Http2MultiplexCodec} is added and the pipeline should be setup with the following
 * handlers.
 *
 * DefaultChannelPipeline {
 *   (sslHandler = {@link io.netty.handler.ssl.SslHandler}),
 *   (CertificateHandler = {@link CertificateHandler}),
 *   (sslHandshakeTimingHandler = {@link SslHandshakeTimingHandler}),
 *   (Http2MultiplexCodec#0 = {@link io.netty.handler.codec.http2.Http2MultiplexCodec})
 * }
 *
 * During clear text channel initialization, the channel pipeline is first configured with
 * {@link HttpClientCodec}, {@link Http2ClientUpgradeCodec}, and {@link Http2ProtocolUpgradeHandler}.
 * An upgrade request is sent immediately upon the channel becoming active. If upgrade to
 * HTTP/2 fails, appropriate failure exception will be set to the initialization {@link ChannelPromise}.
 * If upgrade succeed, {@link Http2MultiplexCodec} is added and the pipeline should be setup with
 * the following handlers.
 *
 * DefaultChannelPipeline{
 *   (HttpClientCodec#0 = {@link io.netty.handler.codec.http.HttpClientCodec}),
 *   (HttpClientUpgradeHandler#0 = {@link io.netty.handler.codec.http.HttpClientUpgradeHandler}),
 *   (Http2MultiplexCodec#0 = {@link io.netty.handler.codec.http2.Http2MultiplexCodec})
 * }
 *
 * Common to both SSL and clear text, HTTP/2 streams are represented as child channel of the parent
 * channel established above. Once the parent channel is established, new stream child channels can
 * be created on demand from the {@link Http2StreamChannelInitializer}. The stream child channel pipelines
 * are established with the follow pipeline handlers.
 *
 * Http2MultiplexCodec$DefaultHttp2StreamChannel$1{
 *   (Http2StreamChannelInitializer#0 = {@link Http2StreamChannelInitializer}),
 *   (outboundDataHandler = {@link Http2MessageEncoders.DataEncoder}),
 *   (outboundRequestHandler = {@link Http2MessageEncoders.RequestEncoder}),
 *   (inboundDataHandler = {@link Http2MessageDecoders.DataDecoder}),
 *   (inboundRequestHandler = {@link Http2MessageDecoders.ResponseDecoder}),
 *   (schemeHandler = {@link SchemeHandler}),
 *   (streamDuplexHandler = {@link ClientEntityStreamHandler}),
 *   (timeoutHandler = {@link CancelTimeoutHandler}),
 *   (channelPoolHandler = {@link ChannelLifecycleHandler})
 * }
 *
 * Remote created streams (even number streams) are not supported on the client side. The pipeline
 * of remote created streams are setup with a single handler to log errors.
 *
 * Http2MultiplexCodec$DefaultHttp2StreamChannel$1{
 *   (unsupportedHandler = {@link UnsupportedHandler})
 * }
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
class Http2ChannelInitializer extends ChannelInitializer<NioSocketChannel>
{
  private static final long MAX_INITIAL_STREAM_WINDOW_SIZE = 8 * 1024 * 1024;
  private static final boolean IS_CLIENT = true;

  private final SSLContext _sslContext;
  private final SSLParameters _sslParameters;
  private final int _maxInitialLineLength;
  private final int _maxHeaderSize;
  private final int _maxChunkSize;
  private final int _maxContentLength;
  private final boolean _ssl;
  private final boolean _enableSSLSessionResumption;

  Http2ChannelInitializer(SSLContext sslContext, SSLParameters sslParameters, int maxInitialLineLength,
      int maxHeaderSize, int maxChunkSize, long maxContentLength, boolean enableSSLSessionResumption)
  {
    _sslContext = sslContext;
    _sslParameters = sslParameters;
    _maxInitialLineLength = maxInitialLineLength;
    _maxHeaderSize = maxHeaderSize;
    _maxChunkSize = maxChunkSize;
    _maxContentLength = maxContentLength > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) maxContentLength;
    _ssl = _sslContext != null && _sslParameters != null;
    _enableSSLSessionResumption = enableSSLSessionResumption;
  }

  @Override
  protected void initChannel(NioSocketChannel channel) throws SSLException
  {
    if (_ssl)
    {
      configureSsl(channel);
    }
    else
    {
      configureClearText(channel);
    }
  }

  /**
   * Configure the pipeline for TLS ALPN negotiation to HTTP/2.
   */
  private void configureSsl(NioSocketChannel channel) throws SSLException
  {
    final SslContext sslCtx = createSslContext();
    final ChannelPromise alpnPromise = channel.newPromise();

    channel.attr(NettyChannelAttributes.INITIALIZATION_FUTURE).set(alpnPromise);

    channel.pipeline().addLast(
        SessionResumptionSslHandler.PIPELINE_SESSION_RESUMPTION_HANDLER,
        new SessionResumptionSslHandler(sslCtx, _enableSSLSessionResumption));
    channel.pipeline().addLast(new Http2AlpnHandler(alpnPromise, createHttp2Settings()));
  }


  private JdkSslContext createSslContext()
  {
    // Ideally we would use the SslContextBuilder class provided by Netty here however the builder
    // does not support constructing from existing SSLContext and SSLParameters which we already use.
    return new JdkSslContext(
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
        _sslParameters.getNeedClientAuth() ? ClientAuth.REQUIRE : ClientAuth.OPTIONAL,
        null,  false);
  }

  /**
   * Configure the pipeline for HTTP/2 clear text.
   */
  private void configureClearText(NioSocketChannel channel)
  {
    final HttpClientCodec sourceCodec = new HttpClientCodec(_maxInitialLineLength, _maxHeaderSize, _maxChunkSize);

    Http2ClientUpgradeCodec upgradeCodec = new Http2ClientUpgradeCodec(
        Http2MultiplexCodecBuilder
        .forClient(new UnsupportedHandler())
        .initialSettings(createHttp2Settings())
        .withUpgradeStreamHandler(new ChannelInboundHandlerAdapter())
        .build());

    final ChannelPromise upgradePromise = channel.newPromise();
    channel.attr(NettyChannelAttributes.INITIALIZATION_FUTURE).set(upgradePromise);

    channel.pipeline().addLast(sourceCodec);
    channel.pipeline().addLast(new HttpClientUpgradeHandler(sourceCodec, upgradeCodec, _maxContentLength));
    channel.pipeline().addLast(new Http2ProtocolUpgradeHandler(upgradePromise));
  }

  private Http2Settings createHttp2Settings()
  {
    final Http2Settings settings = new Http2Settings();
    settings.initialWindowSize((int) Math.min(MAX_INITIAL_STREAM_WINDOW_SIZE, _maxContentLength));
    settings.maxHeaderListSize(_maxHeaderSize);
    return settings;
  }
}

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

package com.linkedin.r2.netty.client.http;

import com.linkedin.r2.netty.handler.common.CancelTimeoutHandler;
import com.linkedin.r2.netty.handler.common.CertificateHandler;
import com.linkedin.r2.netty.handler.common.ChannelLifecycleHandler;
import com.linkedin.r2.netty.handler.common.ClientEntityStreamHandler;
import com.linkedin.r2.netty.handler.common.SchemeHandler;
import com.linkedin.r2.netty.handler.common.SessionResumptionSslHandler;
import com.linkedin.r2.netty.handler.common.SslHandshakeTimingHandler;
import com.linkedin.r2.netty.handler.http.HttpMessageDecoders;
import com.linkedin.r2.netty.handler.http.HttpMessageEncoders;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpScheme;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * HTTP/1.1 implementation of {@link ChannelInitializer}. If the channel established is SSL(TLS),
 * the channel pipeline is setup with the following additional handlers.
 *
 * DefaultChannelPipeline {
 *   (sslHandler = {@link io.netty.handler.ssl.SslHandler}),
 *   (CertificateHandler = {@link CertificateHandler}),
 *   (sslHandshakeTimingHandler = {@link SslHandshakeTimingHandler})
 * }
 *
 * The rest of the handlers are common between SSL and non-SSL.
 *
 * DefaultChannelPipeline {
 *   (codec = {@link io.netty.handler.codec.http.HttpClientCodec}),
 *   (outboundRestRequestEncoder = {@link HttpMessageEncoders.RestRequestEncoder}),
 *   (outboundStreamDataEncoder = {@link HttpMessageEncoders.DataEncoder}),
 *   (outboundStreamRequestEncoder = {@link HttpMessageEncoders.StreamRequestEncoder}),
 *   (inboundDataDecoder = {@link HttpMessageDecoders.DataDecoder}),
 *   (inboundRequestDecoder = {@link HttpMessageDecoders.ResponseDecoder}),
 *   (schemeHandler = {@link SchemeHandler}),
 *   (streamDuplexHandler = {@link ClientEntityStreamHandler}),
 *   (timeoutHandler = {@link CancelTimeoutHandler}),
 *   (channelPoolHandler = {@link ChannelLifecycleHandler})
 * }
 *
 * @author Sean Sheng
 * @author Nizar Mankulangara
 */
class HttpChannelInitializer extends ChannelInitializer<NioSocketChannel>
{
  /**
   * HTTP/2 stream channels are not recyclable and should be disposed upon completion.
   */
  private static final boolean RECYCLE_CHANNEL = true;

  private final SSLContext _sslContext;
  private final SSLParameters _sslParameters;
  private final int _maxInitialLineLength;
  private final int _maxHeaderSize;
  private final int _maxChunkSize;
  private final int _maxContentLength;
  private final int _sslHandShakeTimeout;
  private final boolean _ssl;
  private final boolean _enableSSLSessionResumption;

  HttpChannelInitializer(SSLContext sslContext, SSLParameters sslParameters, int maxInitialLineLength,
      int maxHeaderSize, int maxChunkSize, long maxContentLength, boolean enableSSLSessionResumption,
      int sslHandShakeTimeout)
  {
    _sslContext = sslContext;
    _sslParameters = sslParameters;
    _maxInitialLineLength = maxInitialLineLength;
    _maxHeaderSize = maxHeaderSize;
    _maxChunkSize = maxChunkSize;
    _maxContentLength = maxContentLength > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) maxContentLength;
    _sslHandShakeTimeout = sslHandShakeTimeout;
    _ssl = _sslContext != null && _sslParameters != null;
    _enableSSLSessionResumption = enableSSLSessionResumption;
  }

  @Override
  protected void initChannel(NioSocketChannel channel)
  {
    if (_ssl)
    {
      channel.pipeline().addLast(SessionResumptionSslHandler.PIPELINE_SESSION_RESUMPTION_HANDLER,
          new SessionResumptionSslHandler(_sslContext, _sslParameters, _enableSSLSessionResumption, _sslHandShakeTimeout));
    }

    channel.pipeline().addLast("codec", new HttpClientCodec(_maxInitialLineLength, _maxHeaderSize, _maxChunkSize));
    channel.pipeline().addLast("outboundRestRequestEncoder", HttpMessageEncoders.newRestRequestEncoder());
    channel.pipeline().addLast("outboundStreamDataEncoder", HttpMessageEncoders.newDataEncoder());
    channel.pipeline().addLast("outboundStreamRequestEncoder", HttpMessageEncoders.newStreamRequestEncoder());
    channel.pipeline().addLast("inboundDataDecoder", HttpMessageDecoders.newDataDecoder());
    channel.pipeline().addLast("inboundRequestDecoder", HttpMessageDecoders.newResponseDecoder());
    channel.pipeline().addLast("schemeHandler", new SchemeHandler(_ssl ? HttpScheme.HTTPS.toString() : HttpScheme.HTTP.toString()));
    channel.pipeline().addLast("streamDuplexHandler", new ClientEntityStreamHandler(_maxContentLength));
    channel.pipeline().addLast("timeoutHandler", new CancelTimeoutHandler());
    channel.pipeline().addLast("channelPoolHandler", new ChannelLifecycleHandler(RECYCLE_CHANNEL));
  }
}

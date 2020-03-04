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

package com.linkedin.r2.netty.handler.common;

import com.linkedin.r2.netty.common.SslHandlerUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * SSL handshake, is often an expensive operation. Luckily, it has been developed a feature that allows to resume
 * past connections.
 * <p>
 * The {@link javax.net.ssl.SSLEngine} once created doesn't have context about connections or addresses,
 * but if host and port are specified at its creation, can use the session resumption feature.
 * <p>
 * This class just initialize the pipeline, adding the SSL handlers. It cannot be in the #initChannel of the
 * PipelineInitializer, because when initiating the channel, we are not yet aware of the remote address.
 * Only once connected we can know the remote address and create the SSLEngine with it to take advantage of the
 * session resumption
 *
 * The class will initiate a SSL handshake upon the connection takes place.
 *
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class SessionResumptionSslHandler extends ChannelOutboundHandlerAdapter
{
  public static final String PIPELINE_SESSION_RESUMPTION_HANDLER = "SessionResumptionSslHandler";
  public static final AttributeKey<SessionResumptionSslHandler> CHANNEL_SESSION_RESUMPTION_HANDLER =
      AttributeKey.valueOf("sslSessionResumptionHandler");

  private final SslHandlerGenerator _hostPortToSslHandler;
  private final int _sslHandShakeTimeout;

  /**
   * @param sslContext note that the type is SslContext (netty implementation) and not SSLContext (JDK implementation)
   */
  public SessionResumptionSslHandler(SslContext sslContext, boolean enableResumption, int sslHandShakeTimeout)
  {
    _sslHandShakeTimeout = sslHandShakeTimeout;
    _hostPortToSslHandler = enableResumption ?
      (ctx, host, port) -> sslContext.newHandler(ctx.alloc(), host, port) :
      (ctx, host, port) -> sslContext.newHandler(ctx.alloc());

  }

  /**
   * @param sslContext note that the type is SSLContext (JDK implementation) and not SslContext (netty implementation)
   */
  public SessionResumptionSslHandler(SSLContext sslContext, SSLParameters sslParameters,
      boolean enableResumption, int sslHandShakeTimeout)
  {
    _sslHandShakeTimeout = sslHandShakeTimeout;
    _hostPortToSslHandler = enableResumption ?
      (ctx, host, port) -> SslHandlerUtil.getClientSslHandler(sslContext, sslParameters, host, port) :
      (ctx, host, port) -> SslHandlerUtil.getSslHandler(sslContext, sslParameters, true);
  }

  @Override
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
      ChannelPromise promise) throws Exception
  {
    final InetSocketAddress address = ((InetSocketAddress) remoteAddress);
    final SslHandler sslHandler = _hostPortToSslHandler.create(ctx, address.getHostName(), address.getPort());
    sslHandler.setHandshakeTimeout(_sslHandShakeTimeout, TimeUnit.MILLISECONDS);

    ctx.pipeline().addAfter(PIPELINE_SESSION_RESUMPTION_HANDLER, SslHandlerUtil.PIPELINE_SSL_HANDLER, sslHandler);
    ctx.pipeline().addAfter(SslHandlerUtil.PIPELINE_SSL_HANDLER, SslHandshakeTimingHandler.SSL_HANDSHAKE_TIMING_HANDLER,
        new SslHandshakeTimingHandler(sslHandler.handshakeFuture()));

    // the certificate handler should be run only after the handshake is completed (and therefore after the ssl handler)
    ctx.pipeline().addAfter(SslHandlerUtil.PIPELINE_SSL_HANDLER, CertificateHandler.PIPELINE_CERTIFICATE_HANDLER,
        new CertificateHandler(sslHandler));

    ctx.pipeline().remove(PIPELINE_SESSION_RESUMPTION_HANDLER);

    super.connect(ctx, remoteAddress, localAddress, promise);
  }

  @FunctionalInterface
  interface SslHandlerGenerator
  {
    SslHandler create(ChannelHandlerContext ctx, String host, int port);
  }
}

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

import com.linkedin.r2.transport.common.bridge.common.RequestWithCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.r2.transport.http.client.TimeoutAsyncPoolHandle;
import com.linkedin.r2.netty.handler.common.SessionResumptionSslHandler;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.util.internal.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A handler that triggers the ALPN protocol negotiate upon adding to the pipeline by
 * listening to upstream {@link SslHandshakeCompletionEvent}. Calls to #write and #flush
 * are suspended util negotiation is complete.
 *
 * The handler removes itself if protocol h2 is negotiated. If any protocol other than h2
 * is negotiated, the handler will error out all subsequent requests.
 */
class Http2AlpnHandler extends ChannelDuplexHandler
{
  private static final Logger LOG = LoggerFactory.getLogger(Http2AlpnHandler.class);
  public static final String PIPELINE_ALPN_HANDLER = "alpnHandler";

  private final SslContext _sslContext;
  private final Http2StreamCodec _http2Handler;

  private ChannelPromise _alpnPromise;
  private final boolean _enableSSLSessionResumption;
  private final int _sslHandShakeTimeout;

  public Http2AlpnHandler(SslContext sslContext, Http2StreamCodec http2Handler, boolean enableSSLSessionResumption,
      int sslHandShakeTimeout)
  {
    ObjectUtil.checkNotNull(sslContext, "sslContext");
    ObjectUtil.checkNotNull(http2Handler, "http2Handler");

    _sslContext = sslContext;
    _http2Handler = http2Handler;
    _enableSSLSessionResumption = enableSSLSessionResumption;
    _sslHandShakeTimeout = sslHandShakeTimeout;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx)
  {
    _alpnPromise = ctx.channel().newPromise();

    // the class will take care of establishing the SSL connection
    ctx.pipeline().addFirst(SessionResumptionSslHandler.PIPELINE_SESSION_RESUMPTION_HANDLER,
      new SessionResumptionSslHandler(_sslContext, _enableSSLSessionResumption, _sslHandShakeTimeout));

    // Fail the ALPN promise when channel is closed
    ctx.channel().closeFuture().addListener(future -> {
      if (!_alpnPromise.isDone())
      {
        _alpnPromise.setFailure(new ChannelException("HTTP/2 ALPN did not complete before channel closed"));
      }
    });
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    if (!(msg instanceof RequestWithCallback))
    {
      ctx.write(msg, promise);
      return;
    }

    _alpnPromise.addListener(f -> {
      ChannelFuture future = (ChannelFuture) f;
      if (future.isSuccess())
      {
        ctx.write(msg, promise);
      }
      else
      {
        // Releases the async pool handle
        @SuppressWarnings("unchecked")
        TimeoutAsyncPoolHandle<?> handle = ((RequestWithCallback<?, ?, TimeoutAsyncPoolHandle<?>>) msg).handle();
        handle.dispose();

        // Invokes user specified callback with error
        TransportCallback<?> callback = ((RequestWithCallback) msg).callback();
        callback.onResponse(TransportResponseImpl.error(future.cause()));
      }
    });
  }

  @Override
  public void flush(final ChannelHandlerContext ctx) throws Exception
  {
    _alpnPromise.addListener(f -> {
      ChannelFuture future = (ChannelFuture) f;
      if (future.isSuccess())
      {
        ctx.flush();
      }
    });
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof SslHandshakeCompletionEvent)
    {
      SslHandshakeCompletionEvent handshakeEvent = (SslHandshakeCompletionEvent) evt;
      if (handshakeEvent.isSuccess())
      {
        LOG.debug("SSL handshake succeeded");
        SslHandler sslHandler = ctx.pipeline().get(SslHandler.class);
        if (sslHandler == null)
        {
          ctx.fireExceptionCaught(new IllegalStateException("cannot find a SslHandler in the pipeline (required for " +
              "application-level protocol negotiation)"));
          return;
        }
        String protocol = sslHandler.applicationProtocol();
        if (ApplicationProtocolNames.HTTP_2.equals(protocol))
        {
          LOG.debug("HTTP/2 is negotiated");

          // Add HTTP/2 handler
          // by "adding before" the alpn handler, we guarantee that once the alpnPromise is completed
          // the request will be handled by the codec and all the possible exceptions thrown will be
          // handled by a single stream instead of the whole channel
          ctx.pipeline().addBefore(PIPELINE_ALPN_HANDLER, Http2StreamCodec.PIPELINE_HTTP2_CODEC_HANDLER, _http2Handler);

          // Remove handler from pipeline after negotiation is complete
          ctx.pipeline().remove(this);
          _alpnPromise.setSuccess();
        }
        else
        {
          LOG.error("Protocol {}, instead of HTTP/2, is negotiated through ALPN", protocol);
          _alpnPromise.setFailure(new IllegalStateException("HTTP/2 ALPN negotiation failed"));
        }
      }
      else
      {
        LOG.error("SSL handshake failed", handshakeEvent.cause());
        _alpnPromise.setFailure(handshakeEvent.cause());
      }
    }

    ctx.fireUserEventTriggered(evt);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
  {
    LOG.error("Application level protocol negotiation failed", cause);
    if (!_alpnPromise.isDone())
    {
      _alpnPromise.setFailure(cause);
    }
    ctx.fireExceptionCaught(cause);
  }
}

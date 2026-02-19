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

import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.message.timing.TimingImportance;
import com.linkedin.r2.message.timing.TimingKey;
import com.linkedin.r2.message.timing.TimingNameConstants;
import com.linkedin.r2.netty.common.NettyChannelAttributes;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import java.security.cert.Certificate;


/**
 * An SSL handler that records time in establishing a handshake.
 *
 * SSL hand shake starts when {@link SslHandler} is added to {@link io.netty.channel.ChannelPipeline}.
 * This handler is added after {@link SslHandler}, so technically this timer is started after hand shake begins,
 * but the difference should be negligible.
 *
 * @author Xialin Zhu
 */
public class SslHandshakeTimingHandler extends ChannelOutboundHandlerAdapter
{
  public static final String SSL_HANDSHAKE_TIMING_HANDLER = "sslHandshakeTimingHandler";

  public static final AttributeKey<Long> SSL_HANDSHAKE_START_TIME = AttributeKey.valueOf("sslHandshakeStartTime");

  public static final TimingKey TIMING_KEY = TimingKey.registerNewKey(TimingNameConstants.SSL_HANDSHAKE, TimingImportance.LOW);

  private final Future<Channel> _handshakeFuture;

  public SslHandshakeTimingHandler(Future<Channel> handshakeFuture)
  {
    _handshakeFuture = handshakeFuture;
  }

  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception
  {
    long startTime = System.nanoTime();
    _handshakeFuture.addListener(future -> {
      if (future.isSuccess())
      {
        long duration = System.nanoTime() - startTime;
        ctx.channel().attr(SSL_HANDSHAKE_START_TIME).set(duration);
      }
    });
  }

  public static <T> TransportCallback<T> getSslTimingCallback(Channel channel, RequestContext requestContext, TransportCallback<T> callback)
  {
    return response -> {
      Long duration = channel.attr(SslHandshakeTimingHandler.SSL_HANDSHAKE_START_TIME).getAndSet(null);
      if (duration != null)
      {
        TimingContextUtil.markTiming(requestContext, TIMING_KEY, duration);
      }

      // Try reading the server certificate from the current channel first (works for HTTP/1.1
      // where CertificateHandler sets it directly on the channel). 
      Certificate[] peerCerts = channel.attr(NettyChannelAttributes.SERVER_CERTIFICATES).get();

      // If not found, read from the parent channel (works for HTTP/2 where the pool hands out
      // child stream channels but the certificate is set on the parent TCP channel).
      if (peerCerts == null && channel.parent() != null)
      {
        peerCerts = channel.parent().attr(NettyChannelAttributes.SERVER_CERTIFICATES).get();
      }
      
      if (peerCerts != null)
      {
        requestContext.putLocalAttr(R2Constants.SERVER_CERT, peerCerts);
      }

      callback.onResponse(response);
    };
  }
}

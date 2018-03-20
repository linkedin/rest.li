/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.client.common;

import com.linkedin.r2.transport.http.client.common.ssl.SslSessionNotTrustedException;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionValidator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import java.util.List;


/**
 * In the case the user requires the Server verification, we extract the
 * generated session and we run a validity check on it
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class CertificateHandler extends ChannelOutboundHandlerAdapter
{
  private final SslHandler _sslHandler;

  public static final String PIPELINE_CERTIFICATE_HANDLER = "CertificateHandler";

  public static final AttributeKey<SslSessionValidator> REQUESTED_SSL_SESSION_VALIDATOR
      = AttributeKey.valueOf("requestedSslSessionValidator");

  public CertificateHandler(SslHandler sslHandler)
  {
    _sslHandler = sslHandler;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    _sslHandler.handshakeFuture().addListener(future -> {
      SslSessionValidator sslSessionValidator = ctx.channel().attr(REQUESTED_SSL_SESSION_VALIDATOR).getAndSet(null);

      // if cert is empty, the check is disabled and not needed by the user, therefore don't check
      if (future.isSuccess() && sslSessionValidator != null)
      {
        try
        {
          sslSessionValidator.validatePeerSession(_sslHandler.engine().getSession());
        }
        catch (SslSessionNotTrustedException e)
        {
          ctx.fireExceptionCaught(e);
          return;
        }
      }
      ctx.write(msg, promise);
    });
  }

  @Override
  public void flush(ChannelHandlerContext ctx) throws Exception
  {
    _sslHandler.handshakeFuture().addListener(future -> ctx.flush());
  }
}

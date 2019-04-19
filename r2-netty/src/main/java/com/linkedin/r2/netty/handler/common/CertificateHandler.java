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

package com.linkedin.r2.netty.handler.common;

import com.linkedin.r2.netty.common.NettyChannelAttributes;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionNotTrustedException;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionValidator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.SslHandler;

/**
 * In the case the user requires the Server verification, we extract the
 * generated session and we run a validity check on it
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class CertificateHandler extends ChannelOutboundHandlerAdapter
{
  private final SslHandler _sslHandler;
  private SslSessionValidator _cachedSessionValidator;

  public static final String PIPELINE_CERTIFICATE_HANDLER = "CertificateHandler";

  public CertificateHandler(SslHandler sslHandler)
  {
    _sslHandler = sslHandler;
    _cachedSessionValidator = null;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
  {
    _sslHandler.handshakeFuture().addListener(future -> {
      // if the sslHandler (before this one), wasn't able to complete handshake, there is no reason to run the
      // SSLValidation, nor send anything on the channel
      if (!future.isSuccess())
      {
        return;
      }

      SslSessionValidator sslSessionValidator = ctx.channel().attr(NettyChannelAttributes.SSL_SESSION_VALIDATOR).getAndSet(null);

      // If cert is empty, the check is disabled and not needed by the user, therefore don't check.
      // Also if sslSessionValidator is the same as the previous one we cached, skipping the check.
      if (sslSessionValidator != null && !sslSessionValidator.equals(_cachedSessionValidator))
      {
        _cachedSessionValidator = sslSessionValidator;
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
  public void flush(ChannelHandlerContext ctx)
  {
    _sslHandler.handshakeFuture().addListener(future -> ctx.flush());
  }
}

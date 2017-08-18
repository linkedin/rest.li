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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;


/**
 * In the case the user requires the Server verification, we extract the server's principal certificate and compare it
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class CertificateHandler extends ChannelOutboundHandlerAdapter
{
  public static final AttributeKey<String> EXPECTED_SERVER_CERT_PRINCIPAL_ATTR_KEY
    = AttributeKey.valueOf("expectedServerCertPrincipal");
  private final SslHandler _sslHandler;

  public CertificateHandler(SslHandler sslHandler)
  {
    _sslHandler = sslHandler;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {

    _sslHandler.handshakeFuture().addListener(future -> {
      String expectedPrincipalName = ctx.channel().attr(EXPECTED_SERVER_CERT_PRINCIPAL_ATTR_KEY).getAndSet(null);

      // if cert is empty, the check is disabled and not needed by the user, therefore don't check
      if (expectedPrincipalName != null)
      {
        String actualPrincipalName = _sslHandler.engine().getSession().getPeerPrincipal().getName();
        if (!expectedPrincipalName.equals(actualPrincipalName))
        {
          ctx.fireExceptionCaught(new ServerCertPrincipalNameMismatchException(expectedPrincipalName, actualPrincipalName));
          return;
        }
      }
      ctx.write(msg, promise);
    });

  }
}

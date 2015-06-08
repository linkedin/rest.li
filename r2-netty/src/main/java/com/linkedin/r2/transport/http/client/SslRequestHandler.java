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

package com.linkedin.r2.transport.http.client;

import com.linkedin.r2.message.Request;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.net.URI;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;


/**
 * @author Ang Xu
 * @version $Revision: $
 */
public class SslRequestHandler extends ChannelOutboundHandlerAdapter
{
  private static final String       HTTPS_SCHEME          = "https";
  private static final String       SSL_HANDLER           = "SslHandler";

  private final SslHandler          _sslHandler;
  private String                    _firstTimeScheme;

  public SslRequestHandler(SSLContext sslContext, SSLParameters sslParameters)
  {
    if (sslContext == null)
    {
      _sslHandler = null;
    }
    else
    {
      SSLEngine sslEngine = sslContext.createSSLEngine();
      sslEngine.setUseClientMode(true);
      if (sslParameters != null)
      {
        String[] cipherSuites = sslParameters.getCipherSuites();
        if (cipherSuites != null && cipherSuites.length > 0)
        {
          sslEngine.setEnabledCipherSuites(sslParameters.getCipherSuites());
        }
        String[] protocols = sslParameters.getProtocols();
        if (protocols != null && protocols.length > 0)
        {
          sslEngine.setEnabledProtocols(sslParameters.getProtocols());
        }
      }
      _sslHandler = new SslHandler(sslEngine);
    }
  }

  /**
   * Override this method to set the handlers for SSL connection the first time this channel
   * is used to make a request. Once used, the scheme of the request on this channel cannot be changed.
   */
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
  {
    if (msg instanceof Request)
    {
      Request request = (Request) msg;
      URI uri = request.getURI();
      String scheme = uri.getScheme();
      if (_firstTimeScheme == null)
      {
        // If this channel is configured for TLS AND this is an HTTPS request, add SSL
        // handler to the channel pipeline
        if (scheme.equalsIgnoreCase(HTTPS_SCHEME))
        {
          if (_sslHandler == null)
          {
            throw new IllegalStateException("The client hasn't been configured with SSLContext "
                + "- cannot make an https request to " + uri);
          }
          /** Note: {@link SslHandler} will initiate a handshake upon being added to the pipeline. */
          ctx.pipeline().addFirst(SSL_HANDLER, _sslHandler);
        }
        _firstTimeScheme = scheme;
      } else if (!scheme.equalsIgnoreCase(_firstTimeScheme))
      {
        throw new IllegalStateException(String.format("Cannot switch scheme from %s to %s for %s",
            _firstTimeScheme, scheme, ctx.channel().remoteAddress()));
      }
    }

    ctx.write(msg, promise);
  }

  @Override
  public void flush(final ChannelHandlerContext ctx) throws Exception
  {
    if (_firstTimeScheme == null)
    {
      throw new IllegalStateException("Flush is called before any request has been written into this channel!");
    }
    if (_firstTimeScheme.equalsIgnoreCase(HTTPS_SCHEME))
    {
      // make sure we don't call ctx#flush() immediately when the handshake is in progress.
      _sslHandler.handshakeFuture().addListener(new FutureListener<Channel>()
      {
        @Override
        public void operationComplete(Future<Channel> future) throws Exception
        {
          ctx.flush();
        }
      });
    }
    else
    {
      ctx.flush();
    }
  }
}

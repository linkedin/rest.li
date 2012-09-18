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

import java.net.URI;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;
import org.jboss.netty.handler.ssl.SslHandler;

import com.linkedin.r2.message.rest.RestRequest;

public class SslRequestHandler extends SimpleChannelDownstreamHandler
{
  private static final String       HTTPS_SCHEME          = "https";
  private static final String       SSL_HANDLER           = "SslHandler";
  private static final String       SECURE_CLIENT_HANDLER = "SecureClientHandler";

  private final SslHandler          _sslHandler;
  private final SecureClientHandler _secureClientHandler  = new SecureClientHandler();
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
   *
   * @param ctx {@link ChannelHandlerContext}
   * @param e {@link MessageEvent}
   *
   * @see org.jboss.netty.channel.SimpleChannelDownstreamHandler#writeRequested(org.jboss.netty.channel.ChannelHandlerContext,
   *      org.jboss.netty.channel.MessageEvent)
   */
  @Override
  public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception
  {
    RestRequest restRequest = (RestRequest) e.getMessage();
    URI uri = restRequest.getURI();
    String scheme = uri.getScheme();
    if (_firstTimeScheme == null)
    {
      // If this channel is configured for TLS AND this is an HTTPS request, add SSL
      // handshake initiation and encryption handlers to the channel pipeline
      if (scheme.equalsIgnoreCase(HTTPS_SCHEME))
      {
        if (_sslHandler == null)
        {
          throw new IllegalStateException("The client hasn't been configured with SSLContext "
              + "- cannot make an https request to " + uri);
        }
        ChannelPipeline pipeline = ctx.getPipeline();
        pipeline.addFirst(SECURE_CLIENT_HANDLER, _secureClientHandler);
        pipeline.addFirst(SSL_HANDLER, _sslHandler);
      }
      _firstTimeScheme = scheme;
    }
    else
    {
      if (!scheme.equalsIgnoreCase(_firstTimeScheme))
      {
        throw new IllegalStateException(String.format("Cannot switch scheme from %s to %s for %s",
                                                      _firstTimeScheme,
                                                      scheme,
                                                      ctx.getChannel().getRemoteAddress()));
      }
    }

    super.writeRequested(ctx, e);
  }


}

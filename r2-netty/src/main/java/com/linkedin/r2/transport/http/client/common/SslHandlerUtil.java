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

import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;


/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class SslHandlerUtil extends ChannelOutboundHandlerAdapter
{

  public final static String PIPELINE_SSL_HANDLER = "sslHandler";

  public static SslHandler getSslHandler(SSLContext sslContext, SSLParameters sslParameters)
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
    return new SslHandler(sslEngine);
  }
}

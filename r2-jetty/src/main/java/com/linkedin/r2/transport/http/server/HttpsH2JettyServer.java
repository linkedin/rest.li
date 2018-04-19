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

package com.linkedin.r2.transport.http.server;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;


/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class HttpsH2JettyServer extends HttpJettyServer
{
  private final int _sslPort;
  private final String _keyStore;
  private final String _keyStorePassword;

  public HttpsH2JettyServer(int port,
                            int sslPort,
                            String keyStore,
                            String keyStorePassword,
                            String contextPath,
                            int threadPoolSize,
                            HttpDispatcher dispatcher,
                            HttpJettyServer.ServletType servletType,
                            int asyncTimeOut,
                            boolean restOverStream)
  {
    super(port, contextPath, threadPoolSize, dispatcher, servletType, asyncTimeOut, restOverStream);
    _sslPort = sslPort;
    _keyStore = keyStore;
    _keyStorePassword = keyStorePassword;
  }

  @Override
  protected Connector[] getConnectors(Server server)
  {
    SslContextFactory sslContextFactory = new SslContextFactory();
    sslContextFactory.setKeyStorePath(_keyStore);
    sslContextFactory.setKeyStorePassword(_keyStorePassword);
    sslContextFactory.setTrustStorePath(_keyStore);
    sslContextFactory.setTrustStorePassword(_keyStorePassword);
    sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
    sslContextFactory.setUseCipherSuitesOrder(true);

    HttpConfiguration https_config = new HttpConfiguration();
    https_config.setSecureScheme(HttpScheme.HTTPS.asString());
    https_config.setSecurePort(_sslPort);

    // HTTPS Configuration
    HttpConfiguration http2_config = new HttpConfiguration(https_config);
    http2_config.addCustomizer(new SecureRequestCustomizer());

    // HTTP/2 Connection Factory
    HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(http2_config)
    {
      /**
       * Required to override since we are using legacy versions in testing which would not be otherwise accepted
       */
      @Override
      public boolean isAcceptable(String protocol, String tlsProtocol, String tlsCipher)
      {
        return true;
      }
    };

    NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable();
    ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
    alpn.setDefaultProtocol("h2");

    // SSL Connection Factory
    SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

    // Connector supporting HTTP/2, http1.1 and negotiation protocols
    ServerConnector http2Connector =
      new ServerConnector(server, ssl, alpn, h2, new HttpConnectionFactory(https_config, HttpCompliance.RFC2616));
    http2Connector.setPort(_sslPort);
    server.addConnector(http2Connector);


    return new ServerConnector[]{http2Connector};
  }
}
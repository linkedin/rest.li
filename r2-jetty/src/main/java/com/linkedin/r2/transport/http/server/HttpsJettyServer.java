/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.r2.transport.http.server;


import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;


/**
 * @author Ang Xu
 * @version $Revision: $
 */
public class HttpsJettyServer extends HttpJettyServer
{
  private final int _sslPort;
  private final String _keyStore;
  private final String _keyStorePassword;

  public HttpsJettyServer(int port,
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
  protected Connector[] getConnectors()
  {
    SslContextFactory sslContextFactory = new SslContextFactory();
    sslContextFactory.setKeyStorePath(_keyStore);
    sslContextFactory.setKeyStorePassword(_keyStorePassword);
    sslContextFactory.setTrustStore(_keyStore);
    sslContextFactory.setTrustStorePassword(_keyStorePassword);

    Connector sslConnector = new SslSelectChannelConnector(sslContextFactory);
    sslConnector.setPort(_sslPort);

    Connector[] httpConnectors = super.getConnectors();
    Connector[] connectors = new Connector[httpConnectors.length + 1];
    int i  = 0;
    for (Connector c : httpConnectors)
    {
      connectors[i++] = c;
    }
    connectors[i++] = sslConnector;

    return connectors;
  }
}

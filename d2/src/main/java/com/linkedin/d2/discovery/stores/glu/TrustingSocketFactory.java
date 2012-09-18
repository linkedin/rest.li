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

package com.linkedin.d2.discovery.stores.glu;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClientError;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

/**
 * Copy and paste from HttpClient's contrib folder (EasyProtocolSocketFactory). This
 * completely disables SSL verification, so that bad certificates will be allowed. Useful
 * for non-production testing.
 *
 * @author criccomini
 *
 */
public class TrustingSocketFactory implements SecureProtocolSocketFactory
{

  private SSLContext sslcontext = null;

  public TrustingSocketFactory()
  {
    super();
  }

  private static SSLContext createEasySSLContext()
  {
    try
    {
      SSLContext context = SSLContext.getInstance("SSL");
      context.init(null, new TrustManager[] { new X509TrustManager()
      {
        public java.security.cert.X509Certificate[] getAcceptedIssuers()
        {
          return null;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
                                       String authType)
        {
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
                                       String authType)
        {
        }
      } }, null);
      return context;
    }
    catch (Exception e)
    {
      throw new HttpClientError(e.toString());
    }
  }

  private SSLContext getSSLContext()
  {
    if (this.sslcontext == null)
    {
      this.sslcontext = createEasySSLContext();
    }
    return this.sslcontext;
  }

  public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException,
      UnknownHostException
  {

    return getSSLContext().getSocketFactory().createSocket(host,
                                                           port,
                                                           clientHost,
                                                           clientPort);
  }

  public Socket createSocket(final String host,
                             final int port,
                             final InetAddress localAddress,
                             final int localPort,
                             final HttpConnectionParams params) throws IOException,
      UnknownHostException,
      ConnectTimeoutException
  {
    if (params == null)
    {
      throw new IllegalArgumentException("Parameters may not be null");
    }
    int timeout = params.getConnectionTimeout();
    SocketFactory socketfactory = getSSLContext().getSocketFactory();
    if (timeout == 0)
    {
      return socketfactory.createSocket(host, port, localAddress, localPort);
    }
    else
    {
      Socket socket = socketfactory.createSocket();
      SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
      SocketAddress remoteaddr = new InetSocketAddress(host, port);
      socket.bind(localaddr);
      socket.connect(remoteaddr, timeout);
      return socket;
    }
  }

  public Socket createSocket(String host, int port) throws IOException,
      UnknownHostException
  {
    return getSSLContext().getSocketFactory().createSocket(host, port);
  }

  public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException,
      UnknownHostException
  {
    return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
  }
}

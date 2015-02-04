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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;

/**
 * Copy and paste from HttpClient's contrib folder (EasyProtocolSocketFactory). This
 * completely disables SSL verification, so that bad certificates will be allowed. Useful
 * for non-production testing.
 *
 * @author criccomini
 *
 */
public class TrustingSocketFactory implements LayeredConnectionSocketFactory
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
      throw new RuntimeException(e);
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

  public Socket createLayeredSocket(Socket socket, String target, int port, HttpContext context) throws IOException
  {
    return getSSLContext().getSocketFactory().createSocket(socket, target, port, false);
  }

  public Socket createSocket(HttpContext context) throws IOException
  {
    return getSSLContext().getSocketFactory().createSocket();
  }

  public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException
  {
    SocketFactory socketfactory = getSSLContext().getSocketFactory();
    if (connectTimeout == 0)
    {
      return socketfactory.createSocket(host.getHostName(), host.getPort(), localAddress.getAddress(), localAddress.getPort());
    }
    else
    {
      Socket socket = socketfactory.createSocket();
      SocketAddress localaddr = new InetSocketAddress(localAddress.getAddress(), localAddress.getPort());
      SocketAddress remoteaddr = new InetSocketAddress(remoteAddress.getAddress(), remoteAddress.getPort());
      socket.bind(localaddr);
      socket.connect(remoteaddr, connectTimeout);
      return socket;
    }
  }
}

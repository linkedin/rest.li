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

package test.r2.integ;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.sample.echo.EchoService;
import com.linkedin.r2.sample.echo.rest.RestEchoClient;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.Server;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


/**
 * @author Ang Xu
 */
public class TestHttpsEcho extends AbstractEchoServiceTest
{
  // A self-signed server certificate. DO NOT use it outside integration test!!!
  private final String keyStore = getClass().getClassLoader().getResource("keystore").getPath();
  private final String keyStorePassword = "password";

  private static final int PORT = 11990;

  private final int _port;
  private final boolean _clientROS;
  private final boolean _serverROS;

  @Factory(dataProvider = "configs")
  public TestHttpsEcho(boolean clientROS, boolean serverROS, int port)
  {
    _port = port;
    _clientROS = clientROS;
    _serverROS = serverROS;
  }

  @DataProvider
  public static Object[][] configs()
  {
    return new Object[][] {
        {true, true, PORT},
        {true, false, PORT + 1},
        {false, true, PORT + 2},
        {false, false, PORT + 3}
    };
  }


  @Override
  protected EchoService getEchoClient(Client client, URI uri)
  {
    return new RestEchoClient(Bootstrap.createHttpsURI(_port, uri), client);
  }

  @Override
  protected Client createClient(FilterChain filters) throws Exception
  {
    final Map<String, Object> properties = new HashMap<String, Object>();

    //load the keystore
    KeyStore certKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    certKeyStore.load(new FileInputStream(keyStore), keyStorePassword.toCharArray());

    //set KeyManger to use X509
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(certKeyStore, keyStorePassword.toCharArray());

    //use a standard trust manager and load server certificate
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
    tmf.init(certKeyStore);

    //set context to TLS and initialize it
    SSLContext context = SSLContext.getInstance("TLS");
    context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

    properties.put(HttpClientFactory.HTTP_SSL_CONTEXT, context);
    properties.put(HttpClientFactory.HTTP_SSL_PARAMS, context.getDefaultSSLParameters());

    final TransportClient client = new HttpClientFactory.Builder()
        .setFilterChain(filters)
        .build()
        .getClient(properties);
    return new TransportClientAdapter(client, _clientROS);
  }

  @Override
  protected Server createServer(FilterChain filters)
  {
    return Bootstrap.createHttpsServer(_port, keyStore, keyStorePassword, filters, _serverROS);
  }

  /**
   * Test that https-enabled server and client can speak plain HTTP as well.
   */
  @Test
  public void testHttpEcho() throws Exception
  {
    final EchoService client = new RestEchoClient(Bootstrap.createHttpURI(Bootstrap.getEchoURI()), _client);

    final String msg = "This is a simple http echo message";
    final FutureCallback<String> callback = new FutureCallback<String>();
    client.echo(msg, callback);

    Assert.assertEquals(callback.get(), msg);
  }

}

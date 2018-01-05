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

package test.r2.integ;

import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.sample.echo.rest.RestEchoClient;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.Server;
import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.testng.annotations.DataProvider;


/**
 * @author Ang Xu
 */
abstract public class AbstractTestHttps extends AbstractEchoServiceTest
{
  // A self-signed server certificate. DO NOT use it outside integration test!!!
  private final String keyStore = getClass().getClassLoader().getResource("keystore").getPath();
  private final String keyStorePassword = "password";

  private static final int PORT = 11990;

  final int _port;
  final boolean _clientROS;
  final boolean _serverROS;

  public AbstractTestHttps(boolean clientROS, boolean serverROS, int port)
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
  protected RestEchoClient getEchoClient(Client client, URI uri)
  {
    return new RestEchoClient(Bootstrap.createHttpsURI(_port, uri), client);
  }

  protected SSLContext getContext() throws Exception
  {
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
    return context;
  }

  @Override
  protected Client createClient(FilterChain filters) throws Exception
  {
    return Bootstrap.createHttpsClient(filters, _clientROS, getContext(), null);
  }

  @Override
  protected Server createServer(FilterChain filters) throws Exception
  {
    return Bootstrap.createHttpsServer(_port, keyStore, keyStorePassword, filters, _serverROS);
  }
}
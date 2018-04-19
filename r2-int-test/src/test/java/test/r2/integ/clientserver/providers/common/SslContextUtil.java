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

package test.r2.integ.clientserver.providers.common;

import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

public final class SslContextUtil
{
  // A self-signed server certificate. DO NOT use it outside integration test!!!
  public static final String KEY_STORE = SslContextUtil.class.getClassLoader().getResource("keystore").getPath();
  public static final String KEY_STORE_PASSWORD = "password";

  private static final String ENDPOINT_IDENTIFICATION_ALGORITHM = "HTTPS";

  private static final String[] CIPHER_SUITE = {"TLS_RSA_WITH_AES_128_CBC_SHA256"};
  private static final String[] PROTOCOLS = {"TLSv1.2"};

  private static final int HTTPS_TO_HTTP_PORT_SPAN = 1000;

  public static SSLContext getContext() throws Exception
  {
    //load the keystore
    KeyStore certKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    certKeyStore.load(new FileInputStream(KEY_STORE), KEY_STORE_PASSWORD.toCharArray());

    //set KeyManger to use X509
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(certKeyStore, KEY_STORE_PASSWORD.toCharArray());

    //use a standard trust manager and load server certificate
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
    tmf.init(certKeyStore);

    //set context to TLS and initialize it
    SSLContext context = SSLContext.getInstance("TLS");
    context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    return context;
  }

  public static SSLParameters getSSLParameters()
  {
    SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm(ENDPOINT_IDENTIFICATION_ALGORITHM);
    sslParameters.setCipherSuites(CIPHER_SUITE);
    sslParameters.setProtocols(PROTOCOLS);
    return sslParameters;
  }

  public static int getHttpPortFromHttps(int httpsPort)
  {
    return httpsPort + HTTPS_TO_HTTP_PORT_SPAN;
  }

}

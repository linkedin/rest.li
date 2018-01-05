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

import com.linkedin.r2.transport.http.util.SslHandlerUtil;
import io.netty.handler.ssl.SslHandler;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.internal.junit.ArrayAsserts;


/**
 * @author Sean Sheng
 */
public class TestSslHandlerUtil
{
  private static final String[] CIPHER_SUITE_WHITELIST = {
      "TLS_RSA_WITH_AES_128_CBC_SHA256",
      "TLS_RSA_WITH_AES_128_GCM_SHA256",
      "TLS_RSA_WITH_AES_128_CBC_SHA",
      "SSL_RSA_WITH_NULL_MD5",
      "SSL_RSA_WITH_NULL_SHA"
  };

  private static final String[] PROTOCOLS = {
      "TLSv1.2"
  };

  private static final String ENDPOINT_IDENTIFICATION_ALGORITHM = "HTTPS";
  private static final boolean NEED_CLIENT_AUTH = true;

  @Test
  public void testGetSslHandler() throws Exception
  {
    final SSLContext sslContext = SSLContext.getDefault();
    final SSLParameters sslParameters = sslContext.getDefaultSSLParameters();

    sslParameters.setCipherSuites(CIPHER_SUITE_WHITELIST);
    sslParameters.setEndpointIdentificationAlgorithm(ENDPOINT_IDENTIFICATION_ALGORITHM);
    sslParameters.setNeedClientAuth(NEED_CLIENT_AUTH);
    sslParameters.setProtocols(PROTOCOLS);

    final SslHandler sslHandler = SslHandlerUtil.getClientSslHandler(sslContext, sslParameters);
    Assert.assertNotNull(sslHandler);

    final SSLEngine sslEngine = sslHandler.engine();
    Assert.assertNotNull(sslEngine);
    Assert.assertEquals(sslEngine.getSSLParameters().getEndpointIdentificationAlgorithm(), ENDPOINT_IDENTIFICATION_ALGORITHM);
    Assert.assertEquals(sslEngine.getSSLParameters().getNeedClientAuth(), NEED_CLIENT_AUTH);
    ArrayAsserts.assertArrayEquals(sslEngine.getSSLParameters().getCipherSuites(), CIPHER_SUITE_WHITELIST);
    ArrayAsserts.assertArrayEquals(sslEngine.getSSLParameters().getProtocols(), PROTOCOLS);
  }
}

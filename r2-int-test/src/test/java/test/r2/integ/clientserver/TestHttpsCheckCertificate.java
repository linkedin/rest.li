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

package test.r2.integ.clientserver;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.sample.echo.rest.RestEchoClient;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionNotTrustedException;
import com.linkedin.r2.transport.http.client.common.ssl.SslSessionValidator;
import com.linkedin.test.util.ExceptionTestUtil;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSession;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractEchoServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;

/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class TestHttpsCheckCertificate extends AbstractEchoServiceTest
{

  @Factory(dataProvider = "allHttps", dataProviderClass = ClientServerConfiguration.class)
  public TestHttpsCheckCertificate(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }

  @Test
  public void testHttpsEchoWithUnvalidSession()
  {
    try
    {
      testHttpsEchoWithSessionValidator(sslSession -> {
        throw new SslSessionNotTrustedException();
      });
      Assert.fail("Certificate was trusted even if it wasn't supped to be");
    }
    catch (Exception e)
    {
      ExceptionTestUtil.verifyCauseChain(e, RemoteInvocationException.class, SslSessionNotTrustedException.class);
    }
  }

  @Test
  public void testHttpsEchoWithValidSession() throws Exception
  {
    testHttpsEchoWithSessionValidator(SSLSession::isValid);
  }

  /**
   * If the user doesn't specify a session validator, anything is allowed and the requests should simply succeed
   */
  @Test
  public void testHttpsEchoWithNoSessioValidator() throws Exception
  {
    testHttpsEchoWithSessionValidator(null);
  }

  private void testHttpsEchoWithSessionValidator(SslSessionValidator sslSessionValidator) throws Exception
  {
    final RestEchoClient client = getEchoClient(_client, Bootstrap.getEchoURI());

    final String msg = "This is a simple echo message";
    final FutureCallback<String> callback = new FutureCallback<>();
    RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.REQUESTED_SSL_SESSION_VALIDATOR, sslSessionValidator);
    client.echo(msg, requestContext, callback);

    String actual = callback.get(20, TimeUnit.SECONDS);
    Assert.assertEquals(actual, msg);
  }

}
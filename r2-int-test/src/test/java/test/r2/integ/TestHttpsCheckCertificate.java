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

package test.r2.integ;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.sample.echo.rest.RestEchoClient;
import com.linkedin.r2.transport.http.client.common.ServerCertPrincipalNameMismatchException;
import com.linkedin.test.util.ExceptionTestUtil;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 * @author Francesco Capponi (fcapponi@linkedin.com)
 */
public class TestHttpsCheckCertificate extends AbstractTestHttps
{

  @Factory(dataProvider = "configs")
  public TestHttpsCheckCertificate(boolean clientROS, boolean serverROS, int port)
  {
    super(clientROS, serverROS, port);
  }

  @Test
  public void testHttpsEchoWithWrongCertPrincipal() throws Exception
  {
    try
    {
      testHttpsEchoWithCertPrincipal("WRONG CERTIFICATE NAME");
      Assert.fail();
    }
    catch (Exception e)
    {
      ExceptionTestUtil.verifyCauseChain(e, RemoteInvocationException.class, ServerCertPrincipalNameMismatchException.class);
    }
  }

  @Test
  public void testHttpsEchoWithCorrectCertPrincipal() throws Exception
  {
    testHttpsEchoWithCertPrincipal("CN=com.linkedin.r2,OU=r2-int-test,O=LinkedIn,L=Unknown,ST=Unknown,C=Unknown");
  }

  /**
   * If the user doesn't specify a principal cert name, anything is allowed and the requests should simply succeed
   */
  @Test
  public void testHttpsEchoWithNoCertPrincipal() throws Exception
  {
    testHttpsEchoWithCertPrincipal(null);
  }

  private void testHttpsEchoWithCertPrincipal(String principalName) throws Exception
  {
    final RestEchoClient client = getEchoClient(_client, Bootstrap.getEchoURI());

    final String msg = "This is a simple echo message";
    final FutureCallback<String> callback = new FutureCallback<>();
    RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.EXPECTED_SERVER_CERT_PRINCIPAL_NAME, principalName);
    client.echo(msg, requestContext, callback);

    String actual = callback.get(2, TimeUnit.SECONDS);
    Assert.assertEquals(actual, msg);
  }

}
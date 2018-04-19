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

package test.r2.integ.clientserver;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.sample.echo.EchoService;
import com.linkedin.r2.sample.echo.rest.RestEchoClient;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractEchoServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.common.SslContextUtil;
import test.r2.integ.clientserver.providers.server.ServerProvider;


/**
 * @author Ang Xu
 */
public class TestHttpsEcho extends AbstractEchoServiceTest
{

  @Factory(dataProvider = "allHttps", dataProviderClass = ClientServerConfiguration.class)
  public TestHttpsEcho(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }

  /**
   * Test that https-enabled server and client can speak plain HTTP as well.
   */
  @Test
  public void testHttpEcho() throws Exception
  {
    final EchoService client = new RestEchoClient(
      Bootstrap.createURI(SslContextUtil.getHttpPortFromHttps(_port), Bootstrap.getEchoURI(), false), createClient());

    final String msg = "This is a simple http echo message";
    final FutureCallback<String> callback = new FutureCallback<String>();
    client.echo(msg, callback);

    Assert.assertEquals(callback.get(), msg);
  }

}
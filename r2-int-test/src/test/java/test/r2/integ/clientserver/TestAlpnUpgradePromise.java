/*
   Copyright (c) 2019 LinkedIn Corp.

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
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.sample.echo.EchoService;
import com.linkedin.r2.sample.echo.rest.RestEchoClient;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractEchoServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.client.Http2ClientProvider;
import test.r2.integ.clientserver.providers.client.Https2ClientProvider;
import test.r2.integ.clientserver.providers.server.Http1JettyServerProvider;
import test.r2.integ.clientserver.providers.server.Http2JettyServerProvider;
import test.r2.integ.clientserver.providers.server.Https1JettyServerProvider;
import test.r2.integ.clientserver.providers.server.Https2JettyServerProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;

/**
 * @author Nizar Mankulangara
 */
public class TestAlpnUpgradePromise extends AbstractEchoServiceTest
{
  @Factory(dataProvider = "allMixedCombinations", dataProviderClass = ClientServerConfiguration.class)
  public TestAlpnUpgradePromise(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }

  @Test
  public void testClearTextAndAlpn() throws Exception
  {
    if (isClearTextUpgradeFailureCombination())
    {
      testClearTestUpgradeFailure();
    }
    else if(isAlpnFailureCombination())
    {
      testAlpnFailure();
    }
    else if(isValidClearTextOrAlpnCombination())
    {
      testClientMessageEcho();
    }
  }

  private void testClearTestUpgradeFailure()
  {
    final EchoService client = new RestEchoClient(
        Bootstrap.createURI(_port, Bootstrap.getEchoURI(), _serverProvider.isSsl()), _client);

    final String msg = "This is a simple http echo message";
    final FutureCallback<String> callback = new FutureCallback<>();

    try
    {
      client.echo(msg, callback);
      Assert.assertEquals(callback.get(), msg);
      Assert.fail("Should not have reached here !");
    }
    catch (Exception ex)
    {
      Throwable throwable = ExceptionUtils.getRootCause(ex);
      Assert.assertTrue(throwable instanceof IllegalStateException);
      Assert.assertEquals(throwable.getMessage(), "HTTP/2 clear text upgrade failed");
    }
  }

  public void testAlpnFailure() throws Exception
  {
    final EchoService client = new RestEchoClient(
        Bootstrap.createURI(_port, Bootstrap.getEchoURI(), _serverProvider.isSsl()), _client);

    final String msg = "This is a simple http echo message";
    final FutureCallback<String> callback = new FutureCallback<>();

    try
    {
      client.echo(msg, callback);
      Assert.assertEquals(callback.get(), msg);
      Assert.fail("Should not have reached here !");
    }
    catch (Exception ex)
    {
      Throwable throwable = ExceptionUtils.getRootCause(ex);
      Assert.assertTrue(throwable instanceof IllegalStateException);
      Assert.assertEquals(throwable.getMessage(), "Unsupported protocol 'http/1.1' is negotiated.");
    }
  }

  public void testClientMessageEcho() throws Exception
  {
    final EchoService client = new RestEchoClient(
        Bootstrap.createURI(_port, Bootstrap.getEchoURI(), _serverProvider.isSsl()), _client);

    final String msg = "This is a simple http echo message";
    final FutureCallback<String> callback = new FutureCallback<>();
    client.echo(msg, callback);

    Assert.assertEquals(callback.get(), msg);
  }

  private boolean isAlpnFailureCombination()
  {
    return _clientProvider.getUsePipelineV2() &&
        _clientProvider instanceof Https2ClientProvider &&
        _serverProvider instanceof Https1JettyServerProvider;
  }

  private boolean isClearTextUpgradeFailureCombination()
  {
    return _clientProvider.getUsePipelineV2() &&
        _clientProvider instanceof Http2ClientProvider &&
        _serverProvider instanceof Http1JettyServerProvider;
  }

  private boolean isValidClearTextOrAlpnCombination()
  {
    if (!_clientProvider.getUsePipelineV2())
    {
      return false;
    }

    if (_clientProvider instanceof Https2ClientProvider &&
        _serverProvider instanceof Https2JettyServerProvider)
    {
      return true;
    }

    return _clientProvider instanceof Http2ClientProvider && _serverProvider instanceof Http2JettyServerProvider;
  }
}

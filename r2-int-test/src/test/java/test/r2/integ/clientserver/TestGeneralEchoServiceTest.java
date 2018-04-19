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

/**
 * $Id: $
 */

package test.r2.integ.clientserver;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.sample.echo.EchoService;
import com.linkedin.r2.sample.echo.rest.RestEchoClient;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractEchoServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */
public class TestGeneralEchoServiceTest extends AbstractEchoServiceTest
{
  private final String _toServerKey = "to-server";
  private final String _toServerValue = "this value goes to the server";

  private final String _toClientKey = "to-client";
  private final String _toClientValue = "this value goes to the client";

  @Factory(dataProvider = "allCombinations", dataProviderClass = ClientServerConfiguration.class)
  public TestGeneralEchoServiceTest(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }

  @Test
  public void testEcho() throws Exception
  {
    final EchoService client = getEchoClient(_client, Bootstrap.getEchoURI());

    final String msg = "This is a simple echo message";
    final FutureCallback<String> callback = new FutureCallback<String>();
    client.echo(msg, callback);

    String actual = callback.get();
    Assert.assertEquals(actual, msg);
    Assert.assertEquals(_clientLengthFilter.getRequestEntityLength(), msg.length());
    Assert.assertEquals(_clientLengthFilter.getResponseEntityLength(), msg.length());
    Assert.assertEquals(_serverLengthFilter.getRequestEntityLength(), msg.length());
    Assert.assertEquals(_serverLengthFilter.getResponseEntityLength(), msg.length());

  }

  @Test
  public void testUnknownServiceUri()
  {
    final EchoService client = getEchoClient(_client, URI.create("/unknown-service"));

    final String msg = "This is a simple echo message";
    final FutureCallback<String> callback = new FutureCallback<String>();
    client.echo(msg, callback);

    try
    {
      callback.get();
      Assert.fail("Should have thrown an exception");
    }
    catch (Exception e)
    {
      // expected
    }
  }

  @Test(enabled = false)
  public void testBadRestURI()
  {
    final EchoService client = getEchoClient(_client, URI.create("/unknown-service"));
    if (!(client instanceof RestEchoClient))
    {
      return;
    }

    final String msg = "This is a simple echo message";
    final FutureCallback<String> callback = new FutureCallback<String>();
    client.echo(msg, callback);

    try
    {
      callback.get();
      Assert.fail("Should have thrown an exception");
    }
    catch (Exception e)
    {
      Assert.assertTrue(e instanceof ExecutionException);
      Assert.assertTrue(e.getCause() instanceof RestException);
      RestException re = (RestException) e.getCause();
      Assert.assertEquals(re.getResponse().getStatus(), RestStatus.NOT_FOUND);
    }
  }

  @Test
  public void testThrowingEchoService() throws Exception
  {
    final EchoService client = getEchoClient(_client, Bootstrap.getThrowingEchoURI());

    final String msg = "This is a simple echo message";
    final FutureCallback<String> callback = new FutureCallback<String>();
    client.echo(msg, callback);

    try
    {
      callback.get();
      Assert.fail("Should have thrown an exception");
    }
    catch (ExecutionException e)
    {
      Assert.assertTrue(e.getCause() instanceof RemoteInvocationException);
    }
  }

  @Test
  public void testOnExceptionEchoService() throws Exception
  {
    final EchoService client = getEchoClient(_client, Bootstrap.getOnExceptionEchoURI());

    final String msg = "This is a simple echo message";
    final FutureCallback<String> callback = new FutureCallback<String>();
    client.echo(msg, callback);

    try
    {
      callback.get();
      Assert.fail("Should have thrown an exception");
    }
    catch (ExecutionException e)
    {
      Assert.assertTrue(e.getCause() instanceof RemoteInvocationException);
    }
  }

  @Test
  public void testFilterChain() throws Exception
  {
    final EchoService client = getEchoClient(_client, Bootstrap.getEchoURI());

    final String msg = "This is a simple echo message";
    final FutureCallback<String> callback = new FutureCallback<String>();

    client.echo(msg, callback);
    callback.get();

    // Make sure the server got its wire attribute
    Assert.assertEquals(_serverCaptureFilter.getRequest().get(_toServerKey), _toServerValue);

    Assert.assertEquals(_serverCaptureFilter.getResponse().get(_toClientKey), _toClientValue);

    // Make sure the client got its wire attribute, but not the server's wire attribute
    Assert.assertEquals(_clientCaptureFilter.getResponse().get(_toClientKey), _toClientValue);
    Assert.assertNull(_clientCaptureFilter.getResponse().get(_toServerKey));
  }

  @Test
  public void testFilterChainOnException() throws Exception
  {
    final EchoService client = getEchoClient(_client, URI.create("/unknown-service"));

    final String msg = "This is a simple echo message";
    final FutureCallback<String> callback = new FutureCallback<String>();

    client.echo(msg, callback);
    try
    {
      callback.get();
      Assert.fail("Should have thrown an exception");
    }
    catch (Exception e)
    {
      // expected
    }

    // Make sure the server got its wire attribute
    Assert.assertEquals(_serverCaptureFilter
      .getRequest()
      .get(_toServerKey), _toServerValue);

    // Make sure the client got its wire attribute, but not the server's wire attribute
    Assert.assertEquals(_clientCaptureFilter.getResponse().get(_toClientKey), _toClientValue);
    Assert.assertNull(_clientCaptureFilter.getResponse().get(_toServerKey));
  }

}

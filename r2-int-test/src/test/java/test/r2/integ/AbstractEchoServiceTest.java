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

package test.r2.integ;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.sample.echo.EchoService;
import com.linkedin.r2.sample.echo.rest.RestEchoClient;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.Server;
import com.linkedin.common.util.None;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.concurrent.ExecutionException;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public abstract class AbstractEchoServiceTest
{
  private final String _toServerKey = "to-server";
  private final String _toServerValue = "this value goes to the server";

  private final String _toClientKey = "to-client";
  private final String _toClientValue = "this value goes to the client";

  protected Client _client;

  private Server _server;

  private CaptureWireAttributesFilter _serverCaptureFilter;
  private CaptureWireAttributesFilter _clientCaptureFilter;

  @BeforeClass
  protected void setUp() throws Exception
  {
    _serverCaptureFilter = new CaptureWireAttributesFilter();
    _clientCaptureFilter = new CaptureWireAttributesFilter();

    final FilterChain serverFilters = FilterChains.empty()
            .addFirst(_serverCaptureFilter)
            .addLast(new SendWireAttributeFilter(_toClientKey, _toClientValue, false));

    final FilterChain clientFilters = FilterChains.empty()
            .addFirst(_clientCaptureFilter)
            .addLast(new SendWireAttributeFilter(_toServerKey, _toServerValue, true));

    _client = createClient(clientFilters);

    _server = createServer(serverFilters);
    _server.start();
  }

  @AfterClass
  protected void tearDown() throws Exception
  {
    final FutureCallback<None> callback = new FutureCallback<None>();
    _client.shutdown(callback);

    try
    {
      callback.get();
    }
    finally
    {
      if (_server != null)
      {
        _server.stop();
        _server.waitForStop();
      }
    }
  }

  @Test
  public void testEcho() throws Exception
  {
    final EchoService client = getEchoClient(_client, Bootstrap.getEchoURI());

    final String msg = "This is a simple echo message";
    final FutureCallback<String> callback = new FutureCallback<String>();
    client.echo(msg, callback);

    Assert.assertEquals(callback.get(), msg);
  }

  @Test
  public void testUnknownServiceUri() throws Exception
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

  @Test
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
      RestException re = (RestException)e.getCause();
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
    Assert.assertEquals(_serverCaptureFilter.getRequest().get(_toServerKey), _toServerValue);

    // Make sure the client got its wire attribute, but not the server's wire attribute
    Assert.assertEquals(_clientCaptureFilter.getResponse().get(_toClientKey), _toClientValue);
    Assert.assertNull(_clientCaptureFilter.getResponse().get(_toServerKey));
  }

  protected abstract EchoService getEchoClient(Client client, URI uri);

  protected abstract Client createClient(FilterChain filters) throws Exception;

  protected abstract Server createServer(FilterChain filters);

}

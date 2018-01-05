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
import com.linkedin.r2.sample.echo.EchoServiceImpl;
import com.linkedin.r2.sample.echo.rest.RestEchoClient;
import com.linkedin.r2.sample.echo.rest.RestEchoServer;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.Server;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.server.HttpNettyServerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


/**
 * @author Dengpan Yin
 */
public class TestNettyHttpsEcho extends AbstractTestHttps
{
  @Factory(dataProvider = "configs")
  public TestNettyHttpsEcho(boolean clientROS, boolean serverROS, int port)
  {
    super(clientROS, serverROS, port);
  }

  @Override
  protected Server createServer(FilterChain filters) throws Exception
  {
    final SSLContext sslContext = getContext();

    return createServer(filters, sslContext, null);
  }

  protected Server createServer(FilterChain filters, SSLContext sslContext, SSLParameters sslParameters) throws Exception
  {
    final TransportDispatcher dispatcher = new TransportDispatcherBuilder()
        .addRestHandler(Bootstrap.getEchoURI(), new RestEchoServer(new EchoServiceImpl()))
        .build();

    return new HttpNettyServerFactory(filters)
        .createServer(_port, dispatcher, sslContext, sslParameters);
  }

  /**
   * SSL disabled Netty server is able to process http request from SSL enabled netty client.
   */
  @Test
  public void testInsecureServerProcessHttpRequestFromSecureClient() throws Exception
  {
    final EchoService service = new RestEchoClient(Bootstrap.createHttpURI(_port, Bootstrap.getEchoURI()), _client);

    if (_server != null)
    {
      _server.stop();
      _server.waitForStop();
    }
    _server = createServer(getServerFilters(), null, null);
    _server.start();

    final FutureCallback<String> callback = new FutureCallback<String>();
    service.echo(ECHO_MSG, callback);

    final String actual = callback.get();
    Assert.assertEquals(actual, ECHO_MSG);
  }

  /**
   * SSL disabled Netty server is unable to process https request from SSL enabled netty client
   */
  @Test
  public void testInsecureServerProcessHttpsRequestFromSecureClient() throws Exception
  {
    final EchoService service = new RestEchoClient(Bootstrap.createHttpsURI(_port, Bootstrap.getEchoURI()), _client);

    if (_server != null)
    {
      _server.stop();
      _server.waitForStop();
    }
    _server = createServer(getServerFilters(), null, null);
    _server.start();

    final FutureCallback<String> callback = new FutureCallback<String>();
    service.echo(ECHO_MSG, callback);

    try
    {
      callback.get();
      Assert.fail("Should have thrown an exception.");
    }
    catch (Exception e)
    {
      // expected
      System.out.println("The exception is expected.");
    }
  }

  /**
   * SSL enabled Netty server is unable to process http request from SSL disabled netty client.
   */
  @Test
  public void testSecureServerProcessHttpRequestFromInsecureClient() throws Exception
  {
    final Client client = Bootstrap.createHttpClient(getClientFilters(), _clientROS);
    final EchoService service = new RestEchoClient(Bootstrap.createHttpURI(_port, Bootstrap.getEchoURI()), client);

    final FutureCallback<String> callback = new FutureCallback<String>();
    service.echo(ECHO_MSG, callback);

    try
    {
      callback.get();
      Assert.fail("Should have thrown an exception.");
    }
    catch (Exception e)
    {
      // expected
      System.out.println("The exception is expected.");
    }
  }

  /**
   * SSL enabled Netty server is unable to process https request from SSL disabled netty client.
   */
  @Test
  public void testSecureServerProcessHttpsRequestFromInsecureClient() throws Exception
  {
    final Client client = Bootstrap.createHttpClient(getClientFilters(), _clientROS);
    final EchoService service = new RestEchoClient(Bootstrap.createHttpsURI(_port, Bootstrap.getEchoURI()), client);

    final FutureCallback<String> callback = new FutureCallback<String>();
    service.echo(ECHO_MSG, callback);

    try
    {
      callback.get();
      Assert.fail("Should have thrown an exception.");
    }
    catch (Exception e)
    {
      // expected
      System.out.println("The exception is expected.");
    }
  }
}
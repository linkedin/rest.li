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

package test.r2.integ;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.r2.RemoteInvocationException;
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
import com.linkedin.r2.transport.http.server.HttpNettyServerBuilder;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import java.net.URI;


/**
 * @author Dengpan Yin
 */
public class TestNettyHttpsEcho extends AbstractTestHttps
{
  private static final int TEST_CASE_TIME_OUT = 10000;
  private static final int CALL_BACK_TIME_OUT = 1000;

  @Factory(dataProvider = "configs")
  public TestNettyHttpsEcho(boolean clientROS, boolean serverROS, int port)
  {
    super(clientROS, serverROS, port);
  }

  @Override
  protected Server createServer(FilterChain filters) throws Exception
  {
    return createHttpsServer(filters, _port);
  }

  protected TransportDispatcher getTransportDispatcher()
  {
    return new TransportDispatcherBuilder()
        .addRestHandler(Bootstrap.getEchoURI(), new RestEchoServer(new EchoServiceImpl()))
        .build();
  }

  protected Server createHttpServer(FilterChain filters, int port) throws Exception
  {
    final TransportDispatcher dispatcher = getTransportDispatcher();

    return new HttpNettyServerBuilder().filters(filters).port(port).transportDispatcher(dispatcher).build();
  }

  protected Server createHttpsServer(FilterChain filters, int port) throws Exception
  {
    final TransportDispatcher dispatcher = getTransportDispatcher();

    return new HttpNettyServerBuilder()
        .port(port)
        .filters(filters)
        .transportDispatcher(dispatcher)
        .sslContext(getContext()).build();
  }

  /**
   * SSL disabled Netty server is able to process http request from SSL enabled netty client.
   */
  @Test(timeOut = TEST_CASE_TIME_OUT)
  public void testInsecureServerProcessHttpRequestFromSecureClient() throws Exception
  {
    testInsecureServerProcessRequestFromSecureClient(true);
  }

  /**
   * SSL disabled Netty server is unable to process https request from SSL enabled netty client
   */
  @Test(timeOut = TEST_CASE_TIME_OUT)
  public void testInsecureServerProcessHttpsRequestFromSecureClient() throws Exception
  {
    testInsecureServerProcessRequestFromSecureClient(false);
  }

  /**
   * SSL enabled Netty server is unable to process http request from SSL disabled netty client.
   */
  @Test(timeOut = TEST_CASE_TIME_OUT)
  public void testSecureServerProcessHttpRequestFromInsecureClient() throws Exception
  {
    testSecureServerProcessRequestFromInsecureClient(true);
  }

  /**
   * SSL enabled Netty server is unable to process https request from SSL disabled netty client.
   */
  @Test(timeOut = TEST_CASE_TIME_OUT)
  public void testSecureServerProcessHttpsRequestFromInsecureClient() throws Exception
  {
    testSecureServerProcessRequestFromInsecureClient(false);
  }

  private void testInsecureServerProcessRequestFromSecureClient(boolean httpUri) throws Exception
  {
    final FilterChain filters = getServerFilters();
    final Client client = createClient(filters);
    final int port = _port + 1;
    final URI uri = httpUri ? Bootstrap.createHttpURI(port, Bootstrap.getEchoURI()) : Bootstrap.createHttpsURI(port, Bootstrap.getEchoURI());
    final EchoService echoClient = new RestEchoClient(uri, client);
    final Server server = createHttpServer(filters, port);

    try
    {
      server.start();
      final FutureCallback<String> callback = new FutureCallback<String>();
      echoClient.echo(ECHO_MSG, callback);

      final String actual = callback.get(CALL_BACK_TIME_OUT, TimeUnit.MILLISECONDS);
      if (httpUri)
      {
        Assert.assertEquals(actual, ECHO_MSG);
      }
      else
      {
        Assert.fail("Should have thrown an exception.");
      }
    }
    catch (Exception e)
    {
      if (!httpUri)
      {
        Assert.assertTrue(e.getCause() instanceof RemoteInvocationException);
      }
      else
      {
        Assert.fail("Unexpected Exception:", e);
      }
    }
    finally
    {
      tearDown(client, server);
    }
  }

  private void testSecureServerProcessRequestFromInsecureClient(boolean httpUri) throws Exception
  {
    final FilterChain filters = getServerFilters();
    final Client client = Bootstrap.createHttpClient(filters, _clientROS);
    final int port = _port + 1;
    final URI uri = httpUri ? Bootstrap.createHttpURI(port, Bootstrap.getEchoURI()) : Bootstrap.createHttpsURI(port, Bootstrap.getEchoURI());
    final EchoService echoClient = new RestEchoClient(uri, client);
    final Server server = createHttpsServer(filters, port);

    try
    {
      server.start();
      final FutureCallback<String> callback = new FutureCallback<String>();
      echoClient.echo(ECHO_MSG, callback);

      callback.get(CALL_BACK_TIME_OUT, TimeUnit.MILLISECONDS);
      Assert.fail("Should have thrown an exception.");
    }
    catch (Exception e)
    {
      Assert.assertTrue(e.getCause() instanceof RemoteInvocationException);
    }
    finally
    {
      tearDown(client, server);
    }
  }
}
package test.r2.integ.clientserver;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.message.stream.StreamFilterAdapters;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import com.linkedin.r2.transport.http.server.HttpJettyServer;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractEchoServiceTest;
import test.r2.integ.clientserver.providers.AbstractServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;
import test.r2.integ.helper.CaptureWireAttributesFilter;
import test.r2.integ.helper.EchoHandler;
import test.r2.integ.helper.LogEntityLengthFilter;
import test.r2.integ.helper.SendWireAttributeFilter;


/**
 * @author Zhenkai Zhu
 * @author Nizar Mankulangara
 */
public class TestClientShutdown extends AbstractEchoServiceTest
{
  private static final URI ECHO_URI = URI.create("/echo");

  @Factory(dataProvider = "allCombinations", dataProviderClass = ClientServerConfiguration.class)
  public TestClientShutdown(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }


  @Override
  @AfterClass
  public void tearDown() throws Exception
  {
    // since _client is already shutdown as part of the test...we need to pass a dummy client for the teardown
    Client dummyClient = _clientProvider.createClient(getClientFilterChain());
    tearDown(dummyClient, _server);
  }


  @Test
  public void testShutdown() throws Exception
  {
    TransportClientFactory clientFactory = new HttpClientFactory.Builder().build();

    RestRequestBuilder builder = new RestRequestBuilder(_clientProvider.createHttpURI(_port, ECHO_URI));
    byte[] content = new byte[100];
    builder.setEntity(content);
    Future<RestResponse> future = _client.restRequest(builder.build());
    RestResponse response = future.get(30, TimeUnit.SECONDS);
    Assert.assertEquals(response.getEntity().copyBytes(), content);

    final FutureCallback<None> clientShutdownCallback = new FutureCallback<>();
    _client.shutdown(clientShutdownCallback);

    // we should catch those clients that do not shutdown properly in 5 seconds
    clientShutdownCallback.get(5000, TimeUnit.MILLISECONDS);

    final FutureCallback<None> factoryShutdownCallback = new FutureCallback<>();
    clientFactory.shutdown(factoryShutdownCallback);
    factoryShutdownCallback.get();
  }
}

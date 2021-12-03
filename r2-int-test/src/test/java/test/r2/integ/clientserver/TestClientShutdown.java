package test.r2.integ.clientserver;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractEchoServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;


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
    // since _client is already shutdown as part of the test...we need to pass a null for the client during tearDown
    tearDown(null, _server);
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

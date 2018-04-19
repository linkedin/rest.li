package test.r2.integ;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
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
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import test.r2.integ.helper.EchoHandler;

/**
 * @auther Zhenkai Zhu
 */
public class TestClientShutdown
{
  private static final int PORT = 10101;
  private static final URI ECHO_URI = URI.create("/echo");

  @DataProvider
  public static Object[][] configs()
  {
    return new Object[][]{
        { true, true, HttpProtocolVersion.HTTP_1_1.name() },
        { true, true, HttpProtocolVersion.HTTP_2.name() },
        { true, false, HttpProtocolVersion.HTTP_1_1.name() },
        { true, false, HttpProtocolVersion.HTTP_2.name() },
        { false, true, HttpProtocolVersion.HTTP_1_1.name() },
        { false, true, HttpProtocolVersion.HTTP_2.name() },
        { false, false, HttpProtocolVersion.HTTP_1_1.name() },
        { false, false, HttpProtocolVersion.HTTP_2.name() },
    };
  }

  @Test(dataProvider = "configs")
  public void testShutdown(boolean clientROS, boolean serverROS, String protocolVersion) throws Exception
  {
    TransportClientFactory clientFactory = new HttpClientFactory.Builder().build();
    Map<String, String> clientProperties = new HashMap<String, String>();
    // very long shutdown timeout
    clientProperties.put(HttpClientFactory.HTTP_SHUTDOWN_TIMEOUT, "60000");
    clientProperties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, protocolVersion);

    Client client = new TransportClientAdapter(clientFactory.getClient(clientProperties), clientROS);
    TransportDispatcher dispatcher = new TransportDispatcherBuilder().addRestHandler(ECHO_URI, new EchoHandler()).build();
    HttpServer server = new HttpServerFactory(HttpJettyServer.ServletType.RAP).createH2cServer(PORT, dispatcher, serverROS);
    try
    {
      server.start();

      RestRequestBuilder builder = new RestRequestBuilder(URI.create("http://localhost:" + PORT + ECHO_URI));
      byte[] content = new byte[100];
      builder.setEntity(content);
      Future<RestResponse> future = client.restRequest(builder.build());
      RestResponse response = future.get(30, TimeUnit.SECONDS);
      Assert.assertEquals(response.getEntity().copyBytes(), content);

      final FutureCallback<None> clientShutdownCallback = new FutureCallback<None>();
      client.shutdown(clientShutdownCallback);

      // we should catch those clients that do not shutdown properly in 5 seconds
      clientShutdownCallback.get(5000, TimeUnit.MILLISECONDS);

      final FutureCallback<None> factoryShutdownCallback = new FutureCallback<None>();
      clientFactory.shutdown(factoryShutdownCallback);
      factoryShutdownCallback.get();
    }
    finally
    {
      if (server != null) {
        server.stop();
        server.waitForStop();
      }
    }
  }
}

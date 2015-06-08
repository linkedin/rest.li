package test.r2.integ;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
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

/**
 * @auther Zhenkai Zhu
 */

public class TestClientShutdown
{
  private static final int PORT = 10101;
  private static final URI ECHO_URI = URI.create("/echo");
  private HttpServer _server;
  private TransportClientFactory _clientFactory;
  private Client _client;

  @DataProvider
  public static Object[][] configs()
  {
    return new Object[][] {{true, true}, {true, false}, {false, true}, {false, false}};
  }

  @Test(dataProvider = "configs")
  public void testShutdown(boolean clientROS, boolean serverROS) throws Exception
  {
    _clientFactory = new HttpClientFactory();
    Map<String, String> clientProperties = new HashMap<String, String>();
    // very long shutdown timeout
    clientProperties.put(HttpClientFactory.HTTP_SHUTDOWN_TIMEOUT, "60000");
    _client = new TransportClientAdapter(_clientFactory.getClient(clientProperties), clientROS);
    TransportDispatcher dispatcher = new TransportDispatcherBuilder().addRestHandler(ECHO_URI, new EchoHandler()).build();
    _server = new HttpServerFactory(HttpJettyServer.ServletType.RAP).createServer(PORT, dispatcher, serverROS);
    _server.start();

    RestRequestBuilder builder = new RestRequestBuilder(URI.create("http://localhost:" + PORT + ECHO_URI));
    byte[] content = new byte[100];
    builder.setEntity(content);
    Future<RestResponse> future = _client.restRequest(builder.build());
    RestResponse response = future.get(30, TimeUnit.SECONDS);
    Assert.assertEquals(response.getEntity().copyBytes(), content);

    final FutureCallback<None> clientShutdownCallback = new FutureCallback<None>();
    _client.shutdown(clientShutdownCallback);

    // we should catch those clients that do not shutdown properly in 5 seconds
    clientShutdownCallback.get(5000, TimeUnit.MILLISECONDS);

    final FutureCallback<None> factoryShutdownCallback = new FutureCallback<None>();
    _clientFactory.shutdown(factoryShutdownCallback);
    factoryShutdownCallback.get();

    if (_server != null) {
      _server.stop();
      _server.waitForStop();
    }
  }

  private static class EchoHandler implements RestRequestHandler
  {
    @Override
    public void handleRequest(RestRequest request, RequestContext requestContext, final Callback<RestResponse> callback)
    {
      RestResponseBuilder builder = new RestResponseBuilder();
      callback.onSuccess(builder.setEntity(request.getEntity()).build());
    }
  }
}

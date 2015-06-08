package test.r2.integ;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.Server;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandlerAdapter;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.server.TransportCallbackAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.server.HttpJettyServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Zhenkai Zhu
 */
public class TestQueryTunnel
{
  private static int PORT = 9003;
  private static int IS_TUNNELED_RESPONSE_CODE = 200;
  private static int IS_NOT_TUNNELED_RESPONSE_CODE = 201;
  private static int QUERY_TUNNEL_THRESHOLD = 8;
  private Client _client;
  private Server _server;
  private TransportClientFactory _clientFactory;

  private final boolean _clientROS;
  private final boolean _serverROS;
  private final HttpJettyServer.ServletType _servletType;
  private final int _port;

  @Factory(dataProvider = "configs")
  public TestQueryTunnel(boolean clientROS, boolean serverROS, HttpJettyServer.ServletType servletType, int port)
  {
    _clientROS = clientROS;
    _serverROS = serverROS;
    _servletType = servletType;
    _port = port;
  }

  @DataProvider
  public static Object[][] configs()
  {
    return new Object[][] {
        {true, true, HttpJettyServer.ServletType.RAP, PORT},
        {true, false, HttpJettyServer.ServletType.RAP, PORT + 1},
        {false, true, HttpJettyServer.ServletType.RAP, PORT + 2},
        {false, false, HttpJettyServer.ServletType.RAP, PORT + 3},
        {true, true, HttpJettyServer.ServletType.ASYNC_EVENT, PORT + 4},
        {true, false, HttpJettyServer.ServletType.ASYNC_EVENT, PORT + 5},
        {false, true, HttpJettyServer.ServletType.ASYNC_EVENT, PORT + 6},
        {false, false, HttpJettyServer.ServletType.ASYNC_EVENT, PORT + 7}
    };
  }

  @BeforeClass
  protected void setUp() throws Exception
  {
    Map<String, String> clientProperties = new HashMap<String, String>();
    clientProperties.put(HttpClientFactory.HTTP_QUERY_POST_THRESHOLD, String.valueOf(QUERY_TUNNEL_THRESHOLD));
    _clientFactory = new HttpClientFactory();
    final TransportClient transportClient = _clientFactory
        .getClient(clientProperties);

    _client = new TransportClientAdapter(transportClient, _clientROS);

    final RestRequestHandler restHandler = new CheckQueryTunnelHandler();
    final StreamRequestHandler streamHandler = new StreamRequestHandlerAdapter(restHandler);

    TransportDispatcher dispatcher = new TransportDispatcher()
    {
      @Override
      public void handleRestRequest(RestRequest req, Map<String, String> wireAttrs, RequestContext requestContext,
                                    TransportCallback<RestResponse> callback)
      {
        restHandler.handleRequest(req, requestContext, new TransportCallbackAdapter<RestResponse>(callback));
      }

      @Override
      public void handleStreamRequest(StreamRequest req, Map<String, String> wireAttrs,
                             RequestContext requestContext, TransportCallback<StreamResponse> callback)
      {
        streamHandler.handleRequest(req, requestContext, new TransportCallbackAdapter<StreamResponse>(callback));
      }
    };
    _server = new HttpServerFactory(_servletType).createServer(_port, dispatcher, _serverROS);
    _server.start();
  }

  @Test
  public void testShouldNotQueryTunnel() throws Exception
  {
    String shortQuery = buildQuery(QUERY_TUNNEL_THRESHOLD - 1);
    RestResponse response = getResponse(shortQuery, new RequestContext());
    Assert.assertEquals(response.getStatus(), IS_NOT_TUNNELED_RESPONSE_CODE);
    Assert.assertEquals(response.getEntity().copyBytes(), shortQuery.getBytes());

  }

  @Test
  public void testShouldQueryTunnel() throws Exception
  {
    String longQuery = buildQuery(QUERY_TUNNEL_THRESHOLD);
    RestResponse response = getResponse(longQuery, new RequestContext());
    Assert.assertEquals(response.getStatus(), IS_TUNNELED_RESPONSE_CODE);
    Assert.assertEquals(response.getEntity().copyBytes(), longQuery.getBytes());
  }

  @Test
  public void testForceQueryTunnel() throws Exception
  {
    String shortQuery = buildQuery(QUERY_TUNNEL_THRESHOLD - 1);
    RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.FORCE_QUERY_TUNNEL, true);
    RestResponse response = getResponse(shortQuery, requestContext);
    Assert.assertEquals(response.getStatus(), IS_TUNNELED_RESPONSE_CODE);
    Assert.assertEquals(response.getEntity().copyBytes(), shortQuery.getBytes());
  }

  private String buildQuery(int len)
  {
    StringBuilder builder = new StringBuilder("id=");
    for (int i = 0; i < len - 3; i++)
    {
      builder.append("a");
    }
    return builder.toString();
  }

  private RestResponse getResponse(String query, RequestContext requestContext) throws Exception
  {
    URI uri = URI.create("http://localhost:" + _port + "/checkQuery?" + query);
    RestRequestBuilder builder = new RestRequestBuilder(uri);
    return  _client.restRequest(builder.build(), requestContext).get(5000, TimeUnit.MILLISECONDS);
  }

  @AfterClass
  protected void tearDown() throws Exception
  {
    final FutureCallback<None> callback = new FutureCallback<None>();
    _client.shutdown(callback);

    callback.get();

    final FutureCallback<None> factoryCallback = new FutureCallback<None>();
    _clientFactory.shutdown(factoryCallback);
    factoryCallback.get();

    _server.stop();
    _server.waitForStop();
  }

  private class CheckQueryTunnelHandler implements RestRequestHandler
  {
    @Override
    public void handleRequest(RestRequest request, RequestContext requestContext, Callback<RestResponse> callback)
    {
      RestResponseBuilder builder = new RestResponseBuilder().setEntity(request.getURI().getRawQuery().getBytes());
      Object isQueryTunnel = requestContext.getLocalAttr(R2Constants.IS_QUERY_TUNNELED);
      if (isQueryTunnel != null && (Boolean) isQueryTunnel)
      {
        builder.setStatus(IS_TUNNELED_RESPONSE_CODE).build();
      }
      else
      {
        builder.setStatus(IS_NOT_TUNNELED_RESPONSE_CODE).build();
      }
      callback.onSuccess(builder.build());
    }
  }

}

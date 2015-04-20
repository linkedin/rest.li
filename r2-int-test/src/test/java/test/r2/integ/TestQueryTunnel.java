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
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.Server;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.server.TransportCallbackAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
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

  @BeforeClass
  protected void setUp() throws Exception
  {
    Map<String, String> clientProperties = new HashMap<String, String>();
    clientProperties.put(HttpClientFactory.HTTP_QUERY_POST_THRESHOLD, String.valueOf(QUERY_TUNNEL_THRESHOLD));
    _clientFactory = new HttpClientFactory();
    final TransportClient transportClient = _clientFactory
        .getClient(clientProperties);

    _client = new TransportClientAdapter(transportClient);

    final RestRequestHandler handler = new CheckQueryTunnelHandler();

    TransportDispatcher dispatcher = new TransportDispatcher()
    {
      @Override
      public void handleRestRequest(RestRequest req, Map<String, String> wireAttrs,
                             RequestContext requestContext, TransportCallback<RestResponse> callback)
      {
        handler.handleRequest(req, requestContext, new TransportCallbackAdapter<RestResponse>(callback));
      }
    };
    _server = new HttpServerFactory().createServer(PORT, dispatcher);
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
    URI uri = URI.create("http://localhost:" + PORT + "/checkQuery?" + query);
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

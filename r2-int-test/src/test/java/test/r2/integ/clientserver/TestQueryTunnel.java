package test.r2.integ.clientserver;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandlerAdapter;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.server.TransportCallbackAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;


/**
 * @author Zhenkai Zhu
 */
public class TestQueryTunnel extends AbstractServiceTest
{
  private static int IS_TUNNELED_RESPONSE_CODE = 200;
  private static int IS_NOT_TUNNELED_RESPONSE_CODE = 201;
  private static int QUERY_TUNNEL_THRESHOLD = 8;

  @Factory(dataProvider = "allCombinations", dataProviderClass = ClientServerConfiguration.class)
  public TestQueryTunnel(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
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
    System.out.println(_clientProvider.getClass().toString() + ":" + _serverProvider.getClass());
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
    URI uri =_clientProvider.createHttpURI( _port, new URI("/checkQuery?" + query));
    RestRequestBuilder builder = new RestRequestBuilder(uri);
    return  _client.restRequest(builder.build(), requestContext).get(5000, TimeUnit.MILLISECONDS);
  }

  @Override
  protected Map<String, Object> getHttpClientProperties()
  {
    Map<String, Object> clientProperties = new HashMap<>();
    clientProperties.put(HttpClientFactory.HTTP_QUERY_POST_THRESHOLD, String.valueOf(QUERY_TUNNEL_THRESHOLD));
    return clientProperties;
  }


  @Override
  protected TransportDispatcher getTransportDispatcher() {
    final RestRequestHandler restHandler = new CheckQueryTunnelHandler();
    final StreamRequestHandler streamHandler = new StreamRequestHandlerAdapter(restHandler);

    return new TransportDispatcher()
    {
      @Override
      public void handleRestRequest(RestRequest req, Map<String, String> wireAttrs, RequestContext requestContext,
          TransportCallback<RestResponse> callback)
      {
        restHandler.handleRequest(req, requestContext, new TransportCallbackAdapter<>(callback));
      }

      @Override
      public void handleStreamRequest(StreamRequest req, Map<String, String> wireAttrs,
          RequestContext requestContext, TransportCallback<StreamResponse> callback)
      {
        streamHandler.handleRequest(req, requestContext, new TransportCallbackAdapter<>(callback));
      }
    };
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

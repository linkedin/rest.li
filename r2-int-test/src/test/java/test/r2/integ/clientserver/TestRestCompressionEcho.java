package test.r2.integ.clientserver;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.compression.ClientCompressionFilter;
import com.linkedin.r2.filter.compression.EncodingType;
import com.linkedin.r2.filter.compression.ServerCompressionFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;

/**
 * @author Zhenkai Zhu
 * @author Nizar Mankulangara
 */
public class TestRestCompressionEcho extends AbstractServiceTest
{
  private static final int THRESHOLD = 4096;
  private static final boolean REST_OVER_STREAM = false;
  protected static final long LARGE_BYTES_NUM = THRESHOLD * THRESHOLD;
  protected static final long SMALL_BYTES_NUM = THRESHOLD - 1;
  private static final URI ECHO_URI = URI.create("/echo");


  protected final RestFilter _compressionFilter = new ServerCompressionFilter(EncodingType.values(), new CompressionConfig(THRESHOLD));

  private List<Client> _clients = new ArrayList<>();

  @Factory(dataProvider = "allRestCombinations", dataProviderClass = ClientServerConfiguration.class)
  public TestRestCompressionEcho(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }

  @AfterClass
  public void tearDown() throws Exception
  {
    for (Client client : _clients)
    {
      final FutureCallback<None> clientShutdownCallback = new FutureCallback<>();
      client.shutdown(clientShutdownCallback);
      clientShutdownCallback.get();
    }

    super.tearDown(_client, _server);
  }


  @Override
  protected TransportDispatcher getTransportDispatcher()
  {
    return new TransportDispatcherBuilder(REST_OVER_STREAM)
        .addRestHandler(ECHO_URI, new RestEchoHandler())
        .build();
  }

  @Override
  protected FilterChain getServerFilterChain()
  {
    return FilterChains.createRestChain(_compressionFilter);
  }

  @Override
  protected Map<String, Object> getHttpClientProperties()
  {
    Map<String, Object> clientProperties = new HashMap<>();
    clientProperties.put(HttpClientFactory.HTTP_MAX_RESPONSE_SIZE, String.valueOf(LARGE_BYTES_NUM * 2));
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "60000");
    return clientProperties;
  }

  @DataProvider
  public Object[][] compressionEchoData() throws Exception
  {
    EncodingType[] encodings =
        new EncodingType[]{
            EncodingType.GZIP,
            EncodingType.SNAPPY,
            EncodingType.IDENTITY
        };

    Object[][] args = new Object[2 * encodings.length * encodings.length][2];

    int cur = 0;
    for (EncodingType requestEncoding : encodings)
    {
      for (EncodingType acceptEncoding : encodings)
      {
        RestFilter clientCompressionFilter =
            new ClientCompressionFilter(requestEncoding,
                new CompressionConfig(THRESHOLD),
                new EncodingType[]{acceptEncoding},
                new CompressionConfig(THRESHOLD),
                Arrays.asList(new String[]{"*"}));

        Client client = _clientProvider.createClient(FilterChains.createRestChain(clientCompressionFilter), getHttpClientProperties());
        args[cur][0] =  client;
        args[cur][1] = LARGE_BYTES_NUM;
        cur++;
        _clients.add(client);
      }
    }
    // test data that won't trigger compression
    for (EncodingType requestEncoding : encodings)
    {
      for (EncodingType acceptEncoding : encodings)
      {
        RestFilter clientCompressionFilter =
            new ClientCompressionFilter(requestEncoding,
                new CompressionConfig(THRESHOLD),
                new EncodingType[]{acceptEncoding},
                new CompressionConfig(THRESHOLD),
                Arrays.asList(new String[]{"*"}));

        Client client = _clientProvider.createClient(FilterChains.createRestChain(clientCompressionFilter), getHttpClientProperties());
        args[cur][0] = client;
        args[cur][1] = SMALL_BYTES_NUM;
        cur++;
        _clients.add(client);
      }
    }
    return args;
  }

  @Test(dataProvider = "compressionEchoData")
  public void testResponseCompression(Client client, long bytes)
      throws InterruptedException, TimeoutException, ExecutionException
  {
    RestRequestBuilder builder = new RestRequestBuilder((_clientProvider.createHttpURI(_port, ECHO_URI)));
    byte[] content = new byte[(int)bytes];
    for (int i = 0; i < bytes; i++)
    {
      content[i] = (byte) (i % 256);
    }
    RestRequest request = builder.setEntity(content).build();

    final FutureCallback<RestResponse> callback = new FutureCallback<>();
    RequestContext requestContext = new RequestContext();

    // OPERATION is required to enabled response compression
    requestContext.putLocalAttr(R2Constants.OPERATION, "get");
    client.restRequest(request, requestContext, callback);

    final RestResponse response = callback.get(60, TimeUnit.SECONDS);
    Assert.assertEquals(response.getStatus(), RestStatus.OK);

    Assert.assertEquals(response.getEntity().copyBytes(), content);
  }

  private static class RestEchoHandler implements RestRequestHandler
  {
    @Override
    public void handleRequest(RestRequest request, RequestContext requestContext, final Callback<RestResponse> callback)
    {
      RestResponseBuilder builder = new RestResponseBuilder();
      callback.onSuccess(builder.setEntity(request.getEntity()).build());
    }
  }
}

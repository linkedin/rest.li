package test.r2.integ;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.CompressionConfig;
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
import com.linkedin.r2.sample.Bootstrap;
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @auther Zhenkai Zhu
 */

public class TestRestCompressionEcho
{
  protected static final int PORT = 11938;
  private static final int THRESHOLD = 4096;
  private static final boolean REST_OVER_STREAM = false;
  protected static final long LARGE_BYTES_NUM = THRESHOLD * THRESHOLD;
  protected static final long SMALL_BYTES_NUM = THRESHOLD - 1;
  private static final URI ECHO_URI = URI.create("/echo");


  protected final RestFilter _compressionFilter = new ServerCompressionFilter(EncodingType.values(), new CompressionConfig(THRESHOLD));

  private HttpServer _server;

  private List<TransportClientFactory> _clientFactories = new ArrayList<TransportClientFactory>();
  private List<Client> _clients = new ArrayList<Client>();

  private final HttpJettyServer.ServletType _servletType;

  @Factory(dataProvider = "configs")
  public TestRestCompressionEcho(HttpJettyServer.ServletType servletType)
  {
    _servletType = servletType;
  }

  @DataProvider
  public static Object[][] configs()
  {
    return new Object[][] {{HttpJettyServer.ServletType.RAP}, {HttpJettyServer.ServletType.ASYNC_EVENT}};
  }

  @BeforeClass
  public void setup() throws IOException
  {
    _server = getServerFactory().createH2cServer(PORT, getTransportDispatcher(), REST_OVER_STREAM);
    _server.start();
  }

  @AfterClass
  public void tearDown() throws Exception
  {
    for (Client client : _clients)
    {
      final FutureCallback<None> clientShutdownCallback = new FutureCallback<None>();
      client.shutdown(clientShutdownCallback);
      clientShutdownCallback.get();
    }
    for (TransportClientFactory factory : _clientFactories)
    {
      final FutureCallback<None> factoryShutdownCallback = new FutureCallback<None>();
      factory.shutdown(factoryShutdownCallback);
      factoryShutdownCallback.get();
    }

    if (_server != null) {
      _server.stop();
      _server.waitForStop();
    }
  }

  protected HttpServerFactory getServerFactory()
  {
    return new HttpServerFactory(FilterChains.createRestChain(_compressionFilter), _servletType);
  }

  protected TransportDispatcher getTransportDispatcher()
  {
    return new TransportDispatcherBuilder(REST_OVER_STREAM)
        .addRestHandler(ECHO_URI, new RestEchoHandler())
        .build();
  }

  protected Map<String, String> getHttp1ClientProperties()
  {
    Map<String, String> clientProperties = new HashMap<String, String>();
    clientProperties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_1_1);
    clientProperties.put(HttpClientFactory.HTTP_MAX_RESPONSE_SIZE, String.valueOf(LARGE_BYTES_NUM * 2));
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "60000");
    return clientProperties;
  }

  protected Map<String, String> getHttp2ClientProperties()
  {
    Map<String, String> clientProperties = new HashMap<String, String>();
    clientProperties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, HttpProtocolVersion.HTTP_2);
    clientProperties.put(HttpClientFactory.HTTP_MAX_RESPONSE_SIZE, String.valueOf(LARGE_BYTES_NUM * 2));
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "60000");
    return clientProperties;
  }

  @DataProvider
  public Object[][] compressionEchoData()
  {
    EncodingType[] encodings =
        new EncodingType[]{
            EncodingType.GZIP,
            EncodingType.SNAPPY,
            EncodingType.IDENTITY
        };
    Object[][] args = new Object[4 * encodings.length * encodings.length][2];


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

        TransportClientFactory factory = new HttpClientFactory.Builder()
            .setFilterChain(FilterChains.createRestChain(clientCompressionFilter))
            .build();
        Client http1Client = new TransportClientAdapter(factory.getClient(getHttp1ClientProperties()), REST_OVER_STREAM);
        Client http2Client = new TransportClientAdapter(factory.getClient(getHttp2ClientProperties()), REST_OVER_STREAM);
        args[cur][0] = http1Client;
        args[cur][1] = LARGE_BYTES_NUM;
        args[cur + 1][0] = http1Client;
        args[cur + 1][1] = SMALL_BYTES_NUM;
        cur += 2;
        _clientFactories.add(factory);
        _clients.add(http1Client);
        _clients.add(http2Client);
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

        TransportClientFactory factory = new HttpClientFactory.Builder()
            .setFilterChain(FilterChains.createRestChain(clientCompressionFilter))
            .build();
        Client http1Client = new TransportClientAdapter(factory.getClient(getHttp1ClientProperties()), REST_OVER_STREAM);
        Client http2Client = new TransportClientAdapter(factory.getClient(getHttp2ClientProperties()), REST_OVER_STREAM);
        args[cur][0] = http1Client;
        args[cur][1] = SMALL_BYTES_NUM;
        args[cur + 1][0] = http1Client;
        args[cur + 1][1] = SMALL_BYTES_NUM;
        cur += 2;
        _clientFactories.add(factory);
        _clients.add(http1Client);
        _clients.add(http2Client);
      }
    }
    return args;
  }


  @Test(dataProvider = "compressionEchoData")
  public void testResponseCompression(Client client, long bytes)
      throws InterruptedException, TimeoutException, ExecutionException
  {
    RestRequestBuilder builder = new RestRequestBuilder((Bootstrap.createHttpURI(PORT, ECHO_URI)));
    byte[] content = new byte[(int)bytes];
    for (int i = 0; i < bytes; i++)
    {
      content[i] = (byte) (i % 256);
    }
    RestRequest request = builder.setEntity(content).build();

    final FutureCallback<RestResponse> callback = new FutureCallback<RestResponse>();
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

package test.r2.integ.clientserver;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.filter.compression.ClientStreamCompressionFilter;
import com.linkedin.r2.filter.compression.ServerStreamCompressionFilter;
import com.linkedin.r2.filter.compression.streaming.StreamEncodingType;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.Server;
import com.linkedin.r2.transport.common.StreamRequestHandler;
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;
import test.r2.integ.helper.BytesReader;
import test.r2.integ.helper.BytesWriter;


/**
 * @author Ang Xu
 * @author Nizar Mankulangara
 */
public class TestCompressionEcho extends AbstractServiceTest
{
  private static final int THRESHOLD = 4096;
  protected static final byte BYTE = 75;
  protected static final long LARGE_BYTES_NUM = THRESHOLD * THRESHOLD;
  protected static final long SMALL_BYTES_NUM = THRESHOLD - 1;
  private static final URI ECHO_URI = URI.create("/echo");

  private List<Client> _clients = new ArrayList<Client>();


  @Factory(dataProvider = "allStreamCombinations", dataProviderClass = ClientServerConfiguration.class)
  public TestCompressionEcho(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }

  protected FilterChain getServerFilterChain()
  {
    final StreamFilter _compressionFilter = new ServerStreamCompressionFilter(StreamEncodingType.values(), _executor, THRESHOLD);
    return FilterChains.createStreamChain(_compressionFilter);
  }

  protected TransportDispatcher getTransportDispatcher()
  {
    return new TransportDispatcherBuilder()
        .addStreamHandler(ECHO_URI, new SteamEchoHandler())
        .build();
  }

  @Override
  protected void tearDown(Client client, Server server) throws Exception
  {
    for (Client compressionClient : _clients)
    {
      final FutureCallback<None> clientShutdownCallback = new FutureCallback<None>();
      compressionClient.shutdown(clientShutdownCallback);
      clientShutdownCallback.get();
    }

    super.tearDown(client, server);
  }

  @DataProvider
  public Object[][] compressionEchoData() throws Exception
  {
    StreamEncodingType[] encodings =
        new StreamEncodingType[]{
            StreamEncodingType.GZIP,
            StreamEncodingType.DEFLATE,
            StreamEncodingType.SNAPPY_FRAMED,
            StreamEncodingType.BZIP2,
            StreamEncodingType.IDENTITY
        };
    Object[][] args = new Object[2 * encodings.length * encodings.length][2];

    int cur = 0;
    for (StreamEncodingType requestEncoding : encodings)
    {
      for (StreamEncodingType acceptEncoding : encodings)
      {
        StreamFilter clientCompressionFilter =
            new ClientStreamCompressionFilter(requestEncoding,
                new CompressionConfig(THRESHOLD),
                new StreamEncodingType[]{acceptEncoding},
                new CompressionConfig(THRESHOLD),
                Arrays.asList(new String[]{"*"}),
                _executor);

        Client client = createClient(FilterChains.createStreamChain(clientCompressionFilter));
        args[cur][0] = client;
        args[cur][1] = LARGE_BYTES_NUM;
        cur ++;
        _clients.add(client);
      }
    }
    // test data that won't trigger compression
    for (StreamEncodingType requestEncoding : encodings)
    {
      for (StreamEncodingType acceptEncoding : encodings)
      {
        StreamFilter clientCompressionFilter =
            new ClientStreamCompressionFilter(requestEncoding,
                new CompressionConfig(THRESHOLD),
                new StreamEncodingType[]{acceptEncoding},
                new CompressionConfig(THRESHOLD),
                Arrays.asList(new String[]{"*"}),
                _executor);

        Client client = createClient(FilterChains.createStreamChain(clientCompressionFilter));
        args[cur][0] = client;
        args[cur][1] = SMALL_BYTES_NUM;
        cur ++;
        _clients.add(client);
      }
    }
    return args;
  }


  @Test(dataProvider = "compressionEchoData")
  public void testResponseCompression(Client client, long bytes)
      throws InterruptedException, TimeoutException, ExecutionException
  {
    StreamRequestBuilder builder = new StreamRequestBuilder((_clientProvider.createHttpURI(_port, ECHO_URI)));
    BytesWriter writer = new BytesWriter(bytes, BYTE);
    StreamRequest request = builder.build(EntityStreams.newEntityStream(writer));

    // add operation to enable sending accept encoding
    RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(R2Constants.OPERATION, "get");
    final FutureCallback<StreamResponse> callback = new FutureCallback<StreamResponse>();
    client.streamRequest(request, requestContext, callback);

    final StreamResponse response = callback.get(60, TimeUnit.SECONDS);
    Assert.assertEquals(response.getStatus(), RestStatus.OK);

    final FutureCallback<None> readerCallback = new FutureCallback<None>();
    final BytesReader reader = new BytesReader(BYTE, readerCallback);
    response.getEntityStream().setReader(reader);

    readerCallback.get(60, TimeUnit.SECONDS);
    Assert.assertEquals(reader.getTotalBytes(), bytes);
    Assert.assertTrue(reader.allBytesCorrect());
  }

  @Override
  protected Map<String, Object> getHttpClientProperties()
  {
    Map<String, Object> clientProperties = new HashMap<String, Object>();
    clientProperties.put(HttpClientFactory.HTTP_MAX_RESPONSE_SIZE, String.valueOf(LARGE_BYTES_NUM * 2));
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "60000");
    return clientProperties;
  }

  private static class SteamEchoHandler implements StreamRequestHandler
  {
    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext, final Callback<StreamResponse> callback)
    {
      StreamResponseBuilder builder = new StreamResponseBuilder();
      callback.onSuccess(builder.build(request.getEntityStream()));
    }
  }
}

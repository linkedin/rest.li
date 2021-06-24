package test.r2.integ.clientserver;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.compression.ClientStreamCompressionFilter;
import com.linkedin.r2.filter.compression.streaming.Bzip2Compressor;
import com.linkedin.r2.filter.compression.streaming.DeflateCompressor;
import com.linkedin.r2.filter.compression.streaming.GzipCompressor;
import com.linkedin.r2.filter.compression.streaming.SnappyCompressor;
import com.linkedin.r2.filter.compression.streaming.StreamEncodingType;
import com.linkedin.r2.filter.compression.streaming.StreamingCompressor;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import test.r2.integ.helper.BytesWriter;


/**
 * @author Ang Xu
 * @author Nizar Mankulangara
 */
public class TestRequestCompression extends AbstractServiceTest
{
  private static final URI GZIP_URI  = URI.create("/" + StreamEncodingType.GZIP.getHttpName());
  private static final URI DEFLATE_URI  = URI.create("/" + StreamEncodingType.DEFLATE.getHttpName());
  private static final URI BZIP2_URI  = URI.create("/" + StreamEncodingType.BZIP2.getHttpName());
  private static final URI SNAPPY_URI  = URI.create("/" + StreamEncodingType.SNAPPY_FRAMED.getHttpName());
  private static final URI NO_COMPRESSION_URI = URI.create("/noCompression");

  private static final byte BYTE = 50;
  private static final int THRESHOLD = 4096;
  private static final int NUM_BYTES = 1024 * 1024 * 16;

  private ExecutorService _executor = Executors.newCachedThreadPool();
  private List<Client> _clients = new ArrayList<>();

  @Factory(dataProvider = "allStreamCombinations", dataProviderClass = ClientServerConfiguration.class)
  public TestRequestCompression(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }

  @Override
  protected void tearDown(Client client, Server server) throws Exception
  {
    for (Client compressionClient : _clients)
    {
      final FutureCallback<None> clientShutdownCallback = new FutureCallback<>();
      compressionClient.shutdown(clientShutdownCallback);
      clientShutdownCallback.get();
    }

    super.tearDown(client, server);
  }

  @Override
  protected TransportDispatcher getTransportDispatcher()
  {
    return new TransportDispatcherBuilder()
        .addStreamHandler(GZIP_URI, new StreamCompressionHandler(new GzipCompressor(_executor)))
        .addStreamHandler(DEFLATE_URI, new StreamCompressionHandler(new DeflateCompressor(_executor)))
        .addStreamHandler(BZIP2_URI, new StreamCompressionHandler(new Bzip2Compressor(_executor)))
        .addStreamHandler(SNAPPY_URI, new StreamCompressionHandler(new SnappyCompressor(_executor)))
        .addStreamHandler(NO_COMPRESSION_URI, new NoCompressionHandler())
        .build();
  }

  @DataProvider
  public Object[][] requestCompressionData() throws Exception
  {
    StreamEncodingType[] encodings =
        new StreamEncodingType[]{
            StreamEncodingType.GZIP,
            StreamEncodingType.DEFLATE,
            StreamEncodingType.SNAPPY_FRAMED,
            StreamEncodingType.BZIP2,
        };

    Object[][] args = new Object[encodings.length][2];

    int cur = 0;
    for (StreamEncodingType requestEncoding : encodings)
    {
      StreamFilter clientCompressionFilter =
          new ClientStreamCompressionFilter(requestEncoding, new CompressionConfig(THRESHOLD), null, new CompressionConfig(THRESHOLD),
              Arrays.asList(new String[]{"*"}), _executor);

      HashMap<String, Object> properties = new HashMap<>();
      properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "10000");
      Client client = _clientProvider.createClient(FilterChains.createStreamChain(clientCompressionFilter), properties);
      args[cur][0] = client;
      args[cur][1] = URI.create("/" + requestEncoding.getHttpName());
      cur++;
      _clients.add(client);
    }
    return args;
  }

  @DataProvider
  public Object[][] noCompressionData() throws Exception
  {
    StreamEncodingType[] encodings =
        new StreamEncodingType[]{
            StreamEncodingType.GZIP,
            StreamEncodingType.DEFLATE,
            StreamEncodingType.SNAPPY_FRAMED,
            StreamEncodingType.BZIP2,
            StreamEncodingType.IDENTITY
        };

    Object[][] args = new Object[encodings.length][1];

    int cur = 0;
    for (StreamEncodingType requestEncoding : encodings)
    {
      StreamFilter clientCompressionFilter =
          new ClientStreamCompressionFilter(requestEncoding, new CompressionConfig(THRESHOLD), null, new CompressionConfig(THRESHOLD),
              Arrays.asList(new String[]{"*"}), _executor);

      HashMap<String, Object> properties = new HashMap<>();
      Client client = _clientProvider.createClient(FilterChains.createStreamChain(clientCompressionFilter), properties);
      args[cur][0] = client;
      //args[cur][1] = URI.create("/" + requestEncoding.getHttpName());
      cur++;
      _clients.add(client);
    }
    return args;
  }

  @Test(dataProvider = "noCompressionData")
  public void testNoCompression(Client client)
      throws InterruptedException, TimeoutException, ExecutionException
  {
    StreamRequestBuilder builder = new StreamRequestBuilder((_clientProvider.createHttpURI(_port, NO_COMPRESSION_URI)));
    BytesWriter writer = new BytesWriter(THRESHOLD-1, BYTE);
    StreamRequest request = builder.build(EntityStreams.newEntityStream(writer));

    final FutureCallback<StreamResponse> callback = new FutureCallback<>();
    client.streamRequest(request, callback);

    final StreamResponse response = callback.get(60, TimeUnit.SECONDS);
    Assert.assertEquals(response.getStatus(), RestStatus.OK);
  }

  @Test(dataProvider = "requestCompressionData")
  public void testRequestCompression(Client client, URI uri)
      throws InterruptedException, TimeoutException, ExecutionException
  {
    StreamRequestBuilder builder = new StreamRequestBuilder((_clientProvider.createHttpURI(_port, uri)));
    BytesWriter writer = new BytesWriter(NUM_BYTES, BYTE);
    StreamRequest request = builder.build(EntityStreams.newEntityStream(writer));

    final FutureCallback<StreamResponse> callback = new FutureCallback<>();
    client.streamRequest(request, callback);

    final StreamResponse response = callback.get(60, TimeUnit.SECONDS);
    Assert.assertEquals(response.getStatus(), RestStatus.OK);
  }

  private static class NoCompressionHandler implements StreamRequestHandler
  {
    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext, final Callback<StreamResponse> callback)
    {
      final ByteChecker reader = new ByteChecker(BYTE, THRESHOLD-1, new Callback<None>()
      {
        @Override
        public void onError(Throwable e)
        {
          callback.onError(e);
        }

        @Override
        public void onSuccess(None result)
        {
          RestResponse response = RestStatus.responseForStatus(RestStatus.OK, "");
          callback.onSuccess(Messages.toStreamResponse(response));
        }
      });
      request.getEntityStream().setReader(reader);
    }
  }

  private static class StreamCompressionHandler implements StreamRequestHandler
  {
    private final StreamingCompressor _compressor;

    public StreamCompressionHandler(StreamingCompressor compressor)
    {
      _compressor = compressor;
    }

    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext, final Callback<StreamResponse> callback)
    {
      EntityStream uncompressedStream = _compressor.inflate(request.getEntityStream());
      final ByteChecker reader = new ByteChecker(BYTE, NUM_BYTES, new Callback<None>()
      {
        @Override
        public void onError(Throwable e)
        {
          callback.onError(e);
        }

        @Override
        public void onSuccess(None result)
        {
          RestResponse response = RestStatus.responseForStatus(RestStatus.OK, "");
          callback.onSuccess(Messages.toStreamResponse(response));
        }
      });
      uncompressedStream.setReader(reader);
    }
  }

  private static class ByteChecker implements Reader
  {
    private final byte _expectedByte;
    private final int _expectedLength;
    private final Callback<None> _callback;

    private ReadHandle _rh;
    private int _length;
    private boolean _bytesCorrect;

    public ByteChecker(byte b, int len, Callback<None> callback)
    {
      _expectedByte = b;
      _expectedLength = len;
      _callback = callback;
      _bytesCorrect = true;
    }

    @Override
    public void onInit(ReadHandle rh)
    {
      _rh = rh;
      _rh.request(Integer.MAX_VALUE);
    }

    @Override
    public void onDataAvailable(ByteString data)
    {
      _length += data.length();
      byte [] bytes = data.copyBytes();
      for (byte b : bytes)
      {
        if (b != _expectedByte)
        {
          _bytesCorrect = false;
        }
      }
      _rh.request(1);
    }

    @Override
    public void onDone()
    {
      if (_bytesCorrect && _length == _expectedLength)
      {
        _callback.onSuccess(None.none());
      }
      else
      {
        _callback.onError(new IllegalArgumentException("input stream is incorrect"));
      }
    }

    @Override
    public void onError(Throwable e)
    {
      _callback.onError(e);
    }
  }
}

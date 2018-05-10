package test.r2.integ;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.compression.ClientStreamCompressionFilter;
import com.linkedin.r2.filter.compression.streaming.StreamEncodingType;
import com.linkedin.r2.filter.compression.streaming.Bzip2Compressor;
import com.linkedin.r2.filter.compression.streaming.DeflateCompressor;
import com.linkedin.r2.filter.compression.streaming.GzipCompressor;
import com.linkedin.r2.filter.compression.streaming.SnappyCompressor;
import com.linkedin.r2.filter.compression.streaming.StreamingCompressor;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.TransportClientFactory;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.common.HttpProtocolVersion;
import com.linkedin.r2.transport.http.server.HttpJettyServer;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Ang Xu
 */
public class TestRequestCompression
{
  private static final URI GZIP_URI  = URI.create("/" + StreamEncodingType.GZIP.getHttpName());
  private static final URI DEFLATE_URI  = URI.create("/" + StreamEncodingType.DEFLATE.getHttpName());
  private static final URI BZIP2_URI  = URI.create("/" + StreamEncodingType.BZIP2.getHttpName());
  private static final URI SNAPPY_URI  = URI.create("/" + StreamEncodingType.SNAPPY_FRAMED.getHttpName());
  private static final URI NO_COMPRESSION_URI = URI.create("/noCompression");


  private static final int PORT = 11940;
  private static final byte BYTE = 50;
  private static final int THRESHOLD = 4096;
  private static final int NUM_BYTES = 1024 * 1024 * 16;



  private ExecutorService _executor = Executors.newCachedThreadPool();
  private HttpServer _server;
  private List<TransportClientFactory> _clientFactories = new ArrayList<TransportClientFactory>();
  private List<Client> _clients = new ArrayList<Client>();


  @BeforeClass
  public void setup() throws IOException
  {
    _server = new HttpServerFactory(HttpJettyServer.ServletType.ASYNC_EVENT).createH2cServer(PORT,
        getTransportDispatcher(), true);
    _server.start();
  }

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
  public Object[][] requestCompressionData()
  {
    StreamEncodingType[] encodings =
        new StreamEncodingType[]{
            StreamEncodingType.GZIP,
            StreamEncodingType.DEFLATE,
            StreamEncodingType.SNAPPY_FRAMED,
            StreamEncodingType.BZIP2,
        };
    String[] protocols = new String[] {
        HttpProtocolVersion.HTTP_1_1.name(),
        HttpProtocolVersion.HTTP_2.name(),
    };
    Object[][] args = new Object[encodings.length * protocols.length][2];

    int cur = 0;
    for (StreamEncodingType requestEncoding : encodings)
    {
      for (String protocol : protocols)
      {
        StreamFilter clientCompressionFilter =
            new ClientStreamCompressionFilter(requestEncoding, new CompressionConfig(THRESHOLD), null, new CompressionConfig(THRESHOLD),
                Arrays.asList(new String[]{"*"}), _executor);

        TransportClientFactory factory =
            new HttpClientFactory.Builder().setFilterChain(FilterChains.createStreamChain(clientCompressionFilter))
                .build();
        HashMap<String, String> properties = new HashMap<>();
        properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, protocol);
        properties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "10000");
        Client client = new TransportClientAdapter(factory.getClient(properties), true);
        args[cur][0] = client;
        args[cur][1] = URI.create("/" + requestEncoding.getHttpName());
        cur++;
        _clientFactories.add(factory);
        _clients.add(client);
      }
    }
    return args;
  }

  @DataProvider
  public Object[][] noCompressionData()
  {
    StreamEncodingType[] encodings =
        new StreamEncodingType[]{
            StreamEncodingType.GZIP,
            StreamEncodingType.DEFLATE,
            StreamEncodingType.SNAPPY_FRAMED,
            StreamEncodingType.BZIP2,
            StreamEncodingType.IDENTITY
        };
    String[] protocols = new String[] {
        HttpProtocolVersion.HTTP_1_1.name(),
        HttpProtocolVersion.HTTP_2.name(),
    };
    Object[][] args = new Object[encodings.length * protocols.length][1];

    int cur = 0;
    for (StreamEncodingType requestEncoding : encodings)
    {
      for (String protocol : protocols)
      {
        StreamFilter clientCompressionFilter =
            new ClientStreamCompressionFilter(requestEncoding, new CompressionConfig(THRESHOLD), null, new CompressionConfig(THRESHOLD),
                Arrays.asList(new String[]{"*"}), _executor);

        TransportClientFactory factory =
            new HttpClientFactory.Builder().setFilterChain(FilterChains.createStreamChain(clientCompressionFilter))
                .build();
        HashMap<String, String> properties = new HashMap<>();
        properties.put(HttpClientFactory.HTTP_PROTOCOL_VERSION, protocol);
        Client client = new TransportClientAdapter(factory.getClient(Collections.<String, String>emptyMap()), true);
        args[cur][0] = client;
        //args[cur][1] = URI.create("/" + requestEncoding.getHttpName());
        cur++;
        _clientFactories.add(factory);
        _clients.add(client);
      }
    }
    return args;
  }

  @Test(dataProvider = "noCompressionData")
  public void testNoCompression(Client client)
      throws InterruptedException, TimeoutException, ExecutionException
  {
    StreamRequestBuilder builder = new StreamRequestBuilder((Bootstrap.createHttpURI(PORT, NO_COMPRESSION_URI)));
    BytesWriter writer = new BytesWriter(THRESHOLD-1, BYTE);
    StreamRequest request = builder.build(EntityStreams.newEntityStream(writer));

    final FutureCallback<StreamResponse> callback = new FutureCallback<StreamResponse>();
    client.streamRequest(request, callback);

    final StreamResponse response = callback.get(60, TimeUnit.SECONDS);
    Assert.assertEquals(response.getStatus(), RestStatus.OK);
  }

  @Test(dataProvider = "requestCompressionData")
  public void testRequestCompression(Client client, URI uri)
      throws InterruptedException, TimeoutException, ExecutionException
  {
    StreamRequestBuilder builder = new StreamRequestBuilder((Bootstrap.createHttpURI(PORT, uri)));
    BytesWriter writer = new BytesWriter(NUM_BYTES, BYTE);
    StreamRequest request = builder.build(EntityStreams.newEntityStream(writer));

    final FutureCallback<StreamResponse> callback = new FutureCallback<StreamResponse>();
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

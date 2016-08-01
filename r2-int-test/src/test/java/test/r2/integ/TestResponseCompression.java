/*
   Copyright (c) 2015 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package test.r2.integ;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.compression.ServerStreamCompressionFilter;
import com.linkedin.r2.filter.compression.streaming.StreamEncodingType;
import com.linkedin.r2.filter.compression.streaming.Bzip2Compressor;
import com.linkedin.r2.filter.compression.streaming.DeflateCompressor;
import com.linkedin.r2.filter.compression.streaming.GzipCompressor;
import com.linkedin.r2.filter.compression.streaming.NoopCompressor;
import com.linkedin.r2.filter.compression.streaming.SnappyCompressor;
import com.linkedin.r2.filter.compression.streaming.StreamingCompressor;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.DrainReader;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.common.HttpConstants;
import com.linkedin.r2.transport.http.server.HttpJettyServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;


/**
 * @author Ang Xu
 */
public class TestResponseCompression extends AbstractStreamTest
{
  private static final URI SMALL_URI  = URI.create("/small");
  private static final URI TINY_URI   = URI.create("/tiny");

  protected ExecutorService _executor = Executors.newCachedThreadPool();
  protected StreamFilter _compressionFilter =
      new ServerStreamCompressionFilter(StreamEncodingType.values(), _executor, (int)TINY_BYTES_NUM+1);

  private final HttpJettyServer.ServletType _servletType;

  @Factory(dataProvider = "configs")
  public TestResponseCompression(HttpJettyServer.ServletType servletType)
  {
    _servletType = servletType;
  }

  @DataProvider
  public static Object[][] configs()
  {
    return new Object[][] {{HttpJettyServer.ServletType.RAP}, {HttpJettyServer.ServletType.ASYNC_EVENT}};
  }

  @AfterClass
  public void afterClass() throws Exception
  {
    _executor.shutdown();
  }

  @Override
  protected HttpServerFactory getServerFactory()
  {
    return new HttpServerFactory(FilterChains.createStreamChain(_compressionFilter), _servletType);
  }

  @Override
  protected TransportDispatcher getTransportDispatcher()
  {
    return new TransportDispatcherBuilder()
        .addStreamHandler(SMALL_URI, new BytesWriterRequestHandler(BYTE, SMALL_BYTES_NUM))
        .addStreamHandler(TINY_URI, new BytesWriterRequestHandler(BYTE, TINY_BYTES_NUM))
        .build();
  }

  @Override
  protected Map<String, String> getHttp1ClientProperties()
  {
    Map<String, String> clientProperties = super.getHttp1ClientProperties();
    clientProperties.put(HttpClientFactory.HTTP_MAX_RESPONSE_SIZE, String.valueOf(LARGE_BYTES_NUM * 2));
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "60000");
    return clientProperties;
  }

  @Override
  protected Map<String, String> getHttp2ClientProperties()
  {
    Map<String, String> clientProperties = super.getHttp2ClientProperties();
    clientProperties.put(HttpClientFactory.HTTP_MAX_RESPONSE_SIZE, String.valueOf(LARGE_BYTES_NUM * 2));
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "60000");
    return clientProperties;
  }

  @Test
  public void testDeflateCompression()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "deflate", new DeflateCompressor(_executor));
  }

  @Test
  public void testGzipCompression()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "gzip", new GzipCompressor(_executor));
  }

  @Test
  public void testBzip2Compression()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "bzip2", new Bzip2Compressor(_executor));
  }

  @Test
  public void testSnappyCompression()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "x-snappy-framed", new SnappyCompressor(_executor));
  }

  @Test
  public void testSnappyCompression2()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM,
                            "x-snappy-framed;q=1, bzip2;q=0.75, gzip;q=0.5, defalte;q=0",
                            new SnappyCompressor(_executor));
  }

  @Test
  public void testSnappyCompression3()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "x-snappy-framed, *;q=0",
                            new SnappyCompressor(_executor));
  }

  @Test
  public void testNoCompression()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "identity", new NoopCompressor());
  }

  @Test
  public void testNoCompression2()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "", new NoopCompressor());
  }

  @Test
  public void testNoCompression3()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "foobar", new NoopCompressor());
  }

  @Test
  public void testCompressionThreshold()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(TINY_URI, TINY_BYTES_NUM, "x-snappy-framed", new NoopCompressor());
  }

  @Test
  public void testBadEncoding()
      throws TimeoutException, InterruptedException
  {
    testEncodingNotAcceptable("foobar, identity;q=0");
  }

  private void testResponseCompression(URI uri, long bytes, String acceptEncoding, final StreamingCompressor compressor)
      throws InterruptedException, TimeoutException, ExecutionException
  {
    for (Client client : clients())
    {
      StreamRequestBuilder builder = new StreamRequestBuilder((Bootstrap.createHttpURI(PORT, uri)));
      builder.addHeaderValue(HttpConstants.ACCEPT_ENCODING, acceptEncoding);
      StreamRequest request = builder.build(EntityStreams.emptyStream());

      final FutureCallback<StreamResponse> callback = new FutureCallback<StreamResponse>();
      client.streamRequest(request, callback);

      final StreamResponse response = callback.get(60, TimeUnit.SECONDS);
      Assert.assertEquals(response.getStatus(), RestStatus.OK);

      final FutureCallback<None> readerCallback = new FutureCallback<None>();
      final BytesReader reader = new BytesReader(BYTE, readerCallback);
      final EntityStream decompressedStream = compressor.inflate(response.getEntityStream());
      decompressedStream.setReader(reader);

      readerCallback.get(60, TimeUnit.SECONDS);
      Assert.assertEquals(reader.getTotalBytes(), bytes);
      Assert.assertTrue(reader.allBytesCorrect());
    }
  }

  public void testEncodingNotAcceptable(String acceptEncoding)
      throws TimeoutException, InterruptedException
  {
    for (Client client : clients())
    {
      StreamRequestBuilder builder = new StreamRequestBuilder((Bootstrap.createHttpURI(PORT, SMALL_URI)));
      if (acceptEncoding != null)
      {
        builder.addHeaderValue(HttpConstants.ACCEPT_ENCODING, acceptEncoding);
      }
      StreamRequest request = builder.build(EntityStreams.emptyStream());

      final FutureCallback<StreamResponse> callback = new FutureCallback<StreamResponse>();
      client.streamRequest(request, callback);
      try
      {
        final StreamResponse response = callback.get(60, TimeUnit.SECONDS);
        Assert.fail("Should have thrown exception when encoding is not acceptable");
      } catch (ExecutionException e)
      {
        Throwable t = e.getCause();
        Assert.assertTrue(t instanceof StreamException);
        StreamResponse response = ((StreamException) t).getResponse();
        Assert.assertEquals(response.getStatus(), HttpConstants.NOT_ACCEPTABLE);
      }
    }
  }

  private static class BytesWriterRequestHandler implements StreamRequestHandler
  {
    private final byte _b;
    private final long _bytesNum;
    private volatile TimedBytesWriter _writer;

    BytesWriterRequestHandler(byte b, long bytesNUm)
    {
      _b = b;
      _bytesNum = bytesNUm;
    }

    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext, final Callback<StreamResponse> callback)
    {
      request.getEntityStream().setReader(new DrainReader());
      _writer = createWriter(_bytesNum, _b);
      StreamResponse response = buildResponse(_writer);
      callback.onSuccess(response);
    }

    TimedBytesWriter getWriter()
    {
      return _writer;
    }

    protected TimedBytesWriter createWriter(long bytesNum, byte b)
    {
      return new TimedBytesWriter(_bytesNum, _b);
    }

    StreamResponse buildResponse(Writer writer)
    {
      return new StreamResponseBuilder().build(EntityStreams.newEntityStream(writer));
    }
  }
}

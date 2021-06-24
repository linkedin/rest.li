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

package test.r2.integ.clientserver;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.compression.ServerStreamCompressionFilter;
import com.linkedin.r2.filter.compression.streaming.StreamEncodingType;
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
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.common.HttpConstants;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;
import test.r2.integ.helper.BytesReader;
import test.r2.integ.helper.TimedBytesWriter;


/**
 * @author Ang Xu
 * @author Nizar Mankulangara
 */
public class TestStreamResponseCompression extends AbstractServiceTest
{
  private static final URI SMALL_URI  = URI.create("/small");
  private static final URI TINY_URI   = URI.create("/tiny");

  private ExecutorService _executor = Executors.newCachedThreadPool();
  private StreamFilter _compressionFilter =
      new ServerStreamCompressionFilter(StreamEncodingType.values(), _executor, (int)TINY_BYTES_NUM+1);

  @Factory(dataProvider = "allStreamCombinations", dataProviderClass = ClientServerConfiguration.class)
  public TestStreamResponseCompression(ClientProvider clientProvider, ServerProvider serverProvider, int port) {
    super(clientProvider, serverProvider, port);
  }

  @AfterClass
  public void afterClass() throws Exception
  {
    _executor.shutdown();
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
  protected FilterChain getServerFilterChain()
  {
    return FilterChains.createStreamChain(_compressionFilter);
  }

  @Override
  protected Map<String, Object> getHttpClientProperties()
  {
    Map<String, Object> clientProperties = new HashMap<>();
    clientProperties.put(HttpClientFactory.HTTP_MAX_RESPONSE_SIZE, String.valueOf(LARGE_BYTES_NUM * 2));
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "60000");
    return clientProperties;
  }

  @Test
  public void testDeflateCompression()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "deflate");
  }

  @Test
  public void testGzipCompression()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "gzip");
  }

  @Test
  public void testBzip2Compression()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "bzip2");
  }

  @Test
  public void testSnappyCompression()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "x-snappy-framed");
  }

  @Test
  public void testSnappyCompression2()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM,
                            "x-snappy-framed;q=1, bzip2;q=0.75, gzip;q=0.5, defalte;q=0");
  }

  @Test
  public void testSnappyCompression3()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "x-snappy-framed, *;q=0");
  }

  @Test
  public void testNoCompression()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "identity");
  }

  @Test
  public void testNoCompression2()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "");
  }

  @Test
  public void testNoCompression3()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(SMALL_URI, SMALL_BYTES_NUM, "foobar");
  }

  @Test
  public void testCompressionThreshold()
      throws InterruptedException, ExecutionException, TimeoutException
  {
    testResponseCompression(TINY_URI, TINY_BYTES_NUM, "x-snappy-framed");
  }

  @Test
  public void testBadEncoding()
      throws TimeoutException, InterruptedException
  {
    testEncodingNotAcceptable("foobar, identity;q=0");
  }

  private void testResponseCompression(URI uri, long bytes, String acceptEncoding)
      throws InterruptedException, TimeoutException, ExecutionException
  {
    StreamRequestBuilder builder = new StreamRequestBuilder((_clientProvider.createHttpURI(_port, uri)));
    builder.addHeaderValue(HttpConstants.ACCEPT_ENCODING, acceptEncoding);
    StreamRequest request = builder.build(EntityStreams.emptyStream());

    final FutureCallback<StreamResponse> callback = new FutureCallback<>();
    _client.streamRequest(request, callback);

    final StreamResponse response = callback.get(60, TimeUnit.SECONDS);
    Assert.assertEquals(response.getStatus(), RestStatus.OK);

    final FutureCallback<None> readerCallback = new FutureCallback<>();
    final BytesReader reader = new BytesReader(BYTE, readerCallback);
    final EntityStream decompressedStream = response.getEntityStream();
    decompressedStream.setReader(reader);

    readerCallback.get(60, TimeUnit.SECONDS);
    Assert.assertEquals(reader.getTotalBytes(), bytes);
    Assert.assertTrue(reader.allBytesCorrect());
  }

  public void testEncodingNotAcceptable(String acceptEncoding)
      throws TimeoutException, InterruptedException
  {
    StreamRequestBuilder builder = new StreamRequestBuilder((_clientProvider.createHttpURI(_port, SMALL_URI)));
    if (acceptEncoding != null)
    {
      builder.addHeaderValue(HttpConstants.ACCEPT_ENCODING, acceptEncoding);
    }
    StreamRequest request = builder.build(EntityStreams.emptyStream());

    final FutureCallback<StreamResponse> callback = new FutureCallback<>();
    _client.streamRequest(request, callback);
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

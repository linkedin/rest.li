/*
   Copyright (c) 2019 LinkedIn Corp.

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
import com.linkedin.common.util.None;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.netty.common.StreamingTimeout;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.test.util.retry.ThreeRetries;
import com.linkedin.util.clock.SystemClock;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.testng.Assert;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import test.r2.integ.clientserver.providers.AbstractServiceTest;
import test.r2.integ.clientserver.providers.ClientServerConfiguration;
import test.r2.integ.clientserver.providers.client.ClientProvider;
import test.r2.integ.clientserver.providers.server.ServerProvider;
import test.r2.integ.helper.BytesReader;
import test.r2.integ.helper.BytesWriter;
import test.r2.integ.helper.TimedBytesReader;


/**
 * @author Nizar Mankulangara
 */
public class TestStreamingTimeout extends AbstractServiceTest
{
  private static final String REQUEST_TIMEOUT_MESSAGE = "Exceeded request timeout of %sms";
  private static final URI NON_RATE_LIMITED_URI = URI.create("/large");
  private static final URI RATE_LIMITED_URI = URI.create("/rated-limited");
  private static final int HTTP_STREAMING_TIMEOUT = 1000;
  private static final int HTTP_REQUEST_TIMEOUT = 30000;
  private static RequestHandler _requestHandler;

  @Factory(dataProvider = "allPipelineV2StreamCombinations", dataProviderClass = ClientServerConfiguration.class)
  public TestStreamingTimeout(ClientProvider clientProvider, ServerProvider serverProvider, int port)
  {
    super(clientProvider, serverProvider, port);
  }

  @Override
  protected TransportDispatcher getTransportDispatcher()
  {
    _scheduler = Executors.newSingleThreadScheduledExecutor();
    _requestHandler = new RequestHandler(BYTE);

    return new TransportDispatcherBuilder()
        .addStreamHandler(NON_RATE_LIMITED_URI, _requestHandler)
        .addStreamHandler(RATE_LIMITED_URI, new StreamingTimeoutHandler(_scheduler, HTTP_STREAMING_TIMEOUT, BYTE))
        .build();
  }

  @Override
  protected Map<String, Object> getHttpClientProperties()
  {
    final Map<String, Object> clientProperties = new HashMap<>();
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(HTTP_REQUEST_TIMEOUT));
    clientProperties.put(HttpClientFactory.HTTP_STREAMING_TIMEOUT, String.valueOf(HTTP_STREAMING_TIMEOUT));
    return clientProperties;
  }

  @Test
  public void testStreamSuccessWithoutStreamingTimeout() throws Exception
  {
    final long totalBytes = TINY_BYTES_NUM;
    final EntityStream entityStream = EntityStreams.newEntityStream(new BytesWriter(totalBytes, BYTE));
    final StreamRequestBuilder builder = new StreamRequestBuilder(_clientProvider.createHttpURI(_port, NON_RATE_LIMITED_URI));
    final StreamRequest request = builder.setMethod("POST").build(entityStream);
    final AtomicInteger status = new AtomicInteger(-1);
    final CountDownLatch latch = new CountDownLatch(1);
    final Callback<StreamResponse> callback = expectSuccessCallback(latch, status);

    _client.streamRequest(request, callback);
    latch.await(HTTP_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);

    Assert.assertEquals(status.get(), RestStatus.OK);
    final BytesReader reader = _requestHandler.getReader();
    Assert.assertNotNull(reader);
    Assert.assertEquals(totalBytes, reader.getTotalBytes());
    Assert.assertTrue(reader.allBytesCorrect());
  }

  @Test(retryAnalyzer = ThreeRetries.class)
  public void testStreamTimeoutWithStreamingTimeoutInServerStream() throws Exception
  {
    final EntityStream entityStream = EntityStreams.newEntityStream(new BytesWriter(SMALL_BYTES_NUM, BYTE));
    final StreamRequestBuilder builder = new StreamRequestBuilder(_clientProvider.createHttpURI(_port, RATE_LIMITED_URI));
    final StreamRequest request = builder.setMethod("POST").build(entityStream);
    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    final Callback<StreamResponse> callback = expectErrorCallback(latch, throwable);

    _client.streamRequest(request, callback);
    latch.await(HTTP_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);

    Assert.assertNotNull(throwable.get());
    final Throwable rootCause = ExceptionUtils.getRootCause(throwable.get());
    Assert.assertTrue(rootCause instanceof TimeoutException);
    final TimeoutException timeoutException = (TimeoutException) rootCause;
    Assert.assertEquals(timeoutException.getMessage(), String.format(StreamingTimeout.STREAMING_TIMEOUT_MESSAGE, HTTP_STREAMING_TIMEOUT));
  }

  @Test
  public void testStreamTimeoutWhenGreaterThanRequestTimeout() throws Exception
  {
    final EntityStream entityStream = EntityStreams.newEntityStream(new BytesWriter(SMALL_BYTES_NUM, BYTE));
    final StreamRequestBuilder builder = new StreamRequestBuilder(_clientProvider.createHttpURI(_port, RATE_LIMITED_URI));
    final StreamRequest request = builder.setMethod("POST").build(entityStream);
    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    final Callback<StreamResponse> callback = expectErrorCallback(latch, throwable);

    Map<String, Object> clientProperties = getHttpClientProperties();
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(HTTP_STREAMING_TIMEOUT));
    clientProperties.put(HttpClientFactory.HTTP_STREAMING_TIMEOUT, String.valueOf(HTTP_STREAMING_TIMEOUT));
    Client client = _clientProvider.createClient(getClientFilterChain(), clientProperties);

    client.streamRequest(request, callback);
    latch.await(HTTP_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);

    Assert.assertNotNull(throwable.get());
    final Throwable rootCause = ExceptionUtils.getRootCause(throwable.get());
    Assert.assertTrue(rootCause instanceof TimeoutException);
    final TimeoutException timeoutException = (TimeoutException) rootCause;
    Assert.assertEquals(timeoutException.getMessage(), String.format(REQUEST_TIMEOUT_MESSAGE, HTTP_STREAMING_TIMEOUT));
    tearDown(client);
  }


  @Test
  public void testStreamTimeoutWithStreamTimeoutInClientStream() throws Exception
  {
    final EntityStream entityStream = EntityStreams.newEntityStream(new BytesWriter(LARGE_BYTES_NUM, BYTE){

      int count = 2;

      @Override
      protected void afterWrite(WriteHandle wh, long written)
      {
        count = count * 2;
        long delay = Math.min(count, HTTP_STREAMING_TIMEOUT);

        try
        {
          Thread.sleep(delay);
        }
        catch (Exception ex)
        {
          // Do Nothing
        }
      }
    });

    final StreamRequestBuilder builder = new StreamRequestBuilder(_clientProvider.createHttpURI(_port, NON_RATE_LIMITED_URI));
    final StreamRequest request = builder.setMethod("POST").build(entityStream);
    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    final Callback<StreamResponse> callback = expectErrorCallback(latch, throwable);

    _client.streamRequest(request, callback);
    latch.await(30000, TimeUnit.MILLISECONDS);

    Assert.assertNotNull(throwable.get());
    final Throwable rootCause = ExceptionUtils.getRootCause(throwable.get());
    Assert.assertTrue(rootCause instanceof TimeoutException);
    final TimeoutException timeoutException = (TimeoutException) rootCause;
    Assert.assertEquals(timeoutException.getMessage(), String.format(StreamingTimeout.STREAMING_TIMEOUT_MESSAGE, HTTP_STREAMING_TIMEOUT));
  }

  private static Callback<StreamResponse> expectSuccessCallback(final CountDownLatch latch, final AtomicInteger status)
  {
    return new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        latch.countDown();
      }

      @Override
      public void onSuccess(StreamResponse result)
      {
        status.set(result.getStatus());
        latch.countDown();
      }
    };
  }

  private static Callback<StreamResponse> expectErrorCallback(final CountDownLatch latch, final AtomicReference<Throwable> throwable)
  {
    return new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        throwable.set(e);
        latch.countDown();
      }

      @Override
      public void onSuccess(StreamResponse result)
      {
        latch.countDown();
      }
    };
  }

  private static class RequestHandler implements StreamRequestHandler
  {
    private final byte _b;
    private TimedBytesReader _reader;

    RequestHandler(byte b)
    {
      _b = b;
    }

    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext, final Callback<StreamResponse> callback)
    {
      Callback<None> readerCallback = new Callback<None>()
      {
        @Override
        public void onError(Throwable e)
        {
          RestException restException = new RestException(RestStatus.responseForError(500, e));
          callback.onError(restException);
        }

        @Override
        public void onSuccess(None result)
        {
          RestResponse response = RestStatus.responseForStatus(RestStatus.OK, "");
          callback.onSuccess(Messages.toStreamResponse(response));
        }
      };
      _reader = createReader(_b, readerCallback);
      request.getEntityStream().setReader(_reader);
    }

    TimedBytesReader getReader()
    {
      return _reader;
    }

    protected TimedBytesReader createReader(byte b, Callback<None> readerCallback)
    {
      return new TimedBytesReader(_b, readerCallback);
    }
  }

  private static class StreamingTimeoutHandler extends RequestHandler
  {
    private final ScheduledExecutorService _scheduler;
    private final long _maxDelay;

    StreamingTimeoutHandler(ScheduledExecutorService scheduler, long maxDelay, byte b)
    {
      super(b);
      _scheduler = scheduler;
      _maxDelay = maxDelay;
    }

    @Override
    protected TimedBytesReader createReader(byte b, Callback<None> readerCallback)
    {
      return new TimedBytesReader(b, readerCallback)
      {
        int count = 2;

        @Override
        public void requestMore(final ReadHandle rh)
        {
          count = count * 2 ;
          long delay = Math.min(count, _maxDelay);
          _scheduler.schedule(() -> rh.request(1), delay, TimeUnit.MILLISECONDS);
        }
      };
    }
  }
}

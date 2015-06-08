package test.r2.integ;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.data.ByteString;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Messages;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;

import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.server.HttpJettyServer;
import com.linkedin.r2.transport.http.server.HttpServerFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class tests client sending streaming request && server receiving streaming request
 *
 * @author Zhenkai Zhu
 */
public class TestStreamRequest extends AbstractStreamTest
{

  private static final URI LARGE_URI = URI.create("/large");
  private static final URI FOOBAR_URI = URI.create("/foobar");
  private static final URI RATE_LIMITED_URI = URI.create("/rated-limited");
  private static final URI ERROR_RECEIVER_URI = URI.create("/error-receiver");
  private CheckRequestHandler _checkRequestHandler;
  private RateLimitedRequestHandler _rateLimitedRequestHandler;
  private final HttpJettyServer.ServletType _servletType;


  @Factory(dataProvider = "configs")
  public TestStreamRequest(HttpJettyServer.ServletType servletType)
  {
    _servletType = servletType;
  }

  @DataProvider
  public static Object[][] configs()
  {
    return new Object[][] {{HttpJettyServer.ServletType.RAP}, {HttpJettyServer.ServletType.ASYNC_EVENT}};
  }

  @Override
  protected HttpServerFactory getServerFactory()
  {
    return new HttpServerFactory(_servletType);
  }

  @Override
  protected TransportDispatcher getTransportDispatcher()
  {
    _scheduler = Executors.newSingleThreadScheduledExecutor();
    _checkRequestHandler = new CheckRequestHandler(BYTE);
    _rateLimitedRequestHandler = new RateLimitedRequestHandler(_scheduler, INTERVAL, BYTE);
    return new TransportDispatcherBuilder()
        .addStreamHandler(LARGE_URI, _checkRequestHandler)
        .addStreamHandler(FOOBAR_URI, new CheckRequestHandler(BYTE))
        .addStreamHandler(RATE_LIMITED_URI, _rateLimitedRequestHandler)
        .addStreamHandler(ERROR_RECEIVER_URI, new ThrowWhenReceivingRequestHandler())
        .build();
  }

  @Override
  protected Map<String, String> getClientProperties()
  {
    Map<String, String> clientProperties = new HashMap<String, String>();
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "30000");
    return clientProperties;
  }

  @Test
  public void testRequestLarge() throws Exception
  {
    final long totalBytes = LARGE_BYTES_NUM;
    EntityStream entityStream = EntityStreams.newEntityStream(new BytesWriter(totalBytes, BYTE));
    StreamRequestBuilder builder = new StreamRequestBuilder(Bootstrap.createHttpURI(PORT, LARGE_URI));
    StreamRequest request = builder.setMethod("POST").build(entityStream);



    final AtomicInteger status = new AtomicInteger(-1);
    final CountDownLatch latch = new CountDownLatch(1);
    Callback<StreamResponse> callback = expectSuccessCallback(latch, status);
    _client.streamRequest(request, callback);
    latch.await(60000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(status.get(), RestStatus.OK);
    BytesReader reader = _checkRequestHandler.getReader();
    Assert.assertNotNull(reader);
    Assert.assertEquals(totalBytes, reader.getTotalBytes());
    Assert.assertTrue(reader.allBytesCorrect());
  }

  // jetty 404 tests singled out
  @Test(enabled = false)
  public void test404() throws Exception
  {
    final long totalBytes = TINY_BYTES_NUM;
    EntityStream entityStream = EntityStreams.newEntityStream(new BytesWriter(totalBytes, BYTE));
    StreamRequestBuilder builder = new StreamRequestBuilder(Bootstrap.createHttpURI(PORT, URI.create("/boo")));
    StreamRequest request = builder.setMethod("POST").build(entityStream);
    final AtomicInteger status = new AtomicInteger(-1);
    final CountDownLatch latch = new CountDownLatch(1);
    Callback<StreamResponse> callback = expectErrorCallback(latch, status);
    _client.streamRequest(request, callback);
    latch.await(60000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(status.get(), 404);
  }

  @Test
  public void testErrorWriter() throws Exception
  {
    final long totalBytes = SMALL_BYTES_NUM;
    EntityStream entityStream = EntityStreams.newEntityStream(new ErrorWriter(totalBytes, BYTE));
    StreamRequestBuilder builder = new StreamRequestBuilder(Bootstrap.createHttpURI(PORT, FOOBAR_URI));
    StreamRequest request = builder.setMethod("POST").build(entityStream);
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
    Callback<StreamResponse> callback = new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        error.set(e);
        latch.countDown();
      }

      @Override
      public void onSuccess(StreamResponse result)
      {
        latch.countDown();
      }
    };
    _client.streamRequest(request, callback);
    latch.await();
    Assert.assertNotNull(error.get());
  }

  @Test
  public void testErrorReceiver() throws Exception
  {
    final long totalBytes = SMALL_BYTES_NUM;
    EntityStream entityStream = EntityStreams.newEntityStream(new BytesWriter(totalBytes, BYTE));
    StreamRequestBuilder builder = new StreamRequestBuilder(Bootstrap.createHttpURI(PORT, ERROR_RECEIVER_URI));
    StreamRequest request = builder.setMethod("POST").build(entityStream);
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
    Callback<StreamResponse> callback = new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        error.set(e);
        latch.countDown();
      }

      @Override
      public void onSuccess(StreamResponse result)
      {
        latch.countDown();
      }
    };
    _client.streamRequest(request, callback);
    latch.await();
    Assert.assertNotNull(error.get());
  }

  @Test
  public void testBackPressure() throws Exception
  {
    final long totalBytes = SMALL_BYTES_NUM;
    TimedBytesWriter writer = new TimedBytesWriter(totalBytes, BYTE);
    EntityStream entityStream = EntityStreams.newEntityStream(writer);
    StreamRequestBuilder builder = new StreamRequestBuilder(Bootstrap.createHttpURI(PORT, RATE_LIMITED_URI));
    StreamRequest request = builder.setMethod("POST").build(entityStream);
    final AtomicInteger status = new AtomicInteger(-1);
    final CountDownLatch latch = new CountDownLatch(1);
    Callback<StreamResponse> callback = expectSuccessCallback(latch, status);
    _client.streamRequest(request, callback);
    latch.await(60000, TimeUnit.MILLISECONDS);
    Assert.assertEquals(status.get(), RestStatus.OK);
    TimedBytesReader reader = _rateLimitedRequestHandler.getReader();
    Assert.assertNotNull(reader);
    Assert.assertEquals(totalBytes, reader.getTotalBytes());
    Assert.assertTrue(reader.allBytesCorrect());
    long clientSendTimespan = writer.getStopTime() - writer.getStartTime();
    long serverReceiveTimespan = reader.getStopTime() - reader.getStartTime();
    Assert.assertTrue(serverReceiveTimespan > 1000);
    double diff = Math.abs(serverReceiveTimespan - clientSendTimespan);
    double diffRatio = diff / clientSendTimespan;
    // make it generous to reduce the chance occasional test failures
    Assert.assertTrue(diffRatio < 0.2);
  }

  private static class CheckRequestHandler implements StreamRequestHandler
  {
    private final byte _b;
    private TimedBytesReader _reader;

    CheckRequestHandler(byte b)
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

  private static class RateLimitedRequestHandler extends CheckRequestHandler
  {
    private final ScheduledExecutorService _scheduler;
    private final long _interval;


    RateLimitedRequestHandler(ScheduledExecutorService scheduler, long interval, byte b)
    {
      super((b));
      _scheduler = scheduler;
      _interval = interval;
    }

    @Override
    protected TimedBytesReader createReader(byte b, Callback<None> readerCallback)
    {
      return new TimedBytesReader(b, readerCallback)
      {
        int count = 0;

        @Override
        public void requestMore(final ReadHandle rh)
        {
          count++;
          if (count % 16 == 0)
          {
            _scheduler.schedule(new Runnable()
            {
              @Override
              public void run()
              {
                rh.request(1);
              }
            },_interval, TimeUnit.MILLISECONDS);
          }
          else
          {
            rh.request(1);
          }
        }
      };
    }
  }

  private static class ThrowWhenReceivingRequestHandler implements StreamRequestHandler
  {
    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext, final Callback<StreamResponse> callback)
    {
      request.getEntityStream().setReader(new Reader()
      {
        ReadHandle _rh;

        @Override
        public void onInit(ReadHandle rh)
        {
          _rh = rh;
          _rh.request(10);
        }

        @Override
        public void onDataAvailable(ByteString data)
        {
          throw new RuntimeException("some exception throw due to bug");
        }

        @Override
        public void onDone()
        {

        }

        @Override
        public void onError(Throwable e)
        {

        }
      });
    }
  }

  private static Callback<StreamResponse> expectErrorCallback(final CountDownLatch latch, final AtomicInteger status)
  {
    return new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        if (e instanceof StreamException)
        {
          StreamResponse errorResponse = ((StreamException) e).getResponse();
          status.set(errorResponse.getStatus());
        }
        latch.countDown();
      }

      @Override
      public void onSuccess(StreamResponse result)
      {
        latch.countDown();
        throw new RuntimeException("Should have failed with 404");
      }
    };
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


  private static class ErrorWriter extends TimedBytesWriter
  {
    private long _total;

    ErrorWriter(long total, byte fill)
    {
      super(total * 2, fill);
      _total = total;
    }

    @Override
    protected void afterWrite(WriteHandle wh, long written)
    {
      if (written > _total)
      {
        _total = _total * 2;
        wh.error(new RuntimeException("Error for testing"));
      }
    }
  }
}

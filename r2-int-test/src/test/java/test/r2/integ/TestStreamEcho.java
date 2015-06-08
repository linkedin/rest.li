package test.r2.integ;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rest.RestStatus;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.RestRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.StreamRequestHandlerAdapter;
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
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Zhenkai Zhu
 */
public class TestStreamEcho extends AbstractStreamTest
{
  private static final URI ECHO_URI = URI.create("/echo");
  private static final URI ASYNC_ECHO_URI = URI.create("/async-echo");
  private static final URI DELAYED_ECHO_URI = URI.create("/delayed-echo");

  private final HttpJettyServer.ServletType _servletType;


  @Factory(dataProvider = "configs")
  public TestStreamEcho(HttpJettyServer.ServletType servletType)
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
    return new TransportDispatcherBuilder(Collections.<URI, RestRequestHandler>emptyMap(), getHandlers(), true)
        .build();
  }

  protected Map<URI, StreamRequestHandler> getHandlers()
  {
    Map<URI, StreamRequestHandler> handlers = new HashMap<URI, StreamRequestHandler>();
    handlers.put(ECHO_URI, new SteamEchoHandler());
    handlers.put(ASYNC_ECHO_URI, new SteamAsyncEchoHandler(_scheduler));
    handlers.put(DELAYED_ECHO_URI, new StreamRequestHandlerAdapter(new DelayedStoreAndForwardEchoHandler()));
    return handlers;
  }

  @Override
  protected Map<String, String> getClientProperties()
  {
    Map<String, String> clientProperties = new HashMap<String, String>();
    clientProperties.put(HttpClientFactory.HTTP_MAX_RESPONSE_SIZE, String.valueOf(LARGE_BYTES_NUM * 2));
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "30000");
    return clientProperties;
  }

  @Test
  public void testNormalEchoSmall() throws Exception
  {
    testNormalEcho(SMALL_BYTES_NUM, ECHO_URI);
  }

  @Test
  public void testNormalEchoLarge() throws Exception
  {
    testNormalEcho(LARGE_BYTES_NUM, ECHO_URI);
  }

  @Test
  public void testNormalAsyncEchoSmall() throws Exception
  {
    testNormalEcho(SMALL_BYTES_NUM, ASYNC_ECHO_URI);
  }

  @Test
  public void testNormalAsyncEchoLarge() throws Exception
  {
    testNormalEcho(LARGE_BYTES_NUM, ASYNC_ECHO_URI);
  }

  private void testNormalEcho(long bytesNum, URI uri) throws Exception
  {
    BytesWriter writer = new BytesWriter(bytesNum, BYTE);
    StreamRequest request = new StreamRequestBuilder(Bootstrap.createHttpURI(PORT, uri))
        .build(EntityStreams.newEntityStream(writer));

    final AtomicInteger status = new AtomicInteger(-1);
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

    final Callback<None> readerCallback = getReaderCallback(latch, error);

    final BytesReader reader = new BytesReader(BYTE, readerCallback);

    Callback<StreamResponse> callback = getCallback(status, readerCallback, reader);

    _client.streamRequest(request, callback);
    latch.await(60000, TimeUnit.MILLISECONDS);
    Assert.assertNull(error.get());
    Assert.assertEquals(status.get(), RestStatus.OK);
    Assert.assertEquals(reader.getTotalBytes(), bytesNum);
    Assert.assertTrue(reader.allBytesCorrect());
  }

  @Test
  public void testBackPressureEcho() throws Exception
  {
    TimedBytesWriter writer = new TimedBytesWriter(SMALL_BYTES_NUM, BYTE);
    StreamRequest request = new StreamRequestBuilder(Bootstrap.createHttpURI(PORT, ECHO_URI))
        .build(EntityStreams.newEntityStream(writer));

    final AtomicInteger status = new AtomicInteger(-1);
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Throwable> error = new AtomicReference<Throwable>();

    final Callback<None> readerCallback = getReaderCallback(latch, error);

    final TimedBytesReader reader = new TimedBytesReader(BYTE, readerCallback)
    {
      int count = 0;

      @Override
      protected void requestMore(final ReadHandle rh)
      {
        count ++;
        if (count % 16 == 0)
        {
          _scheduler.schedule(new Runnable()
          {
            @Override
            public void run()
            {
              rh.request(1);
            }
          }, INTERVAL, TimeUnit.MILLISECONDS);
        }
        else
        {
          rh.request(1);
        }
      }
    };

    Callback<StreamResponse> callback = getCallback(status, readerCallback, reader);

    _client.streamRequest(request, callback);
    latch.await(60000, TimeUnit.MILLISECONDS);
    Assert.assertNull(error.get());
    Assert.assertEquals(status.get(), RestStatus.OK);
    Assert.assertEquals(reader.getTotalBytes(), SMALL_BYTES_NUM);
    Assert.assertTrue(reader.allBytesCorrect());

    long clientSendTimespan = writer.getStopTime()- writer.getStartTime();
    long clientReceiveTimespan = reader.getStopTime() - reader.getStartTime();
    double diff = Math.abs(clientReceiveTimespan - clientSendTimespan);
    double diffRatio = diff / clientSendTimespan;
    // make it generous to reduce the chance occasional test failures
    Assert.assertTrue(diffRatio < 0.2);
  }

  @Test
  public void testDelayedEcho() throws Exception
  {
    RestRequest restRequest = new RestRequestBuilder(Bootstrap.createHttpURI(PORT, DELAYED_ECHO_URI))
        .setEntity("wei ni hao ma?".getBytes()).build();
    RestResponse response = _client.restRequest(restRequest).get();
    Assert.assertEquals(response.getEntity().asString(Charset.defaultCharset()), "wei ni hao ma?");
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

  private static class SteamAsyncEchoHandler implements StreamRequestHandler
  {
    private final ScheduledExecutorService _scheduler;

    SteamAsyncEchoHandler(ScheduledExecutorService scheduler)
    {
      _scheduler = scheduler;
    }

    @Override
    public void handleRequest(final StreamRequest request, RequestContext requestContext, final Callback<StreamResponse> callback)
    {
      _scheduler.schedule(new Runnable()
      {
        @Override
        public void run()
        {
          StreamResponseBuilder builder = new StreamResponseBuilder();
          callback.onSuccess(builder.build(request.getEntityStream()));
        }
      }, 10, TimeUnit.MILLISECONDS);
    }
  }

  private Callback<None> getReaderCallback(final CountDownLatch latch, final AtomicReference<Throwable> error)
  {
    return new Callback<None>()
    {
      @Override
      public void onError(Throwable e)
      {
        error.set(e);
        latch.countDown();
      }

      @Override
      public void onSuccess(None result)
      {
        latch.countDown();
      }
    };
  }

  private Callback<StreamResponse> getCallback(final AtomicInteger status, final Callback<None> readerCallback, final BytesReader reader)
  {
    return new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        readerCallback.onError(e);
      }

      @Override
      public void onSuccess(StreamResponse result)
      {
        status.set(result.getStatus());
        result.getEntityStream().setReader(reader);
      }
    };
  }

  private class DelayedStoreAndForwardEchoHandler implements RestRequestHandler
  {
    @Override
    public void handleRequest(final RestRequest request, RequestContext requestContext, final Callback<RestResponse> callback)
    {
      RestResponse restResponse = new RestResponseBuilder().setEntity(request.getEntity()).build();
      callback.onSuccess(restResponse);
    }
  }
}

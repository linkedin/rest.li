package test.r2.integ;

import com.linkedin.common.callback.Callback;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.DrainReader;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.StreamRequestHandler;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;
import com.linkedin.r2.transport.common.bridge.server.TransportDispatcherBuilder;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Zhenkai Zhu
 */
public class TestClientTimeout extends AbstractStreamTest
{
  private static final URI TIMEOUT_BEFORE_RESPONSE_URI = URI.create("/timeout-before-response");
  private static final URI TIMEOUT_DURING_RESPONSE_URI = URI.create("/timeout-during-response");
  private static final URI NORMAL_URI = URI.create("/normal");

  @Override
  protected TransportDispatcher getTransportDispatcher()
  {
    _scheduler = Executors.newSingleThreadScheduledExecutor();
    return new TransportDispatcherBuilder()
        .addStreamHandler(TIMEOUT_BEFORE_RESPONSE_URI, new DelayBeforeResponseHandler())
        .addStreamHandler(TIMEOUT_DURING_RESPONSE_URI, new DelayDuringResponseHandler())
        .addStreamHandler(NORMAL_URI, new NormalHandler())
        .build();
  }

  @Override
  protected Map<String, String> getClientProperties()
  {
    Map<String, String> clientProperties = new HashMap<String, String>();
    clientProperties.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, "500");
    return clientProperties;
  }

  @Test
  public void testTimeoutBeforeResponse() throws Exception
  {
    Future<RestResponse> future = _client.restRequest(
        new RestRequestBuilder(Bootstrap.createHttpURI(PORT, TIMEOUT_BEFORE_RESPONSE_URI)).build());
    try
    {
      future.get(1000, TimeUnit.MILLISECONDS);
      Assert.fail("should have timed out");
    }
    catch (ExecutionException ex)
    {
      Throwable throwable = ExceptionUtils.getRootCause(ex);
      Assert.assertTrue(throwable instanceof TimeoutException);
      // should fail with not getting a response
      Assert.assertEquals(throwable.getMessage(), "Exceeded request timeout of 500ms");
    }
  }

  @Test
  public void testTimeoutDuringResponse() throws Exception
  {
    Future<RestResponse> future = _client.restRequest(
        new RestRequestBuilder(Bootstrap.createHttpURI(PORT, TIMEOUT_DURING_RESPONSE_URI)).build());
    try
    {
      RestResponse res = future.get(1000, TimeUnit.MILLISECONDS);
      Assert.fail("should have timed out");
    }
    catch (ExecutionException ex)
    {
      Throwable throwable = ExceptionUtils.getRootCause(ex);
      Assert.assertTrue(throwable instanceof TimeoutException);
      // should fail with timeout while streaming response
      Assert.assertEquals(throwable.getMessage(), "Timeout while receiving the response entity.");
    }
  }

  @Test
  public void testReadAfterTimeout() throws Exception
  {
    StreamRequest request = new StreamRequestBuilder(Bootstrap.createHttpURI(PORT, NORMAL_URI)).build(EntityStreams.emptyStream());
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<StreamResponse> response = new AtomicReference<StreamResponse>();
    _client.streamRequest(request, new Callback<StreamResponse>()
    {
      @Override
      public void onError(Throwable e)
      {
        latch.countDown();
      }

      @Override
      public void onSuccess(StreamResponse result)
      {
        response.set(result);
        latch.countDown();
      }
    });
    latch.await(500, TimeUnit.MILLISECONDS);
    Assert.assertNotNull(response.get());

    // let it timeout before we read
    Thread.sleep(600);

    final AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();
    final CountDownLatch errorLatch = new CountDownLatch(1);
    Reader reader = new DrainReader()
    {
      @Override
      public void onError(Throwable ex)
      {
        throwable.set(ex);
        errorLatch.countDown();
      }
    };
    response.get().getEntityStream().setReader(reader);
    errorLatch.await(500, TimeUnit.MILLISECONDS);
    Assert.assertNotNull(throwable.get());
    Throwable rootCause = ExceptionUtils.getRootCause(throwable.get());
    Assert.assertTrue(rootCause instanceof TimeoutException);
    Assert.assertEquals(rootCause.getMessage(), "Timeout while receiving the response entity.");
  }

  private class DelayBeforeResponseHandler implements StreamRequestHandler
  {
    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext, final Callback<StreamResponse> callback)
    {
      request.getEntityStream().setReader(new DrainReader());
      _scheduler.schedule(new Runnable()
      {
        @Override
        public void run()
        {
          callback.onSuccess(new StreamResponseBuilder().build(EntityStreams.emptyStream()));
        }
      }, 600, TimeUnit.MILLISECONDS);
    }
  }

  private class DelayDuringResponseHandler implements StreamRequestHandler
  {
    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext, final Callback<StreamResponse> callback)
    {
      request.getEntityStream().setReader(new DrainReader());
      Writer writer = new BytesWriter(100 * 1024, BYTE)
      {
        private final AtomicBoolean _slept = new AtomicBoolean(false);
        @Override
        protected void afterWrite(WriteHandle wh, long written)
        {

          if (written > 50 * 1024 && _slept.compareAndSet(false, true))
          {
            try
            {
              Thread.sleep(600);
            }
            catch (Exception ex)
            {
              // do nothing
            }
          }
        }
      };
      callback.onSuccess(new StreamResponseBuilder().build(EntityStreams.newEntityStream(writer)));
    }
  }


  private class NormalHandler implements StreamRequestHandler
  {
    @Override
    public void handleRequest(StreamRequest request, RequestContext requestContext, final Callback<StreamResponse> callback)
    {
      request.getEntityStream().setReader(new DrainReader());
      callback.onSuccess(new StreamResponseBuilder().build(EntityStreams.newEntityStream(new BytesWriter(1024 * 100, (byte) 100))));
    }
  }

}

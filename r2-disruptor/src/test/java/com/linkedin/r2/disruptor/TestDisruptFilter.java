/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.r2.disruptor;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.util.clock.SystemClock;
import com.linkedin.util.clock.SettableClock;

import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;


/**
 * @author Sean Sheng
 * @version $Revision$
 */
public class TestDisruptFilter
{
  private static final String DISRUPT_CONTEXT_KEY = "R2_DISRUPT_CONTEXT";

  private static final int SCHEDULER_THREADS = 1;
  private static final int EXECUTOR_THREADS = 1;
  private static final int TEST_TIMEOUT = 5000;
  private static final String URI = "http://foo.com/";

  private static final int REQUEST_TIMEOUT = 0;
  private static final long REQUEST_LATENCY = 0;
  private static final long MINIMUM_LATENCY = 20;

  private final ScheduledExecutorService _scheduler = new ScheduledThreadPoolExecutor(SCHEDULER_THREADS);
  private final ExecutorService _executor = Executors.newFixedThreadPool(EXECUTOR_THREADS);
  private SettableClock _clock = new SettableClock();

  @AfterClass
  public void doAfterClass()
  {
    _scheduler.shutdown();
    _executor.shutdown();
  }

  @Test
  public void testRestLatencyDisrupt() throws Exception
  {
    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.delay(REQUEST_LATENCY));

    final DisruptFilter filter = new DisruptFilter(_scheduler, _executor, REQUEST_TIMEOUT, _clock);
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    final NextFilter<RestRequest, RestResponse> next = new NextFilter<RestRequest, RestResponse>()
    {
      @Override
      public void onRequest(RestRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        success.set(true);
        latch.countDown();
      }

      @Override
      public void onResponse(RestResponse restResponse, RequestContext requestContext,
          Map<String, String> wireAttrs)
      {
        latch.countDown();
      }

      @Override
      public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        latch.countDown();
      }
    };

    filter.onRestRequest(new RestRequestBuilder(new URI(URI)).build(), requestContext, Collections.emptyMap(), next);
    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Missing NextFilter invocation");
    Assert.assertTrue(success.get(), "Unexpected method invocation");
  }

  @Test
  public void testStreamLatencyDisrupt() throws Exception
  {
    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.delay(REQUEST_LATENCY));

    final DisruptFilter filter = new DisruptFilter(_scheduler, _executor, REQUEST_TIMEOUT, _clock);
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    final NextFilter<StreamRequest, StreamResponse> next = new NextFilter<StreamRequest, StreamResponse>()
    {
      @Override
      public void onRequest(StreamRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        success.set(true);
        latch.countDown();
      }

      @Override
      public void onResponse(StreamResponse restResponse, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        latch.countDown();
      }

      @Override
      public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        latch.countDown();
      }
    };

    filter.onStreamRequest(new StreamRequestBuilder(new URI(URI)).build(EntityStreams.emptyStream()), requestContext,
        Collections.emptyMap(), next);
    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Missing NextFilter invocation");
    Assert.assertTrue(success.get(), "Unexpected method invocation");
  }

  @Test
  public void testMinimumDelayRealDelayLessThanSpecified() throws Exception {
    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.minimumDelay(MINIMUM_LATENCY));

    final DisruptFilter filter = new DisruptFilter(_scheduler, _executor, REQUEST_TIMEOUT, _clock);
    final CountDownLatch latch = new CountDownLatch(2);
    final AtomicBoolean onRequestSuccess = new AtomicBoolean(false);
    final AtomicBoolean onResponseSuccess = new AtomicBoolean(false);
    final NextFilter<RestRequest, RestResponse> next = new NextFilter<RestRequest, RestResponse>() {
      @Override
      public void onRequest(RestRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs) {
        onRequestSuccess.set(true);
        latch.countDown();
      }

      @Override
      public void onResponse(RestResponse restResponse, RequestContext requestContext, Map<String, String> wireAttrs) {
        onResponseSuccess.set(true);
        latch.countDown();
      }

      @Override
      public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs) {
        Assert.fail("onError should not be called.");
      }
    };

    long start = SystemClock.instance().currentTimeMillis();
    filter.onRestRequest(new RestRequestBuilder(new URI(URI)).build(), requestContext, Collections.emptyMap(), next);
    filter.onRestResponse(new RestResponseBuilder().build(), requestContext, Collections.emptyMap(), next);
    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Missing NextFilter invocation");
    Assert.assertTrue(SystemClock.instance().currentTimeMillis() - start >= MINIMUM_LATENCY,
        "total elapsed time should be longer than the specified minimum delay.");
    Assert.assertTrue(onRequestSuccess.get(), "Unexpected method invocation");
    Assert.assertTrue(onResponseSuccess.get(), "Unexpected method invocation");
  }

  @Test
  public void testMinimumDelayRealDelayMoreThanSpecified() throws Exception {
    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.minimumDelay(MINIMUM_LATENCY));

    final DisruptFilter filter = new DisruptFilter(_scheduler, _executor, REQUEST_TIMEOUT, _clock);
    final CountDownLatch latch = new CountDownLatch(2);
    final AtomicBoolean onRequestSuccess = new AtomicBoolean(false);
    final AtomicBoolean onResponseSuccess = new AtomicBoolean(false);
    final NextFilter<RestRequest, RestResponse> next = new NextFilter<RestRequest, RestResponse>() {
      @Override
      public void onRequest(RestRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs) {
        onRequestSuccess.set(true);
        latch.countDown();
      }

      @Override
      public void onResponse(RestResponse restResponse, RequestContext requestContext, Map<String, String> wireAttrs) {
        onResponseSuccess.set(true);
        latch.countDown();
      }

      @Override
      public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs) {
        Assert.fail("onError should not be called.");
      }
    };

    long currentTimeMs = 100;
    _clock.setCurrentTimeMillis(currentTimeMs);
    filter.onRestRequest(new RestRequestBuilder(new URI(URI)).build(), requestContext, Collections.emptyMap(), next);

    // Simulates that real processing took longer than the specified MINIMUM_LATENCY.
    _clock.setCurrentTimeMillis(currentTimeMs + MINIMUM_LATENCY);
    filter.onRestResponse(new RestResponseBuilder().build(), requestContext, Collections.emptyMap(), next);

    // Since the real processing is simulated and no delay should be added, we expect nextFilter should be invoked soon.
    Assert.assertTrue(latch.await(10, TimeUnit.MILLISECONDS), "Missing NextFilter invocation");
    Assert.assertTrue(onRequestSuccess.get(), "Unexpected method invocation");
    Assert.assertTrue(onResponseSuccess.get(), "Unexpected method invocation");
  }

  @Test
  public void testMinimumDelayNoRequestStartTime() throws Exception {
    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.minimumDelay(MINIMUM_LATENCY));

    final DisruptFilter filter = new DisruptFilter(_scheduler, _executor, REQUEST_TIMEOUT, _clock);
    final CountDownLatch latch = new CountDownLatch(2);
    final AtomicBoolean onRequestSuccess = new AtomicBoolean(false);
    final AtomicBoolean onResponseSuccess = new AtomicBoolean(false);
    final NextFilter<RestRequest, RestResponse> next = new NextFilter<RestRequest, RestResponse>() {
      @Override
      public void onRequest(RestRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs) {
        onRequestSuccess.set(true);
        latch.countDown();
      }

      @Override
      public void onResponse(RestResponse restResponse, RequestContext requestContext, Map<String, String> wireAttrs) {
        onResponseSuccess.set(true);
        latch.countDown();
      }

      @Override
      public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs) {
        Assert.fail("onError should not be called.");
      }
    };

    long start = SystemClock.instance().currentTimeMillis();
    filter.onRestRequest(new RestRequestBuilder(new URI(URI)).build(), requestContext, Collections.emptyMap(), next);
    ((DisruptContexts.MinimumDelayDisruptContext)
        requestContext.getLocalAttr(DisruptContext.DISRUPT_CONTEXT_KEY)).requestStartTime(0);
    filter.onRestResponse(new RestResponseBuilder().build(), requestContext, Collections.emptyMap(), next);
    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Missing NextFilter invocation");
    Assert.assertTrue(SystemClock.instance().currentTimeMillis() - start < MINIMUM_LATENCY,
        "Delay should not be added if request start time is not logged");
    Assert.assertTrue(onRequestSuccess.get(), "Unexpected method invocation");
    Assert.assertTrue(onResponseSuccess.get(), "Unexpected method invocation");
  }

  @Test
  public void testRestTimeoutDisrupt() throws Exception
  {
    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.timeout());

    final DisruptFilter filter = new DisruptFilter(_scheduler, _executor, REQUEST_TIMEOUT, _clock);
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    final NextFilter<RestRequest, RestResponse> next = new NextFilter<RestRequest, RestResponse>()
    {
      @Override
      public void onRequest(RestRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        latch.countDown();
      }

      @Override
      public void onResponse(RestResponse restResponse, RequestContext requestContext,
          Map<String, String> wireAttrs)
      {
        latch.countDown();
      }

      @Override
      public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        success.set(ex instanceof TimeoutException);
        latch.countDown();
      }
    };

    filter.onRestRequest(new RestRequestBuilder(new URI(URI)).build(), requestContext, Collections.emptyMap(), next);
    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Missing NextFilter invocation");
    Assert.assertTrue(success.get(), "Unexpected method invocation");
  }

  @Test
  public void testStreamTimeoutDisrupt() throws Exception
  {
    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.timeout());

    final DisruptFilter filter = new DisruptFilter(_scheduler, _executor, REQUEST_TIMEOUT, _clock);
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    final NextFilter<StreamRequest, StreamResponse> next = new NextFilter<StreamRequest, StreamResponse>()
    {
      @Override
      public void onRequest(StreamRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        latch.countDown();
      }

      @Override
      public void onResponse(StreamResponse restResponse, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        latch.countDown();
      }

      @Override
      public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        success.set(ex instanceof TimeoutException);
        latch.countDown();
      }
    };

    filter.onStreamRequest(new StreamRequestBuilder(new URI(URI)).build(EntityStreams.emptyStream()), requestContext,
        Collections.emptyMap(), next);
    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Missing NextFilter invocation");
    Assert.assertTrue(success.get(), "Unexpected method invocation");
  }

  @Test
  public void testRestErrorDisrupt() throws Exception
  {
    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.error(REQUEST_LATENCY));

    final DisruptFilter filter = new DisruptFilter(_scheduler, _executor, REQUEST_TIMEOUT, _clock);
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    final NextFilter<RestRequest, RestResponse> next = new NextFilter<RestRequest, RestResponse>()
    {
      @Override
      public void onRequest(RestRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        latch.countDown();
      }

      @Override
      public void onResponse(RestResponse restResponse, RequestContext requestContext,
          Map<String, String> wireAttrs)
      {
        latch.countDown();
      }

      @Override
      public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        success.set(ex instanceof DisruptedException);
        latch.countDown();
      }
    };

    filter.onRestRequest(new RestRequestBuilder(new URI(URI)).build(), requestContext, Collections.emptyMap(), next);
    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Missing NextFilter invocation");
    Assert.assertTrue(success.get(), "Unexpected method invocation");
  }

  @Test
  public void testStreamErrorDisrupt() throws Exception
  {
    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.error(REQUEST_LATENCY));

    final DisruptFilter filter = new DisruptFilter(_scheduler, _executor, REQUEST_TIMEOUT, _clock);
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    final NextFilter<StreamRequest, StreamResponse> next = new NextFilter<StreamRequest, StreamResponse>()
    {
      @Override
      public void onRequest(StreamRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        latch.countDown();
      }

      @Override
      public void onResponse(StreamResponse restResponse, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        latch.countDown();
      }

      @Override
      public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        success.set(ex instanceof DisruptedException);
        latch.countDown();
      }
    };

    filter.onStreamRequest(new StreamRequestBuilder(new URI(URI)).build(EntityStreams.emptyStream()), requestContext,
        Collections.emptyMap(), next);
    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Missing NextFilter invocation");
    Assert.assertTrue(success.get(), "Unexpected method invocation");
  }

  @Test
  public void testSchedulerRejectExecution() throws Exception
  {
    ScheduledExecutorService rejectedScheduler = EasyMock.createStrictMock(ScheduledExecutorService.class);
    EasyMock.expect(rejectedScheduler.schedule(
        EasyMock.anyObject(Runnable.class),
        EasyMock.anyLong(),
        EasyMock.anyObject(TimeUnit.class))).andThrow(new RejectedExecutionException());

    EasyMock.replay(rejectedScheduler);

    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.error(REQUEST_LATENCY));

    final DisruptFilter filter = new DisruptFilter(rejectedScheduler, _executor, REQUEST_TIMEOUT, _clock);
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicBoolean success = new AtomicBoolean(false);
    final NextFilter<StreamRequest, StreamResponse> next = new NextFilter<StreamRequest, StreamResponse>()
    {
      @Override
      public void onRequest(StreamRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        success.set(true);
        latch.countDown();
      }

      @Override
      public void onResponse(StreamResponse restResponse, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        latch.countDown();
      }

      @Override
      public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        latch.countDown();
      }
    };

    filter.onStreamRequest(new StreamRequestBuilder(
        new URI(URI)).build(EntityStreams.emptyStream()),
        requestContext,
        Collections.emptyMap(),
        next);
    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Missing NextFilter invocation");
    Assert.assertTrue(success.get(), "Unexpected method invocation");

    EasyMock.verify(rejectedScheduler);
    EasyMock.reset(rejectedScheduler);
  }

  @Test
  public void testExecutorRejectExecution() throws Exception
  {
    final AtomicBoolean success = new AtomicBoolean(false);
    final CountDownLatch latch = new CountDownLatch(1);

    ExecutorService rejectedExecutor = EasyMock.createStrictMock(ExecutorService.class);
    rejectedExecutor.execute(EasyMock.anyObject(Runnable.class));
    EasyMock.expectLastCall().andAnswer(() -> {
      success.set(true);
      latch.countDown();
      throw new RejectedExecutionException();
    });

    EasyMock.replay(rejectedExecutor);

    final RequestContext requestContext = new RequestContext();
    requestContext.putLocalAttr(DISRUPT_CONTEXT_KEY, DisruptContexts.error(REQUEST_LATENCY));

    final DisruptFilter filter = new DisruptFilter(_scheduler, rejectedExecutor, REQUEST_TIMEOUT, _clock);
    final NextFilter<StreamRequest, StreamResponse> next = new NextFilter<StreamRequest, StreamResponse>()
    {
      @Override
      public void onRequest(StreamRequest restRequest, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        success.set(false);
        latch.countDown();
      }

      @Override
      public void onResponse(StreamResponse restResponse, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        success.set(false);
        latch.countDown();
      }

      @Override
      public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs)
      {
        success.set(false);
        latch.countDown();
      }
    };

    filter.onStreamRequest(new StreamRequestBuilder(
            new URI(URI)).build(EntityStreams.emptyStream()),
        requestContext,
        Collections.emptyMap(), next);
    Assert.assertTrue(latch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS), "Missing NextFilter invocation");
    Assert.assertTrue(success.get(), "Unexpected method invocation");

    EasyMock.verify(rejectedExecutor);
    EasyMock.reset(rejectedExecutor);
  }
}

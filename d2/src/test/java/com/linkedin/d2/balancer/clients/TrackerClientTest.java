/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.d2.balancer.clients;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.data.ByteString;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.filter.R2Constants;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.r2.message.stream.entitystream.DrainReader;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SettableClock;
import com.linkedin.util.degrader.CallTracker;
import com.linkedin.util.degrader.DegraderControl;
import com.linkedin.util.degrader.DegraderImpl;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;

public class TrackerClientTest
{
  @Test(groups = { "small", "back-end" })
  public void testClientStreamRequest() throws URISyntaxException
  {
    URI uri = URI.create("http://test.qa.com:1234/foo");
    double weight = 3d;
    TestClient wrappedClient = new TestClient(true);
    Clock clock = new SettableClock();
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(2);
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(3d));
    TrackerClient client = new TrackerClient(uri, partitionDataMap, wrappedClient, clock, null);

    assertEquals(client.getUri(), uri);
    Double clientWeight = client.getPartitionWeight(DefaultPartitionAccessor.DEFAULT_PARTITION_ID);
    assertEquals(clientWeight, weight);
    assertEquals(client.getWrappedClient(), wrappedClient);

    StreamRequest streamRequest = new StreamRequestBuilder(uri).build(EntityStreams.emptyStream());
    Map<String, String> restWireAttrs = new HashMap<String, String>();
    TestTransportCallback<StreamResponse> restCallback =
        new TestTransportCallback<StreamResponse>();

    client.streamRequest(streamRequest, new RequestContext(), restWireAttrs, restCallback);

    assertFalse(restCallback.response.hasError());
    assertSame(wrappedClient.streamRequest, streamRequest);
    assertEquals(wrappedClient.restWireAttrs, restWireAttrs);
  }

  @Test(groups = { "small", "back-end" })
  public void testClientRestRequest() throws URISyntaxException
  {
    URI uri = URI.create("http://test.qa.com:1234/foo");
    double weight = 3d;
    TestClient wrappedClient = new TestClient();
    Clock clock = new SettableClock();
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(2);
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(3d));
    TrackerClient client = new TrackerClient(uri, partitionDataMap, wrappedClient, clock, null);

    assertEquals(client.getUri(), uri);
    Double clientWeight = client.getPartitionWeight(DefaultPartitionAccessor.DEFAULT_PARTITION_ID);
    assertEquals(clientWeight, weight);
    assertEquals(client.getWrappedClient(), wrappedClient);

    RestRequest restRequest = new RestRequestBuilder(uri).build();
    Map<String, String> restWireAttrs = new HashMap<String, String>();
    TestTransportCallback<RestResponse> restCallback =
        new TestTransportCallback<RestResponse>();

    client.restRequest(restRequest, new RequestContext(), restWireAttrs, restCallback);

    assertFalse(restCallback.response.hasError());
    assertEquals(wrappedClient.restRequest, restRequest);
    assertEquals(wrappedClient.restWireAttrs, restWireAttrs);
  }

  @Test
  public void testCallTrackingRestRequest() throws Exception
  {
    URI uri = URI.create("http://test.qa.com:1234/foo");
    SettableClock clock = new SettableClock();
    AtomicInteger action = new AtomicInteger(0);
    TransportClient tc = new TransportClient() {
      @Override
      public void restRequest(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs, TransportCallback<RestResponse> callback) {
          clock.addDuration(5);
          switch (action.get())
          {
            // success
            case 0: callback.onResponse(TransportResponseImpl.success(RestResponse.NO_RESPONSE));
                    break;
            // fail with rest exception
            case 1: callback.onResponse(TransportResponseImpl.error(RestException.forError(500, "rest exception")));
                    break;
            // fail with timeout exception
            case 2: callback.onResponse(TransportResponseImpl.error(new RemoteInvocationException(new TimeoutException())));
                    break;
            // fail with other exception
            default: callback.onResponse(TransportResponseImpl.error(new RuntimeException()));
                    break;
          }
      }

      @Override
      public void shutdown(Callback<None> callback) {}
    };

    TrackerClient client = createTrackerClient(tc, clock, uri);
    CallTracker callTracker = client.getCallTracker();
    CallTracker.CallStats stats;
    DegraderControl degraderControl = client.getDegraderControl(DefaultPartitionAccessor.DEFAULT_PARTITION_ID);
    client.restRequest(new RestRequestBuilder(uri).build(), new RequestContext(), new HashMap<>(), new TestTransportCallback<>());
    clock.addDuration(5000);
    stats = callTracker.getCallStats();
    Assert.assertEquals(stats.getCallCount(), 1);
    Assert.assertEquals(stats.getErrorCount(), 0);
    Assert.assertEquals(stats.getCallCountTotal(), 1);
    Assert.assertEquals(stats.getErrorCountTotal(), 0);
    Assert.assertEquals(degraderControl.getCurrentComputedDropRate(), 0.0, 0.001);
    action.set(1);
    client.restRequest(new RestRequestBuilder(uri).build(), new RequestContext(), new HashMap<>(), new TestTransportCallback<>());
    clock.addDuration(5000);
    stats = callTracker.getCallStats();
    Assert.assertEquals(stats.getCallCount(), 1);
    Assert.assertEquals(stats.getErrorCount(), 1);
    Assert.assertEquals(stats.getCallCountTotal(), 2);
    Assert.assertEquals(stats.getErrorCountTotal(), 1);
    Assert.assertEquals(degraderControl.getCurrentComputedDropRate(), 0.2, 0.001);
    action.set(2);
    client.restRequest(new RestRequestBuilder(uri).build(), new RequestContext(), new HashMap<>(), new TestTransportCallback<>());
    clock.addDuration(5000);
    stats = callTracker.getCallStats();
    Assert.assertEquals(stats.getCallCount(), 1);
    Assert.assertEquals(stats.getErrorCount(), 1);
    Assert.assertEquals(stats.getCallCountTotal(), 3);
    Assert.assertEquals(stats.getErrorCountTotal(), 2);
    Assert.assertEquals(degraderControl.getCurrentComputedDropRate(), 0.4, 0.001);
    action.set(3);
    client.restRequest(new RestRequestBuilder(uri).build(), new RequestContext(), new HashMap<>(), new TestTransportCallback<>());
    clock.addDuration(5000);
    stats = callTracker.getCallStats();
    Assert.assertEquals(stats.getCallCount(), 1);
    Assert.assertEquals(stats.getErrorCount(), 1);
    Assert.assertEquals(stats.getCallCountTotal(), 4);
    Assert.assertEquals(stats.getErrorCountTotal(), 3);
    Assert.assertEquals(degraderControl.getCurrentComputedDropRate(), 0.2, 0.001);
  }

  @Test
  public void testCallTrackingStreamRequest() throws Exception
  {
    URI uri = URI.create("http://test.qa.com:1234/foo");
    SettableClock clock = new SettableClock();
    AtomicInteger action = new AtomicInteger(0);
    TransportClient tc = new TransportClient() {
      @Override
      public void restRequest(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs, TransportCallback<RestResponse> callback) {
      }

      @Override
      public void streamRequest(StreamRequest request,
                                RequestContext requestContext,
                                Map<String, String> wireAttrs,
                                TransportCallback<StreamResponse> callback) {
        clock.addDuration(5);
        switch (action.get())
        {
          // success
          case 0: callback.onResponse(TransportResponseImpl.success(new StreamResponseBuilder().build(EntityStreams.emptyStream())));
            break;
          // fail with stream exception
          case 1: callback.onResponse(TransportResponseImpl.error(
              new StreamException(new StreamResponseBuilder().setStatus(500).build(EntityStreams.emptyStream()))));
            break;
          // fail with timeout exception
          case 2: callback.onResponse(TransportResponseImpl.error(new RemoteInvocationException(new TimeoutException())));
            break;
          // fail with other exception
          default: callback.onResponse(TransportResponseImpl.error(new RuntimeException()));
            break;
        }
      }

      @Override
      public void shutdown(Callback<None> callback) {}
    };

    TrackerClient client = createTrackerClient(tc, clock, uri);
    CallTracker callTracker = client.getCallTracker();
    CallTracker.CallStats stats;
    DegraderControl degraderControl = client.getDegraderControl(DefaultPartitionAccessor.DEFAULT_PARTITION_ID);
    DelayConsumeCallback delayConsumeCallback = new DelayConsumeCallback();
    client.streamRequest(new StreamRequestBuilder(uri).build(EntityStreams.emptyStream()), new RequestContext(), new HashMap<>(), delayConsumeCallback);
    clock.addDuration(5);
    // we only recorded the time when stream response arrives, but callcompletion.endcall hasn't been called yet.
    Assert.assertEquals(callTracker.getCurrentCallCountTotal(), 0);
    Assert.assertEquals(callTracker.getCurrentErrorCountTotal(), 0);

    // delay
    clock.addDuration(100);
    delayConsumeCallback.consume();
    clock.addDuration(5000);
    // now that we consumed the entity stream, callcompletion.endcall has been called.
    stats = callTracker.getCallStats();
    Assert.assertEquals(stats.getCallCount(), 1);
    Assert.assertEquals(stats.getErrorCount(), 0);
    Assert.assertEquals(stats.getCallCountTotal(), 1);
    Assert.assertEquals(stats.getErrorCountTotal(), 0);
    Assert.assertEquals(degraderControl.getCurrentComputedDropRate(), 0.0, 0.001);

    action.set(1);
    client.streamRequest(new StreamRequestBuilder(uri).build(EntityStreams.emptyStream()), new RequestContext(), new HashMap<>(), delayConsumeCallback);
    clock.addDuration(5);
    // we endcall with error immediately for stream exception, even before the entity is consumed
    Assert.assertEquals(callTracker.getCurrentCallCountTotal(), 2);
    Assert.assertEquals(callTracker.getCurrentErrorCountTotal(), 1);
    delayConsumeCallback.consume();
    clock.addDuration(5000);
    // no change in tracking after entity is consumed
    stats = callTracker.getCallStats();
    Assert.assertEquals(stats.getCallCount(), 1);
    Assert.assertEquals(stats.getErrorCount(), 1);
    Assert.assertEquals(stats.getCallCountTotal(), 2);
    Assert.assertEquals(stats.getErrorCountTotal(), 1);
    Assert.assertEquals(degraderControl.getCurrentComputedDropRate(), 0.2, 0.001);

    action.set(2);
    client.streamRequest(new StreamRequestBuilder(uri).build(EntityStreams.emptyStream()), new RequestContext(), new HashMap<>(), new TestTransportCallback<>());
    clock.addDuration(5);
    Assert.assertEquals(callTracker.getCurrentCallCountTotal(), 3);
    Assert.assertEquals(callTracker.getCurrentErrorCountTotal(), 2);
    clock.addDuration(5000);
    stats = callTracker.getCallStats();
    Assert.assertEquals(stats.getCallCount(), 1);
    Assert.assertEquals(stats.getErrorCount(), 1);
    Assert.assertEquals(stats.getCallCountTotal(), 3);
    Assert.assertEquals(stats.getErrorCountTotal(), 2);
    Assert.assertEquals(degraderControl.getCurrentComputedDropRate(), 0.4, 0.001);

    action.set(3);
    client.streamRequest(new StreamRequestBuilder(uri).build(EntityStreams.emptyStream()), new RequestContext(), new HashMap<>(), new TestTransportCallback<>());
    clock.addDuration(5);
    Assert.assertEquals(callTracker.getCurrentCallCountTotal(), 4);
    Assert.assertEquals(callTracker.getCurrentErrorCountTotal(), 3);
    clock.addDuration(5000);
    stats = callTracker.getCallStats();
    Assert.assertEquals(stats.getCallCount(), 1);
    Assert.assertEquals(stats.getErrorCount(), 1);
    Assert.assertEquals(stats.getCallCountTotal(), 4);
    Assert.assertEquals(stats.getErrorCountTotal(), 3);
    Assert.assertEquals(degraderControl.getCurrentComputedDropRate(), 0.2, 0.001);
  }

  private TrackerClient createTrackerClient(TransportClient tc, Clock clock, URI uri)
  {
    double weight = 3d;
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(2);
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(3d));
    DegraderImpl.Config config = new DegraderImpl.Config();
    config.setHighErrorRate(0.1);
    config.setLowErrorRate(0.0);
    config.setMinCallCount(1);
    return new TrackerClient(uri, partitionDataMap, tc, clock, config);
  }

  public static class TestClient implements TransportClient
  {
    public static final int DEFAULT_REQUEST_TIMEOUT = 500;
    public StreamRequest                   streamRequest;
    public RestRequest                     restRequest;
    public RequestContext                  restRequestContext;
    public Map<String, String>             restWireAttrs;
    public TransportCallback<StreamResponse> streamCallback;
    public TransportCallback<RestResponse>   restCallback;
    public ScheduledExecutorService _scheduler;

    public boolean                         shutdownCalled;
    private final boolean _emptyResponse;
    private boolean _dontCallCallback;
    private int _minRequestTimeout;

    public TestClient() { this(true);}

    public TestClient(boolean emptyResponse)
    {
      this(emptyResponse, false, DEFAULT_REQUEST_TIMEOUT);
    }

    public TestClient(boolean emptyResponse, boolean dontCallCallback, int minRequestTimeout)
    {
      _emptyResponse = emptyResponse;
      _dontCallCallback = dontCallCallback;

      // this parameter is important to respect the contract between R2 and D2 to never have a connection shorter than
      // the request timeout to not affect the D2 loadbalancing/degrading
      _minRequestTimeout = minRequestTimeout;
      _scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void restRequest(RestRequest request,
                     RequestContext requestContext,
                     Map<String, String> wireAttrs,
                     TransportCallback<RestResponse> callback)
    {
      restRequest = request;
      restRequestContext = requestContext;
      restWireAttrs = wireAttrs;
      restCallback = callback;
      RestResponseBuilder builder = new RestResponseBuilder();
      RestResponse response = _emptyResponse ? builder.build() :
          builder.setEntity("This is not empty".getBytes()).build();
      if (_dontCallCallback)
      {
        scheduleTimeout(requestContext, callback);
        return;
      }
      callback.onResponse(TransportResponseImpl.success(response));
    }

    @Override
    public void streamRequest(StreamRequest request,
                            RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            TransportCallback<StreamResponse> callback)
    {
      streamRequest = request;
      restRequestContext = requestContext;
      restWireAttrs = wireAttrs;
      streamCallback = callback;

      StreamResponseBuilder builder = new StreamResponseBuilder();
      StreamResponse response = _emptyResponse ? builder.build(EntityStreams.emptyStream())
          : builder.build(EntityStreams.newEntityStream(new ByteStringWriter(ByteString.copy("This is not empty".getBytes()))));
      if (_dontCallCallback)
      {
        scheduleTimeout(requestContext, callback);
        return;
      }
      callback.onResponse(TransportResponseImpl.success(response, wireAttrs));
    }

    private <T> void scheduleTimeout(RequestContext requestContext, TransportCallback<T> callback)
    {
      Integer requestTimeout = (Integer) requestContext.getLocalAttr(R2Constants.REQUEST_TIMEOUT);
      if (requestTimeout == null)
      {
        return;
      }
      if (requestTimeout < _minRequestTimeout)
      {
        throw new RuntimeException(
            "The timeout is always supposed to be greater than the timeout defined by the service."
                + " This error is enforced in the tests");
      }
      _scheduler.schedule(() -> callback.onResponse(
          TransportResponseImpl.error(new TimeoutException("Timeout expired after " + requestTimeout + "ms"))),
          requestTimeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown(Callback<None> callback)
    {
      shutdownCalled = true;

      callback.onSuccess(None.none());
    }
  }

  public static class TestTransportCallback<T> implements TransportCallback<T>
  {
    public TransportResponse<T> response;

    @Override
    public void onResponse(TransportResponse<T> response)
    {
      this.response = response;
    }
  }

  public static class TestCallback<T> implements Callback<T>
  {
    public Throwable e;
    public T         t;

    @Override
    public void onError(Throwable e)
    {
      this.e = e;
    }

    @Override
    public void onSuccess(T t)
    {
      this.t = t;
    }
  }

  private static class DelayConsumeCallback implements TransportCallback<StreamResponse> {
    StreamResponse _response;
    @Override
    public void onResponse(TransportResponse<StreamResponse> response) {
      if (response.hasError() && response.getError() instanceof StreamException) {
        _response = ((StreamException) response.getError()).getResponse();
      } else {
        _response = response.getResponse();
      }
    }

    public void consume()
    {
      if (_response != null) {
        _response.getEntityStream().setReader(new DrainReader());
      }
    }
  };
}

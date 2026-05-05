package com.linkedin.d2.balancer.clients;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamRequestBuilder;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.Observer;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.r2.transport.common.bridge.common.TransportResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportResponseImpl;
import com.linkedin.util.clock.SettableClock;
import com.linkedin.util.clock.SystemClock;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests {@link TrackerClientImpl}.
 *
 * <p>In addition to coverage of the basic configuration accessors, this class verifies the
 * per-call duration instrumentation introduced for OTel:
 * <ul>
 *   <li>{@link TrackerClientImpl#setPerCallDurationListener(PerCallDurationListener)} stores
 *       the listener and tolerates {@code null}.</li>
 *   <li>The listener is invoked with the correct duration on every callback path:
 *       REST success, REST error, stream transport error, stream {@code onDone}, and stream
 *       {@code onError} during streaming.</li>
 *   <li>Optional {@link org.testng.annotations.DataProvider}-driven tests exercise the same
 *       scenarios as compact matrix rows (see {@code *_Parametrized} methods).</li>
 * </ul>
 */
public class TrackerClientImplTest
{
  private static final URI URI_FOO = URI.create("http://foo.example:1234/svc");
  private static final long DEFAULT_INTERVAL_MS = 1000L;

  private TrackerClientImpl _trackerClient;

  private static PerCallDurationListener durationSink(
      AtomicLong durationOut, AtomicReference<PerCallDurationSemantics> semanticsOut)
  {
    return (d, s) -> {
      durationOut.set(d);
      semanticsOut.set(s);
    };
  }

  @Test
  public void testDoNotLoadBalance()
  {
    boolean doNotLoadBalance = true;
    _trackerClient = new TrackerClientImpl(URI.create("uri"), new HashMap<>(), null, SystemClock.instance(), 1000, (test) -> false, false, false, doNotLoadBalance);

    Assert.assertEquals(_trackerClient.doNotLoadBalance(), doNotLoadBalance);

    doNotLoadBalance = false;
    _trackerClient = new TrackerClientImpl(URI.create("uri"), new HashMap<>(), null, SystemClock.instance(), 1000, (test) -> false, false, false, doNotLoadBalance);

    Assert.assertEquals(_trackerClient.doNotLoadBalance(), doNotLoadBalance);
  }

  // ---------------------------------------------------------------------------
  // setPerCallDurationListener
  // ---------------------------------------------------------------------------

  @Test
  public void testSetPerCallDurationListenerNullIsTolerated()
  {
    SettableClock clock = new SettableClock();
    RecordingTransportClient transport = new RecordingTransportClient();
    TrackerClientImpl client = newClient(clock, transport);

    // Listener defaults to a no-op; setting null should fall back to that no-op without throwing.
    client.setPerCallDurationListener(null);

    // Simulate a successful REST call to make sure no NPE is raised when the listener fires.
    client.restRequest(new RestRequestBuilder(URI_FOO).build(), new RequestContext(), new HashMap<>(),
        new CapturingTransportCallback<>());
    clock.addDuration(50);
    transport.restCallback.onResponse(TransportResponseImpl.success(new RestResponseBuilder().build()));
    // Reaching this line without an exception is the assertion.
  }

  // ---------------------------------------------------------------------------
  // REST callback paths (line 224 in TrackerClientImpl)
  // ---------------------------------------------------------------------------

  @Test
  public void testRestSuccessInvokesListenerWithMeasuredDuration()
  {
    SettableClock clock = new SettableClock();
    RecordingTransportClient transport = new RecordingTransportClient();
    TrackerClientImpl client = newClient(clock, transport);

    AtomicLong observedDuration = new AtomicLong(-1);
    AtomicReference<PerCallDurationSemantics> observedSemantics = new AtomicReference<>();
    client.setPerCallDurationListener(durationSink(observedDuration, observedSemantics));

    client.restRequest(new RestRequestBuilder(URI_FOO).build(), new RequestContext(), new HashMap<>(),
        new CapturingTransportCallback<>());

    long expectedDuration = 137L;
    clock.addDuration(expectedDuration);
    transport.restCallback.onResponse(TransportResponseImpl.success(new RestResponseBuilder().build()));

    Assert.assertEquals("REST success path should record the measured duration", expectedDuration,
        observedDuration.get());
    Assert.assertEquals(PerCallDurationSemantics.FULL_ROUND_TRIP, observedSemantics.get());
  }

  @Test
  public void testRestErrorInvokesListenerWithMeasuredDuration()
  {
    SettableClock clock = new SettableClock();
    RecordingTransportClient transport = new RecordingTransportClient();
    TrackerClientImpl client = newClient(clock, transport);

    AtomicLong observedDuration = new AtomicLong(-1);
    AtomicReference<PerCallDurationSemantics> observedSemantics = new AtomicReference<>();
    client.setPerCallDurationListener(durationSink(observedDuration, observedSemantics));

    client.restRequest(new RestRequestBuilder(URI_FOO).build(), new RequestContext(), new HashMap<>(),
        new CapturingTransportCallback<>());

    long expectedDuration = 250L;
    clock.addDuration(expectedDuration);
    transport.restCallback.onResponse(
        TransportResponseImpl.error(new RemoteInvocationException("simulated REST error")));

    Assert.assertEquals("REST error path should record the measured duration", expectedDuration,
        observedDuration.get());
    Assert.assertEquals(PerCallDurationSemantics.FULL_ROUND_TRIP, observedSemantics.get());
  }

  // ---------------------------------------------------------------------------
  // Stream callback paths
  // ---------------------------------------------------------------------------

  /**
   * Verifies the immediate stream-transport-error path (line 269 in TrackerClientImpl): when the
   * transport callback fires with an error, the listener is invoked using the current clock time
   * minus the start time.
   */
  @Test
  public void testStreamTransportErrorInvokesListenerWithMeasuredDuration()
  {
    SettableClock clock = new SettableClock();
    RecordingTransportClient transport = new RecordingTransportClient();
    TrackerClientImpl client = newClient(clock, transport);

    AtomicLong observedDuration = new AtomicLong(-1);
    AtomicReference<PerCallDurationSemantics> observedSemantics = new AtomicReference<>();
    client.setPerCallDurationListener(durationSink(observedDuration, observedSemantics));

    StreamRequest streamRequest = new StreamRequestBuilder(URI_FOO).build(EntityStreams.emptyStream());
    client.streamRequest(streamRequest, new RequestContext(), new HashMap<>(), new CapturingTransportCallback<>());

    long expectedDuration = 73L;
    clock.addDuration(expectedDuration);
    transport.streamCallback.onResponse(
        TransportResponseImpl.error(new RemoteInvocationException("simulated stream transport error")));

    Assert.assertEquals("Stream transport-error path should record the measured duration",
        expectedDuration, observedDuration.get());
    Assert.assertEquals(PerCallDurationSemantics.FULL_ROUND_TRIP, observedSemantics.get());
  }

  /**
   * Verifies the stream success path (line 303 in TrackerClientImpl): the listener should receive
   * {@code firstByteTime - startTime}, NOT the time at {@code onDone}. This is the documented
   * D2 behavior (avoid penalizing servers for client-side back-pressure during streaming).
   */
  @Test
  public void testStreamSuccessOnDoneInvokesListenerWithFirstByteDuration()
  {
    SettableClock clock = new SettableClock();
    RecordingTransportClient transport = new RecordingTransportClient();
    TrackerClientImpl client = newClient(clock, transport);

    AtomicLong observedDuration = new AtomicLong(-1);
    AtomicReference<PerCallDurationSemantics> observedSemantics = new AtomicReference<>();
    client.setPerCallDurationListener(durationSink(observedDuration, observedSemantics));

    StreamRequest streamRequest = new StreamRequestBuilder(URI_FOO).build(EntityStreams.emptyStream());
    client.streamRequest(streamRequest, new RequestContext(), new HashMap<>(), new CapturingTransportCallback<>());

    // Time until first byte arrives.
    long firstByteOffset = 90L;
    clock.addDuration(firstByteOffset);

    // Build a stream response backed by a stream that captures the observer the strategy adds.
    CapturingEntityStream entityStream = new CapturingEntityStream();
    StreamResponse response = new StreamResponseBuilder().build(entityStream);
    transport.streamCallback.onResponse(TransportResponseImpl.success(response));

    Assert.assertNotNull("Strategy must add an observer to the entity stream",
        entityStream.observer);

    // Simulate a slow client consuming the body (back-pressure). The listener must NOT include
    // this delay -- it should only reflect time-to-first-byte.
    clock.addDuration(500L);
    entityStream.observer.onDone();

    Assert.assertEquals("Stream success path should record firstByteTime - startTime, not the "
        + "onDone time", firstByteOffset, observedDuration.get());
    Assert.assertEquals(PerCallDurationSemantics.TIME_TO_FIRST_BYTE, observedSemantics.get());
  }

  /**
   * Verifies the stream error-during-streaming path: when the stream observer fires
   * {@code onError} after the first byte has arrived, the listener should receive
   * {@code firstByteTime - startTime} (TTFB), aligned with the success {@code onDone} path. This
   * avoids penalizing the server for time spent streaming the body — which can be dominated by
   * client-side back-pressure rather than server responsiveness.
   */
  @Test
  public void testStreamErrorAfterFirstByteRecordsTtfbNotFullDuration()
  {
    SettableClock clock = new SettableClock();
    RecordingTransportClient transport = new RecordingTransportClient();
    TrackerClientImpl client = newClient(clock, transport);

    AtomicLong observedDuration = new AtomicLong(-1);
    AtomicReference<PerCallDurationSemantics> observedSemantics = new AtomicReference<>();
    client.setPerCallDurationListener(durationSink(observedDuration, observedSemantics));

    StreamRequest streamRequest = new StreamRequestBuilder(URI_FOO).build(EntityStreams.emptyStream());
    client.streamRequest(streamRequest, new RequestContext(), new HashMap<>(), new CapturingTransportCallback<>());

    long firstByteOffset = 60L;
    clock.addDuration(firstByteOffset);

    CapturingEntityStream entityStream = new CapturingEntityStream();
    StreamResponse response = new StreamResponseBuilder().build(entityStream);
    transport.streamCallback.onResponse(TransportResponseImpl.success(response));

    // Stream errors mid-flight after additional time. This extra time should NOT be reflected in
    // the recorded duration -- the listener should only see firstByteTime - startTime, matching
    // the onDone path for cross-path histogram consistency.
    long extraStreamingTime = 200L;
    clock.addDuration(extraStreamingTime);
    entityStream.observer.onError(new RemoteInvocationException("simulated streaming error"));

    Assert.assertEquals("Stream onError path should record firstByteTime - startTime (TTFB), "
        + "ignoring streaming-time after first byte", firstByteOffset, observedDuration.get());
    Assert.assertEquals(PerCallDurationSemantics.TIME_TO_FIRST_BYTE, observedSemantics.get());
  }

  // ---------------------------------------------------------------------------
  // Parametrized variants (same scenarios as explicit tests above; matrix-style coverage)
  // ---------------------------------------------------------------------------

  @DataProvider(name = "doNotLoadBalanceFlags")
  public Object[][] doNotLoadBalanceFlags()
  {
    return new Object[][] {{true}, {false}};
  }

  @Test(dataProvider = "doNotLoadBalanceFlags")
  public void testDoNotLoadBalance_Parametrized(boolean doNotLoadBalance)
  {
    _trackerClient = new TrackerClientImpl(URI.create("uri"), new HashMap<>(), null, SystemClock.instance(), 1000,
        (test) -> false, false, false, doNotLoadBalance);

    Assert.assertEquals(_trackerClient.doNotLoadBalance(), doNotLoadBalance);
  }

  @DataProvider(name = "restDurationPaths")
  public Object[][] restDurationPaths()
  {
    return new Object[][] {
        {true, 137L, "REST success path should record the measured duration"},
        {false, 250L, "REST error path should record the measured duration"}
    };
  }

  @Test(dataProvider = "restDurationPaths")
  public void testRestInvokesListenerWithMeasuredDuration_Parametrized(boolean restSuccess, long expectedDuration,
      String assertionMessage)
  {
    SettableClock clock = new SettableClock();
    RecordingTransportClient transport = new RecordingTransportClient();
    TrackerClientImpl client = newClient(clock, transport);

    AtomicLong observedDuration = new AtomicLong(-1);
    AtomicReference<PerCallDurationSemantics> observedSemantics = new AtomicReference<>();
    client.setPerCallDurationListener(durationSink(observedDuration, observedSemantics));

    client.restRequest(new RestRequestBuilder(URI_FOO).build(), new RequestContext(), new HashMap<>(),
        new CapturingTransportCallback<>());

    clock.addDuration(expectedDuration);
    if (restSuccess)
    {
      transport.restCallback.onResponse(TransportResponseImpl.success(new RestResponseBuilder().build()));
    }
    else
    {
      transport.restCallback.onResponse(
          TransportResponseImpl.error(new RemoteInvocationException("simulated REST error")));
    }

    Assert.assertEquals(assertionMessage, expectedDuration, observedDuration.get());
    Assert.assertEquals(PerCallDurationSemantics.FULL_ROUND_TRIP, observedSemantics.get());
  }

  /**
   * Parametrized counterpart to {@link #testStreamSuccessOnDoneInvokesListenerWithFirstByteDuration()}
   * and {@link #testStreamErrorAfterFirstByteRecordsTtfbNotFullDuration()}.
   */
  @DataProvider(name = "streamTtfbAfterFirstBytePaths")
  public Object[][] streamTtfbAfterFirstBytePaths()
  {
    return new Object[][] {
        {
          true,
          90L,
          500L,
          "Stream success path should record firstByteTime - startTime, not the onDone time"
        },
        {
          false,
          60L,
          200L,
          "Stream onError path should record firstByteTime - startTime (TTFB), "
              + "ignoring streaming-time after first byte"
        }
    };
  }

  @Test(dataProvider = "streamTtfbAfterFirstBytePaths")
  public void testStreamRecordsTtfbAfterFirstByte_Parametrized(boolean completeWithOnDone, long firstByteOffset,
      long delayAfterFirstByte, String assertionMessage)
  {
    SettableClock clock = new SettableClock();
    RecordingTransportClient transport = new RecordingTransportClient();
    TrackerClientImpl client = newClient(clock, transport);

    AtomicLong observedDuration = new AtomicLong(-1);
    AtomicReference<PerCallDurationSemantics> observedSemantics = new AtomicReference<>();
    client.setPerCallDurationListener(durationSink(observedDuration, observedSemantics));

    StreamRequest streamRequest = new StreamRequestBuilder(URI_FOO).build(EntityStreams.emptyStream());
    client.streamRequest(streamRequest, new RequestContext(), new HashMap<>(), new CapturingTransportCallback<>());

    clock.addDuration(firstByteOffset);

    CapturingEntityStream entityStream = new CapturingEntityStream();
    StreamResponse response = new StreamResponseBuilder().build(entityStream);
    transport.streamCallback.onResponse(TransportResponseImpl.success(response));

    Assert.assertNotNull("Strategy must add an observer to the entity stream", entityStream.observer);

    clock.addDuration(delayAfterFirstByte);
    if (completeWithOnDone)
    {
      entityStream.observer.onDone();
    }
    else
    {
      entityStream.observer.onError(new RemoteInvocationException("simulated streaming error"));
    }

    Assert.assertEquals(assertionMessage, firstByteOffset, observedDuration.get());
    Assert.assertEquals(PerCallDurationSemantics.TIME_TO_FIRST_BYTE, observedSemantics.get());
  }

  /**
   * Verifies graceful degradation: if the per-call listener throws, the wrapped transport
   * callback must still be invoked so the application's request is not stuck. The exception must
   * not escape from {@link com.linkedin.r2.transport.common.bridge.common.TransportCallback#onResponse}.
   */
  @Test
  public void testListenerExceptionDoesNotBlockWrappedCallback()
  {
    SettableClock clock = new SettableClock();
    RecordingTransportClient transport = new RecordingTransportClient();
    TrackerClientImpl client = newClient(clock, transport);

    AtomicLong listenerInvocations = new AtomicLong();
    client.setPerCallDurationListener((duration, semantics) -> {
      listenerInvocations.incrementAndGet();
      throw new RuntimeException("simulated OTel SDK failure");
    });

    AtomicLong wrappedCallbackInvocations = new AtomicLong();
    TransportCallback<RestResponse> wrappedCallback = response -> wrappedCallbackInvocations.incrementAndGet();

    // The exception from the listener must NOT propagate out of onResponse; if it did, the test
    // would fail with an uncaught RuntimeException here.
    client.restRequest(new RestRequestBuilder(URI_FOO).build(), new RequestContext(), new HashMap<>(),
        wrappedCallback);
    clock.addDuration(50);
    transport.restCallback.onResponse(TransportResponseImpl.success(new RestResponseBuilder().build()));

    Assert.assertEquals("Listener should still have been invoked once", 1, listenerInvocations.get());
    Assert.assertEquals("Wrapped callback must fire even when the listener throws", 1,
        wrappedCallbackInvocations.get());
  }

  /**
   * Sanity check: replacing the listener with another non-null listener swaps the destination.
   */
  @Test
  public void testSetPerCallDurationListenerReplacesPreviousListener()
  {
    SettableClock clock = new SettableClock();
    RecordingTransportClient transport = new RecordingTransportClient();
    TrackerClientImpl client = newClient(clock, transport);

    AtomicReference<Long> firstSink = new AtomicReference<>();
    AtomicReference<Long> secondSink = new AtomicReference<>();
    client.setPerCallDurationListener((d, s) -> firstSink.set(d));
    client.setPerCallDurationListener((d, s) -> secondSink.set(d));

    client.restRequest(new RestRequestBuilder(URI_FOO).build(), new RequestContext(), new HashMap<>(),
        new CapturingTransportCallback<>());
    clock.addDuration(42);
    transport.restCallback.onResponse(TransportResponseImpl.success(new RestResponseBuilder().build()));

    Assert.assertNull("First listener must not receive any duration after being replaced",
        firstSink.get());
    Assert.assertNotNull("Second listener must receive the duration", secondSink.get());
    Assert.assertEquals(42L, secondSink.get().longValue());
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static TrackerClientImpl newClient(SettableClock clock, TransportClient transport)
  {
    Map<Integer, com.linkedin.d2.balancer.properties.PartitionData> partitionData = new HashMap<>();
    partitionData.put(0, new com.linkedin.d2.balancer.properties.PartitionData(1));
    return new TrackerClientImpl(URI_FOO, partitionData, transport, clock, DEFAULT_INTERVAL_MS,
        status -> false, false, false, false);
  }

  /**
   * Captures the most recently passed-in transport callbacks so the test can drive them.
   */
  private static final class RecordingTransportClient implements TransportClient
  {
    volatile TransportCallback<RestResponse> restCallback;
    volatile TransportCallback<StreamResponse> streamCallback;

    @Override
    public void restRequest(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
        TransportCallback<RestResponse> callback)
    {
      restCallback = callback;
    }

    @Override
    public void streamRequest(StreamRequest request, RequestContext requestContext, Map<String, String> wireAttrs,
        TransportCallback<StreamResponse> callback)
    {
      streamCallback = callback;
    }

    @Override
    public void shutdown(Callback<None> callback)
    {
      callback.onSuccess(None.none());
    }
  }

  /**
   * Captures the {@link Observer} the strategy adds to the entity stream so the test can drive
   * {@code onDone} / {@code onError} directly without relying on the real reader/writer flow.
   */
  private static final class CapturingEntityStream implements EntityStream
  {
    volatile Observer observer;

    @Override
    public void addObserver(Observer o)
    {
      observer = o;
    }

    @Override
    public void setReader(Reader r)
    {
      // Not used by these tests; production code only adds an observer.
    }
  }

  /**
   * No-op {@link TransportCallback} used to satisfy the wrappedCallback invariant. The tests
   * inspect side effects on the per-call duration listener, not on the wrapped callback.
   */
  private static final class CapturingTransportCallback<T> implements TransportCallback<T>
  {
    @Override
    public void onResponse(TransportResponse<T> response)
    {
    }
  }
}

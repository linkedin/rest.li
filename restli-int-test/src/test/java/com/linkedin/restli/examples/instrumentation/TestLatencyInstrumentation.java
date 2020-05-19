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

package com.linkedin.restli.examples.instrumentation;

import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.filter.message.stream.StreamFilter;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.timing.FrameworkTimingKeys;
import com.linkedin.r2.message.timing.TimingContextUtil;
import com.linkedin.r2.message.timing.TimingImportance;
import com.linkedin.r2.message.timing.TimingKey;
import com.linkedin.r2.util.RequestContextUtil;
import com.linkedin.r2.util.finalizer.RequestFinalizer;
import com.linkedin.r2.util.finalizer.RequestFinalizerManager;
import com.linkedin.restli.client.CreateIdEntityRequest;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.IdEntityResponse;
import com.linkedin.restli.examples.RestLiIntegrationTest;
import com.linkedin.restli.examples.instrumentation.api.InstrumentationControl;
import com.linkedin.restli.examples.instrumentation.client.LatencyInstrumentationBuilders;
import com.linkedin.restli.examples.instrumentation.server.LatencyInstrumentationResource;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.test.util.retry.SingleRetry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * Tests to ensure that framework latency information collected using {@link FrameworkTimingKeys} and
 * {@link TimingContextUtil} is consistent and as-expected.
 *
 * These integration tests send requests to {@link LatencyInstrumentationResource}.
 *
 * TODO: Once instrumentation is supported for scatter-gather, fix the test logic and the assertions here.
 * TODO: CLIENT_RESPONSE_RESTLI_ERROR_DESERIALIZATION isn't tested at all due to downstream batching, ideally fix this.
 * TODO: Don't re-init on every method invocation, this may require an overall optimization to how our int-tests work.
 *
 * @author Evan Williams
 */
public class TestLatencyInstrumentation extends RestLiIntegrationTest
{
  // These timing keys will not be present in the error code path
  private static final Set<TimingKey> TIMING_KEYS_MISSING_ON_ERROR =
      new HashSet<>(Arrays.asList(FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_PROJECTION_APPLY.key(),
          FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_SERIALIZATION.key(),
          // The downstream request is a batch request, so its errors are serialized normally
          FrameworkTimingKeys.CLIENT_RESPONSE_RESTLI_ERROR_DESERIALIZATION.key()));
  // These timing keys will not be present in the success code path
  private static final Set<TimingKey> TIMING_KEYS_MISSING_ON_SUCCESS =
      new HashSet<>(Arrays.asList(FrameworkTimingKeys.SERVER_RESPONSE_RESTLI_ERROR_SERIALIZATION.key(),
          FrameworkTimingKeys.CLIENT_RESPONSE_RESTLI_ERROR_DESERIALIZATION.key()));
  // These timing keys are always present because the timing importance threshold is loaded in the custom R2 filter
  private static final Set<TimingKey> TIMING_KEYS_ALWAYS_PRESENT =
      new HashSet<>(Arrays.asList(FrameworkTimingKeys.SERVER_REQUEST.key(),
          FrameworkTimingKeys.SERVER_REQUEST_R2.key(),
          FrameworkTimingKeys.SERVER_REQUEST_R2_FILTER_CHAIN.key()));
  // These timing keys will not be present when using protocol 2.0.0
  private static final Set<TimingKey> TIMING_KEYS_MISSING_ON_PROTOCOL_2_0_0 =
      Collections.singleton(FrameworkTimingKeys.SERVER_REQUEST_RESTLI_URI_PARSE_1.key());

  private static final double NANOS_TO_MILLIS = .000001;

  private Map<TimingKey, TimingContextUtil.TimingContext> _resultMap;
  private CountDownLatch _countDownLatch;
  private boolean _useStreaming;
  private TimingImportance _timingImportanceThreshold;

  @Override
  public boolean forceUseStreamServer()
  {
    return _useStreaming;
  }

  @BeforeMethod
  public void beforeMethod(Object[] args) throws Exception
  {
    _countDownLatch = new CountDownLatch(1);
    _useStreaming = (boolean) args[0];
    _timingImportanceThreshold = (TimingImportance) args[3];
    final InstrumentationTrackingFilter instrumentationTrackingFilter = new InstrumentationTrackingFilter();
    final FilterChain filterChain = FilterChains.empty()
        .addFirst(instrumentationTrackingFilter)
        .addFirstRest(instrumentationTrackingFilter);
    // System.setProperty("test.useStreamCodecServer", "true"); // Uncomment this to test with server streaming codec in IDEA
    super.init(Collections.emptyList(), filterChain, false);
  }

  @AfterMethod
  public void afterMethod() throws Exception
  {
    super.shutdown();
  }

  @DataProvider(name = "latencyInstrumentation")
  private Object[][] provideLatencyInstrumentationData()
  {
    List<Object[]> data = new ArrayList<>();
    boolean[] booleanOptions = new boolean[] { true, false };
    Collection<TimingImportance> timingImportanceOptions = new ArrayList<>(Arrays.asList(TimingImportance.values()));
    timingImportanceOptions.add(null);
    for (boolean useStreaming : booleanOptions)
    {
      for (boolean forceException : booleanOptions)
      {
        for (boolean useScatterGather : booleanOptions)
        {
          for (TimingImportance timingImportanceThreshold : timingImportanceOptions)
          {
            data.add(new Object[] { useStreaming, forceException, useScatterGather, timingImportanceThreshold });
          }
        }
      }
    }
    return data.toArray(new Object[data.size()][4]);
  }

  /**
   * Ensures that timing data is recorded for all the various timing markers in the framework, and compares them
   * to one another to ensure that the timing is logical (e.g. Rest.li filter chain should take less time than the
   * Rest.li layer as a whole). Checks this for combinations of rest vs. streaming, success vs. error, and various
   * {@link TimingImportance} thresholds.
   *
   * Note that the "useStreaming" parameter does NOT affect whether the integration test client (this) uses streaming or
   * whether the server uses the streaming codec; this is determined by whether the project properties
   * "test.useStreamCodecClient" and "test.useStreamCodecServer", respectively, are set to true or false (this is done
   * via Gradle, so it cannot be tested in IDEA without manually setting the properties). The integration test client
   * using streaming should be inconsequential for this test, but the server using the streaming codec will actually
   * affect the outcome.
   *
   * @param useStreaming whether the server should use an underlying streaming server ("restOverStream") and whether the
   *                     downstream request should use streaming (see the disclaimer above)
   * @param forceException whether the upstream and downstream resources should trigger the error response path
   * @param timingImportanceThreshold impacts which keys are included in the request context
   */
  @Test(dataProvider = "latencyInstrumentation", retryAnalyzer = SingleRetry.class)
  public void testLatencyInstrumentation(boolean useStreaming, boolean forceException, boolean useScatterGather,
      TimingImportance timingImportanceThreshold) throws RemoteInvocationException, InterruptedException
  {
    makeUpstreamRequest(useStreaming, forceException, useScatterGather);
    checkTimingKeyCompleteness(forceException, timingImportanceThreshold, useScatterGather);
    checkTimingKeyConsistency();
    checkTimingKeySubsetSums(timingImportanceThreshold, useScatterGather);
  }

  /**
   * Make the "upstream" request (as opposed to the "downstream" request made from the resource method) using a set of
   * test parameters. Waits for the timing keys to be recorded by the {@link InstrumentationTrackingFilter} before
   * returning.
   * @param useStreaming parameter from the test method
   * @param forceException parameter from the test method
   */
  private void makeUpstreamRequest(boolean useStreaming, boolean forceException, boolean useScatterGather)
      throws RemoteInvocationException, InterruptedException
  {
    InstrumentationControl instrumentationControl = new InstrumentationControl()
        .setServiceUriPrefix(FILTERS_URI_PREFIX)
        .setUseStreaming(useStreaming)
        .setForceException(forceException)
        .setUseScatterGather(useScatterGather);

    CreateIdEntityRequest<Long, InstrumentationControl> createRequest = new LatencyInstrumentationBuilders()
        .createAndGet()
        .input(instrumentationControl)
        .build();

    ResponseFuture<IdEntityResponse<Long, InstrumentationControl>> response = getClient().sendRequest(createRequest);

    try
    {
      response.getResponseEntity();
      if (forceException)
      {
        Assert.fail("Forcing exception, should've failed.");
      }
    }
    catch (RestLiResponseException e)
    {
      if (e.getStatus() != 400)
      {
        Assert.fail("Server responded with a non-400 error: " + e.getServiceErrorStackTrace());
      }
      if (!forceException)
      {
        Assert.fail("Not forcing exception, didn't expect failure.");
      }
    }

    // Wait for the server to send the response and save the timings
    final boolean success = _countDownLatch.await(10, TimeUnit.SECONDS);

    if (!success)
    {
      Assert.fail("Request timed out!");
    }
  }

  /**
   * Ensures that the recorded timing keys are "complete", meaning that all keys which are expected to be present
   * are present. Also ensures that no unexpected keys are present.
   * @param forceException parameter from the test method
   * @param timingImportanceThreshold parameter from the test method
   */
  private void checkTimingKeyCompleteness(boolean forceException, TimingImportance timingImportanceThreshold,
      boolean useScatterGather)
  {
    // Form the set of expected timing keys using the current test parameters
    Set<TimingKey> expectedKeys = Arrays.stream(FrameworkTimingKeys.values())
        .map(FrameworkTimingKeys::key)
        // For now, expect client keys to be missing if using scatter gather
        .filter(timingKey -> !(useScatterGather && timingKey.getName().startsWith(FrameworkTimingKeys.KEY_PREFIX + "client")))
        // Expect some keys to be missing depending on if an exception is being forced
        .filter(timingKey -> {
          if (forceException)
          {
            return !TIMING_KEYS_MISSING_ON_ERROR.contains(timingKey);
          }
          else
          {
            return !TIMING_KEYS_MISSING_ON_SUCCESS.contains(timingKey);
          }
        })
        // Expect some keys to be missing since using protocol 2.0.0
        .filter(timingKey -> !TIMING_KEYS_MISSING_ON_PROTOCOL_2_0_0.contains(timingKey))
        // Only expect keys that are included by the current timing importance threshold
        .filter(timingKey -> timingImportanceThreshold == null ||
            TIMING_KEYS_ALWAYS_PRESENT.contains(timingKey) ||
            timingKey.getTimingImportance().isAtLeast(timingImportanceThreshold))
        .collect(Collectors.toSet());

    // Check that all keys have complete timings (not -1) and that there are no unexpected keys
    for (TimingKey timingKey : _resultMap.keySet())
    {
      if (expectedKeys.contains(timingKey))
      {
        expectedKeys.remove(timingKey);
        Assert.assertNotEquals(_resultMap.get(timingKey).getDurationNano(), -1, timingKey.getName() + " is -1");
      }
      else if (timingKey.getName().contains(FrameworkTimingKeys.KEY_PREFIX))
      {
        Assert.fail("Encountered unexpected key: " + timingKey);
      }
    }

    // Check that the set of recorded timing keys is "complete"
    Assert.assertTrue(expectedKeys.isEmpty(), "Missing keys: " + expectedKeys.stream()
        .map(key -> String.format("\"%s\"", key.getName()))
        .collect(Collectors.joining(", ")));
  }

  /**
   * Ensures that the set of recorded timing keys is "consistent", meaning that for any code path (denoted by key "A")
   * which is a subset of another code path (denoted by key "B"), the duration of key A must be less than that of key B.
   */
  private void checkTimingKeyConsistency()
  {
    ArrayList<Map.Entry<TimingKey, TimingContextUtil.TimingContext>> entrySet = new ArrayList<>(_resultMap.entrySet());
    int size = entrySet.size();

    // Check that framework key subsets are consistent
    // (e.g. duration of "fwk/server/response" > duration of "fwk/server/response/restli")
    for (int i = 0; i < size; i++)
    {
      TimingKey keyA = entrySet.get(i).getKey();
      if (!keyA.getName().contains(FrameworkTimingKeys.KEY_PREFIX))
      {
        continue;
      }

      for (int j = 0; j < size; j++)
      {
        TimingKey keyB = entrySet.get(j).getKey();
        if (i == j || !keyB.getName().contains(FrameworkTimingKeys.KEY_PREFIX))
        {
          continue;
        }

        if (keyA.getName().contains(keyB.getName()))
        {
          TimingContextUtil.TimingContext contextA = entrySet.get(i).getValue();
          TimingContextUtil.TimingContext contextB = entrySet.get(j).getValue();

          String message = String.format("Expected %s (%.2fms) < %s (%.2fms)",
              keyA, contextA.getDurationNano() * NANOS_TO_MILLIS,
              keyB, contextB.getDurationNano() * NANOS_TO_MILLIS);

          Assert.assertTrue(contextA.getDurationNano() < contextB.getDurationNano(), message);
        }
      }
    }
  }

  /**
   * Ensures (in specific cases) the sum of two code paths should be less than or equal to a superset code path of both.
   * @param timingImportanceThreshold parameter from the test method
   */
  private void checkTimingKeySubsetSums(TimingImportance timingImportanceThreshold, boolean useScatterGather)
  {
    if (timingImportanceThreshold == null || TimingImportance.MEDIUM.isAtLeast(timingImportanceThreshold))
    {
      assertSubsetSum(FrameworkTimingKeys.SERVER_REQUEST_R2.key(),
          FrameworkTimingKeys.SERVER_REQUEST_RESTLI.key(),
          FrameworkTimingKeys.SERVER_REQUEST.key());
      assertSubsetSum(FrameworkTimingKeys.SERVER_RESPONSE_R2.key(),
          FrameworkTimingKeys.SERVER_RESPONSE_RESTLI.key(),
          FrameworkTimingKeys.SERVER_RESPONSE.key());
      // For now, client timings are disabled for scatter-gather requests
      if (!useScatterGather)
      {
        assertSubsetSum(FrameworkTimingKeys.CLIENT_REQUEST_R2.key(),
            FrameworkTimingKeys.CLIENT_REQUEST_RESTLI.key(),
            FrameworkTimingKeys.CLIENT_REQUEST.key());
        assertSubsetSum(FrameworkTimingKeys.CLIENT_RESPONSE_R2.key(),
            FrameworkTimingKeys.CLIENT_RESPONSE_RESTLI.key(),
            FrameworkTimingKeys.CLIENT_RESPONSE.key());
      }
    }
  }

  /**
   * Asserts that the sum of the durations of two timing keys is less than or equal to that of the third.
   * @param keyA the first summand key
   * @param keyB the second summand key
   * @param keyC the summation key
   */
  private void assertSubsetSum(TimingKey keyA, TimingKey keyB, TimingKey keyC)
  {
    long durationA = _resultMap.get(keyA).getDurationNano();
    long durationB = _resultMap.get(keyB).getDurationNano();
    long durationC = _resultMap.get(keyC).getDurationNano();

    String message = String.format("Expected %s (%.2fms) + %s (%.2fms) <= %s (%.2fms)",
        keyA, durationA * NANOS_TO_MILLIS,
        keyB, durationB * NANOS_TO_MILLIS,
        keyC, durationC * NANOS_TO_MILLIS);

    Assert.assertTrue(durationA + durationB <= durationC, message);
  }

  /**
   * R2 filter that adds the "timing importance" threshold to the request context, and also registers the request
   * finalizer that allows the integration test to gather timing data from the downstream service call.
   */
  private class InstrumentationTrackingFilter implements RestFilter, StreamFilter
  {
    @Override
    public void onRestRequest(RestRequest req,
        RequestContext requestContext,
        Map<String, String> wireAttrs,
        NextFilter<RestRequest, RestResponse> nextFilter)
    {
      requestContext.putLocalAttr(TimingContextUtil.TIMING_IMPORTANCE_THRESHOLD_KEY_NAME, _timingImportanceThreshold);
      nextFilter.onRequest(req, requestContext, wireAttrs);
    }

    @Override
    public void onStreamRequest(StreamRequest req,
        RequestContext requestContext,
        Map<String, String> wireAttrs,
        NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      requestContext.putLocalAttr(TimingContextUtil.TIMING_IMPORTANCE_THRESHOLD_KEY_NAME, _timingImportanceThreshold);
      nextFilter.onRequest(req, requestContext, wireAttrs);
    }

    @Override
    public void onRestResponse(RestResponse res,
        RequestContext requestContext,
        Map<String, String> wireAttrs,
        NextFilter<RestRequest, RestResponse> nextFilter)
    {
      registerRequestFinalizer(res, requestContext);
      nextFilter.onResponse(res, requestContext, wireAttrs);
    }

    @Override
    public void onStreamResponse(StreamResponse res,
        RequestContext requestContext,
        Map<String, String> wireAttrs,
        NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      registerRequestFinalizer(res, requestContext);
      nextFilter.onResponse(res, requestContext, wireAttrs);
    }

    @Override
    public void onRestError(Throwable ex,
        RequestContext requestContext,
        Map<String, String> wireAttrs,
        NextFilter<RestRequest, RestResponse> nextFilter)
    {
      registerRequestFinalizer(ex, requestContext);
      nextFilter.onError(ex, requestContext, wireAttrs);
    }

    @Override
    public void onStreamError(Throwable ex,
        RequestContext requestContext,
        Map<String, String> wireAttrs,
        NextFilter<StreamRequest, StreamResponse> nextFilter)
    {
      registerRequestFinalizer(ex, requestContext);
      nextFilter.onError(ex, requestContext, wireAttrs);
    }

    /**
     * Register the request finalizer, but only if it has the correct header.
     * This is to avoid tracking data for the protocol version fetch request.
     */
    private void registerRequestFinalizer(Response res, RequestContext requestContext)
    {
      if (Boolean.valueOf(res.getHeader(LatencyInstrumentationResource.HAS_CLIENT_TIMINGS_HEADER)))
      {
        registerRequestFinalizer(requestContext);
      }
    }

    /**
     * Register the request finalizer, but only if it has the correct service error code.
     * This is to avoid tracking data prematurely on the downstream call.
     */
    private void registerRequestFinalizer(Throwable ex, RequestContext requestContext)
    {
      final RestLiServiceException cause = (RestLiServiceException) ex.getCause();
      if (cause.hasCode() && cause.getCode().equals(LatencyInstrumentationResource.UPSTREAM_ERROR_CODE))
      {
        registerRequestFinalizer(requestContext);
      }
    }

    /**
     * Register the request finalizer which will collect the timing data from the request context.
     */
    private void registerRequestFinalizer(RequestContext requestContext)
    {
      RequestFinalizerManager manager = RequestContextUtil.getServerRequestFinalizerManager(requestContext);
      manager.registerRequestFinalizer(new InstrumentationTrackingRequestFinalizer());
    }
  }

  /**
   * Request finalizer that's performed at the very end of the "server response" code path, right before the response
   * is sent over the wire. This finalizer collects the response's timing data so that the unit test logic can perform
   * analysis on it. It also releases a latch so that the unit test analyzes the timing data only once the whole request
   * is complete.
   */
  private class InstrumentationTrackingRequestFinalizer implements RequestFinalizer
  {
    @Override
    public void finalizeRequest(Request request, Response response, RequestContext requestContext, Throwable error)
    {
      final Map<TimingKey, TimingContextUtil.TimingContext> timingsMap = TimingContextUtil.getTimingsMap(requestContext);
      _resultMap = new HashMap<>(timingsMap);
      _countDownLatch.countDown();
    }
  }
}

package com.linkedin.darkcluster;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.linkedin.darkcluster.api.DarkClusterConstants;
import com.linkedin.darkcluster.api.DarkClusterResponseValidationMetricsCollector;
import com.linkedin.darkcluster.api.ResponseValidationMetricsHeader;
import com.linkedin.darkcluster.filter.DarkClusterResponseValidationMetricsReaderFilter;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class TestDarkClusterResponseValidationMetricsReaderFilter {
  @Mock
  private DarkClusterResponseValidationMetricsCollector _metricsAggregator;
  private ExecutorService _executorService;
  private DarkClusterResponseValidationMetricsReaderFilter _filter;

  @BeforeMethod
  public void setup() {
    MockitoAnnotations.initMocks(this);
    _executorService = Executors.newSingleThreadExecutor();
    _filter = new DarkClusterResponseValidationMetricsReaderFilter(_metricsAggregator, _executorService);
  }

  @Test
  public void testOnRestRequestWithValidationMetricsHeader() throws JsonProcessingException, InterruptedException {
    RestRequest request = Mockito.mock(RestRequest.class);
    RequestContext context = Mockito.mock(RequestContext.class);
    NextFilter<RestRequest, RestResponse> nextFilter = new MockNextFilter();
    Map<String, Long> metrics = ImmutableMap.of("SUCCESS_COUNT", 10L, "FAILURE_COUNT", 1L);
    ResponseValidationMetricsHeader header = new ResponseValidationMetricsHeader("host",
        new ResponseValidationMetricsHeader.ResponseValidationMetrics(metrics, 1L));
    Mockito.when(request.getHeader(DarkClusterConstants.RESPONSE_VALIDATION_METRICS_HEADER_NAME))
        .thenReturn(header.serialize());
    _filter.onRestRequest(request, context, null, nextFilter);
    waitForLatch();
    Mockito.verify(_metricsAggregator).collect(header);
  }

  @Test
  public void testOnRestRequestWithNoValidationMetricsHeader() {
    RestRequest request = Mockito.mock(RestRequest.class);
    RequestContext context = Mockito.mock(RequestContext.class);
    NextFilter<RestRequest, RestResponse> nextFilter = new MockNextFilter();
    _filter.onRestRequest(request, context, null, nextFilter);
  }

  @Test
  public void testOnRestRequestWithInvalidMetricsInHeader() {
    RestRequest request = Mockito.mock(RestRequest.class);
    RequestContext context = Mockito.mock(RequestContext.class);
    NextFilter<RestRequest, RestResponse> nextFilter = new MockNextFilter();
    Mockito.when(request.getHeader(DarkClusterConstants.RESPONSE_VALIDATION_METRICS_HEADER_NAME))
        .thenReturn("metrics");
    _filter.onRestRequest(request, context, null, nextFilter);
    Mockito.verifyZeroInteractions(_metricsAggregator);
  }

  private void waitForLatch() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    Runnable runnable = latch::countDown;
    _executorService.submit(runnable);
    if (!latch.await(60, TimeUnit.SECONDS)) {
      Assert.fail("Unable to execute task");
    }
  }

  private static class MockNextFilter implements NextFilter<RestRequest, RestResponse> {

    @Override
    public void onRequest(RestRequest request, RequestContext requestContext, Map<String, String> wireAttrs) {
      // do nothing
    }

    @Override
    public void onResponse(RestResponse restResponse, RequestContext requestContext, Map<String, String> wireAttrs) {
      // do nothing
    }

    @Override
    public void onError(Throwable ex, RequestContext requestContext, Map<String, String> wireAttrs) {
      // do nothing
    }
  }
}

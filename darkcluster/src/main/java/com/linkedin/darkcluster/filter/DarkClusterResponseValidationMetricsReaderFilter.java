package com.linkedin.darkcluster.filter;

import com.linkedin.darkcluster.api.DarkClusterResponseValidationMetricsCollector;
import com.linkedin.darkcluster.api.ResponseValidationMetricsHeader;
import com.linkedin.darkcluster.api.DarkClusterConstants;
import com.linkedin.r2.filter.NextFilter;
import com.linkedin.r2.filter.message.rest.RestFilter;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A filter that is to be enabled only in a dark cluster that is meant to handle response validation metrics
 * sent from source
 */
public class DarkClusterResponseValidationMetricsReaderFilter implements RestFilter {
  private final DarkClusterResponseValidationMetricsCollector _metricsCollector;
  private final ExecutorService _executorService;

  private static final Logger LOG = LoggerFactory.getLogger(DarkClusterResponseValidationMetricsReaderFilter.class);

  public DarkClusterResponseValidationMetricsReaderFilter(
      @Nonnull DarkClusterResponseValidationMetricsCollector metricsCollector,
      @Nonnull ExecutorService executorService) {
    _metricsCollector = metricsCollector;
    _executorService = executorService;
  }

  /**
   * Does the following:
   * <li>Reads the response validation metrics header from the request</li>
   * <li>Deserializes the header value</li>
   * <li>In case of deserialization error, it logs the exception and moves on since we do not want to block subsequent processing</li>
   * <li>Calls the aggregator to aggregate metrics from the incoming source asynchronously</li>
   */
  @Override
  public void onRestRequest(RestRequest req, RequestContext requestContext, Map<String, String> wireAttrs,
      NextFilter<RestRequest, RestResponse> nextFilter) {
    String metricsAsJson = req.getHeader(DarkClusterConstants.RESPONSE_VALIDATION_METRICS_HEADER_NAME);
    if (metricsAsJson != null) {
      try {
        ResponseValidationMetricsHeader header = ResponseValidationMetricsHeader.deserialize(metricsAsJson);
        _executorService.execute(() -> _metricsCollector.collect(header));
      } catch (Exception e) {
        LOG.error("Error deserializing metrics from header. Header value: {}", metricsAsJson, e);
      }
    }
    nextFilter.onRequest(req, requestContext, wireAttrs);
  }
}


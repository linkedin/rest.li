package com.linkedin.darkcluster.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.linkedin.darkcluster.api.DarkRequestHeaderGenerator;
import com.linkedin.darkcluster.api.DispatcherResponseValidationMetricsHolder;
import com.linkedin.darkcluster.api.ResponseValidationMetricsHeader;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Impl of {@link DarkRequestHeaderGenerator} for generating headers for dark cluster response validation metrics.
 * The header value consists of the serialized form of the validation metrics which the dark cluster can consume.
 */
public class DarkResponseMetricsHeaderGenerator implements DarkRequestHeaderGenerator {
  private final DispatcherResponseValidationMetricsHolder _metricsHolder;
  private final Supplier<String> _sourceSupplier;

  private static final Logger LOG = LoggerFactory.getLogger(DarkRequestHeaderGenerator.class);

  /**
   * @param metricsHolder  impl of {@link DispatcherResponseValidationMetricsHolder} to return the metrics aggregated so far
   *                       in the dispatcher
   * @param sourceSupplier a supplier of source identifier that can uniquely identify a dispatcher instance sending out
   *                       validation metrics
   */
  public DarkResponseMetricsHeaderGenerator(DispatcherResponseValidationMetricsHolder metricsHolder,
      Supplier<String> sourceSupplier) {
    _metricsHolder = metricsHolder;
    _sourceSupplier = sourceSupplier;
  }

  /**
   * Retrieves the metrics from {@link DispatcherResponseValidationMetricsHolder} for the given dark cluster.
   * Serialize the metrics and return a name -> value pair
   * If there is no metrics for the corresponding dark cluster or there are any failures while serializing the header value,
   * return an empty key / value pair.
   */
  @Override
  public Optional<HeaderNameValuePair> get(String darkClusterName) {
    ResponseValidationMetricsHeader.ResponseValidationMetrics metrics = _metricsHolder.get(darkClusterName);
    if (metrics != null) {
      ResponseValidationMetricsHeader header = new ResponseValidationMetricsHeader(_sourceSupplier.get(), metrics);
      try {
        String headerValue = header.serialize();
        return Optional.of(new HeaderNameValuePair(DarkClusterConstants.RESPONSE_VALIDATION_METRICS_HEADER_NAME, headerValue));
      } catch (JsonProcessingException e) {
        LOG.error("Error serializing response validation metrics to header string for: {}", header, e);
      }
    }
    return Optional.empty();
  }
}

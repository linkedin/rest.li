package com.linkedin.darkcluster.api;

import java.util.Map;

/**
 * Collector of incoming response validation metrics from dispatchers.
 * This is meant to be called on the dark cluster hosts where the request headers containing response validation metrics are read.
 * A dark cluster host may receive metrics from multiple dispatchers and this interface defines method for collecting these
 * incoming metrics.
 */
public interface DarkClusterResponseValidationMetricsCollector {
  /**
   * Collects the incoming header metrics into the existing aggregated metrics
   */
  void collect(ResponseValidationMetricsHeader header);

  /**
   * Returns the metrics collected so far by aggregating all metrics that it has collected from all sources
   * @return a map of metric name -> value: each key/value pair representing the metrics that were defined by the user
   * while performing response validation in {@link com.linkedin.darkcluster.api.DarkClusterVerifier}
   * Eg: {RESPONSE_PREDICTION_SCORE_MATCH_COUNT -> 10, RESPONSE_PREDICTION_SCORE_MISMATCH_COUNT -> 1}
   */
  Map<String, Long> get();
}

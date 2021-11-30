package com.linkedin.darkcluster.api;

import java.util.Map;


/**
 * This is used on the dispatcher side to hold response validation metrics for all dark clusters.
 * It manages aggregating response validation metrics over time for any given dark cluster.
 * Clients who implement {@link com.linkedin.darkcluster.api.DarkClusterVerifier} can use this to update the metrics
 * computed as part of response validation.
 */
public interface DispatcherResponseValidationMetricsHolder {
  /**
   * To retrieve the response validation metrics for a given dark cluster
   */
  ResponseValidationMetricsHeader.ResponseValidationMetrics get(String darkClusterName);

  /**
   * Method to add metrics collected over time for a given dark cluster
   * Users can call this method in {@link com.linkedin.darkcluster.api.DarkClusterVerifier} after performing response
   * validation
   * @param darkClusterName name of the dark cluster against which the response validation was performed
   * @param metrics a key / value pair indicating metric name and the counter value
   *                eg: {RESPONSE_PREDICTION_SCORE_MATCH_COUNT -> 10, RESPONSE_PREDICTION_SCORE_MISMATCH_COUNT -> 1}
   *                these are metrics that would ultimately be sent out to dark clusters for consumption
   */
  void add(String darkClusterName, Map<String, Long> metrics);
}

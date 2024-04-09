package com.linkedin.d2.jmx;


/**
 * Interface for providing metrics for Indis Observer
 */
public interface IndisObserverMetricsProvider {
  /**
   * Get 50 Percentile of observer latency, which is from when the resource is updated on the observer to when the
   * client receives it.
   */
  long getLatency50Pct();

  /**
   * Get 90 Percentile of observer latency, which is from when the resource is updated on the observer to when the
   * client receives it.
   */
  long getLatency99Pct();

  /**
   * Get Avg of observer latency, which is from when the resource is updated on the observer to when the
   * client receives it.
   */
  double getLatencyAverage();

  /**
   * Track the latency of the observer
   * @param latency the latency to track
   */
  void trackLatency(long latency);
}

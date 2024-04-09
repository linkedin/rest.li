package com.linkedin.d2.jmx;


/**
 * Interface for providing metrics for Xds Server
 */
public interface XdsServerMetricsProvider {
  /**
   * Get 50 Percentile of Xds server latency, which is from when the resource is updated on the Xds server to when the
   * client receives it.
   */
  long getLatency50Pct();

  /**
   * Get 90 Percentile of Xds server latency, which is from when the resource is updated on the Xds server to when the
   * client receives it.
   */
  long getLatency99Pct();

  /**
   * Get Avg of Xds server latency, which is from when the resource is updated on the Xds server to when the
   * client receives it.
   */
  double getLatencyAverage();

  /**
   * Track the latency of the Xds server.
   * @param latency the latency to track
   */
  void trackLatency(long latency);
}

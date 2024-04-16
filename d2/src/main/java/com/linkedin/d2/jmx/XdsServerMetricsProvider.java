package com.linkedin.d2.jmx;


/**
 * Interface for providing metrics for Xds Server
 */
public interface XdsServerMetricsProvider {
  /**
   * Get minimum of Xds server latency, which is from when the resource is updated on the Xds server to when the
   * client receives it.
   */
  long getLatencyMin();

  /**
   * Get Avg of Xds server latency, which is from when the resource is updated on the Xds server to when the
   * client receives it.
   */
  double getLatencyAverage();

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
   * Get 99.9 Percentile of Xds server latency, which is from when the resource is updated on the Xds server to when the
   * client receives it.
   */
  long getLatency99_9Pct();

  /**
   * Get maximum of Xds server latency, which is from when the resource is updated on the Xds server to when the
   * client receives it.
   */
  long getLatencyMax();

  /**
   * Track the latency of the Xds server.
   * @param latency the latency to track
   */
  void trackLatency(long latency);
}

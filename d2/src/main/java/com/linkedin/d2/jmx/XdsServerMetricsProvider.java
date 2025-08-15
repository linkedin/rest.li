package com.linkedin.d2.jmx;


/**
 * Interface for providing metrics for Xds Server.
 * Xds server latency is calculated:
 * -- When IRV is not enabled, the client will receive all its interested resources every time it (re-)connects,
 * so the latency should be tracked based on the max of (resource modified time, subscribed time). Caveat in
 * this is that if some resource is modified and the update is not received for network issues, then the
 * client reconnects, the latency will be tracked based on the new subscribed time, and the real latency of
 * that update is lost.
 * -- When IRV is enabled, the caveat above will be fixed. Since the client will never receive resources that it already
 * received with IRV, except the first fetch, so after skipping the first fetch we can track latency always based
 * on the resource modified time.
 */
public interface XdsServerMetricsProvider {
  /**
   * Get minimum of Xds server latency.
   */
  long getLatencyMin();

  /**
   * Get Avg of Xds server latency.
   */
  double getLatencyAverage();

  /**
   * Get 50 Percentile of Xds server latency.
   */
  long getLatency50Pct();

  /**
   * Get 90 Percentile of Xds server latency.
   */
  long getLatency99Pct();

  /**
   * Get 99.9 Percentile of Xds server latency.
   */
  long getLatency99_9Pct();

  /**
   * Get maximum of Xds server latency.
   */
  long getLatencyMax();

  /**
   * Track the latency of the Xds server.
   * @param latency the latency to track
   */
  void trackLatency(long latency);
}

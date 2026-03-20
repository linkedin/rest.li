package com.linkedin.d2.jmx;

/**
 * Interface for OpenTelemetry metrics collection for the DegraderLoadBalancerStrategyV3.
 * This provider captures drop rates, latency distributions, and hash ring metrics for the degrader load balancer.
 *
 * All metrics are tagged with two dimensions: serviceName and scheme.
 */
public interface DegraderLoadBalancerStrategyV3OtelMetricsProvider {

  /**
   * Records a host's average latency as a raw value into the OpenTelemetry histogram.
   * Called once per host per update cycle; the histogram aggregates standard deviation,
   * percentiles, and max across the recorded observations.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param hostLatencyMs the average latency of the host in milliseconds
   */
  void recordHostLatency(String serviceName, String scheme, long hostLatencyMs);

  /**
   * Updates the current cluster-level override drop rate gauge.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param overrideClusterDropRate the current override drop rate in range [0.0, 1.0]
   */
  void updateOverrideClusterDropRate(String serviceName, String scheme, double overrideClusterDropRate);

  /**
   * Updates the total number of points across all hosts in the consistent hash ring.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param totalPointsInHashRing the total number of points in the hash ring
   */
  void updateTotalPointsInHashRing(String serviceName, String scheme, int totalPointsInHashRing);
}

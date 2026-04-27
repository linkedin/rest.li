package com.linkedin.d2.jmx;

/**
 * Common base contract for OpenTelemetry metrics providers used by D2 load balancer strategies.
 *
 * <p>This interface captures the metrics that are shared across strategy implementations (e.g.
 * {@link DegraderLoadBalancerStrategyV3OtelMetricsProvider} and
 * {@link RelativeLoadBalancerStrategyOtelMetricsProvider}) so they don't drift over time.
 * Strategy-specific metrics live on the per-strategy sub-interfaces.
 *
 * <p>All metrics defined here are tagged with two dimensions: {@code serviceName} and
 * {@code scheme}. {@code serviceName} identifies the D2 service; {@code scheme} identifies the
 * URI scheme (e.g. {@code "http"} or {@code "https"}) the strategy is associated with.
 */
public interface LoadBalancerStrategyOtelMetricsProvider {

  /**
   * Records a host's average latency as a raw value into the OpenTelemetry histogram.
   * Called once per completed call; the histogram aggregates standard deviation, percentiles,
   * and max across the recorded observations.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param hostLatencyMs the duration the server contributed to the call in milliseconds
   */
  void recordHostLatency(String serviceName, String scheme, long hostLatencyMs);

  /**
   * Updates the total number of points across all hosts in the consistent hash ring.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param totalPointsInHashRing the total number of points in the hash ring
   */
  void updateTotalPointsInHashRing(String serviceName, String scheme, int totalPointsInHashRing);
}

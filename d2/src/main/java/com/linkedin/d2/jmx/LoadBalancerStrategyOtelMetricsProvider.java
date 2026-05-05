package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.clients.PerCallDurationSemantics;

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
   * Records a per-call host latency sample.
   *
   * <p>Implementations MUST emit a single histogram and attach {@code semantics} as an attribute,
   * so {@link PerCallDurationSemantics#FULL_ROUND_TRIP} and
   * {@link PerCallDurationSemantics#TIME_TO_FIRST_BYTE} samples remain queryable independently.
   *
   * @param serviceName   D2 service name
   * @param scheme        URI scheme (e.g. {@code "http"}, {@code "https"})
   * @param hostLatencyMs duration in milliseconds
   * @param semantics     what {@code hostLatencyMs} measures; emit as a histogram attribute
   */
  void recordHostLatency(String serviceName, String scheme, long hostLatencyMs,
      PerCallDurationSemantics semantics);

  /**
   * Updates the total number of points across all hosts in the consistent hash ring.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param totalPointsInHashRing the total number of points in the hash ring
   */
  void updateTotalPointsInHashRing(String serviceName, String scheme, int totalPointsInHashRing);
}

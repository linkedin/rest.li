package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.clients.PerCallDurationSemantics;

/**
 * No-Op implementation of {@link DegraderLoadBalancerStrategyV3OtelMetricsProvider}.
 * Used when OpenTelemetry metrics are disabled.
 */
public class NoOpDegraderLoadBalancerStrategyV3OtelMetricsProvider implements DegraderLoadBalancerStrategyV3OtelMetricsProvider {

  /**
   * {@inheritDoc}
   */
  @Override
  public void recordHostLatency(String serviceName, String scheme, long hostLatencyMs,
      PerCallDurationSemantics semantics) {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateOverrideClusterDropRate(String serviceName, String scheme, double overrideClusterDropRate) {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateTotalPointsInHashRing(String serviceName, String scheme, int totalPointsInHashRing) {
    // No-op
  }
}

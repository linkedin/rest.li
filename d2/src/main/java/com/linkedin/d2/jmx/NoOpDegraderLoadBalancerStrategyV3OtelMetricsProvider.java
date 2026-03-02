package com.linkedin.d2.jmx;

/**
 * No-Op implementation of {@link DegraderLoadBalancerStrategyV3OtelMetricsProvider}.
 * Used when OpenTelemetry metrics are disabled.
 */
public class NoOpDegraderLoadBalancerStrategyV3OtelMetricsProvider implements DegraderLoadBalancerStrategyV3OtelMetricsProvider {

  /**
   * {@inheritDoc}
   */
  @Override
  public void recordHostLatency(String serviceName, String scheme, long hostLatencyMs) {
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

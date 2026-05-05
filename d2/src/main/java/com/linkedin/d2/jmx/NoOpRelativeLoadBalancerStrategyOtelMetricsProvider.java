package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.clients.PerCallDurationSemantics;

/**
 * No-Op implementation of {@link RelativeLoadBalancerStrategyOtelMetricsProvider}.
 * Used when OpenTelemetry metrics are disabled.
 */
public class NoOpRelativeLoadBalancerStrategyOtelMetricsProvider implements RelativeLoadBalancerStrategyOtelMetricsProvider {

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
  public void updateTotalHostsInAllPartitionsCount(String serviceName, String scheme, int totalHostsInAllPartitionsCount) {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateUnhealthyHostsCount(String serviceName, String scheme, int unhealthyHostsCount) {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateQuarantineHostsCount(String serviceName, String scheme, int quarantineHostsCount) {
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

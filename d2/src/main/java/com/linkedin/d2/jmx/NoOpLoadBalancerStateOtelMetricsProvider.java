package com.linkedin.d2.jmx;

/**
 * No-Op implementation of {@link LoadBalancerStateOtelMetricsProvider}.
 * Used when OpenTelemetry metrics are disabled.
 */
public class NoOpLoadBalancerStateOtelMetricsProvider implements LoadBalancerStateOtelMetricsProvider {

  @Override
  public void recordClusterCount(String clientName, long regularClusterCount, long symlinkClusterCount) {
    // no-op
  }

  @Override
  public void recordServiceCount(String clientName, long serviceCount) {
    // no-op
  }
}

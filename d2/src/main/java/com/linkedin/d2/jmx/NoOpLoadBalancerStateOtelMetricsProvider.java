package com.linkedin.d2.jmx;

/**
 * No-Op implementation of {@link LoadBalancerStateOtelMetricsProvider}.
 * Used when OpenTelemetry metrics are disabled.
 */
public class NoOpLoadBalancerStateOtelMetricsProvider implements LoadBalancerStateOtelMetricsProvider {

  @Override
  public void recordClusterCount(String clientName, int regularClusterCount, long symlinkClusterCount) {
    // no-op
  }

  @Override
  public void recordServiceCount(String clientName, int serviceCount) {
    // no-op
  }
}

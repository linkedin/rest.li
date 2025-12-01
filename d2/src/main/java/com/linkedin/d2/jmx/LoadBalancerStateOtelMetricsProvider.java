package com.linkedin.d2.jmx;

/**
 * Interface for OpenTelemetry metrics collection for LoadBalancerState sensor.
 */
public interface LoadBalancerStateOtelMetricsProvider {

  /**
   * Records both regular and symlink cluster counts for a client.
   *
   * @param clientName the client name
   * @param regularClusterCount regular cluster count
   * @param symlinkClusterCount symlink cluster count
   */
  void recordClusterCount(String clientName, int regularClusterCount, long symlinkClusterCount);

  /**
   * Records service count for a client.
   *
   * @param clientName the client name
   * @param serviceCount service count
   */
  void recordServiceCount(String clientName, int serviceCount);
}

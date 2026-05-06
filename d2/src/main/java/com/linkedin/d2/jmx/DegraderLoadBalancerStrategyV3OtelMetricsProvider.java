package com.linkedin.d2.jmx;

/**
 * Interface for OpenTelemetry metrics collection for the DegraderLoadBalancerStrategyV3.
 * This provider captures drop rates, latency distributions, and hash ring metrics for the degrader
 * load balancer.
 *
 * <p>Methods inherited from {@link LoadBalancerStrategyOtelMetricsProvider} cover metrics shared
 * with other LB strategy providers (host latency, hash ring point count). The methods declared
 * directly on this interface are degrader-specific.
 *
 * <p>All metrics are tagged with two dimensions: {@code serviceName} and {@code scheme}.
 */
public interface DegraderLoadBalancerStrategyV3OtelMetricsProvider extends LoadBalancerStrategyOtelMetricsProvider {

  /**
   * Updates the current cluster-level override drop rate gauge.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param overrideClusterDropRate the current override drop rate in range [0.0, 1.0]
   */
  void updateOverrideClusterDropRate(String serviceName, String scheme, double overrideClusterDropRate);
}

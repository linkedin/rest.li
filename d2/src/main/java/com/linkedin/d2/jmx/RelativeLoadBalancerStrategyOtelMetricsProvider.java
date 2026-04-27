package com.linkedin.d2.jmx;

/**
 * Interface for OpenTelemetry metrics collection for the RelativeLoadBalancerStrategy.
 * This provider captures latency distributions, host counts, and other load balancer metrics.
 *
 * <p>Methods inherited from {@link LoadBalancerStrategyOtelMetricsProvider} cover metrics shared
 * with other LB strategy providers (host latency, hash ring point count). The methods declared
 * directly on this interface are specific to the relative LB strategy.
 *
 * <p>All metrics are tagged with two dimensions: {@code serviceName} and {@code scheme}.
 */
public interface RelativeLoadBalancerStrategyOtelMetricsProvider extends LoadBalancerStrategyOtelMetricsProvider {

  /**
   * Updates the number of total hosts in all partitions regardless of their status.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param totalHostsInAllPartitionsCount the count of total hosts in all partitions
   */
  void updateTotalHostsInAllPartitionsCount(String serviceName, String scheme, int totalHostsInAllPartitionsCount);

  /**
   * Updates the number of unhealthy hosts.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param unhealthyHostsCount the count of unhealthy hosts
   */
  void updateUnhealthyHostsCount(String serviceName, String scheme, int unhealthyHostsCount);

  /**
   * Updates the number of hosts in quarantine.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param quarantineHostsCount the count of quarantine hosts
   */
  void updateQuarantineHostsCount(String serviceName, String scheme, int quarantineHostsCount);
}

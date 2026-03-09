package com.linkedin.d2.jmx;

/**
 * Interface for OpenTelemetry metrics collection for the RelativeLoadBalancerStrategy.
 * This provider captures latency distributions, host counts, and other load balancer metrics.
 *
 * All metrics are tagged with two dimensions: serviceName and scheme.</p>
 */
public interface RelativeLoadBalancerStrategyOtelMetricsProvider {

  /**
   * Records a host's average latency in the OpenTelemetry histogram.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param hostLatencyMs the average latency of the host in milliseconds
   */
  void recordHostLatency(String serviceName, String scheme, long hostLatencyMs);

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

  /**
   * Updates the total number of points in hash ring.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param totalPointsInHashRing the total points in the hash ring
   */
  void updateTotalPointsInHashRing(String serviceName, String scheme, int totalPointsInHashRing);
}

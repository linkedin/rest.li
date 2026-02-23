/*
   Copyright (c) 2024 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.d2.jmx;

/**
 * Interface for OpenTelemetry metrics collection for the RelativeLoadBalancerStrategy.
 * This provider captures latency distributions, host counts, and other load balancer metrics.
 *
 * <p>Latency metrics are recorded as raw values in a histogram, allowing OTEL to automatically
 * compute statistical aggregations like percentiles (p50, p90, p99), averages, min, max, standard
 * deviation, and other derived metrics.</p>
 *
 * <p>All metrics are tagged with two dimensions: serviceName and scheme.</p>
 */
public interface RelativeLoadBalancerStrategyOtelMetricsProvider {

  /**
   * Records a host's average latency in the OpenTelemetry histogram.
   * OTEL will automatically compute percentiles (p50, p90, p99), averages, min, max,
   * and other derived statistics from the recorded values.
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

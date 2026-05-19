/*
   Copyright (c) 2026 LinkedIn Corp.

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

/** OTel metrics for {@code RelativeLoadBalancerStrategy}. */
public interface RelativeLoadBalancerStrategyOtelMetricsProvider extends LoadBalancerStrategyOtelMetricsProvider
{
  /**
   * Updates the number of total hosts in all partitions regardless of their status.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param totalHostsInAllPartitionsCount the count of total hosts in all partitions
   */
  void updateTotalHostsInAllPartitionsCount(String serviceName, String scheme, int totalHostsInAllPartitionsCount);

  /**
   * Updates the number of hosts currently in a degraded state, broken down by status.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param status the degraded-host bucket this count belongs to
   * @param count the number of hosts currently in {@code status}
   */
  void updateDegradedHostsCount(String serviceName, String scheme, HostStatus status, int count);
}

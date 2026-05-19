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

import com.linkedin.d2.balancer.clients.PerCallDurationSemantics;

/** Shared OTel metrics for D2 load balancer strategies. */
public interface LoadBalancerStrategyOtelMetricsProvider
{
  /**
   * Records a per-call host latency sample.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param hostLatencyMs the duration in milliseconds
   * @param semantics the semantics of the latency
   */
  void recordHostLatency(String serviceName, String scheme, long hostLatencyMs,
      PerCallDurationSemantics semantics);

  /**
   * Updates the total number of points across all hosts in the consistent hash ring.
   *
   * @param serviceName the name of the service
   * @param scheme the load balancer scheme (e.g., "http", "https")
   * @param totalPointsInHashRing the total number of points in the hash ring
   */
  void updateTotalPointsInHashRing(String serviceName, String scheme, int totalPointsInHashRing);
}

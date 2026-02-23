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
 * No-Op implementation of {@link RelativeLoadBalancerStrategyOtelMetricsProvider}.
 * Used when OpenTelemetry metrics are disabled.
 */
public class NoOpRelativeLoadBalancerStrategyOtelMetricsProvider implements RelativeLoadBalancerStrategyOtelMetricsProvider {

  /**
   * {@inheritDoc}
   */
  @Override
  public void recordHostLatency(String serviceName, String scheme, long hostLatencyMs) {
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

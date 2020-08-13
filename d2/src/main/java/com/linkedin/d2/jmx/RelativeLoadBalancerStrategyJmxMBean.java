/*
   Copyright (c) 2020 LinkedIn Corp.

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

public interface RelativeLoadBalancerStrategyJmxMBean
{

  /**
   *
   * @return the standard deviation of cluster latencies
   */
  double getLatencyStandardDeviation();

  /**
   *
   * @return the mean absolute deviation of cluster latencies
   */
  double getLatencyMeanAbsoluteDeviation();

  /**
   *
   * @return the standard deviation of cluster latencies that are above average
   */
  double getAboveAverageLatencyStandardDeviation();

  /**
   *
   * @return the relative ratio between max latency and average cluster latency
   */
  double getMaxLatencyRelativeFactor();

  /**
   *
   * @return the relative ratio between nth percentile latency and average cluster latency
   */
  double getNthPercentileLatencyRelativeFactor(double pct);

  /**
   *
   * @return the number of unhealthy hosts
   */
  int getUnhealthyHostsCount();

  /**
   *
   * @return the number of hosts in quarantine
   */
  int getQuarantineHostsCount();

  /**
   *
   * @return number of total points in hash ring
   */
  int getTotalPointsInHashRing();
}

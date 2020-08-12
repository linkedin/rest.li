package com.linkedin.d2.jmx;

public interface RelativeLoadBalancerStrategyJmxMBean
{

  /**
   *
   * @return the standard deviation of cluster latency
   */
  double getLatencyStandardDeviation();

  /**
   *
   * @return the average absolute deviation around average of cluster latency
   */
  double getLatencyAverageAbsoluteDeviation();

  /**
   *
   * @return the median absolute deviation around median of cluster latency
   */
  double getLatencyMedianAbsoluteDeviation();

  /**
   *
   * @return the maximum absolute deviation around average of cluster latency
   */
  double getLatencyMaxAbsoluteDeviation();

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

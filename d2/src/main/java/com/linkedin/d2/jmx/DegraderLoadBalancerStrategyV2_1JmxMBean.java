package com.linkedin.d2.jmx;

public interface DegraderLoadBalancerStrategyV2_1JmxMBean
{

  /**
   *
   * @return the current overrideClusterDropRate
   */
  double getOverrideClusterDropRate();

  /**
   *
   * @return String representation of this strategy
   */
  String toString();

  /**
   *
   * @return number of total points in hash ring
   */
  int getTotalPointsInHashRing();

  /**
   *
   * @return the hash ring points mapping between URI --> #points
   */
  String getPointsMap();

  /**
   * This method assumes unhealthy clients are clients whose hash ring points are below
   * the default value for healthy client. (This value is points_per_weight * weight of the client)
   * We assume that the weight is defaulted to 1.
   *
   * @return String representation of pair of unhealthy client's URI : # of points / # points for perfect health
   */
  String getUnhealthyClientsPoints();
}

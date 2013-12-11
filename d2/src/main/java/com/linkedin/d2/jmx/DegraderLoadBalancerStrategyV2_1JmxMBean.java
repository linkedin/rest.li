package com.linkedin.d2.jmx;

public interface DegraderLoadBalancerStrategyV2_1JmxMBean
{
  double getOverrideClusterDropRate();

  String toString();

  int getTotalPointsInHashRing();
}

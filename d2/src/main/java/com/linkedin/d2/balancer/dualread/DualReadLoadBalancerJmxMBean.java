package com.linkedin.d2.balancer.dualread;

public interface DualReadLoadBalancerJmxMBean
{
  int getServicePropertiesErrorCount();

  int getClusterPropertiesErrorCount();

  int getUriPropertiesErrorCount();

  int getServicePropertiesEvictCount();

  int getClusterPropertiesEvictCount();

  int getUriPropertiesEvictCount();
}

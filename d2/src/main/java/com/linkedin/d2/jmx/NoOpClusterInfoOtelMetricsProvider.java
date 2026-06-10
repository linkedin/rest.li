package com.linkedin.d2.jmx;

public class NoOpClusterInfoOtelMetricsProvider implements ClusterInfoOtelMetricsProvider
{
  @Override
  public void recordCanaryDistributionPolicy(String clusterName, int policy)
  {
    // No-op
  }
}

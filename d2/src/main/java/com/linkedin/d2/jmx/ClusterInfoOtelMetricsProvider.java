package com.linkedin.d2.jmx;

/**
 * Interface for OpenTelemetry metrics collection for ClusterInfo.
 */
public interface ClusterInfoOtelMetricsProvider {

    /**
   * Records the current canary distribution policy for the cluster 
   * (0=STABLE, 1=CANARY, -1=UNSPECIFIED)
   * 
   * @param clusterName the name of the cluster
   * @param policy the canary distribution policy
   */
    void recordCanaryDistributionPolicy(String clusterName, int policy);
    
}

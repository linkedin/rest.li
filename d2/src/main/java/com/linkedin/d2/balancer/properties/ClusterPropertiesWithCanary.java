package com.linkedin.d2.balancer.properties;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cluster properties with canary configs and a distribution strategy, serialized and stored on service registry.
 */
public class ClusterPropertiesWithCanary extends ClusterProperties
{
  protected final ClusterProperties _canaryConfigs;
  protected final CanaryDistributionStrategy _canaryDistributionStrategy;

  public ClusterPropertiesWithCanary(String clusterName,
                                     List<String> prioritizedSchemes,
                                     Map<String, String> properties,
                                     Set<URI> bannedUris,
                                     PartitionProperties partitionProperties,
                                     List<String> sslSessionValidationStrings,
                                     Map<String, Object> darkClusters,
                                     boolean delegated,
                                     CanaryDistributionStrategy distributionStrategy,
                                     ClusterProperties canaryConfigs)
  {
    super(clusterName, prioritizedSchemes, properties, bannedUris, partitionProperties, sslSessionValidationStrings, darkClusters, delegated);
    _canaryConfigs = canaryConfigs;
    _canaryDistributionStrategy = distributionStrategy;
  }

  public ClusterProperties getCanaryConfigs()
  {
    return _canaryConfigs;
  }

  public CanaryDistributionStrategy getCanaryDistributionStrategy()
  {
    return _canaryDistributionStrategy;
  }

  @Override
  public String toString()
  {
    return "ClusterPropertiesWithCanary [_stableClusterProperties=" + super.toString() + ", _canaryConfigs=" + _canaryConfigs.toString()
      + ", _canaryDistributionStrategy=" + _canaryDistributionStrategy.toString();
  }

  @Override
  public int hashCode()
  {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + _canaryDistributionStrategy.hashCode();
    result = prime * result + _canaryConfigs.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    boolean superEquals = super.equals(obj); // compare overall class and stable configs
    if (!superEquals)
    {
      return false;
    }

    ClusterPropertiesWithCanary other = (ClusterPropertiesWithCanary) obj;
    return _canaryDistributionStrategy.equals(other.getCanaryDistributionStrategy()) && _canaryConfigs.equals(other.getCanaryConfigs());
  }
}

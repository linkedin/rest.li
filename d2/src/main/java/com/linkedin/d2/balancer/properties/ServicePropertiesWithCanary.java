package com.linkedin.d2.balancer.properties;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service properties with canary configs and a distribution strategy, serialized and stored on service registry.
 */
public class ServicePropertiesWithCanary extends ServiceProperties
{
  protected final ServiceProperties _canaryConfigs;
  protected final CanaryDistributionStrategy _canaryDistributionStrategy;

  public ServicePropertiesWithCanary(String serviceName, String clusterName, String path, List<String> prioritizedStrategyList,
                                     CanaryDistributionStrategy distributionStrategy, ServiceProperties canaryConfigs)
  {
    super(serviceName, clusterName, path, prioritizedStrategyList, Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(),
          Collections.<String, String>emptyMap(), Collections.<String>emptyList(), Collections.<URI>emptySet());
    _canaryConfigs = canaryConfigs;
    _canaryDistributionStrategy = distributionStrategy;
  }

  public ServicePropertiesWithCanary(String serviceName, String clusterName, String path, List<String> prioritizedStrategyList,
                                     Map<String, Object> loadBalancerStrategyProperties, Map<String, Object> transportClientProperties,
                                     Map<String, String> degraderProperties, List<String> prioritizedSchemes, Set<URI> banned,
                                     Map<String, Object> serviceMetadataProperties, List<Map<String, Object>> backupRequests,
                                     Map<String, Object> relativeStrategyProperties, boolean enableClusterSubsetting, int minClusterSubsetSize,
                                     CanaryDistributionStrategy distributionStrategy, ServiceProperties canaryConfigs)
  {
    super(serviceName, clusterName, path, prioritizedStrategyList, loadBalancerStrategyProperties, transportClientProperties, degraderProperties,
          prioritizedSchemes, banned, serviceMetadataProperties, backupRequests, relativeStrategyProperties, enableClusterSubsetting,
          minClusterSubsetSize);
    _canaryDistributionStrategy = distributionStrategy;
    _canaryConfigs = canaryConfigs;
  }

  public ServiceProperties getCanaryConfigs()
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
    return "ServicePropertiesWithCanary [_stableServiceProperties=" + super.toString() + ", _canaryConfigs=" + _canaryConfigs.toString()
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

    ServicePropertiesWithCanary other = (ServicePropertiesWithCanary) obj;
    return _canaryDistributionStrategy.equals(other.getCanaryDistributionStrategy()) && _canaryConfigs.equals(other.getCanaryConfigs());
  }
}

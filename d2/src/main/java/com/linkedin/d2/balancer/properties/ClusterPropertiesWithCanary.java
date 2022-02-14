/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.d2.balancer.properties;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * ClusterPropertiesWithCanary includes {@link ClusterProperties} and a canary version of the cluster properties, and a distribution strategy.
 * It is serialized and stored on service registry (like Zookeeper).
 * Canary cluster properties are to be used when a D2 client falls into the canary group, as opposed to the original cluster properties.
 *
 * NOTE: {@link ClusterStoreProperties} includes ALL properties on a cluster store on service registry (zookeeper).
 *
 * DEPRECATED: use {@link ClusterStoreProperties} instead.
 */
@Deprecated
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

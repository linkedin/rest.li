/*
   Copyright (c) 2022 LinkedIn Corp.

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.linkedin.d2.balancer.util.canary.CanaryDistributionProvider;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * ClusterStoreProperties has ALL the properties serialized and stored on a cluster store on service registry (like zookeeper).
 * It includes cluster properties, canary cluster properties, canary distribution strategy and could also include properties of
 * other D2 features applicable to the cluster but are not part of the cluster properties, such as application fail-over.
 *
 * NOTE: Most likely you want POJO's here (e.g: Map<String, Object>), and not include pegasus generated objects, because
 * certain objects, like DarkClusterConfigMap, are serialized differently than
 * how Jackson would serialize the object (for instance, using different key names), and
 * that will cause problems in serialization/deserialization.
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // NOTE: fields with null values will NOT be serialized (won't be included in ZK data)
public class ClusterStoreProperties extends ClusterProperties
{
  protected final ClusterProperties _canaryConfigs;
  protected final CanaryDistributionStrategy _canaryDistributionStrategy;
  protected final FailoutProperties _failoutProperties;

  public ClusterStoreProperties(String clusterName)
  {
    this(clusterName, Collections.<String>emptyList());
  }

  public ClusterStoreProperties(String clusterName, List<String> prioritizedSchemes)
  {
    this(clusterName, prioritizedSchemes, Collections.<String,String>emptyMap());
  }

  public ClusterStoreProperties(String clusterName,
      List<String> prioritizedSchemes,
      Map<String, String> properties)
  {
    this(clusterName, prioritizedSchemes, properties, new HashSet<>());
  }

  public ClusterStoreProperties(String clusterName,
      List<String> prioritizedSchemes,
      Map<String, String> properties,
      Set<URI> bannedUris)
  {
    this(clusterName, prioritizedSchemes, properties, bannedUris, NullPartitionProperties.getInstance());
  }

  public ClusterStoreProperties(String clusterName,
      List<String> prioritizedSchemes,
      Map<String, String> properties,
      Set<URI> bannedUris,
      PartitionProperties partitionProperties)
  {
    this(clusterName, prioritizedSchemes, properties, bannedUris, partitionProperties, Collections.emptyList());
  }

  public ClusterStoreProperties(String clusterName,
      List<String> prioritizedSchemes,
      Map<String, String> properties,
      Set<URI> bannedUris,
      PartitionProperties partitionProperties,
      List<String> sslSessionValidationStrings)
  {
    this(clusterName, prioritizedSchemes, properties, bannedUris, partitionProperties, sslSessionValidationStrings,
        (Map<String, Object>) null, false);
  }

  public ClusterStoreProperties(String clusterName,
      List<String> prioritizedSchemes,
      Map<String, String> properties,
      Set<URI> bannedUris,
      PartitionProperties partitionProperties,
      List<String> sslSessionValidationStrings,
      Map<String, Object> darkClusters,
      boolean delegated)
  {
    this(clusterName, prioritizedSchemes, properties, bannedUris, partitionProperties, sslSessionValidationStrings, darkClusters, delegated,
        null, null, null);
  }

  public ClusterStoreProperties(String clusterName,
      List<String> prioritizedSchemes,
      Map<String, String> properties,
      Set<URI> bannedUris,
      PartitionProperties partitionProperties,
      List<String> sslSessionValidationStrings,
      Map<String, Object> darkClusters,
      boolean delegated,
      ClusterProperties canaryConfigs,
      CanaryDistributionStrategy distributionStrategy)
  {
    super(clusterName, prioritizedSchemes, properties, bannedUris, partitionProperties, sslSessionValidationStrings, darkClusters, delegated);
    _canaryConfigs = canaryConfigs;
    _canaryDistributionStrategy = distributionStrategy;
    _failoutProperties = null;
  }

  public ClusterStoreProperties(ClusterProperties stableConfigs,
      ClusterProperties canaryConfigs,
      CanaryDistributionStrategy distributionStrategy,
      FailoutProperties failoutProperties)
  {
    super(stableConfigs.getClusterName(), stableConfigs.getPrioritizedSchemes(),
        stableConfigs.getProperties(), stableConfigs.getBannedUris(), stableConfigs.getPartitionProperties(),
        stableConfigs.getSslSessionValidationStrings(), stableConfigs.getDarkClusters(),
        stableConfigs.isDelegated());
    _canaryConfigs = canaryConfigs;
    _canaryDistributionStrategy = distributionStrategy;
    _failoutProperties = failoutProperties;
  }

  public ClusterStoreProperties(String clusterName,
      List<String> prioritizedSchemes,
      Map<String, String> properties,
      Set<URI> bannedUris,
      PartitionProperties partitionProperties,
      List<String> sslSessionValidationStrings,
      Map<String, Object> darkClusters,
      boolean delegated,
      ClusterProperties canaryConfigs,
      CanaryDistributionStrategy distributionStrategy,
      FailoutProperties failoutProperties)
  {
    super(clusterName, prioritizedSchemes, properties, bannedUris, partitionProperties, sslSessionValidationStrings, darkClusters, delegated);
    _canaryConfigs = canaryConfigs;
    _canaryDistributionStrategy = distributionStrategy;
    _failoutProperties = failoutProperties;
  }

  public ClusterStoreProperties(ClusterProperties stableConfigs,
      ClusterProperties canaryConfigs,
      CanaryDistributionStrategy distributionStrategy)
  {
    super(stableConfigs.getClusterName(), stableConfigs.getPrioritizedSchemes(),
        stableConfigs.getProperties(), stableConfigs.getBannedUris(), stableConfigs.getPartitionProperties(),
        stableConfigs.getSslSessionValidationStrings(), stableConfigs.getDarkClusters(),
        stableConfigs.isDelegated());
    _canaryConfigs = canaryConfigs;
    _canaryDistributionStrategy = distributionStrategy;
    _failoutProperties = null;
  }

  public ClusterProperties getCanaryConfigs()
  {
    return _canaryConfigs;
  }

  public CanaryDistributionStrategy getCanaryDistributionStrategy()
  {
    return _canaryDistributionStrategy;
  }

  public FailoutProperties getFailoutProperties()
  {
    return _failoutProperties;
  }

  public boolean hasCanary() {
    return _canaryConfigs != null && _canaryDistributionStrategy != null;
  }

  /**
   * Given a canary distribution (stable or canary), return the corresponding distributed/picked cluster properties.
   */
  public ClusterProperties getDistributedClusterProperties(CanaryDistributionProvider.Distribution distribution)
  {
    if (distribution.equals(CanaryDistributionProvider.Distribution.CANARY) && hasCanary())
    {
      return _canaryConfigs;
    }
    return new ClusterProperties(this); // make a copy of stable configs with the super class copy constructor
  }

  @Override
  public String toString()
  {
    return "ClusterStoreProperties [_stableClusterProperties=" + super.toString() + ", _canaryConfigs=" + _canaryConfigs
        + ", _canaryDistributionStrategy=" + _canaryDistributionStrategy
        + ", _failoutProperties=" + _failoutProperties + "]";
  }

  @Override
  public int hashCode()
  {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((_canaryConfigs == null) ? 0 : _canaryConfigs.hashCode());
    result = prime * result + ((_canaryDistributionStrategy == null) ? 0 : _canaryDistributionStrategy.hashCode());
    result = prime * result + ((_failoutProperties == null) ? 0 : _failoutProperties.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (!super.equals(obj))
    {
      return false;
    }

    return (obj instanceof ClusterStoreProperties) && canaryEquals((ClusterStoreProperties) obj) && failoutEquals((ClusterStoreProperties) obj);
  }

  private boolean failoutEquals(ClusterStoreProperties other)
  {
    if (_failoutProperties == null && other.getFailoutProperties() != null) {
      return false;
    }
    if (_failoutProperties != null && !_failoutProperties.equals(other.getFailoutProperties())) {
      return false;
    }
    return true;
  }

  private boolean canaryEquals(ClusterStoreProperties other)
  {
    if (hasCanary() != other.hasCanary())
    {
      return false;
    }
    return !hasCanary()
        || (_canaryConfigs.equals(other.getCanaryConfigs()) && _canaryDistributionStrategy.equals(other.getCanaryDistributionStrategy()));
  }
}

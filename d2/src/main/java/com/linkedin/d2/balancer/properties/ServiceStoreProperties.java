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

import com.linkedin.d2.balancer.subsetting.SubsettingStrategy;
import com.linkedin.d2.balancer.util.canary.CanaryDistributionProvider;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * ServiceStoreProperties has ALL the properties serialized and stored on a service store on service registry (like zookeeper).
 * It includes service properties, canary service properties, canary distribution strategy and could also include properties of
 * other D2 features applicable to the service but are not part of the service properties.
 *
 * NOTE: Most likely you want POJO's here (e.g: Map<String, Object>), and not include pegasus generated objects, because
 * certain objects are serialized differently than how Jackson would serialize the object (for instance, using different key names), and
 * that will cause problems in serialization/deserialization.
 */
public class ServiceStoreProperties extends ServiceProperties
{
  protected final ServiceProperties _canaryConfigs;
  protected final CanaryDistributionStrategy _canaryDistributionStrategy;

  public ServiceStoreProperties(String serviceName, String clusterName, String path,
      List<String> prioritizedStrategyList) {
    this(serviceName, clusterName, path, prioritizedStrategyList,
        Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(),
        Collections.<String, String>emptyMap(),
        Collections.<String>emptyList(), Collections.<URI>emptySet());
  }

  public ServiceStoreProperties(String serviceName, String clusterName, String path,
      List<String> prioritizedStrategyList, ServiceProperties canaryConfigs, CanaryDistributionStrategy distributionStrategy) {
    super(serviceName, clusterName, path, prioritizedStrategyList,
        Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(),
        Collections.<String, String>emptyMap(),
        Collections.<String>emptyList(), Collections.<URI>emptySet());
    _canaryConfigs = canaryConfigs;
    _canaryDistributionStrategy = distributionStrategy;
  }

  public ServiceStoreProperties(String serviceName, String clusterName, String path,
      List<String> prioritizedStrategyList, Map<String, Object> loadBalancerStrategyProperties) {
    this(serviceName, clusterName, path, prioritizedStrategyList, loadBalancerStrategyProperties,
        Collections.<String, Object>emptyMap(), Collections.<String, String>emptyMap(),
        Collections.<String>emptyList(), Collections.<URI>emptySet());
  }

  public ServiceStoreProperties(String serviceName, String clusterName, String path,
      List<String> prioritizedStrategyList, Map<String, Object> loadBalancerStrategyProperties,
      Map<String, Object> transportClientProperties, Map<String, String> degraderProperties,
      List<String> prioritizedSchemes, Set<URI> banned) {
    this(serviceName, clusterName, path, prioritizedStrategyList, loadBalancerStrategyProperties,
        transportClientProperties, degraderProperties, prioritizedSchemes, banned, Collections.<String,Object>emptyMap());
  }

  public ServiceStoreProperties(String serviceName, String clusterName, String path,
      List<String> prioritizedStrategyList, Map<String, Object> loadBalancerStrategyProperties,
      Map<String, Object> transportClientProperties, Map<String, String> degraderProperties,
      List<String> prioritizedSchemes, Set<URI> banned, Map<String, Object> serviceMetadataProperties) {
    this(serviceName, clusterName, path, prioritizedStrategyList, loadBalancerStrategyProperties,
        transportClientProperties, degraderProperties, prioritizedSchemes, banned, serviceMetadataProperties, Collections.emptyList());
  }

  public ServiceStoreProperties(String serviceName, String clusterName, String path,
      List<String> prioritizedStrategyList, Map<String, Object> loadBalancerStrategyProperties,
      Map<String, Object> transportClientProperties, Map<String, String> degraderProperties,
      List<String> prioritizedSchemes, Set<URI> banned, Map<String, Object> serviceMetadataProperties,
      List<Map<String, Object>> backupRequests) {
    this(serviceName, clusterName, path, prioritizedStrategyList, loadBalancerStrategyProperties,
        transportClientProperties, degraderProperties, prioritizedSchemes, banned, serviceMetadataProperties,
        backupRequests, null);
  }

  public ServiceStoreProperties(String serviceName, String clusterName, String path,
      List<String> prioritizedStrategyList, Map<String, Object> loadBalancerStrategyProperties,
      Map<String, Object> transportClientProperties, Map<String, String> degraderProperties,
      List<String> prioritizedSchemes, Set<URI> banned, Map<String, Object> serviceMetadataProperties,
      List<Map<String, Object>> backupRequests, Map<String, Object> relativeStrategyProperties) {
    this(serviceName, clusterName, path, prioritizedStrategyList, loadBalancerStrategyProperties,
        transportClientProperties, degraderProperties, prioritizedSchemes, banned, serviceMetadataProperties,
        backupRequests, relativeStrategyProperties, SubsettingStrategy.DEFAULT_ENABLE_CLUSTER_SUBSETTING, SubsettingStrategy.DEFAULT_CLUSTER_SUBSET_SIZE);
  }

  public ServiceStoreProperties(String serviceName, String clusterName, String path,
      List<String> prioritizedStrategyList, Map<String, Object> loadBalancerStrategyProperties,
      Map<String, Object> transportClientProperties, Map<String, String> degraderProperties,
      List<String> prioritizedSchemes, Set<URI> banned, Map<String, Object> serviceMetadataProperties,
      List<Map<String, Object>> backupRequests, Map<String, Object> relativeStrategyProperties,
      boolean enableClusterSubsetting, int minClusterSubsetSize) {
    this(serviceName, clusterName, path, prioritizedStrategyList, loadBalancerStrategyProperties,
        transportClientProperties, degraderProperties, prioritizedSchemes, banned, serviceMetadataProperties,
        backupRequests, relativeStrategyProperties, enableClusterSubsetting, minClusterSubsetSize, null, null);
  }

  public ServiceStoreProperties(String serviceName, String clusterName, String path,
      List<String> prioritizedStrategyList, Map<String, Object> loadBalancerStrategyProperties,
      Map<String, Object> transportClientProperties, Map<String, String> degraderProperties,
      List<String> prioritizedSchemes, Set<URI> banned, Map<String, Object> serviceMetadataProperties,
      List<Map<String, Object>> backupRequests, Map<String, Object> relativeStrategyProperties,
      boolean enableClusterSubsetting, int minClusterSubsetSize, ServiceProperties canaryConfigs, CanaryDistributionStrategy distributionStrategy)
  {
    super(serviceName, clusterName, path, prioritizedStrategyList, loadBalancerStrategyProperties,
        transportClientProperties, degraderProperties, prioritizedSchemes, banned, serviceMetadataProperties,
        backupRequests, relativeStrategyProperties, enableClusterSubsetting, minClusterSubsetSize);
    _canaryConfigs = canaryConfigs;
    _canaryDistributionStrategy = distributionStrategy;
  }

  public ServiceStoreProperties(ServiceProperties stableConfigs, ServiceProperties canaryConfigs, CanaryDistributionStrategy distributionStrategy)
  {
    super(stableConfigs.getServiceName(), stableConfigs.getClusterName(), stableConfigs.getPath(),
        stableConfigs.getLoadBalancerStrategyList(), stableConfigs.getLoadBalancerStrategyProperties(),
        stableConfigs.getTransportClientProperties(), stableConfigs.getDegraderProperties(),
        stableConfigs.getPrioritizedSchemes(), stableConfigs.getBanned(), stableConfigs.getServiceMetadataProperties(),
        stableConfigs.getBackupRequests(), stableConfigs.getRelativeStrategyProperties(),
        stableConfigs.isEnableClusterSubsetting(), stableConfigs.getMinClusterSubsetSize());
    _canaryConfigs = canaryConfigs;
    _canaryDistributionStrategy = distributionStrategy;
  }

  public ServiceProperties getCanaryConfigs()
  {
    return _canaryConfigs;
  }

  public CanaryDistributionStrategy getCanaryDistributionStrategy()
  {
    return _canaryDistributionStrategy;
  }

  public boolean hasCanary() {
    return _canaryConfigs != null && _canaryDistributionStrategy != null;
  }

  /**
   * Given a canary distribution (stable or canary), return the corresponding distributed/picked service properties.
   */
  public ServiceProperties getDistributedServiceProperties(CanaryDistributionProvider.Distribution distribution)
  {
    if (distribution.equals(CanaryDistributionProvider.Distribution.CANARY) || hasCanary())
    {
      return _canaryConfigs;
    }
    return new ServiceProperties(getServiceName(), getClusterName(), getPath(), getLoadBalancerStrategyList(), getLoadBalancerStrategyProperties(),
        getTransportClientProperties(), getDegraderProperties(), getPrioritizedSchemes(), getBanned(), getServiceMetadataProperties(), getBackupRequests(),
        getRelativeStrategyProperties(), isEnableClusterSubsetting(), getMinClusterSubsetSize());
  }

  @Override
  public String toString()
  {
    return "ServiceStoreProperties [_stableServiceProperties=" + super.toString() + ", _canaryConfigs=" + _canaryConfigs
        + ", _canaryDistributionStrategy=" + _canaryDistributionStrategy + "]";
  }

  @Override
  public int hashCode()
  {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((_canaryConfigs == null) ? 0 : _canaryConfigs.hashCode());
    result = prime * result + ((_canaryDistributionStrategy == null) ? 0 : _canaryDistributionStrategy.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (!super.equals(obj))
    {
      return false;
    }
    ServiceStoreProperties other = (ServiceStoreProperties) obj;
    return canaryEquals(other);
  }

  private boolean canaryEquals(ServiceStoreProperties other)
  {
    if (hasCanary() != other.hasCanary())
    {
      return false;
    }
    return !hasCanary()
        || (_canaryConfigs.equals(other.getCanaryConfigs()) && _canaryDistributionStrategy.equals(other.getCanaryDistributionStrategy()));
  }
}

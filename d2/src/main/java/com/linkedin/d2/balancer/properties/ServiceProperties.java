/*
   Copyright (c) 2012 LinkedIn Corp.

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
import com.linkedin.util.ArgumentUtil;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * ServiceProperties are the properties that define a service and its behaviors.
 * It is the serialized service object as part of {@link ServiceStoreProperties} that is stored in zookeeper.
 *
 * NOTE: {@link ServiceStoreProperties} includes ALL properties on a service store on service registry (zookeeper).
 *
 * Serialization NOTE: Most likely you want POJO's here (e.g: Map<String, Object>), and not include pegasus generated objects, because
 * certain objects are serialized differently than how Jackson would serialize the object (for instance, using different key names), and
 * that will cause problems in serialization/deserialization.
 */
public class ServiceProperties
{
  private final String _serviceName;
  private final String _clusterName;
  private final String _path;
  private final List<String> _prioritizedStrategyList;
  private final Map<String, Object> _loadBalancerStrategyProperties;
  private final Map<String, Object> _transportClientProperties;
  private final Map<String, Object> _relativeStrategyProperties;
  private final List<Map<String, Object>> _backupRequests;  // each map in the list represents one backup requests strategy
  private final Map<String, String> _degraderProperties;
  private final List<String> _prioritizedSchemes;
  private final Set<URI> _banned;
  private final Map<String, Object> _serviceMetadataProperties;
  private final boolean _enableClusterSubsetting;
  private final int _minClusterSubsetSize;

  public ServiceProperties(String serviceName,
                           String clusterName,
                           String path,
                           List<String> prioritizedStrategyList)
  {
    this(serviceName, clusterName, path, prioritizedStrategyList,
         Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(),
         Collections.<String, String>emptyMap(),
         Collections.<String>emptyList(), Collections.<URI>emptySet());
  }

  // The addition of the StrategyList is to allow new strategies to be introduced and be used as they
  // become available during code rollout. The intention is that this StrategyList replaces the
  // StrategyName once this List is available everywhere.
  public ServiceProperties(String serviceName,
                           String clusterName,
                           String path,
                           List<String> prioritizedStrategyList,
                           Map<String,Object> loadBalancerStrategyProperties)
  {
    this(serviceName, clusterName, path, prioritizedStrategyList, loadBalancerStrategyProperties,
         Collections.<String, Object>emptyMap(), Collections.<String, String>emptyMap(),
         Collections.<String>emptyList(), Collections.<URI>emptySet());
  }

  public ServiceProperties(String serviceName,
                           String clusterName,
                           String path,
                           List<String> prioritizedStrategyList,
                           Map<String,Object> loadBalancerStrategyProperties,
                           Map<String,Object> transportClientProperties,
                           Map<String,String> degraderProperties,
                           List<String> prioritizedSchemes,
                           Set<URI> banned)
  {
    this(serviceName, clusterName, path, prioritizedStrategyList, loadBalancerStrategyProperties,
         transportClientProperties, degraderProperties, prioritizedSchemes, banned,
         Collections.<String,Object>emptyMap());
  }

  public ServiceProperties(String serviceName,
      String clusterName,
      String path,
      List<String> prioritizedStrategyList,
      Map<String,Object> loadBalancerStrategyProperties,
      Map<String,Object> transportClientProperties,
      Map<String,String> degraderProperties,
      List<String> prioritizedSchemes,
      Set<URI> banned,
      Map<String,Object> serviceMetadataProperties)
  {
    this(serviceName, clusterName, path, prioritizedStrategyList, loadBalancerStrategyProperties,
         transportClientProperties, degraderProperties, prioritizedSchemes, banned,
         serviceMetadataProperties, Collections.emptyList());
  }

  public ServiceProperties(String serviceName,
                           String clusterName,
                           String path,
                           List<String> prioritizedStrategyList,
                           Map<String,Object> loadBalancerStrategyProperties,
                           Map<String,Object> transportClientProperties,
                           Map<String,String> degraderProperties,
                           List<String> prioritizedSchemes,
                           Set<URI> banned,
                           Map<String,Object> serviceMetadataProperties,
                           List<Map<String,Object>> backupRequests)
  {
    this(serviceName, clusterName, path, prioritizedStrategyList, loadBalancerStrategyProperties, transportClientProperties, degraderProperties,
         prioritizedSchemes, banned, serviceMetadataProperties, backupRequests, null);
  }

  public ServiceProperties(String serviceName,
                           String clusterName,
                           String path,
                           List<String> prioritizedStrategyList,
                           Map<String,Object> loadBalancerStrategyProperties,
                           Map<String,Object> transportClientProperties,
                           Map<String,String> degraderProperties,
                           List<String> prioritizedSchemes,
                           Set<URI> banned,
                           Map<String,Object> serviceMetadataProperties,
                           List<Map<String,Object>> backupRequests,
                           Map<String, Object> relativeStrategyProperties)
  {
    this(serviceName, clusterName, path, prioritizedStrategyList, loadBalancerStrategyProperties, transportClientProperties, degraderProperties,
        prioritizedSchemes, banned, serviceMetadataProperties, backupRequests, relativeStrategyProperties,
        SubsettingStrategy.DEFAULT_ENABLE_CLUSTER_SUBSETTING, SubsettingStrategy.DEFAULT_CLUSTER_SUBSET_SIZE);
  }

  public ServiceProperties(String serviceName,
      String clusterName,
      String path,
      List<String> prioritizedStrategyList,
      Map<String,Object> loadBalancerStrategyProperties,
      Map<String,Object> transportClientProperties,
      Map<String,String> degraderProperties,
      List<String> prioritizedSchemes,
      Set<URI> banned,
      Map<String,Object> serviceMetadataProperties,
      List<Map<String,Object>> backupRequests,
      Map<String, Object> relativeStrategyProperties,
      boolean enableClusterSubsetting,
      int minClusterSubsetSize)
  {
    ArgumentUtil.notNull(serviceName, PropertyKeys.SERVICE_NAME);
    ArgumentUtil.notNull(clusterName, PropertyKeys.CLUSTER_NAME);
    ArgumentUtil.notNull(path, PropertyKeys.PATH);

    if (prioritizedStrategyList == null || prioritizedStrategyList.isEmpty())
    {
      throw new NullPointerException("loadBalancerStrategyList is null or empty");
    }

    _backupRequests =
        Collections.unmodifiableList(backupRequests == null ? Collections.emptyList() : backupRequests);
    _serviceName = serviceName;
    _clusterName = clusterName;
    _path = path;
    _prioritizedStrategyList = Collections.unmodifiableList(prioritizedStrategyList);
    _loadBalancerStrategyProperties = loadBalancerStrategyProperties != null
        ? Collections.unmodifiableMap(loadBalancerStrategyProperties) : Collections.emptyMap();
    _transportClientProperties = (transportClientProperties != null) ?
        Collections.unmodifiableMap(transportClientProperties) : Collections.emptyMap();
    _degraderProperties = (degraderProperties != null) ? Collections.unmodifiableMap(degraderProperties) :
        Collections.<String, String>emptyMap();
    _prioritizedSchemes = (prioritizedSchemes != null) ? Collections.unmodifiableList(prioritizedSchemes) :
        Collections.<String>emptyList();
    _banned = (banned != null) ? Collections.unmodifiableSet(banned) : Collections.emptySet();
    _serviceMetadataProperties = (serviceMetadataProperties != null) ? Collections.unmodifiableMap(serviceMetadataProperties) :
        Collections.<String,Object>emptyMap();
    _relativeStrategyProperties = relativeStrategyProperties != null
        ? relativeStrategyProperties : Collections.emptyMap();
    _enableClusterSubsetting = enableClusterSubsetting;
    _minClusterSubsetSize = minClusterSubsetSize;
  }

  public ServiceProperties(ServiceProperties other)
  {
    this(other._serviceName, other._clusterName, other._path, other._prioritizedStrategyList, other._loadBalancerStrategyProperties,
        other._transportClientProperties, other._degraderProperties, other._prioritizedSchemes, other._banned, other._serviceMetadataProperties,
        other._backupRequests, other._relativeStrategyProperties, other._enableClusterSubsetting, other._minClusterSubsetSize);
  }

  public String getClusterName()
  {
    return _clusterName;
  }

  /**
   * @return Prioritized {@link com.linkedin.d2.balancer.strategies.LoadBalancerStrategy} list.
   */
  public List<String> getLoadBalancerStrategyList()
  {
    return _prioritizedStrategyList;
  }

  public String getPath()
  {
    return _path;
  }

  public String getServiceName()
  {
    return _serviceName;
  }

  /**
   * @return Properties used by load balancer component of {@link com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3}.
   */
  public Map<String,Object> getLoadBalancerStrategyProperties()
  {
    return _loadBalancerStrategyProperties;
  }

  /**
   * @return Properties used by degrader component of {@link com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3}.
   */
  public Map<String, String> getDegraderProperties()
  {
    return _degraderProperties;
  }

  /**
   * @return Properties used by {@link com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategy}.
   */
  public Map<String, Object> getRelativeStrategyProperties()
  {
    return _relativeStrategyProperties;
  }

  public Map<String, Object> getTransportClientProperties()
  {
    return _transportClientProperties;
  }

  public List<Map<String, Object>> getBackupRequests()
  {
    return _backupRequests;
  }

  public List<String> getPrioritizedSchemes()
  {
    return _prioritizedSchemes;
  }

  public Set<URI> getBanned()
  {
    return _banned;
  }

  public boolean isBanned(URI uri)
  {
    return _banned.contains(uri);
  }

  public Map<String,Object> getServiceMetadataProperties()
  {
    return _serviceMetadataProperties;
  }

  public boolean isEnableClusterSubsetting()
  {
    return _enableClusterSubsetting;
  }

  public int getMinClusterSubsetSize()
  {
    return _minClusterSubsetSize;
  }

  @Override
  public String toString()
  {
    return "ServiceProperties [_clusterName=" + _clusterName
        +  ", _path=" + _path
        + ", _serviceName=" + _serviceName + ", _loadBalancerStrategyList=" + _prioritizedStrategyList
        + ", _loadBalancerStrategyProperties="
        + _loadBalancerStrategyProperties
        + ", _transportClientProperties="
        + _transportClientProperties
        + ", _relativeStrategyProperties="
        + _relativeStrategyProperties
        + ", _degraderProperties="
        + _degraderProperties
        + ", prioritizedSchemes="
        + _prioritizedSchemes
        + ", bannedUris="
        + _banned
        + ", serviceMetadata="
        + _serviceMetadataProperties
        + ", backupRequests="
        + _backupRequests
        + ", enableClusterSubsetting="
        + _enableClusterSubsetting
        + ", minimumClusterSubsetSize="
        + _minClusterSubsetSize
        + "]";
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + _clusterName.hashCode();
    result = prime * result + _prioritizedStrategyList.hashCode();
    result = prime * result + _path.hashCode();
    result = prime * result + _serviceName.hashCode();
    result = prime * result + _loadBalancerStrategyProperties.hashCode();
    result = prime * result + _degraderProperties.hashCode();
    result = prime * result + _transportClientProperties.hashCode();
    result = prime * result + _backupRequests.hashCode();
    result = prime * result + _prioritizedSchemes.hashCode();
    result = prime * result + _banned.hashCode();
    result = prime * result + _serviceMetadataProperties.hashCode();
    result = prime * result + _relativeStrategyProperties.hashCode();
    result = prime * result + Boolean.hashCode(_enableClusterSubsetting);
    result = prime * result + Integer.hashCode(_minClusterSubsetSize);
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ServiceProperties other = (ServiceProperties) obj;
    if (!_clusterName.equals(other._clusterName))
      return false;
    if (!_prioritizedStrategyList.equals(other._prioritizedStrategyList))
      return false;
    if (!_path.equals(other._path))
      return false;
    if (!_serviceName.equals(other._serviceName))
      return false;
    if (!_loadBalancerStrategyProperties.equals(other._loadBalancerStrategyProperties))
      return false;
    if (!_transportClientProperties.equals(other._transportClientProperties))
      return false;
    if (!_backupRequests.equals(other._backupRequests))
      return false;
    if (!_degraderProperties.equals(other._degraderProperties))
      return false;
    if (!_prioritizedSchemes.equals(other._prioritizedSchemes))
      return false;
    if (!_banned.equals(other._banned))
      return false;
    if (!_serviceMetadataProperties.equals(other._serviceMetadataProperties))
      return false;
    if (!_relativeStrategyProperties.equals(other._relativeStrategyProperties))
      return false;
    if (_enableClusterSubsetting != other._enableClusterSubsetting)
      return false;
    if (_minClusterSubsetSize != other._minClusterSubsetSize)
      return false;
    return true;
  }

}

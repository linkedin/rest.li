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

import com.linkedin.util.ArgumentUtil;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ServiceProperties
{
  private final String _serviceName;
  private final String _clusterName;
  private final String _path;
  private final String _loadBalancerStrategyName;
  private final List<String> _loadBalancerStrategyList;
  private final Map<String,Object> _loadBalancerStrategyProperties;
  private final Map<String,Object> _transportClientProperties;
  private final Map<String,String> _degraderProperties;
  private final List<String> _prioritizedSchemes;
  private final Set<URI> _banned;

  public ServiceProperties(String serviceName,
                           String clusterName,
                           String path,
                           String loadBalancerStrategyName)
  {
    this(serviceName, clusterName, path, loadBalancerStrategyName, null,
         Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(),
         Collections.<String, String>emptyMap(),
         Collections.<String>emptyList(), Collections.<URI>emptySet());
  }

  public ServiceProperties(String serviceName,
                           String clusterName,
                           String path,
                           String loadBalancerStrategyName,
                           Map<String,Object> loadBalancerStrategyProperties)
  {
    this(serviceName, clusterName, path, loadBalancerStrategyName, null, loadBalancerStrategyProperties,
         Collections.<String, Object>emptyMap(), Collections.<String, String>emptyMap(),
                  Collections.<String>emptyList(), Collections.<URI>emptySet());
  }

  // The addition of the StrategyList is to allow new strategies to be introduced and be used as they
  // become available during code rollout. The intention is that this StrategyList replaces the
  // StrategyName once this List is available everywhere.
  public ServiceProperties(String serviceName,
                           String clusterName,
                           String path,
                           String loadBalancerStrategyName,
                           List<String> loadBalancerStrategyList,
                           Map<String,Object> loadBalancerStrategyProperties)
  {
    this(serviceName,clusterName,path,loadBalancerStrategyName,loadBalancerStrategyList,loadBalancerStrategyProperties,
         Collections.<String, Object>emptyMap(), Collections.<String, String>emptyMap(),
         Collections.<String>emptyList(), Collections.<URI>emptySet());
  }

  public ServiceProperties(String serviceName,
                           String clusterName,
                           String path,
                           String loadBalancerStrategyName,
                           List<String> loadBalancerStrategyList,
                           Map<String,Object> loadBalancerStrategyProperties,
                           Map<String,Object> transportClientProperties,
                           Map<String,String> degraderProperties,
                           List<String> prioritizedSchemes,
                           Set<URI> banned)
  {
    ArgumentUtil.notNull(serviceName, PropertyKeys.SERVICE_NAME);
    ArgumentUtil.notNull(clusterName, PropertyKeys.CLUSTER_NAME);
    ArgumentUtil.notNull(path, PropertyKeys.PATH);
    ArgumentUtil.notNull(loadBalancerStrategyProperties, "loadBalancerStrategyProperties");
    if (loadBalancerStrategyName == null && (loadBalancerStrategyList == null || loadBalancerStrategyList.isEmpty()))
    {
      throw new NullPointerException("Both loadBalancerStrategyName and loadBalancerStrategyList are null");
    }

    _serviceName = serviceName;
    _clusterName = clusterName;
    _path = path;
    _loadBalancerStrategyName = loadBalancerStrategyName;
    _loadBalancerStrategyList = (loadBalancerStrategyList != null) ?
            Collections.unmodifiableList(loadBalancerStrategyList)
            : Collections.<String>emptyList();
    _loadBalancerStrategyProperties = Collections.unmodifiableMap(loadBalancerStrategyProperties);
    _transportClientProperties = (transportClientProperties != null) ?
        Collections.unmodifiableMap(transportClientProperties) : Collections.<String, Object>emptyMap();
    _degraderProperties = (degraderProperties != null) ? Collections.unmodifiableMap(degraderProperties) :
      Collections.<String, String>emptyMap();
    _prioritizedSchemes = (prioritizedSchemes != null) ? Collections.unmodifiableList(prioritizedSchemes) :
       Collections.<String>emptyList();
    _banned = (banned != null) ? Collections.unmodifiableSet(banned) : Collections.<URI>emptySet();
  }


  public String getClusterName()
  {
    return _clusterName;
  }

  public String getLoadBalancerStrategyName()
  {
    return _loadBalancerStrategyName;
  }

  public List<String> getLoadBalancerStrategyList()
  {
    return _loadBalancerStrategyList;
  }

  public String getPath()
  {
    return _path;
  }

  public String getServiceName()
  {
    return _serviceName;
  }

  public Map<String,Object> getLoadBalancerStrategyProperties()
  {
    return _loadBalancerStrategyProperties;
  }

  public Map<String, Object> getTransportClientProperties()
  {
    return _transportClientProperties;
  }

  public Map<String, String> getDegraderProperties()
  {
    return _degraderProperties;
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

  @Override
  public String toString()
  {
    return "ServiceProperties [_clusterName=" + _clusterName
        + ", _loadBalancerStrategyName=" + _loadBalancerStrategyName + ", _path=" + _path
        + ", _serviceName=" + _serviceName + ", _loadBalancerStrategyList=" + _loadBalancerStrategyList
        + ", _loadBalancerStrategyProperties="
        + _loadBalancerStrategyProperties
        + ", _transportClientProperties="
        + _transportClientProperties
        + ", _degraderProperties="
        + _degraderProperties
        + ", prioritizedSchemes="
        + _prioritizedSchemes
        + ", bannedUris="
        + _banned
        + "]";
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + _clusterName.hashCode();
    result = prime * result + _loadBalancerStrategyName.hashCode();
    result = prime * result + _loadBalancerStrategyList.hashCode();
    result = prime * result + _path.hashCode();
    result = prime * result + _serviceName.hashCode();
    result = prime * result + _loadBalancerStrategyProperties.hashCode();
    result = prime * result + _degraderProperties.hashCode();
    result = prime * result + _transportClientProperties.hashCode();
    result = prime * result + _prioritizedSchemes.hashCode();
    result = prime * result + _banned.hashCode();
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
    if (!_loadBalancerStrategyName.equals(other._loadBalancerStrategyName))
      return false;
    if (!_loadBalancerStrategyList.equals(other._loadBalancerStrategyList))
      return false;
    if (!_path.equals(other._path))
      return false;
    if (!_serviceName.equals(other._serviceName))
      return false;
    if (!_loadBalancerStrategyProperties.equals(other._loadBalancerStrategyProperties))
      return false;
    if (!_transportClientProperties.equals(other._transportClientProperties))
          return false;
    if (!_degraderProperties.equals(other._degraderProperties))
          return false;
    if (!_prioritizedSchemes.equals(other._prioritizedSchemes))
          return false;
    if (!_banned.equals(other._banned))
          return false;
    return true;
  }

}

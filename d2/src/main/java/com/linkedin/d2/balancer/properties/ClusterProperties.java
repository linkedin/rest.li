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

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClusterProperties
{
  private final String                _clusterName;
  private final Map<String, String>   _properties;
  private final PartitionProperties   _partitionProperties;

  //deprecated because we are moving these properties down to ServiceProperties
  @Deprecated
  private final Set<URI>              _banned;
  @Deprecated
  private final List<String>          _prioritizedSchemes;

  public ClusterProperties(String clusterName)
  {
    this(clusterName, Collections.<String>emptyList());
  }

  public ClusterProperties(String clusterName, List<String> prioritizedSchemes)
  {
    this(clusterName, prioritizedSchemes, Collections.<String,String>emptyMap());
  }

  public ClusterProperties(String clusterName,
                           List<String> prioritizedSchemes,
                           Map<String, String> properties)
  {
    this(clusterName, prioritizedSchemes, properties, new HashSet<URI>());
  }

  public ClusterProperties(String clusterName,
                           List<String> prioritizedSchemes,
                           Map<String, String> properties,
                           Set<URI> banned)
  {
    this(clusterName, prioritizedSchemes, properties, banned, NullPartitionProperties.getInstance());
  }

  public ClusterProperties(String clusterName,
                           List<String> prioritizedSchemes,
                           Map<String, String> properties,
                           Set<URI> banned,
                           PartitionProperties partitionProperties)
  {
    _clusterName = clusterName;
    _prioritizedSchemes =
        (prioritizedSchemes != null) ? Collections.unmodifiableList(prioritizedSchemes)
            : Collections.<String>emptyList();
    _properties = (properties == null) ? Collections.<String,String>emptyMap() : Collections.unmodifiableMap(properties);
    _banned = Collections.unmodifiableSet(banned);
    _partitionProperties = partitionProperties;
  }

  public boolean isBanned(URI uri)
  {
    return _banned.contains(uri);
  }

  public Set<URI> getBanned()
  {
    return _banned;
  }

  public String getClusterName()
  {
    return _clusterName;
  }

  public List<String> getPrioritizedSchemes()
  {
    return _prioritizedSchemes;
  }

  public Map<String, String> getProperties()
  {
    return _properties;
  }

  public PartitionProperties getPartitionProperties()
  {
    return _partitionProperties;
  }

  @Override
  public String toString()
  {
    return "ClusterProperties [_clusterName=" + _clusterName + ", _prioritizedSchemes="
        + _prioritizedSchemes + ", _properties=" + _properties + ", _banned=" + _banned
        + ", _partitionProperties=" + _partitionProperties + "]";
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_banned == null) ? 0 : _banned.hashCode());
    result = prime * result + ((_clusterName == null) ? 0 : _clusterName.hashCode());
    result =
        prime * result
            + ((_prioritizedSchemes == null) ? 0 : _prioritizedSchemes.hashCode());
    result = prime * result + ((_properties == null) ? 0 : _properties.hashCode());
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
    ClusterProperties other = (ClusterProperties) obj;
    if (_banned == null)
    {
      if (other._banned != null)
        return false;
    }
    else if (!_banned.equals(other._banned))
      return false;
    if (_clusterName == null)
    {
      if (other._clusterName != null)
        return false;
    }
    else if (!_clusterName.equals(other._clusterName))
      return false;
    if (_prioritizedSchemes == null)
    {
      if (other._prioritizedSchemes != null)
        return false;
    }
    else if (!_prioritizedSchemes.equals(other._prioritizedSchemes))
      return false;
    if (_properties == null)
    {
      if (other._properties != null)
        return false;
    }
    else if (!_properties.equals(other._properties))
      return false;

    return true;
  }
}

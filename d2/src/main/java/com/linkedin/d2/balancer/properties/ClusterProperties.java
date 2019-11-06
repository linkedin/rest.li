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

import com.linkedin.d2.DarkClusterConfigMap;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClusterProperties
{
  public static final float DARK_CLUSTER_DEFAULT_MULTIPLIER = 0.0f;
  public static final int DARK_CLUSTER_DEFAULT_TARGET_RATE = 0;
  public static final int DARK_CLUSTER_DEFAULT_MAX_RATE = 2147483647;

  private final String                _clusterName;
  private final Map<String, String>   _properties;
  private final PartitionProperties   _partitionProperties;
  private final List<String> _sslSessionValidationStrings;

  private final Set<URI> _bannedUris;
  @Deprecated
  private final List<String>          _prioritizedSchemes;
  private final DarkClusterConfigMap _darkClusters;

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
    this(clusterName, prioritizedSchemes, properties, new HashSet<>());
  }

  public ClusterProperties(String clusterName,
                           List<String> prioritizedSchemes,
                           Map<String, String> properties,
                           Set<URI> bannedUris)
  {
    this(clusterName, prioritizedSchemes, properties, bannedUris, NullPartitionProperties.getInstance());
  }

  public ClusterProperties(String clusterName,
                           List<String> prioritizedSchemes,
                           Map<String, String> properties,
                           Set<URI> bannedUris,
                           PartitionProperties partitionProperties)
  {
    this(clusterName, prioritizedSchemes, properties, bannedUris, partitionProperties, Collections.emptyList());
  }

  public ClusterProperties(String clusterName,
      List<String> prioritizedSchemes,
      Map<String, String> properties,
      Set<URI> bannedUris,
      PartitionProperties partitionProperties,
      List<String> sslSessionValidationStrings)
  {
    this(clusterName, prioritizedSchemes, properties, bannedUris, partitionProperties, sslSessionValidationStrings,
        null);
  }

  public ClusterProperties(String clusterName,
      List<String> prioritizedSchemes,
      Map<String, String> properties,
      Set<URI> bannedUris,
      PartitionProperties partitionProperties,
      List<String> sslSessionValidationStrings,
      DarkClusterConfigMap darkClusters)

  {
    _clusterName = clusterName;
    _prioritizedSchemes =
        (prioritizedSchemes != null) ? Collections.unmodifiableList(prioritizedSchemes)
            : Collections.<String>emptyList();
    _properties = (properties == null) ? Collections.<String,String>emptyMap() : Collections.unmodifiableMap(properties);
    _bannedUris = bannedUris != null ? Collections.unmodifiableSet(bannedUris) : Collections.emptySet();
    _partitionProperties = partitionProperties;
    _sslSessionValidationStrings = sslSessionValidationStrings == null ? Collections.emptyList() : Collections.unmodifiableList(
        sslSessionValidationStrings);
    _darkClusters = darkClusters == null ? new DarkClusterConfigMap() : darkClusters;
  }

  public boolean isBanned(URI uri)
  {
    return _bannedUris.contains(uri);
  }

  public Set<URI> getBannedUris()
  {
    return _bannedUris;
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

  public List<String> getSslSessionValidationStrings()
  {
    return _sslSessionValidationStrings;
  }

  public DarkClusterConfigMap getDarkClusters()
  {
    return _darkClusters;
  }

  @Override
  public String toString()
  {
    return "ClusterProperties [_clusterName=" + _clusterName + ", _prioritizedSchemes="
        + _prioritizedSchemes + ", _properties=" + _properties + ", _bannedUris=" + _bannedUris
        + ", _partitionProperties=" + _partitionProperties + ", _sslSessionValidationStrings=" + _sslSessionValidationStrings
        + ", _darkClusterConfigMap=" + _darkClusters + "]";
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_bannedUris == null) ? 0 : _bannedUris.hashCode());
    result = prime * result + ((_clusterName == null) ? 0 : _clusterName.hashCode());
    result =
        prime * result
            + ((_prioritizedSchemes == null) ? 0 : _prioritizedSchemes.hashCode());
    result = prime * result + ((_properties == null) ? 0 : _properties.hashCode());
    result = prime * result + ((_partitionProperties == null) ? 0 : _partitionProperties.hashCode());
    result = prime * result + ((_sslSessionValidationStrings == null) ? 0 : _sslSessionValidationStrings.hashCode());
    result = prime * result + ((_darkClusters == null) ? 0 : _darkClusters.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    ClusterProperties other = (ClusterProperties) obj;
    if (!_bannedUris.equals(other._bannedUris))
    {
      return false;
    }
    if (!_clusterName.equals(other._clusterName))
    {
      return false;
    }
    if (!_prioritizedSchemes.equals(other._prioritizedSchemes))
    {
      return false;
    }
    if (!_properties.equals(other._properties))
    {
      return false;
    }
    if (!_partitionProperties.equals(other._partitionProperties))
    {
      return false;
    }
    if (!_darkClusters.equals(other._darkClusters))
    {
      return false;
    }
    return _sslSessionValidationStrings.equals(other._sslSessionValidationStrings);
  }
}

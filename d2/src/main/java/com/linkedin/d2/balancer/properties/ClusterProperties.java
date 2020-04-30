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
import com.linkedin.d2.balancer.config.DarkClustersConverter;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ClusterProperties is the serialized cluster object that is stored in zookeeper.
 *
 * Most likely you want POJO's here, and not include pegasus generated objects, because
 * certain objects, like transportClientProperties, are serialized differently than
 * how Jackson would serialize the object (for instance, using different key names), and
 * that will cause problems in serialization/deserialization. This is the reason _darkClusters
 * is a Map<String, Object> and not DarkClusterConfigMap. For simple objects that won't ever be
 * expanded it may be ok to reuse the pegasus objects.
 */
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
  private final Map<String, Object> _darkClusters;
  private final boolean              _delegated;

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
         (Map<String, Object>) null, false);
  }

  /**
   * @deprecated see below constructor for note on deprecation.
   */
  @Deprecated
  public ClusterProperties(String clusterName,
      List<String> prioritizedSchemes,
      Map<String, String> properties,
      Set<URI> bannedUris,
      PartitionProperties partitionProperties,
      List<String> sslSessionValidationStrings,
      DarkClusterConfigMap darkClusters)
  {
    this(clusterName, prioritizedSchemes, properties, bannedUris, partitionProperties, sslSessionValidationStrings,
        (Map<String, Object>)null, false);
  }

  /**
   * @deprecated Use the constructor that uses a Map instead of DarkClusterConfigMap. Using this object is not flexible enough to hold
   * transportClientProperties, because {@link com.linkedin.d2.balancer.config.TransportClientPropertiesConverter} uses different
   * keys in it's serialization than how Jackson would serialize D2TransportClientProperties. That is problematic since ClusterProperties
   * already should have had all necessary conversions done, but in this case the pegasus objects don't have a mechanism to allow the conversions.
   * The solution is to use a Map<String, Object> to pass in the darkClusters.
   */
  @Deprecated
  public ClusterProperties(String clusterName,
      List<String> prioritizedSchemes,
      Map<String, String> properties,
      Set<URI> bannedUris,
      PartitionProperties partitionProperties,
      List<String> sslSessionValidationStrings,
      DarkClusterConfigMap darkClusters,
      boolean delegated)
  {
    // Don't support this constructor by forcing a no-op dark cluster.
    this(clusterName, prioritizedSchemes, properties, bannedUris, partitionProperties, sslSessionValidationStrings, (Map<String, Object>)null, false);
  }

  public ClusterProperties(String clusterName,
                           List<String> prioritizedSchemes,
                           Map<String, String> properties,
                           Set<URI> bannedUris,
                           PartitionProperties partitionProperties,
                           List<String> sslSessionValidationStrings,
                           Map<String, Object> darkClusters,
                           boolean delegated)
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
    _darkClusters = darkClusters == null ? new HashMap<>() : darkClusters;
    _delegated = delegated;
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

  public Map<String, Object> getDarkClusters()
  {
    return _darkClusters;
  }

  // named so jackson won't use this method. This gives a more typesafe view of the dark clusters.
  public DarkClusterConfigMap accessDarkClusters()
  {
    return DarkClustersConverter.toConfig(_darkClusters);
  }

  public boolean isDelegated()
  {
    return _delegated;
  }

  @Override
  public String toString()
  {
    return "ClusterProperties [_clusterName=" + _clusterName + ", _prioritizedSchemes="
        + _prioritizedSchemes + ", _properties=" + _properties + ", _bannedUris=" + _bannedUris
        + ", _partitionProperties=" + _partitionProperties + ", _sslSessionValidationStrings=" + _sslSessionValidationStrings
        + ", _darkClusterConfigMap=" + _darkClusters + ", _delegated=" + _delegated + "]";
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
    result = prime * result + ((_delegated) ? 1 : 0);
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
    if (_delegated != other._delegated)
    {
      return false;
    }
    return _sslSessionValidationStrings.equals(other._sslSessionValidationStrings);
  }
}

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UriProperties
{
  private final String                              _clusterName;
  // this will categorize uris based on scheme and partitions.
  // For example, if we want to know the uris for scheme "http" and partition 5,
  // we can immediately get it by _urisBySchemeAndPartition.get("http").get(5)
  private final Map<String, Map<Integer, Set<URI>>>  _urisBySchemeAndPartition;
  // for serialization
  private final Map<URI, Map<Integer, PartitionData>> _partitionDesc;

  public UriProperties(String clusterName, Map<URI, Map<Integer, PartitionData>> partitionDescriptions)
  {
    _clusterName = clusterName;
    Map<URI, Map<Integer, PartitionData>> partitionDescriptionsMap = new HashMap<URI, Map<Integer, PartitionData>>(partitionDescriptions.size() * 2);
    for (Map.Entry<URI, Map<Integer, PartitionData>> entry : partitionDescriptions.entrySet())
    {
      partitionDescriptionsMap.put(entry.getKey(), Collections.unmodifiableMap(
          new HashMap<Integer, PartitionData>(entry.getValue())));
    }
    _partitionDesc = Collections.unmodifiableMap(partitionDescriptionsMap);

    // group uris by scheme and partition
    HashMap<String, Map<Integer, Set<URI>>> urisBySchemeAndPartition = new HashMap<String, Map<Integer, Set<URI>>>();
    for (Map.Entry<URI, Map<Integer, PartitionData>> entry : _partitionDesc.entrySet())
    {
      final URI uri = entry.getKey();
      Map<Integer, Set<URI>> map= urisBySchemeAndPartition.get(uri.getScheme());
      if (map == null)
      {
        map = new HashMap<Integer, Set<URI>>();
        urisBySchemeAndPartition.put(uri.getScheme(), map);
      }

      Map<Integer, PartitionData> partitions = entry.getValue();
      for (final Integer partitionId : partitions.keySet())
      {
        Set<URI> uriSet = map.get(partitionId);
        if (uriSet == null)
        {
          uriSet = new HashSet<URI>();
          map.put(partitionId, uriSet);
        }
        uriSet.add(uri);
      }
    }

    // make it unmodifiable
    for (Map.Entry<String, Map<Integer, Set<URI>>> entry : urisBySchemeAndPartition.entrySet())
    {
      final String scheme = entry.getKey();
      Map<Integer, Set<URI>> partitionUris = entry.getValue();
      for (Map.Entry<Integer, Set<URI>> partitionUriEntry: partitionUris.entrySet())
      {
        partitionUris.put(partitionUriEntry.getKey(), Collections.unmodifiableSet(partitionUriEntry.getValue()));
      }

      urisBySchemeAndPartition.put(scheme, Collections.unmodifiableMap(partitionUris));
    }

    _urisBySchemeAndPartition =
        Collections.unmodifiableMap(urisBySchemeAndPartition);
  }

  public String getClusterName()
  {
    return _clusterName;
  }

  public Set<URI> Uris()
  {
    return _partitionDesc.keySet();
  }

  public Map<Integer, PartitionData> getPartitionDataMap(URI uri)
  {
    return _partitionDesc.get(uri);
  }

  public Map<URI, Map<Integer, PartitionData>> getPartitionDesc()
  {
    return _partitionDesc;
  }

  public Set<URI> getUriBySchemeAndPartition(String scheme, int partitionId)
  {
    Map<Integer, Set<URI>> schemeUris = _urisBySchemeAndPartition.get(scheme);
    if (schemeUris == null)
    {
      return null;
    }
    else
    {
      return schemeUris.get(partitionId);
    }
  }

  @Override
  public String toString()
  {
    return "UriProperties [_clusterName=" + _clusterName + ", _urisBySchemeAndPartition="
        + _urisBySchemeAndPartition + "_partitions=" + _partitionDesc + "]";
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_clusterName == null) ? 0 : _clusterName.hashCode());
    result = prime * result + ((_partitionDesc == null) ? 0 : _partitionDesc.hashCode());
    result = prime * result + ((_urisBySchemeAndPartition == null) ? 0 : _urisBySchemeAndPartition.hashCode());
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
    UriProperties other = (UriProperties) obj;
    if (_clusterName == null)
    {
      if (other._clusterName != null)
        return false;
    }
    else if (!_clusterName.equals(other._clusterName))
      return false;

    if (_partitionDesc == null)
    {
      if (other._partitionDesc != null)
        return false;
    }
    else if (!_partitionDesc.equals(other._partitionDesc))
      return false;

    if (_urisBySchemeAndPartition == null)
    {
      if (other._urisBySchemeAndPartition != null)
        return false;
    }
    else if (!_urisBySchemeAndPartition.equals(other._urisBySchemeAndPartition))
      return false;

    return true;
  }

}

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

import com.linkedin.d2.discovery.stores.zk.ZooKeeperPropertyMerger;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UriPropertiesMerger implements ZooKeeperPropertyMerger<UriProperties>
{
  @Override
  public UriProperties merge(String listenTo, Collection<UriProperties> propertiesToMerge)
  {
    Map<URI, Map<Integer, PartitionData>> partitionData = new HashMap<URI, Map<Integer, PartitionData>>();
    Map<URI, Map<String, Object>> uriSpecificProperties = new HashMap<URI, Map<String, Object>>();

    String clusterName = listenTo;

    for (UriProperties property : propertiesToMerge)
    {
      for (Map.Entry<URI, Map<Integer, PartitionData>> entry : property.getPartitionDesc().entrySet())
      {
        partitionData.put(entry.getKey(), entry.getValue());
      }
      for (Map.Entry<URI, Map<String, Object>> entry: property.getUriSpecificProperties().entrySet())
      {
        uriSpecificProperties.put(entry.getKey(), entry.getValue());
      }
    }

    return new UriProperties(clusterName, partitionData, uriSpecificProperties);
  }

  @Override
  public String unmerge(String listenTo,
                        UriProperties toDelete,
                        Map<String, UriProperties> propertiesToMerge)
  {
    if (toDelete.Uris().size() > 1)
    {
      // TODO log this.. shouldn't have multiple uris defined in a single
      // ephemeral node
    }

    Set<URI> uris = toDelete.Uris();
    if (uris.size() == 0)
    {
      throw new IllegalArgumentException("The Uri to delete is not specified.");
    }

    URI uri = uris.iterator().next();

    for (Map.Entry<String, UriProperties> property : propertiesToMerge.entrySet())
    {
      if (property.getValue().Uris().contains(uri))
      {
        return property.getKey();
      }
    }

    return null;
  }
}

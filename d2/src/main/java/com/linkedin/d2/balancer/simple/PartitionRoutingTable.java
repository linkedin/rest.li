/*
   Copyright (c) 2026 LinkedIn Corp.

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

package com.linkedin.d2.balancer.simple;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;


/**
 * Immutable pre-computed routing table for a single service. Maps (scheme, partitionId) pairs
 * to their resolved TrackerClients and weighted URI maps, eliminating per-request O(n)
 * HashMap rebuilds in {@link SimpleLoadBalancer#getPotentialClients}.
 *
 * Instances are rebuilt on topology changes (URI/cluster/service property updates) and
 * atomically swapped into {@link SimpleLoadBalancerState#_routingSnapshots}.
 */
class PartitionRoutingTable
{
  private final Map<SchemePartitionKey, Map<URI, TrackerClient>> _partitionClients;
  private final Map<SchemePartitionKey, Map<URI, Double>> _weightedUris;

  private PartitionRoutingTable(Map<SchemePartitionKey, Map<URI, TrackerClient>> partitionClients,
                                Map<SchemePartitionKey, Map<URI, Double>> weightedUris)
  {
    _partitionClients = Collections.unmodifiableMap(partitionClients);
    _weightedUris = Collections.unmodifiableMap(weightedUris);
  }

  /**
   * Returns the pre-computed TrackerClient map for the given scheme and partition, or an empty map on miss.
   */
  Map<URI, TrackerClient> getPartitionClients(String scheme, int partitionId)
  {
    return _partitionClients.getOrDefault(new SchemePartitionKey(scheme, partitionId), Collections.emptyMap());
  }

  /**
   * Returns the pre-computed weighted URI map for the given scheme and partition, or an empty map on miss.
   */
  Map<URI, Double> getWeightedUris(String scheme, int partitionId)
  {
    return _weightedUris.getOrDefault(new SchemePartitionKey(scheme, partitionId), Collections.emptyMap());
  }

  /**
   * Builds a PartitionRoutingTable from the current topology state, filtering banned URIs
   * and resolving TrackerClients. Returns {@code null} if any required input is null.
   *
   * @param uriProperties     the URI-to-partition mapping for the cluster
   * @param serviceProperties the service configuration (used for ban checks)
   * @param clusterProperties the cluster configuration (used for ban checks)
   * @param trackerClients    the current TrackerClient map for the service
   * @return a new immutable PartitionRoutingTable, or null if inputs are incomplete
   */
  @Nullable
  static PartitionRoutingTable build(UriProperties uriProperties,
                                     ServiceProperties serviceProperties,
                                     ClusterProperties clusterProperties,
                                     Map<URI, TrackerClient> trackerClients)
  {
    if (uriProperties == null || serviceProperties == null || clusterProperties == null || trackerClients == null)
    {
      return null;
    }

    Map<SchemePartitionKey, Map<URI, TrackerClient>> partitionClients = new HashMap<>();
    Map<SchemePartitionKey, Map<URI, Double>> weightedUris = new HashMap<>();

    // Iterate over all URIs and their partition data to build the routing entries
    for (Map.Entry<URI, Map<Integer, PartitionData>> entry : uriProperties.getPartitionDesc().entrySet())
    {
      URI uri = entry.getKey();

      // Skip banned URIs
      if (serviceProperties.isBanned(uri) || clusterProperties.isBanned(uri))
      {
        continue;
      }

      TrackerClient client = trackerClients.get(uri);
      String scheme = uri.getScheme();

      for (Map.Entry<Integer, PartitionData> partitionEntry : entry.getValue().entrySet())
      {
        int partitionId = partitionEntry.getKey();
        SchemePartitionKey key = new SchemePartitionKey(scheme, partitionId);

        // Add to partition clients map if a TrackerClient exists
        if (client != null)
        {
          partitionClients.computeIfAbsent(key, k -> new HashMap<>()).put(uri, client);
        }

        // Always add to weighted URIs map (weight comes from PartitionData)
        double weight = partitionEntry.getValue().getWeight();
        weightedUris.computeIfAbsent(key, k -> new HashMap<>()).put(uri, weight);
      }
    }

    // Wrap all inner maps as unmodifiable
    partitionClients.replaceAll((k, v) -> Collections.unmodifiableMap(v));
    weightedUris.replaceAll((k, v) -> Collections.unmodifiableMap(v));

    return new PartitionRoutingTable(partitionClients, weightedUris);
  }

  /**
   * Composite key of URI scheme and partition ID for routing table lookups.
   */
  static final class SchemePartitionKey
  {
    private final String _scheme;
    private final int _partitionId;

    SchemePartitionKey(String scheme, int partitionId)
    {
      _scheme = scheme;
      _partitionId = partitionId;
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o)
      {
        return true;
      }
      if (!(o instanceof SchemePartitionKey))
      {
        return false;
      }
      SchemePartitionKey that = (SchemePartitionKey) o;
      return _partitionId == that._partitionId && Objects.equals(_scheme, that._scheme);
    }

    @Override
    public int hashCode()
    {
      return Objects.hash(_scheme, _partitionId);
    }
  }
}

/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.d2.balancer.subsetting;

import com.linkedin.d2.balancer.clients.TrackerClient;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.http.annotation.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * State of cluster subsetting
 */
public class SubsettingState
{
  private static final Logger LOG = LoggerFactory.getLogger(SubsettingState.class);
  private final Object _lock = new Object();
  private final SubsettingStrategyFactory _subsettingStrategyFactory;

  /**
   * Map from serviceName => Partition Id => subset map
   */
  @GuardedBy("_lock")
  private final Map<String, Map<Integer, Map<URI, TrackerClient>>> _weightedSubsetsCache;

  /**
   * Map from serviceName => Partition Id => all potential clients
   */
  @GuardedBy("_lock")
  private final Map<String, Map<Integer, Set<URI>>> _potentialClientsMap;

  @GuardedBy("_lock")
  private final Map<String, Integer> _minClusterSubsetSizeMap;

  @GuardedBy("_lock")
  private long _version;

  @GuardedBy("_lock")
  private long _peerClusterVersion;

  public SubsettingState(SubsettingStrategyFactory subsettingStrategyFactory)
  {
    _version = -1;
    _peerClusterVersion = -1;
    _subsettingStrategyFactory = subsettingStrategyFactory;
    _weightedSubsetsCache = new HashMap<>();
    _potentialClientsMap = new HashMap<>();
    _minClusterSubsetSizeMap = new HashMap<>();
  }

  public long getPeerClusterVersion(String serviceName,
      int minClusterSubsetSize,
      int partitionId)
  {
    SubsettingStrategy<URI> subsettingStrategy = _subsettingStrategyFactory.get(serviceName, minClusterSubsetSize, partitionId);

    if (subsettingStrategy == null)
    {
      return -1;
    }

    return subsettingStrategy.getPeerClusterVersion();
  }

  public Map<URI, TrackerClient> getClientsSubset(String serviceName,
      int minClusterSubsetSize,
      int partitionId,
      Map<URI, TrackerClient> potentialClients,
      long version)
  {
    SubsettingStrategy<URI> subsettingStrategy = _subsettingStrategyFactory.get(serviceName, minClusterSubsetSize, partitionId);

    if (subsettingStrategy == null)
    {
      return potentialClients;
    }

    long peerClusterVersion = subsettingStrategy.getPeerClusterVersion();

    synchronized (_lock)
    {
      if (isCacheValid(version, peerClusterVersion, minClusterSubsetSize, serviceName))
      {
        Map<Integer, Map<URI, TrackerClient>> serviceCache = _weightedSubsetsCache.get(serviceName);
        if (serviceCache != null && serviceCache.containsKey(partitionId))
        {
          return serviceCache.get(partitionId);
        }
      }

      LOG.debug("Calculate subset for version: " + _version + ", peerClusterVersion: " + _peerClusterVersion);

      Map<URI, Double> weightMap = potentialClients.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getPartitionWeight(partitionId)));
      Map<URI, Double> subsetMap = subsettingStrategy.getWeightedSubset(weightMap);

      if (subsetMap == null)
      {
        return potentialClients;
      }
      else
      {
        Set<URI> oldPotentialClients = Collections.emptySet();

        Map<Integer, Set<URI>> oldServicePotentialClients = _potentialClientsMap.get(serviceName);
        if (oldServicePotentialClients != null)
        {
          oldPotentialClients = oldServicePotentialClients.getOrDefault(partitionId, Collections.emptySet());
        }

        Map<URI, TrackerClient> subsetClients = new HashMap<>();
        for (Map.Entry<URI, Double> entry: subsetMap.entrySet())
        {
          URI uri = entry.getKey();
          TrackerClient client = potentialClients.get(uri);
          if (oldPotentialClients.contains(uri))
          {
            client.setDoNotSlowStart(partitionId, true);
          }
          client.setSubsetWeight(partitionId, entry.getValue());
          subsetClients.put(uri, client);
        }

        Map<Integer, Map<URI, TrackerClient>> serviceCache = _weightedSubsetsCache.computeIfAbsent(serviceName, k -> new HashMap<>());
        serviceCache.put(partitionId, subsetClients);

        Map<Integer, Set<URI>> servicePotentialClients = _potentialClientsMap.computeIfAbsent(serviceName, k -> new HashMap<>());
        servicePotentialClients.put(partitionId, potentialClients.keySet());

        _version = version;
        _peerClusterVersion = peerClusterVersion;
        _minClusterSubsetSizeMap.put(serviceName, minClusterSubsetSize);
        return subsetClients;
      }
    }
  }

  private boolean isCacheValid(long version, long peerClusterVersion, int minClusterSubsetSize, String serviceName)
  {
    return version == _version &&
        peerClusterVersion == _peerClusterVersion &&
        minClusterSubsetSize == _minClusterSubsetSizeMap.getOrDefault(serviceName, -1);
  }
}

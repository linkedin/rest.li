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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.annotation.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SubsettingState
{
  private static final Logger LOG = LoggerFactory.getLogger(SubsettingState.class);
  private final Object _lock = new Object();
  private final SubsettingStrategyFactory _subsettingStrategyFactory;

  @GuardedBy("_lock")
  private final Map<String, Map<Integer, Map<URI, TrackerClient>>> _weightedSubsetsCache;

  @GuardedBy("_lock")
  private long _version;

  @GuardedBy("_lock")
  private long _peerClusterVersion;

  @GuardedBy("_lock")
  private long _minClusterSubsetSize;

  public SubsettingState(SubsettingStrategyFactory subsettingStrategyFactory)
  {
    _version = -1;
    _peerClusterVersion = -1;
    _minClusterSubsetSize = -1;
    _subsettingStrategyFactory = subsettingStrategyFactory;
    _weightedSubsetsCache = new HashMap<>();
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
      if (isCacheValid(version, peerClusterVersion, minClusterSubsetSize))
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
        Map<URI, TrackerClient> subsetClients = new HashMap<>();
        for (Map.Entry<URI, Double> entry: subsetMap.entrySet())
        {
          URI uri = entry.getKey();
          TrackerClient client = potentialClients.get(uri);
          client.setSubsetWeight(partitionId, entry.getValue());
          subsetClients.put(uri, client);
        }

        Map<Integer, Map<URI, TrackerClient>> serviceCache = _weightedSubsetsCache.computeIfAbsent(serviceName, k -> new HashMap<>());
        serviceCache.put(partitionId, subsetClients);
        _version = version;
        _peerClusterVersion = peerClusterVersion;
        _minClusterSubsetSize = minClusterSubsetSize;
        return subsetClients;
      }
    }
  }

  private boolean isCacheValid(long version, long peerClusterVersion, int minClusterSubsetSize)
  {
    return version == _version &&
        peerClusterVersion == _peerClusterVersion &&
        minClusterSubsetSize == _minClusterSubsetSize;
  }
}

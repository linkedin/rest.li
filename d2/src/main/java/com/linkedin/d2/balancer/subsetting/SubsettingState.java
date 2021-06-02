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
import com.linkedin.d2.balancer.simple.SimpleLoadBalancerState;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * State of cluster subsetting
 */
public class SubsettingState
{
  private static final Logger LOG = LoggerFactory.getLogger(SubsettingState.class);
  private final ConcurrentMap<String, Object> _lockMap = new ConcurrentHashMap<>();

  private final SubsettingStrategyFactory _subsettingStrategyFactory;
  private final DeterministicSubsettingMetadataProvider _subsettingMetadataProvider;

  /**
   * Map from serviceName => SubsetCache
   */
  private final Map<String, SubsetCache> _subsetCache;

  public SubsettingState(SubsettingStrategyFactory subsettingStrategyFactory,
      DeterministicSubsettingMetadataProvider subsettingMetadataProvider)
  {
    _subsettingMetadataProvider = subsettingMetadataProvider;
    _subsettingStrategyFactory = subsettingStrategyFactory;
    _subsetCache = new HashMap<>();
  }

  public SubsetItem getClientsSubset(String serviceName,
      int minClusterSubsetSize,
      int partitionId,
      Map<URI, TrackerClient> potentialClients,
      long version,
      SimpleLoadBalancerState state)
  {
    SubsettingStrategy<URI> subsettingStrategy = _subsettingStrategyFactory.get(serviceName, minClusterSubsetSize, partitionId);

    if (subsettingStrategy == null)
    {
      return new SubsetItem(false, potentialClients);
    }

    DeterministicSubsettingMetadata metadata = _subsettingMetadataProvider.getSubsettingMetadata(state);

    if (metadata == null)
    {
      return new SubsetItem(false, potentialClients);
    }

    synchronized (_lockMap.computeIfAbsent(serviceName, name -> new Object()))
    {
      SubsetCache subsetCache = _subsetCache.get(serviceName);
      if (isCacheValid(version, metadata.getPeerClusterVersion(), minClusterSubsetSize, subsetCache))
      {
        if (subsetCache.getWeightedSubsets().containsKey(partitionId))
        {
          return new SubsetItem(false, subsetCache.getWeightedSubsets().get(partitionId));
        }
      }

      Map<URI, Double> weightMap = potentialClients.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getPartitionWeight(partitionId)));
      Map<URI, Double> subsetMap = subsettingStrategy.getWeightedSubset(weightMap, metadata);

      if (subsetMap == null)
      {
        return new SubsetItem(false, potentialClients);
      }
      else
      {
        Set<URI> oldPotentialClients = Collections.emptySet();

        if (subsetCache != null)
        {
          oldPotentialClients = subsetCache.getPotentialClients().getOrDefault(partitionId, Collections.emptySet());
        }

        Map<URI, TrackerClient> subsetClients = new HashMap<>();
        for (Map.Entry<URI, Double> entry: subsetMap.entrySet())
        {
          URI uri = entry.getKey();
          TrackerClient client = potentialClients.get(uri);
          if (oldPotentialClients.contains(uri))
          {
            client.setDoNotSlowStart(true);
          }
          client.setSubsetWeight(partitionId, entry.getValue());
          subsetClients.put(uri, client);
        }

        if (subsetCache != null)
        {
          subsetCache.setVersion(version);
          subsetCache.setPeerClusterVersion(metadata.getPeerClusterVersion());
          subsetCache.setMinClusterSubsetSize(minClusterSubsetSize);
          subsetCache.getPotentialClients().put(partitionId, potentialClients.keySet());
          subsetCache.getWeightedSubsets().put(partitionId, subsetClients);
        }
        else
        {
          Map<Integer, Set<URI>> servicePotentialClients = new HashMap<>();
          Map<Integer, Map<URI, TrackerClient>> serviceWeightedSubset = new HashMap<>();
          servicePotentialClients.put(partitionId, potentialClients.keySet());
          serviceWeightedSubset.put(partitionId, subsetClients);

          _subsetCache.put(serviceName, new SubsetCache(version, metadata.getPeerClusterVersion(),
              minClusterSubsetSize, servicePotentialClients, serviceWeightedSubset));
        }

        LOG.debug("Subset cache updated for service " + serviceName + ": " + subsetCache);

        return new SubsetItem(true, subsetClients);
      }
    }
  }

  private boolean isCacheValid(long version, long peerClusterVersion, int minClusterSubsetSize, SubsetCache subsetCache)
  {
    return subsetCache != null && version == subsetCache.getVersion() &&
        peerClusterVersion == subsetCache.getPeerClusterVersion() &&
        minClusterSubsetSize == subsetCache.getMinClusterSubsetSize();
  }

  private static class SubsetCache
  {
    private long _version;
    private long _peerClusterVersion;
    private int _minClusterSubsetSize;
    private final Map<Integer, Set<URI>> _potentialClients;
    private final Map<Integer, Map<URI, TrackerClient>> _weightedSubsets;

    SubsetCache(long version, long peerClusterVersion, int minClusterSubsetSize,
        Map<Integer, Set<URI>> potentialClients, Map<Integer, Map<URI, TrackerClient>> weightedSubsets)
    {
      _version = version;
      _peerClusterVersion = peerClusterVersion;
      _minClusterSubsetSize = minClusterSubsetSize;
      _potentialClients = potentialClients;
      _weightedSubsets = weightedSubsets;
    }

    public long getVersion()
    {
      return _version;
    }

    public long getPeerClusterVersion()
    {
      return _peerClusterVersion;
    }

    public int getMinClusterSubsetSize()
    {
      return _minClusterSubsetSize;
    }

    public Map<Integer, Set<URI>> getPotentialClients()
    {
      return _potentialClients;
    }

    public Map<Integer, Map<URI, TrackerClient>> getWeightedSubsets()
    {
      return _weightedSubsets;
    }

    public void setVersion(long version)
    {
      _version = version;
    }

    public void setPeerClusterVersion(long peerClusterVersion)
    {
      _peerClusterVersion = peerClusterVersion;
    }

    public void setMinClusterSubsetSize(int minClusterSubsetSize)
    {
      _minClusterSubsetSize = minClusterSubsetSize;
    }

    @Override
    public String toString() {
      return "SubsetCache{" + "_version=" + _version + ", _peerClusterVersion=" + _peerClusterVersion
          + ", _minClusterSubsetSize=" + _minClusterSubsetSize + ", _potentialClients=" + _potentialClients
          + ", _weightedSubsets=" + _weightedSubsets + '}';
    }
  }

  public static class SubsetItem
  {
    private final boolean _shouldForceUpdate;
    private final Map<URI, TrackerClient> _weightedSubset;

    public SubsetItem(boolean shouldForceUpdate, Map<URI, TrackerClient> weightedSubset)
    {
      _shouldForceUpdate = shouldForceUpdate;
      _weightedSubset = weightedSubset;
    }

    public boolean shouldForceUpdate()
    {
      return _shouldForceUpdate;
    }

    public Map<URI, TrackerClient> getWeightedSubset()
    {
      return _weightedSubset;
    }
  }
}

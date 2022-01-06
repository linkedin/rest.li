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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
      Map<URI, Double> possibleUris,
      long version,
      SimpleLoadBalancerState state)
  {
    SubsettingStrategy<URI> subsettingStrategy = _subsettingStrategyFactory.get(serviceName, minClusterSubsetSize, partitionId);

    if (subsettingStrategy == null)
    {
      return new SubsetItem(false, possibleUris, Collections.emptySet());
    }

    DeterministicSubsettingMetadata metadata = _subsettingMetadataProvider.getSubsettingMetadata(state);

    if (metadata == null)
    {
      return new SubsetItem(false, possibleUris, Collections.emptySet());
    }

    synchronized (_lockMap.computeIfAbsent(serviceName, name -> new Object()))
    {
      SubsetCache subsetCache = _subsetCache.get(serviceName);
      if (isCacheValid(version, metadata.getPeerClusterVersion(), minClusterSubsetSize, subsetCache))
      {
        if (subsetCache.getWeightedSubsets().containsKey(partitionId))
        {
          return new SubsetItem(false, subsetCache.getWeightedSubsets().get(partitionId), Collections.emptySet());
        }
      }

      Map<URI, Double> subsetMap = subsettingStrategy.getWeightedSubset(possibleUris, metadata);

      if (subsetMap == null)
      {
        return new SubsetItem(false, possibleUris, Collections.emptySet());
      }
      else
      {
        LOG.info("Force updating subset cache for service " + serviceName);
        Set<URI> doNotSlowStartUris = new HashSet<>();

        if (subsetCache != null)
        {
          Set<URI> oldPossibleUris = subsetCache.getPossibleUris().getOrDefault(partitionId, Collections.emptySet());
          for (URI uri : subsetMap.keySet())
          {
            if (oldPossibleUris.contains(uri))
            {
              doNotSlowStartUris.add(uri);
            }
          }
          subsetCache.setVersion(version);
          subsetCache.setPeerClusterVersion(metadata.getPeerClusterVersion());
          subsetCache.setMinClusterSubsetSize(minClusterSubsetSize);
          subsetCache.getPossibleUris().put(partitionId, possibleUris.keySet());
          subsetCache.getWeightedSubsets().put(partitionId, subsetMap);
        }
        else
        {
          Map<Integer, Set<URI>> servicePossibleUris = new HashMap<>();
          Map<Integer, Map<URI, Double>> serviceWeightedSubset = new HashMap<>();
          servicePossibleUris.put(partitionId, possibleUris.keySet());
          serviceWeightedSubset.put(partitionId, subsetMap);
          subsetCache = new SubsetCache(version, metadata.getPeerClusterVersion(),
              minClusterSubsetSize, servicePossibleUris, serviceWeightedSubset);

          _subsetCache.put(serviceName, subsetCache);
        }

        LOG.debug("Subset cache updated for service " + serviceName + ": " + subsetCache);

        return new SubsetItem(true, subsetMap, doNotSlowStartUris);
      }
    }
  }

  private boolean isCacheValid(long version, long peerClusterVersion, int minClusterSubsetSize, SubsetCache subsetCache)
  {
    return subsetCache != null && version == subsetCache.getVersion() &&
        peerClusterVersion == subsetCache.getPeerClusterVersion() &&
        minClusterSubsetSize == subsetCache.getMinClusterSubsetSize();
  }

  public void invalidateCache(String serviceName)
  {
    synchronized (_lockMap.computeIfAbsent(serviceName, name -> new Object()))
    {
      LOG.info("Invalidating subset cache for service " + serviceName);
      _subsetCache.remove(serviceName);
    }
  }

  private static class SubsetCache
  {
    private long _version;
    private long _peerClusterVersion;
    private int _minClusterSubsetSize;
    private final Map<Integer, Set<URI>> _possibleUris;
    private final Map<Integer, Map<URI, Double>> _weightedSubsets;

    SubsetCache(long version, long peerClusterVersion, int minClusterSubsetSize,
        Map<Integer, Set<URI>> possibleUris, Map<Integer, Map<URI, Double>> weightedSubsets)
    {
      _version = version;
      _peerClusterVersion = peerClusterVersion;
      _minClusterSubsetSize = minClusterSubsetSize;
      _possibleUris = possibleUris;
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

    public Map<Integer, Set<URI>> getPossibleUris()
    {
      return _possibleUris;
    }

    public Map<Integer, Map<URI, Double>> getWeightedSubsets()
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
          + ", _minClusterSubsetSize=" + _minClusterSubsetSize + ", _possibleUris=" + _possibleUris
          + ", _weightedSubsets=" + _weightedSubsets + '}';
    }
  }

  /**
   * Encapsulates the result of subsetting
   */
  public static class SubsetItem
  {
    private final boolean _shouldForceUpdate;
    private final Map<URI, Double> _weightedUriSubset;
    private final Set<URI> _doNotSlowStartUris;
    private final Map<URI, TrackerClient> _weightedClientSubset;

    public SubsetItem(boolean shouldForceUpdate, Map<URI, Double> weightedUriSubset, Set<URI> doNotSlowStartUris)
    {
      _shouldForceUpdate = shouldForceUpdate;
      _weightedUriSubset = weightedUriSubset;
      _doNotSlowStartUris = doNotSlowStartUris;
      _weightedClientSubset = null;
    }

    public SubsetItem(SubsetItem subsetItem, Map<URI, TrackerClient> weightedClientSubset)
    {
      _shouldForceUpdate = subsetItem.shouldForceUpdate();
      _weightedUriSubset = subsetItem.getWeightedUriSubset();
      _doNotSlowStartUris = subsetItem.getDoNotSlowStartUris();
      _weightedClientSubset = weightedClientSubset;
    }

    public boolean shouldForceUpdate()
    {
      return _shouldForceUpdate;
    }

    public Map<URI, Double> getWeightedUriSubset()
    {
      return _weightedUriSubset;
    }

    public Set<URI> getDoNotSlowStartUris()
    {
      return _doNotSlowStartUris;
    }

    public Map<URI, TrackerClient> getWeightedSubset()
    {
      return _weightedClientSubset;
    }
  }
}

/*
   Copyright (c) 2014 LinkedIn Corp.

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


package com.linkedin.d2.balancer.util;


import com.linkedin.d2.balancer.util.HostToKeyResult.ErrorType;
import com.linkedin.d2.balancer.util.HostToKeyResult.UnmappedKey;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Contains the result of KeyMapper.mapKeysV3 and KepMapper.getAllPartitionsMultipleHosts.
 * This data object can compute the mapping of hosts to keys for a particular "iteration"
 *
 * It's possible that the same host appears twice depending on how the cluster is being partitioned.
 *
 * Example use case:
 *
 * We have two partitions.
 * key 1 is mapped to partition 0. Host1, host2 are returned for this partition.
 * key 2 is mapped to partition 1. Host2 is returned for this partition.
 *
 * Calling getFirstResult() will return a mapping of [key1] - [Host1], [key2] - [Host2]
 *
 * Calling getResult(1) (which means the second iteration) will return a mapping of [key1] - [Host2], key2 unmapped
 *
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 * @author Xialin Zhu
 */
public class HostToKeyMapper<K> implements HostSet
{
  private final Map<Integer, KeysAndHosts<K>> _partitionInfoMap;
  private final int _limitHostPerPartition;
  private final int _partitionCount;
  private final Set<UnmappedKey<K>> _unmappedKeys;
  private final Map<Integer, Integer> _partitionsWithoutEnoughHosts;

  public HostToKeyMapper(Collection<K> unmappedKeys, Map<Integer, KeysAndHosts<K>> partitionInfoMap,
                         int limitHostPerPartition, int partitionCount, Map<Integer, Integer> partitionsWithoutEnoughHosts)
  {
    if (limitHostPerPartition <= 0)
    {
      throw new IllegalArgumentException("MaxNumHost cannot be less than 1");
    }
    final Set<UnmappedKey<K>> unmappedKeysSet = new HashSet<UnmappedKey<K>>();
    _partitionInfoMap = Collections.unmodifiableMap(partitionInfoMap);
    _limitHostPerPartition = limitHostPerPartition;
    _partitionsWithoutEnoughHosts = partitionsWithoutEnoughHosts;
    _partitionCount = partitionCount;
    for (K key : unmappedKeys)
    {
      unmappedKeysSet.add(new UnmappedKey<K>(key, ErrorType.FAIL_TO_FIND_PARTITION));
    }
    _unmappedKeys = Collections.unmodifiableSet(unmappedKeysSet);
  }

  /**
   * This is a more general form of getFirstResult. This method will return mapping of keys to host (of the the
   * iteration given as argument). The assumption here is we're using the same set of keys.
   *
   * This is used for someone who wants to make parallel scatter gather. The set of keys being used
   * is always the same regardless of the iteration
   *
   * @param whichIteration this is 0 based iteration. So first iteration is 0 and 2nd iteration is 1, etc.
   * @return the MapKeyResult for that iteration. If we can't find MapKeyResult for that iteration, we'll return null
   */
  public HostToKeyResult<K> getResult(int whichIteration)
  {
    return doGetResult(whichIteration, _partitionInfoMap, new HashSet<UnmappedKey<K>>(_unmappedKeys));
  }

  /**
   * This is used usually in conjunction with getFirstResult() in a sequential scatter gather. Typical use case is as
   * follow: the user calls getFirstResult() then send scatter gather request. After the user receives back the result,
   * some keys failed to be returned. So the user issues a second scatter gather request but with reduced number
   * of keys. So that's why we accept the keys param in addition to the iteration number.
   *
   * @param whichIteration this is 0 based. So first attempt is 0 and 2nd attempt is 1
   * @param keys
   * @return the MapKeyResult for that attempt. If we can't find MapKeyResult for that attempt, we'll return null
   */
  public HostToKeyResult<K> getResult(int whichIteration, Collection<K> keys)
  {
    Map<Integer, KeysAndHosts<K>> newPartitionInfoMap = new HashMap<Integer, KeysAndHosts<K>>();
    for (Map.Entry<Integer, KeysAndHosts<K>> entry : _partitionInfoMap.entrySet())
    {
      Collection<K> keysForPartition = entry.getValue().getKeys();
      List<K> newKeyList = new ArrayList<K>();

      for (Iterator<K> iterator = keysForPartition.iterator(); iterator.hasNext();)
      {
        K key = iterator.next();
        if (keys.contains(key))
        {
          newKeyList.add(key);
        }
      }

      newPartitionInfoMap.put(entry.getKey(), new KeysAndHosts<K>(newKeyList, entry.getValue().getHosts()));
    }

    return doGetResult(whichIteration, newPartitionInfoMap, new HashSet<UnmappedKey<K>>(_unmappedKeys));
  }

  private HostToKeyResult<K> doGetResult(int whichIteration, Map<Integer, KeysAndHosts<K>> partitionInfoMap, Collection<UnmappedKey<K>> unmappedKeys)
  {
    if (whichIteration >= _limitHostPerPartition)
    {
      return null;
    }

    Map<URI, Collection<K>> hostToKeysMerge = new HashMap<URI, Collection<K>>();
    for (Map.Entry<Integer, KeysAndHosts<K>> entry : partitionInfoMap.entrySet())
    {
      Collection<K> keysForThisPartition = entry.getValue().getKeys();
      if (keysForThisPartition == null || keysForThisPartition.size() == 0)
      {
        // this means there is no need to worry about this partition. The user didn't pass any keys
        // that concern this partition
        continue;
      }
      else
      {
        // this means there's some keys that belong to this partition. Find the host and try to merge if there a
        // duplicate host that we've seen in previous iteration
        List<URI> hosts = entry.getValue().getHosts();
        mergeKeys(hosts, keysForThisPartition, unmappedKeys, whichIteration, hostToKeysMerge);
      }
    }

    return new HostToKeyResult<K>(hostToKeysMerge, unmappedKeys);
  }

  // utility method to merge keys that maps to the same host. This method does the merging in hostToKeysMerge that
  // gets passed in
  private void mergeKeys(List<URI> hosts, Collection<K> keys,
                         Collection<UnmappedKey<K>> unmappedKeys,
                         int whichIteration,
                         Map<URI, Collection<K>> hostToKeysMerge)
  {
    if (whichIteration >= hosts.size())
    {
      for (K key : keys)
      {
        unmappedKeys.add(new UnmappedKey<K>(key, ErrorType.NO_HOST_AVAILABLE_IN_PARTITION));
      }
    }
    else
    {
      URI currentHost = hosts.get(whichIteration);
      Collection<K> keysForCurrentHost = hostToKeysMerge.get(currentHost);
      if (keysForCurrentHost == null)
      {
        keysForCurrentHost = new HashSet<K>();
        hostToKeysMerge.put(currentHost, keysForCurrentHost);
      }
      keysForCurrentHost.addAll(keys);
    }
  }

  public List<URI> getAllHosts()
  {
    Set<URI> hosts = new HashSet<URI>();
    for (Map.Entry<Integer, KeysAndHosts<K>> entry : _partitionInfoMap.entrySet())
    {
      hosts.addAll(entry.getValue().getHosts());
    }
    return new ArrayList<URI>(hosts);
  }

  public List<URI> getHosts(int partitionId)
  {
    if (_partitionInfoMap.containsKey(partitionId))
    {
      return Collections.unmodifiableList(_partitionInfoMap.get(partitionId).getHosts());
    }
    else
    {
      throw new IllegalArgumentException("PartitionId " + partitionId + " is not found");
    }
  }

  public int getPartitionCount()
  {
    return _partitionCount;
  }

  public Map<Integer, Integer> getPartitionsWithoutEnoughHosts()
  {
    return _partitionsWithoutEnoughHosts;
  }

  public Map<Integer, KeysAndHosts<K>> getPartitionInfoMap()
  {
    return _partitionInfoMap;
  }

  public int getLimitHostPerPartition()
  {
    return _limitHostPerPartition;
  }

  public Set<UnmappedKey<K>> getUnmappedKeys()
  {
    return _unmappedKeys;
  }
}

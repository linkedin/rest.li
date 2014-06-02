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
import com.linkedin.d2.balancer.util.partitions.PartitionAccessException;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


/**
 * Contains the result of KeyMapper.mapKeysV3. This data object can compute the mapping of hosts to keys
 * for a particular "iteration"
 *
 * Example use case:
 *
 * We have 2 keys 1 and 2.
 * Let's say for key 1, there are host 1,2,3.
 * and for key 2 there is only host 4.
 * Let's say the user wants 2 hosts per key.
 *
 * Then this object will contain the following mapping
 * {
 *   list:
 *   - [key 1], [host1, host2]
 *   - [key 2], [host4]
 * }
 *
 * when the user calls getFirstResult(), this indicates the user wants to know the mapping of the first "iteration".
 * So we return:
 * {
 *   - [key1], [host1]
 *   - [key2], [host4]
 * }
 *
 * when the user calls getResult(1), this indicates the user wants to know the mapping of the second "iteration".
 * So we return the following
 * {
 *   - [key1] , [host2]
 *   - [key2] is unmapped
 * }
 *
 * ------------------------
 *
 * It's possible that the same host appears twice depending on how the cluster is being partitioned.
 * For example
 *
 * We have keys 1,2,3.
 * keys 1 is served by host 1,2
 * keys 2 is served by host 2,3
 * keys 3 is served by host 1,3
 * we want to get 2 hosts per key because we want to try a backup retry
 *
 * We provide a method in HostToKeyMapper to "combine" the result depending on which "iteration" are you on
 *
 * So for the first try we will return
 * {
 *   list:
 *   - [key1, key3], [host 1]
 *   - [key2]. [host2]
 * }
 *
 * for the 2nd try we will return
 *
 * {
 *   list:
 *   - [key1], [host2]
 *   - [key2, key3]. [host3]
 * }
 *
 * Note that host2 appears twice and it appears on different key depending on which try you're on.
 *
 *
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 */
public class HostToKeyMapper<K>
{
  private final MapKeyHostPartitionResult<K> _mapResult;
  private final PartitionAccessor _partitionAccessor;
  private final int _maxNumOfHosts;
  private final Collection<K> _originalKeys;

  public HostToKeyMapper(MapKeyHostPartitionResult<K> mapResult, PartitionAccessor partitionAccessor,
                         int maxNumOfHosts, Collection<K> keys)
  {
    if (maxNumOfHosts <= 0)
    {
      throw new IllegalArgumentException("MaxNumHost cannot be less than 1");
    }
    _partitionAccessor = partitionAccessor;
    _mapResult = mapResult;
    _maxNumOfHosts = maxNumOfHosts;
    _originalKeys = Collections.unmodifiableCollection(keys);
  }

  /**
   * returns the key-server mapping for the first attempt to send request. This assumes the keys are the original
   * set of keys.
   *
   * Typical use case:
   * The user calls mapKeys() for a d2service passing a set of keys and ask for a number of host.
   * The user then get HostToKeyMapper and call getFirstResult() The returned object maps
   * the original set of keys to the host that the user should send the first scatter gather. If some of the calls
   * in the first scatter gather fails the user needs to retry using either getResult(int) or getResult(int, Collection)
   *
   * @return
   */
  public HostToKeyResult<URI, K> getFirstResult()
  {
    return getResult(0);
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
  public HostToKeyResult<URI, K> getResult(int whichIteration)
  {
    return getResult(whichIteration, _originalKeys);
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
  public HostToKeyResult<URI, K> getResult(int whichIteration, Collection<K> keys)
  {
    if (whichIteration >= _maxNumOfHosts)
    {
      return null;
    }
    Collection<UnmappedKey<K>> unmappedKeys = new HashSet<UnmappedKey<K>>();
    //build a partitionId -> keys map. So if there is no key for that partition we don't have to process.
    Map<Integer, Collection<K>> partitionIdToKeyMap = new HashMap<Integer, Collection<K>>();
    for (K key : keys)
    {
      try
      {
        Integer partitionId = _partitionAccessor.getPartitionId(key.toString());
        Collection<K> k = partitionIdToKeyMap.get(partitionId);
        if (k == null)
        {
          k = new HashSet<K>();
          partitionIdToKeyMap.put(partitionId, k);
        }
        k.add(key);
      }
      catch (PartitionAccessException e)
      {
        unmappedKeys.add(new UnmappedKey<K>(key, ErrorType.FAIL_TO_FIND_PARTITION));
      }
    }

    Map<URI, Collection<K>> hostToKeysMerge = new HashMap<URI, Collection<K>>();
    for (Map.Entry<Integer, KeysAndHosts<K>> entry : _mapResult.getPartitionInfoMap().entrySet())
    {
      Integer partitionId = entry.getKey();
      Collection<K> keysForThisPartition = partitionIdToKeyMap.get(partitionId);
      if (keysForThisPartition == null)
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
    List<KeysAndHosts<K>> mapResult = new ArrayList<KeysAndHosts<K>>();
    for (Map.Entry<URI, Collection<K>> entry: hostToKeysMerge.entrySet())
    {
      mapResult.add(new KeysAndHosts<K>(entry.getValue(), Arrays.asList(entry.getKey())));
    }
    return new HostToKeyResult<URI, K>(mapResult, unmappedKeys);
  }
}

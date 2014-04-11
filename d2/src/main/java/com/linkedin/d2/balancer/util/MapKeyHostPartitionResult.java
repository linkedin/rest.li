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


import java.util.Collection;
import java.util.Collections;
import java.util.Map;


/**
 * Data transfer object used by com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider
 * to encapsulate the information about a partition
 *
 * @param <K> Keys
 * @author Oby Sumampouw
 */
public class MapKeyHostPartitionResult<K>
{
  private final Collection<K> _unmappedKeys;
  private final Map<Integer, KeysAndHosts<K>> _partitionInfoMap;
  private final Collection<Integer> _partitionWithoutEnoughHost;
  private final int _maxNumOfKeysPerHost;

  public static final int UNLIMITED_CAPACITY = Integer.MAX_VALUE;

  public MapKeyHostPartitionResult(Collection<K> unmappedKeys, Map<Integer, KeysAndHosts<K>> partitionInfoMap,
                                   Collection<Integer> partitionWithoutEnoughHost,
                                   int maxNumOfKeysPerHost)
  {
    _unmappedKeys = Collections.unmodifiableCollection(unmappedKeys);
    _partitionInfoMap = Collections.unmodifiableMap(partitionInfoMap);
    _partitionWithoutEnoughHost = Collections.unmodifiableCollection(partitionWithoutEnoughHost);
    if (maxNumOfKeysPerHost <= 0)
    {
      throw new IllegalArgumentException("Cannot set maxNumOfKeysPerHost that is less than 1");
    }
    _maxNumOfKeysPerHost = maxNumOfKeysPerHost;
  }

  public MapKeyHostPartitionResult(Collection<K> unmappedKeys, Map<Integer, KeysAndHosts<K>> partitionInfoMap,
                                   Collection<Integer> partitionWithoutEnoughHost)
  {
    _unmappedKeys = Collections.unmodifiableCollection(unmappedKeys);
    _partitionInfoMap = Collections.unmodifiableMap(partitionInfoMap);
    _partitionWithoutEnoughHost = Collections.unmodifiableCollection(partitionWithoutEnoughHost);
    _maxNumOfKeysPerHost = UNLIMITED_CAPACITY;
  }

  public Collection<K> getUnmappedKeys()
  {
    return _unmappedKeys;
  }

  public Map<Integer, KeysAndHosts<K>> getPartitionInfoMap()
  {
    return _partitionInfoMap;
  }

  public Collection<Integer> getPartitionWithoutEnoughHost()
  {
    return _partitionWithoutEnoughHost;
  }

  public int getMaxNumOfKeysPerHost()
  {
    return _maxNumOfKeysPerHost;
  }
}

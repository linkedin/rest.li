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

/**
 * $Id: $
 */

package com.linkedin.d2.balancer.util.hashing;


import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.HostToKeyMapper;
import com.linkedin.d2.balancer.util.MapKeyResult;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Josh Walker
 * @author Xialin Zhu
 * @version $Revision: $
 */

public class ConsistentHashKeyMapper implements KeyMapper
{
  private final HashFunction<String[]> _hashFunction;
  private final HashRingProvider _ringProvider;
  private final PartitionInfoProvider _partitionInfoProvider;
  private final Random _random;
  private final Logger _log = LoggerFactory.getLogger(ConsistentHashKeyMapper.class);

  public ConsistentHashKeyMapper(HashRingProvider ringProvider, PartitionInfoProvider partitionInfoProvider)
  {
    _hashFunction = new MD5Hash();
    if (ringProvider == null)
    {
      throw new IllegalArgumentException("ringProvider must not be null");
    }
    if (partitionInfoProvider == null)
    {
      throw new UnsupportedOperationException("partitionInfoProvider must not be null");
    }
    _ringProvider = ringProvider;
    _partitionInfoProvider = partitionInfoProvider;
    _random = new Random();
  }

  public ConsistentHashKeyMapper(HashRingProvider ringProvider)
  {
    this(ringProvider, null);
  }

  @Override
  public <K> HostToKeyMapper<K> mapKeysV3(URI serviceUri, Collection<K> keys, int limitNumHostsPerPartition)
          throws ServiceUnavailableException
  {
    return getHostToKeyMapper(serviceUri, keys, limitNumHostsPerPartition, null);
  }

  @Override
  public <K, S> HostToKeyMapper<K> mapKeysV3(URI serviceUri, Collection<K> keys, int limitNumHostsPerPartition, S stickyKey)
          throws ServiceUnavailableException
  {
    return getHostToKeyMapper(serviceUri, keys, limitNumHostsPerPartition, stickyKey);
  }

  @Override
  public HostToKeyMapper<Integer> getAllPartitionsMultipleHosts(URI serviceUri, int numHostPerPartition)
      throws ServiceUnavailableException
  {
    return getHostToKeyMapper(serviceUri, null, numHostPerPartition, null);
  }

  @Override
  public <S> HostToKeyMapper<Integer> getAllPartitionsMultipleHosts(URI serviceUri, int limitHostPerPartition, final S stickyKey)
      throws ServiceUnavailableException
  {
    return getHostToKeyMapper(serviceUri, null, limitHostPerPartition, stickyKey);
  }

  public <K, S> HostToKeyMapper<K> getHostToKeyMapper(URI serviceUri, Collection<K> keys, int limitHostPerPartition, final S stickyKey)
          throws ServiceUnavailableException
  {
    final int hash = (stickyKey == null ? _random.nextInt() : stickyKey.hashCode());
    return _partitionInfoProvider.getPartitionInformation(serviceUri, keys, limitHostPerPartition, hash);
  }

  @Override
  public <K> MapKeyResult<URI, K> mapKeysV2(URI serviceUri, Iterable<K> keys)
          throws ServiceUnavailableException
  {
    // distribute keys to rings for partitions
    // Note that we assume the keys are partitioning keys
    MapKeyResult<Ring<URI>, K> keyToPartitionResult = _ringProvider.getRings(serviceUri, keys);
    Map<Ring<URI>, Collection<K>> ringToKeys = keyToPartitionResult.getMapResult();

    Map<URI, Collection<K>> result = new HashMap<URI, Collection<K>>();
    Collection<MapKeyResult.UnmappedKey<K>> unmappedKeys = new ArrayList<MapKeyResult.UnmappedKey<K>>();

    // first collect unmappedkeys in ditributing keys to partitions
    unmappedKeys.addAll(keyToPartitionResult.getUnmappedKeys());

    // for each partition, distribute keys to different server uris
    for (Map.Entry<Ring<URI>, Collection<K>> entry : ringToKeys.entrySet())
    {
      MapKeyResult<URI, K> keyToHostResult = doMapKeys(entry.getKey(), entry.getValue());

      // collect map key to host result
      Map<URI, Collection<K>> hostToKeys = keyToHostResult.getMapResult();
      for (Map.Entry<URI, Collection<K>> hostEntry : hostToKeys.entrySet())
      {
        URI uri = hostEntry.getKey();
        Collection<K> collection = result.get(uri);
        if (collection == null)
        {
          collection = new ArrayList<K>();
          result.put(uri, collection);
        }
        collection.addAll(hostEntry.getValue());
      }

      // collect unmapped keys
      unmappedKeys.addAll(keyToHostResult.getUnmappedKeys());
    }

    return new MapKeyResult<URI, K>(result, unmappedKeys);
  }

  private <K> MapKeyResult<URI, K> doMapKeys(Ring<URI> ring, Iterable<K> keys)
      throws ServiceUnavailableException
  {
    String[] keyTokens = new String[1];
    List<MapKeyResult.UnmappedKey<K>> unmappedKeys = new ArrayList<MapKeyResult.UnmappedKey<K>>();
    Map<URI, Collection<K>> result = new HashMap<URI, Collection<K>>();
    for (K key : keys)
    {
      keyTokens[0] = key.toString();
      int hashCode = _hashFunction.hash(keyTokens);

      URI uri = ring.get(hashCode);
      if (uri == null)
      {
        unmappedKeys.add(new MapKeyResult.UnmappedKey<K>(key, MapKeyResult.ErrorType.NO_HOST_AVAILABLE_IN_PARTITION));
        continue;
      }

      Collection<K> collection = result.get(uri);
      if (collection == null)
      {
        collection = new ArrayList<K>();
        result.put(uri, collection);
      }
      collection.add(key);
    }
    return new MapKeyResult<URI, K>(result, unmappedKeys);
  }
}

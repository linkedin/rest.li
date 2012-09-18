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
import com.linkedin.d2.balancer.util.AllPartitionsResult;
import com.linkedin.d2.balancer.util.MapKeyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class ConsistentHashKeyMapper
        implements KeyMapper
{
  private final HashFunction<String[]> _hashFunction;
  private final HashRingProvider _ringProvider;
  private final Logger _log = LoggerFactory.getLogger(ConsistentHashKeyMapper.class);

  public ConsistentHashKeyMapper(HashRingProvider ringProvider)
  {
    _hashFunction = new MD5Hash();
    if (ringProvider == null)
    {
      throw new IllegalArgumentException("ringProvider must not be null");
    }
    _ringProvider = ringProvider;
  }

  @Override
  public AllPartitionsResult<URI> getAllPartitions(URI serviceUri) throws ServiceUnavailableException
  {
    Map<Integer, Ring<URI>> ringMap = _ringProvider.getRings(serviceUri);
    String[] uriStrings = {serviceUri.toString()};
    int hashCode = _hashFunction.hash(uriStrings);

    final Set<URI> hostUris = new HashSet<URI>();
    final Set<Integer> missingPartitions = new HashSet<Integer>();

    // find one host uri from each partition ring for the hash code of the service uri
    for (Map.Entry<Integer, Ring<URI>> entry : ringMap.entrySet())
    {
      Ring<URI> ring = entry.getValue();
      URI uri = ring.get(hashCode);
      if (uri != null)
      {
        hostUris.add(uri);
      }
      else
      {
        missingPartitions.add(entry.getKey());
      }
    }

    if (hostUris.isEmpty())
    {
      throw new ServiceUnavailableException(serviceUri.getAuthority(), "No valid target host uri found for: " + serviceUri.toString());
    }

    return new AllPartitionsResult<URI>(hostUris, ringMap.size(), missingPartitions);
  }

  @Deprecated
  @Override
  public <K> Map<URI, Set<K>> mapKeys(URI serviceUri, Set<K> keys)
          throws ServiceUnavailableException
  {
    MapKeyResult<URI, K> mapKeyResult = mapKeysV2(serviceUri, keys);
    Map<URI, Collection<K>> collectionMap = mapKeyResult.getMapResult();
    Map<URI, Set<K>> result = new HashMap<URI, Set<K>>();
    for (Map.Entry<URI, Collection<K>> entry : collectionMap.entrySet())
    {
      result.put(entry.getKey(), new HashSet<K>(entry.getValue()));
    }

    return result;
  }

  @Deprecated
  @Override
  public <K> Map<URI, Collection<K>> mapKeys(URI serviceUri, Iterable<K> keys)
    throws ServiceUnavailableException
  {
    MapKeyResult<URI, K> result = mapKeysV2(serviceUri, keys);
    return result.getMapResult();
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

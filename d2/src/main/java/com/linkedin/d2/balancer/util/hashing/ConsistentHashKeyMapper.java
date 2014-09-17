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
import com.linkedin.d2.balancer.util.AllPartitionsMultipleHostsResult;
import com.linkedin.d2.balancer.util.AllPartitionsResult;
import com.linkedin.d2.balancer.util.HostToKeyMapper;
import com.linkedin.d2.balancer.util.MapKeyHostPartitionResult;
import com.linkedin.d2.balancer.util.MapKeyResult;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import java.util.Random;
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

public class ConsistentHashKeyMapper implements KeyMapper
{
  private final HashFunction<String[]> _hashFunction;
  private final HashRingProvider _ringProvider;
  private final PartitionInfoProvider _partitionInfoProvider;
  private final Logger _log = LoggerFactory.getLogger(ConsistentHashKeyMapper.class);

  public ConsistentHashKeyMapper(HashRingProvider ringProvider, PartitionInfoProvider partitionInfoProvider)
  {
    _hashFunction = new MD5Hash();
    if (ringProvider == null)
    {
      throw new IllegalArgumentException("ringProvider must not be null");
    }
    _ringProvider = ringProvider;
    _partitionInfoProvider = partitionInfoProvider;
  }

  public ConsistentHashKeyMapper(HashRingProvider ringProvider)
  {
    this(ringProvider, null);
  }


  @Override
  public AllPartitionsResult<URI> getAllPartitions(URI serviceUri) throws ServiceUnavailableException
  {
    String[] uriStrings = {serviceUri.toString()};
    int hashCode = _hashFunction.hash(uriStrings);

    return getAllPartitions(serviceUri, hashCode);
  }

  @Override
  public AllPartitionsResult<URI> getAllPartitions(URI serviceUri, int hashCode)
      throws ServiceUnavailableException
  {
    Map<Integer, Ring<URI>> ringMap = _ringProvider.getRings(serviceUri);

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

  @Override
  public AllPartitionsMultipleHostsResult<URI> getAllPartitionsMultipleHosts(URI serviceUri, int numHostPerPartition)
      throws ServiceUnavailableException
  {
    if (_partitionInfoProvider == null)
    {
      throw new UnsupportedOperationException("This method is unavailable if partitionInfoProvider is not provided.");
    }
    final Random random = new Random();
    return _partitionInfoProvider.getAllPartitionMultipleHosts(serviceUri, numHostPerPartition,
        new PartitionInfoProvider.HashProvider() {
          @Override
          public int nextHash() {
            return random.nextInt();
          }
        });
  }

  @Override
  public <S> AllPartitionsMultipleHostsResult<URI> getAllPartitionsMultipleHosts(URI serviceUri,
                                                                                    int limitHostPerPartition,
                                                                                    final S stickyKey)
      throws ServiceUnavailableException
  {
    if (_partitionInfoProvider == null)
    {
      throw new UnsupportedOperationException("This method is unavailable if partitionInfoProvider is not provided.");
    }
    return _partitionInfoProvider.getAllPartitionMultipleHosts(serviceUri, limitHostPerPartition,
        new StickyKeyHashProvider<S>(stickyKey));
  }

  /**
  * Given a d2 service URI (for example : d2://articles), a collection of partition keys and a desired number of
  * hosts per partition, this returns all the partition that contains the given key for the given d2 service.
  * We also return information about the partition such as the hosts and the keys that belong to that partition.
  *
  * The returned hosts are picked semi randomly weighted based on the health of the hosts.
  * For example if partition 1 contains 10 hosts and you want us to return 3 hosts then we will
  * pick 3 hosts randomly from partition 1. But healthy hosts will have higher chance of showing up compared to
  * unhealthy hosts.
  *
  * If there are not enough host in the partition to fulfill the requested number of hosts,
  * we will try to return as many as we can. For example if partition 1 contains
  * 3 hosts but you want 10 hosts, we will still return 3 hosts. MapKeyHostPartition has a field that tells you
  * the partitions that doesn't have enough hosts to meet the requirement. This will make it easier for you
  * to know which partition can't fulfill the number of host requirements
  *
  * MapKeyHostPartitionResult has a map of partitionId to the keys and hosts that belong to that partition.
  * It also contains the information about what is the maximum key a host can process. The default is Integer.MAX_INT
  * meaning there's no limit on the amount of keys you can send to. But normally a service puts a cap on the
  * number of keys a host can serve.
  *
  * An example use case:
  * We have a d2 service called "articles". Articles is split into 3 partitions.
  * Partition 0 is hosted in foo1.com and foo2.com
  * Partition 1 is hosted in bar1.com, bar2.com and bar3.com.
  * Partition 2 has no hosts
  * Let's say keys 1,2,3 are hosted in partition 0 and keys 4, 5, 6 are hosted in partition 1,
  * and keys 7,8,9 are hosted in partition 2, and lastly keys 100,101,102 are hosted in partition 3.
  *
  * So given serviceUri = d2://articles, keys = [1,2,3,4,9,10], limitNumHostsPerPartition = 2
  *
  * returns:
  * {
  *   map:
  *      0 -> hostUris = [foo1.com, foo2.com], keys = [1,2,3]
  *      1 -> hostUris = [bar3.com, bar2.com], keys = [4]
  *      2 -> hostUris = [], keys = [9]
  *
  *   unmappedKeys = [10]
  *   partitionWithoutEnoughHost = [2]
  * }
  *
  * Note: we don't return partition 3 because there is no key that mapped to partition 3.
  * Keys that can't be mapped to any partition will be put inside unmappedKey.
  * This API can be used for scatter gather with retrying. In the example above,
  * you get a list of partitionInfo. Then you can send a batch request to foo1.com with key 1,2,3 and bar1.com with
  * key 4. If foo1.com is taking too long, you can cancel the first request and retry sending key 1,3
  * to foo2.com. The hosts returned from this method are ordered according to our load balancing metrics.
  *
  * @param serviceURI
  * @param keys
  * @param limitNumHostsPerPartition
  * @return
  * @throws ServiceUnavailableException
  */
  <K> MapKeyHostPartitionResult<K> getPartitionInfo(URI serviceURI,
                                                                 Collection<K> keys,
                                                                 int limitNumHostsPerPartition)
      throws ServiceUnavailableException
  {
    if (_partitionInfoProvider == null)
    {
      throw new UnsupportedOperationException("This method is unavailable if partitionInfoProvider is not provided.");
    }
    final Random random = new Random();
    return _partitionInfoProvider.getPartitionInformation(serviceURI, keys, limitNumHostsPerPartition,
                 new PartitionInfoProvider.HashProvider()
                 {
                   @Override
                   public int nextHash()
                   {
                     return random.nextInt();
                   }
                 });
  }

  /**
   * Similar to getPartitionInfo method above, but this accepts a stickyKey of type S. The stickyKey changes the
   * behavior of the returned hosts. Instead of returning hosts semi-randomly, we use the sticky to determine which
   * host we return. This means if we use the same sticky key, the list of hosts returned will be the same.
   *
   * @param <K> Partition Key
   * @param <S> Sticky Key
   * @return
   * @throws ServiceUnavailableException
   */
   <K, S> MapKeyHostPartitionResult<K> getPartitionInfo(URI serviceURI,
                                                                                                  Collection<K> keys,
                                                                                                  int limitNumHostsPerPartition,
                                                                                                  final S stickyKey)
      throws ServiceUnavailableException
  {
    if (_partitionInfoProvider == null)
    {
      throw new UnsupportedOperationException("This method is unavailable if partitionInfoProvider is not provided.");
    }
    return _partitionInfoProvider.getPartitionInformation(serviceURI, keys, limitNumHostsPerPartition,
        new StickyKeyHashProvider<S>(stickyKey));
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

  @Override
  public <K> HostToKeyMapper<K> mapKeysV3(URI serviceUri, Collection<K> keys, int limitNumHostsPerPartition)
      throws ServiceUnavailableException
  {
    if (_partitionInfoProvider == null)
    {
      throw new UnsupportedOperationException("This method is unavailable if partitionInfoProvider is not provided.");
    }
    return new HostToKeyMapper<K>(getPartitionInfo(serviceUri, keys, limitNumHostsPerPartition),
                                  _partitionInfoProvider.getPartitionAccessor(serviceUri),
                                  limitNumHostsPerPartition);
  }

  @Override
  public <K, S> HostToKeyMapper<K> mapKeysV3(URI serviceUri,
                                               Collection<K> keys,
                                               int limitNumHostsPerPartition,
                                               S stickyKey)
      throws ServiceUnavailableException
  {
    if (_partitionInfoProvider == null)
    {
      throw new UnsupportedOperationException("This method is unavailable if partitionInfoProvider is not provided.");
    }
    return new HostToKeyMapper<K>(getPartitionInfo(serviceUri, keys, limitNumHostsPerPartition,
                                                           stickyKey),
                                    _partitionInfoProvider.getPartitionAccessor(serviceUri),
                                    limitNumHostsPerPartition);
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

  private class StickyKeyHashProvider<S> implements PartitionInfoProvider.HashProvider
  {
    private final S _stickyKey;
    String[] _lastToken = new String[]{ "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
    int _lastIndex = 0;

    public StickyKeyHashProvider(S stickyKey) {
      _stickyKey = stickyKey;
    }

    @Override
    public int nextHash()
    {
      /*
      we keep a circular string tokens for md5 hash.
      every call to nextHash will update this array index from 0->1->2...->9->0->1
      for example we have key = "key".
      So at the 1st iteration we have
      ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"]
      we insert key
      ["key", "2", "3", "4", "5", "6", "7", "8", "9", "10"]
      hash it to get 392801752083
      then we replace key with 392801752083
      ["392801752083","2", "3", "4", "5", "6", "7", "8", "9", "10"]

      next iteration #2
      ["392801752083", "key", "3", "4", "5", "6", "7", "8", "9", "10"]
      hash it to get 875872391
      then replace key with 875872391
      ["392801752083", "875872391", "3", "4", "5", "6", "7", "8", "9", "10"]

      eventually all the number will be replaced and at 11th iteration, we replace the first element
      that is "392801752083" with the next hash and so on.
      */

      _lastToken[_lastIndex] = _stickyKey.toString();
      int result = _hashFunction.hash(_lastToken);
      _lastToken[_lastIndex] = Integer.toString(result);
      _lastIndex ++;
      _lastIndex = _lastIndex % _lastToken.length;
      return result;
    }
  }
}

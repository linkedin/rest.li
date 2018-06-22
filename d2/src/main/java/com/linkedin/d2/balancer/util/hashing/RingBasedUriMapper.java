/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util.hashing;

import com.linkedin.d2.balancer.Facilities;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.URIMapper;
import com.linkedin.d2.balancer.util.LoadBalancerUtil;
import com.linkedin.d2.balancer.util.URIKeyPair;
import com.linkedin.d2.balancer.util.URIMappingResult;
import com.linkedin.d2.balancer.util.URIRequest;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessException;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessor;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.r2.message.Request;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


/**
 * Implementation of URIMapper.
 *
 * It uses the {@link Ring}s in {@link com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3} to perform
 * sticky routing. If DegraderLoadBalancerStrategy is not used, host selection will be random.
 *
 * @author Alex Jing
 */

public class RingBasedUriMapper implements URIMapper
{
  private final HashRingProvider _hashRingProvider;
  private final PartitionInfoProvider _partitionInfoProvider;

  public RingBasedUriMapper(HashRingProvider hashRingProvider, PartitionInfoProvider partitionInfoProvider)
  {
    _hashRingProvider = hashRingProvider;
    _partitionInfoProvider = partitionInfoProvider;
  }

  public RingBasedUriMapper(Facilities facilities)
  {
    this(facilities.getHashRingProvider(), facilities.getPartitionInfoProvider());
  }

  /**
   * To achieve scatter-gather, there will be two passes.
   *
   * Pass 1: All requests are assigned a partitionId based on partition properties
   *         If partitioning is not enabled, all requests will have default partitionId of 0;
   *
   * Pass 2: All requests in the same partition will be routed based on the ring of that partition.
   *         If sticky routing is not specified, ONE host on the ring of that partition will be assigned for all hosts assigned to that partition.
   *
   * Unmapped key in either step will be collected in the unmapped keySet in the result.
   *
   * @param <KEY> type of provided key
   * @param requestUriKeyPairs a list of URIKeyPair, each contains a d2 request uri and a unique resource key.
   * @return {@link URIMappingResult} that contains host to keySet mapping as well as unmapped keys.
   * @throws ServiceUnavailableException when the requested service is not available
   */
  @Override
  public <KEY> URIMappingResult<KEY> mapUris(List<URIKeyPair<KEY>> requestUriKeyPairs)
      throws ServiceUnavailableException
  {
    // API assumes that all requests will be made to the same service, just use the first request to get the service name and act as sample uri
    URI sampleURI = requestUriKeyPairs.get(0).getRequestUri();
    String serviceName = LoadBalancerUtil.getServiceNameFromUri(sampleURI);

    // To achieve scatter-gather, we require the following information
    PartitionAccessor accessor = _partitionInfoProvider.getPartitionAccessor(serviceName);
    Map<Integer, Ring<URI>> rings = _hashRingProvider.getRings(sampleURI);
    HashFunction<Request> hashFunction = _hashRingProvider.getRequestHashFunction(serviceName);

    Set<KEY> unmapped = new HashSet<>();

    // Pass One
    Map<Integer, List<URIKeyPair<KEY>>> requestsByPartition = distributeToPartitions(requestUriKeyPairs, accessor, unmapped);

    // Pass Two
    Map<Set<KEY>, URI> keySetToHost = distributeToHosts(requestsByPartition, rings, hashFunction, unmapped);

    return new URIMappingResult<KEY>(keySetToHost, unmapped);
  }

  /**
   * Scatter gather is need if either the given service needs sticky routing or the given service is partitioned or both
   * @throws ServiceUnavailableException when the requested service is not available
   */
  @Override
  public boolean needScatterGather(String serviceName) throws ServiceUnavailableException
  {
    return isPartitioningEnabled(serviceName) || isStickyEnabled(serviceName);
  }

  /**
   * Determines if sticky routing is enabled for the given service.
   * Sticky routing is deemed enabled if the Loadbalancer hash method is UriRegex.
   *
   * @throws ServiceUnavailableException when the requested service is not available
   */
  private boolean isStickyEnabled(String serviceName) throws ServiceUnavailableException
  {
    HashFunction<Request> hashFunction = _hashRingProvider.getRequestHashFunction(serviceName);
    return hashFunction instanceof URIRegexHash;
  }

  /**
   * Determines if partitioning is enabled for the given service.
   * Partitioning is deemed enabled if there are more than 1 partition.
   *
   * @throws ServiceUnavailableException when the requested service is not available
   */
  private boolean isPartitioningEnabled(String serviceName) throws ServiceUnavailableException
  {
    PartitionAccessor accessor = _partitionInfoProvider.getPartitionAccessor(serviceName);
    return accessor.getMaxPartitionId() > 0;
  }

  private <KEY> Map<Integer, List<URIKeyPair<KEY>>> distributeToPartitions(List<URIKeyPair<KEY>> requestUriKeyPairs,
      PartitionAccessor accessor, Set<KEY> unmapped)
  {
    if (accessor.getMaxPartitionId() == 0)
    {
      return distributeToPartitionsUnpartitioned(requestUriKeyPairs);
    }

    Map<Integer, List<URIKeyPair<KEY>>> requestListsByPartitionId = new HashMap<>();

    requestUriKeyPairs.stream().forEach(request -> {
      try
      {
        int partitionId = accessor.getPartitionId(request.getRequestUri());
        requestListsByPartitionId.putIfAbsent(partitionId, new ArrayList<>());
        requestListsByPartitionId.get(partitionId).add(request);
      } catch (PartitionAccessException e)
      {
        unmapped.add(request.getKey());
      }
    });

    return requestListsByPartitionId;
  }

  /**
   * If unparititoned, we map all uris to the default partition, i.e. partition 0
   */
  private <KEY> Map<Integer, List<URIKeyPair<KEY>>> distributeToPartitionsUnpartitioned(
      List<URIKeyPair<KEY>> requestUriKeyPairs)
  {
    return Collections.singletonMap(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, requestUriKeyPairs);
  }

  private <KEY> Map<Set<KEY>, URI> distributeToHosts(
      Map<Integer, List<URIKeyPair<KEY>>> requestsByParititonId, Map<Integer, Ring<URI>> rings,
      HashFunction<Request> hashFunction, Set<KEY> unmapped)
  {
    if (hashFunction instanceof RandomHash)
    {
      return distributeToHostNonSticky(requestsByParititonId, rings);
    }

    Map<URI, Set<KEY>> hostToKeySet = new HashMap<>();
    for (Map.Entry<Integer, List<URIKeyPair<KEY>>> entry : requestsByParititonId.entrySet())
    {
      for (URIKeyPair<KEY> request : entry.getValue())
      {
        int hashcode = hashFunction.hash(new URIRequest(request.getRequestUri()));
        URI resolvedHost = rings.get(entry.getKey()).get(hashcode);
        if (resolvedHost == null)
        {
          unmapped.add(request.getKey());
        }
        else
        {
          hostToKeySet.computeIfAbsent(resolvedHost, host -> new HashSet<>());
          hostToKeySet.get(resolvedHost).add(request.getKey());
        }
      }
    }

    // Simply reverse the mapping between host and keySet since the keySets and hosts have one-to-one mapping
    return hostToKeySet.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
  }

  /**
   * if sticky is not enabled, map all uris of the same partition to ONE host. If the same host is picked for multiple partitions,
   * keys to those partitions will be merged into one set.
   */
  private <KEY> Map<Set<KEY>, URI> distributeToHostNonSticky(
      Map<Integer, List<URIKeyPair<KEY>>> requestsByParititonId, Map<Integer, Ring<URI>> rings)
  {
    Map<URI, Set<KEY>> hostToKeySet = new HashMap<>();
    for (Map.Entry<Integer, List<URIKeyPair<KEY>>> entry : requestsByParititonId.entrySet()) {
      URI resolvedHost = rings.get(entry.getKey()).get(ThreadLocalRandom.current().nextInt());
      hostToKeySet.computeIfAbsent(resolvedHost, host -> new HashSet<>());
      hostToKeySet.get(resolvedHost).addAll(convertURIKeyPairListToKeySet(entry.getValue()));
    }

    // Simply reverse the mapping between host and keySet since the keySets and hosts have one-to-one mapping
    return hostToKeySet.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
  }

  private static <KEY> Set<KEY> convertURIKeyPairListToKeySet(List<URIKeyPair<KEY>> list)
  {
    return list.stream().map(URIKeyPair::getKey).collect(Collectors.toSet());
  }
}

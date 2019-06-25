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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
  private static final Logger LOG = LoggerFactory.getLogger(RingBasedUriMapper.class);
  private static final int PARTITION_NOT_FOUND_ID = -1;

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
    if (requestUriKeyPairs == null || requestUriKeyPairs.isEmpty())
    {
      return new URIMappingResult<>(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    // API assumes that all requests will be made to the same service, just use the first request to get the service name and act as sample uri
    URI sampleURI = requestUriKeyPairs.get(0).getRequestUri();
    String serviceName = LoadBalancerUtil.getServiceNameFromUri(sampleURI);

    // To achieve scatter-gather, we require the following information
    PartitionAccessor accessor = _partitionInfoProvider.getPartitionAccessor(serviceName);
    Map<Integer, Ring<URI>> rings = _hashRingProvider.getRings(sampleURI);
    HashFunction<Request> hashFunction = _hashRingProvider.getRequestHashFunction(serviceName);

    Map<Integer, Set<KEY>> unmapped = new HashMap<>();

    // Pass One
    Map<Integer, List<URIKeyPair<KEY>>> requestsByPartition =
        distributeToPartitions(requestUriKeyPairs, accessor, unmapped);

    // Pass Two
    Map<URI, Integer> hostToParitionId = new HashMap<>();
    Map<URI, Set<KEY>> hostToKeySet = distributeToHosts(requestsByPartition, rings, hashFunction, hostToParitionId, unmapped);

    return new URIMappingResult<KEY>(hostToKeySet, unmapped, hostToParitionId);
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
      PartitionAccessor accessor, Map<Integer, Set<KEY>> unmapped)
  {
    if (accessor.getMaxPartitionId() == 0)
    {
      return distributeToPartitionsUnpartitioned(requestUriKeyPairs);
    }

    if (checkPartitionIdOverride(requestUriKeyPairs))
    {
      return doPartitionIdOverride(requestUriKeyPairs.get(0));
    }

    Map<Integer, List<URIKeyPair<KEY>>> requestListsByPartitionId = new HashMap<>();

    requestUriKeyPairs.forEach(request -> {
      try
      {
        int partitionId = accessor.getPartitionId(request.getRequestUri());
        requestListsByPartitionId.putIfAbsent(partitionId, new ArrayList<>());
        requestListsByPartitionId.get(partitionId).add(request);
      }
      catch (PartitionAccessException e)
      {
        unmapped.computeIfAbsent(PARTITION_NOT_FOUND_ID, k -> new HashSet<>()).add(request.getKey());
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

  private <KEY> Map<URI, Set<KEY>> distributeToHosts(
      Map<Integer, List<URIKeyPair<KEY>>> requestsByParititonId,
      Map<Integer, Ring<URI>> rings,
      HashFunction<Request> hashFunction,
      Map<URI, Integer> hostToPartitionId,
      Map<Integer, Set<KEY>> unmapped)
  {
    if (hashFunction instanceof RandomHash)
    {
      return distributeToHostNonSticky(requestsByParititonId, rings, hostToPartitionId, unmapped);
    }

    Map<URI, Set<KEY>> hostToKeySet = new HashMap<>();
    for (Map.Entry<Integer, List<URIKeyPair<KEY>>> entry : requestsByParititonId.entrySet())
    {
      int partitionId = entry.getKey();
      for (URIKeyPair<KEY> request : entry.getValue())
      {
        int hashcode = hashFunction.hash(new URIRequest(request.getRequestUri()));
        URI resolvedHost = rings.get(partitionId).get(hashcode);

        if (resolvedHost == null)
        {
          // under custom use case, key will be null, in which case we will just return a map from partition id to empty set
          // Users should be able to understand what partitions do not have available hosts by examining the keys in "unmapped"
          Set<KEY> unmappedKeys = convertURIKeyPairListToKeySet(entry.getValue());
          unmapped.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(unmappedKeys);
          break;
        }
        else
        {
          // under custom use case, key will be null, in which case we will just return a map from uri to empty set
          hostToPartitionId.putIfAbsent(resolvedHost, entry.getKey());
          Set<KEY> newSet = hostToKeySet.computeIfAbsent(resolvedHost, host -> new HashSet<>());
          if (request.getKey() != null)
          {
            newSet.add(request.getKey());
          }
        }
      }
    }

    return hostToKeySet;
  }

  /**
   * if sticky is not enabled, map all uris of the same partition to ONE host. If the same host is picked for multiple partitions,
   * keys to those partitions will be merged into one set.
   */
  private <KEY> Map<URI, Set<KEY>> distributeToHostNonSticky(Map<Integer, List<URIKeyPair<KEY>>> requestsByParititonId,
      Map<Integer, Ring<URI>> rings, Map<URI, Integer> hostToPartitionId, Map<Integer, Set<KEY>> unmapped)
  {
    Map<URI, Set<KEY>> hostToKeySet = new HashMap<>();
    for (Map.Entry<Integer, List<URIKeyPair<KEY>>> entry : requestsByParititonId.entrySet())
    {
      URI resolvedHost = rings.get(entry.getKey()).get(ThreadLocalRandom.current().nextInt());
      Set<KEY> allKeys = convertURIKeyPairListToKeySet(entry.getValue());

      if (resolvedHost == null)
      {
        unmapped.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(allKeys);
      }
      else
      {
        hostToPartitionId.putIfAbsent(resolvedHost, entry.getKey());
        hostToKeySet.computeIfAbsent(resolvedHost, host -> new HashSet<>()).addAll(allKeys);
      }
    }

    return hostToKeySet;
  }

  private static <KEY> Set<KEY> convertURIKeyPairListToKeySet(List<URIKeyPair<KEY>> list)
  {
    if (list.stream().anyMatch(uriKeyPair -> uriKeyPair.getKey() == null))
    {
      // under custom use case, key will be null, in which case we will just return a map from uri to empty set
      return Collections.emptySet();
    }
    return list.stream().map(URIKeyPair::getKey).collect(Collectors.toSet());
  }

  /**
   * Check for custom use case of URIMapper. Custom use case allows user to specify a set of partition ids to scatter the request to.
   * Under custom use case, only ONE URIKeyPair is allowed; all overridden partition ids should be put in it.
   * @param requests requests to be scattered
   * @param <KEY> request key, which should be Null under custom use case
   * @return true if d2 partitioning should be bypassed
   */
  private <KEY> boolean checkPartitionIdOverride(List<URIKeyPair<KEY>> requests)
  {
    if (requests.stream().anyMatch(URIKeyPair::hasOverriddenPartitionIds))
    {
      if (requests.size() == 1)
      {
        LOG.debug("Use partition ids provided by custom scatter gather strategy");
        return true;
      }
      else
      {
        throw new IllegalStateException(
            "More than one request with overridden partition ids are provided. "
                + "Consider put all partition ids in one set or send different request if URI is different");
      }
    }
    return false;
  }

  /**
   *  when partition ids are overridden, this function will return a map from each partition id to ONE URIKeyPair, where the
   *  URIKeyPair has Null as key and its request uri is used to determine sticky routing.
   * @param request request with overridden partition ids
   * @param <KEY> should be null in this case
   * @return a map from partition ids to one URIKeyPair, whose uri will be used to determine stickiness later on.
   */
  private <KEY> Map<Integer, List<URIKeyPair<KEY>>> doPartitionIdOverride(URIKeyPair<KEY> request)
  {
    return request.getOverriddenPartitionIds()
        .stream()
        .collect(Collectors.toMap(Function.identity(), partitionId -> Collections.singletonList(request)));
  }
}

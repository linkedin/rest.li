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

import com.google.common.collect.Lists;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.properties.RangeBasedPartitionProperties;
import com.linkedin.d2.balancer.util.URIKeyPair;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.d2.balancer.util.partitions.RangeBasedPartitionAccessor;
import com.linkedin.r2.message.Request;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.mockito.Mockito;

import static org.mockito.Matchers.*;


/**
 * Utility functions used in {@link RingBasedURIMapperTest} and URIMapperVSKeyMapperBenchmark.
 *
 * @author Alex Jing
 */
public class URIMapperTestUtil
{
  private static final String PARTITION_KEY_REGEX = "partition=(\\d+)";
  private static final String URI_KEY_REGEX = "/(\\d+)";
  private static final String D2_PREFIX = "d2://";
  private static final String HOST_NAME_TEMPLATE = "http://test-%d-partition-%d/resources";

  public static final String TEST_SERVICE = "testService";

  /**
   * return {@link URIRegexHash} if sticky, {@link RandomHash} otherwise
   */
  public static HashFunction<Request> getHashFunction(boolean sticky)
  {
    return sticky ? new URIRegexHash(Collections.singletonList(URI_KEY_REGEX), false, false) : new RandomHash();
  }

  /**
   * Create {@link StaticRingProvider} for testing purpose
   */
  public static HashRingProvider createStaticHashRingProvider(int totalHostCount, int partitionCount,
      HashFunction<Request> hashFunction)
  {
    int hostPerPartition = totalHostCount / partitionCount;
    List<Integer> hostsIds = IntStream.range(0, totalHostCount).boxed().collect(Collectors.toList());
    List<List<Integer>> hostsIdsByPartition = Lists.partition(hostsIds, hostPerPartition);

    List<Ring<URI>> rings = new ArrayList<>();
    int partiitonId = 0;
    for (List<Integer> uriList : hostsIdsByPartition) {
      int parId = partiitonId;
      Map<URI, Integer> hostMap = uriList.stream().collect(Collectors.toMap(e -> createHostURI(parId, e), e -> 100));
      Ring<URI> ring = new MPConsistentHashRing<>(hostMap);
      rings.add(ring);
      partiitonId++;
    }
    StaticRingProvider ringProvider = new StaticRingProvider(rings);
    ringProvider.setHashFunction(hashFunction);
    return ringProvider;
  }

  /**
   * Create a mock PartitionInfoProvider that returns {@link RangeBasedPartitionAccessor} for testing
   */
  public static PartitionInfoProvider createPartitionInfoProvider(int partitionCount) throws ServiceUnavailableException
  {
    PartitionInfoProvider infoProvider = Mockito.mock(PartitionInfoProvider.class);
    RangeBasedPartitionProperties properties =
        new RangeBasedPartitionProperties(PARTITION_KEY_REGEX, 0, 1, partitionCount);
    RangeBasedPartitionAccessor accessor = new RangeBasedPartitionAccessor(properties);
    Mockito.when(infoProvider.getPartitionAccessor(anyObject())).thenReturn(accessor);
    return infoProvider;
  }

  /**
   * Generate a list of requests for {@link com.linkedin.d2.balancer.URIMapper}, each with a unique key
   */
  public List<URIKeyPair<Integer>> generateRequests(int partitionCount, int requestsPerPartition)
  {
    UniqueKeyProvider keyProvider = new UniqueKeyProvider(requestsPerPartition * partitionCount);
    List<URIKeyPair<Integer>> requests = new ArrayList<>();
    IntStream.range(0, partitionCount).forEach(partitionId -> {
      IntStream.range(0, requestsPerPartition).forEach(count -> {
        requests.add(createRequestURI(TEST_SERVICE, partitionId, keyProvider.getKey()));
      });
    });
    return requests;
  }

  public static <KEY> URIKeyPair<KEY> createRequestURI(String serviceName, int partitionId, KEY key)
  {
    URI uri = null;
    try {
      uri = new URI(D2_PREFIX + serviceName + "/" + key + "?" + "partition=" + partitionId + "");
    } catch (URISyntaxException e) {
      // won't happen
    }
    return new URIKeyPair<>(key, uri);
  }

  public static URI createHostURI(int partitionId, int identitifier)
  {
    URI uri = null;
    try {
      // For test convenience, assuming each host only serves one partition
      uri = new URI(String.format(HOST_NAME_TEMPLATE, identitifier, partitionId));
    } catch (URISyntaxException e) {
      // won't happen
    }
    return uri;
  }

  /**
   * Generate unique integer keys in a thread-safe fashion
   */
  private class UniqueKeyProvider
  {
    private AtomicInteger _count;
    private int _maxKey;

    UniqueKeyProvider(int totalKeyCount)
    {
      _count = new AtomicInteger(0);
      _maxKey = totalKeyCount - 1;
    }

    int getKey()
    {
      int key = _count.getAndIncrement();
      if (key > _maxKey) {
        throw new RuntimeException("requests more keys than allowed!");
      }
      return key;
    }
  }
}

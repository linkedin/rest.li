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

import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.URIMapper;
import com.linkedin.d2.balancer.util.URIKeyPair;
import com.linkedin.d2.balancer.util.URIMappingResult;
import com.linkedin.d2.balancer.util.URIRequest;
import com.linkedin.d2.balancer.util.partitions.PartitionAccessException;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import com.linkedin.r2.message.Request;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.balancer.util.hashing.URIMapperTestUtil.*;


public class RingBasedURIMapperTest
{
  private static final String TEST_SERVICE = URIMapperTestUtil.TEST_SERVICE;
  private static final URIMapperTestUtil testUtil = new URIMapperTestUtil();

  @Test
  public void testNeedScatterGather() throws ServiceUnavailableException
  {
    // Both sticky and partitioned
    HashRingProvider ringProvider = createStaticHashRingProvider(100, 10, getHashFunction(true));
    PartitionInfoProvider infoProvider = createRangeBasedPartitionInfoProvider(10);
    URIMapper mapper = new RingBasedUriMapper(ringProvider, infoProvider);
    Assert.assertTrue(mapper.needScatterGather(TEST_SERVICE));

    // Only sticky
    ringProvider = createStaticHashRingProvider(100, 1, getHashFunction(true));
    infoProvider = createRangeBasedPartitionInfoProvider(1);
    mapper = new RingBasedUriMapper(ringProvider, infoProvider);
    Assert.assertTrue(mapper.needScatterGather(TEST_SERVICE));

    // Only partitioned
    ringProvider = createStaticHashRingProvider(100, 10, getHashFunction(false));
    infoProvider = createRangeBasedPartitionInfoProvider(10);
    mapper = new RingBasedUriMapper(ringProvider, infoProvider);
    Assert.assertTrue(mapper.needScatterGather(TEST_SERVICE));

    // neither
    ringProvider = createStaticHashRingProvider(100, 1, getHashFunction(false));
    infoProvider = createRangeBasedPartitionInfoProvider(1);
    mapper = new RingBasedUriMapper(ringProvider, infoProvider);
    Assert.assertFalse(mapper.needScatterGather(TEST_SERVICE));
  }

  @Test
  public void testMapUrisPartitionedOnly() throws ServiceUnavailableException
  {
    int partitionCount = 10;
    int requestPerPartition = 100;
    int totalHostCount = 100;

    HashRingProvider ringProvider =
        createStaticHashRingProvider(totalHostCount, partitionCount, getHashFunction(false));
    PartitionInfoProvider infoProvider = createRangeBasedPartitionInfoProvider(partitionCount);
    URIMapper mapper = new RingBasedUriMapper(ringProvider, infoProvider);

    List<URIKeyPair<Integer>> requests = testUtil.generateRequests(partitionCount, requestPerPartition);

    URIMappingResult<Integer> results = mapper.mapUris(requests);
    Map<URI, Set<Integer>> mapping = results.getMappedKeys();

    // No unmapped keys
    Assert.assertTrue(results.getUnmappedKeys().isEmpty());

    // Without sticky routing, one host should be returned for each partition
    Assert.assertEquals(10, mapping.size());

    Set<Integer> mappedKeys = mapping.values().stream().reduce(new HashSet<>(), (e1, e2) -> {
      e1.addAll(e2);
      return e1;
    });

    int mappedKeyCount = mapping.values().stream().map(Set::size).reduce(Integer::sum).get();

    // Collective exhaustiveness and mutual exclusiveness
    Assert.assertEquals(partitionCount * requestPerPartition, mappedKeys.size());
    Assert.assertEquals(partitionCount * requestPerPartition, mappedKeyCount);
  }

  @Test
  public void testMapUrisStickyRoutingOnly() throws ServiceUnavailableException, PartitionAccessException
  {
    int partitionCount = 1;
    int requestPerPartition = 1000;
    int totalHostCount = 100;

    HashRingProvider ringProvider = createStaticHashRingProvider(totalHostCount, partitionCount, getHashFunction(true));
    PartitionInfoProvider infoProvider = createRangeBasedPartitionInfoProvider(partitionCount);
    URIMapper mapper = new RingBasedUriMapper(ringProvider, infoProvider);

    List<URIKeyPair<Integer>> requests = testUtil.generateRequests(partitionCount, requestPerPartition);

    URIMappingResult<Integer> results1 = mapper.mapUris(requests);
    URIMappingResult<Integer> results2 = mapper.mapUris(requests);

    // Sticky routing between two runs
    Assert.assertEquals(results1.getMappedKeys(), results2.getMappedKeys());
    Assert.assertEquals(results1.getUnmappedKeys(), results2.getUnmappedKeys());

    Map<URI, Set<Integer>> mapping = results1.getMappedKeys();

    // Testing universal stickiness, take out 50 requests randomly and make sure they would be resolved to the same host as does URIMapper
    Collections.shuffle(requests);
    HashFunction<Request> hashFunction = ringProvider.getRequestHashFunction(TEST_SERVICE);
    for (int i = 0; i < 50; i++)
    {
      URIKeyPair<Integer> request = requests.get(i);
      int partitionId = infoProvider.getPartitionAccessor(TEST_SERVICE).getPartitionId(request.getRequestUri());
      Ring<URI> ring = ringProvider.getRings(request.getRequestUri()).get(partitionId);
      URI uri = ring.get(hashFunction.hash(new URIRequest(request.getRequestUri())));
      Assert.assertTrue(mapping.keySet().contains(uri));
    }
  }

  @Test
  public void testStickyAndPartitioning() throws ServiceUnavailableException
  {
    int partitionCount = 10;
    int requestPerPartition = 100;
    int totalHostCount = 100;

    HashRingProvider ringProvider = createStaticHashRingProvider(totalHostCount, partitionCount, getHashFunction(true));
    PartitionInfoProvider infoProvider = createRangeBasedPartitionInfoProvider(partitionCount);
    URIMapper mapper = new RingBasedUriMapper(ringProvider, infoProvider);

    List<URIKeyPair<Integer>> requests = testUtil.generateRequests(partitionCount, requestPerPartition);

    URIMappingResult<Integer> results = mapper.mapUris(requests);
    Map<URI, Set<Integer>> mapping = results.getMappedKeys();
    Map<Integer, Set<Integer>> unmappedKeys = results.getUnmappedKeys();

    Assert.assertTrue(unmappedKeys.isEmpty());
    Assert.assertEquals(100, mapping.size());
  }

  @Test
  public void testNonStickyAndNonPartitioning() throws ServiceUnavailableException
  {
    int partitionCount = 1;
    int requestPerPartition = 1000;
    int totalHostCount = 100;

    HashRingProvider ringProvider =
        createStaticHashRingProvider(totalHostCount, partitionCount, getHashFunction(false));
    PartitionInfoProvider infoProvider = createRangeBasedPartitionInfoProvider(partitionCount);
    URIMapper mapper = new RingBasedUriMapper(ringProvider, infoProvider);

    List<URIKeyPair<Integer>> requests = testUtil.generateRequests(partitionCount, requestPerPartition);

    URIMappingResult<Integer> results = mapper.mapUris(requests);
    Map<URI, Set<Integer>> mapping = results.getMappedKeys();
    Map<Integer, Set<Integer>> unmappedKeys = results.getUnmappedKeys();

    Assert.assertTrue(unmappedKeys.isEmpty());
    Assert.assertEquals(1, mapping.size());
    Assert.assertEquals(1000, mapping.values().iterator().next().size());
  }

  /**
   * If one host supports multiple partitions and for those partitions, this same one host happens to be picked, URIMapper should
   * merge the key entries for those partitions.
   */
  @Test
  public void testSameHostSupportingMultiplePartitions() throws ServiceUnavailableException
  {
    int partitionCount = 10;
    int requestPerPartition = 100;

    // one host supporting 10 partitions
    URI host = createHostURI(0, 0);
    List<Ring<URI>> rings = IntStream.range(0, partitionCount)
        .boxed()
        .map(i -> new MPConsistentHashRing<>(Collections.singletonMap(host, 100)))
        .collect(Collectors.toList());
    StaticRingProvider ringProvider = new StaticRingProvider(rings);
    ringProvider.setHashFunction(new RandomHash());

    PartitionInfoProvider infoProvider = createRangeBasedPartitionInfoProvider(partitionCount);
    URIMapper mapper = new RingBasedUriMapper(ringProvider, infoProvider);

    List<URIKeyPair<Integer>> requests = testUtil.generateRequests(partitionCount, requestPerPartition);

    URIMappingResult<Integer> results = mapper.mapUris(requests);
    Map<URI, Set<Integer>> mapping = results.getMappedKeys();
    Map<Integer, Set<Integer>> unmappedKeys = results.getUnmappedKeys();

    Assert.assertTrue(unmappedKeys.isEmpty());
    Assert.assertEquals(1, mapping.size());
    Assert.assertEquals(1000, mapping.values().iterator().next().size());
  }

  @Test
  public void testErrorHandling() throws ServiceUnavailableException, URISyntaxException
  {
    int partitionCount = 10;
    int totalHostCount = 100;

    HashRingProvider ringProvider = createStaticHashRingProvider(totalHostCount, partitionCount, getHashFunction(true));
    PartitionInfoProvider infoProvider = createRangeBasedPartitionInfoProvider(partitionCount);
    URIMapper mapper = new RingBasedUriMapper(ringProvider, infoProvider);

    URIKeyPair<Integer> requestWithoutPartitionId = new URIKeyPair<>(42, new URI("d2://badService/2"));
    URIKeyPair<Integer> requestWithoutKey = new URIKeyPair<>(43, new URI("d2://badService/partitionId=3"));
    URIKeyPair<Integer> requestWithoutBoth = new URIKeyPair<>(44, new URI("d2://badService"));

    List<URIKeyPair<Integer>> requests =
        Arrays.asList(requestWithoutKey, requestWithoutPartitionId, requestWithoutBoth);

    URIMappingResult<Integer> result = mapper.mapUris(requests);
    Assert.assertTrue(result.getMappedKeys().isEmpty());
    Assert.assertTrue(result.getUnmappedKeys().get(-1).contains(42));
    Assert.assertTrue(result.getUnmappedKeys().get(-1).contains(43));
    Assert.assertTrue(result.getUnmappedKeys().get(-1).contains(44));
  }

  @Test
  public void testUniversalStickiness() throws ServiceUnavailableException, URISyntaxException
  {
    int partitionCount = 4;
    int totalHostCount = 200;

    HashRingProvider ringProvider = createStaticHashRingProvider(totalHostCount, partitionCount, getHashFunction(true));
    HashFunction<Request> hashFunction = ringProvider.getRequestHashFunction(TEST_SERVICE);
    PartitionInfoProvider infoProvider = createRangeBasedPartitionInfoProvider(partitionCount);
    URIMapper mapper = new RingBasedUriMapper(ringProvider, infoProvider);

    URIKeyPair<Integer> request1 = new URIKeyPair<>(1, new URI("d2://testService/1")); // no partition, will be unmapped
    URIKeyPair<Integer> request2 = new URIKeyPair<>(2, new URI("d2://testService/2?partition=0")); // partition 0
    URIKeyPair<Integer> request3 = new URIKeyPair<>(3, new URI("d2://testService/3?partition=1")); // partition 1
    URIKeyPair<Integer> request4 = new URIKeyPair<>(4, new URI("d2://testService/4?partition=2")); // partition 2
    URIKeyPair<Integer> request5 = new URIKeyPair<>(5, new URI("d2://testService/5?partition=3")); // partition 3
    URIKeyPair<Integer> request6 =
        new URIKeyPair<>(6, new URI("d2://testService/6?partition=0")); // partition 0 with different sticky key
    URIKeyPair<Integer> request7 =
        new URIKeyPair<>(7, new URI("d2://testService/7?partition=1")); // partition 1 with different sticky key
    URIKeyPair<Integer> request8 =
        new URIKeyPair<>(8, new URI("d2://testService/8?partition=2")); // partition 2 with different sticky key
    URIKeyPair<Integer> request9 =
        new URIKeyPair<>(9, new URI("d2://testService/9?partition=3")); // partition 3 with different sticky key
    URIKeyPair<Integer> request10 =
        new URIKeyPair<>(10, new URI("d2://testService/10?partition=0&uuid=1"));// with extra parameters

    List<URIKeyPair<Integer>> requests =
        Arrays.asList(request1, request2, request3, request4, request5, request6, request7, request8, request9,
            request10);

    // uriMapper mapping
    URIMappingResult<Integer> uriMapperResult = mapper.mapUris(requests);

    // normal mapping
    Map<Integer, Set<Integer>> normalUnmapped = new HashMap<>();
    Map<URI, Set<Integer>> normalHostToKeySet = new HashMap<>();
    for (URIKeyPair<Integer> request : requests)
    {
      int partitionId = 0;
      try
      {
        partitionId = infoProvider.getPartitionAccessor(TEST_SERVICE).getPartitionId(request.getRequestUri());
      }
      catch (PartitionAccessException e)
      {
        normalUnmapped.computeIfAbsent(-1, k -> new HashSet<>()).add(request.getKey());
      }
      Ring<URI> ring = ringProvider.getRings(request.getRequestUri()).get(partitionId);
      URI uri = ring.get(hashFunction.hash(new URIRequest(request.getRequestUri())));
      normalHostToKeySet.computeIfAbsent(uri, k -> new HashSet<>());
      normalHostToKeySet.get(uri).add(request.getKey());
    }

    // they should have the same results
    Assert.assertEquals(uriMapperResult.getUnmappedKeys(), normalUnmapped);
    for (Map.Entry<URI, Set<Integer>> resolvedKeys : uriMapperResult.getMappedKeys().entrySet())
    {
      Set<Integer> uriMapperKeySet = resolvedKeys.getValue();
      Assert.assertTrue(normalHostToKeySet.containsKey(resolvedKeys.getKey()));
      Set<Integer> normalKeySet = normalHostToKeySet.get(resolvedKeys.getKey());
      Assert.assertEquals(uriMapperKeySet, normalKeySet);
    }
  }

  @Test(dataProvider = "stickyPartitionPermutation")
  public void testPartitionIdOverride(boolean sticky, boolean partitioned) throws Exception
  {
    int partitionCount = partitioned ? 10 : 1;
    int totalHostCount = 100;

    HashRingProvider ringProvider = createStaticHashRingProvider(totalHostCount, partitionCount, getHashFunction(sticky));
    PartitionInfoProvider infoProvider = createRangeBasedPartitionInfoProvider(partitionCount);
    URIMapper mapper = new RingBasedUriMapper(ringProvider, infoProvider);

    URIKeyPair<Integer> request = new URIKeyPair<>(new URI("d2://testService/1"),
        IntStream.range(0, partitionCount).boxed().collect(Collectors.toSet()));

    if (partitioned)
    {
      Assert.assertThrows(() -> mapper.mapUris(Arrays.asList(request, request)));
    }

    URIMappingResult<Integer> uriMapperResult = mapper.mapUris(Collections.singletonList(request));
    Map<URI, Set<Integer>> mappedKeys = uriMapperResult.getMappedKeys();
    Assert.assertTrue(uriMapperResult.getUnmappedKeys().isEmpty());
    Assert.assertEquals(mappedKeys.size(), partitionCount);
    Assert.assertEquals(
        mappedKeys.keySet().stream().map(URIMapperTestUtil::getPartitionIdForURI).collect(Collectors.toSet()).size(),
        partitionCount);
    for (Set<Integer> keys : mappedKeys.values())
    {
      Assert.assertTrue(keys.isEmpty());
    }
  }

  @Test(dataProvider = "stickyPartitionPermutation")
  public void testUnmappedKeys(boolean sticky, boolean partitioned) throws Exception
  {
    int partitionCount = partitioned ? 10 : 1;
    int requestPerPartition = 100;

    List<Ring<URI>> rings = new ArrayList<>();
    IntStream.range(0, partitionCount).forEach(i -> rings.add(new MPConsistentHashRing<>(Collections.emptyMap())));
    StaticRingProvider ringProvider = new StaticRingProvider(rings);
    ringProvider.setHashFunction(getHashFunction(sticky));
    PartitionInfoProvider infoProvider = createRangeBasedPartitionInfoProvider(partitionCount);
    URIMapper mapper = new RingBasedUriMapper(ringProvider, infoProvider);

    List<URIKeyPair<Integer>> requests = testUtil.generateRequests(partitionCount, requestPerPartition);
    URIMappingResult<Integer> uriMapperResultNormal = mapper.mapUris(requests);
    Assert.assertTrue(uriMapperResultNormal.getUnmappedKeys().size() == partitionCount);
    uriMapperResultNormal.getUnmappedKeys()
        .forEach((key, value) -> Assert.assertTrue(value.size() == requestPerPartition));

    URIKeyPair<Integer> request = new URIKeyPair<>(new URI("d2://testService/1"),
        IntStream.range(0, partitionCount).boxed().collect(Collectors.toSet()));
    URIMappingResult<Integer> uriMapperResultCustom = mapper.mapUris(Collections.singletonList(request));
    Assert.assertTrue(uriMapperResultCustom.getUnmappedKeys().size() == partitionCount);
    uriMapperResultCustom.getUnmappedKeys()
        .forEach((key, value) -> Assert.assertTrue(value.isEmpty()));
  }

  @DataProvider
  public Object[][] stickyPartitionPermutation()
  {
    return new Object[][]{
        {true, true},
        {true, false},
        {false, true},
        {false, false}
    };
  }
}

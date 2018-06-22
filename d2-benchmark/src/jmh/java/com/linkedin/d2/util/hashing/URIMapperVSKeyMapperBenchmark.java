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

package com.linkedin.d2.util.hashing;

import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.URIMapper;
import com.linkedin.d2.balancer.util.MapKeyResult;
import com.linkedin.d2.balancer.util.URIKeyPair;
import com.linkedin.d2.balancer.util.URIMappingResult;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashKeyMapper;
import com.linkedin.d2.balancer.util.hashing.HashRingProvider;
import com.linkedin.d2.balancer.util.hashing.RingBasedUriMapper;
import com.linkedin.d2.balancer.util.hashing.URIMapperTestUtil;
import com.linkedin.d2.balancer.util.partitions.PartitionInfoProvider;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import static com.linkedin.d2.balancer.util.hashing.URIMapperTestUtil.*;


@Fork(2)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class URIMapperVSKeyMapperBenchmark
{
  private static final URIMapperTestUtil testUtil = new URIMapperTestUtil();

  @State(Scope.Benchmark)
  public static class Case1_StickyAndPartitioned_100Hosts_10Partition_1000Requests_State
  {
    URIMapper _uriMapper;
    KeyMapper _keyMapper;

    // uriMapper argument
    List<URIKeyPair<Integer>> _uriMapperRequests;

    // keyMapper argument
    URI _serviceURI;
    Iterable<Integer> _keys;

    public Case1_StickyAndPartitioned_100Hosts_10Partition_1000Requests_State()
    {
      try {
        HashRingProvider hashRingProvider = createStaticHashRingProvider(100, 10, getHashFunction(true));
        PartitionInfoProvider infoProvider = createPartitionInfoProvider(10);

        _uriMapper = new RingBasedUriMapper(hashRingProvider, infoProvider);
        _uriMapperRequests = testUtil.generateRequests(10, 100);

        _keyMapper = new ConsistentHashKeyMapper(hashRingProvider, infoProvider);
        _serviceURI = new URI("d2://testService");
        _keys = IntStream.range(0, 1000).boxed().collect(Collectors.toList());
      } catch (Exception e) {
        // ignore exceptions
      }
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public URIMappingResult<Integer> Case1MeasureURIMapper(
      Case1_StickyAndPartitioned_100Hosts_10Partition_1000Requests_State state) throws ServiceUnavailableException
  {
    return state._uriMapper.mapUris(state._uriMapperRequests);
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public MapKeyResult<URI, Integer> Case1MeasureKeymapper(
      Case1_StickyAndPartitioned_100Hosts_10Partition_1000Requests_State state) throws ServiceUnavailableException
  {
    return state._keyMapper.mapKeysV2(state._serviceURI, state._keys);
  }

  @State(Scope.Benchmark)
  public static class Case2_Sticky_100Hosts_1Partition_10000Requests_State
  {
    URIMapper _uriMapper;
    KeyMapper _keyMapper;

    // uriMapper argument
    List<URIKeyPair<Integer>> _uriMapperRequests;

    // keyMapper argument
    URI _serviceURI;
    Iterable<Integer> _keys;

    public Case2_Sticky_100Hosts_1Partition_10000Requests_State()
    {
      try {
        HashRingProvider hashRingProvider = createStaticHashRingProvider(100, 1, getHashFunction(true));
        PartitionInfoProvider infoProvider = createPartitionInfoProvider(1);

        _uriMapper = new RingBasedUriMapper(hashRingProvider, infoProvider);
        _uriMapperRequests = testUtil.generateRequests(1, 10000);

        _keyMapper = new ConsistentHashKeyMapper(hashRingProvider, infoProvider);
        _serviceURI = new URI("d2://testService");
        _keys = IntStream.range(0, 10000).boxed().collect(Collectors.toList());
      } catch (Exception e) {
        // ignore exceptions
      }
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public URIMappingResult<Integer> Case2MeasureURIMapper(Case2_Sticky_100Hosts_1Partition_10000Requests_State state)
      throws ServiceUnavailableException
  {
    return state._uriMapper.mapUris(state._uriMapperRequests);
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public MapKeyResult<URI, Integer> Case2MeasureKeymapper(Case2_Sticky_100Hosts_1Partition_10000Requests_State state)
      throws ServiceUnavailableException
  {
    return state._keyMapper.mapKeysV2(state._serviceURI, state._keys);
  }

  @State(Scope.Benchmark)
  public static class Case3_Partitioned_100Hosts_10Partition_10000Requests_State
  {
    URIMapper _uriMapper;
    KeyMapper _keyMapper;

    // uriMapper argument
    List<URIKeyPair<Integer>> _uriMapperRequests;

    // keyMapper argument
    URI _serviceURI;
    Iterable<Integer> _keys;

    public Case3_Partitioned_100Hosts_10Partition_10000Requests_State()
    {
      try {
        HashRingProvider hashRingProvider = createStaticHashRingProvider(100, 10, getHashFunction(false));
        PartitionInfoProvider infoProvider = createPartitionInfoProvider(10);

        _uriMapper = new RingBasedUriMapper(hashRingProvider, infoProvider);
        _uriMapperRequests = testUtil.generateRequests(10, 1000);

        _keyMapper = new ConsistentHashKeyMapper(hashRingProvider, infoProvider);
        _serviceURI = new URI("d2://testService");
        _keys = IntStream.range(0, 10000).boxed().collect(Collectors.toList());
      } catch (Exception e) {
        // ignore exceptions
      }
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public URIMappingResult<Integer> Case3MeasureURIMapper(
      Case3_Partitioned_100Hosts_10Partition_10000Requests_State state) throws ServiceUnavailableException
  {
    return state._uriMapper.mapUris(state._uriMapperRequests);
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.MILLISECONDS)
  public MapKeyResult<URI, Integer> Case3MeasureKeymapper(
      Case3_Partitioned_100Hosts_10Partition_10000Requests_State state) throws ServiceUnavailableException
  {
    return state._keyMapper.mapKeysV2(state._serviceURI, state._keys);
  }
}

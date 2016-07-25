/*
   Copyright (c) 2016 LinkedIn Corp.

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

import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.MPConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.Ring;
import java.net.URI;
import java.util.Map;
import java.util.Random;
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


/**
 * @author Ang Xu
 */
@Fork(2)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class ConsistentHashRingBenchmark {

  @State(Scope.Benchmark)
  public static class MPCHash_10Hosts_11Probes_State {
    Ring<URI> _ring = new MPConsistentHashRing<>(buildPointsMap(10, 100), 11);
    Random _random = new Random();
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureMPCHash_10Hosts_11Probes(MPCHash_10Hosts_11Probes_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @State(Scope.Benchmark)
  public static class MPCHash_10Hosts_21Probes_State {
    Ring<URI> _ring = new MPConsistentHashRing<>(buildPointsMap(10, 100), 21);
    Random _random = new Random();
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureMPCHash_10Hosts_21Probes(MPCHash_10Hosts_21Probes_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @State(Scope.Benchmark)
  public static class MPCHash_100Hosts_11Probes_State {
    Ring<URI> _ring = new MPConsistentHashRing<>(buildPointsMap(100, 100), 11);
    Random _random = new Random();
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureMPCHash_100Hosts_11Probes(MPCHash_100Hosts_11Probes_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @State(Scope.Benchmark)
  public static class MPCHash_100Hosts_21Probes_State {
    Ring<URI> _ring = new MPConsistentHashRing<>(buildPointsMap(100, 100), 21);
    Random _random = new Random();
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureMPCHash_100Hosts_21Probes(MPCHash_100Hosts_21Probes_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @State(Scope.Benchmark)
  public static class ConsistentHashRing_10Hosts_100PointsPerHost_State {
    Ring<URI> _ring = new ConsistentHashRing<URI>(buildPointsMap(10, 100));
    Random _random = new Random();
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureonsistentHashRing_10Hosts_100PointsPerHost(ConsistentHashRing_10Hosts_100PointsPerHost_State state) {
    return state._ring.get(state._random.nextInt());
  }


  @State(Scope.Benchmark)
  public static class ConsistentHashRing_100Hosts_100PointsPerHost_State {
    Ring<URI> _ring = new ConsistentHashRing<URI>(buildPointsMap(100, 100));
    Random _random = new Random();
  }

  @Benchmark
  @BenchmarkMode(Mode.SampleTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureonsistentHashRing_100Hosts_100PointsPerHost(ConsistentHashRing_100Hosts_100PointsPerHost_State state) {
    return state._ring.get(state._random.nextInt());
  }

  private static Map<URI, Integer> buildPointsMap(int numHosts, int numPointsPerHost) {
    return IntStream.range(0, numHosts).boxed().collect(
        Collectors.toMap(
            key -> URI.create(String.format("app-%04d.linkedin.com", key)),
            value -> numPointsPerHost));
  }
}

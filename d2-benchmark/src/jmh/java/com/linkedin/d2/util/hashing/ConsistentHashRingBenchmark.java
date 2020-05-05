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

import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.strategies.degrader.DelegatingRingFactory;
import com.linkedin.d2.balancer.strategies.degrader.MPConsistentHashRingFactory;
import com.linkedin.d2.balancer.strategies.degrader.RingFactory;
import com.linkedin.d2.balancer.util.hashing.BoundedLoadConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.MPConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.util.degrader.CallTracker;
import com.linkedin.util.degrader.CallTrackerImpl;
import com.linkedin.util.degrader.DegraderImpl;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
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
    Ring<URI> _ring = new MPConsistentHashRing<>(buildPointsMap(10, 100), 11, 1);
    Random _random = new Random();
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureMPCHash_10Hosts_11Probes(MPCHash_10Hosts_11Probes_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @State(Scope.Benchmark)
  public static class BoundedLoad_MPCHash_10Hosts_11Probes_State {
    RingFactory<URI> factory = new MPConsistentHashRingFactory<>(11, 1);
    Map<URI, Integer> pointsMap = buildPointsMap(10, 100);
    Ring<URI> _ring = new BoundedLoadConsistentHashRing<>(factory, pointsMap, new HashMap<>(), 1.25);
    Random _random = new Random();
    int _key = _random.nextInt();
    URI _mostWantedHost = _ring.get(_key);
    Ring<URI> _ringFull = new BoundedLoadConsistentHashRing<>(factory, pointsMap, createCallTrackerMap(_mostWantedHost, 100), 1.25);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureBoundedLoad_MPCHash_10Hosts_11Probes(BoundedLoad_MPCHash_10Hosts_11Probes_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureBoundedLoad_firstFull_MPCHash_10Hosts_11Probes(BoundedLoad_MPCHash_10Hosts_11Probes_State state) {
    return state._ringFull.get(state._key);
  }

  @State(Scope.Benchmark)
  public static class MPCHash_10Hosts_21Probes_State {
    Ring<URI> _ring = new MPConsistentHashRing<>(buildPointsMap(10, 100), 21, 1);
    Random _random = new Random();
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureMPCHash_10Hosts_21Probes(MPCHash_10Hosts_21Probes_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @State(Scope.Benchmark)
  public static class BoundedLoad_MPCHash_10Hosts_21Probes_State {
    RingFactory<URI> factory = new MPConsistentHashRingFactory<>(21, 1);
    Map<URI, Integer> pointsMap = buildPointsMap(10, 100);
    Ring<URI> _ring = new BoundedLoadConsistentHashRing<>(factory, pointsMap, new HashMap<>(), 1.25);
    Random _random = new Random();
    int _key = _random.nextInt();
    URI _mostWantedHost = _ring.get(_key);
    Ring<URI> _ringFull = new BoundedLoadConsistentHashRing<>(factory, pointsMap, createCallTrackerMap(_mostWantedHost, 100), 1.25);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureBoundedLoad_MPCHash_10Hosts_21Probes(BoundedLoad_MPCHash_10Hosts_21Probes_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureBoundedLoad_firstFull_MPCHash_10Hosts_21Probes(BoundedLoad_MPCHash_10Hosts_21Probes_State state) {
    return state._ringFull.get(state._key);
  }

  @State(Scope.Benchmark)
  public static class MPCHash_100Hosts_11Probes_State {
    Ring<URI> _ring = new MPConsistentHashRing<>(buildPointsMap(100, 100), 11, 1);
    Random _random = new Random();
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureMPCHash_100Hosts_11Probes(MPCHash_100Hosts_11Probes_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @State(Scope.Benchmark)
  public static class BoundedLoad_MPCHash_100Hosts_11Probes_State {
    RingFactory<URI> factory = new MPConsistentHashRingFactory<>(11, 1);
    Map<URI, Integer> pointsMap = buildPointsMap(100, 100);
    Ring<URI> _ring = new BoundedLoadConsistentHashRing<>(factory, pointsMap, new HashMap<>(), 1.25);
    Random _random = new Random();
    int _key = _random.nextInt();
    URI _mostWantedHost = _ring.get(_key);
    Ring<URI> _ringFull = new BoundedLoadConsistentHashRing<>(factory, pointsMap, createCallTrackerMap(_mostWantedHost, 100), 1.25);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureBoundedLoad_MPCHash_100Hosts_11Probes(BoundedLoad_MPCHash_100Hosts_11Probes_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureBoundedLoad_firstFull_MPCHash_100Hosts_11Probes(BoundedLoad_MPCHash_100Hosts_11Probes_State state) {
    return state._ringFull.get(state._key);
  }

  @State(Scope.Benchmark)
  public static class MPCHash_100Hosts_21Probes_State {
    Ring<URI> _ring = new MPConsistentHashRing<>(buildPointsMap(100, 100), 21, 1);
    Random _random = new Random();
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureMPCHash_100Hosts_21Probes(MPCHash_100Hosts_21Probes_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @State(Scope.Benchmark)
  public static class BoundedLoad_MPCHash_100Hosts_21Probes_State {
    RingFactory<URI> factory = new MPConsistentHashRingFactory<>(21, 1);
    Map<URI, Integer> pointsMap = buildPointsMap(100, 100);
    Ring<URI> _ring = new BoundedLoadConsistentHashRing<>(factory, pointsMap, new HashMap<>(), 1.25);
    Random _random = new Random();
    int _key = _random.nextInt();
    URI _mostWantedHost = _ring.get(_key);
    Ring<URI> _ringFull = new BoundedLoadConsistentHashRing<>(factory, pointsMap, createCallTrackerMap(_mostWantedHost, 100), 1.25);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureBoundedLoad_MPCHash_100Hosts_21Probes(BoundedLoad_MPCHash_100Hosts_21Probes_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureBoundedLoad_firstFull_MPCHash_100Hosts_21Probes(BoundedLoad_MPCHash_100Hosts_21Probes_State state) {
    return state._ringFull.get(state._key);
  }

  @State(Scope.Benchmark)
  public static class ConsistentHashRing_10Hosts_100PointsPerHost_State {
    Ring<URI> _ring = new ConsistentHashRing<URI>(buildPointsMap(10, 100));
    Random _random = new Random();
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureConsistentHashRing_10Hosts_100PointsPerHost(ConsistentHashRing_10Hosts_100PointsPerHost_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @State(Scope.Benchmark)
  public static class BoundedLoad_ConsistentHashRing_10Hosts_100PointsPerHost_State {
    RingFactory<URI> factory = new DelegatingRingFactory<>(getConfig("pointBased", 1, 1));
    Map<URI, Integer> pointsMap = buildPointsMap(10, 100);
    Ring<URI> _ring = new BoundedLoadConsistentHashRing<>(factory, pointsMap, new HashMap<>(), 1.25);
    Random _random = new Random();
    int _key = _random.nextInt();
    URI _mostWantedHost = _ring.get(_key);
    Ring<URI> _ringFull = new BoundedLoadConsistentHashRing<>(factory, pointsMap, createCallTrackerMap(_mostWantedHost, 100), 1.25);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureBoundedLoad_ConsistentHashRing_10Hosts_100PointsPerHost_State(BoundedLoad_ConsistentHashRing_10Hosts_100PointsPerHost_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureBoundedLoad_firstFull_ConsistentHashRing_10Hosts_100PointsPerHost_State(BoundedLoad_ConsistentHashRing_10Hosts_100PointsPerHost_State state) {
    return state._ringFull.get(state._key);
  }


  @State(Scope.Benchmark)
  public static class ConsistentHashRing_100Hosts_100PointsPerHost_State {
    Ring<URI> _ring = new ConsistentHashRing<URI>(buildPointsMap(100, 100));
    Random _random = new Random();
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureConsistentHashRing_100Hosts_100PointsPerHost(ConsistentHashRing_100Hosts_100PointsPerHost_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @State(Scope.Benchmark)
  public static class BoundedLoad_ConsistentHashRing_100Hosts_100PointsPerHost_State {
    RingFactory<URI> factory = new DelegatingRingFactory<>(getConfig("pointBased", 1, 1));
    Map<URI, Integer> pointsMap = buildPointsMap(100, 100);
    Ring<URI> _ring = new BoundedLoadConsistentHashRing<>(factory, pointsMap, new HashMap<>(), 1.25);
    Random _random = new Random();
    int _key = _random.nextInt();
    URI _mostWantedHost = _ring.get(_key);
    Ring<URI> _ringFull = new BoundedLoadConsistentHashRing<>(factory, pointsMap, createCallTrackerMap(_mostWantedHost, 100), 1.25);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureBoundedLoad_ConsistentHashRing_100Hosts_100PointsPerHost_State(BoundedLoad_ConsistentHashRing_100Hosts_100PointsPerHost_State state) {
    return state._ring.get(state._random.nextInt());
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public URI measureBoundedLoad_firstFull_ConsistentHashRing_100Hosts_100PointsPerHost_State(BoundedLoad_ConsistentHashRing_100Hosts_100PointsPerHost_State state) {
    return state._ringFull.get(state._key);
  }



  private static Map<URI, Integer> buildPointsMap(int numHosts, int numPointsPerHost) {
    return IntStream.range(0, numHosts).boxed().collect(
        Collectors.toMap(
            key -> URI.create(String.format("app-%04d.linkedin.com", key)),
            value -> numPointsPerHost));
  }

  private static DegraderLoadBalancerStrategyConfig getConfig(String hashingAlgorithm, int numProbes, int pointsPerHost) {
    return new DegraderLoadBalancerStrategyConfig(
        1000, DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_ONLY_AT_INTERVAL,
        100, null, Collections.<String, Object>emptyMap(),
        DegraderLoadBalancerStrategyConfig.DEFAULT_CLOCK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_INITIAL_RECOVERY_LEVEL,
        DegraderLoadBalancerStrategyConfig.DEFAULT_RAMP_FACTOR,
        DegraderLoadBalancerStrategyConfig.DEFAULT_HIGH_WATER_MARK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_LOW_WATER_MARK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_UP,
        DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_DOWN,
        DegraderLoadBalancerStrategyConfig.DEFAULT_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_HASHRING_POINT_CLEANUP_RATE, hashingAlgorithm,
        numProbes, pointsPerHost,
        DegraderLoadBalancerStrategyConfig.DEFAULT_BOUNDED_LOAD_BALANCING_FACTOR,
        null,
        DegraderLoadBalancerStrategyConfig.DEFAULT_QUARANTINE_MAXPERCENT,
        null, null, DegraderLoadBalancerStrategyConfig.DEFAULT_QUARANTINE_METHOD,
        null, DegraderImpl.DEFAULT_LOW_LATENCY, null,
        DegraderLoadBalancerStrategyConfig.DEFAULT_LOW_EVENT_EMITTING_INTERVAL,
        DegraderLoadBalancerStrategyConfig.DEFAULT_HIGH_EVENT_EMITTING_INTERVAL,
        DegraderLoadBalancerStrategyConfig.DEFAULT_CLUSTER_NAME);
  }

  private static Map<URI, CallTracker> createCallTrackerMap(URI mostWantedHost, int load)
  {
    Map<URI, CallTracker> callTrackerMap = new HashMap<>();
    CallTracker callTracker = new CallTrackerImpl(5000L);

    IntStream.range(0, load)
        .forEach(e -> callTracker.startCall());

    callTrackerMap.put(mostWantedHost, callTracker);
    return callTrackerMap;
  }
}

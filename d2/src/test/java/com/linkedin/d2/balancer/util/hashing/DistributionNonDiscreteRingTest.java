package com.linkedin.d2.balancer.util.hashing;

import com.linkedin.d2.balancer.strategies.DistributionNonDiscreteRingFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


public class DistributionNonDiscreteRingTest {

  private double calculateStandardDeviation(List<Integer> counts) {
    int sum = 0;
    for (int count : counts) {
      sum += count;
    }
    double mean = sum / counts.size();
    double squaredSum = 0;
    for (int count : counts) {
      squaredSum += Math.pow(count - mean, 2);
    }
    double variance = squaredSum / counts.size();
    return Math.sqrt(variance);
  }

  private List<URI> addHostsToPointMap(int numHosts, int point, Map<URI, Integer> map) throws Exception {
    List<URI> hosts = new ArrayList<>();
    for (int i = 0; i < numHosts; i++) {
      URI host = new URI("http://test/" + i + "-" + point);
      map.put(host, point);
      hosts.add(host);
    }
    return hosts;
  }

  private void trial(int trialTimes, Map<URI, Integer> countsMap, Ring<URI> ring) {
    int trial = 0;
    while (trial < trialTimes) {
      trial++;
      URI host = ring.get(trial);
      int count = countsMap.get(host);
      countsMap.put(host, count + 1);
    }
  }

  @Test
  public void testEvenDistribution() throws Exception {
    int numHosts = 20;
    int trials = 100000;

    Map<URI, Integer> pointsMap = new HashMap<>();
    Map<URI, Integer> countsMap = new HashMap<>();

    List<URI> hosts = addHostsToPointMap(numHosts, 100, pointsMap);
    for (URI host : hosts) {
      countsMap.put(host, 0);
    }
    Ring<URI> ring = new DistributionNonDiscreteRingFactory<URI>().createRing(pointsMap);
    trial(trials, countsMap, ring);

    double sd = calculateStandardDeviation(new ArrayList<Integer>(countsMap.values()));
    Assert.assertTrue(sd < 0.05 * trials / numHosts);
  }

  @Test
  public void testLoadBalancingCapacity() throws Exception {
    Map<URI, Integer> pointsMap = new HashMap<>();
    Map<URI, Integer> countsMap = new HashMap<>();

    List<URI> goodHosts = addHostsToPointMap(10, 100, pointsMap);
    List<URI> averageHosts = addHostsToPointMap(10, 80, pointsMap);
    List<URI> badHosts = addHostsToPointMap(10, 40, pointsMap);

    goodHosts.forEach((host) -> {
      countsMap.put(host, 0);
    });
    averageHosts.forEach((host) -> {
      countsMap.put(host, 0);
    });
    badHosts.forEach((host) -> {
      countsMap.put(host, 0);
    });

    Ring<URI> ring = new DistributionNonDiscreteRingFactory<URI>().createRing(pointsMap);

    int trials = 100000;
    trial(trials, countsMap, ring);

    double goodAvg = goodHosts.stream().map((host) -> {
      return countsMap.get(host);
    }).reduce(0, (a, b) -> a + b) / goodHosts.size();

    double averageAvg = averageHosts.stream().map((host) -> {
      return countsMap.get(host);
    }).reduce(0, (a, b) -> a + b) / averageHosts.size();

    double badAvg = badHosts.stream().map((host) -> {
      return countsMap.get(host);
    }).reduce(0, (a, b) -> a + b) / badHosts.size();

    Assert.assertTrue(goodAvg > averageAvg);
    Assert.assertTrue(averageAvg > badAvg);
  }

  @Test
  public void testRingIterator() throws Exception {
    Map<URI, Integer> pointsMap = new HashMap<>();
    Map<URI, Integer> countsMap = new HashMap<>();

    List<URI> hosts = addHostsToPointMap(10, 100, pointsMap);
    Ring<URI> ring = new DistributionNonDiscreteRingFactory<URI>().createRing(pointsMap);

    hosts.forEach((host) -> {
      countsMap.put(host, 0);
    });

    int trial = 10000;
    while (trial > 0) {
      trial--;
      Iterator<URI> iter = ring.getIterator(0);
      URI host = iter.next();
      int count = countsMap.get(host);
      countsMap.put(host, count + 1);
    }
    int sum = countsMap.values().stream().reduce(0, (a, b) -> a + b);
    Assert.assertTrue(sum == 10000);
  }

  @Test
  public void testLowProbabilityHost() throws Exception {
    Map<URI, Integer> pointsMap = new HashMap<>();
    Map<URI, Integer> countsMap = new HashMap<>();

    List<URI> goodHosts = addHostsToPointMap(9, 100, pointsMap);
    List<URI> slowStartHost = addHostsToPointMap(1, 1, pointsMap);
    Ring<URI> ring = new DistributionNonDiscreteRingFactory<URI>().createRing(pointsMap);
    List<URI> results = new ArrayList<>();
    Iterator<URI> iter = ring.getIterator(0);
    long startTime = System.currentTimeMillis();
    while (iter.hasNext()) {
      results.add(iter.next());
    }
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    Assert.assertTrue(results.size() == 10);
  }

  @Test
  public void testEmptyMap() throws Exception {
    Map<URI, Integer> pointsMap = new HashMap<>();
    Ring<URI> ring = new DistributionNonDiscreteRingFactory<URI>().createRing(pointsMap);
    Iterator<URI> iter = ring.getIterator(0);
    Assert.assertFalse(iter.hasNext());
    Assert.assertNull(ring.get(0));
  }
 }

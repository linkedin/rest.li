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

package com.linkedin.d2.balancer.simple;

import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.properties.ClusterProperties;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.strategies.degrader.DegraderRingFactory;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Simple unit test to simulate the delay of different hosts so we can
 * observe/debug the D2 loadBalancer behavior.
 */

public class SimpleLoadBalancerDelayTest
{
  public static void main(String[] args) throws Exception
  {
    new SimpleLoadBalancerDelayTest().testLoadBalancerWithDelay();
    _log.info("Done");
  }

  private static final Logger _log = LoggerFactory.getLogger(SimpleLoadBalancerDelayTest.class);

  @Test(groups = { "small", "back-end" }, enabled = false)
  public void testLoadBalancerWithDelay() throws Exception
  {
    // Generate service, cluster and uri properties for d2
    URI uri1 = URI.create("http://test.qa1.com:1234");
    URI uri2 = URI.create("http://test.qa2.com:2345");
    URI uri3 = URI.create("http://test.qa3.com:6789");

    Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>(3);
    uriData.put(uri1, partitionData);
    uriData.put(uri2, partitionData);
    uriData.put(uri3, partitionData);

    ClusterProperties clusterProperties = new ClusterProperties("cluster-1");

    List<String> prioritizedSchemes = Collections.singletonList("http");
    ServiceProperties serviceProperties = new ServiceProperties("foo",
        "cluster-1",
        "/foo",
        Arrays.asList("degrader"),
        Collections.emptyMap(),
        null,
        null,
        prioritizedSchemes,
        null);

    UriProperties uriProperties = new UriProperties("cluster-1", uriData);

    // Create the delay generator for the uris
    URI expectedUri1 = URI.create("http://test.qa1.com:1234/foo");
    URI expectedUri2 = URI.create("http://test.qa2.com:2345/foo");
    URI expectedUri3 = URI.create("http://test.qa3.com:6789/foo");

    // Construct the delay patterns: for each URI there is a list of delays for each interval
    Map<String, List<Long>> delayMaps = new HashMap<>();
    delayMaps.put("http://test.qa1.com:1234/foo", Arrays.asList(50l, 60l, 75l, 55l, 60l, 80l, 50l));
    delayMaps.put("http://test.qa1.com:2345/foo", Arrays.asList(60l, 60l, 50l, 60l, 50l, 80l, 50l));
    delayMaps.put("http://test.qa1.com:6789/foo", Arrays.asList(80l, 3000l, 3000l, 3000l, 5000l, 80l, 50l));
    LoadBalancerSimulator.TimedValueGenerator<String> delayGenerator = new DelayValueGenerator(
            delayMaps, DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);

    // Construct the QPS generator
    LoadBalancerSimulator.QPSGenerator qpsGenerator = new ConstantQPSGenerator(1000);

    // pass all the info to the simulator
    LoadBalancerSimulator loadBalancerSimulator = new LoadBalancerSimulator(serviceProperties,
        clusterProperties, uriProperties, delayGenerator, qpsGenerator);

    // Start the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
    printStates(loadBalancerSimulator);

    // the points for uri3 should be 100
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 100);
    // the uri3 should be used around 33% of all queries. Due to the hashring variance we need
    // to check the range.
    // uri3 will be degrading further after the previous interval
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri3) <= 0.375);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri3) >= 0.295);

    // wait for 2 intervals due to call dropping involved
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri3 should be 80
    // Also if the loadbalancing strategy changed, the numbers could be lower
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 80);
    // the uri3 should be used around 28%, will be degrading further next
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri3) <= 0.32);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri3) >= 0.24);

    // continue the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri3 should be around 40
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 39);
    // the uri3 should be used around 16%, will be recovering next
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri3) <= 0.20);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri3) >= 0.12);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 3);
    printStates(loadBalancerSimulator);
    // the points for uri3 should be around 60, recovering
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 59);

    // Done. Shutdown the simulation
    loadBalancerSimulator.shutdown();
  }

  @Test(groups = { "small", "back-end" }, enabled = false)
  public void testLoadBalancerWithSlowStartClient() throws Exception
  {
    // Generate service, cluster and uri properties for d2
    URI uri1 = URI.create("http://test.qa1.com:1234");
    URI uri2 = URI.create("http://test.qa2.com:2345");
    URI uri3 = URI.create("http://test.qa3.com:6789");
    String clusterName = "cluster-2";

    Map<Integer, PartitionData> partitionData = new HashMap<>(1);
    partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<>(3);
    uriData.put(uri1, partitionData);
    uriData.put(uri2, partitionData);
    uriData.put(uri3, partitionData);

    ClusterProperties clusterProperties = new ClusterProperties(clusterName);

    List<String> prioritizedSchemes = Collections.singletonList("http");
    // enable multi-probe consistent hashing
    Map<String, Object> lbStrategyProperties = Collections.singletonMap(PropertyKeys.HTTP_LB_CONSISTENT_HASH_ALGORITHM,
        DegraderRingFactory.MULTI_PROBE_CONSISTENT_HASH);
    // set initial drop rate and slow start threshold
    Map<String, String> degraderProperties = new HashMap<>();
    degraderProperties.put(PropertyKeys.DEGRADER_INITIAL_DROP_RATE, "0.99");
    degraderProperties.put(PropertyKeys.DEGRADER_SLOW_START_THRESHOLD, "0.1");

    // constant delay generator
    LoadBalancerSimulator.TimedValueGenerator<String> delayGenerator = (uri, time, unit) -> 100l;

    // constant QPS generator
    LoadBalancerSimulator.QPSGenerator qpsGenerator = () -> 1000;

    Map<String, Object> transportClientProperties = Collections.singletonMap("DelayGenerator", delayGenerator);
    ServiceProperties serviceProperties = new ServiceProperties("foo",
        clusterName,
        "/foo",
        Arrays.asList("degrader"),
        lbStrategyProperties,
        transportClientProperties,
        degraderProperties,
        prioritizedSchemes,
        null);

    UriProperties uriProperties = new UriProperties(clusterName, uriData);

    // pass all the info to the simulator
    LoadBalancerSimulator loadBalancerSimulator = new LoadBalancerSimulator(serviceProperties,
        clusterProperties, uriProperties, delayGenerator, qpsGenerator);

    // Start the simulation, wait for 10 UPDATE_INTERVALS to make sure all uris are fully ramped.
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 20);
    printStates(loadBalancerSimulator);

    URI uri4 = URI.create("http://test.qa4.com:9876");
    uriData.put(uri4, partitionData);
    uriProperties = new UriProperties(clusterName, uriData);

    loadBalancerSimulator.updateUriProperties(uriProperties);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
    printStates(loadBalancerSimulator);

    // Create the delay generator for the uris
    URI expectedUri4 = URI.create("http://test.qa4.com:9876/foo");
    loadBalancerSimulator.getCountPercent(expectedUri4);

    // the points for uri4 should be 1 and call count percentage is 0.3%.
    double callCountPercent = loadBalancerSimulator.getCountPercent(expectedUri4);
    assertTrue(callCountPercent <= 0.006, "expected percentage is less than 0.006, actual is " + callCountPercent);

    // wait for 2 intervals due to call dropping
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);

    // the points for uri4 should be 4 and call count percentage is 1.3%
    callCountPercent = loadBalancerSimulator.getCountPercent(expectedUri4);
    assertTrue(callCountPercent <= 0.02, "expected percentage is less than 0.02, actual is " + callCountPercent);

    // wait for 2 intervals due to call dropping
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);

    // the points for uri4 should be 16 and call count percentage is 5%
    callCountPercent = loadBalancerSimulator.getCountPercent(expectedUri4);
    assertTrue(callCountPercent <= 0.07, "expected percentage is less than 0.07, actual is " + callCountPercent);
    assertTrue(callCountPercent >= 0.03, "expected percentage is larger than 0.03, actual is " + callCountPercent);

    // wait for 2 intervals due to call dropping
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);

    // the points for uri4 should be 56 and call count percentage is 16%
    callCountPercent = loadBalancerSimulator.getCountPercent(expectedUri4);
    assertTrue(callCountPercent <= 0.18, "expected percentage is less than 0.18, actual is " + callCountPercent);
    assertTrue(callCountPercent >= 0.12, "expected percentage is larger than 0.12, actual is " + callCountPercent);

    // wait for 2 intervals due to call dropping
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);

    // the points for uri4 should be 96 and call count percentage is 24%
    callCountPercent = loadBalancerSimulator.getCountPercent(expectedUri4);
    assertTrue(callCountPercent <= 0.28, "expected percentage is less than 0.26, actual is " + callCountPercent);
    assertTrue(callCountPercent >= 0.20, "expected percentage is larger than 0.22, actual is " + callCountPercent);
  }

  /**
   * Simple test to verify quarantine add/evict operations
   * @throws Exception
   */
  @Test(groups = { "small", "back-end" })
  public void loadBalancerQuarantineSmokingTest() throws Exception
  {
    String uri1 = "test.qa1.com:1234";
    String uri2 = "test.qa2.com:2345";
    List<String> uris = Arrays.asList(uri1, uri2);

    // Construct the delay patterns: for each URI there is a list of delays for each interval
    Map<String, List<Long>> delayMaps = new HashMap<>();
    delayMaps.put("test.qa1.com:1234", Arrays.asList(80l, 3000l, 3000l, 3000l, 3000l,  80l, 60l, 80l, 80l, 60l, 80l, 80l));
    delayMaps.put("test.qa2.com:2345", Arrays.asList(80l, 80l, 60l, 80l, 50l, 80l, 80l, 80l, 60l, 80l, 60l, 80l));
    LoadBalancerSimulator.TimedValueGenerator<String> delayGenerator = new DelayValueGenerator(delayMaps,
        DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);

    // Enable quarantine by setting the max percent to 0.05
    Map<String, Object> strategyProperties =
        Collections.singletonMap(PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT, 0.05);

    // Construct the QPS generator
    LoadBalancerSimulator.QPSGenerator qpsGenerator = new ConstantQPSGenerator(1000);

    // Create the simulator
    LoadBalancerSimulator loadBalancerSimulator = LoadBalancerSimulationBuilder.build(
        "cluster-1", "foo", uris, strategyProperties, null, null, delayGenerator, qpsGenerator);
    URI expectedUri1 = LoadBalancerSimulationBuilder.getExpectedUri("test.qa1.com:1234", "foo");

    // Start the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
    printStates(loadBalancerSimulator);

    // the points for uri1 should be 100
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri1) <= 0.55);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri1) >= 0.45);

    // wait for 2 intervals due to call dropping involved
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 60
    // Also if the loadbalancing strategy changed, the numbers could be lower
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 60);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri1) <= 0.65);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri1) >= 0.25);

    // continue the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 0 as it is now in quarantine
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 0);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 0);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 5);
    printStates(loadBalancerSimulator);
    // uri1 should fully recovered by now
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);
    // the points for uri3 should be around 60, recovering

    _log.info(loadBalancerSimulator.getClockedExecutor().toString());

    // Done. Shutdown the simulation
    loadBalancerSimulator.shutdown();
  }

  /**
   * quarantine shutdown operation
   * @throws Exception
   */
  @Test(groups = { "small", "back-end" }, expectedExceptions = {java.util.concurrent.RejectedExecutionException.class})
  public void loadBalancerQuarantineShutdownTest() throws Exception
  {
    String uri1 = "test.qa1.com:1234";
    String uri2 = "test.qa2.com:2345";
    List<String> uris = Arrays.asList(uri1, uri2);

    // Construct the delay patterns: for each URI there is a list of delays for each interval
    Map<String, List<Long>> delayMaps = new HashMap<>();
    delayMaps.put("test.qa1.com:1234", Arrays.asList(80l, 3000l, 3000l, 3000l, 3000l,  80l, 60l, 80l, 80l, 60l, 80l, 80l));
    delayMaps.put("test.qa2.com:2345", Arrays.asList(80l, 80l, 60l, 80l, 50l, 80l, 80l, 80l, 60l, 80l, 60l, 80l));
    LoadBalancerSimulator.TimedValueGenerator<String> delayGenerator = new DelayValueGenerator(delayMaps,
        DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);

    // Enable quarantine by setting the max percent to 0.05
    Map<String, Object> strategyProperties =
        Collections.singletonMap(PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT, 0.05);

    // Construct the QPS generator
    LoadBalancerSimulator.QPSGenerator qpsGenerator = new ConstantQPSGenerator(1000);

    // Create the simulator
    LoadBalancerSimulator loadBalancerSimulator = LoadBalancerSimulationBuilder.build(
        "cluster-1", "foo", uris, strategyProperties, null, null, delayGenerator, qpsGenerator);
    URI expectedUri1 = LoadBalancerSimulationBuilder.getExpectedUri("test.qa1.com:1234", "foo");

    // Start the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
    printStates(loadBalancerSimulator);

    // the points for uri1 should be 100
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    // wait for 2 intervals due to call dropping involved
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 60
    // Also if the loadbalancing strategy changed, the numbers could be lower
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 60);

    // continue the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 0 as it is now in quarantine
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 0);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    loadBalancerSimulator.shutdown();
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 0 as it is now in quarantine
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 0);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);

    _log.info(loadBalancerSimulator.getClockedExecutor().toString());

  }

  /**
   * Test to verify quarantine works with the default degrader step up/down
   * @throws Exception
   */
  @Test(groups = { "small", "back-end" })
  public void loadBalancerQuarantineDefaultDegraderTest() throws Exception
  {
    String uri1 = "test.qa1.com:1234";
    String uri2 = "test.qa2.com:2345";
    List<String> uris = Arrays.asList(uri1, uri2);

    // Construct the delay patterns: for each URI there is a list of delays for each interval
    Map<String, List<Long>> delayMaps = new HashMap<>();
    delayMaps.put("test.qa1.com:1234", Arrays.asList(80l, 3000l, 3000l, 3000l, 70l, 3000l, 3200l, 3400l, 80l, 60l, 80l,
        10l, 60l, 80l, 80l, 60l, 80l, 80l, 60l, 80l, 80l, 60l, 80l, 80l, 60l, 80l, 80l, 60l, 80l, 80l, 60l, 80l, 80l, 60l, 80l, 80l));
    delayMaps.put("test.qa2.com:2345", Arrays.asList(80l, 80l, 60l, 80l, 50l, 80l, 80l, 80l, 60l, 80l, 60l, 80l, 60l,
        20l, 35l, 60l, 28l, 32l, 64l, 60l, 80l, 80l, 60l, 80l, 80l, 60l, 80l, 80l, 60l, 80l, 80l, 60l, 80l, 80l, 60l, 80l, 80l));
    LoadBalancerSimulator.TimedValueGenerator<String> delayGenerator = new DelayValueGenerator(delayMaps,
        DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);

    // Enable quarantine by setting the max percent to 0.05
    Map<String, Object> strategyProperties =
        Collections.singletonMap(PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT, 0.05);

    // Construct the QPS generator
    LoadBalancerSimulator.QPSGenerator qpsGenerator = new ConstantQPSGenerator(1000);
    // use the default up/down steps for degrading/recovering
    Map<String, String> degraderProperties = new HashMap<>();
    degraderProperties.put(PropertyKeys.DEGRADER_DOWN_STEP, "0.05");
    degraderProperties.put(PropertyKeys.DEGRADER_UP_STEP, "0.2");

    // Create the simulator
    LoadBalancerSimulator loadBalancerSimulator = LoadBalancerSimulationBuilder.build(
        "cluster-1", "foo", uris, strategyProperties, null, degraderProperties, delayGenerator, qpsGenerator);
    URI expectedUri1 = LoadBalancerSimulationBuilder.getExpectedUri("test.qa1.com:1234", "foo");

    // Start the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
    printStates(loadBalancerSimulator);

    // the points for uri1 should be 100
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    // wait for 2 intervals due to call dropping involved
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 80
    // Also if the loadbalancing strategy changed, the numbers could be lower
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 80);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    assertTrue(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1) > 30);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // even recover a little, still not in quarantine yet
    assertTrue(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1) > 10);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 0);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 24);
    printStates(loadBalancerSimulator);
    // fully recovery needs much longer time
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);
    // the points for uri3 should be around 60, recovering

    _log.info(loadBalancerSimulator.getClockedExecutor().toString());

    // Done. Shutdown the simulation
    loadBalancerSimulator.shutdown();
  }

  /**
   * Simple test to verify quarantine checking at the state update phase
   * @throws Exception
   */
  @Test(groups = { "small", "back-end" })
  public void loadBalancerQuarantineCheckingTest() throws Exception
  {
    String uri1 = "test.qa1.com:1234";
    String uri2 = "test.qa2.com:2345";
    List<String> uris = Arrays.asList(uri1, uri2);

    // Construct the delay patterns: for each URI there is a list of delays for each interval
    Map<String, List<Long>> delayMaps = new HashMap<>();
    delayMaps.put("test.qa1.com:1234", Arrays.asList(1000l, 3000l, 3000l, 3000l, 3000l,  3080l, 60l, 80l, 80l, 60l, 80l, 80l));
    delayMaps.put("test.qa2.com:2345", Arrays.asList(180l, 180l, 160l, 180l, 150l, 180l, 80l, 80l, 60l, 80l, 60l, 80l));
    LoadBalancerSimulator.TimedValueGenerator<String> delayGenerator = new DelayValueGenerator(delayMaps,
        DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);

    // Enable quarantine by setting the max percent to 0.05
    Map<String, Object> strategyProperties =
        Collections.singletonMap(PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT, 0.05);

    // Construct the QPS generator
    LoadBalancerSimulator.QPSGenerator qpsGenerator = new ConstantQPSGenerator(1000);

    // Create the simulator
    LoadBalancerSimulator loadBalancerSimulator = LoadBalancerSimulationBuilder.build(
        "cluster-1", "foo", uris, strategyProperties, null, null, delayGenerator, qpsGenerator);
    URI expectedUri1 = LoadBalancerSimulationBuilder.getExpectedUri("test.qa1.com:1234", "foo");

    // Start the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
    printStates(loadBalancerSimulator);

    // the points for uri1 should be 100
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri1) <= 0.55);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri1) >= 0.45);

    // wait for 2 intervals due to call dropping involved
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 60
    // Also if the loadbalancing strategy changed, the numbers could be lower
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 60);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri1) <= 0.65);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri1) >= 0.25);

    // continue the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 4);
    printStates(loadBalancerSimulator);
    // the points for uri1 should not be 0 as quarantine is not enabled
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 1);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 6);
    printStates(loadBalancerSimulator);
    // uri1 should fully recovered by now
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);
    // the points for uri3 should be around 60, recovering

    _log.info(loadBalancerSimulator.getClockedExecutor().toString());

    // Done. Shutdown the simulation
    loadBalancerSimulator.shutdown();
  }

  /**
   * Simple test to verify quarantine add/evict operations
   * @throws Exception
   */
  @Test(groups = { "small", "back-end" })
  public void loadBalancerQuarantineWithExpBackoffTest() throws Exception
  {
    String uri1 = "test.qa1.com:1234";
    String uri2 = "test.qa2.com:2345";
    List<String> uris = Arrays.asList(uri1, uri2);

    // Construct the delay patterns: for each URI there is a list of delays for each interval
    Map<String, List<Long>> delayMaps = new HashMap<>();
    delayMaps.put("test.qa1.com:1234", Arrays.asList(80l, 3000l, 3000l, 3000l, 3000l, 3000l, 3000l, 70l, 60l, 70l,
        80l, 60l, 70l, 80l, 70l, 60l, 75l));
    delayMaps.put("test.qa2.com:2345", Arrays.asList(70l, 70l, 60l, 70l, 50l, 80l, 60l, 70l, 70l, 70l, 60l,
        70l, 60l, 75l, 90l, 70l));
    LoadBalancerSimulator.TimedValueGenerator<String> delayGenerator = new DelayValueGenerator(delayMaps,
        DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);

    // Enable quarantine by setting the max percent to 0.05
    Map<String, Object> strategyProperties =
        Collections.singletonMap(PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT, 0.05);

    // Construct the QPS generator
    LoadBalancerSimulator.QPSGenerator qpsGenerator = new ConstantQPSGenerator(1000);

    // Create the simulator
    LoadBalancerSimulator loadBalancerSimulator = LoadBalancerSimulationBuilder.build(
        "cluster-1", "foo", uris, strategyProperties, null, null, delayGenerator, qpsGenerator);
    URI expectedUri1 = LoadBalancerSimulationBuilder.getExpectedUri("test.qa1.com:1234", "foo");

    // Start the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
    printStates(loadBalancerSimulator);

    // the points for uri1 should be 100
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    // wait for 2 intervals due to call dropping involved
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 60
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 60);

    // continue the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 0 as it is now in quarantine
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 0);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    // waiting for longer time to recover
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 4);
    printStates(loadBalancerSimulator);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 0);

    // Evicted from quarantine finally. The point number should be none zero
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    assertTrue(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1) > 0);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 4);
    printStates(loadBalancerSimulator);
    // uri1 should fully recovered by now
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    // the points for uri3 should be around 60, recovering

    _log.info(loadBalancerSimulator.getClockedExecutor().toString());

    // Done. Shutdown the simulation
    loadBalancerSimulator.shutdown();
  }


  /**
   * Client is quarantined again after evicted, with longer backoff time
   * @throws Exception
   */
  @Test(groups = { "small", "back-end" })
  public void loadBalancerQuarantineReQuarantineTest() throws Exception
  {
    String uri1 = "test.qa1.com:1234";
    String uri2 = "test.qa2.com:2345";
    List<String> uris = Arrays.asList(uri1, uri2);

    // Construct the delay patterns: for each URI there is a list of delays for each interval
    Map<String, List<Long>> delayMaps = new HashMap<>();
    delayMaps.put("test.qa1.com:1234", Arrays.asList(80l, 3000l, 3000l, 3000l, 3000l, 3000l, 90l, 3150l, 3100l,
        3800l, 3150l, 90l, 80l, 90l, 60l, 65l, 80l, 20l));
    delayMaps.put("test.qa2.com:2345", Arrays.asList(90l, 90l, 60l, 90l, 50l, 80l, 60l, 90l, 90l, 90l, 60l,
        90l, 60l, 65l, 180l, 90l, 120l, 60l));
    LoadBalancerSimulator.TimedValueGenerator<String> delayGenerator = new DelayValueGenerator(delayMaps,
        DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);

    // Enable quarantine by setting the max percent to 0.05
    Map<String, Object> strategyProperties =
        Collections.singletonMap(PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT, 0.05);

    // Construct the QPS generator
    LoadBalancerSimulator.QPSGenerator qpsGenerator = new ConstantQPSGenerator(1000);

    // Create the simulator
    LoadBalancerSimulator loadBalancerSimulator = LoadBalancerSimulationBuilder.build(
        "cluster-1", "foo", uris, strategyProperties, null, null, delayGenerator, qpsGenerator);
    URI expectedUri1 = LoadBalancerSimulationBuilder.getExpectedUri("test.qa1.com:1234", "foo");

    // Start the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
    printStates(loadBalancerSimulator);

    // the points for uri1 should be 100
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    // wait for 2 intervals due to call dropping involved
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 60
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 60);

    // continue the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 0 as it is now in quarantine
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 0);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 0);

    // Evicted from quarantine finally. The point number should be none zero
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    assertTrue(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1) > 0);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // uri1 should be quarantined again
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 0);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 3);
    printStates(loadBalancerSimulator);
    // uri1 should be evicted again
    assertTrue(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1) > 0);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 4);
    printStates(loadBalancerSimulator);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);

    _log.info(loadBalancerSimulator.getClockedExecutor().toString());

    // Done. Shutdown the simulation
    loadBalancerSimulator.shutdown();
  }


  /**
   * Quaratine with long checking interval
   * @throws Exception
   */
  @Test(groups = { "small", "back-end" })
  public void loadBalancerQuarantineLongIntervalTest() throws Exception
  {
    String uri1 = "test.qa1.com:1234";
    String uri2 = "test.qa2.com:2345";
    List<String> uris = Arrays.asList(uri1, uri2);

    // Construct the delay patterns: for each URI there is a list of delays for each interval
    Map<String, List<Long>> delayMaps = new HashMap<>();
    delayMaps.put("test.qa1.com:1234", Arrays.asList(80l, 3000l, 3000l, 3000l, 3000l,  60l, 65l, 60l, 80l, 65l, 60l, 80l));
    delayMaps.put("test.qa2.com:2345", Arrays.asList(60l, 60l, 65l, 60l, 50l, 80l, 60l, 60l, 65l, 60l, 65l, 60l));
    LoadBalancerSimulator.TimedValueGenerator<String> delayGenerator = new DelayValueGenerator(delayMaps,
        DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);

    // Enable quarantine by setting the max percent to 0.05
    Map<String, Object> strategyProperties =
        Collections.singletonMap(PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT, 0.05);
    // check interval is 1000ms, so 10 checks will span across 2 check intervals
    // strategyProperties.put(PropertyKeys.HTTP_LB_QUARANTINE_CHECK_INTERVAL, "1000");

    // Construct the QPS generator
    LoadBalancerSimulator.QPSGenerator qpsGenerator = new ConstantQPSGenerator(1000);

    // Create the simulator
    LoadBalancerSimulator loadBalancerSimulator = LoadBalancerSimulationBuilder.build(
        "cluster-1", "foo", uris, strategyProperties, null, null, delayGenerator, qpsGenerator);
    URI expectedUri1 = LoadBalancerSimulationBuilder.getExpectedUri("test.qa1.com:1234", "foo");

    // Start the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 100
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri1) <= 0.55);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri1) >= 0.45);

    // wait for 2 intervals due to call dropping involved
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 60
    // Also if the loadbalancing strategy changed, the numbers could be lower
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 60);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri1) <= 0.65);
    // assertTrue(loadBalancerSimulator.getCountPercent(expectedUri1) >= 0.25);

    // continue the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 0 as it is now in quarantine
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 0);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 0);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 7);
    printStates(loadBalancerSimulator);
    // uri1 should fully recovered by now
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);
    // the points for uri3 should be around 60, recovering

    _log.info(loadBalancerSimulator.getClockedExecutor().toString());

    // Done. Shutdown the simulation
    loadBalancerSimulator.shutdown();
  }


  /**
   * Quarantine with CAP: no more client can be added when CAP reached
   * @throws Exception
   */
  @Test(groups = { "small", "back-end" })
  public void loadBalancerQuarantineMaxNumTest() throws Exception
  {
    String uri1 = "test.qa1.com:1234";
    String uri2 = "test.qa2.com:2345";
    String uri3 = "test.qa3.com:6789";
    List<String> uris = Arrays.asList(uri1, uri2, uri3);

    // Construct the delay patterns: for each URI there is a list of delays for each interval
    Map<String, List<Long>> delayMaps = new HashMap<>();
    delayMaps.put("test.qa1.com:1234", Arrays.asList(80l, 3000l, 3000l, 3000l, 3000l,  90l, 75l, 90l, 80l, 75l, 90l, 80l));
    delayMaps.put("test.qa2.com:2345", Arrays.asList(90l, 90l, 75l, 90l, 50l, 80l, 90l, 90l, 75l, 90l, 75l, 90l));
    delayMaps.put("test.qa3.com:6789", Arrays.asList(80l, 3000l, 3000l, 3000l, 3000l,  90l, 75l, 90l, 80l, 75l, 90l, 80l));
    LoadBalancerSimulator.TimedValueGenerator<String> delayGenerator = new DelayValueGenerator(delayMaps,
        DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);

    // Enable quarantine by setting the max percent to 0.05
    Map<String, Object> strategyProperties =
        Collections.singletonMap(PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT, 0.05);

    // Construct the QPS generator
    LoadBalancerSimulator.QPSGenerator qpsGenerator = new ConstantQPSGenerator(1000);

    // Create the simulator
    LoadBalancerSimulator loadBalancerSimulator = LoadBalancerSimulationBuilder.build(
        "cluster-1", "foo", uris, strategyProperties, null, null, delayGenerator, qpsGenerator);
    URI expectedUri1 = LoadBalancerSimulationBuilder.getExpectedUri("test.qa1.com:1234", "foo");

    // Start the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 100
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 100);

    // wait for 2 intervals due to call dropping involved
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri1/uri3 should be 60
    // Also if the loadbalancing strategy changed, the numbers could be lower
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 60);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 60);

    // continue the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri3 should be 0 as it is now in quarantine, uri1 should not be 0
    assertTrue(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1) > 0);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 0);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 0);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 7);
    printStates(loadBalancerSimulator);
    // uri1/uri3 should fully recovered by now
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 100);
    // the points for uri3 should be around 60, recovering

    _log.info(loadBalancerSimulator.getClockedExecutor().toString());

    // Done. Shutdown the simulation
    loadBalancerSimulator.shutdown();
  }

  /**
   * When quarantine is full, the rest of the clients should degrading just as no quarantine presents
   * @throws Exception
   */
  @Test(groups = { "small", "back-end" })
  public void loadBalancerQuarantineMixTest() throws Exception
  {
    String uri1 = "test.qa1.com:1234";
    String uri2 = "test.qa2.com:2345";
    String uri3 = "test.qa3.com:6789";
    List<String> uris = Arrays.asList(uri1, uri2, uri3);

    // Construct the delay patterns: for each URI there is a list of delays for each interval
    Map<String, List<Long>> delayMaps = new HashMap<>();
    delayMaps.put("test.qa1.com:1234", Arrays.asList(80l, 3000l, 3000l, 3000l, 3000l, 3090l, 3075l, 90l, 80l, 75l, 90l, 80l, 20l, 60l, 85l, 60l));
    delayMaps.put("test.qa2.com:2345", Arrays.asList(90l, 90l, 75l, 90l, 50l, 80l, 90l, 90l, 75l, 90l, 75l, 90l, 50l, 60l, 20l, 80l, 60l, 20l));
    delayMaps.put("test.qa3.com:6789", Arrays.asList(80l, 3000l, 3000l, 3000l, 3000l, 90l, 75l, 90l, 80l, 75l, 90l, 80l, 200l, 50l, 60l, 85l, 50l));
    LoadBalancerSimulator.TimedValueGenerator<String> delayGenerator = new DelayValueGenerator(delayMaps,
        DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);

    // Enable quarantine by setting the max percent to 0.05
    Map<String, Object> strategyProperties =
        Collections.singletonMap(PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT, 0.05);

    // Construct the QPS generator
    LoadBalancerSimulator.QPSGenerator qpsGenerator = new ConstantQPSGenerator(1000);

    // Create the simulator
    LoadBalancerSimulator loadBalancerSimulator = LoadBalancerSimulationBuilder.build(
        "cluster-1", "foo", uris, strategyProperties, null, null, delayGenerator, qpsGenerator);
    URI expectedUri1 = LoadBalancerSimulationBuilder.getExpectedUri("test.qa1.com:1234", "foo");

    // Start the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
    printStates(loadBalancerSimulator);
    // the points for uri1 should be 100
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri2), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 100);

    // wait for 2 intervals due to call dropping involved
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri1/uri3 should be 60
    // Also if the loadbalancing strategy changed, the numbers could be lower
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 60);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 60);

    // continue the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri3 should be 0 as it is now in quarantine, uri1 should not be 0
    assertTrue(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1) > 0);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 0);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // uri1 points in minimal (1)
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 1);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // uri 3 evicted, uri 1 in quarantine
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 0);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 1);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 7);
    printStates(loadBalancerSimulator);
    // uri1/uri3 should fully recovered by now
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri1), 100);
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 100);

    _log.info(loadBalancerSimulator.getClockedExecutor().toString());

    // Done. Shutdown the simulation
    loadBalancerSimulator.shutdown();
  }

    /**
     * LoadBalancerSimulationBuilder buildup the LoadBalancerSimulator
     */
  private static class LoadBalancerSimulationBuilder
  {
    public static LoadBalancerSimulator build(
        String clusterName,
        String serviceName,
        List<String> uris,
        Map<String, Object> strategyProperties,
        Map<String, Object> transportProperties,
        Map<String, String> degraderProperties,
        LoadBalancerSimulator.TimedValueGenerator<String> delayGenerator,
        LoadBalancerSimulator.QPSGenerator qpsGenerator)
        throws InterruptedException, ExecutionException
    {
      // only support 1 partition for now
      Map<Integer, PartitionData> partitionData = new HashMap<Integer, PartitionData>(1);
      partitionData.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
      Map<URI, Map<Integer, PartitionData>> uriData = new HashMap<URI, Map<Integer, PartitionData>>(2);

      for (String uriString : uris)
      {
        URI uri = URI.create("http://" + uriString);
        uriData.put(uri, partitionData);
      }
      UriProperties uriProperties = new UriProperties(clusterName, uriData);

      ClusterProperties clusterProperties = new ClusterProperties(clusterName);
      // Enable quarantine by setting the weight > 0
      if (strategyProperties == null)
      {
        strategyProperties = new HashMap<>();
      }
      if (transportProperties == null)
      {
        transportProperties = new HashMap<>();
      }
      transportProperties.put("DelayGenerator", delayGenerator);

      if (degraderProperties == null)
      {
        degraderProperties = new HashMap<>();
        // set bigger up/down step for faster degrading/recovering
        degraderProperties.put(PropertyKeys.DEGRADER_DOWN_STEP, "0.4");
        degraderProperties.put(PropertyKeys.DEGRADER_UP_STEP, "0.4");
      }
      List<String> prioritizedSchemes = Collections.singletonList("http");
      ServiceProperties serviceProperties = new ServiceProperties(serviceName,
          "cluster-1",
          "/" + serviceName,
          Arrays.asList("degrader"),
          strategyProperties,
          transportProperties,
          degraderProperties,
          prioritizedSchemes,
          null);

      return new LoadBalancerSimulator(serviceProperties, clusterProperties, uriProperties,
          delayGenerator, qpsGenerator);
    }

    public static URI getExpectedUri(String uriString, String serviceName)
    {
      return URI.create("http://" + uriString + "/" + serviceName);
    }
  }

  /**
   * ListDelayGenerator generate the delay with a predefined list of delays for the given URI
   */
  private class ListDelayGenerator implements LoadBalancerSimulator.DelayGenerator<URI>
  {
    private final Map<URI, List<Long>> _delayMaps;
    private final Map<URI, Iterator<Long>> _delayPointer = new HashMap<>();

    ListDelayGenerator(Map<URI, List<Long>> delayMaps)
    {
      _delayMaps = delayMaps;
      _delayMaps.forEach((k, v) -> _delayPointer.put(k, v.iterator()));
    }

    @Override
    public long nextDelay(URI uri)
    {
      if (!_delayPointer.containsKey(uri) || !_delayPointer.get(uri).hasNext())
      {
        throw new IllegalArgumentException("No more delay");
      }
      return _delayPointer.get(uri).next();
    }
  }

  private class ConstantQPSGenerator implements LoadBalancerSimulator.QPSGenerator
  {
    private final int _qps;

    ConstantQPSGenerator(int qps)
    {
      _qps = qps;
    }

    @Override
    public int nextQPS()
    {
      return _qps;
    }
  }

  private class DelayValueGenerator implements LoadBalancerSimulator.TimedValueGenerator<String>
  {
    private final Map<String, List<Long>> _valueMap;
    private final long _intervalMilli;

    public DelayValueGenerator(Map<String, List<Long>> valueMap, long interval)
    {
      _valueMap = valueMap;
      _intervalMilli = interval;
    }

    @Override
    public long getValue(String uri, long time, TimeUnit unit)
    {
      int idx = (int) (unit.convert(time, TimeUnit.MILLISECONDS) / _intervalMilli);
      if (_valueMap.containsKey(uri))
      {
        return _valueMap.get(uri).get(idx);
      }
      else
      {
        throw new IllegalArgumentException("URI does not exist");
      }
    }
  }

  private static void printStates(LoadBalancerSimulator simulator)
  {
    Map<URI, Integer> counterMaps = simulator.getClientCounters();
    counterMaps.forEach((k,v) -> { _log.info("{} - Client {}: {}",
        new Object[] { simulator.getClock().currentTimeMillis(), k, v}); });
    Map<URI, Integer> ringMap = null;
    try
    {
       ringMap = simulator.getPoints("foo", 0);
    }
    catch (ServiceUnavailableException e)
    {
      _log.error("Service foo unavailable!" + e);
    }
    ringMap.forEach((k,v) -> { _log.info("{} - points {}: {}",
        new Object[] {simulator.getClock().currentTimeMillis(), k, v}); });
  }
}

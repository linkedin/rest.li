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
import com.linkedin.d2.balancer.properties.ServiceProperties;
import com.linkedin.d2.balancer.properties.UriProperties;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
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

  @Test(groups = { "small", "back-end" })
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
    Map<URI, List<Long>> delayMaps = new HashMap<>();
    delayMaps.put(expectedUri1, Arrays.asList(50l, 100l, 200l, 120l, 60l, 80l, 150l));
    delayMaps.put(expectedUri2, Arrays.asList(100l, 100l, 150l, 100l, 50l, 80l, 150l));
    delayMaps.put(expectedUri3, Arrays.asList(80l, 3000l, 3000l, 3000l, 5000l, 80l, 150l));
    LoadBalancerSimulator.DelayGenerator<URI> delayGenerator = new ListDelayGenerator(delayMaps);

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
    assertTrue(loadBalancerSimulator.getCountPercent(expectedUri3) <= 0.375);
    assertTrue(loadBalancerSimulator.getCountPercent(expectedUri3) >= 0.295);

    // wait for 2 intervals due to call dropping involved
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri3 should be 80
    // Also if the loadbalancing strategy changed, the numbers could be lower
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 80);
    // the uri3 should be used around 28%, will be degrading further next
    assertTrue(loadBalancerSimulator.getCountPercent(expectedUri3) <= 0.32);
    assertTrue(loadBalancerSimulator.getCountPercent(expectedUri3) >= 0.24);

    // continue the simulation
    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 2);
    printStates(loadBalancerSimulator);
    // the points for uri3 should be around 40
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 39);
    // the uri3 should be used around 16%, will be recovering next
    assertTrue(loadBalancerSimulator.getCountPercent(expectedUri3) <= 0.20);
    assertTrue(loadBalancerSimulator.getCountPercent(expectedUri3) >= 0.12);

    loadBalancerSimulator.runWait(DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS * 3);
    printStates(loadBalancerSimulator);
    // the points for uri3 should be around 60, recovering
    assertEquals(loadBalancerSimulator.getPoint("foo", DefaultPartitionAccessor.DEFAULT_PARTITION_ID,
        uri3), 59);

    // Done. Shutdown the simulation
    loadBalancerSimulator.shutdown();
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

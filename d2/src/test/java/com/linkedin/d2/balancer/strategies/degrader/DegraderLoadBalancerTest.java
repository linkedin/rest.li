/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.d2.balancer.strategies.degrader;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.KeyMapper;
import com.linkedin.d2.balancer.LoadBalancerClient;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.util.URIRequest;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing.Point;
import com.linkedin.d2.balancer.util.hashing.URIRegexHash;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SettableClock;
import com.linkedin.util.clock.SystemClock;
import com.linkedin.util.clock.Time;
import com.linkedin.util.degrader.CallCompletion;
import com.linkedin.util.degrader.DegraderControl;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class DegraderLoadBalancerTest
{
  private static final long _defaultInterval = Time.milliseconds(5000);
  private static final int DEFAULT_PARTITION_ID = DefaultPartitionAccessor.DEFAULT_PARTITION_ID;

  public static void main(String[] args) throws URISyntaxException,
      InterruptedException
  {
    DegraderLoadBalancerTest test = new DegraderLoadBalancerTest();

    test.testWeightedBalancingRing();
  }

  public static TrackerClient getTrackerClient(DegraderLoadBalancerStrategyV3 strategy,
                                               Request request,
                                               RequestContext requestContext,
                                               long clusterGenerationId,
                                               List<TrackerClient> trackerClients)
  {
    return strategy.getTrackerClient(request, requestContext, clusterGenerationId, DefaultPartitionAccessor.DEFAULT_PARTITION_ID, trackerClients);
  }

  public static Map<Integer, PartitionData> getDefaultPartitionData(double weight)
  {
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(2);
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(weight));
    return partitionDataMap;
  }

  @Test(groups = { "small", "back-end" })
  public void testBadTrackerClients() throws URISyntaxException
  {
    DegraderLoadBalancerStrategyV3 strategy = getStrategy();

    // test null twice (first time will have no state)
    for (int i = 0; i < 2; ++i)
    {
      assertNull(getTrackerClient(strategy, null, new RequestContext(), 0, null));
    }

    strategy = getStrategy();

    // test empty twice (first time will have no state)
    for (int i = 0; i < 2; ++i)
    {
      assertNull(getTrackerClient(strategy, null, new RequestContext(), 0, new ArrayList<TrackerClient>()));
    }

    // test same cluster generation id but different client lists
    strategy = getStrategy();
    List<TrackerClient> clients1 = new ArrayList<TrackerClient>();
    SettableClock clock1 = new SettableClock();
    SettableClock clock2 = new SettableClock();
    List<TrackerClient> clients2 = new ArrayList<TrackerClient>();
    SettableClock clock3 = new SettableClock();
    SettableClock clock4 = new SettableClock();

    clients1.add(getClient(URI.create("http://test.linkedin.com:3242/fdsaf"), clock1));
    clients1.add(getClient(URI.create("http://test.linkedin.com:3243/fdsaf"), clock2));

    clients2.add(getClient(URI.create("http://asdbasdf.com:3242/fdsaf"), clock3));
    clients2.add(getClient(URI.create("ftp://example.com:21/what"), clock4));

    // same cluster generation id but different tracker clients
    assertNotNull(getTrackerClient(strategy, null, new RequestContext(), 0, clients2));
    assertNull(getTrackerClient(strategy, null, new RequestContext(), 0, clients1));

    // now trigger an update by cluster generation id
    assertNotNull(getTrackerClient(strategy, null, new RequestContext(), 1, clients1));
    assertNull(getTrackerClient(strategy, null, new RequestContext(), 1, clients2));
  }

  @Test(groups = { "small", "back-end" })
  public void testStateIsNotNullAndCallCountIsZero() throws URISyntaxException
  {
    DegraderLoadBalancerStrategyV3 strategy =
        new DegraderLoadBalancerStrategyV3(new DegraderLoadBalancerStrategyConfig(5000,
                                                                                Double.MAX_VALUE));
    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    SettableClock clock1 = new SettableClock();
    SettableClock clock2 = new SettableClock();

    clients.add(getClient(URI.create("http://test.linkedin.com:3242/fdsaf"), clock1));
    clients.add(getClient(URI.create("http://test.linkedin.com:3243/fdsaf"), clock2));

    clock1.addDuration(5000);

    // state is not null and call count is 0
    assertTrue(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(0,
        strategy.getState().getPartitionState(DEFAULT_PARTITION_ID),strategy.getConfig(), true));

    // this should trigger setting _state (generation id is different) with an override
    // of 0d
    getTrackerClient(strategy, null, new RequestContext(), 0, clients);

    // should not have overridden anything, and default is 0
    for (TrackerClient client : clients)
    {
      assertEquals(client.getDegraderControl(DEFAULT_PARTITION_ID).getOverrideDropRate(), 0d);
    }

    // this should trigger setting _state (state is null and count > 0) with an override
    // of 0d
    assertNotNull(getTrackerClient(strategy, null, new RequestContext(), -1, clients));
  }

  @Test(groups = { "small", "back-end" })
  public void testStateIsNullAndCallCountIsGreaterThanZero() throws URISyntaxException,
      InterruptedException
  {
    // check for average cluster latency < max latency
    // max so we don't time out from lag on testing machine
    DegraderLoadBalancerStrategyV3 strategy =
        new DegraderLoadBalancerStrategyV3(new DegraderLoadBalancerStrategyConfig(5000,
                                                                                Double.MAX_VALUE));
    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    TestClock clock1 = new TestClock();
    TestClock clock2 = new TestClock();

    clients.add(getClient(URI.create("http://test.linkedin.com:3242/fdsaf"), clock1));
    clients.add(getClient(URI.create("http://test.linkedin.com:3243/fdsaf"), clock2));

    for (int i = 0; i < 1000; ++i)
    {
      clients.get(i % 2).getCallTracker().startCall().endCall();
    }

    clock1.addMs(5000);

    // state is null and call count is > 0
    assertTrue(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(-1,
        strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));

    // this should trigger setting _state (state is null and count > 0) with an override
    // of 0d
    getTrackerClient(strategy, null, new RequestContext(), -1, clients);

    for (TrackerClient client : clients)
    {
      assertEquals(client.getDegraderControl(DEFAULT_PARTITION_ID).getOverrideDropRate(), 0d);
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testStateIsNullAndCallCountIsGreaterThanZeroWithLatency() throws URISyntaxException,
      InterruptedException
  {
    // check for average cluster latency < max latency
    // max so we don't time out from lag on testing machine
    DegraderLoadBalancerStrategyV3 strategy =
        new DegraderLoadBalancerStrategyV3(new DegraderLoadBalancerStrategyConfig(5000, -1d));
    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    TestClock clock1 = new TestClock();
    TestClock clock2 = new TestClock();

    clients.add(getClient(URI.create("http://test.linkedin.com:3242/fdsaf"), clock1));
    clients.add(getClient(URI.create("http://test.linkedin.com:3243/fdsaf"), clock2));

    for (int i = 0; i < 1000; ++i)
    {
      clients.get(i % 2).getCallTracker().startCall().endCall();
    }

    clock1.addMs(5000);

    // state is null and call count is > 0
    assertTrue(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(-1,
        strategy.getState().getPartitionState(DEFAULT_PARTITION_ID),strategy.getConfig(), true));

    // this should trigger setting _state (state is null and count > 0)
    getTrackerClient(strategy, null, new RequestContext(), -1, clients);

    // we should not have set the overrideDropRate here, since we only adjust
    // either the state or the global overrideDropRate. Since the state was null,
    // we chose to initialize the state first.
    for (TrackerClient client : clients)
    {
      assertEquals(client.getDegraderControl(DEFAULT_PARTITION_ID).getOverrideDropRate(), 0.0);
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testDropDueToDegrader() throws URISyntaxException
  {
    DegraderLoadBalancerStrategyV3 strategy = getStrategy();
    List<TrackerClient> clients = new ArrayList<TrackerClient>();

    clients.add(getClient(URI.create("http://test.linkedin.com:3242/fdsaf"), new TestClock()));
    clients.add(getClient(URI.create("http://test.linkedin.com:3243/fdsaf"), new TestClock()));

    // first verify that we're getting clients
    assertNotNull(getTrackerClient(strategy, null, new RequestContext(), 0, clients));

    assertFalse(clients.get(0).getDegrader(DEFAULT_PARTITION_ID).checkDrop());
    assertFalse(clients.get(1).getDegrader(DEFAULT_PARTITION_ID).checkDrop());

    // now force drop rate to 100% for entire cluster
    DegraderLoadBalancerStrategyV3.overrideClusterDropRate(DEFAULT_PARTITION_ID, 1d, clients);

    // now verify that everything is dropping
    assertNull(getTrackerClient(strategy, null, new RequestContext(), 1, clients));

    assertTrue(clients.get(0).getDegrader(DEFAULT_PARTITION_ID).checkDrop());
    assertTrue(clients.get(1).getDegrader(DEFAULT_PARTITION_ID).checkDrop());
  }

  @Test(groups = { "small", "back-end" })
  public void testRandom() throws URISyntaxException
  {
    DegraderLoadBalancerStrategyV3 strategy = getStrategy();
    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");
    URI uri2 = URI.create("http://test.linkedin.com:3243/fdsaf");

    clients.add(getClient(uri1, new TestClock()));
    clients.add(getClient(uri2, new TestClock()));

    // since cluster call count is 0, we will default to random
    for (int i = 0; i < 1000; ++i)
    {
      TrackerClient client = getTrackerClient(strategy, null, new RequestContext(), 0, clients);

      assertNotNull(client);
      assertTrue(clients.contains(client));
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testOneTrackerClient() throws URISyntaxException
  {
    DegraderLoadBalancerStrategyV3 strategy = getStrategy();
    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");

    clients.add(getClient(uri1, new TestClock()));

    // should always get the only client in the list
    for (int i = 0; i < 1000; ++i)
    {
      assertEquals(getTrackerClient(strategy, null, new RequestContext(), 0, clients), clients.get(0));
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testOneTrackerClientForPartition() throws URISyntaxException
  {
    DegraderLoadBalancerStrategyV3 strategy = getStrategy();
    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");
    Map<Integer, PartitionData> weightMap = new HashMap<Integer, PartitionData>();
    weightMap.put(0, new PartitionData(1d));
    TrackerClient client = new TrackerClient(uri1,
        weightMap,
        new TestLoadBalancerClient(uri1),
        new TestClock());

    clients.add(client);

    // should always get the only client in the list
    for (int i = 0; i < 1000; ++i)
    {
      assertEquals(strategy.getTrackerClient(null, new RequestContext(), 0, 0, clients), client);
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testWeightedBalancingWithDeadClient() throws URISyntaxException
  {
    Map<String,Object> myMap = new HashMap<String, Object>();
    myMap.put("updateIntervalMs", 5000L);
    myMap.put("maxClusterLatencyWithoutDegrading", 100.0);
    // this test expected the dead tracker client to not recover through the
    // getTrackerClient mechanism. It only recovered through explicit calls to client1/client2.
    // While we have fixed this problem, keeping this testcase to show how we can completely disable
    // a tracker client through the getTrackerClient method.
    myMap.put("initialRecoveryLevel", 0.0);
    DegraderLoadBalancerStrategyV3 strategy = getStrategy(myMap);
    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");
    URI uri2 = URI.create("http://test.linkedin.com:3243/fdsaf");
    TestClock clock1 = new TestClock();
    TestClock clock2 = new TestClock();
    TrackerClient client1 = getClient(uri1, clock1);
    TrackerClient client2 = getClient(uri2, clock2);

    clients.add(client1);
    clients.add(client2);

    // force client2 to be disabled
    DegraderControl dcClient2Default = client2.getDegraderControl(DEFAULT_PARTITION_ID);
    dcClient2Default.setMinCallCount(1);
    dcClient2Default.setOverrideMinCallCount(1);
    dcClient2Default.setMaxDropRate(1d);
    dcClient2Default.setUpStep(1d);
    dcClient2Default.setHighErrorRate(0);
    CallCompletion cc = client2.getCallTracker().startCall();
    clock2.addMs(10000);
    cc.endCallWithError();

    clock1.addMs(15000);
    clock2.addMs(5000);

    System.err.println(dcClient2Default.getCurrentComputedDropRate());
    System.err.println(dcClient2Default.getCurrentComputedDropRate());

    // now verify that we only get client1
    for (int i = 0; i < 1000; ++i)
    {
      assertEquals(getTrackerClient(strategy, null, new RequestContext(), 0, clients), client1);
    }

    // now force client1 to be disabled
    DegraderControl dcClient1Default = client1.getDegraderControl(DEFAULT_PARTITION_ID);
    dcClient1Default.setMinCallCount(1);
    dcClient1Default.setOverrideMinCallCount(1);
    dcClient1Default.setMaxDropRate(1d);
    dcClient1Default.setUpStep(1d);
    dcClient1Default.setHighErrorRate(0);
    cc = client1.getCallTracker().startCall();
    clock1.addMs(10000);
    cc.endCallWithError();

    clock1.addMs(5000);

    // now verify that we never get a client back
    for (int i = 0; i < 1000; ++i)
    {
      assertNull(getTrackerClient(strategy, null, new RequestContext(), 1, clients));
    }

    // now enable client1 and client2
    clock1.addMs(15000);
    clock2.addMs(15000);
    client1.getCallTracker().startCall().endCall();
    client2.getCallTracker().startCall().endCall();
    clock1.addMs(5000);
    clock2.addMs(5000);

    // now verify that we get client 1 or 2
    for (int i = 0; i < 1000; ++i)
    {
      assertTrue(clients.contains(getTrackerClient(strategy, null, new RequestContext(), 2, clients)));
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testWeightedBalancingRing() throws URISyntaxException
  {
    DegraderLoadBalancerStrategyV3 strategy = getStrategy();
    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");
    URI uri2 = URI.create("http://test.linkedin.com:3243/fdsaf");
    TestClock clock1 = new TestClock();
    TestClock clock2 = new TestClock();
    TrackerClient client1 =
        new TrackerClient(uri1, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri1), clock1);
    TrackerClient client2 =
        new TrackerClient(uri2, getDefaultPartitionData(0.8d), new TestLoadBalancerClient(uri2), clock2);

    clients.add(client1);
    clients.add(client2);

    System.err.println(client2.getDegraderControl(DEFAULT_PARTITION_ID).getCurrentComputedDropRate());
    System.err.println(client1.getDegraderControl(DEFAULT_PARTITION_ID).getCurrentComputedDropRate());

    // trigger a state update
    assertNotNull(getTrackerClient(strategy, null, new RequestContext(), 1, clients));

    // now verify that the ring has degraded client 2 by 20%
    ConsistentHashRing<URI> ring =
        (ConsistentHashRing<URI>) strategy.getState().getPartitionState(DEFAULT_PARTITION_ID).getRing();

    Map<URI, AtomicInteger> count = new HashMap<URI, AtomicInteger>();

    count.put(uri1, new AtomicInteger(0));
    count.put(uri2, new AtomicInteger(0));

    for (Point<URI> point : ring.getPoints())
    {
      count.get(point.getT()).incrementAndGet();
    }

    // .8 weight should degrade the weight of client2 by 20%
    assertEquals(count.get(uri1).get(), 100);
    assertEquals(count.get(uri2).get(), 80);

    // now do a basic verification to verify getTrackerClient is properly weighting things
    double calls = 10000d;
    int client1Count = 0;
    int client2Count = 0;
    double tolerance = 0.05d;

    for (int i = 0; i < calls; ++i)
    {
      TrackerClient client = getTrackerClient(strategy, null, new RequestContext(), 1, clients);

      assertNotNull(client);

      if (client.getUri().equals(uri1))
      {
        ++client1Count;
      }
      else
      {
        ++client2Count;
      }
    }

    assertTrue(Math.abs((client1Count / calls) - (100 / 180d)) < tolerance);
    assertTrue(Math.abs((client2Count / calls) - (80 / 180d)) < tolerance);
  }

  @Test(groups = { "small", "back-end" })
  public void testBalancingRing() throws URISyntaxException
  {
    DegraderLoadBalancerStrategyV3 strategy = getStrategy();
    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://someTestService/someTestUrl");
    URI uri2 = URI.create("http://abcxfweuoeueoueoueoukeueoueoueoueoueouo/2354");
    TestClock clock1 = new TestClock();
    TestClock clock2 = new TestClock();
    TrackerClient client1 = getClient(uri1, clock1);
    TrackerClient client2 = getClient(uri2, clock2);

    clients.add(client1);
    clients.add(client2);

    // force client2 to be disabled
    DegraderControl dcClient2Default = client2.getDegraderControl(DEFAULT_PARTITION_ID);
    dcClient2Default.setOverrideMinCallCount(1);
    dcClient2Default.setMinCallCount(1);
    dcClient2Default.setMaxDropRate(1d);
    dcClient2Default.setUpStep(0.4d);
    dcClient2Default.setHighErrorRate(0);
    CallCompletion cc = client2.getCallTracker().startCall();
    clock2.addMs(1);
    cc.endCallWithError();

    clock1.addMs(15000);
    clock2.addMs(5000);

    System.err.println(dcClient2Default.getCurrentComputedDropRate());
    System.err.println(client1.getDegraderControl(DEFAULT_PARTITION_ID).getCurrentComputedDropRate());

    // trigger a state update
    assertNotNull(getTrackerClient(strategy, null, new RequestContext(), 1, clients));

    // now verify that the ring has degraded client 2 by 20%
    ConsistentHashRing<URI> ring =
        (ConsistentHashRing<URI>) strategy.getState().getPartitionState(DEFAULT_PARTITION_ID).getRing();

    Map<URI, AtomicInteger> count = new HashMap<URI, AtomicInteger>();

    count.put(uri1, new AtomicInteger(0));
    count.put(uri2, new AtomicInteger(0));

    for (Point<URI> point : ring.getPoints())
    {
      count.get(point.getT()).incrementAndGet();
    }

    // .4 degradation should degrade the weight of client2 by 40%
    assertEquals(count.get(uri1).get(), 100);
    assertEquals(count.get(uri2).get(), 60);

    // now do a basic verification to verify getTrackerClient is properly weighting things
    double calls = 10000d;
    int client1Count = 0;
    int client2Count = 0;
    double tolerance = 0.05d;

    for (int i = 0; i < calls; ++i)
    {
      TrackerClient client = getTrackerClient(strategy, null, new RequestContext(), 1, clients);

      assertNotNull(client);

      if (client.getUri().equals(uri1))
      {
        ++client1Count;
      }
      else
      {
        ++client2Count;
      }
    }

    assertTrue(Math.abs((client1Count / calls) - (100 / 160d)) < tolerance);
    assertTrue(Math.abs((client2Count / calls) - (60 / 160d)) < tolerance);
  }

  @Test(groups = { "small", "back-end" })
  public void testWeightedAndLatencyDegradationBalancingRingWithPartitions() throws URISyntaxException
  {
    DegraderLoadBalancerStrategyV3 strategy = getStrategy();

    List<TrackerClient> clientsForPartition0 = new ArrayList<TrackerClient>();
    List<TrackerClient> clientsForPartition1 = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://someTestService/someTestUrl");
    URI uri2 = URI.create("http://abcxfweuoeueoueoueoukeueoueoueoueoueouo/2354");
    URI uri3 = URI.create("http://slashdot/blah");
    URI uri4 = URI.create("http://idle/server");
    TestClock clock1 = new TestClock();
    TestClock clock2 = new TestClock();
    TestClock clock3 = new TestClock();


    @SuppressWarnings("serial")
    TrackerClient client1 =  new TrackerClient(uri1,
        new HashMap<Integer, PartitionData>(){{put(0, new PartitionData(1d));}},
        new TestLoadBalancerClient(uri1), clock1);
    @SuppressWarnings("serial")
    TrackerClient client2 =  new TrackerClient(uri2,
        new HashMap<Integer, PartitionData>(){{put(0, new PartitionData(0.5d)); put(1, new PartitionData(0.5d));}},
        new TestLoadBalancerClient(uri2), clock2);
    @SuppressWarnings("serial")
    TrackerClient client3 =  new TrackerClient(uri3,
        new HashMap<Integer, PartitionData>(){{put(1, new PartitionData(1d));}},
        new TestLoadBalancerClient(uri3), clock3);


    final int partitionId0 = 0;
    clientsForPartition0.add(client1);
    clientsForPartition0.add(client2);

    final int partitionId1 = 1;
    clientsForPartition1.add(client2);
    clientsForPartition1.add(client3);

    // force client2 to be disabled
    DegraderControl dcClient2Partition0 = client2.getDegraderControl(0);
    DegraderControl dcClient2Partition1 = client2.getDegraderControl(1);
    dcClient2Partition0.setOverrideMinCallCount(1);
    dcClient2Partition0.setMinCallCount(1);
    dcClient2Partition0.setMaxDropRate(1d);
    dcClient2Partition0.setUpStep(0.4d);
    dcClient2Partition0.setHighErrorRate(0);

    dcClient2Partition1.setOverrideMinCallCount(1);
    dcClient2Partition1.setMinCallCount(1);
    dcClient2Partition1.setMaxDropRate(1d);
    dcClient2Partition1.setUpStep(0.4d);
    dcClient2Partition1.setHighErrorRate(0);
    CallCompletion cc = client2.getCallTracker().startCall();
    clock2.addMs(1);
    cc.endCallWithError();

    // force client3 to be disabled
    DegraderControl dcClient3Partition1 = client3.getDegraderControl(1);
    dcClient3Partition1.setOverrideMinCallCount(1);
    dcClient3Partition1.setMinCallCount(1);
    dcClient3Partition1.setMaxDropRate(1d);
    dcClient3Partition1.setHighErrorRate(0);
    dcClient3Partition1.setUpStep(0.2d);
    CallCompletion cc3 = client3.getCallTracker().startCall();
    clock3.addMs(1);
    cc3.endCallWithError();

    clock1.addMs(15000);
    clock2.addMs(5000);
    clock3.addMs(5000);

    // trigger a state update
    assertNotNull(strategy.getTrackerClient(null, new RequestContext(), 1, partitionId0, clientsForPartition0));
    assertNotNull(strategy.getTrackerClient(null, new RequestContext(), 1, partitionId1, clientsForPartition1));
    assertNotNull(strategy.getRing(1,partitionId0, clientsForPartition0));
    assertNotNull(strategy.getRing(1, partitionId1, clientsForPartition1));

    ConsistentHashRing<URI> ring0 =
        (ConsistentHashRing<URI>) strategy.getState().getPartitionState(partitionId0).getRing();

    ConsistentHashRing<URI> ring1 =
        (ConsistentHashRing<URI>) strategy.getState().getPartitionState(partitionId1).getRing();

    Map<URI, AtomicInteger> count0 = new HashMap<URI, AtomicInteger>();

    count0.put(uri1, new AtomicInteger(0));
    count0.put(uri2, new AtomicInteger(0));

    for (Point<URI> point : ring0.getPoints())
    {
      count0.get(point.getT()).incrementAndGet();
    }

    // .4 degradation on a .5 weighted node should degrade the weight of client2 by 30
    assertEquals(count0.get(uri1).get(), 100);
    assertEquals(count0.get(uri2).get(), 30);

    Map<URI, AtomicInteger> count1 = new HashMap<URI, AtomicInteger>();

    count1.put(uri2, new AtomicInteger(0));
    count1.put(uri3, new AtomicInteger(0));
    count1.put(uri4, new AtomicInteger(0));

    for (Point<URI> point : ring1.getPoints())
    {
      count1.get(point.getT()).incrementAndGet();
    }

    // .4 degradation on a .5 weighted node should degrade the weight of client2 by 30
    // .2 degradation on a 1 weighted node should degrade the weight of client3 by 80
    assertEquals(count1.get(uri3).get(), 80);
    assertEquals(count1.get(uri2).get(), 30);
    // uri4 should be ignored due to non-specified partition weight
    assertEquals(count1.get(uri4).get(), 0);

    // now do a basic verification to verify getTrackerClient is properly weighting things
    int calls = 10000;
    int client1Count = 0;
    int client2Count = 0;
    double tolerance = 0.05d;

    for (int i = 0; i < calls; ++i)
    {
      TrackerClient client = strategy.getTrackerClient(null, new RequestContext(), 1, partitionId0, clientsForPartition0);

      assertNotNull(client);

      if (client.getUri().equals(uri1))
      {
        ++client1Count;
      }
      else
      {
        ++client2Count;
      }
    }

    assertTrue(Math.abs((client1Count / (double)calls) - (100 / 130d)) < tolerance);
    assertTrue(Math.abs((client2Count / (double)calls) - (30 / 130d)) < tolerance);


    client2Count = 0;
    int client3Count = 0;
    int client4Count = 0;

    for (int i = 0; i < calls; ++i)
    {
      TrackerClient client = strategy.getTrackerClient(null, new RequestContext(), 1, partitionId1, clientsForPartition1);

      assertNotNull(client);

      if (client.getUri().equals(uri3))
      {
        ++client3Count;
      }
      else if (client.getUri().equals(uri2))
      {
        ++client2Count;
      }
      else
      {
        ++client4Count;
      }
    }

    assertTrue(Math.abs((client3Count / (double)calls) - (80 / 110d)) < tolerance);
    assertTrue(Math.abs((client2Count / (double)calls) - (30 / 110d)) < tolerance);
    assertTrue(client4Count == 0);
  }

  @Test(groups = { "small", "back-end" })
  public void testWeightedAndLatencyDegradationBalancingRing() throws URISyntaxException
  {
    DegraderLoadBalancerStrategyV3 strategy = getStrategy();
    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");
    URI uri2 = URI.create("http://test.linkedin.com:3243/fdsaf");
    TestClock clock1 = new TestClock();
    TestClock clock2 = new TestClock();
    TrackerClient client1 =
        new TrackerClient(uri1, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri1), clock1);
    TrackerClient client2 =
        new TrackerClient(uri2, getDefaultPartitionData(0.8d), new TestLoadBalancerClient(uri2), clock2);

    clients.add(client1);
    clients.add(client2);

    DegraderControl dcClient2Default = client2.getDegraderControl(DEFAULT_PARTITION_ID);
    dcClient2Default.setOverrideMinCallCount(1);
    dcClient2Default.setMinCallCount(1);
    dcClient2Default.setMaxDropRate(1d);
    dcClient2Default.setUpStep(0.4d);
    dcClient2Default.setHighErrorRate(0);
    CallCompletion cc = client2.getCallTracker().startCall();
    clock2.addMs(1);
    cc.endCallWithError();

    clock1.addMs(15000);
    clock2.addMs(5000);

    System.err.println(dcClient2Default.getCurrentComputedDropRate());
    System.err.println(client1.getDegraderControl(DEFAULT_PARTITION_ID).getCurrentComputedDropRate());

    // trigger a state update
    assertNotNull(getTrackerClient(strategy, null, new RequestContext(), 1, clients));

    // now verify that the ring has degraded client 2 by 20%
    ConsistentHashRing<URI> ring =
        (ConsistentHashRing<URI>) strategy.getState().getPartitionState(DEFAULT_PARTITION_ID).getRing();

    Map<URI, AtomicInteger> count = new HashMap<URI, AtomicInteger>();

    count.put(uri1, new AtomicInteger(0));
    count.put(uri2, new AtomicInteger(0));

    for (Point<URI> point : ring.getPoints())
    {
      count.get(point.getT()).incrementAndGet();
    }

    System.err.println(count);

    // .4 degradation on a .8 weighted node should degrade the weight of client2 by 48
    // points. 100 * (1 - 0.4) * 0.8 = 48
    assertEquals(count.get(uri1).get(), 100);
    assertEquals(count.get(uri2).get(), 48);

    // now do a basic verification to verify getTrackerClient is properly weighting things
    double calls = 10000d;
    int client1Count = 0;
    int client2Count = 0;
    double tolerance = 0.05d;

    for (int i = 0; i < calls; ++i)
    {
      TrackerClient client = getTrackerClient(strategy, null, new RequestContext(), 1, clients);

      assertNotNull(client);

      if (client.getUri().equals(uri1))
      {
        ++client1Count;
      }
      else
      {
        ++client2Count;
      }
    }

    assertTrue(Math.abs((client1Count / calls) - (100 / 148d)) < tolerance);
    assertTrue(Math.abs((client2Count / calls) - (48 / 148d)) < tolerance);
  }

  @Test(groups = { "small", "back-end" })
  public void testshouldUpdatePartition() throws URISyntaxException
  {
    Map<String,Object> myConfig = new HashMap<String,Object>();
    TestClock testClock = new TestClock();
    myConfig.put("clock", testClock);
    myConfig.put("updateIntervalMs", 5000L);
    myConfig.put("maxClusterLatencyWithoutDegrading", 100d);
    DegraderLoadBalancerStrategyV3 strategy = getStrategy(myConfig);
    List<TrackerClient> clients = new ArrayList<TrackerClient>();

    clients.add(getClient(URI.create("http://test.linkedin.com:3242/fdsaf")));
    clients.add(getClient(URI.create("http://test.linkedin.com:3243/fdsaf")));

    // state is default initialized, new cluster generation
    assertTrue(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(0,
        strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));

    // this will trigger setting _state
    getTrackerClient(strategy, null, new RequestContext(), 0, clients);

    // state is not null, but we're on the same cluster generation id, and 5 seconds
    // haven't gone by
    testClock.addMs(1);
    assertFalse(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(0,
        strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));

    // this will trigger setting _state (generation id changed)
    getTrackerClient(strategy, null, new RequestContext(), 1, clients);

    // state is not null, and cluster generation has not changed
    testClock.addMs(1);
    assertFalse(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(1,
        strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));

    // state is not null, and force 5s to go by with the same cluster generation id
    testClock.addMs(5000);
    assertTrue(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(-1,
        strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));

    // now try a new cluster generation id
    assertTrue(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(2,
        strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));

    // this will trigger setting _state
    getTrackerClient(strategy, null, new RequestContext(), 2, clients);

    // now try a new cluster generation id
    testClock.addMs(-10);
    assertFalse(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(2,
        strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));
  }

  @Test(groups = { "small", "back-end" })
  public void testOverrideClusterDropRate() throws URISyntaxException
  {
    DegraderLoadBalancerStrategyV3 strategy = getStrategy();
    List<TrackerClient> clients = new ArrayList<TrackerClient>();

    clients.add(getClient(URI.create("http://test.linkedin.com:3242/fdsaf")));
    clients.add(getClient(URI.create("http://test.linkedin.com:3243/fdsaf")));

    strategy.overrideClusterDropRate(DEFAULT_PARTITION_ID, 1d, clients);

    for (TrackerClient client : clients)
    {
      assertEquals(client.getDegraderControl(DEFAULT_PARTITION_ID).getOverrideDropRate(), 1d);
      assertTrue(client.getDegrader(DEFAULT_PARTITION_ID).checkDrop());
    }

    strategy.overrideClusterDropRate(DEFAULT_PARTITION_ID, -1d, clients);

    // if we don't override, the degrader isn't degraded, so should not drop
    for (TrackerClient client : clients)
    {
      assertEquals(client.getDegraderControl(DEFAULT_PARTITION_ID).getOverrideDropRate(), -1d);
      assertFalse(client.getDegrader(DEFAULT_PARTITION_ID).checkDrop());
    }

    strategy.overrideClusterDropRate(DEFAULT_PARTITION_ID, 0d, clients);

    for (TrackerClient client : clients)
    {
      assertEquals(client.getDegraderControl(DEFAULT_PARTITION_ID).getOverrideDropRate(), 0d);
      assertFalse(client.getDegrader(DEFAULT_PARTITION_ID).checkDrop());
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testRegexHashingConsistency()
  {
    final int NUM_SERVERS = 100;

    DegraderLoadBalancerStrategyV3 strategy = new DegraderLoadBalancerStrategyV3(
            new DegraderLoadBalancerStrategyConfig(
                    5000, 500, 1.0, 100, DegraderLoadBalancerStrategyV3.HASH_METHOD_URI_REGEX,
                    Collections.<String,Object>singletonMap(URIRegexHash.KEY_REGEXES,
                    Collections.singletonList("(.*)")), SystemClock.instance(),
                    DegraderLoadBalancerStrategyConfig.DEFAULT_INITIAL_RECOVERY_LEVEL,
                    DegraderLoadBalancerStrategyConfig.DEFAULT_RAMP_FACTOR,
                    DegraderLoadBalancerStrategyConfig.DEFAULT_HIGH_WATER_MARK,
                    DegraderLoadBalancerStrategyConfig.DEFAULT_LOW_WATER_MARK,
                    DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_UP,
                    DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_DOWN));
    List<TrackerClient> clients = new ArrayList<TrackerClient>(NUM_SERVERS);

    for (int i = 0; i < NUM_SERVERS; i++)
    {
      clients.add(getClient(URI.create("http://server" + i + ".testing:9876/foobar")));
    }

    final int NUM_URIS = 1000;
    final int NUM_CHECKS = 10;
    final Map<TrackerClient,Integer> serverCounts = new HashMap<TrackerClient, Integer>();

    for (int i = 0; i < NUM_URIS; i++)
    {
      URIRequest request = new URIRequest("d2://fooService/this/is/a/test/" + i);
      TrackerClient lastClient = null;
      for (int j = 0; j < NUM_CHECKS; j++)
      {
        TrackerClient client = getTrackerClient(strategy, request, new RequestContext(), 0, clients);
        assertNotNull(client);
        if (lastClient != null)
        {
          assertEquals(client, lastClient);
        }
        lastClient = client;
      }
      Integer count = serverCounts.get(lastClient);
      if (count == null)
      {
        count = 0;
      }
      serverCounts.put(lastClient, count + 1);
    }

    // TODO... should check the distribution of hits/server, should be pretty even, but how
    // even is even?  Also note this depends on pointsPerServer and other configurable parameters.

    // TODO... another test will check that when a TrackerClient is removed, the distribution
    // doesn't change too much.

  }

  @Test
  public void testTargetHostHeaderBinding()
  {
    final int NUM_SERVERS = 10;
    DegraderLoadBalancerStrategyV3 strategy = getStrategy();
    List<TrackerClient> clients = new ArrayList<TrackerClient>(NUM_SERVERS);
    for (int ii=0; ii<NUM_SERVERS; ++ii)
    {
      clients.add(getClient(URI.create("http://server" + ii + ".testing:9876/foobar")));
    }

    Map<TrackerClient, Integer> serverCounts = new HashMap<TrackerClient, Integer>();
    RestRequestBuilder builder = new RestRequestBuilder(URI.create("d2://fooservice"));
    final int NUM_REQUESTS=100;
    for (int ii=0; ii<NUM_REQUESTS; ++ii)
    {
      TrackerClient client = getTrackerClient(strategy, builder.build(), new RequestContext(), 0, clients);
      Integer count = serverCounts.get(client);
      if (count == null)
      {
        count = 0;
      }
      serverCounts.put(client, count + 1);
    }

    //First, check that requests are normally evenly distributed.
    Assert.assertEquals(serverCounts.size(), NUM_SERVERS);

    serverCounts.clear();
    RestRequest request = builder.build();

    RequestContext context = new RequestContext();
    KeyMapper.TargetHostHints.setRequestContextTargetHost(context, clients.get(0).getUri());

    for (int ii=0; ii<NUM_REQUESTS; ++ii)
    {
      TrackerClient client = getTrackerClient(strategy, request, context, 0, clients);
      Integer count = serverCounts.get(client);
      if (count == null)
      {
        count = 0;
      }
      serverCounts.put(client, count + 1);
    }

    Assert.assertEquals(serverCounts.size(), 1);
    Assert.assertEquals(serverCounts.keySet().iterator().next(), clients.get(0));
  }

  @Test
  public void testTargetHostHeaderBindingInvalidHost()
  {
    final int NUM_SERVERS = 10;
    DegraderLoadBalancerStrategyV3 strategy = getStrategy();
    List<TrackerClient> clients = new ArrayList<TrackerClient>(NUM_SERVERS);
    for (int ii=0; ii<NUM_SERVERS; ++ii)
    {
      clients.add(getClient(URI.create("http://server" + ii + ".testing:9876/foobar")));
    }

    RestRequestBuilder builder = new RestRequestBuilder(URI.create("d2://fooservice"));
    RestRequest request = builder.build();
    RequestContext context = new RequestContext();
    KeyMapper.TargetHostHints.setRequestContextTargetHost(context, URI.create("http://notinclientlist.testing:9876/foobar"));


    TrackerClient client = getTrackerClient(strategy, request, context, 0, clients);

    Assert.assertNull(client);

  }

  @Test(groups = { "small", "back-end" })
  public void testClusterRecoveryFast1TC()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    TestClock clock = new TestClock();
    myMap.put("clock", clock);
    // This recovery level will put one point into the hash ring, which is good enough to
    // send traffic to it because it is the only member of the cluster.
    myMap.put("initialRecoverLevel", 0.01);
    myMap.put("ringRampFactor", 2.0);
    myMap.put("updateIntervalMs", timeInterval);
    int stepsToFullRecovery = 0;
    clusterRecovery1TC(myMap, clock, stepsToFullRecovery, timeInterval);
  }

  @Test(groups = { "small", "back-end" })
  public void testClusterRecoverySlow1TC()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    TestClock clock = new TestClock();
    myMap.put("clock", clock);
    // We want the degrader to have one cooling off period, and then re-enter the ring. This
    myMap.put("initialRecoveryLevel", 0.005);
    myMap.put("ringRampFactor", 2.0);
    myMap.put("updateIntervalMs", timeInterval);
    // it will take two intervals for the TC to be reintroduced into the hash ring.
    int stepsToFullRecovery = 1;
    clusterRecovery1TC(myMap, clock, stepsToFullRecovery, timeInterval);
  }

  public void clusterRecovery1TC(Map<String, Object> myMap, TestClock clock,
                                 int stepsToFullRecovery, Long timeInterval)
  {
    final int NUM_CHECKS = 5;
    final Long TIME_INTERVAL = timeInterval;
    int localStepsToFullRecovery = stepsToFullRecovery;

    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.configFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategy = new DegraderLoadBalancerStrategyV3(config);

    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");
    URIRequest request = new URIRequest(uri1);

    TrackerClient client1 =
            new TrackerClient(uri1, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri1), clock);

    clients.add(client1);

    // force client1 to be disabled
    DegraderControl dcClient1Default = client1.getDegraderControl(DEFAULT_PARTITION_ID);
    dcClient1Default.setOverrideMinCallCount(5);
    dcClient1Default.setMinCallCount(5);
    dcClient1Default.setMaxDropRate(1d);
    dcClient1Default.setUpStep(1.0d);

    List<CallCompletion> ccList = new ArrayList<CallCompletion>();
    CallCompletion cc;
    for (int j = 0; j < NUM_CHECKS; j++)

    {
      cc = client1.getCallTracker().startCall();

      ccList.add(cc);
    }

    // add high latency and errors to shut off traffic to this tracker client.
    // note: the default values for highError and lowError in the degrader are 1.1,
    // which means we don't use errorRates when deciding when to lb/degrade.
    // In addition, because we changed to use the
    clock.addMs(3500);
    //for (int j = 0; j < NUM_CHECKS; j++)
    for (Iterator<CallCompletion> iter = ccList.listIterator(); iter.hasNext();)
    {
      cc = iter.next();
      cc.endCallWithError();
      iter.remove();
    }

    // go to next time interval.
    clock.addMs(TIME_INTERVAL);

    Assert.assertEquals(dcClient1Default.getCurrentComputedDropRate(), 1.0);

    // trigger a state update
    TrackerClient resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
    if (config.getInitialRecoveryLevel() < 0.01)
    {
      //the returned TrackerClient should be null
      assertNull(resultTC,"expected null trackerclient");

      // In the next time interval, the load balancer should reintroduce the TC
      // back into the ring because there was an entire time interval where no calls went to this
      // tracker client, so it's time to try it out. We need to enter this code at least once.
      do
      {
        // go to next time interval.
        clock.addMs(TIME_INTERVAL);
        // try adjusting the hash ring on this updateState
        strategy.setStrategy(DEFAULT_PARTITION_ID,
            DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE);
        resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
        localStepsToFullRecovery--;
      }
      while (localStepsToFullRecovery > 0);
    }
    assertNotNull(resultTC,"expected non-null trackerclient");

    // make calls to the tracker client to verify that it's on the road to healthy status.
    for (int j = 0; j < NUM_CHECKS; j++)
    {
      cc = resultTC.getCallTracker().startCall();
      ccList.add(cc);
    }

    clock.addMs(10);

    for (Iterator<CallCompletion> iter = ccList.listIterator(); iter.hasNext();)
    {
      cc = iter.next();
      cc.endCall();
      iter.remove();
    }

    // go to next time interval.
    clock.addMs(TIME_INTERVAL);

    Assert.assertTrue(dcClient1Default.getCurrentComputedDropRate() < 1d);

    resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
    assertNotNull(resultTC,"expected non-null trackerclient");
  }

  @Test(groups = { "small", "back-end" })
  public void testHighLowWatermarks()
  {
    final int NUM_CHECKS = 5;
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    double globalStepUp = 0.4;
    double globalStepDown = 0.4;
    double highWaterMark = 1000;
    double lowWaterMark = 50;
    TestClock clock = new TestClock();
    myMap.put("clock", clock);
    myMap.put("updateIntervalMs", timeInterval);
    myMap.put("globalStepUp", globalStepUp);
    myMap.put("globalStepDown", globalStepDown);
    myMap.put("highWaterMark", highWaterMark);
    myMap.put("lowWaterMark", lowWaterMark);

    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.configFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategy = new DegraderLoadBalancerStrategyV3(config);

    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");
    URIRequest request = new URIRequest(uri1);

    TrackerClient client1 =
            new TrackerClient(uri1, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri1), clock);

    clients.add(client1);

    DegraderControl dcClient1Default = client1.getDegraderControl(DEFAULT_PARTITION_ID);
    dcClient1Default.setOverrideMinCallCount(5);
    dcClient1Default.setMinCallCount(5);

    List<CallCompletion> ccList = new ArrayList<CallCompletion>();
    CallCompletion cc;

    TrackerClient resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
    // The override drop rate should be zero at this point.
    assertEquals(dcClient1Default.getOverrideDropRate(),0.0);

    // make high latency calls to the tracker client, verify the override drop rate doesn't change
    for (int j = 0; j < NUM_CHECKS; j++)
    {
      cc = client1.getCallTracker().startCall();
      ccList.add(cc);
    }

    clock.addMs((long)highWaterMark);

    for (Iterator<CallCompletion> iter = ccList.listIterator(); iter.hasNext();)
    {
      cc = iter.next();
      cc.endCall();
      iter.remove();
    }

    // go to next time interval.
    clock.addMs(timeInterval);

    // try call dropping on the next updateState
    strategy.setStrategy(DEFAULT_PARTITION_ID,
        DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.CALL_DROPPING);
    resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
    // we now expect that the override drop rate stepped up because updateState
    // made that decision.
    assertEquals(dcClient1Default.getOverrideDropRate(), globalStepUp);

    // make mid latency calls to the tracker client, verify the override drop rate doesn't change
    for (int j = 0; j < NUM_CHECKS; j++)
    {
      // need to use client1 because the resultTC may be null
      cc = client1.getCallTracker().startCall();
      ccList.add(cc);
    }

    clock.addMs((long)highWaterMark - 1);

    for (Iterator<CallCompletion> iter = ccList.listIterator(); iter.hasNext();)
    {
      cc = iter.next();
      cc.endCall();
      iter.remove();
    }

    // go to next time interval.
    clock.addMs(timeInterval);

    double previousOverrideDropRate = dcClient1Default.getOverrideDropRate();

    // try call dropping on the next updateState
    strategy.setStrategy(DEFAULT_PARTITION_ID,
        DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.CALL_DROPPING);
    resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
    assertEquals(dcClient1Default.getOverrideDropRate(), previousOverrideDropRate );

    // make low latency calls to the tracker client, verify the override drop rate decreases
    for (int j = 0; j < NUM_CHECKS; j++)
    {
      cc = client1.getCallTracker().startCall();
      ccList.add(cc);
    }

    clock.addMs((long)lowWaterMark);

    for (Iterator<CallCompletion> iter = ccList.listIterator(); iter.hasNext();)
    {
      cc = iter.next();
      cc.endCall();
      iter.remove();
    }

    // go to next time interval.
    clock.addMs(timeInterval);

    // try Call dropping on this updateState
    strategy.setStrategy(DEFAULT_PARTITION_ID,
        DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.CALL_DROPPING);
    resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
    assertEquals(resultTC.getDegraderControl(DEFAULT_PARTITION_ID).getOverrideDropRate(), 0.0 );
  }

  @Test(groups = { "small", "back-end" })
  public void testClusterRecovery2TC()
  {
    final int NUM_CHECKS = 5;
    final Long TIME_INTERVAL = 5000L;
    Map<String, Object> myMap = new HashMap<String, Object>();
    // 1,2,4,8,16,32,64,100% steps, given a 2x recovery step coefficient
    int localStepsToFullRecovery = 8;
    myMap.put("initialRecoveryLevel", 0.005);
    myMap.put("ringRampFactor", 2d);
    myMap.put("updateIntervalMs", TIME_INTERVAL);

    TestClock clock = new TestClock();
    myMap.put("clock", clock);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.configFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategy = new DegraderLoadBalancerStrategyV3(config);

    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");
    URI uri2 = URI.create("http://test.linkedin.com:3243/fdsaf");
    URIRequest request = new URIRequest(uri1);

    List<CallCompletion> ccList = new ArrayList<CallCompletion>();
    CallCompletion cc;

    TrackerClient client1 =
            new TrackerClient(uri1, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri1), clock);
    TrackerClient client2 =
            new TrackerClient(uri2, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri2), clock);

    clients.add(client1);
    clients.add(client2);

    // force client1 to be disabled if we encounter errors/high latency
    DegraderControl dcClient1Default = client1.getDegraderControl(DEFAULT_PARTITION_ID);
    dcClient1Default.setMinCallCount(5);
    dcClient1Default.setOverrideMinCallCount(5);
    dcClient1Default.setUpStep(1.0);
    // force client2 to be disabled if we encounter errors/high latency
    DegraderControl dcClient2Default = client2.getDegraderControl(DEFAULT_PARTITION_ID);
    dcClient2Default.setOverrideMinCallCount(5);
    dcClient2Default.setMinCallCount(5);
    dcClient2Default.setUpStep(0.4);

    // Have one cycle of successful calls to verify valid tracker clients returned.
    // try load balancing on this updateState, need to updateState before forcing the strategy.
    TrackerClient resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
    strategy.setStrategy(DEFAULT_PARTITION_ID,
        DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE);
    resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
    assertNotNull(resultTC, "expected non-null trackerclient");
    for (int j = 0; j < NUM_CHECKS; j++)
    {
      ccList.add(client1.getCallTracker().startCall());
      ccList.add(client2.getCallTracker().startCall());
    }

    clock.addMs(1);
    for (Iterator<CallCompletion> iter = ccList.listIterator(); iter.hasNext();)
    {
      cc = iter.next();
      cc.endCall();
    }

    // bump to next interval, and get stats.
    clock.addMs(5000);

    // try Load balancing on this updateState
    strategy.setStrategy(DEFAULT_PARTITION_ID,
        DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE);
    resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
    assertNotNull(resultTC,"expected non-null trackerclient");

    Assert.assertEquals(dcClient1Default.getCurrentComputedDropRate(), 0.0);
    Assert.assertEquals(dcClient2Default.getCurrentComputedDropRate(), 0.0);

    // now simulate a bad cluster state with high error and high latency
    for (int j = 0; j < NUM_CHECKS; j++)
    {
      ccList.add(client1.getCallTracker().startCall());
      ccList.add(client2.getCallTracker().startCall());
    }

    clock.addMs(3500);
    for (Iterator<CallCompletion> iter = ccList.listIterator(); iter.hasNext();)
    {
      cc = iter.next();
      cc.endCallWithError();
    }

    // go to next interval
    clock.addMs(5000);

    Assert.assertEquals(dcClient1Default.getCurrentComputedDropRate(), 1.0);
    Assert.assertEquals(dcClient2Default.getCurrentComputedDropRate(), 0.4);

    // trigger a state update, the returned TrackerClient should be client2
    // because client 1 should have gone up to a 1.0 drop rate, and the cluster should
    // be unhealthy
    strategy.setStrategy(DEFAULT_PARTITION_ID,
        DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE);
    resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
    assertEquals(resultTC, client2);


    // Simulate several time cycles without any calls. The ring recovery mechanism should bump
    // client1 up to full weight in an attempt to route some calls to it. Client2 will stay at
    // it's current drop rate.
    do
    {
      // go to next time interval.
      clock.addMs(TIME_INTERVAL);
      // adjust the hash ring this time.
      strategy.setStrategy(DEFAULT_PARTITION_ID,
          DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE);
      resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
      localStepsToFullRecovery--;
    }
    while (localStepsToFullRecovery > 0);
    assertNotNull(resultTC,"expected non-null trackerclient");

    assertTrue(strategy.getState().getPartitionState(DEFAULT_PARTITION_ID).getPointsMap().get(client1.getUri()) ==
                       client1.getPartitionWeight(DEFAULT_PARTITION_ID) * config.getPointsPerWeight(),
               "client1 did not recover to full weight in hash map.");
    Assert.assertEquals(dcClient2Default.getCurrentComputedDropRate(), 0.4,
                        "client2 drop rate not as expected");


    cc = client1.getCallTracker().startCall();
    clock.addMs(10);
    cc.endCall();
    clock.addMs(TIME_INTERVAL);

    resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
    assertNotNull(resultTC,"expected non-null trackerclient");
  }

  @Test(groups = { "small", "back-end" })
  public void testAdjustedMinCallCount()
  {
    final int NUM_CHECKS = 5;
    final Long TIME_INTERVAL = 5000L;
    Map<String, Object> myMap = new HashMap<String, Object>();
    //myMap.put("initialRecoveryLevel", 0.01);
    //myMap.put("rampFactor", 2d);
    myMap.put("updateIntervalMs", TIME_INTERVAL);

    TestClock clock = new TestClock();
    myMap.put("clock", clock);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.configFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategy = new DegraderLoadBalancerStrategyV3(config);

    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");
    URIRequest request = new URIRequest(uri1);

    List<CallCompletion> ccList = new ArrayList<CallCompletion>();
    CallCompletion cc;

    TrackerClient client1 =
            new TrackerClient(uri1, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri1), clock);

    clients.add(client1);

    // force client1 to be disabled if we encounter errors/high latency
    DegraderControl dcClient1Default = client1.getDegraderControl(DEFAULT_PARTITION_ID);
    dcClient1Default.setOverrideMinCallCount(5);
    dcClient1Default.setMinCallCount(5);
    dcClient1Default.setUpStep(1.0);
    dcClient1Default.setHighErrorRate(0);

    // Issue high latency calls to reduce client1 to the minimum number of hash points allowed.
    // (1 in this case)
    TrackerClient resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
    assertNotNull(resultTC, "expected non-null trackerclient");
    for (int j = 0; j < NUM_CHECKS; j++)
    {
      cc = resultTC.getCallTracker().startCall();

      ccList.add(cc);
    }

    clock.addMs(3500);
    for (int j = 0; j < NUM_CHECKS; j++)
    {
      cc = ccList.get(j);
      cc.endCall();
    }
    // bump to next interval, and get stats.
    clock.addMs(5000);

    // because we want to test out the adjusted min drop rate, force the hash ring adjustment now.
    strategy.setStrategy(DEFAULT_PARTITION_ID,
        DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE);
    resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
    // client1 should be reduced to 1 hash point, but since it is the only TC, it should be the
    // TC returned.
    assertEquals(resultTC, client1, "expected non-null trackerclient");

    assertEquals((int)(strategy.getState().getPartitionState(DEFAULT_PARTITION_ID).getPointsMap().get(client1.getUri())), 1,
                 "expected client1 to have only 1 point in hash map");

    // make low latency call, we expect the computedDropRate to be adjusted because the minimum
    // call count was also scaled down.
    cc = client1.getCallTracker().startCall();
    clock.addMs(10);
    cc.endCall();
    clock.addMs(TIME_INTERVAL);

    Assert.assertTrue(dcClient1Default.getCurrentComputedDropRate() < 1.0,
                      "client1 drop rate not less than 1.");
  }

  public static DegraderLoadBalancerStrategyV3 getStrategy()
  {
    return new DegraderLoadBalancerStrategyV3(new DegraderLoadBalancerStrategyConfig(5000,
                                                                                   100d));
  }

  public static DegraderLoadBalancerStrategyV3 getStrategy(Map<String,Object> map)
  {
    return new DegraderLoadBalancerStrategyV3(DegraderLoadBalancerStrategyConfig.configFromMap(map));
  }

  public static TrackerClient getClient(URI uri)
  {
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(2);
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1d));
    return new TrackerClient(uri, partitionDataMap,new TestLoadBalancerClient(uri));
  }

  public static TrackerClient getClient(URI uri, Clock clock)
  {
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(2);
    partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID, new PartitionData(1));
    return new TrackerClient(uri, partitionDataMap, new TestLoadBalancerClient(uri), clock);
  }

  public static class TestLoadBalancerClient implements LoadBalancerClient
  {

    private URI _uri;

    public TestLoadBalancerClient(URI uri)
    {
      _uri = uri;
    }

    @Override
    public URI getUri()
    {
      return _uri;
    }

    @Override
    public void restRequest(RestRequest request,
                            RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            TransportCallback<RestResponse> callback)
    {
      // Do nothing
    }

    @Override
    public void rpcRequest(RpcRequest request,
                           RequestContext requestContext,
                           Map<String, String> wireAttrs,
                           TransportCallback<RpcResponse> callback)
    {
      // Do nothing
    }

    @Override
    public void shutdown(Callback<None> callback)
    {
      callback.onSuccess(None.none());
    }

    @Override
    public String toString()
    {
      return "TestTrackerClient _uri=" + _uri;
    }
  }

  public class TestClock implements Clock
  {
    private long _currentTimeMillis;

    public TestClock()
    {
      _currentTimeMillis = 0l;
    }

    public void addMs(long ms)
    {
      _currentTimeMillis += ms;
    }

    @Override
    public long currentTimeMillis()
    {
      return _currentTimeMillis;
    }
  }
}

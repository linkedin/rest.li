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
import com.linkedin.d2.balancer.clients.TrackerClientTest;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.strategies.LoadBalancerStrategy;
import com.linkedin.d2.balancer.util.URIRequest;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing.Point;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.d2.balancer.util.hashing.URIRegexHash;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.common.TransportCallback;
import com.linkedin.util.clock.Clock;
import com.linkedin.util.clock.SettableClock;
import com.linkedin.util.clock.SystemClock;
import com.linkedin.util.degrader.CallCompletion;
import com.linkedin.util.degrader.DegraderControl;
import com.linkedin.util.degrader.DegraderImpl;
import com.linkedin.util.degrader.ErrorType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


public class DegraderLoadBalancerTest
{
  private static final int DEFAULT_PARTITION_ID = DefaultPartitionAccessor.DEFAULT_PARTITION_ID;

  public static void main(String[] args) throws URISyntaxException,
          InterruptedException
  {
    DegraderLoadBalancerTest test = new DegraderLoadBalancerTest();

    test.testWeightedBalancingRing();
  }

  public static TrackerClient getTrackerClient(LoadBalancerStrategy strategy,
                                               Request request,
                                               RequestContext requestContext,
                                               long clusterGenerationId,
                                               List<TrackerClient> trackerClients)
  {
    return strategy.getTrackerClient(request, requestContext, clusterGenerationId, DefaultPartitionAccessor.DEFAULT_PARTITION_ID, trackerClients);
  }

  public static Map<Integer, PartitionData> getDefaultPartitionData(double weight)
  {
    return getDefaultPartitionData(weight, 1);
  }

  public static Map<Integer, PartitionData> getDefaultPartitionData(double weight, int numberOfPartitions)
  {
    PartitionData data = new PartitionData(weight);
    Map<Integer, PartitionData> partitionDataMap = new HashMap<Integer, PartitionData>(numberOfPartitions + 1);
    for (int p = 0; p < numberOfPartitions; ++p)
      partitionDataMap.put(DefaultPartitionAccessor.DEFAULT_PARTITION_ID + p, data);
    return partitionDataMap;
  }

  @Test(groups = { "small", "back-end" })
  public void testDegraderLoadBalancerStateComparison()
          throws URISyntaxException
  {

    long clusterGenerationId = 1;
    long lastUpdated = 29999;
    long updateIntervalMs = 5000;
    DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState.Strategy strategy =
            DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState.Strategy.LOAD_BALANCE;
    long currentAverageClusterLatency = 3000;
    Map<String, Object> configMap = new HashMap<String, Object>();
    configMap.put(PropertyKeys.HTTP_LB_LOW_WATER_MARK, 500d);
    configMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_POINTS_PER_WEIGHT, 120);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(configMap);
    long clusterCallCount = 15;

    double currentOverrideDropRate = 0.4;
    boolean initialized = true;
    String name = "degraderV2";
    Map<URI, Integer> points = new HashMap<URI, Integer>();
    Map<TrackerClient,Double> recoveryMap = new HashMap<TrackerClient, Double>();
    URI uri1 = new URI("http://test.linkedin.com:10010/abc0");
    URI uri2 = new URI("http://test.linkedin.com:10010/abc1");
    URI uri3 = new URI("http://test.linkedin.com:10010/abc2");
    points.put(uri1, 100);
    points.put(uri2, 50);
    points.put(uri3, 120);
    TestClock clock = new TestClock();

    List<TrackerClient> clients = createTrackerClient(3, clock, null);
    List<TrackerClientUpdater> clientUpdaters = new ArrayList<TrackerClientUpdater>();
    for (TrackerClient client : clients)
    {
      recoveryMap.put(client, 0.0);
      clientUpdaters.add(new TrackerClientUpdater(client, DEFAULT_PARTITION_ID));
    }


    DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState oldStateV2 =
            new DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState(updateIntervalMs,
                    clusterGenerationId,
                    points,
                    lastUpdated,
                    strategy,
                    currentOverrideDropRate,
                    currentAverageClusterLatency,
                    initialized,
                    recoveryMap,
                    name, null, clusterCallCount);

    DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState newStateV2 =
            new DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState(updateIntervalMs,
                    clusterGenerationId,
                    points,
                    lastUpdated,
                    strategy,
                    currentOverrideDropRate,
                    currentAverageClusterLatency,
                    initialized,
                    recoveryMap,
                    name, null, clusterCallCount);

    assertTrue(DegraderLoadBalancerStrategyV2_1.isOldStateTheSameAsNewState(oldStateV2, newStateV2));

    newStateV2 = new DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState(updateIntervalMs,
            clusterGenerationId + 1,
            points,
            lastUpdated,
            strategy,
            currentOverrideDropRate,
            currentAverageClusterLatency,
            initialized,
            recoveryMap,
            name, null,
            clusterCallCount);

    assertFalse(DegraderLoadBalancerStrategyV2_1.isOldStateTheSameAsNewState(oldStateV2, newStateV2));

    //we don't care about last updated
    newStateV2 = new DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState(updateIntervalMs,
            clusterGenerationId,
            points,
            lastUpdated + 30,
            strategy,
            currentOverrideDropRate,
            currentAverageClusterLatency,
            initialized,
            recoveryMap,
            name, null, clusterCallCount);

    assertTrue(DegraderLoadBalancerStrategyV2_1.isOldStateTheSameAsNewState(oldStateV2, newStateV2));

    points.put(uri1, 30);
    newStateV2 = new DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState(updateIntervalMs,
            clusterGenerationId,
            points,
            lastUpdated,
            strategy,
            currentOverrideDropRate,
            currentAverageClusterLatency,
            initialized,
            recoveryMap,
            name, null, clusterCallCount);

    points.put(uri1, 100);

    newStateV2 = new DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState(updateIntervalMs,
            clusterGenerationId,
            points,
            lastUpdated,
            strategy,
            currentOverrideDropRate - 0.1,
            currentAverageClusterLatency,
            initialized,
            recoveryMap,
            name, null, clusterCallCount);

    assertFalse(DegraderLoadBalancerStrategyV2_1.isOldStateTheSameAsNewState(oldStateV2, newStateV2));
    //we don't care about averageClusterLatency as far as for printing the state
    newStateV2 = new DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState(updateIntervalMs,
            clusterGenerationId,
            points,
            lastUpdated,
            strategy,
            currentOverrideDropRate,
            currentAverageClusterLatency + 3,
            initialized,
            recoveryMap,
            name, null, clusterCallCount);
    assertTrue(DegraderLoadBalancerStrategyV2_1.isOldStateTheSameAsNewState(oldStateV2, newStateV2));
    for (TrackerClient client : clients)
    {
      recoveryMap.put(client, 0.3);
    }

    newStateV2 = new DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState(updateIntervalMs,
            clusterGenerationId,
            points,
            lastUpdated,
            strategy,
            currentOverrideDropRate,
            currentAverageClusterLatency,
            initialized,
            recoveryMap,
            name, null, clusterCallCount);
    assertFalse(DegraderLoadBalancerStrategyV2_1.isOldStateTheSameAsNewState(oldStateV2, newStateV2));

    //test state health comparison

    assertFalse(DegraderLoadBalancerStrategyV2_1.isNewStateHealthy(newStateV2, config, clientUpdaters));
    //make cluster average latency to be 300 to be lower than lowWaterMark but still not healthy because
    //points map has clients with less than perfect health
    newStateV2 = new DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState(updateIntervalMs,
            clusterGenerationId,
            points,
            lastUpdated,
            strategy,
            currentOverrideDropRate,
            300,
            initialized,
            recoveryMap,
            name, null, clusterCallCount);

    assertFalse(DegraderLoadBalancerStrategyV2_1.isNewStateHealthy(newStateV2, config, clientUpdaters));
    //make all points to have 120 so the cluster becomes "healthy"
    points.put(uri1, 120);
    points.put(uri2, 120);
    points.put(uri3, 120);

    newStateV2 = new DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState(updateIntervalMs,
            clusterGenerationId,
            points,
            lastUpdated,
            strategy,
            currentOverrideDropRate,
            300,
            initialized,
            recoveryMap,
            name, null, clusterCallCount);
    assertTrue(DegraderLoadBalancerStrategyV2_1.isNewStateHealthy(newStateV2, config, clientUpdaters));

    //if currentAverageClusterLatency is > low water mark then cluster becomes unhealthy
    newStateV2 = new DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState(updateIntervalMs,
            clusterGenerationId,
            points,
            lastUpdated,
            strategy,
            currentOverrideDropRate,
            currentAverageClusterLatency,
            initialized,
            recoveryMap,
            name, null, clusterCallCount);

    assertFalse(DegraderLoadBalancerStrategyV2_1.isNewStateHealthy(newStateV2, config, clientUpdaters));

    //test DegraderLoadBalancerV3

    points.put(uri1, 100);
    points.put(uri2, 50);
    points.put(uri3, 120);

    DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy strategyV3 =
            DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.CALL_DROPPING;

    DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState oldStateV3 = new
            DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(clusterGenerationId,
            lastUpdated,
            initialized,
            points,
            strategyV3,
            currentOverrideDropRate,
            currentAverageClusterLatency,
            recoveryMap,
            name, null,
            clusterCallCount);

    DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState newStateV3 = new
            DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(clusterGenerationId,
            lastUpdated,
            initialized,
            points,
            strategyV3,
            currentOverrideDropRate,
            currentAverageClusterLatency,
            recoveryMap,
            name, null,
            clusterCallCount);

    assertTrue(DegraderLoadBalancerStrategyV3.isOldStateTheSameAsNewState(oldStateV3, newStateV3));

    newStateV3 = new DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(clusterGenerationId + 1,
            lastUpdated,
            initialized,
            points,
            strategyV3,
            currentOverrideDropRate,
            currentAverageClusterLatency,
            recoveryMap,
            name, null,
            clusterCallCount);

    assertTrue(DegraderLoadBalancerStrategyV3.isOldStateTheSameAsNewState(oldStateV3, newStateV3));

    newStateV3 = new DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(clusterGenerationId,
            lastUpdated + 300,
            initialized,
            points,
            strategyV3,
            currentOverrideDropRate,
            currentAverageClusterLatency,
            recoveryMap,
            name, null, clusterCallCount);

    assertTrue(DegraderLoadBalancerStrategyV3.isOldStateTheSameAsNewState(oldStateV3, newStateV3));

    points.put(uri2, 77);
    newStateV3 = new DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(clusterGenerationId,
            lastUpdated,
            initialized,
            points,
            strategyV3,
            currentOverrideDropRate,
            currentAverageClusterLatency,
            recoveryMap,
            name, null,
            clusterCallCount);
    assertFalse(DegraderLoadBalancerStrategyV3.isOldStateTheSameAsNewState(oldStateV3, newStateV3));

    points.put(uri2, 50);

    newStateV3 = new DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(clusterGenerationId,
            lastUpdated,
            initialized,
            points,
            strategyV3,
            currentOverrideDropRate + 0.4,
            currentAverageClusterLatency,
            recoveryMap,
            name, null, clusterCallCount);
    assertFalse(DegraderLoadBalancerStrategyV3.isOldStateTheSameAsNewState(oldStateV3, newStateV3));

    newStateV3 = new DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(clusterGenerationId,
            lastUpdated,
            initialized,
            points,
            strategyV3,
            currentOverrideDropRate,
            currentAverageClusterLatency + 55,
            recoveryMap,
            name, null, clusterCallCount);
    //we don't care about averageClusterLatency for comparing states
    assertTrue(DegraderLoadBalancerStrategyV3.isOldStateTheSameAsNewState(oldStateV3, newStateV3));

    for (TrackerClient client : clients)
    {
      recoveryMap.put(client, 0.5);
    }

    newStateV3 = new DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(clusterGenerationId,
            lastUpdated,
            initialized,
            points,
            strategyV3,
            currentOverrideDropRate,
            currentAverageClusterLatency,
            recoveryMap,
            name, null, clusterCallCount);
    assertFalse(DegraderLoadBalancerStrategyV3.isOldStateTheSameAsNewState(oldStateV3, newStateV3));

    //test state health comparison

    assertFalse(DegraderLoadBalancerStrategyV3.isNewStateHealthy(newStateV3, config, clientUpdaters, DEFAULT_PARTITION_ID));
    //make cluster average latency to be 300 to be lower than lowWaterMark but still not healthy because
    //points map has clients with less than perfect health
    newStateV3 = new DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(clusterGenerationId,
            lastUpdated,
            initialized,
            points,
            strategyV3,
            currentOverrideDropRate,
            300,
            recoveryMap,
            name, null,
            clusterCallCount);

    assertFalse(DegraderLoadBalancerStrategyV3.isNewStateHealthy(newStateV3, config, clientUpdaters, DEFAULT_PARTITION_ID));
    //make all points to have 120 so the cluster becomes "healthy"
    points.put(uri1, 120);
    points.put(uri2, 120);
    points.put(uri3, 120);

    newStateV3 = new DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(clusterGenerationId,
            lastUpdated,
            initialized,
            points,
            strategyV3,
            currentOverrideDropRate,
            300,
            recoveryMap,
            name, null,
            clusterCallCount);
    assertTrue(DegraderLoadBalancerStrategyV3.isNewStateHealthy(newStateV3, config, clientUpdaters, DEFAULT_PARTITION_ID));

    //if currentAverageClusterLatency is > low water mark then cluster becomes unhealthy
    newStateV3 = new DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(clusterGenerationId,
            lastUpdated,
            initialized,
            points,
            strategyV3,
            currentOverrideDropRate,
            currentAverageClusterLatency,
            recoveryMap,
            name, null,
            clusterCallCount);

    assertFalse(DegraderLoadBalancerStrategyV3.isNewStateHealthy(newStateV3, config, clientUpdaters, DEFAULT_PARTITION_ID));
  }

  /** A type of checked exception that we intentionally throw during testing */
  private static class DummyCheckedException extends Exception
  {
    private static final long serialVersionUID = 1;

    public void throwMe()
    {
      DummyCheckedException.<RuntimeException>throwAny(this);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAny(Throwable e)
            throws E
    {
      throw (E) e;
    }
  }

  private static class DummyException extends RuntimeException
  {
    static final long serialVersionUID = 1L;
    // A type of runtime exception that we intentionally throw during testing
  }

  /**
   * this class will throw error when getHighWaterMark() is called thus triggering a failed state update
   */
  private static class MockDegraderLoadBalancerStrategyConfig extends DegraderLoadBalancerStrategyConfig
  {
    public MockDegraderLoadBalancerStrategyConfig(DegraderLoadBalancerStrategyConfig config)
    {
      super(config.getUpdateIntervalMs(),
              config.isUpdateOnlyAtInterval(),
              config.getPointsPerWeight(),
              config.getHashMethod(),
              config.getHashConfig(),
              config.getClock(),
              config.getInitialRecoveryLevel(),
              config.getRingRampFactor(),
              config.getHighWaterMark(),
              config.getLowWaterMark(),
              config.getGlobalStepUp(),
              config.getGlobalStepDown(),
              config.getMinClusterCallCountHighWaterMark(),
              config.getMinClusterCallCountLowWaterMark());
    }

    @Override
    public double getHighWaterMark()
    {
      //throw an exception when updateState is called when the strategy is CALL_DROPPING when enabled
      throw new DummyException();
    }
  }

  private static class BrokenTrackerClient extends TrackerClient
  {
    public BrokenTrackerClient(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient,
                               Clock clock, DegraderImpl.Config config)
    {
      super(uri, partitionDataMap, wrappedClient, clock, config);
    }

    private final AtomicBoolean _exceptionEnabled = new AtomicBoolean(true);

    @Override
    public Double getPartitionWeight(int partitionId)
    {
      //throw an exception when updateState is called when the strategy is LOAD_BALANCER
      if (_exceptionEnabled.compareAndSet(true, false))
      {
        throw new DummyException();
      }
      return 1.0;
    }

    public void reset()
    {
      _exceptionEnabled.set(true);
    }
  }

  private static class TrackerClientMetrics
  {
    private final double _overrideDropRate;
    private final double _maxDropRate;
    private final int _overrideMinCallCount;

    TrackerClientMetrics(double overrideDropRate, double maxDropRate, int overrideMinCallCount)
    {
      _overrideDropRate = overrideDropRate;
      _maxDropRate = maxDropRate;
      _overrideMinCallCount = overrideMinCallCount;
    }

    @Override
    public boolean equals(Object other)
    {
      if (other == null)
        return false;
      if (other instanceof TrackerClientMetrics)
      {
        TrackerClientMetrics metrics = (TrackerClientMetrics) other;
        return _overrideMinCallCount == metrics._overrideMinCallCount
                && Math.abs(_overrideDropRate - metrics._overrideDropRate) < 0.0001
                && Math.abs(_maxDropRate - metrics._maxDropRate) < 0.0001;
      }
      return false;
    }

    @Override
    public int hashCode()
    {
      int result;
      long temp;
      temp = Double.doubleToLongBits(_overrideDropRate);
      result = (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(_maxDropRate);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      result = 31 * result + _overrideMinCallCount;
      return result;
    }
  }

  private static Map<TrackerClient, TrackerClientMetrics> getTrackerClientMetrics(List<TrackerClient> clients)
  {
    Map<TrackerClient, TrackerClientMetrics> map = new HashMap<TrackerClient, TrackerClientMetrics>();
    for (TrackerClient client : clients)
    {
      DegraderControl degraderControl = client.getDegraderControl(DEFAULT_PARTITION_ID);
      map.put(client, new TrackerClientMetrics(degraderControl.getOverrideDropRate(),
              degraderControl.getMaxDropRate(),
              degraderControl.getOverrideMinCallCount()));
    }
    return map;
  }

  @Test(groups = { "small", "back-end" })
  public void testDegraderLoadBalancerHandlingExceptionInUpdate()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS , timeInterval);
    Map<String, String> degraderProperties = new HashMap<String,String>();
    degraderProperties.put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, "0.5");
    degraderProperties.put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, "0.2");
    DegraderImpl.Config degraderConfig = DegraderConfigFactory.toDegraderConfig(degraderProperties);
    final List<TrackerClient> clients = createTrackerClient(3, clock, degraderConfig);
    DegraderLoadBalancerStrategyConfig unbrokenConfig = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyConfig brokenConfig = new MockDegraderLoadBalancerStrategyConfig(unbrokenConfig);

    URI uri4 = URI.create("http://test.linkedin.com:10010/abc4");
    //this client will throw exception when getDegraderControl is called hence triggering a failed state update
    BrokenTrackerClient brokenClient =   new BrokenTrackerClient(uri4,
            getDefaultPartitionData(1d),
            new TestLoadBalancerClient(uri4), clock, null);
    clients.add(brokenClient);

    //test DegraderLoadBalancerStrategyV2_1 when the strategy is LOAD_BALANCE
    final DegraderLoadBalancerStrategyV2_1 strategyV2 = new DegraderLoadBalancerStrategyV2_1(brokenConfig, "testStrategyV2", null);
    DegraderLoadBalancerStrategyAdapter strategyAdapterV2 = new DegraderLoadBalancerStrategyAdapter(strategyV2);
    //simulate 100 threads trying to get client at the same time. Make sure that they won't be blocked if an exception
    //occurs during updateState()
    runMultiThreadedTest(strategyAdapterV2, clients, 100, true);
    DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState stateV2 = strategyV2.getState();
    // only one exception would occur and other thread would succeed in initializing immediately after
    assertTrue(stateV2.isInitialized());
    assertEquals(stateV2.getStrategy(), DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState.Strategy.CALL_DROPPING);
    brokenClient.reset();

    //test DegraderLoadBalancerStrategyV3 when the strategy is LOAD_BALANCE
    DegraderLoadBalancerStrategyV3 strategyV3 = new DegraderLoadBalancerStrategyV3(brokenConfig, "testStrategyV3", null);
    DegraderLoadBalancerStrategyAdapter strategyAdapterV3 = new DegraderLoadBalancerStrategyAdapter(strategyV3);
    //simulate 100 threads trying to get client at the same time. Make sure that they won't be blocked if an exception
    //occurs during updateState()
    runMultiThreadedTest(strategyAdapterV3, clients, 100, true);
    DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState stateV3 = strategyV3.getState().
            getPartitionState(0);
    // only one exception would occur and other thread would succeed in initializing immediately after
    assertTrue(stateV3.isInitialized());
    assertEquals(stateV3.getStrategy(),
            DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.CALL_DROPPING);
    brokenClient.reset();

    // test DegraderLoadBalancerStrategy when the strategy is CALL_DROPPING. We have to make some prepare the
    // environment by simulating lots of high latency calls to the tracker client
    int numberOfCallsPerClient = 10;
    List<CallCompletion> callCompletions = new ArrayList<CallCompletion>();
    for (TrackerClient client : clients)
    {
      for (int i = 0; i < numberOfCallsPerClient; i++)
      {
        callCompletions.add(client.getCallTracker().startCall());
      }
    }

    clock.addMs(brokenConfig.getUpdateIntervalMs() - 1000);
    for (CallCompletion cc : callCompletions)
    {
      for (int i = 0; i < numberOfCallsPerClient; i++)
      {
        cc.endCall();
      }
    }
    clock.addMs(1000);

    Map<TrackerClient, TrackerClientMetrics> beforeStateUpdate = getTrackerClientMetrics(clients);

    //test DegraderLoadBalancerStrategyV2_1 when the strategy is CALL_DROPPING
    strategyV2.setStrategy(DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState.Strategy.CALL_DROPPING);
    strategyV3.setStrategy(DEFAULT_PARTITION_ID, DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.CALL_DROPPING);
    runMultiThreadedTest(strategyAdapterV2, clients, 100, true);
    stateV2 = strategyV2.getState();

    //MockDegraderLoadBalancerStrategyConfig getHighWaterMark should have been called and throw an exception every time and update would fail for any thread

    // no side-effects on state when update fails
    assertEquals(stateV2.getStrategy(), DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState.Strategy.CALL_DROPPING);
    // no side-effects on tracker clients when update fails
    Map<TrackerClient, TrackerClientMetrics> afterFailedV2StateUpdate = getTrackerClientMetrics(clients);
    for (TrackerClient client : clients)
    {
      assertEquals(beforeStateUpdate.get(client), afterFailedV2StateUpdate.get(client));
    }

    runMultiThreadedTest(strategyAdapterV3, clients, 100, true);
    stateV3 = strategyV3.getState().getPartitionState(0);
    // no side-effects on state when update fails
    assertEquals(stateV3.getStrategy(), DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.CALL_DROPPING);
    // no side-effects on tracker clients when update fails
    Map<TrackerClient, TrackerClientMetrics> afterFailedV3StateUpdate = getTrackerClientMetrics(clients);
    for (TrackerClient client : clients)
    {
      assertEquals(beforeStateUpdate.get(client), afterFailedV3StateUpdate.get(client));
    }

    brokenClient.reset();

    //this time we'll change the config to the correct one so it won't throw exception when strategy is CALL_DROPPING
    // update would succeed and state and trackerclients are expected to be mutated
    callCompletions.clear();
    for (TrackerClient client : clients)
    {
      for (int i = 0; i < numberOfCallsPerClient; i++)
      {
        callCompletions.add(client.getCallTracker().startCall());
      }
    }

    clock.addMs(brokenConfig.getUpdateIntervalMs() - 1000);
    for (CallCompletion cc : callCompletions)
    {
      for (int i = 0; i < numberOfCallsPerClient; i++)
      {
        cc.endCall();
      }
    }
    clock.addMs(1000);


    strategyV2.setConfig(unbrokenConfig);
    beforeStateUpdate = getTrackerClientMetrics(clients);
    // when we run this, the strategy is CALL_DROPPING, and our clients' latency is 4000 MS so our current override
    // drop rate is going to be 0.2 That means occasionally some tracker client will be null
    runMultiThreadedTest(strategyAdapterV2, clients, 100, false);
    stateV2 = strategyV2.getState();
    // This time update should succeed, and both state and trackerclients are updated
    Map<TrackerClient, TrackerClientMetrics> afterV2StateUpdate = getTrackerClientMetrics(clients);
    for (TrackerClient client : clients)
    {
      assertNotEquals(beforeStateUpdate.get(client), afterV2StateUpdate.get(client));
    }
    assertEquals(stateV2.getStrategy(), DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState.Strategy.LOAD_BALANCE);

    brokenClient.reset();

    // reset metrics on tracker client's degrader control
    for(TrackerClient client : clients)
    {
      TrackerClientMetrics originalMetrics = beforeStateUpdate.get(client);
      DegraderControl degraderControl = client.getDegraderControl(DEFAULT_PARTITION_ID);
      degraderControl.setOverrideDropRate(originalMetrics._overrideDropRate);
      degraderControl.setMaxDropRate(originalMetrics._maxDropRate);
      degraderControl.setOverrideMinCallCount(originalMetrics._overrideMinCallCount);
    }

    callCompletions.clear();
    for (TrackerClient client : clients)
    {
      for (int i = 0; i < numberOfCallsPerClient; i++)
      {
        callCompletions.add(client.getCallTracker().startCall());
      }
    }

    clock.addMs(brokenConfig.getUpdateIntervalMs() - 1000);
    for (CallCompletion cc : callCompletions)
    {
      for (int i = 0; i < numberOfCallsPerClient; i++)
      {
        cc.endCall();
      }
    }
    clock.addMs(1000);

    strategyV3.setConfig(unbrokenConfig);
    beforeStateUpdate = getTrackerClientMetrics(clients);
    runMultiThreadedTest(strategyAdapterV3, clients, 100, false);
    stateV3 = strategyV3.getState().getPartitionState(0);
    // This time update should succeed, and both state and trackerclients are updated
    Map<TrackerClient, TrackerClientMetrics> afterV3StateUpdate = getTrackerClientMetrics(clients);
    for (TrackerClient client : clients)
    {
      assertNotEquals(beforeStateUpdate.get(client), afterV3StateUpdate.get(client));
    }

    assertEquals(stateV3.getStrategy(), DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE);
  }

  private void runMultiThreadedTest(final DegraderLoadBalancerStrategyAdapter strategyAdapter,
                                    final List<TrackerClient> clients,
                                    final int numberOfThread,
                                    final boolean trackerClientMustNotBeNull)
  {
    final CountDownLatch exitLatch = new CountDownLatch(numberOfThread);
    final CountDownLatch startLatch = new CountDownLatch(numberOfThread);
    ExecutorService executorService = Executors.newFixedThreadPool(numberOfThread);
    List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

    for (int i = 0; i < numberOfThread; i++)
    {
      // testNG won't be able to "know" if a child thread's assertion is false, so to get around it we
      // need to throw an exception so executorService can notify the main thread about the false assertion
      // even though executorService has an invokeAll method, it doesn't seem run the threads at the same time
      // so we have to use a latch to make sure all the threads run at the same time to simulate concurrent access
      Future<Boolean> future = executorService.submit(new Runnable(){

        @Override
        public void run()
        {
          startLatch.countDown();
          try
          {
            //wait until all threads are ready to run together
            startLatch.await();
          }
          catch (InterruptedException e)
          {
            throw new RuntimeException("Failed the test because thread was interrupted");
          }

          // when a thread failed to initialize a state, other threads would try immediately
          // if config is correct, only one thread would fail because broken client throws exception only once util next reset,
          // and state update would succeed
          // if config is broken, every thread would fail and state update would be unsuccessful
          try
          {
            TrackerClient resultTC = getTrackerClient(strategyAdapter, null, new RequestContext(), 1, clients);
            if (trackerClientMustNotBeNull && resultTC == null)
            {
              throw new RuntimeException("Failed the test because resultTC returns null");
            }
          }
          catch (DummyException ex)
          {
            // expected
          }

          exitLatch.countDown();
          try
          {
            //make sure all threads are not stuck on WAIT_THREAD
            if (!exitLatch.await(5, TimeUnit.SECONDS))
            {
              throw new RuntimeException("Failed the test because we waited longer than 1 second");
            }
          }
          catch (InterruptedException e)
          {
            throw new RuntimeException("Failed the test because thread was interrupted");
          }
        }
      }, Boolean.TRUE);
      futures.add(future);
    }

    for (Future<Boolean> future : futures)
    {
      try
      {
        assertTrue(future.get());
      }
      catch (Exception e)
      {
        fail("something is failing", e);
      }
    }
    executorService.shutdownNow();
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
            new DegraderLoadBalancerStrategyV3(new DegraderLoadBalancerStrategyConfig(5000),
                    "DegraderLoadBalancerTest", null);
    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    SettableClock clock1 = new SettableClock();
    SettableClock clock2 = new SettableClock();

    clients.add(getClient(URI.create("http://test.linkedin.com:3242/fdsaf"), clock1));
    clients.add(getClient(URI.create("http://test.linkedin.com:3243/fdsaf"), clock2));

    clock1.addDuration(5000);

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
            new DegraderLoadBalancerStrategyV3(new DegraderLoadBalancerStrategyConfig(5000),
                    "DegraderLoadBalancerTest", null);
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
            new DegraderLoadBalancerStrategyV3(new DegraderLoadBalancerStrategyConfig(5000),
                    "DegraderLoadBalancerTest",
                    null
            );
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
    List<TrackerClientUpdater> clientUpdaters = new ArrayList<TrackerClientUpdater>();

    clients.add(getClient(URI.create("http://test.linkedin.com:3242/fdsaf"), new TestClock()));
    clients.add(getClient(URI.create("http://test.linkedin.com:3243/fdsaf"), new TestClock()));

    for (TrackerClient client : clients)
    {
      clientUpdaters.add(new TrackerClientUpdater(client, DEFAULT_PARTITION_ID));
    }

    // first verify that we're getting clients
    assertNotNull(getTrackerClient(strategy, null, new RequestContext(), 0, clients));

    assertFalse(clients.get(0).getDegrader(DEFAULT_PARTITION_ID).checkDrop());
    assertFalse(clients.get(1).getDegrader(DEFAULT_PARTITION_ID).checkDrop());

    // now force drop rate to 100% for entire cluster
    DegraderLoadBalancerStrategyV3.overrideClusterDropRate(DEFAULT_PARTITION_ID, 1d, clientUpdaters);

    for (TrackerClientUpdater clientUpdater : clientUpdaters)
    {
      clientUpdater.update();
    }

    // now verify that everything is dropping
    assertNull(getTrackerClient(strategy, null, new RequestContext(), 1, clients));

    assertTrue(clients.get(0).getDegrader(DEFAULT_PARTITION_ID).checkDrop());
    assertTrue(clients.get(1).getDegrader(DEFAULT_PARTITION_ID).checkDrop());
  }

  /**
   * Tests that overrideDropRate is affected by the minCallCount. If we don't have enough calls
   * then it shouldn't affect cluster drop rate. There are many other pathways that we want to test here.
   */
  @Test(groups = { "small", "back-end" })
  public void testLoadBalancerCallDroppingMode()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    double highWaterMark = 1000;
    double lowWaterMark = 500;
    long minCallHighWaterMark = 50l;
    long minCallLowWaterMark = 20l;
    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS, timeInterval);
    myMap.put(PropertyKeys.HTTP_LB_HIGH_WATER_MARK, highWaterMark);
    myMap.put(PropertyKeys.HTTP_LB_LOW_WATER_MARK, lowWaterMark);
    myMap.put(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK, minCallHighWaterMark);
    myMap.put(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK, minCallLowWaterMark);

    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    //test strategy v3
    DegraderLoadBalancerStrategyAdapter strategyAdapter = new DegraderLoadBalancerStrategyAdapter(
            new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest", null));

    final List<TrackerClient> clients = createTrackerClient(3, clock, null);
    testCallDroppingHelper(strategyAdapter, clients, clock, timeInterval);

    //test strategy v2
    strategyAdapter = new DegraderLoadBalancerStrategyAdapter(
            new DegraderLoadBalancerStrategyV2_1(config, "DegraderLoadBalancerTest", null));

    testCallDroppingHelper(strategyAdapter, clients, clock, timeInterval);
  }

  public static boolean isEqual(double d0, double d1) {
    final double epsilon = 0.0000001;
    return d0 == d1 ? true : Math.abs(d0 - d1) < epsilon;
  }

  private void testCallDroppingHelper(DegraderLoadBalancerStrategyAdapter strategyAdapter,
                                      List<TrackerClient> clients, TestClock clock, Long timeInterval)
  {
    //test clusterOverrideDropRate won't increase even though latency is 3000 ms because the traffic is low
    callClients(3000, 0.2, clients, clock, timeInterval, false, false);
    strategyAdapter.setStrategyToCallDrop();
    URIRequest request = new URIRequest(clients.get(0).getUri());
    getTrackerClient(strategyAdapter, request, new RequestContext(), 1, clients);
    assertTrue(isEqual(0.0d, strategyAdapter.getCurrentOverrideDropRate()));

    //if we increase the QPS from 0.2 to 25, then we'll start dropping calls
    callClients(3000, 25, clients, clock, timeInterval, false, false);
    strategyAdapter.setStrategyToCallDrop();
    getTrackerClient(strategyAdapter, request, new RequestContext(), 1, clients);
    assertTrue(isEqual(0.2d, strategyAdapter.getCurrentOverrideDropRate()));;

    // if we set the QPS to be somewhere below high and low water mark then the drop rate stays the same
    // even though the latency is high
    callClients(3000, 2, clients, clock, timeInterval, false, false);
    strategyAdapter.setStrategyToCallDrop();
    getTrackerClient(strategyAdapter, request, new RequestContext(), 1, clients);
    assertTrue(isEqual(0.2d, strategyAdapter.getCurrentOverrideDropRate()));

    // now we want to degrade the cluster even further
    callClients(3000, 25, clients, clock, timeInterval, false, false);
    strategyAdapter.setStrategyToCallDrop();
    getTrackerClient(strategyAdapter, request, new RequestContext(), 1, clients);
    assertTrue(isEqual(0.4d, strategyAdapter.getCurrentOverrideDropRate()));

    callClients(3000, 25, clients, clock, timeInterval, false, false);
    strategyAdapter.setStrategyToCallDrop();
    getTrackerClient(strategyAdapter, request, new RequestContext(), 1, clients);
    assertTrue(isEqual(0.6d, strategyAdapter.getCurrentOverrideDropRate()));

    callClients(3000, 25, clients, clock, timeInterval, false, false);
    strategyAdapter.setStrategyToCallDrop();
    getTrackerClient(strategyAdapter, request, new RequestContext(), 1, clients);
    assertTrue(isEqual(0.8d, strategyAdapter.getCurrentOverrideDropRate()));

    callClients(3000, 25, clients, clock, timeInterval, false, false);
    strategyAdapter.setStrategyToCallDrop();
    getTrackerClient(strategyAdapter, request, new RequestContext(), 1, clients);
    assertTrue(isEqual(1.0d, strategyAdapter.getCurrentOverrideDropRate()));

    //if we have qps below lowWaterMark, we will reduce drop rate even though latency is high
    callClients(3000, 0.5, clients, clock, timeInterval, false, false);
    strategyAdapter.setStrategyToCallDrop();
    getTrackerClient(strategyAdapter, request, new RequestContext(), 1, clients);
    assertTrue(isEqual(0.8d, strategyAdapter.getCurrentOverrideDropRate()));

    //if we have qps below lowWaterMark and qps is low, we will also reduce drop rate
    callClients(100, 0.5, clients, clock, timeInterval, false, false);
    strategyAdapter.setStrategyToCallDrop();
    getTrackerClient(strategyAdapter, request, new RequestContext(), 1, clients);
    assertTrue(isEqual(0.6d, strategyAdapter.getCurrentOverrideDropRate()));

    //if we have qps between lowWaterMark and highWaterMark and latency is low, we will reduce drop rate
    callClients(100, 2, clients, clock, timeInterval, false, false);
    strategyAdapter.setStrategyToCallDrop();
    getTrackerClient(strategyAdapter, request, new RequestContext(), 1, clients);
    assertTrue(isEqual(0.4d, strategyAdapter.getCurrentOverrideDropRate()));

    //if we have qps higher than highWaterMark and latency is low, we will reduce drop rate
    callClients(100, 25, clients, clock, timeInterval, false, false);
    strategyAdapter.setStrategyToCallDrop();
    getTrackerClient(strategyAdapter, request, new RequestContext(), 1, clients);
    assertTrue(isEqual(0.2d, strategyAdapter.getCurrentOverrideDropRate()));
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
            new TestClock(), null);

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
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS, 5000L);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_MAX_CLUSTER_LATENCY_WITHOUT_DEGRADING, 100.0);
    // this test expected the dead tracker client to not recover through the
    // getTrackerClient mechanism. It only recovered through explicit calls to client1/client2.
    // While we have fixed this problem, keeping this testcase to show how we can completely disable
    // a tracker client through the getTrackerClient method.
    myMap.put(PropertyKeys.HTTP_LB_INITIAL_RECOVERY_LEVEL, 0.0);
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
            new TrackerClient(uri1, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri1), clock1, null);
    TrackerClient client2 =
            new TrackerClient(uri2, getDefaultPartitionData(0.8d), new TestLoadBalancerClient(uri2), clock2, null);

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
            new TestLoadBalancerClient(uri1), clock1, null);
    @SuppressWarnings("serial")
    TrackerClient client2 =  new TrackerClient(uri2,
            new HashMap<Integer, PartitionData>(){{put(0, new PartitionData(0.5d)); put(1, new PartitionData(0.5d));}},
            new TestLoadBalancerClient(uri2), clock2, null);
    @SuppressWarnings("serial")
    TrackerClient client3 =  new TrackerClient(uri3,
            new HashMap<Integer, PartitionData>(){{put(1, new PartitionData(1d));}},
            new TestLoadBalancerClient(uri3), clock3, null);


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
            new TrackerClient(uri1, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri1), clock1, null);
    TrackerClient client2 =
            new TrackerClient(uri2, getDefaultPartitionData(0.8d), new TestLoadBalancerClient(uri2), clock2, null);

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
    myConfig.put(PropertyKeys.CLOCK, testClock);
    myConfig.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS, 5000L);
    myConfig.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_MAX_CLUSTER_LATENCY_WITHOUT_DEGRADING, 100d);
    DegraderLoadBalancerStrategyV3 strategy = getStrategy(myConfig);
    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    long clusterCallCount = 15;

    clients.add(getClient(URI.create("http://test.linkedin.com:3242/fdsaf")));
    clients.add(getClient(URI.create("http://test.linkedin.com:3243/fdsaf")));

    // state is default initialized, new cluster generation
    assertTrue(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(0,
            strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));


    DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState current =
            strategy.getState().getPartitionState(DEFAULT_PARTITION_ID);

    current = new DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(0,
            testClock._currentTimeMillis,
            true,
            new HashMap<URI, Integer>(),
            DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE,
            0.0,
            -1,
            new HashMap<TrackerClient, Double>(),
            "Test",
            current.getDegraderProperties(),
            clusterCallCount);
    strategy.getState().setPartitionState(DEFAULT_PARTITION_ID, current);

    // state is not null, but we're on the same cluster generation id, and 5 seconds
    // haven't gone by
    testClock.addMs(1);
    assertFalse(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(0,
            strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));

    // generation Id for the next state is changed
    current = new DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(1,
            testClock._currentTimeMillis,
            true,
            new HashMap<URI, Integer>(),
            DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE,
            0.0,
            -1,
            new HashMap<TrackerClient, Double>(),
            "Test",
            current.getDegraderProperties(),
            clusterCallCount);
    strategy.getState().setPartitionState(DEFAULT_PARTITION_ID, current);

    // state is not null, and cluster generation has changed so we will update
    testClock.addMs(1);
    assertTrue(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(0,
            strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));

    // state is not null, and force 5s to go by with the same cluster generation id
    current = new DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(1,
            testClock._currentTimeMillis,
            true,
            new HashMap<URI, Integer>(),
            DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE,
            0.0,
            -1,
            new HashMap<TrackerClient, Double>(),
            "Test",
            current.getDegraderProperties(),
            clusterCallCount);
    strategy.getState().setPartitionState(DEFAULT_PARTITION_ID, current);

    testClock.addMs(5000);
    assertTrue(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(1,
            strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));


    current = new DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(1,
            testClock._currentTimeMillis,
            true,
            new HashMap<URI, Integer>(),
            DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE,
            0.0,
            -1,
            new HashMap<TrackerClient, Double>(),
            "Test",
            current.getDegraderProperties(),
            clusterCallCount);
    strategy.getState().setPartitionState(DEFAULT_PARTITION_ID, current);

    // now try a new cluster generation id so state will be updated again
    testClock.addMs(15);
    assertTrue(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(2,
            strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));
  }

  @Test(groups = { "small", "back-end" })
  public void testshouldUpdatePartitionOnlyAtInterval() throws URISyntaxException
  {
    Map<String,Object> myConfig = new HashMap<String,Object>();
    TestClock testClock = new TestClock();
    myConfig.put(PropertyKeys.CLOCK, testClock);
    myConfig.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS, 5000L);
    myConfig.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_MAX_CLUSTER_LATENCY_WITHOUT_DEGRADING, 100d);
    myConfig.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_ONLY_AT_INTERVAL, true);
    DegraderLoadBalancerStrategyV3 strategy = getStrategy(myConfig);
    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    long clusterCallCount = 15;

    clients.add(getClient(URI.create("http://test.linkedin.com:3242/fdsaf")));
    clients.add(getClient(URI.create("http://test.linkedin.com:3243/fdsaf")));

    // state is default initialized, new cluster generation
    assertFalse(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(0,
            strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));


    // state is not null, but we're on the same cluster generation id, and 5 seconds
    // haven't gone by
    testClock.addMs(1);
    assertFalse(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(0,
            strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));

    testClock.addMs(5000);
    assertTrue(DegraderLoadBalancerStrategyV3.shouldUpdatePartition(1,
            strategy.getState().getPartitionState(DEFAULT_PARTITION_ID), strategy.getConfig(), true));

    DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState current =
            strategy.getState().getPartitionState(DEFAULT_PARTITION_ID);
    current = new DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState(1,
            testClock._currentTimeMillis,
            true,
            new HashMap<URI, Integer>(),
            DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE,
            0.0,
            -1,
            new HashMap<TrackerClient, Double>(),
            "Test",
            current.getDegraderProperties(),
            clusterCallCount);
    strategy.getState().setPartitionState(DEFAULT_PARTITION_ID, current);
  }

  @Test(groups = { "small", "back-end" })
  public void testOverrideClusterDropRate() throws URISyntaxException
  {
    DegraderLoadBalancerStrategyV3 strategy = getStrategy();
    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    List<TrackerClientUpdater> clientUpdaters = new ArrayList<TrackerClientUpdater>();
    for (TrackerClient client : clients)
    {
      clientUpdaters.add(new TrackerClientUpdater(client, DEFAULT_PARTITION_ID));
    }

    clients.add(getClient(URI.create("http://test.linkedin.com:3242/fdsaf")));
    clients.add(getClient(URI.create("http://test.linkedin.com:3243/fdsaf")));

    DegraderLoadBalancerStrategyV3.overrideClusterDropRate(DEFAULT_PARTITION_ID, 1d, clientUpdaters);

    for (TrackerClientUpdater clientUpdater : clientUpdaters)
    {
      TrackerClient client = clientUpdater.getTrackerClient();
      clientUpdater.update();
      assertEquals(client.getDegraderControl(DEFAULT_PARTITION_ID).getOverrideDropRate(), 1d);
      assertTrue(client.getDegrader(DEFAULT_PARTITION_ID).checkDrop());
    }

    DegraderLoadBalancerStrategyV3.overrideClusterDropRate(DEFAULT_PARTITION_ID, -1d, clientUpdaters);

    // if we don't override, the degrader isn't degraded, so should not drop
    for (TrackerClientUpdater clientUpdater : clientUpdaters)
    {
      TrackerClient client = clientUpdater.getTrackerClient();
      clientUpdater.update();
      assertEquals(client.getDegraderControl(DEFAULT_PARTITION_ID).getOverrideDropRate(), -1d);
      assertFalse(client.getDegrader(DEFAULT_PARTITION_ID).checkDrop());
    }

    DegraderLoadBalancerStrategyV3.overrideClusterDropRate(DEFAULT_PARTITION_ID, 0d, clientUpdaters);

    for (TrackerClientUpdater clientUpdater : clientUpdaters)
    {
      TrackerClient client = clientUpdater.getTrackerClient();
      clientUpdater.update();
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
                    5000, true, 100, DegraderLoadBalancerStrategyV3.HASH_METHOD_URI_REGEX,
                    Collections.<String,Object>singletonMap(URIRegexHash.KEY_REGEXES,
                            Collections.singletonList("(.*)")), SystemClock.instance(),
                    DegraderLoadBalancerStrategyConfig.DEFAULT_INITIAL_RECOVERY_LEVEL,
                    DegraderLoadBalancerStrategyConfig.DEFAULT_RAMP_FACTOR,
                    DegraderLoadBalancerStrategyConfig.DEFAULT_HIGH_WATER_MARK,
                    DegraderLoadBalancerStrategyConfig.DEFAULT_LOW_WATER_MARK,
                    DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_UP,
                    DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_DOWN,
                    DegraderLoadBalancerStrategyConfig.DEFAULT_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK,
                    DegraderLoadBalancerStrategyConfig.DEFAULT_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK),
            "DegraderLoadBalancerTest", null);
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
    myMap.put(PropertyKeys.CLOCK, clock);
    // This recovery level will put one point into the hash ring, which is good enough to
    // send traffic to it because it is the only member of the cluster.
    myMap.put("initialRecoverLevel", 0.01);
    myMap.put(PropertyKeys.HTTP_LB_RING_RAMP_FACTOR, 2.0);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS, timeInterval);
    int stepsToFullRecovery = 0;

    //test Strategy V3
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategyV3 = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);
    DegraderLoadBalancerStrategyAdapter strategy = new DegraderLoadBalancerStrategyAdapter(strategyV3);
    clusterRecovery1TC(myMap, clock, stepsToFullRecovery, timeInterval, strategy, null,
            DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE);

    //test Strategy V2
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV2_1 strategyV2 = new DegraderLoadBalancerStrategyV2_1(config, "DegraderLoadBalancerTest",
            null);
    strategy = new DegraderLoadBalancerStrategyAdapter(strategyV2);
    clusterRecovery1TC(myMap, clock, stepsToFullRecovery, timeInterval, strategy,
            DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState.Strategy.LOAD_BALANCE,
            null);
  }

  @Test(groups = { "small", "back-end" })
  public void testClusterRecoverySlow1TC()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    // We want the degrader to have one cooling off period, and then re-enter the ring. This
    myMap.put(PropertyKeys.HTTP_LB_INITIAL_RECOVERY_LEVEL, 0.005);
    myMap.put(PropertyKeys.HTTP_LB_RING_RAMP_FACTOR, 2.0);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS, timeInterval);
    // it will take two intervals for the TC to be reintroduced into the hash ring.
    int stepsToFullRecovery = 1;

    //test Strategy V3
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategyV3 = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);
    DegraderLoadBalancerStrategyAdapter strategy = new DegraderLoadBalancerStrategyAdapter(strategyV3);
    clusterRecovery1TC(myMap, clock, stepsToFullRecovery, timeInterval, strategy, null,
            DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.LOAD_BALANCE);

    //test Strategy V2
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV2_1 strategyV2 = new DegraderLoadBalancerStrategyV2_1(config, "DegraderLoadBalancerTest",
            null);
    strategy = new DegraderLoadBalancerStrategyAdapter(strategyV2);
    clusterRecovery1TC(myMap, clock, stepsToFullRecovery, timeInterval, strategy,
            DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState.Strategy.LOAD_BALANCE,
            null);
  }

  @Test(groups = { "small", "back-end"})
  public void stressTest()
  {
    final DegraderLoadBalancerStrategyV3 strategyV3 = getStrategy();
    TestClock testClock = new TestClock();
    String baseUri = "http://linkedin.com:9999";
    int numberOfPartitions = 10;
    Map<String, String> degraderProperties = new HashMap<String,String>();
    degraderProperties.put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, "0.5");
    degraderProperties.put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, "0.2");
    DegraderImpl.Config degraderConfig = DegraderConfigFactory.toDegraderConfig(degraderProperties);
    final List<TrackerClient> clients = new ArrayList<TrackerClient>();
    for (int i = 0; i < numberOfPartitions; i++)
    {
      URI uri = URI.create(baseUri + i);
      TrackerClient client =   new TrackerClient(uri,
              getDefaultPartitionData(1, numberOfPartitions),
              new TestLoadBalancerClient(uri), testClock, degraderConfig);
      clients.add(client);
    }

    final ExecutorService executor = Executors.newFixedThreadPool(100);
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch finishLatch = new CountDownLatch(100);
    try
    {
      for (int i = 0; i < numberOfPartitions; i++)
      {
        Assert.assertFalse(strategyV3.getState().getPartitionState(i).isInitialized());
      }
      for (int i = 0; i < 100; i++)
      {
        final int partitionId = i % numberOfPartitions;
        executor.submit(new Runnable()
        {
          @Override
          public void run()
          {
            try
            {
              startLatch.await();
            }
            catch (InterruptedException ex)
            {

            }
            strategyV3.getRing(1, partitionId, clients);
            finishLatch.countDown();
          }
        });
      }
      // all threads would try to getRing simultanously
      startLatch.countDown();
      if (!finishLatch.await(10, TimeUnit.SECONDS))
      {
        fail("Stress test failed to finish within 10 seconds");
      }
      for (int i = 0; i < numberOfPartitions; i++)
      {
        Assert.assertTrue(strategyV3.getState().getPartitionState(i).isInitialized());
      }
    }
    catch (InterruptedException ex)
    {

    }
    finally
    {
      executor.shutdownNow();
    }

  }

  @Test(groups = { "small", "back-end"})
  public void testResizeProblem()
  {
    URI uri = URI.create("http://linkedin.com:9999");

    final ExecutorService executor = Executors.newFixedThreadPool(2);
    int totalSuccessfulInitialization = 0;

    try
    {
      for (int i = 0; i < 20000; i++)
      {
        CountDownLatch joinLatch = new CountDownLatch(2);
        TestClock clock = new TestClock();
        final DegraderLoadBalancerStrategyV3 strategy = getStrategy();
        for (Runnable runnable : createRaceCondition(uri, clock, strategy, joinLatch))
        {
          executor.submit(runnable);
        }
        try
        {
          if (!joinLatch.await(10, TimeUnit.SECONDS))
          {
            fail("Update or resize failed to finish within 10 seconds");
          }
          // Before the fix the resize problem, initialization for partiion 0 would fail if the following race condition happened:
          // thread A sets updateStarted flag for partition 0, thread B resizes partition count and
          // copies state for partition0 with updateStarted == true, thread A clears the flag and updates state for partition 0,
          // thread B swaps in the new array of states for enlarged number of partitions, finishes resize, ignoring thread A's update

          // Now with the fix, we expect the above not to happen
          DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState state = strategy.getState().getPartitionState(0);
          totalSuccessfulInitialization += state.isInitialized() ? 1 : 0;
        }
        catch (InterruptedException ex)
        {
        }
      }
    }
    finally
    {
      executor.shutdownNow();
    }
    assertEquals(totalSuccessfulInitialization, 20000);
  }

  private static class EvilClient extends TrackerClient
  {
    private final CountDownLatch _latch;
    public EvilClient(URI uri, Map<Integer, PartitionData> partitionDataMap, TransportClient wrappedClient,
                      Clock clock, DegraderImpl.Config config, CountDownLatch latch)
    {
      super(uri, partitionDataMap, wrappedClient, clock, config);
      _latch = latch;
    }

    @Override
    public Double getPartitionWeight(int partitionId)
    {
      if(partitionId == 0)
      {
        try
        {
          // wait for latch after setting updateStarted to true
          _latch.await(5, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException ex)
        {
          // do nothing
        }
      }
      return super.getPartitionWeight(partitionId);
    }
  }

  private List<Runnable> createRaceCondition(final URI uri, Clock clock, final DegraderLoadBalancerStrategyV3 strategy, final CountDownLatch joinLatch)
  {
    final CountDownLatch clientLatch = new CountDownLatch(1);
    TrackerClient evilClient = new EvilClient(uri, getDefaultPartitionData(1, 2), new TrackerClientTest.TestClient(),
            clock, null, clientLatch);
    final List<TrackerClient> clients = Collections.singletonList(evilClient);
    final Runnable update = new Runnable()
    {
      @Override
      public void run()
      {
        // getRing will wait for latch in getPartitionWeight
        strategy.getRing(1, 0, clients);
        joinLatch.countDown();
      }
    };

    final Runnable resize = new Runnable()
    {
      @Override
      public void run()
      {
        // releases latch for partition 0
        clientLatch.countDown();
        // resize
        strategy.getRing(1, 1, clients);
        joinLatch.countDown();
      }
    };

    List<Runnable> actions = new ArrayList<Runnable>();
    actions.add(update);
    actions.add(resize);
    return actions;
  }

  @Test(groups = { "small", "back-end" }, dataProvider = "clientGlitch", enabled = false)
  /** The strategy recovers after a TrackerClient throws one Exception. */
  public void testClientGlitch(final int numberOfPartitions,
                               final LoadBalancerStrategy strategy,
                               final TestClock clock,
                               final long timeInterval)
          throws Exception
  {
    final List<TrackerClient> client = Collections.<TrackerClient>singletonList(new ErrorClient(1, numberOfPartitions, clock));
    final int partitionId = DefaultPartitionAccessor.DEFAULT_PARTITION_ID + numberOfPartitions - 1;
    final Callable<Ring<URI>> getRing = new Callable<Ring<URI>>()
    {
      @Override
      public Ring<URI> call()
      {
        return strategy.getRing(1L, partitionId, client);
      }
    };
    try
    {
      getRing.call(); // initialization
      fail("no glitch");
    }
    catch (DummyCheckedException expectedGlitch)
    {
    }
    final int numberOfThreads = 5;
    final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    try
    {
      final List<Future<Ring<URI>>> results = new ArrayList<Future<Ring<URI>>>();
      for (int r = 0; r < numberOfThreads; ++r)
        results.add(executor.submit(getRing));
      clock.addMs(timeInterval);
      Thread.sleep(timeInterval * 2); // During this time,
      // one of the threads should initialize the partition state,
      // and then all of the threads should get the new ring.
      assertEquals(countDone(results), results.size());
    }
    finally
    {
      executor.shutdownNow();
    }
  }

  @DataProvider(name = "clientGlitch")
  public Object[][] clientGlitch()
  {
    long timeInterval = 10; // msec
    TestClock clock = new TestClock();
    Map<String, Object> props = new HashMap<String, Object>();
    props.put(PropertyKeys.CLOCK, clock);
    // We want the degrader to re-enter the ring after one cooling off period:
    props.put(PropertyKeys.HTTP_LB_INITIAL_RECOVERY_LEVEL, 0.005);
    props.put(PropertyKeys.HTTP_LB_RING_RAMP_FACTOR, 1.0);
    props.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS, timeInterval);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(props);
    return new Object[][]{clientGlitchV2(config), clientGlitchV3(1, config), clientGlitchV3(3, config)};
  }

  private Object[] clientGlitchV2(DegraderLoadBalancerStrategyConfig config)
  {
    return new Object[]{1,
            new DegraderLoadBalancerStrategyV2_1(config, "DegraderLoadBalancerTest.V2", null),
            config.getClock(),
            config.getUpdateIntervalMs()};
  }

  private Object[] clientGlitchV3(int numberOfPartitions, DegraderLoadBalancerStrategyConfig config)
  {
    return new Object[]{numberOfPartitions,
            new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest.V3", null),
            config.getClock(),
            config.getUpdateIntervalMs()};
  }

  /** A TrackerClient that throws some DummyCheckedExceptions before starting normal operation. */
  private static class ErrorClient extends TrackerClient
  {
    private static final URI myURI = URI.create("http://nonexistent.nowhere.linkedin.com:9999/ErrorClient");
    private final AtomicLong _numberOfExceptions;

    ErrorClient(int numberOfExceptions, int numberOfPartitions, Clock clock)
    {
      super(myURI, getDefaultPartitionData(1d, numberOfPartitions), new TestLoadBalancerClient(myURI), clock, null);
      _numberOfExceptions = new AtomicLong(numberOfExceptions);
    }

    @Override
    public Double getPartitionWeight(int partitionId)
    {
      if (_numberOfExceptions.getAndDecrement() > 0)
        new DummyCheckedException().throwMe();
      return super.getPartitionWeight(partitionId);
    }

    @Override
    public DegraderControl getDegraderControl(int partitionId)
    {
      if (_numberOfExceptions.getAndDecrement() > 0)
        new DummyCheckedException().throwMe();
      return super.getDegraderControl(partitionId);
    }
  }

  /** Count how many of the given futures are done. */
  private static int countDone(Iterable<? extends Future<?>> results)
          throws ExecutionException, InterruptedException
  {
    int done = 0;
    for (Future<?> result : results)
      try
      {
        result.get(1, TimeUnit.MILLISECONDS);
        ++done;
      }
      catch (TimeoutException notDone)
      {
      }
    return done;
  }

  private static final AtomicLong THREAD_POOL_NUMBER = new AtomicLong(0);

  private static class DegraderLoadBalancerStrategyAdapter implements LoadBalancerStrategy
  {

    final DegraderLoadBalancerStrategyV3 _strategyV3;
    final DegraderLoadBalancerStrategyV2_1 _strategyV2;
    final LoadBalancerStrategy _strategy;

    private DegraderLoadBalancerStrategyAdapter(DegraderLoadBalancerStrategyV2_1 strategyV2)
    {
      _strategyV2 = strategyV2;
      _strategyV3 = null;
      _strategy = strategyV2;
    }

    private DegraderLoadBalancerStrategyAdapter(DegraderLoadBalancerStrategyV3 strategyV3)
    {
      _strategyV2 = null;
      _strategyV3 = strategyV3;
      _strategy = strategyV3;
    }

    public double getOverrideDropRate()
    {
      if (_strategyV2 != null)
      {
        return _strategyV2.getCurrentOverrideDropRate();
      }
      else
      {
        return _strategyV3.getState().getPartitionState(DEFAULT_PARTITION_ID).getCurrentOverrideDropRate();
      }
    }

    public Map<URI, Integer> getPointsMap()
    {
      if (_strategyV2 != null)
      {
        return _strategyV2.getState().getPointsMap();
      }
      else
      {
        return _strategyV3.getState().getPartitionState(DEFAULT_PARTITION_ID).getPointsMap();
      }
    }

    public TrackerClient getTrackerClient(Request request,
                                          RequestContext requestContext,
                                          long clusterGenerationId,
                                          int partitionId,
                                          List<TrackerClient> trackerClients)
    {
      return _strategy.getTrackerClient(request, requestContext, clusterGenerationId, partitionId, trackerClients);
    }

    public Ring<URI> getRing(long clusterGenerationId, int partitionId, List<TrackerClient> trackerClients)
    {
      return _strategy.getRing(clusterGenerationId, partitionId, trackerClients);
    }

    public void setStrategyV3(int partitionID,
                              DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy
                                      strategy)
    {
      _strategyV3.setStrategy(partitionID, strategy);
    }

    public void setStrategyV2(DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState.Strategy strategy)
    {
      _strategyV2.setStrategy(strategy);
    }

    public boolean isStrategyCallDrop()
    {
      if (_strategyV2 != null)
      {
        return _strategyV2.getState().getStrategy() ==
                DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState.Strategy.CALL_DROPPING;
      }
      else
      {
        return _strategyV3.getState().getPartitionState(DEFAULT_PARTITION_ID).getStrategy() ==
                DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.CALL_DROPPING;
      }
    }

    public void setStrategyToCallDrop()
    {
      if (_strategyV2 != null)
      {
        _strategyV2.setStrategy(DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState.Strategy.CALL_DROPPING);
      }
      else if (_strategyV3 != null)
      {
        _strategyV3.setStrategy(DEFAULT_PARTITION_ID,
                DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy.
                        CALL_DROPPING);
      }
      else
      {
        throw new IllegalStateException("should have either strategyV2 or V3");
      }
    }

    public double getCurrentOverrideDropRate()
    {
      if (_strategyV2 != null)
      {
        return _strategyV2.getCurrentOverrideDropRate();
      }
      else
      {
        return _strategyV3.getState().getPartitionState(DEFAULT_PARTITION_ID).getCurrentOverrideDropRate();
      }
    }
  }

  @Test(groups = { "small", "back-end" })
  public void testClusterRecoveryAfter100PercentDropCall()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    // We want the degrader to have one cooling off period, and then re-enter the ring. This
    myMap.put(PropertyKeys.HTTP_LB_INITIAL_RECOVERY_LEVEL, 0.005);
    myMap.put(PropertyKeys.HTTP_LB_RING_RAMP_FACTOR, 2.0);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS, timeInterval);
    myMap.put(PropertyKeys.HTTP_LB_GLOBAL_STEP_UP, 1.0);
    myMap.put(PropertyKeys.HTTP_LB_GLOBAL_STEP_DOWN, 0.8);
    myMap.put(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK, 1l);
    myMap.put(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK, 1l);


    //test Strategy V3
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategyV3 = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);
    DegraderLoadBalancerStrategyAdapter strategy = new DegraderLoadBalancerStrategyAdapter(strategyV3);
    clusterTotalRecovery1TC(myMap,
            clock,
            timeInterval,
            strategy);

    //test Strategy V2
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV2_1 strategyV2 = new DegraderLoadBalancerStrategyV2_1(config, "DegraderLoadBalancerTest",
            null);
    strategy = new DegraderLoadBalancerStrategyAdapter(strategyV2);
    clusterTotalRecovery1TC(myMap, clock, timeInterval, strategy);
  }

  /**
   * simulates the situation where a cluster latency gets so high that we will reduce the number of
   * points in hashring to 0 and then increase the call drop rate to 1.0
   * This will causes the cluster to receive no traffic and we want to see if the cluster can recover
   * from such situation.
   * @param myMap
   * @param clock
   * @param timeInterval
   * @param strategy
   */
  public void clusterTotalRecovery1TC(Map<String, Object> myMap, TestClock clock,
                                      Long timeInterval,
                                      DegraderLoadBalancerStrategyAdapter strategy)
  {
    final int NUM_CHECKS = 5;
    final Long TIME_INTERVAL = timeInterval;

    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);

    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");
    URIRequest request = new URIRequest(uri1);

    TrackerClient client1 =
            new TrackerClient(uri1, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri1), clock, null);

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
    clock.addMs(3500);

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

    // now we mimic the high latency and force the state to drop all calls so to make
    // the overrideClusterDropRate to 1.0
    ccList = new ArrayList<CallCompletion>();
    for (int j = 0; j < NUM_CHECKS; j++)
    {
      cc = client1.getCallTracker().startCall();

      ccList.add(cc);
    }

    //make sure that the latency is really high
    clock.addMs(3500);

    for (Iterator<CallCompletion> iter = ccList.listIterator(); iter.hasNext();)
    {
      cc = iter.next();
      cc.endCallWithError();
      iter.remove();
    }

    // go to next time interval.
    clock.addMs(TIME_INTERVAL);

    // trigger a state update
    resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);

    //this time the cluster override drop rate is set to 1.0 so resultTC should be null because we drop the client
    assertNull(resultTC);
    assertEquals(strategy.getCurrentOverrideDropRate(), config.getGlobalStepUp());

    // add another time interval
    clock.addMs(TIME_INTERVAL);

    // usually we alternate between LoadBalancing and CallDropping strategy but we want to test
    // call dropping strategy
    strategy.setStrategyToCallDrop();

    // we simulate call drop by not calling callCompletion endCall() or endCallWithEror() like we did above
    // because override drop rate is set to 1.0 that means all call will be dropped so resultTc should be null
    resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);

    // this time the cluster override drop rate is set to 0.2 because we're recovering
    assertEquals(strategy.getCurrentOverrideDropRate(), 1 - config.getGlobalStepDown());

    // add another time interval
    clock.addMs(TIME_INTERVAL);

    // set the strategy to callDropping again
    strategy.setStrategyToCallDrop();

    // because override drop rate is set to 0.2 and we simulate as if we still don't get any call
    // this cycle we will set the override drop rate to 0
    resultTC = getTrackerClient(strategy, request, new RequestContext(), 1, clients);
    assertEquals(strategy.getCurrentOverrideDropRate(), 0.0);
  }

  /**
   * helper method to test DegraderLoadBalancerStrategy recovery with 1 TrackerClient.
   *
   * We want to test DegraderV2 and V3 with 2 different strategies : LoadBalacing and Call Dropping.
   * So this method needs to able to handle all 4 permutations.
   *
   * @param myMap
   * @param clock
   * @param stepsToFullRecovery
   * @param timeInterval
   * @param strategy
   */
  public void clusterRecovery1TC(Map<String, Object> myMap, TestClock clock,
                                 int stepsToFullRecovery, Long timeInterval,
                                 DegraderLoadBalancerStrategyAdapter strategy,
                                 DegraderLoadBalancerStrategyV2_1.DegraderLoadBalancerState.Strategy strategyV2,
                                 DegraderLoadBalancerStrategyV3.PartitionDegraderLoadBalancerState.Strategy strategyV3)
  {
    final int NUM_CHECKS = 5;
    final Long TIME_INTERVAL = timeInterval;
    int localStepsToFullRecovery = stepsToFullRecovery;

    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);

    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");
    URIRequest request = new URIRequest(uri1);

    TrackerClient client1 =
            new TrackerClient(uri1, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri1), clock, null);

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
        if (strategyV3 != null)
        {
          strategy.setStrategyV3(DEFAULT_PARTITION_ID, strategyV3);
        }
        else if (strategyV2 != null)
        {
          strategy.setStrategyV2(strategyV2);
        }
        else
        {
          fail("should set strategy (either LoadBalance or Degrader");
        }
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

  /**
   * create multiple trackerClients using the same clock
   * @return
   */
  private List<TrackerClient> createTrackerClient(int n, TestClock clock, DegraderImpl.Config config)
  {
    String baseUri = "http://test.linkedin.com:10010/abc";
    List<TrackerClient> result = new LinkedList<TrackerClient>();
    for (int i = 0; i < n; i++)
    {
      URI uri = URI.create(baseUri + i);
      TrackerClient client =   new TrackerClient(uri,
              getDefaultPartitionData(1d),
              new TestLoadBalancerClient(uri), clock, config);
      result.add(client);
    }
    return result;
  }

  /**
   * simulates calling clients
   * @param milliseconds latency of the call
   * @param qps qps of traffic per client
   * @param clients list of clients being called
   * @param clock
   * @param timeInterval
   * @param withError calling client with error that we don't use for load balancing (any generic error)
   * @param withQualifiedDegraderError calling client with error that we use for load balancing
   */
  private void callClients(long milliseconds, double qps, List<TrackerClient> clients, TestClock clock,
                           long timeInterval, boolean withError, boolean withQualifiedDegraderError)
  {
    LinkedList<CallCompletion> callCompletions = new LinkedList<CallCompletion>();
    int callHowManyTimes = (int)((qps * timeInterval) / 1000);
    for (int i = 0; i < callHowManyTimes; i++)
    {
      for (TrackerClient client : clients)
      {
        CallCompletion cc = client.getCallTracker().startCall();
        callCompletions.add(cc);
      }
    }
    Random random = new Random();
    clock.addMs(milliseconds);

    for (CallCompletion cc : callCompletions)
    {
      if (withError)
      {
        cc.endCallWithError();
      }
      else if (withQualifiedDegraderError)
      {
        //choose a random error type
        if (random.nextBoolean())
        {
          cc.endCallWithError(ErrorType.CLOSED_CHANNEL_EXCEPTION);
        }
        else
        {
          cc.endCallWithError(ErrorType.CONNECT_EXCEPTION);
        }
      }
      else
      {
        cc.endCall();
      }
    }
    //complete a full interval cycle
    clock.addMs(timeInterval - (milliseconds % timeInterval));
  }

  /**
   * simulates calling all the clients with the given QPS according to the given interval.
   * Then verify that the DegraderLoadBalancerState behaves as expected.
   * @param expectedPointsPerClient we'll verify if the points are smaller than the number given here
   * @param isCalledWithError
   * @param isCalledWithErrorForLoadBalancing
   */
  private TrackerClient simulateAndTestOneInterval(long timeInterval,
                                                   TestClock clock,
                                                   double qps,
                                                   List<TrackerClient> clients,
                                                   DegraderLoadBalancerStrategyAdapter adapter,
                                                   long clusterGenerationId,
                                                   Integer expectedPointsPerClient,
                                                   boolean isExpectingDropCallStrategyForNewState,
                                                   double expectedClusterOverrideDropRate,
                                                   long latency,
                                                   boolean isCalledWithError,
                                                   boolean isCalledWithErrorForLoadBalancing)
  {
    callClients(latency, qps, clients, clock, timeInterval, isCalledWithError, isCalledWithErrorForLoadBalancing);
    //create any random URIRequest because we just need a URI to be hashed to get the point in hash ring anyway
    if (clients != null && !clients.isEmpty())
    {
      URIRequest request = new URIRequest(clients.get(0).getUri());
      TrackerClient client = getTrackerClient(adapter, request, new RequestContext(), clusterGenerationId, clients);
      Map<URI, Integer> pointsMap = adapter.getPointsMap();
      for (TrackerClient trackerClient : clients)
      {
        Integer pointsInTheRing = pointsMap.get(trackerClient.getUri());
        assertEquals(pointsInTheRing, expectedPointsPerClient);
      }
      if (isExpectingDropCallStrategyForNewState)
      {
        assertTrue(adapter.isStrategyCallDrop());
      }
      else
      {
        assertFalse(adapter.isStrategyCallDrop());
      }
      assertEquals(adapter.getCurrentOverrideDropRate(), expectedClusterOverrideDropRate);
      return client;
    }
    return null;
  }

  @Test(groups = { "small", "back-end" })
  public void testLowTrafficHighLatency1Client()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS , timeInterval);
    myMap.put(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK, 1l);
    myMap.put(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK, 1l);
    //we need to override the min call count to 0 because we're testing a service with low traffic.
    //if we don't do this, the computedDropRate will not change and we will never be able to recover
    //after we degraded the cluster.
    Map<String,String> degraderImplProperties = new HashMap<String, String>();
    degraderImplProperties.put(PropertyKeys.DEGRADER_MIN_CALL_COUNT, "1");
    degraderImplProperties.put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, "0.5");
    degraderImplProperties.put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, "0.2");
    DegraderImpl.Config degraderConfig = DegraderConfigFactory.toDegraderConfig(degraderImplProperties);
    double qps = 0.3;

    //test Strategy V3
    List<TrackerClient> clients = createTrackerClient(1, clock, degraderConfig);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategyV3 = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);
    DegraderLoadBalancerStrategyAdapter strategy = new DegraderLoadBalancerStrategyAdapter(strategyV3);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);

    //test Strategy V2
    clients = createTrackerClient(1, clock, degraderConfig);
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV2_1 strategyV2 = new DegraderLoadBalancerStrategyV2_1(config, "DegraderLoadBalancerTest",
            null);
    strategy = new DegraderLoadBalancerStrategyAdapter(strategyV2);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);
  }

  @Test(groups = { "small", "back-end" })
  public void testLowTrafficHighLatency10Clients()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS , timeInterval);
    //we need to override the min call count to 0 because we're testing a service with low traffic.
    //if we don't do this, the computedDropRate will not change and we will never be able to recover
    //after we degraded the cluster.
    Map<String,String> degraderImplProperties = new HashMap<String, String>();
    degraderImplProperties.put(PropertyKeys.DEGRADER_MIN_CALL_COUNT, "1");
    degraderImplProperties.put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, "0.5");
    degraderImplProperties.put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, "0.2");
    DegraderImpl.Config degraderConfig = DegraderConfigFactory.toDegraderConfig(degraderImplProperties);
    double qps = 0.3;

    //test Strategy V3
    List<TrackerClient> clients = createTrackerClient(10, clock, degraderConfig);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategyV3 = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);
    DegraderLoadBalancerStrategyAdapter strategy = new DegraderLoadBalancerStrategyAdapter(strategyV3);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);

    //test Strategy V2
    clients = createTrackerClient(10, clock, degraderConfig);
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV2_1 strategyV2 = new DegraderLoadBalancerStrategyV2_1(config, "DegraderLoadBalancerTest",
            null);
    strategy = new DegraderLoadBalancerStrategyAdapter(strategyV2);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);
  }


  @Test(groups = { "small", "back-end" })
  public void testLowTrafficHighLatency100Clients()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS , timeInterval);
    //we need to override the min call count to 0 because we're testing a service with low traffic.
    //if we don't do this, the computedDropRate will not change and we will never be able to recover
    //after we degraded the cluster.
    Map<String,String> degraderImplProperties = new HashMap<String, String>();
    degraderImplProperties.put(PropertyKeys.DEGRADER_MIN_CALL_COUNT, "1");
    degraderImplProperties.put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, "0.5");
    degraderImplProperties.put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, "0.2");
    DegraderImpl.Config degraderConfig = DegraderConfigFactory.toDegraderConfig(degraderImplProperties);
    double qps = 0.3;

    //test Strategy V3
    List<TrackerClient> clients = createTrackerClient(100, clock, degraderConfig);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategyV3 = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);
    DegraderLoadBalancerStrategyAdapter strategy = new DegraderLoadBalancerStrategyAdapter(strategyV3);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);

    //test Strategy V2
    clients = createTrackerClient(100, clock, degraderConfig);
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV2_1 strategyV2 = new DegraderLoadBalancerStrategyV2_1(config, "DegraderLoadBalancerTest",
            null);
    strategy = new DegraderLoadBalancerStrategyAdapter(strategyV2);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);
  }


  @Test(groups = { "small", "back-end" })
  public void testMediumTrafficHighLatency1Client()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS , timeInterval);
    Map<String, String> degraderProperties = new HashMap<String,String>();
    degraderProperties.put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, "0.5");
    degraderProperties.put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, "0.2");
    DegraderImpl.Config degraderConfig = DegraderConfigFactory.toDegraderConfig(degraderProperties);
    double qps = 5.7;

    //test Strategy V3
    List<TrackerClient> clients = createTrackerClient(1, clock, degraderConfig);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategyV3 = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);
    DegraderLoadBalancerStrategyAdapter strategy = new DegraderLoadBalancerStrategyAdapter(strategyV3);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);

    //test Strategy V2
    clients = createTrackerClient(1, clock, degraderConfig);
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV2_1 strategyV2 = new DegraderLoadBalancerStrategyV2_1(config, "DegraderLoadBalancerTest",
            null);
    strategy = new DegraderLoadBalancerStrategyAdapter(strategyV2);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);
  }

  @Test(groups = { "small", "back-end" })
  public void testMediumTrafficHighLatency10Clients()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS , timeInterval);
    Map<String, String> degraderProperties = new HashMap<String,String>();
    degraderProperties.put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, "0.5");
    degraderProperties.put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, "0.2");
    DegraderImpl.Config degraderConfig = DegraderConfigFactory.toDegraderConfig(degraderProperties);
    double qps = 6.3;

    //test Strategy V3
    List<TrackerClient> clients = createTrackerClient(10, clock, degraderConfig);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategyV3 = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);
    DegraderLoadBalancerStrategyAdapter strategy = new DegraderLoadBalancerStrategyAdapter(strategyV3);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);

    //test Strategy V2
    clients = createTrackerClient(10, clock, degraderConfig);
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV2_1 strategyV2 = new DegraderLoadBalancerStrategyV2_1(config, "DegraderLoadBalancerTest",
            null);
    strategy = new DegraderLoadBalancerStrategyAdapter(strategyV2);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);
  }


  @Test(groups = { "small", "back-end" })
  public void testMediumTrafficHighLatency100Clients()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS , timeInterval);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS , timeInterval);
    Map<String, String> degraderProperties = new HashMap<String,String>();
    degraderProperties.put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, "0.5");
    degraderProperties.put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, "0.2");
    DegraderImpl.Config degraderConfig = DegraderConfigFactory.toDegraderConfig(degraderProperties);
    double qps = 7.3;

    //test Strategy V3
    List<TrackerClient> clients = createTrackerClient(100, clock, degraderConfig);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategyV3 = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);
    DegraderLoadBalancerStrategyAdapter strategy = new DegraderLoadBalancerStrategyAdapter(strategyV3);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);

    //test Strategy V2
    clients = createTrackerClient(100, clock, degraderConfig);
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV2_1 strategyV2 = new DegraderLoadBalancerStrategyV2_1(config, "DegraderLoadBalancerTest",
            null);
    strategy = new DegraderLoadBalancerStrategyAdapter(strategyV2);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);
  }


  @Test(groups = { "small", "back-end" })
  public void testHighTrafficHighLatency1Client()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS , timeInterval);
    Map<String, String> degraderProperties = new HashMap<String,String>();
    degraderProperties.put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, "0.5");
    degraderProperties.put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, "0.2");
    DegraderImpl.Config degraderConfig = DegraderConfigFactory.toDegraderConfig(degraderProperties);
    double qps = 121;

    //test Strategy V3
    List<TrackerClient> clients = createTrackerClient(1, clock, degraderConfig);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategyV3 = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);
    DegraderLoadBalancerStrategyAdapter strategy = new DegraderLoadBalancerStrategyAdapter(strategyV3);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);

    //test Strategy V2
    clients = createTrackerClient(1, clock, degraderConfig);
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV2_1 strategyV2 = new DegraderLoadBalancerStrategyV2_1(config, "DegraderLoadBalancerTest",
            null);
    strategy = new DegraderLoadBalancerStrategyAdapter(strategyV2);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);
  }

  @Test(groups = { "small", "back-end" })
  public void testHighTrafficHighLatency10Clients()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS , timeInterval);
    Map<String, String> degraderProperties = new HashMap<String,String>();
    degraderProperties.put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, "0.5");
    degraderProperties.put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, "0.2");
    DegraderImpl.Config degraderConfig = DegraderConfigFactory.toDegraderConfig(degraderProperties);
    double qps = 93;

    //test Strategy V3
    List<TrackerClient> clients = createTrackerClient(10, clock, degraderConfig);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategyV3 = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);
    DegraderLoadBalancerStrategyAdapter strategy = new DegraderLoadBalancerStrategyAdapter(strategyV3);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);

    //test Strategy V2
    clients = createTrackerClient(10, clock, degraderConfig);
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV2_1 strategyV2 = new DegraderLoadBalancerStrategyV2_1(config, "DegraderLoadBalancerTest",
            null);
    strategy = new DegraderLoadBalancerStrategyAdapter(strategyV2);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);
  }


  @Test(groups = { "small", "back-end" })
  public void testHighTrafficHighLatency100Clients()
  {
    Map<String, Object> myMap = new HashMap<String, Object>();
    Long timeInterval = 5000L;
    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS , timeInterval);
    Map<String, String> degraderProperties = new HashMap<String,String>();
    degraderProperties.put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, "0.5");
    degraderProperties.put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, "0.2");
    DegraderImpl.Config degraderConfig = DegraderConfigFactory.toDegraderConfig(degraderProperties);
    double qps = 88;

    //test Strategy V3
    List<TrackerClient> clients = createTrackerClient(100, clock, degraderConfig);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategyV3 = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);
    DegraderLoadBalancerStrategyAdapter strategy = new DegraderLoadBalancerStrategyAdapter(strategyV3);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);

    //test Strategy V2
    clients = createTrackerClient(100, clock, degraderConfig);
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV2_1 strategyV2 = new DegraderLoadBalancerStrategyV2_1(config, "DegraderLoadBalancerTest",
            null);
    strategy = new DegraderLoadBalancerStrategyAdapter(strategyV2);
    testDegraderLoadBalancerSimulator(strategy, clock, timeInterval, clients, qps, degraderConfig);
  }

  private void testDegraderLoadBalancerSimulator(DegraderLoadBalancerStrategyAdapter adapter,
                                                 TestClock clock,
                                                 long timeInterval,
                                                 List<TrackerClient> clients,
                                                 double qps,
                                                 DegraderImpl.Config degraderConfig)
  {

    long clusterGenerationId = 1;
    double overrideDropRate = 0.0;

    //simulate latency 4000 ms
    //1st round we use LOAD_BALANCING strategy. Since we have a high latency we will decrease the number of points
    //from 100 to 80 (transmissionRate * points per weight).
    TrackerClient resultTC = simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            80, true, 0.0, 4000, false, false);
    assertNotNull(resultTC);

    //2nd round drop rate should be increased by DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_UP
    overrideDropRate += DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_UP;
    resultTC = simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            80, false,
            overrideDropRate, 4000, false, false);

    //3rd round. We alternate back to LOAD_BALANCING strategy and we drop the points even more
    resultTC = simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            39, true,
            overrideDropRate, 4000, false, false);

    //4th round. The drop rate should be increased again like 2nd round
    overrideDropRate += DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_UP;
    resultTC = simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            39, false,
            overrideDropRate, 4000, false, false);

    //5th round. Alternate to changing hash ring again.
    resultTC = simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            1, true,
            overrideDropRate, 4000, false, false);

    //6th round. Same as 5th round, we'll increase the drop rate
    overrideDropRate += DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_UP;
    resultTC = simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            1, false,
            overrideDropRate, 4000, false, false);

    //7th round. The # of point in hashring is at the minimum so we can't decrease it further. At this point the client
    //is in recovery mode. But since we can't change the hashring anymore, we'll always in CALL_DROPPING mode
    //so the next strategy is expected to be LOAD_BALANCING mode.
    overrideDropRate += DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_UP;
    resultTC = simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            1, false,
            overrideDropRate, 4000, false, false);

    //8th round. We'll increase the drop rate to the max.
    overrideDropRate += DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_UP;
    resultTC = simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            1, false,
            overrideDropRate, 4000, false, false);

    //9th round, now we'll simulate as if there still a call even though we drop 100% of all request to get
    //tracker client. The assumption is there's some thread that still holds tracker client and we want
    //to make sure we can handle the request and we can't degrade the cluster even further.
    resultTC = simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            1, false,
            overrideDropRate, 4000, false, false);

    //10th round, now we'll simulate as if there's no call because we dropped all request
    //even though we are in LOAD_BALANCING mode and this tracker client is in recovery mode and there's no call
    //so the hashring doesn't change so we go back to reducing the drop rate to 0.8 and that means the next
    //strategy is LOAD_BALANCE
    overrideDropRate -= DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_DOWN;
    resultTC = simulateAndTestOneInterval(timeInterval, clock, 0.0, clients, adapter, clusterGenerationId,
            1, false,
            overrideDropRate, 4000, false, false);

    //11th round, this time we'll simulate the latency is now 1000 ms (so it's within low and high watermark). Drop rate
    //should stay the same and everything else should stay the same
    resultTC = simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            1, false,
            overrideDropRate, 1000, false, false);

    //we'll simulate the client dying one by one until all the clients are gone
    int numberOfClients = clients.size();
    HashSet<URI> uris = new HashSet<URI>();
    HashSet<URI> removedUris = new HashSet<URI>();
    for (TrackerClient client : clients)
    {
      uris.add(client.getUri());
    }
    LinkedList<TrackerClient> removedClients = new LinkedList<TrackerClient>();
    //loadBalancing strategy will always be picked because there is no hash ring changes
    boolean isLoadBalancingStrategyTurn = true;
    for(int i = numberOfClients; i > 0; i--)
    {
      TrackerClient removed = clients.remove(0);
      uris.remove(removed.getUri());
      removedClients.addLast(removed);
      removedUris.add(removed.getUri());
      clusterGenerationId++;
      resultTC = simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
              1, isLoadBalancingStrategyTurn, overrideDropRate, 1000, false, false);
      if (i == 1)
      {
        assertNull(resultTC);
      }
      else
      {
        //if the tracker client is not dropped by overrideClusterDropRate (which could be true because at this point
        //the override drop rate is 0.8)
        if (resultTC != null)
        {
          assertTrue(uris.contains(resultTC.getUri()));
          assertFalse(removedUris.contains(resultTC.getUri()));
        }
      }
    }
    assertTrue(uris.isEmpty());
    assertTrue(clients.isEmpty());
    assertEquals(removedUris.size(), numberOfClients);
    assertEquals(removedClients.size(), numberOfClients);
    //we'll simulate the client start reviving one by one until all clients are back up again
    for (int i = numberOfClients; i > 0 ; i--)
    {
      TrackerClient added = removedClients.remove(0);
      //we have to create a new client. The old client has a degraded DegraderImpl. And in production enviroment
      //when a new client join a cluster, it should be in good state. This means there should be 100 points
      //in the hash ring for this client
      TrackerClient newClient = new TrackerClient(added.getUri(),
              getDefaultPartitionData(1d),
              new TestLoadBalancerClient(added.getUri()), clock, degraderConfig);
      clients.add(newClient);
      uris.add(added.getUri());
      removedUris.remove(added.getUri());
      clusterGenerationId++;
      resultTC = simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
              100, isLoadBalancingStrategyTurn, overrideDropRate, 1000, false, false);
      if (resultTC != null)
      {
        assertTrue(uris.contains(resultTC.getUri()));
        assertFalse(removedUris.contains(resultTC.getUri()));
      }
    }

    //now all the clients are in healthy state. There are 100 points per client in the hash ring but the
    //cluster override drop rate is still degraded at 0.8
    //For the rest of the rounds, we'll simulate latency == 300 ms (lower than low watermark)
    //so the overrideDropRate should have recovered bit by bit to full health. We don't need to change
    //the number of points because there is no hash ring changes
    for (overrideDropRate -= DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_DOWN ;overrideDropRate >= 0;
         overrideDropRate -= DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_DOWN)
    {
      resultTC = simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
              100, false,
              overrideDropRate, 300, false, false);
    }

    //we should have recovered fully by this time
    overrideDropRate = 0.0;
    resultTC = simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            100, false,
            overrideDropRate, 300, false, false);

    assertNotNull(resultTC);

    clusterGenerationId++;

    //simulate the increase of certain error (connect exception, closedChannelException) rate will cause degradation.
    simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            80, true, 0.0, 300, false, true);

    //switching to call dropping strategy
    simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            80, false, 0.0, 300, false, true);

    //continue the degradation
    simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            39, true, 0.0, 300, false, true);

    simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            39, false, 0.0, 300, false, true);

    simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            1, true, 0.0, 300, false, true);

    //now let's remove all the error and see how the cluster recover but we have to wait until next round because
    //this round is CALL_DROP strategy
    simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            1, false, 0.0, 300, false, false);

    simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            39, true, 0.0, 300, false, false);

    simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            39, false, 0.0, 300, false, false);

    simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            80, true, 0.0, 300, false, false);

    simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            80, false, 0.0, 300, false, false);

    simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            100, true, 0.0, 300, false, false);

    //make sure if we have error that is not from CONNECT_EXCEPTION or CLOSED_CHANNEL_EXCEPTION we don't degrade
    simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            100, false, 0.0, 300, true, false);

    //since there's no change in hash ring due to error NOT of CONNECT_EXCEPTION or CLOSED_CHANNEL_EXCEPTION,
    //the strategy won't change to CALL_DROPPING
    simulateAndTestOneInterval(timeInterval, clock, qps, clients, adapter, clusterGenerationId,
            100, false, 0.0, 300, true, false);
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
    myMap.put(PropertyKeys.CLOCK, clock);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS, timeInterval);
    myMap.put(PropertyKeys.HTTP_LB_GLOBAL_STEP_UP, globalStepUp);
    myMap.put(PropertyKeys.HTTP_LB_GLOBAL_STEP_DOWN, globalStepDown);
    myMap.put(PropertyKeys.HTTP_LB_HIGH_WATER_MARK, highWaterMark);
    myMap.put(PropertyKeys.HTTP_LB_LOW_WATER_MARK, lowWaterMark);
    myMap.put(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK, 1l);
    myMap.put(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK, 1l);

    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategy = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);

    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");
    URIRequest request = new URIRequest(uri1);

    TrackerClient client1 =
            new TrackerClient(uri1, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri1), clock, null);

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
    myMap.put(PropertyKeys.HTTP_LB_INITIAL_RECOVERY_LEVEL, 0.005);
    myMap.put(PropertyKeys.HTTP_LB_RING_RAMP_FACTOR, 2d);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS, TIME_INTERVAL);

    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategy = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);

    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");
    URI uri2 = URI.create("http://test.linkedin.com:3243/fdsaf");
    URIRequest request = new URIRequest(uri1);

    List<CallCompletion> ccList = new ArrayList<CallCompletion>();
    CallCompletion cc;

    TrackerClient client1 =
            new TrackerClient(uri1, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri1), clock, null);
    TrackerClient client2 =
            new TrackerClient(uri2, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri2), clock, null);

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
    //myMap.put(PropertyKeys.LB_INITIAL_RECOVERY_LEVEL, 0.01);
    //myMap.put("rampFactor", 2d);
    myMap.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS, TIME_INTERVAL);

    TestClock clock = new TestClock();
    myMap.put(PropertyKeys.CLOCK, clock);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategy = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
            null);

    List<TrackerClient> clients = new ArrayList<TrackerClient>();
    URI uri1 = URI.create("http://test.linkedin.com:3242/fdsaf");
    URIRequest request = new URIRequest(uri1);

    List<CallCompletion> ccList = new ArrayList<CallCompletion>();
    CallCompletion cc;

    TrackerClient client1 =
            new TrackerClient(uri1, getDefaultPartitionData(1d), new TestLoadBalancerClient(uri1), clock, null);

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

  @Test(groups = { "small", "back-end" })
    public void testInconsistentHashAndTrackerclients() throws URISyntaxException,
                                                               InterruptedException
  {
    // check if the inconsistent Hash ring and trackerlients can be handled
    TestClock clock = new TestClock();
    Map<String, Object> myMap = new HashMap<String, Object>();
    myMap.put(PropertyKeys.CLOCK, clock);
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(myMap);
    DegraderLoadBalancerStrategyV3 strategy = new DegraderLoadBalancerStrategyV3(config, "DegraderLoadBalancerTest",
                                                                                 null);

    List<TrackerClient> clients = new ArrayList<TrackerClient>();

    clients.add(getClient(URI.create("http://test.linkedin.com:3242/fdsaf"), clock));
    clients.add(getClient(URI.create("http://test.linkedin.com:3243/fdsaf"), clock));

    TrackerClient chosen = getTrackerClient(strategy, null, new RequestContext(), 0, clients);

    assertNotNull(chosen);

    // remove the client from the list, now the ring and the trackerClient list are inconsistent
    clients.remove(chosen);
    assertNotNull(getTrackerClient(strategy, null, new RequestContext(), 0, clients));
    // update the hash ring we should get the results as well
    assertNotNull(getTrackerClient(strategy, null, new RequestContext(), 1, clients));
  }

    public static DegraderLoadBalancerStrategyV3 getStrategy()
  {
    return new DegraderLoadBalancerStrategyV3(new DegraderLoadBalancerStrategyConfig(5000),
            "DegraderLoadBalancerTest",
            null);
  }

  public static DegraderLoadBalancerStrategyV3 getStrategy(Map<String,Object> map)
  {
    return new DegraderLoadBalancerStrategyV3(DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(map),
            "DegraderLoadBalancerTest",
            null);
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
    return new TrackerClient(uri, partitionDataMap, new TestLoadBalancerClient(uri), clock, null);
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
    public void streamRequest(StreamRequest request,
                            RequestContext requestContext,
                            Map<String, String> wireAttrs,
                            TransportCallback<StreamResponse> callback)
    {
      // Do nothing
    }

    @Override
    public void restRequest(RestRequest request,
                     RequestContext requestContext,
                     Map<String, String> wireAttrs,
                     TransportCallback<RestResponse> callback)
    {

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

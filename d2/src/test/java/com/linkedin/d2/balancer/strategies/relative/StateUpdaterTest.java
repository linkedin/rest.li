/*
   Copyright (c) 2020 LinkedIn Corp.

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

package com.linkedin.d2.balancer.strategies.relative;

import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.balancer.clients.TrackerClient;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;


/**
 * Test for {@link StateUpdater}
 */
public class StateUpdaterTest
{
  private static final int DEFAULT_PARTITION_ID = 0;
  private static final long DEFAULT_CLUSTER_GENERATION_ID = 0;
  private static final int HEALTHY_POINTS = 100;
  private static final int INITIAL_RECOVERY_POINTS = 1;

  private StateUpdater _stateUpdater;
  private ScheduledExecutorService _executorService = Mockito.mock(ScheduledExecutorService.class);
  private final QuarantineManager _quarantineManager = Mockito.mock(QuarantineManager.class);

  private void setup(D2RelativeStrategyProperties relativeStrategyProperties,
      ConcurrentMap<Integer, PartitionState> partitionLoadBalancerStateMap)
  {
    RelativeLoadBalancerStrategyFactory.putDefaultValues(relativeStrategyProperties);
    _stateUpdater = new StateUpdater(relativeStrategyProperties, _quarantineManager, _executorService,
        partitionLoadBalancerStateMap, Collections.emptyList());
  }

  @Test
  public void testInitializePartition()
  {
    setup(new D2RelativeStrategyProperties(), new ConcurrentHashMap<>());

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(2,
        Arrays.asList(20, 20), Arrays.asList(10, 10), Arrays.asList(200L, 500L), Arrays.asList(100L, 200L), Arrays.asList(0, 0));

    assertTrue(_stateUpdater.getPointsMap(DEFAULT_PARTITION_ID).isEmpty(), "There should be no state before initialization");

    _stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID, DEFAULT_CLUSTER_GENERATION_ID);

    assertEquals(_stateUpdater.getPointsMap(DEFAULT_PARTITION_ID).get(trackerClients.get(0).getUri()).intValue(), HEALTHY_POINTS);
    assertEquals(_stateUpdater.getPointsMap(DEFAULT_PARTITION_ID).get(trackerClients.get(1).getUri()).intValue(), HEALTHY_POINTS);
  }

  @Test
  public void testInitializePartitionWithSlowStartInitialHealthScore()
  {
    double initialHealthScore = 0.01;
    D2RelativeStrategyProperties relativeStrategyProperties = new D2RelativeStrategyProperties()
        .setInitialHealthScore(initialHealthScore);
    setup(relativeStrategyProperties, new ConcurrentHashMap<>());

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(2,
        Arrays.asList(20, 20), Arrays.asList(10, 10), Arrays.asList(200L, 500L), Arrays.asList(100L, 200L), Arrays.asList(0, 0));

    assertTrue(_stateUpdater.getPointsMap(DEFAULT_PARTITION_ID).isEmpty(), "There should be no state before initialization");

    _stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID, DEFAULT_CLUSTER_GENERATION_ID);

    assertEquals(_stateUpdater.getPointsMap(DEFAULT_PARTITION_ID).get(trackerClients.get(0).getUri()).intValue(),
        (int) (initialHealthScore * RelativeLoadBalancerStrategyFactory.DEFAULT_POINTS_PER_WEIGHT));
    assertEquals(_stateUpdater.getPointsMap(DEFAULT_PARTITION_ID).get(trackerClients.get(1).getUri()).intValue(),
        (int) (initialHealthScore * RelativeLoadBalancerStrategyFactory.DEFAULT_POINTS_PER_WEIGHT));
  }

  @Test
  public void testInitializePartitionWithMultipleThreads() throws InterruptedException {
    setup(new D2RelativeStrategyProperties(), new ConcurrentHashMap<>());

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(2,
        Arrays.asList(20, 20), Arrays.asList(10, 10), Arrays.asList(200L, 500L), Arrays.asList(100L, 200L), Arrays.asList(0, 0));

    assertTrue(_stateUpdater.getPointsMap(DEFAULT_PARTITION_ID).isEmpty(), "There should be no state before initialization");

    int numThreads = 50;
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    CountDownLatch countDownLatch = new CountDownLatch(numThreads);
    Runnable runnable = () -> {
      PartitionState lastState = _stateUpdater.getPartitionState(DEFAULT_PARTITION_ID);
      _stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID, DEFAULT_CLUSTER_GENERATION_ID);
      PartitionState currentState = _stateUpdater.getPartitionState(DEFAULT_PARTITION_ID);
      if (lastState != null)
      {
        assertEquals(currentState, lastState,
            "The partition state should always be the same object created by the first thread that obtained the lock");
      }
    };

    for (int threadIndex = 0; threadIndex < numThreads; threadIndex ++)
    {
      runIndividualConcurrentTask(executorService, runnable, countDownLatch);
    }


    if (!countDownLatch.await(2, TimeUnit.SECONDS))
    {
      fail("Initialization failed to finish within 2 seconds");
    }

    assertEquals(_stateUpdater.getPointsMap(DEFAULT_PARTITION_ID).get(trackerClients.get(0).getUri()).intValue(), HEALTHY_POINTS);
    assertEquals(_stateUpdater.getPointsMap(DEFAULT_PARTITION_ID).get(trackerClients.get(1).getUri()).intValue(), HEALTHY_POINTS);
    executorService.shutdown();
  }

  @Test
  public void testClusterGenerationIdChange() throws InterruptedException {
    PartitionState state = new PartitionStateTestDataBuilder()
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .build();

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(2,
        Arrays.asList(20, 20), Arrays.asList(10, 10), Arrays.asList(200L, 500L), Arrays.asList(100L, 200L), Arrays.asList(0, 0));

    ConcurrentMap<Integer, PartitionState> partitionLoadBalancerStateMap = new ConcurrentHashMap<>();
    partitionLoadBalancerStateMap.put(DEFAULT_PARTITION_ID, state);
    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    D2RelativeStrategyProperties relativeStrategyProperties = RelativeLoadBalancerStrategyFactory.putDefaultValues(new D2RelativeStrategyProperties());
    _stateUpdater = new StateUpdater(relativeStrategyProperties, _quarantineManager, executorService,
        partitionLoadBalancerStateMap, Collections.emptyList());

    // update will be scheduled twice, once from interval update, once from cluster change
    CountDownLatch countDownLatch = new CountDownLatch(2);
    Mockito.doAnswer(new ExecutionCountDown<>(countDownLatch)).when(_quarantineManager).updateQuarantineState(any(), any(), anyLong());

    // Cluster generation id changed from 0 to 1
    _stateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID, 1);
    if (!countDownLatch.await(5, TimeUnit.SECONDS))
    {
      fail("Initialization failed to finish within 5 seconds");
    }

    assertEquals(_stateUpdater.getPointsMap(DEFAULT_PARTITION_ID).size(), 2);
    executorService.shutdown();
  }

  @Test
  public void testUpdateOnePartition()
  {
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3,
        Arrays.asList(20, 20, 20), Arrays.asList(10, 10, 10), Arrays.asList(200L, 300L, 1000L),
        Arrays.asList(100L, 200L, 500L), Arrays.asList(0, 0, 0));

    PartitionState state = new PartitionStateTestDataBuilder()
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .setTrackerClientStateMap(trackerClients,
            Arrays.asList(1.0, 1.0, 1.0),
            Arrays.asList(TrackerClientState.HealthState.HEALTHY, TrackerClientState.HealthState.HEALTHY, TrackerClientState.HealthState.HEALTHY),
            Arrays.asList(30, 30, 30))
        .build();

    ConcurrentMap<Integer, PartitionState> partitionLoadBalancerStateMap = new ConcurrentHashMap<>();
    partitionLoadBalancerStateMap.put(DEFAULT_PARTITION_ID, state);
    setup(new D2RelativeStrategyProperties(), partitionLoadBalancerStateMap);

    _stateUpdater.updateState();
    Map<URI, Integer> pointsMap = _stateUpdater.getPointsMap(DEFAULT_PARTITION_ID);

    assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(),
        (int) (HEALTHY_POINTS - RelativeLoadBalancerStrategyFactory.DEFAULT_DOWN_STEP * RelativeLoadBalancerStrategyFactory.DEFAULT_POINTS_PER_WEIGHT));
  }

  @Test
  public void testUpdateMultiplePartitions()
  {
    /**
     * There are 2 partitions, and 4 tracker clients in total.
     * Partition 0 contains tracker client 1,2,3
     * Partition 1 contains tracker client 3,4
     * TrackerClient 3 will be unhealthy in partition 0, but not in partition 1
     */
    List<TrackerClient> trackerClients1 = TrackerClientMockHelper.mockTrackerClients(3,
        Arrays.asList(20, 20, 20), Arrays.asList(10, 10, 10), Arrays.asList(200L, 300L, 1000L),
        Arrays.asList(100L, 200L, 500L), Arrays.asList(0, 0, 0));
    List<TrackerClient> trackerClients2 = TrackerClientMockHelper.mockTrackerClients(1,
        Arrays.asList(20), Arrays.asList(10), Arrays.asList(1000L),
        Arrays.asList(600L), Arrays.asList(0));
    trackerClients2.add(trackerClients1.get(2));

    PartitionState state1 = new PartitionStateTestDataBuilder()
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .setTrackerClientStateMap(trackerClients1,
            Arrays.asList(StateUpdater.MAX_HEALTH_SCORE, StateUpdater.MAX_HEALTH_SCORE, StateUpdater.MAX_HEALTH_SCORE),
            Arrays.asList(TrackerClientState.HealthState.HEALTHY, TrackerClientState.HealthState.HEALTHY, TrackerClientState.HealthState.HEALTHY),
            Arrays.asList(30, 30, 30))
        .build();
    PartitionState state2 = new PartitionStateTestDataBuilder()
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .setTrackerClientStateMap(trackerClients2,
            Arrays.asList(StateUpdater.MAX_HEALTH_SCORE, StateUpdater.MAX_HEALTH_SCORE),
            Arrays.asList(TrackerClientState.HealthState.HEALTHY, TrackerClientState.HealthState.HEALTHY),
            Arrays.asList(30, 30))
        .build();

    ConcurrentMap<Integer, PartitionState> partitionLoadBalancerStateMap = new ConcurrentHashMap<>();
    partitionLoadBalancerStateMap.put(0, state1);
    partitionLoadBalancerStateMap.put(1, state2);
    setup(new D2RelativeStrategyProperties(), partitionLoadBalancerStateMap);

    _stateUpdater.updateState();
    URI overlapUri = trackerClients1.get(2).getUri();

    assertEquals(partitionLoadBalancerStateMap.get(0).getPointsMap().get(overlapUri).intValue(),
        (int) (HEALTHY_POINTS - RelativeLoadBalancerStrategyFactory.DEFAULT_DOWN_STEP * RelativeLoadBalancerStrategyFactory.DEFAULT_POINTS_PER_WEIGHT));
    assertEquals(partitionLoadBalancerStateMap.get(1).getPointsMap().get(overlapUri).intValue(), HEALTHY_POINTS);
  }

  @Test
  public void testClusterUrisChange()
  {
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3,
        Arrays.asList(20, 20, 20), Arrays.asList(10, 10, 10), Arrays.asList(200L, 220L, 1000L),
        Arrays.asList(100L, 110L, 500L), Arrays.asList(0, 0, 0));

    PartitionState state = new PartitionStateTestDataBuilder()
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .setTrackerClientStateMap(trackerClients,
            Arrays.asList(StateUpdater.MAX_HEALTH_SCORE, StateUpdater.MAX_HEALTH_SCORE, StateUpdater.MAX_HEALTH_SCORE),
            Arrays.asList(TrackerClientState.HealthState.HEALTHY, TrackerClientState.HealthState.HEALTHY, TrackerClientState.HealthState.HEALTHY),
            Arrays.asList(30, 30, 30))
        .build();

    ConcurrentMap<Integer, PartitionState> partitionLoadBalancerStateMap = new ConcurrentHashMap<>();
    partitionLoadBalancerStateMap.put(DEFAULT_PARTITION_ID, state);
    setup(new D2RelativeStrategyProperties(), partitionLoadBalancerStateMap);

    // New tracker clients set only contains 2 out of 3 tracker clients from the old state
    Set<TrackerClient> newTrackerClientSet = new HashSet<>();
    newTrackerClientSet.add(trackerClients.get(0));
    newTrackerClientSet.add(trackerClients.get(1));
    _stateUpdater.updateStateForPartition(newTrackerClientSet, DEFAULT_PARTITION_ID, state, 1L);

    Map<URI, Integer> pointsMap = _stateUpdater.getPointsMap(DEFAULT_PARTITION_ID);
    assertEquals(pointsMap.size(), 2, "There should only be 2 uris after cluster id change");
    assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), HEALTHY_POINTS);
  }

  @Test(dataProvider = "trackerClients")
  public void testHealthScoreDrop(List<TrackerClient> trackerClients, double highLatencyFactor,
      double highErrorRate, boolean expectToDropHealthScore)
  {
    PartitionState state = new PartitionStateTestDataBuilder()
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .setTrackerClientStateMap(trackerClients,
            Arrays.asList(StateUpdater.MAX_HEALTH_SCORE, StateUpdater.MAX_HEALTH_SCORE, StateUpdater.MAX_HEALTH_SCORE),
            Arrays.asList(TrackerClientState.HealthState.HEALTHY, TrackerClientState.HealthState.HEALTHY, TrackerClientState.HealthState.HEALTHY),
            Arrays.asList(30, 30, 30))
        .build();

    ConcurrentMap<Integer, PartitionState> partitionLoadBalancerStateMap = new ConcurrentHashMap<>();
    partitionLoadBalancerStateMap.put(DEFAULT_PARTITION_ID, state);
    setup(new D2RelativeStrategyProperties()
            .setRelativeLatencyHighThresholdFactor(highLatencyFactor).setHighErrorRate(highErrorRate),
        partitionLoadBalancerStateMap);
    _stateUpdater.updateState();

    Map<URI, Integer> pointsMap = _stateUpdater.getPointsMap(DEFAULT_PARTITION_ID);
    if (!expectToDropHealthScore)
    {
      assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), HEALTHY_POINTS);
      assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), HEALTHY_POINTS);
      assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), HEALTHY_POINTS);
    }
    else
    {
      assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(),
          (int) (HEALTHY_POINTS - RelativeLoadBalancerStrategyFactory.DEFAULT_DOWN_STEP * RelativeLoadBalancerStrategyFactory.DEFAULT_POINTS_PER_WEIGHT));
      assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), HEALTHY_POINTS);
      assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), HEALTHY_POINTS);
    }
  }

  @DataProvider(name = "trackerClients")
  Object[][] getTrackerClients()
  {
    List<Long> defaultLatencyList = Arrays.asList(100L, 100L, 100L);
    List<Long> defaultOutstandingLatencyList = Arrays.asList(20L, 20L, 20L);
    List<Integer> defaultCallCountList = Arrays.asList(20, 20, 20);
    List<Integer> defaultOutstandingCountList = Arrays.asList(10, 10, 10);
    List<Integer> defaultErrorCountList = Arrays.asList(0, 0, 0);
    double defaultHighLatencyFactor = 1.2;
    double defaultHighErrorRate = 0.2;
    int numTrackerClients = 3;
    return new Object[][]
        {
            // Test with different latency and outstanding latencies
            {TrackerClientMockHelper.mockTrackerClients(numTrackerClients,defaultCallCountList, defaultOutstandingCountList,
                Arrays.asList(200L, 220L, 200L), Arrays.asList(100L, 110L, 100L), defaultErrorCountList), defaultHighLatencyFactor, defaultHighErrorRate, false},
            {TrackerClientMockHelper.mockTrackerClients(numTrackerClients,defaultCallCountList, defaultOutstandingCountList,
                Arrays.asList(1000L, 120L, 115L), Arrays.asList(20L, 10L, 15L), defaultErrorCountList), defaultHighLatencyFactor, defaultHighErrorRate, true},
            {TrackerClientMockHelper.mockTrackerClients(numTrackerClients,defaultCallCountList, defaultOutstandingCountList,
                Arrays.asList(100L, 120L, 115L), Arrays.asList(1000L, 10L, 15L), defaultErrorCountList), defaultHighLatencyFactor, defaultHighErrorRate, true},
            {TrackerClientMockHelper.mockTrackerClients(numTrackerClients,defaultCallCountList, defaultOutstandingCountList,
                Arrays.asList(1000L, 500L, 600L), Arrays.asList(900L, 700L, 800L), defaultErrorCountList), 1.5, defaultHighErrorRate, false},

            // Test with different error count and error rates
            {TrackerClientMockHelper.mockTrackerClients(numTrackerClients,Arrays.asList(100, 200, 200), Arrays.asList(0, 0, 0),
                defaultLatencyList, defaultOutstandingLatencyList, Arrays.asList(10, 10, 15)), defaultHighLatencyFactor, defaultHighErrorRate, false},
            {TrackerClientMockHelper.mockTrackerClients(numTrackerClients,Arrays.asList(100, 200, 200), Arrays.asList(0, 0, 0),
                defaultLatencyList, defaultOutstandingLatencyList, Arrays.asList(10, 10, 15)), defaultHighLatencyFactor, 0.09, true},
            {TrackerClientMockHelper.mockTrackerClients(numTrackerClients,Arrays.asList(100, 200, 200), Arrays.asList(0, 0, 0),
                defaultLatencyList, defaultOutstandingLatencyList, Arrays.asList(21, 10, 15)), defaultHighLatencyFactor, defaultHighErrorRate, true},
            {TrackerClientMockHelper.mockTrackerClients(numTrackerClients,Arrays.asList(100, 200, 200), Arrays.asList(0, 0, 0),
                defaultLatencyList, defaultOutstandingLatencyList, Arrays.asList(21, 10, 15)), defaultHighLatencyFactor, 0.3, false}
        };
  }

  @Test
  public void testCallCountBelowMinCallCount()
  {
    int minCallCount = 10;
    // One client has high latency but small call count
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3,
        Arrays.asList(5, 20, 20), Arrays.asList(0, 0, 0), Arrays.asList(1000L, 300L, 300L),
        Arrays.asList(100L, 200L, 200L), Arrays.asList(0, 0, 0));

    PartitionState state = new PartitionStateTestDataBuilder()
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .setTrackerClientStateMap(trackerClients,
            Arrays.asList(StateUpdater.MAX_HEALTH_SCORE, StateUpdater.MAX_HEALTH_SCORE, StateUpdater.MAX_HEALTH_SCORE),
            Arrays.asList(TrackerClientState.HealthState.HEALTHY, TrackerClientState.HealthState.HEALTHY, TrackerClientState.HealthState.HEALTHY),
            Arrays.asList(5, 20, 20),
            minCallCount)
        .build();

    ConcurrentMap<Integer, PartitionState> partitionLoadBalancerStateMap = new ConcurrentHashMap<>();
    partitionLoadBalancerStateMap.put(DEFAULT_PARTITION_ID, state);
    setup(new D2RelativeStrategyProperties().setMinCallCount(minCallCount),
        partitionLoadBalancerStateMap);

    _stateUpdater.updateState();
    Map<URI, Integer> pointsMap = _stateUpdater.getPointsMap(DEFAULT_PARTITION_ID);

    // Verify the host with high latency still has 100 points
    assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), HEALTHY_POINTS);
  }

  @Test(dataProvider = "slowStartThreshold")
  public void testHealthScoreRecover(double currentHealthScore, double slowStartThreshold)
  {
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3,
        Arrays.asList(20, 20, 20), Arrays.asList(0, 0, 0), Arrays.asList(300L, 300L, 300L),
        Arrays.asList(200L, 200L, 200L), Arrays.asList(0, 0, 0));

    PartitionState state = new PartitionStateTestDataBuilder()
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .setTrackerClientStateMap(trackerClients,
            Arrays.asList(currentHealthScore, StateUpdater.MAX_HEALTH_SCORE, StateUpdater.MAX_HEALTH_SCORE),
            Arrays.asList(TrackerClientState.HealthState.UNHEALTHY, TrackerClientState.HealthState.HEALTHY, TrackerClientState.HealthState.HEALTHY),
            Arrays.asList(20, 20, 20))
        .build();

    ConcurrentMap<Integer, PartitionState> partitionLoadBalancerStateMap = new ConcurrentHashMap<>();
    partitionLoadBalancerStateMap.put(DEFAULT_PARTITION_ID, state);
    setup(new D2RelativeStrategyProperties().setSlowStartThreshold(slowStartThreshold),
        partitionLoadBalancerStateMap);

    _stateUpdater.updateState();
    Map<URI, Integer> pointsMap = _stateUpdater.getPointsMap(DEFAULT_PARTITION_ID);

    if (slowStartThreshold == RelativeLoadBalancerStrategyFactory.DEFAULT_SLOW_START_THRESHOLD)
    {
      assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), 5);
      assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), HEALTHY_POINTS);
      assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), HEALTHY_POINTS);
    }
    else if (currentHealthScore == 0.0)
    {
      assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), INITIAL_RECOVERY_POINTS);
      assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), HEALTHY_POINTS);
      assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), HEALTHY_POINTS);
    }
    else if (currentHealthScore == 0.1)
    {
      assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), 20);
      assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), HEALTHY_POINTS);
      assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), HEALTHY_POINTS);
    }
    else
    {
      assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), 30);
      assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), HEALTHY_POINTS);
      assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), HEALTHY_POINTS);
    }
  }

  @DataProvider(name = "slowStartThreshold")
  Object[][] getSlowStartThreshold()
  {
    return new Object[][]
        {
            {0.0, 0.0},
            {0.0, 0.2},
            {0.1, 0.2},
            {0.25, 0.2}
        };
  }

  @Test
  public void testExecutorSchedule() throws InterruptedException {
    setup(new D2RelativeStrategyProperties(), new ConcurrentHashMap<>());

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(2,
        Arrays.asList(20, 20), Arrays.asList(10, 10), Arrays.asList(200L, 200L), Arrays.asList(100L, 100L), Arrays.asList(0, 0));
    PartitionState existingState = new PartitionStateTestDataBuilder()
        .setTrackerClientStateMap(trackerClients,
            Arrays.asList(1.0, 1.0),
            Arrays.asList(TrackerClientState.HealthState.HEALTHY, TrackerClientState.HealthState.HEALTHY),
            Arrays.asList(30, 30))
        .build();
    ConcurrentMap<Integer, PartitionState> stateMap = new ConcurrentHashMap<>();
    stateMap.put(DEFAULT_PARTITION_ID, existingState);

    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    D2RelativeStrategyProperties relativeStrategyProperties = RelativeLoadBalancerStrategyFactory.putDefaultValues(new D2RelativeStrategyProperties());
    _stateUpdater = new StateUpdater(relativeStrategyProperties, _quarantineManager, executorService, stateMap, Collections.emptyList());

    // In 6 seconds, the update should be executed twice
    CountDownLatch countDownLatch = new CountDownLatch(2);
    Mockito.doAnswer(new ExecutionCountDown<>(countDownLatch)).when(_quarantineManager).updateQuarantineState(any(), any(), anyLong());
    if (!countDownLatch.await(6, TimeUnit.SECONDS))
    {
      fail("Initialization failed to finish within 6 seconds");
    }

    Mockito.verify(_quarantineManager, Mockito.atLeast(2)).updateQuarantineState(any(), any(), anyLong());
    assertEquals(_stateUpdater.getPointsMap(DEFAULT_PARTITION_ID).get(trackerClients.get(0).getUri()).intValue(), HEALTHY_POINTS);
    assertEquals(_stateUpdater.getPointsMap(DEFAULT_PARTITION_ID).get(trackerClients.get(1).getUri()).intValue(), HEALTHY_POINTS);
    executorService.shutdown();
  }

  private void runIndividualConcurrentTask(ExecutorService executorService, Runnable runnable, CountDownLatch countDownLatch)
  {
    executorService.submit(() -> {
      runnable.run();
      countDownLatch.countDown();
    });
  }

  private class ExecutionCountDown<Object> implements Answer<Object>
  {
    private final CountDownLatch _countDownLatch;
    ExecutionCountDown(CountDownLatch countDownLatch)
    {
      _countDownLatch = countDownLatch;
    }
    @Override
    public Object answer(InvocationOnMock invocation)
    {
      _countDownLatch.countDown();
      return null;
    }
  }
}

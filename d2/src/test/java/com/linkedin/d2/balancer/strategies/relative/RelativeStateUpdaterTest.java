package com.linkedin.d2.balancer.strategies.relative;

import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.PartitionLoadBalancerStateListener;
import com.linkedin.d2.balancer.strategies.degrader.DistributionNonDiscreteRingFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.d2.balancer.strategies.relative.TrackerClientState.HealthState.*;
import static org.mockito.Matchers.*;


public class RelativeStateUpdaterTest
{
  private static final int DEFAULT_PARTITION_ID = 0;
  private static final long DEFAULT_CLUSTER_GENERATION_ID = 0;

  @Test
  public void testInitializePartition() throws URISyntaxException
  {
    Mocks mocks = new Mocks(new D2RelativeStrategyProperties(), new ConcurrentHashMap<>(),
        Collections.emptyList());

    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(2,
        Arrays.asList(20, 20), Arrays.asList(10, 10), Arrays.asList(200L, 500L), Arrays.asList(100L, 200L), Arrays.asList(0, 0));

    // Verify before initialization the partition does not exist
    Assert.assertTrue(mocks._relativeStateUpdater.getPointsMap(DEFAULT_PARTITION_ID).isEmpty());

    mocks._relativeStateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID, DEFAULT_CLUSTER_GENERATION_ID);

    Assert.assertEquals(mocks._relativeStateUpdater.getPointsMap(DEFAULT_PARTITION_ID).get(trackerClients.get(0).getUri()).intValue(), 100);
    Assert.assertEquals(mocks._relativeStateUpdater.getPointsMap(DEFAULT_PARTITION_ID).get(trackerClients.get(1).getUri()).intValue(), 100);
  }

  @Test
  public void testClusterGenerationIdChange() throws URISyntaxException
  {
    PartitionRelativeLoadBalancerState state = new PartitionRelativeLoadBalancerStateDataBuilder(new DistributionNonDiscreteRingFactory<>())
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .build();
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(2,
        Arrays.asList(20, 20), Arrays.asList(10, 10), Arrays.asList(200L, 500L), Arrays.asList(100L, 200L), Arrays.asList(0, 0));

    ConcurrentMap<Integer, PartitionRelativeLoadBalancerState> partitionLoadBalancerStateMap = new ConcurrentHashMap<>();
    partitionLoadBalancerStateMap.put(DEFAULT_PARTITION_ID, state);
    Mocks mocks = new Mocks(new D2RelativeStrategyProperties(), partitionLoadBalancerStateMap,
        Collections.emptyList());
    // Cluster generation id changed from 0 to 1
    mocks._relativeStateUpdater.updateState(new HashSet<>(trackerClients), DEFAULT_PARTITION_ID, 1);

    Mockito.verify(mocks._executorService).execute(any(Runnable.class));
  }

  @Test
  public void testUpdateOnePartition() throws URISyntaxException
  {
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3,
        Arrays.asList(20, 20, 20), Arrays.asList(10, 10, 10), Arrays.asList(200L, 300L, 1000L),
        Arrays.asList(100L, 200L, 500L), Arrays.asList(0, 0, 0));

    PartitionRelativeLoadBalancerState state = new PartitionRelativeLoadBalancerStateDataBuilder(new DistributionNonDiscreteRingFactory<>())
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .setTrackerClientStateMap(trackerClients, Arrays.asList(1.0, 1.0, 1.0), Arrays.asList(HEALTHY, HEALTHY, HEALTHY),
            Arrays.asList(30, 30, 30), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE,
            RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT)
        .build();

    ConcurrentMap<Integer, PartitionRelativeLoadBalancerState> partitionLoadBalancerStateMap = new ConcurrentHashMap<>();
    partitionLoadBalancerStateMap.put(DEFAULT_PARTITION_ID, state);
    Mocks mocks = new Mocks(new D2RelativeStrategyProperties(), partitionLoadBalancerStateMap,
        Collections.emptyList());

    mocks._relativeStateUpdater.updateState();
    Map<URI, Integer> pointsMap = mocks._relativeStateUpdater.getPointsMap(DEFAULT_PARTITION_ID);

    // Verify one host's health score dropped to 80
    Assert.assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), 100);
    Assert.assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), 100);
    Assert.assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), 80);
  }

  @Test
  public void testUpdateMultiplePartitions() throws URISyntaxException
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

    PartitionRelativeLoadBalancerState state1 = new PartitionRelativeLoadBalancerStateDataBuilder(new DistributionNonDiscreteRingFactory<>())
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .setTrackerClientStateMap(trackerClients1, Arrays.asList(1.0, 1.0, 1.0), Arrays.asList(HEALTHY, HEALTHY, HEALTHY),
            Arrays.asList(30, 30, 30), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE,
            RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT)
        .build();
    PartitionRelativeLoadBalancerState state2 = new PartitionRelativeLoadBalancerStateDataBuilder(new DistributionNonDiscreteRingFactory<>())
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .setTrackerClientStateMap(trackerClients2, Arrays.asList(1.0, 1.0), Arrays.asList(HEALTHY, HEALTHY),
            Arrays.asList(30, 30), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE,
            RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT)
        .build();

    ConcurrentMap<Integer, PartitionRelativeLoadBalancerState> partitionLoadBalancerStateMap = new ConcurrentHashMap<>();
    partitionLoadBalancerStateMap.put(0, state1);
    partitionLoadBalancerStateMap.put(1, state2);
    Mocks mocks = new Mocks(new D2RelativeStrategyProperties(), partitionLoadBalancerStateMap,
        Collections.emptyList());

    mocks._relativeStateUpdater.updateState();
    URI overlapUri = trackerClients1.get(2).getUri();

    // Verify the host has 80 points in partition 0, 100 points in partition 1
    Assert.assertEquals(partitionLoadBalancerStateMap.get(0).getPointsMap().get(overlapUri).intValue(), 80);
    Assert.assertEquals(partitionLoadBalancerStateMap.get(1).getPointsMap().get(overlapUri).intValue(), 100);
  }

  @Test
  public void testClusterUrisChange() throws URISyntaxException
  {
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3,
        Arrays.asList(20, 20, 20), Arrays.asList(10, 10, 10), Arrays.asList(200L, 220L, 1000L),
        Arrays.asList(100L, 110L, 500L), Arrays.asList(0, 0, 0));

    PartitionRelativeLoadBalancerState state = new PartitionRelativeLoadBalancerStateDataBuilder(new DistributionNonDiscreteRingFactory<>())
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .setTrackerClientStateMap(trackerClients, Arrays.asList(1.0, 1.0, 1.0), Arrays.asList(HEALTHY, HEALTHY, HEALTHY),
            Arrays.asList(30, 30, 30), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE,
            RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT)
        .build();

    ConcurrentMap<Integer, PartitionRelativeLoadBalancerState> partitionLoadBalancerStateMap = new ConcurrentHashMap<>();
    partitionLoadBalancerStateMap.put(DEFAULT_PARTITION_ID, state);
    Mocks mocks = new Mocks(new D2RelativeStrategyProperties(), partitionLoadBalancerStateMap,
        Collections.emptyList());

    // New tracker clients set only contains 2 out of 3 tracker clients from the old state
    Set<TrackerClient> newTrackerClientSet = new HashSet<>();
    newTrackerClientSet.add(trackerClients.get(0));
    newTrackerClientSet.add(trackerClients.get(1));
    mocks._relativeStateUpdater.updateStateForPartition(newTrackerClientSet, DEFAULT_PARTITION_ID, state, DEFAULT_CLUSTER_GENERATION_ID);

    // Verify there are only 2 URIs in the points map
    Map<URI, Integer> pointsMap = mocks._relativeStateUpdater.getPointsMap(DEFAULT_PARTITION_ID);
    Assert.assertEquals(pointsMap.size(), 2);
    Assert.assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), 100);
    Assert.assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), 100);
  }

  @Test(dataProvider = "trackerClients")
  public void testHealthScoreDropByLatency(List<TrackerClient> trackerClients, double highLatencyFactor,
      double highErrorRate, boolean expectToDropHealthScore)
  {
    PartitionRelativeLoadBalancerState state = new PartitionRelativeLoadBalancerStateDataBuilder(new DistributionNonDiscreteRingFactory<>())
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .setTrackerClientStateMap(trackerClients, Arrays.asList(1.0, 1.0, 1.0), Arrays.asList(HEALTHY, HEALTHY, HEALTHY),
            Arrays.asList(30, 30, 30), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE,
            RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT)
        .build();

    ConcurrentMap<Integer, PartitionRelativeLoadBalancerState> partitionLoadBalancerStateMap = new ConcurrentHashMap<>();
    partitionLoadBalancerStateMap.put(DEFAULT_PARTITION_ID, state);
    Mocks mocks = new Mocks(new D2RelativeStrategyProperties()
            .setRelativeLatencyHighThresholdFactor(highLatencyFactor).setHighErrorRate(highErrorRate),
        partitionLoadBalancerStateMap,
        Collections.emptyList());
    mocks._relativeStateUpdater.updateState();

    Map<URI, Integer> pointsMap = mocks._relativeStateUpdater.getPointsMap(DEFAULT_PARTITION_ID);
    if (!expectToDropHealthScore)
    {
      Assert.assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), 100);
      Assert.assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), 100);
      Assert.assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), 100);
    } else
    {
      // Experiment 1 dropped because of latency, experiment 2 dropped because of outstanding latency
      Assert.assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), 80);
      Assert.assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), 100);
      Assert.assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), 100);
    }
  }

  @DataProvider(name = "trackerClients")
  Object[][] getTrackerClients() throws URISyntaxException
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
  public void testSmallCallCount() throws URISyntaxException {
    int minCallCount = 10;
    // One client has high latency but small call count
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3,
        Arrays.asList(5, 20, 20), Arrays.asList(0, 0, 0), Arrays.asList(1000L, 300L, 300L),
        Arrays.asList(100L, 200L, 200L), Arrays.asList(0, 0, 0));

    PartitionRelativeLoadBalancerState state = new PartitionRelativeLoadBalancerStateDataBuilder(new DistributionNonDiscreteRingFactory<>())
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .setTrackerClientStateMap(trackerClients, Arrays.asList(1.0, 1.0, 1.0), Arrays.asList(HEALTHY, HEALTHY, HEALTHY),
            Arrays.asList(5, 20, 20), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE, minCallCount)
        .build();

    ConcurrentMap<Integer, PartitionRelativeLoadBalancerState> partitionLoadBalancerStateMap = new ConcurrentHashMap<>();
    partitionLoadBalancerStateMap.put(DEFAULT_PARTITION_ID, state);
    Mocks mocks = new Mocks(new D2RelativeStrategyProperties().setMinCallCount(minCallCount),
        partitionLoadBalancerStateMap, Collections.emptyList());

    mocks._relativeStateUpdater.updateState();
    Map<URI, Integer> pointsMap = mocks._relativeStateUpdater.getPointsMap(DEFAULT_PARTITION_ID);

    // Verify the host with high latency still has 100 points
    Assert.assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), 100);
    Assert.assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), 100);
    Assert.assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), 100);
  }

  @Test(dataProvider = "slowStartThreshold")
  public void testHealthScoreRecover(double currentHealthScore, double slowStartThreshold) throws URISyntaxException {
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3,
        Arrays.asList(20, 20, 20), Arrays.asList(0, 0, 0), Arrays.asList(300L, 300L, 300L),
        Arrays.asList(200L, 200L, 200L), Arrays.asList(0, 0, 0));

    PartitionRelativeLoadBalancerState state = new PartitionRelativeLoadBalancerStateDataBuilder(new DistributionNonDiscreteRingFactory<>())
        .setClusterGenerationId(DEFAULT_CLUSTER_GENERATION_ID)
        .setTrackerClientStateMap(trackerClients, Arrays.asList(currentHealthScore, 1.0, 1.0), Arrays.asList(UNHEALTHY, HEALTHY, HEALTHY),
            Arrays.asList(20, 20, 20), RelativeLoadBalancerStrategyFactory.DEFAULT_INITIAL_HEALTH_SCORE,
            RelativeLoadBalancerStrategyFactory.DEFAULT_MIN_CALL_COUNT)
        .build();

    ConcurrentMap<Integer, PartitionRelativeLoadBalancerState> partitionLoadBalancerStateMap = new ConcurrentHashMap<>();
    partitionLoadBalancerStateMap.put(DEFAULT_PARTITION_ID, state);
    Mocks mocks = new Mocks(new D2RelativeStrategyProperties().setSlowStartThreshold(slowStartThreshold),
        partitionLoadBalancerStateMap, Collections.emptyList());

    mocks._relativeStateUpdater.updateState();
    Map<URI, Integer> pointsMap = mocks._relativeStateUpdater.getPointsMap(DEFAULT_PARTITION_ID);

    if (slowStartThreshold == RelativeLoadBalancerStrategyFactory.DEFAULT_SLOW_START_THRESHOLD)
    {
      Assert.assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), 5);
      Assert.assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), 100);
      Assert.assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), 100);
    } else if (currentHealthScore == 0.0)
    {
      Assert.assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), 1);
      Assert.assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), 100);
      Assert.assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), 100);
    } else if (currentHealthScore == 0.1)
    {
      Assert.assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), 20);
      Assert.assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), 100);
      Assert.assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), 100);
    } else
    {
      Assert.assertEquals(pointsMap.get(trackerClients.get(0).getUri()).intValue(), 30);
      Assert.assertEquals(pointsMap.get(trackerClients.get(1).getUri()).intValue(), 100);
      Assert.assertEquals(pointsMap.get(trackerClients.get(2).getUri()).intValue(), 100);
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

  private static class Mocks
  {
    private final ScheduledExecutorService _executorService = Mockito.mock(ScheduledExecutorService.class);
    private final QuarantineManager _quarantineManager = Mockito.mock(QuarantineManager.class);

    private final RelativeStateUpdater _relativeStateUpdater;

    private Mocks(D2RelativeStrategyProperties relativeStrategyProperties,
        ConcurrentMap<Integer, PartitionRelativeLoadBalancerState> partitionLoadBalancerStateMap,
        List<PartitionLoadBalancerStateListener.Factory<PartitionRelativeLoadBalancerState>> listenerFactories)
    {
      RelativeLoadBalancerStrategyFactory.putDefaultValues(relativeStrategyProperties);
      Mockito.doNothing().when(_quarantineManager).updateQuarantineState(any(PartitionRelativeLoadBalancerState.class), any(PartitionRelativeLoadBalancerState.class),
          anyLong());

      _relativeStateUpdater = new RelativeStateUpdater(relativeStrategyProperties, _quarantineManager, _executorService,
          partitionLoadBalancerStateMap, listenerFactories);
    }
  }
}

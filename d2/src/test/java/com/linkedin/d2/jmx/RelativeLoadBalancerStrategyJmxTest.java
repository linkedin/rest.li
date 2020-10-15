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

package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.clients.TrackerClient;
import com.linkedin.d2.balancer.strategies.relative.PartitionState;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategy;
import com.linkedin.d2.balancer.strategies.relative.TrackerClientMockHelper;
import com.linkedin.d2.balancer.strategies.relative.TrackerClientState;
import com.linkedin.d2.balancer.util.partitions.DefaultPartitionAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import static org.mockito.Matchers.anyInt;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class RelativeLoadBalancerStrategyJmxTest {
  private RelativeLoadBalancerStrategyJmx mockRelativeLoadBalancerStrategyJmx(List<TrackerClient> trackerClients)
  {
    Map<TrackerClient, TrackerClientState> trackerClientsMap = new HashMap<>();
    for (TrackerClient trackerClient : trackerClients)
    {
      trackerClientsMap.put(trackerClient, new TrackerClientState(1, 1));
    }

    RelativeLoadBalancerStrategy strategy = Mockito.mock(RelativeLoadBalancerStrategy.class);
    PartitionState state = Mockito.mock(PartitionState.class);
    Mockito.when(state.getTrackerClientStateMap()).thenReturn(trackerClientsMap);
    Mockito.when(strategy.getFirstValidPartitionId()).thenReturn(DefaultPartitionAccessor.DEFAULT_PARTITION_ID);
    Mockito.when(strategy.getPartitionState(anyInt())).thenReturn(state);

    return new RelativeLoadBalancerStrategyJmx(strategy);
  }

  @Test
  public void testLatencyDeviation()
  {
    List<TrackerClient> trackerClientsEqual = TrackerClientMockHelper.mockTrackerClients(2,
        Arrays.asList(20, 20), Arrays.asList(10, 10), Arrays.asList(200L, 200L), Arrays.asList(100L, 100L), Arrays.asList(0, 0));

    RelativeLoadBalancerStrategyJmx jmx = mockRelativeLoadBalancerStrategyJmx(trackerClientsEqual);
    assertEquals(jmx.getLatencyStandardDeviation(), 0.0);
    assertEquals(jmx.getLatencyMeanAbsoluteDeviation(), 0.0);
    assertEquals(jmx.getAboveAverageLatencyStandardDeviation(), 0.0);

    List<TrackerClient> trackerClientsDiverse1 = TrackerClientMockHelper.mockTrackerClients(3,
        Arrays.asList(20, 20, 20), Arrays.asList(10, 10, 10),
        Arrays.asList(100L, 150L, 200L), Arrays.asList(50L, 75L, 100L), Arrays.asList(0, 0, 0));

    List<TrackerClient> trackerClientsDiverse2 = TrackerClientMockHelper.mockTrackerClients(4,
        Arrays.asList(20, 20, 20, 20), Arrays.asList(10, 10, 10, 10),
        Arrays.asList(100L, 200L, 400L, 600L), Arrays.asList(50L, 100L, 200L, 300L), Arrays.asList(0, 0, 0, 0));

    RelativeLoadBalancerStrategyJmx jmx1 = mockRelativeLoadBalancerStrategyJmx(trackerClientsDiverse1);
    RelativeLoadBalancerStrategyJmx jmx2 = mockRelativeLoadBalancerStrategyJmx(trackerClientsDiverse2);

    assertTrue(jmx2.getLatencyStandardDeviation() > jmx1.getLatencyStandardDeviation());
    assertTrue(jmx2.getLatencyMeanAbsoluteDeviation() > jmx1.getLatencyMeanAbsoluteDeviation());
    assertTrue(jmx2.getAboveAverageLatencyStandardDeviation() > jmx1.getAboveAverageLatencyStandardDeviation());


    // hosts not receiving any traffic should not affect deviation calculation
    List<TrackerClient> trackerClientsDiverse3 = TrackerClientMockHelper.mockTrackerClients(4,
        Arrays.asList(20, 20, 20, 0), Arrays.asList(10, 10, 10, 0),
        Arrays.asList(100L, 150L, 200L, 0L), Arrays.asList(50L, 75L, 100L, 0L), Arrays.asList(0, 0, 0, 0));

    RelativeLoadBalancerStrategyJmx jmx3 = mockRelativeLoadBalancerStrategyJmx(trackerClientsDiverse3);
    assertEquals(jmx3.getLatencyStandardDeviation(), jmx1.getLatencyStandardDeviation());
    assertEquals(jmx3.getLatencyMeanAbsoluteDeviation(), jmx1.getLatencyMeanAbsoluteDeviation());
    assertEquals(jmx3.getAboveAverageLatencyStandardDeviation(), jmx1.getAboveAverageLatencyStandardDeviation());
  }

  @Test
  public void testLatencyRelativeFactor()
  {
    List<TrackerClient> trackerClientsEqual = TrackerClientMockHelper.mockTrackerClients(2,
        Arrays.asList(20, 20), Arrays.asList(10, 10), Arrays.asList(200L, 200L), Arrays.asList(100L, 100L), Arrays.asList(0, 0));

    RelativeLoadBalancerStrategyJmx jmx = mockRelativeLoadBalancerStrategyJmx(trackerClientsEqual);
    assertEquals(jmx.getMaxLatencyRelativeFactor(), 1.0);
    assertEquals(jmx.getNthPercentileLatencyRelativeFactor(0.95), 1.0);

    List<TrackerClient> trackerClientsDiverse = TrackerClientMockHelper.mockTrackerClients(4,
        Arrays.asList(20, 20, 20, 20), Arrays.asList(10, 10, 10, 10),
        Arrays.asList(100L, 200L, 300L, 400L), Arrays.asList(50L, 100L, 150L, 200L), Arrays.asList(0, 0, 0, 0));

    jmx = mockRelativeLoadBalancerStrategyJmx(trackerClientsDiverse);
    double maxLatencyRelativeFactor = jmx.getMaxLatencyRelativeFactor();
    double p95LatencyRelativeFactor = jmx.getNthPercentileLatencyRelativeFactor(0.95);
    assertTrue(maxLatencyRelativeFactor > 1 && maxLatencyRelativeFactor < 2);
    assertTrue(p95LatencyRelativeFactor > 1 && p95LatencyRelativeFactor < 2);
    assertTrue(p95LatencyRelativeFactor < maxLatencyRelativeFactor);
  }

  @Test
  public void testEmptyList()
  {
    RelativeLoadBalancerStrategyJmx jmx = mockRelativeLoadBalancerStrategyJmx(new ArrayList<>());
    assertEquals(jmx.getLatencyStandardDeviation(), 0.0);
    assertEquals(jmx.getLatencyMeanAbsoluteDeviation(), 0.0);
    assertEquals(jmx.getAboveAverageLatencyStandardDeviation(), 0.0);
    assertEquals(jmx.getMaxLatencyRelativeFactor(), 0.0);
    assertEquals(jmx.getNthPercentileLatencyRelativeFactor(0.95), 0.0);
    assertEquals(jmx.getTotalPointsInHashRing(), 0);
    assertEquals(jmx.getUnhealthyHostsCount(), 0);
    assertEquals(jmx.getQuarantineHostsCount(), 0);
  }

  @Test
  public void testZeroLatency()
  {
    List<TrackerClient> trackerClients = TrackerClientMockHelper.mockTrackerClients(3,
        Arrays.asList(0, 0, 0), Arrays.asList(0, 0, 0), Arrays.asList(0L, 0L, 0L), Arrays.asList(0L, 0L, 0L), Arrays.asList(0, 0, 0));

    RelativeLoadBalancerStrategyJmx jmx = mockRelativeLoadBalancerStrategyJmx(trackerClients);
    assertEquals(jmx.getLatencyStandardDeviation(), 0.0);
    assertEquals(jmx.getLatencyMeanAbsoluteDeviation(), 0.0);
    assertEquals(jmx.getAboveAverageLatencyStandardDeviation(), 0.0);
    assertEquals(jmx.getMaxLatencyRelativeFactor(), 0.0);
    assertEquals(jmx.getNthPercentileLatencyRelativeFactor(0.95), 0.0);
    assertEquals(jmx.getTotalPointsInHashRing(), 0);
    assertEquals(jmx.getUnhealthyHostsCount(), 0);
    assertEquals(jmx.getQuarantineHostsCount(), 0);
  }

  @Test
  public void testNoValidPartitionData()
  {
    RelativeLoadBalancerStrategy strategy = Mockito.mock(RelativeLoadBalancerStrategy.class);
    Mockito.when(strategy.getFirstValidPartitionId()).thenReturn(DefaultPartitionAccessor.DEFAULT_PARTITION_ID);
    Mockito.when(strategy.getPartitionState(anyInt())).thenReturn(null);

    RelativeLoadBalancerStrategyJmx jmx = new RelativeLoadBalancerStrategyJmx(strategy);
    assertEquals(jmx.getLatencyStandardDeviation(), 0.0);
    assertEquals(jmx.getLatencyMeanAbsoluteDeviation(), 0.0);
    assertEquals(jmx.getAboveAverageLatencyStandardDeviation(), 0.0);
    assertEquals(jmx.getMaxLatencyRelativeFactor(), 0.0);
    assertEquals(jmx.getNthPercentileLatencyRelativeFactor(0.95), 0.0);
    assertEquals(jmx.getTotalPointsInHashRing(), 0);
    assertEquals(jmx.getUnhealthyHostsCount(), 0);
    assertEquals(jmx.getQuarantineHostsCount(), 0);
  }
}
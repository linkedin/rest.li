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

package com.linkedin.d2.loadbalancer.strategies;

import com.linkedin.d2.balancer.strategies.framework.LoadBalancerStrategyTestRunner;
import com.linkedin.d2.balancer.strategies.framework.LoadBalancerStrategyTestRunnerBuilder;
import com.linkedin.d2.loadBalancerStrategyType;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * Integration tests for all the load balancer strategies
 * TODO: This file just started with a few tests to verify the test framework, will add more tests
 */
public class TestLoadBalancerStrategy
{
  private static final int HEALTHY_POINTS = 100;
  // Sometimes the points change between 1 and 2
  private static final int FULLY_DROPPED_POINTS = 2;

  @Test
  public void testConstantBadHostWithLatency()
  {
    LoadBalancerStrategyTestRunner testRunner =
        LoadBalancerStrategyTestRunnerBuilder.create1Unhealthy4HealthyHostWithLatency(loadBalancerStrategyType.DEGRADER, 3);
    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    //Verify the unhealthy host has 60 points, and all other hosts have 100 points
    assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(), 60);
    assertEquals(pointsMap.get(testRunner.getUri(1)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(2)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(3)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(4)).intValue(), HEALTHY_POINTS);
  }

  @Test
  public void testPointsDropToZeroWithLatency()
  {
    LoadBalancerStrategyTestRunner testRunner =
        LoadBalancerStrategyTestRunnerBuilder.create1GoingBad4HealthyHostWithLatency(loadBalancerStrategyType.DEGRADER, 20);
    testRunner.runWait();

    // Get the point history for the unhealthy host
    List<Integer> pointHistory = testRunner.getPointHistory().get(testRunner.getUri(0));

    // The points will drop to lowest in the end
    assertEquals(pointHistory.get(0).intValue(), HEALTHY_POINTS);
    assertTrue(pointHistory.get(19).intValue() <= FULLY_DROPPED_POINTS);
  }

  @Test
  public void testPointsRecoverToNormalWithLatency()
  {
    LoadBalancerStrategyTestRunner testRunner =
        LoadBalancerStrategyTestRunnerBuilder.create1Receovering4HealthyHostWithLatency(loadBalancerStrategyType.DEGRADER, 30);
    testRunner.runWait();

    // Get the point history for the unhealthy host
    List<Integer> pointHistory = testRunner.getPointHistory().get(testRunner.getUri(0));

    // The points will drop first, finally it will recover to 100
    assertTrue(getLowestPoints(pointHistory) <= FULLY_DROPPED_POINTS);
    assertEquals(pointHistory.get(29).intValue(), HEALTHY_POINTS);
  }

  @Test
  public void testPointsRecoverToNormalWithError()
  {
    LoadBalancerStrategyTestRunner testRunner =
        LoadBalancerStrategyTestRunnerBuilder.create1Receovering4HealthyHostWithError(loadBalancerStrategyType.DEGRADER, 50);
    testRunner.runWait();

    // Get the point history for the unhealthy host
    List<Integer> pointHistory = testRunner.getPointHistory().get(testRunner.getUri(0));

    // The points will drop first, finally it will recover to 100
    assertTrue(getLowestPoints(pointHistory) <= FULLY_DROPPED_POINTS);
    assertEquals(pointHistory.get(49).intValue(), HEALTHY_POINTS);
  }

  private int getLowestPoints(List<Integer> pointHistory) {
    return pointHistory.stream().min(Integer::compareTo)
        .orElse(HEALTHY_POINTS);
  }
}

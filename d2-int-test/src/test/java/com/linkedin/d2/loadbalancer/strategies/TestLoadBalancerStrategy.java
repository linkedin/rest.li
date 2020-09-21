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

package com.linkedin.d2.loadbalancer.strategies;

import com.linkedin.d2.D2QuarantineProperties;
import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.HttpStatusCodeRange;
import com.linkedin.d2.HttpStatusCodeRangeArray;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.strategies.framework.ErrorCountCorrelation;
import com.linkedin.d2.balancer.strategies.framework.LatencyCorrelation;
import com.linkedin.d2.balancer.strategies.framework.LoadBalancerStrategyTestRunner;
import com.linkedin.d2.balancer.strategies.framework.LoadBalancerStrategyTestRunnerBuilder;
import com.linkedin.d2.balancer.strategies.relative.RelativeLoadBalancerStrategyFactory;
import com.linkedin.d2.loadBalancerStrategyType;
import com.linkedin.test.util.retry.SingleRetry;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * Integration tests for the load balancer strategies
 */
public class TestLoadBalancerStrategy
{
  // Some default values to build default test scenarios
  private static final String DEFAULT_SERVICE_NAME = "dummyService";
  private static final int DEFAULT_NUM_HOSTS = 5;
  private static final int DEFAULT_REQUESTS_PER_INTERVAL = 1000;
  private static final String DEFAULT_HIGH_ERROR_RATE = "0.2";
  private static final String DEFAULT_LOW_ERROR_RATE = "0.05";
  private static final double DEFAULT_QUARANTINE_PERCENTAGE = 0.5;
  private static final double DEFAULT_WEIGHT = 1;
  private static final int HEALTHY_ERROR_COUNT = 0;
  private static final int UNHEALTHY_ERROR_COUNT = 100;
  private static final long UNHEALTHY_HOST_CONSTANT_LATENCY = 1000L;
  private static final long HEALTHY_HOST_CONSTANT_LATENCY = 50L;
  private static final LatencyCorrelation HEALTHY_HOST_LATENCY_CORRELATION =
      (callCount, intervalIndex) -> HEALTHY_HOST_CONSTANT_LATENCY;
  private static final ErrorCountCorrelation HEALTHY_HOST_ERROR_COUNT_CORRELATION =
      (callCount, intervalIndex) -> HEALTHY_ERROR_COUNT;
  // As time goes, the host latency becomes longer and longer
  private static final LatencyCorrelation HOST_BECOMING_UNHEALTHY_LATENCY =
      (callCount, intervalIndex) -> Long.min(HEALTHY_HOST_CONSTANT_LATENCY + intervalIndex * 500L, UNHEALTHY_HOST_CONSTANT_LATENCY);
  // As time goes, the host latency becomes shorter and shorter and recovers to healthy state
  private static final LatencyCorrelation HOST_RECOVERING_TO_HEALTHY_LATENCY =
      (callCount, intervalIndex) -> Long.max(UNHEALTHY_HOST_CONSTANT_LATENCY - intervalIndex * 100L, HEALTHY_HOST_CONSTANT_LATENCY);
  // As time goes, the host latency becomes bigger and bigger
  private static final ErrorCountCorrelation HOST_BECOMING_UNHEALTHY_ERROR =
      (callCount, intervalIndex) -> Integer.min(HEALTHY_ERROR_COUNT + intervalIndex * 10, UNHEALTHY_ERROR_COUNT);
  // As time goes, the host error count comes to 0
  private static final ErrorCountCorrelation HOST_RECOVERING_TO_HEALTHY_ERROR =
      (callCount, intervalIndex) -> Integer.max(UNHEALTHY_ERROR_COUNT - intervalIndex * 10, HEALTHY_ERROR_COUNT);

  @SuppressWarnings("serial")
  private static final Map<String, String> DEGRADER_PROPERTIES_WITH_HIGH_LOW_ERROR = new HashMap<String, String>()
  {{
    put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, DEFAULT_HIGH_ERROR_RATE);
    put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, DEFAULT_LOW_ERROR_RATE);
  }};
  private static final int HEALTHY_POINTS = 100;
  private static final int QUARANTINED_POINTS = 0;
  private static final int INITIAL_RECOVERY_POINTS = 0;
  // Sometimes the points change between 1 and 2 for Degrader strategy
  private static final int FULLY_DROPPED_POINTS = 2;

  @Test(dataProvider = "constantBadHost")
  public void testConstantBadHost(LoadBalancerStrategyTestRunner constantBadHostRunner)
  {
    constantBadHostRunner.runWait();
    Map<URI, Integer> pointsMap = constantBadHostRunner.getPoints();

    assertEquals(pointsMap.get(constantBadHostRunner.getUri(0)).intValue(),
        (int) (HEALTHY_POINTS - RelativeLoadBalancerStrategyFactory.DEFAULT_DOWN_STEP * HEALTHY_POINTS * 2),
        "The bad host points should drop to 60");
    assertEquals(pointsMap.get(constantBadHostRunner.getUri(1)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(constantBadHostRunner.getUri(2)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(constantBadHostRunner.getUri(3)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(constantBadHostRunner.getUri(4)).intValue(), HEALTHY_POINTS);
  }

  @DataProvider(name = "constantBadHost")
  public Object[][] getConstantBadHost()
  {
    int numIntervals = 3;
    return new Object[][]
        {
            {create1Unhealthy4HealthyHostWithLatency(loadBalancerStrategyType.DEGRADER, numIntervals)},
            {create1Unhealthy4HealthyHostWithLatency(loadBalancerStrategyType.RELATIVE, numIntervals)},
            {create1Unhealthy4HealthyHostWithError(loadBalancerStrategyType.DEGRADER, numIntervals)},
            {create1Unhealthy4HealthyHostWithError(loadBalancerStrategyType.RELATIVE, numIntervals)},
        };
  }

  @Test(dataProvider = "goingBadHost")
  public void testPointsDropToZero(LoadBalancerStrategyTestRunner goingBadHostRunner)
  {
    goingBadHostRunner.runWait();
    List<Integer> pointHistory = goingBadHostRunner.getPointHistory().get(goingBadHostRunner.getUri(0));

    assertEquals(pointHistory.get(0).intValue(), HEALTHY_POINTS);
    assertTrue(pointHistory.get(19).intValue() <= FULLY_DROPPED_POINTS, "The points should be below 2");
  }

  @DataProvider(name = "goingBadHost")
  public Object[][] getGoingBadHost()
  {
    int numIntervals = 20;
    return new Object[][]
        {
            {create1GoingBad4HealthyHostWithLatency(loadBalancerStrategyType.DEGRADER, numIntervals)},
            {create1GoingBad4HealthyHostWithLatency(loadBalancerStrategyType.RELATIVE, numIntervals)},
            {create1GoingBad4HealthyHostWithError(loadBalancerStrategyType.DEGRADER, numIntervals)},
            {create1GoingBad4HealthyHostWithError(loadBalancerStrategyType.RELATIVE, numIntervals)},
        };
  }

  @Test(dataProvider = "recoveringHost")
  public void testPointsRecoverToNormal(LoadBalancerStrategyTestRunner recoveringHostRunner)
  {
    recoveringHostRunner.runWait();

    // Get the point history for the unhealthy host
    List<Integer> pointHistory = recoveringHostRunner.getPointHistory().get(recoveringHostRunner.getUri(0));

    assertTrue(getLowestPoints(pointHistory) <= FULLY_DROPPED_POINTS, "Points should be fully dropped in the middle");
    assertEquals(pointHistory.get(34).intValue(), HEALTHY_POINTS, "Points should recover to 100");
  }

  @DataProvider(name = "recoveringHost")
  public Object[][] getRecoveringHost()
  {
    int numIntervals = 35;
    return new Object[][]
        {
            {create1Receovering4HealthyHostWithLatency(loadBalancerStrategyType.DEGRADER, numIntervals)},
            {create1Receovering4HealthyHostWithLatency(loadBalancerStrategyType.RELATIVE, numIntervals)},
            {create1Receovering4HealthyHostWithError(loadBalancerStrategyType.DEGRADER, numIntervals)},
            {create1Receovering4HealthyHostWithError(loadBalancerStrategyType.RELATIVE, numIntervals)},
        };
  }

  @Test(dataProvider = "strategy")
  public void testLowQps(loadBalancerStrategyType type)
  {
    Map<String, String> degraderPropertiesWithMinCallCount = new HashMap<>();
    D2RelativeStrategyProperties relativePropertiesWithMinCallCount = new D2RelativeStrategyProperties();
    // Set minCallCount to be 20
    degraderPropertiesWithMinCallCount.put(PropertyKeys.DEGRADER_MIN_CALL_COUNT, "20");
    relativePropertiesWithMinCallCount.setMinCallCount(20);

    LoadBalancerStrategyTestRunnerBuilder
        builder = new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, 5)
        // Only send 10 requests per interval
        .setConstantRequestCount(10)
        .setNumIntervals(10)
        // One host with unhealthy latency
        .setConstantLatency(Arrays.asList(UNHEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY));
    LoadBalancerStrategyTestRunner testRunner = type == loadBalancerStrategyType.DEGRADER
        ? builder.setDegraderStrategies(new HashMap<>(), degraderPropertiesWithMinCallCount).build()
        : builder.setRelativeLoadBalancerStrategies(relativePropertiesWithMinCallCount).build();

    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(1)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(2)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(3)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(4)).intValue(), HEALTHY_POINTS);
  }

  @Test(dataProvider = "strategy")
  public void testGrowingQps(loadBalancerStrategyType type)
  {
    Map<String, String> degraderPropertiesWithMinCallCount = new HashMap<>();
    D2RelativeStrategyProperties relativePropertiesWithMinCallCount = new D2RelativeStrategyProperties();

    // Set minCallCount to be 100
    degraderPropertiesWithMinCallCount.put(PropertyKeys.DEGRADER_MIN_CALL_COUNT, "100");
    relativePropertiesWithMinCallCount.setMinCallCount(100);

    LoadBalancerStrategyTestRunnerBuilder builder =
        new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, 5)
            // send growing traffic: 10, 60, 110, 160...
            .setDynamicRequestCount((intervalIndex) -> 10 + 50 * intervalIndex)
            .setNumIntervals(50)
            // One host with unhealthy latency
            .setConstantLatency(Arrays.asList(UNHEALTHY_HOST_CONSTANT_LATENCY,
                HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
                HEALTHY_HOST_CONSTANT_LATENCY));
     LoadBalancerStrategyTestRunner testRunner = type == loadBalancerStrategyType.DEGRADER
         ? builder.setDegraderStrategies(new HashMap<>(), degraderPropertiesWithMinCallCount).build()
         : builder.setRelativeLoadBalancerStrategies(relativePropertiesWithMinCallCount).build();

    testRunner.runWait();
    List<Integer> pointHistory = testRunner.getPointHistory().get(testRunner.getUri(0));
    int lowestPoints = getLowestPoints(pointHistory);

    assertEquals(pointHistory.get(3).intValue(), HEALTHY_POINTS,
        "The unhealthy host still has 100 points on 4th iteration because QPS was small");
    assertTrue(lowestPoints <= FULLY_DROPPED_POINTS, "The points will eventually drop");
  }

  @Test(dataProvider = "strategy")
  public void testDifferentUpDownStep(loadBalancerStrategyType type) {
    Map<String, String> degraderPropertiesWithUpDownStep = new HashMap<>();
    D2RelativeStrategyProperties relativePropertiesWithUpDownStep = new D2RelativeStrategyProperties();

    // Set up/downStep to be 0.3
    double step = 0.3;
    degraderPropertiesWithUpDownStep.put(PropertyKeys.DEGRADER_UP_STEP, String.valueOf(step));
    degraderPropertiesWithUpDownStep.put(PropertyKeys.DEGRADER_DOWN_STEP, String.valueOf(step));
    relativePropertiesWithUpDownStep.setUpStep(step);
    relativePropertiesWithUpDownStep.setDownStep(step);

    LoadBalancerStrategyTestRunnerBuilder builder =
        new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, 5)
            .setConstantRequestCount(1000)
            .setNumIntervals(3)
            // One host with unhealthy latency
            .setConstantLatency(Arrays.asList(UNHEALTHY_HOST_CONSTANT_LATENCY,
                HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
                HEALTHY_HOST_CONSTANT_LATENCY));
    LoadBalancerStrategyTestRunner testRunner = type == loadBalancerStrategyType.DEGRADER
        ? builder.setDegraderStrategies(new HashMap<>(), degraderPropertiesWithUpDownStep).build()
        : builder.setRelativeLoadBalancerStrategies(relativePropertiesWithUpDownStep).build();

    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(), (int) (HEALTHY_POINTS - 2 * step * HEALTHY_POINTS));
    assertEquals(pointsMap.get(testRunner.getUri(1)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(2)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(3)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(4)).intValue(), HEALTHY_POINTS);
  }

  @Test(dataProvider = "strategy")
  public void testOneHost(loadBalancerStrategyType type) {
    LoadBalancerStrategyTestRunner testRunner =
        new LoadBalancerStrategyTestRunnerBuilder(type,
            // Set to corner case - only 1 host
            DEFAULT_SERVICE_NAME, 1)
            .setConstantRequestCount(100)
            .setNumIntervals(3)
            .setConstantLatency(Arrays.asList(HEALTHY_HOST_CONSTANT_LATENCY))
            .build();
    testRunner.runWait();
    List<Integer> pointHistory = testRunner.getPointHistory().get(testRunner.getUri(0));

    assertEquals(pointHistory.get(2).intValue(), HEALTHY_POINTS);
  }

  @Test(dataProvider = "strategy")
  public void testStayQuarantined(loadBalancerStrategyType type) {
    Map<String, Object> strategyPropertiesWithQuarantineEnabled = new HashMap<>();
    D2RelativeStrategyProperties relativePropertiesWithQuarantineEnabled = new D2RelativeStrategyProperties();
    D2QuarantineProperties quarantineProperties = new D2QuarantineProperties().setQuarantineMaxPercent(DEFAULT_QUARANTINE_PERCENTAGE);

    strategyPropertiesWithQuarantineEnabled.put(PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT, String.valueOf(DEFAULT_QUARANTINE_PERCENTAGE));
    relativePropertiesWithQuarantineEnabled.setQuarantineProperties(quarantineProperties);

    LoadBalancerStrategyTestRunnerBuilder builder =
        new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, 5)
            .setConstantRequestCount(1000)
            .setNumIntervals(10)
            .setConstantLatency(Arrays.asList(UNHEALTHY_HOST_CONSTANT_LATENCY,
                HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
                HEALTHY_HOST_CONSTANT_LATENCY));
    LoadBalancerStrategyTestRunner testRunner = type == loadBalancerStrategyType.DEGRADER
        ? builder.setDegraderStrategies(strategyPropertiesWithQuarantineEnabled, new HashMap<>()).build()
        : builder.setRelativeLoadBalancerStrategies(relativePropertiesWithQuarantineEnabled).build();

    testRunner.runWait();
    List<Integer> pointHistory = testRunner.getPointHistory().get(testRunner.getUri(0));

    assertEquals(pointHistory.get(9).intValue(), QUARANTINED_POINTS);
  }

  @Test(dataProvider = "strategy")
  public void testQuarantineRecovery(loadBalancerStrategyType type) {
    Map<String, Object> strategyPropertiesWithQuarantineEnabled = new HashMap<>();
    D2RelativeStrategyProperties relativePropertiesWithQuarantineEnabled = new D2RelativeStrategyProperties();
    D2QuarantineProperties quarantineProperties = new D2QuarantineProperties().setQuarantineMaxPercent(DEFAULT_QUARANTINE_PERCENTAGE);

    strategyPropertiesWithQuarantineEnabled.put(PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT, String.valueOf(DEFAULT_QUARANTINE_PERCENTAGE));
    relativePropertiesWithQuarantineEnabled.setQuarantineProperties(quarantineProperties);

    LoadBalancerStrategyTestRunnerBuilder builder =
        new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, 5)
            .setConstantRequestCount(1000)
            .setNumIntervals(40)
            .setDynamicLatency(Arrays.asList(HOST_RECOVERING_TO_HEALTHY_LATENCY,
                HEALTHY_HOST_LATENCY_CORRELATION, HEALTHY_HOST_LATENCY_CORRELATION, HEALTHY_HOST_LATENCY_CORRELATION,
                HEALTHY_HOST_LATENCY_CORRELATION));
    LoadBalancerStrategyTestRunner testRunner = type == loadBalancerStrategyType.DEGRADER
        ? builder.setDegraderStrategies(strategyPropertiesWithQuarantineEnabled, new HashMap<>()).build()
        : builder.setRelativeLoadBalancerStrategies(relativePropertiesWithQuarantineEnabled).build();
    testRunner.runWait();
    List<Integer> pointHistory = testRunner.getPointHistory().get(testRunner.getUri(0));

    assertEquals(getLowestPoints(pointHistory), QUARANTINED_POINTS);
    assertEquals(pointHistory.get(39).intValue(), HEALTHY_POINTS);
  }

  @Test(dataProvider = "strategy")
  public void testQuarantineHittingMaxPercentage(loadBalancerStrategyType type) {
    Map<String, Object> strategyPropertiesWithQuarantineEnabled = new HashMap<>();
    D2RelativeStrategyProperties relativePropertiesWithQuarantineEnabled = new D2RelativeStrategyProperties();

    // Only 1/5 of the hosts can be quarantined
    double quarantinePercentage = 0.2;
    strategyPropertiesWithQuarantineEnabled.put(PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT, String.valueOf(quarantinePercentage));
    D2QuarantineProperties quarantineProperties = new D2QuarantineProperties().setQuarantineMaxPercent(quarantinePercentage);
    relativePropertiesWithQuarantineEnabled.setQuarantineProperties(quarantineProperties);

    LoadBalancerStrategyTestRunnerBuilder builder =
        new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, 5)
            .setConstantRequestCount(1000)
            .setNumIntervals(10)
            // 2 unhealthy hosts and 3 healthy host
            .setConstantLatency(Arrays.asList(UNHEALTHY_HOST_CONSTANT_LATENCY, UNHEALTHY_HOST_CONSTANT_LATENCY,
                HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY));
    LoadBalancerStrategyTestRunner testRunner = type == loadBalancerStrategyType.DEGRADER
        ? builder.setDegraderStrategies(strategyPropertiesWithQuarantineEnabled, new HashMap<>()).build()
        : builder.setRelativeLoadBalancerStrategies(relativePropertiesWithQuarantineEnabled).build();

    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    Assert.assertTrue(pointsMap.values().contains(QUARANTINED_POINTS));
    Assert.assertTrue(pointsMap.values().contains(INITIAL_RECOVERY_POINTS), "There should be host that is not quarantined but fully dropped");
  }

  @Test(dataProvider = "strategy")
  public void testFastRecovery(loadBalancerStrategyType type) {
    Map<String, Object> strategyPropertiesWithQuarantineEnabled = new HashMap<>();
    D2RelativeStrategyProperties relativePropertiesWithQuarantineEnabled = new D2RelativeStrategyProperties();

    strategyPropertiesWithQuarantineEnabled.put(PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT, String.valueOf(DEFAULT_QUARANTINE_PERCENTAGE));
    strategyPropertiesWithQuarantineEnabled.put(PropertyKeys.HTTP_LB_RING_RAMP_FACTOR, "2.0");
    relativePropertiesWithQuarantineEnabled.setQuarantineProperties(new D2QuarantineProperties().setQuarantineMaxPercent(DEFAULT_QUARANTINE_PERCENTAGE));
    relativePropertiesWithQuarantineEnabled.setEnableFastRecovery(true);

    LoadBalancerStrategyTestRunnerBuilder builder =
        new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, 5)
            .setConstantRequestCount(100)
            .setNumIntervals(30)
            .setDegraderStrategies(strategyPropertiesWithQuarantineEnabled, new HashMap<>())
            // All hosts with unhealthy latency
            .setDynamicLatency(Arrays.asList(HOST_RECOVERING_TO_HEALTHY_LATENCY,
                HEALTHY_HOST_LATENCY_CORRELATION, HEALTHY_HOST_LATENCY_CORRELATION, HEALTHY_HOST_LATENCY_CORRELATION,
                HEALTHY_HOST_LATENCY_CORRELATION));
    LoadBalancerStrategyTestRunner testRunner = type == loadBalancerStrategyType.DEGRADER
        ? builder.setDegraderStrategies(strategyPropertiesWithQuarantineEnabled, new HashMap<>()).build()
        : builder.setRelativeLoadBalancerStrategies(relativePropertiesWithQuarantineEnabled).build();

    testRunner.runWait();
    List<Integer> pointHistory = testRunner.getPointHistory().get(testRunner.getUri(0));

    assertTrue(hasPointsInHistory(pointHistory, Arrays.asList(2)), "Fast recovery should recover the points from 1 to 2 initially");
  }

  @Test(dataProvider = "strategy", retryAnalyzer = SingleRetry.class)
  public void testSlowStart(loadBalancerStrategyType type) {
    Map<String, String> degraderPropertiesWithSlowStart = new HashMap<>();
    D2RelativeStrategyProperties relativePropertiesWithSlowStart = new D2RelativeStrategyProperties();
    degraderPropertiesWithSlowStart.put(PropertyKeys.DEGRADER_SLOW_START_THRESHOLD, "0.2");
    relativePropertiesWithSlowStart.setSlowStartThreshold(0.2);

    LoadBalancerStrategyTestRunnerBuilder builder =
        new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, 5)
            .setConstantRequestCount(60)
            .setNumIntervals(50)
            .setDegraderStrategies(new HashMap<>(), degraderPropertiesWithSlowStart)
            // All hosts with unhealthy latency
            .setDynamicLatency(Arrays.asList(HOST_RECOVERING_TO_HEALTHY_LATENCY,
                HEALTHY_HOST_LATENCY_CORRELATION, HEALTHY_HOST_LATENCY_CORRELATION, HEALTHY_HOST_LATENCY_CORRELATION,
                HEALTHY_HOST_LATENCY_CORRELATION));
    LoadBalancerStrategyTestRunner testRunner = type == loadBalancerStrategyType.DEGRADER
        ? builder.setDegraderStrategies(new HashMap<>(), degraderPropertiesWithSlowStart).build()
        : builder.setRelativeLoadBalancerStrategies(relativePropertiesWithSlowStart).build();

    testRunner.runWait();
    List<Integer> pointHistory = testRunner.getPointHistory().get(testRunner.getUri(0));

    assertTrue(hasPointsInHistory(pointHistory, Arrays.asList(2, 4, 8, 16)), "Slow start should double the health score when it is below threshold");
  }

  @Test(dataProvider = "strategy")
  public void testSlowStartWithInitialHealthScore(loadBalancerStrategyType type)
  {
    Map<String, String> degraderPropertiesWithSlowStart = new HashMap<>();
    D2RelativeStrategyProperties relativePropertiesWithSlowStart = new D2RelativeStrategyProperties();
    degraderPropertiesWithSlowStart.put(PropertyKeys.DEGRADER_INITIAL_DROP_RATE, "0.99");
    degraderPropertiesWithSlowStart.put(PropertyKeys.DEGRADER_SLOW_START_THRESHOLD, "0.5");
    relativePropertiesWithSlowStart.setInitialHealthScore(0.01);
    relativePropertiesWithSlowStart.setSlowStartThreshold(0.5);

    LoadBalancerStrategyTestRunnerBuilder builder =
        new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, 5)
            .setConstantRequestCount(60)
            .setNumIntervals(30)
            .setDegraderStrategies(new HashMap<>(), degraderPropertiesWithSlowStart)
            // All hosts with healthy latency
            .setConstantLatency(Arrays.asList(HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
                HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY));
    LoadBalancerStrategyTestRunner testRunner = type == loadBalancerStrategyType.DEGRADER
        ? builder.setDegraderStrategies(new HashMap<>(), degraderPropertiesWithSlowStart).build()
        : builder.setRelativeLoadBalancerStrategies(relativePropertiesWithSlowStart).build();

    testRunner.runWait();
    List<Integer> pointHistory = testRunner.getPointHistory().get(testRunner.getUri(0));

    assertTrue(hasPointsInHistory(pointHistory, Arrays.asList(1, 4, 16)));
  }

  @Test(dataProvider = "strategy")
  public void testErrorStatusMatch(loadBalancerStrategyType type)
  {
    Map<String, Object> strategyPropertiesWithErrorFilter = new HashMap<>();
    D2RelativeStrategyProperties relativePropertiesWithErrorFilter = new D2RelativeStrategyProperties();
    // Only 503 is counted as error
    strategyPropertiesWithErrorFilter.put(PropertyKeys.HTTP_LB_ERROR_STATUS_REGEX, "(503)");
    relativePropertiesWithErrorFilter.setErrorStatusFilter(
        new HttpStatusCodeRangeArray(Arrays.asList(new HttpStatusCodeRange().setLowerBound(503).setUpperBound(503))));

    LoadBalancerStrategyTestRunnerBuilder
        builder = new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(6)
        .setConstantLatency(Arrays.asList(HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY))
        .setConstantErrorCount(Arrays.asList(UNHEALTHY_ERROR_COUNT, HEALTHY_ERROR_COUNT, HEALTHY_ERROR_COUNT,
            HEALTHY_ERROR_COUNT, HEALTHY_ERROR_COUNT));
    LoadBalancerStrategyTestRunner testRunner = type == loadBalancerStrategyType.DEGRADER
        ? builder.setDegraderStrategies(strategyPropertiesWithErrorFilter, new HashMap<>()).build()
        : builder.setRelativeLoadBalancerStrategies(relativePropertiesWithErrorFilter).build();

    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    // Event with the error 500, the host is not marked as unhealthy
    assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(1)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(2)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(3)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(4)).intValue(), HEALTHY_POINTS);
  }

  @Test(dataProvider = "strategy")
  public void testPartitionWeightChange(loadBalancerStrategyType type)
  {
    double weight = 0.5;
    Map<Integer, PartitionData> partitionDataMap = new HashMap<>();
    partitionDataMap.put(LoadBalancerStrategyTestRunner.DEFAULT_PARTITION_ID, new PartitionData(weight));

    LoadBalancerStrategyTestRunner testRunner = new LoadBalancerStrategyTestRunnerBuilder(type,
        DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .addPartitionDataMap(0, partitionDataMap)
        .setNumIntervals(3)
        .setConstantLatency(Arrays.asList(HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY))
        .build();

    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(), (int) (weight * HEALTHY_POINTS));
    assertEquals(pointsMap.get(testRunner.getUri(1)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(2)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(3)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(4)).intValue(), HEALTHY_POINTS);
  }

  @DataProvider(name = "strategy")
  public Object[][] getStrategy()
  {
    return new Object[][]
        {
            {loadBalancerStrategyType.DEGRADER},
            {loadBalancerStrategyType.RELATIVE}
        };
  }

  @Test
  public void testMostHostWithHighLatency()
  {
    LoadBalancerStrategyTestRunner testRunner = new LoadBalancerStrategyTestRunnerBuilder(loadBalancerStrategyType.RELATIVE,
        DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(6)
        // 4/5 hosts have high latency, the average will be higher
        .setConstantLatency(Arrays.asList(UNHEALTHY_HOST_CONSTANT_LATENCY,
            UNHEALTHY_HOST_CONSTANT_LATENCY, UNHEALTHY_HOST_CONSTANT_LATENCY, UNHEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY))
        .build();

    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(1)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(2)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(3)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMap.get(testRunner.getUri(4)).intValue(), HEALTHY_POINTS);
  }

  @Test(dataProvider = "highFactor")
  public void testDifferentHighLatencyFactors(double highFactor)
  {
    long unhealthyLatency = 800L;
    long healthyLatency = 400L;
    long avgLatency = (unhealthyLatency + 4 * healthyLatency) / 5;

    D2RelativeStrategyProperties relativeStrategyProperties = new D2RelativeStrategyProperties()
        .setRelativeLatencyHighThresholdFactor(highFactor);

    LoadBalancerStrategyTestRunner testRunner = new LoadBalancerStrategyTestRunnerBuilder(loadBalancerStrategyType.RELATIVE,
        DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(3)
        .setConstantLatency(
            Arrays.asList(unhealthyLatency, healthyLatency, healthyLatency, healthyLatency, healthyLatency))
        .setRelativeLoadBalancerStrategies(relativeStrategyProperties)
        .build();

    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    if (highFactor < (double) unhealthyLatency / avgLatency)
    {
      assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(),
          (int) (HEALTHY_POINTS - RelativeLoadBalancerStrategyFactory.DEFAULT_DOWN_STEP * HEALTHY_POINTS * 2));
    }
    else
    {
      assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(), HEALTHY_POINTS);
    }
  }

  @DataProvider(name = "highFactor")
  public Object[][] getHighFactor()
  {
    return new Object[][]
        {
            {1.2},
            {1.3},
            {1.4},
            {1.5},
            {1.6},
            {1.7},
            {1.8}
        };
  }

  @Test
  public void testOneHostBelongToMultiplePartitions()
  {
    Map<Integer, PartitionData> partitionDataMapForBothPartitions = new HashMap<>();
    partitionDataMapForBothPartitions.put(0, new PartitionData(DEFAULT_WEIGHT));
    partitionDataMapForBothPartitions.put(1, new PartitionData(DEFAULT_WEIGHT));
    Map<Integer, PartitionData> partitionDataMapPartition0 = new HashMap<>();
    partitionDataMapPartition0.put(0, new PartitionData(DEFAULT_WEIGHT));
    Map<Integer, PartitionData> partitionDataMapPartition1 = new HashMap<>();
    partitionDataMapPartition1.put(1, new PartitionData(DEFAULT_WEIGHT));

    LoadBalancerStrategyTestRunner testRunner = new LoadBalancerStrategyTestRunnerBuilder(loadBalancerStrategyType.RELATIVE,
        DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        // There are 2 partitions
        .addPartitionUriMap(0, Arrays.asList(0, 1, 2))
        .addPartitionUriMap(1, Arrays.asList(0, 3, 4))
        .addPartitionDataMap(0, partitionDataMapForBothPartitions)
        .addPartitionDataMap(1, partitionDataMapPartition0)
        .addPartitionDataMap(2, partitionDataMapPartition0)
        .addPartitionDataMap(3, partitionDataMapPartition1)
        .addPartitionDataMap(4, partitionDataMapPartition1)
        .setNumIntervals(3)
        // Host 0, 3, 4 have high latency, host 1 & 2 have healthy latency
        .setConstantLatency(
            Arrays.asList(UNHEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
            UNHEALTHY_HOST_CONSTANT_LATENCY, UNHEALTHY_HOST_CONSTANT_LATENCY))
        .build();

    // Send traffic to partition 0 and 1
    testRunner.runWait(Arrays.asList(0, 1));

    Map<URI, Integer> pointsMapPartition0 = testRunner.getPoints(0);
    Map<URI, Integer> pointsMapPartition1 = testRunner.getPoints(1);

    assertEquals(pointsMapPartition0.get(testRunner.getUri(0)).intValue(),
        (int) (HEALTHY_POINTS - RelativeLoadBalancerStrategyFactory.DEFAULT_DOWN_STEP * HEALTHY_POINTS * 2));
    assertEquals(pointsMapPartition1.get(testRunner.getUri(0)).intValue(), HEALTHY_POINTS);
  }

  @Test
  public void testAllHostsBelongToMultiplePartitions()
  {
    Map<Integer, PartitionData> partitionDataMapForBothPartitions = new HashMap<>();
    partitionDataMapForBothPartitions.put(0, new PartitionData(DEFAULT_WEIGHT));
    partitionDataMapForBothPartitions.put(1, new PartitionData(DEFAULT_WEIGHT));

    LoadBalancerStrategyTestRunner testRunner = new LoadBalancerStrategyTestRunnerBuilder(loadBalancerStrategyType.RELATIVE,
        DEFAULT_SERVICE_NAME, 3)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        // There are 2 partitions
        .addPartitionUriMap(0, Arrays.asList(0, 1, 2))
        .addPartitionUriMap(1, Arrays.asList(0, 1, 2))
        .addPartitionDataMap(0, partitionDataMapForBothPartitions)
        .addPartitionDataMap(1, partitionDataMapForBothPartitions)
        .addPartitionDataMap(2, partitionDataMapForBothPartitions)
        .setNumIntervals(3)
        .setConstantLatency(
            Arrays.asList(UNHEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY))
        .build();

    // Send traffic to partition 0 and 1
    testRunner.runWait(Arrays.asList(0, 1));

    Map<URI, Integer> pointsMapPartition0 = testRunner.getPoints(0);
    Map<URI, Integer> pointsMapPartition1 = testRunner.getPoints(1);

    assertEquals(pointsMapPartition0.get(testRunner.getUri(0)).intValue(),
        (int) (HEALTHY_POINTS - RelativeLoadBalancerStrategyFactory.DEFAULT_DOWN_STEP * HEALTHY_POINTS * 2));
    assertEquals(pointsMapPartition0.get(testRunner.getUri(1)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMapPartition0.get(testRunner.getUri(2)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMapPartition1.get(testRunner.getUri(0)).intValue(),
        (int) (HEALTHY_POINTS - RelativeLoadBalancerStrategyFactory.DEFAULT_DOWN_STEP * HEALTHY_POINTS * 2));
    assertEquals(pointsMapPartition1.get(testRunner.getUri(1)).intValue(), HEALTHY_POINTS);
    assertEquals(pointsMapPartition1.get(testRunner.getUri(2)).intValue(), HEALTHY_POINTS);
  }

  private static int getLowestPoints(List<Integer> pointHistory)
  {
    return pointHistory.stream().min(Integer::compareTo)
        .orElse(HEALTHY_POINTS);
  }

  /**
   * Verify a certain sequence occurred in the point history
   */
  private static boolean hasPointsInHistory(List<Integer> pointHistory, List<Integer> expectedPointsSequence) {
    int expectedPointsIndex = 0;
    int pointHistoryIndex = 0;
    while (pointHistoryIndex < pointHistory.size() && expectedPointsIndex < expectedPointsSequence.size()) {
      if (expectedPointsSequence.get(expectedPointsIndex) != pointHistory.get(pointHistoryIndex)) {
        pointHistoryIndex ++;
        continue;
      }
      pointHistoryIndex ++;
      expectedPointsIndex ++;
    }

    return expectedPointsIndex == expectedPointsSequence.size();
  }

  /**
   * The following methods create some default test scenarios
   */
  private static LoadBalancerStrategyTestRunner create1Unhealthy4HealthyHostWithLatency(loadBalancerStrategyType type, int numIntervals)
  {
    return new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(numIntervals)
        .setConstantLatency(Arrays.asList(UNHEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY))
        .build();
  }

  private static LoadBalancerStrategyTestRunner create1Receovering4HealthyHostWithLatency(loadBalancerStrategyType type, int numIntervals)
  {
    return new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(numIntervals)
        .setDynamicLatency(Arrays.asList(HOST_RECOVERING_TO_HEALTHY_LATENCY,
            HEALTHY_HOST_LATENCY_CORRELATION, HEALTHY_HOST_LATENCY_CORRELATION, HEALTHY_HOST_LATENCY_CORRELATION,
            HEALTHY_HOST_LATENCY_CORRELATION))
        .build();
  }

  private static LoadBalancerStrategyTestRunner create1GoingBad4HealthyHostWithLatency(loadBalancerStrategyType type, int numIntervals)
  {
    return new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(numIntervals)
        .setDynamicLatency(Arrays.asList(HOST_BECOMING_UNHEALTHY_LATENCY,
            HEALTHY_HOST_LATENCY_CORRELATION, HEALTHY_HOST_LATENCY_CORRELATION, HEALTHY_HOST_LATENCY_CORRELATION,
            HEALTHY_HOST_LATENCY_CORRELATION))
        .build();
  }

  private static LoadBalancerStrategyTestRunner create1Unhealthy4HealthyHostWithError(loadBalancerStrategyType type, int numIntervals)
  {
    LoadBalancerStrategyTestRunnerBuilder
        builder = new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(numIntervals)
        .setConstantLatency(Arrays.asList(HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY))
        .setConstantErrorCount(Arrays.asList(UNHEALTHY_ERROR_COUNT, HEALTHY_ERROR_COUNT, HEALTHY_ERROR_COUNT,
            HEALTHY_ERROR_COUNT, HEALTHY_ERROR_COUNT));
    return setDefaultErrorRate(builder, type).build();
  }

  private static LoadBalancerStrategyTestRunner create1Receovering4HealthyHostWithError(loadBalancerStrategyType type, int numIntervals)
  {
    LoadBalancerStrategyTestRunnerBuilder
        builder = new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(numIntervals)
        .setConstantLatency(Arrays.asList(HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY))
        .setDynamicErrorCount(Arrays.asList(HOST_RECOVERING_TO_HEALTHY_ERROR, HEALTHY_HOST_ERROR_COUNT_CORRELATION,
            HEALTHY_HOST_ERROR_COUNT_CORRELATION, HEALTHY_HOST_ERROR_COUNT_CORRELATION, HEALTHY_HOST_ERROR_COUNT_CORRELATION));
    return setDefaultErrorRate(builder, type).build();
  }

  private static LoadBalancerStrategyTestRunner create1GoingBad4HealthyHostWithError(loadBalancerStrategyType type, int numIntervals)
  {
    LoadBalancerStrategyTestRunnerBuilder
        builder = new LoadBalancerStrategyTestRunnerBuilder(type, DEFAULT_SERVICE_NAME, DEFAULT_NUM_HOSTS)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(numIntervals)
        .setConstantLatency(Arrays.asList(HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY, HEALTHY_HOST_CONSTANT_LATENCY,
            HEALTHY_HOST_CONSTANT_LATENCY))
        .setDynamicErrorCount(Arrays.asList(HOST_BECOMING_UNHEALTHY_ERROR, HEALTHY_HOST_ERROR_COUNT_CORRELATION,
            HEALTHY_HOST_ERROR_COUNT_CORRELATION, HEALTHY_HOST_ERROR_COUNT_CORRELATION, HEALTHY_HOST_ERROR_COUNT_CORRELATION));
    return setDefaultErrorRate(builder, type).build();
  }

  private static LoadBalancerStrategyTestRunnerBuilder setDefaultErrorRate(LoadBalancerStrategyTestRunnerBuilder builder, loadBalancerStrategyType type)
  {
    switch (type)
    {
      case RELATIVE:
        return builder.setRelativeLoadBalancerStrategies(new D2RelativeStrategyProperties()
            .setLowErrorRate(Double.valueOf(DEFAULT_LOW_ERROR_RATE))
            .setHighErrorRate(Double.valueOf(DEFAULT_HIGH_ERROR_RATE)));
      case DEGRADER:
      default:
        return builder.setDegraderStrategies(new HashMap<>(), DEGRADER_PROPERTIES_WITH_HIGH_LOW_ERROR);
    }
  }
}

package com.linkedin.d2.loadbalancer.strategies;

import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.balancer.strategies.framework.LatencyCorrelation;
import com.linkedin.d2.balancer.strategies.framework.LoadBalancerStrategyTestRunner;
import com.linkedin.d2.balancer.strategies.framework.LoadBalancerStrategyTestRunnerBuilder;
import com.linkedin.d2.loadBalancerStrategyType;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


/**
 * In this class we simulate the possible production scenarios, and measures the cluster performance
 * The tests are trying to find a reasonable value for relativeLatencyHigh/LowThresholdFactor
 * Please note the test assumptions are subjective here, it assumes cluster average is 200, and 250 is healthy latency
 */
public class TestLoadBalancerPerformanceSimulation {
  private static final Logger LOG = LoggerFactory.getLogger(TestLoadBalancerPerformanceSimulation.class);

  private static final int STAGING_CLUSTER_HOST_NUM = 2;
  private static final int SMALL_CLUSTER_HOST_NUM = 10;
  private static final int MEDIUM_CLUSTER_HOST_NUM = 50;
  private static final int LARGE_CLUSTER_HOST_NUM = 200;
  private static final String DEFAULT_SERVICE_NAME = "dummyService";
  private static final int DEFAULT_REQUESTS_PER_INTERVAL = 1000;
  private static final int HEALTHY_POINTS = 100;
  private static final int UNHEALTHY_POINTS = 1;

  private static final long HEALTHY_LATENCY = 200L;
  private static final long HEALTHY_HIGHER_LATENCY = 250L;
  private static final long MODERATE_BAD_LATENCY = 400L;
  private static final long SEVERE_BAD_LATENCY = 1000L;

  private static final LatencyCorrelation HOST_BECOMING_MODERATE_BAD_LATENCY =
      (callCount, intervalIndex) -> Long.max(SEVERE_BAD_LATENCY - intervalIndex * 100L, MODERATE_BAD_LATENCY);
  private static final LatencyCorrelation HOST_BECOMING_HEALTHY_HIGHER_LATENCY =
      (callCount, intervalIndex) -> Long.max(SEVERE_BAD_LATENCY - intervalIndex * 100L, HEALTHY_HIGHER_LATENCY);
  private static final LatencyCorrelation HOST_BECOMING_HEALTHY_LATENCY =
      (callCount, intervalIndex) -> Long.max(SEVERE_BAD_LATENCY - intervalIndex * 100L, HEALTHY_LATENCY);
  private static final LatencyCorrelation HEALTHY_HOST_LATENCY_CORRELATION =
    (callCount, intervalIndex) -> HEALTHY_LATENCY;

  /**
   * Based on this simulation, a healthy host can be classified as unhealthy when factor = 1.2
   */
  @Test(dataProvider = "relativeLatencyHighThresholdFactor")
  public void testOneConstantHigherLatencyHost(double relativeLatencyHighThresholdFactor, int numHosts)
  {
    LoadBalancerStrategyTestRunner testRunner =
        buildDefaultRunnerWithConstantBadHost(numHosts, HEALTHY_HIGHER_LATENCY, relativeLatencyHighThresholdFactor);
    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    if (relativeLatencyHighThresholdFactor <= 1.2 && numHosts >= SMALL_CLUSTER_HOST_NUM)
    {
      assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(),  UNHEALTHY_POINTS);
    }
    else
    {
      assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(),  HEALTHY_POINTS);
    }
  }

  /**
   * Based on this simulation, when the cluster is extremely small, highThresholdFactor = 1.4 and above may consider an unhealthy host as healthy
   */
  @Test(dataProvider = "relativeLatencyHighThresholdFactor")
  public void testOneConstantModerateBadHostInStagingCluster(double relativeLatencyHighThresholdFactor, int numHosts)
  {
    LoadBalancerStrategyTestRunner testRunner =
        buildDefaultRunnerWithConstantBadHost(numHosts, MODERATE_BAD_LATENCY, relativeLatencyHighThresholdFactor);
    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    if (relativeLatencyHighThresholdFactor >= 1.4 && numHosts <= STAGING_CLUSTER_HOST_NUM)
    {
      assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(),  HEALTHY_POINTS);
    }
    else
    {
      assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(),  UNHEALTHY_POINTS);
    }
  }

  @Test(dataProvider = "relativeLatencyHighThresholdFactor")
  public void testOneConstantSevereBadHost(double relativeLatencyHighThresholdFactor, int numHosts)
  {
    LoadBalancerStrategyTestRunner testRunner =
        buildDefaultRunnerWithConstantBadHost(numHosts, SEVERE_BAD_LATENCY, relativeLatencyHighThresholdFactor);
    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(),  1);
  }

  @Test(dataProvider = "relativeLatencyLowThresholdFactor")
  public void testHostRecoveringToModerateUnhealthy(double relativeLatencyLowThresholdFactor, int numHosts)
  {
    LoadBalancerStrategyTestRunner testRunner =
        buildDefaultRunnerWithRecoveringBadHost(numHosts, HOST_BECOMING_MODERATE_BAD_LATENCY, relativeLatencyLowThresholdFactor);
    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(),  UNHEALTHY_POINTS);
  }

  @Test(dataProvider = "relativeLatencyLowThresholdFactor")
  public void testHostRecoveringToHealthyWithHigherLatency(double relativeLatencyLowThresholdFactor, int numHosts)
  {
    LoadBalancerStrategyTestRunner testRunner =
        buildDefaultRunnerWithRecoveringBadHost(numHosts, HOST_BECOMING_HEALTHY_HIGHER_LATENCY, relativeLatencyLowThresholdFactor);
    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    if (relativeLatencyLowThresholdFactor <= 1.2)
    {
      assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(),  UNHEALTHY_POINTS);
    }
    else
    {
      assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(),  HEALTHY_POINTS);
    }

  }

  @Test(dataProvider = "relativeLatencyLowThresholdFactor")
  public void testHostRecoveringToHealthy(double relativeLatencyLowThresholdFactor, int numHosts)
  {
    LoadBalancerStrategyTestRunner testRunner =
        buildDefaultRunnerWithRecoveringBadHost(numHosts, HOST_BECOMING_HEALTHY_LATENCY, relativeLatencyLowThresholdFactor);
    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(), HEALTHY_POINTS);
  }

  @DataProvider(name = "relativeLatencyHighThresholdFactor")
  public Object[][] getRelativeLatencyHighThresholdFactor()
  {
    double factor1 = 1.2;
    double factor2 = 1.3;
    double factor3 = 1.4;
    double factor4 = 1.5;
    return new Object[][]
        {
            {factor1, STAGING_CLUSTER_HOST_NUM},
            {factor2, STAGING_CLUSTER_HOST_NUM},
            {factor3, STAGING_CLUSTER_HOST_NUM},
            {factor4, STAGING_CLUSTER_HOST_NUM},
            {factor1, SMALL_CLUSTER_HOST_NUM},
            {factor2, SMALL_CLUSTER_HOST_NUM},
            {factor3, SMALL_CLUSTER_HOST_NUM},
            {factor4, SMALL_CLUSTER_HOST_NUM},
            {factor1, MEDIUM_CLUSTER_HOST_NUM},
            {factor2, MEDIUM_CLUSTER_HOST_NUM},
            {factor3, MEDIUM_CLUSTER_HOST_NUM},
            {factor4, MEDIUM_CLUSTER_HOST_NUM},
            {factor1, LARGE_CLUSTER_HOST_NUM},
            {factor2, LARGE_CLUSTER_HOST_NUM},
            {factor3, LARGE_CLUSTER_HOST_NUM},
            {factor4, LARGE_CLUSTER_HOST_NUM}
        };
  }

  @DataProvider(name = "relativeLatencyLowThresholdFactor")
  public Object[][] getRelativeLatencyLowThresholdFactor()
  {
    double factor1 = 1.2;
    double factor2 = 1.3;
    double factor3 = 1.4;
    double factor4 = 1.5;
    return new Object[][]
        {
            {factor1, STAGING_CLUSTER_HOST_NUM},
            {factor2, STAGING_CLUSTER_HOST_NUM},
            {factor3, STAGING_CLUSTER_HOST_NUM},
            {factor4, STAGING_CLUSTER_HOST_NUM},
            {factor1, SMALL_CLUSTER_HOST_NUM},
            {factor2, SMALL_CLUSTER_HOST_NUM},
            {factor3, SMALL_CLUSTER_HOST_NUM},
            {factor4, SMALL_CLUSTER_HOST_NUM},
            {factor1, MEDIUM_CLUSTER_HOST_NUM},
            {factor2, MEDIUM_CLUSTER_HOST_NUM},
            {factor3, MEDIUM_CLUSTER_HOST_NUM},
            {factor4, MEDIUM_CLUSTER_HOST_NUM},
            {factor1, LARGE_CLUSTER_HOST_NUM},
            {factor2, LARGE_CLUSTER_HOST_NUM},
            {factor3, LARGE_CLUSTER_HOST_NUM},
            {factor4, LARGE_CLUSTER_HOST_NUM}
        };
  }

  @Test(dataProvider = "latencyFactorThreshold")
  public void testLinearCallCountLatencyCorrelation(double relativeLatencyLowThresholdFactor, double relativeLatencyHighThresholdFactor,
      int requestCountPerInterval)
  {
    double badHostLinearFactor = 0.1;
    double normalHostLinearFactor = 0.01;
    LoadBalancerStrategyTestRunner testRelativeRunner =
        buildDefaultRelativeRunnerWithLinearLatency(10, badHostLinearFactor, normalHostLinearFactor,
            relativeLatencyHighThresholdFactor, relativeLatencyLowThresholdFactor, requestCountPerInterval);
    testRelativeRunner.runWait();
    double relativeStrategyAverageLatency = testRelativeRunner.getAvgLatency();

    LoadBalancerStrategyTestRunner testDegraderRunner =
        buildDefaultDegraderRunnerWithLinearLatency(10, badHostLinearFactor, normalHostLinearFactor, requestCountPerInterval);
    testDegraderRunner.runWait();
    double degraderStrategyAverageLatency = testDegraderRunner.getAvgLatency();

    if (relativeLatencyHighThresholdFactor <= 1.2 && requestCountPerInterval >= 10000)
    {
      Assert.assertTrue(relativeStrategyAverageLatency < degraderStrategyAverageLatency,
          "With lower latency threshold and higher request number, the load balancer kicks in earlier, which gives a lower average cluster latency");
    }
  }

  @DataProvider(name = "latencyFactorThreshold")
  public Object[][] getLatencyFactorThreshold()
  {
    int highRequestCountPerInterval = 10000;
    int midRequestCountPerInterval = 1000;
    int lowRequestCountPerInterval = 100;
    return new Object[][]
        {
            {1.1, 1.2, highRequestCountPerInterval},
            {1.3, 1.4, highRequestCountPerInterval},
            {1.1, 1.2, midRequestCountPerInterval},
            {1.3, 1.4, midRequestCountPerInterval},
            {1.1, 1.2, lowRequestCountPerInterval},
            {1.3, 1.4, lowRequestCountPerInterval}
        };
  }

  /**
   * Test a list of hosts that have very different latency by nature
   */
  @Test
  public void testDifferentLatency()
  {
    LoadBalancerStrategyTestRunner testRelativeRunner1 =
        buildRelativeRunnerWithDifferentLatency(1.2);
    testRelativeRunner1.runWait();
    double relativeStrategyAverageLatency1 = testRelativeRunner1.getAvgLatency();
    LOG.info("relativeStrategyAverageLatency: " + relativeStrategyAverageLatency1 + ", final points: " + testRelativeRunner1.getPoints());

    LoadBalancerStrategyTestRunner testRelativeRunner2 =
        buildRelativeRunnerWithDifferentLatency(1.3);
    testRelativeRunner2.runWait();
    double relativeStrategyAverageLatency2 = testRelativeRunner2.getAvgLatency();
    LOG.info("relativeStrategyAverageLatency: " + relativeStrategyAverageLatency2 + ", final points: " + testRelativeRunner2.getPoints());

    LoadBalancerStrategyTestRunner testRelativeRunner3 =
        buildRelativeRunnerWithDifferentLatency(1.4);
    testRelativeRunner3.runWait();
    double relativeStrategyAverageLatency3 = testRelativeRunner3.getAvgLatency();
    LOG.info("relativeStrategyAverageLatency: " + relativeStrategyAverageLatency3 + ", final points: " + testRelativeRunner3.getPoints());

    /**
     * With lowest latency factor, half of the hosts are marked as unhealthy, cluster has lower average latency
     * With the highest latency factor, only 1 host is marked as unhealthy
     */
    assertTrue(relativeStrategyAverageLatency1 < relativeStrategyAverageLatency2);
    assertTrue(relativeStrategyAverageLatency2 < relativeStrategyAverageLatency3);
  }

  private LoadBalancerStrategyTestRunner buildDefaultRunnerWithConstantBadHost(int numHosts, long badHostLatency,
      double relativeLatencyHighThresholdFactor)
  {
    List<Long> constantLatencyList = new ArrayList<>();
    constantLatencyList.add(badHostLatency);
    for (int i = 0; i < numHosts - 1; i ++)
    {
      constantLatencyList.add(HEALTHY_LATENCY);
    }

    D2RelativeStrategyProperties relativeStrategyProperties = new D2RelativeStrategyProperties()
        .setRelativeLatencyHighThresholdFactor(relativeLatencyHighThresholdFactor);

    return new LoadBalancerStrategyTestRunnerBuilder(loadBalancerStrategyType.RELATIVE, DEFAULT_SERVICE_NAME, numHosts)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(30)
        .setConstantLatency(constantLatencyList)
        .setRelativeLoadBalancerStrategies(relativeStrategyProperties)
        .build();
  }

  private LoadBalancerStrategyTestRunner buildDefaultRunnerWithRecoveringBadHost(int numHosts, LatencyCorrelation recoveringHostLatencyCorrelation,
      double relativeLatencyLowThresholdFactor)
  {
    List<LatencyCorrelation> latencyCorrelationList = new ArrayList<>();
    latencyCorrelationList.add(recoveringHostLatencyCorrelation);
    for (int i = 0; i < numHosts - 1; i ++)
    {
      latencyCorrelationList.add(HEALTHY_HOST_LATENCY_CORRELATION);
    }

    D2RelativeStrategyProperties relativeStrategyProperties = new D2RelativeStrategyProperties()
        .setRelativeLatencyLowThresholdFactor(relativeLatencyLowThresholdFactor)
        .setRelativeLatencyHighThresholdFactor(1.5);

    return new LoadBalancerStrategyTestRunnerBuilder(loadBalancerStrategyType.RELATIVE, DEFAULT_SERVICE_NAME, numHosts)
        .setConstantRequestCount(DEFAULT_REQUESTS_PER_INTERVAL)
        .setNumIntervals(100)
        .setDynamicLatency(latencyCorrelationList)
        .setRelativeLoadBalancerStrategies(relativeStrategyProperties)
        .build();
  }

  private LoadBalancerStrategyTestRunner buildDefaultRelativeRunnerWithLinearLatency(int numHosts, double badHostLinearFactor,
      double normalHostLinearFactor, double relativeLatencyHighThresholdFactor, double relativeLatencyLowThresholdFactor,
      int requestCountPerInterval)
  {
    List<LatencyCorrelation> latencyCorrelationList = new ArrayList<>();
    latencyCorrelationList.add((requestsPerInterval, intervalIndex) ->
        HEALTHY_LATENCY + (long) (badHostLinearFactor * requestsPerInterval));
    for (int i = 0; i < numHosts - 1; i ++)
    {
      latencyCorrelationList.add((requestsPerInterval, intervalIndex) ->
          HEALTHY_LATENCY + (long) (normalHostLinearFactor * requestsPerInterval));
    }

    D2RelativeStrategyProperties relativeStrategyProperties = new D2RelativeStrategyProperties()
        .setRelativeLatencyHighThresholdFactor(relativeLatencyHighThresholdFactor)
        .setRelativeLatencyLowThresholdFactor(relativeLatencyLowThresholdFactor);

    return new LoadBalancerStrategyTestRunnerBuilder(loadBalancerStrategyType.RELATIVE, DEFAULT_SERVICE_NAME, numHosts)
        .setConstantRequestCount(requestCountPerInterval)
        .setNumIntervals(30)
        .setDynamicLatency(latencyCorrelationList)
        .setRelativeLoadBalancerStrategies(relativeStrategyProperties)
        .build();
  }

  private LoadBalancerStrategyTestRunner buildDefaultDegraderRunnerWithLinearLatency(int numHosts, double badHostLinearFactor,
      double normalHostLinearFactor, int requestCountPerInterval)
  {
    List<LatencyCorrelation> latencyCorrelationList = new ArrayList<>();
    latencyCorrelationList.add((requestsPerInterval, intervalIndex) ->
        HEALTHY_LATENCY + (long) (badHostLinearFactor * requestsPerInterval));
    for (int i = 0; i < numHosts - 1; i ++)
    {
      latencyCorrelationList.add((requestsPerInterval, intervalIndex) ->
          HEALTHY_LATENCY + (long) (normalHostLinearFactor * requestsPerInterval));
    }

    return new LoadBalancerStrategyTestRunnerBuilder(loadBalancerStrategyType.DEGRADER, DEFAULT_SERVICE_NAME, numHosts)
        .setConstantRequestCount(requestCountPerInterval)
        .setNumIntervals(30)
        .setDynamicLatency(latencyCorrelationList)
        .build();
  }

  private LoadBalancerStrategyTestRunner buildRelativeRunnerWithDifferentLatency(double relativeLatencyHighThresholdFactor)
  {
    int minBaseLatency = 100;
    int baseLatencyDiff = 20;
    int numHosts = 10;
    double hostLinearFactor = 0.05;

    List<LatencyCorrelation> latencyCorrelationList = new ArrayList<>();
    for (int i = 0; i < numHosts; i ++)
    {
      long baseLatency = i * baseLatencyDiff + minBaseLatency;
      latencyCorrelationList.add((requestsPerInterval, intervalIndex) ->
          baseLatency + (long) (hostLinearFactor * requestsPerInterval));
    }

    D2RelativeStrategyProperties relativeStrategyProperties = new D2RelativeStrategyProperties()
        .setRelativeLatencyHighThresholdFactor(relativeLatencyHighThresholdFactor);

    return new LoadBalancerStrategyTestRunnerBuilder(loadBalancerStrategyType.RELATIVE, DEFAULT_SERVICE_NAME, numHosts)
        .setConstantRequestCount(10000)
        .setNumIntervals(30)
        .setDynamicLatency(latencyCorrelationList)
        .setRelativeLoadBalancerStrategies(relativeStrategyProperties)
        .build();
  }
}

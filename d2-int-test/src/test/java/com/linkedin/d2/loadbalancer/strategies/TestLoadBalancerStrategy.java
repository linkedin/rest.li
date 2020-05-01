package com.linkedin.d2.loadbalancer.strategies;

import com.linkedin.d2.balancer.strategies.framework.LatencyQPSCorrelation;
import com.linkedin.d2.balancer.strategies.framework.LoadBalancerStrategyTestRunner;
import com.linkedin.d2.balancer.strategies.framework.LoadBalancerStrategyTestRunnerBuilder;
import com.linkedin.d2.loadBalancerStrategyType;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

import static com.linkedin.d2.balancer.properties.PropertyKeys.*;
import static org.testng.Assert.*;


/**
 * Integration tests for all the load balancer strategies
 */
public class TestLoadBalancerStrategy {
  private static final String DUMMY_SERVICE_NAME = "dummyService";

  @Test
  public void testStrategyV3HealthPoints() {
    long healthyLatency = 10L;
    long unhealthyLatency = 3100L; // For strategyV3, the default highLatency is 3000

    LoadBalancerStrategyTestRunner testRunner = new LoadBalancerStrategyTestRunnerBuilder(loadBalancerStrategyType.DEGRADER, null, DUMMY_SERVICE_NAME, 5)
        .addStrategyProperty(HTTP_LB_CONSISTENT_HASH_ALGORITHM, "pointBased")
        .setNumIntervals(3)
        .setConstantRequestCount(1000)
        // Set one host to be unhealthy, the the rest of the hosts to be healthy
        .setConstantLatency(Arrays.asList(
            unhealthyLatency,
            healthyLatency,
            healthyLatency,
            healthyLatency,
            healthyLatency
        ))
        .build();

    testRunner.runWait();
    Map<URI, Integer> pointsMap = testRunner.getPoints();

    //Verify the unhealthy host has 60 points, and all other hosts have 100 points
    assertEquals(pointsMap.get(testRunner.getUri(0)).intValue(), 60);
    assertEquals(pointsMap.get(testRunner.getUri(1)).intValue(), 100);
    assertEquals(pointsMap.get(testRunner.getUri(2)).intValue(), 100);
    assertEquals(pointsMap.get(testRunner.getUri(3)).intValue(), 100);
    assertEquals(pointsMap.get(testRunner.getUri(4)).intValue(), 100);
  }

  @Test
  public void testStrategyV3HealthPointsRecover() {
    LatencyQPSCorrelation unhealthyLatencyCorrelation = new LatencyQPSCorrelation() {
      @Override
      public long getLatency(int requestsPerInterval) {
        /**
         * If the weight is evenly distributed, this host will get around 200 requests per interval
         * If the request count > 180, we will use unhealthy latency
         * With this latency calculation, we may see the weight going down first and recover after getting less QPS
         * The latency may go down and up again and again
         */

        if (requestsPerInterval > 180) {
          return 2820L + requestsPerInterval;
        }
        return 10L;
      }
    };

    LatencyQPSCorrelation healthyLatencyCorrelation = new LatencyQPSCorrelation() {
      @Override
      public long getLatency(int requestsPerInterval) {
        return 10L;
      }
    };

    LoadBalancerStrategyTestRunner testRunner = new LoadBalancerStrategyTestRunnerBuilder(loadBalancerStrategyType.DEGRADER, null, DUMMY_SERVICE_NAME, 5)
        .addStrategyProperty(HTTP_LB_CONSISTENT_HASH_ALGORITHM, "pointBased")
        .setNumIntervals(100)
        .setConstantRequestCount(1000)
        // Set one host to be unhealthy, the the rest of the hosts to be healthy
        .setDynamicLatency(Arrays.asList(
            unhealthyLatencyCorrelation,
            healthyLatencyCorrelation,
            healthyLatencyCorrelation,
            healthyLatencyCorrelation,
            healthyLatencyCorrelation
        ))
        .build();

    testRunner.runWait();

    // Verify the points went down first and went up again (The pattern may repeat multiple times)
    boolean pointsDropped = false;
    boolean pointsRecovered = true;
    List<Integer> pointHistory = testRunner.getPointHistory().get(testRunner.getUri(0));
    int pre = pointHistory.get(0);
    int curr = pointHistory.get(0);
    for (Integer point : pointHistory) {
      pre = curr;
      curr = point;
      if (curr < pre) {
        pointsDropped = true;
      }
      if (pointsDropped && curr > pre) {
        pointsRecovered = true;
      }
      if (pointsDropped && pointsRecovered) {
        break;
      }
    }
    assertTrue(pointsDropped && pointsRecovered);
  }
}

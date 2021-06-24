package com.linkedin.d2.balancer.strategies.degrader;

import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.util.hashing.MPConsistentHashRing;
import com.linkedin.d2.balancer.util.healthcheck.HealthCheckOperations;
import com.linkedin.util.degrader.DegraderImpl;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;


/**
 * TestsS for DegraderLoadBalancerStrategyConfig
 *
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 */
public class DegraderLoadBalancerStrategyConfigTest
{
  @Test
  public void testCreateHttpConfigFromMap()
  {
    Map<String,Object> properties = new HashMap<>();

    long httpUpdateIntervalMs = 5231;
    boolean updateOnlyAtInterval = false;
    double httpMaxClusterLatencyWithoutDegrading = 139.6;
    double httpDefaultSuccessfulTransmissionWeight = 0.88;
    int httpPointsPerWeight = 202;
    String httpHashMethod = "sha1";
    double httpInitialRecoveryLevel = 0.06;
    double httpRingRampFactor = 1.67;
    double httpHighWaterMark = 1866.2;
    double httpLowWaterMark = 555.5;
    double httpGlobalStepUp = 0.17;
    double httpGlobalStepDown = 0.21;
    double hashRingPointCleanUpRate = 0.1;
    String consistentHashAlgo = "multiProbe";
    int numProbes = 1024;
    Map<String, Object> httpHashConfig = new HashMap<>();
    List<String> httpRegexes = new LinkedList<>();
    httpRegexes.add("httphashToken=(\\d+)");
    httpHashConfig.put("regexes", httpRegexes);

    properties.put(PropertyKeys.HTTP_LB_HASH_CONFIG, httpHashConfig);
    properties.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS,
                   httpUpdateIntervalMs);
    properties.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_ONLY_AT_INTERVAL,
                   updateOnlyAtInterval);
    properties.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_MAX_CLUSTER_LATENCY_WITHOUT_DEGRADING,
                   httpMaxClusterLatencyWithoutDegrading);
    properties.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_DEFAULT_SUCCESSFUL_TRANSMISSION_WEIGHT,
                   httpDefaultSuccessfulTransmissionWeight);
    properties.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_POINTS_PER_WEIGHT,
                   httpPointsPerWeight);
    properties.put(PropertyKeys.HTTP_LB_HASH_METHOD, httpHashMethod);
    properties.put(PropertyKeys.HTTP_LB_INITIAL_RECOVERY_LEVEL, httpInitialRecoveryLevel);
    properties.put(PropertyKeys.HTTP_LB_RING_RAMP_FACTOR, httpRingRampFactor);
    properties.put(PropertyKeys.HTTP_LB_HIGH_WATER_MARK, httpHighWaterMark);
    properties.put(PropertyKeys.HTTP_LB_LOW_WATER_MARK, httpLowWaterMark);
    properties.put(PropertyKeys.HTTP_LB_GLOBAL_STEP_DOWN, httpGlobalStepDown);
    properties.put(PropertyKeys.HTTP_LB_GLOBAL_STEP_UP, httpGlobalStepUp);
    properties.put(PropertyKeys.HTTP_LB_HASHRING_POINT_CLEANUP_RATE, hashRingPointCleanUpRate);
    properties.put(PropertyKeys.HTTP_LB_CONSISTENT_HASH_ALGORITHM, consistentHashAlgo);
    properties.put(PropertyKeys.HTTP_LB_CONSISTENT_HASH_NUM_PROBES, Integer.toString(numProbes));

    //now test if there's http, then http config should take more priority
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(properties);

    assertEquals(config.getUpdateIntervalMs(), httpUpdateIntervalMs);
    assertEquals(config.isUpdateOnlyAtInterval(), updateOnlyAtInterval);
    assertEquals(config.getPointsPerWeight(), httpPointsPerWeight);
    assertEquals(config.getHashMethod(), httpHashMethod);
    assertEquals(config.getInitialRecoveryLevel(), httpInitialRecoveryLevel);
    assertEquals(config.getRingRampFactor(), httpRingRampFactor);
    assertEquals(config.getHighWaterMark(), httpHighWaterMark);
    assertEquals(config.getLowWaterMark(), httpLowWaterMark);
    assertEquals(config.getGlobalStepDown(), httpGlobalStepDown);
    assertEquals(config.getGlobalStepUp(), httpGlobalStepUp);
    assertEquals(config.getHashConfig(), httpHashConfig);
    assertEquals(config.getHashRingPointCleanUpRate(), hashRingPointCleanUpRate);
    assertEquals(config.getConsistentHashAlgorithm(), consistentHashAlgo);
    assertEquals(config.getNumProbes(), numProbes);

    //test if there's no config, will the default config value set
    properties.clear();
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(properties);
    assertEquals(config.getUpdateIntervalMs(), DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
    assertEquals(config.isUpdateOnlyAtInterval(), DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_ONLY_AT_INTERVAL);
    assertEquals(config.getPointsPerWeight(), DegraderLoadBalancerStrategyConfig.DEFAULT_POINTS_PER_WEIGHT);
    assertNull(config.getHashMethod());
    assertEquals(config.getInitialRecoveryLevel(), DegraderLoadBalancerStrategyConfig.DEFAULT_INITIAL_RECOVERY_LEVEL);
    assertEquals(config.getRingRampFactor(), DegraderLoadBalancerStrategyConfig.DEFAULT_RAMP_FACTOR);
    assertEquals(config.getHighWaterMark(), DegraderLoadBalancerStrategyConfig.DEFAULT_HIGH_WATER_MARK);
    assertEquals(config.getLowWaterMark(), DegraderLoadBalancerStrategyConfig.DEFAULT_LOW_WATER_MARK);
    assertEquals(config.getGlobalStepDown(), DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_DOWN);
    assertEquals(config.getGlobalStepUp(), DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_UP);
    assertEquals(config.getHashConfig(), Collections.emptyMap());
    assertEquals(config.getHashRingPointCleanUpRate(), DegraderLoadBalancerStrategyConfig.DEFAULT_HASHRING_POINT_CLEANUP_RATE);
    assertEquals(config.getConsistentHashAlgorithm(), null);
    assertEquals(config.getNumProbes(), MPConsistentHashRing.DEFAULT_NUM_PROBES);
    assertEquals(config.getQuarantineLatency(), DegraderImpl.DEFAULT_LOW_LATENCY);
  }

  @Test
  public void testCreateHttpConfigFromMapWithExtraArguments()
  {
    Map<String, Object> properties = new HashMap<>();

    long httpUpdateIntervalMs = 5231;
    boolean updateOnlyAtInterval = false;
    double httpMaxClusterLatencyWithoutDegrading = 139.6;
    double httpDefaultSuccessfulTransmissionWeight = 0.88;
    int httpPointsPerWeight = 202;
    String httpHashMethod = "sha1";
    double httpInitialRecoveryLevel = 0.06;
    double httpRingRampFactor = 1.67;
    double httpHighWaterMark = 1866.2;
    double httpLowWaterMark = 555.5;
    double httpGlobalStepUp = 0.17;
    double httpGlobalStepDown = 0.21;
    double hashRingPointCleanUpRate = 0.1;
    String consistentHashAlgo = "multiProbe";
    int numProbes = 1024;
    long quarantineLatency = 50;
    Map<String, Object> httpHashConfig = new HashMap<>();
    List<String> httpRegexes = new LinkedList<>();
    httpRegexes.add("httphashToken=(\\d+)");
    httpHashConfig.put("regexes", httpRegexes);

    properties.put(PropertyKeys.HTTP_LB_HASH_CONFIG, httpHashConfig);
    properties.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS,
        httpUpdateIntervalMs);
    properties.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_ONLY_AT_INTERVAL,
        updateOnlyAtInterval);
    properties.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_MAX_CLUSTER_LATENCY_WITHOUT_DEGRADING,
        httpMaxClusterLatencyWithoutDegrading);
    properties.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_DEFAULT_SUCCESSFUL_TRANSMISSION_WEIGHT,
        httpDefaultSuccessfulTransmissionWeight);
    properties.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_POINTS_PER_WEIGHT,
        httpPointsPerWeight);
    properties.put(PropertyKeys.HTTP_LB_HASH_METHOD, httpHashMethod);
    properties.put(PropertyKeys.HTTP_LB_INITIAL_RECOVERY_LEVEL, httpInitialRecoveryLevel);
    properties.put(PropertyKeys.HTTP_LB_RING_RAMP_FACTOR, httpRingRampFactor);
    properties.put(PropertyKeys.HTTP_LB_HIGH_WATER_MARK, httpHighWaterMark);
    properties.put(PropertyKeys.HTTP_LB_LOW_WATER_MARK, httpLowWaterMark);
    properties.put(PropertyKeys.HTTP_LB_GLOBAL_STEP_DOWN, httpGlobalStepDown);
    properties.put(PropertyKeys.HTTP_LB_GLOBAL_STEP_UP, httpGlobalStepUp);
    properties.put(PropertyKeys.HTTP_LB_HASHRING_POINT_CLEANUP_RATE, hashRingPointCleanUpRate);
    properties.put(PropertyKeys.HTTP_LB_CONSISTENT_HASH_ALGORITHM, consistentHashAlgo);
    properties.put(PropertyKeys.HTTP_LB_CONSISTENT_HASH_NUM_PROBES, Integer.toString(numProbes));

    HealthCheckOperations healthCheckOperations = new HealthCheckOperations();
    Map<String, String> degraderProperties = new HashMap<>();
    degraderProperties.put(PropertyKeys.DEGRADER_LOW_LATENCY, "200");
    degraderProperties.put(PropertyKeys.DEGRADER_HIGH_LATENCY, "1000");
    degraderProperties.put(PropertyKeys.DEGRADER_MAX_DROP_RATE, "1.0");

    //now test if there's http, then http config should take more priority
    DegraderLoadBalancerStrategyConfig config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(
        properties, healthCheckOperations, null, degraderProperties, null);

    assertEquals(config.getUpdateIntervalMs(), httpUpdateIntervalMs);
    assertEquals(config.isUpdateOnlyAtInterval(), updateOnlyAtInterval);
    assertEquals(config.getPointsPerWeight(), httpPointsPerWeight);
    assertEquals(config.getHashMethod(), httpHashMethod);
    assertEquals(config.getInitialRecoveryLevel(), httpInitialRecoveryLevel);
    assertEquals(config.getRingRampFactor(), httpRingRampFactor);
    assertEquals(config.getHighWaterMark(), httpHighWaterMark);
    assertEquals(config.getLowWaterMark(), httpLowWaterMark);
    assertEquals(config.getGlobalStepDown(), httpGlobalStepDown);
    assertEquals(config.getGlobalStepUp(), httpGlobalStepUp);
    assertEquals(config.getHashConfig(), httpHashConfig);
    assertEquals(config.getHashRingPointCleanUpRate(), hashRingPointCleanUpRate);
    assertEquals(config.getConsistentHashAlgorithm(), consistentHashAlgo);
    assertEquals(config.getNumProbes(), numProbes);
    assertEquals(config.getQuarantineLatency(), 200);

    degraderProperties.put(PropertyKeys.DEGRADER_LOW_LATENCY, "1500");
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(
        properties, healthCheckOperations, null, degraderProperties, null);
    assertEquals(config.getQuarantineLatency(), DegraderLoadBalancerStrategyConfig.MAX_QUARANTINE_LATENCY);

    //test if there's no config, will the default config value set
    properties.clear();
    config = DegraderLoadBalancerStrategyConfig.createHttpConfigFromMap(properties);
    assertEquals(config.getUpdateIntervalMs(), DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_INTERVAL_MS);
    assertEquals(config.isUpdateOnlyAtInterval(), DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_ONLY_AT_INTERVAL);
    assertEquals(config.getPointsPerWeight(), DegraderLoadBalancerStrategyConfig.DEFAULT_POINTS_PER_WEIGHT);
    assertNull(config.getHashMethod());
    assertEquals(config.getInitialRecoveryLevel(), DegraderLoadBalancerStrategyConfig.DEFAULT_INITIAL_RECOVERY_LEVEL);
    assertEquals(config.getRingRampFactor(), DegraderLoadBalancerStrategyConfig.DEFAULT_RAMP_FACTOR);
    assertEquals(config.getHighWaterMark(), DegraderLoadBalancerStrategyConfig.DEFAULT_HIGH_WATER_MARK);
    assertEquals(config.getLowWaterMark(), DegraderLoadBalancerStrategyConfig.DEFAULT_LOW_WATER_MARK);
    assertEquals(config.getGlobalStepDown(), DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_DOWN);
    assertEquals(config.getGlobalStepUp(), DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_UP);
    assertEquals(config.getHashConfig(), Collections.emptyMap());
    assertEquals(config.getHashRingPointCleanUpRate(), DegraderLoadBalancerStrategyConfig.DEFAULT_HASHRING_POINT_CLEANUP_RATE);
    assertEquals(config.getConsistentHashAlgorithm(), null);
    assertEquals(config.getNumProbes(), MPConsistentHashRing.DEFAULT_NUM_PROBES);
    assertEquals(config.getQuarantineLatency(), DegraderImpl.DEFAULT_LOW_LATENCY);
  }
}

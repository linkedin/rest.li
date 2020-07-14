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

package com.linkedin.d2.balancer.config;

import com.linkedin.d2.HttpMethod;
import com.linkedin.d2.HttpStatusCodeRange;
import com.linkedin.d2.HttpStatusCodeRangeArray;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.strategies.DelegatingRingFactory;
import com.linkedin.d2.balancer.util.hashing.URIRegexHash;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.linkedin.d2.ConsistentHashAlgorithm;
import com.linkedin.d2.D2QuarantineProperties;
import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.D2RingProperties;
import com.linkedin.d2.HashConfig;
import com.linkedin.d2.HashMethod;
import com.linkedin.data.template.StringArray;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for {@link RelativeStrategyPropertiesConverter}.
 */
public class RelativeStrategyPropertiesConverterTest
{

  @Test
  public void testRelativeStrategyPropertiesConverter()
  {
    double upStep = 0.2;
    double downStep = 0.1;
    double relativeLatencyHighThresholdFactor = 1.5;
    double relativeLatencyLowThresholdFactor = 1.2;
    double highErrorRate = 0.2;
    double lowErrorRate = 0.1;
    int minCallCount = 1000;
    long updateIntervalMs = 5000;
    double initialHealthScore = 0.0;
    double slowStartThreshold = 0.32;
    HttpStatusCodeRangeArray errorStatusRange = new HttpStatusCodeRangeArray(new HttpStatusCodeRange().setLowerBound(500).setUpperBound(599));
    int emittingIntervalMs = 5000;
    double quarantineMaxPercent = 0.1;
    HttpMethod quarantineMethod = HttpMethod.OPTIONS;
    String healthCheckPath = "";
    int pointsPerWeight = 100;
    HashMethod hashMethod = HashMethod.URI_REGEX;
    StringArray regexes = new StringArray("+231{w+)");
    boolean failOnNoMatch = false;
    boolean warnOnNoMatch = true;
    double hashringPointCleanupRate = 0.2;
    ConsistentHashAlgorithm consistentHashAlgorithm = ConsistentHashAlgorithm.POINT_BASED;
    int numberOfProbes = 1024;
    int numberOfPointsPerHost = 1;
    double boundedLoadBalancingFactor = 1.5;

    D2QuarantineProperties quarantineProperties = new D2QuarantineProperties()
        .setQuarantineMaxPercent(quarantineMaxPercent)
        .setHealthCheckMethod(quarantineMethod)
        .setHealthCheckPath(healthCheckPath);

    HashConfig hashConfig = new HashConfig()
      .setFailOnNoMatch(failOnNoMatch)
      .setUriRegexes(regexes)
      .setWarnOnNoMatch(warnOnNoMatch);

    D2RingProperties ringProperties = new D2RingProperties()
      .setHashRingPointCleanupRate(hashringPointCleanupRate)
      .setBoundedLoadBalancingFactor(boundedLoadBalancingFactor)
      .setConsistentHashAlgorithm(consistentHashAlgorithm)
      .setHashConfig(hashConfig)
      .setHashMethod(hashMethod)
      .setPointsPerWeight(pointsPerWeight)
      .setNumberOfProbes(numberOfProbes)
      .setNumberOfPointsPerHost(numberOfPointsPerHost);

    D2RelativeStrategyProperties properties = new D2RelativeStrategyProperties()
      .setQuarantineProperties(quarantineProperties)
      .setRingProperties(ringProperties)
      .setUpStep(upStep)
      .setDownStep(downStep)
      .setRelativeLatencyHighThresholdFactor(relativeLatencyHighThresholdFactor)
      .setRelativeLatencyLowThresholdFactor(relativeLatencyLowThresholdFactor)
      .setHighErrorRate(highErrorRate)
      .setLowErrorRate(lowErrorRate)
      .setMinCallCount(minCallCount)
      .setUpdateIntervalMs(updateIntervalMs)
      .setInitialHealthScore(initialHealthScore)
      .setSlowStartThreshold(slowStartThreshold)
      .setErrorStatusFilter(errorStatusRange)
      .setEmittingIntervalMs(emittingIntervalMs);

    Map<String, Object> propertyMap = new HashMap<>();
    Map<String, Object> ringPropertyMap = new HashMap<>();
    Map<String, Object> quarantinePropertyMap = new HashMap<>();
    Map<String, Object> hashConfigMap = new HashMap<>();
    Map<String, String> errorStatusRangeMap = new HashMap<>();

    quarantinePropertyMap.put(PropertyKeys.QUARANTINE_MAX_PERCENT, String.valueOf(quarantineMaxPercent));
    quarantinePropertyMap.put(PropertyKeys.QUARANTINE_HEALTH_CHECK_METHOD, quarantineMethod.toString());
    quarantinePropertyMap.put(PropertyKeys.QUARANTINE_HEALTH_CHECK_PATH, healthCheckPath);

    hashConfigMap.put(URIRegexHash.KEY_REGEXES, new ArrayList<>(regexes));
    hashConfigMap.put(URIRegexHash.KEY_WARN_ON_NO_MATCH, String.valueOf(warnOnNoMatch));
    hashConfigMap.put(URIRegexHash.KEY_FAIL_ON_NO_MATCH, String.valueOf(failOnNoMatch));

    ringPropertyMap.put(PropertyKeys.RING_HASH_RING_POINT_CLEANUP_RATE, String.valueOf(hashringPointCleanupRate));
    ringPropertyMap.put(PropertyKeys.RING_BOUNDED_LOAD_BALANCING_FACTOR, String.valueOf(boundedLoadBalancingFactor));
    ringPropertyMap.put(PropertyKeys.RING_CONSISTENT_HASH_ALGORITHM, DelegatingRingFactory.POINT_BASED_CONSISTENT_HASH);
    ringPropertyMap.put(PropertyKeys.RING_HASH_CONFIG, hashConfigMap);
    ringPropertyMap.put(PropertyKeys.RING_HASH_METHOD, hashMethod.toString());
    ringPropertyMap.put(PropertyKeys.RING_POINTS_PER_WEIGHT, String.valueOf(pointsPerWeight));
    ringPropertyMap.put(PropertyKeys.RING_NUMBER_OF_PROBES, String.valueOf(numberOfProbes));
    ringPropertyMap.put(PropertyKeys.RING_NUMBER_OF_POINTS_PER_HOST, String.valueOf(numberOfPointsPerHost));

    errorStatusRangeMap.put(PropertyKeys.ERROR_STATUS_UPPER_BOUND, String.valueOf(errorStatusRange.get(0).getUpperBound()));
    errorStatusRangeMap.put(PropertyKeys.ERROR_STATUS_LOWER_BOUND, String.valueOf(errorStatusRange.get(0).getLowerBound()));

    propertyMap.put(PropertyKeys.QUARANTINE_PROPERTIES, quarantinePropertyMap);
    propertyMap.put(PropertyKeys.RING_PROPERTIES, ringPropertyMap);
    propertyMap.put(PropertyKeys.UP_STEP, String.valueOf(upStep));
    propertyMap.put(PropertyKeys.DOWN_STEP, String.valueOf(downStep));
    propertyMap.put(PropertyKeys.RELATIVE_LATENCY_HIGH_THRESHOLD_FACTOR, String.valueOf(relativeLatencyHighThresholdFactor));
    propertyMap.put(PropertyKeys.RELATIVE_LATENCY_LOW_THRESHOLD_FACTOR, String.valueOf(relativeLatencyLowThresholdFactor));
    propertyMap.put(PropertyKeys.HIGH_ERROR_RATE, String.valueOf(highErrorRate));
    propertyMap.put(PropertyKeys.LOW_ERROR_RATE, String.valueOf(lowErrorRate));
    propertyMap.put(PropertyKeys.MIN_CALL_COUNT, String.valueOf(minCallCount));
    propertyMap.put(PropertyKeys.UPDATE_INTERVAL_MS, String.valueOf(updateIntervalMs));
    propertyMap.put(PropertyKeys.INITIAL_HEALTH_SCORE, String.valueOf(initialHealthScore));
    propertyMap.put(PropertyKeys.SLOW_START_THRESHOLD, String.valueOf(slowStartThreshold));
    propertyMap.put(PropertyKeys.ERROR_STATUS_FILTER, Arrays.asList(errorStatusRangeMap));
    propertyMap.put(PropertyKeys.EMITTING_INTERVAL_MS, String.valueOf(emittingIntervalMs));

    Assert.assertEquals(RelativeStrategyPropertiesConverter.toMap(properties), propertyMap);
    Assert.assertEquals(RelativeStrategyPropertiesConverter.toProperties(propertyMap), properties);
  }
}

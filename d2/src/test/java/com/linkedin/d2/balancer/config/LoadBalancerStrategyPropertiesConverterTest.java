/*
   Copyright (c) 2016 LinkedIn Corp.

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

import com.linkedin.d2.ConsistentHashAlgorithmEnum;
import com.linkedin.d2.D2LoadBalancerStrategyProperties;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.strategies.DelegatingRingFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;
import com.linkedin.d2.balancer.util.hashing.URIRegexHash;
import com.linkedin.d2.hashConfigType;
import com.linkedin.d2.hashMethodEnum;
import com.linkedin.d2.quarantineInfo;
import com.linkedin.data.template.StringArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Ang Xu
 */
public class LoadBalancerStrategyPropertiesConverterTest
{
  @Test
  public void testLoadBalancerStrategyPropertiesConverter()
  {
    final Double globalStepDown = 0.4;
    final Double globalStepUp = 0.3;
    final Double recoveryLevel = 1.0;
    final Double ringRampFactor = 0.01;
    final Double highWaterMark = 1000d;
    final Double lowWaterMark = 500d;
    final Integer pointsPerWeight = 100;
    final Long updateIntervalMs = 50000l;
    final Integer minCallCountHighWaterMark = 3000;
    final Integer minCallCountLowWaterMark = 1500;
    final hashMethodEnum hashMethod = hashMethodEnum.URI_REGEX;
    final hashConfigType hashConfig = new hashConfigType();
    final StringArray regexes = new StringArray("+231{w+)");
    final Double hashringPointCleanupRate = 0.2;
    final ConsistentHashAlgorithmEnum consistentHashAlgorithm = ConsistentHashAlgorithmEnum.MULTI_PROBE;
    final Integer numProbes = 1024;
    final Integer numPointsPerHost = 1;
    final Double quarantineMaxPercent = 0.2;
    final String quarantineMethod = "OPTIONS:/test/path";
    final quarantineInfo quarantineInfo = new quarantineInfo()
        .setQuarantineMaxPercent(quarantineMaxPercent)
        .setQuarantineMethod(quarantineMethod);
    final String errorStatusRegex = "(5..)";
    final Integer lowEmittingInterval = 10;
    final Integer highEmittingInterval = 60;

    hashConfig.setUriRegexes(regexes);
    hashConfig.setWarnOnNoMatch(false);
    hashConfig.setFailOnNoMatch(true);

    Map<String, Object> loadBalancerStrategyProperties = new HashMap<>();
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_GLOBAL_STEP_DOWN, globalStepDown.toString());
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_GLOBAL_STEP_UP, globalStepUp.toString());
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_INITIAL_RECOVERY_LEVEL, recoveryLevel.toString());
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_RING_RAMP_FACTOR, ringRampFactor.toString());
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_HIGH_WATER_MARK, highWaterMark.toString());
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_LOW_WATER_MARK, lowWaterMark.toString());
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_POINTS_PER_WEIGHT, pointsPerWeight.toString());
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS, updateIntervalMs.toString());
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK, minCallCountHighWaterMark.toString());
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK, minCallCountLowWaterMark.toString());
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_HASH_METHOD, DegraderLoadBalancerStrategyV3.HASH_METHOD_URI_REGEX);
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_HASHRING_POINT_CLEANUP_RATE, hashringPointCleanupRate.toString());
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_CONSISTENT_HASH_ALGORITHM, DelegatingRingFactory.MULTI_PROBE_CONSISTENT_HASH);
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_CONSISTENT_HASH_NUM_PROBES, numProbes.toString());
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_CONSISTENT_HASH_POINTS_PER_HOST, numPointsPerHost.toString());

    Map<String, Object> hashConfigMap = new HashMap<>();
    hashConfigMap.put(URIRegexHash.KEY_REGEXES, new ArrayList<>(regexes));
    hashConfigMap.put(URIRegexHash.KEY_WARN_ON_NO_MATCH, "false");
    hashConfigMap.put(URIRegexHash.KEY_FAIL_ON_NO_MATCH, "true");
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_HASH_CONFIG, hashConfigMap);
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_QUARANTINE_MAX_PERCENT, quarantineMaxPercent.toString());
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_QUARANTINE_METHOD, quarantineMethod);
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_ERROR_STATUS_REGEX, errorStatusRegex);
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_LOW_EVENT_EMITTING_INTERVAL, lowEmittingInterval.toString());
    loadBalancerStrategyProperties.put(PropertyKeys.HTTP_LB_HIGH_EVENT_EMITTING_INTERVAL, highEmittingInterval.toString());

    D2LoadBalancerStrategyProperties d2LoadBalancerStrategyProperties =
        new D2LoadBalancerStrategyProperties()
            .setGlobalStepDown(globalStepDown)
            .setGlobalStepUp(globalStepUp)
            .setInitialRecoveryLevel(recoveryLevel)
            .setRingRampFactor(ringRampFactor)
            .setHighWaterMark(highWaterMark)
            .setLowWaterMark(lowWaterMark)
            .setPointsPerWeight(pointsPerWeight)
            .setUpdateIntervalMs(updateIntervalMs)
            .setMinCallCountHighWaterMark(minCallCountHighWaterMark)
            .setMinCallCountLowWaterMark(minCallCountLowWaterMark)
            .setHashMethod(hashMethod)
            .setHashConfig(hashConfig)
            .setHashRingPointCleanupRate(hashringPointCleanupRate)
            .setConsistentHashAlgorithm(consistentHashAlgorithm)
            .setNumberOfProbes(numProbes)
            .setNumberOfPointsPerHost(numPointsPerHost)
            .setQuarantineCfg(quarantineInfo)
            .setErrorStatusRegex(errorStatusRegex)
            .setLowEmittingInterval(lowEmittingInterval)
            .setHighEmittingInterval(highEmittingInterval);

    Assert.assertEquals(LoadBalancerStrategyPropertiesConverter.toConfig(loadBalancerStrategyProperties), d2LoadBalancerStrategyProperties);
    Assert.assertEquals(LoadBalancerStrategyPropertiesConverter.toProperties(d2LoadBalancerStrategyProperties), loadBalancerStrategyProperties);
  }
}

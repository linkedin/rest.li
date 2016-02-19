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

import com.linkedin.d2.D2LoadBalancerStrategyProperties;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;
import com.linkedin.d2.balancer.util.hashing.URIRegexHash;
import com.linkedin.d2.hashConfigType;
import com.linkedin.d2.hashMethodEnum;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.StringMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.linkedin.d2.balancer.properties.util.PropertyUtil.coerce;

/**
 * This class converts {@link D2LoadBalancerStrategyProperties} into
 * a map from String to Object that can be stored in zookeeper and vice versa.
 * @author Ang Xu
 */
public class LoadBalancerStrategyPropertiesConverter
{
  public static Map<String, Object> toProperties(D2LoadBalancerStrategyProperties config)
  {
    if (config == null)
    {
      return Collections.emptyMap();
    }

    Map<String, Object> map = new HashMap<>();
    if (config.hasGlobalStepDown())
    {
      map.put(PropertyKeys.HTTP_LB_GLOBAL_STEP_DOWN, config.getGlobalStepDown().toString());
    }
    if (config.hasGlobalStepUp())
    {
      map.put(PropertyKeys.HTTP_LB_GLOBAL_STEP_UP, config.getGlobalStepUp().toString());
    }
    if (config.hasInitialRecoveryLevel())
    {
      map.put(PropertyKeys.HTTP_LB_INITIAL_RECOVERY_LEVEL, config.getInitialRecoveryLevel().toString());
    }
    if (config.hasRingRampFactor())
    {
      map.put(PropertyKeys.HTTP_LB_RING_RAMP_FACTOR, config.getRingRampFactor().toString());
    }
    if (config.hasHighWaterMark())
    {
      map.put(PropertyKeys.HTTP_LB_HIGH_WATER_MARK, config.getHighWaterMark().toString());
    }
    if (config.hasLowWaterMark())
    {
      map.put(PropertyKeys.HTTP_LB_LOW_WATER_MARK, config.getLowWaterMark().toString());
    }
    if (config.hasPointsPerWeight())
    {
      map.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_POINTS_PER_WEIGHT, config.getPointsPerWeight().toString());
    }
    if (config.hasUpdateIntervalMs())
    {
      map.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS, config.getUpdateIntervalMs().toString());
    }
    if (config.hasMinCallCountHighWaterMark())
    {
      map.put(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK, config.getMinCallCountHighWaterMark().toString());
    }
    if (config.hasHashRingPointCleanupRate())
    {
      map.put(PropertyKeys.HTTP_LB_HASHRING_POINT_CLEANUP_RATE, config.getHashRingPointCleanupRate().toString());
    }
    if (config.hasMinCallCountLowWaterMark())
    {
      map.put(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK, config.getMinCallCountLowWaterMark().toString());
    }
    if (config.hasHashMethod())
    {
      switch (config.getHashMethod())
      {
        case RANDOM:
          map.put(PropertyKeys.HTTP_LB_HASH_METHOD, DegraderLoadBalancerStrategyV3.HASH_METHOD_NONE);
          break;
        case URI_REGEX:
          map.put(PropertyKeys.HTTP_LB_HASH_METHOD, DegraderLoadBalancerStrategyV3.HASH_METHOD_URI_REGEX);
          break;
        default:
          // default to random hash method.
          map.put(PropertyKeys.HTTP_LB_HASH_METHOD, DegraderLoadBalancerStrategyV3.HASH_METHOD_NONE);
      }
    }
    if (config.hasHashConfig())
    {
      hashConfigType hashConfig = config.getHashConfig();
      Map<String, Object> hashConfigProperties = new HashMap<>();
      if (hashConfig.hasUriRegexes())
      {
        hashConfigProperties.put(URIRegexHash.KEY_REGEXES, hashConfig.getUriRegexes().stream().collect(Collectors.toList()));
      }
      map.put(PropertyKeys.HTTP_LB_HASH_CONFIG, hashConfigProperties);
    }
    if (config.hasUpdateOnlyAtInterval())
    {
      map.put(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_ONLY_AT_INTERVAL, config.isUpdateOnlyAtInterval().toString());
    }
    return map;
  }

  @SuppressWarnings({"unchecked"})
  public static D2LoadBalancerStrategyProperties toConfig(Map<String, Object> properties)
  {
    D2LoadBalancerStrategyProperties config = new D2LoadBalancerStrategyProperties();
    if (properties.containsKey(PropertyKeys.HTTP_LB_GLOBAL_STEP_DOWN))
    {
      config.setGlobalStepDown(coerce(properties.get(PropertyKeys.HTTP_LB_GLOBAL_STEP_DOWN), Double.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_LB_GLOBAL_STEP_UP))
    {
      config.setGlobalStepUp(coerce(properties.get(PropertyKeys.HTTP_LB_GLOBAL_STEP_UP), Double.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_LB_INITIAL_RECOVERY_LEVEL))
    {
      config.setInitialRecoveryLevel(coerce(properties.get(PropertyKeys.HTTP_LB_INITIAL_RECOVERY_LEVEL), Double.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_LB_RING_RAMP_FACTOR))
    {
      config.setRingRampFactor(coerce(properties.get(PropertyKeys.HTTP_LB_RING_RAMP_FACTOR), Double.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_LB_HIGH_WATER_MARK))
    {
      config.setHighWaterMark(coerce(properties.get(PropertyKeys.HTTP_LB_HIGH_WATER_MARK), Double.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_LB_LOW_WATER_MARK))
    {
      config.setLowWaterMark(coerce(properties.get(PropertyKeys.HTTP_LB_LOW_WATER_MARK), Double.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_POINTS_PER_WEIGHT))
    {
      config.setPointsPerWeight(
          coerce(properties.get(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_POINTS_PER_WEIGHT), Integer.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS))
    {
      config.setUpdateIntervalMs(
          coerce(properties.get(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_INTERVAL_MS), Long.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK))
    {
      config.setMinCallCountHighWaterMark(
          coerce(properties.get(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK), Long.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK))
    {
      config.setMinCallCountLowWaterMark(
          coerce(properties.get(PropertyKeys.HTTP_LB_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK), Long.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_LB_HASHRING_POINT_CLEANUP_RATE))
    {
      config.setHashRingPointCleanupRate(
          coerce(properties.get(PropertyKeys.HTTP_LB_HASHRING_POINT_CLEANUP_RATE), Double.class));
    }
    if (properties.containsKey(PropertyKeys.HTTP_LB_HASH_METHOD))
    {
      String hashMethodString = coerce(properties.get(PropertyKeys.HTTP_LB_HASH_METHOD), String.class);
      if (DegraderLoadBalancerStrategyV3.HASH_METHOD_NONE.equalsIgnoreCase(hashMethodString))
      {
        config.setHashMethod(hashMethodEnum.RANDOM);
      }
      else if (DegraderLoadBalancerStrategyV3.HASH_METHOD_URI_REGEX.equalsIgnoreCase(hashMethodString))
      {
        config.setHashMethod(hashMethodEnum.URI_REGEX);
      }
    }
    if (properties.containsKey(PropertyKeys.HTTP_LB_HASH_CONFIG)) {
      hashConfigType hashConfig = new hashConfigType();
      Map<String, Object> hashConfigProperties = (Map<String, Object>)properties.get(PropertyKeys.HTTP_LB_HASH_CONFIG);
      if (hashConfigProperties.containsKey(URIRegexHash.KEY_REGEXES))
      {
        List<String> uriRegexes = (List<String>)hashConfigProperties.remove(URIRegexHash.KEY_REGEXES);
        hashConfig.setUriRegexes(new StringArray(uriRegexes));
      }
      config.setHashConfig(hashConfig);
    }
    if (properties.containsKey(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_ONLY_AT_INTERVAL))
    {
      config.setUpdateOnlyAtInterval(
          coerce(properties.get(PropertyKeys.HTTP_LB_STRATEGY_PROPERTIES_UPDATE_ONLY_AT_INTERVAL),
              Boolean.class));
    }
    return config;
  }
}

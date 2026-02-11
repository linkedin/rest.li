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

package com.linkedin.d2.balancer.config;

import com.linkedin.d2.ClientType;
import com.linkedin.d2.ClientTypeArray;
import com.linkedin.d2.LeastRequestLbConfig;
import com.linkedin.d2.LoadBalancingPolicy;
import com.linkedin.d2.LoadBalancingPolicyArray;
import com.linkedin.d2.LoadBalancingPolicyConfig;
import com.linkedin.d2.RingHashLbConfig;
import com.linkedin.d2.RoundRobinLbConfig;
import com.linkedin.d2.SlowStartProperties;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.util.PropertyUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Converts between {@link LoadBalancingPolicyArray} (Pegasus) and
 * {@code List<Map<String, Object>>} used by {@link com.linkedin.d2.balancer.properties.ClusterProperties}.
 * Mirrors the pattern of {@link DarkClustersConverter}.
 */
public final class LoadBalancingPoliciesConverter
{
  private LoadBalancingPoliciesConverter() { }

  /**
   * Converts Pegasus load balancing policies to a list of maps suitable for storage/serialization.
   */
  @SuppressWarnings("unchecked")
  public static List<Map<String, Object>> toProperties(@Nullable LoadBalancingPolicyArray policies)
  {
    if (policies == null || policies.isEmpty())
    {
      return Collections.emptyList();
    }
    List<Map<String, Object>> result = new ArrayList<>(policies.size());
    for (LoadBalancingPolicy policy : policies)
    {
      Map<String, Object> prop = new HashMap<>();
      prop.put("config", configToProperties(policy.getConfig()));
      if (policy.hasClientTypes())
      {
        List<String> clientTypes = new ArrayList<>();
        for (ClientType ct : policy.getClientTypes())
        {
          clientTypes.add(ct.name());
        }
        prop.put("clientTypes", clientTypes);
      }
      result.add(prop);
    }
    return result;
  }

  /**
   * Converts the list-of-maps representation to Pegasus {@link LoadBalancingPolicyArray}.
   */
  @SuppressWarnings("unchecked")
  public static LoadBalancingPolicyArray toConfig(@Nullable List<Map<String, Object>> properties)
  {
    if (properties == null || properties.isEmpty())
    {
      return new LoadBalancingPolicyArray();
    }
    LoadBalancingPolicyArray result = new LoadBalancingPolicyArray();
    for (Map<String, Object> prop : properties)
    {
      LoadBalancingPolicy policy = new LoadBalancingPolicy();
      Object configObj = prop.get("config");
      if (configObj != null && configObj instanceof Map)
      {
        policy.setConfig(configToConfig((Map<String, Object>) configObj));
      }
      Object clientTypesObj = prop.get("clientTypes");
      if (clientTypesObj != null && clientTypesObj instanceof List)
      {
        List<String> clientTypesList = (List<String>) clientTypesObj;
        ClientTypeArray clientTypeArray = new ClientTypeArray();
        for (String s : clientTypesList)
        {
          clientTypeArray.add(ClientType.valueOf(s.toUpperCase()));
        }
        policy.setClientTypes(clientTypeArray);
      }
      result.add(policy);
    }
    return result;
  }

  // --- config (LoadBalancingPolicyConfig union) to/from map ---

  private static Map<String, Object> configToProperties(LoadBalancingPolicyConfig config)
  {
    Map<String, Object> result = new HashMap<>();
    if (config.isRingHashLbConfig())
    {
      result.put("ringHashLbConfig", ringHashLbConfigToProperties(config.getRingHashLbConfig()));
    }
    else if (config.isLeastRequestLbConfig())
    {
      result.put("leastRequestLbConfig", leastRequestLbConfigToProperties(config.getLeastRequestLbConfig()));
    }
    else if (config.isRoundRobinLbConfig())
    {
      result.put("roundRobinLbConfig", roundRobinLbConfigToProperties(config.getRoundRobinLbConfig()));
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static LoadBalancingPolicyConfig configToConfig(Map<String, Object> map)
  {
    if (map.containsKey("ringHashLbConfig") && map.get("ringHashLbConfig") != null)
    {
      return LoadBalancingPolicyConfig.createWithRingHashLbConfig(
          ringHashLbConfigFromProperties((Map<String, Object>) map.get("ringHashLbConfig")));
    }
    if (map.containsKey("leastRequestLbConfig") && map.get("leastRequestLbConfig") != null)
    {
      return LoadBalancingPolicyConfig.createWithLeastRequestLbConfig(
          leastRequestLbConfigFromProperties((Map<String, Object>) map.get("leastRequestLbConfig")));
    }
    if (map.containsKey("roundRobinLbConfig") && map.get("roundRobinLbConfig") != null)
    {
      return LoadBalancingPolicyConfig.createWithRoundRobinLbConfig(
          roundRobinLbConfigFromProperties((Map<String, Object>) map.get("roundRobinLbConfig")));
    }
    throw new IllegalArgumentException("LoadBalancingPolicyConfig map must contain one of: ringHashLbConfig, leastRequestLbConfig, roundRobinLbConfig");
  }

  // --- RingHashLbConfig (Pegasus preserves field names: getMinimum_ring_size, etc.) ---

  private static Map<String, Object> ringHashLbConfigToProperties(RingHashLbConfig config)
  {
    Map<String, Object> map = new HashMap<>();
    if (config.hasMinimum_ring_size())
    {
      map.put("minimum_ring_size", config.getMinimum_ring_size());
    }
    if (config.hasMaximum_ring_size())
    {
      map.put("maximum_ring_size", config.getMaximum_ring_size());
    }
    if (config.hasHash_function())
    {
      map.put("hash_function", config.getHash_function().name());
    }
    return map;
  }

  private static RingHashLbConfig ringHashLbConfigFromProperties(Map<String, Object> map)
  {
    RingHashLbConfig config = new RingHashLbConfig();
    if (map.containsKey("minimum_ring_size"))
    {
      config.setMinimum_ring_size(PropertyUtil.coerce(map.get("minimum_ring_size"), Integer.class));
    }
    if (map.containsKey("maximum_ring_size"))
    {
      config.setMaximum_ring_size(PropertyUtil.coerce(map.get("maximum_ring_size"), Integer.class));
    }
    if (map.containsKey("hash_function"))
    {
      config.setHash_function(com.linkedin.d2.HashFunction.valueOf(
          String.valueOf(map.get("hash_function")).toUpperCase()));
    }
    return config;
  }

  // --- LeastRequestLbConfig (Pegasus preserves field names) ---

  private static Map<String, Object> leastRequestLbConfigToProperties(LeastRequestLbConfig config)
  {
    Map<String, Object> map = new HashMap<>();
    if (config.hasChoice_count())
    {
      map.put("choice_count", config.getChoice_count());
    }
    if (config.hasActive_request_bias())
    {
      map.put("active_request_bias", config.getActive_request_bias());
    }
    if (config.hasSlow_start_config())
    {
      map.put("slow_start_config", slowStartPropertiesToMap(config.getSlow_start_config()));
    }
    return map;
  }

  @SuppressWarnings("unchecked")
  private static LeastRequestLbConfig leastRequestLbConfigFromProperties(Map<String, Object> map)
  {
    LeastRequestLbConfig config = new LeastRequestLbConfig();
    if (map.containsKey("choice_count"))
    {
      config.setChoice_count(PropertyUtil.coerce(map.get("choice_count"), Integer.class));
    }
    if (map.containsKey("active_request_bias"))
    {
      config.setActive_request_bias(PropertyUtil.coerce(map.get("active_request_bias"), Double.class));
    }
    if (map.containsKey("slow_start_config") && map.get("slow_start_config") != null)
    {
      config.setSlow_start_config(mapToSlowStartProperties((Map<String, Object>) map.get("slow_start_config")));
    }
    return config;
  }

  // --- RoundRobinLbConfig (Pegasus preserves field names) ---

  private static Map<String, Object> roundRobinLbConfigToProperties(RoundRobinLbConfig config)
  {
    Map<String, Object> map = new HashMap<>();
    if (config.hasSlow_start_config())
    {
      map.put("slow_start_config", slowStartPropertiesToMap(config.getSlow_start_config()));
    }
    return map;
  }

  @SuppressWarnings("unchecked")
  private static RoundRobinLbConfig roundRobinLbConfigFromProperties(Map<String, Object> map)
  {
    RoundRobinLbConfig config = new RoundRobinLbConfig();
    if (map.containsKey("slow_start_config") && map.get("slow_start_config") != null)
    {
      config.setSlow_start_config(mapToSlowStartProperties((Map<String, Object>) map.get("slow_start_config")));
    }
    return config;
  }

  // --- SlowStartProperties (Pegasus) <-> Map (matches PropertyKeys for cluster SlowStartProperties) ---

  private static Map<String, Object> slowStartPropertiesToMap(SlowStartProperties props)
  {
    Map<String, Object> map = new HashMap<>();
    map.put(PropertyKeys.SLOW_START_DISABLED, props.isDisabled());
    map.put(PropertyKeys.SLOW_START_WINDOW_DURATION, props.getWindowDurationSeconds());
    map.put(PropertyKeys.SLOW_START_AGGRESSION, props.getAggression());
    map.put(PropertyKeys.SLOW_START_MIN_WEIGHT_PERCENT, props.getMinWeightPercent());
    return map;
  }

  private static SlowStartProperties mapToSlowStartProperties(Map<String, Object> map)
  {
    SlowStartProperties props = new SlowStartProperties();
    if (map.containsKey(PropertyKeys.SLOW_START_DISABLED))
    {
      props.setDisabled(PropertyUtil.coerce(map.get(PropertyKeys.SLOW_START_DISABLED), Boolean.class));
    }
    if (map.containsKey(PropertyKeys.SLOW_START_WINDOW_DURATION))
    {
      props.setWindowDurationSeconds(PropertyUtil.coerce(map.get(PropertyKeys.SLOW_START_WINDOW_DURATION), Integer.class));
    }
    if (map.containsKey(PropertyKeys.SLOW_START_AGGRESSION))
    {
      props.setAggression(PropertyUtil.coerce(map.get(PropertyKeys.SLOW_START_AGGRESSION), Double.class));
    }
    if (map.containsKey(PropertyKeys.SLOW_START_MIN_WEIGHT_PERCENT))
    {
      props.setMinWeightPercent(PropertyUtil.coerce(map.get(PropertyKeys.SLOW_START_MIN_WEIGHT_PERCENT), Double.class));
    }
    return props;
  }
}

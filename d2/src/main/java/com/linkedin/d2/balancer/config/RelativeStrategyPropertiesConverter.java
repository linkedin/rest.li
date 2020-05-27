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

import com.linkedin.d2.ConsistentHashAlgorithm;
import com.linkedin.d2.D2QuarantineProperties;
import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.D2RingProperties;
import com.linkedin.d2.HashConfig;
import com.linkedin.d2.HashMethod;
import com.linkedin.d2.HttpMethod;
import com.linkedin.d2.HttpStatusCodeRange;
import com.linkedin.d2.HttpStatusCodeRangeArray;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.strategies.degrader.DelegatingRingFactory;
import com.linkedin.d2.balancer.util.hashing.URIRegexHash;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.template.StringArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.linkedin.d2.balancer.properties.util.PropertyUtil.coerce;


/**
 * Converter for {@link com.linkedin.d2.D2RelativeStrategyProperties}.
 */
public class RelativeStrategyPropertiesConverter
{
  private static final JacksonDataCodec CODEC = new JacksonDataCodec();
  private static final ValidationOptions VALIDATION_OPTIONS =
    new ValidationOptions(RequiredMode.FIXUP_ABSENT_WITH_DEFAULT, CoercionMode.STRING_TO_PRIMITIVE);

  /**
   * Convert {@link D2RelativeStrategyProperties} to Map
   *
   * @param properties relative strategy properties
   * @return The converted key-value map
   */
  public static Map<String, Object> toMap(D2RelativeStrategyProperties properties)
  {
    if (properties == null)
    {
      return Collections.emptyMap();
    }

    Map<String, Object> map = new HashMap<>();
    if (properties.hasUpStep())
    {
      map.put(PropertyKeys.UP_STEP, properties.getUpStep().toString());
    }
    if (properties.hasDownStep())
    {
      map.put(PropertyKeys.DOWN_STEP, properties.getDownStep().toString());
    }
    if (properties.hasRelativeLatencyHighThresholdFactor())
    {
      map.put(PropertyKeys.RELATIVE_LATENCY_HIGH_THRESHOLD_FACTOR, properties.getRelativeLatencyHighThresholdFactor().toString());
    }
    if (properties.hasRelativeLatencyLowThresholdFactor())
    {
      map.put(PropertyKeys.RELATIVE_LATENCY_LOW_THRESHOLD_FACTOR, properties.getRelativeLatencyLowThresholdFactor().toString());
    }
    if (properties.hasHighErrorRate())
    {
      map.put(PropertyKeys.HIGH_ERROR_RATE, properties.getHighErrorRate().toString());
    }
    if (properties.hasLowErrorRate())
    {
      map.put(PropertyKeys.LOW_ERROR_RATE, properties.getLowErrorRate().toString());
    }
    if (properties.hasMinCallCount())
    {
      map.put(PropertyKeys.MIN_CALL_COUNT, properties.getMinCallCount().toString());
    }
    if (properties.hasUpdateIntervalMs())
    {
      map.put(PropertyKeys.UPDATE_INTERVAL_MS, properties.getUpdateIntervalMs().toString());
    }
    if (properties.hasInitialHealthScore())
    {
      map.put(PropertyKeys.INITIAL_HEALTH_SCORE, properties.getInitialHealthScore().toString());
    }
    if (properties.hasSlowStartThreshold())
    {
      map.put(PropertyKeys.SLOW_START_THRESHOLD, properties.getSlowStartThreshold().toString());
    }
    if (properties.hasEmittingIntervalMs())
    {
      map.put(PropertyKeys.EMITTING_INTERVAL_MS, properties.getEmittingIntervalMs().toString());
    }
    if (properties.hasEnableFastRecovery())
    {
      map.put(PropertyKeys.ENABLE_FAST_RECOVERY, properties.isEnableFastRecovery().toString());
    }
    if (properties.hasErrorStatusFilter())
    {
      List<Map<String, Object>> errorStatusFilterList = new ArrayList<>();
      for (HttpStatusCodeRange errorStatusRange : properties.getErrorStatusFilter())
      {
        Map<String, Object> errorStatusFilterMap = new HashMap<>();
        errorStatusFilterMap.put(PropertyKeys.ERROR_STATUS_LOWER_BOUND, errorStatusRange.getLowerBound().toString());
        errorStatusFilterMap.put(PropertyKeys.ERROR_STATUS_UPPER_BOUND, errorStatusRange.getUpperBound().toString());
        errorStatusFilterList.add(errorStatusFilterMap);
      }
      map.put(PropertyKeys.ERROR_STATUS_FILTER, errorStatusFilterList);
    }

    if (properties.hasQuarantineProperties())
    {
      D2QuarantineProperties quarantineProperties = properties.getQuarantineProperties();
      Map<String, Object> quarantinePropertyMap = toQuarantinePropertyMap(quarantineProperties);
      map.put(PropertyKeys.QUARANTINE_PROPERTIES, quarantinePropertyMap);
    }

    if (properties.hasRingProperties())
    {
      D2RingProperties ringProperties = properties.getRingProperties();
      Map<String, Object> ringPropertyMap = toRingPropertyMap(ringProperties);
      map.put(PropertyKeys.RING_PROPERTIES, ringPropertyMap);
    }
    return map;
  }

  /**
   * Convert from map to {@link D2RelativeStrategyProperties}
   *
   * @param properties key-value map that defines the relative load balancer related properties
   * @return The converted {@link D2RelativeStrategyProperties}
   */
  @SuppressWarnings({"unchecked"})
  public static D2RelativeStrategyProperties toProperties(Map<String, Object> properties)
  {
    D2RelativeStrategyProperties config = new D2RelativeStrategyProperties();

    if (properties.containsKey(PropertyKeys.UP_STEP))
    {
      config.setUpStep(coerce(properties.get(PropertyKeys.UP_STEP), Double.class));
    }
    if (properties.containsKey(PropertyKeys.DOWN_STEP))
    {
      config.setDownStep(coerce(properties.get(PropertyKeys.DOWN_STEP), Double.class));
    }
    if (properties.containsKey(PropertyKeys.RELATIVE_LATENCY_HIGH_THRESHOLD_FACTOR))
    {
      config.setRelativeLatencyHighThresholdFactor(coerce(properties.get(PropertyKeys.RELATIVE_LATENCY_HIGH_THRESHOLD_FACTOR), Double.class));
    }
    if (properties.containsKey(PropertyKeys.RELATIVE_LATENCY_LOW_THRESHOLD_FACTOR))
    {
      config.setRelativeLatencyLowThresholdFactor(coerce(properties.get(PropertyKeys.RELATIVE_LATENCY_LOW_THRESHOLD_FACTOR), Double.class));
    }
    if (properties.containsKey(PropertyKeys.HIGH_ERROR_RATE))
    {
      config.setHighErrorRate(coerce(properties.get(PropertyKeys.HIGH_ERROR_RATE), Double.class));
    }
    if (properties.containsKey(PropertyKeys.LOW_ERROR_RATE))
    {
      config.setLowErrorRate(coerce(properties.get(PropertyKeys.LOW_ERROR_RATE), Double.class));
    }
    if (properties.containsKey(PropertyKeys.MIN_CALL_COUNT))
    {
      config.setMinCallCount(coerce(properties.get(PropertyKeys.MIN_CALL_COUNT), Integer.class));
    }
    if (properties.containsKey(PropertyKeys.UPDATE_INTERVAL_MS))
    {
      config.setUpdateIntervalMs(coerce(properties.get(PropertyKeys.UPDATE_INTERVAL_MS), Long.class));
    }
    if (properties.containsKey(PropertyKeys.INITIAL_HEALTH_SCORE))
    {
      config.setInitialHealthScore(coerce(properties.get(PropertyKeys.INITIAL_HEALTH_SCORE), Double.class));
    }
    if (properties.containsKey(PropertyKeys.SLOW_START_THRESHOLD))
    {
      config.setSlowStartThreshold(coerce(properties.get(PropertyKeys.SLOW_START_THRESHOLD), Double.class));
    }
    if (properties.containsKey(PropertyKeys.EMITTING_INTERVAL_MS))
    {
      config.setEmittingIntervalMs(coerce(properties.get(PropertyKeys.EMITTING_INTERVAL_MS), Long.class));
    }
    if (properties.containsKey(PropertyKeys.ENABLE_FAST_RECOVERY))
    {
      config.setEnableFastRecovery(coerce(properties.get(PropertyKeys.ENABLE_FAST_RECOVERY), Boolean.class));
    }
    if (properties.containsKey(PropertyKeys.ERROR_STATUS_FILTER))
    {
      HttpStatusCodeRangeArray array = new HttpStatusCodeRangeArray();
      List<Map<String, Object>> errorStatusFilterList = (List<Map<String, Object>>) properties.get(PropertyKeys.ERROR_STATUS_FILTER);
      for (Map<String, Object> errorStatusRange : errorStatusFilterList)
      {
        HttpStatusCodeRange httpStatusCodeRange = new HttpStatusCodeRange()
            .setUpperBound(coerce(errorStatusRange.get(PropertyKeys.ERROR_STATUS_UPPER_BOUND), Integer.class))
            .setLowerBound(coerce(errorStatusRange.get(PropertyKeys.ERROR_STATUS_LOWER_BOUND), Integer.class));
        array.add(httpStatusCodeRange);
      }
      config.setErrorStatusFilter(array);
    }

    if (properties.containsKey(PropertyKeys.QUARANTINE_PROPERTIES))
    {
      config.setQuarantineProperties(toQuarantineProperties((Map<String, Object>) properties.get(PropertyKeys.QUARANTINE_PROPERTIES)));
    }
    if (properties.containsKey(PropertyKeys.RING_PROPERTIES))
    {
      config.setRingProperties(toRingProperties((Map<String, Object>) properties.get(PropertyKeys.RING_PROPERTIES)));
    }
    return config;
  }

  private static Map<String, Object> toQuarantinePropertyMap(D2QuarantineProperties quarantineProperties)
  {
    Map<String, Object> quarantinePropertyMap = new HashMap<>();
    if (quarantineProperties.hasQuarantineMaxPercent())
    {
      quarantinePropertyMap.put(PropertyKeys.QUARANTINE_MAX_PERCENT, quarantineProperties.getQuarantineMaxPercent().toString());
    }
    if (quarantineProperties.hasHealthCheckMethod())
    {
      quarantinePropertyMap.put(PropertyKeys.QUARANTINE_HEALTH_CHECK_METHOD, quarantineProperties.getHealthCheckMethod().toString());
    }
    if (quarantineProperties.hasHealthCheckPath())
    {
      quarantinePropertyMap.put(PropertyKeys.QUARANTINE_HEALTH_CHECK_PATH, quarantineProperties.getHealthCheckPath());
    }

    return quarantinePropertyMap;
  }

  private static D2QuarantineProperties toQuarantineProperties(Map<String, Object> quarantinePropertyMap)
  {
    D2QuarantineProperties quarantineProperties = new D2QuarantineProperties();
    if (quarantinePropertyMap.containsKey(PropertyKeys.QUARANTINE_MAX_PERCENT))
    {
      quarantineProperties.setQuarantineMaxPercent(coerce(quarantinePropertyMap.get(PropertyKeys.QUARANTINE_MAX_PERCENT), Double.class));
    }
    if (quarantinePropertyMap.containsKey(PropertyKeys.QUARANTINE_HEALTH_CHECK_METHOD))
    {
      String httpMethod = (String) quarantinePropertyMap.get(PropertyKeys.QUARANTINE_HEALTH_CHECK_METHOD);
      if (HttpMethod.OPTIONS.name().equalsIgnoreCase(httpMethod))
      {
        quarantineProperties.setHealthCheckMethod(HttpMethod.OPTIONS);
      }
      else if (HttpMethod.GET.name().equalsIgnoreCase(httpMethod))
      {
        quarantineProperties.setHealthCheckMethod(HttpMethod.GET);
      }
    }
    if (quarantinePropertyMap.containsKey(PropertyKeys.QUARANTINE_HEALTH_CHECK_PATH))
    {
      quarantineProperties.setHealthCheckPath(coerce(quarantinePropertyMap.get(PropertyKeys.QUARANTINE_HEALTH_CHECK_PATH), String.class));
    }

    return quarantineProperties;
  }

  private static Map<String, Object> toRingPropertyMap(D2RingProperties ringProperties)
  {
    Map<String, Object> ringPropertyMap = new HashMap<>();
    if (ringProperties.hasPointsPerWeight())
    {
      ringPropertyMap.put(PropertyKeys.RING_POINTS_PER_WEIGHT, ringProperties.getPointsPerWeight().toString());
    }
    if (ringProperties.hasHashMethod())
    {
      ringPropertyMap.put(PropertyKeys.RING_HASH_METHOD, ringProperties.getHashMethod().toString());
    }
    if (ringProperties.hasHashConfig())
    {
      Map<String, Object> hashConfigMap = toHashConfigMap(ringProperties.getHashConfig());
      ringPropertyMap.put(PropertyKeys.RING_HASH_CONFIG, hashConfigMap);
    }
    if (ringProperties.hasHashRingPointCleanupRate())
    {
      ringPropertyMap.put(PropertyKeys.RING_HASH_RING_POINT_CLEANUP_RATE, ringProperties.getHashRingPointCleanupRate().toString());
    }
    if (ringProperties.hasConsistentHashAlgorithm())
    {
      switch (ringProperties.getConsistentHashAlgorithm())
      {
        case MULTI_PROBE:
          ringPropertyMap.put(PropertyKeys.RING_CONSISTENT_HASH_ALGORITHM, DelegatingRingFactory.MULTI_PROBE_CONSISTENT_HASH);
          break;
        case POINT_BASED:
          ringPropertyMap.put(PropertyKeys.RING_CONSISTENT_HASH_ALGORITHM, DelegatingRingFactory.POINT_BASED_CONSISTENT_HASH);
          break;
        case DISTRIBUTION_BASED:
          ringPropertyMap.put(PropertyKeys.RING_CONSISTENT_HASH_ALGORITHM, DelegatingRingFactory.DISTRIBUTION_NON_HASH);
      }
    }
    if (ringProperties.hasNumberOfProbes())
    {
      ringPropertyMap.put(PropertyKeys.RING_NUMBER_OF_PROBES, ringProperties.getNumberOfProbes().toString());
    }
    if (ringProperties.hasNumberOfPointsPerHost())
    {
      ringPropertyMap.put(PropertyKeys.RING_NUMBER_OF_POINTS_PER_HOST, ringProperties.getNumberOfPointsPerHost().toString());
    }
    if (ringProperties.hasBoundedLoadBalancingFactor())
    {
      ringPropertyMap.put(PropertyKeys.RING_BOUNDED_LOAD_BALANCING_FACTOR, ringProperties.getBoundedLoadBalancingFactor().toString());
    }

    return ringPropertyMap;
  }

  @SuppressWarnings({"unchecked"})
  private static D2RingProperties toRingProperties(Map<String, Object> ringPropertyMap)
  {
    D2RingProperties ringProperties = new D2RingProperties();
    if (ringPropertyMap.containsKey(PropertyKeys.RING_POINTS_PER_WEIGHT))
    {
      ringProperties.setPointsPerWeight(coerce(ringPropertyMap.get(PropertyKeys.RING_POINTS_PER_WEIGHT), Integer.class));
    }
    if (ringPropertyMap.containsKey(PropertyKeys.RING_HASH_METHOD))
    {
      String hashMethod = (String) ringPropertyMap.get(PropertyKeys.RING_HASH_METHOD);
      if (HashMethod.URI_REGEX.name().equalsIgnoreCase(hashMethod))
      {
        ringProperties.setHashMethod(HashMethod.URI_REGEX);
      }
      else if (HashMethod.RANDOM.name().equalsIgnoreCase(hashMethod))
      {
        ringProperties.setHashMethod(HashMethod.RANDOM);
      }
    }
    if (ringPropertyMap.containsKey(PropertyKeys.RING_HASH_CONFIG))
    {
      HashConfig hashConfig = toHashConfig((Map<String, Object>) ringPropertyMap.get(PropertyKeys.RING_HASH_CONFIG));
      ringProperties.setHashConfig(hashConfig);
    }
    if (ringPropertyMap.containsKey(PropertyKeys.RING_HASH_RING_POINT_CLEANUP_RATE))
    {
      ringProperties.setHashRingPointCleanupRate(coerce(ringPropertyMap.get(PropertyKeys.RING_HASH_RING_POINT_CLEANUP_RATE), Double.class));
    }
    if (ringPropertyMap.containsKey(PropertyKeys.RING_CONSISTENT_HASH_ALGORITHM))
    {
      String consistentHashAlgorithm = (String) ringPropertyMap.get(PropertyKeys.RING_CONSISTENT_HASH_ALGORITHM);
      if (DelegatingRingFactory.POINT_BASED_CONSISTENT_HASH.equalsIgnoreCase(consistentHashAlgorithm))
      {
        ringProperties.setConsistentHashAlgorithm(ConsistentHashAlgorithm.POINT_BASED);
      }
      else if (DelegatingRingFactory.MULTI_PROBE_CONSISTENT_HASH.equalsIgnoreCase(consistentHashAlgorithm))
      {
        ringProperties.setConsistentHashAlgorithm(ConsistentHashAlgorithm.MULTI_PROBE);
      }
      else if (DelegatingRingFactory.DISTRIBUTION_NON_HASH.equalsIgnoreCase(consistentHashAlgorithm))
      {
        ringProperties.setConsistentHashAlgorithm(ConsistentHashAlgorithm.DISTRIBUTION_BASED);
      }
    }
    if (ringPropertyMap.containsKey(PropertyKeys.RING_NUMBER_OF_PROBES))
    {
      ringProperties.setNumberOfProbes(coerce(ringPropertyMap.get(PropertyKeys.RING_NUMBER_OF_PROBES), Integer.class));
    }
    if (ringPropertyMap.containsKey(PropertyKeys.RING_NUMBER_OF_POINTS_PER_HOST))
    {
      ringProperties.setNumberOfPointsPerHost(coerce(ringPropertyMap.get(PropertyKeys.RING_NUMBER_OF_POINTS_PER_HOST), Integer.class));
    }
    if (ringPropertyMap.containsKey(PropertyKeys.RING_BOUNDED_LOAD_BALANCING_FACTOR))
    {
      ringProperties.setBoundedLoadBalancingFactor(coerce(ringPropertyMap.get(PropertyKeys.RING_BOUNDED_LOAD_BALANCING_FACTOR), Double.class));
    }

    return ringProperties;
  }

  /**
   * Convert from {@link HashConfig} to a map of property name and value
   *
   * @param hashConfig The hash config of a hash ring
   * @return The converted map
   */
  public static Map<String, Object> toHashConfigMap(HashConfig hashConfig)
  {
    Map<String, Object> hashConfigMap = new HashMap<>();
    if (hashConfig.hasUriRegexes())
    {
      hashConfigMap.put(URIRegexHash.KEY_REGEXES, hashConfig.getUriRegexes().stream().collect(Collectors.toList()));
    }
    if (hashConfig.hasFailOnNoMatch()) {
      hashConfigMap.put(URIRegexHash.KEY_FAIL_ON_NO_MATCH, hashConfig.isFailOnNoMatch().toString());
    }
    if (hashConfig.hasWarnOnNoMatch()) {
      hashConfigMap.put(URIRegexHash.KEY_WARN_ON_NO_MATCH, hashConfig.isWarnOnNoMatch().toString());
    }
    return hashConfigMap;
  }

  @SuppressWarnings({"unchecked"})
  private static HashConfig toHashConfig(Map<String, Object> hashConfigMap)
  {
    HashConfig hashConfig = new HashConfig();
    if (hashConfigMap.containsKey(URIRegexHash.KEY_REGEXES))
    {
      List<String> uriRegexes = (List<String>) hashConfigMap.get(URIRegexHash.KEY_REGEXES);
      hashConfig.setUriRegexes(new StringArray(uriRegexes));
    }
    if (hashConfigMap.containsKey(URIRegexHash.KEY_WARN_ON_NO_MATCH))
    {
      String warnOnNoMatchString = (String) hashConfigMap.get(URIRegexHash.KEY_WARN_ON_NO_MATCH);
      hashConfig.setWarnOnNoMatch(Boolean.parseBoolean(warnOnNoMatchString));
    }
    if (hashConfigMap.containsKey(URIRegexHash.KEY_FAIL_ON_NO_MATCH))
    {
      String failOnNoMatchString = (String) hashConfigMap.get(URIRegexHash.KEY_FAIL_ON_NO_MATCH);
      hashConfig.setFailOnNoMatch(Boolean.parseBoolean(failOnNoMatchString));
    }
    return hashConfig;
  }
}

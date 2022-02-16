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

package com.linkedin.d2.balancer.properties;

import com.linkedin.d2.balancer.properties.util.PropertyUtil;
import com.linkedin.d2.balancer.util.JacksonUtil;
import com.linkedin.d2.discovery.PropertyBuilder;
import com.linkedin.d2.discovery.PropertySerializationException;
import com.linkedin.d2.discovery.PropertySerializer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.linkedin.d2.balancer.properties.util.PropertyUtil.mapGet;


public class ClusterPropertiesJsonSerializer implements
    PropertySerializer<ClusterProperties>, PropertyBuilder<ClusterProperties>
{
  private static final Logger _log = LoggerFactory.getLogger(ClusterPropertiesJsonSerializer.class);

  @Override
  public byte[] toBytes(ClusterProperties property)
  {
    try
    {
      return JacksonUtil.getObjectMapper().writeValueAsString(property).getBytes("UTF-8");
    }
    catch (Exception e)
    {
      _log.error("Failed to write property to bytes: ", e);
    }

    return null;
  }

  @Override
  public ClusterProperties fromBytes(byte[] bytes) throws PropertySerializationException
  {
    try
    {
      @SuppressWarnings("unchecked")
      Map<String, Object> untyped =
          JacksonUtil.getObjectMapper().readValue(new String(bytes, "UTF-8"), HashMap.class);
      return fromMap(untyped);

    }
    catch (Exception e)
    {
      throw new PropertySerializationException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T mapGetOrDefault(Map<String, Object> map, String key, T defaultValue)
  {
    T value = (T) map.get(key);
    if (value == null)
    {
      value = defaultValue;
    }
    return value;
  }

  /**
   * Always return the composite class {@link ClusterStoreProperties} to include ALL properties stored on service registry (like Zookeeper),
   * such as canary configs, distribution strategy, etc.
   */
  @Override
  public ClusterProperties fromMap(Map<String, Object> map) {
    ClusterProperties stableConfigs = buildClusterPropertiesFromMap(map);
    ClusterProperties canaryConfigs = null;
    CanaryDistributionStrategy distributionStrategy = null;
    // get canary properties and canary distribution strategy, if exist
    Map<String, Object> canaryConfigsMap = mapGet(map, PropertyKeys.CANARY_CONFIGS);
    Map<String, Object> distributionStrategyMap = mapGet(map, PropertyKeys.CANARY_DISTRIBUTION_STRATEGY);
    if (canaryConfigsMap != null && !canaryConfigsMap.isEmpty()
        && distributionStrategyMap != null && !distributionStrategyMap.isEmpty())
    {
      canaryConfigs = buildClusterPropertiesFromMap(canaryConfigsMap);
      distributionStrategy = new CanaryDistributionStrategy(
        mapGetOrDefault(distributionStrategyMap, PropertyKeys.CANARY_STRATEGY, CanaryDistributionStrategy.DEFAULT_STRATEGY_LABEL),
        mapGetOrDefault(distributionStrategyMap, PropertyKeys.PERCENTAGE_STRATEGY_PROPERTIES, Collections.emptyMap()),
        mapGetOrDefault(distributionStrategyMap, PropertyKeys.TARGET_HOSTS_STRATEGY_PROPERTIES, Collections.emptyMap()),
        mapGetOrDefault(distributionStrategyMap, PropertyKeys.TARGET_APPLICATIONS_STRATEGY_PROPERTIES, Collections.emptyMap()));
    }
    return new ClusterStoreProperties(stableConfigs, canaryConfigs, distributionStrategy);
  }

  /**
   * Build cluster configs from map. This could be for either stable or canary configs.
   */
  private ClusterProperties buildClusterPropertiesFromMap(Map<String, Object> map)
  {
    List<String> bannedList = mapGet(map, PropertyKeys.BANNED_URIS);
    Set<URI> banned = (bannedList == null) ? Collections.emptySet()
        : bannedList.stream().map(URI::create).collect(Collectors.toSet());

    String clusterName = PropertyUtil.checkAndGetValue(map, PropertyKeys.CLUSTER_NAME, String.class, "ClusterProperties");
    List<String> prioritizedSchemes = mapGet(map, PropertyKeys.PRIORITIZED_SCHEMES);
    Map<String, String> properties = mapGet(map, "properties");
    Map<String, Object> partitionPropertiesMap = mapGet(map, PropertyKeys.PARTITION_PROPERTIES);
    PartitionProperties partitionProperties;
    String scope = "cluster: " + clusterName;
    List<String> validationList = mapGet(map, PropertyKeys.SSL_VALIDATION_STRINGS);
    if (partitionPropertiesMap != null)
    {
      PartitionProperties.PartitionType partitionType =
          PropertyUtil.checkAndGetValue(partitionPropertiesMap, PropertyKeys.PARTITION_TYPE, PartitionProperties.PartitionType.class, scope);
      switch (partitionType)
      {
        case RANGE:
        {
          long keyRangeStart =
              PropertyUtil.checkAndGetValue(partitionPropertiesMap, PropertyKeys.KEY_RANGE_START, Number.class, scope).longValue();
          long partitionSize =
              PropertyUtil.checkAndGetValue(partitionPropertiesMap, PropertyKeys.PARTITION_SIZE, Number.class, scope).longValue();
          int partitionCount =
              PropertyUtil.checkAndGetValue(partitionPropertiesMap, PropertyKeys.PARTITION_COUNT, Number.class, scope).intValue();
          String partitionKeyRegex =
              PropertyUtil.checkAndGetValue(partitionPropertiesMap, PropertyKeys.PARTITION_KEY_REGEX, String.class, scope);
          partitionProperties =
              new RangeBasedPartitionProperties(partitionKeyRegex, keyRangeStart, partitionSize, partitionCount);

          break;
        }
        case HASH:
        {
          int partitionCount =
              PropertyUtil.checkAndGetValue(partitionPropertiesMap, PropertyKeys.PARTITION_COUNT, Number.class, scope).intValue();
          String partitionKeyRegex =
              PropertyUtil.checkAndGetValue(partitionPropertiesMap, PropertyKeys.PARTITION_KEY_REGEX, String.class, scope);
          HashBasedPartitionProperties.HashAlgorithm algorithm =
              PropertyUtil.checkAndGetValue(partitionPropertiesMap, PropertyKeys.HASH_ALGORITHM, HashBasedPartitionProperties.HashAlgorithm.class, scope);
          partitionProperties =
              new HashBasedPartitionProperties(partitionKeyRegex, partitionCount, algorithm);
          break;
        }
        case CUSTOM:
        {
          int partitionCount = partitionPropertiesMap.containsKey(PropertyKeys.PARTITION_COUNT)
              ? PropertyUtil.checkAndGetValue(partitionPropertiesMap, PropertyKeys.PARTITION_COUNT, Number.class, scope).intValue()
              : 0;

          @SuppressWarnings("unchecked")
          List<String> partitionAccessorList =partitionPropertiesMap.containsKey(PropertyKeys.PARTITION_ACCESSOR_LIST)
              ? PropertyUtil.checkAndGetValue(partitionPropertiesMap, PropertyKeys.PARTITION_ACCESSOR_LIST, List.class, scope)
              : Collections.emptyList();
          partitionProperties = new CustomizedPartitionProperties(partitionCount, partitionAccessorList);
          break;
        }
        case NONE:
          partitionProperties = NullPartitionProperties.getInstance();
          break;
        default:
          throw new IllegalArgumentException("In " + scope + ": Unsupported partitionType: " + partitionType);
      }
    }
    else
    {
      partitionProperties = NullPartitionProperties.getInstance();
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> darkClusterProperty = (Map<String, Object>) map.get(PropertyKeys.DARK_CLUSTER_MAP);

    boolean delegated = false;
    if (map.containsKey(PropertyKeys.DELEGATED)) {
      delegated = mapGet(map, PropertyKeys.DELEGATED);
    }
    return new ClusterProperties(clusterName, prioritizedSchemes, properties, banned, partitionProperties, validationList,
        darkClusterProperty, delegated);
  }
}

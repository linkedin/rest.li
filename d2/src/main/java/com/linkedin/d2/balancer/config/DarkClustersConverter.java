/*
   Copyright (c) 2019 LinkedIn Corp.

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

import com.linkedin.d2.D2TransportClientProperties;
import com.linkedin.d2.DarkClusterConfig;
import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.DarkClusterStrategyName;
import com.linkedin.d2.DarkClusterStrategyNameArray;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.balancer.properties.util.PropertyUtil;
import com.linkedin.data.DataList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.linkedin.d2.balancer.properties.ClusterProperties.DARK_CLUSTER_DEFAULT_MULTIPLIER;

/**
 * This class converts {@link DarkClusterConfigMap} into a Map
 * that can be stored in zookeeper and vice versa.
 *
 * @author David Hoa (dhoa@linkedin.com)
 */
public class DarkClustersConverter
{
  @SuppressWarnings("unchecked")
  public static Map<String, Object> toProperties(DarkClusterConfigMap config)
  {
    if (config == null)
    {
      return Collections.emptyMap();
    }
    else
    {
      Map<String, Object> darkProps = new HashMap<>();
      for (Map.Entry<String,DarkClusterConfig> entry : config.entrySet())
      {
        String darkClusterName = entry.getKey();
        DarkClusterConfig darkClusterConfig = entry.getValue();
        Map<String, Object> prop = new HashMap<>();
        if (darkClusterConfig.hasMultiplier())
        {
          prop.put(PropertyKeys.DARK_CLUSTER_MULTIPLIER, darkClusterConfig.getMultiplier().toString());
        }

        if (darkClusterConfig.hasDispatcherOutboundTargetRate())
        {
          prop.put(PropertyKeys.DARK_CLUSTER_OUTBOUND_TARGET_RATE,
                   darkClusterConfig.getDispatcherOutboundTargetRate().toString());
        }

        if (darkClusterConfig.hasDispatcherMaxRequestsToBuffer())
        {
          prop.put(PropertyKeys.DARK_CLUSTER_MAX_REQUESTS_TO_BUFFER,
                   darkClusterConfig.getDispatcherMaxRequestsToBuffer());
        }

        if (darkClusterConfig.hasDispatcherBufferedRequestExpiryInSeconds())
        {
          prop.put(PropertyKeys.DARK_CLUSTER_BUFFERED_REQUEST_EXPIRY_IN_SECONDS,
                   darkClusterConfig.getDispatcherBufferedRequestExpiryInSeconds());
        }

        if (darkClusterConfig.hasDarkClusterStrategyPrioritizedList())
        {
          DarkClusterStrategyNameArray strategyNameArray = darkClusterConfig.getDarkClusterStrategyPrioritizedList();
          List<String> strategyList = new ArrayList<>();
          for (DarkClusterStrategyName type : strategyNameArray)
          {
            strategyList.add(type.toString());
          }
          prop.put(PropertyKeys.DARK_CLUSTER_STRATEGY_LIST, strategyList);
        }

        if (darkClusterConfig.hasTransportClientProperties())
        {
          prop.put(PropertyKeys.DARK_CLUSTER_TRANSPORT_CLIENT_PROPERTIES,
                   TransportClientPropertiesConverter.toProperties(darkClusterConfig.getTransportClientProperties()));
        }
        darkProps.put(darkClusterName, prop);
      }
      return darkProps;
    }
  }

  public static DarkClusterConfigMap toConfig(Map<String, Object> properties)
  {
    DarkClusterConfigMap configMap = new DarkClusterConfigMap();
    for (Map.Entry<String, Object> entry : properties.entrySet())
    {
      String darkClusterName = entry.getKey();
      DarkClusterConfig darkClusterConfig = new DarkClusterConfig();
      @SuppressWarnings("unchecked")
      Map<String, Object> props = (Map<String, Object>) entry.getValue();
      if (props.containsKey(PropertyKeys.DARK_CLUSTER_MULTIPLIER))
      {
        darkClusterConfig.setMultiplier(PropertyUtil.coerce(props.get(PropertyKeys.DARK_CLUSTER_MULTIPLIER), Float.class));
      }
      else
      {
        // to maintain backwards compatibility with previously ser/de, set the default on deserialization
        darkClusterConfig.setMultiplier(DARK_CLUSTER_DEFAULT_MULTIPLIER);
      }

      if (props.containsKey(PropertyKeys.DARK_CLUSTER_STRATEGY_LIST))
      {
        DataList dataList = new DataList();
        @SuppressWarnings("unchecked")
        List<String> strategyList = (List<String>)props.get(PropertyKeys.DARK_CLUSTER_STRATEGY_LIST);
        dataList.addAll(strategyList);

        DarkClusterStrategyNameArray darkClusterStrategyNameArray = new DarkClusterStrategyNameArray(dataList);
        darkClusterConfig.setDarkClusterStrategyPrioritizedList(darkClusterStrategyNameArray);
      }

      if (props.containsKey(PropertyKeys.DARK_CLUSTER_TRANSPORT_CLIENT_PROPERTIES))
      {
        @SuppressWarnings("unchecked")
        D2TransportClientProperties transportClientProperties = TransportClientPropertiesConverter.toConfig(
          (Map<String, Object>)props.get(PropertyKeys.DARK_CLUSTER_TRANSPORT_CLIENT_PROPERTIES));
        darkClusterConfig.setTransportClientProperties(transportClientProperties);
      }
      configMap.put(darkClusterName, darkClusterConfig);
    }
    return configMap;
  }
}

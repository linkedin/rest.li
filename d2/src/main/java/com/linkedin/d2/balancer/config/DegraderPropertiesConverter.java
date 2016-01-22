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

import com.linkedin.d2.D2DegraderProperties;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.d2.latencyType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.linkedin.d2.balancer.properties.util.PropertyUtil.coerce;


/**
 * This class converts {@link D2DegraderProperties} into a map from String to String
 * and vice versa.
 * @author Ang Xu
 */
public class DegraderPropertiesConverter
{
  public static Map<String, String> toProperties(D2DegraderProperties config)
  {
    if (config == null)
    {
      return Collections.emptyMap();
    }

    Map<String, String> map = new HashMap<>();
    if (config.hasLogEnabled())
    {
      map.put(PropertyKeys.DEGRADER_LOG_ENABLED, config.isLogEnabled().toString());
    }
    if (config.hasMaxDropRate())
    {
      map.put(PropertyKeys.DEGRADER_MAX_DROP_RATE, config.getMaxDropRate().toString());
    }
    if (config.hasUpStep())
    {
      map.put(PropertyKeys.DEGRADER_UP_STEP, config.getUpStep().toString());
    }
    if (config.hasDownStep())
    {
      map.put(PropertyKeys.DEGRADER_DOWN_STEP, config.getDownStep().toString());
    }
    if (config.hasMinCallCount())
    {
      map.put(PropertyKeys.DEGRADER_MIN_CALL_COUNT, config.getMinCallCount().toString());
    }
    if (config.hasHighLatency())
    {
      map.put(PropertyKeys.DEGRADER_HIGH_LATENCY, config.getHighLatency().toString());
    }
    if (config.hasLowLatency())
    {
      map.put(PropertyKeys.DEGRADER_LOW_LATENCY, config.getLowLatency().toString());
    }
    if (config.hasHighErrorRate())
    {
      map.put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, config.getHighErrorRate().toString());
    }
    if (config.hasLowErrorRate())
    {
      map.put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, config.getLowErrorRate().toString());
    }
    if (config.hasHighOutstanding())
    {
      map.put(PropertyKeys.DEGRADER_HIGH_OUTSTANDING, config.getHighOutstanding().toString());
    }
    if (config.hasLowOutstanding())
    {
      map.put(PropertyKeys.DEGRADER_LOW_OUTSTANDING, config.getLowOutstanding().toString());
    }
    if (config.hasMinOutstandingCount())
    {
      map.put(PropertyKeys.DEGRADER_MIN_OUTSTANDING_COUNT, config.getMinOutstandingCount().toString());
    }
    if (config.hasMaxDropDuration())
    {
      map.put(PropertyKeys.DEGRADER_MAX_DROP_DURATION, config.getMaxDropDuration().toString());
    }
    if (config.hasLatencyToUse())
    {
      map.put(PropertyKeys.DEGRADER_LATENCY_TO_USE, config.getLatencyToUse().name());
    }
    return map;
  }

  public static D2DegraderProperties toConfig(Map<String, String> properties)
  {
    D2DegraderProperties config = new D2DegraderProperties();

    if (properties.containsKey(PropertyKeys.DEGRADER_LOG_ENABLED))
    {
      config.setLogEnabled(coerce(properties.get(PropertyKeys.DEGRADER_LOG_ENABLED), Boolean.class));
    }
    if (properties.containsKey(PropertyKeys.DEGRADER_MAX_DROP_RATE))
    {
      config.setMaxDropRate(coerce(properties.get(PropertyKeys.DEGRADER_MAX_DROP_RATE), Double.class));
    }
    if (properties.containsKey(PropertyKeys.DEGRADER_UP_STEP))
    {
      config.setUpStep(coerce(properties.get(PropertyKeys.DEGRADER_UP_STEP), Double.class));
    }
    if (properties.containsKey(PropertyKeys.DEGRADER_DOWN_STEP))
    {
      config.setDownStep(coerce(properties.get(PropertyKeys.DEGRADER_DOWN_STEP), Double.class));
    }
    if (properties.containsKey(PropertyKeys.DEGRADER_MIN_CALL_COUNT))
    {
      config.setMinCallCount(coerce(properties.get(PropertyKeys.DEGRADER_MIN_CALL_COUNT), Integer.class));
    }
    if (properties.containsKey(PropertyKeys.DEGRADER_HIGH_LATENCY))
    {
      config.setHighLatency(coerce(properties.get(PropertyKeys.DEGRADER_HIGH_LATENCY), Integer.class));
    }
    if (properties.containsKey(PropertyKeys.DEGRADER_LOW_LATENCY))
    {
      config.setLowLatency(coerce(properties.get(PropertyKeys.DEGRADER_LOW_LATENCY), Integer.class));
    }
    if (properties.containsKey(PropertyKeys.DEGRADER_HIGH_ERROR_RATE))
    {
      config.setHighErrorRate(coerce(properties.get(PropertyKeys.DEGRADER_HIGH_ERROR_RATE), Double.class));
    }
    if (properties.containsKey(PropertyKeys.DEGRADER_LOW_ERROR_RATE))
    {
      config.setLowErrorRate(coerce(properties.get(PropertyKeys.DEGRADER_LOW_ERROR_RATE), Double.class));
    }
    if (properties.containsKey(PropertyKeys.DEGRADER_HIGH_OUTSTANDING))
    {
      config.setHighOutstanding(coerce(properties.get(PropertyKeys.DEGRADER_HIGH_OUTSTANDING), Integer.class));
    }
    if (properties.containsKey(PropertyKeys.DEGRADER_LOW_OUTSTANDING))
    {
      config.setLowOutstanding(coerce(properties.get(PropertyKeys.DEGRADER_LOW_OUTSTANDING), Integer.class));
    }
    if (properties.containsKey(PropertyKeys.DEGRADER_MIN_OUTSTANDING_COUNT))
    {
      config.setMinOutstandingCount(coerce(properties.get(PropertyKeys.DEGRADER_MIN_OUTSTANDING_COUNT),
          Integer.class));
    }
    if (properties.containsKey(PropertyKeys.DEGRADER_MAX_DROP_DURATION))
    {
      config.setMaxDropDuration(coerce(properties.get(PropertyKeys.DEGRADER_MAX_DROP_DURATION), Long.class));
    }
    if (properties.containsKey(PropertyKeys.DEGRADER_LATENCY_TO_USE))
    {
      config.setLatencyToUse(latencyType.valueOf(properties.get(PropertyKeys.DEGRADER_LATENCY_TO_USE)));
    }
    return config;
  }
}

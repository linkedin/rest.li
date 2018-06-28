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

package com.linkedin.d2.balancer.strategies.degrader;


import com.linkedin.common.util.MapUtil;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.util.degrader.DegraderImpl;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.discovery.util.LogUtil.warn;


/**
 * Factory for Translating Degrader Properties Map to Degrader Config
 *
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 */
public class DegraderConfigFactory
{
  private static final Logger _log = LoggerFactory.getLogger(DegraderConfigFactory.class);

  public static DegraderImpl.Config toDegraderConfig(Map<String, String> properties)
  {
    DegraderImpl.Config config = new DegraderImpl.Config();
    if (properties == null || properties.isEmpty())
    {
      return config;
    }
    else
    {
      // we are not modifying config's callTracker, name, clock and overrideDropRate because
      // that will be set during the construction of TrackerClient
      config.setLogEnabled(MapUtil.getWithDefault(properties,
                                                  PropertyKeys.DEGRADER_LOG_ENABLED,
                                                  DegraderImpl.DEFAULT_LOG_ENABLED));

      config.setLogThreshold(MapUtil.getWithDefault(properties, PropertyKeys.DEGRADER_LOG_THRESHOLD,
          DegraderImpl.DEFAULT_LOG_THRESHOLD));

      if (properties.get(PropertyKeys.DEGRADER_LATENCY_TO_USE) != null)
      {
        try
        {
          config.setLatencyToUse(DegraderImpl.LatencyToUse.valueOf(properties.get(PropertyKeys.
                                                                                      DEGRADER_LATENCY_TO_USE)));
        }
        catch (IllegalArgumentException e)
        {
          warn(_log, "Received an illegal enum for latencyToUse in the cluster properties. The enum is " +
              properties.get(PropertyKeys.DEGRADER_LATENCY_TO_USE), e);
          config.setLatencyToUse(DegraderImpl.DEFAULT_LATENCY_TO_USE);
        }
      }

      config.setMaxDropRate(MapUtil.getWithDefault(properties, PropertyKeys.DEGRADER_MAX_DROP_RATE,
                                                   DegraderImpl.DEFAULT_MAX_DROP_RATE));

      config.setMaxDropDuration(MapUtil.getWithDefault(properties,
                                                       PropertyKeys.DEGRADER_MAX_DROP_DURATION,
                                                       DegraderImpl.DEFAULT_MAX_DROP_DURATION ));

      config.setUpStep(MapUtil.getWithDefault(properties, PropertyKeys.DEGRADER_UP_STEP,
                                              DegraderImpl.DEFAULT_UP_STEP));

      config.setDownStep(MapUtil.getWithDefault(properties, PropertyKeys.DEGRADER_DOWN_STEP,
                                                DegraderImpl.DEFAULT_DOWN_STEP));

      config.setMinCallCount(MapUtil.getWithDefault(properties,
                                                    PropertyKeys.DEGRADER_MIN_CALL_COUNT,
                                                    DegraderImpl.DEFAULT_MIN_CALL_COUNT));

      config.setHighLatency(MapUtil.getWithDefault(properties,
                                                   PropertyKeys.DEGRADER_HIGH_LATENCY,
                                                   DegraderImpl.DEFAULT_HIGH_LATENCY));

      config.setLowLatency(MapUtil.getWithDefault(properties,
                                                  PropertyKeys.DEGRADER_LOW_LATENCY,
                                                  DegraderImpl.DEFAULT_LOW_LATENCY));

      config.setHighErrorRate(MapUtil.getWithDefault(properties,
                                                     PropertyKeys.DEGRADER_HIGH_ERROR_RATE,
                                                     DegraderImpl.DEFAULT_HIGH_ERROR_RATE));

      config.setLowErrorRate(MapUtil.getWithDefault(properties,
                                                    PropertyKeys.DEGRADER_LOW_ERROR_RATE,
                                                    DegraderImpl.DEFAULT_LOW_ERROR_RATE));

      config.setHighOutstanding(MapUtil.getWithDefault(properties,
                                                       PropertyKeys.DEGRADER_HIGH_OUTSTANDING,
                                                       DegraderImpl.DEFAULT_HIGH_OUTSTANDING));

      config.setLowOutstanding(MapUtil.getWithDefault(properties,
                                                      PropertyKeys.DEGRADER_LOW_OUTSTANDING,
                                                      DegraderImpl.DEFAULT_LOW_OUTSTANDING));

      config.setMinOutstandingCount(MapUtil.getWithDefault(properties,
                                                           PropertyKeys.DEGRADER_MIN_OUTSTANDING_COUNT,
                                                           DegraderImpl.DEFAULT_MIN_OUTSTANDING_COUNT));

      config.setOverrideMinCallCount(MapUtil.getWithDefault(properties,
                                                            PropertyKeys.DEGRADER_OVERRIDE_MIN_CALL_COUNT,
                                                            DegraderImpl.DEFAULT_OVERRIDE_MIN_CALL_COUNT));

      config.setInitialDropRate(MapUtil.getWithDefault(properties,
                                                       PropertyKeys.DEGRADER_INITIAL_DROP_RATE,
                                                       DegraderImpl.DEFAULT_INITIAL_DROP_RATE));

      config.setSlowStartThreshold(MapUtil.getWithDefault(properties,
                                                          PropertyKeys.DEGRADER_SLOW_START_THRESHOLD,
                                                          DegraderImpl.DEFAULT_SLOW_START_THRESHOLD));

      config.setPreemptiveRequestTimeoutRate(MapUtil.getWithDefault(properties,
                                                                    PropertyKeys.DEGRADER_PREEMPTIVE_REQUEST_TIMEOUT_RATE,
                                                                    DegraderImpl.DEFAULT_PREEMPTIVE_REQUEST_TIMEOUT_RATE));
    }
    return config;
  }
}

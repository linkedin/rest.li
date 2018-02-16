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
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Ang Xu
 */
public class DegraderPropertiesConverterTest
{
  @Test
  public void testDegraderPropertiesConverter()
  {
    final Boolean logEnabled = true;
    final Double maxDropRate = 0.4;
    final Double upStep = 0.2;
    final Double downStep = 0.3;
    final Integer minCallCount = 1000;
    final Integer highLatency = 60000;
    final Integer lowLatency = 10000;
    final Double highErrorRate = 0.5;
    final Double lowErrorRate = 0.25;
    final Integer highOutstanding = 1234;
    final Integer lowOutstanding = 123;
    final Integer minOutstandingCount = 5;
    final Long maxDropDuration = 50000l;
    final latencyType latencyToUse = latencyType.PCT50;
    final Double initialDropRate = 0.1;
    final Double slowStartThreshold = 0.32;
    final Double logThreshold = 0.8;

    Map<String, String> degraderProperties = new HashMap<>();
    degraderProperties.put(PropertyKeys.DEGRADER_LOG_ENABLED, logEnabled.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_MAX_DROP_RATE, maxDropRate.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_UP_STEP, upStep.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_DOWN_STEP, downStep.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_MIN_CALL_COUNT, minCallCount.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_HIGH_LATENCY, highLatency.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_LOW_LATENCY, lowLatency.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, highErrorRate.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, lowErrorRate.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_HIGH_OUTSTANDING, highOutstanding.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_LOW_OUTSTANDING, lowOutstanding.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_MIN_OUTSTANDING_COUNT, minOutstandingCount.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_MAX_DROP_DURATION, maxDropDuration.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_LATENCY_TO_USE, latencyToUse.name());
    degraderProperties.put(PropertyKeys.DEGRADER_INITIAL_DROP_RATE, initialDropRate.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_SLOW_START_THRESHOLD, slowStartThreshold.toString());
    degraderProperties.put(PropertyKeys.DEGRADER_LOG_THRESHOLD, logThreshold.toString());

    D2DegraderProperties d2DegraderProperties =
        new D2DegraderProperties()
        .setLogEnabled(logEnabled)
        .setMaxDropRate(maxDropRate)
        .setUpStep(upStep)
        .setDownStep(downStep)
        .setMinCallCount(minCallCount)
        .setHighLatency(highLatency)
        .setLowLatency(lowLatency)
        .setHighErrorRate(highErrorRate)
        .setLowErrorRate(lowErrorRate)
        .setHighOutstanding(highOutstanding)
        .setLowOutstanding(lowOutstanding)
        .setMinOutstandingCount(minOutstandingCount)
        .setMaxDropDuration(maxDropDuration)
        .setLatencyToUse(latencyToUse)
        .setInitialDropRate(initialDropRate)
        .setSlowStartThreshold(slowStartThreshold)
        .setLogThreshold(logThreshold);

    Assert.assertEquals(DegraderPropertiesConverter.toConfig(degraderProperties), d2DegraderProperties);
    Assert.assertEquals(DegraderPropertiesConverter.toProperties(d2DegraderProperties), degraderProperties);
  }
}

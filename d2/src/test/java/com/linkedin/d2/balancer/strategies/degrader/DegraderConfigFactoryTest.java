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


import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.util.degrader.DegraderImpl;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


/**
 * Test for DegraderConfigFactory
 *
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 */
public class DegraderConfigFactoryTest
{
  @Test
  public void testToDegraderConfig()
  {
    Map<String,String> properties = new HashMap<>();;
    Boolean logEnabled = false;
    DegraderImpl.LatencyToUse latencyToUse = DegraderImpl.LatencyToUse.PCT95;
    Double maxDropRate = 0.33;
    Long maxDropDuration = 23190l;
    Double upStep = 0.3;
    Double downstep = 0.2;
    Integer minCallCount = 22;
    Long highLatency = 30000000l;
    Long lowLatency = 244499l;
    Double highErrorRate = 0.5;
    Double lowErrorRate = 0.1;
    Long highOutstanding = 10000l;
    Long lowOutstanding = 3000l;
    Integer minOutstandingCount = 10;
    Integer overrideMinCallCount = 5;
    Double logThreshold = 0.8;
    Double preemptiveRequestTimeoutRate = 0.5;
    properties.put(PropertyKeys.DEGRADER_LOG_ENABLED, logEnabled.toString());
    properties.put(PropertyKeys.DEGRADER_LATENCY_TO_USE, latencyToUse.toString());
    properties.put(PropertyKeys.DEGRADER_MAX_DROP_RATE, maxDropRate.toString());
    properties.put(PropertyKeys.DEGRADER_MAX_DROP_DURATION, maxDropDuration.toString());
    properties.put(PropertyKeys.DEGRADER_UP_STEP, upStep.toString());
    properties.put(PropertyKeys.DEGRADER_DOWN_STEP, downstep.toString());
    properties.put(PropertyKeys.DEGRADER_MIN_CALL_COUNT, minCallCount.toString());
    properties.put(PropertyKeys.DEGRADER_HIGH_LATENCY, highLatency.toString());
    properties.put(PropertyKeys.DEGRADER_LOW_LATENCY, lowLatency.toString());
    properties.put(PropertyKeys.DEGRADER_HIGH_ERROR_RATE, highErrorRate.toString());
    properties.put(PropertyKeys.DEGRADER_LOW_ERROR_RATE, lowErrorRate.toString());
    properties.put(PropertyKeys.DEGRADER_HIGH_OUTSTANDING, highOutstanding.toString());
    properties.put(PropertyKeys.DEGRADER_LOW_OUTSTANDING, lowOutstanding.toString());
    properties.put(PropertyKeys.DEGRADER_MIN_OUTSTANDING_COUNT, minOutstandingCount.toString());
    properties.put(PropertyKeys.DEGRADER_OVERRIDE_MIN_CALL_COUNT, overrideMinCallCount.toString());
    properties.put(PropertyKeys.DEGRADER_LOG_THRESHOLD, logThreshold.toString());
    properties.put(PropertyKeys.DEGRADER_PREEMPTIVE_REQUEST_TIMEOUT_RATE, preemptiveRequestTimeoutRate.toString());
    DegraderImpl.Config config = DegraderConfigFactory.toDegraderConfig(properties);
    assertEquals(config.isLogEnabled(), logEnabled.booleanValue());
    assertEquals(config.getLatencyToUse(), latencyToUse);
    assertEquals(config.getMaxDropRate(), maxDropRate);
    assertEquals(config.getMaxDropDuration(), maxDropDuration.longValue());
    assertEquals(config.getUpStep(), upStep.doubleValue());
    assertEquals(config.getDownStep(), downstep.doubleValue());
    assertEquals(config.getMinCallCount(), minCallCount.intValue());
    assertEquals(config.getHighLatency(), highLatency.longValue());
    assertEquals(config.getLowLatency(), lowLatency.longValue());
    assertEquals(config.getHighErrorRate(), highErrorRate.doubleValue());
    assertEquals(config.getLowErrorRate(), lowErrorRate.doubleValue());
    assertEquals(config.getHighOutstanding(), highOutstanding.longValue());
    assertEquals(config.getLowOutstanding(), lowOutstanding.longValue());
    assertEquals(config.getMinOutstandingCount(), minOutstandingCount.longValue());
    assertEquals(config.getOverrideMinCallCount(), overrideMinCallCount.intValue());
    assertEquals(config.getLogThreshold(), logThreshold);
    assertEquals(config.getPreemptiveRequestTimeoutRate(), preemptiveRequestTimeoutRate);
  }
}

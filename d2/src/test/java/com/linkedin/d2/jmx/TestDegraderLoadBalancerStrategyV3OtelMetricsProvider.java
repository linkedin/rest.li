/*
   Copyright (c) 2026 LinkedIn Corp.

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
package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.clients.PerCallDurationSemantics;


/** Recording test double for {@link DegraderLoadBalancerStrategyV3OtelMetricsProvider}. */
public class TestDegraderLoadBalancerStrategyV3OtelMetricsProvider extends AbstractRecordingOtelMetricsProvider
    implements DegraderLoadBalancerStrategyV3OtelMetricsProvider
{
  @Override
  public void recordHostLatency(String serviceName, String scheme, long hostLatencyMs,
      PerCallDurationSemantics semantics)
  {
    recordLong("recordHostLatency", serviceName, scheme, hostLatencyMs, semantics);
  }

  @Override
  public void updateOverrideClusterDropRate(String serviceName, String scheme, double overrideClusterDropRate)
  {
    recordDouble("updateOverrideClusterDropRate", serviceName, scheme, overrideClusterDropRate);
  }

  @Override
  public void updateTotalPointsInHashRing(String serviceName, String scheme, int totalPointsInHashRing)
  {
    recordInt("updateTotalPointsInHashRing", serviceName, scheme, totalPointsInHashRing);
  }
}

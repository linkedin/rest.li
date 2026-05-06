package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.clients.PerCallDurationSemantics;


/**
 * Test implementation of {@link DegraderLoadBalancerStrategyV3OtelMetricsProvider} that records
 * each invocation for verification. All recording / inspection helpers live on
 * {@link AbstractRecordingOtelMetricsProvider} so the two strategy test fixtures stay in lockstep.
 */
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

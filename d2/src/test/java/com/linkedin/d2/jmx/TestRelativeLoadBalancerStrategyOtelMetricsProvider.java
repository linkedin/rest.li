package com.linkedin.d2.jmx;

import com.linkedin.d2.balancer.clients.PerCallDurationSemantics;


/**
 * Test implementation of {@link RelativeLoadBalancerStrategyOtelMetricsProvider} that records each
 * invocation for verification. All recording / inspection helpers live on
 * {@link AbstractRecordingOtelMetricsProvider} so the two strategy test fixtures stay in lockstep.
 */
public class TestRelativeLoadBalancerStrategyOtelMetricsProvider extends AbstractRecordingOtelMetricsProvider
    implements RelativeLoadBalancerStrategyOtelMetricsProvider
{
  @Override
  public void recordHostLatency(String serviceName, String scheme, long hostLatencyMs,
      PerCallDurationSemantics semantics)
  {
    recordLong("recordHostLatency", serviceName, scheme, hostLatencyMs, semantics);
  }

  @Override
  public void updateTotalHostsInAllPartitionsCount(String serviceName, String scheme, int totalHostsInAllPartitionsCount)
  {
    recordInt("updateTotalHostsInAllPartitionsCount", serviceName, scheme, totalHostsInAllPartitionsCount);
  }

  @Override
  public void updateUnhealthyHostsCount(String serviceName, String scheme, int unhealthyHostsCount)
  {
    recordInt("updateUnhealthyHostsCount", serviceName, scheme, unhealthyHostsCount);
  }

  @Override
  public void updateQuarantineHostsCount(String serviceName, String scheme, int quarantineHostsCount)
  {
    recordInt("updateQuarantineHostsCount", serviceName, scheme, quarantineHostsCount);
  }

  @Override
  public void updateTotalPointsInHashRing(String serviceName, String scheme, int totalPointsInHashRing)
  {
    recordInt("updateTotalPointsInHashRing", serviceName, scheme, totalPointsInHashRing);
  }
}

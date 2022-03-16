package com.linkedin.d2.balancer.clusterfailout;

import com.linkedin.d2.balancer.LoadBalancerState;

/**
 * Factory for creating a {@link FailoutConfigProvider}.
 */
public interface FailoutConfigProviderFactory
{
  FailoutConfigProvider create(LoadBalancerState loadBalancerState);
}

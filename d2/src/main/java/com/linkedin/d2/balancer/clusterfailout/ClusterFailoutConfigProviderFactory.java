package com.linkedin.d2.balancer.clusterfailout;

import com.linkedin.d2.balancer.LoadBalancerState;

/**
 * Factory for creating a {@link ClusterFailoutConfigProvider}.
 */
public interface ClusterFailoutConfigProviderFactory
{
  ClusterFailoutConfigProvider create(LoadBalancerState loadBalancerState);
}

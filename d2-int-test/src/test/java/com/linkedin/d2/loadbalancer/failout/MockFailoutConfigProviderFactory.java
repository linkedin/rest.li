package com.linkedin.d2.loadbalancer.failout;

import com.linkedin.d2.balancer.LoadBalancerState;
import com.linkedin.d2.balancer.clusterfailout.FailoutConfig;
import com.linkedin.d2.balancer.clusterfailout.FailoutConfigProvider;
import com.linkedin.d2.balancer.clusterfailout.FailoutConfigProviderFactory;
import com.linkedin.d2.balancer.clusterfailout.ZKFailoutConfigProvider;
import com.linkedin.d2.balancer.properties.FailoutProperties;

import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MockFailoutConfigProviderFactory implements FailoutConfigProviderFactory
{
  private FailoutConfigProvider _failoutConfigProvider;

  @Override
  public FailoutConfigProvider create(LoadBalancerState loadBalancerState)
  {
    if (_failoutConfigProvider == null)
    {
      _failoutConfigProvider = new MockFailoutConfigProvider(loadBalancerState);
    }
    return _failoutConfigProvider;
  }

  public static class MockFailoutConfigProvider extends ZKFailoutConfigProvider
  {

    public MockFailoutConfigProvider(LoadBalancerState loadBalancerState)
    {
      super(loadBalancerState);
    }

    @Nullable
    @Override
    public FailoutConfig createFailoutConfig(@Nonnull String clusterName, @Nullable FailoutProperties failoutProperties)
    {
      if (failoutProperties == null)
      {
        return null;
      }
      return new FailoutConfig()
      {
        @Override
        public boolean isFailedOut()
        {
          return false;
        }

        @Override
        public Set<String> getPeerClusters()
        {
          return null;
        }
      };
    }
  }
}

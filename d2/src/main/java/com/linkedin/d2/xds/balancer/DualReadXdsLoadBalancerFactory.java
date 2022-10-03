package com.linkedin.d2.xds.balancer;

import com.linkedin.d2.balancer.D2ClientConfig;
import com.linkedin.d2.balancer.LoadBalancerWithFacilities;
import com.linkedin.d2.balancer.LoadBalancerWithFacilitiesFactory;
import com.linkedin.d2.balancer.dualread.DualReadLoadBalancer;
import com.linkedin.d2.balancer.dualread.DualReadModeProvider;
import java.util.concurrent.Executors;


public class DualReadXdsLoadBalancerFactory implements LoadBalancerWithFacilitiesFactory
{
  private final LoadBalancerWithFacilitiesFactory _oldLbFactory;
  private final LoadBalancerWithFacilitiesFactory _newLbFactory;
  private final DualReadModeProvider _dualReadModeProvider;

  public DualReadXdsLoadBalancerFactory(LoadBalancerWithFacilitiesFactory oldLbFactory,
      LoadBalancerWithFacilitiesFactory newLbFactory, DualReadModeProvider dualReadModeProvider)
  {
    _oldLbFactory = oldLbFactory;
    _newLbFactory = newLbFactory;
    _dualReadModeProvider = dualReadModeProvider;
  }

  @Override
  public LoadBalancerWithFacilities create(D2ClientConfig config)
  {
    return new DualReadLoadBalancer(_oldLbFactory.create(config), _newLbFactory.create(config), _dualReadModeProvider,
        Executors.newSingleThreadScheduledExecutor(), 10);
  }
}

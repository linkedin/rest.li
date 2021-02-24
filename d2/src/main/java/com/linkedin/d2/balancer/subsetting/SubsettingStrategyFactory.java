package com.linkedin.d2.balancer.subsetting;

import com.linkedin.d2.balancer.properties.ServiceProperties;
import java.net.URI;


public interface SubsettingStrategyFactory
{
  SubsettingStrategyFactory NO_OP_SUBSETTING_STRATEGY_FACTORY = (serviceName, serviceProperties, partitionId) -> null;

  /**
   * get retrieves the {@link SubsettingStrategy} corresponding to the serviceName and partition Id
   * @return {@link SubsettingStrategy}
   */
   SubsettingStrategy<URI> get(String serviceName, ServiceProperties serviceProperties, int partitionId);
}

package com.linkedin.d2.balancer.util;

import com.linkedin.d2.D2CanaryDistributionStrategy;


/**
 * Canary Distribution Provider is supposed to provide information about canary distributions.
 */
public interface CanaryDistributionProvider {

  /**
   * Decide the canary distribution given a distribution strategy.
   * @param strategy a canary distribution strategy
   * @return the distribution result
   */
  Distribution distribute(D2CanaryDistributionStrategy strategy);

  /**
   * Canary distributions.
   * STABLE - to use the stable config.
   * CANARY - to use the canary config.
   */
  enum Distribution {
    STABLE,
    CANARY
  }
}

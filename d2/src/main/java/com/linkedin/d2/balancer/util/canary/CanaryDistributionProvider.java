/*
   Copyright (c) 2022 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util.canary;

import com.linkedin.d2.D2CanaryDistributionStrategy;


/**
 * Provide information about canary distributions. Canary distributions can be used to ramp new D2 configs with a portion of clients
 * before being fully deployed to all. It can also be used in any scenario that needs to distribute D2 clients into stable vs canary groups.
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

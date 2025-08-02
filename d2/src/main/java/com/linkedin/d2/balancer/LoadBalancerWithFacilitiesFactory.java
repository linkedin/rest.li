/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.d2.balancer;

import com.linkedin.d2.discovery.util.D2Utils;
import javax.annotation.Nonnull;
import org.slf4j.Logger;


/**
 * Factory for creating instance of {@link LoadBalancerWithFacilities}
 */
public interface LoadBalancerWithFacilitiesFactory
{
  String LOAD_BALANCER_TYPE_WARNING = "[ACTION REQUIRED] Zookeeper-based D2 Client "
      + "is deprecated (unless talking to a locally-deployed ZK, or for testing EI ZK) and must be migrated to INDIS. "
      + "See instructions at go/onboardindis.\n"
      + "Failing to do so will block other apps from stopping ZK announcements and will be escalated for site-up "
      + "stability.";

  /**
   * Creates instance of {@link LoadBalancerWithFacilities}
   * @param config configuration of d2 client
   * @return new instance of {@link LoadBalancerWithFacilities}
   */
  LoadBalancerWithFacilities create(D2ClientConfig config);

  default void logLoadBalancerTypeWarning(@Nonnull Logger LOG)
  {
    LOG.error(LOAD_BALANCER_TYPE_WARNING);
  }

  default void logAppProps(@Nonnull Logger LOG)
  {
    LOG.info("LI properties:\n {}", D2Utils.getSystemProperties());
  }
}

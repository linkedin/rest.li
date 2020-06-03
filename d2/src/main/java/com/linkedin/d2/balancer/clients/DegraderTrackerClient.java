/*
   Copyright (c) 2020 LinkedIn Corp.

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
package com.linkedin.d2.balancer.clients;

import com.linkedin.util.degrader.CallTracker;
import com.linkedin.util.degrader.Degrader;
import com.linkedin.util.degrader.DegraderControl;

/**
 * {@link TrackerClient} that contains additional methods needed for degrader strategy.
 *
 * @see com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3
 */
public interface DegraderTrackerClient extends TrackerClient
{

  /**
   * @param partitionId Partition ID.
   * @return Degrader corresponding to the given partition.
   */
  Degrader getDegrader(int partitionId);

  /**
   * @param partitionId Partition ID.
   * @return DegraderControl corresponding to the given partition.
   */
  DegraderControl getDegraderControl(int partitionId);
}

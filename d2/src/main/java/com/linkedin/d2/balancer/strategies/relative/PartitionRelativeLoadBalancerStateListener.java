/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.d2.balancer.strategies.relative;


/**
 * The listener that listens to the change of a partition in relative load balancer
 */
public interface PartitionRelativeLoadBalancerStateListener
{
  void onUpdate(PartitionRelativeLoadBalancerState state);

  /**
   * Creates an instance of {@link PartitionRelativeLoadBalancerStateListener} with a given partition ID.
   */
  interface Factory
  {
    /**
     * Creates an instance of {@link PartitionRelativeLoadBalancerStateListener}.
     * @param partitionId Paritition ID
     * @return An instance of {@link PartitionRelativeLoadBalancerStateListener} with the partition ID.
     */
    PartitionRelativeLoadBalancerStateListener create(int partitionId);
  }
}

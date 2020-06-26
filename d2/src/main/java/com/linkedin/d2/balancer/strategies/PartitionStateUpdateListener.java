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

package com.linkedin.d2.balancer.strategies;


/**
 * The listener that listens to the change of a partition in the load balancer
 */
public interface PartitionStateUpdateListener<T>
{
  void onUpdate(T state);

  /**
   * Creates an instance of {@link PartitionStateUpdateListener} with a given partition ID.
   */
  interface Factory<T>
  {
    /**
     * Creates an instance of {@link PartitionStateUpdateListener}.
     * @param partitionId Partition ID
     * @return An instance of {@link PartitionStateUpdateListener} with the partition ID.
     */
    PartitionStateUpdateListener<T> create(int partitionId);
  }
}

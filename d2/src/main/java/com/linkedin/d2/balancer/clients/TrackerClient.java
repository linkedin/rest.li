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

import java.util.Map;

import javax.annotation.Nullable;

import com.linkedin.d2.balancer.LoadBalancerClient;
import com.linkedin.d2.balancer.properties.PartitionData;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.util.degrader.CallTracker;

/**
 * A client that tracks call stats and supports partitioning.
 */
public interface TrackerClient extends LoadBalancerClient
{

  /**
   * @return CallStats tracked in the latest interval.
   */
  CallTracker.CallStats getLatestCallStats();

  /**
   * @return {@link PartitionData} map.
   */
  Map<Integer, PartitionData> getPartitionDataMap();

  /**
   * @return {@link TransportClient} that sends the requests.
   */
  TransportClient getTransportClient();

  /**
   * @return is the host should not perform slow start
   */
  boolean doNotSlowStart();

  /**
   * @param partitionId Partition ID key.
   * @return Weight of specified partition or null if no partition with the ID exists.
   */
  @Nullable
  default Double getPartitionWeight(int partitionId)
  {
    PartitionData partitionData = getPartitionDataMap().get(partitionId);
    return partitionData == null ? null : partitionData.getWeight();
  }

  /**
   * @return CallTracker.
   */
  CallTracker getCallTracker();
}

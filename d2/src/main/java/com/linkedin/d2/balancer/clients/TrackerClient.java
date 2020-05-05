package com.linkedin.d2.balancer.clients;

import java.util.Map;

import javax.annotation.Nullable;

import com.linkedin.d2.balancer.LoadBalancerClient;
import com.linkedin.d2.balancer.properties.PartitionData;
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
   * @param partitionId Partition ID key.
   * @return Weight of specified partition or null if no partition with the ID exists.
   */
  @Nullable
  default Double getPartitionWeight(int partitionId)
  {
    PartitionData partitionData = getPartitionDataMap().get(partitionId);
    return partitionData == null ? null : partitionData.getWeight();
  }
}

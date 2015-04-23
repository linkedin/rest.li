package com.linkedin.d2.balancer.util;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * This is the return type of KepMapper.getAllPartitionsMultipleHosts
 * {@see com.linkedin.d2.balancer.util.HostToKeyMapper}
 *
 * @author Xialin Zhu
 */
public interface HostSet
{
  /**
   * Get the union set of hosts in all partitions
   */
  public List<URI> getAllHosts();

  /**
   * Get hosts for a particular partition
   */
  public List<URI> getHosts(int partitionId);

  public int getPartitionCount();

  public Map<Integer, Integer> getPartitionsWithoutEnoughHosts();
}

/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util.partitions;


import com.linkedin.d2.balancer.ServiceUnavailableException;
import com.linkedin.d2.balancer.util.HostToKeyMapper;
import java.net.URI;
import java.util.Collection;


/**
 * provides partition information for a specific d2 service
 *
 * @author Oby Sumampouw
 */
public interface PartitionInfoProvider
{
  /**
   * Given a service Uri and a limit, this returns map of partitionId -> selection of hosts in that partition and also
   * the keys that belong to that partition. This also returns the limit of keys the services can take.
   *
   * @param serviceUri for example d2://articles
   * @param limitHostPerPartition the number of hosts that we should return for this partition. Must be larger than 0.
   *                              if there are not enough hosts in the partition we will return as many as we can
   * @param keys all the keys we want to find the partition for.
   *             if it's null we will return hosts in all partitions
   * @param hash this will be used to help determine the host uri that we return
   * @return
   * @throws ServiceUnavailableException
   */
  public <K> HostToKeyMapper<K> getPartitionInformation(URI serviceUri, Collection<K> keys, int limitHostPerPartition, int hash)
      throws ServiceUnavailableException;


  /**
   * Provides a partitionAccessor object that can tell which partition a key belongs to.
   * 
   * @param serviceUri for example d2://articles
   * @return partitionAccessor
   * @throws ServiceUnavailableException
   */
  public PartitionAccessor getPartitionAccessor(URI serviceUri) throws ServiceUnavailableException;
}

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
import com.linkedin.d2.balancer.util.AllPartitionsMultipleHostsResult;
import com.linkedin.d2.balancer.util.MapKeyHostPartitionResult;
import java.net.URI;
import java.util.Collection;
import java.util.List;


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
   * To determine which hosts that we return, we rely on the user-provided HashProvider.
   *
   * Note: If there are 5 hosts in partition 1 and 4 hosts in partition 2. If you pass limitHostPerPartition == 10,
   * We will still return 5 hosts in partition 1 and 4 hosts in partition 2. If you pass limitHostPerPartition == 2
   * then we only return 2 hosts in partition 1 and 2 hosts in partition 2.
   *
   * @param serviceUri for example d2://articles
   * @param limitHostPerPartition the number of hosts that we should return for this partition. Must be larger than 0.
   * @param keys all the keys we want to find the partition for
   * @param hashProvider this will be used to help determine the host uri that we return
   * @return
   * @throws ServiceUnavailableException
   */
  public <K> MapKeyHostPartitionResult<K> getPartitionInformation(URI serviceUri, Collection<K> keys,
                                                                        int limitHostPerPartition,
                                                                        HashProvider hashProvider)
      throws ServiceUnavailableException;

  /**
   * Provides a partitionAccessor object that can tell which partition a key belongs to.
   *
   * @param serviceUri for example d2://articles
   * @return partitionAccessor
   * @throws ServiceUnavailableException
   */
  public PartitionAccessor getPartitionAccessor(URI serviceUri) throws ServiceUnavailableException;

  /**
   * This method returns a mapping of each partition to the hosts that hold that partition. We return numHostPerPartition
   * number of hosts for each partition. If so many hosts are not available then we put it into a unavailablePartitions map.
   * @param serviceUri for example d2://articles
   * @param numHostPerPartition Number of hosts to be returned for each partition
   * @param hashProvider Hash provider to access elements of a ring
   * @return AllPartitionsMultipleHostsResult<URI> that contains the partitionId to hosts mapping
   */
  public AllPartitionsMultipleHostsResult<URI> getAllPartitionMultipleHosts(URI serviceUri,
                                                                                int numHostPerPartition,
                                                                                HashProvider hashProvider)
      throws ServiceUnavailableException;

  public interface HashProvider
  {
    /**
     * this method returns an int that we use as a key with {@link com.linkedin.d2.balancer.util.hashing.Ring}
     * to select URI to be included in the result of getPartitionInformation. The most basic implementation
     * should return a random number every time it's called.
     *
     * But if you want the result from getPartitionInformation to always return the same URI host,
     * then nextHash() should return the same ordered list of number when called multiple times.
     *
     * @return
     */
    public int nextHash();
  }

}

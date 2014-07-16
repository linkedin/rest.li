/*
   Copyright (c) 2012 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * AllPartitionsMultipleHostsResult includes a map of partition id covering all available partitions
 * to unique host URIs, an int of the total partition number, and a map of unavailable partitions to number of hosts
 * that could not be found for that partition.
 * Caller should check the missing partitions
 * if they want to verify if all partitions are available.
 * A partition is put into unavailablePartitions if no host URI is registered for the partition
 * or some of them are not working
 */
public class AllPartitionsMultipleHostsResult<T>
{
  private final Map<Integer, List<T>> _partitionInfo;
  private final int _partitionCount;
  private final Map<Integer, Integer> _partitionsWithoutEnoughHosts;

  public AllPartitionsMultipleHostsResult(Map<Integer, List<T>> hostsInfo, int partitionCount, Map<Integer, Integer> partitionsWithoutEnoughHosts)
  {
    _partitionInfo = Collections.unmodifiableMap(hostsInfo);
    _partitionCount = partitionCount;
    _partitionsWithoutEnoughHosts = Collections.unmodifiableMap(partitionsWithoutEnoughHosts);
  }

  public List<T> getPartitionInfo(int partitionId)
  {
    if (_partitionInfo.containsKey(partitionId))
    {
      return Collections.unmodifiableList(_partitionInfo.get(partitionId));
    }
    else
    {
      throw new IllegalArgumentException("PartitionId " + partitionId + " is not found");
    }
  }

  public int getPartitionCount()
  {
    return _partitionCount;
  }

  public Map<Integer, Integer> getPartitionsWithoutEnoughHosts()
  {
    return _partitionsWithoutEnoughHosts;
  }
}

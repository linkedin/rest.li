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

import java.util.Collection;
import java.util.Collections;

/**
 * AllPartitionsResult includes a collection of unique host URIs which cover all available partitions,
 * an int of the total partition number, and a collection of available partitions.
 * Caller should check the missing partitions
 * if they want to verify if all partitions are available.
 * A partition is put into unavailablePartitions if no host URI is registered for the partition
 * or none of them is working
 */
public class AllPartitionsResult<T>
{
  private final Collection<T> _partitionInfo;
  private final int _partitionCount;
  private final Collection<Integer> _unavailablePartitions;

  public AllPartitionsResult(Collection<T> hostsInfo, int partitionCount, Collection<Integer> unavailablePartitions)
  {
    _partitionInfo = Collections.unmodifiableCollection(hostsInfo);
    _partitionCount = partitionCount;
    _unavailablePartitions = Collections.unmodifiableCollection(unavailablePartitions);
  }

  public Collection<T> getPartitionInfo()
  {
    return _partitionInfo;
  }

  public int getPartitionCount()
  {
    return _partitionCount;
  }

  public Collection<Integer> getUnavailablePartitions()
  {
    return _unavailablePartitions;
  }
}

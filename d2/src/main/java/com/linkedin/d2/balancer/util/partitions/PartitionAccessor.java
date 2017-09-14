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

package com.linkedin.d2.balancer.util.partitions;

import java.net.URI;

/**
 * This is the accessor for partition properties. It is here because we want to keep partition properties
 * as data storage only and move the logic of manipulating the data to a separate place.
 * To get a PartitionAccessor, one should use {@link PartitionAccessorFactory}
 */
public interface PartitionAccessor extends BasePartitionAccessor
{
  /**
   * We're moving towards using BasePartitionAccessor for all partition accesses
   * (including both loadbalancing and keyMapping). The default
   * implementation here is used for backward compatibility purpose.
   * Will be deprecated after all the existing users get updated.
   *
   * @param key: input key
   * @return partitionId
   * @throws PartitionAccessException
   */
  default int getPartitionId(String key) throws PartitionAccessException
  {
    URI uri;
    try
    {
      uri = URI.create(key);
    }
    catch (IllegalArgumentException e)
    {
      throw new PartitionAccessException(e);
    }
    return getPartitionId(uri);
  }

  /**
   *
   * @return MaxPartitionId for the cluster
   */
  int getMaxPartitionId();
}


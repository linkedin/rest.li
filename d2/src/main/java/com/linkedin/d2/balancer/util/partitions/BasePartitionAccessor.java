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


package com.linkedin.d2.balancer.util.partitions;

import java.net.URI;
import java.util.Objects;


/**
 * BasePartitionAccessor returns partitionId according to the given URI.
 *
 */
public interface BasePartitionAccessor
{
  /**
   * Given uri as input, return the corresponding partitionID
   *
   * @param uri input URI
   * @return partitionID
   * @throws PartitionAccessException see {@link PartitionAccessException}
   */
  int getPartitionId(URI uri) throws PartitionAccessException;

  /**
   * Given the setting of the partition accessor, check if the setting can be supported
   * @return true if supportable
   */
  default boolean checkSupportable(String settings) {
    return Objects.equals(getClass().getSimpleName(), settings);
  }
}


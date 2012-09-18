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

import com.linkedin.d2.balancer.properties.HashBasedPartitionProperties;
import com.linkedin.d2.balancer.properties.PartitionProperties;
import com.linkedin.d2.balancer.properties.RangeBasedPartitionProperties;

/**
 * This is the factory to create {@link PartitionAccessor} for different partition properites
 *
 */
public class PartitionAccessorFactory
{
  public static PartitionAccessor getPartitionAccessor(PartitionProperties properties)
  {
    switch(properties.getPartitionType())
    {
      case RANGE:
        return new RangeBasedPartitionAccessor((RangeBasedPartitionProperties)properties);
      case HASH:
        return new HashBasedPartitionAccessor((HashBasedPartitionProperties)properties);
      case NONE:
        return DefaultPartitionAccessor.getInstance();
      default:
        break;
    }

    throw new IllegalArgumentException("Unsupported partition properties type.");
  }
}

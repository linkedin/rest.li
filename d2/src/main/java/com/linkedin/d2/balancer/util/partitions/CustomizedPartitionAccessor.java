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

import com.linkedin.d2.balancer.properties.CustomizedPartitionProperties;

import java.net.URI;

/**
 * CustomizedPartitionAccessor implements PartitionAccessor interface to provide partition properties for
 * the custom partition.
 */
public class CustomizedPartitionAccessor implements PartitionAccessor
{
  final private int _maxPartitionId;
  final private BasePartitionAccessor _partitionAccessor;

  public CustomizedPartitionAccessor(CustomizedPartitionProperties properties, BasePartitionAccessor partitionAccessor)
  {
    _maxPartitionId = properties.getPartitionCount() - 1;
    _partitionAccessor = partitionAccessor;
  }

  @Override
  public int getPartitionId(URI uri)
      throws PartitionAccessException
  {
    return _partitionAccessor.getPartitionId(uri);
  }

  @Override
  public int getMaxPartitionId()
  {
    return _maxPartitionId;
  }
}

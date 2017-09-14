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

package com.linkedin.d2.balancer.properties;

import java.util.List;

/**
 * Properties for Custom Partition
 */

public class CustomizedPartitionProperties implements PartitionProperties
{
  private final int _partitionCount;
  private final List<String> _partitionAccessorList;

  public CustomizedPartitionProperties(int partitionCount, List<String> partitionAccessorList)
  {
    _partitionCount = partitionCount;
    _partitionAccessorList = partitionAccessorList;
  }

  @Override
  public PartitionType getPartitionType()
  {
    return PartitionType.CUSTOM;
  }

  public int getPartitionCount()
  {
    return _partitionCount;
  }

  public List<String> getPartitionAccessorList()
  {
    return _partitionAccessorList;
  }
}

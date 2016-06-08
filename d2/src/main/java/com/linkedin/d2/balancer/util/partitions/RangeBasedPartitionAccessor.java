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

import com.linkedin.d2.balancer.properties.RangeBasedPartitionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangeBasedPartitionAccessor extends AbstractPartitionAccessor
{
  final private RangeBasedPartitionProperties _properties;
  private static final Logger _log = LoggerFactory.getLogger(RangeBasedPartitionAccessor.class);

  public RangeBasedPartitionAccessor(RangeBasedPartitionProperties properties)
  {
    super(properties.getPartitionKeyRegex(), properties.getPartitionCount() - 1);
    _properties = properties;
  }

  @Override
  public int getPartitionId(String key)
      throws PartitionAccessException
  {
    try
    {
      long longKey = Long.parseLong(key);
      if (longKey < 0)
      {
        throw new PartitionAccessException("Partition key needs to be non negative in range based partitions: " + longKey);
      }
      int partitionId = (int) ((longKey - _properties.getKeyRangeStart()) / _properties.getPartitionSize());
      int partitionCount = _properties.getPartitionCount();
      if (partitionId >= partitionCount || partitionId < 0)
      {
        throw new PartitionAccessException("Partition id out of range: " + partitionId + ", partitionId range is [0, "+
        + (partitionCount - 1) + "]" );
      }
      _log.debug("Getting partitionId for key ({}): {}", key, partitionId);
      return partitionId;
    }
    catch (NumberFormatException e)
    {
      throw new PartitionAccessException("Using RangeBasedPartitions. Keys should be long values, but failed to parse key to long: " + key);
    }
  }
}
